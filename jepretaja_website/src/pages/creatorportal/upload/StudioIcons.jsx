/**
 * Ikon garis kecil untuk Studio Unggah. Sengaja dibuat lokal (bukan menambah
 * nama ke components/Icon.jsx) karena ikon-ikon ini — kamera, putar, tagar,
 * pin — hanya dipakai di layar ini.
 */
const dasar = {
  width: 20, height: 20, viewBox: '0 0 24 24', fill: 'none',
  stroke: 'currentColor', strokeWidth: 1.9, strokeLinecap: 'round', strokeLinejoin: 'round',
  'aria-hidden': true, focusable: false,
};

const Svg = ({ ukuran, children, ...sisa }) => (
  <svg {...dasar} {...(ukuran ? { width: ukuran, height: ukuran } : {})} {...sisa}>{children}</svg>
);

export const IkonUnggah = (p) => <Svg {...p}><path d="M12 16V4" /><path d="m7 9 5-5 5 5" /><path d="M5 20h14" /></Svg>;
export const IkonKamera = (p) => <Svg {...p}><path d="M4 8a2 2 0 0 1 2-2h1.5l1.2-1.6A1 1 0 0 1 9.5 4h5a1 1 0 0 1 .8.4L16.5 6H18a2 2 0 0 1 2 2v9a2 2 0 0 1-2 2H6a2 2 0 0 1-2-2Z" /><circle cx="12" cy="12.5" r="3.3" /></Svg>;
export const IkonVideo = (p) => <Svg {...p}><rect x="3" y="6" width="13" height="12" rx="2.5" /><path d="m16 10.5 5-3v9l-5-3Z" /></Svg>;
export const IkonFoto = (p) => <Svg {...p}><rect x="3.5" y="4.5" width="17" height="15" rx="2.5" /><circle cx="9" cy="10" r="1.6" /><path d="m4 17 5-4.5 4 3.5 3-2.5 4 3.5" /></Svg>;
export const IkonPutar = (p) => <Svg {...p} fill="currentColor" stroke="none"><path d="M8 5.5v13a1 1 0 0 0 1.5.86l10.5-6.5a1 1 0 0 0 0-1.72L9.5 4.64A1 1 0 0 0 8 5.5Z" /></Svg>;
export const IkonJeda = (p) => <Svg {...p} fill="currentColor" stroke="none"><rect x="6.5" y="5" width="4" height="14" rx="1.2" /><rect x="13.5" y="5" width="4" height="14" rx="1.2" /></Svg>;
export const IkonTutup = (p) => <Svg {...p}><path d="M6 6l12 12M18 6 6 18" /></Svg>;
export const IkonKiri = (p) => <Svg {...p}><path d="m14.5 6-6 6 6 6" /></Svg>;
export const IkonKanan = (p) => <Svg {...p}><path d="m9.5 6 6 6-6 6" /></Svg>;
export const IkonCentang = (p) => <Svg {...p} strokeWidth={2.4}><path d="m5 12.5 4.5 4.5L19 7.5" /></Svg>;
export const IkonTagar = (p) => <Svg {...p}><path d="M9 4 7 20M17 4l-2 16M4.5 9h15M4 15h15" /></Svg>;
export const IkonPin = (p) => <Svg {...p}><path d="M12 21s-6.5-5.6-6.5-11a6.5 6.5 0 0 1 13 0c0 5.4-6.5 11-6.5 11Z" /><circle cx="12" cy="10" r="2.3" /></Svg>;
export const IkonPaket = (p) => <Svg {...p}><path d="M3.5 8 12 3.5 20.5 8v8L12 20.5 3.5 16Z" /><path d="m3.5 8 8.5 4.5L20.5 8M12 12.5v8" /></Svg>;
export const IkonKomentar = (p) => <Svg {...p}><path d="M20 12a8 8 0 0 1-11.6 7.1L4 20l1-3.9A8 8 0 1 1 20 12Z" /></Svg>;
export const IkonSimpan = (p) => <Svg {...p}><path d="M7 4h10a1 1 0 0 1 1 1v15l-6-4-6 4V5a1 1 0 0 1 1-1Z" /></Svg>;
export const IkonSuka = (p) => <Svg {...p}><path d="M12 20s-7.5-4.6-7.5-10.3A4.2 4.2 0 0 1 12 7.4a4.2 4.2 0 0 1 7.5 2.3C19.5 15.4 12 20 12 20Z" /></Svg>;
export const IkonBagikan = (p) => <Svg {...p}><path d="M13 5v4C7.5 9.3 4.5 12.6 4 19c1.8-3.4 4.8-5 9-5v4l7-6.5Z" /></Svg>;
export const IkonMata = (p) => <Svg {...p}><path d="M2.5 12S6 5.5 12 5.5 21.5 12 21.5 12 18 18.5 12 18.5 2.5 12 2.5 12Z" /><circle cx="12" cy="12" r="2.8" /></Svg>;
export const IkonUlang = (p) => <Svg {...p}><path d="M4 12a8 8 0 0 1 13.7-5.6L20 8.5" /><path d="M20 4v4.5h-4.5" /><path d="M20 12a8 8 0 0 1-13.7 5.6L4 15.5" /><path d="M4 20v-4.5h4.5" /></Svg>;
export const IkonPeringatan = (p) => <Svg {...p}><path d="M12 4 2.8 19.5h18.4Z" /><path d="M12 10v4.5M12 17.4v.1" /></Svg>;
export const IkonAwan = (p) => <Svg {...p}><path d="M7 18a4.5 4.5 0 0 1-.6-8.96A6 6 0 0 1 18 8.6 4.7 4.7 0 0 1 17.5 18Z" /></Svg>;
export const IkonSampul = (p) => <Svg {...p}><rect x="4" y="3.5" width="16" height="17" rx="2.5" /><path d="M8 8h8M8 12h5" /></Svg>;
export const IkonSampah = (p) => <Svg {...p}><path d="M4.5 7h15M9.5 7V4.8h5V7M6.5 7l.8 12h9.4l.8-12" /></Svg>;
export const IkonTambah = (p) => <Svg {...p}><path d="M12 5v14M5 12h14" /></Svg>;
