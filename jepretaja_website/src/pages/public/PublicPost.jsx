import { useEffect } from 'react';
import { Link, useParams } from 'react-router-dom';
import { doc } from 'firebase/firestore';
import { db } from '../../firebase/config';
import { PATHS } from '../../firebase/paths';
import { useDocument } from '../../hooks/useDocument';
import Seo from '../../components/Seo';

export default function PublicPost() {
  const { id } = useParams();
  const { data: post, loading } = useDocument(doc(db, PATHS.explorePosts, id));
  if (loading) return <div className="loading">Memuat karya...</div>;
  if (!post || post.status !== 'published') return <div className="empty-state">Karya tidak ditemukan.</div>;

  const title = post.caption || post.title || `Karya ${post.creatorName || 'Creator JepretAja'}`;
  const description = `${title} - karya ${post.creatorName || 'creator'} di JepretAja${post.location ? `, ${post.location}` : ''}.`;
  const media = post.thumbnailUrl || post.mediaUrls?.[0];
  useEffect(() => {
    const data = {
      '@context': 'https://schema.org',
      '@type': post.type === 'video' ? 'VideoObject' : 'ImageObject',
      name: title,
      description,
      contentUrl: post.mediaUrls?.[0] || media,
      thumbnailUrl: post.thumbnailUrl || media,
      creator: { '@type': 'Person', name: post.creatorName || 'Creator JepretAja' },
      ...(post.type === 'video' ? { uploadDate: post.createdAt?.toDate?.()?.toISOString?.() || undefined } : {}),
    };
    const script = document.createElement('script');
    script.type = 'application/ld+json';
    script.dataset.jepretajaMediaSchema = 'true';
    script.textContent = JSON.stringify(data);
    document.head.appendChild(script);
    return () => script.remove();
  }, [description, media, post, title]);
  return (
    <>
      <Seo title={`${title} | JepretAja`} description={description.slice(0, 155)} path={`/karya/${id}`} image={media} type="article" />
      <main className="landing-container" style={{ padding: '48px 20px', maxWidth: 900 }}>
        <Link to={`/profil/${post.creatorId}`} className="text-meta">Profil {post.creatorName || 'creator'}</Link>
        <h1 className="page-title" style={{ marginTop: 28 }}>{title}</h1>
        {media && post.type === 'video' ? (
          <video controls preload="metadata" poster={post.thumbnailUrl || undefined} src={post.mediaUrls?.[0]} style={{ width: '100%', maxHeight: 640, borderRadius: 10, margin: '24px 0' }} />
        ) : media ? <img src={media} alt={title} style={{ width: '100%', maxHeight: 640, objectFit: 'contain', borderRadius: 10, margin: '24px 0' }} /> : null}
        <p className="landing-subtitle">{description}</p>
        {post.category && <p className="text-meta">Kategori: {post.category}</p>}
        {post.location && <p className="text-meta">Lokasi: {post.location}</p>}
      </main>
    </>
  );
}