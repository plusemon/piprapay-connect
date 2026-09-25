package com.plusemon.bizlipay.service

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.provider.Settings
import android.util.Log
import androidx.core.app.NotificationCompat
import com.plusemon.bizlipay.MainActivity
import com.plusemon.bizlipay.R
import com.plusemon.bizlipay.data.prefs.MerchantPreferences
import com.plusemon.bizlipay.data.repository.TransactionRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

open class BizliPayService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.IO + Job())
    private var heartbeatJob: Job? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        if (action == ACTION_STOP) {
            Log.d(TAG, "Stopping BizliPay foreground service")
            MerchantPreferences.getInstance(this).setServiceEnabled(false)
            heartbeatJob?.cancel()
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return START_NOT_STICKY
        }

        Log.d(TAG, "Starting BizliPay foreground keep-alive service")
        MerchantPreferences.getInstance(this).setServiceEnabled(true)
        val notification = buildForegroundNotification()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }

        startHeartbeatLoop()

        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        heartbeatJob?.cancel()
    }

    /**
     * Triggers the heartbeat ping every 15 minutes as per specification.
     */
    private fun startHeartbeatLoop() {
        heartbeatJob?.cancel()
        heartbeatJob = serviceScope.launch {
            val repository = TransactionRepository(applicationContext)
            val prefs = MerchantPreferences.getInstance(applicationContext)

            while (isActive) {
                if (prefs.isPaired()) {
                    try {
                        Log.d(TAG, "Executing 15-minute BizliPay heartbeat ping")
                        val result = repository.sendHeartbeat()
                        if (result.isUnauthorized) {
                            Log.w(TAG, "Heartbeat returned 401 Unauthorized. Stopping service.")
                            stopForeground(STOP_FOREGROUND_REMOVE)
                            stopSelf()
                            break
                        }
                    } catch (e: Exception) {
                        Log.w(TAG, "Heartbeat failed: ${e.message}")
                    }
                }
                // Delay 15 minutes
                delay(15 * 60 * 1000L)
            }
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.channel_description)
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun buildForegroundNotification(): Notification {
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openAppPendingIntent = PendingIntent.getActivity(
            this,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(this, BizliPayService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this,
            1,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val deviceUid = MerchantPreferences.getInstance(this).getDeviceUid()

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("BizliPay Sync Active")
            .setContentText("Monitoring MFS SMS • Device: $deviceUid")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setContentIntent(openAppPendingIntent)
            .addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                "Stop Service",
                stopPendingIntent
            )
            .build()
    }

    companion object {
        const val TAG = "BizliPayService"
        const val CHANNEL_ID = "bizlipay_service_channel"
        const val NOTIFICATION_ID = 1001
        const val ACTION_START = "com.bizlipay.action.START"
        const val ACTION_STOP = "com.bizlipay.action.STOP"

        fun start(context: Context) {
            val intent = Intent(context, BizliPayService::class.java).apply {
                action = ACTION_START
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, BizliPayService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }

        fun isBatteryOptimizationIgnored(context: Context): Boolean {
            val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
            return powerManager.isIgnoringBatteryOptimizations(context.packageName)
        }

        @SuppressLint("BatteryLife")
        fun getBatteryOptimizationIntent(context: Context): Intent {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                try {
                    Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                        data = Uri.parse("package:${context.packageName}")
                    }
                } catch (_: Exception) {
                    Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                }
            } else {
                Intent(Settings.ACTION_SETTINGS)
            }
        }
    }
}

open class BizliPayForegroundService : BizliPayService() {
    companion object {
        fun start(context: Context) = BizliPayService.start(context)
        fun stop(context: Context) = BizliPayService.stop(context)
        fun isBatteryOptimizationIgnored(context: Context) = BizliPayService.isBatteryOptimizationIgnored(context)
        fun getBatteryOptimizationIntent(context: Context) = BizliPayService.getBatteryOptimizationIntent(context)
    }
}
