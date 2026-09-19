import { useMemo, useState } from 'react';
import { useSearchParams } from 'react-router-dom';
import { addDoc, collection, deleteDoc, doc, serverTimestamp, updateDoc, where } from 'firebase/firestore';
import { db } from '../../firebase/config';
import { PATHS } from '../../firebase/paths';
import { useCollection } from '../../hooks/useCollection';
import { useAuth } from '../../auth/AuthContext';
import StatusBadge from '../../components/StatusBadge';
import StatCard from '../../components/StatCard';
import EmptyState from '../../components/EmptyState';
import ErrorState from '../../components/ErrorState';
import { useConfirm } from '../../components/ConfirmDialog';
import { formatDateTime, compactNumber } from '../../utils/format';
import { byNewest } from '../../utils/sort';
import CreatorPortfolio from './CreatorPortfolio';
import UploadStudio from './upload/UploadStudio';
import { jadikanTagar } from './upload/teks';

const TAB = [
  { id: 'semua', label: 'Semua' },
  { id: 'pending_review', label: 'Menunggu Review' },
  { id: 'published', label: 'Tayang' },
  { id: 'rejected', label: 'Ditolak' },
  { id: 'hidden', label: 'Disembunyikan' },
];

// Tagar usulan bila creator belum punya riwayat unggahan.
const TAGAR_DASAR = ['fotografi', 'wedding', 'prewedding', 'portrait', 'goldenhour', 'jepretaja'];

const TAMPILAN = ['unggah', 'kelola', 'portfolio'];

/**
 * "Unggahan Saya" — karya Explore milik creator yang sedang login.
 *
 * Sampai sekarang portal creator tidak punya satu pun layar untuk ini: karya
 * hanya bisa diunggah dan dilihat lewat aplikasi Android, sementara admin
 * justru punya layar penuh untuk memoderasinya. Akibatnya seorang creator
 * yang karyanya ditolak tidak punya cara apa pun untuk mengetahui alasannya
 * dari web, apalagi memperbaikinya.
 *
 * Karena itu `moderationNote` — catatan yang WAJIB diisi admin saat menolak
 * atau menyembunyikan karya (lihat pages/explore/PostDetail.jsx) — ditampilkan
 * mencolok di kartu karyanya. Catatan itu memang ditulis untuk dibaca creator;
 * tanpa layar ini, ia tidak pernah sampai ke tujuannya.
 *
 * Layar ini punya tiga tampilan (parameter `?tab=`): `unggah` — Studio Unggah
 * bergaya TikTok (pages/creatorportal/upload), `kelola` — daftar karya beserta
 * status review, dan `portfolio`. Formulir unggah lama sudah digantikan Studio;
 * yang tersisa di sini hanya penulisan ke Firestore dan daftar karya.
 *
 * Batas yang datang dari firestore.rules dan sengaja tidak dilawan:
 * creator boleh mengubah dan menghapus karyanya sendiri, TAPI tidak boleh
 * menyentuh `status` maupun `moderationStatus`. Jadi tidak ada tombol
 * "terbitkan ulang" di sini — memindahkan karya yang ditolak kembali ke antrian
 * adalah keputusan admin, bukan keputusan pengunggahnya.
 */
export default function CreatorPosts() {
  const { user, creatorProfile } = useAuth();
  const uid = user?.uid;
  const { confirm, dialog } = useConfirm();
  const [params, setParams] = useSearchParams();

  const { data: posts, loading, error } = useCollection(
    collection(db, PATHS.explorePosts),
    uid ? [where('creatorId', '==', uid)] : []
  );
  const { data: kategori } = useCollection(collection(db, PATHS.categories));
  const { data: paket } = useCollection(
    collection(db, PATHS.packages),
    uid ? [where('creatorId', '==', uid)] : []
  );

  const tampilan = TAMPILAN.includes(params.get('tab')) ? params.get('tab') : 'unggah';
  const gantiTampilan = (t) => setParams(t === 'unggah' ? {} : { tab: t });

  const [tab, setTab] = useState('semua');
  const [query, setQuery] = useState('');
  const [diubah, setDiubah] = useState(null);
  const [studioKey, setStudioKey] = useState(0);
  const [pesan, setPesan] = useState(null);

  const daftar = useMemo(() => {
    const urut = byNewest(posts);
    // Karya berstatus 'deleted' sudah dibuang admin; menampilkannya di sini
    // hanya membingungkan karena creator tidak bisa berbuat apa-apa atasnya.
    const terlihat = urut.filter((p) => p.status !== 'deleted');
    const sesuaiStatus = tab === 'semua' ? terlihat : terlihat.filter((p) => p.status === tab);
    const kata = query.trim().toLowerCase();
    return kata
      ? sesuaiStatus.filter((p) => [p.title, p.caption, p.category, p.location].filter(Boolean).join(' ').toLowerCase().includes(kata))
      : sesuaiStatus;
  }, [posts, tab, query]);

  const jumlahPerStatus = posts.reduce((acc, p) => {
    acc[p.status] = (acc[p.status] || 0) + 1;
    return acc;
  }, {});

  const totalTayang = jumlahPerStatus.published || 0;
  const totalSuka = posts.reduce((t, p) => t + (Number(p.metrics?.like) || 0), 0);
  const totalDilihat = posts.reduce((t, p) => t + (Number(p.metrics?.view) || 0), 0);
  const totalKarya = posts.filter((p) => p.status !== 'deleted').length;

  // Usulan untuk Studio: tagar yang paling sering dipakai creator ini, lalu
  // nama kategori, lalu bekal awal — supaya creator baru pun tidak menghadapi
  // kolom tagar yang kosong melompong.
  const saranTagar = useMemo(() => {
    const hitung = new Map();
    posts.forEach((p) => (p.tags || []).forEach((t) => {
      const kunci = jadikanTagar(t);
      if (kunci) hitung.set(kunci, (hitung.get(kunci) || 0) + 1);
    }));
    const populer = [...hitung.entries()].sort((a, b) => b[1] - a[1]).map(([t]) => t);
    const dariKategori = kategori.map((k) => jadikanTagar(k.name || k.id)).filter(Boolean);
    return [...new Set([...populer, ...dariKategori, ...TAGAR_DASAR])].slice(0, 12);
  }, [posts, kategori]);

  const lokasiTerakhir = useMemo(
    () => [...new Set(byNewest(posts).map((p) => (p.location || '').trim()).filter(Boolean))].slice(0, 5),
    [posts]
  );

  const bukaUbah = (p) => {
    setPesan(null);
    setDiubah(p);
    gantiTampilan('unggah');
    window.scrollTo({ top: 0, behavior: 'smooth' });
  };

  const bukaBaru = () => {
    setPesan(null);
    setDiubah(null);
    setStudioKey((k) => k + 1);
    gantiTampilan('unggah');
  };

  /**
   * Satu-satunya tempat Studio menulis ke Firestore.
   * Saat mengubah, `status` sengaja TIDAK ikut dikirim: firestore.rules menolak
   * setiap perubahan yang menyentuhnya, dan mengirim ulang nilai yang sama
   * hanya menambah risiko kalau suatu saat aturannya diperketat.
   */
  const simpanKarya = async (isi) => {
    try {
      if (diubah) {
        await updateDoc(doc(db, PATHS.explorePosts, diubah.id), isi);
      } else {
        await addDoc(collection(db, PATHS.explorePosts), {
          ...isi,
          creatorId: uid,
          creatorName: creatorProfile?.displayName || null,
          status: 'pending_review',
          moderationNote: null,
          metrics: { like: 0, comment: 0, save: 0, view: 0, share: 0 },
          createdAt: serverTimestamp(),
        });
      }
    } catch (err) {
      throw new Error(
        err.code === 'permission-denied'
          ? 'Perubahan ditolak server. Status dan hasil moderasi hanya bisa diubah admin.'
          : err.message || 'Gagal menyimpan karya.'
      );
    }
  };

  const selesaiStudio = (tujuan) => {
    setDiubah(null);
    // Studio dibuat ulang agar layar "terkirim" tidak muncul lagi nanti.
    setStudioKey((k) => k + 1);
    gantiTampilan(tujuan === 'baru' ? 'unggah' : 'kelola');
  };

  const hapus = async (p) => {
    const ok = await confirm({
      title: 'Hapus karya ini?',
      message: 'Karya beserta suka dan komentarnya hilang dari aplikasi dan tidak bisa dikembalikan.',
      danger: true,
      confirmLabel: 'Ya, Hapus',
    });
    if (!ok) return;
    try {
      await deleteDoc(doc(db, PATHS.explorePosts, p.id));
      setPesan({ tipe: 'sukses', teks: 'Karya dihapus.' });
    } catch (err) {
      setPesan({ tipe: 'gagal', teks: err.message || 'Gagal menghapus karya.' });
    }
  };

  const pindah = (
    <div className="creator-mode-switcher" role="tablist" aria-label="Tampilan unggahan">
      {[
        { id: 'unggah', label: diubah ? 'Ubah Karya' : 'Unggah' },
        { id: 'kelola', label: `Kelola Karya${totalKarya ? ` (${totalKarya})` : ''}` },
        { id: 'portfolio', label: 'Portfolio' },
      ].map((t) => (
        <button
          key={t.id}
          type="button"
          role="tab"
          aria-selected={tampilan === t.id}
          className={`creator-mode-btn ${tampilan === t.id ? 'active' : ''}`}
          onClick={() => gantiTampilan(t.id)}
        >
          {t.label}
        </button>
      ))}
    </div>
  );

  return (
    <div className={`creator-shell${tampilan === 'unggah' ? ' lebar' : ''}`}>
      {dialog}
      {pindah}

      {tampilan === 'portfolio' && <CreatorPortfolio embedded />}

      {/* Studio tetap terpasang (hanya disembunyikan) saat pindah tab, supaya
          unggahan yang sedang berjalan tidak putus hanya karena creator
          mengintip tab Kelola Karya. */}
      <div hidden={tampilan !== 'unggah'}>
        <UploadStudio
          key={diubah ? `ubah-${diubah.id}` : `baru-${studioKey}`}
          aktif={tampilan === 'unggah'}
          uid={uid}
          namaCreator={creatorProfile?.displayName}
          kategori={kategori}
          paket={paket}
          saranTagar={saranTagar}
          lokasiTerakhir={lokasiTerakhir}
          karya={diubah}
          onSubmit={simpanKarya}
          onSelesai={selesaiStudio}
          onBatal={() => { setDiubah(null); gantiTampilan('kelola'); }}
        />
      </div>

      {tampilan === 'kelola' && (
        <>
          <div className="creator-posts-toolbar card">
            <div>
              <div className="section-title" style={{ marginBottom: 3 }}>Kelola semua karya</div>
              <div className="text-meta">Pantau status review, performa, dan tindak lanjut setiap unggahan.</div>
            </div>
            <button className="btn btn-primary" type="button" onClick={bukaBaru}>+ Unggah Karya</button>
            <div className="creator-posts-search">
              <input className="input" value={query} onChange={(e) => setQuery(e.target.value)} placeholder="Cari judul, caption, kategori..." />
            </div>
            <div className="creator-posts-tabs" role="tablist" aria-label="Filter status unggahan">
              {TAB.map((item) => (
                <button key={item.id} type="button" role="tab" aria-selected={tab === item.id}
                  className={`creator-posts-tab${tab === item.id ? ' active' : ''}`} onClick={() => setTab(item.id)}>
                  {item.label}<span>{item.id === 'semua' ? posts.length : jumlahPerStatus[item.id] || 0}</span>
                </button>
              ))}
            </div>
          </div>

          {pesan && (
            <div className={`card ${pesan.tipe === 'sukses' ? 'banner-success' : 'banner-danger'} creator-message-wrap`}>
              <div className="msg">{pesan.teks}</div>
            </div>
          )}

          <div className="creator-grid-summary">
            <div className="grid grid-3 mb-lg creator-stats">
              <StatCard label="Karya Tayang" value={compactNumber(totalTayang)} delta={`${posts.length} total unggahan`} icon="image" tone="utama" />
              <StatCard label="Total Suka" value={compactNumber(totalSuka)} icon="star" tone="peringatan" />
              <StatCard label="Total Dilihat" value={compactNumber(totalDilihat)} icon="chart" tone="info" />
            </div>
            <div className="section-title creator-posts-title">Unggahan Saya</div>
            {loading ? (
              <div className="loading">Memuat...</div>
            ) : error ? (
              <ErrorState error={error} onRetry={() => window.location.reload()} />
            ) : daftar.length === 0 ? (
              <EmptyState title="Belum ada unggahan" hint="Karya yang dikirim akan muncul di sini beserta status review-nya." />
            ) : (
              <div className="grid grid-3 creator-post-list">
                {daftar.map((post) => (
                  <article className="card creator-post-card" key={post.id}>
                    {post.type === 'video' && post.mediaUrls?.[0] ? (
                      <video className="creator-post-thumb" src={post.mediaUrls[0]} poster={post.thumbnailUrl && post.thumbnailUrl !== post.mediaUrls[0] ? post.thumbnailUrl : undefined} muted preload="metadata" />
                    ) : post.thumbnailUrl || post.mediaUrls?.[0] ? (
                      <img src={post.thumbnailUrl || post.mediaUrls[0]} alt="" className="creator-post-thumb" />
                    ) : <div className="creator-post-thumb creator-post-thumb-empty">Belum ada media</div>}
                    <div className="creator-post-card-head">
                      <strong>{post.title || post.caption || 'Tanpa judul'}</strong>
                      <StatusBadge status={post.status} />
                    </div>
                    <div className="text-meta">{post.category || 'Tanpa kategori'} · {post.type === 'video' ? 'Video' : `${post.mediaUrls?.length || 0} foto`} · {formatDateTime(post.createdAt)}</div>
                    <p className="creator-post-caption">{post.caption || 'Tanpa caption'}</p>
                    <div className="creator-post-metrics"><span>♡ {post.metrics?.like || 0}</span><span>◉ {post.metrics?.view || 0}</span><span>▱ {post.metrics?.comment || 0}</span></div>
                    {post.moderationNote && (
                      <div className="creator-moderation-note">Catatan admin: {post.moderationNote}</div>
                    )}
                    <div className="creator-post-card-actions">
                      <button className="btn btn-outline btn-sm" type="button" onClick={() => bukaUbah(post)}>Ubah</button>
                      <button className="btn btn-danger btn-sm" type="button" onClick={() => hapus(post)}>Hapus</button>
                    </div>
                  </article>
                ))}
              </div>
            )}
          </div>
        </>
      )}
    </div>
  );
}
