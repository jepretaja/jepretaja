import { useEffect, useRef, useState } from 'react';
import { formatUkuran } from '../../../utils/imageFile';
import { formatDurasi } from '../../../utils/videoFile';
import { MAKS_FOTO } from './useMediaQueue';
import CoverPicker from './CoverPicker';
import {
  IkonAwan, IkonFoto, IkonKamera, IkonKiri, IkonKanan, IkonPeringatan, IkonSampah,
  IkonSampul, IkonTambah, IkonUlang, IkonUnggah, IkonVideo,
} from './StudioIcons';

const AKSEPTASI = 'image/*,video/mp4,video/webm,video/quicktime';
const KELILING = 2 * Math.PI * 17;

/** Layar sentuh (ponsel/tablet): di sini `capture` benar-benar membuka kamera. */
const layarSentuh = () => typeof window !== 'undefined'
  && typeof window.matchMedia === 'function'
  && window.matchMedia('(pointer: coarse)').matches;

function Cincin({ persen }) {
  return (
    <svg className="studio-cincin" viewBox="0 0 40 40" aria-hidden="true">
      <circle cx="20" cy="20" r="17" className="alur" />
      <circle
        cx="20" cy="20" r="17" className="isi"
        strokeDasharray={KELILING}
        strokeDashoffset={KELILING * (1 - Math.max(0.04, persen / 100))}
      />
    </svg>
  );
}

function Lapisan({ item, onCoba, onHapus }) {
  if (item.status === 'selesai') return null;
  if (item.status === 'gagal') {
    return (
      <div className="studio-lapisan galat" role="alert">
        <IkonPeringatan ukuran={22} />
        <span className="studio-lapisan-teks">{item.error || 'Gagal diunggah'}</span>
        <div className="studio-lapisan-aksi">
          {onCoba && <button type="button" onClick={onCoba}><IkonUlang ukuran={14} /> Coba lagi</button>}
          <button type="button" onClick={onHapus}>Hapus</button>
        </div>
      </div>
    );
  }
  return (
    <div className="studio-lapisan" role="status" aria-live="polite">
      <div className="studio-cincin-bungkus">
        <Cincin persen={item.status === 'unggah' ? item.progress : 4} />
        <span>{item.status === 'unggah' ? `${item.progress}%` : <IkonAwan ukuran={16} />}</span>
      </div>
      <span className="studio-lapisan-teks">{item.status === 'unggah' ? 'Mengunggah…' : 'Menyiapkan…'}</span>
    </div>
  );
}

/**
 * Tahap 1 Studio: memilih dan mengatur media.
 * Kosong  → area seret-lepas besar dengan tombol pilih/kamera.
 * Berisi  → foto: grid yang bisa diurutkan; video: kartu video + pemilih sampul.
 */
export default function MediaStage({ queue, sampul, onSampul, folder, galat, aktif = true }) {
  const inputBerkas = useRef(null);
  const inputFoto = useRef(null);
  const inputRekam = useRef(null);
  const [seret, setSeret] = useState(false);
  const [sentuh] = useState(layarSentuh);
  const dalamZona = useRef(0);

  const { items, mode } = queue;

  // Tempel tangkapan layar di mana pun di halaman ini — kecuali saat sedang
  // mengetik di kolom teks, supaya menempel teks di caption tidak terganggu.
  useEffect(() => {
    if (!aktif) return undefined;
    const saatTempel = (e) => {
      const el = e.target;
      if (el && (el.tagName === 'INPUT' || el.tagName === 'TEXTAREA' || el.isContentEditable)) return;
      const berkas = [...(e.clipboardData?.files || [])];
      if (berkas.length) { e.preventDefault(); queue.tambah(berkas); }
    };
    document.addEventListener('paste', saatTempel);
    return () => document.removeEventListener('paste', saatTempel);
  }, [aktif, queue.tambah]); // eslint-disable-line react-hooks/exhaustive-deps

  const pilih = (e) => {
    queue.tambah(e.target.files);
    e.target.value = '';
  };

  const zonaProps = {
    onDragEnter: (e) => { e.preventDefault(); dalamZona.current += 1; setSeret(true); },
    onDragOver: (e) => { e.preventDefault(); },
    onDragLeave: () => { dalamZona.current = Math.max(0, dalamZona.current - 1); if (!dalamZona.current) setSeret(false); },
    onDrop: (e) => {
      e.preventDefault();
      dalamZona.current = 0;
      setSeret(false);
      queue.tambah(e.dataTransfer.files);
    },
  };

  const inputs = (
    <>
      <input ref={inputBerkas} type="file" accept={AKSEPTASI} multiple hidden onChange={pilih} />
      <input ref={inputFoto} type="file" accept="image/*" capture="environment" hidden onChange={pilih} />
      <input ref={inputRekam} type="file" accept="video/*" capture="environment" hidden onChange={pilih} />
    </>
  );

  const pesan = queue.pesan && <div className="studio-pesan galat" role="alert"><IkonPeringatan ukuran={16} /> {queue.pesan}</div>;
  const galatWajib = galat && <div className="studio-pesan galat" role="alert" data-galat><IkonPeringatan ukuran={16} /> {galat}</div>;

  if (items.length === 0) {
    return (
      <div>
        <div className={`studio-zona${seret ? ' seret' : ''}`} {...zonaProps}>
          {inputs}
          <div className="studio-zona-ikon"><IkonUnggah ukuran={30} /></div>
          <h3>Pilih video atau foto untuk diunggah</h3>
          <p>Seret dan lepas di sini, tempel dari papan klip, atau pilih dari perangkat.</p>
          <div className="studio-zona-aksi">
            <button type="button" className="studio-tombol utama" onClick={() => inputBerkas.current?.click()}>
              <IkonUnggah ukuran={18} /> Pilih file
            </button>
            {sentuh && (
              <>
                <button type="button" className="studio-tombol" onClick={() => inputFoto.current?.click()}><IkonKamera ukuran={18} /> Foto</button>
                <button type="button" className="studio-tombol" onClick={() => inputRekam.current?.click()}><IkonVideo ukuran={18} /> Rekam</button>
              </>
            )}
          </div>
          <ul className="studio-spek">
            <li><IkonVideo ukuran={15} /> <span><b>Video</b> · MP4, WebM, MOV · maks. 100 MB · satu video per postingan</span></li>
            <li><IkonFoto ukuran={15} /> <span><b>Foto</b> · JPG, PNG, WEBP · hingga {MAKS_FOTO} foto sekaligus</span></li>
          </ul>
          <p className="studio-tips">Tips: video vertikal 9:16 umumnya tampil paling penuh di feed bergaya TikTok.</p>
        </div>
        {pesan}
        {galatWajib}
      </div>
    );
  }

  if (mode === 'video') {
    const v = items[0];
    return (
      <div {...zonaProps}>
        {inputs}
        <div className="studio-video-kartu">
          <div className="studio-video-thumb">
            <video src={v.previewUrl} muted playsInline preload="metadata" />
            <Lapisan item={v} onCoba={() => queue.cobaLagi(v.id)} onHapus={() => queue.hapus(v.id)} />
          </div>
          <div className="studio-video-info">
            <strong title={v.name}>{v.name || 'Video unggahan'}</strong>
            <div className="studio-chips">
              {v.durasi ? <span className="studio-chip">{formatDurasi(v.durasi)}</span> : null}
              {v.lebar && v.tinggi ? <span className="studio-chip">{v.lebar}×{v.tinggi} · {v.tinggi >= v.lebar ? 'Vertikal' : 'Horizontal'}</span> : null}
              {v.size ? <span className="studio-chip">{formatUkuran(v.size)}</span> : null}
              {v.status === 'selesai' && <span className="studio-chip ok">Terunggah</span>}
            </div>
            {v.status === 'unggah' && (
              <div className="studio-bar" role="progressbar" aria-valuenow={v.progress} aria-valuemin={0} aria-valuemax={100} aria-label="Progres unggah video">
                <div style={{ width: `${v.progress}%` }} />
              </div>
            )}
            <div className="studio-video-aksi">
              <button
                type="button"
                className="studio-tombol kecil"
                onClick={() => { queue.hapus(v.id); setTimeout(() => inputBerkas.current?.click(), 0); }}
              >
                <IkonUlang ukuran={15} /> Ganti video
              </button>
              <button type="button" className="studio-tombol kecil bahaya" onClick={() => queue.hapus(v.id)}>
                <IkonSampah ukuran={15} /> Hapus
              </button>
            </div>
          </div>
        </div>

        <CoverPicker key={v.id} item={v} sampul={sampul} onSampul={onSampul} folder={folder} />
        {pesan}
        {galatWajib}
      </div>
    );
  }

  return (
    <div className={`studio-foto${seret ? ' seret' : ''}`} {...zonaProps}>
      {inputs}
      <div className="studio-grid" role="list" aria-label="Foto yang akan diunggah">
        {items.map((it, i) => (
          <div className="studio-tile" role="listitem" key={it.id} data-status={it.status}>
            <img src={it.previewUrl} alt={`Foto ${i + 1}`} />
            {i === 0 && <span className="studio-tanda"><IkonSampul ukuran={12} /> Sampul</span>}
            {it.tertanam && <span className="studio-tanda kanan" title="Disimpan di dalam dokumen, bukan di Cloudinary">tertanam</span>}
            <Lapisan item={it} onCoba={() => queue.cobaLagi(it.id)} onHapus={() => queue.hapus(it.id)} />
            {it.status !== 'gagal' && (
              <div className="studio-tile-aksi">
                <button type="button" onClick={() => queue.geser(it.id, -1)} disabled={i === 0} aria-label="Geser ke kiri"><IkonKiri ukuran={16} /></button>
                <button type="button" onClick={() => queue.geser(it.id, 1)} disabled={i === items.length - 1} aria-label="Geser ke kanan"><IkonKanan ukuran={16} /></button>
                {i > 0 && <button type="button" onClick={() => queue.jadikanSampul(it.id)} aria-label="Jadikan sampul" title="Jadikan sampul"><IkonSampul ukuran={16} /></button>}
                <button type="button" className="bahaya" onClick={() => queue.hapus(it.id)} aria-label="Hapus foto"><IkonSampah ukuran={16} /></button>
              </div>
            )}
          </div>
        ))}
        {items.length < MAKS_FOTO && (
          <button type="button" className="studio-tile tambah" onClick={() => inputBerkas.current?.click()}>
            <IkonTambah ukuran={26} />
            <span>Tambah foto</span>
            <small>{items.length}/{MAKS_FOTO}</small>
          </button>
        )}
      </div>
      <p className="studio-hint">Foto pertama menjadi sampul. Geser urutan dengan tombol panah — penonton menggesernya dari kiri ke kanan.</p>
      {pesan}
      {galatWajib}
    </div>
  );
}
