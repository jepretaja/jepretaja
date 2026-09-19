import { Link } from 'react-router-dom';
import Seo from '../../components/Seo';

export default function Contact() {
  return (
    <>
      <Seo
        title="Kontak JepretAja | Bantuan dan Dukungan"
        description="Hubungi tim JepretAja untuk bantuan akun, booking, pembayaran, konten, dan pertanyaan terkait layanan."
        path="/kontak"
      />
      <main className="landing-container" style={{ padding: '56px 20px', maxWidth: 860 }}>
        <Link to="/" className="text-meta">Kembali ke beranda</Link>
        <h1 className="page-title" style={{ marginTop: 28 }}>Kontak JepretAja</h1>
        <p className="landing-subtitle">Kami membantu pelanggan dan creator memahami layanan, menyelesaikan kendala booking, serta melaporkan masalah keamanan atau konten.</p>
        <section className="grid grid-2" style={{ marginTop: 28 }}>
          <article className="card"><h2>Dukungan umum</h2><p>Untuk bantuan akun, booking, chat, dan penggunaan aplikasi.</p><a href="mailto:support.soxvo@gmail.com">support.soxvo@gmail.com</a></article>
          <article className="card"><h2>Jam layanan</h2><p>Senin-Jumat<br />09.00-18.00 WIB</p><p className="text-meta">Mohon sertakan email akun dan nomor booking bila pertanyaan berkaitan dengan transaksi.</p></article>
        </section>
        <section className="card" style={{ marginTop: 20 }}><h2>Pelaporan konten</h2><p>Gunakan tombol laporan di aplikasi untuk konten yang melanggar aturan. Untuk laporan hukum atau keamanan mendesak, kirimkan detail ke alamat dukungan dengan subjek yang jelas.</p></section>
        <p className="text-meta" style={{ marginTop: 24 }}>Informasi data pribadi tersedia di <Link to="/privasi">Kebijakan Privasi</Link>.</p>
      </main>
    </>
  );
}