package org.nirmalam.chant

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.Switch
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.nirmalam.chant.tracking.ChantTrackingService
import org.nirmalam.chant.ui.NirmalamTheme
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()
    private var scheduleAfterNotificationPermission = false
    private val microphonePermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) startTracking()
    }
    private val notificationPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) {
        if (scheduleAfterNotificationPermission) viewModel.planEveningPractice()
        scheduleAfterNotificationPermission = false
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val count by viewModel.count.collectAsStateWithLifecycle()
            val dashboard by viewModel.dashboard.collectAsStateWithLifecycle()
            val meditationToneEnabled by viewModel.meditationToneEnabled.collectAsStateWithLifecycle()
            NirmalamTheme {
                ChantHome(count, dashboard, meditationToneEnabled, viewModel::addManualTally, viewModel::saveIntention, viewModel::setMeditationToneEnabled, ::planPractice, ::requestTracking)
            }
        }
    }

    private fun requestTracking() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) startTracking()
        else microphonePermission.launch(Manifest.permission.RECORD_AUDIO)
    }

    private fun startTracking() {
        ChantTrackingService.createChannel(this)
        ContextCompat.startForegroundService(this, Intent(this, ChantTrackingService::class.java))
    }

    private fun planPractice() {
        if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            scheduleAfterNotificationPermission = true
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            viewModel.planEveningPractice()
        }
    }
}

@androidx.compose.runtime.Composable
private fun ChantHome(count: Int, dashboard: DashboardState, meditationToneEnabled: Boolean, onAdd: () -> Unit, onSaveIntention: (String) -> Unit, onToneChange: (Boolean) -> Unit, onPlan: () -> Unit, onStart: () -> Unit) {
    var intention by remember { mutableStateOf("") }
    Box(Modifier.fillMaxSize()) {
        MysticBackground()
        LazyColumn(
        modifier = Modifier.fillMaxWidth().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
            OmMark()
            Spacer(Modifier.height(22.dp))
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.fillMaxWidth().padding(28.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("TODAY'S CHANTS", style = MaterialTheme.typography.labelLarge)
                    Text(count.toString(), fontSize = 112.sp, lineHeight = 118.sp, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.primary)
                    Text("of 108", style = MaterialTheme.typography.titleMedium)
                    MalaProgress(count)
                }
            }
            Spacer(Modifier.height(24.dp))
            Button(onClick = onStart, modifier = Modifier.fillMaxWidth().height(56.dp)) { Text("Start private voice tracking") }
            Spacer(Modifier.height(12.dp))
            Button(
                onClick = onAdd, modifier = Modifier.fillMaxWidth().height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surface)
            ) { Text("Add one manually") }
            Spacer(Modifier.height(12.dp))
            TextField(
                value = intention, onValueChange = { intention = it }, modifier = Modifier.fillMaxWidth(),
                label = { Text("Today's intention") }, singleLine = true
            )
            Spacer(Modifier.height(8.dp))
            Button(onClick = { onSaveIntention(intention) }, modifier = Modifier.fillMaxWidth()) { Text("Save intention") }
            Spacer(Modifier.height(12.dp))
            MeditationToneCard(meditationToneEnabled, onToneChange)
            Spacer(Modifier.height(12.dp))
            Text("No ads. No analytics. Audio never leaves this device.", textAlign = TextAlign.Center, style = MaterialTheme.typography.bodySmall)
            }
        }
        item { RhythmCard(dashboard.streakDays, dashboard.sessionsThisWeek) }
        item { DashboardHeading("Planned & scheduled") }
        if (dashboard.planned.isEmpty()) {
            item {
                Button(onClick = onPlan, modifier = Modifier.fillMaxWidth()) { Text("Plan an evening practice") }
            }
        } else {
            items(dashboard.planned, key = { it.id }) { plan ->
                ActivityCard(plan.title, "Scheduled ${formatActivityTime(plan.scheduledFor)} · ${plan.targetCount} chants")
            }
            item {
                Button(onClick = onPlan, modifier = Modifier.fillMaxWidth()) { Text("Plan another evening practice") }
            }
        }
        item { DashboardHeading("Activities performed") }
        if (dashboard.performed.isEmpty()) {
            item { EmptyActivityCard("Your completed chant sessions will appear here.") }
        } else {
            items(dashboard.performed, key = { it.id }) { activity ->
                ActivityCard(activity.title, buildString {
                    append("${activity.tallyCount} of ${activity.targetCount} chants · ${formatActivityTime(activity.startedAt)}")
                    activity.intention?.let { append("\nIntention: $it") }
                })
            }
        }
    }
    }
}

@androidx.compose.runtime.Composable
private fun DashboardHeading(title: String) = Text(
    title, modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
    style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold
)

@androidx.compose.runtime.Composable
private fun ActivityCard(title: String, details: String) = Card(Modifier.fillMaxWidth()) {
    Column(Modifier.padding(18.dp)) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(4.dp))
        Text(details, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.secondary)
    }
}

@androidx.compose.runtime.Composable
private fun EmptyActivityCard(message: String) = Card(Modifier.fillMaxWidth()) {
    Text(message, modifier = Modifier.padding(18.dp), color = MaterialTheme.colorScheme.secondary)
}

@androidx.compose.runtime.Composable
private fun RhythmCard(streakDays: Int, sessionsThisWeek: Int) = Card(Modifier.fillMaxWidth()) {
    Column(Modifier.padding(18.dp)) {
        Text("Your rhythm", style = MaterialTheme.typography.titleMedium)
        Text("$streakDays-day local streak · $sessionsThisWeek sessions in the last 7 days", color = MaterialTheme.colorScheme.secondary)
    }
}

@androidx.compose.runtime.Composable
private fun MeditationToneCard(enabled: Boolean, onChange: (Boolean) -> Unit) = Card(Modifier.fillMaxWidth()) {
    androidx.compose.foundation.layout.Row(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(Modifier.padding(end = 16.dp)) {
            Text("Meditation tone", style = MaterialTheme.typography.titleMedium)
            Text("A soft local tone after each chant", color = MaterialTheme.colorScheme.secondary, style = MaterialTheme.typography.bodySmall)
        }
        Switch(checked = enabled, onCheckedChange = onChange)
    }
}

@androidx.compose.runtime.Composable
private fun MalaProgress(count: Int) = Canvas(Modifier.fillMaxWidth().height(42.dp).padding(top = 12.dp)) {
    val beadCount = 27
    val spacing = size.width / beadCount
    val radius = (spacing * 0.25f).coerceAtMost(size.height / 3)
    repeat(beadCount) { index ->
        val progressAtBead = (index + 1) * 4
        drawCircle(
            color = if (count >= progressAtBead) Color(0xFFF0BE71) else Color(0xFF4B625D),
            radius = radius,
            center = androidx.compose.ui.geometry.Offset(spacing * index + spacing / 2, size.height / 2)
        )
    }
}

@androidx.compose.runtime.Composable
private fun OmMark() {
    val transition = rememberInfiniteTransition(label = "omPulse")
    val scale by transition.animateFloat(
        initialValue = 0.96f, targetValue = 1.04f,
        animationSpec = infiniteRepeatable(tween(3_400, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "omScale"
    )
    Text(
        "ॐ", modifier = Modifier.graphicsLayer(scaleX = scale, scaleY = scale),
        fontSize = 78.sp, color = Color(0xFFFFD894), textAlign = TextAlign.Center
    )
}

@androidx.compose.runtime.Composable
private fun MysticBackground() {
    val transition = rememberInfiniteTransition(label = "mysticBackground")
    val phase by transition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(14_000, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "backgroundPhase"
    )
    Canvas(Modifier.fillMaxSize()) {
        drawRect(Brush.verticalGradient(listOf(Color(0xFF071314), Color(0xFF142D35), Color(0xFF251837))))
        drawCircle(Color(0x554E8AFF), radius = size.minDimension * (0.30f + phase * 0.10f), center = androidx.compose.ui.geometry.Offset(size.width * (0.18f + phase * 0.2f), size.height * 0.18f))
        drawCircle(Color(0x44F2B86B), radius = size.minDimension * (0.24f + (1f - phase) * 0.10f), center = androidx.compose.ui.geometry.Offset(size.width * (0.78f - phase * 0.2f), size.height * 0.68f))
        drawCircle(Color(0x33FFD5AC), radius = size.minDimension * 0.12f, center = androidx.compose.ui.geometry.Offset(size.width * 0.5f, size.height * (0.42f + phase * 0.12f)))
    }
}

private val activityTimeFormatter = DateTimeFormatter.ofPattern("EEE, d MMM · h:mm a")
private fun formatActivityTime(time: java.time.Instant): String = activityTimeFormatter.format(time.atZone(ZoneId.systemDefault()))
