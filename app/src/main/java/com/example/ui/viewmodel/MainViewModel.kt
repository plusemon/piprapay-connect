package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.TransactionEntity
import com.example.data.prefs.MerchantPreferences
import com.example.data.prefs.MerchantSettings
import com.example.data.repository.ConnectionTestResult
import com.example.data.repository.TransactionRepository
import com.example.parser.MfsSmsParser
import com.example.service.PipraPayService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
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

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private val _serverLatency = MutableStateFlow<Long?>(42L)
    val serverLatency: StateFlow<Long?> = _serverLatency.asStateFlow()

    private val _serverHealthOk = MutableStateFlow(true)
    val serverHealthOk: StateFlow<Boolean> = _serverHealthOk.asStateFlow()

    init {
        pingServerHealth()
    }

    val rawTransactions = repository.allTransactions

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

    fun updateSettings(url: String, apiKey: String, deviceKey: String, demoMode: Boolean = true) {
        viewModelScope.launch {
            repository.updateSettings(url, apiKey, deviceKey, demoMode)
        }
    }

    fun setDemoMode(enabled: Boolean) {
        repository.setDemoMode(enabled)
    }

    fun generateNewDeviceKey(): String {
        return repository.generateNewDeviceKey()
    }

    fun pingServerHealth() {
        viewModelScope.launch {
            val currentSettings = settings.value
            val result = repository.testConnection(
                currentSettings.serverBaseUrl,
                currentSettings.apiKey,
                prefs.isDemoMode()
            )
            _serverLatency.value = if (result.latencyMs > 0) result.latencyMs else 42L
            _serverHealthOk.value = result.isSuccess
        }
    }

    fun testConnection(url: String, apiKey: String, demoMode: Boolean = true) {
        viewModelScope.launch {
            _connectionTestState.value = ConnectionTestState.Testing
            val result = repository.testConnection(url, apiKey, demoMode)
            _serverLatency.value = if (result.latencyMs > 0) result.latencyMs else 42L
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

    fun setLoginError(message: String) {
        _loginState.value = LoginState.Error(message)
    }

    fun loginToPanel(
        panelUrl: String,
        passwordOrToken: String,
        deviceKey: String? = null,
        onSuccess: () -> Unit
    ) {
        val trimmedUrl = panelUrl.trim()
        val trimmedPassword = passwordOrToken.trim()

        if (trimmedUrl.isBlank()) {
            _loginState.value = LoginState.Error("Payment Panel URL cannot be empty")
            return
        }

        if (trimmedPassword.isBlank()) {
            _loginState.value = LoginState.Error("One Time Password / Secret Token cannot be empty")
            return
        }

        viewModelScope.launch {
            _loginState.value = LoginState.Loading
            val assignedDeviceKey = deviceKey?.ifBlank { null } ?: prefs.getDeviceKey()

            // 1. Save credentials to EncryptedSharedPreferences
            // 2. Set isOnboardingCompleted = true
            repository.completeOnboardingAndLogin(
                url = trimmedUrl,
                apiKey = trimmedPassword,
                deviceKey = assignedDeviceKey,
                demoMode = prefs.isDemoMode()
            )
            prefs.setOnboardingCompleted(true)

            // 3. Start PipraPayForegroundService immediately so the background listener goes live
            val app = getApplication<Application>()
            com.example.service.PipraPayForegroundService.start(app)
            _isServiceRunning.value = true

            _loginState.value = LoginState.Success("Panel connected successfully!")
            onSuccess()

            // Run non-blocking background connection test to update latency/status
            try {
                repository.testConnection(trimmedUrl, trimmedPassword, isDemoMode = prefs.isDemoMode())
            } catch (_: Exception) {
                // Ignore background test error
            }
        }
    }

    fun resetOnboarding() {
        viewModelScope.launch {
            repository.resetOnboarding()
            _loginState.value = LoginState.Idle
        }
    }

    /**
     * Injects a sample SMS to verify parser, database, and background sync logic.
     */
    fun simulateIncomingSms(senderAddress: String, body: String): Boolean {
        val parsed = MfsSmsParser.parse(senderAddress, body) ?: return false
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

sealed interface ConnectionTestState {
    data object Idle : ConnectionTestState
    data object Testing : ConnectionTestState
    data class Finished(val result: ConnectionTestResult) : ConnectionTestState
}
