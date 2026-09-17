import { Link, useParams } from 'react-router-dom';
import Seo from '../../components/Seo';

const ARTICLES = [
  {
    slug: 'cara-memilih-fotografer-untuk-acara',
    title: 'Cara Memilih Fotografer untuk Acara Penting',
    summary: 'Panduan praktis membandingkan gaya, pengalaman, paket, lokasi, dan komunikasi sebelum memilih fotografer.',
    sections: [
      ['Mulai dari kebutuhan acara', 'Tentukan jenis acara, jumlah tamu, durasi, dan hasil yang paling penting. Fotografer untuk pernikahan membutuhkan pendekatan berbeda dari fotografer produk atau acara kantor. Catatan sederhana ini membantu Anda menjelaskan kebutuhan dengan jelas.'],
      ['Lihat gaya dan karya sebelumnya', 'Perhatikan konsistensi warna, cara menangkap momen, komposisi, serta kualitas foto di berbagai kondisi cahaya. Jangan hanya memilih berdasarkan satu foto terbaik; lihat beberapa karya agar Anda mendapat gambaran yang lebih jujur.'],
      ['Bandingkan isi paket', 'Harga bukan satu-satunya pertimbangan. Periksa durasi pemotretan, jumlah creator, jumlah foto yang diedit, format hasil, estimasi pengiriman, dan biaya tambahan perjalanan. Tanyakan hal yang belum dijelaskan sebelum melakukan booking.'],
      ['Pastikan komunikasi berjalan baik', 'Creator yang responsif akan lebih mudah diajak menyusun jadwal dan memahami preferensi Anda. Simpan kesepakatan penting di dalam chat dan gunakan alur booking resmi agar riwayatnya mudah ditelusuri.'],
    ],
  },
  {
    slug: 'persiapan-sesi-foto',
    title: 'Persiapan Sesi Foto agar Hasil Lebih Maksimal',
    summary: 'Checklist sederhana untuk lokasi, pakaian, waktu, properti, dan komunikasi sebelum sesi foto dimulai.',
    sections: [
      ['Susun moodboard singkat', 'Kumpulkan beberapa referensi yang menunjukkan suasana, warna, atau pose yang Anda sukai. Moodboard tidak harus panjang; lima sampai sepuluh referensi sudah cukup untuk menyamakan ekspektasi dengan creator.'],
      ['Pilih waktu dan lokasi', 'Cahaya, cuaca, akses kendaraan, dan kepadatan lokasi dapat memengaruhi hasil. Diskusikan rencana cadangan jika sesi dilakukan di luar ruangan dan pastikan waktu perjalanan sudah diperhitungkan.'],
      ['Siapkan pakaian dan properti', 'Bawa pilihan pakaian yang nyaman dan sesuai konsep. Hindari pola yang terlalu ramai bila ingin perhatian tetap berada pada wajah atau produk. Properti kecil yang bermakna juga dapat membuat foto terasa lebih personal.'],
      ['Konfirmasi detail sehari sebelumnya', 'Pastikan alamat, jam bertemu, kontak creator, durasi, dan kebutuhan khusus sudah disepakati. Simpan bukti booking dan jangan mengirim PIN, OTP, atau password melalui chat.'],
    ],
  },
  {
    slug: 'keamanan-booking-online',
    title: 'Tips Aman Melakukan Booking Fotografi Online',
    summary: 'Langkah untuk memeriksa creator, memahami pembayaran, dan menjaga informasi pribadi selama transaksi.',
    sections: [
      ['Periksa identitas dan portofolio', 'Gunakan profil creator, ulasan, kategori layanan, lokasi, dan karya yang tersedia sebagai bahan pertimbangan. Bila informasi penting belum jelas, tanyakan melalui chat sebelum memesan.'],
      ['Gunakan jalur pembayaran resmi', 'Bayar hanya ke rekening dan nominal yang ditampilkan oleh sistem. Status pembayaran dianggap diterima setelah diverifikasi, bukan hanya karena bukti transfer sudah dikirim.'],
      ['Jaga informasi sensitif', 'JepretAja tidak memerlukan PIN, password, atau OTP perbankan melalui chat. Jangan membagikan kredensial tersebut kepada siapa pun, termasuk pihak yang mengaku sebagai admin.'],
      ['Simpan bukti komunikasi', 'Gunakan fitur booking dan chat di aplikasi agar kesepakatan, jadwal, dan perubahan layanan memiliki riwayat yang bisa ditinjau bila terjadi kendala.'],
    ],
  },
  {
    slug: 'panduan-creator-membangun-portofolio',
    title: 'Panduan Creator Membangun Portofolio yang Dipercaya',
    summary: 'Cara menyusun profil, memilih karya, menulis paket, dan menjaga komunikasi agar lebih mudah ditemukan pelanggan.',
    sections: [
      ['Lengkapi profil dengan spesifik', 'Tuliskan kota layanan, jenis fotografi, gaya visual, pengalaman, dan peralatan yang relevan. Informasi spesifik membantu pelanggan memahami apakah layanan Anda cocok untuk kebutuhannya.'],
      ['Pilih karya yang mewakili layanan', 'Unggah karya yang benar-benar ingin Anda kerjakan kembali. Gunakan caption yang menjelaskan konteks tanpa klaim berlebihan, dan pastikan Anda memiliki hak untuk menampilkan setiap foto atau video.'],
      ['Buat paket yang mudah dibandingkan', 'Jelaskan durasi, jumlah personel, hasil yang diterima, estimasi waktu pengiriman, dan biaya tambahan. Paket yang jelas mengurangi pertanyaan berulang dan membantu pelanggan mengambil keputusan.'],
      ['Jaga konsistensi setelah booking', 'Respons yang tepat waktu, konfirmasi jadwal, dan pembaruan status membuat pengalaman pelanggan lebih baik. Ulasan yang baik biasanya dibangun dari proses yang rapi, bukan hanya dari hasil akhir.'],
    ],
  },
];

function ArticleSchema({ article }) {
  return <script type="application/ld+json" dangerouslySetInnerHTML={{ __html: JSON.stringify({
    '@context': 'https://schema.org', '@type': 'Article', headline: article.title,
    description: article.summary, author: { '@type': 'Organization', name: 'JepretAja' },
    publisher: { '@type': 'Organization', name: 'JepretAja' },
  }) }} />;
}

export default function Articles() {
  const { slug } = useParams();
  const article = ARTICLES.find((item) => item.slug === slug);
  if (!article) {
    return (
      <>
        <Seo title="Artikel JepretAja | Panduan Fotografi dan Booking" description="Panduan JepretAja tentang fotografi, booking creator, keamanan transaksi, dan membangun portofolio." path="/artikel" />
        <main className="landing-container" style={{ padding: '56px 20px', maxWidth: 960 }}>
          <Link to="/" className="text-meta">Kembali ke beranda</Link>
          <h1 className="page-title" style={{ marginTop: 28 }}>Artikel JepretAja</h1>
          <p className="landing-subtitle">Panduan praktis untuk pelanggan dan creator dalam merencanakan sesi foto, memilih layanan, dan menggunakan JepretAja dengan aman.</p>
          <div className="grid grid-2" style={{ marginTop: 32 }}>
            {ARTICLES.map((item) => <Link className="card" key={item.slug} to={`/artikel/${item.slug}`}><h2>{item.title}</h2><p>{item.summary}</p><span className="text-meta">Baca panduan</span></Link>)}
          </div>
        </main>
      </>
    );
  }

  return (
    <>
      <Seo title={`${article.title} | JepretAja`} description={article.summary} path={`/artikel/${article.slug}`} />
      <ArticleSchema article={article} />
      <main className="landing-container" style={{ padding: '56px 20px', maxWidth: 860 }}>
        <Link to="/artikel" className="text-meta">Semua artikel</Link>
        <h1 className="page-title" style={{ marginTop: 28 }}>{article.title}</h1>
        <p className="landing-subtitle">{article.summary}</p>
        <article className="card" style={{ marginTop: 32 }}>
          {article.sections.map(([heading, text]) => <section key={heading}><h2>{heading}</h2><p>{text}</p></section>)}
        </article>
        <p className="text-meta" style={{ marginTop: 24 }}>Gunakan <Link to="/kontak">Kontak JepretAja</Link> bila Anda membutuhkan bantuan lebih lanjut.</p>
      </main>
    </>
  );
}