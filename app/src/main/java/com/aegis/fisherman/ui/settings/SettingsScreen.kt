package com.aegis.fisherman.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun SettingsScreen() {
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Settings", style = MaterialTheme.typography.headlineMedium)

        SettingsSection(
            title = "Boat unit",
            body = "Device name filter: ${com.aegis.fisherman.ble.BleUuids.DEVICE_NAME_PREFIX} " +
                "(set in ble/BleUuids.kt - match this to what the ESP32 firmware advertises)."
        )
        SettingsSection(
            title = "Units",
            body = "Speed: knots · Distance: metres/km. TODO: make this user-toggleable " +
                "(nautical miles is common for fishermen further offshore)."
        )
        SettingsSection(
            title = "Language",
            body = "TODO: add Tamil (and other regional language) string resources - " +
                "res/values-ta/strings.xml - since this app targets fishermen directly."
        )
        SettingsSection(
            title = "Home fishing ground",
            body = "TODO: let the fisherman save a lat/lng + label here, used by both the " +
                "Weather sync and the map's default centering."
        )
    }
}

@Composable
private fun SettingsSection(title: String, body: String) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, style = MaterialTheme.typography.titleLarge)
            Text(body, style = MaterialTheme.typography.bodyLarge)
        }
    }
}
