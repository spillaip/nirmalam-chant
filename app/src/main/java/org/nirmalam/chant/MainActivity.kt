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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Slider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
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

private enum class HomeSection(val label: String) { PRACTICE("Practice"), JOURNEY("Journey"), SETTINGS("Settings") }

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
            val targetReached by viewModel.targetReached.collectAsStateWithLifecycle()
            val currentTarget by viewModel.currentTarget.collectAsStateWithLifecycle()
            val hapticsEnabled by viewModel.hapticsEnabled.collectAsStateWithLifecycle()
            val voiceThreshold by viewModel.voiceThreshold.collectAsStateWithLifecycle()
            val defaultTarget by viewModel.defaultTarget.collectAsStateWithLifecycle()
            val canUndoManualTally by viewModel.canUndoManualTally.collectAsStateWithLifecycle()
            val voiceTracking by ChantTrackingService.isListening.collectAsStateWithLifecycle()
            NirmalamTheme {
                ChantHome(count, currentTarget, targetReached, dashboard, meditationToneEnabled, hapticsEnabled, voiceThreshold, defaultTarget, canUndoManualTally, voiceTracking, viewModel::addManualTally, viewModel::undoManualTally, viewModel::beginNextPractice, viewModel::saveIntention, viewModel::setMeditationToneEnabled, viewModel::setHapticsEnabled, viewModel::setVoiceThreshold, viewModel::setDefaultTarget, ::planPractice, ::requestTracking, ::stopTracking, viewModel::startPlannedPractice, viewModel::editPlan, viewModel::postponePlan, viewModel::skipPlan, viewModel::deletePlan)
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

    private fun stopTracking() {
        stopService(Intent(this, ChantTrackingService::class.java))
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
private fun ChantHome(count: Int, currentTarget: Int, targetReached: Boolean, dashboard: DashboardState, meditationToneEnabled: Boolean, hapticsEnabled: Boolean, voiceThreshold: Float, defaultTarget: Int, canUndoManualTally: Boolean, voiceTracking: Boolean, onAdd: () -> Unit, onUndo: () -> Unit, onBeginNext: () -> Unit, onSaveIntention: (String) -> Unit, onToneChange: (Boolean) -> Unit, onHapticsChange: (Boolean) -> Unit, onThresholdChange: (Float) -> Unit, onTargetChange: (Int) -> Unit, onPlan: () -> Unit, onStart: () -> Unit, onStop: () -> Unit, onStartPlan: (org.nirmalam.chant.data.PracticePlan) -> Unit, onEditPlan: (org.nirmalam.chant.data.PracticePlan, String, Int, Boolean) -> Unit, onPostponePlan: (org.nirmalam.chant.data.PracticePlan) -> Unit, onSkipPlan: (org.nirmalam.chant.data.PracticePlan) -> Unit, onDeletePlan: (org.nirmalam.chant.data.PracticePlan) -> Unit) {
    var intention by remember { mutableStateOf("") }
    var practiceMode by remember { mutableStateOf(false) }
    var section by remember { mutableStateOf(HomeSection.PRACTICE) }
    var showCompletion by remember { mutableStateOf(false) }
    LaunchedEffect(targetReached) { if (targetReached) showCompletion = true }
    if (practiceMode) {
        PracticeFocus(count, currentTarget, targetReached, onAdd, onBeginNext, onStart, onStop) { practiceMode = false }
        return
    }
    Scaffold(
        containerColor = Color.Transparent,
        bottomBar = {
            NavigationBar {
                HomeSection.entries.forEach { destination ->
                    NavigationBarItem(
                        selected = destination == section,
                        onClick = { section = destination },
                        icon = { Text(when (destination) { HomeSection.PRACTICE -> "ॐ"; HomeSection.JOURNEY -> "◷"; HomeSection.SETTINGS -> "⚙" }) },
                        label = { Text(destination.label) }
                    )
                }
            }
        }
    ) { contentPadding ->
        Box(Modifier.fillMaxSize()) {
            MysticBackground()
            LazyColumn(
                modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(contentPadding).padding(horizontal = 20.dp, vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
        if (section == HomeSection.PRACTICE) {
        item {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
            OmMark()
            Spacer(Modifier.height(12.dp))
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.fillMaxWidth().padding(vertical = 26.dp, horizontal = 20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("TODAY'S CHANTS", style = MaterialTheme.typography.labelLarge)
                    Text(count.toString(), fontSize = 112.sp, lineHeight = 118.sp, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.primary)
                    Text(if (targetReached) "$currentTarget complete" else "of $currentTarget", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.secondary)
                    MalaProgress(count, currentTarget, targetReached)
                }
            }
            Spacer(Modifier.height(8.dp))
            if (targetReached) {
                Button(onClick = onBeginNext, modifier = Modifier.fillMaxWidth().height(56.dp)) { Text("Begin next practice") }
            } else {
                Button(onClick = {
                    if (voiceTracking) onStop() else onStart()
                }, modifier = Modifier.fillMaxWidth().height(56.dp)) {
                    Text(if (voiceTracking) "Stop voice tracking" else "Start voice tracking")
                }
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = onAdd, modifier = Modifier.fillMaxWidth().height(52.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant, contentColor = MaterialTheme.colorScheme.onSurfaceVariant)
                ) { Text("Add one manually") }
                if (canUndoManualTally) {
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(onClick = onUndo, modifier = Modifier.fillMaxWidth()) { Text("Undo last manual count") }
                }
                Spacer(Modifier.height(8.dp))
                OutlinedButton(onClick = { practiceMode = true }, modifier = Modifier.fillMaxWidth().height(48.dp)) { Text("Enter focus practice") }
            }
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = intention, onValueChange = { intention = it }, modifier = Modifier.fillMaxWidth(),
                label = { Text("Today's intention") }, singleLine = true
            )
            Spacer(Modifier.height(6.dp))
            androidx.compose.foundation.layout.Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                OutlinedButton(onClick = { intention = "Peace" }) { Text("Peace") }
                OutlinedButton(onClick = { intention = "Gratitude" }) { Text("Gratitude") }
                OutlinedButton(onClick = { intention = "Focus" }) { Text("Focus") }
            }
            Spacer(Modifier.height(8.dp))
            Button(onClick = { onSaveIntention(intention) }, modifier = Modifier.fillMaxWidth()) { Text("Save intention") }
            }
        }
        item { RhythmCard(dashboard.streakDays, dashboard.sessionsThisWeek) }
        }
        if (section == HomeSection.JOURNEY) {
        item { DashboardHeading("Planned & scheduled") }
        if (dashboard.planned.isEmpty()) {
            item {
                Button(onClick = onPlan, modifier = Modifier.fillMaxWidth()) { Text("Plan an evening practice") }
            }
        } else {
            items(dashboard.planned, key = { it.id }) { plan ->
                PlanCard(plan, onStartPlan, onEditPlan, onPostponePlan, onSkipPlan, onDeletePlan)
            }
            item {
                Button(onClick = onPlan, modifier = Modifier.fillMaxWidth()) { Text("Plan another evening practice") }
            }
        }
        }
        if (section == HomeSection.JOURNEY) {
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
        if (section == HomeSection.SETTINGS) {
            item {
                MeditationToneCard(meditationToneEnabled, onToneChange)
                Spacer(Modifier.height(12.dp))
                SettingsCard(defaultTarget, hapticsEnabled, voiceThreshold, onTargetChange, onHapticsChange, onThresholdChange)
                Spacer(Modifier.height(12.dp))
                Text("No ads. No analytics. Audio never leaves this device.", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center, style = MaterialTheme.typography.bodySmall)
            }
        }
            }
        }
    }
    if (showCompletion) AlertDialog(
        onDismissRequest = { showCompletion = false },
        title = { Text("ॐ  Practice complete") },
        text = { Text("$currentTarget chants have been recorded privately on this device. Take a quiet breath before beginning again.") },
        confirmButton = { Button(onClick = { showCompletion = false }) { Text("Rest in stillness") } }
    )
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
private fun SettingsCard(target: Int, hapticsEnabled: Boolean, threshold: Float, onTargetChange: (Int) -> Unit, onHapticsChange: (Boolean) -> Unit, onThresholdChange: (Float) -> Unit) = Card(Modifier.fillMaxWidth()) {
    var targetText by remember(target) { mutableStateOf(target.toString()) }
    Column(Modifier.padding(16.dp)) {
        Text("Practice settings", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = targetText, onValueChange = { value ->
                targetText = value.filter(Char::isDigit).take(5)
                targetText.toIntOrNull()?.let(onTargetChange)
            }, modifier = Modifier.fillMaxWidth(), singleLine = true,
            label = { Text("Chants in next practice") }
        )
        Spacer(Modifier.height(8.dp))
        Text("Mala milestones", style = MaterialTheme.typography.bodyMedium)
        androidx.compose.foundation.layout.Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf(27, 54, 108).forEach { milestone ->
                OutlinedButton(onClick = { onTargetChange(milestone) }) { Text(milestone.toString()) }
            }
        }
        Spacer(Modifier.height(8.dp))
        androidx.compose.foundation.layout.Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column { Text("Haptic feedback"); Text("Pulse after each count", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary) }
            Switch(checked = hapticsEnabled, onCheckedChange = onHapticsChange)
        }
        Spacer(Modifier.height(8.dp))
        Text("Whisper / noise sensitivity", style = MaterialTheme.typography.bodyMedium)
        Slider(value = threshold, onValueChange = onThresholdChange, valueRange = 100f..2000f)
        Text("Lower values hear softer whispers; raise it in a noisy room.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
    }
}

@androidx.compose.runtime.Composable
private fun PlanCard(plan: org.nirmalam.chant.data.PracticePlan, onStart: (org.nirmalam.chant.data.PracticePlan) -> Unit, onEdit: (org.nirmalam.chant.data.PracticePlan, String, Int, Boolean) -> Unit, onPostpone: (org.nirmalam.chant.data.PracticePlan) -> Unit, onSkip: (org.nirmalam.chant.data.PracticePlan) -> Unit, onDelete: (org.nirmalam.chant.data.PracticePlan) -> Unit) {
    var editing by remember { mutableStateOf(false) }
    var title by remember(plan.id) { mutableStateOf(plan.title) }
    var target by remember(plan.id) { mutableStateOf(plan.targetCount.toString()) }
    var reminderEnabled by remember(plan.id) { mutableStateOf(plan.reminderEnabled) }
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text(plan.title, style = MaterialTheme.typography.titleMedium)
            Text("${formatActivityTime(plan.scheduledFor)} · ${plan.targetCount} chants", color = MaterialTheme.colorScheme.secondary)
            androidx.compose.foundation.layout.Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { onStart(plan) }) { Text("Start") }
                OutlinedButton(onClick = { editing = true }) { Text("Edit") }
                OutlinedButton(onClick = { onPostpone(plan) }) { Text("Tomorrow") }
            }
            androidx.compose.foundation.layout.Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { onSkip(plan) }) { Text("Skip") }
                OutlinedButton(onClick = { onDelete(plan) }) { Text("Delete") }
            }
        }
    }
    if (editing) AlertDialog(
        onDismissRequest = { editing = false },
        title = { Text("Edit practice") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(title, { title = it }, label = { Text("Practice name") }, singleLine = true)
                OutlinedTextField(target, { target = it.filter(Char::isDigit).take(5) }, label = { Text("Chant target") }, singleLine = true)
                androidx.compose.foundation.layout.Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Local reminder")
                    Switch(reminderEnabled, { reminderEnabled = it })
                }
            }
        },
        confirmButton = { Button(onClick = { onEdit(plan, title, target.toIntOrNull() ?: plan.targetCount, reminderEnabled); editing = false }) { Text("Save") } },
        dismissButton = { OutlinedButton(onClick = { editing = false }) { Text("Cancel") } }
    )
}

@androidx.compose.runtime.Composable
private fun PracticeFocus(count: Int, target: Int, targetReached: Boolean, onAdd: () -> Unit, onBeginNext: () -> Unit, onStart: () -> Unit, onStop: () -> Unit, onExit: () -> Unit) {
    Box(Modifier.fillMaxSize()) {
        MysticBackground()
        Column(
            modifier = Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            OmMark()
            Spacer(Modifier.height(20.dp))
            Text(if (targetReached) "Practice complete" else "Focus practice", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.secondary)
            Text(count.toString(), fontSize = 128.sp, lineHeight = 132.sp, color = MaterialTheme.colorScheme.primary)
            Text(if (targetReached) "$target of $target" else "of $target", style = MaterialTheme.typography.titleMedium)
            MalaProgress(count, target, targetReached)
            Spacer(Modifier.height(30.dp))
            if (targetReached) {
                Button(onClick = onBeginNext, modifier = Modifier.fillMaxWidth().height(60.dp)) { Text("Begin next practice") }
            } else {
                Button(onClick = onStart, modifier = Modifier.fillMaxWidth().height(60.dp)) { Text("Start listening") }
                Spacer(Modifier.height(10.dp))
                Button(onClick = onAdd, modifier = Modifier.fillMaxWidth().height(56.dp), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant, contentColor = MaterialTheme.colorScheme.onSurfaceVariant)) { Text("Add one") }
                Spacer(Modifier.height(10.dp))
                OutlinedButton(onClick = onStop, modifier = Modifier.fillMaxWidth()) { Text("Pause listening") }
            }
            Spacer(Modifier.height(18.dp))
            OutlinedButton(onClick = onExit) { Text("Return to dashboard") }
        }
    }
}

@androidx.compose.runtime.Composable
private fun MalaProgress(count: Int, target: Int, complete: Boolean) = Canvas(Modifier.fillMaxWidth().height(42.dp).padding(top = 12.dp)) {
    val beadCount = 27
    val spacing = size.width / beadCount
    val radius = (spacing * 0.25f).coerceAtMost(size.height / 3)
    repeat(beadCount) { index ->
        val progressAtBead = ((index + 1) * target + beadCount - 1) / beadCount
        drawCircle(
            color = if (count >= progressAtBead || complete) Color(0xFFF0BE71) else Color(0xFF4B625D),
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
        drawRect(Brush.verticalGradient(listOf(Color(0xFF061817), Color(0xFF0C2926), Color(0xFF171128))))
        drawCircle(Color(0x224E8AFF), radius = size.minDimension * (0.22f + phase * 0.05f), center = androidx.compose.ui.geometry.Offset(size.width * (0.16f + phase * 0.08f), size.height * 0.16f))
        drawCircle(Color(0x2249B98F), radius = size.minDimension * (0.18f + (1f - phase) * 0.06f), center = androidx.compose.ui.geometry.Offset(size.width * (0.82f - phase * 0.08f), size.height * 0.72f))
    }
}

private val activityTimeFormatter = DateTimeFormatter.ofPattern("EEE, d MMM · h:mm a")
private fun formatActivityTime(time: java.time.Instant): String = activityTimeFormatter.format(time.atZone(ZoneId.systemDefault()))
