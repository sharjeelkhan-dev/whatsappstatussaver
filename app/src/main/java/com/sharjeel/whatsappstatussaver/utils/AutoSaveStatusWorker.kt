package com.sharjeel.whatsappstatussaver.utils

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.sharjeel.whatsappstatussaver.data.local.datastore.AppSettings
import com.sharjeel.whatsappstatussaver.data.models.PlatformType
import com.sharjeel.whatsappstatussaver.data.repository.StorageRepository
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.firstOrNull
import java.util.concurrent.TimeUnit

class AutoSaveStatusWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface AutoSaveWorkerEntryPoint {
        fun storageRepository(): StorageRepository
        fun appSettings(): AppSettings
    }

    override suspend fun doWork(): Result {
        return try {
            val entryPoint = EntryPointAccessors.fromApplication(
                context.applicationContext,
                AutoSaveWorkerEntryPoint::class.java
            )
            val storageRepository = entryPoint.storageRepository()
            val appSettings = entryPoint.appSettings()

            val isAutoSaveEnabled = appSettings.autoSaveEnabledFlow.firstOrNull() ?: false
            if (!isAutoSaveEnabled) {
                return Result.success()
            }

            Log.d("AutoSaveWorker", "Running background Auto-Save status check...")
            val platforms = listOf(PlatformType.WHATSAPP, PlatformType.WHATSAPP_BUSINESS)

            var autoSavedCount = 0
            for (platform in platforms) {
                val (statuses, _) = storageRepository.getStatuses(platform)
                for (status in statuses) {
                    val saved = storageRepository.saveStatus(status)
                    if (saved) autoSavedCount++
                }
            }

            Log.d("AutoSaveWorker", "Auto-Save check complete. New statuses saved: $autoSavedCount")
            Result.success()
        } catch (e: Exception) {
            Log.e("AutoSaveWorker", "Error executing auto-save background job", e)
            Result.failure()
        }
    }

    companion object {
        private const val WORK_NAME = "AutoSaveStatusWorker"

        fun scheduleAutoSaveWorker(context: Context, enable: Boolean) {
            val workManager = WorkManager.getInstance(context)
            if (enable) {
                val workRequest = PeriodicWorkRequestBuilder<AutoSaveStatusWorker>(
                    15, TimeUnit.MINUTES
                ).build()
                workManager.enqueueUniquePeriodicWork(
                    WORK_NAME,
                    ExistingPeriodicWorkPolicy.KEEP,
                    workRequest
                )
            } else {
                workManager.cancelUniqueWork(WORK_NAME)
            }
        }
    }
}