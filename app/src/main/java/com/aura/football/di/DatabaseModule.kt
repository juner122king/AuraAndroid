package com.aura.football.di

import android.content.Context
import androidx.room.Room
import com.aura.football.data.local.AuraDatabase
import com.aura.football.data.local.dao.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAuraDatabase(
        @ApplicationContext context: Context
    ): AuraDatabase {
        return Room.databaseBuilder(
            context,
            AuraDatabase::class.java,
            "aura_database"
        )
            .addMigrations(
                AuraDatabase.MIGRATION_1_2,
                AuraDatabase.MIGRATION_2_3,
                AuraDatabase.MIGRATION_3_4
            )
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    @Singleton
    fun provideMatchDao(database: AuraDatabase): MatchDao {
        return database.matchDao()
    }

    @Provides
    @Singleton
    fun provideTeamDao(database: AuraDatabase): TeamDao {
        return database.teamDao()
    }

    @Provides
    @Singleton
    fun provideLeagueDao(database: AuraDatabase): LeagueDao {
        return database.leagueDao()
    }

    @Provides
    @Singleton
    fun providePredictionDao(database: AuraDatabase): PredictionDao {
        return database.predictionDao()
    }
}
