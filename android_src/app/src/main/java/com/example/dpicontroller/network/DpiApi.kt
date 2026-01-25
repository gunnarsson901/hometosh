package com.example.dpicontroller.network

import com.example.dpicontroller.model.DpiConfig
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface DpiApi {
    @GET("/api/config")
    suspend fun getConfig(): Response<DpiConfig>

    @POST("/api/config")
    suspend fun saveConfig(@Body config: DpiConfig): Response<Map<String, String>>
    
    @POST("/api/reboot")
    suspend fun reboot(): Response<Map<String, String>>
}
