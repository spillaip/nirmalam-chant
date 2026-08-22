package com.nirmalamgroup.nirmalamchant.tracking

import android.content.Context
import org.tensorflow.lite.Interpreter
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel

/**
 * Runs a bundled `assets/chant_classifier.tflite` model entirely on-device when present.
 * Until a validated model is supplied, the calibrated local energy gate remains the safe fallback.
 */
class LocalChantClassifier(private val context: Context) : AutoCloseable {
    private val interpreter: Interpreter? = runCatching {
        context.assets.openFd("chant_classifier.tflite").use { descriptor ->
            descriptor.createInputStream().channel.map(FileChannel.MapMode.READ_ONLY, descriptor.startOffset, descriptor.declaredLength)
        }.let(::Interpreter)
    }.getOrNull()

    fun isChantCandidate(samples: ShortArray, size: Int, rms: Double): Boolean {
        val model = interpreter ?: return rms >= FeedbackPreferences.voiceThreshold(context)
        return runCatching {
            val input = ByteBuffer.allocateDirect(1_600 * Float.SIZE_BYTES).order(ByteOrder.nativeOrder())
            repeat(1_600) { index ->
                val sample = if (index < size) samples[index].toFloat() / Short.MAX_VALUE else 0f
                input.putFloat(sample)
            }
            input.rewind()
            val output = Array(1) { FloatArray(2) }
            model.run(input, output)
            output[0].getOrElse(1) { 0f } >= 0.75f
        }.getOrElse { rms >= FeedbackPreferences.voiceThreshold(context) }
    }

    override fun close() { interpreter?.close() }
}
