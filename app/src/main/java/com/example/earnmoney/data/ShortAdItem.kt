package com.example.earnmoney.data

import androidx.compose.ui.graphics.Color
import java.util.UUID

data class ShortAdItem(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val sponsorName: String,
    val description: String,
    val hashtags: String,
    val grossRewardUsd: Double,
    val pointsReward: Int,
    val watchSecondsRequired: Int = 5,
    val likesCount: String = "12.4K",
    val commentsCount: String = "842",
    val videoUrl: String,
    val bgGradient: List<Color>
)

object ShortAdRepository {
    fun getShortsFeed(): List<ShortAdItem> {
        return listOf(
            ShortAdItem(
                title = "Next-Gen Gaming Smartphone Unleashed 🚀",
                sponsorName = "@SuperGameTech",
                description = "Experience 120FPS ultra-smooth gameplay with active cooling!",
                hashtags = "#gaming #tech #smartphone #ad",
                grossRewardUsd = 0.05,
                pointsReward = 50,
                watchSecondsRequired = 5,
                likesCount = "24.1K",
                commentsCount = "1.2K",
                videoUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4",
                bgGradient = listOf(Color(0xFF0F172A), Color(0xFF1E1B4B), Color(0xFF311B92))
            ),
            ShortAdItem(
                title = "5% Cashback Credit Card with Zero Annual Fee 💳",
                sponsorName = "@FintechGlobal",
                description = "Get instant approval and double cash back on all dining & travel!",
                hashtags = "#finance #cashback #money #ad",
                grossRewardUsd = 0.08,
                pointsReward = 80,
                watchSecondsRequired = 6,
                likesCount = "18.9K",
                commentsCount = "950",
                videoUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerEscapes.mp4",
                bgGradient = listOf(Color(0xFF064E3B), Color(0xFF0F172A), Color(0xFF047857))
            ),
            ShortAdItem(
                title = "Smart Fitness Watch & Sleep Tracker ⌚",
                sponsorName = "@PulseFitLife",
                description = "Track heart rate, blood oxygen, and AI sleep coaching!",
                hashtags = "#fitness #health #smartwatch #ad",
                grossRewardUsd = 0.06,
                pointsReward = 60,
                watchSecondsRequired = 5,
                likesCount = "45.2K",
                commentsCount = "2.1K",
                videoUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerFun.mp4",
                bgGradient = listOf(Color(0xFF881337), Color(0xFF1E293B), Color(0xFF4C0519))
            ),
            ShortAdItem(
                title = "Stream Unlimited 4K Movies & Shows 🎬",
                sponsorName = "@CloudStreamApp",
                description = "Start your 30-day free trial now. No credit card required!",
                hashtags = "#movies #streaming #entertainment #ad",
                grossRewardUsd = 0.10,
                pointsReward = 100,
                watchSecondsRequired = 8,
                likesCount = "62.8K",
                commentsCount = "3.4K",
                videoUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerJoylikes.mp4",
                bgGradient = listOf(Color(0xFF1E1B4B), Color(0xFF4C1D95), Color(0xFF0F172A))
            ),
            ShortAdItem(
                title = "AI Code Assistant for Developers ⚡",
                sponsorName = "@DevAIStudio",
                description = "Write unit tests and refactor code 10x faster with AI!",
                hashtags = "#developer #coding #ai #ad",
                grossRewardUsd = 0.07,
                pointsReward = 70,
                watchSecondsRequired = 5,
                likesCount = "31.5K",
                commentsCount = "1.8K",
                videoUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerMeltdowns.mp4",
                bgGradient = listOf(Color(0xFF1E293B), Color(0xFF0284C7), Color(0xFF0F172A))
            )
        )
    }
}
