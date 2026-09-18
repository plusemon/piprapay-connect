package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.api.HandshakeVerificationResponse
import com.example.data.model.TransactionEntity
import com.example.data.prefs.MerchantPreferences
import com.example.data.prefs.MerchantSettings
import com.example.data.repository.ConnectionTestResult
import com.example.data.repository.TransactionRepository
import com.example.parser.MfsSmsParser
import com.example.service.PipraPayService
import com.example.util.AlertManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class DashboardStats(
    val totalCount: Int = 0,
    val syncedCount: Int = 0,
    val pendingCount: Int = 0,
    val failedCount: Int = 0,
    val totalAmount: Double = 0.0
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = TransactionRepository(application)
    private val prefs = MerchantPreferences.getInstance(application)

    val settings: StateFlow<MerchantSettings> = repository.settingsFlow

    // Tracks latest verified wallet balance per provider (matching PipraPay pp_balance_verification)
    val latestBalances: StateFlow<Map<String, Double>> = repository.allTransactions.map { list ->
        val balanceMap = mutableMapOf<String, Double>()
        list.filter { it.balance != null }
            .sortedBy { it.timestamp }
            .forEach { trx ->
                balanceMap[trx.provider.uppercase()] = trx.balance ?: 0.0
            }
        balanceMap
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyMap()
    )

    private val _isServiceRunning = MutableStateFlow(prefs.isServiceEnabled())
    val isServiceRunning: StateFlow<Boolean> = _isServiceRunning.asStateFlow()

    private val _isBatteryOptimizationIgnored = MutableStateFlow(
        PipraPayService.isBatteryOptimizationIgnored(application)
    )
    val isBatteryOptimizationIgnored: StateFlow<Boolean> = _isBatteryOptimizationIgnored.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedProviderFilter = MutableStateFlow("ALL")
    val selectedProviderFilter: StateFlow<String> = _selectedProviderFilter.asStateFlow()

    private val _selectedStatusFilter = MutableStateFlow("ALL")
    val selectedStatusFilter: StateFlow<String> = _selectedStatusFilter.asStateFlow()

    private val _connectionTestState = MutableStateFlow<ConnectionTestState>(ConnectionTestState.Idle)
    val connectionTestState: StateFlow<ConnectionTestState> = _connectionTestState.asStateFlow()

    private val _loginState = MutableStateFlow<LoginState>(LoginState.Idle)
    val loginState: StateFlow<LoginState> = _loginState.asStateFlow()

    private val _settingsSaveState = MutableStateFlow<SettingsSaveState>(SettingsSaveState.Idle)
    val settingsSaveState: StateFlow<SettingsSaveState> = _settingsSaveState.asStateFlow()

    private val _qrVerificationState = MutableStateFlow<QrVerificationState>(QrVerificationState.Idle)
    val qrVerificationState: StateFlow<QrVerificationState> = _qrVerificationState.asStateFlow()

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private val _serverLatency = MutableStateFlow<Long?>(42L)
    val serverLatency: StateFlow<Long?> = _serverLatency.asStateFlow()

    private val _serverHealthOk = MutableStateFlow(true)
    val serverHealthOk: StateFlow<Boolean> = _serverHealthOk.asStateFlow()

    private val _accountInfo = MutableStateFlow<com.example.data.api.CompanionAccountInfo?>(null)
    val accountInfo: StateFlow<com.example.data.api.CompanionAccountInfo?> = _accountInfo.asStateFlow()

    init {
        pingServerHealth()
        viewModelScope.launch {
            if (prefs.getSessionToken().isNotBlank()) {
                val info = repository.refreshAccountInfo()
                _accountInfo.value = info
                repository.refreshWhitelistedSenders()
            }
        }
    }

    val rawTransactions = repository.allTransactions
    val recentTransactions: StateFlow<List<TransactionEntity>> = repository.recentTransactions.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Filtered transactions for UI list
    val filteredTransactions: StateFlow<List<TransactionEntity>> = combine(
        repository.allTransactions,
        _searchQuery,
        _selectedProviderFilter,
        _selectedStatusFilter
    ) { transactions, query, provider, status ->
        val trimmedQuery = query.trim()
        transactions.filter { trx ->
            val matchesQuery = trimmedQuery.isBlank() ||
                trx.trxId.contains(trimmedQuery, ignoreCase = true) ||
                trx.senderNumber.contains(trimmedQuery, ignoreCase = true) ||
                trx.rawMessage.contains(trimmedQuery, ignoreCase = true) ||
                trx.amount.toString().contains(trimmedQuery, ignoreCase = true) ||
                String.format(java.util.Locale.US, "%.2f", trx.amount).contains(trimmedQuery) ||
                java.text.DecimalFormat("#,##0.00").format(trx.amount).contains(trimmedQuery)

            val matchesProvider = provider == "ALL" || trx.provider.equals(provider, ignoreCase = true)
            val matchesStatus = status == "ALL" || trx.syncStatus.equals(status, ignoreCase = true)

            matchesQuery && matchesProvider && matchesStatus
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Dynamic stats computation
    val stats: StateFlow<DashboardStats> = repository.allTransactions.combine(MutableStateFlow(Unit)) { trxs, _ ->
        val total = trxs.size
        val synced = trxs.count { it.syncStatus == "SYNCED" }
        val pending = trxs.count { it.syncStatus == "PENDING" }
        val failed = trxs.count { it.syncStatus == "FAILED" }
        val amount = trxs.sumOf { it.amount }
        DashboardStats(total, synced, pending, failed, amount)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = DashboardStats()
    )

    fun refreshBatteryOptimizationStatus() {
        _isBatteryOptimizationIgnored.value = PipraPayService.isBatteryOptimizationIgnored(getApplication())
    }

    fun toggleService(enable: Boolean) {
        val app = getApplication<Application>()
        if (enable) {
            PipraPayService.start(app)
            _isServiceRunning.value = true
        } else {
            PipraPayService.stop(app)
            _isServiceRunning.value = false
        }
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setProviderFilter(provider: String) {
        _selectedProviderFilter.value = provider
    }

    fun setStatusFilter(status: String) {
        _selectedStatusFilter.value = status
    }

    fun triggerManualSync() {
        viewModelScope.launch {
            _isSyncing.value = true
            repository.triggerManualSync()
            kotlinx.coroutines.delay(1200)
            _isSyncing.value = false
        }
    }

    fun resyncTransaction(trxId: String) {
        viewModelScope.launch {
            repository.resyncSingleTransaction(trxId)
        }
    }

    fun deleteTransaction(trxId: String) {
        viewModelScope.launch {
            repository.deleteTransaction(trxId)
        }
    }

    fun clearAllTransactions() {
        viewModelScope.launch {
            repository.clearAll()
        }
    }

    fun updateSettings(url: String, apiKey: String, deviceKey: String, otp: String? = null) {
        viewModelScope.launch {
            repository.updateSettings(url, apiKey, deviceKey, otp)
        }
    }

    fun generateNewDeviceKey(): String {
        return repository.generateNewDeviceKey()
    }

    fun pingServerHealth() {
        viewModelScope.launch {
            val currentSettings = settings.value
            val result = repository.testConnection(
                currentSettings.serverBaseUrl,
                currentSettings.apiKey
            )
            _serverLatency.value = if (result.latencyMs > 0) result.latencyMs else null
            _serverHealthOk.value = result.isSuccess
        }
    }

    fun testConnection(url: String, apiKey: String) {
        viewModelScope.launch {
            _connectionTestState.value = ConnectionTestState.Testing
            val result = repository.testConnection(url, apiKey)
            _serverLatency.value = if (result.latencyMs > 0) result.latencyMs else null
            _serverHealthOk.value = result.isSuccess
            _connectionTestState.value = ConnectionTestState.Finished(result)
        }
    }

    fun resetConnectionTestState() {
        _connectionTestState.value = ConnectionTestState.Idle
    }

    fun resetLoginState() {
        _loginState.value = LoginState.Idle
    }

    fun resetSettingsSaveState() {
        _settingsSaveState.value = SettingsSaveState.Idle
    }

    fun resetQrVerificationState() {
        _qrVerificationState.value = QrVerificationState.Idle
    }

    fun setLoginError(message: String) {
        _loginState.value = LoginState.Error(message)
    }

    fun refreshCompanionData() {
        viewModelScope.launch {
            _isSyncing.value = true
            val info = repository.refreshAccountInfo()
            _accountInfo.value = info
            repository.refreshWhitelistedSenders()
            _isSyncing.value = false
        }
    }

    fun loginToPanel(
        panelUrl: String,
        passwordOrToken: String,
        deviceKey: String? = null,
        otp: String = "",
        onSuccess: () -> Unit
    ) {
        val trimmedUrl = panelUrl.trim()
        val trimmedPassword = passwordOrToken.trim()

        if (trimmedUrl.isBlank()) {
            _loginState.value = LoginState.Error("Endpoint not found: Check your Payment Panel URL.")
            return
        }

        if (trimmedPassword.isBlank()) {
            _loginState.value = LoginState.Error("Authentication failed: Invalid Merchant API Key.")
            return
        }

        viewModelScope.launch {
            _loginState.value = LoginState.Loading
            val assignedDeviceKey = deviceKey?.ifBlank { null } ?: prefs.getDeviceKey()
            val effectiveOtp = otp.ifBlank { trimmedPassword }

            // 1. Strict Handshake Verification (8s timeout, /api/v1/companion/verify or /ping)
            val handshake = repository.verifyHandshake(
                serverUrl = trimmedUrl,
                apiKey = trimmedPassword,
                deviceId = assignedDeviceKey
            )

            if (!handshake.isSuccess) {
                // If direct verify returned non-2xx, try companion OTP login if appropriate
                val companionResp = try {
                    repository.companionLogin(
                        url = trimmedUrl,
                        otp = effectiveOtp,
                        deviceKey = assignedDeviceKey
                    )
                } catch (_: Exception) { null }

                if (companionResp == null || !companionResp.success) {
                    val errorMsg = handshake.errorMessage ?: "Authentication failed: Invalid Merchant API Key."
                    _loginState.value = LoginState.Error(errorMsg)
                    return@launch
                }
            }

            // 2. Success State (HTTP 200)
            // Save credentials to Encrypted Vault
            repository.completeOnboardingAndLogin(
                url = trimmedUrl,
                apiKey = trimmedPassword,
                deviceKey = assignedDeviceKey,
                otp = effectiveOtp
            )
            prefs.setServiceEnabled(true)
            prefs.setOnboardingCompleted(true)

            // Set active connection flag & Start foreground service
            val app = getApplication<Application>()
            com.example.service.PipraPayForegroundService.start(app)
            _isServiceRunning.value = true

            // Trigger short success haptic pulse
            AlertManager.triggerHapticPulse(app)

            _loginState.value = LoginState.Success("Panel connected successfully!")
            onSuccess()

            // Update latency/status in background
            try {
                _serverLatency.value = handshake.latencyMs.takeIf { it > 0 } ?: 42L
                _serverHealthOk.value = true
                val info = repository.refreshAccountInfo()
                if (info != null) {
                    _accountInfo.value = info
                }
            } catch (_: Exception) {}
        }
    }

    fun verifyAndSaveSettings(
        url: String,
        apiKey: String,
        deviceKey: String,
        onSuccess: () -> Unit
    ) {
        val trimmedUrl = url.trim()
        val trimmedKey = apiKey.trim()
        val trimmedDevice = deviceKey.trim().ifBlank { prefs.getDeviceKey() }

        if (trimmedUrl.isBlank()) {
            _settingsSaveState.value = SettingsSaveState.Error("Endpoint not found: Check your Payment Panel URL.")
            return
        }

        if (trimmedKey.isBlank()) {
            _settingsSaveState.value = SettingsSaveState.Error("Authentication failed: Invalid Merchant API Key.")
            return
        }

        viewModelScope.launch {
            _settingsSaveState.value = SettingsSaveState.Loading

            val handshake = repository.verifyHandshake(
                serverUrl = trimmedUrl,
                apiKey = trimmedKey,
                deviceId = trimmedDevice
            )

            if (!handshake.isSuccess) {
                val errorMsg = handshake.errorMessage ?: "Authentication failed: Invalid Merchant API Key."
                _settingsSaveState.value = SettingsSaveState.Error(errorMsg)
                return@launch
            }

            // Success State (HTTP 200)
            // Save credentials to Encrypted Vault
            repository.updateSettings(trimmedUrl, trimmedKey, trimmedDevice)
            prefs.setServiceEnabled(true)
            _isServiceRunning.value = true

            val app = getApplication<Application>()
            com.example.service.PipraPayForegroundService.start(app)

            // Trigger short success haptic pulse
            AlertManager.triggerHapticPulse(app)

            _serverLatency.value = handshake.latencyMs.takeIf { it > 0 } ?: 35L
            _serverHealthOk.value = true
            _settingsSaveState.value = SettingsSaveState.Success("Handshake verified! Settings saved.")
            onSuccess()
        }
    }

    fun verifyAndApplyQrConfig(
        serverUrl: String,
        apiKey: String,
        deviceKey: String,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        val trimmedUrl = serverUrl.trim()
        val trimmedKey = apiKey.trim()
        val trimmedDevice = deviceKey.trim().ifBlank { prefs.getDeviceKey() }

        viewModelScope.launch {
            _qrVerificationState.value = QrVerificationState.Verifying

            val handshake = repository.verifyHandshake(
                serverUrl = trimmedUrl,
                apiKey = trimmedKey,
                deviceId = trimmedDevice
            )

            if (!handshake.isSuccess) {
                // If handshake failed, try companion OTP login if applicable
                val companionResp = try {
                    repository.companionLogin(
                        url = trimmedUrl,
                        otp = trimmedKey,
                        deviceKey = trimmedDevice
                    )
                } catch (_: Exception) { null }

                if (companionResp == null || !companionResp.success) {
                    val errorMsg = handshake.errorMessage ?: "QR pairing failed: Invalid credentials or expired token."
                    _qrVerificationState.value = QrVerificationState.Error(errorMsg)
                    onFailure(errorMsg)
                    return@launch
                }
            }

            // Success State (HTTP 200)
            // Save credentials to Encrypted Vault
            repository.completeOnboardingAndLogin(
                url = trimmedUrl,
                apiKey = trimmedKey,
                deviceKey = trimmedDevice,
                otp = trimmedKey
            )
            prefs.setServiceEnabled(true)
            prefs.setOnboardingCompleted(true)

            val app = getApplication<Application>()
            com.example.service.PipraPayForegroundService.start(app)
            _isServiceRunning.value = true

            // Trigger short success haptic pulse
            AlertManager.triggerHapticPulse(app)

            _serverLatency.value = handshake.latencyMs.takeIf { it > 0 } ?: 40L
            _serverHealthOk.value = true
            _qrVerificationState.value = QrVerificationState.Success("Connected to gateway!")
            onSuccess()
        }
    }

    fun setHapticEnabled(enabled: Boolean) {
        prefs.setHapticEnabled(enabled)
    }

    fun setAudioToneEnabled(enabled: Boolean) {
        prefs.setAudioToneEnabled(enabled)
    }

    fun setDarkModeEnabled(enabled: Boolean) {
        prefs.setDarkModeEnabled(enabled)
    }

    fun testAlertFeedback() {
        AlertManager.triggerHapticPulse(getApplication())
        AlertManager.playPosChime(getApplication())
    }

    fun resetOnboarding() {
        viewModelScope.launch {
            repository.resetOnboarding()
            _loginState.value = LoginState.Idle
        }
    }

    /**
     * Injects an SMS to test parser, database, and background sync logic.
     */
    fun simulateIncomingSms(senderAddress: String, body: String): Boolean {
        val parsed = MfsSmsParser.parse(senderAddress, body) ?: return false
        AlertManager.playInflowAlert(getApplication())
        viewModelScope.launch {
            repository.insertParsedTransaction(parsed)
        }
        return true
    }
}

sealed interface LoginState {
    data object Idle : LoginState
    data object Loading : LoginState
    data class Success(val message: String) : LoginState
    data class Error(val message: String) : LoginState
}

sealed interface SettingsSaveState {
    data object Idle : SettingsSaveState
    data object Loading : SettingsSaveState
    data class Success(val message: String) : SettingsSaveState
    data class Error(val message: String) : SettingsSaveState
}

sealed interface QrVerificationState {
    data object Idle : QrVerificationState
    data object Verifying : QrVerificationState
    data class Success(val message: String) : QrVerificationState
    data class Error(val message: String) : QrVerificationState
}

sealed interface ConnectionTestState {
    data object Idle : ConnectionTestState
    data object Testing : ConnectionTestState
    data class Finished(val result: ConnectionTestResult) : ConnectionTestState
}
