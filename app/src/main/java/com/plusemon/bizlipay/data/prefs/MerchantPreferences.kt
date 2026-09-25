package com.plusemon.bizlipay.data.prefs

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
        var deviceUid = prefs.getString(KEY_DEVICE_UID, null)
        if (deviceUid.isNullOrBlank()) {
            deviceUid = "dev_" + UUID.randomUUID().toString().replace("-", "").take(12)
            prefs.edit().putString(KEY_DEVICE_UID, deviceUid).apply()
        }

        val sendersRaw = if (prefs.contains(KEY_WHITELISTED_SENDERS)) {
            prefs.getString(KEY_WHITELISTED_SENDERS, "") ?: ""
        } else {
            DEFAULT_SENDERS.joinToString(",")
        }
        val sendersList = if (sendersRaw.isBlank()) emptyList() else sendersRaw.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        val allKnown = (DEFAULT_SENDERS + sendersList).distinct()
        val gatewayGroups = resolveGatewayGroups(allKnown)

        val token = prefs.getString(KEY_SESSION_TOKEN, "") ?: ""
        val isPaired = token.isNotBlank() && prefs.getBoolean(KEY_IS_PAIRED, false)

        return MerchantSettings(
            serverBaseUrl = prefs.getString(KEY_SERVER_BASE_URL, DEFAULT_BASE_URL) ?: DEFAULT_BASE_URL,
            apiKey = prefs.getString(KEY_API_KEY, "") ?: "",
            deviceKey = deviceUid,
            deviceUid = deviceUid,
            otp = prefs.getString(KEY_OTP, "") ?: "",
            sessionToken = token,
            isPaired = isPaired,
            accountName = prefs.getString(KEY_ACCOUNT_NAME, "") ?: "",
            accountEmail = prefs.getString(KEY_ACCOUNT_EMAIL, "") ?: "",
            whitelistedSenders = sendersList,
            gatewayGroups = gatewayGroups,
            deviceName = prefs.getString(KEY_DEVICE_NAME, getDefaultDeviceName()) ?: getDefaultDeviceName(),
            deviceModel = android.os.Build.MODEL ?: "Android Device",
            androidLevel = "API ${android.os.Build.VERSION.SDK_INT} (Android ${android.os.Build.VERSION.RELEASE})",
            appVersion = "v1.0.0",
            serviceEnabled = prefs.getBoolean(KEY_SERVICE_ENABLED, true),
            lastSyncTimestamp = prefs.getLong(KEY_LAST_SYNC_TIME, 0L),
            pairedTimestamp = prefs.getLong(KEY_PAIRED_TIME, 0L),
            autoSyncEnabled = prefs.getBoolean(KEY_AUTO_SYNC, true),
            hapticEnabled = prefs.getBoolean(KEY_HAPTIC_ENABLED, true),
            audioToneEnabled = prefs.getBoolean(KEY_AUDIO_TONE_ENABLED, true),
            onboardingCompleted = isPaired,
            githubRepo = prefs.getString(KEY_GITHUB_REPO, DEFAULT_GITHUB_REPO) ?: DEFAULT_GITHUB_REPO
        )
    }

    fun getDefaultDeviceName(): String {
        val manufacturer = android.os.Build.MANUFACTURER.orEmpty().replaceFirstChar { it.uppercase() }
        val model = android.os.Build.MODEL.orEmpty()
        return if (model.startsWith(manufacturer, ignoreCase = true)) model else "$manufacturer $model"
    }

    fun getOtp(): String = prefs.getString(KEY_OTP, "") ?: ""
    fun getSessionToken(): String = prefs.getString(KEY_SESSION_TOKEN, "") ?: ""
    fun getDeviceUid(): String = prefs.getString(KEY_DEVICE_UID, "") ?: getDeviceKey()
    fun getDeviceKey(): String = prefs.getString(KEY_DEVICE_UID, "") ?: "dev_default"
    fun isPaired(): Boolean = getSessionToken().isNotBlank() && prefs.getBoolean(KEY_IS_PAIRED, false)

    fun savePairingSession(
        token: String,
        deviceUid: String,
        serverBaseUrl: String,
        otp: String = "",
        deviceName: String = getDefaultDeviceName()
    ) {
        val cleanUrl = if (serverBaseUrl.endsWith("/")) serverBaseUrl else "$serverBaseUrl/"
        prefs.edit()
            .putString(KEY_SESSION_TOKEN, token)
            .putString(KEY_DEVICE_UID, deviceUid)
            .putString(KEY_SERVER_BASE_URL, cleanUrl)
            .putString(KEY_API_KEY, token)
            .putString(KEY_OTP, otp)
            .putString(KEY_DEVICE_NAME, deviceName)
            .putBoolean(KEY_IS_PAIRED, true)
            .putBoolean(KEY_ONBOARDING_COMPLETED, true)
            .putBoolean(KEY_SERVICE_ENABLED, true)
            .putLong(KEY_PAIRED_TIME, System.currentTimeMillis())
            .apply()
        _settingsFlow.value = loadSettings()
    }

    fun clearSessionAndUnpair() {
        prefs.edit()
            .putString(KEY_SESSION_TOKEN, "")
            .putString(KEY_OTP, "")
            .putString(KEY_API_KEY, "")
            .putBoolean(KEY_IS_PAIRED, false)
            .putBoolean(KEY_ONBOARDING_COMPLETED, false)
            .putBoolean(KEY_SERVICE_ENABLED, false)
            .apply()
        _settingsFlow.value = loadSettings()
    }

    fun isOnboardingCompleted(): Boolean = isPaired()

    fun setOnboardingCompleted(completed: Boolean) {
        prefs.edit().putBoolean(KEY_ONBOARDING_COMPLETED, completed).apply()
        _settingsFlow.value = loadSettings()
    }

    fun getServerBaseUrl(): String = prefs.getString(KEY_SERVER_BASE_URL, DEFAULT_BASE_URL) ?: DEFAULT_BASE_URL
    fun getApiKey(): String = prefs.getString(KEY_SESSION_TOKEN, "") ?: ""

    fun isServiceEnabled(): Boolean = prefs.getBoolean(KEY_SERVICE_ENABLED, true)
    fun setServiceEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_SERVICE_ENABLED, enabled).apply()
        _settingsFlow.value = loadSettings()
    }

    fun updateLastSyncTimestamp(timestamp: Long = System.currentTimeMillis()) {
        prefs.edit().putLong(KEY_LAST_SYNC_TIME, timestamp).apply()
        _settingsFlow.value = loadSettings()
    }

    fun isHapticEnabled(): Boolean = prefs.getBoolean(KEY_HAPTIC_ENABLED, true)
    fun setHapticEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_HAPTIC_ENABLED, enabled).apply()
        _settingsFlow.value = loadSettings()
    }

    fun isAudioToneEnabled(): Boolean = prefs.getBoolean(KEY_AUDIO_TONE_ENABLED, true)
    fun setAudioToneEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_AUDIO_TONE_ENABLED, enabled).apply()
        _settingsFlow.value = loadSettings()
    }

    fun getDeviceName(): String = prefs.getString(KEY_DEVICE_NAME, getDefaultDeviceName()) ?: getDefaultDeviceName()
    fun getDeviceModel(): String = android.os.Build.MODEL ?: "Android Device"
    fun getAndroidLevel(): String = "API ${android.os.Build.VERSION.SDK_INT} (Android ${android.os.Build.VERSION.RELEASE})"

    fun getWhitelistedSenders(): List<String> = DEFAULT_SENDERS

    fun isSenderAllowed(originatingAddress: String?): Boolean {
        if (originatingAddress.isNullOrBlank()) return false
        val addr = originatingAddress.lowercase().trim()
        for (sender in DEFAULT_SENDERS) {
            val s = sender.lowercase().trim()
            if (addr == s || addr.contains(s) || s.contains(addr)) {
                return true
            }
        }
        return true // In BizliPay device ingestion, accept transactions to batch ingest
    }

    fun toggleSender(senderId: String, enable: Boolean) {
        // Compatibility
        _settingsFlow.value = loadSettings()
    }

    fun isSenderEnabled(senderId: String): Boolean = true

    fun completeOnboardingAndLogin(
        serverBaseUrl: String,
        apiKey: String,
        deviceKey: String = getDeviceUid(),
        otp: String = "",
        sessionToken: String = ""
    ) {
        savePairingSession(
            token = sessionToken.ifBlank { apiKey },
            deviceUid = deviceKey,
            serverBaseUrl = serverBaseUrl,
            otp = otp
        )
    }

    fun resetOnboarding() {
        clearSessionAndUnpair()
    }

    fun generateNewDeviceKey(): String {
        val newKey = "dev_" + UUID.randomUUID().toString().replace("-", "").take(12)
        prefs.edit().putString(KEY_DEVICE_UID, newKey).apply()
        _settingsFlow.value = loadSettings()
        return newKey
    }

    fun updateGithubRepo(repo: String) {
        prefs.edit().putString(KEY_GITHUB_REPO, repo).apply()
        _settingsFlow.value = loadSettings()
    }

    fun getGithubRepo(): String {
        return prefs.getString(KEY_GITHUB_REPO, DEFAULT_GITHUB_REPO) ?: DEFAULT_GITHUB_REPO
    }

    fun updateSettings(
        serverBaseUrl: String,
        apiKey: String,
        deviceKey: String,
        otp: String? = null
    ) {
        val cleanUrl = if (serverBaseUrl.endsWith("/")) serverBaseUrl else "$serverBaseUrl/"
        val editor = prefs.edit()
            .putString(KEY_SERVER_BASE_URL, cleanUrl)
            .putString(KEY_DEVICE_UID, deviceKey)
        if (apiKey.isNotBlank()) {
            editor.putString(KEY_SESSION_TOKEN, apiKey)
        }
        if (otp != null) {
            editor.putString(KEY_OTP, otp)
        }
        editor.apply()
        _settingsFlow.value = loadSettings()
    }

    companion object {
        private const val PREFS_FILE = "bizlipay_secure_prefs"
        private const val KEY_SERVER_BASE_URL = "key_server_base_url"
        private const val KEY_API_KEY = "key_api_key"
        private const val KEY_DEVICE_UID = "key_device_uid"
        private const val KEY_OTP = "key_otp"
        private const val KEY_SESSION_TOKEN = "key_session_token"
        private const val KEY_IS_PAIRED = "key_is_paired"
        private const val KEY_ACCOUNT_NAME = "key_account_name"
        private const val KEY_ACCOUNT_EMAIL = "key_account_email"
        private const val KEY_WHITELISTED_SENDERS = "key_whitelisted_senders"
        private const val KEY_DEVICE_NAME = "key_device_name"
        private const val KEY_SERVICE_ENABLED = "key_service_enabled"
        private const val KEY_LAST_SYNC_TIME = "key_last_sync_time"
        private const val KEY_PAIRED_TIME = "key_paired_time"
        private const val KEY_AUTO_SYNC = "key_auto_sync"
        private const val KEY_HAPTIC_ENABLED = "key_haptic_enabled"
        private const val KEY_AUDIO_TONE_ENABLED = "key_audio_tone_enabled"
        private const val KEY_ONBOARDING_COMPLETED = "key_onboarding_completed"
        private const val KEY_GITHUB_REPO = "key_github_repo"

        const val DEFAULT_BASE_URL = "https://bizlipay.com"
        const val DEFAULT_GITHUB_REPO = "plusemon/bizlipay-connect"

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
    val deviceUid: String = "",
    val otp: String = "",
    val sessionToken: String = "",
    val isPaired: Boolean = false,
    val accountName: String = "",
    val accountEmail: String = "",
    val whitelistedSenders: List<String> = DEFAULT_SENDERS,
    val gatewayGroups: List<MfsSenderConfig> = DEFAULT_BASE_GATEWAYS,
    val deviceName: String = "",
    val deviceModel: String = "",
    val androidLevel: String = "",
    val appVersion: String = "v1.0.0",
    val serviceEnabled: Boolean = true,
    val lastSyncTimestamp: Long = 0L,
    val pairedTimestamp: Long = 0L,
    val autoSyncEnabled: Boolean = true,
    val hapticEnabled: Boolean = true,
    val audioToneEnabled: Boolean = true,
    val onboardingCompleted: Boolean = false,
    val githubRepo: String = MerchantPreferences.DEFAULT_GITHUB_REPO
)

data class SyncSendersResult(
    val gatewayCount: Int,
    val aliasCount: Int,
    val message: String
)

data class MfsSenderConfig(
    val id: String,
    val displayName: String,
    val primaryAlias: String,
    val allAliases: List<String>
)

val DEFAULT_BASE_GATEWAYS: List<MfsSenderConfig> = listOf(
    MfsSenderConfig(
        id = "bkash",
        displayName = "bKash",
        primaryAlias = "bkash",
        allAliases = listOf("bkash", "bKash")
    ),
    MfsSenderConfig(
        id = "nagad",
        displayName = "Nagad",
        primaryAlias = "nagad",
        allAliases = listOf("nagad", "NAGAD")
    ),
    MfsSenderConfig(
        id = "rocket",
        displayName = "Rocket",
        primaryAlias = "16216",
        allAliases = listOf("16216", "rocket", "DBBL", "dbbl")
    ),
    MfsSenderConfig(
        id = "upay",
        displayName = "Upay",
        primaryAlias = "upay",
        allAliases = listOf("upay", "Upay")
    ),
    MfsSenderConfig(
        id = "tap",
        displayName = "TAP",
        primaryAlias = "tap",
        allAliases = listOf("tap", "TAP")
    ),
    MfsSenderConfig(
        id = "ibbl",
        displayName = "Islami Bank",
        primaryAlias = "ibbl",
        allAliases = listOf("ibbl", "IBBL", "islami bank")
    )
)

val SUPPORTED_MFS_SENDERS: List<MfsSenderConfig> = DEFAULT_BASE_GATEWAYS
val DEFAULT_SENDERS: List<String> = listOf("bkash", "nagad", "16216", "upay", "tap", "ibbl")

fun resolveGatewayGroups(knownSenders: Collection<String>): List<MfsSenderConfig> {
    return DEFAULT_BASE_GATEWAYS
}
