import { adminDb, FieldValue } from '../firebaseAdmin.js';
import { requireUser } from '../authUser.js';
import { readBody, requireString, notFound, conflict, forbidden, HttpError } from '../http.js';

const DEFAULT_BANK = {
  bankName: 'BCA',
  bankAccountNumber: '1234567890',
  bankAccountName: 'PT JepretAja Indonesia',
};

const MIDTRANS_ORDER_TTL_MS = 24 * 60 * 60 * 1000;

function midtransBaseUrl() {
  return process.env.MIDTRANS_ENV === 'production'
    ? 'https://app.midtrans.com'
    : 'https://app.sandbox.midtrans.com';
}

/**
 * Membuat transaksi Snap di Midtrans dan mengembalikan token + redirect URL.
 *
 * order_id Midtrans bersifat sekali pakai selamanya per merchant — tidak
 * boleh dipakai ulang bahkan untuk transaksi yang gagal/kedaluwarsa — jadi
 * setiap panggilan di sini memakai order_id baru (bookingId + timestamp),
 * berbeda dari transfer manual yang boleh menerbitkan instruksi yang sama.
 */
async function createMidtransSnapTransaction({ bookingId, booking, actor, amount }) {
  const serverKey = process.env.MIDTRANS_SERVER_KEY;
  if (!serverKey) {
    throw conflict('Metode pembayaran Midtrans belum dikonfigurasi. Hubungi admin.');
  }

  const orderId = `${bookingId}-${Date.now()}`;
  const namaPelanggan = actor.profile?.name || actor.email || 'Pelanggan JepretAja';
  const [firstName, ...rest] = namaPelanggan.trim().split(/\s+/);

  const payload = {
    transaction_details: { order_id: orderId, gross_amount: amount },
    credit_card: { secure: true },
    customer_details: {
      first_name: firstName || 'Pelanggan',
      last_name: rest.join(' ') || undefined,
      email: actor.email || undefined,
      phone: actor.profile?.phone || undefined,
    },
    item_details: [{
      id: bookingId,
      price: amount,
      quantity: 1,
      name: (booking.packageName || 'Paket pemotretan JepretAja').slice(0, 50),
    }],
  };

  let response;
  try {
    response = await fetch(`${midtransBaseUrl()}/snap/v1/transactions`, {
      method: 'POST',
      headers: {
        Authorization: `Basic ${Buffer.from(`${serverKey}:`).toString('base64')}`,
        'Content-Type': 'application/json',
        Accept: 'application/json',
      },
      body: JSON.stringify(payload),
    });
  } catch {
    throw new HttpError(502, 'midtrans-unreachable', 'Tidak bisa menghubungi gateway pembayaran. Coba lagi.');
  }

  const data = await response.json().catch(() => ({}));
  if (!response.ok || !data.token) {
    const pesan = Array.isArray(data.error_messages) ? data.error_messages.join(', ') : null;
    throw new HttpError(502, 'midtrans-error', pesan || 'Gateway pembayaran menolak permintaan. Coba lagi.');
  }

  return { orderId, token: data.token, redirectUrl: data.redirect_url };
}

export function computeUniqueCode(bookingId) {
  if (!bookingId || !bookingId.trim()) return 111;
  const seed = [...bookingId].reduce((sum, ch) => sum + ch.charCodeAt(0), 0);
  return ((seed % 900) + 100) % 1000;
}

export function buildTransferInstruction({
  bookingId,
  amount,
  bankName,
  bankAccountNumber,
  bankAccountName,
  expiresAtMs,
}) {
  const uniqueCode = computeUniqueCode(bookingId);
  const transferAmount = Number(amount) + uniqueCode;
  const expiresAt = Number(expiresAtMs || Date.now() + 24 * 60 * 60 * 1000);

  return {
    status: 'awaiting_transfer',
    provider: 'transfer_manual',
    method: 'bank_transfer',
    uniqueCode,
    transferAmount,
    amount: Number(amount),
    bankName: bankName || DEFAULT_BANK.bankName,
    bankAccountNumber: bankAccountNumber || DEFAULT_BANK.bankAccountNumber,
    bankAccountName: bankAccountName || DEFAULT_BANK.bankAccountName,
    expiresAt,
    instruction: `Transfer sebesar Rp ${transferAmount.toLocaleString('id-ID')} ke rekening ${bankName || DEFAULT_BANK.bankName} ${bankAccountNumber || DEFAULT_BANK.bankAccountNumber} a.n. ${bankAccountName || DEFAULT_BANK.bankAccountName}. Masukkan kode unik ${uniqueCode} agar pembayaran bisa dicocokkan.`,
  };
}

/**
 * Membuat instruksi/transaksi pembayaran untuk booking.
 *
 * Dua jalur aktif:
 * - `transfer_manual` (default): pelanggan membayar ke rekening yang
 *   ditetapkan di settings/general, admin memverifikasi bukti transfer di
 *   panel web lewat confirmManualPayment.
 * - `midtrans`: server membuat transaksi Snap sungguhan ke Midtrans (GoPay,
 *   QRIS, kartu, VA bank, dll — semua kanal yang diaktifkan di dashboard
 *   Midtrans muncul di satu halaman Snap yang sama), dan status finalnya
 *   datang lewat webhook di api/midtrans.js.
 */
export async function createPaymentOrder(req) {
  const actor = await requireUser(req);
  const body = readBody(req);
  const bookingId = requireString(body.bookingId, 'bookingId');
  const method = body.method === 'midtrans' ? 'midtrans' : 'transfer_manual';

  const db = adminDb();
  const bookingRef = db.collection('bookings').doc(bookingId);
  const bookingSnap = await bookingRef.get();
  if (!bookingSnap.exists) throw notFound('Booking tidak ditemukan.');

  const booking = bookingSnap.data();
  if (booking.customerId !== actor.uid) throw forbidden('Booking ini bukan milik Anda.');
  if (booking.status !== 'pending_payment') {
    throw conflict(`Booking berstatus "${booking.status}", tidak menunggu pembayaran.`);
  }

  if (booking.paymentId) {
    const paymentRef = db.collection('payments').doc(booking.paymentId);
    const existing = await paymentRef.get();
    const old = existing.data();

    if (old?.provider === 'transfer_manual' && ['awaiting_transfer', 'paid', 'rejected'].includes(old.status) && method === 'transfer_manual') {
      return {
        paymentId: paymentRef.id,
        ...buildTransferInstruction({
          bookingId,
          amount: Number(old.amount) || Number(booking.total) || 0,
          bankName: old.bankName || DEFAULT_BANK.bankName,
          bankAccountNumber: old.bankAccountNumber || DEFAULT_BANK.bankAccountNumber,
          bankAccountName: old.bankAccountName || DEFAULT_BANK.bankAccountName,
          expiresAtMs: old.expiresAt?.toMillis?.() ?? Date.now() + 24 * 60 * 60 * 1000,
        }),
      };
    }

    // Transaksi Snap Midtrans yang masih berlaku (belum lewat masa aktifnya)
    // dikembalikan apa adanya, supaya menekan ulang "Bayar Sekarang" tidak
    // membuat transaksi baru yang sia-sia di Midtrans.
    if (old?.provider === 'midtrans' && old.status === 'pending' && method === 'midtrans' && Number(old.expiresAt) > Date.now()) {
      return {
        paymentId: paymentRef.id,
        bookingId,
        amount: old.amount,
        redirectUrl: old.redirectUrl,
        snapToken: old.snapToken,
        provider: 'midtrans',
        status: 'pending',
      };
    }

    // Kalau tidak — pembayaran lama sudah final (rejected/expired) atau
    // pelanggan berpindah metode — biarkan lanjut ke bawah untuk menerbitkan
    // pembayaran baru, KECUALI kalau masih benar-benar aktif diproses.
    if (old && old.status !== 'rejected' && old.status !== 'expired' && !(Number(old.expiresAt) > 0 && Number(old.expiresAt) <= Date.now())) {
      throw conflict('Pembayaran untuk booking ini sedang diproses. Tunggu sebentar lalu coba lagi.');
    }
  }

  const amount = Number(booking.total) || 0;
  if (amount <= 0) throw conflict('Total booking tidak valid untuk pembayaran.');

  if (method === 'midtrans') {
    const mt = await createMidtransSnapTransaction({ bookingId, booking, actor, amount });
    const paymentRef = db.collection('payments').doc(`booking_${bookingId}`);
    const expiresAt = Date.now() + MIDTRANS_ORDER_TTL_MS;

    await db.runTransaction(async (tx) => {
      const latestBooking = await tx.get(bookingRef);
      const latest = latestBooking.data();
      if (!latest || latest.customerId !== actor.uid) throw forbidden('Booking ini bukan milik Anda.');
      if (latest.status !== 'pending_payment') throw conflict(`Booking berstatus "${latest.status}", tidak menunggu pembayaran.`);

      tx.set(paymentRef, {
        paymentId: paymentRef.id,
        bookingId,
        customerId: actor.uid,
        creatorId: latest.creatorId,
        provider: 'midtrans',
        method: 'midtrans',
        amount,
        orderId: mt.orderId,
        snapToken: mt.token,
        redirectUrl: mt.redirectUrl,
        status: 'pending',
        expiresAt,
        createdAt: FieldValue.serverTimestamp(),
        updatedAt: FieldValue.serverTimestamp(),
      });

      tx.update(bookingRef, {
        paymentId: paymentRef.id,
        updatedAt: FieldValue.serverTimestamp(),
      });
    });

    return {
      paymentId: paymentRef.id,
      bookingId,
      amount,
      redirectUrl: mt.redirectUrl,
      snapToken: mt.token,
      provider: 'midtrans',
      status: 'pending',
    };
  }

  const settingsSnap = await db.collection('settings').doc('general').get();
  const settings = settingsSnap.data() || {};
  const bankName = (settings.payoutBankName || '').trim() || DEFAULT_BANK.bankName;
  const bankAccountNumber = (settings.payoutAccountNumber || '').trim() || DEFAULT_BANK.bankAccountNumber;
  const bankAccountName = (settings.payoutAccountName || '').trim() || DEFAULT_BANK.bankAccountName;

  if (!bankAccountNumber || bankAccountNumber === '1234567890' || !bankAccountName || bankAccountName === 'PT JepretAja Indonesia') {
    throw conflict('Rekening tujuan transfer manual belum dikonfigurasi. Hubungi admin untuk mengisi pengaturan pembayaran.');
  }

  const paymentRef = db.collection('payments').doc(`booking_${bookingId}`);
  const expiresAt = Date.now() + 24 * 60 * 60 * 1000;
  const instruction = buildTransferInstruction({
    bookingId,
    amount,
    bankName,
    bankAccountNumber,
    bankAccountName,
    expiresAtMs: expiresAt,
  });

  await db.runTransaction(async (tx) => {
    const latestBooking = await tx.get(bookingRef);
    const latest = latestBooking.data();
    if (!latest || latest.customerId !== actor.uid) throw forbidden('Booking ini bukan milik Anda.');
    if (latest.status !== 'pending_payment') throw conflict(`Booking berstatus "${latest.status}", tidak menunggu pembayaran.`);

    tx.set(paymentRef, {
      paymentId: paymentRef.id,
      bookingId,
      customerId: actor.uid,
      creatorId: latest.creatorId,
      provider: 'transfer_manual',
      method: 'bank_transfer',
      amount,
      uniqueCode: instruction.uniqueCode,
      transferAmount: instruction.transferAmount,
      bankName,
      bankAccountNumber,
      bankAccountName,
      status: 'awaiting_transfer',
      expiresAt: instruction.expiresAt,
      createdAt: FieldValue.serverTimestamp(),
      updatedAt: FieldValue.serverTimestamp(),
    });

    tx.update(bookingRef, {
      paymentId: paymentRef.id,
      updatedAt: FieldValue.serverTimestamp(),
    });
  });

  return {
    paymentId: paymentRef.id,
    bookingId,
    amount,
    uniqueCode: instruction.uniqueCode,
    transferAmount: instruction.transferAmount,
    bankName,
    bankAccountNumber,
    bankAccountName,
    expiresAt: instruction.expiresAt,
    instruction: instruction.instruction,
    provider: 'transfer_manual',
    status: 'awaiting_transfer',
  };
}
