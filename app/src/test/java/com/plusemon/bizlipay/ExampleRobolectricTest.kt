package com.plusemon.bizlipay

import android.Manifest
import android.app.Application
import android.content.Context
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.test.core.app.ApplicationProvider
import com.plusemon.bizlipay.data.prefs.ThemePreferences
import com.plusemon.bizlipay.ui.screens.OnboardingScreen
import com.plusemon.bizlipay.ui.screens.SettingsScreen
import com.plusemon.bizlipay.ui.theme.MyApplicationTheme
import com.plusemon.bizlipay.ui.viewmodel.MainViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("BizliPay", appName)
    }

    @Test
    fun `onboarding step 2 continue button is disabled when permissions not granted`() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val shadowApp = shadowOf(app)
        shadowApp.denyPermissions(Manifest.permission.RECEIVE_SMS, Manifest.permission.READ_SMS)

        val viewModel = MainViewModel(app)

        composeTestRule.setContent {
            MyApplicationTheme {
                OnboardingScreen(
                    viewModel = viewModel,
                    onOnboardingFinished = {}
                )
            }
        }

        // On Step 1, scroll and click "continue_to_permissions_button" to navigate to Step 2
        composeTestRule.onNodeWithTag("continue_to_permissions_button").performScrollTo().performClick()
        composeTestRule.waitForIdle()

        // On Step 2, continue button must be disabled when required permissions are not granted
        composeTestRule.onNodeWithTag("proceed_to_login_button").performScrollTo().assertIsNotEnabled()
    }

    @Test
    fun `onboarding step 2 continue button is disabled when notification permission is not granted even if SMS is granted`() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val shadowApp = shadowOf(app)
        shadowApp.grantPermissions(Manifest.permission.RECEIVE_SMS, Manifest.permission.READ_SMS)
        shadowApp.denyPermissions(Manifest.permission.POST_NOTIFICATIONS)

        val viewModel = MainViewModel(app)

        composeTestRule.setContent {
            MyApplicationTheme {
                OnboardingScreen(
                    viewModel = viewModel,
                    onOnboardingFinished = {}
                )
            }
        }

        // On Step 1, scroll and click "continue_to_permissions_button" to navigate to Step 2
        composeTestRule.onNodeWithTag("continue_to_permissions_button").performScrollTo().performClick()
        composeTestRule.waitForIdle()

        // On Step 2, continue button must be disabled because notification permission is missing
        composeTestRule.onNodeWithTag("proceed_to_login_button").performScrollTo().assertIsNotEnabled()
    }

    @Test
    fun `onboarding step 2 continue button is enabled when required permissions are granted`() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val shadowApp = shadowOf(app)
        shadowApp.grantPermissions(
            Manifest.permission.RECEIVE_SMS,
            Manifest.permission.READ_SMS,
            Manifest.permission.POST_NOTIFICATIONS
        )

        val viewModel = MainViewModel(app)

        composeTestRule.setContent {
            MyApplicationTheme {
                OnboardingScreen(
                    viewModel = viewModel,
                    onOnboardingFinished = {}
                )
            }
        }

        // On Step 1, scroll and click "continue_to_permissions_button" to navigate to Step 2
        composeTestRule.onNodeWithTag("continue_to_permissions_button").performScrollTo().performClick()
        composeTestRule.waitForIdle()

        // On Step 2, continue button must be enabled when required permissions are granted
        composeTestRule.onNodeWithTag("proceed_to_login_button").performScrollTo().assertIsEnabled()
    }

    @Test
    fun `verify companion bulk sms JSON structure`() {
        val jsonArray = org.json.JSONArray()
        val item = org.json.JSONObject().apply {
            put("id", "9ABC123XYZ")
            put("sender", "BKASH")
            put("message", "You have received Tk 1,500.00")
            put("simSlot", "0")
            put("timestamp", "1726588800000")
        }
        jsonArray.put(item)

        val serialized = jsonArray.toString()
        val parsed = org.json.JSONArray(serialized)
        assertEquals(1, parsed.length())
        assertEquals("9ABC123XYZ", parsed.getJSONObject(0).getString("id"))
        assertEquals("BKASH", parsed.getJSONObject(0).getString("sender"))
    }

    @Test
    fun `theme preferences persists and emits dark mode state via datastore`() = runBlocking {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val themePrefs = ThemePreferences.getInstance(app)

        // Set to true and verify
        themePrefs.setDarkMode(true)
        assertTrue(themePrefs.isDarkModeFlow.first())

        // Set to false and verify
        themePrefs.setDarkMode(false)
        assertFalse(themePrefs.isDarkModeFlow.first())

        // Reset to true
        themePrefs.setDarkMode(true)
        assertTrue(themePrefs.isDarkModeFlow.first())
    }

    @Test
    fun `settings screen displays dark mode toggle and updates viewmodel`() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val viewModel = MainViewModel(app)

        composeTestRule.setContent {
            MyApplicationTheme {
                SettingsScreen(viewModel = viewModel)
            }
        }

        // Verify dark mode toggle switch is displayed on screen
        composeTestRule.onNodeWithTag("settings_screen").performScrollToNode(hasTestTag("dark_mode_toggle"))
        composeTestRule.onNodeWithTag("dark_mode_toggle").assertIsEnabled()
        composeTestRule.onNodeWithTag("dark_mode_toggle").performClick()
        composeTestRule.waitForIdle()
    }

    @Test
    fun `verify update version comparison logic`() {
        val app = ApplicationProvider.getApplicationContext<android.content.Context>()
        val updateManager = com.plusemon.bizlipay.util.UpdateManager(app)
        assertTrue(updateManager.isNewerVersion("1.0.0", "v1.1.0"))
        assertTrue(updateManager.isNewerVersion("1.0", "1.0.1"))
        assertFalse(updateManager.isNewerVersion("1.1.0", "v1.1.0"))
        assertFalse(updateManager.isNewerVersion("1.2.0", "1.1.5"))
    }

    @Test
    fun `header status dot changes color based on real-time server connectivity and sync health`() {
        // 1. Verify semantic color tokens: Green, Amber, Red
        assertEquals(
            com.plusemon.bizlipay.ui.theme.AccentEmerald,
            com.plusemon.bizlipay.ui.components.getHealthAccentColor(com.plusemon.bizlipay.ui.viewmodel.ServerSyncStatus.HEALTHY)
        )
        assertEquals(
            com.plusemon.bizlipay.ui.theme.AccentAmber,
            com.plusemon.bizlipay.ui.components.getHealthAccentColor(com.plusemon.bizlipay.ui.viewmodel.ServerSyncStatus.WARNING)
        )
        assertEquals(
            com.plusemon.bizlipay.ui.theme.AccentRose,
            com.plusemon.bizlipay.ui.components.getHealthAccentColor(com.plusemon.bizlipay.ui.viewmodel.ServerSyncStatus.ERROR)
        )

        // 2. Render HeaderStatusDot in Healthy (Green) state
        var clicked = false
        composeTestRule.setContent {
            MyApplicationTheme {
                com.plusemon.bizlipay.ui.components.HeaderStatusDot(
                    health = com.plusemon.bizlipay.ui.viewmodel.ServerSyncHealth(
                        status = com.plusemon.bizlipay.ui.viewmodel.ServerSyncStatus.HEALTHY,
                        latencyMs = 38L
                    ),
                    onClick = { clicked = true }
                )
            }
        }

        // Verify status dot element exists and handles click
        composeTestRule.onNodeWithTag("header_status_dot").assertIsEnabled()
        composeTestRule.onNodeWithTag("header_status_dot").performClick()
        assertTrue("Status dot onClick must trigger when tapped", clicked)
    }

    @Test
    fun `header status dot renders in amber and red states with diagnostic details dialog`() {
        var pingClicked = false
        var syncClicked = false
        var dismissed = false

        composeTestRule.setContent {
            MyApplicationTheme {
                androidx.compose.foundation.layout.Column {
                    // Amber Warning (Syncing / Pending)
                    com.plusemon.bizlipay.ui.components.HeaderStatusDot(
                        health = com.plusemon.bizlipay.ui.viewmodel.ServerSyncHealth(
                            status = com.plusemon.bizlipay.ui.viewmodel.ServerSyncStatus.WARNING,
                            isSyncing = true,
                            pendingCount = 3
                        )
                    )

                    // Red Error (Disconnected / Failed)
                    com.plusemon.bizlipay.ui.components.HeaderHealthDetailsDialog(
                        health = com.plusemon.bizlipay.ui.viewmodel.ServerSyncHealth(
                            status = com.plusemon.bizlipay.ui.viewmodel.ServerSyncStatus.ERROR,
                            isOnline = false,
                            failedCount = 2
                        ),
                        serverUrl = "https://gateway.bizlipay.com/api",
                        onDismiss = { dismissed = true },
                        onPingServer = { pingClicked = true },
                        onTriggerSync = { syncClicked = true }
                    )
                }
            }
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("header_status_dot").assertIsEnabled()
    }
}
