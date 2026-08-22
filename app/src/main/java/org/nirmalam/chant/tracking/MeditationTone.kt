package org.nirmalam.chant.tracking

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlin.math.PI
import kotlin.math.sin
import java.util.concurrent.atomic.AtomicBoolean

/** A brief synthesized tone; it has no audio asset, recording, or network dependency. */
object MeditationTone {
    private const val sampleRate = 22_050
    private val playing = AtomicBoolean(false)

    fun play() {
        if (!playing.compareAndSet(false, true)) return
        Thread({
            val frames = (sampleRate * 0.22).toInt()
            val data = ShortArray(frames)
            for (frame in data.indices) {
                val t = frame.toDouble() / sampleRate
                val envelope = if (t < 0.02) t / 0.02 else (1.0 - (t - 0.02) / 0.20).coerceAtLeast(0.0)
                val wave = sin(2 * PI * 432.0 * t) + 0.18 * sin(2 * PI * 864.0 * t)
                data[frame] = (wave * envelope * 2_800).toInt().toShort()
            }
            val track = AudioTrack.Builder()
                .setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION).build())
                .setAudioFormat(AudioFormat.Builder().setSampleRate(sampleRate).setEncoding(AudioFormat.ENCODING_PCM_16BIT).setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build())
                .setBufferSizeInBytes(data.size * Short.SIZE_BYTES)
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build()
            try { track.write(data, 0, data.size); track.play(); Thread.sleep(240) } finally { track.release(); playing.set(false) }
        }, "MeditationTone").start()
    }
}

object FeedbackPreferences {
    private const val PREFS = "meditation_feedback"
    private const val SOUND_ENABLED = "sound_enabled"
    private const val HAPTICS_ENABLED = "haptics_enabled"
    private const val VOICE_THRESHOLD = "voice_threshold"
    fun isSoundEnabled(context: Context): Boolean = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(SOUND_ENABLED, false)
    fun setSoundEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putBoolean(SOUND_ENABLED, enabled).apply()
    }
    fun isHapticsEnabled(context: Context): Boolean = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(HAPTICS_ENABLED, true)
    fun setHapticsEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putBoolean(HAPTICS_ENABLED, enabled).apply()
    }
    fun voiceThreshold(context: Context): Float = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getFloat(VOICE_THRESHOLD, 450f)
    fun setVoiceThreshold(context: Context, threshold: Float) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putFloat(VOICE_THRESHOLD, threshold.coerceIn(100f, 8_000f)).apply()
    }
}
