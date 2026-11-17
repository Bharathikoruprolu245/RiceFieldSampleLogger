package com.biomedixai.ricefieldsamplelogger.network

import com.google.gson.annotations.SerializedName

data class OpenMeteoResponse(
    val latitude: Double,
    val longitude: Double,
    val elevation: Double?,
    val current: CurrentWeather?
)

data class CurrentWeather(
    val time: String?,
    val interval: Int?,
    @SerializedName("temperature_2m")
    val temperature2m: Double?,
    @SerializedName("relative_humidity_2m")
    val relativeHumidity2m: Double?,
    val precipitation: Double?,
    @SerializedName("soil_moisture_0_to_1cm")
    val soilMoisture0To1cm: Double?
)
