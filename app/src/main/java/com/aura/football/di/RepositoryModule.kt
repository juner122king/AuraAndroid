package com.aura.football.di

import com.aura.football.data.repository.LeagueRepositoryImpl
import com.aura.football.data.repository.MatchRepositoryImpl
import com.aura.football.data.repository.TeamRepositoryImpl
import com.aura.football.domain.repository.LeagueRepository
import com.aura.football.domain.repository.MatchRepository
import com.aura.football.domain.repository.TeamRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindMatchRepository(
        impl: MatchRepositoryImpl
    ): MatchRepository

    @Binds
    @Singleton
    abstract fun bindLeagueRepository(
        impl: LeagueRepositoryImpl
    ): LeagueRepository

    @Binds
    @Singleton
    abstract fun bindTeamRepository(
        impl: TeamRepositoryImpl
    ): TeamRepository
}
