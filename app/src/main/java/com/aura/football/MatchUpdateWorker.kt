package com.aura.football

import android.content.Context
import androidx.work.Constraints
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.aura.football.data.local.dao.MatchDao
import com.aura.football.domain.repository.MatchRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.time.LocalDate
import java.util.concurrent.TimeUnit

@HiltWorker
class MatchUpdateWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val repository: MatchRepository,
    private val matchDao: MatchDao
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            repository.updateLiveMatches()

            val cutoffDate = LocalDate.now().minusDays(90).toString()
            matchDao.deleteOldMatches(cutoffDate)
            scheduleNextRun()

            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }

    private suspend fun scheduleNextRun() {
        val today = LocalDate.now()
        val tomorrow = today.plusDays(1)
        val hasLiveMatches = matchDao.countLiveMatches() > 0
        val hasMatchesToday = matchDao.countScheduledMatchesBetween(
            today.toString(),
            tomorrow.toString()
        ) > 0

        val delayMinutes = if (hasLiveMatches || hasMatchesToday) {
            AuraApplication.ACTIVE_POLLING_MINUTES
        } else {
            AuraApplication.IDLE_POLLING_MINUTES
        }

        val nextRequest = OneTimeWorkRequestBuilder<MatchUpdateWorker>()
            .setInitialDelay(delayMinutes, TimeUnit.MINUTES)
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .build()

        WorkManager.getInstance(applicationContext).enqueueUniqueWork(
            AuraApplication.MATCH_UPDATE_WORK_NAME,
            ExistingWorkPolicy.REPLACE,
            nextRequest
        )
    }
}
