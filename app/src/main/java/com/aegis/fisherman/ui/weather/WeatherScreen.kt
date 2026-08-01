package com.aegis.fisherman.ui.weather

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
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
        Text("Weather (offline forecast)", style = MaterialTheme.typography.headlineMedium)

        if (weather == null) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Text(
                    "No forecast cached yet. Connect to Wi-Fi/data on shore and run " +
                        "\"Before You Sail\" sync before heading out.",
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodyLarge
                )
            }
            return@Column
        }

        val w = weather!!
        Text(
            "Last synced: ${formatTimestamp(w.fetchedAtEpochSec)} - for ${w.validForDate}",
            style = MaterialTheme.typography.bodyLarge
        )

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            StatTile("Wind", "%.0f km/h".format(w.windSpeedKmh), Modifier.weight(1f))
            StatTile("Wind direction", "${w.windDirectionDeg}°", Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            StatTile("Rain chance", "${w.rainChancePercent}%", Modifier.weight(1f))
            StatTile("Rainfall", "%.1f mm".format(w.rainfallMm), Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            StatTile("Air temp", "%.1f °C".format(w.temperatureC), Modifier.weight(1f))
            StatTile(
                "Sea surface temp",
                w.seaSurfaceTempC?.let { "%.1f °C".format(it) } ?: "Not available",
                Modifier.weight(1f)
            )
        }

        w.advisory?.let {
            Card(modifier = Modifier.fillMaxWidth()) {
                Text("⚠ $it", modifier = Modifier.padding(16.dp), style = MaterialTheme.typography.bodyLarge)
            }
        }
    }
}

private fun formatTimestamp(epochSec: Long): String =
    DateTimeFormatter.ofPattern("dd MMM, HH:mm")
        .withZone(ZoneId.systemDefault())
        .format(Instant.ofEpochSecond(epochSec))
