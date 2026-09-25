package com.plusemon.bizlipay.data.repository

import android.content.Context
import android.os.BatteryManager
import android.os.Build
import com.plusemon.bizlipay.data.api.BatchSmsResult
import com.plusemon.bizlipay.data.api.BizliPayApiClient
import com.plusemon.bizlipay.data.api.CompanionAccountInfo
import com.plusemon.bizlipay.data.api.CompanionLoginResponse
import com.plusemon.bizlipay.data.api.DeviceBatchSmsItem
import com.plusemon.bizlipay.data.api.HandshakeVerificationResponse
import com.plusemon.bizlipay.data.api.HeartbeatResult
import com.plusemon.bizlipay.data.api.PairResult
import com.plusemon.bizlipay.data.db.AppDatabase
import com.plusemon.bizlipay.data.model.TransactionEntity
import com.plusemon.bizlipay.data.prefs.MerchantPreferences
import com.plusemon.bizlipay.data.prefs.MerchantSettings
import com.plusemon.bizlipay.parser.MfsSmsParser
import com.plusemon.bizlipay.parser.ParsedMfsTransaction
import com.plusemon.bizlipay.sync.SyncWorker
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
    val recentTransactions: Flow<List<TransactionEntity>> = dao.getRecentTransactionsFlow(15)
    val pendingCount: Flow<Int> = dao.getPendingCountFlow()
    val totalCount: Flow<Int> = dao.getTransactionCountFlow()
    val syncedCount: Flow<Int> = dao.getSyncedCountFlow()
    val settingsFlow: StateFlow<MerchantSettings> = prefs.settingsFlow

    /**
     * Store incoming SMS into Room database and trigger immediate batch sync.
     */
    suspend fun insertSms(
        sender: String,
        message: String,
        simSlot: String = "1",
        timestamp: Long = System.currentTimeMillis()
    ): Long = withContext(Dispatchers.IO) {
        val parsed = MfsSmsParser.parse(sender, message, timestamp)

        val entity = TransactionEntity(
            sender = sender,
            message = message,
            sim_slot = simSlot,
            timestamp = timestamp,
            is_synced = false,
            sync_attempts = 0,
            trxId = parsed?.trxId ?: "",
            provider = parsed?.provider ?: sender,
            senderKey = parsed?.senderKey ?: sender.lowercase(),
            senderNumber = parsed?.senderNumber ?: "",
            amount = parsed?.amount ?: 0.0,
            balance = parsed?.balance,
            currency = parsed?.currency ?: "BDT",
            type = parsed?.type ?: "received"
        )

        val rowId = dao.insertTransaction(entity)
        if (rowId != -1L) {
            SyncWorker.enqueueSync(context, forceNew = true)
        }
        rowId
    }

    suspend fun insertParsedTransaction(parsed: ParsedMfsTransaction): Boolean = withContext(Dispatchers.IO) {
        val entity = TransactionEntity(
            sender = parsed.provider,
            message = parsed.rawMessage,
            sim_slot = "1",
            timestamp = parsed.timestamp,
            is_synced = false,
            sync_attempts = 0,
            trxId = parsed.trxId,
            provider = parsed.provider,
            senderKey = parsed.senderKey,
            senderNumber = parsed.senderNumber,
            amount = parsed.amount,
            balance = parsed.balance,
            currency = parsed.currency,
            type = parsed.type
        )
        val rowId = dao.insertTransaction(entity)
        if (rowId != -1L) {
            SyncWorker.enqueueSync(context, forceNew = true)
            true
        } else {
            false
        }
    }

    /**
     * Pair Device using RESTful Endpoint: POST /api/v1/device/pair
     */
    suspend fun pairDevice(
        serverUrl: String,
        otp: String,
        deviceName: String = prefs.getDeviceName(),
        deviceModel: String = prefs.getDeviceModel(),
        androidLevel: String = prefs.getAndroidLevel(),
        appVersion: String = "v1.0.0"
    ): PairResult = withContext(Dispatchers.IO) {
        val result = BizliPayApiClient.pair(
            baseUrl = serverUrl,
            otp = otp,
            deviceName = deviceName,
            deviceModel = deviceModel,
            androidLevel = androidLevel,
            appVersion = appVersion
        )

        if (result.isSuccess && !result.token.isNullOrBlank()) {
            prefs.savePairingSession(
                token = result.token,
                deviceUid = result.deviceUid ?: "dev_${otp.take(6)}",
                serverBaseUrl = serverUrl,
                otp = otp,
                deviceName = deviceName
            )
        }
        result
    }

    /**
     * Heartbeat & Health Telemetry: POST /api/v1/device/heartbeat
     */
    suspend fun sendHeartbeat(): HeartbeatResult = withContext(Dispatchers.IO) {
        val baseUrl = prefs.getServerBaseUrl()
        val token = prefs.getSessionToken()
        if (token.isBlank()) {
            return@withContext HeartbeatResult(
                isSuccess = false,
                errorMessage = "No active session"
            )
        }

        val bm = context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
        val batteryLevel = bm?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) ?: 85
        val appVersion = "v1.0.0"

        val result = BizliPayApiClient.heartbeat(baseUrl, token, batteryLevel, appVersion)

        if (result.isUnauthorized) {
            // HTTP 401 Unauthorized: clear saved session and mark device as unpaired
            prefs.clearSessionAndUnpair()
        } else if (result.isSuccess) {
            prefs.updateLastSyncTimestamp()
        }

        result
    }

    /**
     * Batch SMS Ingestion: POST /api/v1/device/sms/batch
     * Batches up to 50 unsynced messages and sends to backend.
     */
    suspend fun syncBatchSms(limit: Int = 50): BatchSmsResult = withContext(Dispatchers.IO) {
        val unsynced = dao.getUnsyncedMessages(limit)
        if (unsynced.isEmpty()) {
            return@withContext BatchSmsResult(isSuccess = true, message = "No unsynced messages")
        }

        val baseUrl = prefs.getServerBaseUrl()
        val token = prefs.getSessionToken()

        if (token.isBlank()) {
            return@withContext BatchSmsResult(
                isSuccess = false,
                message = "Device not paired. Session token missing.",
                isUnauthorized = true
            )
        }

        val items = unsynced.map { trx ->
            // timestamp in seconds (or standard unix timestamp)
            val tsInSeconds = if (trx.timestamp > 10_000_000_000L) trx.timestamp / 1000L else trx.timestamp
            DeviceBatchSmsItem(
                sender = trx.sender,
                message = trx.message,
                sim_slot = trx.sim_slot.ifBlank { "1" },
                timestamp = tsInSeconds
            )
        }

        val result = BizliPayApiClient.sendBatchSms(baseUrl, token, items)

        if (result.isUnauthorized) {
            prefs.clearSessionAndUnpair()
            return@withContext result
        }

        val ids = unsynced.map { it.id }
        if (result.isSuccess) {
            dao.markAsSynced(ids)
            prefs.updateLastSyncTimestamp()
        } else {
            dao.incrementSyncAttempts(ids, result.message)
        }

        result
    }

    suspend fun triggerManualSync() = withContext(Dispatchers.IO) {
        SyncWorker.enqueueSync(context, forceNew = true)
    }

    suspend fun resyncSingleTransaction(trxId: String) = withContext(Dispatchers.IO) {
        dao.updateSyncFailure(trxId, 0, null)
        SyncWorker.enqueueSync(context, forceNew = true)
    }

    suspend fun deleteTransaction(id: Long) = withContext(Dispatchers.IO) {
        dao.deleteTransaction(id)
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

    suspend fun unpairDevice() = withContext(Dispatchers.IO) {
        prefs.clearSessionAndUnpair()
    }

    suspend fun verifyHandshake(
        serverUrl: String,
        apiKey: String,
        deviceId: String = prefs.getDeviceKey()
    ): HandshakeVerificationResponse = withContext(Dispatchers.IO) {
        val result = pairDevice(serverUrl, apiKey)
        HandshakeVerificationResponse(
            isSuccess = result.isSuccess,
            httpCode = result.httpCode,
            errorMessage = if (!result.isSuccess) result.message else null,
            latencyMs = result.latencyMs,
            token = result.token
        )
    }

    suspend fun companionLogin(
        url: String,
        otp: String,
        deviceKey: String = prefs.getDeviceKey()
    ): CompanionLoginResponse = withContext(Dispatchers.IO) {
        val result = pairDevice(url, otp)
        CompanionLoginResponse(
            success = result.isSuccess,
            token = result.token,
            device_uid = result.deviceUid,
            title = if (result.isSuccess) "Device Paired" else "Pairing Failed",
            message = result.message
        )
    }

    suspend fun refreshAccountInfo(): CompanionAccountInfo? = withContext(Dispatchers.IO) {
        val hb = sendHeartbeat()
        if (hb.isSuccess) {
            CompanionAccountInfo(
                success = true,
                fullname = hb.deviceName ?: prefs.getDeviceName(),
                email = "Connected Device"
            )
        } else {
            null
        }
    }

    suspend fun refreshWhitelistedSenders(): List<String> = withContext(Dispatchers.IO) {
        prefs.getWhitelistedSenders()
    }

    suspend fun completeOnboardingAndLogin(
        url: String,
        apiKey: String,
        deviceKey: String = prefs.getDeviceKey(),
        otp: String = ""
    ) = withContext(Dispatchers.IO) {
        prefs.completeOnboardingAndLogin(url, apiKey, deviceKey, otp)
    }

    suspend fun resetOnboarding() = withContext(Dispatchers.IO) {
        prefs.clearSessionAndUnpair()
    }

    fun generateNewDeviceKey(): String {
        return prefs.generateNewDeviceKey()
    }

    suspend fun testConnection(baseUrl: String, tokenOrOtp: String): ConnectionTestResult = withContext(Dispatchers.IO) {
        val token = prefs.getSessionToken().ifBlank { tokenOrOtp }
        val startTime = System.currentTimeMillis()

        if (token.isNotBlank()) {
            val hb = BizliPayApiClient.heartbeat(baseUrl, token, 100)
            val latency = System.currentTimeMillis() - startTime
            if (hb.isSuccess) {
                return@withContext ConnectionTestResult(
                    isSuccess = true,
                    httpCode = hb.httpCode ?: 200,
                    latencyMs = latency,
                    message = "Connected to BizliPay Gateway in ${latency}ms"
                )
            } else if (hb.isUnauthorized) {
                return@withContext ConnectionTestResult(
                    isSuccess = false,
                    httpCode = 401,
                    latencyMs = latency,
                    message = "Unauthorized: Session token expired. Please pair again."
                )
            }
        }

        // Test pair endpoint if OTP is 6 digits
        if (tokenOrOtp.isNotBlank()) {
            val pair = BizliPayApiClient.pair(
                baseUrl = baseUrl,
                otp = tokenOrOtp,
                deviceName = prefs.getDeviceName(),
                deviceModel = prefs.getDeviceModel(),
                androidLevel = prefs.getAndroidLevel()
            )
            val latency = System.currentTimeMillis() - startTime
            return@withContext ConnectionTestResult(
                isSuccess = pair.isSuccess,
                httpCode = pair.httpCode,
                latencyMs = latency,
                message = pair.message
            )
        }

        ConnectionTestResult(
            isSuccess = false,
            message = "No credentials provided for testing"
        )
    }
}
