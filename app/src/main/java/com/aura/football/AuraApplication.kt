package com.aura.football

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class AuraApplication : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    override fun onCreate() {
        super.onCreate()

        // Use self-rescheduling one-time work so polling frequency can adapt to match state.
        setupMatchUpdateWorker()
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    private fun setupMatchUpdateWorker() {
        val updateRequest = OneTimeWorkRequestBuilder<MatchUpdateWorker>()
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .build()

        WorkManager.getInstance(this).enqueueUniqueWork(
            MATCH_UPDATE_WORK_NAME,
            ExistingWorkPolicy.KEEP,
            updateRequest
        )
    }

    companion object {
        const val MATCH_UPDATE_WORK_NAME = "match_update"
        const val ACTIVE_POLLING_MINUTES = 15L
        const val IDLE_POLLING_MINUTES = 120L
    }
}
