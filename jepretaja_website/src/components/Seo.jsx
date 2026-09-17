import { useEffect } from 'react';

const SITE_URL = 'https://jepretaja-website.vercel.app';

function setMeta(attribute, value, content) {
  let element = document.head.querySelector(`meta[${attribute}="${value}"]`);
  if (!element) {
    element = document.createElement('meta');
    element.setAttribute(attribute, value);
    document.head.appendChild(element);
  }
  element.setAttribute('content', content);
}

export default function Seo({ title, description, path, image, type = 'website' }) {
  useEffect(() => {
    const canonical = new URL(path || window.location.pathname, SITE_URL).toString();
    document.title = title;
    setMeta('name', 'description', description);
    setMeta('name', 'robots', 'index, follow, max-image-preview:large');
    setMeta('property', 'og:type', type);
    setMeta('property', 'og:title', title);
    setMeta('property', 'og:description', description);
    setMeta('property', 'og:url', canonical);
    if (image) setMeta('property', 'og:image', image);

    let link = document.head.querySelector('link[rel="canonical"]');
    if (!link) {
      link = document.createElement('link');
      link.rel = 'canonical';
      document.head.appendChild(link);
    }
    link.href = canonical;
    return () => {
      document.title = 'JepretAja | Booking Fotografer dan Creator';
    };
  }, [description, image, path, title, type]);

  return null;
}

export { SITE_URL };