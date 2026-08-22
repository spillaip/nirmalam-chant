package org.nirmalam.chant.tracking

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.nirmalam.chant.MainActivity
import org.nirmalam.chant.NirmalamApplication
import org.nirmalam.chant.data.TallySource
import java.util.concurrent.atomic.AtomicBoolean

class ChantTrackingService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var detector: VoiceChantDetector? = null
    private val started = AtomicBoolean(false)

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIFICATION_ID, notification())
        if (!started.compareAndSet(false, true)) return START_STICKY
        scope.launch {
            val repository = (application as NirmalamApplication).repository
            val session = repository.getOrCreateActiveSession()
            detector = VoiceChantDetector(this@ChantTrackingService) {
                scope.launch {
                    val result = repository.record(session, TallySource.VOICE)
                    if (result.recorded) ChantFeedback.give(this@ChantTrackingService, result.count)
                    if (result.reachedTarget) stopSelf()
                }
            }.also { it.start() }
        }
        return START_STICKY
    }

    override fun onDestroy() { detector?.stop(); started.set(false); super.onDestroy() }
    override fun onBind(intent: Intent?): IBinder? = null

    private fun notification() = NotificationCompat.Builder(this, CHANNEL_ID)
        .setSmallIcon(android.R.drawable.ic_btn_speak_now)
        .setContentTitle("Nirmalam Chant is listening")
        .setContentText("Voice processing stays on this device")
        .setOngoing(true)
        .setContentIntent(PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE))
        .build()

    companion object {
        private const val CHANNEL_ID = "chant_tracking"
        private const val NOTIFICATION_ID = 108
        fun createChannel(service: android.content.Context) {
            val channel = NotificationChannel(CHANNEL_ID, "Chant tracking", NotificationManager.IMPORTANCE_LOW)
            service.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }
}
