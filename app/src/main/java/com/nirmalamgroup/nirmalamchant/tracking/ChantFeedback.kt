package com.nirmalamgroup.nirmalamchant.tracking

import android.content.Context
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

object ChantFeedback {
    fun give(context: Context, count: Int) {
        if (FeedbackPreferences.isHapticsEnabled(context)) pulse(context, count)
        if (FeedbackPreferences.isSoundEnabled(context)) MeditationTone.play()
    }

    private fun pulse(context: Context, count: Int) {
        val vibrator = if (android.os.Build.VERSION.SDK_INT >= 31) {
            context.getSystemService(VibratorManager::class.java).defaultVibrator
        } else {
            @Suppress("DEPRECATION") context.getSystemService(Vibrator::class.java)
        }
        if (!vibrator.hasVibrator()) return
        val effect = if (count == 27 || count == 54 || count == 108) {
            VibrationEffect.createWaveform(longArrayOf(0, 24, 55, 32), -1)
        } else {
            VibrationEffect.createOneShot(12, VibrationEffect.DEFAULT_AMPLITUDE)
        }
        vibrator.vibrate(effect)
    }
}
