package com.aegis.fisherman.ui.history

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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

    Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("TRIP LOG", style = MaterialTheme.typography.titleLarge, color = androidx.compose.ui.graphics.Color.White)
        Text(
            "On-phone record of every reading from the boat unit.",
            style = MaterialTheme.typography.bodyLarge,
            color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.6f)
        )

        if (selectedTripId == null) {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(tripIds) { tripId ->
                    com.aegis.fisherman.ui.components.GlassCard(
                        modifier = Modifier.fillMaxWidth().clickable { viewModel.selectTrip(tripId) }
                    ) {
                        Text(
                            text = "Trip: $tripId", 
                            modifier = Modifier.padding(8.dp), 
                            style = MaterialTheme.typography.bodyLarge,
                            color = androidx.compose.ui.graphics.Color.White
                        )
                    }
                }
            }
        } else {
            Text("Selected: $selectedTripId", style = MaterialTheme.typography.titleLarge, color = androidx.compose.ui.graphics.Color.White)
            LazyColumn(
                contentPadding = PaddingValues(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(entries) { entry ->
                    com.aegis.fisherman.ui.components.GlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        alpha = 0.15f
                    ) {
                        Column(modifier = Modifier.padding(4.dp)) {
                            Text(
                                text = "${entry.latitude}, ${entry.longitude}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = androidx.compose.ui.graphics.Color.White
                            )
                            Text(
                                text = entry.zone,
                                style = MaterialTheme.typography.labelLarge,
                                color = zoneColor(ZoneStatus.fromString(entry.zone))
                            )
                            Text(
                                text = "dist: ${entry.distanceToBoundaryMeters ?: "--"} m  ·  " +
                                    "speed: ${entry.speedKnots ?: "--"} kn",
                                style = MaterialTheme.typography.bodySmall,
                                color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.6f)
                            )
                        }
                    }
                }
            }
        }
    }
}
