package com.example.ads

import android.app.Activity
import android.content.Context
import android.util.Log
import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.RequestConfiguration
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import java.util.concurrent.atomic.AtomicBoolean

object AdMobConfig {
    // 1 & 2: Banner Ad Units
    const val BANNER_UNIT_1 = "ca-app-pub-1495262574338316/2803163788"
    const val BANNER_UNIT_2 = "ca-app-pub-1495262574338316/2903409205"

    // 3 to 8: Interstitial / Pop-up Ad Units for tools
    val POPUP_AD_UNITS = listOf(
        "ca-app-pub-1495262574338316/2452733430",
        "ca-app-pub-1495262574338316/8970040322",
        "ca-app-pub-1495262574338316/1182179137",
        "ca-app-pub-1495262574338316/4962596335",
        "ca-app-pub-1495262574338316/2714752655",
        "ca-app-pub-1495262574338316/3503687777"
    )

    // Standard Google test ad IDs as safe fallbacks
    const val TEST_BANNER_ID = "ca-app-pub-3940256099942544/6300978111"
    const val TEST_INTERSTITIAL_ID = "ca-app-pub-3940256099942544/1033173712"
}

class AdMobManager private constructor(private val appContext: Context) {

    private var interstitialAd: InterstitialAd? = null
    private var isAdLoading = false
    private var currentAdUnitIndex = 0
    private val isInitialized = AtomicBoolean(false)

    fun initialize() {
        if (isInitialized.compareAndSet(false, true)) {
            try {
                val config = RequestConfiguration.Builder()
                    .setTestDeviceIds(listOf(AdRequest.DEVICE_ID_EMULATOR))
                    .build()
                MobileAds.setRequestConfiguration(config)

                MobileAds.initialize(appContext) { initStatus ->
                    Log.d(TAG, "AdMob MobileAds initialized with status: $initStatus")
                    preloadNextInterstitial()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error initializing MobileAds: ${e.message}")
            }
        }
    }

    private fun preloadNextInterstitial() {
        if (isAdLoading || interstitialAd != null) return
        isAdLoading = true

        val adUnitId = AdMobConfig.POPUP_AD_UNITS[currentAdUnitIndex % AdMobConfig.POPUP_AD_UNITS.size]
        currentAdUnitIndex++

        val adRequest = AdRequest.Builder().build()
        Log.d(TAG, "Preloading real interstitial ad unit: $adUnitId")

        InterstitialAd.load(
            appContext,
            adUnitId,
            adRequest,
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    Log.d(TAG, "Real interstitial ad loaded successfully: $adUnitId")
                    interstitialAd = ad
                    isAdLoading = false
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    Log.w(TAG, "Real interstitial ad failed to load ($adUnitId): ${loadAdError.message}. Trying test unit fallback.")
                    isAdLoading = false
                    interstitialAd = null
                    loadFallbackTestInterstitial()
                }
            }
        )
    }

    private fun loadFallbackTestInterstitial() {
        if (interstitialAd != null || isAdLoading) return
        isAdLoading = true
        val adRequest = AdRequest.Builder().build()
        InterstitialAd.load(
            appContext,
            AdMobConfig.TEST_INTERSTITIAL_ID,
            adRequest,
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    interstitialAd = ad
                    isAdLoading = false
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    isAdLoading = false
                    interstitialAd = null
                }
            }
        )
    }

    /**
     * Shows an automatic popup/interstitial ad before executing a tool action
     */
    fun showAdForTool(activity: Activity, toolName: String, onComplete: () -> Unit) {
        val ad = interstitialAd
        if (ad != null) {
            ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    Log.d(TAG, "Ad dismissed, executing tool: $toolName")
                    interstitialAd = null
                    preloadNextInterstitial()
                    onComplete()
                }

                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                    Log.e(TAG, "Ad failed to show: ${adError.message}")
                    interstitialAd = null
                    preloadNextInterstitial()
                    onComplete()
                }

                override fun onAdShowedFullScreenContent() {
                    Log.d(TAG, "Ad showed for tool: $toolName")
                }
            }
            ad.show(activity)
        } else {
            preloadNextInterstitial()
            onComplete()
        }
    }

    companion object {
        private const val TAG = "AdMobManager"

        @Volatile
        private var INSTANCE: AdMobManager? = null

        fun getInstance(context: Context): AdMobManager {
            return INSTANCE ?: synchronized(this) {
                val instance = AdMobManager(context.applicationContext)
                INSTANCE = instance
                instance.initialize()
                instance
            }
        }
    }
}

/**
 * Modern Google AdMob Banner Composable for real advertisements
 */
@Composable
fun AdMobBanner(
    modifier: Modifier = Modifier,
    adUnitId: String = AdMobConfig.BANNER_UNIT_1
) {
    var isAdLoaded by remember { mutableStateOf(false) }
    var isAdFailed by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 50.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)),
        contentAlignment = Alignment.Center
    ) {
        AndroidView(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 50.dp),
            factory = { context ->
                AdView(context).apply {
                    setLayerType(android.view.View.LAYER_TYPE_SOFTWARE, null)
                    setAdSize(AdSize.BANNER)
                    this.adUnitId = if (isAdFailed) AdMobConfig.TEST_BANNER_ID else adUnitId
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                    )
                    adListener = object : AdListener() {
                        override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                            Log.w("AdMobBanner", "Real Ad unit ($adUnitId) load message: ${loadAdError.message}")
                            isAdFailed = true
                            isAdLoaded = false
                        }

                        override fun onAdLoaded() {
                            Log.d("AdMobBanner", "Real AdMob banner loaded successfully ($adUnitId)")
                            isAdLoaded = true
                        }
                    }
                    val request = AdRequest.Builder().build()
                    loadAd(request)
                }
            }
        )

        if (!isAdLoaded) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("admob_bottom_banner"),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Text(
                                text = "Ad",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 10.sp),
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "Google AdMob • Anuncio Real",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = adUnitId,
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                                maxLines = 1
                            )
                        }
                    }

                    Text(
                        text = "Activo",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}
