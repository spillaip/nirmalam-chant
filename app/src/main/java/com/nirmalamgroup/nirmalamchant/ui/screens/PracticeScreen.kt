package com.nirmalamgroup.nirmalamchant.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.shape.RoundedCornerShape
import com.nirmalamgroup.nirmalamchant.DashboardState
import com.nirmalamgroup.nirmalamchant.data.ChantProfile

@Composable
internal fun PracticeScreen(
    count: Int, currentTarget: Int, targetReached: Boolean, dashboard: DashboardState,
    canUndoManualTally: Boolean, intention: String, onIntentionChange: (String) -> Unit,
    onAdd: () -> Unit, onUndo: () -> Unit, onBeginNext: () -> Unit,
    onFocus: () -> Unit, onResetRequest: () -> Unit, onSaveIntention: (String) -> Unit,
    autoIntervalText: String, onAutoIntervalChange: (String) -> Unit,
    autoIntervalSeconds: Int?, autoRunning: Boolean, onToggleAuto: () -> Unit,
    profiles: List<ChantProfile>, selectedProfileId: String?,
    onSelectProfile: (String) -> Unit, onSaveProfile: (String?, String, Int, Int) -> Unit
) {
    var useMala by rememberSaveable { mutableStateOf(false) }
    var editSankalpa by rememberSaveable { mutableStateOf(false) }
    var editingProfile by remember { mutableStateOf(false) }
    var editId by remember { mutableStateOf<String?>(null) }
    var nameText by remember { mutableStateOf("") }
    var targetText by remember { mutableStateOf("108") }
    var secondsText by remember { mutableStateOf("3") }
    val currentProfile = profiles.firstOrNull { it.id == selectedProfileId }
    var chooseChant by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text("NIRMALAM CHANT", style = MaterialTheme.typography.labelLarge, color = Color(0xFFF3C779))
        Text("Your moment of stillness", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Light)
        Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF173B31))) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("YOUR CHANT", style = MaterialTheme.typography.labelSmall, color = Color(0xFFF3C779))
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(currentProfile?.name ?: "My practice", style = MaterialTheme.typography.titleLarge, color = Color.White)
                        Text("${currentProfile?.intervalSeconds ?: autoIntervalSeconds ?: 3}s per chant  ·  Goal $currentTarget", style = MaterialTheme.typography.bodySmall, color = Color(0xFFBED3C8))
                    }
                    OutlinedButton(onClick = { chooseChant = true }) { Text("Change") }
                }
            }
        }
        if (chooseChant) AlertDialog(onDismissRequest = { chooseChant = false },
            title = { Text("Choose your chant") },
            text = { Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                profiles.forEach { profile ->
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = selectedProfileId == profile.id, onClick = { onSelectProfile(profile.id); chooseChant = false })
                        TextButton(onClick = { onSelectProfile(profile.id); chooseChant = false }, modifier = Modifier.weight(1f)) {
                            Text(profile.name, maxLines = 2)
                        }
                        TextButton(onClick = {
                            chooseChant = false; editId = profile.id; nameText = profile.name
                            targetText = profile.targetCount.toString(); secondsText = profile.intervalSeconds.toString(); editingProfile = true
                        }) { Text("Edit") }
                    }
                }
                if (profiles.size < 5) TextButton(onClick = {
                    chooseChant = false; editId = null; nameText = ""; targetText = "108"; secondsText = "3"; editingProfile = true
                }) { Text("+ Add chant (${profiles.size}/5)") }
            } },
            confirmButton = { TextButton(onClick = { chooseChant = false }) { Text("Done") } })
        // View switching doesn't alter the profile or current session.
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            SegmentedButton(selected = !useMala, onClick = { useMala = false }, shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)) { Text("Circle") }
            SegmentedButton(selected = useMala, onClick = { useMala = true }, shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)) { Text("Mala") }
        }
        if (useMala) DigitalMala(count, currentTarget, targetReached, onAdd)
        else ManualCounter(count, currentTarget, targetReached, onAdd)
        Text(if (targetReached) "Your mala is complete" else "Tap the circle or use automatic counting", style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.secondary)
        if (editingProfile) AlertDialog(
            onDismissRequest = { editingProfile = false },
            title = { Text(if (editId == null) "Add chant" else "Edit chant") },
            text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(nameText, { nameText = it.take(80) }, label = { Text("Chant name") })
                OutlinedTextField(targetText, { targetText = it.filter(Char::isDigit) }, label = { Text("Target") })
                OutlinedTextField(secondsText, { secondsText = it.filter(Char::isDigit) }, label = { Text("Seconds per chant") })
            } },
            confirmButton = { Button(enabled = nameText.isNotBlank() && (targetText.toIntOrNull() ?: 0) in 1..10000 && (secondsText.toIntOrNull() ?: 0) in 1..3600, onClick = {
                onSaveProfile(editId, nameText, targetText.toInt(), secondsText.toInt()); editingProfile = false
            }) { Text("Save") } },
            dismissButton = { TextButton(onClick = { editingProfile = false }) { Text("Cancel") } }
        )
        if (targetReached) {
            Button(onClick = onBeginNext, Modifier.fillMaxWidth().height(56.dp)) { Text("Begin next mala") }
        } else {
            Button(onClick = onAdd, Modifier.fillMaxWidth().height(56.dp)) { Text("+ 1  ·  Count chant") }
        }
        Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF173B31))) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("AUTOMATIC COUNTING", style = MaterialTheme.typography.labelSmall, color = Color(0xFFF3C779))
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Column(Modifier.weight(1f)) {
                        Text("Every ${autoIntervalSeconds ?: "—"} seconds", style = MaterialTheme.typography.titleMedium, color = Color.White)
                        Text(if (autoRunning) "Counting · tap Pause to stop" else "Paused · start when ready", style = MaterialTheme.typography.bodySmall, color = Color(0xFFBED3C8))
                    }
                    Button(onClick = onToggleAuto, enabled = !targetReached && (autoRunning || autoIntervalSeconds != null)) {
                        Text(if (autoRunning) "Pause" else "Start")
                    }
                }
                if (selectedProfileId == null) {
                    OutlinedTextField(value = autoIntervalText, onValueChange = { onAutoIntervalChange(it.filter(Char::isDigit).take(4)) },
                        label = { Text("Seconds per chant (1–3600)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(), singleLine = true)
                } else {
                    Text("Change the interval using Change → Edit chant.", style = MaterialTheme.typography.labelSmall, color = Color(0xFFBED3C8))
                }
            }
        }
        // Presets are opt-in and use the same persisted per-profile interval as custom editing.
        if (currentProfile != null) {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Chant pace", style = MaterialTheme.typography.titleMedium)
                    Text("Choose a comfortable interval for ${currentProfile.name}. Start the timer separately.",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("Gentle" to 6, "Steady" to 3, "Deep focus" to 2).forEach { (label, seconds) ->
                            val selected = currentProfile.intervalSeconds == seconds
                            OutlinedButton(
                                onClick = { onSaveProfile(currentProfile.id, currentProfile.name, currentProfile.targetCount, seconds) },
                                enabled = !autoRunning && !selected,
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(horizontal = 2.dp)
                            ) { Text("$label\n${seconds}s", textAlign = TextAlign.Center, style = MaterialTheme.typography.labelSmall) }
                        }
                    }
                    Text("Custom pace: Change → Edit chant. Pause automatic counting before changing pace.",
                        style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                }
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(onClick = onUndo, enabled = canUndoManualTally, modifier = Modifier.weight(1f)) { Text("Undo") }
            OutlinedButton(onClick = onFocus, modifier = Modifier.weight(1f)) { Text("Focus mode") }
        }
        if (count > 0 && !targetReached) TextButton(onClick = onResetRequest) { Text("Reset this practice") }
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Daily Sankalpa · optional", style = MaterialTheme.typography.titleMedium)
                if (intention.isNotBlank() && !editSankalpa) {
                    Text(intention, style = MaterialTheme.typography.bodyLarge)
                    TextButton(onClick = { editSankalpa = true }) { Text("Edit intention") }
                } else if (!editSankalpa) {
                    Text("Begin with a personal intention, or simply chant.", color = MaterialTheme.colorScheme.secondary)
                    OutlinedButton(onClick = { editSankalpa = true }) { Text("Set today's intention") }
                } else {
                    OutlinedTextField(value = intention, onValueChange = onIntentionChange,
                        label = { Text("What brings you here today?") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("Peace", "Gratitude", "Focus").forEach { value ->
                            TextButton(onClick = { onIntentionChange(value) }) { Text(value) }
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { onSaveIntention(intention); editSankalpa = false }) { Text("Save") }
                        TextButton(onClick = { onIntentionChange(""); onSaveIntention(""); editSankalpa = false }) { Text("Clear") }
                        TextButton(onClick = { editSankalpa = false }) { Text("Close") }
                    }
                }
            }
        }
        RitualExperience(profiles, selectedProfileId, count, targetReached, autoRunning, intention, onIntentionChange, onSaveIntention, onSelectProfile)
        RhythmCard(dashboard.streakDays, dashboard.sessionsThisWeek)
    }
}
