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
import com.example.data.repository.TransactionRepository
import com.example.parser.MfsSmsParser
import com.example.sync.SyncWorker
import com.example.util.AlertManager
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
        val messagesBySender = messages.filterNotNull().groupBy {
            it.displayOriginatingAddress ?: it.originatingAddress ?: "UNKNOWN"
        }

        // Extract SIM slot index from SMS broadcast intent (0-indexed converted to 1-indexed string)
        val slotIndex = intent.getIntExtra("slot", intent.getIntExtra("simSlot", intent.getIntExtra("simId", -1)))
        val detectedSimSlot = if (slotIndex >= 0) (slotIndex + 1).toString() else "1"

        val repository = TransactionRepository(context)

        for ((sender, smsList) in messagesBySender) {
            val fullBody = smsList.joinToString(separator = "") { it.displayMessageBody ?: it.messageBody ?: "" }
            val timestamp = smsList.firstOrNull()?.timestampMillis ?: System.currentTimeMillis()

            val pendingResult = goAsync()
            receiverScope.launch {
                try {
                    // Save to Room and trigger immediate batch sync
                    val rowId = repository.insertSms(
                        sender = sender,
                        message = fullBody,
                        simSlot = detectedSimSlot,
                        timestamp = timestamp
                    )

                    Log.d(TAG, "SMS saved with ID $rowId from sender $sender. Enqueued sync worker.")

                    // Play POS alert feedback if MFS
                    val parsed = MfsSmsParser.parse(sender, fullBody, timestamp)
                    if (parsed != null) {
                        AlertManager.playInflowAlert(context)
                        notifyTransactionDetected(context, parsed.provider, parsed.amount, parsed.trxId)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to persist and sync SMS: ${e.message}", e)
                } finally {
                    pendingResult.finish()
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
            val channelId = "bizlipay_trx_channel"

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    channelId,
                    "Detected Transactions",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Alerts when a new MFS payment is captured"
                }
                notificationManager.createNotificationChannel(channel)
            }

            val formattedAmount = String.format("৳ %.2f", amount)
            val notification = NotificationCompat.Builder(context, channelId)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("$provider Payment: $formattedAmount")
                .setContentText("TrxID: $trxId • Enqueued for BizliPay sync")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .build()

            notificationManager.notify(trxId.hashCode(), notification)
        } catch (e: Exception) {
            Log.w(TAG, "Could not post transaction alert notification: ${e.message}")
        }
    }

    companion object {
        private const val TAG = "BizliPaySmsReceiver"
    }
}
