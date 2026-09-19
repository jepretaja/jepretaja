/**
 * Draf unggahan di perangkat ini (localStorage). Kunci lama dipertahankan
 * (`creator-post-draft-<uid>`) supaya draf yang sudah ada tetap terbaca.
 *
 * Yang disimpan hanya URL media yang sudah terunggah. Data-URL besar tidak ikut
 * karena satu foto saja bisa menghabiskan kuota localStorage (~5 MB) dan
 * menulisnya melempar galat — yang dulu tidak ditangkap sama sekali.
 */
const kunci = (uid) => `creator-post-draft-${uid}`;
const BATAS_KARAKTER = 1_500_000;

export function bacaDraf(uid) {
  if (!uid) return null;
  try {
    const mentah = JSON.parse(localStorage.getItem(kunci(uid)) || 'null');
    if (!mentah || typeof mentah !== 'object') return null;

    // Bentuk lama: tags berupa string "a, b", media dan type di level atas.
    const extraTags = Array.isArray(mentah.extraTags)
      ? mentah.extraTags
      : String(mentah.tags || '').split(',').map((s) => s.trim().replace(/^#/, '')).filter(Boolean);
    const media = (Array.isArray(mentah.media) ? mentah.media : []).filter((u) => typeof u === 'string' && u);
    const data = {
      caption: String(mentah.caption || ''),
      category: String(mentah.category || ''),
      location: String(mentah.location || ''),
      extraTags,
      packageId: mentah.packageId || '',
      commentPolicy: mentah.commentPolicy || 'all',
      allowSave: mentah.allowSave !== false,
      jenis: mentah.jenis || mentah.type || 'photo',
      media,
      sampulUrl: mentah.sampulUrl || '',
    };
    if (!data.caption.trim() && media.length === 0) return null;
    return { data, disimpanPada: Number(mentah.disimpanPada) || null };
  } catch {
    localStorage.removeItem(kunci(uid));
    return null;
  }
}

/** @returns {'ok'|'tanpa-media'|'gagal'} */
export function simpanDraf(uid, data) {
  if (!uid) return 'gagal';
  const badan = { v: 2, ...data, disimpanPada: Date.now() };
  const coba = (isi) => {
    const teks = JSON.stringify(isi);
    if (teks.length > BATAS_KARAKTER) throw new Error('terlalu besar');
    localStorage.setItem(kunci(uid), teks);
  };
  try {
    coba(badan);
    return 'ok';
  } catch {
    try {
      coba({ ...badan, media: [], sampulUrl: '' });
      return 'tanpa-media';
    } catch {
      return 'gagal';
    }
  }
}

export function hapusDraf(uid) {
  if (!uid) return;
  try { localStorage.removeItem(kunci(uid)); } catch { /* diabaikan */ }
}
