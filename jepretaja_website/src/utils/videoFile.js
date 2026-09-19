/**
 * Bantuan membaca video di browser: metadata (durasi, resolusi) dan bingkai
 * diam untuk sampul. Semua terjadi lokal — tidak ada yang dikirim ke mana pun.
 */

export function formatDurasi(detik) {
  const total = Math.max(0, Math.round(Number(detik) || 0));
  const m = Math.floor(total / 60);
  const s = String(total % 60).padStart(2, '0');
  return `${m}:${s}`;
}

/** Memuat video sampai bingkai pertamanya siap digambar. */
export function bukaVideo(src) {
  return new Promise((resolve, reject) => {
    const v = document.createElement('video');
    v.muted = true;
    v.playsInline = true;
    v.preload = 'auto';
    // Video dari server lain butuh CORS supaya bingkainya boleh digambar ke
    // canvas; blob: lokal tidak perlu (dan tidak boleh) memakainya.
    if (/^https?:/i.test(src)) v.crossOrigin = 'anonymous';
    const timer = setTimeout(() => reject(new Error('Video terlalu lama dimuat.')), 15000);
    v.onloadeddata = () => { clearTimeout(timer); resolve(v); };
    v.onerror = () => { clearTimeout(timer); reject(new Error('Video tidak bisa dibaca browser.')); };
    v.src = src;
  });
}

function pindahKe(v, detik) {
  return new Promise((resolve, reject) => {
    const timer = setTimeout(() => reject(new Error('Gagal berpindah ke bingkai video.')), 8000);
    v.onseeked = () => { clearTimeout(timer); resolve(); };
    const batas = Number.isFinite(v.duration) ? Math.max(0, v.duration - 0.05) : detik;
    v.currentTime = Math.max(0, Math.min(detik, batas));
  });
}

function gambarBingkai(v, { sisiMaks, mutu, jenis }) {
  const lebarAsli = v.videoWidth;
  const tinggiAsli = v.videoHeight;
  if (!lebarAsli || !tinggiAsli) throw new Error('Ukuran video tidak terbaca.');
  const skala = Math.min(1, sisiMaks / Math.max(lebarAsli, tinggiAsli));
  const canvas = document.createElement('canvas');
  canvas.width = Math.max(1, Math.round(lebarAsli * skala));
  canvas.height = Math.max(1, Math.round(tinggiAsli * skala));
  canvas.getContext('2d').drawImage(v, 0, 0, canvas.width, canvas.height);
  if (jenis === 'url') return canvas.toDataURL('image/jpeg', mutu);
  return new Promise((resolve, reject) => {
    canvas.toBlob((b) => (b ? resolve(b) : reject(new Error('Gagal membuat sampul.'))), 'image/jpeg', mutu);
  });
}

export async function bacaMetadataVideo(src) {
  const v = await bukaVideo(src);
  return { durasi: v.duration, lebar: v.videoWidth, tinggi: v.videoHeight };
}

/** Satu bingkai video pada `detik` sebagai Blob JPEG. */
export async function ambilBingkai(src, detik, { sisiMaks = 1080, mutu = 0.86 } = {}) {
  const v = await bukaVideo(src);
  await pindahKe(v, detik);
  return gambarBingkai(v, { sisiMaks, mutu, jenis: 'blob' });
}

/** Deretan bingkai kecil (data URL) untuk filmstrip pemilih sampul. */
export async function buatFilmstrip(src, durasi, jumlah = 8, { batal } = {}) {
  const v = await bukaVideo(src);
  const hasil = [];
  for (let i = 0; i < jumlah; i += 1) {
    if (batal?.()) return hasil;
    const detik = (durasi * (i + 0.5)) / jumlah;
    await pindahKe(v, detik);
    hasil.push({ detik, gambar: gambarBingkai(v, { sisiMaks: 140, mutu: 0.6, jenis: 'url' }) });
  }
  return hasil;
}
