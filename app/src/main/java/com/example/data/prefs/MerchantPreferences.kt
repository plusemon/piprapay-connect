package com.example.data.prefs

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

class MerchantPreferences private constructor(context: Context) {

    private val prefs: SharedPreferences = createEncryptedPreferences(context)

    private val _settingsFlow = MutableStateFlow(loadSettings())
    val settingsFlow: StateFlow<MerchantSettings> = _settingsFlow.asStateFlow()

    private fun loadSettings(): MerchantSettings {
        var deviceKey = prefs.getString(KEY_DEVICE_KEY, null)
        if (deviceKey.isNullOrBlank()) {
            deviceKey = "DEV-" + UUID.randomUUID().toString().take(8).uppercase()
            prefs.edit().putString(KEY_DEVICE_KEY, deviceKey).apply()
        }

        return MerchantSettings(
            serverBaseUrl = prefs.getString(KEY_SERVER_BASE_URL, DEFAULT_BASE_URL) ?: DEFAULT_BASE_URL,
            apiKey = prefs.getString(KEY_API_KEY, "") ?: "",
            deviceKey = deviceKey,
            otp = prefs.getString(KEY_OTP, "") ?: "",
            deviceName = prefs.getString(KEY_DEVICE_NAME, getDefaultDeviceName()) ?: getDefaultDeviceName(),
            deviceModel = android.os.Build.MODEL ?: "Android Device",
            androidLevel = "API ${android.os.Build.VERSION.SDK_INT} (Android ${android.os.Build.VERSION.RELEASE})",
            appVersion = "1.0.0",
            serviceEnabled = prefs.getBoolean(KEY_SERVICE_ENABLED, true),
            lastSyncTimestamp = prefs.getLong(KEY_LAST_SYNC_TIME, 0L),
            autoSyncEnabled = prefs.getBoolean(KEY_AUTO_SYNC, true),
            onboardingCompleted = prefs.getBoolean(KEY_ONBOARDING_COMPLETED, false)
        )
    }

    private fun getDefaultDeviceName(): String {
        val manufacturer = android.os.Build.MANUFACTURER.orEmpty().replaceFirstChar { it.uppercase() }
        val model = android.os.Build.MODEL.orEmpty()
        return if (model.startsWith(manufacturer, ignoreCase = true)) model else "$manufacturer $model"
    }

    fun getOtp(): String = prefs.getString(KEY_OTP, "") ?: ""
    fun getDeviceName(): String = prefs.getString(KEY_DEVICE_NAME, getDefaultDeviceName()) ?: getDefaultDeviceName()
    fun getDeviceModel(): String = android.os.Build.MODEL ?: "Android"
    fun getAndroidLevel(): String = "API ${android.os.Build.VERSION.SDK_INT}"

    fun isOnboardingCompleted(): Boolean {
        return prefs.getBoolean(KEY_ONBOARDING_COMPLETED, false)
    }

    fun setOnboardingCompleted(completed: Boolean) {
        prefs.edit().putBoolean(KEY_ONBOARDING_COMPLETED, completed).apply()
        _settingsFlow.value = loadSettings()
    }

    fun completeOnboardingAndLogin(
        serverBaseUrl: String,
        apiKey: String,
        deviceKey: String = getDeviceKey(),
        otp: String = ""
    ) {
        val sanitizedUrl = if (serverBaseUrl.endsWith("/")) serverBaseUrl else "$serverBaseUrl/"
        prefs.edit()
            .putString(KEY_SERVER_BASE_URL, sanitizedUrl)
            .putString(KEY_API_KEY, apiKey)
            .putString(KEY_DEVICE_KEY, deviceKey.ifBlank { getDeviceKey() })
            .putString(KEY_OTP, otp)
            .putBoolean(KEY_SERVICE_ENABLED, true)
            .putBoolean(KEY_ONBOARDING_COMPLETED, true)
            .apply()
        _settingsFlow.value = loadSettings()
    }

    fun resetOnboarding() {
        prefs.edit()
            .putBoolean(KEY_ONBOARDING_COMPLETED, false)
            .apply()
        _settingsFlow.value = loadSettings()
    }

    fun getServerBaseUrl(): String {
        return prefs.getString(KEY_SERVER_BASE_URL, DEFAULT_BASE_URL) ?: DEFAULT_BASE_URL
    }

    fun getApiKey(): String {
        return prefs.getString(KEY_API_KEY, "") ?: ""
    }

    fun getDeviceKey(): String {
        var key = prefs.getString(KEY_DEVICE_KEY, null)
        if (key.isNullOrBlank()) {
            key = "DEV-" + UUID.randomUUID().toString().take(8).uppercase()
            prefs.edit().putString(KEY_DEVICE_KEY, key).apply()
        }
        return key
    }

    fun isServiceEnabled(): Boolean {
        return prefs.getBoolean(KEY_SERVICE_ENABLED, true)
    }

    fun setServiceEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_SERVICE_ENABLED, enabled).apply()
        _settingsFlow.value = loadSettings()
    }

    fun updateSettings(
        serverBaseUrl: String,
        apiKey: String,
        deviceKey: String,
        otp: String? = null
    ) {
        val sanitizedUrl = if (serverBaseUrl.endsWith("/")) serverBaseUrl else "$serverBaseUrl/"
        val editor = prefs.edit()
            .putString(KEY_SERVER_BASE_URL, sanitizedUrl)
            .putString(KEY_API_KEY, apiKey)
            .putString(KEY_DEVICE_KEY, deviceKey.ifBlank { getDeviceKey() })
        if (otp != null) {
            editor.putString(KEY_OTP, otp)
        }
        editor.apply()
        _settingsFlow.value = loadSettings()
    }

    fun updateLastSyncTimestamp(timestamp: Long = System.currentTimeMillis()) {
        prefs.edit().putLong(KEY_LAST_SYNC_TIME, timestamp).apply()
        _settingsFlow.value = loadSettings()
    }

    fun generateNewDeviceKey(): String {
        val newKey = "DEV-" + UUID.randomUUID().toString().take(8).uppercase()
        prefs.edit().putString(KEY_DEVICE_KEY, newKey).apply()
        _settingsFlow.value = loadSettings()
        return newKey
    }

    companion object {
        private const val PREFS_FILE = "piprapay_secure_prefs"
        private const val KEY_SERVER_BASE_URL = "key_server_base_url"
        private const val KEY_API_KEY = "key_api_key"
        private const val KEY_DEVICE_KEY = "key_device_key"
        private const val KEY_OTP = "key_otp"
        private const val KEY_DEVICE_NAME = "key_device_name"
        private const val KEY_SERVICE_ENABLED = "key_service_enabled"
        private const val KEY_LAST_SYNC_TIME = "key_last_sync_time"
        private const val KEY_AUTO_SYNC = "key_auto_sync"
        private const val KEY_ONBOARDING_COMPLETED = "key_onboarding_completed"

        const val DEFAULT_BASE_URL = "https://api.piprapay.com/"

        @Volatile
        private var instance: MerchantPreferences? = null

        fun getInstance(context: Context): MerchantPreferences {
            return instance ?: synchronized(this) {
                instance ?: MerchantPreferences(context.applicationContext).also { instance = it }
            }
        }

        private fun createEncryptedPreferences(context: Context): SharedPreferences {
            return try {
                val masterKey = MasterKey.Builder(context)
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                    .build()

                EncryptedSharedPreferences.create(
                    context,
                    PREFS_FILE,
                    masterKey,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
                )
            } catch (e: Exception) {
                Log.w("MerchantPreferences", "Falling back to standard SharedPreferences: ${e.message}")
                context.getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE)
            }
        }
    }
}

data class MerchantSettings(
    val serverBaseUrl: String = MerchantPreferences.DEFAULT_BASE_URL,
    val apiKey: String = "",
    val deviceKey: String = "",
    val otp: String = "",
    val deviceName: String = "",
    val deviceModel: String = "",
    val androidLevel: String = "",
    val appVersion: String = "1.0.0",
    val serviceEnabled: Boolean = true,
    val lastSyncTimestamp: Long = 0L,
    val autoSyncEnabled: Boolean = true,
    val onboardingCompleted: Boolean = false
)
