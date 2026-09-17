import { useEffect } from 'react';

/**
 * AdSense hanya dimuat pada area publik dan hanya setelah Publisher ID resmi
 * tersedia. Admin panel tidak memuat iklan, dan ID kosong tidak menambahkan
 * script AdSense yang akan gagal atau menghasilkan iklan tidak valid.
 */
export default function AdSenseScript() {
  const clientId = import.meta.env.VITE_ADSENSE_CLIENT_ID;

  useEffect(() => {
    if (!clientId || document.querySelector('script[data-jepretaja-adsense]')) return undefined;
    const script = document.createElement('script');
    script.async = true;
    script.crossOrigin = 'anonymous';
    script.dataset.jepretajaAdsense = 'true';
    script.src = `https://pagead2.googlesyndication.com/pagead/js/adsbygoogle.js?client=${clientId}`;
    document.head.appendChild(script);
    return () => script.remove();
  }, [clientId]);

  return null;
}