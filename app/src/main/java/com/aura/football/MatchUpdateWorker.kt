package com.aura.football

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.aura.football.data.local.dao.MatchDao
import com.aura.football.domain.repository.MatchRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.time.LocalDate

@HiltWorker
class MatchUpdateWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val repository: MatchRepository,
    private val matchDao: MatchDao
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            // Update all live matches
            repository.updateLiveMatches()

            // Clean up old match data (older than 90 days)
            val cutoffDate = LocalDate.now().minusDays(90).toString()
            matchDao.deleteOldMatches(cutoffDate)

            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.retry()
        }
    }
}
