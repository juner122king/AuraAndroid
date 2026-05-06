package com.aura.football.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.aura.football.data.local.dao.*
import com.aura.football.data.local.entity.*

@Database(
    entities = [
        MatchEntity::class,
        TeamEntity::class,
        LeagueEntity::class,
        PredictionEntity::class,
        StandingEntity::class,
        LeagueTeamCrossRef::class
    ],
    version = 5,
    exportSchema = true
)
abstract class AuraDatabase : RoomDatabase() {
    abstract fun matchDao(): MatchDao
    abstract fun teamDao(): TeamDao
    abstract fun leagueDao(): LeagueDao
    abstract fun predictionDao(): PredictionDao
    abstract fun standingDao(): StandingDao
    abstract fun leagueTeamDao(): LeagueTeamDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // 添加 name_zh 和 short_name_zh 列到 teams 表
                database.execSQL("ALTER TABLE teams ADD COLUMN name_zh TEXT")
                database.execSQL("ALTER TABLE teams ADD COLUMN short_name_zh TEXT")
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // 添加 round 和 round_number 列到 matches 表
                database.execSQL("ALTER TABLE matches ADD COLUMN round TEXT")
                database.execSQL("ALTER TABLE matches ADD COLUMN round_number INTEGER")
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // 重命名 leagues 表的 logo_url 列为 emblem_url
                database.execSQL("ALTER TABLE leagues RENAME COLUMN logo_url TO emblem_url")
            }
        }

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `standings` (
                        `league_id` INTEGER NOT NULL,
                        `team_id` INTEGER NOT NULL,
                        `position` INTEGER NOT NULL,
                        `played` INTEGER NOT NULL,
                        `won` INTEGER NOT NULL,
                        `drawn` INTEGER NOT NULL,
                        `lost` INTEGER NOT NULL,
                        `goals_for` INTEGER NOT NULL,
                        `goals_against` INTEGER NOT NULL,
                        `goal_difference` INTEGER NOT NULL,
                        `points` INTEGER NOT NULL,
                        `updated_at` INTEGER NOT NULL,
                        PRIMARY KEY(`league_id`, `team_id`)
                    )
                    """
                )
                database.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_standings_league_id` ON `standings` (`league_id`)"
                )
                database.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_standings_team_id` ON `standings` (`team_id`)"
                )
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `league_team_cross_refs` (
                        `league_id` INTEGER NOT NULL,
                        `team_id` INTEGER NOT NULL,
                        `updated_at` INTEGER NOT NULL,
                        PRIMARY KEY(`league_id`, `team_id`)
                    )
                    """
                )
                database.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_league_team_cross_refs_league_id` ON `league_team_cross_refs` (`league_id`)"
                )
                database.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_league_team_cross_refs_team_id` ON `league_team_cross_refs` (`team_id`)"
                )
            }
        }
    }
}
