# JepretAja — Android (Kotlin + Jetpack Compose)

Rewrite native dari versi Flutter dengan backend produksi di Vercel dan Firebase.
Project ini memakai Kotlin + Jetpack Compose, Firestore untuk data realtime,
endpoint Vercel `POST /api/app` untuk aksi server, dan Cloudinary untuk media.

## Status Android Saat Ini

**Sudah selesai:**
- Struktur project Gradle (Kotlin DSL) + Hilt DI + Jetpack Compose
- Tema (warna sama persis dengan versi Flutter: `#FF5A3C`)
- Semua model data (User, Creator, ExplorePost, Portfolio, Package, Booking
  dengan state machine 13-status, Chat, Wallet, Payment, Review, Notification)
- Semua repository (Auth, Creator, Explore, Booking, Payment, Availability,
  Chat, Wallet) — memakai Firestore untuk data dan `ApiClient` ke endpoint
  Vercel untuk aksi server yang memerlukan otorisasi.
- Google Sign-In dengan `ActivityResultLauncher` yang benar
- Layar autentikasi, Home, Explore, Search, Nearby, Creator Profile, Package
  Detail, Booking, Payment, Chat, Notifications, Profile, Favorites, Reviews,
  Help, dan seluruh Creator Dashboard.
- NavGraph merangkai alur customer dan creator, termasuk upload karya,
  pengelolaan paket/portfolio/booking, wallet, withdrawal, ketersediaan, dan
  pengaturan.
- Build debug tersedia untuk pengujian; build release harus memakai keystore
  pribadi dan konfigurasi produksi yang disimpan sebagai secret.

### Pekerjaan yang masih perlu diuji atau disempurnakan

- Uji end-to-end pada perangkat Android nyata untuk login Google, upload media,
  booking, pembayaran, chat, notifikasi, dan withdrawal.
- Konfigurasi production Firebase, Cloudinary, dan endpoint Vercel di secret
  atau `gradle.properties` sebelum distribusi luas.
- APK Release bertanda tangan keystore pribadi untuk pembaruan dan distribusi
  production. APK GitHub Release saat ini adalah debug build untuk pengujian.

## Setup

### Lokasi tanpa Google Maps berbayar

Aplikasi tidak memakai Google Maps SDK atau `GoogleMap`. Pemilihan lokasi dan
geocoding menggunakan Android `Geocoder` bawaan perangkat melalui
`GeocoderHelper`, sehingga tidak membutuhkan API key, kartu, billing, atau
secret tambahan. Hasil geocoding bergantung pada layanan lokasi yang tersedia
di perangkat dan koneksi internetnya.

### 1. Firebase — pakai project YANG SAMA dengan versi Flutter (kalau ada)

Kalau Anda sudah pernah `flutterfire configure` untuk versi Flutter,
**pakai project Firebase yang sama** — jangan buat baru, supaya data
Firestore/Storage yang sudah ada tetap kepakai.

Di [Firebase Console](https://console.firebase.google.com) → project Anda
→ **Project Settings** → **Your apps** → cek apakah sudah ada Android app
dengan package `com.jepretaja.app`:
- **Kalau sudah ada** (dari versi Flutter): klik ikon Android app itu →
  download `google-services.json` lagi (sama saja, tidak perlu app baru)
- **Kalau belum ada**: **Add app** → Android → package name
  `com.jepretaja.app` → download `google-services.json`

Letakkan file itu di **`app/google-services.json`** (bukan di root).

### 2. Buka di Android Studio

1. `File > Open` → pilih folder `jepretaja_android_kotlin`
2. Tunggu Gradle sync (pertama kali agak lama, download dependency)
3. Kalau muncul error `Unresolved reference: default_web_client_id` — itu
   tandanya `google-services.json` belum ada/belum di-sync, ulangi langkah 1

### 3. Jalankan

Pilih emulator/device Android di dropdown toolbar → klik ▶ (Shift+F10)

## Kenapa Google Sign-In butuh langkah ekstra

Selain `google-services.json`, Google Sign-In butuh **SHA-1 fingerprint**
project Anda didaftarkan ke Firebase:

```bash
# Windows PowerShell, dari folder project:
.\gradlew signingReport
```

Cari `SHA1` di output (untuk variant debug), copy, lalu di Firebase Console
→ Project Settings → Your apps → Android app → **Add fingerprint** → paste.
Tanpa ini, Google Sign-In akan gagal dengan error `DEVELOPER_ERROR`.

Untuk debug keystore pada mesin pengembangan ini, SHA-1 yang perlu didaftarkan
adalah `F3:B4:CF:E3:FB:29:F0:46:BC:94:EA:73:37:66:E0:BB:2E:71:02:51`.
Setelah menambahkan fingerprint, unduh ulang `google-services.json`, letakkan
kembali di `app/google-services.json`, aktifkan provider Google di Firebase
Authentication, lalu build dan pasang ulang APK debug.

## Backend produksi

Backend web berada di `jepretaja_website/`: endpoint `POST /api/app` berjalan
di Vercel, sementara Authentication, Firestore, App Check, dan FCM berjalan
di Firebase. Aturan Firestore harus dideploy dari project website.

## GitHub, Vercel, dan APK

Project ini adalah aplikasi Android, bukan project web. Vercel tidak
menjalankan file APK. Pola deploy yang benar:

1. Deploy backend web yang memiliki endpoint `POST /api/app` ke Vercel.
2. Upload repository Android ini ke GitHub.
3. Di GitHub → `Settings` → `Secrets and variables` → `Actions`, tambahkan:
  - `GOOGLE_SERVICES_JSON`: isi file `app/google-services.json` dalam Base64.
  - `API_BASE_URL`: domain Vercel, misalnya `https://api.jepretaja.vercel.app`.
  - `CLOUDINARY_CLOUD_NAME` dan `CLOUDINARY_UPLOAD_PRESET` bila fitur upload dipakai.
4. Push ke branch `main`. Workflow debug dapat membuat APK untuk pengujian.
  Untuk Play Store, workflow release harus memakai keystore dan menghasilkan
  AAB signed, bukan debug APK.

APK debug dapat dipasang langsung pada perangkat Android setelah mengizinkan
instalasi dari sumber tidak dikenal. Untuk Google Play, gunakan APK/AAB rilis
dengan keystore pribadi; jangan memasukkan keystore atau `google-services.json`
ke repository publik.
