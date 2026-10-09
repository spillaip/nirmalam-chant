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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
internal fun ManualCounter(count: Int, target: Int, complete: Boolean, onTap: () -> Unit, modifier: Modifier = Modifier) {
    val progress = (count.toFloat() / target.coerceAtLeast(1)).coerceIn(0f, 1f)
    Box(modifier.size(264.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize().padding(8.dp)) {
            val line = 11.dp.toPx()
            drawArc(Color(0xFF35574E), -90f, 360f, false, style = Stroke(line, cap = StrokeCap.Round))
            drawArc(Color(0xFFF3C779), -90f, 360f * progress, false, style = Stroke(line, cap = StrokeCap.Round))
        }
        Surface(
            modifier = Modifier.size(218.dp).semantics { contentDescription = if (complete) "Practice completed: $count of $target" else "Tap to count one chant: $count of $target" }
                .clickable(enabled = !complete, onClickLabel = "Add one chant", onClick = onTap),
            shape = CircleShape, color = Color(0xFF12382F), tonalElevation = 6.dp
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                Text("ॐ", fontSize = 36.sp, color = Color(0xFFF3C779))
                Text("$count", fontSize = 80.sp, lineHeight = 92.sp, fontWeight = FontWeight.Light, color = Color.White)
                Text("of $target", style = MaterialTheme.typography.titleMedium, color = Color(0xFFBAD3CA))
                Text(if (complete) "COMPLETE" else "TAP TO CHANT", style = MaterialTheme.typography.labelSmall, color = Color(0xFFF3C779))
            }
        }
    }
}
