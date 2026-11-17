package com.biomedixai.ricefieldsamplelogger.network

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface OpenMeteoApi {

    @GET("v1/forecast")
    suspend fun getWeather(
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double,
        @Query("current") current: String = "temperature_2m,relative_humidity_2m,precipitation,soil_moisture_0_to_1cm",
        @Query("timezone") timezone: String = "auto"
    ): Response<OpenMeteoResponse>
}
