package com.koyeresolutions.ksexpire.notifications

import android.app.AlarmManager
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.koyeresolutions.ksexpire.MainActivity
import com.koyeresolutions.ksexpire.R
import com.koyeresolutions.ksexpire.data.entities.Item
import com.koyeresolutions.ksexpire.utils.Constants
import com.koyeresolutions.ksexpire.utils.CurrencyUtils
import java.util.Calendar
import java.util.concurrent.TimeUnit

/**
 * Manager para notificaciones locales
 *
 * Cada recordatorio se dispara a las [REMINDER_HOUR] (hora local) del día que corresponde.
 * Si ese momento ya pasó pero el vencimiento aún no, el recordatorio se muestra de inmediato
 * (una sola vez por vencimiento), en lugar de descartarse en silencio.
 */
class NotificationManager(private val context: Context) {

    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    private val sentPrefs = context.getSharedPreferences(SENT_PREFS_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val TAG = "NotificationManager"
        private const val SUBSCRIPTION_NOTIFICATION_ID_BASE = 100000
        private const val WARRANTY_30_DAYS_NOTIFICATION_ID_BASE = 200000
        private const val WARRANTY_7_DAYS_NOTIFICATION_ID_BASE = 300000
        private const val FREE_TRIAL_2_DAYS_NOTIFICATION_ID_BASE = 400000
        private const val FREE_TRIAL_TODAY_NOTIFICATION_ID_BASE = 500000

        /** Hora local a la que se disparan los recordatorios */
        const val REMINDER_HOUR = 9

        const val EXTRA_ITEM_ID = "item_id"
        const val EXTRA_NOTIFICATION_TYPE = "notification_type"
        const val EXTRA_DUE_DATE = "due_date"

        private const val SENT_PREFS_NAME = "ks_expire_sent_notifications"

        /** Inicio (00:00) del día de [timestamp] en hora local */
        fun startOfDay(timestamp: Long): Long = Calendar.getInstance().apply {
            timeInMillis = timestamp
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        /** Fin del día de [timestamp]: el ítem sigue vigente hasta ese momento */
        fun endOfDay(timestamp: Long): Long = Calendar.getInstance().apply {
            timeInMillis = startOfDay(timestamp)
            add(Calendar.DAY_OF_YEAR, 1)
        }.timeInMillis - 1

        /** Días de calendario entre hoy y [dueDate] (0 = hoy, 1 = mañana) */
        fun calendarDaysUntil(dueDate: Long, now: Long = System.currentTimeMillis()): Int {
            val diff = startOfDay(dueDate) - startOfDay(now)
            // Redondear para tolerar días de 23/25 h por cambio de horario
            return Math.round(diff / TimeUnit.DAYS.toMillis(1).toDouble()).toInt()
        }
    }

    /**
     * Recordatorio: tipo + cuántos días antes del vencimiento se dispara
     */
    private data class Reminder(val type: NotificationType, val daysBefore: Int)

    /**
     * Momento de disparo: [REMINDER_HOUR] del día (dueDate - daysBefore)
     */
    private fun triggerTime(dueDate: Long, daysBefore: Int): Long = Calendar.getInstance().apply {
        timeInMillis = startOfDay(dueDate)
        add(Calendar.DAY_OF_YEAR, -daysBefore)
        set(Calendar.HOUR_OF_DAY, REMINDER_HOUR)
    }.timeInMillis

    /**
     * Programar un grupo de recordatorios para un vencimiento.
     * - Los que caen en el futuro se programan como alarma.
     * - Si alguno ya pasó (p. ej. el ítem se creó con poca anticipación), se muestra ahora
     *   el más cercano al vencimiento, siempre que no se haya mostrado antes.
     */
    private fun scheduleReminders(item: Item, dueDate: Long, reminders: List<Reminder>) {
        val now = System.currentTimeMillis()
        if (now > endOfDay(dueDate)) return

        val (missed, upcoming) = reminders.partition { triggerTime(dueDate, it.daysBefore) <= now }

        upcoming.forEach { reminder ->
            scheduleAlarmSafely(
                triggerTime(dueDate, reminder.daysBefore),
                createAlarmPendingIntent(item, reminder.type, dueDate)
            )
        }

        missed.minByOrNull { it.daysBefore }?.let { reminder ->
            if (!wasSent(item, reminder.type, dueDate)) {
                showNotification(item, reminder.type, dueDate)
            }
        }
    }

    /**
     * Programar alarma exacta de forma segura
     * Verifica permisos en Android 12+ antes de programar
     * Si no tiene permiso de alarma exacta, usa alarma inexacta como fallback
     */
    private fun scheduleAlarmSafely(triggerTime: Long, pendingIntent: PendingIntent) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
                // Fallback: alarma inexacta (puede retrasarse algunos minutos)
                Log.w(TAG, "Sin permiso de alarma exacta, usando setAndAllowWhileIdle")
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
            } else {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException al programar alarma: ${e.message}")
            try {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
            } catch (e2: Exception) {
                Log.e(TAG, "Error total al programar alarma: ${e2.message}")
            }
        }
    }

    /**
     * Programar notificación para suscripción
     * Notifica 1 día antes del cobro (configurable)
     */
    fun scheduleSubscriptionNotification(item: Item, daysBefore: Int = Constants.DEFAULT_SUBSCRIPTION_REMINDER_DAYS) {
        if (!item.isSubscription() || !item.isActive) return
        scheduleReminders(item, item.expiryDate, listOf(Reminder(NotificationType.SUBSCRIPTION, daysBefore)))
    }

    /**
     * Programar notificaciones para garantía
     * Notifica 30 días y 7 días antes del vencimiento (configurable)
     */
    fun scheduleWarrantyNotifications(item: Item, schedule30Days: Boolean = true, schedule7Days: Boolean = true) {
        if (!item.isWarranty() || !item.isActive) return

        val reminders = buildList {
            if (schedule30Days) add(Reminder(NotificationType.WARRANTY_30_DAYS, Constants.DEFAULT_WARRANTY_REMINDER_DAYS_1))
            if (schedule7Days) add(Reminder(NotificationType.WARRANTY_7_DAYS, Constants.DEFAULT_WARRANTY_REMINDER_DAYS_2))
        }
        scheduleReminders(item, item.expiryDate, reminders)
    }

    /**
     * Programar notificaciones para período de prueba gratuita
     * Notifica 2 días antes y el mismo día del vencimiento
     */
    fun scheduleFreeTrialNotifications(item: Item) {
        val trialEnd = item.freeTrialEndDate
        if (!item.isFreeTrial || trialEnd == null || !item.isActive) return

        scheduleReminders(
            item,
            trialEnd,
            listOf(
                Reminder(NotificationType.FREE_TRIAL_2_DAYS, 2),
                Reminder(NotificationType.FREE_TRIAL_TODAY, 0)
            )
        )
    }

    /**
     * Cancelar notificaciones de prueba gratuita
     */
    fun cancelFreeTrialNotifications(item: Item) {
        cancelNotification(item, NotificationType.FREE_TRIAL_2_DAYS)
        cancelNotification(item, NotificationType.FREE_TRIAL_TODAY)
    }

    /**
     * Cancelar notificaciones de garantía
     */
    fun cancelWarrantyNotifications(item: Item) {
        cancelNotification(item, NotificationType.WARRANTY_30_DAYS)
        cancelNotification(item, NotificationType.WARRANTY_7_DAYS)
    }

    /**
     * Cancelar notificación de suscripción
     */
    fun cancelSubscriptionNotification(item: Item) {
        cancelNotification(item, NotificationType.SUBSCRIPTION)
    }

    /**
     * Cancelar todas las notificaciones de un ítem
     */
    fun cancelItemNotifications(item: Item) {
        cancelSubscriptionNotification(item)
        cancelWarrantyNotifications(item)
        cancelFreeTrialNotifications(item)
    }

    /**
     * Cancelar notificación específica
     * El PendingIntent se identifica por requestCode + action + componente (los extras no cuentan)
     */
    private fun cancelNotification(item: Item, type: NotificationType) {
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            getNotificationId(item, type),
            createNotificationIntent(item, type, 0L),
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )

        pendingIntent?.let {
            alarmManager.cancel(it)
            it.cancel()
        }
    }

    /**
     * Mostrar notificación inmediata y registrarla como enviada
     * @param dueDate vencimiento al que se refiere el recordatorio
     */
    fun showNotification(item: Item, type: NotificationType, dueDate: Long = dueDateFor(item, type)) {
        val daysLeft = calendarDaysUntil(dueDate).coerceAtLeast(0)
        val notification = when (type) {
            NotificationType.SUBSCRIPTION -> createSubscriptionNotification(item, daysLeft)
            NotificationType.WARRANTY_30_DAYS,
            NotificationType.WARRANTY_7_DAYS -> createWarrantyNotification(item, daysLeft)
            NotificationType.FREE_TRIAL_2_DAYS -> createFreeTrialWarningNotification(item, daysLeft)
            NotificationType.FREE_TRIAL_TODAY -> createFreeTrialTodayNotification(item)
        }

        try {
            notificationManager.notify(getNotificationId(item, type), notification.build())
            markSent(item, type, dueDate)
        } catch (e: SecurityException) {
            // Sin permiso POST_NOTIFICATIONS: no marcar como enviada para reintentar luego
            Log.w(TAG, "Sin permiso para mostrar notificaciones: ${e.message}")
        }
    }

    /**
     * Fecha de vencimiento a la que apunta cada tipo de recordatorio
     */
    fun dueDateFor(item: Item, type: NotificationType): Long = when (type) {
        NotificationType.FREE_TRIAL_2_DAYS,
        NotificationType.FREE_TRIAL_TODAY -> item.freeTrialEndDate ?: item.expiryDate
        else -> item.expiryDate
    }

    /**
     * Texto relativo: "hoy", "mañana" o "en N días"
     */
    private fun dueLabel(daysLeft: Int): String = when (daysLeft) {
        0 -> context.getString(R.string.notification_due_today)
        1 -> context.getString(R.string.notification_due_tomorrow)
        else -> context.getString(R.string.notification_due_in_days, daysLeft)
    }

    /**
     * Crear notificación para suscripción
     */
    private fun createSubscriptionNotification(item: Item, daysLeft: Int): NotificationCompat.Builder {
        val title = context.getString(R.string.notification_subscription_title, item.name)
        val text = if (item.price != null) {
            context.getString(
                R.string.notification_subscription_text,
                CurrencyUtils.formatPrice(context, item.price),
                dueLabel(daysLeft)
            )
        } else {
            context.getString(R.string.notification_subscription_text_no_price, dueLabel(daysLeft))
        }

        return NotificationCompat.Builder(context, NotificationChannels.SUBSCRIPTIONS_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_subscription)
            .setContentTitle(title)
            .setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(createMainActivityIntent())
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
    }

    /**
     * Crear notificación para garantía
     */
    private fun createWarrantyNotification(item: Item, daysLeft: Int): NotificationCompat.Builder {
        val title = context.getString(R.string.notification_warranty_title, item.name)
        val text = context.getString(R.string.notification_warranty_text, dueLabel(daysLeft))

        return NotificationCompat.Builder(context, NotificationChannels.WARRANTIES_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_warranty)
            .setContentTitle(title)
            .setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(createMainActivityIntent())
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
    }

    /**
     * Crear notificación para prueba gratuita (antes del fin)
     */
    private fun createFreeTrialWarningNotification(item: Item, daysLeft: Int): NotificationCompat.Builder {
        val title = context.getString(R.string.notification_free_trial_warning_title, item.name)
        val text = if (item.price != null) {
            context.getString(
                R.string.notification_free_trial_warning_text_price,
                dueLabel(daysLeft),
                CurrencyUtils.formatPrice(context, item.price)
            )
        } else {
            context.getString(R.string.notification_free_trial_warning_text, dueLabel(daysLeft))
        }

        return NotificationCompat.Builder(context, NotificationChannels.FREE_TRIAL_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_warning)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(createMainActivityIntent())
            .setCategory(NotificationCompat.CATEGORY_ALARM)
    }

    /**
     * Crear notificación para prueba gratuita (mismo día)
     */
    private fun createFreeTrialTodayNotification(item: Item): NotificationCompat.Builder {
        val title = context.getString(R.string.notification_free_trial_today_title, item.name)
        val text = if (item.price != null) {
            context.getString(
                R.string.notification_free_trial_today_text_price,
                item.name,
                CurrencyUtils.formatPrice(context, item.price)
            )
        } else {
            context.getString(R.string.notification_free_trial_today_text, item.name)
        }

        return NotificationCompat.Builder(context, NotificationChannels.FREE_TRIAL_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_warning)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(createMainActivityIntent())
            .setCategory(NotificationCompat.CATEGORY_ALARM)
    }

    /**
     * Crear intent para notificación
     */
    private fun createNotificationIntent(item: Item, type: NotificationType, dueDate: Long): Intent {
        return Intent(context, NotificationReceiver::class.java).apply {
            putExtra(EXTRA_ITEM_ID, item.id)
            putExtra(EXTRA_NOTIFICATION_TYPE, type.ordinal)
            putExtra(EXTRA_DUE_DATE, dueDate)
            action = "com.koyeresolutions.ksexpire.NOTIFICATION_ACTION"
        }
    }

    private fun createAlarmPendingIntent(item: Item, type: NotificationType, dueDate: Long): PendingIntent {
        return PendingIntent.getBroadcast(
            context,
            getNotificationId(item, type),
            createNotificationIntent(item, type, dueDate),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    /**
     * Crear intent para abrir la app principal
     */
    private fun createMainActivityIntent(): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        return PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    /**
     * Generar ID único para notificación
     * Cada tipo tiene su propio rango de 100000 IDs para evitar colisiones
     */
    private fun getNotificationId(item: Item, type: NotificationType): Int {
        return when (type) {
            NotificationType.SUBSCRIPTION -> SUBSCRIPTION_NOTIFICATION_ID_BASE + item.id.toInt()
            NotificationType.WARRANTY_30_DAYS -> WARRANTY_30_DAYS_NOTIFICATION_ID_BASE + item.id.toInt()
            NotificationType.WARRANTY_7_DAYS -> WARRANTY_7_DAYS_NOTIFICATION_ID_BASE + item.id.toInt()
            NotificationType.FREE_TRIAL_2_DAYS -> FREE_TRIAL_2_DAYS_NOTIFICATION_ID_BASE + item.id.toInt()
            NotificationType.FREE_TRIAL_TODAY -> FREE_TRIAL_TODAY_NOTIFICATION_ID_BASE + item.id.toInt()
        }
    }

    // ==================== REGISTRO DE ENVIADAS ====================
    // Evita repetir el mismo recordatorio cada vez que se reprograma (al abrir la app, a diario, etc.)

    private fun sentKey(item: Item, type: NotificationType, dueDate: Long) =
        "${type.name}_${item.id}_${startOfDay(dueDate)}"

    fun wasSent(item: Item, type: NotificationType, dueDate: Long): Boolean =
        sentPrefs.contains(sentKey(item, type, dueDate))

    private fun markSent(item: Item, type: NotificationType, dueDate: Long) {
        sentPrefs.edit().putLong(sentKey(item, type, dueDate), dueDate).apply()
    }

    /**
     * Eliminar registros de vencimientos que ya pasaron
     */
    fun pruneSentRecords() {
        val cutoff = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(2)
        val editor = sentPrefs.edit()
        sentPrefs.all.forEach { (key, value) ->
            if (value !is Long || value < cutoff) editor.remove(key)
        }
        editor.apply()
    }

    enum class NotificationType {
        SUBSCRIPTION,
        WARRANTY_30_DAYS,
        WARRANTY_7_DAYS,
        FREE_TRIAL_2_DAYS,
        FREE_TRIAL_TODAY
    }
}
