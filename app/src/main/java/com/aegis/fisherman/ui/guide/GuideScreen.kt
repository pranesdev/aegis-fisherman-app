package com.aegis.fisherman.ui.guide

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = Color.Transparent,
            contentColor = Color.White,
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                    color = com.aegis.fisherman.ui.theme.AegisColors.ZoneSafe
                )
            }
        ) {
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
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(species) { fish ->
            com.aegis.fisherman.ui.components.GlassCard(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = fish.commonName, 
                    style = MaterialTheme.typography.headlineSmall,
                    color = Color.White
                )
                fish.localName?.let { 
                    Text(it, style = MaterialTheme.typography.titleMedium, color = com.aegis.fisherman.ui.theme.AegisColors.ZoneWarning) 
                }
                Text(
                    text = fish.scientificName, 
                    style = MaterialTheme.typography.bodyMedium, 
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                    color = Color.White.copy(alpha = 0.6f)
                )
                
                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), thickness = 0.5.dp, color = Color.White.copy(alpha = 0.2f))
                
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Column {
                        Text("DEPTH", style = MaterialTheme.typography.labelSmall, color = com.aegis.fisherman.ui.theme.AegisColors.ZoneSafe)
                        Text(fish.typicalDepthRangeM, style = MaterialTheme.typography.bodyLarge, color = Color.White)
                    }
                    Column {
                        Text("SEASON", style = MaterialTheme.typography.labelSmall, color = com.aegis.fisherman.ui.theme.AegisColors.ZoneSafe)
                        Text(fish.season, style = MaterialTheme.typography.bodyLarge, color = Color.White)
                    }
                }

                if (fish.isRestrictedOrBanned) {
                    Surface(
                        color = com.aegis.fisherman.ui.theme.AegisColors.ZoneDanger.copy(alpha = 0.2f),
                        shape = MaterialTheme.shapes.small,
                        border = androidx.compose.foundation.BorderStroke(1.dp, com.aegis.fisherman.ui.theme.AegisColors.ZoneDanger.copy(alpha = 0.4f))
                    ) {
                        Row(
                            modifier = Modifier.padding(8.dp),
                            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Warning, contentDescription = null, tint = com.aegis.fisherman.ui.theme.AegisColors.ZoneDanger, modifier = Modifier.size(16.dp))
                            androidx.compose.foundation.layout.Spacer(Modifier.size(8.dp))
                            Text(
                                "RESTRICTED SPECIES",
                                color = com.aegis.fisherman.ui.theme.AegisColors.ZoneDanger,
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                    }
                }
                
                fish.notes?.let { 
                    Text(it, style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.7f)) 
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
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(zones) { zone ->
            com.aegis.fisherman.ui.components.GlassCard(modifier = Modifier.fillMaxWidth()) {
                Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    Icon(Icons.Default.Info, contentDescription = null, tint = com.aegis.fisherman.ui.theme.AegisColors.ZoneSafe)
                    androidx.compose.foundation.layout.Spacer(Modifier.size(8.dp))
                    Text(
                        text = zone.name, 
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White
                    )
                }
                Text(
                    zone.type.name.replace('_', ' '),
                    style = MaterialTheme.typography.labelLarge,
                    color = com.aegis.fisherman.ui.theme.AegisColors.ZoneWarning
                )
                Text(
                    text = zone.description, 
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.7f)
                )
            }
        }
    }
}

private val PaddingValuesAll16 = androidx.compose.foundation.layout.PaddingValues(16.dp)
