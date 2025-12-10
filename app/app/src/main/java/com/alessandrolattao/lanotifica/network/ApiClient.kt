package com.alessandrolattao.lanotifica.network

import com.alessandrolattao.lanotifica.util.CryptoUtils
import com.alessandrolattao.lanotifica.util.UrlUtils
import okhttp3.Interceptor
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object ApiClient {

    @Volatile
    private var currentBaseUrl: String? = null

    @Volatile
    private var currentToken: String? = null

    @Volatile
    private var currentFingerprint: String? = null

    @Volatile
    private var api: NotificationApi? = null

    fun getApi(baseUrl: String, token: String, expectedFingerprint: String): NotificationApi {
        val normalizedUrl = UrlUtils.normalizeUrl(baseUrl)

        if (api != null &&
            currentBaseUrl == normalizedUrl &&
            currentToken == token &&
            currentFingerprint == expectedFingerprint) {
            return api!!
        }

        synchronized(this) {
            val baseClient = CryptoUtils.createPinnedOkHttpClient(expectedFingerprint)
            val client = baseClient.newBuilder()
                .addInterceptor(authInterceptor(token))
                .addInterceptor(
                    HttpLoggingInterceptor().apply {
                        level = HttpLoggingInterceptor.Level.BODY
                    }
                )
                .build()

            val retrofit = Retrofit.Builder()
                .baseUrl(normalizedUrl)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build()

            currentBaseUrl = normalizedUrl
            currentToken = token
            currentFingerprint = expectedFingerprint
            api = retrofit.create(NotificationApi::class.java)
            return api!!
        }
    }

    private fun authInterceptor(token: String): Interceptor {
        return Interceptor { chain ->
            val request = chain.request().newBuilder()
                .addHeader("Authorization", "Bearer $token")
                .build()
            chain.proceed(request)
        }
    }
}
