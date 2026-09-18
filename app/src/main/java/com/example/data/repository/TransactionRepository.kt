package com.example.data.repository

import android.content.Context
import com.example.data.api.ApiClient
import com.example.data.api.CompanionAccountInfo
import com.example.data.api.CompanionLoginResponse
import com.example.data.api.PipraPayCompanionClient
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

    suspend fun companionLogin(
        url: String,
        otp: String,
        deviceKey: String = prefs.getDeviceKey()
    ): CompanionLoginResponse = withContext(Dispatchers.IO) {
        val loginResp = PipraPayCompanionClient.login(
            baseUrl = url,
            otp = otp,
            deviceName = prefs.getDeviceName(),
            deviceModel = prefs.getDeviceModel(),
            androidLevel = prefs.getAndroidLevel(),
            appVersion = "1.0.0"
        )

        if (loginResp.success && !loginResp.token.isNullOrBlank()) {
            val token = loginResp.token

            // 1. Fetch Account Information (merchant name, email, stats)
            val accountInfo = try {
                PipraPayCompanionClient.getAccountInformation(url, token)
            } catch (_: Exception) { null }

            // 2. Fetch Whitelisted Senders (bkash, nagad, upay, etc.)
            val senders = try {
                PipraPayCompanionClient.getWhitelistedSenders(url, token)
            } catch (_: Exception) { emptyList() }

            // 3. Persist securely
            prefs.completeOnboardingAndLogin(
                serverBaseUrl = url,
                apiKey = otp,
                deviceKey = deviceKey,
                otp = otp,
                sessionToken = token
            )
            prefs.saveCompanionSession(
                token = token,
                accountName = accountInfo?.fullname,
                accountEmail = accountInfo?.email,
                senders = senders
            )
        }
        loginResp
    }

    suspend fun refreshAccountInfo(): CompanionAccountInfo? = withContext(Dispatchers.IO) {
        val url = prefs.getServerBaseUrl()
        val token = prefs.getSessionToken()
        if (token.isBlank()) return@withContext null
        val info = PipraPayCompanionClient.getAccountInformation(url, token)
        if (info.success) {
            prefs.saveCompanionSession(
                token = token,
                accountName = info.fullname,
                accountEmail = info.email
            )
        }
        info
    }

    suspend fun refreshWhitelistedSenders(): List<String> = withContext(Dispatchers.IO) {
        val url = prefs.getServerBaseUrl()
        val token = prefs.getSessionToken()
        if (token.isBlank()) return@withContext emptyList()
        val senders = PipraPayCompanionClient.getWhitelistedSenders(url, token)
        if (senders.isNotEmpty()) {
            prefs.saveCompanionSession(token = token, senders = senders)
        }
        senders
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

    suspend fun testConnection(baseUrl: String, apiKeyOrOtp: String): ConnectionTestResult = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        try {
            val token = prefs.getSessionToken()
            if (token.isNotBlank()) {
                val accountInfo = PipraPayCompanionClient.getAccountInformation(baseUrl, token)
                val latency = System.currentTimeMillis() - startTime
                if (accountInfo.success) {
                    return@withContext ConnectionTestResult(
                        isSuccess = true,
                        httpCode = 200,
                        latencyMs = latency,
                        message = "Connected to PipraPay Companion (${accountInfo.fullname}) in ${latency}ms"
                    )
                }
            }

            // If OTP is provided, test companion login
            if (apiKeyOrOtp.isNotBlank() && apiKeyOrOtp.length in 6..12 && apiKeyOrOtp.all { it.isDigit() }) {
                val loginTest = PipraPayCompanionClient.login(
                    baseUrl = baseUrl,
                    otp = apiKeyOrOtp,
                    deviceName = prefs.getDeviceName(),
                    deviceModel = prefs.getDeviceModel(),
                    androidLevel = prefs.getAndroidLevel()
                )
                val latency = System.currentTimeMillis() - startTime
                if (loginTest.success) {
                    return@withContext ConnectionTestResult(
                        isSuccess = true,
                        httpCode = 200,
                        latencyMs = latency,
                        message = "PipraPay OTP Verified! Token generated in ${latency}ms"
                    )
                }
            }

            // Fallback: ping endpoint / HTTP probe
            val api = ApiClient.createApi(baseUrl, apiKeyOrOtp)
            val authHeader = if (apiKeyOrOtp.isNotBlank()) "Bearer $apiKeyOrOtp" else null

            val response = try {
                api.pingServer(authHeader)
            } catch (_: Exception) {
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
                    message = "Server reached, but Authentication failed (HTTP $code)."
                )
            } else if (code == 404) {
                ConnectionTestResult(
                    isSuccess = true,
                    httpCode = code,
                    latencyMs = latency,
                    message = "PipraPay server online at $baseUrl in ${latency}ms"
                )
            } else {
                ConnectionTestResult(
                    isSuccess = false,
                    httpCode = code,
                    latencyMs = latency,
                    message = "Server returned HTTP $code: ${response.message()}"
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
