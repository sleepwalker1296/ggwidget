package com.example.ggwidget.worker

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

object WidgetUpdateScheduler {
    fun scheduleWidgetUpdate(context: Context) {
        val workRequest = PeriodicWorkRequestBuilder<WidgetUpdateWorker>(10, TimeUnit.MINUTES)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            "WidgetUpdateWork",
            ExistingPeriodicWorkPolicy.REPLACE, // Исправляем ошибку!
            workRequest
        )
    }
}
