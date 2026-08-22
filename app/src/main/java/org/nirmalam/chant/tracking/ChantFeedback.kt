package org.nirmalam.chant.tracking

import android.content.Context
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

object ChantFeedback {
    fun give(context: Context, count: Int) {
        pulse(context, count)
        if (FeedbackPreferences.isSoundEnabled(context)) MeditationTone.play()
    }

    private fun pulse(context: Context, count: Int) {
        val vibrator = if (android.os.Build.VERSION.SDK_INT >= 31) {
            context.getSystemService(VibratorManager::class.java).defaultVibrator
        } else {
            @Suppress("DEPRECATION") context.getSystemService(Vibrator::class.java)
        }
        if (!vibrator.hasVibrator()) return
        val effect = if (count % 108 == 0) {
            VibrationEffect.createWaveform(longArrayOf(0, 35, 70, 55), -1)
        } else {
            VibrationEffect.createOneShot(22, VibrationEffect.DEFAULT_AMPLITUDE)
        }
        vibrator.vibrate(effect)
    }
}
