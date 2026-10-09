package com.nirmalamgroup.nirmalamchant

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import com.nirmalamgroup.nirmalamchant.ui.NirmalamTheme
import com.nirmalamgroup.nirmalamchant.ui.screens.ChantHome

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()
    private var scheduleAfterNotificationPermission = false
    private val notificationPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted && scheduleAfterNotificationPermission) viewModel.planEveningPractice()
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
            val defaultTarget by viewModel.defaultTarget.collectAsStateWithLifecycle()
            val savedIntention by viewModel.intention.collectAsStateWithLifecycle()
            val canUndoManualTally by viewModel.canUndoManualTally.collectAsStateWithLifecycle()
            val profiles by viewModel.profiles.collectAsStateWithLifecycle()
            val selectedProfileId by viewModel.selectedProfileId.collectAsStateWithLifecycle()
            NirmalamTheme {
                ChantHome(
                    count = count, currentTarget = currentTarget, targetReached = targetReached,
                    profiles = profiles, selectedProfileId = selectedProfileId,
                    onSelectProfile = viewModel::selectProfile, onSaveProfile = viewModel::saveChantProfile,
                    savedIntention = savedIntention, dashboard = dashboard,
                    meditationToneEnabled = meditationToneEnabled, hapticsEnabled = hapticsEnabled,
                    defaultTarget = defaultTarget, canUndoManualTally = canUndoManualTally,
                    onAdd = viewModel::addManualTally, onUndo = viewModel::undoManualTally,
                    onReset = viewModel::resetCurrentPractice, onBeginNext = viewModel::beginNextPractice,
                    onSaveIntention = viewModel::saveIntention,
                    onToneChange = viewModel::setMeditationToneEnabled,
                    onHapticsChange = viewModel::setHapticsEnabled,
                    onTargetChange = viewModel::setDefaultTarget,
                    onPlan = ::planPractice, onStartPlan = viewModel::startPlannedPractice,
                    onEditPlan = viewModel::editPlan, onPostponePlan = viewModel::postponePlan,
                    onSkipPlan = viewModel::skipPlan, onDeletePlan = viewModel::deletePlan
                )
            }
        }
    }

    private fun planPractice() {
        if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            scheduleAfterNotificationPermission = true
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else viewModel.planEveningPractice()
    }
}
