import { Link } from 'react-router-dom';
import Seo from '../../components/Seo';

export default function About() {
  return (
    <>
      <Seo
        title="Tentang JepretAja | Platform Creator dan Fotografi"
        description="Kenali JepretAja, platform Indonesia untuk menemukan creator fotografi, melihat karya, dan melakukan booking dengan lebih mudah."
        path="/tentang"
      />
      <main className="landing-container" style={{ padding: '56px 20px', maxWidth: 860 }}>
        <Link to="/" className="text-meta">Kembali ke beranda</Link>
        <h1 className="page-title" style={{ marginTop: 28 }}>Tentang JepretAja</h1>
        <p className="landing-subtitle">JepretAja mempertemukan pelanggan dengan fotografer dan videografer lokal untuk membantu setiap orang menemukan creator yang sesuai dengan momen mereka.</p>
        <section className="card" style={{ marginTop: 28 }}>
          <h2>Misi kami</h2>
          <p>Kami ingin membuat proses menemukan creator, membandingkan paket, melakukan booking, dan mengelola karya menjadi lebih jelas dan dapat dipercaya.</p>
          <h2>Untuk pelanggan</h2>
          <p>Pelanggan dapat menjelajahi karya, melihat profil creator, memilih paket, mengajukan booking, dan berkomunikasi melalui satu aplikasi.</p>
          <h2>Untuk creator</h2>
          <p>Creator dapat membangun portofolio, menampilkan paket jasa, menerima permintaan booking, mengelola ketersediaan, dan memantau pendapatan.</p>
          <h2>Keamanan dan moderasi</h2>
          <p>Karya yang tampil di Explore melewati proses moderasi. Kami menyediakan pelaporan, pengelolaan privasi, dan mekanisme sengketa untuk membantu menjaga pengalaman pengguna.</p>
        </section>
        <p className="text-meta" style={{ marginTop: 24 }}>Baca juga <Link to="/privasi">Kebijakan Privasi</Link> dan <Link to="/syarat">Syarat Layanan</Link>.</p>
      </main>
    </>
  );
}