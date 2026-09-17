import { adminDb } from './_lib/firebaseAdmin.js';

const SITE_URL = 'https://jepretaja-website.vercel.app';

function escapeXml(value) {
  return String(value).replace(/[<>&'\"]/g, (character) => ({ '<': '&lt;', '>': '&gt;', '&': '&amp;', "'": '&apos;', '"': '&quot;' }[character]));
}

function lastModified(snapshot) {
  const timestamp = snapshot.get('updatedAt') || snapshot.get('createdAt');
  return timestamp?.toDate?.().toISOString().slice(0, 10) || null;
}

export default async function handler(req, res) {
  if (req.method !== 'GET') return res.status(405).end();
  try {
    const database = adminDb();
    const [creators, posts] = await Promise.all([
      database.collection('creators').where('status', '==', 'active').get(),
      database.collection('explore_posts').where('status', '==', 'published').get(),
    ]);
    const urls = [
      ...['/', '/tentang', '/artikel', '/kontak', '/panduan', '/status', '/privasi', '/syarat', '/hapus-akun'].map((url) => ({ url })),
      ...creators.docs.map((item) => ({ url: `/profil/${encodeURIComponent(item.id)}`, lastmod: lastModified(item) })),
      ...posts.docs.map((item) => ({
        url: `/karya/${encodeURIComponent(item.id)}`,
        lastmod: lastModified(item),
        image: item.get('type') !== 'video' ? item.get('thumbnailUrl') || item.get('mediaUrls')?.[0] : item.get('thumbnailUrl'),
        video: item.get('type') === 'video' ? item.get('mediaUrls')?.[0] : null,
        title: item.get('caption') || item.get('title') || `Karya ${item.get('creatorName') || 'creator'}`,
      })),
    ];
    const body = `<?xml version="1.0" encoding="UTF-8"?><urlset xmlns="http://www.sitemaps.org/schemas/sitemap/0.9" xmlns:image="http://www.google.com/schemas/sitemap-image/1.1" xmlns:video="http://www.google.com/schemas/sitemap-video/1.1">${urls.map(({ url, lastmod, image, video, title }) => `<url><loc>${escapeXml(`${SITE_URL}${url}`)}</loc>${lastmod ? `<lastmod>${lastmod}</lastmod>` : ''}${image ? `<image:image><image:loc>${escapeXml(image)}</image:loc></image:image>` : ''}${video ? `<video:video><video:thumbnail_loc>${escapeXml(image || video)}</video:thumbnail_loc><video:title>${escapeXml(title)}</video:title><video:content_loc>${escapeXml(video)}</video:content_loc></video:video>` : ''}</url>`).join('')}</urlset>`;
    res.setHeader('Content-Type', 'application/xml; charset=utf-8');
    res.setHeader('Cache-Control', 'public, s-maxage=60, stale-while-revalidate=300');
    return res.status(200).send(body);
  } catch (error) {
    console.error('[sitemap] failed:', error);
    const fallback = `<?xml version="1.0" encoding="UTF-8"?><urlset xmlns="http://www.sitemaps.org/schemas/sitemap/0.9"><url><loc>${SITE_URL}/</loc></url><url><loc>${SITE_URL}/tentang</loc></url><url><loc>${SITE_URL}/artikel</loc></url><url><loc>${SITE_URL}/kontak</loc></url><url><loc>${SITE_URL}/panduan</loc></url><url><loc>${SITE_URL}/status</loc></url><url><loc>${SITE_URL}/privasi</loc></url><url><loc>${SITE_URL}/syarat</loc></url></urlset>`;
    res.setHeader('Content-Type', 'application/xml; charset=utf-8');
    res.setHeader('Cache-Control', 'public, s-maxage=300');
    return res.status(200).send(fallback);
  }
}