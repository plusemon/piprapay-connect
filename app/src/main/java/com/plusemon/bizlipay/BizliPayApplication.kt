package com.plusemon.bizlipay

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.plusemon.bizlipay.data.db.AppDatabase
import com.plusemon.bizlipay.data.prefs.MerchantPreferences
import com.plusemon.bizlipay.service.BizliPayService

class BizliPayApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        instance = this

        // Warm up Room DB & encrypted preferences
        AppDatabase.getInstance(this)
        val prefs = MerchantPreferences.getInstance(this)

        createNotificationChannels()

        // Auto-start foreground service if enabled by merchant
        if (prefs.isServiceEnabled()) {
            try {
                BizliPayService.start(this)
            } catch (e: Exception) {
                // Background start restriction may apply until user opens UI
            }
        }
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(NotificationManager::class.java)

            // Channel for foreground service
            val serviceChannel = NotificationChannel(
                BizliPayService.CHANNEL_ID,
                getString(R.string.channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.channel_description)
                setShowBadge(false)
            }

            // Channel for received transaction alerts
            val trxChannel = NotificationChannel(
                "bizlipay_trx_channel",
                "Transaction Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Real-time alerts when MFS SMS is detected"
                setShowBadge(true)
            }

            manager.createNotificationChannel(serviceChannel)
            manager.createNotificationChannel(trxChannel)
        }
    }

    companion object {
        lateinit var instance: BizliPayApplication
            private set
    }
}

typealias PipraPayApplication = BizliPayApplication
