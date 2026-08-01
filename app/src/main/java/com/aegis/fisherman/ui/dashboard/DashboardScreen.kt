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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
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
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("AEGIS Dashboard", style = MaterialTheme.typography.headlineMedium)

        ZoneStatusBanner(zone = position?.zone ?: ZoneStatus.UNKNOWN)

        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            StatTile(
                label = "Speed",
                value = position?.speedKnots?.let { "%.1f kn".format(it) } ?: "--",
                modifier = Modifier.weight(1f)
            )
            StatTile(
                label = "Distance to boundary",
                value = position?.distanceToBoundaryMeters?.let { formatDistance(it) } ?: "--",
                modifier = Modifier.weight(1f)
            )
        }

        StatTile(
            label = "Last update from boat unit",
            value = position?.timestampEpochSec?.let { formatTimestamp(it) } ?: "No data yet",
            modifier = Modifier.fillMaxWidth()
        )

        Text(
            text = connectionStatusLabel(connectionState),
            style = MaterialTheme.typography.bodyLarge
        )

        when (connectionState) {
            BleConnectionState.DISCONNECTED, BleConnectionState.FAILED ->
                Button(onClick = { permissionLauncher.launch(requiredBlePermissions()) }) {
                    Text("Connect to boat unit")
                }
            BleConnectionState.SCANNING, BleConnectionState.CONNECTING ->
                Text("Working...", style = MaterialTheme.typography.bodyLarge)
            BleConnectionState.CONNECTED ->
                OutlinedButton(onClick = { viewModel.disconnectFromBoatUnit() }) {
                    Text("Disconnect")
                }
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
