package com.koyeresolutions.ksexpire.workers

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.koyeresolutions.ksexpire.data.database.AppDatabase
import com.koyeresolutions.ksexpire.notifications.NotificationManager

/**
 * Worker para reprogramar notificaciones después del reinicio
 * Se ejecuta en segundo plano para restaurar todas las alarmas
 */
class NotificationRescheduleWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            val database = AppDatabase.getDatabase(applicationContext)
            val allItems = database.itemDao().getAllItemsForBackup()
            
            val notificationManager = NotificationManager(applicationContext)
            notificationManager.rescheduleAllNotifications(allItems)
            
            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.retry()
        }
    }
}