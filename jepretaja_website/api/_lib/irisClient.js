/**
 * Client tipis untuk Midtrans Iris — produk terpisah dari Snap/Core API yang
 * dipakai `midtrans.js`, dengan API key, host, dan alur approval sendiri.
 *
 * PENTING — ini bukan sesuatu yang bisa "dinyalakan" murni lewat kode:
 * 1. Akun Midtrans harus mengajukan & disetujui untuk fitur Iris Disbursement
 *    (proses bisnis/KYC di sisi Midtrans, bukan konfigurasi teknis).
 * 2. Setelah disetujui, Iris punya dua peran: "creator" (yang mengajukan
 *    payout — itu yang dilakukan `createPayout` di bawah) dan "approver"
 *    (orang berbeda yang menyetujui payout via OTP, biasanya lewat Iris
 *    Dashboard). Payout yang dibuat lewat API TETAP menunggu approval itu
 *    sebelum dana benar-benar terkirim — ini pengaman anti-fraud dari
 *    Midtrans sendiri, tidak bisa dilewati dari kode kita.
 * 3. Begitu approver menyetujui (atau menolak), Midtrans mengirim webhook ke
 *    `api/midtrans-iris-callback.js`, dan withdrawal ditandai selesai/gagal
 *    secara otomatis dari situ — lihat file itu.
 *
 * Kalau MIDTRANS_IRIS_API_KEY belum diisi di environment, seluruh fungsi di
 * sini melempar error yang jelas, dan pemanggilnya (disburseWithdrawalIris di
 * withdrawals.js) menyuruh admin memakai jalur transfer manual yang sudah
 * ada — bukan mogok tanpa penjelasan.
 */

function irisBaseUrl() {
  return process.env.MIDTRANS_ENV === 'production'
    ? 'https://app.midtrans.com/iris/api/v1'
    : 'https://app.sandbox.midtrans.com/iris/api/v1';
}

function irisApiKey() {
  const key = process.env.MIDTRANS_IRIS_API_KEY;
  if (!key) {
    throw Object.assign(new Error('IRIS_NOT_CONFIGURED'), { code: 'iris-not-configured' });
  }
  return key;
}

function irisHeaders() {
  return {
    Authorization: `Basic ${Buffer.from(`${irisApiKey()}:`).toString('base64')}`,
    'Content-Type': 'application/json',
  };
}

/**
 * Peta bank ke kode yang dikenal Iris. Sengaja HANYA berisi bank yang
 * kodenya sudah terkonfirmasi dari dokumentasi Midtrans — daripada menebak
 * kode untuk bank/e-wallet lain dan berisiko salah kirim dana yang tidak
 * bisa ditarik balik. Bank di luar daftar ini otomatis jatuh ke transfer
 * manual (lihat disburseWithdrawalIris).
 */
export const IRIS_BANK_CODE = {
  BCA: 'bca',
  BNI: 'bni',
  BRI: 'bri',
  Mandiri: 'mandiri',
  'CIMB Niaga': 'cimb',
  Permata: 'permata',
  Danamon: 'danamon',
  BTN: 'btn',
};

/**
 * Mengajukan satu payout. Mengembalikan { referenceNo, status } dari
 * Midtrans (status di titik ini biasanya "queued", menunggu approval).
 */
export async function createIrisPayout({ beneficiaryName, beneficiaryAccount, beneficiaryBank, amount, notes }) {
  const res = await fetch(`${irisBaseUrl()}/payouts`, {
    method: 'POST',
    headers: irisHeaders(),
    body: JSON.stringify({
      payouts: [{
        beneficiary_name: beneficiaryName,
        beneficiary_account: beneficiaryAccount,
        beneficiary_bank: beneficiaryBank,
        amount: String(amount),
        notes: notes || 'Withdrawal JepretAja',
      }],
    }),
  });
  const data = await res.json().catch(() => ({}));
  if (!res.ok) {
    const message = data?.error_message || data?.errors?.[0] || `Iris menolak payout (HTTP ${res.status}).`;
    throw Object.assign(new Error(message), { code: 'iris-request-failed' });
  }
  const item = Array.isArray(data) ? data[0] : (data?.payouts?.[0] || data);
  if (!item?.reference_no) {
    throw Object.assign(new Error('Respons Iris tidak berisi reference_no.'), { code: 'iris-request-failed' });
  }
  return { referenceNo: item.reference_no, status: item.status || 'queued' };
}

/**
 * Dipakai kalau webhook telat/tidak sampai — admin bisa memicu pengecekan
 * status manual dari panel.
 */
export async function getIrisPayoutStatus(referenceNo) {
  const res = await fetch(`${irisBaseUrl()}/payouts/${encodeURIComponent(referenceNo)}`, {
    method: 'GET',
    headers: irisHeaders(),
  });
  const data = await res.json().catch(() => ({}));
  if (!res.ok) {
    throw Object.assign(new Error(data?.error_message || `Gagal mengambil status payout (HTTP ${res.status}).`), { code: 'iris-request-failed' });
  }
  return data;
}
