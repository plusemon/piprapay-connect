package com.example

import android.Manifest
import android.app.Application
import android.content.Context
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.core.app.ApplicationProvider
import com.example.ui.screens.OnboardingScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.MainViewModel
import org.junit.Assert.assertEquals
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
        assertEquals("PipraPay Connect", appName)
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
}
