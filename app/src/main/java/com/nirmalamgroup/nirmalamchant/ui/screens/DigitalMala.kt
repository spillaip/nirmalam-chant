package com.nirmalamgroup.nirmalamchant.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/** A 108-bead visual representation. The tap surface remains the same size as the original counter. */
@Composable
internal fun DigitalMala(count: Int, target: Int, complete: Boolean, onTap: () -> Unit) {
    val beadsLit = if (complete) 108 else (count % 108).coerceIn(0, 108)
    Box(Modifier.size(264.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2, size.height / 2)
            val radius = size.minDimension * 0.42f
            val beadRadius = size.minDimension * 0.012f
            repeat(108) { index ->
                val angle = (index.toFloat() / 108f * 2f * PI - PI / 2).toFloat()
                val position = Offset(center.x + cos(angle) * radius, center.y + sin(angle) * radius)
                drawCircle(if (index < beadsLit) Color(0xFFF3C779) else Color(0xFF4E7065), beadRadius, position)
            }
        }
        Surface(
            modifier = Modifier.size(205.dp)
                .semantics { contentDescription = "Digital mala: $count of $target chants. ${if (complete) "Complete" else "Tap to add one chant"}" }
                .clickable(enabled = !complete, onClickLabel = "Add one chant", onClick = onTap),
            color = Color(0xFF12382F), shape = CircleShape, tonalElevation = 6.dp
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                Text("ॐ", fontSize = 32.sp, color = Color(0xFFF3C779))
                Text("$count", fontSize = 72.sp, fontWeight = FontWeight.Light, color = Color.White)
                Text("of $target", style = MaterialTheme.typography.titleMedium, color = Color(0xFFBAD3CA))
                Text(if (complete) "COMPLETE" else "TAP TO CHANT", style = MaterialTheme.typography.labelSmall, color = Color(0xFFF3C779))
            }
        }
    }
}
