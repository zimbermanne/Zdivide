package com.example.dualscreen.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface CalibrationDao {
    @Query("SELECT * FROM calibration_profiles WHERE id = :id")
    suspend fun getProfileById(id: String): CalibrationProfile?

    @Query("SELECT * FROM calibration_profiles")
    fun getAllProfiles(): Flow<List<CalibrationProfile>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveProfile(profile: CalibrationProfile)

    @Query("DELETE FROM calibration_profiles WHERE id = :id")
    suspend fun deleteProfile(id: String)
}
