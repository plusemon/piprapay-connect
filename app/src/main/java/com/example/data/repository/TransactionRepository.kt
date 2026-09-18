package com.example.data.repository

import android.content.Context
import com.example.data.api.ApiClient
import com.example.data.db.AppDatabase
import com.example.data.model.TransactionEntity
import com.example.data.prefs.MerchantPreferences
import com.example.data.prefs.MerchantSettings
import com.example.parser.ParsedMfsTransaction
import com.example.sync.SyncWorker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext

data class ConnectionTestResult(
    val isSuccess: Boolean,
    val httpCode: Int? = null,
    val latencyMs: Long = 0L,
    val message: String
)

class TransactionRepository(private val context: Context) {

    private val db = AppDatabase.getInstance(context)
    private val dao = db.transactionDao()
    private val prefs = MerchantPreferences.getInstance(context)

    val allTransactions: Flow<List<TransactionEntity>> = dao.getAllTransactionsFlow()
    val pendingCount: Flow<Int> = dao.getPendingCountFlow()
    val totalCount: Flow<Int> = dao.getTransactionCountFlow()
    val settingsFlow: StateFlow<MerchantSettings> = prefs.settingsFlow

    suspend fun insertParsedTransaction(parsed: ParsedMfsTransaction): Boolean = withContext(Dispatchers.IO) {
        val entity = TransactionEntity(
            trxId = parsed.trxId,
            provider = parsed.provider,
            senderKey = parsed.senderKey,
            senderNumber = parsed.senderNumber,
            amount = parsed.amount,
            balance = parsed.balance,
            currency = parsed.currency,
            type = parsed.type,
            simSlot = 1,
            rawMessage = parsed.rawMessage,
            timestamp = parsed.timestamp,
            syncStatus = "PENDING",
            retryCount = 0
        )
        val rowId = dao.insertTransaction(entity)
        if (rowId != -1L) {
            SyncWorker.enqueueSync(context, forceNew = true)
            true
        } else {
            false
        }
    }

    suspend fun triggerManualSync() = withContext(Dispatchers.IO) {
        SyncWorker.enqueueSync(context, forceNew = true)
    }

    suspend fun resyncSingleTransaction(trxId: String) = withContext(Dispatchers.IO) {
        dao.updateStatus(trxId, "PENDING")
        SyncWorker.enqueueSync(context, forceNew = true)
    }

    suspend fun deleteTransaction(trxId: String) = withContext(Dispatchers.IO) {
        dao.deleteTransaction(trxId)
    }

    suspend fun clearAll() = withContext(Dispatchers.IO) {
        dao.clearAll()
    }

    suspend fun updateSettings(url: String, apiKey: String, deviceKey: String, otp: String? = null) = withContext(Dispatchers.IO) {
        prefs.updateSettings(url, apiKey, deviceKey, otp)
    }

    suspend fun completeOnboardingAndLogin(
        url: String,
        apiKey: String,
        deviceKey: String = prefs.getDeviceKey(),
        otp: String = ""
    ) = withContext(Dispatchers.IO) {
        prefs.completeOnboardingAndLogin(url, apiKey, deviceKey, otp)
        registerDeviceWithBackend(url, apiKey, deviceKey, otp)
    }

    private suspend fun registerDeviceWithBackend(
        url: String,
        apiKey: String,
        deviceKey: String,
        otp: String
    ) {
        try {
            val api = ApiClient.createApi(url, apiKey)
            val req = com.example.data.api.DeviceRegisterRequest(
                deviceId = deviceKey,
                otp = otp,
                name = prefs.getDeviceName(),
                model = prefs.getDeviceModel(),
                androidLevel = prefs.getAndroidLevel(),
                appVersion = "1.0.0",
                status = "active"
            )
            val authHeader = if (apiKey.isNotBlank()) "Bearer $apiKey" else ""
            api.registerDevice(authHeader, req)
        } catch (_: Exception) {
            // Non-blocking - device will also be tracked on first SMS sync
        }
    }

    suspend fun resetOnboarding() = withContext(Dispatchers.IO) {
        prefs.resetOnboarding()
    }

    fun generateNewDeviceKey(): String {
        return prefs.generateNewDeviceKey()
    }

    suspend fun testConnection(baseUrl: String, apiKey: String): ConnectionTestResult = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        try {
            val api = ApiClient.createApi(baseUrl, apiKey)
            val authHeader = if (apiKey.isNotBlank()) "Bearer $apiKey" else null

            val response = try {
                api.pingServer(authHeader)
            } catch (_: Exception) {
                // Fallback to health endpoint
                api.healthCheck()
            }

            val latency = System.currentTimeMillis() - startTime
            val code = response.code()

            if (response.isSuccessful || code == 200 || code == 204) {
                ConnectionTestResult(
                    isSuccess = true,
                    httpCode = code,
                    latencyMs = latency,
                    message = "Server reachable! HTTP $code response in ${latency}ms"
                )
            } else if (code == 401 || code == 403) {
                ConnectionTestResult(
                    isSuccess = false,
                    httpCode = code,
                    latencyMs = latency,
                    message = "Server reached, but Authentication failed (HTTP $code). Verify Merchant API Key."
                )
            } else if (code == 404) {
                ConnectionTestResult(
                    isSuccess = true,
                    httpCode = code,
                    latencyMs = latency,
                    message = "Server reachable at $baseUrl (HTTP 404 on ping). Ready for /api/sms/receive."
                )
            } else {
                ConnectionTestResult(
                    isSuccess = false,
                    httpCode = code,
                    latencyMs = latency,
                    message = "Server returned error HTTP $code: ${response.message()}"
                )
            }
        } catch (e: Exception) {
            val latency = System.currentTimeMillis() - startTime
            ConnectionTestResult(
                isSuccess = false,
                httpCode = null,
                latencyMs = latency,
                message = e.localizedMessage ?: "Connection failed. Check Server URL and network."
            )
        }
    }
}
