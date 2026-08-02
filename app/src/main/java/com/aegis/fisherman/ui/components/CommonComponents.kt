package com.aegis.fisherman.ui.components

import androidx.compose.animation.animateColor
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
 * Animated Ocean Backdrop.
 */
@Composable
fun WavesBackground(content: @Composable BoxScope.() -> Unit) {
    val transition = rememberInfiniteTransition(label = "OceanWave")
    
    val colorShift by transition.animateColor(
        initialValue = AegisColors.DeepOcean,
        targetValue = Color(0xFF1A4B6E),
        animationSpec = infiniteRepeatable(
            animation = tween(5000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "ColorShift"
    )

    val offset by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(20000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "WaveOffset"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .drawBehind {
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(colorShift, AegisColors.DeepOcean),
                        startY = 0f,
                        endY = size.height
                    )
                )
                
                val centerX = (offset * size.width * 2f) - size.width
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color.White.copy(alpha = 0.05f), Color.Transparent),
                        center = androidx.compose.ui.geometry.Offset(centerX, size.height * 0.4f),
                        radius = size.maxDimension
                    )
                )
            }
    ) {
        this.content()
    }
}

/** 
 * Core Glassmorphism Surface.
 * Includes a subtle hairline gradient border and frosted transparency.
 */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    alpha: Float = 0.20f,
    glowColor: Color = Color.Transparent,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(26.dp))
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    0.0f to Color.White.copy(alpha = 0.4f),
                    1.0f to Color.White.copy(alpha = 0.1f)
                ),
                shape = RoundedCornerShape(26.dp)
            )
            .drawBehind {
                if (glowColor != Color.Transparent) {
                    drawCircle(
                        color = glowColor.copy(alpha = 0.1f),
                        radius = size.maxDimension * 0.8f,
                        center = androidx.compose.ui.geometry.Offset(size.width / 2, size.height / 2)
                    )
                }
            },
        color = Color.White.copy(alpha = alpha),
        shape = RoundedCornerShape(26.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            this.content()
        }
    }
}

@Composable
fun ZoneStatusBanner(zone: ZoneStatus, modifier: Modifier = Modifier) {
    val alpha = if (zone == ZoneStatus.DANGER) 0.4f else 0.25f
    
    GlassCard(
        modifier = modifier.fillMaxWidth(),
        alpha = alpha,
        glowColor = zoneColor(zone)
    ) {
        Box(
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = zoneLabel(zone),
                color = if (zone == ZoneStatus.DANGER) Color.White else zoneColor(zone),
                style = MaterialTheme.typography.headlineSmall
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
                    modifier = Modifier.size(20.dp).then(iconModifier),
                    tint = Color.White.copy(alpha = 0.7f)
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(
                text = label.uppercase(),
                style = MaterialTheme.typography.labelLarge,
                color = Color.White.copy(alpha = 0.7f)
            )
        }
        Text(
            text = value,
            style = MaterialTheme.typography.headlineMedium, // Slightly smaller to prevent wrapping (e.g. km/h)
            color = Color.White,
            maxLines = 1
        )
    }
}

@Composable
fun SpeedometerGauge(speedKnots: Double, modifier: Modifier = Modifier) {
    GlassCard(modifier = modifier.height(180.dp)) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
            Canvas(modifier = Modifier.size(140.dp)) {
                val sweep = 240f
                val start = 150f
                
                // Background track
                drawArc(
                    color = Color.White.copy(alpha = 0.1f),
                    startAngle = start,
                    sweepAngle = sweep,
                    useCenter = false,
                    style = Stroke(width = 12.dp.toPx(), cap = StrokeCap.Round)
                )
                
                // Active speed track
                val speedSweep = (speedKnots / 25.0 * sweep).toFloat().coerceIn(0f, sweep)
                drawArc(
                    brush = Brush.linearGradient(listOf(AegisColors.ZoneSafe, AegisColors.ZoneWarning)),
                    startAngle = start,
                    sweepAngle = speedSweep,
                    useCenter = false,
                    style = Stroke(width = 12.dp.toPx(), cap = StrokeCap.Round)
                )
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "%.1f".format(speedKnots),
                    style = MaterialTheme.typography.displayLarge.copy(fontSize = 42.sp),
                    color = Color.White
                )
                Text(
                    text = "KNOTS",
                    style = MaterialTheme.typography.labelLarge,
                    color = Color.White.copy(alpha = 0.6f)
                )
            }
        }
    }
}

@Composable
fun CompassRose(heading: Float, modifier: Modifier = Modifier) {
    GlassCard(modifier = modifier.size(180.dp)) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
            Canvas(modifier = Modifier.size(120.dp)) {
                // Outer ring
                drawCircle(
                    color = Color.White.copy(alpha = 0.15f),
                    style = Stroke(width = 2.dp.toPx())
                )
                
                // Cardinal points
                val radius = size.minDimension / 2
                // Skip full draw for brevity, just a needle for now
            }
            
            // Rotating Needle
            Icon(
                imageVector = Icons.Default.Navigation,
                contentDescription = null,
                modifier = Modifier.size(60.dp).rotate(heading),
                tint = AegisColors.ZoneDanger
            )
            
            Text(
                text = "${heading.toInt()}°",
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 10.dp),
                style = MaterialTheme.typography.titleMedium,
                color = Color.White
            )
        }
    }
}
@Composable
fun TideWidget(modifier: Modifier = Modifier) {
    GlassCard(modifier = modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.Navigation,
                contentDescription = null,
                modifier = Modifier.size(18.dp).rotate(90f),
                tint = Color.Cyan
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "TIDE",
                style = MaterialTheme.typography.labelLarge,
                color = Color.White.copy(alpha = 0.6f)
            )
        }
        Text(
            text = "HIGH (1.4m)",
            style = MaterialTheme.typography.titleMedium,
            color = Color.White
        )
        Text(
            text = "Next Low: 18:45",
            style = MaterialTheme.typography.bodySmall,
            color = Color.White.copy(alpha = 0.5f)
        )
    }
}

