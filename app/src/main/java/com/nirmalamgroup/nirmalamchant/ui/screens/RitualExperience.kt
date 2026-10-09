package com.nirmalamgroup.nirmalamchant.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.nirmalamgroup.nirmalamchant.data.ChantProfile
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate

/** Optional local-only practice companions. Never owns or advances the chant counter. */
@Composable
internal fun RitualExperience(
    profiles: List<ChantProfile>, selectedProfileId: String?, count: Int, targetReached: Boolean,
    autoRunning: Boolean, intention: String, onIntentionChange: (String) -> Unit,
    onSaveIntention: (String) -> Unit, onSelectProfile: (String) -> Unit
) {
    val context = LocalContext.current
    val prefs = remember(context) { context.getSharedPreferences("nirmalam_ritual_v57", 0) }
    var breathing by remember { mutableStateOf(false) }
    var seconds by remember { mutableIntStateOf(prefs.getInt("breath_seconds", 4).coerceIn(3, 8)) }
    var showOpening by remember { mutableStateOf(false) }
    var openedSession by remember { mutableStateOf(false) }
    var sankalpaDraft by remember(intention) { mutableStateOf(intention) }
    var beforeMood by remember { mutableIntStateOf(0) }
    var afterMood by remember { mutableIntStateOf(0) }
    var ritualName by remember { mutableStateOf("") }
    var selectedSteps by remember { mutableStateOf<List<String>>(emptyList()) }
    var rituals by remember { mutableStateOf(readRituals(prefs.getString("rituals", "[]"))) }
    var activeRitual by remember { mutableStateOf<String?>(null) }
    var activeIndex by remember { mutableIntStateOf(0) }
    var ritualMessage by remember { mutableStateOf<String?>(null) }

    // Offer a guided intention only when explicitly requested; never block a session.
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Guided Sankalpa · optional", style = MaterialTheme.typography.titleMedium)
            Text(if (intention.isBlank()) "Set an intention before practice, or begin without one." else "Today's intention: $intention",
                style = MaterialTheme.typography.bodySmall)
            OutlinedButton(onClick = { sankalpaDraft = intention; showOpening = true }) { Text("Opening ritual") }
        }
    }
    if (showOpening) AlertDialog(onDismissRequest = { showOpening = false }, title = { Text("Begin with intention") },
        text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Notice your breath naturally, then choose what this practice means to you. You may skip.")
            OutlinedTextField(sankalpaDraft, { sankalpaDraft = it.take(180) }, label = { Text("My Sankalpa (optional)") })
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                listOf("Peace", "Gratitude", "Compassion").forEach { suggestion ->
                    TextButton(onClick = { sankalpaDraft = suggestion }) { Text(suggestion, style = MaterialTheme.typography.labelSmall) }
                }
            }
        } },
        confirmButton = { Button(onClick = { onIntentionChange(sankalpaDraft); onSaveIntention(sankalpaDraft); showOpening = false }) { Text("Begin practice") } },
        dismissButton = { TextButton(onClick = { showOpening = false }) { Text("Skip") } })

    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Breathing rhythm companion", style = MaterialTheme.typography.titleMedium)
            Text("Optional gentle inhale/exhale guidance. Breathe comfortably; no holding or forced pace.", style = MaterialTheme.typography.bodySmall)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Switch(checked = breathing, onCheckedChange = { breathing = it })
                Text(if (breathing) "Guidance on" else "Guidance off")
                Spacer(Modifier.weight(1f))
                TextButton(onClick = { seconds = (seconds - 1).coerceAtLeast(3); prefs.edit().putInt("breath_seconds", seconds).apply() }, enabled = seconds > 3) { Text("−") }
                Text("${seconds}s")
                TextButton(onClick = { seconds = (seconds + 1).coerceAtMost(8); prefs.edit().putInt("breath_seconds", seconds).apply() }, enabled = seconds < 8) { Text("+") }
            }
            if (breathing) BreathVisual(seconds)
        }
    }

    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Mindfulness check-in · optional", style = MaterialTheme.typography.titleMedium)
            Text("How settled do you feel? 1 = unsettled, 5 = very settled. Not a clinical measure.", style = MaterialTheme.typography.bodySmall)
            MoodSelector("Before practice", beforeMood) { beforeMood = it; saveMood(prefs, "before", it, selectedProfileId) }
            MoodSelector("After practice", afterMood) { afterMood = it; saveMood(prefs, "after", it, selectedProfileId) }
            Text("Check-ins stay on this device. You may leave either unanswered.", style = MaterialTheme.typography.labelSmall)
        }
    }

    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Personal chant rituals", style = MaterialTheme.typography.titleMedium)
            Text("Create an ordered sequence from your saved chants. Switching steps is manual and pauses auto-counting.", style = MaterialTheme.typography.bodySmall)
            OutlinedTextField(ritualName, { ritualName = it.take(60) }, modifier = Modifier.fillMaxWidth(), label = { Text("Ritual name") }, singleLine = true)
            profiles.forEach { profile ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = profile.id in selectedSteps, onCheckedChange = { checked ->
                        selectedSteps = if (checked) selectedSteps + profile.id else selectedSteps - profile.id
                    })
                    Text(profile.name + " · ${profile.targetCount} chants · ${profile.intervalSeconds}s")
                }
            }
            Button(enabled = ritualName.isNotBlank() && selectedSteps.isNotEmpty() && rituals.size < 10,
                onClick = {
                    rituals = rituals + SavedRitual(ritualName.trim(), selectedSteps)
                    prefs.edit().putString("rituals", writeRituals(rituals)).apply()
                    ritualName = ""; selectedSteps = emptyList()
                }) { Text("Save ritual (${rituals.size}/10)") }
            rituals.forEachIndexed { index, ritual ->
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(ritual.name, style = MaterialTheme.typography.titleSmall)
                        Text(ritual.ids.mapNotNull { id -> profiles.firstOrNull { it.id == id }?.name }.joinToString(" → "), style = MaterialTheme.typography.bodySmall)
                    }
                    TextButton(onClick = {
                        activeRitual = ritual.name; activeIndex = 0
                        ritual.ids.firstOrNull { id -> profiles.any { it.id == id } }?.let(onSelectProfile)
                        ritualMessage = "Step 1 of ${ritual.ids.size}. Start the timer when ready."
                    }, enabled = !autoRunning) { Text("Start") }
                    TextButton(onClick = {
                        rituals = rituals.filterIndexed { i, _ -> i != index }
                        prefs.edit().putString("rituals", writeRituals(rituals)).apply()
                        if (activeRitual == ritual.name) activeRitual = null
                    }) { Text("Remove") }
                }
            }
            val current = rituals.firstOrNull { it.name == activeRitual }
            if (current != null) {
                Text("Ritual: ${current.name} · step ${activeIndex + 1}/${current.ids.size}")
                ritualMessage?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = {
                        if (activeIndex + 1 >= current.ids.size) {
                            activeRitual = null; ritualMessage = "Ritual finished"
                        } else {
                            activeIndex += 1
                            current.ids.getOrNull(activeIndex)?.let(onSelectProfile)
                            ritualMessage = "Step ${activeIndex + 1} of ${current.ids.size}."
                        }
                    }, enabled = targetReached && !autoRunning) { Text(if (activeIndex + 1 == current.ids.size) "Finish ritual" else "Next chant") }
                    OutlinedButton(onClick = { activeRitual = null }, enabled = !autoRunning) { Text("End ritual") }
                }
                if (!targetReached) Text("Complete this chant's target before moving to the next step.", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun MoodSelector(title: String, selected: Int, onSelect: (Int) -> Unit) {
    Text(title, style = MaterialTheme.typography.labelLarge)
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
        (1..5).forEach { number ->
            FilterChip(selected = selected == number, onClick = { onSelect(number) }, label = { Text(number.toString()) })
        }
    }
}

@Composable
private fun BreathVisual(seconds: Int) {
    val transition = rememberInfiniteTransition(label = "breath")
    val scale by transition.animateFloat(initialValue = 0.55f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(seconds * 1000, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "breathing circle")
    Box(Modifier.fillMaxWidth().height(112.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(104.dp)) {
            drawCircle(Color(0xFFF3C779).copy(alpha = .8f), radius = size.minDimension * .42f * scale, style = Stroke(width = 5.dp.toPx()))
        }
        Text("In · Out", color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.labelSmall)
    }
}

private data class SavedRitual(val name: String, val ids: List<String>)
private fun readRituals(raw: String?): List<SavedRitual> = try {
    val array = JSONArray(raw ?: "[]")
    (0 until array.length()).map { i ->
        val obj = array.getJSONObject(i)
        val items = obj.getJSONArray("steps")
        SavedRitual(obj.getString("name"), (0 until items.length()).map { items.getString(it) })
    }
} catch (_: Exception) { emptyList() }
private fun writeRituals(rituals: List<SavedRitual>): String = JSONArray().apply {
    rituals.forEach { r -> put(JSONObject().put("name", r.name).put("steps", JSONArray(r.ids))) }
}.toString()
private fun saveMood(prefs: android.content.SharedPreferences, phase: String, rating: Int, profile: String?) {
    val key = "mood_history"
    val history = try { JSONArray(prefs.getString(key, "[]")) } catch (_: Exception) { JSONArray() }
    history.put(JSONObject().put("date", LocalDate.now().toString()).put("phase", phase).put("rating", rating).put("profile", profile ?: ""))
    // Bound local storage growth without sending anything off device.
    val trimmed = JSONArray()
    for (i in (history.length() - 499).coerceAtLeast(0) until history.length()) trimmed.put(history.get(i))
    prefs.edit().putString(key, trimmed.toString()).apply()
}
