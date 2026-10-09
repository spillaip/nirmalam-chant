package com.nirmalamgroup.nirmalamchant.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.background
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.nirmalamgroup.nirmalamchant.DashboardState
import com.nirmalamgroup.nirmalamchant.data.PracticePlan

@Composable
internal fun JourneyScreen(
    dashboard: DashboardState, onPlan: () -> Unit,
    onStartPlan: (PracticePlan) -> Unit,
    onEditPlan: (PracticePlan, String, Int, Boolean) -> Unit,
    onPostponePlan: (PracticePlan) -> Unit,
    onSkipPlan: (PracticePlan) -> Unit,
    onDeletePlan: (PracticePlan) -> Unit
) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        DashboardHeading("Your journey")
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            JourneyStat("Chants", dashboard.completedForInsights.sumOf { it.tallyCount }.toString(), Modifier.weight(1f))
            JourneyStat("Sessions", dashboard.completedForInsights.count { it.tallyCount > 0 }.toString(), Modifier.weight(1f))
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            JourneyStat("Streak", "${dashboard.streakDays} days", Modifier.weight(1f))
            JourneyStat("This week", dashboard.sessionsThisWeek.toString(), Modifier.weight(1f))
        }
        SmartMalaJourney(dashboard)
        ConsistencyCalendar(dashboard)
        PracticeInsights(dashboard)
        Text("Your completed practices, saved on this device.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
        DashboardHeading("Planned & scheduled")
        if (dashboard.planned.isEmpty()) {
            Button(onClick = onPlan, modifier = Modifier.fillMaxWidth()) { Text("Plan an evening practice") }
        } else {
            dashboard.planned.forEach { plan ->
                key(plan.id) { PlanCard(plan, onStartPlan, onEditPlan, onPostponePlan, onSkipPlan, onDeletePlan) }
            }
            Button(onClick = onPlan, modifier = Modifier.fillMaxWidth()) { Text("Plan another evening practice") }
        }
        DashboardHeading("Recent practice history")
        if (dashboard.performed.isEmpty()) {
            EmptyActivityCard("Your practice history will appear here.")
        } else {
            dashboard.performed.forEach { activity ->
                key(activity.id) {
                    ActivityCard(activity.title, buildString {
                        append("${activity.tallyCount} of ${activity.targetCount} chants · ${formatActivityTime(activity.startedAt)}")
                        activity.intention?.let { append("\nIntention: $it") }
                    })
                }
            }
        }
    }
}

@Composable
private fun JourneyStat(label: String, value: String, modifier: Modifier = Modifier) {
    Card(modifier) {
        Column(Modifier.padding(15.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text(value, style = MaterialTheme.typography.headlineSmall, color = Color(0xFFF3C779))
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.secondary)
        }
    }
}

/** Aggregated from all completed sessions in the local Room database. */
@Composable
private fun PracticeInsights(dashboard: DashboardState) {
    val today = java.time.LocalDate.now()
    val recent = dashboard.completedForInsights.filter { it.tallyCount > 0 }
    val seven = recent.filter { !it.startedAt.atZone(java.time.ZoneId.systemDefault()).toLocalDate().isBefore(today.minusDays(6)) }
    val thirty = recent.filter { !it.startedAt.atZone(java.time.ZoneId.systemDefault()).toLocalDate().isBefore(today.minusDays(29)) }
    DashboardHeading("Practice insights")
    Text("From all completed sessions saved on this device", style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.secondary)
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        JourneyStat("Last 7 days", "${seven.sumOf { it.tallyCount }} chants", Modifier.weight(1f))
        JourneyStat("Last 30 days", "${thirty.sumOf { it.tallyCount }} chants", Modifier.weight(1f))
    }
    if (recent.isEmpty()) {
        EmptyActivityCard("Complete a practice to see your weekly and monthly insights.")
    } else {
        val dailyCounts = (6 downTo 0).map { offset ->
            val day = today.minusDays(offset.toLong())
            day to seven.filter { it.startedAt.atZone(java.time.ZoneId.systemDefault()).toLocalDate() == day }
                .sumOf { it.tallyCount }
        }
        val maxCount = dailyCounts.maxOf { it.second }.coerceAtLeast(1)
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Last seven days", style = MaterialTheme.typography.titleSmall)
                dailyCounts.forEach { (day, chants) ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(day.dayOfWeek.name.take(3), modifier = Modifier.width(46.dp), style = MaterialTheme.typography.labelSmall)
                        LinearProgressIndicator(progress = { chants.toFloat() / maxCount },
                            modifier = Modifier.weight(1f).height(8.dp))
                        Text("$chants", modifier = Modifier.width(48.dp),
                            style = MaterialTheme.typography.labelSmall, textAlign = androidx.compose.ui.text.style.TextAlign.End)
                    }
                }
            }
        }
    }
}


/** Counts completed 108-repetition malas, including multi-mala targets. Partial remainders carry across sessions. */
@Composable
private fun SmartMalaJourney(dashboard: DashboardState) {
    val completed = dashboard.completedForInsights.filter { it.tallyCount > 0 }
    val total = completed.sumOf { it.tallyCount.toLong() }
    val malas = total / 108L
    val beads = (total % 108L).toInt()
    DashboardHeading("Smart Mala Journey")
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("$malas completed malas", style = MaterialTheme.typography.headlineSmall, color = Color(0xFFF3C779))
            Text("$beads of 108 toward the next mala", style = MaterialTheme.typography.bodyMedium)
            LinearProgressIndicator(progress = { beads / 108f }, modifier = Modifier.fillMaxWidth().height(9.dp))
            Text("Based on completed practice sessions. Unfinished sessions are not included.",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
        }
    }
}

/** Thirty-day local practice calendar; square opacity tracks daily chant volume. */
@Composable
private fun ConsistencyCalendar(dashboard: DashboardState) {
    val today = java.time.LocalDate.now()
    val timezone = java.time.ZoneId.systemDefault()
    val counts = dashboard.completedForInsights.filter { it.tallyCount > 0 }
        .groupBy { it.startedAt.atZone(timezone).toLocalDate() }
        .mapValues { (_, activities) -> activities.sumOf { it.tallyCount } }
    val days = (29 downTo 0).map { today.minusDays(it.toLong()) }
    val max = days.maxOf { counts[it] ?: 0 }.coerceAtLeast(1)
    DashboardHeading("Spiritual consistency")
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
            Text("Last 30 days · ${days.count { (counts[it] ?: 0) > 0 }} active days",
                style = MaterialTheme.typography.titleSmall)
            days.chunked(7).forEach { week ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    week.forEach { day ->
                        val n = counts[day] ?: 0
                        val strength = if (n == 0) 0f else 0.25f + 0.75f * n / max.toFloat()
                        val shade = if (n == 0) Color(0xFF3B554A) else Color(0xFF63BE87).copy(alpha = strength)
                        Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                            androidx.compose.foundation.layout.Box(
                                Modifier.fillMaxWidth().height(25.dp)
                                    .background(shade, androidx.compose.foundation.shape.RoundedCornerShape(5.dp))
                                    .semantics { contentDescription = "$day: $n chants" }
                            )
                            Text(day.dayOfMonth.toString(), style = MaterialTheme.typography.labelSmall)
                        }
                    }
                    repeat(7 - week.size) { Spacer(Modifier.weight(1f)) }
                }
            }
            Text("Darker squares indicate more chants. Only completed sessions are shown.",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
        }
    }
}
