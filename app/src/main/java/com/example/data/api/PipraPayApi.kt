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
    @Json(name = "device_key") val deviceKey: String,
    @Json(name = "provider") val provider: String,
    @Json(name = "trx_id") val trxId: String,
    @Json(name = "sender_number") val senderNumber: String,
    @Json(name = "amount") val amount: Double,
    @Json(name = "raw_sms") val rawSms: String,
    @Json(name = "timestamp") val timestamp: Long
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
