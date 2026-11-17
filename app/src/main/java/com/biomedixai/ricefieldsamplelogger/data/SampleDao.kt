package com.biomedixai.ricefieldsamplelogger.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface SampleDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSample(sample: SampleEntity): Long

    @Update
    suspend fun updateSample(sample: SampleEntity)

    @Delete
    suspend fun deleteSample(sample: SampleEntity)

    @Query("SELECT * FROM samples ORDER BY id DESC")
    fun getAllSamples(): Flow<List<SampleEntity>>

    @Query("SELECT * FROM samples WHERE riceVariety = :variety ORDER BY id DESC")
    fun getSamplesByVariety(variety: String): Flow<List<SampleEntity>>

    @Query("SELECT * FROM samples WHERE id = :id LIMIT 1")
    suspend fun getSampleById(id: Long): SampleEntity?
}
