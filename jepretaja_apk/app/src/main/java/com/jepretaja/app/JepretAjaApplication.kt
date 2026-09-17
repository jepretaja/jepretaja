package com.jepretaja.app

import android.app.Application
import android.content.Context
import android.util.Log
import com.google.android.gms.ads.MobileAds
import com.google.firebase.crashlytics.FirebaseCrashlytics
import dagger.hilt.android.HiltAndroidApp

/**
 * Entry point Hilt DI + inisialisasi Firebase App Check (section 45).
 * Debug provider dipakai di BuildConfig.DEBUG supaya bisa dites tanpa
 * signing release — WAJIB daftarkan debug token yang muncul di Logcat ke
 * Firebase Console > App Check > Manage debug tokens.
 */
@HiltAndroidApp
class JepretAjaApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        appContext = applicationContext

        runCatching { MobileAds.initialize(this) }
            .onFailure { Log.w(TAG, "AdMob tidak berhasil diinisialisasi; aplikasi tetap dijalankan.", it) }

        // Pemasangan App Check dipisah per build type (lihat src/debug dan
        // src/release). Penyedia debug kini hanya ada di build debug, jadi ia
        // tidak bisa lagi disebut dari kode bersama ini — dan itu memang
        // tujuannya: kelas penerobos App Check tidak ikut ke aplikasi rilis.
        runCatching { installAppCheck() }
            .onFailure { Log.w(TAG, "App Check tidak berhasil diinisialisasi; aplikasi tetap dijalankan.", it) }

        runCatching {
            FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(!BuildConfig.DEBUG)
        }.onFailure {
            Log.w(TAG, "Crashlytics tidak berhasil diinisialisasi.", it)
        }
    }

    companion object {
        private const val TAG = "JepretAjaApplication"

        // Dipakai AnalyticsService (object biasa, di luar graf Hilt) untuk
        // akses FirebaseAnalytics.getInstance(context) di luar graf Hilt.
        lateinit var appContext: Context
            private set
    }
}
