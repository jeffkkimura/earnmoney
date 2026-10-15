package com.example.earnmoney.data

import java.util.UUID

enum class AdType(val displayName: String) {
    REWARDED_VIDEO("Rewarded Video"),
    INTERSTITIAL("Quick Interstitial"),
    HIGH_ECPM_OFFER("High-Yield Offer"),
    BANNER("Passive Banner")
}

data class AdOffer(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val description: String,
    val adType: AdType,
    val grossRewardUsd: Double,
    val pointsReward: Int,
    val durationSeconds: Int,
    val sponsorName: String,
    val category: String,
    val cooldownSeconds: Int = 0
)

enum class PayoutMethod(val displayName: String, val iconName: String, val hint: String) {
    PAYPAL("PayPal Cash", "paypal", "Enter your PayPal Email"),
    AMAZON_GIFT_CARD("Amazon Gift Card", "amazon", "Enter your Email for e-Gift Code"),
    CRYPTO_USDT("Crypto (USDT TRC20)", "crypto", "Enter your USDT TRC20 Address"),
    GOOGLE_PLAY("Google Play Code", "playstore", "Enter Email for Redeem Code"),
    DIRECT_BANK("Direct Bank Transfer", "bank", "Enter Account Number & Routing/SWIFT")
}

enum class PayoutStatus(val displayName: String) {
    PENDING("Pending Review"),
    PROCESSING("Processing"),
    COMPLETED("Paid Successfully"),
    REJECTED("Rejected")
}

data class PayoutRequest(
    val id: String = UUID.randomUUID().toString(),
    val timestampMillis: Long = System.currentTimeMillis(),
    val method: PayoutMethod,
    val accountDetails: String,
    val amountUsd: Double,
    val status: PayoutStatus = PayoutStatus.PENDING
)

data class EarningsTransaction(
    val id: String = UUID.randomUUID().toString(),
    val timestampMillis: Long = System.currentTimeMillis(),
    val title: String,
    val adType: AdType,
    val grossAmountUsd: Double,
    val developerCutUsd: Double,
    val userNetAmountUsd: Double,
    val pointsEarned: Int
)

data class UserAccount(
    val userId: String = "USER-" + UUID.randomUUID().toString().take(6).uppercase(),
    val totalPoints: Int = 250,
    val netBalanceUsd: Double = 1.25,
    val developerCutPercentage: Double = 20.0, // e.g. 20% platform commission cut
    val totalGrossRevenueGenerated: Double = 1.5625,
    val totalDeveloperEarnings: Double = 0.3125,
    val totalAdsWatched: Int = 5,
    val dailyStreakDays: Int = 2,
    val lastCheckInDateMillis: Long = 0L,
    val payoutThresholdUsd: Double = 10.0,
    val simulatedEcpmUsd: Double = 25.0,
    val connectOnWifiOnly: Boolean = true, // Default method: Connect on Wi-Fi only
    val useTestAdsMode: Boolean = false // Toggle test ads vs live production ads
)
