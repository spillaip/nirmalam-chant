package com.nirmalamgroup.nirmalamchant.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.delay
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nirmalamgroup.nirmalamchant.DashboardState
import com.nirmalamgroup.nirmalamchant.data.PracticePlan
import com.nirmalamgroup.nirmalamchant.data.ChantProfile
import java.time.ZoneId
import java.time.format.DateTimeFormatter

internal enum class HomeSection(val label: String) { PRACTICE("Practice"), JOURNEY("Journey"), SETTINGS("Settings") }

@Composable
internal fun ChantHome(
    count: Int, currentTarget: Int, targetReached: Boolean, savedIntention: String,
    profiles: List<ChantProfile>, selectedProfileId: String?,
    onSelectProfile: (String) -> Unit, onSaveProfile: (String?, String, Int, Int) -> Unit,
    dashboard: DashboardState, meditationToneEnabled: Boolean, hapticsEnabled: Boolean,
    defaultTarget: Int, canUndoManualTally: Boolean, onAdd: () -> Unit,
    onUndo: () -> Unit, onReset: () -> Unit, onBeginNext: () -> Unit,
    onSaveIntention: (String) -> Unit, onToneChange: (Boolean) -> Unit,
    onHapticsChange: (Boolean) -> Unit, onTargetChange: (Int) -> Unit,
    onPlan: () -> Unit, onStartPlan: (PracticePlan) -> Unit,
    onEditPlan: (PracticePlan, String, Int, Boolean) -> Unit,
    onPostponePlan: (PracticePlan) -> Unit, onSkipPlan: (PracticePlan) -> Unit,
    onDeletePlan: (PracticePlan) -> Unit
) {
    var intention by rememberSaveable { mutableStateOf("") }
    LaunchedEffect(savedIntention) { intention = savedIntention }
    var focusMode by rememberSaveable { mutableStateOf(false) }
    var section by rememberSaveable { mutableStateOf(HomeSection.PRACTICE) }
    var showCompletion by rememberSaveable { mutableStateOf(false) }
    var showReset by rememberSaveable { mutableStateOf(false) }
    var milestoneMessage by remember { mutableStateOf<String?>(null) }
    // Timed mode is deliberately opt-in and never auto-resumes after app restart.
    var autoIntervalText by rememberSaveable { mutableStateOf("3") }
    var autoRunning by remember { mutableStateOf(false) }
    val selectedProfile = profiles.firstOrNull { it.id == selectedProfileId }
    val intervalSeconds = selectedProfile?.intervalSeconds ?: autoIntervalText.toIntOrNull()?.takeIf { it in 1..3600 }
    LaunchedEffect(selectedProfileId) { autoRunning = false }
    LaunchedEffect(targetReached) { if (targetReached) autoRunning = false }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) autoRunning = false
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    // Trigger only from an explicit tap, not from count restoration after app restart.
    val onCountTap: () -> Unit = {
        val next = count + 1
        if (!targetReached && next <= currentTarget) {
            milestoneMessage = when (next) {
                27 -> "27 chants · a quiet moment of progress"
                54 -> "54 chants · halfway through a mala"
                108 -> "108 chants · mala complete"
                else -> null
            }
        }
        onAdd()
    }
    val latestCountTap by rememberUpdatedState(onCountTap)
    // Focus Mode changes presentation only; it must not cancel or restart the count timer.
    // Loop cancels on Pause, leaving Practice, or completing the target.
    LaunchedEffect(autoRunning, intervalSeconds, section, targetReached) {
        if (autoRunning && intervalSeconds != null && section == HomeSection.PRACTICE && !targetReached) {
            while (true) {
                delay(intervalSeconds * 1000L)
                // Once per elapsed interval; same safe Room tally path as a manual tap.
                latestCountTap()
            }
        }
    }
    LaunchedEffect(milestoneMessage) {
        if (milestoneMessage != null) {
            kotlinx.coroutines.delay(2500)
            milestoneMessage = null
        }
    }
    if (showReset) AlertDialog(onDismissRequest = { showReset = false },
        title = { Text("Reset your count?") },
        text = { Text("This resets the current practice. Your completed history is preserved.") },
        confirmButton = { Button(onClick = { autoRunning = false; onReset(); showReset = false }) { Text("Reset") } },
        dismissButton = { OutlinedButton(onClick = { showReset = false }) { Text("Cancel") } })
    if (focusMode) {
        Box(Modifier.fillMaxSize()) {
            PracticeFocus(count, currentTarget, targetReached, onCountTap, onBeginNext) { focusMode = false }
            milestoneMessage?.let { MilestoneNotice(it, Modifier.align(Alignment.TopCenter).statusBarsPadding()) }
        }
        return
    }
    Scaffold(containerColor = Color.Transparent, bottomBar = {
        NavigationBar(containerColor = Color(0xFF102F28)) {
            HomeSection.entries.forEach { destination ->
                NavigationBarItem(selected = section == destination, onClick = { if (section != destination) autoRunning = false; section = destination },
                    icon = { Text(when (destination) { HomeSection.PRACTICE -> "ॐ"; HomeSection.JOURNEY -> "◷"; HomeSection.SETTINGS -> "⚙" }) },
                    label = { Text(destination.label) })
            }
        }
    }) { padding ->
        Box(Modifier.fillMaxSize()) {
            MysticBackground()
            LazyColumn(Modifier.fillMaxSize().statusBarsPadding().padding(padding).padding(horizontal = 20.dp, vertical = 18.dp),
                horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                item {
                    when(section) {
                        HomeSection.PRACTICE -> PracticeScreen(count, currentTarget, targetReached, dashboard,
                            canUndoManualTally, intention, { intention = it }, onCountTap, onUndo, { autoRunning = false; onBeginNext() },
                            { focusMode = true }, { autoRunning = false; showReset = true }, onSaveIntention,
                            autoIntervalText, { autoIntervalText = it; autoRunning = false }, intervalSeconds,
                            autoRunning, { autoRunning = !autoRunning },
                            profiles, selectedProfileId,
                            { id -> autoRunning = false; onSelectProfile(id) }, onSaveProfile)
                        HomeSection.JOURNEY -> JourneyScreen(dashboard, onPlan,
                            { plan -> autoRunning = false; onStartPlan(plan); section = HomeSection.PRACTICE },
                            onEditPlan, onPostponePlan, onSkipPlan, onDeletePlan)
                        HomeSection.SETTINGS -> SettingsScreen(meditationToneEnabled, hapticsEnabled,
                            defaultTarget, onToneChange, onTargetChange, onHapticsChange)
                    }
                }
            }
            milestoneMessage?.let { MilestoneNotice(it, Modifier.align(Alignment.TopCenter).statusBarsPadding()) }
        }
    }

}

@androidx.compose.runtime.Composable
internal fun PrivacyCard() {
    var showPolicy by remember { mutableStateOf(false) }
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text("Privacy by design", style = MaterialTheme.typography.titleMedium)
            Text("No ads, analytics, accounts, or cloud sync. Manual chants are counted locally; no microphone access is requested.", color = MaterialTheme.colorScheme.secondary)
            OutlinedButton(onClick = { showPolicy = true }) { Text("Read privacy policy") }
        }
    }
    if (showPolicy) AlertDialog(
        onDismissRequest = { showPolicy = false },
        title = { Text("Nirmalam Chant privacy") },
        text = { Text("Chant counts, intentions, settings, and plans stay in local app storage. The app uses manual counting only and does not access the microphone. You can remove data by clearing the app's storage or uninstalling the app.") },
        confirmButton = { Button(onClick = { showPolicy = false }) { Text("Close") } }
    )
}

@androidx.compose.runtime.Composable
internal fun DashboardHeading(title: String) = Text(
    title, modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
    style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold
)

@androidx.compose.runtime.Composable
internal fun ActivityCard(title: String, details: String) = Card(Modifier.fillMaxWidth()) {
    Column(Modifier.padding(18.dp)) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(4.dp))
        Text(details, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.secondary)
    }
}

@androidx.compose.runtime.Composable
internal fun EmptyActivityCard(message: String) = Card(Modifier.fillMaxWidth()) {
    Text(message, modifier = Modifier.padding(18.dp), color = MaterialTheme.colorScheme.secondary)
}

@androidx.compose.runtime.Composable
internal fun RhythmCard(streakDays: Int, sessionsThisWeek: Int) = Card(Modifier.fillMaxWidth()) {
    Column(Modifier.padding(18.dp)) {
        Text("Your rhythm", style = MaterialTheme.typography.titleMedium)
        Text("$streakDays-day local streak · $sessionsThisWeek sessions in the last 7 days", color = MaterialTheme.colorScheme.secondary)
    }
}

@androidx.compose.runtime.Composable
internal fun MeditationToneCard(enabled: Boolean, onChange: (Boolean) -> Unit) = Card(Modifier.fillMaxWidth()) {
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
internal fun SettingsCard(target: Int, hapticsEnabled: Boolean, onTargetChange: (Int) -> Unit, onHapticsChange: (Boolean) -> Unit) = Card(Modifier.fillMaxWidth()) {
    var targetText by remember(target) { mutableStateOf(target.toString()) }
    Column(Modifier.padding(16.dp)) {
        Text("Practice settings", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = targetText, onValueChange = { value ->
                targetText = value.filter(Char::isDigit).take(5)
                // Apply only when Save is pressed; avoid changing settings mid-edit.
            }, modifier = Modifier.fillMaxWidth(), singleLine = true,
            label = { Text("Chants in next practice") }
        )
        val validTarget = targetText.toIntOrNull()?.takeIf { it in 1..10_000 }
        Button(onClick = { validTarget?.let(onTargetChange) }, enabled = validTarget != null && validTarget != target) {
            Text("Save chant target")
        }
        if (validTarget == null) Text("Enter a target between 1 and 10,000", color = MaterialTheme.colorScheme.error)
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
    }
}

@androidx.compose.runtime.Composable
internal fun PlanCard(plan: com.nirmalamgroup.nirmalamchant.data.PracticePlan, onStart: (com.nirmalamgroup.nirmalamchant.data.PracticePlan) -> Unit, onEdit: (com.nirmalamgroup.nirmalamchant.data.PracticePlan, String, Int, Boolean) -> Unit, onPostpone: (com.nirmalamgroup.nirmalamchant.data.PracticePlan) -> Unit, onSkip: (com.nirmalamgroup.nirmalamchant.data.PracticePlan) -> Unit, onDelete: (com.nirmalamgroup.nirmalamchant.data.PracticePlan) -> Unit) {
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
internal fun PracticeFocus(count: Int, target: Int, targetReached: Boolean, onAdd: () -> Unit, onBeginNext: () -> Unit, onExit: () -> Unit) {
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
            ManualCounter(count, target, targetReached, onAdd)
            Text(if (targetReached) "$target of $target" else "of $target", style = MaterialTheme.typography.titleMedium)
            MalaProgress(count, target, targetReached)
            Spacer(Modifier.height(30.dp))
            if (targetReached) {
                Button(onClick = onBeginNext, modifier = Modifier.fillMaxWidth().height(60.dp)) { Text("Begin next practice") }
            } else {
                
                Spacer(Modifier.height(10.dp))
                Button(onClick = onAdd, modifier = Modifier.fillMaxWidth().height(56.dp), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant, contentColor = MaterialTheme.colorScheme.onSurfaceVariant)) { Text("+ 1  ·  Count chant") }
            }
            Spacer(Modifier.height(18.dp))
            OutlinedButton(onClick = onExit) { Text("Return to dashboard") }
        }
    }
}

@androidx.compose.runtime.Composable
internal fun MalaProgress(count: Int, target: Int, complete: Boolean) = Canvas(Modifier.fillMaxWidth().height(42.dp).padding(top = 12.dp)) {
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
internal fun OmMark() {
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
internal fun MysticBackground() {
    // Static background avoids continuous full-screen canvas invalidations.
    val phase = 0.5f
    Canvas(Modifier.fillMaxSize()) {
        drawRect(Brush.verticalGradient(listOf(Color(0xFF061817), Color(0xFF0C2926), Color(0xFF171128))))
        drawCircle(Color(0x224E8AFF), radius = size.minDimension * (0.22f + phase * 0.05f), center = androidx.compose.ui.geometry.Offset(size.width * (0.16f + phase * 0.08f), size.height * 0.16f))
        drawCircle(Color(0x2249B98F), radius = size.minDimension * (0.18f + (1f - phase) * 0.06f), center = androidx.compose.ui.geometry.Offset(size.width * (0.82f - phase * 0.08f), size.height * 0.72f))
    }
}

internal val activityTimeFormatter = DateTimeFormatter.ofPattern("EEE, d MMM · h:mm a")
internal fun formatActivityTime(time: java.time.Instant): String = activityTimeFormatter.format(time.atZone(ZoneId.systemDefault()))

@Composable
private fun MilestoneNotice(message: String, modifier: Modifier = Modifier) {
    Surface(modifier = modifier.padding(top = 8.dp, start = 22.dp, end = 22.dp),
        shape = RoundedCornerShape(22.dp), color = Color(0xFF244B3D), tonalElevation = 2.dp) {
        Text(message, modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp),
            color = Color(0xFFF3C779), style = MaterialTheme.typography.labelLarge)
    }
}
