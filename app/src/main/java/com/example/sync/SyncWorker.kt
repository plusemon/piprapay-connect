package com.example.sync

import android.content.Context
import android.util.Log
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.data.api.ApiClient
import com.example.data.api.SmsSyncRequest
import com.example.data.db.AppDatabase
import com.example.data.prefs.MerchantPreferences
import java.io.IOException
import java.net.UnknownHostException
import java.util.concurrent.TimeUnit

class SyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    private val db = AppDatabase.getInstance(appContext)
    private val transactionDao = db.transactionDao()
    private val prefs = MerchantPreferences.getInstance(appContext)

    override suspend fun doWork(): Result {
        val pendingTransactions = transactionDao.getPendingTransactions()
        if (pendingTransactions.isEmpty()) {
            Log.d(TAG, "No pending transactions to sync.")
            return Result.success()
        }

        val baseUrl = prefs.getServerBaseUrl()
        val apiKey = prefs.getApiKey()
        val deviceKey = prefs.getDeviceKey()
        val isDemoMode = prefs.isDemoMode()

        Log.d(TAG, "Starting sync for ${pendingTransactions.size} transactions to $baseUrl (demoMode=$isDemoMode)")

        val api = try {
            ApiClient.createApi(baseUrl, apiKey, isDemoMode)
        } catch (e: Exception) {
            Log.w(TAG, "Could not initialize API client: ${e.message}")
            return Result.retry()
        }

        var anyFailed = false

        for (trx in pendingTransactions) {
            try {
                val request = SmsSyncRequest(
                    deviceKey = deviceKey,
                    provider = trx.provider,
                    trxId = trx.trxId,
                    senderNumber = trx.senderNumber,
                    amount = trx.amount,
                    rawSms = trx.rawMessage,
                    timestamp = trx.timestamp
                )

                val response = api.syncSmsTransaction(
                    authorization = "Bearer $apiKey",
                    request = request
                )

                if (response.isSuccessful) {
                    val code = response.code()
                    if (code == 200 || code == 201) {
                        Log.d(TAG, "Successfully synced transaction: ${trx.trxId}")
                        transactionDao.updateStatus(trx.trxId, "SYNCED")
                    } else {
                        val error = "Server returned HTTP $code"
                        handleFailure(trx.trxId, trx.retryCount, error, isTransient = false)
                        anyFailed = true
                    }
                } else {
                    val error = "HTTP ${response.code()}: ${response.message()}"
                    handleFailure(trx.trxId, trx.retryCount, error, isTransient = false)
                    anyFailed = true
                }
            } catch (e: UnknownHostException) {
                // Host cannot be resolved yet (offline / DNS not configured)
                Log.w(TAG, "Host unreachable for ${trx.trxId}: ${e.message}. Queued offline.")
                handleFailure(trx.trxId, trx.retryCount, "Queued offline (Host unreachable)", isTransient = true)
                anyFailed = true
            } catch (e: IOException) {
                // Network timeout or connection drop
                Log.w(TAG, "Network connection issue for ${trx.trxId}: ${e.message}")
                handleFailure(trx.trxId, trx.retryCount, "Network connection pending", isTransient = true)
                anyFailed = true
            } catch (e: Exception) {
                Log.w(TAG, "Error syncing ${trx.trxId}: ${e.message}")
                handleFailure(trx.trxId, trx.retryCount, e.localizedMessage ?: "Sync error", isTransient = false)
                anyFailed = true
            }
        }

        prefs.updateLastSyncTimestamp()

        return if (anyFailed) {
            if (runAttemptCount < MAX_RETRIES) {
                Result.retry()
            } else {
                Result.success() // Keep transactions queued in DB without failing the WorkManager job
            }
        } else {
            Result.success()
        }
    }

    private suspend fun handleFailure(
        trxId: String,
        currentRetryCount: Int,
        errorMessage: String,
        isTransient: Boolean
    ) {
        val nextRetry = currentRetryCount + 1
        val newStatus = if (!isTransient && nextRetry >= MAX_RETRIES) "FAILED" else "PENDING"
        transactionDao.updateSyncFailure(trxId, newStatus, nextRetry, errorMessage)
    }

    companion object {
        const val TAG = "PipraPaySyncWorker"
        const val UNIQUE_WORK_NAME = "piprapay_sms_sync_work"
        const val MAX_RETRIES = 5

        fun enqueueSync(context: Context, forceNew: Boolean = false) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val syncRequest = OneTimeWorkRequestBuilder<SyncWorker>()
                .setConstraints(constraints)
                .setBackoffCriteria(
                    BackoffPolicy.EXPONENTIAL,
                    15,
                    TimeUnit.SECONDS
                )
                .addTag(TAG)
                .build()

            val policy = if (forceNew) {
                ExistingWorkPolicy.REPLACE
            } else {
                ExistingWorkPolicy.KEEP
            }

            WorkManager.getInstance(context.applicationContext).enqueueUniqueWork(
                UNIQUE_WORK_NAME,
                policy,
                syncRequest
            )
        }
    }
}
