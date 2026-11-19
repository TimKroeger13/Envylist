package app.neidliste.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import app.neidliste.container
import java.util.concurrent.TimeUnit

/** Moves ticked-off tasks into the archive once their short grace period is over. */
class ArchiveWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val stillPending = applicationContext.container.repository.sweep()
        if (stillPending) {
            WorkManager.getInstance(applicationContext).enqueueUniqueWork(
                NAME,
                ExistingWorkPolicy.APPEND_OR_REPLACE,
                OneTimeWorkRequestBuilder<ArchiveWorker>().setInitialDelay(3, TimeUnit.SECONDS).build(),
            )
        }
        return Result.success()
    }

    companion object {
        const val NAME = "archive-sweep"
    }
}
