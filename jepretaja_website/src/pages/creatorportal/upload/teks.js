/** Batas panjang caption. */
export const MAKS_CAPTION = 2200;

const POLA = /#([\p{L}\p{N}_]+)/gu;

/** Semua tagar unik di dalam caption, huruf kecil, tanpa tanda #. */
export function ambilTagar(caption) {
  const hasil = [];
  for (const m of (caption || '').matchAll(POLA)) {
    const t = m[1].toLowerCase();
    if (!hasil.includes(t)) hasil.push(t);
  }
  return hasil;
}

/** Tagar tidak boleh mengandung spasi/simbol: "Golden Hour!" → "goldenhour". */
export function jadikanTagar(kata) {
  return String(kata || '').toLowerCase().replace(/[^\p{L}\p{N}_]/gu, '');
}

/** Menambah `#tagar` ke ujung caption dengan pemisah yang benar. */
export function tambahTagar(caption, tagar) {
  const dasar = caption || '';
  const sambung = dasar === '' || /\s$/.test(dasar) ? '' : ' ';
  return `${dasar}${sambung}#${tagar} `;
}
