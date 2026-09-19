import { Fragment, useEffect, useRef, useState } from 'react';
import { formatCurrency } from '../../../utils/format';
import {
  IkonBagikan, IkonJeda, IkonKanan, IkonKiri, IkonKomentar, IkonPaket, IkonPin,
  IkonPutar, IkonSimpan, IkonSuka,
} from './StudioIcons';

const POLA_TAGAR = /(#[\p{L}\p{N}_]+)/gu;

/** Caption dengan tagar ditebalkan, persis seperti tampil di feed. */
function CaptionBertagar({ teks }) {
  return teks.split(POLA_TAGAR).map((bagian, i) => (
    bagian.startsWith('#') && bagian.length > 1
      ? <b key={i} className="tagar">{bagian}</b>
      : <Fragment key={i}>{bagian}</Fragment>
  ));
}

/**
 * Pratinjau feed. Meniru layar Explore — media penuh, kolom aksi di kanan,
 * caption dan tautan paket di kiri bawah — dan ikut berubah seketika saat
 * caption, sampul, atau pengaturan komentar/simpan diubah, sehingga creator
 * tahu persis seperti apa karyanya sebelum dikirim.
 */
export default function PhonePreview({
  media, kind, cover, caption, namaCreator, kategori, lokasi, paket, komentar, bolehSimpan,
}) {
  const [indeks, setIndeks] = useState(0);
  const [memutar, setMemutar] = useState(false);
  const [terbuka, setTerbuka] = useState(false);
  const videoRef = useRef(null);

  useEffect(() => { if (indeks >= media.length) setIndeks(Math.max(0, media.length - 1)); }, [media.length, indeks]);
  useEffect(() => { setMemutar(false); }, [media[0]]);

  const ada = media.length > 0;
  const saatIni = media[indeks];
  const inisial = (namaCreator || 'C').trim().charAt(0).toUpperCase();

  const alihPutar = () => {
    const v = videoRef.current;
    if (!v) return;
    if (v.paused) { v.play().then(() => setMemutar(true)).catch(() => {}); } else { v.pause(); setMemutar(false); }
  };

  return (
    <div className="studio-hp" aria-label="Pratinjau di feed">
      <div className="studio-hp-layar">
        {!ada && (
          <div className="studio-hp-kosong">
            <IkonPutar ukuran={28} />
            <span>Pratinjau karyamu muncul di sini</span>
          </div>
        )}

        {ada && kind === 'video' && (
          <button type="button" className="studio-hp-media" onClick={alihPutar} aria-label={memutar ? 'Jeda video' : 'Putar video'}>
            <video
              ref={videoRef}
              src={saatIni}
              poster={cover || undefined}
              muted
              loop
              playsInline
              preload="metadata"
            />
            {!memutar && <span className="studio-hp-putar"><IkonPutar ukuran={26} /></span>}
            {memutar && <span className="studio-hp-putar redup"><IkonJeda ukuran={22} /></span>}
          </button>
        )}

        {ada && kind !== 'video' && (
          <div className="studio-hp-media">
            <img src={saatIni} alt="" />
            {media.length > 1 && (
              <>
                <button type="button" className="studio-hp-nav kiri" disabled={indeks === 0} onClick={() => setIndeks(indeks - 1)} aria-label="Foto sebelumnya"><IkonKiri ukuran={16} /></button>
                <button type="button" className="studio-hp-nav kanan" disabled={indeks === media.length - 1} onClick={() => setIndeks(indeks + 1)} aria-label="Foto berikutnya"><IkonKanan ukuran={16} /></button>
                <div className="studio-hp-titik" aria-hidden="true">
                  {media.map((_, i) => <i key={i} className={i === indeks ? 'aktif' : ''} />)}
                </div>
              </>
            )}
          </div>
        )}

        <div className="studio-hp-atas" aria-hidden="true">
          <span>Mengikuti</span><span className="aktif">Explore</span>
        </div>

        <div className="studio-hp-rel" aria-hidden="true">
          <div className="studio-hp-avatar">{inisial}</div>
          <div><IkonSuka ukuran={26} /><small>Suka</small></div>
          <div className={komentar === 'off' ? 'mati' : ''}><IkonKomentar ukuran={26} /><small>{komentar === 'off' ? 'Ditutup' : 'Komentar'}</small></div>
          <div className={bolehSimpan ? '' : 'mati'}><IkonSimpan ukuran={26} /><small>{bolehSimpan ? 'Simpan' : 'Nonaktif'}</small></div>
          <div><IkonBagikan ukuran={26} /><small>Bagikan</small></div>
        </div>

        <div className="studio-hp-bawah">
          <div className="studio-hp-nama">@{(namaCreator || 'creator').toLowerCase().replace(/\s+/g, '')}</div>
          {caption.trim() ? (
            <p
              className={`studio-hp-caption${terbuka ? ' terbuka' : ''}`}
              onClick={() => setTerbuka((t) => !t)}
            >
              <CaptionBertagar teks={caption} />
            </p>
          ) : (
            <p className="studio-hp-caption kosong">Caption karyamu akan tampil di sini…</p>
          )}
          <div className="studio-hp-lencana">
            {kategori && <span>{kategori}</span>}
            {lokasi && <span><IkonPin ukuran={12} /> {lokasi}</span>}
          </div>
          {paket && (
            <div className="studio-hp-paket">
              <IkonPaket ukuran={16} />
              <span>
                <b>{paket.name}</b>
                {paket.price ? <small>{formatCurrency(paket.price)}</small> : null}
              </span>
              <em>Pesan</em>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
