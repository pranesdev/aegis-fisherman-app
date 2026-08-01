package com.aegis.fisherman.ui.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aegis.fisherman.ui.components.zoneColor
import com.aegis.fisherman.data.model.ZoneStatus

@Composable
fun TripHistoryScreen(viewModel: TripHistoryViewModel = viewModel()) {
    val tripIds by viewModel.tripIds.collectAsState()
    val selectedTripId by viewModel.selectedTripId.collectAsState()
    val entries by viewModel.selectedTripEntries.collectAsState()

    Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Trip log", style = MaterialTheme.typography.headlineMedium)
        Text(
            "On-phone record of every reading from the boat unit, one entry per trip - a " +
                "convenience mirror of the boat unit's own SD-card blackbox, not a replacement for it.",
            style = MaterialTheme.typography.bodyLarge
        )

        if (selectedTripId == null) {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(tripIds) { tripId ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { viewModel.selectTrip(tripId) }
                    ) {
                        Text(tripId, modifier = Modifier.padding(16.dp), style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        } else {
            Text("Trip: $selectedTripId", style = MaterialTheme.typography.titleLarge)
            LazyColumn(
                contentPadding = PaddingValues(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(entries) { entry ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                "${entry.latitude}, ${entry.longitude}  ·  ${entry.zone}",
                                color = zoneColor(ZoneStatus.fromString(entry.zone))
                            )
                            Text(
                                "dist: ${entry.distanceToBoundaryMeters ?: "--"} m  ·  " +
                                    "speed: ${entry.speedKnots ?: "--"} kn  ·  ts: ${entry.timestampEpochSec}",
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                }
            }
        }
    }
}
