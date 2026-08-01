package com.aegis.fisherman.ui.weather

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingFlat
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.DeviceThermostat
import androidx.compose.material.icons.filled.Umbrella
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.Waves
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aegis.fisherman.ui.components.StatTile
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun WeatherScreen(viewModel: WeatherViewModel = viewModel()) {
    val weather by viewModel.cachedWeather.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "WEATHER FORECAST", 
            style = MaterialTheme.typography.titleLarge,
            color = Color.White
        )

        if (weather == null) {
            com.aegis.fisherman.ui.components.GlassCard(modifier = Modifier.fillMaxWidth()) {
                Text(
                    "No forecast cached. Run sync on shore before heading out.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White
                )
            }
            return@Column
        }

        val w = weather!!
        Text(
            "Last synced: ${formatTimestamp(w.fetchedAtEpochSec)}",
            style = MaterialTheme.typography.labelLarge,
            color = Color.White.copy(alpha = 0.6f)
        )

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            StatTile(
                label = "Wind", 
                value = "%.0f km/h".format(w.windSpeedKmh), 
                icon = Icons.Default.Air,
                modifier = Modifier.weight(1f)
            )
            StatTile(
                label = "Direction", 
                value = "${w.windDirectionDeg}°", 
                icon = Icons.AutoMirrored.Filled.TrendingFlat,
                modifier = Modifier.weight(1f),
                iconModifier = Modifier.rotate(w.windDirectionDeg.toFloat())
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            StatTile(
                label = "Rain chance", 
                value = "${w.rainChancePercent}%", 
                icon = Icons.Default.Umbrella,
                modifier = Modifier.weight(1f)
            )
            StatTile(
                label = "Rainfall", 
                value = "%.1f mm".format(w.rainfallMm), 
                icon = Icons.Default.WaterDrop,
                modifier = Modifier.weight(1f)
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            StatTile(
                label = "Air temp", 
                value = "%.1f °C".format(w.temperatureC), 
                icon = Icons.Default.DeviceThermostat,
                modifier = Modifier.weight(1f)
            )
            StatTile(
                label = "Sea surface",
                value = w.seaSurfaceTempC?.let { "%.1f °C".format(it) } ?: "--",
                icon = Icons.Default.Waves,
                modifier = Modifier.weight(1f)
            )
        }

        w.advisory?.let {
            com.aegis.fisherman.ui.components.GlassCard(
                modifier = Modifier.fillMaxWidth(),
                glowColor = com.aegis.fisherman.ui.theme.AegisColors.ZoneDanger,
                alpha = 0.3f
            ) {
                Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = com.aegis.fisherman.ui.theme.AegisColors.ZoneDanger)
                    androidx.compose.foundation.layout.Spacer(Modifier.size(12.dp))
                    Text(it, style = MaterialTheme.typography.bodyLarge, color = Color.White)
                }
            }
        }
    }
}

private fun formatTimestamp(epochSec: Long): String =
    DateTimeFormatter.ofPattern("dd MMM, HH:mm")
        .withZone(ZoneId.systemDefault())
        .format(Instant.ofEpochSecond(epochSec))
