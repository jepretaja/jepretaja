import { useCallback, useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { doc, getDoc } from 'firebase/firestore';
import { db } from '../../firebase/config';
import { PATHS } from '../../firebase/paths';
import { formatDate } from '../../utils/format';

const DEFAULT_NOTICES = [
  {
    type: 'pemeliharaan',
    title: 'Pemeliharaan server terjadwal',
    body: 'Pemeliharaan rutin basis data dilakukan setiap Minggu pukul 01.00-03.00 WIB. Panel tetap dapat diakses, namun proses penarikan dana ditunda sementara.',
  },
  {
    type: 'rilis',
    title: 'Verifikasi transfer manual kini tersedia',
    body: 'Pembayaran melalui transfer bank dapat dicocokkan langsung dari menu Verifikasi Transfer. Cocokkan nominal pada kolom Transfer yang sudah termasuk kode unik.',
  },
  {
    type: 'internal',
    title: 'Pengingat keamanan akun',
    body: 'Jangan membagikan kredensial admin kepada siapa pun. Setiap tindakan yang Anda lakukan tercatat pada Audit Logs beserta identitas akun Anda.',
  },
];

const DEFAULT_CONTACT = {
  support: 'support.soxvo@gmail.com',
  emergency: '+62 812-0000-0000',
  hours: 'Senin-Jumat, 09.00-18.00 WIB',
};

const LABELS = {
  pemeliharaan: ['Pemeliharaan', 'warning'],
  rilis: ['Rilis Baru', 'success'],
  internal: ['Internal', 'info'],
  penting: ['Penting', 'danger'],
};

const LIMIT_STABLE = 1500;
const LIMIT_SLOW = 4000;
const LIMIT_TIMEOUT = 8000;

export default function SystemStatus() {
  const [notices, setNotices] = useState(DEFAULT_NOTICES);
  const [contact, setContact] = useState(DEFAULT_CONTACT);
  const [status, setStatus] = useState({ state: 'checking', latency: null, checkedAt: null });

  const checkStatus = useCallback(async () => {
    setStatus((current) => ({ ...current, state: 'checking' }));
    const started = performance.now();
    try {
      await Promise.race([
        getDoc(doc(db, PATHS.settings, 'general')),
        new Promise((_, reject) => setTimeout(() => reject(new Error('timeout')), LIMIT_TIMEOUT)),
      ]);
      const latency = Math.round(performance.now() - started);
      setStatus({
        state: latency <= LIMIT_STABLE ? 'stable' : latency <= LIMIT_SLOW ? 'slow' : 'slow',
        latency,
        checkedAt: new Date(),
      });
    } catch {
      setStatus({ state: 'outage', latency: null, checkedAt: new Date() });
    }
  }, []);

  useEffect(() => { checkStatus(); }, [checkStatus]);

  useEffect(() => {
    getDoc(doc(db, PATHS.settings, 'announcements')).then((snapshot) => {
      if (!snapshot.exists()) return;
      const data = snapshot.data();
      if (Array.isArray(data.items) && data.items.length) setNotices(data.items);
      if (data.kontak) {
        setContact((current) => ({
          support: data.kontak.timSupport || current.support,
          emergency: data.kontak.daruratIT || current.emergency,
          hours: data.kontak.jamOperasional || current.hours,
        }));
      }
    }).catch(() => {});
  }, []);

  const system = {
    checking: ['Memeriksa...', 'neutral'],
    stable: ['Stabil', 'success'],
    slow: ['Lambat', 'warning'],
    outage: ['Gangguan', 'danger'],
  }[status.state];

  return (
    <div className="landing status-page">
      <header className="landing-header">
        <div className="landing-container landing-header-inner">
          <Link to="/" className="landing-brand">
            <span className="landing-mark">JA</span>
            <span>
              <span className="landing-brand-name">JepretAja</span>
              <span className="landing-brand-sub">System Status</span>
            </span>
          </Link>
          <nav className="landing-nav">
            <Link to="/">Beranda</Link>
            <Link to="/panduan">Panduan</Link>
            <Link className="btn btn-primary" to="/login">Masuk ke Dashboard</Link>
          </nav>
        </div>
      </header>

      <main className="landing-container status-main">
        <section className="status-hero">
          <div>
            <span className="landing-pill landing-pill-success">Operational intelligence</span>
            <h1 className="landing-title">Status sistem yang jelas, saat Anda membutuhkannya.</h1>
            <p className="landing-subtitle">Pantau kesehatan portal, baca pengumuman penting, dan temukan jalur bantuan tanpa harus masuk ke dashboard.</p>
          </div>
          <div className="status-orb" aria-hidden="true"><span>JA</span></div>
        </section>

        <div className="status-grid">
          <section className="landing-panel status-panel status-notices">
            <div className="landing-panel-head">
              <div>
                <span className="status-eyebrow">Updates</span>
                <h2 className="landing-panel-title">Papan Pengumuman</h2>
              </div>
              <span className="text-meta">{notices.length} informasi</span>
            </div>
            <ul className="notice-list">
              {notices.map((notice, index) => {
                const [label, tone] = LABELS[notice.tipe || notice.type] || LABELS.internal;
                return (
                  <li className="notice-item" key={`${notice.judul || notice.title}-${index}`}>
                    <div className="notice-head">
                      <span className={`badge badge-${tone}`}>{label}</span>
                      {(notice.tanggal || notice.date) && <span className="text-meta">{formatDate(notice.tanggal || notice.date)}</span>}
                    </div>
                    <h3 className="notice-title">{notice.judul || notice.title}</h3>
                    <p className="notice-body">{notice.isi || notice.body}</p>
                  </li>
                );
              })}
            </ul>
          </section>

          <div className="status-column">
            <section className="landing-panel status-panel">
              <div className="landing-panel-head">
                <div>
                  <span className="status-eyebrow">Live telemetry</span>
                  <h2 className="landing-panel-title">Status Sistem</h2>
                </div>
                <button className="btn btn-outline btn-sm" onClick={checkStatus}>Periksa Ulang</button>
              </div>
              <div className="status-row"><span className="status-name"><span className="landing-dot landing-dot-success" /> Portal Web</span><span className="badge badge-success">Online</span></div>
              <div className="status-row"><span className="status-name"><span className={`landing-dot landing-dot-${system[1]}`} /> Database</span><span className={`badge badge-${system[1]}`}>{system[0]}</span></div>
              {status.latency !== null && <div className="status-row status-row-sub"><span className="text-meta">Waktu respons database</span><span className="text-meta num">{status.latency} ms</span></div>}
              {status.checkedAt && <p className="status-time">Terakhir diperiksa {status.checkedAt.toLocaleTimeString('id-ID', { hour: '2-digit', minute: '2-digit', second: '2-digit' })} WIB</p>}
              {status.state === 'outage' && <p className="status-warning">Database tidak merespons. Hubungi tim IT sebelum melakukan tindakan finansial.</p>}
            </section>

            <section className="landing-panel status-panel" id="bantuan">
              <span className="status-eyebrow">Human support</span>
              <h2 className="landing-panel-title">Bantuan IT</h2>
              <div className="detail-row"><span className="k">Tim Support</span><span className="v"><a href={`mailto:${contact.support}`}>{contact.support}</a></span></div>
              <div className="detail-row"><span className="k">Darurat IT</span><span className="v">{contact.emergency}</span></div>
              <div className="detail-row"><span className="k">Jam Operasional</span><span className="v">{contact.hours}</span></div>
            </section>
          </div>
        </div>
      </main>

      <footer className="landing-footer"><div className="landing-container landing-footer-inner"><span className="landing-footer-brand">JepretAja System Status</span><Link to="/">Kembali ke Beranda</Link></div></footer>
    </div>
  );
}
