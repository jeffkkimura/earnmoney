package com.example.earnmoney.ads

import android.app.Activity
import android.content.Context
import android.util.Log
import com.example.earnmoney.data.AdOffer
import com.example.earnmoney.data.AdType
import com.example.earnmoney.data.ShortAdItem
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AdPlaybackState(
    val isPlaying: Boolean = false,
    val adOffer: AdOffer? = null,
    val totalSeconds: Int = 0,
    val remainingSeconds: Int = 0,
    val isRewardClaimable: Boolean = false,
    val isFinished: Boolean = false,
    val isRealAdMob: Boolean = false
)

class AdManager {

    companion object {
        private const val TAG = "AdManager"

        // Production Live Google AdMob Unit IDs
        const val BANNER_AD_UNIT_ID = "ca-app-pub-1922336933751478/1601785803"
        const val INTERSTITIAL_AD_UNIT_ID = "ca-app-pub-1922336933751478/9376159036"
        const val REWARDED_AD_UNIT_ID = "ca-app-pub-1922336933751478/5453400908"

        // Google Sample Test Unit IDs (Guaranteed to return instant test ads)
        const val SAMPLE_BANNER_AD_UNIT_ID = "ca-app-pub-3940256099942544/6300978111"
        const val SAMPLE_INTERSTITIAL_AD_UNIT_ID = "ca-app-pub-3940256099942544/1033173712"
        const val SAMPLE_REWARDED_AD_UNIT_ID = "ca-app-pub-3940256099942544/5224354917"

        fun getBannerAdUnitId(useTestMode: Boolean): String =
            if (useTestMode && com.earnmoney.myapp.BuildConfig.DEBUG) SAMPLE_BANNER_AD_UNIT_ID else BANNER_AD_UNIT_ID

        fun getInterstitialAdUnitId(useTestMode: Boolean): String =
            if (useTestMode && com.earnmoney.myapp.BuildConfig.DEBUG) SAMPLE_INTERSTITIAL_AD_UNIT_ID else INTERSTITIAL_AD_UNIT_ID

        fun getRewardedAdUnitId(useTestMode: Boolean): String =
            if (useTestMode && com.earnmoney.myapp.BuildConfig.DEBUG) SAMPLE_REWARDED_AD_UNIT_ID else REWARDED_AD_UNIT_ID
    }

    private val scope = CoroutineScope(Dispatchers.Main)
    private var adPlaybackJob: Job? = null

    private val _playbackState = MutableStateFlow(AdPlaybackState())
    val playbackState: StateFlow<AdPlaybackState> = _playbackState.asStateFlow()

    private var isAdMobInitialized = false

    fun initializeMobileAds(context: Context) {
        if (isAdMobInitialized) return
        MobileAds.initialize(context) { initializationStatus ->
            Log.d(TAG, "AdMob initialized: ${initializationStatus.adapterStatusMap.keys}")
            isAdMobInitialized = true
        }
    }

    fun showRealAdMobRewardedForShort(
        activity: Activity,
        shortAdItem: ShortAdItem,
        useTestMode: Boolean = false,
        onRewardEarned: () -> Unit
    ) {
        val unitId = getRewardedAdUnitId(useTestMode)
        val adRequest = AdRequest.Builder().build()
        Log.d(TAG, "Loading Rewarded Ad [TestMode=$useTestMode, UnitID=$unitId]")

        RewardedAd.load(
            activity,
            unitId,
            adRequest,
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(rewardedAd: RewardedAd) {
                    Log.d(TAG, "AdMob Rewarded Ad Loaded for Short: ${shortAdItem.title}")
                    rewardedAd.show(activity) { rewardItem ->
                        Log.d(TAG, "User earned reward from AdMob video short: ${rewardItem.amount}")
                        onRewardEarned()
                    }
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    Log.w(TAG, "AdMob Rewarded Ad failed to load: ${loadAdError.message}. Granting reward via fallback.")
                    onRewardEarned()
                }
            }
        )
    }

    fun startAdPlayback(activity: Activity, offer: AdOffer, useTestMode: Boolean = false) {
        when (offer.adType) {
            AdType.REWARDED_VIDEO -> loadAndShowRewardedAdMob(activity, offer, useTestMode)
            AdType.INTERSTITIAL -> loadAndShowInterstitialAdMob(activity, offer, useTestMode)
            else -> startSimulatedAdPlayback(offer)
        }
    }

    private fun loadAndShowRewardedAdMob(activity: Activity, offer: AdOffer, useTestMode: Boolean) {
        val unitId = getRewardedAdUnitId(useTestMode)
        val adRequest = AdRequest.Builder().build()
        Log.d(TAG, "Loading Rewarded Ad [TestMode=$useTestMode, UnitID=$unitId]")

        RewardedAd.load(
            activity,
            unitId,
            adRequest,
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(rewardedAd: RewardedAd) {
                    Log.d(TAG, "AdMob Rewarded Ad Loaded!")
                    rewardedAd.show(activity) { rewardItem ->
                        Log.d(TAG, "AdMob User Earned Reward: ${rewardItem.amount} ${rewardItem.type}")
                        _playbackState.value = AdPlaybackState(
                            isPlaying = true,
                            adOffer = offer,
                            totalSeconds = offer.durationSeconds,
                            remainingSeconds = 0,
                            isRewardClaimable = true,
                            isFinished = true,
                            isRealAdMob = true
                        )
                    }
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    Log.w(TAG, "AdMob Rewarded Ad failed to load: ${loadAdError.message}. Falling back to simulator overlay.")
                    startSimulatedAdPlayback(offer)
                }
            }
        )
    }

    private fun loadAndShowInterstitialAdMob(activity: Activity, offer: AdOffer, useTestMode: Boolean) {
        val unitId = getInterstitialAdUnitId(useTestMode)
        val adRequest = AdRequest.Builder().build()
        Log.d(TAG, "Loading Interstitial Ad [TestMode=$useTestMode, UnitID=$unitId]")

        InterstitialAd.load(
            activity,
            unitId,
            adRequest,
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(interstitialAd: InterstitialAd) {
                    Log.d(TAG, "AdMob Interstitial Ad Loaded!")
                    interstitialAd.show(activity)
                    _playbackState.value = AdPlaybackState(
                        isPlaying = true,
                        adOffer = offer,
                        totalSeconds = offer.durationSeconds,
                        remainingSeconds = 0,
                        isRewardClaimable = true,
                        isFinished = true,
                        isRealAdMob = true
                    )
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    Log.w(TAG, "AdMob Interstitial Ad failed to load: ${loadAdError.message}. Falling back to simulator overlay.")
                    startSimulatedAdPlayback(offer)
                }
            }
        )
    }

    private fun startSimulatedAdPlayback(offer: AdOffer) {
        adPlaybackJob?.cancel()
        _playbackState.value = AdPlaybackState(
            isPlaying = true,
            adOffer = offer,
            totalSeconds = offer.durationSeconds,
            remainingSeconds = offer.durationSeconds,
            isRewardClaimable = false,
            isFinished = false,
            isRealAdMob = false
        )

        adPlaybackJob = scope.launch {
            var currentSec = offer.durationSeconds
            while (currentSec > 0) {
                delay(1000L)
                currentSec--
                _playbackState.value = _playbackState.value.copy(
                    remainingSeconds = currentSec,
                    isRewardClaimable = currentSec == 0
                )
            }
            _playbackState.value = _playbackState.value.copy(
                isFinished = true,
                isRewardClaimable = true
            )
        }
    }

    fun dismissAd() {
        adPlaybackJob?.cancel()
        _playbackState.value = AdPlaybackState()
    }
}
