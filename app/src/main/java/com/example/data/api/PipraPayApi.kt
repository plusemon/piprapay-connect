package com.example.data.api

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Compatibility bridge redirecting legacy references to BizliPayApiClient RESTful endpoints.
 */
data class CompanionLoginResponse(
    val success: Boolean,
    val token: String? = null,
    val device_uid: String? = null,
    val title: String? = null,
    val message: String? = null
)

data class HandshakeVerificationResponse(
    val isSuccess: Boolean,
    val httpCode: Int? = null,
    val errorMessage: String? = null,
    val latencyMs: Long = 0L,
    val token: String? = null
)

data class CompanionAccountInfo(
    val success: Boolean,
    val fullname: String = "",
    val email: String = "",
    val storedCount: Int = 0,
    val usedCount: Int = 0,
    val errorCount: Int = 0,
    val storedList: List<CompanionSmsItem> = emptyList(),
    val usedList: List<CompanionSmsItem> = emptyList(),
    val errorList: List<CompanionSmsItem> = emptyList(),
    val errorMessage: String? = null
)

data class CompanionSmsItem(
    val id: String = "",
    val sender: String = "",
    val message: String = "",
    val reason: String = "",
    val simslot: String = "0",
    val timestamp: String = "",
    val status: String = ""
)

data class CompanionTransmitResult(
    val success: Boolean,
    val title: String? = null,
    val message: String? = null
)

object PipraPayCompanionClient {

    suspend fun login(
        baseUrl: String,
        otp: String,
        deviceName: String,
        deviceModel: String,
        androidLevel: String,
        appVersion: String = "v1.0.0"
    ): CompanionLoginResponse = withContext(Dispatchers.IO) {
        val result = BizliPayApiClient.pair(
            baseUrl = baseUrl,
            otp = otp,
            deviceName = deviceName,
            deviceModel = deviceModel,
            androidLevel = androidLevel,
            appVersion = appVersion
        )
        CompanionLoginResponse(
            success = result.isSuccess,
            token = result.token,
            device_uid = result.deviceUid,
            title = if (result.isSuccess) "Device Paired" else "Pairing Failed",
            message = result.message
        )
    }

    suspend fun getAccountInformation(
        baseUrl: String,
        token: String
    ): CompanionAccountInfo = withContext(Dispatchers.IO) {
        // Ping heartbeat to verify session
        val hb = BizliPayApiClient.heartbeat(baseUrl, token, 100)
        if (hb.isSuccess) {
            CompanionAccountInfo(
                success = true,
                fullname = hb.deviceName ?: "BizliPay Merchant",
                email = "Connected Device"
            )
        } else {
            CompanionAccountInfo(
                success = false,
                errorMessage = hb.errorMessage
            )
        }
    }

    suspend fun getWhitelistedSenders(
        baseUrl: String,
        token: String
    ): List<String> = listOf("bkash", "nagad", "16216", "upay", "tap", "ibbl")

    suspend fun transmitSmsBulk(
        baseUrl: String,
        token: String,
        smsListJson: String
    ): CompanionTransmitResult = withContext(Dispatchers.IO) {
        // Parse items and send via RESTful batch endpoint
        val items = mutableListOf<DeviceBatchSmsItem>()
        try {
            val arr = org.json.JSONArray(smsListJson)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                items.add(
                    DeviceBatchSmsItem(
                        sender = obj.optString("sender"),
                        message = obj.optString("message"),
                        sim_slot = obj.optString("simSlot", "1"),
                        timestamp = obj.optLong("timestamp", System.currentTimeMillis() / 1000L)
                    )
                )
            }
        } catch (_: Exception) {}

        val batchResult = BizliPayApiClient.sendBatchSms(baseUrl, token, items)
        CompanionTransmitResult(
            success = batchResult.isSuccess,
            title = if (batchResult.isSuccess) "Batch Transmitted" else "Transmit Error",
            message = batchResult.message
        )
    }
}

object HandshakeAuthenticator {
    suspend fun verify(
        serverUrl: String,
        apiKey: String,
        deviceId: String
    ): HandshakeVerificationResponse = withContext(Dispatchers.IO) {
        val result = BizliPayApiClient.pair(
            baseUrl = serverUrl,
            otp = apiKey,
            deviceName = android.os.Build.MODEL ?: "Android Device",
            deviceModel = android.os.Build.MODEL ?: "Device",
            androidLevel = "API ${android.os.Build.VERSION.SDK_INT} (Android ${android.os.Build.VERSION.RELEASE})"
        )
        HandshakeVerificationResponse(
            isSuccess = result.isSuccess,
            httpCode = result.httpCode,
            errorMessage = if (!result.isSuccess) result.message else null,
            latencyMs = result.latencyMs,
            token = result.token
        )
    }
}

object ApiClient {
    fun createApi(baseUrl: String, apiKey: String?): BizliPayDeviceApi {
        return BizliPayApiClient.createDeviceApi(baseUrl)
    }
}
