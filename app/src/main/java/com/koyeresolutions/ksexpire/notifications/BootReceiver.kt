package com.koyeresolutions.ksexpire.notifications

import android.app.AlarmManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.koyeresolutions.ksexpire.workers.NotificationRescheduleWorker

/**
 * Receptor para eventos del sistema que invalidan las alarmas
 * (reinicio, actualización de la app, concesión del permiso de alarmas exactas)
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED -> {
                NotificationRescheduleWorker.runNow(context)
            }
        }
    }
}
