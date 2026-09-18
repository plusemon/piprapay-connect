package com.example.receiver

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Telephony
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.R
import com.example.data.db.AppDatabase
import com.example.data.model.TransactionEntity
import com.example.parser.MfsSmsParser
import com.example.sync.SyncWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class SmsBroadcastReceiver : BroadcastReceiver() {

    private val receiverScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION &&
            action != "android.provider.Telephony.SMS_RECEIVED"
        ) {
            return
        }

        val messages = try {
            Telephony.Sms.Intents.getMessagesFromIntent(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Error retrieving SMS PDUs from intent: ${e.message}")
            return
        }

        if (messages.isNullOrEmpty()) {
            return
        }

        // Group multi-part SMS messages by originating address
        val messagesBySender = messages.filterNotNull().groupBy { it.displayOriginatingAddress ?: it.originatingAddress ?: "UNKNOWN" }

        // Extract SIM slot index from SMS broadcast intent (0-indexed converted to 1-indexed)
        val slotIndex = intent.getIntExtra("slot", intent.getIntExtra("simSlot", intent.getIntExtra("simId", -1)))
        val detectedSimSlot = if (slotIndex >= 0) slotIndex + 1 else 1

        for ((sender, smsList) in messagesBySender) {
            val fullBody = smsList.joinToString(separator = "") { it.displayMessageBody ?: it.messageBody ?: "" }
            val timestamp = smsList.firstOrNull()?.timestampMillis ?: System.currentTimeMillis()

            val parsed = MfsSmsParser.parse(
                senderAddress = sender,
                messageBody = fullBody,
                smsTimestamp = timestamp
            )

            if (parsed != null) {
                Log.d(TAG, "MFS transaction recognized: ${parsed.provider} TrxID=${parsed.trxId} Amount=${parsed.amount} Balance=${parsed.balance}")

                val pendingResult = goAsync()
                receiverScope.launch {
                    try {
                        val db = AppDatabase.getInstance(context)
                        val entity = TransactionEntity(
                            trxId = parsed.trxId,
                            provider = parsed.provider,
                            senderKey = parsed.senderKey,
                            senderNumber = parsed.senderNumber,
                            amount = parsed.amount,
                            balance = parsed.balance,
                            currency = parsed.currency,
                            type = parsed.type,
                            simSlot = detectedSimSlot,
                            rawMessage = parsed.rawMessage,
                            timestamp = parsed.timestamp,
                            syncStatus = "PENDING",
                            retryCount = 0
                        )

                        val insertRowId = db.transactionDao().insertTransaction(entity)
                        if (insertRowId != -1L) {
                            Log.d(TAG, "Transaction stored with id $insertRowId. Enqueuing sync worker.")
                            SyncWorker.enqueueSync(context, forceNew = false)
                            notifyTransactionDetected(context, parsed.provider, parsed.amount, parsed.trxId)
                        } else {
                            Log.d(TAG, "Duplicate transaction ignored: ${parsed.trxId}")
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to persist detected transaction: ${e.message}", e)
                    } finally {
                        pendingResult.finish()
                    }
                }
            }
        }
    }

    private fun notifyTransactionDetected(
        context: Context,
        provider: String,
        amount: Double,
        trxId: String
    ) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                val hasPermission = ContextCompat.checkSelfPermission(
                    context,
                    android.Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
                if (!hasPermission) return
            }

            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val channelId = "piprapay_trx_channel"

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    channelId,
                    "Detected Transactions",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Alerts when a new MFS SMS payment is captured"
                }
                notificationManager.createNotificationChannel(channel)
            }

            val formattedAmount = String.format("৳ %.2f", amount)
            val notification = NotificationCompat.Builder(context, channelId)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("$provider Payment Received: $formattedAmount")
                .setContentText("TrxID: $trxId • Enqueued for PipraPay sync")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .build()

            notificationManager.notify(trxId.hashCode(), notification)
        } catch (e: Exception) {
            Log.w(TAG, "Could not post transaction alert notification: ${e.message}")
        }
    }

    companion object {
        private const val TAG = "SmsBroadcastReceiver"
    }
}
