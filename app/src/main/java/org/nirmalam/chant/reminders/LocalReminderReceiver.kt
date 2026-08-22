package org.nirmalam.chant.reminders

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import org.nirmalam.chant.MainActivity

/** Local-only, user-created reminder. It never contacts a server or account. */
class LocalReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        createChannel(context)
        val openApp = PendingIntent.getActivity(context, 0, Intent(context, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE)
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(intent.getStringExtra(EXTRA_TITLE) ?: "Practice time")
            .setContentText("Your planned Nirmalam chant session is ready.")
            .setContentIntent(openApp)
            .setAutoCancel(true)
            .build()
        context.getSystemService(NotificationManager::class.java).notify(intent.getIntExtra(EXTRA_ID, 0), notification)
    }

    companion object {
        const val CHANNEL_ID = "local_practice_reminders"
        const val EXTRA_TITLE = "title"
        const val EXTRA_ID = "id"
        fun createChannel(context: Context) {
            context.getSystemService(NotificationManager::class.java).createNotificationChannel(
                NotificationChannel(CHANNEL_ID, "Practice reminders", NotificationManager.IMPORTANCE_DEFAULT)
            )
        }
    }
}

object LocalReminderScheduler {
    fun schedule(context: Context, planId: String, title: String, atMillis: Long) {
        val requestCode = planId.hashCode()
        val intent = Intent(context, LocalReminderReceiver::class.java)
            .putExtra(LocalReminderReceiver.EXTRA_TITLE, title)
            .putExtra(LocalReminderReceiver.EXTRA_ID, requestCode)
        val pending = PendingIntent.getBroadcast(context, requestCode, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val alarm = context.getSystemService(AlarmManager::class.java)
        // Inexact delivery protects battery life and avoids exact-alarm privileges.
        alarm.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, atMillis, pending)
    }
}
