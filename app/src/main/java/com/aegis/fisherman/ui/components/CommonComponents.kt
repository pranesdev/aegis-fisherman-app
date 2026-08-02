package com.aegis.fisherman.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
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
    ZoneStatus.SAFE -> "MARITIME STATUS: SAFE"
    ZoneStatus.WARNING -> "WARNING: APPROACHING BOUNDARY"
    ZoneStatus.DANGER -> "DANGER: AT BOUNDARY"
    ZoneStatus.UNKNOWN -> "NO SIGNAL FROM BOAT UNIT"
}

/** 
 * Core Glassmorphism Surface.
 * Includes a subtle hairline gradient border and frosted transparency.
 */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    alpha: Float = 0.15f,
    glowColor: Color = Color.Transparent,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(24.dp))
            .drawBehind {
                if (glowColor != Color.Transparent) {
                    drawCircle(
                        color = glowColor.copy(alpha = 0.15f),
                        radius = size.maxDimension,
                        alpha = 0.2f
                    )
                }
            }
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    0.0f to Color.White.copy(alpha = 0.4f),
                    1.0f to Color.White.copy(alpha = 0.05f)
                ),
                shape = RoundedCornerShape(24.dp)
            ),
        color = Color.White.copy(alpha = alpha),
        shape = RoundedCornerShape(24.dp),
        content = {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                this.content()
            }
        }
    )
}

/** Emphasized glass indicator - the "least transparent" card for safety visibility. */
@Composable
fun ZoneStatusBanner(zone: ZoneStatus, modifier: Modifier = Modifier) {
    val alpha = if (zone == ZoneStatus.DANGER) 0.4f else 0.25f
    
    GlassCard(
        modifier = modifier.fillMaxWidth(),
        alpha = alpha,
        glowColor = zoneColor(zone)
    ) {
        Box(
            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = zoneLabel(zone),
                color = if (zone == ZoneStatus.DANGER) Color.White else zoneColor(zone),
                style = MaterialTheme.typography.titleLarge
            )
        }
    }
}

@Composable
fun StatTile(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    iconModifier: Modifier = Modifier
) {
    GlassCard(modifier = modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp).then(iconModifier),
                    tint = AegisColors.Foam.copy(alpha = 0.6f)
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(
                text = label.uppercase(),
                style = MaterialTheme.typography.labelLarge,
                color = AegisColors.Foam.copy(alpha = 0.6f)
            )
        }
        Text(
            text = value,
            style = MaterialTheme.typography.displaySmall,
            color = AegisColors.Foam
        )
    }
}
