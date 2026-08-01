package com.aegis.fisherman.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.aegis.fisherman.data.model.ZoneStatus
import com.aegis.fisherman.ui.theme.AegisColors

fun zoneColor(zone: ZoneStatus): Color = when (zone) {
    ZoneStatus.SAFE -> AegisColors.ZoneSafe
    ZoneStatus.WARNING -> AegisColors.ZoneWarning
    ZoneStatus.DANGER -> AegisColors.ZoneDanger
    ZoneStatus.UNKNOWN -> AegisColors.ZoneUnknown
}

fun zoneLabel(zone: ZoneStatus): String = when (zone) {
    ZoneStatus.SAFE -> "SAFE"
    ZoneStatus.WARNING -> "WARNING - approaching boundary"
    ZoneStatus.DANGER -> "DANGER - near/at boundary"
    ZoneStatus.UNKNOWN -> "NO SIGNAL FROM BOAT UNIT"
}

/** Big, unmissable zone banner - mirrors the boat unit's own LED/buzzer state (Section 4 of doc). */
@Composable
fun ZoneStatusBanner(zone: ZoneStatus, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(zoneColor(zone), RoundedCornerShape(16.dp))
            .padding(vertical = 24.dp, horizontal = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = zoneLabel(zone),
            color = Color.White,
            style = MaterialTheme.typography.headlineMedium
        )
    }
}

@Composable
fun StatTile(label: String, value: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(text = label, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
            Text(text = value, style = MaterialTheme.typography.displayLarge, color = MaterialTheme.colorScheme.onSurface)
        }
    }
}
