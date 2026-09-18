package com.example.data.api

import com.squareup.moshi.Json
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.ResponseBody
import okhttp3.logging.HttpLoggingInterceptor
import org.json.JSONArray
import org.json.JSONObject
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import java.util.concurrent.TimeUnit

data class SmsSyncRequest(
    @Json(name = "source") val source: String = "app",
    @Json(name = "device_id") val deviceId: String,
    @Json(name = "device_key") val deviceKey: String = deviceId,
    @Json(name = "sender") val sender: String,
    @Json(name = "sender_key") val senderKey: String,
    @Json(name = "simslot") val simslot: Int = 1,
    @Json(name = "number") val number: String,
    @Json(name = "sender_number") val senderNumber: String = number,
    @Json(name = "amount") val amount: Double,
    @Json(name = "currency") val currency: String = "BDT",
    @Json(name = "trx_id") val trxId: String,
    @Json(name = "balance") val balance: Double? = null,
    @Json(name = "message") val message: String,
    @Json(name = "raw_sms") val rawSms: String = message,
    @Json(name = "type") val type: String = "received",
    @Json(name = "provider") val provider: String = senderKey.uppercase(),
    @Json(name = "timestamp") val timestamp: Long = System.currentTimeMillis()
)

data class DeviceRegisterRequest(
    @Json(name = "device_id") val deviceId: String,
    @Json(name = "otp") val otp: String,
    @Json(name = "name") val name: String,
    @Json(name = "model") val model: String,
    @Json(name = "android_level") val androidLevel: String,
    @Json(name = "app_version") val appVersion: String,
    @Json(name = "status") val status: String = "active"
)

data class DeviceHeartbeatRequest(
    @Json(name = "device_id") val deviceId: String,
    @Json(name = "status") val status: String = "active",
    @Json(name = "battery_level") val batteryLevel: Int? = null,
    @Json(name = "last_sync") val lastSync: Long = System.currentTimeMillis()
)

data class SmsSyncResponse(
    @Json(name = "success") val success: Boolean? = true,
    @Json(name = "message") val message: String? = null,
    @Json(name = "status") val status: String? = null,
    @Json(name = "trx_id") val trxId: String? = null
)

interface PipraPayApi {

    @POST("api/sms/receive")
    suspend fun syncSmsTransaction(
        @Header("Authorization") authorization: String,
        @Body request: SmsSyncRequest
    ): Response<SmsSyncResponse>

    @POST("api/device/register")
    suspend fun registerDevice(
        @Header("Authorization") authorization: String,
        @Body request: DeviceRegisterRequest
    ): Response<ResponseBody>

    @POST("api/device/connect")
    suspend fun connectDevice(
        @Header("Authorization") authorization: String,
        @Body request: DeviceRegisterRequest
    ): Response<ResponseBody>

    @POST("api/device/sync")
    suspend fun syncHeartbeat(
        @Header("Authorization") authorization: String,
        @Body request: DeviceHeartbeatRequest
    ): Response<ResponseBody>

    @GET("api/ping")
    suspend fun pingServer(
        @Header("Authorization") authorization: String?
    ): Response<ResponseBody>

    @GET("api/health")
    suspend fun healthCheck(): Response<ResponseBody>
}

data class HandshakeVerificationResponse(
    val isSuccess: Boolean,
    val httpCode: Int? = null,
    val errorMessage: String? = null,
    val latencyMs: Long = 0L,
    val token: String? = null
)

object HandshakeAuthenticator {
    private val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(8, TimeUnit.SECONDS)
            .readTimeout(8, TimeUnit.SECONDS)
            .writeTimeout(8, TimeUnit.SECONDS)
            .callTimeout(8, TimeUnit.SECONDS)
            .build()
    }

    suspend fun verify(
        serverUrl: String,
        apiKey: String,
        deviceId: String
    ): HandshakeVerificationResponse = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        val trimmedUrl = serverUrl.trim()
        val cleanBaseUrl = if (trimmedUrl.endsWith("/")) trimmedUrl.dropLast(1) else trimmedUrl

        val verifyUrl = "$cleanBaseUrl/api/v1/companion/verify"
        val pingUrl = "$cleanBaseUrl/api/ping"

        fun doRequest(url: String): HandshakeVerificationResponse {
            val req = Request.Builder()
                .url(url)
                .get()
                .header("X-Merchant-Key", apiKey.trim())
                .header("X-Device-Id", deviceId.trim())
                .header("Accept", "application/json")
                .header("User-Agent", "PipraPay-Companion-Android/1.0")
                .build()

            try {
                client.newCall(req).execute().use { response ->
                    val latency = System.currentTimeMillis() - startTime
                    val code = response.code
                    val body = response.body?.string().orEmpty()

                    if (code in 200..299) {
                        var token: String? = null
                        if (body.isNotBlank()) {
                            try {
                                val json = JSONObject(body)
                                token = json.optString("token").takeIf { it.isNotBlank() }
                            } catch (_: Exception) {}
                        }
                        return HandshakeVerificationResponse(
                            isSuccess = true,
                            httpCode = code,
                            latencyMs = latency,
                            token = token
                        )
                    } else if (code == 401 || code == 403) {
                        return HandshakeVerificationResponse(
                            isSuccess = false,
                            httpCode = code,
                            errorMessage = "Authentication failed: Invalid Merchant API Key.",
                            latencyMs = latency
                        )
                    } else if (code == 404) {
                        return HandshakeVerificationResponse(
                            isSuccess = false,
                            httpCode = 404,
                            errorMessage = "Endpoint not found: Check your Payment Panel URL.",
                            latencyMs = latency
                        )
                    } else {
                        return HandshakeVerificationResponse(
                            isSuccess = false,
                            httpCode = code,
                            errorMessage = "Authentication failed: Server returned HTTP $code.",
                            latencyMs = latency
                        )
                    }
                }
            } catch (e: java.net.SocketTimeoutException) {
                val latency = System.currentTimeMillis() - startTime
                return HandshakeVerificationResponse(
                    isSuccess = false,
                    httpCode = null,
                    errorMessage = "Connection timed out. Check network or server status.",
                    latencyMs = latency
                )
            } catch (e: java.io.InterruptedIOException) {
                val latency = System.currentTimeMillis() - startTime
                return HandshakeVerificationResponse(
                    isSuccess = false,
                    httpCode = null,
                    errorMessage = "Connection timed out. Check network or server status.",
                    latencyMs = latency
                )
            } catch (e: Exception) {
                val latency = System.currentTimeMillis() - startTime
                return HandshakeVerificationResponse(
                    isSuccess = false,
                    httpCode = null,
                    errorMessage = "Connection timed out. Check network or server status.",
                    latencyMs = latency
                )
            }
        }

        // Primary: verifyUrl
        val primaryResult = doRequest(verifyUrl)
        if (primaryResult.isSuccess) {
            return@withContext primaryResult
        }

        // If 404 on verify endpoint, try ping endpoint as fallback
        if (primaryResult.httpCode == 404) {
            val pingResult = doRequest(pingUrl)
            if (pingResult.isSuccess) {
                return@withContext pingResult
            }
            if (pingResult.httpCode == 401 || pingResult.httpCode == 403) {
                return@withContext HandshakeVerificationResponse(
                    isSuccess = false,
                    httpCode = pingResult.httpCode,
                    errorMessage = "Authentication failed: Invalid Merchant API Key.",
                    latencyMs = pingResult.latencyMs
                )
            }
            if (pingResult.httpCode == 404) {
                return@withContext HandshakeVerificationResponse(
                    isSuccess = false,
                    httpCode = 404,
                    errorMessage = "Endpoint not found: Check your Payment Panel URL.",
                    latencyMs = pingResult.latencyMs
                )
            }
            return@withContext pingResult
        }

        return@withContext primaryResult
    }
}

object ApiClient {

    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private fun getOkHttpClient(apiKey: String?): OkHttpClient {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }

        return OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .addInterceptor(loggingInterceptor)
            .addInterceptor { chain ->
                val originalRequest = chain.request()
                val requestBuilder = originalRequest.newBuilder()
                    .addHeader("Content-Type", "application/json")
                    .addHeader("Accept", "application/json")
                if (!apiKey.isNullOrBlank()) {
                    requestBuilder.addHeader("Authorization", "Bearer $apiKey")
                }
                chain.proceed(requestBuilder.build())
            }
            .build()
    }

    fun createApi(baseUrl: String, apiKey: String?): PipraPayApi {
        val sanitizedUrl = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"
        return Retrofit.Builder()
            .baseUrl(sanitizedUrl)
            .client(getOkHttpClient(apiKey))
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(PipraPayApi::class.java)
    }
}

data class CompanionLoginResponse(
    val success: Boolean,
    val token: String? = null,
    val title: String? = null,
    val message: String? = null
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

    private val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .build()
    }

    private fun sanitizeUrl(baseUrl: String): String {
        val trimmed = baseUrl.trim()
        return if (trimmed.endsWith("/")) trimmed else "$trimmed/"
    }

    suspend fun login(
        baseUrl: String,
        otp: String,
        deviceName: String,
        deviceModel: String,
        androidLevel: String,
        appVersion: String = "1.0.0"
    ): CompanionLoginResponse = withContext(Dispatchers.IO) {
        try {
            val url = sanitizeUrl(baseUrl)
            val formBody = FormBody.Builder()
                .add("action-companion", "login")
                .add("onetimepassword", otp.trim())
                .add("name", deviceName)
                .add("model", deviceModel)
                .add("android_level", androidLevel)
                .add("app_version", appVersion)
                .build()

            val request = Request.Builder()
                .url(url)
                .post(formBody)
                .header("User-Agent", "PipraPay-Companion-Android/1.0")
                .header("Accept", "application/json")
                .build()

            client.newCall(request).execute().use { response ->
                val body = response.body?.string().orEmpty()
                if (response.isSuccessful && body.isNotBlank()) {
                    val json = JSONObject(body)
                    val status = json.optString("status")
                    if (status.equals("true", ignoreCase = true)) {
                        CompanionLoginResponse(
                            success = true,
                            token = json.optString("token"),
                            title = "Connected",
                            message = "Paired with PipraPay server successfully"
                        )
                    } else {
                        CompanionLoginResponse(
                            success = false,
                            title = json.optString("title", "Connection Failed"),
                            message = json.optString("message", "Invalid credentials or expired OTP")
                        )
                    }
                } else {
                    CompanionLoginResponse(
                        success = false,
                        title = "HTTP Error ${response.code}",
                        message = "Server returned status ${response.code}"
                    )
                }
            }
        } catch (e: Exception) {
            CompanionLoginResponse(
                success = false,
                title = "Network Exception",
                message = e.localizedMessage ?: "Failed to connect to PipraPay server"
            )
        }
    }

    suspend fun getAccountInformation(
        baseUrl: String,
        token: String
    ): CompanionAccountInfo = withContext(Dispatchers.IO) {
        try {
            val url = sanitizeUrl(baseUrl)
            val formBody = FormBody.Builder()
                .add("action-companion", "account-information")
                .add("token", token.trim())
                .build()

            val request = Request.Builder()
                .url(url)
                .post(formBody)
                .header("User-Agent", "PipraPay-Companion-Android/1.0")
                .header("Accept", "application/json")
                .build()

            client.newCall(request).execute().use { response ->
                val body = response.body?.string().orEmpty()
                if (response.isSuccessful && body.isNotBlank()) {
                    val json = JSONObject(body)
                    val status = json.optString("status")
                    if (status.equals("true", ignoreCase = true)) {
                        fun parseList(key: String): List<CompanionSmsItem> {
                            val arr = json.optJSONArray(key) ?: return emptyList()
                            val list = mutableListOf<CompanionSmsItem>()
                            for (i in 0 until arr.length()) {
                                val obj = arr.optJSONObject(i) ?: continue
                                list.add(
                                    CompanionSmsItem(
                                        id = obj.optString("id"),
                                        sender = obj.optString("sender"),
                                        message = obj.optString("message"),
                                        reason = obj.optString("reason"),
                                        simslot = obj.optString("simslot", "0"),
                                        timestamp = obj.optString("timestamp"),
                                        status = obj.optString("status")
                                    )
                                )
                            }
                            return list
                        }

                        CompanionAccountInfo(
                            success = true,
                            fullname = json.optString("fullname"),
                            email = json.optString("email"),
                            storedCount = json.optInt("stored_count"),
                            usedCount = json.optInt("used_count"),
                            errorCount = json.optInt("error_count"),
                            storedList = parseList("stored"),
                            usedList = parseList("used"),
                            errorList = parseList("error")
                        )
                    } else {
                        CompanionAccountInfo(
                            success = false,
                            errorMessage = json.optString("message", "Failed to retrieve account details")
                        )
                    }
                } else {
                    CompanionAccountInfo(
                        success = false,
                        errorMessage = "HTTP ${response.code}"
                    )
                }
            }
        } catch (e: Exception) {
            CompanionAccountInfo(
                success = false,
                errorMessage = e.localizedMessage
            )
        }
    }

    suspend fun getWhitelistedSenders(
        baseUrl: String,
        token: String
    ): List<String> = withContext(Dispatchers.IO) {
        try {
            val url = sanitizeUrl(baseUrl)
            val formBody = FormBody.Builder()
                .add("action-companion", "sms-transmit-sender")
                .add("token", token.trim())
                .build()

            val request = Request.Builder()
                .url(url)
                .post(formBody)
                .header("User-Agent", "PipraPay-Companion-Android/1.0")
                .header("Accept", "application/json")
                .build()

            client.newCall(request).execute().use { response ->
                val body = response.body?.string().orEmpty()
                if (response.isSuccessful && body.isNotBlank()) {
                    val json = JSONObject(body)
                    val status = json.optString("status")
                    if (status.equals("true", ignoreCase = true)) {
                        val arr = json.optJSONArray("senders")
                        if (arr != null) {
                            val list = mutableListOf<String>()
                            for (i in 0 until arr.length()) {
                                val s = arr.optString(i)
                                if (s.isNotBlank()) list.add(s.trim())
                            }
                            return@withContext list
                        }
                    }
                }
            }
            emptyList()
        } catch (_: Exception) {
            emptyList()
        }
    }

    suspend fun transmitSmsBulk(
        baseUrl: String,
        token: String,
        smsListJson: String
    ): CompanionTransmitResult = withContext(Dispatchers.IO) {
        try {
            val url = sanitizeUrl(baseUrl)
            val formBody = FormBody.Builder()
                .add("action-companion", "sms-transmit-bulk")
                .add("token", token.trim())
                .add("sms_list", smsListJson)
                .build()

            val request = Request.Builder()
                .url(url)
                .post(formBody)
                .header("User-Agent", "PipraPay-Companion-Android/1.0")
                .header("Accept", "application/json")
                .build()

            client.newCall(request).execute().use { response ->
                val body = response.body?.string().orEmpty()
                if (response.isSuccessful && body.isNotBlank()) {
                    val json = JSONObject(body)
                    val status = json.optString("status")
                    val isSuccess = status.equals("true", ignoreCase = true)
                    CompanionTransmitResult(
                        success = isSuccess,
                        title = json.optString("title", if (isSuccess) "SMS Transmitted" else "Transmit Notice"),
                        message = json.optString("message")
                    )
                } else {
                    CompanionTransmitResult(
                        success = false,
                        title = "HTTP ${response.code}",
                        message = "Server returned error status ${response.code}"
                    )
                }
            }
        } catch (e: Exception) {
            CompanionTransmitResult(
                success = false,
                title = "Network Error",
                message = e.localizedMessage ?: "Failed to transmit SMS"
            )
        }
    }
}
