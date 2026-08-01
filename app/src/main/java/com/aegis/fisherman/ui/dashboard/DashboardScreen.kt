package com.aegis.fisherman.ui.dashboard

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Update
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.size
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aegis.fisherman.data.model.BleConnectionState
import com.aegis.fisherman.data.model.ZoneStatus
import com.aegis.fisherman.ui.components.StatTile
import com.aegis.fisherman.ui.components.ZoneStatusBanner
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun DashboardScreen(viewModel: DashboardViewModel = viewModel()) {
    val connectionState by viewModel.connectionState.collectAsState()
    val position by viewModel.latestPosition.collectAsState()

    var permissionsGranted by remember { mutableStateOf(false) }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        permissionsGranted = results.values.all { it }
        if (permissionsGranted) viewModel.connectToBoatUnit()
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
            Text("SEA CONDITIONS", style = MaterialTheme.typography.titleLarge, color = com.aegis.fisherman.ui.theme.AegisColors.Foam)
            ConnectionStatusPill(connectionState)
        }

        ZoneStatusBanner(zone = position?.zone ?: ZoneStatus.UNKNOWN)

        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            StatTile(
                label = "Speed",
                value = position?.speedKnots?.let { "%.1f kn".format(it) } ?: "--",
                icon = Icons.Default.Speed,
                modifier = Modifier.weight(1f)
            )
            StatTile(
                label = "Boundary Dist",
                value = position?.distanceToBoundaryMeters?.let { formatDistance(it) } ?: "--",
                icon = Icons.Default.LocationOn,
                modifier = Modifier.weight(1f)
            )
        }

        com.aegis.fisherman.ui.components.GlassCard(modifier = Modifier.fillMaxWidth()) {
            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                Icon(Icons.Default.Update, contentDescription = null, tint = com.aegis.fisherman.ui.theme.AegisColors.Foam.copy(alpha = 0.6f))
                androidx.compose.foundation.layout.Spacer(Modifier.width(8.dp))
                Text(
                    text = "LATEST SYNC",
                    style = MaterialTheme.typography.labelLarge,
                    color = com.aegis.fisherman.ui.theme.AegisColors.Foam.copy(alpha = 0.6f)
                )
            }
            Text(
                text = position?.timestampEpochSec?.let { formatTimestamp(it) } ?: "Waiting for signal...",
                style = MaterialTheme.typography.headlineMedium,
                color = com.aegis.fisherman.ui.theme.AegisColors.Foam
            )
        }

        com.aegis.fisherman.ui.components.GlassCard(
            modifier = Modifier.fillMaxWidth(),
            alpha = 0.1f
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = connectionStatusLabel(connectionState),
                    style = MaterialTheme.typography.bodyLarge,
                    color = com.aegis.fisherman.ui.theme.AegisColors.Foam
                )

                when (connectionState) {
                    BleConnectionState.DISCONNECTED, BleConnectionState.FAILED ->
                        Button(
                            onClick = { permissionLauncher.launch(requiredBlePermissions()) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp)
                        ) {
                            Icon(Icons.Default.Bluetooth, contentDescription = null)
                            androidx.compose.foundation.layout.Spacer(Modifier.width(8.dp))
                            Text("CONNECT TO BOAT UNIT")
                        }
                    BleConnectionState.SCANNING, BleConnectionState.CONNECTING ->
                        androidx.compose.material3.LinearProgressIndicator(
                            modifier = Modifier.fillMaxWidth(),
                            color = com.aegis.fisherman.ui.theme.AegisColors.ZoneSafe
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
    BleConnectionState.SCANNING -> "Searching for boat unit (${com.aegis.fisherman.ble.BleUuids.DEVICE_NAME_PREFIX})..."
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
