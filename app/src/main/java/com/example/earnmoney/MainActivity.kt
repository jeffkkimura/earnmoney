package com.example.earnmoney

import com.earnmoney.myapp.R
import android.app.Activity
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.earnmoney.data.AdType
import com.example.earnmoney.ui.EarnMoneyViewModel
import com.example.earnmoney.ui.components.AdPlayerDialog
import com.example.earnmoney.ui.screens.CashOutScreen
import com.example.earnmoney.ui.screens.HomeScreen
import com.example.earnmoney.ui.screens.SettingsScreen
import com.example.earnmoney.ui.screens.ShortsFeedScreen
import com.example.earnmoney.ui.theme.EarnMoneyTheme

enum class NavTab(val title: String, val icon: ImageVector) {
    SHORTS("Shorts", Icons.Default.PlayArrow),
    DASHBOARD("Dashboard", Icons.Default.Home),
    CASHOUT("Cash Out", Icons.Default.AccountBalanceWallet),
    SETTINGS("Dev Admin", Icons.Default.AdminPanelSettings)
}

class MainActivity : ComponentActivity() {

    private val viewModel: EarnMoneyViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            EarnMoneyTheme {
                EarnMoneyApp(viewModel = viewModel)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EarnMoneyApp(viewModel: EarnMoneyViewModel) {
    val context = LocalContext.current
    val activity = context as? Activity

    var selectedTab by remember { mutableStateOf(NavTab.SHORTS) }
    val snackbarHostState = remember { SnackbarHostState() }

    val userAccount by viewModel.userAccount.collectAsState()
    val transactions by viewModel.transactions.collectAsState()
    val payoutRequests by viewModel.payoutRequests.collectAsState()
    val adOffers by viewModel.adOffers.collectAsState()
    val shortsFeed by viewModel.shortsFeed.collectAsState()
    val adPlaybackState by viewModel.adPlaybackState.collectAsState()
    val uiMessage by viewModel.uiMessage.collectAsState()

    LaunchedEffect(uiMessage) {
        val msg = uiMessage
        if (msg != null) {
            snackbarHostState.showSnackbar(msg)
            viewModel.clearMessage()
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_cartoon_dollar_logo),
                            contentDescription = "Earn Money Logo",
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "EarnMoney",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 20.sp
                        )
                    }
                },
                actions = {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.padding(end = 12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = null,
                                tint = Color(0xFFFBBF24),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "${userAccount.totalPoints} pts",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            NavigationBar {
                NavTab.entries.forEach { tab ->
                    NavigationBarItem(
                        selected = selectedTab == tab,
                        onClick = { selectedTab = tab },
                        icon = { Icon(imageVector = tab.icon, contentDescription = tab.title) },
                        label = { Text(tab.title) }
                    )
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Crossfade(targetState = selectedTab, label = "ScreenTransition") { tab ->
                when (tab) {
                    NavTab.SHORTS -> ShortsFeedScreen(
                        userAccount = userAccount,
                        shortsFeed = shortsFeed,
                        onPlayRealAdMob = { shortItem ->
                            activity?.let { viewModel.playRealShortAd(it, shortItem) }
                        },
                        onClaimReward = { shortItem -> viewModel.claimShortReward(shortItem) }
                    )
                    NavTab.DASHBOARD -> HomeScreen(
                        userAccount = userAccount,
                        transactions = transactions,
                        onWatchQuickAd = {
                            val offer = adOffers.firstOrNull { it.adType == AdType.REWARDED_VIDEO } ?: adOffers.first()
                            activity?.let { viewModel.watchAd(it, offer) }
                        },
                        onClaimDailyCheckIn = { viewModel.claimDailyCheckIn() },
                        onNavigateToWatchAds = { selectedTab = NavTab.SHORTS },
                        onNavigateToCashout = { selectedTab = NavTab.CASHOUT }
                    )
                    NavTab.CASHOUT -> CashOutScreen(
                        userAccount = userAccount,
                        payoutRequests = payoutRequests,
                        onSubmitPayoutRequest = { method, details, amt ->
                            viewModel.submitPayoutRequest(method, details, amt)
                        }
                    )
                    NavTab.SETTINGS -> SettingsScreen(
                        userAccount = userAccount,
                        onUpdateDeveloperCut = { pct -> viewModel.updateDeveloperCut(pct) },
                        onUpdateEcpm = { ecpm -> viewModel.updateSimulatedEcpm(ecpm) },
                        onUpdateConnectOnWifiOnly = { enabled -> viewModel.updateConnectOnWifiOnly(enabled) },
                        onUpdateUseTestAdsMode = { enabled -> viewModel.updateUseTestAdsMode(enabled) },
                        onResetData = { viewModel.resetData() }
                    )
                }
            }

            // Ad Player Dialog Overlay
            if (adPlaybackState.isPlaying && !adPlaybackState.isRealAdMob) {
                AdPlayerDialog(
                    playbackState = adPlaybackState,
                    developerCutPercentage = userAccount.developerCutPercentage,
                    onClaimReward = { viewModel.claimAdReward() },
                    onDismiss = { viewModel.dismissAd() }
                )
            }
        }
    }
}
