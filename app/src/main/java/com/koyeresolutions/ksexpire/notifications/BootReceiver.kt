package com.koyeresolutions.ksexpire.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.koyeresolutions.ksexpire.workers.NotificationRescheduleWorker

/**
 * Receptor para eventos de reinicio del sistema
 * Reprograma todas las notificaciones después del reinicio
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            Intent.ACTION_PACKAGE_REPLACED -> {
                // Programar trabajo para reprogramar notificaciones
                scheduleNotificationReschedule(context)
            }
        }
    }

    /**
     * Programar trabajo para reprogramar notificaciones
     */
    private fun scheduleNotificationReschedule(context: Context) {
        val workRequest = OneTimeWorkRequestBuilder<NotificationRescheduleWorker>()
            .build()
        
        WorkManager.getInstance(context).enqueue(workRequest)
    }
}