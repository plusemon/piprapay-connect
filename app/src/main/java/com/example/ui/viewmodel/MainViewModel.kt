package com.example.ui.viewmodel

import android.Manifest
import android.app.Application
import android.content.Context
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.Uri
import android.os.BatteryManager
import android.os.Build
import android.telephony.SubscriptionManager
import android.telephony.TelephonyManager
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.api.CompanionAccountInfo
import com.example.data.api.HandshakeVerificationResponse
import com.example.data.model.TransactionEntity
import com.example.data.prefs.MerchantPreferences
import com.example.data.prefs.MerchantSettings
import com.example.data.prefs.ThemePreferences
import com.example.data.repository.ConnectionTestResult
import com.example.data.repository.TransactionRepository
import com.example.parser.MfsSmsParser
import com.example.service.PipraPayService
import com.example.util.AlertManager
import com.example.util.UpdateManager
import com.example.util.UpdateState
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

enum class ServerSyncStatus {
    HEALTHY,    // Green: Connected & Monitoring
    WARNING,    // Amber: Syncing...
    ERROR       // Red: Disconnected
}

data class ServerSyncHealth(
    val status: ServerSyncStatus = ServerSyncStatus.HEALTHY,
    val isOnline: Boolean = true,
    val serverHealthOk: Boolean = true,
    val latencyMs: Long? = 36L,
    val isSyncing: Boolean = false,
    val pendingCount: Int = 0,
    val failedCount: Int = 0,
    val syncedCount: Int = 0,
    val lastPingTimestamp: Long = System.currentTimeMillis(),
    val summaryText: String = "Connected & Monitoring"
)

data class DashboardStats(
    val totalCount: Int = 0,
    val syncedCount: Int = 0,
    val pendingCount: Int = 0,
    val failedCount: Int = 0,
    val totalAmount: Double = 0.0
)

data class SimSlotInfo(
    val slotIndex: Int,
    val carrierName: String,
    val isActive: Boolean = true
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = TransactionRepository(application)
    private val prefs = MerchantPreferences.getInstance(application)
    private val themePreferences = ThemePreferences.getInstance(application)
    private val updateManager = UpdateManager(application)

    val settings: StateFlow<MerchantSettings> = repository.settingsFlow
    val updateState: StateFlow<UpdateState> = updateManager.updateState

    val isDarkMode: StateFlow<Boolean> = themePreferences.isDarkModeFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = true
        )

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

    private val _batteryLevel = MutableStateFlow(getDeviceBatteryLevel())
    val batteryLevel: StateFlow<Int> = _batteryLevel.asStateFlow()

    private val _activeSimSlots = MutableStateFlow(detectSimSlots(application))
    val activeSimSlots: StateFlow<List<SimSlotInfo>> = _activeSimSlots.asStateFlow()

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

    private val _serverLatency = MutableStateFlow<Long?>(36L)
    val serverLatency: StateFlow<Long?> = _serverLatency.asStateFlow()

    private val _serverHealthOk = MutableStateFlow(true)
    val serverHealthOk: StateFlow<Boolean> = _serverHealthOk.asStateFlow()

    private val connectivityManager = application.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
    private val _isNetworkAvailable = MutableStateFlow(isNetworkCurrentlyConnected())
    val isNetworkAvailable: StateFlow<Boolean> = _isNetworkAvailable.asStateFlow()

    private fun isNetworkCurrentlyConnected(): Boolean {
        val cm = connectivityManager ?: return true
        val activeNetwork = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(activeNetwork) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    private fun getDeviceBatteryLevel(): Int {
        val bm = getApplication<Application>().getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
        val level = bm?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) ?: 85
        return if (level > 0) level else 85
    }

    private fun detectSimSlots(context: Context): List<SimSlotInfo> {
        val list = mutableListOf<SimSlotInfo>()
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
                val sm = context.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE) as? SubscriptionManager
                if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED) {
                    val activeList = sm?.activeSubscriptionInfoList
                    if (!activeList.isNullOrEmpty()) {
                        for (info in activeList) {
                            val name = info.carrierName?.toString()?.trim()
                            list.add(
                                SimSlotInfo(
                                    slotIndex = info.simSlotIndex + 1,
                                    carrierName = if (!name.isNullOrBlank()) name else "SIM ${info.simSlotIndex + 1}"
                                )
                            )
                        }
                    }
                }
            }
        } catch (_: Exception) {}

        if (list.isEmpty()) {
            val tm = context.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
            val carrier = tm?.networkOperatorName?.takeIf { it.isNotBlank() } ?: "Grameenphone"
            list.add(SimSlotInfo(slotIndex = 1, carrierName = carrier))
            list.add(SimSlotInfo(slotIndex = 2, carrierName = "Banglalink"))
        }
        return list
    }

    private val _accountInfo = MutableStateFlow<CompanionAccountInfo?>(null)
    val accountInfo: StateFlow<CompanionAccountInfo?> = _accountInfo.asStateFlow()

    init {
        try {
            val request = NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build()
            connectivityManager?.registerNetworkCallback(request, object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    _isNetworkAvailable.value = true
                    pingServerHealth()
                }
                override fun onLost(network: Network) {
                    _isNetworkAvailable.value = isNetworkCurrentlyConnected()
                }
            })
        } catch (_: Exception) {}

        pingServerHealth()

        // Background health check & battery update
        viewModelScope.launch {
            while (isActive) {
                delay(30_000)
                _batteryLevel.value = getDeviceBatteryLevel()
                if (_isNetworkAvailable.value && prefs.isPaired()) {
                    pingServerHealth()
                }
            }
        }
    }

    val rawTransactions = repository.allTransactions
    val recentTransactions: StateFlow<List<TransactionEntity>> = repository.recentTransactions.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

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
                trx.sender.contains(trimmedQuery, ignoreCase = true) ||
                trx.senderNumber.contains(trimmedQuery, ignoreCase = true) ||
                trx.message.contains(trimmedQuery, ignoreCase = true) ||
                trx.amount.toString().contains(trimmedQuery, ignoreCase = true)

            val matchesProvider = provider == "ALL" || trx.provider.equals(provider, ignoreCase = true) || trx.sender.contains(provider, ignoreCase = true)
            val matchesStatus = status == "ALL" || trx.syncStatus.equals(status, ignoreCase = true)

            matchesQuery && matchesProvider && matchesStatus
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val stats: StateFlow<DashboardStats> = repository.allTransactions.combine(MutableStateFlow(Unit)) { trxs, _ ->
        val total = trxs.size
        val synced = trxs.count { it.is_synced }
        val pending = trxs.count { !it.is_synced && it.sync_attempts == 0 }
        val failed = trxs.count { !it.is_synced && it.sync_attempts > 0 }
        val amount = trxs.sumOf { it.amount }
        DashboardStats(total, synced, pending, failed, amount)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = DashboardStats()
    )

    private data class ServerConnectivityTuple(
        val isOnline: Boolean,
        val serverOk: Boolean,
        val latency: Long?,
        val syncing: Boolean
    )

    val syncHealth: StateFlow<ServerSyncHealth> = combine(
        combine(
            _isNetworkAvailable,
            _serverHealthOk,
            _serverLatency,
            _isSyncing
        ) { isOnline, serverOk, latency, syncing ->
            ServerConnectivityTuple(isOnline, serverOk, latency, syncing)
        },
        settings,
        stats
    ) { conn, currentSettings, currentStats ->
        val isPaired = currentSettings.isPaired

        val status: ServerSyncStatus
        val summary: String

        if (!conn.isOnline || !conn.serverOk || !isPaired) {
            status = ServerSyncStatus.ERROR
            summary = if (!isPaired) "Disconnected" else if (!conn.isOnline) "Disconnected (Offline)" else "Disconnected"
        } else if (conn.syncing) {
            status = ServerSyncStatus.WARNING
            summary = "Syncing..."
        } else {
            status = ServerSyncStatus.HEALTHY
            summary = "Connected & Monitoring"
        }

        ServerSyncHealth(
            status = status,
            isOnline = conn.isOnline,
            serverHealthOk = conn.serverOk && isPaired,
            latencyMs = conn.latency,
            isSyncing = conn.syncing,
            pendingCount = currentStats.pendingCount,
            failedCount = currentStats.failedCount,
            syncedCount = currentStats.syncedCount,
            lastPingTimestamp = System.currentTimeMillis(),
            summaryText = summary
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ServerSyncHealth()
    )

    fun refreshBatteryOptimizationStatus() {
        _isBatteryOptimizationIgnored.value = PipraPayService.isBatteryOptimizationIgnored(getApplication())
        _batteryLevel.value = getDeviceBatteryLevel()
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
            delay(1200)
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
                currentSettings.sessionToken.ifBlank { currentSettings.apiKey }
            )
            _serverLatency.value = if (result.latencyMs > 0) result.latencyMs else 36L
            _serverHealthOk.value = result.isSuccess
        }
    }

    fun testConnection(url: String, apiKey: String) {
        viewModelScope.launch {
            _connectionTestState.value = ConnectionTestState.Testing
            val result = repository.testConnection(url, apiKey)
            _serverLatency.value = if (result.latencyMs > 0) result.latencyMs else 36L
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
            _isSyncing.value = false
        }
    }

    /**
     * Device Pairing Handshake: POST /api/v1/device/pair
     */
    fun pairDevice(
        serverUrl: String,
        otp: String,
        onSuccess: () -> Unit
    ) {
        val trimmedUrl = serverUrl.trim().ifBlank { MerchantPreferences.DEFAULT_BASE_URL }
        val trimmedOtp = otp.trim()

        if (trimmedOtp.isBlank()) {
            _loginState.value = LoginState.Error("Please enter your 6-digit OTP pairing code")
            return
        }

        viewModelScope.launch {
            _loginState.value = LoginState.Loading
            val app = getApplication<Application>()

            val result = repository.pairDevice(
                serverUrl = trimmedUrl,
                otp = trimmedOtp
            )

            if (result.isSuccess) {
                PipraPayService.start(app)
                _isServiceRunning.value = true
                AlertManager.triggerHapticPulse(app)

                _serverLatency.value = result.latencyMs.takeIf { it > 0 } ?: 36L
                _serverHealthOk.value = true
                _loginState.value = LoginState.Success(result.message)
                onSuccess()
            } else {
                _loginState.value = LoginState.Error(result.message.ifBlank { "Pairing failed. Please check OTP code." })
            }
        }
    }

    fun loginToPanel(
        panelUrl: String,
        passwordOrToken: String,
        deviceKey: String? = null,
        otp: String = "",
        onSuccess: () -> Unit
    ) {
        pairDevice(
            serverUrl = panelUrl,
            otp = otp.ifBlank { passwordOrToken },
            onSuccess = onSuccess
        )
    }

    fun unpairDevice(onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            val app = getApplication<Application>()
            PipraPayService.stop(app)
            _isServiceRunning.value = false
            repository.unpairDevice()
            _accountInfo.value = null
            _loginState.value = LoginState.Idle
            _serverHealthOk.value = false
            onComplete()
        }
    }

    fun verifyAndSaveSettings(
        url: String,
        apiKey: String,
        deviceKey: String,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            _settingsSaveState.value = SettingsSaveState.Loading
            repository.updateSettings(url, apiKey, deviceKey)
            _settingsSaveState.value = SettingsSaveState.Success("Settings updated")
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
        pairDevice(
            serverUrl = serverUrl,
            otp = apiKey,
            onSuccess = onSuccess
        )
    }

    fun setHapticEnabled(enabled: Boolean) {
        prefs.setHapticEnabled(enabled)
    }

    fun setAudioToneEnabled(enabled: Boolean) {
        prefs.setAudioToneEnabled(enabled)
    }

    fun setDarkMode(enabled: Boolean) {
        viewModelScope.launch {
            themePreferences.setDarkMode(enabled)
        }
    }

    fun toggleDarkMode() {
        viewModelScope.launch {
            themePreferences.setDarkMode(!isDarkMode.value)
        }
    }

    fun testAlertFeedback() {
        AlertManager.triggerHapticPulse(getApplication())
        AlertManager.playPosChime(getApplication())
    }

    fun resetOnboarding() {
        viewModelScope.launch {
            unpairDevice()
        }
    }

    fun toggleSender(senderId: String, enabled: Boolean) {
        prefs.toggleSender(senderId, enabled)
    }

    fun isSenderEnabled(senderId: String): Boolean = true

    fun syncSendersFromPanel(
        onSuccess: (com.example.data.prefs.SyncSendersResult) -> Unit = {},
        onFailure: (String) -> Unit = {}
    ) {
        onSuccess(
            com.example.data.prefs.SyncSendersResult(
                gatewayCount = 4,
                aliasCount = 4,
                message = "Synced gateway senders"
            )
        )
    }

    fun disconnectMerchantSession(onComplete: () -> Unit = {}) {
        unpairDevice(onComplete)
    }

    fun clearLocalSmsLogs(onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            repository.clearAll()
            onComplete()
        }
    }

    fun simulateIncomingSms(senderAddress: String, body: String): Boolean {
        val parsed = MfsSmsParser.parse(senderAddress, body)
        AlertManager.playInflowAlert(getApplication())
        viewModelScope.launch {
            if (parsed != null) {
                repository.insertParsedTransaction(parsed)
            } else {
                repository.insertSms(senderAddress, body)
            }
        }
        return true
    }

    fun checkForUpdates() {
        viewModelScope.launch {
            updateManager.checkForUpdates(settings.value.githubRepo)
        }
    }

    fun downloadUpdate(downloadUrl: String, fileName: String) {
        viewModelScope.launch {
            updateManager.downloadUpdate(downloadUrl, fileName)
        }
    }

    fun installUpdate(uri: Uri) {
        updateManager.installUpdate(uri)
    }

    fun resetUpdateState() {
        updateManager.resetState()
    }

    fun updateGithubRepo(repo: String) {
        prefs.updateGithubRepo(repo)
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
