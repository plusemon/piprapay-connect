package com.plusemon.bizlipay.data.api

import com.squareup.moshi.Json
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.logging.HttpLoggingInterceptor
import org.json.JSONArray
import org.json.JSONObject
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST
import java.util.concurrent.TimeUnit

// =========================================================================
// BizliPay Device RESTful API Models
// =========================================================================

data class DevicePairRequest(
    @field:Json(name = "otp") val otp: String,
    @field:Json(name = "name") val name: String,
    @field:Json(name = "model") val model: String,
    @field:Json(name = "android_level") val android_level: String,
    @field:Json(name = "app_version") val app_version: String
)

data class DevicePairResponse(
    @field:Json(name = "status") val status: Boolean,
    @field:Json(name = "message") val message: String? = null,
    @field:Json(name = "token") val token: String? = null,
    @field:Json(name = "device_uid") val device_uid: String? = null
)

data class DeviceHeartbeatRequest(
    @field:Json(name = "battery_level") val battery_level: Int,
    @field:Json(name = "app_version") val app_version: String
)

data class DeviceHeartbeatResponse(
    @field:Json(name = "status") val status: Boolean,
    @field:Json(name = "device_name") val device_name: String? = null,
    @field:Json(name = "last_sync") val last_sync: String? = null,
    @field:Json(name = "message") val message: String? = null
)

data class DeviceBatchSmsItem(
    @field:Json(name = "sender") val sender: String,
    @field:Json(name = "message") val message: String,
    @field:Json(name = "sim_slot") val sim_slot: String,
    @field:Json(name = "timestamp") val timestamp: Long
)

data class DeviceBatchSmsRequest(
    @field:Json(name = "messages") val messages: List<DeviceBatchSmsItem>
)

data class DeviceBatchSmsResponse(
    @field:Json(name = "status") val status: Boolean,
    @field:Json(name = "message") val message: String? = null,
    @field:Json(name = "processed_count") val processed_count: Int? = null
)

interface BizliPayDeviceApi {

    @POST("api/v1/device/pair")
    suspend fun pairDevice(
        @Body request: DevicePairRequest
    ): Response<DevicePairResponse>

    @POST("api/v1/device/heartbeat")
    suspend fun sendHeartbeat(
        @Header("Authorization") authorization: String,
        @Body request: DeviceHeartbeatRequest
    ): Response<DeviceHeartbeatResponse>

    @POST("api/v1/device/sms/batch")
    suspend fun sendSmsBatch(
        @Header("Authorization") authorization: String,
        @Body request: DeviceBatchSmsRequest
    ): Response<DeviceBatchSmsResponse>
}

object BizliPayApiClient {

    private const val USER_AGENT = "BizliPay-Connect-Android/1.0"
    private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()

    private val moshi: Moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    val okHttpClient: OkHttpClient by lazy {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .addInterceptor(logging)
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .header("User-Agent", USER_AGENT)
                    .header("Accept", "application/json")
                    .header("Content-Type", "application/json")
                    .build()
                chain.proceed(request)
            }
            .build()
    }

    fun createDeviceApi(baseUrl: String): BizliPayDeviceApi {
        val sanitizedUrl = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"
        return Retrofit.Builder()
            .baseUrl(sanitizedUrl)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(BizliPayDeviceApi::class.java)
    }

    /**
     * Raw OkHttp implementation of pairing endpoint: POST /api/v1/device/pair
     */
    suspend fun pair(
        baseUrl: String,
        otp: String,
        deviceName: String,
        deviceModel: String,
        androidLevel: String,
        appVersion: String = "v1.0.0"
    ): PairResult = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        val cleanUrl = baseUrl.trim().removeSuffix("/")
        val endpoint = "$cleanUrl/api/v1/device/pair"

        val jsonBody = JSONObject().apply {
            put("otp", otp.trim())
            put("name", deviceName)
            put("model", deviceModel)
            put("android_level", androidLevel)
            put("app_version", appVersion)
        }.toString()

        val request = Request.Builder()
            .url(endpoint)
            .post(jsonBody.toRequestBody(JSON_MEDIA_TYPE))
            .header("User-Agent", USER_AGENT)
            .header("Accept", "application/json")
            .header("Content-Type", "application/json")
            .build()

        try {
            okHttpClient.newCall(request).execute().use { response ->
                val latency = System.currentTimeMillis() - startTime
                val code = response.code
                val bodyStr = response.body?.string().orEmpty()

                if (response.isSuccessful && bodyStr.isNotBlank()) {
                    val json = JSONObject(bodyStr)
                    val status = json.optBoolean("status", false) || json.optString("status").equals("true", ignoreCase = true)
                    val message = json.optString("message", if (status) "Device paired successfully" else "Pairing failed")
                    val token = json.optString("token")
                    val deviceUid = json.optString("device_uid")

                    if (status && token.isNotBlank()) {
                        PairResult(
                            isSuccess = true,
                            httpCode = code,
                            token = token,
                            deviceUid = deviceUid.ifBlank { "dev_" + otp.take(6) },
                            message = message,
                            latencyMs = latency
                        )
                    } else {
                        PairResult(
                            isSuccess = false,
                            httpCode = code,
                            message = message.ifBlank { "Invalid or expired OTP code" },
                            latencyMs = latency
                        )
                    }
                } else if (code == 401 || code == 403) {
                    PairResult(
                        isSuccess = false,
                        httpCode = code,
                        message = "Invalid pairing OTP or unauthorized request",
                        latencyMs = latency
                    )
                } else {
                    PairResult(
                        isSuccess = false,
                        httpCode = code,
                        message = "Server returned error (HTTP $code)",
                        latencyMs = latency
                    )
                }
            }
        } catch (e: Exception) {
            val latency = System.currentTimeMillis() - startTime
            PairResult(
                isSuccess = false,
                httpCode = null,
                message = e.localizedMessage ?: "Connection error. Check Server URL and network.",
                latencyMs = latency
            )
        }
    }

    /**
     * Heartbeat & Health Telemetry: POST /api/v1/device/heartbeat
     */
    suspend fun heartbeat(
        baseUrl: String,
        token: String,
        batteryLevel: Int,
        appVersion: String = "v1.0.0"
    ): HeartbeatResult = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        val cleanUrl = baseUrl.trim().removeSuffix("/")
        val endpoint = "$cleanUrl/api/v1/device/heartbeat"

        val jsonBody = JSONObject().apply {
            put("battery_level", batteryLevel)
            put("app_version", appVersion)
        }.toString()

        val request = Request.Builder()
            .url(endpoint)
            .post(jsonBody.toRequestBody(JSON_MEDIA_TYPE))
            .header("Authorization", "Bearer ${token.trim()}")
            .header("User-Agent", USER_AGENT)
            .header("Accept", "application/json")
            .header("Content-Type", "application/json")
            .build()

        try {
            okHttpClient.newCall(request).execute().use { response ->
                val latency = System.currentTimeMillis() - startTime
                val code = response.code
                val bodyStr = response.body?.string().orEmpty()

                if (response.isSuccessful && bodyStr.isNotBlank()) {
                    val json = JSONObject(bodyStr)
                    val status = json.optBoolean("status", true)
                    val deviceName = json.optString("device_name")
                    val lastSync = json.optString("last_sync")

                    HeartbeatResult(
                        isSuccess = status,
                        httpCode = code,
                        deviceName = deviceName,
                        lastSync = lastSync,
                        isUnauthorized = false,
                        latencyMs = latency
                    )
                } else if (code == 401) {
                    HeartbeatResult(
                        isSuccess = false,
                        httpCode = 401,
                        isUnauthorized = true,
                        errorMessage = "Device session revoked (HTTP 401)",
                        latencyMs = latency
                    )
                } else {
                    HeartbeatResult(
                        isSuccess = false,
                        httpCode = code,
                        isUnauthorized = false,
                        errorMessage = "HTTP $code: ${response.message}",
                        latencyMs = latency
                    )
                }
            }
        } catch (e: Exception) {
            val latency = System.currentTimeMillis() - startTime
            HeartbeatResult(
                isSuccess = false,
                httpCode = null,
                isUnauthorized = false,
                errorMessage = e.localizedMessage ?: "Network error during heartbeat",
                latencyMs = latency
            )
        }
    }

    /**
     * Batch SMS Ingestion: POST /api/v1/device/sms/batch
     */
    suspend fun sendBatchSms(
        baseUrl: String,
        token: String,
        items: List<DeviceBatchSmsItem>
    ): BatchSmsResult = withContext(Dispatchers.IO) {
        val cleanUrl = baseUrl.trim().removeSuffix("/")
        val endpoint = "$cleanUrl/api/v1/device/sms/batch"

        val jsonArray = JSONArray()
        for (item in items) {
            val obj = JSONObject().apply {
                put("sender", item.sender)
                put("message", item.message)
                put("sim_slot", item.sim_slot)
                put("timestamp", item.timestamp)
            }
            jsonArray.put(obj)
        }

        val jsonBody = JSONObject().apply {
            put("messages", jsonArray)
        }.toString()

        val request = Request.Builder()
            .url(endpoint)
            .post(jsonBody.toRequestBody(JSON_MEDIA_TYPE))
            .header("Authorization", "Bearer ${token.trim()}")
            .header("User-Agent", USER_AGENT)
            .header("Accept", "application/json")
            .header("Content-Type", "application/json")
            .build()

        try {
            okHttpClient.newCall(request).execute().use { response ->
                val code = response.code
                val bodyStr = response.body?.string().orEmpty()

                if (response.isSuccessful && bodyStr.isNotBlank()) {
                    val json = JSONObject(bodyStr)
                    val status = json.optBoolean("status", true)
                    val message = json.optString("message", "Successfully processed")
                    val processedCount = json.optInt("processed_count", items.size)

                    BatchSmsResult(
                        isSuccess = status,
                        httpCode = code,
                        message = message,
                        processedCount = processedCount,
                        isUnauthorized = false
                    )
                } else if (code == 401) {
                    BatchSmsResult(
                        isSuccess = false,
                        httpCode = 401,
                        message = "Unauthorized: Session token invalid or expired",
                        isUnauthorized = true
                    )
                } else {
                    BatchSmsResult(
                        isSuccess = false,
                        httpCode = code,
                        message = "HTTP $code: ${response.message}",
                        isUnauthorized = false
                    )
                }
            }
        } catch (e: Exception) {
            BatchSmsResult(
                isSuccess = false,
                httpCode = null,
                message = e.localizedMessage ?: "Failed to transmit SMS batch",
                isUnauthorized = false
            )
        }
    }
}

data class PairResult(
    val isSuccess: Boolean,
    val httpCode: Int? = null,
    val token: String? = null,
    val deviceUid: String? = null,
    val message: String = "",
    val latencyMs: Long = 0L
)

data class HeartbeatResult(
    val isSuccess: Boolean,
    val httpCode: Int? = null,
    val deviceName: String? = null,
    val lastSync: String? = null,
    val isUnauthorized: Boolean = false,
    val errorMessage: String? = null,
    val latencyMs: Long = 0L
)

data class BatchSmsResult(
    val isSuccess: Boolean,
    val httpCode: Int? = null,
    val message: String = "",
    val processedCount: Int = 0,
    val isUnauthorized: Boolean = false
)
