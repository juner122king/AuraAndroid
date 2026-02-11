package com.aura.football.data.local.dao

import androidx.room.*
import com.aura.football.data.local.entity.LeagueEntity

@Dao
interface LeagueDao {

    @Query("SELECT * FROM leagues WHERE id = :leagueId")
    suspend fun getLeagueById(leagueId: Long): LeagueEntity?

    @Query("SELECT * FROM leagues ORDER BY name ASC")
    suspend fun getAllLeagues(): List<LeagueEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLeagues(leagues: List<LeagueEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLeague(league: LeagueEntity)

    @Query("DELETE FROM leagues")
    suspend fun deleteAll()
}
