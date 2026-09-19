import crypto from 'node:crypto';
import { adminDb, FieldValue } from './_lib/firebaseAdmin.js';
import { writeNotification } from './_lib/notify.js';

function validSignature(body) {
  const key = process.env.MIDTRANS_SERVER_KEY;
  if (!key || !body.order_id || !body.status_code || !body.gross_amount || !body.signature_key) return false;
  const expected = crypto.createHash('sha512')
    .update(`${body.order_id}${body.status_code}${body.gross_amount}${key}`)
    .digest('hex');
  const actual = Buffer.from(body.signature_key);
  const wanted = Buffer.from(expected);
  return actual.length === wanted.length && crypto.timingSafeEqual(wanted, actual);
}

function paymentStatus(body) {
  if (body.transaction_status === 'settlement') return 'paid';
  if (body.transaction_status === 'capture' && body.fraud_status !== 'deny') return 'paid';
  if (body.transaction_status === 'pending') return 'pending';
  if (['deny', 'cancel', 'expire', 'failure'].includes(body.transaction_status)) return 'rejected';
  return null;
}

export default async function handler(req, res) {
  if (req.method !== 'POST') return res.status(405).json({ error: 'Method not allowed' });
  const body = typeof req.body === 'string' ? JSON.parse(req.body) : (req.body || {});
  if (!validSignature(body)) return res.status(401).json({ error: 'Invalid signature' });
  const nextStatus = paymentStatus(body);
  if (!nextStatus) return res.status(200).json({ ok: true, ignored: true });

  const db = adminDb();
  const matches = await db.collection('payments').where('orderId', '==', body.order_id).limit(1).get();
  if (matches.empty) return res.status(404).json({ error: 'Payment not found' });
  const paymentRef = matches.docs[0].ref;

  await db.runTransaction(async (tx) => {
    const paymentSnap = await tx.get(paymentRef);
    const payment = paymentSnap.data();
    if (!payment || payment.status === 'paid' || payment.status === 'rejected') return;
    const bookingRef = db.collection('bookings').doc(payment.bookingId);
    const bookingSnap = await tx.get(bookingRef);
    if (!bookingSnap.exists) return;
    const booking = bookingSnap.data();
    tx.update(paymentRef, {
      status: nextStatus,
      transactionStatus: body.transaction_status,
      transactionId: body.transaction_id || null,
      statusUpdatedAt: FieldValue.serverTimestamp(),
      ...(nextStatus === 'paid' ? { paidAt: FieldValue.serverTimestamp() } : {}),
    });
    if (nextStatus === 'paid' && booking.status === 'pending_payment') {
      tx.update(bookingRef, { status: 'paid', paidAt: FieldValue.serverTimestamp(), updatedAt: FieldValue.serverTimestamp() });
      const amount = Number(payment.amount) || 0;
      const feePercent = Number(booking.priceBreakdown?.platformFeePercent) || 0;
      const platformFee = Number(booking.priceBreakdown?.platformFee) || Math.floor(amount * feePercent / 100);
      const creatorShare = Math.max(0, amount - platformFee);

      tx.set(db.collection('escrow_transactions').doc(payment.bookingId), {
        bookingId: payment.bookingId, paymentId: payment.paymentId, customerId: booking.customerId,
        creatorId: booking.creatorId, amount, platformFee, creatorShare,
        holdStatus: 'held', releaseStatus: 'pending', providerReference: body.transaction_id || body.order_id,
        heldAt: FieldValue.serverTimestamp(),
      });

      // Dana creator dicatat sebagai pending sejak pembayaran diverifikasi —
      // sama seperti confirmManualPayment di adminPayments.js. Tanpa ini,
      // pembayaran Midtrans membuat dokumen escrow "held" tapi wallet
      // creator tidak pernah tahu ada dana ditahan, sehingga total dana
      // ditahan yang tampil di panel admin (HeldFunds.jsx) dan saldo
      // pending creator jadi tidak akurat khusus untuk booking yang dibayar
      // lewat Midtrans.
      const walletRef = db.collection('wallets').doc(booking.creatorId);
      const walletSnap = await tx.get(walletRef);
      const wallet = walletSnap.data() || {};
      const pendingBefore = Number(wallet.pendingBalance) || 0;
      const pendingAfter = pendingBefore + creatorShare;
      tx.set(walletRef, {
        creatorId: booking.creatorId,
        pendingBalance: pendingAfter,
        availableBalance: Number(wallet.availableBalance) || 0,
        totalEarnings: Number(wallet.totalEarnings) || 0,
        updatedAt: FieldValue.serverTimestamp(),
      }, { merge: true });
      tx.set(db.collection('wallet_transactions').doc(), {
        creatorId: booking.creatorId,
        referenceId: payment.bookingId,
        type: 'escrow_hold',
        amount: creatorShare,
        balanceBefore: pendingBefore,
        balanceAfter: pendingAfter,
        status: 'held',
        note: 'Dana ditahan setelah pembayaran Midtrans terverifikasi.',
        createdAt: FieldValue.serverTimestamp(),
      });

      tx.set(db.collection('booking_status_history').doc(), {
        bookingId: payment.bookingId,
        fromStatus: 'pending_payment',
        toStatus: 'paid',
        actorId: 'midtrans_webhook',
        actorRole: 'system',
        reason: `Pembayaran Midtrans (${body.transaction_status})`,
        createdAt: FieldValue.serverTimestamp(),
      });

      writeNotification(db, tx, { userId: booking.customerId, type: 'payment', title: 'Pembayaran berhasil', body: 'Pembayaran Midtrans berhasil. Booking kamu sedang diproses.', referenceId: payment.bookingId });
      writeNotification(db, tx, { userId: booking.creatorId, type: 'payment', title: 'Booking sudah dibayar', body: 'Pembayaran pelanggan berhasil. Dana ditahan sampai pekerjaan selesai.', referenceId: payment.bookingId });
    } else if (nextStatus === 'rejected') {
      writeNotification(db, tx, { userId: booking.customerId, type: 'payment', title: 'Pembayaran gagal', body: 'Transaksi Midtrans tidak berhasil. Kamu bisa mencoba metode lain.', referenceId: payment.bookingId });
    }
  });
  return res.status(200).json({ ok: true });
}