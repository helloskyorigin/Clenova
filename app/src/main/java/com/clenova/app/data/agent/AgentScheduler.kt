package com.clenova.app.data.agent

import android.content.Context
import android.os.StatFs
import androidx.work.*
import com.clenova.app.data.preferences.DataStorePreferencesRepository
import com.clenova.app.data.storage.MediaScanner
import com.clenova.app.data.storage.mediaAccess
import com.clenova.app.domain.model.AutopilotStatus
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.TimeUnit

internal object AgentScheduler {
    const val PERIODIC = "clenova_agent_periodic"
    const val IMMEDIATE = "clenova_agent_now"
    fun activate(context: Context, days: Int) {
        val manager = WorkManager.getInstance(context.applicationContext)
        val request = PeriodicWorkRequestBuilder<AgentCheckWorker>(days.toLong(), TimeUnit.DAYS)
            .setInitialDelay(days.toLong(), TimeUnit.DAYS)
            .setConstraints(Constraints.Builder().setRequiresBatteryNotLow(true).build()).build()
        manager.enqueueUniquePeriodicWork(PERIODIC, ExistingPeriodicWorkPolicy.CANCEL_AND_REENQUEUE, request)
        checkNow(context)
    }
    fun checkNow(context: Context) {
        WorkManager.getInstance(context.applicationContext).enqueueUniqueWork(
            IMMEDIATE, ExistingWorkPolicy.REPLACE, OneTimeWorkRequestBuilder<AgentCheckWorker>().build())
    }
    fun pause(context: Context) {
        WorkManager.getInstance(context.applicationContext).apply { cancelUniqueWork(PERIODIC); cancelUniqueWork(IMMEDIATE) }
    }
}

/** Read-only analysis. A worker never launches consent UI or modifies media. */
class AgentCheckWorker(context: Context, parameters: WorkerParameters) : CoroutineWorker(context, parameters) {
    override suspend fun doWork(): Result = mutex.withLock {
        val repo = DataStorePreferencesRepository(applicationContext)
        val prefs = repo.userPreferences.first()
        if (prefs.autopilotStatus != AutopilotStatus.Active) return@withLock Result.success()
        val store = AgentReportStore(applicationContext)
        try {
            val report = if (!mediaAccess(applicationContext).any) AgentReport.problem("permission")
            else {
                val available = StatFs(applicationContext.filesDir.absolutePath).availableBytes
                AgentReport.from(MediaScanner(applicationContext).scan { _, _, _ -> }, available, System.currentTimeMillis())
            }
            kotlinx.coroutines.currentCoroutineContext().let { it[kotlinx.coroutines.Job]?.ensureActive() }
            val current = repo.userPreferences.first()
            if (current.autopilotStatus == AutopilotStatus.Active && current.storageGoalGb == prefs.storageGoalGb && current.checkIntervalDays == prefs.checkIntervalDays)
                store.save(report)
            Result.success()
        } catch (e: CancellationException) { throw e }
        catch (_: Exception) {
            if (repo.userPreferences.first().autopilotStatus == AutopilotStatus.Active) store.save(AgentReport.problem("check"))
            Result.failure()
        }
    }
    companion object { private val mutex = Mutex() }
}
