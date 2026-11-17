package com.biomedixai.ricefieldsamplelogger.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "samples")
data class SampleEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val sampleId: String,
    val riceVariety: String,
    val collectionDate: String,
    val latitude: Double,
    val longitude: Double,
    val temperature: Double,
    val humidity: Double,
    val precipitation: Double,
    val soilMoisture: Double,
    val sequenceLength: Int,
    val gcContent: Double,
    val cisElementsJson: String,
    val ssrMarkersJson: String
)
