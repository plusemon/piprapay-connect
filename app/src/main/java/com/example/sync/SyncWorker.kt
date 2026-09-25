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
import com.example.data.repository.TransactionRepository
import java.util.concurrent.TimeUnit

class SyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    private val repository = TransactionRepository(appContext)

    override suspend fun doWork(): Result {
        Log.d(TAG, "Starting BizliPay SMS batch sync worker")

        val result = repository.syncBatchSms(limit = 50)

        return when {
            result.isSuccess -> {
                Log.d(TAG, "Batch sync completed: ${result.message} (${result.processedCount} messages)")
                Result.success()
            }
            result.isUnauthorized -> {
                Log.w(TAG, "Batch sync unauthorized (HTTP 401). Device marked unpaired.")
                Result.failure()
            }
            else -> {
                Log.w(TAG, "Batch sync failed: ${result.message}")
                if (runAttemptCount < MAX_RETRIES) {
                    Result.retry()
                } else {
                    Result.success() // Keep unsynced in Room without killing WorkManager
                }
            }
        }
    }

    companion object {
        const val TAG = "BizliPaySyncWorker"
        const val UNIQUE_WORK_NAME = "bizlipay_sms_batch_sync_work"
        const val MAX_RETRIES = 3

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
