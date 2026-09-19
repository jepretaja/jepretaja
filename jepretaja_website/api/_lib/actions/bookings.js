import { adminDb, FieldValue } from '../firebaseAdmin.js';
import { requireAdmin } from '../rbac.js';
import { writeAudit } from '../audit.js';
import { readBody, requireString, notFound, conflict } from '../http.js';

/**
 * Graf transisi status booking — harus sinkron dengan object BookingStatus di
 * app/src/main/java/com/jepretaja/app/data/model/BookingModel.kt.
 *
 * Admin override tetap divalidasi di sini: tanpa graf ini seorang admin bisa
 * memindahkan booking dari "draft" langsung ke "funds_released" dan melepas
 * dana untuk booking yang belum pernah dibayar.
 */
export const BOOKING_TRANSITIONS = {
  draft: ['pending_payment', 'cancelled'],
  pending_payment: ['paid', 'confirmed', 'rejected', 'cancelled'],
  paid: ['confirmed', 'rejected', 'cancelled', 'refund_requested'],
  confirmed: ['upcoming', 'rejected', 'cancelled', 'refund_requested'],
  upcoming: ['in_progress', 'cancelled', 'refund_requested'],
  in_progress: ['completed', 'disputed'],
  completed: ['customer_confirmed', 'disputed', 'refund_requested'],
  customer_confirmed: ['funds_released', 'disputed'],
  funds_released: ['reviewed'],
  reviewed: [],
  cancelled: [],
  refund_requested: ['cancelled', 'confirmed', 'disputed'],
  disputed: ['funds_released', 'cancelled', 'confirmed'],
  rejected: [],
};

/**
 * Status dengan efek finansial yang HARUS lewat fungsi khusus (yang benar
 * benar memindahkan uang di escrow/wallet), bukan lewat override generik ini:
 * - "paid" hanya boleh terjadi lewat confirmManualPayment / webhook Midtrans.
 * - "funds_released" hanya boleh lewat releaseEscrow / resolveDispute.
 * Endpoint ini cuma mengubah label status booking, TIDAK menyentuh escrow
 * atau wallet sama sekali — kalau kedua status ini diizinkan lewat sini,
 * booking bisa terlihat "sudah dibayar"/"dana sudah dirilis" padahal secara
 * ledger uangnya tidak pernah berpindah.
 */
const STATUS_HARUS_LEWAT_FUNGSI_KHUSUS = new Set(['paid', 'funds_released']);

export async function adminUpdateBookingStatus(req) {
  const actor = await requireAdmin(req, 'manage_booking');
  const body = readBody(req);
  const bookingId = requireString(body.bookingId, 'bookingId');
  const toStatus = requireString(body.toStatus, 'toStatus');
  const reason = typeof body.reason === 'string' && body.reason.trim() ? body.reason.trim() : 'manual override by admin';

  if (!Object.prototype.hasOwnProperty.call(BOOKING_TRANSITIONS, toStatus)) {
    throw conflict(`Status "${toStatus}" tidak dikenal.`);
  }
  if (STATUS_HARUS_LEWAT_FUNGSI_KHUSUS.has(toStatus)) {
    throw conflict(
      `Status "${toStatus}" tidak bisa diset lewat override manual karena melibatkan perpindahan dana. ` +
      (toStatus === 'paid'
        ? 'Gunakan konfirmasi pembayaran (transfer manual/Midtrans).'
        : 'Gunakan "Lepas Dana" (releaseEscrow) atau resolusi dispute.')
    );
  }

  const db = adminDb();
  const ref = db.collection('bookings').doc(bookingId);
  const escrowRef = db.collection('escrow_transactions').doc(bookingId);

  return db.runTransaction(async (tx) => {
    const snap = await tx.get(ref);
    if (!snap.exists) throw notFound('Booking tidak ditemukan.');
    const booking = snap.data();
    const fromStatus = booking.status || 'draft';

    if (fromStatus === toStatus) {
      throw conflict(`Booking sudah berstatus "${toStatus}".`);
    }
    const allowed = BOOKING_TRANSITIONS[fromStatus] || [];
    if (!allowed.includes(toStatus)) {
      throw conflict(
        `Transisi "${fromStatus}" ke "${toStatus}" tidak diizinkan. ` +
          (allowed.length ? `Dari "${fromStatus}" hanya bisa ke: ${allowed.join(', ')}.` : `"${fromStatus}" adalah status akhir.`)
      );
    }

    // "cancelled" lewat sini tidak pernah menyentuh escrow. Kalau booking ini
    // sudah punya dana tertahan yang belum dirilis, membatalkannya di sini
    // akan membuat dana itu terjebak selamanya — "cancelled" adalah status
    // akhir tanpa transisi lanjutan, jadi tidak ada jalan lagi untuk
    // membersihkan escrow-nya setelah ini. Kasus itu wajib lewat
    // processRefund atau resolveDispute, yang memang membersihkan escrow dan
    // wallet sebagai bagian dari transaksinya.
    if (toStatus === 'cancelled') {
      const eSnap = await tx.get(escrowRef);
      if (eSnap.exists && eSnap.data().releaseStatus === 'pending') {
        throw conflict(
          'Booking ini masih punya dana tertahan di escrow. Batalkan lewat proses Refund atau Dispute ' +
          'supaya dananya ikut dibersihkan/dikembalikan, bukan lewat override status.'
        );
      }
    }

    const update = {
      status: toStatus,
      statusUpdatedAt: FieldValue.serverTimestamp(),
      statusUpdatedBy: actor.uid,
    };
    if (toStatus === 'cancelled') update.cancelledAt = FieldValue.serverTimestamp();
    if (toStatus === 'customer_confirmed') update.customerConfirmedAt = FieldValue.serverTimestamp();

    tx.update(ref, update);
    writeAudit(db, actor, {
      action: 'admin_update_booking_status',
      targetType: 'booking',
      targetId: bookingId,
      reason,
      metadata: { fromStatus, toStatus },
    }, tx);

    return { bookingId, fromStatus, toStatus };
  });
}
