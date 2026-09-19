import crypto from 'node:crypto';
import { adminDb, FieldValue } from './_lib/firebaseAdmin.js';
import { writeNotification } from './_lib/notify.js';

/**
 * Menerima notifikasi status payout dari Midtrans Iris dan menyelesaikan
 * withdrawal secara otomatis — jalur ini yang membuat disburseWithdrawalIris
 * (di withdrawals.js) benar-benar "otomatis", bukan cuma mengirim payout
 * lalu diam.
 *
 * URL ini harus didaftarkan di Iris Dashboard sebagai Payout Notification
 * URL, memakai path bertanda tangan di bawah supaya endpoint publik ini
 * tidak bisa dipakai sembarang orang untuk memalsukan "withdrawal sukses".
 *
 * Skema tanda tangan mengikuti dokumentasi Iris: SHA512(payload_json +
 * MIDTRANS_IRIS_API_KEY). Payload mentah (bukan hasil re-serialize) yang
 * dipakai untuk verifikasi, karena urutan key JSON bisa berubah kalau
 * di-parse lalu di-stringify ulang.
 */
function validSignature(rawBody, signatureHeader) {
  const key = process.env.MIDTRANS_IRIS_API_KEY;
  if (!key || !signatureHeader) return false;
  const expected = crypto.createHash('sha512').update(rawBody + key).digest('hex');
  const a = Buffer.from(signatureHeader);
  const b = Buffer.from(expected);
  return a.length === b.length && crypto.timingSafeEqual(a, b);
}

export default async function handler(req, res) {
  if (req.method !== 'POST') return res.status(405).json({ error: 'Method not allowed' });

  // Vercel biasanya sudah mem-parse req.body; kita butuh string mentah untuk
  // verifikasi tanda tangan, jadi re-serialize dengan urutan key apa adanya
  // dari body yang sudah diparse tidak aman — kalau raw body tersedia (mis.
  // lewat req.rawBody yang disuntik middleware), pakai itu. Sebagai jalan
  // aman kalau tidak tersedia, kita tetap verifikasi tapi longgar dicatat.
  const rawBody = typeof req.rawBody === 'string' ? req.rawBody : JSON.stringify(req.body || {});
  const body = typeof req.body === 'string' ? JSON.parse(req.body) : (req.body || {});
  const signature = req.headers['x-callback-signature'] || body.signature_key || '';

  if (!validSignature(rawBody, signature)) {
    console.error('[midtrans-iris-callback] tanda tangan tidak valid, notifikasi ditolak.');
    return res.status(401).json({ error: 'Invalid signature' });
  }

  const referenceNo = body.reference_no;
  const status = body.status; // 'processed' | 'completed' | 'failed'
  if (!referenceNo || !status) return res.status(200).json({ ok: true, ignored: true });

  const db = adminDb();
  const matches = await db.collection('withdrawals').where('irisReferenceNo', '==', referenceNo).limit(1).get();
  if (matches.empty) return res.status(404).json({ error: 'Withdrawal untuk reference_no ini tidak ditemukan' });
  const wRef = matches.docs[0].ref;

  await db.runTransaction(async (tx) => {
    const wSnap = await tx.get(wRef);
    const w = wSnap.data();
    // Sudah final sebelumnya (mis. webhook terkirim dua kali) — jangan proses ulang.
    if (!w || w.status === 'success' || w.status === 'failed') return;

    if (status === 'completed') {
      const amount = Number(w.amount) || 0;
      const walletRef = db.collection('wallets').doc(w.creatorId);
      const walletSnap = await tx.get(walletRef);
      const wallet = walletSnap.data() || {};
      const withdrawnBefore = Number(wallet.withdrawn) || 0;
      tx.update(walletRef, { withdrawn: withdrawnBefore + amount, updatedAt: FieldValue.serverTimestamp() });
      tx.update(wRef, {
        status: 'success',
        irisStatus: status,
        bankReference: referenceNo,
        completedAt: FieldValue.serverTimestamp(),
      });
      writeNotification(db, tx, {
        userId: w.creatorId,
        type: 'withdrawal',
        title: 'Dana sudah ditransfer',
        body: `Rp${amount.toLocaleString('id-ID')} sudah dikirim otomatis ke rekeningmu via Midtrans Iris.`,
        referenceId: wSnap.id,
      });
    } else if (status === 'failed') {
      // Sama seperti markWithdrawalManual('failed'): kembalikan saldo yang
      // sudah dipotong saat approve, supaya creator tidak kehilangan dana
      // hanya karena bank tujuan menolak transfer.
      const amount = Number(w.amount) || 0;
      const walletRef = db.collection('wallets').doc(w.creatorId);
      const walletSnap = await tx.get(walletRef);
      const wallet = walletSnap.data() || {};
      const before = Number(wallet.availableBalance) || 0;
      const after = before + amount;
      tx.update(walletRef, { availableBalance: after, updatedAt: FieldValue.serverTimestamp() });
      tx.update(wRef, {
        status: 'failed',
        irisStatus: status,
        failureReason: body.reason || 'Payout ditolak oleh Midtrans Iris atau bank tujuan.',
        completedAt: FieldValue.serverTimestamp(),
      });
      writeNotification(db, tx, {
        userId: w.creatorId,
        type: 'withdrawal',
        title: 'Transfer gagal, saldo dikembalikan',
        body: body.reason || 'Payout otomatis gagal. Saldo sudah dikembalikan ke akunmu.',
        referenceId: wSnap.id,
      });
    } else {
      // 'processed' — masih dalam perjalanan, cuma catat status terakhir.
      tx.update(wRef, { irisStatus: status });
    }
  });

  return res.status(200).json({ ok: true });
}
