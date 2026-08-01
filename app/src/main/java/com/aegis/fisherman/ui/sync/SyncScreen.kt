package com.aegis.fisherman.ui.sync

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun SyncScreen(viewModel: SyncViewModel = viewModel()) {
    val state by viewModel.state.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Before You Sail", style = MaterialTheme.typography.headlineMedium)
        Text(
            "Run this on shore Wi-Fi/mobile data before every trip. It downloads everything " +
                "the app needs to work with zero connectivity at sea: today's weather, map " +
                "tiles for your area, and the fish/zone reference guide.",
            style = MaterialTheme.typography.bodyLarge
        )

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Sync status: ${state.name}", style = MaterialTheme.typography.bodyLarge)
                Button(onClick = {
                    // TODO: replace with the fisherman's saved home port / usual fishing ground,
                    // picked in Settings, instead of this placeholder coordinate.
                    viewModel.runSync(
                        locationLabel = "Home fishing ground",
                        lat = 8.5,
                        lng = 79.6
                    )
                }) {
                    Text("Sync now")
                }
            }
        }

        Text(
            "Map tile pack and fish/zone reference refresh are stubbed in this scaffold " +
                "(see sync/OfflinePackSyncWorker.kt) - weather sync is fully wired to " +
                "Open-Meteo as a working example.",
            style = MaterialTheme.typography.bodyLarge
        )
    }
}
