package com.nirmalamgroup.nirmalamchant.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
internal fun SettingsScreen(meditationToneEnabled: Boolean, hapticsEnabled: Boolean,
    defaultTarget: Int, onToneChange: (Boolean) -> Unit, onTargetChange: (Int) -> Unit,
    onHapticsChange: (Boolean) -> Unit) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("SETTINGS", style = MaterialTheme.typography.labelLarge, color = androidx.compose.ui.graphics.Color(0xFFF3C779))
        Text("Make practice your own", style = MaterialTheme.typography.headlineSmall)
        MeditationToneCard(meditationToneEnabled, onToneChange)
        SettingsCard(defaultTarget, hapticsEnabled, onTargetChange, onHapticsChange)
        PrivacyCard()
    }
}
