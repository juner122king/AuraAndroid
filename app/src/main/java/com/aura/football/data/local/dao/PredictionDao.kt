package com.aura.football.data.local.dao

import androidx.room.*
import com.aura.football.data.local.entity.PredictionEntity

@Dao
interface PredictionDao {

    @Query("SELECT * FROM predictions WHERE match_id = :matchId")
    suspend fun getPredictionByMatchId(matchId: Long): PredictionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPredictions(predictions: List<PredictionEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPrediction(prediction: PredictionEntity)

    @Query("DELETE FROM predictions WHERE match_id IN (:matchIds)")
    suspend fun deletePredictionsByMatchIds(matchIds: List<Long>)

    @Query("DELETE FROM predictions")
    suspend fun deleteAll()
}
