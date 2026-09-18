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

        val sendersRaw = if (prefs.contains(KEY_WHITELISTED_SENDERS)) {
            prefs.getString(KEY_WHITELISTED_SENDERS, "") ?: ""
        } else {
            DEFAULT_SENDERS.joinToString(",")
        }
        val sendersList = if (sendersRaw.isBlank()) emptyList() else sendersRaw.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        val allKnown = (DEFAULT_SENDERS + sendersList + getAllSyncedSendersInternal()).distinct()
        val gatewayGroups = resolveGatewayGroups(allKnown)

        return MerchantSettings(
            serverBaseUrl = prefs.getString(KEY_SERVER_BASE_URL, DEFAULT_BASE_URL) ?: DEFAULT_BASE_URL,
            apiKey = prefs.getString(KEY_API_KEY, "") ?: "",
            deviceKey = deviceKey,
            otp = prefs.getString(KEY_OTP, "") ?: "",
            sessionToken = prefs.getString(KEY_SESSION_TOKEN, "") ?: "",
            accountName = prefs.getString(KEY_ACCOUNT_NAME, "") ?: "",
            accountEmail = prefs.getString(KEY_ACCOUNT_EMAIL, "") ?: "",
            whitelistedSenders = sendersList,
            gatewayGroups = gatewayGroups,
            deviceName = prefs.getString(KEY_DEVICE_NAME, getDefaultDeviceName()) ?: getDefaultDeviceName(),
            deviceModel = android.os.Build.MODEL ?: "Android Device",
            androidLevel = "API ${android.os.Build.VERSION.SDK_INT} (Android ${android.os.Build.VERSION.RELEASE})",
            appVersion = "1.0.0",
            serviceEnabled = prefs.getBoolean(KEY_SERVICE_ENABLED, true),
            lastSyncTimestamp = prefs.getLong(KEY_LAST_SYNC_TIME, 0L),
            autoSyncEnabled = prefs.getBoolean(KEY_AUTO_SYNC, true),
            hapticEnabled = prefs.getBoolean(KEY_HAPTIC_ENABLED, true),
            audioToneEnabled = prefs.getBoolean(KEY_AUDIO_TONE_ENABLED, true),
            onboardingCompleted = prefs.getBoolean(KEY_ONBOARDING_COMPLETED, false),
            githubRepo = prefs.getString(KEY_GITHUB_REPO, DEFAULT_GITHUB_REPO) ?: DEFAULT_GITHUB_REPO
        )
    }

    private fun getDefaultDeviceName(): String {
        val manufacturer = android.os.Build.MANUFACTURER.orEmpty().replaceFirstChar { it.uppercase() }
        val model = android.os.Build.MODEL.orEmpty()
        return if (model.startsWith(manufacturer, ignoreCase = true)) model else "$manufacturer $model"
    }

    fun getOtp(): String = prefs.getString(KEY_OTP, "") ?: ""
    fun getSessionToken(): String = prefs.getString(KEY_SESSION_TOKEN, "") ?: ""
    fun getAccountName(): String = prefs.getString(KEY_ACCOUNT_NAME, "") ?: ""
    fun getAccountEmail(): String = prefs.getString(KEY_ACCOUNT_EMAIL, "") ?: ""
    fun getWhitelistedSenders(): List<String> {
        val raw = if (prefs.contains(KEY_WHITELISTED_SENDERS)) {
            prefs.getString(KEY_WHITELISTED_SENDERS, "") ?: ""
        } else {
            DEFAULT_SENDERS.joinToString(",")
        }
        return if (raw.isBlank()) emptyList() else raw.split(",").map { it.trim() }.filter { it.isNotEmpty() }
    }

    private fun getAllSyncedSendersInternal(): List<String> {
        val raw = prefs.getString(KEY_ALL_SYNCED_SENDERS, "") ?: ""
        return if (raw.isBlank()) emptyList() else raw.split(",").map { it.trim() }.filter { it.isNotEmpty() }
    }

    fun getAllSyncedSenders(): List<String> {
        val raw = prefs.getString(KEY_ALL_SYNCED_SENDERS, "") ?: ""
        val list = if (raw.isBlank()) emptyList() else raw.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        return (DEFAULT_SENDERS + list + getWhitelistedSenders()).distinct()
    }

    fun getGatewayGroups(): List<MfsSenderConfig> {
        return resolveGatewayGroups(getAllSyncedSenders())
    }

    fun calculateSyncResult(syncedSenders: List<String>): SyncSendersResult {
        val cleanSenders = syncedSenders.map { it.trim() }.filter { it.isNotEmpty() }.distinct()
        val groups = resolveGatewayGroups(cleanSenders)
        val gatewayCount = groups.size
        val aliasCount = cleanSenders.size
        val message = if (gatewayCount != aliasCount && aliasCount > 0) {
            "Synced $gatewayCount payment gateways ($aliasCount sender aliases)"
        } else {
            "Synced $gatewayCount senders from panel"
        }
        return SyncSendersResult(
            gatewayCount = gatewayCount,
            aliasCount = aliasCount,
            message = message
        )
    }

    fun setWhitelistedSenders(senders: List<String>) {
        val cleanSenders = senders.map { it.trim() }.filter { it.isNotEmpty() }.distinct()
        val allSynced = (getAllSyncedSendersInternal() + cleanSenders).distinct()
        prefs.edit()
            .putString(KEY_ALL_SYNCED_SENDERS, allSynced.joinToString(","))
            .putString(KEY_WHITELISTED_SENDERS, cleanSenders.joinToString(","))
            .apply()
        _settingsFlow.value = loadSettings()
    }

    fun isSenderEnabled(senderId: String): Boolean {
        val senders = getWhitelistedSenders()
        val matchingConfig = getGatewayGroups().firstOrNull { it.id.equals(senderId, ignoreCase = true) }
        return if (matchingConfig != null) {
            senders.any { s ->
                s.equals(matchingConfig.id, ignoreCase = true) ||
                s.equals(matchingConfig.primaryAlias, ignoreCase = true) ||
                matchingConfig.allAliases.any { it.equals(s, ignoreCase = true) }
            }
        } else {
            senders.any { it.equals(senderId, ignoreCase = true) }
        }
    }

    fun toggleSender(senderId: String, enable: Boolean) {
        val current = getWhitelistedSenders().toMutableList()
        val matchingConfig = getGatewayGroups().firstOrNull { it.id.equals(senderId, ignoreCase = true) }
        val keysToRemove = matchingConfig?.let { listOf(it.id, it.primaryAlias) + it.allAliases } ?: listOf(senderId)

        current.removeAll { key -> keysToRemove.any { it.equals(key, ignoreCase = true) } }
        if (enable) {
            val keysToAdd = matchingConfig?.allAliases?.ifEmpty { listOf(matchingConfig.primaryAlias) } ?: listOf(senderId)
            current.addAll(keysToAdd)
        }
        val distinctCurrent = current.distinct()
        prefs.edit().putString(KEY_WHITELISTED_SENDERS, distinctCurrent.joinToString(",")).apply()
        _settingsFlow.value = loadSettings()
    }

    fun isSenderAllowed(originatingAddress: String?): Boolean {
        if (originatingAddress.isNullOrBlank()) return false
        val addr = originatingAddress.lowercase().trim()
        val activeSenders = getWhitelistedSenders()
        if (activeSenders.isEmpty()) return false

        for (config in getGatewayGroups()) {
            if (isSenderEnabled(config.id)) {
                if (config.allAliases.any { alias ->
                    val a = alias.lowercase().trim()
                    addr == a || addr.contains(a) || a.contains(addr)
                }) {
                    return true
                }
            }
        }

        // Also check any direct senders in active whitelist
        for (sender in activeSenders) {
            val s = sender.lowercase().trim()
            if (addr == s || addr.contains(s) || s.contains(addr)) {
                return true
            }
        }
        return false
    }

    fun getDeviceName(): String = prefs.getString(KEY_DEVICE_NAME, getDefaultDeviceName()) ?: getDefaultDeviceName()
    fun getDeviceModel(): String = android.os.Build.MODEL ?: "Android"
    fun getAndroidLevel(): String = "API ${android.os.Build.VERSION.SDK_INT}"

    fun saveCompanionSession(
        token: String,
        accountName: String? = null,
        accountEmail: String? = null,
        senders: List<String>? = null
    ) {
        val editor = prefs.edit().putString(KEY_SESSION_TOKEN, token)
        if (accountName != null) editor.putString(KEY_ACCOUNT_NAME, accountName)
        if (accountEmail != null) editor.putString(KEY_ACCOUNT_EMAIL, accountEmail)
        if (senders != null) {
            val cleanSenders = senders.map { it.trim() }.filter { it.isNotEmpty() }.distinct()
            val allSynced = (getAllSyncedSendersInternal() + cleanSenders).distinct()
            editor.putString(KEY_ALL_SYNCED_SENDERS, allSynced.joinToString(","))
            editor.putString(KEY_WHITELISTED_SENDERS, cleanSenders.joinToString(","))
        }
        editor.apply()
        _settingsFlow.value = loadSettings()
    }

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
        otp: String = "",
        sessionToken: String = ""
    ) {
        val sanitizedUrl = if (serverBaseUrl.endsWith("/")) serverBaseUrl else "$serverBaseUrl/"
        val editor = prefs.edit()
            .putString(KEY_SERVER_BASE_URL, sanitizedUrl)
            .putString(KEY_API_KEY, apiKey)
            .putString(KEY_DEVICE_KEY, deviceKey.ifBlank { getDeviceKey() })
            .putString(KEY_OTP, otp)
            .putBoolean(KEY_SERVICE_ENABLED, true)
            .putBoolean(KEY_ONBOARDING_COMPLETED, true)
        if (sessionToken.isNotBlank()) {
            editor.putString(KEY_SESSION_TOKEN, sessionToken)
        }
        editor.apply()
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

    fun isHapticEnabled(): Boolean {
        return prefs.getBoolean(KEY_HAPTIC_ENABLED, true)
    }

    fun setHapticEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_HAPTIC_ENABLED, enabled).apply()
        _settingsFlow.value = loadSettings()
    }

    fun isAudioToneEnabled(): Boolean {
        return prefs.getBoolean(KEY_AUDIO_TONE_ENABLED, true)
    }

    fun setAudioToneEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_AUDIO_TONE_ENABLED, enabled).apply()
        _settingsFlow.value = loadSettings()
    }

    fun getGithubRepo(): String {
        return prefs.getString(KEY_GITHUB_REPO, DEFAULT_GITHUB_REPO) ?: DEFAULT_GITHUB_REPO
    }

    fun updateGithubRepo(repo: String) {
        prefs.edit().putString(KEY_GITHUB_REPO, repo.trim()).apply()
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
        private const val KEY_SESSION_TOKEN = "key_session_token"
        private const val KEY_ACCOUNT_NAME = "key_account_name"
        private const val KEY_ACCOUNT_EMAIL = "key_account_email"
        private const val KEY_WHITELISTED_SENDERS = "key_whitelisted_senders"
        private const val KEY_ALL_SYNCED_SENDERS = "key_all_synced_senders"
        private const val KEY_DEVICE_NAME = "key_device_name"
        private const val KEY_SERVICE_ENABLED = "key_service_enabled"
        private const val KEY_LAST_SYNC_TIME = "key_last_sync_time"
        private const val KEY_AUTO_SYNC = "key_auto_sync"
        private const val KEY_HAPTIC_ENABLED = "key_haptic_enabled"
        private const val KEY_AUDIO_TONE_ENABLED = "key_audio_tone_enabled"
        private const val KEY_ONBOARDING_COMPLETED = "key_onboarding_completed"
        private const val KEY_GITHUB_REPO = "key_github_repo"

        const val DEFAULT_BASE_URL = "https://pay.emon.bd/"
        const val DEFAULT_GITHUB_REPO = "plusemon/piprapay-connect"

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
    // LinkedHashMap preserves order: standard gateways first, then dynamic ones
    val gatewayMap = linkedMapOf<String, Triple<String, String, MutableSet<String>>>()

    // Initialize with standard gateways
    for (base in DEFAULT_BASE_GATEWAYS) {
        gatewayMap[base.id] = Triple(base.displayName, base.primaryAlias, base.allAliases.toMutableSet())
    }

    // Classify all known senders
    for (sender in knownSenders) {
        val trimmed = sender.trim()
        if (trimmed.isEmpty()) continue
        val lower = trimmed.lowercase()

        val matchedBaseId: String? = when {
            lower == "bkash" || lower.contains("bkash") -> "bkash"
            lower == "nagad" || lower.contains("nagad") -> "nagad"
            lower == "rocket" || lower == "16216" || lower.contains("rocket") || lower.contains("dbbl") -> "rocket"
            lower == "upay" || lower.contains("upay") -> "upay"
            lower == "tap" || lower == "tap." || lower.startsWith("tap") -> "tap"
            lower == "ibbl" || lower.contains("ibbl") || lower.contains("islami") -> "ibbl"
            lower == "pathaopay" || lower.contains("pathao") -> "pathaopay"
            lower == "telecash" || lower.contains("telecash") -> "telecash"
            lower == "surecash" || lower.contains("surecash") -> "surecash"
            lower == "okwallet" || lower.contains("okwallet") -> "okwallet"
            else -> null
        }

        if (matchedBaseId != null) {
            val existing = gatewayMap[matchedBaseId]
            if (existing != null) {
                existing.third.add(trimmed)
            } else {
                val displayName = when (matchedBaseId) {
                    "pathaopay" -> "Pathao Pay"
                    "telecash" -> "TeleCash"
                    "surecash" -> "SureCash"
                    "okwallet" -> "OK Wallet"
                    else -> trimmed.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
                }
                gatewayMap[matchedBaseId] = Triple(displayName, trimmed, mutableSetOf(trimmed))
            }
        } else {
            // Dynamic custom sender rule (e.g. shortcodes like "01847-348685")
            val id = lower
            val existing = gatewayMap[id]
            if (existing != null) {
                existing.third.add(trimmed)
            } else {
                val displayName = if (trimmed.all { it.isDigit() || it == '-' || it == '+' }) {
                    trimmed
                } else {
                    trimmed.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
                }
                gatewayMap[id] = Triple(displayName, trimmed, mutableSetOf(trimmed))
            }
        }
    }

    return gatewayMap.map { (id, triple) ->
        MfsSenderConfig(
            id = id,
            displayName = triple.first,
            primaryAlias = triple.second,
            allAliases = triple.third.toList()
        )
    }
}

data class MerchantSettings(
    val serverBaseUrl: String = MerchantPreferences.DEFAULT_BASE_URL,
    val apiKey: String = "",
    val deviceKey: String = "",
    val otp: String = "",
    val sessionToken: String = "",
    val accountName: String = "",
    val accountEmail: String = "",
    val whitelistedSenders: List<String> = DEFAULT_SENDERS,
    val gatewayGroups: List<MfsSenderConfig> = DEFAULT_BASE_GATEWAYS,
    val deviceName: String = "",
    val deviceModel: String = "",
    val androidLevel: String = "",
    val appVersion: String = "1.0.0",
    val serviceEnabled: Boolean = true,
    val lastSyncTimestamp: Long = 0L,
    val autoSyncEnabled: Boolean = true,
    val hapticEnabled: Boolean = true,
    val audioToneEnabled: Boolean = true,
    val onboardingCompleted: Boolean = false,
    val githubRepo: String = MerchantPreferences.DEFAULT_GITHUB_REPO
)
