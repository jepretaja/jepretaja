package com.jepretaja.app.ui.components

import android.graphics.Color as AndroidColor
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.nativead.MediaView
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdOptions
import com.google.android.gms.ads.nativead.NativeAdView
import com.jepretaja.app.BuildConfig

/** Native ad satu halaman di feed Explore. MediaView memutar video bila tersedia. */
@Composable
fun AdMobNativeExplore(modifier: Modifier = Modifier) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var nativeAd by remember { mutableStateOf<NativeAd?>(null) }

    DisposableEffect(Unit) {
        val loader = AdLoader.Builder(context, BuildConfig.ADMOB_NATIVE_AD_UNIT_ID)
            .forNativeAd { loaded ->
                nativeAd?.destroy()
                nativeAd = loaded
            }
            .withNativeAdOptions(
                NativeAdOptions.Builder()
                    .setMediaAspectRatio(NativeAdOptions.NATIVE_MEDIA_ASPECT_RATIO_PORTRAIT)
                    .build(),
            )
            .build()
        loader.loadAd(AdRequest.Builder().build())
        onDispose { nativeAd?.destroy() }
    }

    nativeAd?.let { ad ->
        AndroidView(
            modifier = modifier.fillMaxSize(),
            factory = { createNativeAdView(it) },
            update = { bindNativeAd(it, ad) },
        )
    }
}

private fun createNativeAdView(context: android.content.Context): NativeAdView {
    val adView = NativeAdView(context)
    val root = LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(context, 16), dp(context, 52), dp(context, 16), dp(context, 24))
        setBackgroundColor(AndroidColor.BLACK)
    }

    val media = MediaView(context).apply {
        layoutParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            0,
            1f,
        )
    }
    val label = TextView(context).apply {
        text = "IKLAN"
        setTextColor(AndroidColor.LTGRAY)
        textSize = 11f
        setPadding(0, dp(context, 10), 0, dp(context, 4))
    }
    val headline = TextView(context).apply {
        setTextColor(AndroidColor.WHITE)
        textSize = 20f
        setMaxLines(2)
    }
    val body = TextView(context).apply {
        setTextColor(AndroidColor.LTGRAY)
        textSize = 14f
        setMaxLines(3)
        setPadding(0, dp(context, 6), 0, dp(context, 10))
    }
    val cta = Button(context).apply {
        isAllCaps = false
        setTextColor(AndroidColor.WHITE)
        setBackgroundColor(AndroidColor.rgb(45, 105, 220))
    }

    root.addView(media)
    root.addView(label)
    root.addView(headline)
    root.addView(body)
    root.addView(cta, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(context, 48)))
    adView.addView(root)
    adView.mediaView = media
    adView.headlineView = headline
    adView.bodyView = body
    adView.callToActionView = cta
    return adView
}

private fun bindNativeAd(adView: NativeAdView, ad: NativeAd) {
    (adView.headlineView as? TextView)?.text = ad.headline.orEmpty()
    (adView.bodyView as? TextView)?.text = ad.body.orEmpty()
    (adView.callToActionView as? Button)?.text = ad.callToAction.orEmpty()
    adView.mediaView?.mediaContent = ad.mediaContent
    adView.setNativeAd(ad)
}

private fun dp(context: android.content.Context, value: Int): Int =
    (value * context.resources.displayMetrics.density).toInt()
