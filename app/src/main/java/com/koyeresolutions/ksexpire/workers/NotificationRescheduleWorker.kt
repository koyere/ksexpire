package com.koyeresolutions.ksexpire.workers

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.koyeresolutions.ksexpire.services.NotificationService
import java.util.concurrent.TimeUnit

/**
 * Worker para reprogramar notificaciones
 * Se ejecuta tras reinicio/actualización y una vez al día como red de seguridad:
 * restaura alarmas que el sistema haya descartado y muestra recordatorios perdidos
 */
class NotificationRescheduleWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            NotificationService(applicationContext).rescheduleAllNotifications()
            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.retry()
        }
    }

    companion object {
        private const val DAILY_WORK_NAME = "daily_notification_check"

        /**
         * Ejecutar una reprogramación inmediata
         */
        fun runNow(context: Context) {
            WorkManager.getInstance(context)
                .enqueue(OneTimeWorkRequestBuilder<NotificationRescheduleWorker>().build())
        }

        /**
         * Programar la revisión diaria (no se duplica si ya existe)
         */
        fun scheduleDaily(context: Context) {
            val request = PeriodicWorkRequestBuilder<NotificationRescheduleWorker>(1, TimeUnit.DAYS).build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                DAILY_WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }
    }
}
