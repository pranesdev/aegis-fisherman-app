package com.aegis.fisherman.ui.dashboard

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Update
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Switch
import androidx.compose.ui.draw.scale
import com.aegis.fisherman.ui.components.TideWidget
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aegis.fisherman.data.model.BleConnectionState
import com.aegis.fisherman.data.model.ZoneStatus
import com.aegis.fisherman.ui.components.GlassCard
import com.aegis.fisherman.ui.components.StatTile
import com.aegis.fisherman.ui.components.ZoneStatusBanner
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun DashboardScreen(viewModel: DashboardViewModel = viewModel()) {
    val connectionState by viewModel.connectionState.collectAsState()
    val position by viewModel.latestPosition.collectAsState()
    val haptic = LocalHapticFeedback.current

    // Haptic feedback on zone status change
    androidx.compose.runtime.LaunchedEffect(position?.zone) {
        when (position?.zone) {
            ZoneStatus.WARNING -> haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            ZoneStatus.DANGER -> {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                kotlinx.coroutines.delay(200)
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            }
            else -> {}
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
        ) {
            Text("DASHBOARD", style = MaterialTheme.typography.titleLarge, color = Color.White)
            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                Text("DEMO", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.5f))
                Switch(
                    checked = viewModel.demoMode.collectAsState().value,
                    onCheckedChange = { viewModel.toggleDemoMode(it) },
                    modifier = Modifier.scale(0.7f)
                )
                Spacer(Modifier.width(8.dp))
                ConnectionStatusPill(connectionState)
            }
        }

        ZoneStatusBanner(zone = position?.zone ?: ZoneStatus.UNKNOWN)

        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            com.aegis.fisherman.ui.components.SpeedometerGauge(
                speedKnots = position?.speedKnots ?: 0.0,
                modifier = Modifier.weight(1.2f)
            )
            com.aegis.fisherman.ui.components.CompassRose(
                heading = 219f,
                modifier = Modifier.weight(1f)
            )
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            StatTile(
                label = "Boundary Dist",
                value = position?.distanceToBoundaryMeters?.let { formatDistance(it) } ?: "--",
                icon = Icons.Default.LocationOn,
                modifier = Modifier.weight(1f)
            )
            TideWidget(modifier = Modifier.weight(1f))
        }

        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Update, 
                    contentDescription = null, 
                    tint = Color.White.copy(alpha = 0.6f),
                    modifier = Modifier.size(18.dp)
                )
                androidx.compose.foundation.layout.Spacer(Modifier.width(8.dp))
                Text(
                    text = "LAST UPDATE",
                    style = MaterialTheme.typography.labelLarge,
                    color = Color.White.copy(alpha = 0.6f)
                )
            }
            Text(
                text = position?.timestampEpochSec?.let { formatTimestamp(it) } ?: "Waiting for signal...",
                style = MaterialTheme.typography.headlineMedium,
                color = Color.White
            )
        }

        var permissionsGranted by remember { mutableStateOf(false) }
        val permissionLauncher = rememberLauncherForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { results ->
            permissionsGranted = results.values.all { it }
            if (permissionsGranted) viewModel.connectToBoatUnit()
        }

        GlassCard(
            modifier = Modifier.fillMaxWidth(),
            alpha = 0.1f
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = connectionStatusLabel(connectionState),
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White
                )

                when (connectionState) {
                    BleConnectionState.DISCONNECTED, BleConnectionState.FAILED ->
                        Button(
                            onClick = { permissionLauncher.launch(requiredBlePermissions()) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp),
                            colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                                containerColor = Color.White.copy(alpha = 0.2f),
                                contentColor = Color.White
                            )
                        ) {
                            Icon(Icons.Default.Bluetooth, contentDescription = null)
                            androidx.compose.foundation.layout.Spacer(Modifier.width(8.dp))
                            Text("CONNECT TO BOAT UNIT")
                        }
                    BleConnectionState.SCANNING, BleConnectionState.CONNECTING ->
                        androidx.compose.material3.LinearProgressIndicator(
                            modifier = Modifier.fillMaxWidth(),
                            color = com.aegis.fisherman.ui.theme.AegisColors.ZoneSafe,
                            trackColor = Color.White.copy(alpha = 0.1f)
                        )
                    BleConnectionState.CONNECTED ->
                        OutlinedButton(
                            onClick = { viewModel.disconnectFromBoatUnit() },
                            modifier = Modifier.fillMaxWidth(),
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.3f))
                        ) {
                            Text("DISCONNECT", color = Color.White)
                        }
                }
            }
        }
    }
}

@Composable
private fun ConnectionStatusPill(state: BleConnectionState) {
    val color = when (state) {
        BleConnectionState.CONNECTED -> com.aegis.fisherman.ui.theme.AegisColors.ZoneSafe
        BleConnectionState.SCANNING, BleConnectionState.CONNECTING -> com.aegis.fisherman.ui.theme.AegisColors.ZoneWarning
        else -> com.aegis.fisherman.ui.theme.AegisColors.ZoneDanger
    }

    val transition = rememberInfiniteTransition(label = "Sonar")
    val pulseScale by transition.animateFloat(
        initialValue = 1f,
        targetValue = 2.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200),
            repeatMode = RepeatMode.Restart
        ),
        label = "Pulse"
    )
    
    Surface(
        shape = androidx.compose.foundation.shape.CircleShape,
        color = Color.White.copy(alpha = 0.1f),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.2f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
        ) {
            androidx.compose.foundation.layout.Box(
                modifier = Modifier
                    .size(8.dp)
                    .drawBehind {
                        if (state == BleConnectionState.SCANNING || state == BleConnectionState.CONNECTING) {
                            drawCircle(
                                color = color.copy(alpha = 0.4f * (1f - (pulseScale - 1f) / 1.5f)), 
                                radius = size.width * pulseScale
                            )
                        }
                    }
                    .background(color, androidx.compose.foundation.shape.CircleShape)
            )
            androidx.compose.foundation.layout.Spacer(Modifier.width(8.dp))
            Text(
                text = state.name,
                style = MaterialTheme.typography.labelLarge,
                color = Color.White
            )
        }
    }
}

private fun connectionStatusLabel(state: BleConnectionState) = when (state) {
    BleConnectionState.DISCONNECTED -> "Not connected to boat unit"
    BleConnectionState.SCANNING -> "Searching for boat unit..."
    BleConnectionState.CONNECTING -> "Connecting..."
    BleConnectionState.CONNECTED -> "Connected to boat unit"
    BleConnectionState.FAILED -> "Couldn't connect - check the boat unit is powered on and in range"
}

private fun formatDistance(meters: Double): String =
    if (meters >= 1000) "%.2f km".format(meters / 1000) else "%.0f m".format(meters)

private fun formatTimestamp(epochSec: Long): String =
    DateTimeFormatter.ofPattern("HH:mm:ss")
        .withZone(ZoneId.systemDefault())
        .format(Instant.ofEpochSecond(epochSec))

private fun requiredBlePermissions(): Array<String> =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        arrayOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT)
    } else {
        arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
    }
