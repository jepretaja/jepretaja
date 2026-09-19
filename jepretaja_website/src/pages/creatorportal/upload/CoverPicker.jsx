import { useCallback, useEffect, useRef, useState } from 'react';
import { storageTersedia, unggahBerkas } from '../../../firebase/storage';
import { ambilBingkai, buatFilmstrip, formatDurasi } from '../../../utils/videoFile';
import { IkonAwan, IkonCentang, IkonPeringatan } from './StudioIcons';

/**
 * Pemilih sampul video: geser penggeser atau ketuk salah satu bingkai di
 * filmstrip. Bingkai yang dipilih diambil dari video secara lokal, dijadikan
 * JPEG, lalu diunggah sebagai gambar sampul (`thumbnailUrl`) — sebelumnya
 * thumbnail sebuah video diisi dengan URL videonya sendiri.
 *
 * Sampul pertama diambil otomatis (sekitar detik ke-1) supaya karya tidak
 * pernah terbit tanpa gambar sampul walau creator tidak membuka pemilih ini.
 * Kegagalan mengambil sampul tidak menghalangi publikasi: `thumbnailUrl`
 * jatuh kembali ke perilaku lama.
 */
export default function CoverPicker({ item, sampul, onSampul, folder }) {
  const videoRef = useRef(null);
  const token = useRef(0);
  const jeda = useRef(null);
  const lokalTerakhir = useRef(null);
  const [durasi, setDurasi] = useState(item.durasi || 0);
  const [detik, setDetik] = useState(sampul.detik ?? 0);
  const [filmstrip, setFilmstrip] = useState([]);

  useEffect(() => { if (item.durasi) setDurasi(item.durasi); }, [item.durasi]);

  useEffect(() => () => {
    clearTimeout(jeda.current);
    token.current += 1;
    if (lokalTerakhir.current) URL.revokeObjectURL(lokalTerakhir.current);
  }, []);

  const ambil = useCallback(async (dtk) => {
    const saya = (token.current += 1);
    onSampul({ status: 'proses', error: null, detik: dtk });
    try {
      const blob = await ambilBingkai(item.previewUrl, dtk);
      if (saya !== token.current) return;
      const lokal = URL.createObjectURL(blob);
      if (lokalTerakhir.current) URL.revokeObjectURL(lokalTerakhir.current);
      lokalTerakhir.current = lokal;
      onSampul({ previewUrl: lokal, url: '', status: storageTersedia() ? 'unggah' : 'gagal', detik: dtk });
      if (!storageTersedia()) return;
      const url = await unggahBerkas(blob, folder);
      if (saya !== token.current) return;
      onSampul({ url, status: 'selesai' });
    } catch (err) {
      if (saya !== token.current) return;
      onSampul({ status: 'gagal', error: err?.message || 'Sampul gagal dibuat.' });
    }
  }, [item.previewUrl, folder, onSampul]);

  // Sampul otomatis, sekali saja per video.
  const sudahOtomatis = useRef(false);
  useEffect(() => {
    if (sudahOtomatis.current || !durasi || sampul.url || sampul.previewUrl) return;
    sudahOtomatis.current = true;
    const awal = Math.min(1, durasi / 2);
    setDetik(awal);
    ambil(awal);
  }, [durasi, sampul.url, sampul.previewUrl, ambil]);

  // Filmstrip dibuat pelan-pelan di latar belakang; kalau gagal (mis. CORS
  // pada video lama) pemilih tetap bisa dipakai lewat penggeser saja.
  useEffect(() => {
    if (!durasi) return undefined;
    let batal = false;
    buatFilmstrip(item.previewUrl, durasi, 8, { batal: () => batal })
      .then((h) => { if (!batal) setFilmstrip(h); })
      .catch(() => {});
    return () => { batal = true; };
  }, [item.previewUrl, durasi]);

  const geser = (nilai) => {
    setDetik(nilai);
    if (videoRef.current) videoRef.current.currentTime = nilai;
    clearTimeout(jeda.current);
    jeda.current = setTimeout(() => ambil(nilai), 450);
  };

  const pilihBingkai = (dtk) => {
    clearTimeout(jeda.current);
    setDetik(dtk);
    if (videoRef.current) videoRef.current.currentTime = dtk;
    ambil(dtk);
  };

  const status = sampul.status;

  return (
    <div className="studio-cover">
      <div className="studio-cover-frame">
        <video
          ref={videoRef}
          src={item.previewUrl}
          muted
          playsInline
          preload="auto"
          onLoadedMetadata={(e) => { if (!durasi && Number.isFinite(e.currentTarget.duration)) setDurasi(e.currentTarget.duration); }}
        />
      </div>

      <div className="studio-cover-body">
        <div className="studio-cover-head">
          <strong>Sampul video</strong>
          <span className={`studio-cover-status ${status}`} role="status" aria-live="polite">
            {status === 'proses' && 'Mengambil bingkai…'}
            {status === 'unggah' && <><IkonAwan ukuran={14} /> Menyimpan sampul…</>}
            {status === 'selesai' && <><IkonCentang ukuran={14} /> Sampul siap</>}
            {status === 'gagal' && <><IkonPeringatan ukuran={14} /> {sampul.error || 'Sampul gagal'}</>}
          </span>
        </div>
        <p className="studio-hint">Geser untuk memilih bingkai yang paling menarik. Ini gambar yang dilihat orang sebelum video diputar.</p>

        {filmstrip.length > 0 && (
          <div className="studio-filmstrip" role="group" aria-label="Pilih bingkai sampul">
            {filmstrip.map((b) => (
              <button
                key={b.detik}
                type="button"
                className={Math.abs(b.detik - detik) < durasi / 16 ? 'aktif' : ''}
                onClick={() => pilihBingkai(b.detik)}
                aria-label={`Bingkai pada detik ${formatDurasi(b.detik)}`}
              >
                <img src={b.gambar} alt="" />
              </button>
            ))}
          </div>
        )}

        <div className="studio-slider-baris">
          <input
            type="range"
            className="studio-slider"
            min={0}
            max={durasi || 1}
            step={0.1}
            value={Math.min(detik, durasi || 1)}
            disabled={!durasi}
            onChange={(e) => geser(Number(e.target.value))}
            aria-label="Posisi bingkai sampul"
          />
          <span className="studio-slider-waktu">{formatDurasi(detik)} / {formatDurasi(durasi)}</span>
        </div>
      </div>
    </div>
  );
}
