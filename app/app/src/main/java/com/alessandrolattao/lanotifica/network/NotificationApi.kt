package com.alessandrolattao.lanotifica.network

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.HTTP
import retrofit2.http.POST

data class NotificationRequest(
    val key: String,
    val app_name: String,
    val package_name: String,
    val title: String,
    val message: String
)

data class DismissRequest(
    val key: String
)

data class NotificationResponse(
    val status: String
)

interface NotificationApi {
    @POST("/notification")
    suspend fun sendNotification(@Body request: NotificationRequest): Response<NotificationResponse>

    @HTTP(method = "DELETE", path = "/notification/dismiss", hasBody = true)
    suspend fun dismissNotification(@Body request: DismissRequest): Response<NotificationResponse>
}
