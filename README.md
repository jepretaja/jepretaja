# JepretAja

JepretAja adalah platform yang terdiri dari dua bagian utama:

- Android app di [jepretaja_apk](jepretaja_apk)
- Web admin + portal creator di [jepretaja_website](jepretaja_website)

## Struktur proyek

- `jepretaja_apk` — aplikasi Android Kotlin/Jetpack Compose
- `jepretaja_website` — frontend admin/portal React + Vite + Firebase backend API

## Deployment aktif

- GitHub: https://github.com/jepretaja/jepretaja
- Vercel production: https://jepretaja-website.vercel.app

## Verifikasi yang sudah dijalankan

- Website tests: 5 passed, 0 failed
- Website build: Vite production build berhasil
- Android compile: Gradle compileDebugKotlin berhasil

## Menjalankan website lokal

```bash
cd jepretaja_website
npm install
npm run dev
```

## Menjalankan Android lokal

```bash
cd jepretaja_apk
./gradlew :app:compileDebugKotlin
```

## Catatan

Project ini sudah dipush ke GitHub dan dideploy ke Vercel dalam kondisi yang sudah diverifikasi. Untuk update berikutnya, lakukan push ke branch default GitHub dan deploy otomatis akan mengikuti setup yang ada di Vercel.
