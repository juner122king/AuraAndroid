package com.aura.football.data.local.dao

import androidx.room.*
import com.aura.football.data.local.entity.MatchEntity
import com.aura.football.data.local.entity.MatchWithRelations
import kotlinx.coroutines.flow.Flow

@Dao
interface MatchDao {

    @Query("""
        SELECT
            m.*,
            ht.id as ht_id, ht.name as ht_name, ht.logo_url as ht_logo_url, ht.short_name as ht_short_name,
            at.id as at_id, at.name as at_name, at.logo_url as at_logo_url, at.short_name as at_short_name,
            l.id as lg_id, l.name as lg_name, l.country as lg_country, l.logo_url as lg_logo_url,
            p.match_id as pred_match_id, p.model_version as pred_model_version,
            p.home_win_prob as pred_home_win_prob, p.draw_prob as pred_draw_prob,
            p.away_win_prob as pred_away_win_prob, p.confidence as pred_confidence,
            p.explanation as pred_explanation
        FROM matches m
        INNER JOIN teams ht ON m.home_team_id = ht.id
        INNER JOIN teams at ON m.away_team_id = at.id
        INNER JOIN leagues l ON m.league_id = l.id
        LEFT JOIN predictions p ON m.id = p.match_id
        WHERE m.match_time BETWEEN :startDate AND :endDate
        ORDER BY m.match_time ASC
    """)
    fun getMatches(startDate: String, endDate: String): Flow<List<MatchWithRelations>>

    @Query("""
        SELECT
            m.*,
            ht.id as ht_id, ht.name as ht_name, ht.logo_url as ht_logo_url, ht.short_name as ht_short_name,
            at.id as at_id, at.name as at_name, at.logo_url as at_logo_url, at.short_name as at_short_name,
            l.id as lg_id, l.name as lg_name, l.country as lg_country, l.logo_url as lg_logo_url,
            p.match_id as pred_match_id, p.model_version as pred_model_version,
            p.home_win_prob as pred_home_win_prob, p.draw_prob as pred_draw_prob,
            p.away_win_prob as pred_away_win_prob, p.confidence as pred_confidence,
            p.explanation as pred_explanation
        FROM matches m
        INNER JOIN teams ht ON m.home_team_id = ht.id
        INNER JOIN teams at ON m.away_team_id = at.id
        INNER JOIN leagues l ON m.league_id = l.id
        LEFT JOIN predictions p ON m.id = p.match_id
        WHERE m.id = :matchId
    """)
    suspend fun getMatchById(matchId: Long): MatchWithRelations?

    @Query("SELECT * FROM matches WHERE status = 'live'")
    suspend fun getLiveMatches(): List<MatchEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMatches(matches: List<MatchEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMatch(match: MatchEntity)

    @Query("DELETE FROM matches WHERE match_time < :date")
    suspend fun deleteOldMatches(date: String)

    @Query("DELETE FROM matches")
    suspend fun deleteAll()
}
