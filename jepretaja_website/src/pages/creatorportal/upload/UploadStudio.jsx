import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { Link } from 'react-router-dom';
import { formatCurrency } from '../../../utils/format';
import MediaStage from './MediaStage';
import PhonePreview from './PhonePreview';
import { useMediaQueue } from './useMediaQueue';
import { bacaDraf, hapusDraf, simpanDraf } from './draf';
import { MAKS_CAPTION, ambilTagar, jadikanTagar, tambahTagar } from './teks';
import {
  IkonAwan, IkonCentang, IkonMata, IkonPaket, IkonPeringatan, IkonPin, IkonTagar, IkonTutup,
} from './StudioIcons';
import './studio.css';

const KEBIJAKAN_KOMENTAR = [
  { id: 'all', label: 'Semua orang' },
  { id: 'followers', label: 'Pengikut' },
  { id: 'off', label: 'Dimatikan' },
];

const jam = (ms) => new Intl.DateTimeFormat('id-ID', { hour: '2-digit', minute: '2-digit' }).format(new Date(ms));
const tanggalJam = (ms) => new Intl.DateTimeFormat('id-ID', { day: 'numeric', month: 'short', hour: '2-digit', minute: '2-digit' }).format(new Date(ms));

function Sakelar({ aktif, onUbah, label, id }) {
  return (
    <button
      type="button"
      role="switch"
      id={id}
      aria-checked={aktif}
      aria-label={label}
      className={`studio-sakelar${aktif ? ' aktif' : ''}`}
      onClick={() => onUbah(!aktif)}
    >
      <i />
    </button>
  );
}

/**
 * Studio Unggah creator — pengganti formulir lama.
 *
 * Alurnya meniru TikTok Studio: pilih media (unggah langsung berjalan dengan
 * progres), tulis caption dengan tagar, lengkapi detail, lalu publikasikan —
 * sambil pratinjau feed di sisi kanan berubah seketika. Draf tersimpan otomatis.
 *
 * Komponen ini tidak menyentuh Firestore. `onSubmit(isi)` dari pemanggil yang
 * menulis (create/update), sehingga aturan firestore.rules — creator tidak
 * boleh menyentuh `status`/`moderationStatus` — tetap terpusat di CreatorPosts.
 *
 * @param {object} props
 * @param {string} props.uid
 * @param {string} props.namaCreator
 * @param {Array<{id:string,name?:string}>} props.kategori
 * @param {Array<{id:string,name:string,price?:number,active?:boolean}>} props.paket
 * @param {string[]} props.saranTagar     tagar usulan (tanpa #)
 * @param {string[]} props.lokasiTerakhir lokasi yang pernah dipakai creator
 * @param {object|null} props.karya       karya yang sedang diubah, atau null
 * @param {(isi:object)=>Promise<void>} props.onSubmit
 * @param {(tujuan:'kelola'|'baru')=>void} props.onSelesai
 * @param {()=>void} [props.onBatal]
 * @param {boolean} [props.aktif] false saat tab disembunyikan (menahan tempel-dari-papan-klip)
 */
export default function UploadStudio({
  uid, namaCreator, kategori = [], paket = [], saranTagar = [], lokasiTerakhir = [],
  karya = null, onSubmit, onSelesai, onBatal, aktif = true,
}) {
  const mengubah = Boolean(karya);
  const folder = `explore/${uid || 'creator'}`;

  const tagarAwal = karya ? ambilTagar(karya.caption) : [];
  const [caption, setCaption] = useState(karya?.caption || '');
  const [category, setCategory] = useState(karya?.category || '');
  const [location, setLocation] = useState(karya?.location || '');
  const [extraTags, setExtraTags] = useState(
    () => (karya?.tags || []).map(jadikanTagar).filter((t) => t && !tagarAwal.includes(t))
  );
  const [packageId, setPackageId] = useState(karya?.packageId || '');
  const [commentPolicy, setCommentPolicy] = useState(karya?.commentPolicy || 'all');
  const [allowSave, setAllowSave] = useState(karya ? karya.allowSave !== false : true);

  const queue = useMediaQueue({
    folder,
    awal: karya?.mediaUrls || [],
    jenisAwal: karya?.type || 'photo',
  });

  const [sampul, setSampul] = useState(() => {
    if (karya?.type === 'video' && karya.thumbnailUrl && karya.thumbnailUrl !== karya.mediaUrls?.[0]) {
      return { url: karya.thumbnailUrl, previewUrl: karya.thumbnailUrl, status: 'selesai', detik: null };
    }
    return { url: '', previewUrl: '', status: 'idle', detik: null };
  });
  const ubahSampul = useCallback((patch) => setSampul((s) => ({ ...s, ...patch })), []);

  // Video diganti/dihapus → sampul lama tidak berlaku lagi.
  const idVideo = queue.mode === 'video' ? queue.items[0]?.id : null;
  const idVideoSebelumnya = useRef(idVideo);
  useEffect(() => {
    if (idVideoSebelumnya.current !== idVideo && idVideoSebelumnya.current !== null) {
      setSampul({ url: '', previewUrl: '', status: 'idle', detik: null });
    }
    idVideoSebelumnya.current = idVideo;
  }, [idVideo]);

  const [galat, setGalat] = useState({});
  const [kirim, setKirim] = useState('diam'); // 'diam' | 'mengirim' | 'sukses'
  const [pratinjau, setPratinjau] = useState(false);
  const [draf, setDraf] = useState(() => (mengubah ? null : bacaDraf(uid)));
  const [drafTersimpan, setDrafTersimpan] = useState(null);
  const [catatanDraf, setCatatanDraf] = useState(null);
  const teksCaption = useRef(null);

  const tagarCaption = useMemo(() => ambilTagar(caption), [caption]);
  const semuaTagar = useMemo(() => [...new Set([...tagarCaption, ...extraTags])], [tagarCaption, extraTags]);
  const paketDipilih = paket.find((p) => p.id === packageId) || null;
  const daftarPaket = paket.filter((p) => p.active !== false || p.id === packageId);

  const kosong = !caption.trim() && !queue.items.length && !category && !location && !packageId && extraTags.length === 0;
  const kotor = !kosong && kirim !== 'sukses';

  // Konfirmasi sebelum menutup tab kalau ada unggahan berjalan atau isian yang belum terkirim.
  useEffect(() => {
    if (!(kotor || queue.sibuk) || kirim === 'sukses') return undefined;
    const jaga = (e) => { e.preventDefault(); e.returnValue = ''; };
    window.addEventListener('beforeunload', jaga);
    return () => window.removeEventListener('beforeunload', jaga);
  }, [kotor, queue.sibuk, kirim]);

  const bentukDraf = useCallback(() => ({
    caption, category, location, extraTags, packageId, commentPolicy, allowSave,
    jenis: queue.mode === 'video' ? 'video' : 'photo',
    media: queue.items.filter((i) => i.status === 'selesai' && !i.url.startsWith('data:')).map((i) => i.url),
    sampulUrl: sampul.url || '',
  }), [caption, category, location, extraTags, packageId, commentPolicy, allowSave, queue.mode, queue.items, sampul.url]);

  const tulisDraf = useCallback((manual) => {
    const hasil = simpanDraf(uid, bentukDraf());
    if (hasil === 'gagal') {
      setCatatanDraf('Draf tidak bisa disimpan — penyimpanan browser penuh.');
      return;
    }
    setDrafTersimpan(Date.now());
    setCatatanDraf(hasil === 'tanpa-media' ? 'Draf tersimpan tanpa media (terlalu besar untuk browser).' : (manual ? 'Draf disimpan di perangkat ini.' : null));
  }, [uid, bentukDraf]);

  // Simpan otomatis 1 detik setelah perubahan terakhir. Ditahan selama tawaran
  // "Lanjutkan draf" masih tampil supaya draf lama tidak tertimpa isian kosong.
  useEffect(() => {
    if (mengubah || kirim !== 'diam' || draf || kosong) return undefined;
    const t = setTimeout(() => tulisDraf(false), 1000);
    return () => clearTimeout(t);
  }, [mengubah, kirim, draf, kosong, tulisDraf]);

  const lanjutkanDraf = () => {
    const d = draf.data;
    setCaption(d.caption);
    setCategory(d.category);
    setLocation(d.location);
    setExtraTags(d.extraTags.map(jadikanTagar).filter(Boolean));
    setPackageId(d.packageId);
    setCommentPolicy(d.commentPolicy);
    setAllowSave(d.allowSave);
    queue.isiDari(d.media, d.jenis);
    setSampul(d.sampulUrl
      ? { url: d.sampulUrl, previewUrl: d.sampulUrl, status: 'selesai', detik: null }
      : { url: '', previewUrl: '', status: 'idle', detik: null });
    setDraf(null);
  };

  const buangDraf = () => { hapusDraf(uid); setDraf(null); };

  const sisipTagar = () => {
    const el = teksCaption.current;
    setCaption((c) => `${c}${c === '' || /\s$/.test(c) ? '' : ' '}#`);
    setTimeout(() => { el?.focus(); el?.setSelectionRange(el.value.length, el.value.length); }, 0);
  };

  const usulan = useMemo(
    () => saranTagar.filter((t) => t && !semuaTagar.includes(t)).slice(0, 10),
    [saranTagar, semuaTagar]
  );

  const publikasi = async () => {
    const g = {};
    if (queue.items.length === 0) g.media = 'Tambahkan minimal satu foto atau video.';
    else if (queue.sibuk) g.media = 'Tunggu sampai unggahan media selesai.';
    else if (queue.gagal) g.media = 'Ada media yang gagal diunggah. Coba lagi atau hapus dulu.';
    else if (queue.mode === 'video' && (sampul.status === 'unggah' || sampul.status === 'proses')) g.media = 'Sampul video masih diproses. Tunggu sebentar.';
    if (!caption.trim()) g.caption = 'Caption wajib diisi — itu yang dibaca calon pelanggan di feed.';
    setGalat(g);
    if (Object.keys(g).length) {
      setTimeout(() => document.querySelector('[data-galat]')?.scrollIntoView({ behavior: 'smooth', block: 'center' }), 60);
      return;
    }

    const urls = queue.items.map((i) => i.url);
    const video = queue.mode === 'video';
    const durasi = video ? queue.items[0].durasi : null;
    const isi = {
      caption: caption.trim(),
      category: category.trim() || null,
      location: location.trim() || null,
      tags: semuaTagar,
      mediaUrls: urls,
      type: video ? 'video' : 'photo',
      // Video: sampul hasil pilihan; kalau gagal dibuat, jatuh ke perilaku lama.
      thumbnailUrl: video ? (sampul.url || urls[0]) : urls[0],
      commentPolicy,
      allowSave,
      packageId: paketDipilih?.id || null,
      packageName: paketDipilih?.name || null,
      durationSec: video ? (durasi ? Math.round(durasi) : (karya?.durationSec ?? null)) : null,
    };

    setKirim('mengirim');
    try {
      await onSubmit(isi);
      hapusDraf(uid);
      setKirim('sukses');
    } catch (err) {
      setGalat({ umum: err?.message || 'Gagal menyimpan karya.' });
      setKirim('diam');
      setTimeout(() => document.querySelector('[data-galat]')?.scrollIntoView({ behavior: 'smooth', block: 'center' }), 60);
    }
  };

  if (kirim === 'sukses') {
    return (
      <div className="studio">
        <div className="studio-sukses" role="status">
          <div className="studio-sukses-ikon"><IkonCentang ukuran={38} /></div>
          <h2>{mengubah ? 'Perubahan tersimpan' : 'Karya terkirim!'}</h2>
          <p>
            {mengubah
              ? 'Karyamu sudah diperbarui. Status review-nya tidak berubah.'
              : 'Karyamu sedang menunggu review moderator. Setelah disetujui, karya tayang di Explore — pantau statusnya di Kelola Karya.'}
          </p>
          <div className="studio-sukses-aksi">
            <button type="button" className="studio-tombol utama" onClick={() => onSelesai('kelola')}>Lihat karya saya</button>
            {!mengubah && <button type="button" className="studio-tombol" onClick={() => onSelesai('baru')}>Unggah karya lain</button>}
          </div>
        </div>
      </div>
    );
  }

  const mediaSiap = queue.siap;
  const captionSiap = caption.trim().length > 0;
  const langkah = [
    { nama: 'Media', selesai: mediaSiap },
    { nama: 'Detail', selesai: mediaSiap && captionSiap },
    { nama: 'Publikasi', selesai: false },
  ];
  const langkahAktif = langkah.findIndex((l) => !l.selesai);

  const fotoPratinjau = queue.items.map((i) => i.previewUrl).filter(Boolean);
  const namaKategori = (k) => k.name || k.id;

  return (
    <div className="studio">
      <header className="studio-kepala">
        <div>
          <h1>{mengubah ? 'Ubah karya' : 'Unggah karya'}</h1>
          <p>{mengubah ? 'Perbarui media, caption, dan pengaturan karyamu.' : 'Tampilkan hasil jepretanmu ke calon pelanggan di Explore.'}</p>
        </div>
        <div className="studio-kepala-aksi">
          {!mengubah && (
            <span className="studio-status-draf" role="status" aria-live="polite">
              {drafTersimpan ? <><IkonCentang ukuran={14} /> Draf tersimpan {jam(drafTersimpan)}</> : 'Draf tersimpan otomatis'}
            </span>
          )}
          <button type="button" className="studio-tombol kecil studio-btn-pratinjau" onClick={() => setPratinjau(true)}>
            <IkonMata ukuran={16} /> Pratinjau
          </button>
        </div>
      </header>

      <ol className="studio-langkah" aria-label="Tahapan unggah">
        {langkah.map((l, i) => (
          <li key={l.nama} className={l.selesai ? 'selesai' : (i === langkahAktif ? 'aktif' : '')} aria-current={i === langkahAktif ? 'step' : undefined}>
            <span className="studio-langkah-nomor">{l.selesai ? <IkonCentang ukuran={13} /> : i + 1}</span>
            <span>{l.nama}</span>
          </li>
        ))}
      </ol>

      {draf && (
        <div className="studio-banner" role="region" aria-label="Draf tersimpan">
          <IkonAwan ukuran={20} />
          <div>
            <strong>Lanjutkan draf terakhirmu?</strong>
            <span>
              {draf.data.caption.trim() ? `“${draf.data.caption.trim().slice(0, 48)}${draf.data.caption.trim().length > 48 ? '…' : ''}”` : 'Draf tanpa caption'}
              {draf.disimpanPada ? ` · disimpan ${tanggalJam(draf.disimpanPada)}` : ''}
              {draf.data.media.length ? ` · ${draf.data.media.length} media` : ''}
            </span>
          </div>
          <div className="studio-banner-aksi">
            <button type="button" className="studio-tombol kecil utama" onClick={lanjutkanDraf}>Lanjutkan</button>
            <button type="button" className="studio-tombol kecil" onClick={buangDraf}>Mulai baru</button>
          </div>
        </div>
      )}

      {galat.umum && (
        <div className="studio-pesan galat" role="alert" data-galat><IkonPeringatan ukuran={16} /> {galat.umum}</div>
      )}

      <div className="studio-tata">
        <div className="studio-kolom">
          <section className="studio-kartu" aria-labelledby="studio-h-media">
            <div className="studio-kartu-kepala">
              <span className="studio-nomor">1</span>
              <div>
                <h2 id="studio-h-media">Media</h2>
                <p>Unggahan langsung berjalan begitu file dipilih.</p>
              </div>
            </div>
            <MediaStage aktif={aktif} queue={queue} sampul={sampul} onSampul={ubahSampul} folder={folder} galat={galat.media} />
          </section>

          <section className="studio-kartu" aria-labelledby="studio-h-detail">
            <div className="studio-kartu-kepala">
              <span className="studio-nomor">2</span>
              <div>
                <h2 id="studio-h-detail">Detail</h2>
                <p>Ceritakan karyamu supaya mudah ditemukan.</p>
              </div>
            </div>

            <div className="studio-medan">
              <div className="studio-medan-kepala">
                <label htmlFor="studio-caption">Caption</label>
                <span className={`studio-hitung${caption.length > MAKS_CAPTION * 0.9 ? ' hampir' : ''}`}>{caption.length}/{MAKS_CAPTION}</span>
              </div>
              <textarea
                id="studio-caption"
                ref={teksCaption}
                className={`studio-input${galat.caption ? ' salah' : ''}`}
                rows={4}
                maxLength={MAKS_CAPTION}
                value={caption}
                placeholder="Ceritakan karya ini… tambahkan #tagar supaya mudah ditemukan"
                aria-invalid={Boolean(galat.caption)}
                onChange={(e) => { setCaption(e.target.value); if (galat.caption) setGalat((g) => ({ ...g, caption: null })); }}
              />
              {galat.caption && <div className="studio-pesan galat kecil" role="alert" data-galat><IkonPeringatan ukuran={14} /> {galat.caption}</div>}
              <div className="studio-caption-alat">
                <button type="button" className="studio-tombol kecil" onClick={sisipTagar}><IkonTagar ukuran={15} /> Tagar</button>
                {usulan.map((t) => (
                  <button key={t} type="button" className="studio-saran" onClick={() => setCaption((c) => tambahTagar(c, t))}>#{t}</button>
                ))}
              </div>
              {extraTags.length > 0 && (
                <div className="studio-caption-alat">
                  <span className="studio-hint">Tagar dari unggahan sebelumnya:</span>
                  {extraTags.map((t) => (
                    <span className="studio-chip tagar" key={t}>
                      #{t}
                      <button type="button" aria-label={`Hapus tagar ${t}`} onClick={() => setExtraTags((l) => l.filter((x) => x !== t))}><IkonTutup ukuran={12} /></button>
                    </span>
                  ))}
                </div>
              )}
            </div>

            <div className="studio-medan">
              <div className="studio-medan-kepala"><label id="studio-l-kategori">Kategori</label></div>
              {kategori.length > 0 ? (
                <div className="studio-pil" role="radiogroup" aria-labelledby="studio-l-kategori">
                  <button type="button" role="radio" aria-checked={category === ''} className={category === '' ? 'aktif' : ''} onClick={() => setCategory('')}>Tanpa kategori</button>
                  {kategori.map((k) => (
                    <button key={k.id} type="button" role="radio" aria-checked={category === namaKategori(k)} className={category === namaKategori(k) ? 'aktif' : ''} onClick={() => setCategory(namaKategori(k))}>
                      {namaKategori(k)}
                    </button>
                  ))}
                </div>
              ) : (
                <input className="studio-input" value={category} placeholder="Wedding" onChange={(e) => setCategory(e.target.value)} aria-labelledby="studio-l-kategori" />
              )}
            </div>

            <div className="studio-medan">
              <div className="studio-medan-kepala"><label htmlFor="studio-lokasi">Lokasi</label></div>
              <div className="studio-input-ikon">
                <IkonPin ukuran={18} />
                <input id="studio-lokasi" className="studio-input" value={location} placeholder="Tambahkan lokasi, mis. Yogyakarta" onChange={(e) => setLocation(e.target.value)} />
              </div>
              {lokasiTerakhir.filter((l) => l !== location).length > 0 && (
                <div className="studio-caption-alat">
                  {lokasiTerakhir.filter((l) => l !== location).slice(0, 4).map((l) => (
                    <button key={l} type="button" className="studio-saran" onClick={() => setLocation(l)}>{l}</button>
                  ))}
                </div>
              )}
            </div>
          </section>

          <section className="studio-kartu" aria-labelledby="studio-h-paket">
            <div className="studio-kartu-kepala">
              <span className="studio-nomor"><IkonPaket ukuran={16} /></span>
              <div>
                <h2 id="studio-h-paket">Tautkan paket jasa</h2>
                <p>Penonton bisa langsung memesan dari karya ini. Opsional.</p>
              </div>
            </div>
            {daftarPaket.length === 0 ? (
              <div className="studio-kosong-kecil">
                Belum ada paket jasa. <Link to="/creator/packages">Buat paket</Link> dulu supaya karya bisa mengarahkan penonton ke booking.
              </div>
            ) : (
              <div className="studio-paket" role="radiogroup" aria-label="Paket jasa">
                <button type="button" role="radio" aria-checked={packageId === ''} className={`studio-paket-item${packageId === '' ? ' aktif' : ''}`} onClick={() => setPackageId('')}>
                  <span className="studio-radio" />
                  <span><b>Tanpa tautan</b><small>Karya tampil tanpa tombol pesan</small></span>
                </button>
                {daftarPaket.map((p) => (
                  <button key={p.id} type="button" role="radio" aria-checked={packageId === p.id} className={`studio-paket-item${packageId === p.id ? ' aktif' : ''}`} onClick={() => setPackageId(p.id)}>
                    <span className="studio-radio" />
                    <span><b>{p.name}</b><small>{formatCurrency(p.price)}{p.active === false ? ' · nonaktif' : ''}</small></span>
                  </button>
                ))}
              </div>
            )}
          </section>

          <section className="studio-kartu" aria-labelledby="studio-h-atur">
            <div className="studio-kartu-kepala">
              <span className="studio-nomor"><IkonMata ukuran={16} /></span>
              <div>
                <h2 id="studio-h-atur">Pengaturan</h2>
                <p>Berlaku juga di server, bukan sekadar tampilan.</p>
              </div>
            </div>
            <div className="studio-atur">
              <div className="studio-atur-baris">
                <div><b id="studio-l-komentar">Siapa yang boleh berkomentar</b><small>Kolom komentar bisa ditutup kapan saja.</small></div>
                <div className="studio-segmen" role="radiogroup" aria-labelledby="studio-l-komentar">
                  {KEBIJAKAN_KOMENTAR.map((k) => (
                    <button key={k.id} type="button" role="radio" aria-checked={commentPolicy === k.id} className={commentPolicy === k.id ? 'aktif' : ''} onClick={() => setCommentPolicy(k.id)}>{k.label}</button>
                  ))}
                </div>
              </div>
              <div className="studio-atur-baris">
                <div><b id="studio-l-simpan">Izinkan orang menyimpan karya ini</b><small>Tombol simpan disembunyikan jika dimatikan.</small></div>
                <Sakelar aktif={allowSave} onUbah={setAllowSave} label="Izinkan menyimpan karya" id="studio-simpan" />
              </div>
            </div>
          </section>
        </div>

        <aside className={`studio-samping${pratinjau ? ' buka' : ''}`} aria-label="Pratinjau">
          <div className="studio-samping-dalam">
            <button type="button" className="studio-tutup-pratinjau" onClick={() => setPratinjau(false)} aria-label="Tutup pratinjau"><IkonTutup ukuran={18} /></button>
            <div className="studio-samping-judul">Pratinjau di feed</div>
            <PhonePreview
              media={fotoPratinjau}
              kind={queue.mode === 'video' ? 'video' : 'foto'}
              cover={sampul.previewUrl}
              caption={caption}
              namaCreator={namaCreator}
              kategori={category}
              lokasi={location.trim()}
              paket={paketDipilih}
              komentar={commentPolicy}
              bolehSimpan={allowSave}
            />
            <p className="studio-hint tengah">Tampilan sebenarnya bisa sedikit berbeda di setiap perangkat.</p>
          </div>
        </aside>
      </div>

      <div className="studio-aksi" role="region" aria-label="Aksi publikasi">
        <div className="studio-aksi-status" role="status" aria-live="polite">
          {queue.sibuk ? (
            <span className="studio-sibuk"><IkonAwan ukuran={16} /> Mengunggah media… {queue.selesai}/{queue.items.length}</span>
          ) : (
            <>
              <span className={`studio-cek${mediaSiap ? ' ok' : ''}`}>{mediaSiap ? <IkonCentang ukuran={13} /> : <i />} Media</span>
              <span className={`studio-cek${captionSiap ? ' ok' : ''}`}>{captionSiap ? <IkonCentang ukuran={13} /> : <i />} Caption</span>
            </>
          )}
          {catatanDraf && <span className="studio-catatan">{catatanDraf}</span>}
        </div>
        <div className="studio-aksi-tombol">
          {mengubah ? (
            <button type="button" className="studio-tombol" onClick={onBatal} disabled={kirim === 'mengirim'}>Batal</button>
          ) : (
            <button type="button" className="studio-tombol" onClick={() => tulisDraf(true)} disabled={kosong || kirim === 'mengirim'}>Simpan draf</button>
          )}
          <button type="button" className="studio-tombol utama besar" onClick={publikasi} disabled={kirim === 'mengirim' || queue.sibuk}>
            {kirim === 'mengirim' ? 'Mengirim…' : (mengubah ? 'Simpan perubahan' : 'Publikasikan')}
          </button>
        </div>
      </div>
    </div>
  );
}
