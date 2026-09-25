package com.plusemon.bizlipay.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.FilterList
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.plusemon.bizlipay.ui.components.HeaderHealthDetailsDialog
import com.plusemon.bizlipay.ui.components.HeaderStatusDot
import com.plusemon.bizlipay.ui.components.BizliPayLogoLockup
import com.plusemon.bizlipay.ui.screens.DashboardScreen
import com.plusemon.bizlipay.ui.screens.LoginScreen
import com.plusemon.bizlipay.ui.screens.OnboardingScreen
import com.plusemon.bizlipay.ui.screens.OperationsScreen
import com.plusemon.bizlipay.ui.screens.QrScannerScreen
import com.plusemon.bizlipay.ui.screens.SendersScreen
import com.plusemon.bizlipay.ui.screens.SettingsScreen
import com.plusemon.bizlipay.ui.theme.AccentEmerald
import com.plusemon.bizlipay.ui.theme.AccentRose
import com.plusemon.bizlipay.ui.theme.BorderZinc800
import com.plusemon.bizlipay.ui.theme.CanvasBlack
import com.plusemon.bizlipay.ui.theme.GhostEmeraldBg
import com.plusemon.bizlipay.ui.theme.GhostEmeraldBorder
import com.plusemon.bizlipay.ui.theme.GhostRoseBg
import com.plusemon.bizlipay.ui.theme.GhostRoseBorder
import com.plusemon.bizlipay.ui.theme.BizliTheme
import com.plusemon.bizlipay.ui.theme.TextWhite
import com.plusemon.bizlipay.ui.theme.TextZinc400
import com.plusemon.bizlipay.ui.theme.TextZinc500
import com.plusemon.bizlipay.ui.viewmodel.MainViewModel

const val ROUTE_ONBOARDING = "onboarding"
const val ROUTE_LOGIN = "login"

sealed class AppDestination(
    val route: String,
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    data object Dashboard : AppDestination(
        route = "dashboard",
        title = "Dashboard",
        selectedIcon = Icons.Filled.Dashboard,
        unselectedIcon = Icons.Outlined.Dashboard
    )

    data object Senders : AppDestination(
        route = "senders",
        title = "Senders",
        selectedIcon = Icons.Filled.FilterList,
        unselectedIcon = Icons.Outlined.FilterList
    )

    data object Operations : AppDestination(
        route = "operations",
        title = "Operations",
        selectedIcon = Icons.Filled.Security,
        unselectedIcon = Icons.Outlined.Security
    )

    data object Settings : AppDestination(
        route = "settings",
        title = "Settings",
        selectedIcon = Icons.Filled.Settings,
        unselectedIcon = Icons.Outlined.Settings
    )

    data object QrSetup : AppDestination(
        route = "qr_setup",
        title = "QR Setup",
        selectedIcon = Icons.Filled.QrCodeScanner,
        unselectedIcon = Icons.Outlined.QrCodeScanner
    )
}

val navDestinations = listOf(
    AppDestination.Dashboard,
    AppDestination.Senders,
    AppDestination.Operations,
    AppDestination.Settings
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BizliPayApp(
    viewModel: MainViewModel,
    navController: NavHostController = rememberNavController()
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val isServiceRunning by viewModel.isServiceRunning.collectAsStateWithLifecycle()
    val syncHealth by viewModel.syncHealth.collectAsStateWithLifecycle()
    var showHealthDialog by remember { mutableStateOf(false) }
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: (
        if (settings.onboardingCompleted) AppDestination.Dashboard.route else ROUTE_ONBOARDING
    )

    val isTopLevelDestination = currentRoute in navDestinations.map { it.route }

    // Reactively navigate to onboarding if user resets onboarding/panel from settings
    LaunchedEffect(settings.onboardingCompleted) {
        if (!settings.onboardingCompleted && currentRoute != ROUTE_ONBOARDING && currentRoute != ROUTE_LOGIN) {
            navController.navigate(ROUTE_ONBOARDING) {
                popUpTo(0) { inclusive = true }
                launchSingleTop = true
            }
        }
    }

    if (showHealthDialog) {
        HeaderHealthDetailsDialog(
            health = syncHealth,
            serverUrl = settings.serverBaseUrl,
            onDismiss = { showHealthDialog = false },
            onPingServer = { viewModel.pingServerHealth() },
            onTriggerSync = { viewModel.triggerManualSync() }
        )
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            if (isTopLevelDestination && currentRoute != AppDestination.Dashboard.route) {
                TopAppBar(
                    title = {
                        BizliPayLogoLockup(
                            iconSize = 28.dp,
                            titleFontSize = 16,
                            subtitle = null,
                            statusHealth = syncHealth
                        )
                    },
                    actions = {
                        Row(
                            modifier = Modifier.padding(end = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Small persistent status dot (green/amber/red) based on real-time server connectivity & sync health
                            HeaderStatusDot(
                                health = syncHealth,
                                onClick = { showHealthDialog = true }
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background
                    )
                )
            }
        },
        bottomBar = {
            if (isTopLevelDestination) {
                val colors = BizliTheme.colors
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("bottom_nav_bar"),
                    color = colors.container,
                    border = BorderStroke(1.dp, colors.border)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(bottom = 6.dp)
                            .height(60.dp)
                            .padding(horizontal = 4.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceAround,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        navDestinations.forEach { destination ->
                            val isSelected = currentRoute == destination.route
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .clickable {
                                        if (currentRoute != destination.route) {
                                            navController.navigate(destination.route) {
                                                popUpTo(navController.graph.findStartDestination().id) {
                                                    saveState = true
                                                }
                                                launchSingleTop = true
                                                restoreState = true
                                            }
                                        }
                                    }
                                    .testTag("nav_item_${destination.route}"),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.SpaceBetween
                            ) {
                                // Subtle top indicator line for active tab
                                Box(
                                    modifier = Modifier
                                        .width(28.dp)
                                        .height(2.dp)
                                        .clip(RoundedCornerShape(1.dp))
                                        .background(if (isSelected) com.plusemon.bizlipay.ui.theme.Indigo500 else Color.Transparent)
                                )

                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    Icon(
                                        imageVector = if (isSelected) destination.selectedIcon else destination.unselectedIcon,
                                        contentDescription = destination.title,
                                        tint = if (isSelected) com.plusemon.bizlipay.ui.theme.Indigo500 else colors.textMuted,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Text(
                                        text = destination.title,
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                                        color = if (isSelected) com.plusemon.bizlipay.ui.theme.Indigo500 else colors.textMuted
                                    )
                                }

                                Spacer(modifier = Modifier.height(2.dp))
                            }
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = if (settings.onboardingCompleted) AppDestination.Dashboard.route else ROUTE_ONBOARDING,
            modifier = Modifier.padding(if (isTopLevelDestination) innerPadding else androidx.compose.foundation.layout.PaddingValues(0.dp))
        ) {
            composable(ROUTE_ONBOARDING) {
                OnboardingScreen(
                    viewModel = viewModel,
                    onOnboardingFinished = {
                        // Navigate directly to DashboardScreen and clear Login/Onboarding from backstack
                        navController.navigate(AppDestination.Dashboard.route) {
                            popUpTo(0) { inclusive = true }
                            launchSingleTop = true
                        }
                    }
                )
            }

            composable(ROUTE_LOGIN) {
                LoginScreen(
                    viewModel = viewModel,
                    onLoginSuccess = {
                        // Navigate directly to DashboardScreen and clear Login/Onboarding from backstack
                        navController.navigate(AppDestination.Dashboard.route) {
                            popUpTo(0) { inclusive = true }
                            launchSingleTop = true
                        }
                    },
                    onBack = {
                        navController.popBackStack()
                    },
                    onNavigateToQr = {
                        navController.navigate(AppDestination.QrSetup.route)
                    }
                )
            }

            composable(AppDestination.Dashboard.route) {
                DashboardScreen(viewModel = viewModel)
            }

            composable(AppDestination.Senders.route) {
                SendersScreen(viewModel = viewModel)
            }

            composable(AppDestination.Operations.route) {
                OperationsScreen(viewModel = viewModel)
            }

            composable(AppDestination.Settings.route) {
                SettingsScreen(viewModel = viewModel)
            }

            composable(AppDestination.QrSetup.route) {
                QrScannerScreen(
                    viewModel = viewModel,
                    onConfigApplied = {
                        navController.navigate(AppDestination.Dashboard.route) {
                            popUpTo(AppDestination.Dashboard.route) { inclusive = true }
                            launchSingleTop = true
                        }
                    },
                    onNavigateToSettings = {
                        navController.navigate(AppDestination.Settings.route)
                    }
                )
            }
        }
    }
}

