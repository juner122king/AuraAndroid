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
        PredictionEntity::class
    ],
    version = 4,
    exportSchema = true
)
abstract class AuraDatabase : RoomDatabase() {
    abstract fun matchDao(): MatchDao
    abstract fun teamDao(): TeamDao
    abstract fun leagueDao(): LeagueDao
    abstract fun predictionDao(): PredictionDao

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
    }
}
