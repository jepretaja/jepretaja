import { useMemo } from 'react';
import { Link, useParams } from 'react-router-dom';
import { collection, doc, where } from 'firebase/firestore';
import { db } from '../../firebase/config';
import { PATHS } from '../../firebase/paths';
import { useDocument } from '../../hooks/useDocument';
import { useCollection } from '../../hooks/useCollection';
import Seo, { SITE_URL } from '../../components/Seo';

export default function PublicCreatorProfile() {
  const { id } = useParams();
  const { data: creator, loading } = useDocument(doc(db, PATHS.creators, id));
  const { data: posts } = useCollection(collection(db, PATHS.explorePosts), [where('creatorId', '==', id)]);
  const { data: portfolios } = useCollection(collection(db, PATHS.portfolios), [where('creatorId', '==', id)]);
  const publishedPosts = useMemo(() => posts.filter((post) => post.status === 'published'), [posts]);
  const visiblePortfolios = useMemo(() => portfolios.filter((item) => item.status !== 'draft' && item.status !== 'rejected'), [portfolios]);

  if (loading) return <div className="loading">Memuat profil creator...</div>;
  if (!creator || creator.status === 'suspended' || creator.status === 'deleted') return <div className="empty-state">Profil creator tidak ditemukan.</div>;

  const name = creator.displayName || 'Creator JepretAja';
  const description = creator.bio || `${name} di ${creator.city || 'Indonesia'} - profil creator, portofolio, dan karya JepretAja.`;

  return (
    <>
      <Seo title={`${name} | Creator JepretAja`} description={description.slice(0, 155)} path={`/profil/${id}`} image={creator.coverUrl || creator.photoUrl} />
      <main className="landing-container" style={{ padding: '48px 20px' }}>
        <Link to="/" className="text-meta">Kembali ke JepretAja</Link>
        <header style={{ margin: '28px 0 36px', maxWidth: 760 }}>
          {creator.photoUrl && <img src={creator.photoUrl} alt={name} style={{ width: 96, height: 96, objectFit: 'cover', borderRadius: '50%', marginBottom: 18 }} />}
          <h1 className="page-title">{name}</h1>
          <p className="landing-subtitle">{description}</p>
          {creator.city && <p className="text-meta">{creator.city}{creator.categories?.length ? ` · ${creator.categories.join(', ')}` : ''}</p>}
        </header>
        <section>
          <h2 className="section-title">Karya {name}</h2>
          <div className="grid grid-3">
            {publishedPosts.map((post) => (
              <Link className="card" key={post.id} to={`/karya/${post.id}`}>
                {post.thumbnailUrl && <img src={post.thumbnailUrl} alt={post.caption || `Karya ${name}`} style={{ width: '100%', aspectRatio: '4 / 3', objectFit: 'cover', borderRadius: 8 }} />}
                <h3>{post.caption || post.title || 'Karya creator'}</h3>
                <p className="text-meta">{post.category || 'Fotografi'}</p>
              </Link>
            ))}
          </div>
          {!publishedPosts.length && !visiblePortfolios.length && <p className="text-meta">Belum ada karya publik.</p>}
        </section>
        {visiblePortfolios.length > 0 && (
          <section style={{ marginTop: 40 }}>
            <h2 className="section-title">Portofolio</h2>
            <div className="grid grid-3">
              {visiblePortfolios.map((item) => <article className="card" key={item.id}><h3>{item.title || item.name || 'Portofolio'}</h3><p>{item.description || item.caption || ''}</p></article>)}
            </div>
          </section>
        )}
        <a className="btn btn-primary" href={`https://jepretaja-website.vercel.app/downloads/jepretaja.apk`} style={{ marginTop: 36 }}>Unduh aplikasi JepretAja</a>
      </main>
    </>
  );
}