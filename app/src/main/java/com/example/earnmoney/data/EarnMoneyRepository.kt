package com.example.earnmoney.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.TimeUnit

class EarnMoneyRepository(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("earn_money_prefs", Context.MODE_PRIVATE)

    private val _userAccount = MutableStateFlow(loadUserAccount())
    val userAccount: StateFlow<UserAccount> = _userAccount.asStateFlow()

    private val _transactions = MutableStateFlow(loadInitialTransactions())
    val transactions: StateFlow<List<EarningsTransaction>> = _transactions.asStateFlow()

    private val _payoutRequests = MutableStateFlow(loadInitialPayoutRequests())
    val payoutRequests: StateFlow<List<PayoutRequest>> = _payoutRequests.asStateFlow()

    private val _adOffers = MutableStateFlow(getAvailableOffersList())
    val adOffers: StateFlow<List<AdOffer>> = _adOffers.asStateFlow()

    fun recordShortAdWatched(shortAdItem: ShortAdItem) {
        val current = _userAccount.value
        val gross = shortAdItem.grossRewardUsd
        val devCut = gross * (current.developerCutPercentage / 100.0)
        val userNet = gross - devCut
        val points = shortAdItem.pointsReward

        val updatedAccount = current.copy(
            totalPoints = current.totalPoints + points,
            netBalanceUsd = current.netBalanceUsd + userNet,
            totalGrossRevenueGenerated = current.totalGrossRevenueGenerated + gross,
            totalDeveloperEarnings = current.totalDeveloperEarnings + devCut,
            totalAdsWatched = current.totalAdsWatched + 1
        )
        _userAccount.value = updatedAccount
        saveUserAccount(updatedAccount)

        val newTransaction = EarningsTransaction(
            title = shortAdItem.title,
            adType = AdType.REWARDED_VIDEO,
            grossAmountUsd = gross,
            developerCutUsd = devCut,
            userNetAmountUsd = userNet,
            pointsEarned = points
        )
        _transactions.value = listOf(newTransaction) + _transactions.value
    }

    fun recordAdWatched(adOffer: AdOffer) {
        val current = _userAccount.value
        val gross = adOffer.grossRewardUsd
        val devCut = gross * (current.developerCutPercentage / 100.0)
        val userNet = gross - devCut
        val points = adOffer.pointsReward

        val updatedAccount = current.copy(
            totalPoints = current.totalPoints + points,
            netBalanceUsd = current.netBalanceUsd + userNet,
            totalGrossRevenueGenerated = current.totalGrossRevenueGenerated + gross,
            totalDeveloperEarnings = current.totalDeveloperEarnings + devCut,
            totalAdsWatched = current.totalAdsWatched + 1
        )
        _userAccount.value = updatedAccount
        saveUserAccount(updatedAccount)

        val newTransaction = EarningsTransaction(
            title = adOffer.title,
            adType = adOffer.adType,
            grossAmountUsd = gross,
            developerCutUsd = devCut,
            userNetAmountUsd = userNet,
            pointsEarned = points
        )
        val updatedTransactions = listOf(newTransaction) + _transactions.value
        _transactions.value = updatedTransactions
    }

    fun claimDailyCheckIn(): Boolean {
        val current = _userAccount.value
        val now = System.currentTimeMillis()
        val oneDayMillis = TimeUnit.DAYS.toMillis(1)

        // Check if claimed today
        if (current.lastCheckInDateMillis > 0 && (now - current.lastCheckInDateMillis) < oneDayMillis) {
            return false
        }

        val gross = 0.10
        val devCut = gross * (current.developerCutPercentage / 100.0)
        val userNet = gross - devCut
        val points = 100

        val updatedAccount = current.copy(
            totalPoints = current.totalPoints + points,
            netBalanceUsd = current.netBalanceUsd + userNet,
            totalGrossRevenueGenerated = current.totalGrossRevenueGenerated + gross,
            totalDeveloperEarnings = current.totalDeveloperEarnings + devCut,
            dailyStreakDays = current.dailyStreakDays + 1,
            lastCheckInDateMillis = now
        )
        _userAccount.value = updatedAccount
        saveUserAccount(updatedAccount)

        val newTransaction = EarningsTransaction(
            title = "Daily Check-in Bonus",
            adType = AdType.REWARDED_VIDEO,
            grossAmountUsd = gross,
            developerCutUsd = devCut,
            userNetAmountUsd = userNet,
            pointsEarned = points
        )
        _transactions.value = listOf(newTransaction) + _transactions.value
        return true
    }

    fun submitPayoutRequest(method: PayoutMethod, accountDetails: String, amountUsd: Double): Boolean {
        val current = _userAccount.value
        if (amountUsd <= 0 || current.netBalanceUsd < amountUsd) {
            return false
        }

        val updatedAccount = current.copy(
            netBalanceUsd = current.netBalanceUsd - amountUsd
        )
        _userAccount.value = updatedAccount
        saveUserAccount(updatedAccount)

        val newRequest = PayoutRequest(
            method = method,
            accountDetails = accountDetails,
            amountUsd = amountUsd,
            status = PayoutStatus.PENDING
        )
        _payoutRequests.value = listOf(newRequest) + _payoutRequests.value
        return true
    }

    fun updateDeveloperCutPercentage(percentage: Double) {
        val clamped = percentage.coerceIn(0.0, 50.0)
        val updated = _userAccount.value.copy(developerCutPercentage = clamped)
        _userAccount.value = updated
        saveUserAccount(updated)
    }

    fun updateSimulatedEcpm(ecpmUsd: Double) {
        val clamped = ecpmUsd.coerceIn(1.0, 100.0)
        val updated = _userAccount.value.copy(simulatedEcpmUsd = clamped)
        _userAccount.value = updated
        saveUserAccount(updated)
        refreshAdOffersWithEcpm(clamped)
    }

    fun updateConnectOnWifiOnly(enabled: Boolean) {
        val updated = _userAccount.value.copy(connectOnWifiOnly = enabled)
        _userAccount.value = updated
        saveUserAccount(updated)
    }

    fun updateUseTestAdsMode(enabled: Boolean) {
        val updated = _userAccount.value.copy(useTestAdsMode = enabled)
        _userAccount.value = updated
        saveUserAccount(updated)
    }

    fun resetData() {
        val defaultAccount = UserAccount()
        _userAccount.value = defaultAccount
        saveUserAccount(defaultAccount)
        _transactions.value = loadInitialTransactions()
        _payoutRequests.value = emptyList()
    }

    private fun refreshAdOffersWithEcpm(ecpmUsd: Double) {
        // eCPM = ($ Revenue / 1000 Views) => Revenue per view = eCPM / 1000
        val baseMultiplier = ecpmUsd / 25.0 // normalized against default $25 CPM
        _adOffers.value = getAvailableOffersList().map { offer ->
            val updatedGross = (offer.grossRewardUsd * baseMultiplier).coerceAtLeast(0.01)
            val updatedPoints = (offer.pointsReward * baseMultiplier).toInt().coerceAtLeast(10)
            offer.copy(
                grossRewardUsd = (updatedGross * 100).toInt() / 100.0,
                pointsReward = updatedPoints
            )
        }
    }

    private fun loadUserAccount(): UserAccount {
        val points = prefs.getInt("totalPoints", 250)
        val balanceBits = prefs.getLong("netBalanceUsdBits", java.lang.Double.doubleToRawLongBits(1.25))
        val devCutBits = prefs.getLong("devCutBits", java.lang.Double.doubleToRawLongBits(20.0))
        val grossBits = prefs.getLong("grossBits", java.lang.Double.doubleToRawLongBits(1.5625))
        val devEarnBits = prefs.getLong("devEarnBits", java.lang.Double.doubleToRawLongBits(0.3125))
        val adsWatched = prefs.getInt("totalAdsWatched", 5)
        val streak = prefs.getInt("dailyStreakDays", 2)
        val lastCheckIn = prefs.getLong("lastCheckInDateMillis", 0L)
        val ecpmBits = prefs.getLong("ecpmBits", java.lang.Double.doubleToRawLongBits(25.0))
        val wifiOnly = prefs.getBoolean("connectOnWifiOnly", true)
        val testAds = prefs.getBoolean("useTestAdsMode", false)

        return UserAccount(
            totalPoints = points,
            netBalanceUsd = java.lang.Double.longBitsToDouble(balanceBits),
            developerCutPercentage = java.lang.Double.longBitsToDouble(devCutBits),
            totalGrossRevenueGenerated = java.lang.Double.longBitsToDouble(grossBits),
            totalDeveloperEarnings = java.lang.Double.longBitsToDouble(devEarnBits),
            totalAdsWatched = adsWatched,
            dailyStreakDays = streak,
            lastCheckInDateMillis = lastCheckIn,
            simulatedEcpmUsd = java.lang.Double.longBitsToDouble(ecpmBits),
            connectOnWifiOnly = wifiOnly,
            useTestAdsMode = testAds
        )
    }

    private fun saveUserAccount(account: UserAccount) {
        prefs.edit()
            .putInt("totalPoints", account.totalPoints)
            .putLong("netBalanceUsdBits", java.lang.Double.doubleToRawLongBits(account.netBalanceUsd))
            .putLong("devCutBits", java.lang.Double.doubleToRawLongBits(account.developerCutPercentage))
            .putLong("grossBits", java.lang.Double.doubleToRawLongBits(account.totalGrossRevenueGenerated))
            .putLong("devEarnBits", java.lang.Double.doubleToRawLongBits(account.totalDeveloperEarnings))
            .putInt("totalAdsWatched", account.totalAdsWatched)
            .putInt("dailyStreakDays", account.dailyStreakDays)
            .putLong("lastCheckInDateMillis", account.lastCheckInDateMillis)
            .putLong("ecpmBits", java.lang.Double.doubleToRawLongBits(account.simulatedEcpmUsd))
            .putBoolean("connectOnWifiOnly", account.connectOnWifiOnly)
            .putBoolean("useTestAdsMode", account.useTestAdsMode)
            .apply()
    }

    private fun getAvailableOffersList(): List<AdOffer> {
        return listOf(
            AdOffer(
                title = "High Reward Video Ad",
                description = "Watch 15-second sponsored gaming trailer",
                adType = AdType.REWARDED_VIDEO,
                grossRewardUsd = 0.05,
                pointsReward = 50,
                durationSeconds = 15,
                sponsorName = "SuperGame Studios",
                category = "Gaming & Apps"
            ),
            AdOffer(
                title = "Premium Sponsor Reel",
                description = "Watch 30-second brand spotlight video",
                adType = AdType.REWARDED_VIDEO,
                grossRewardUsd = 0.10,
                pointsReward = 100,
                durationSeconds = 30,
                sponsorName = "TechGear Global",
                category = "Consumer Electronics"
            ),
            AdOffer(
                title = "Quick Interstitial Ad",
                description = "View 5-second fast interactive ad card",
                adType = AdType.INTERSTITIAL,
                grossRewardUsd = 0.02,
                pointsReward = 20,
                durationSeconds = 5,
                sponsorName = "CloudStream App",
                category = "Entertainment"
            ),
            AdOffer(
                title = "High-Yield Survey Ad",
                description = "Complete quick 20-second opinion video offer",
                adType = AdType.HIGH_ECPM_OFFER,
                grossRewardUsd = 0.15,
                pointsReward = 150,
                durationSeconds = 20,
                sponsorName = "Consumer Insights Inc",
                category = "Surveys & Market"
            ),
            AdOffer(
                title = "Passive Banner Impression",
                description = "Interact with active top banner advertisement",
                adType = AdType.BANNER,
                grossRewardUsd = 0.01,
                pointsReward = 10,
                durationSeconds = 3,
                sponsorName = "Fintech Mobile",
                category = "Finance"
            )
        )
    }

    private fun loadInitialTransactions(): List<EarningsTransaction> {
        return listOf(
            EarningsTransaction(
                title = "High Reward Video Ad",
                adType = AdType.REWARDED_VIDEO,
                grossAmountUsd = 0.05,
                developerCutUsd = 0.01,
                userNetAmountUsd = 0.04,
                pointsEarned = 50
            ),
            EarningsTransaction(
                title = "Quick Interstitial Ad",
                adType = AdType.INTERSTITIAL,
                grossAmountUsd = 0.02,
                developerCutUsd = 0.004,
                userNetAmountUsd = 0.016,
                pointsEarned = 20
            ),
            EarningsTransaction(
                title = "Welcome Signup Bonus",
                adType = AdType.REWARDED_VIDEO,
                grossAmountUsd = 1.00,
                developerCutUsd = 0.20,
                userNetAmountUsd = 0.80,
                pointsEarned = 1000
            )
        )
    }

    private fun loadInitialPayoutRequests(): List<PayoutRequest> {
        return listOf(
            PayoutRequest(
                method = PayoutMethod.PAYPAL,
                accountDetails = "user.wallet@example.com",
                amountUsd = 5.00,
                status = PayoutStatus.COMPLETED
            )
        )
    }
}
