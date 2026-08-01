package com.aegis.fisherman.ui.guide

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aegis.fisherman.data.model.FishSpecies
import com.aegis.fisherman.data.model.RestrictedZone

@Composable
fun GuideScreen(viewModel: GuideViewModel = viewModel()) {
    val fishSpecies by viewModel.fishSpecies.collectAsState()
    val restrictedZones by viewModel.restrictedZones.collectAsState()
    var selectedTab by remember { mutableIntStateOf(0) }

    Column(modifier = Modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = selectedTab) {
            Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("Fish species") })
            Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("Restricted zones") })
        }

        when (selectedTab) {
            0 -> FishSpeciesList(fishSpecies)
            1 -> RestrictedZonesList(restrictedZones)
        }
    }
}

@Composable
private fun FishSpeciesList(species: List<FishSpecies>) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValuesAll16,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(species) { fish ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(fish.commonName, style = MaterialTheme.typography.titleLarge)
                    fish.localName?.let { Text("Local name: $it", style = MaterialTheme.typography.bodyLarge) }
                    Text(fish.scientificName, style = MaterialTheme.typography.bodyLarge)
                    Text("Depth: ${fish.typicalDepthRangeM}  ·  Season: ${fish.season}", style = MaterialTheme.typography.bodyLarge)
                    if (fish.isRestrictedOrBanned) {
                        Text(
                            "⚠ Restricted / protected - verify before landing",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                    fish.notes?.let { Text(it, style = MaterialTheme.typography.bodyLarge) }
                }
            }
        }
    }
}

@Composable
private fun RestrictedZonesList(zones: List<RestrictedZone>) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValuesAll16,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(zones) { zone ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(zone.name, style = MaterialTheme.typography.titleLarge)
                    Text(zone.type.name.replace('_', ' '), style = MaterialTheme.typography.bodyLarge)
                    Text(zone.description, style = MaterialTheme.typography.bodyLarge)
                }
            }
        }
    }
}

private val PaddingValuesAll16 = androidx.compose.foundation.layout.PaddingValues(16.dp)
