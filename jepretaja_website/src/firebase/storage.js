/**
 * Cloudinary unsigned upload untuk foto dan video.
 * Cloud name dan upload preset memang publik; API secret tidak boleh berada di
 * browser. Folder tetap dikirim agar media creator mudah dikelola di dashboard.
 */
export function storageTersedia() {
  return Boolean(import.meta.env.VITE_CLOUDINARY_CLOUD_NAME)
    && Boolean(import.meta.env.VITE_CLOUDINARY_UPLOAD_PRESET);
}

/**
 * Kode galat Storage yang berarti "fitur ini memang tidak tersedia untuk
 * proyek ini", bukan "unggahan tadi kebetulan gagal". Hanya untuk kode-kode
 * inilah beralih ke penyimpanan di dalam dokumen masuk akal; masalah jaringan
 * sesaat justru lebih baik ditampilkan apa adanya supaya bisa dicoba ulang.
 */
export function storageTidakTersedia(err) {
  return err?.code === 'cloudinary-not-configured';
}

/** Nama berkas yang tidak bisa saling menimpa walau dua orang mengunggah
 *  pada detik yang sama. */
function namaBerkas(ekstensi = 'jpg') {
  const acak = Math.random().toString(36).slice(2, 8);
  return `${Date.now()}-${acak}.${ekstensi}`;
}

/**
 * @param {Blob} blob berkas yang sudah dikompres
 * @param {string} folder mis. 'portfolios/<uid>' — harus cocok dengan storage.rules
 * @returns {Promise<string>} URL unduhan yang bisa dipakai <img src> dan APK
 */
export async function unggahBerkas(blob, folder, { onProgress, signal } = {}) {
  if (!storageTersedia()) {
    const error = new Error('Cloudinary belum dikonfigurasi. Isi VITE_CLOUDINARY_CLOUD_NAME dan VITE_CLOUDINARY_UPLOAD_PRESET.');
    error.code = 'cloudinary-not-configured';
    throw error;
  }

  const resourceType = blob.type.startsWith('video/') ? 'video' : 'image';
  const endpoint = `https://api.cloudinary.com/v1_1/${import.meta.env.VITE_CLOUDINARY_CLOUD_NAME}/${resourceType}/upload`;
  const body = new FormData();
  body.append('file', blob, namaBerkas(blob.type.split('/')[1] || 'bin'));
  body.append('upload_preset', import.meta.env.VITE_CLOUDINARY_UPLOAD_PRESET);
  body.append('folder', `jepretaja/${folder}`);

  // Progres dan pembatalan hanya bisa lewat XMLHttpRequest — fetch belum punya
  // event progres unggahan. Pemanggil yang tidak memintanya tetap memakai fetch.
  if (onProgress || signal) return unggahDenganProgres(endpoint, body, { onProgress, signal });

  const response = await fetch(endpoint, { method: 'POST', body });
  const result = await response.json().catch(() => ({}));
  if (!response.ok || !result.secure_url) {
    const detail = result.error?.message || `Cloudinary gagal mengunggah media (${response.status}).`;
    const error = new Error(detail);
    error.code = 'cloudinary-upload-failed';
    throw error;
  }
  return result.secure_url;
}

function unggahDenganProgres(endpoint, body, { onProgress, signal }) {
  return new Promise((resolve, reject) => {
    const galat = (pesan, kode) => Object.assign(new Error(pesan), { code: kode });
    if (signal?.aborted) {
      reject(galat('Unggahan dibatalkan.', 'upload-aborted'));
      return;
    }

    const xhr = new XMLHttpRequest();
    xhr.open('POST', endpoint);
    xhr.upload.onprogress = (e) => {
      if (e.lengthComputable && e.total > 0) onProgress?.(e.loaded / e.total);
    };
    xhr.onload = () => {
      let hasil = {};
      try { hasil = JSON.parse(xhr.responseText); } catch { /* balasan bukan JSON */ }
      if (xhr.status >= 200 && xhr.status < 300 && hasil.secure_url) {
        onProgress?.(1);
        resolve(hasil.secure_url);
        return;
      }
      reject(galat(
        hasil.error?.message || `Cloudinary gagal mengunggah media (${xhr.status}).`,
        'cloudinary-upload-failed'
      ));
    };
    xhr.onerror = () => reject(galat('Koneksi terputus saat mengunggah. Periksa internet lalu coba lagi.', 'cloudinary-network'));
    xhr.onabort = () => reject(galat('Unggahan dibatalkan.', 'upload-aborted'));
    signal?.addEventListener('abort', () => xhr.abort(), { once: true });
    xhr.send(body);
  });
}

export const unggahGambar = unggahBerkas;
