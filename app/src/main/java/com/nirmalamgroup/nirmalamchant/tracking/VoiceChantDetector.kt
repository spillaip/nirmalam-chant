package com.nirmalamgroup.nirmalamchant.tracking

import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import kotlin.math.sqrt

/**
 * Audio buffers never leave the device and are never persisted. A bundled TFLite model is used
 * when available; the locally configured energy gate remains the intentional offline fallback.
 */
class VoiceChantDetector(context: android.content.Context, private val onChant: () -> Unit) {
    @Volatile private var running = false
    private var lastDetectionMs = 0L
    private val classifier = LocalChantClassifier(context.applicationContext)

    fun start() {
        if (running) return
        running = true
        Thread(::captureLoop, "LocalVoiceDetector").start()
    }

    fun stop() { running = false }

    private fun captureLoop() {
        val sampleRate = 16_000
        val minBuffer = AudioRecord.getMinBufferSize(
            sampleRate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT
        ).coerceAtLeast(sampleRate / 2)
        val recorder = AudioRecord(
            MediaRecorder.AudioSource.VOICE_RECOGNITION, sampleRate,
            AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, minBuffer
        )
        val buffer = ShortArray(minBuffer / 2)
        try {
            recorder.startRecording()
            while (running) {
                val read = recorder.read(buffer, 0, buffer.size)
                val now = android.os.SystemClock.elapsedRealtime()
                if (read > 0 && isChantCandidate(buffer, read) && now - lastDetectionMs >= 250) {
                    lastDetectionMs = now // supports up to 240 CPM without double tallying
                    onChant()
                }
            }
        } finally {
            recorder.stop()
            recorder.release()
            classifier.close()
        }
    }

    private fun isChantCandidate(buffer: ShortArray, size: Int): Boolean {
        val rms = sqrt((0 until size).sumOf { buffer[it].toDouble() * buffer[it] } / size)
        return classifier.isChantCandidate(buffer, size, rms)
    }
}
