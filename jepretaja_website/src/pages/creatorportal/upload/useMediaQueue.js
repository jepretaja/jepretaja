import { useCallback, useEffect, useRef, useState } from 'react';
import { storageTersedia, storageTidakTersedia, unggahBerkas } from '../../../firebase/storage';
import {
  blobKeDataUrl, formatUkuran, kompresGambar, periksaBerkasMedia, ukuranDataUrl,
} from '../../../utils/imageFile';
import { bacaMetadataVideo } from '../../../utils/videoFile';

export const MAKS_FOTO = 8;

/** Ambang aman gambar yang ditanam di dokumen Firestore (batas dokumen 1 MB). */
const BATAS_TERTANAM = 700 * 1024;

let urutan = 0;
const idBaru = () => `m${Date.now().toString(36)}${(urutan += 1).toString(36)}`;

const adalahBlob = (url) => typeof url === 'string' && url.startsWith('blob:');

/**
 * Antrian media unggahan: setiap berkas langsung diproses (dikompres untuk
 * foto) dan diunggah begitu dipilih, lengkap dengan progres per berkas —
 * seperti TikTok yang sudah mengunggah di latar belakang selagi pengguna
 * masih menulis caption.
 *
 * Aturan satu postingan (sama dengan yang dipakai APK):
 *  - video: tepat SATU video, atau
 *  - foto : 1 sampai MAKS_FOTO foto.
 * Video dan foto tidak dicampur. Sebelumnya keduanya bisa bercampur dan
 * `type` postingan tersangkut di 'video' walau videonya sudah dihapus.
 *
 * status item: 'proses' (dikompres/dibaca) → 'unggah' → 'selesai' | 'gagal'
 */
export function useMediaQueue({ folder, awal = [], jenisAwal = 'photo' }) {
  const [items, setItems] = useState(() => awal.filter(Boolean).map((url) => ({
    id: idBaru(),
    kind: jenisAwal === 'video' ? 'video' : 'image',
    url,
    previewUrl: url,
    name: '',
    size: 0,
    status: 'selesai',
    progress: 100,
  })));
  const [pesan, setPesan] = useState(null);

  const itemsRef = useRef(items);
  itemsRef.current = items;
  const berkas = useRef(new Map());   // id -> File asli, untuk "Coba lagi"
  const pembatal = useRef(new Map()); // id -> AbortController
  const hidup = useRef(true);

  const ubah = useCallback((id, patch) => {
    if (!hidup.current) return;
    setItems((daftar) => daftar.map((i) => (i.id === id ? { ...i, ...patch } : i)));
  }, []);

  useEffect(() => {
    hidup.current = true;
    return () => {
      hidup.current = false;
      pembatal.current.forEach((c) => c.abort());
      itemsRef.current.forEach((i) => { if (adalahBlob(i.previewUrl)) URL.revokeObjectURL(i.previewUrl); });
    };
  }, []);

  const proses = useCallback(async (item) => {
    const file = berkas.current.get(item.id);
    if (!file) return;
    const kontrol = new AbortController();
    pembatal.current.set(item.id, kontrol);
    ubah(item.id, { status: 'proses', progress: 0, error: null });

    let terakhir = -1;
    const lapor = (p) => {
      const persen = Math.round(p * 100);
      if (persen !== terakhir) { terakhir = persen; ubah(item.id, { progress: persen }); }
    };
    const opsi = { signal: kontrol.signal, onProgress: lapor };

    try {
      if (item.kind === 'video') {
        const meta = await bacaMetadataVideo(item.previewUrl).catch(() => null);
        if (meta) ubah(item.id, meta);
        if (!storageTersedia()) {
          throw new Error('Video membutuhkan Cloudinary. Isi VITE_CLOUDINARY_CLOUD_NAME dan VITE_CLOUDINARY_UPLOAD_PRESET, lalu deploy ulang.');
        }
        ubah(item.id, { status: 'unggah' });
        const url = await unggahBerkas(file, folder, opsi);
        ubah(item.id, { url, status: 'selesai', progress: 100 });
        return;
      }

      // Foto: rasio asli dipertahankan (rasio: null) — tidak ada pemotongan paksa.
      let url = '';
      let previewBaru = null;
      if (storageTersedia()) {
        const { blob, lebar, tinggi } = await kompresGambar(file, { sisiMaks: 1600, targetByte: 400 * 1024, rasio: null });
        previewBaru = URL.createObjectURL(blob);
        ubah(item.id, { lebar, tinggi, previewUrl: previewBaru, status: 'unggah' });
        try {
          url = await unggahBerkas(blob, folder, opsi);
        } catch (err) {
          if (!storageTidakTersedia(err)) throw err;
        }
      }
      if (!url) {
        // Cadangan: gambar kecil ditanam di dokumen. Batasnya ditegakkan di sini
        // karena dokumen Firestore yang melewati 1 MB GAGAL DISIMPAN seutuhnya.
        const { blob, lebar, tinggi } = await kompresGambar(file, { sisiMaks: 1000, targetByte: 150 * 1024, rasio: null });
        const dataUrl = await blobKeDataUrl(blob);
        const terpakai = itemsRef.current
          .filter((i) => i.id !== item.id)
          .reduce((t, i) => t + ukuranDataUrl(i.url), 0);
        if (terpakai + dataUrl.length > BATAS_TERTANAM) {
          throw new Error(`Foto tidak muat. Penyimpanan di dalam dokumen dibatasi ${formatUkuran(BATAS_TERTANAM)}; hapus foto lain atau aktifkan Cloudinary.`);
        }
        url = dataUrl;
        ubah(item.id, { lebar, tinggi, tertanam: true });
      }
      ubah(item.id, { url, status: 'selesai', progress: 100 });
      // previewUrl sudah diganti ke versi terkompres; blob asli tidak dipakai lagi.
      if (previewBaru && adalahBlob(item.previewUrl)) URL.revokeObjectURL(item.previewUrl);
    } catch (err) {
      if (err?.code === 'upload-aborted') return;
      ubah(item.id, { status: 'gagal', error: err?.message || 'Gagal memproses berkas.' });
    } finally {
      pembatal.current.delete(item.id);
    }
  }, [folder, ubah]);

  const tambah = useCallback((daftarBerkas) => {
    const files = [...(daftarBerkas || [])];
    if (!files.length) return;
    setPesan(null);

    const saatIni = itemsRef.current;
    const modeSaatIni = saatIni.length ? (saatIni[0].kind === 'video' ? 'video' : 'foto') : null;
    const modeBaru = modeSaatIni || (files[0].type.startsWith('video/') ? 'video' : 'foto');
    const catatan = [];
    let diterima;

    if (modeBaru === 'video') {
      if (saatIni.length) {
        setPesan('Satu postingan hanya memuat satu video. Hapus video yang lama untuk menggantinya.');
        return;
      }
      const video = files.find((f) => f.type.startsWith('video/'));
      diterima = video ? [video] : [];
      if (files.length > 1) catatan.push('Hanya satu video yang dipakai; berkas lainnya diabaikan.');
    } else {
      const foto = files.filter((f) => !f.type.startsWith('video/'));
      if (foto.length < files.length) catatan.push('Video dan foto tidak bisa digabung dalam satu postingan; video diabaikan.');
      const sisa = MAKS_FOTO - saatIni.length;
      if (sisa <= 0) {
        setPesan(`Sudah mencapai batas ${MAKS_FOTO} foto.`);
        return;
      }
      diterima = foto.slice(0, sisa);
      if (foto.length > sisa) catatan.push(`${foto.length - sisa} foto tidak ditambahkan (maksimal ${MAKS_FOTO}).`);
    }

    const baru = [];
    diterima.forEach((file) => {
      const salah = periksaBerkasMedia(file, true);
      if (salah) { catatan.push(salah); return; }
      const item = {
        id: idBaru(),
        kind: file.type.startsWith('video/') ? 'video' : 'image',
        url: '',
        previewUrl: URL.createObjectURL(file),
        name: file.name,
        size: file.size,
        status: 'proses',
        progress: 0,
      };
      berkas.current.set(item.id, file);
      baru.push(item);
    });

    if (baru.length) {
      setItems((daftar) => [...daftar, ...baru]);
      // Berurutan, bukan serentak: memuat beberapa foto 12 MP sekaligus bisa
      // mematikan tab di ponsel.
      (async () => { for (const item of baru) await proses(item); })();
    }
    if (catatan.length) setPesan(catatan.join(' '));
  }, [proses]);

  const hapus = useCallback((id) => {
    pembatal.current.get(id)?.abort();
    berkas.current.delete(id);
    setPesan(null);
    setItems((daftar) => {
      const target = daftar.find((i) => i.id === id);
      if (target && adalahBlob(target.previewUrl)) URL.revokeObjectURL(target.previewUrl);
      return daftar.filter((i) => i.id !== id);
    });
  }, []);

  const geser = useCallback((id, arah) => {
    setItems((daftar) => {
      const i = daftar.findIndex((x) => x.id === id);
      const j = i + arah;
      if (i < 0 || j < 0 || j >= daftar.length) return daftar;
      const salin = [...daftar];
      [salin[i], salin[j]] = [salin[j], salin[i]];
      return salin;
    });
  }, []);

  const jadikanSampul = useCallback((id) => {
    setItems((daftar) => {
      const i = daftar.findIndex((x) => x.id === id);
      if (i <= 0) return daftar;
      const salin = [...daftar];
      const [dipindah] = salin.splice(i, 1);
      return [dipindah, ...salin];
    });
  }, []);

  const cobaLagi = useCallback((id) => {
    const item = itemsRef.current.find((i) => i.id === id);
    if (item) proses(item);
  }, [proses]);

  /** Mengisi antrian dari URL yang sudah ada (memulihkan draf). */
  const isiDari = useCallback((urls, jenis) => {
    pembatal.current.forEach((c) => c.abort());
    itemsRef.current.forEach((i) => { if (adalahBlob(i.previewUrl)) URL.revokeObjectURL(i.previewUrl); });
    berkas.current.clear();
    setPesan(null);
    setItems((urls || []).filter(Boolean).map((url) => ({
      id: idBaru(),
      kind: jenis === 'video' ? 'video' : 'image',
      url,
      previewUrl: url,
      name: '',
      size: 0,
      status: 'selesai',
      progress: 100,
    })));
  }, []);

  const sibuk = items.some((i) => i.status === 'proses' || i.status === 'unggah');
  const gagal = items.some((i) => i.status === 'gagal');
  const selesai = items.filter((i) => i.status === 'selesai').length;

  return {
    items,
    mode: items.length ? (items[0].kind === 'video' ? 'video' : 'foto') : null,
    pesan,
    sibuk,
    gagal,
    selesai,
    siap: items.length > 0 && !sibuk && !gagal,
    tambah,
    hapus,
    geser,
    jadikanSampul,
    cobaLagi,
    isiDari,
  };
}
