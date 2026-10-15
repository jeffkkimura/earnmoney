package com.example.earnmoney.ui

import android.app.Activity
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.earnmoney.ads.AdManager
import com.example.earnmoney.ads.AdPlaybackState
import com.example.earnmoney.data.AdOffer
import com.example.earnmoney.data.EarnMoneyRepository
import com.example.earnmoney.data.EarningsTransaction
import com.example.earnmoney.data.PayoutMethod
import com.example.earnmoney.data.PayoutRequest
import com.example.earnmoney.data.ShortAdItem
import com.example.earnmoney.data.ShortAdRepository
import com.example.earnmoney.data.UserAccount
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import java.util.Locale

class EarnMoneyViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = EarnMoneyRepository(application)
    private val adManager = AdManager()

    init {
        adManager.initializeMobileAds(application)
    }

    val userAccount: StateFlow<UserAccount> = repository.userAccount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UserAccount())

    val transactions: StateFlow<List<EarningsTransaction>> = repository.transactions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val payoutRequests: StateFlow<List<PayoutRequest>> = repository.payoutRequests
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val adOffers: StateFlow<List<AdOffer>> = repository.adOffers
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val shortsFeed: StateFlow<List<ShortAdItem>> = MutableStateFlow(ShortAdRepository.getShortsFeed())
        .asStateFlow()

    val adPlaybackState: StateFlow<AdPlaybackState> = adManager.playbackState

    private val _uiMessage = MutableStateFlow<String?>(null)
    val uiMessage: StateFlow<String?> = _uiMessage.asStateFlow()

    fun playRealShortAd(activity: Activity, shortAdItem: ShortAdItem) {
        val testMode = userAccount.value.useTestAdsMode
        adManager.showRealAdMobRewardedForShort(activity, shortAdItem, useTestMode = testMode) {
            claimShortReward(shortAdItem)
        }
    }

    fun claimShortReward(shortAdItem: ShortAdItem) {
        val devCutPct = userAccount.value.developerCutPercentage
        val gross = shortAdItem.grossRewardUsd
        val devCut = gross * (devCutPct / 100.0)
        val net = gross - devCut

        repository.recordShortAdWatched(shortAdItem)

        _uiMessage.value = String.format(
            Locale.US,
            "🎉 +$%.3f Net Earned! (+%d pts) [Dev Fee: $%.3f]",
            net, shortAdItem.pointsReward, devCut
        )
    }

    fun watchAd(activity: Activity, offer: AdOffer) {
        val testMode = userAccount.value.useTestAdsMode
        adManager.startAdPlayback(activity, offer, useTestMode = testMode)
    }

    fun claimAdReward() {
        val state = adPlaybackState.value
        val offer = state.adOffer
        if (offer != null && state.isRewardClaimable) {
            val devCutPct = userAccount.value.developerCutPercentage
            val gross = offer.grossRewardUsd
            val devCut = gross * (devCutPct / 100.0)
            val net = gross - devCut

            repository.recordAdWatched(offer)
            adManager.dismissAd()

            _uiMessage.value = String.format(
                Locale.US,
                "Success! You earned +$%.3f (+%d pts). Platform fee: $%.3f",
                net, offer.pointsReward, devCut
            )
        }
    }

    fun dismissAd() {
        adManager.dismissAd()
    }

    fun claimDailyCheckIn() {
        val success = repository.claimDailyCheckIn()
        if (success) {
            _uiMessage.value = "Daily Check-in bonus claimed! +100 Points added."
        } else {
            _uiMessage.value = "Already claimed today! Come back tomorrow."
        }
    }

    fun submitPayoutRequest(method: PayoutMethod, accountDetails: String, amountUsd: Double): Boolean {
        val success = repository.submitPayoutRequest(method, accountDetails, amountUsd)
        if (success) {
            _uiMessage.value = String.format(
                Locale.US,
                "Payout request for $%.2f via %s submitted successfully!",
                amountUsd, method.displayName
            )
        } else {
            _uiMessage.value = "Payout failed. Insufficient net balance or invalid amount."
        }
        return success
    }

    fun updateDeveloperCut(percentage: Double) {
        repository.updateDeveloperCutPercentage(percentage)
        _uiMessage.value = String.format(Locale.US, "Platform developer cut updated to %.0f%%", percentage)
    }

    fun updateSimulatedEcpm(ecpmUsd: Double) {
        repository.updateSimulatedEcpm(ecpmUsd)
        _uiMessage.value = String.format(Locale.US, "Simulated eCPM set to $%.2f / 1k impressions", ecpmUsd)
    }

    fun updateConnectOnWifiOnly(enabled: Boolean) {
        repository.updateConnectOnWifiOnly(enabled)
        val status = if (enabled) "Enabled (Wi-Fi Only)" else "Disabled (Cellular Allowed)"
        _uiMessage.value = "Connect on Wi-Fi Only: $status"
    }

    fun updateUseTestAdsMode(enabled: Boolean) {
        repository.updateUseTestAdsMode(enabled)
        val status = if (enabled) "ON (Sample Test Ads Active)" else "OFF (Live Production Ads Active)"
        _uiMessage.value = "Test Ads Mode: $status"
    }

    fun resetData() {
        repository.resetData()
        _uiMessage.value = "All earnings, balances, and history reset to default."
    }

    fun clearMessage() {
        _uiMessage.value = null
    }
}
