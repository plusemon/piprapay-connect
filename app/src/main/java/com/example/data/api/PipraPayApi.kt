package com.example.data.api

import com.squareup.moshi.Json
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.ResponseBody
import okhttp3.logging.HttpLoggingInterceptor
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
