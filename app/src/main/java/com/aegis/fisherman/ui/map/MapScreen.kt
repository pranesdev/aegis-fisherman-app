package com.aegis.fisherman.ui.map

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddLocation
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Satellite
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aegis.fisherman.data.model.BoatPosition
import com.aegis.fisherman.data.model.RestrictedZone
import com.aegis.fisherman.data.repository.BathymetryRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.osmdroid.tileprovider.MapTileProviderArray
import org.osmdroid.tileprovider.MapTileProviderBasic
import org.osmdroid.tileprovider.modules.MBTilesFileArchive
import org.osmdroid.tileprovider.modules.MapTileFileArchiveProvider
import org.osmdroid.tileprovider.util.SimpleRegisterReceiver
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.tileprovider.tilesource.XYTileSource
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polygon
import org.osmdroid.views.overlay.Polyline
import org.osmdroid.views.overlay.TilesOverlay
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Base map source: defaults to OpenStreetMap online tiles for development convenience.
 * Before real sea use, swap TileSourceFactory.MAPNIK for an offline archive (osmdroid supports
 * .mbtiles / .sqlite tile archives via OfflineTileProvider) populated by the "Before You Sail"
 * sync - see sync/OfflinePackSyncWorker.kt.
 *
 * The bathymetry layer *is* wired up (GEBCO depth-shaded overlay, bundled as
 * assets/bathymetry/bathymetry.mbtiles, zoom [BathymetryRepository.MIN_ZOOM]-
 * [BathymetryRepository.MAX_ZOOM]) - see the LaunchedEffect below and
 * data/repository/BathymetryRepository.kt for how it's installed and attached as a second
 * TilesOverlay via osmdroid's MBTilesFileArchive.
 */
@Composable
fun MapScreen(viewModel: MapViewModel = viewModel()) {
    val position by viewModel.latestPosition.collectAsState()
    val restrictedZones by viewModel.restrictedZones.collectAsState()
    val satelliteEnabled by viewModel.satelliteEnabled.collectAsState()
    val demoMode by viewModel.demoMode.collectAsState()
    val trail by viewModel.breadcrumbTrail.collectAsState()
    val savedSpots by viewModel.savedSpots.collectAsState()
    
    val context = LocalContext.current

    val mapView = remember {
        MapView(context).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            controller.setZoom(9.0)
            controller.setCenter(GeoPoint(9.0, 79.5))
        }
    }

    val boatMarker = remember { Marker(mapView) }
    val breadcrumbTrail = remember { Polyline().apply { 
        outlinePaint.color = android.graphics.Color.GREEN 
        outlinePaint.strokeWidth = 5f
    }}

    // Satellite layer (NASA GIBS)
    val satelliteOverlay = remember {
        val yesterday = LocalDate.now().minusDays(1).format(DateTimeFormatter.ISO_LOCAL_DATE)
        val satelliteSource = XYTileSource(
            "NasaGibsSatellite",
            1, 9, 256, ".jpg",
            arrayOf("https://gibs.earthdata.nasa.gov/wmts/epsg3857/best/MODIS_Terra_CorrectedReflectance_TrueColor/default/$yesterday/GoogleMapsCompatible_Level9/")
        )
        val provider = MapTileProviderBasic(context, satelliteSource)
        TilesOverlay(provider, context).apply {
            loadingBackgroundColor = android.graphics.Color.TRANSPARENT
            loadingLineColor = android.graphics.Color.TRANSPARENT
        }
    }

    LaunchedEffect(satelliteEnabled) {
        mapView.overlays.remove(satelliteOverlay)
        if (satelliteEnabled) mapView.overlays.add(0, satelliteOverlay)
        mapView.invalidate()
    }

    LaunchedEffect(Unit) {
        val archiveFile = withContext(Dispatchers.IO) { viewModel.ensureBathymetryArchive() }
        val archive = MBTilesFileArchive.getDatabaseFileArchive(archiveFile)
        val bathymetryTileSource = XYTileSource(
            "AegisBathymetry",
            BathymetryRepository.MIN_ZOOM,
            BathymetryRepository.MAX_ZOOM,
            256,
            ".png",
            arrayOf(),
        )
        val archiveProvider = MapTileFileArchiveProvider(
            SimpleRegisterReceiver(context),
            bathymetryTileSource,
            arrayOf(archive),
        )
        val bathymetryTileProvider = MapTileProviderArray(bathymetryTileSource, null, arrayOf(archiveProvider))
        val bathymetryOverlay = TilesOverlay(bathymetryTileProvider, context).apply {
            loadingBackgroundColor = android.graphics.Color.TRANSPARENT
            loadingLineColor = android.graphics.Color.TRANSPARENT
        }
        
        val satelliteIndex = mapView.overlays.indexOf(satelliteOverlay)
        val insertIndex = if (satelliteIndex != -1) satelliteIndex + 1 else 0
        mapView.overlays.add(insertIndex, bathymetryOverlay)
        mapView.invalidate()
    }

    LaunchedEffect(restrictedZones) {
        mapView.overlays.removeAll { it is Polygon }
        restrictedZones.forEach { zone -> mapView.overlays.add(zone.toPolygon(mapView)) }
        mapView.invalidate()
    }

    LaunchedEffect(savedSpots) {
        mapView.overlays.removeAll { it is Marker && it.title != "Your boat" }
        savedSpots.forEach { spot ->
            val marker = Marker(mapView)
            marker.position = GeoPoint(spot.latitude, spot.longitude)
            marker.title = spot.name
            marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            mapView.overlays.add(marker)
        }
        mapView.invalidate()
    }

    LaunchedEffect(trail) {
        breadcrumbTrail.setPoints(trail.map { GeoPoint(it.latitude, it.longitude) })
        if (!mapView.overlays.contains(breadcrumbTrail)) mapView.overlays.add(breadcrumbTrail)
        mapView.invalidate()
    }

    LaunchedEffect(position) {
        val pos = position ?: return@LaunchedEffect
        val point = GeoPoint(pos.latitude, pos.longitude)
        boatMarker.position = point
        boatMarker.title = "Your boat"
        if (!mapView.overlays.contains(boatMarker)) mapView.overlays.add(boatMarker)
        mapView.controller.animateTo(point)
        mapView.invalidate()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(factory = { mapView }, modifier = Modifier.fillMaxSize())

        Column(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = { viewModel.toggleDemoMode(!demoMode) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (demoMode) Color.Red.copy(alpha = 0.6f) else Color.Black.copy(alpha = 0.4f)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(if (demoMode) Icons.Default.Stop else Icons.Default.PlayArrow, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(if (demoMode) "STOP DEMO" else "START DEMO")
            }

            Button(
                onClick = { viewModel.saveCurrentSpot("Secret Spot ${savedSpots.size + 1}") },
                colors = ButtonDefaults.buttonColors(containerColor = Color.Black.copy(alpha = 0.4f)),
                shape = RoundedCornerShape(12.dp),
                enabled = position != null
            ) {
                Icon(Icons.Default.AddLocation, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("DROP PIN")
            }
        }

        com.aegis.fisherman.ui.components.GlassCard(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 80.dp, start = 12.dp, end = 12.dp)
                .fillMaxWidth(),
            alpha = 0.3f
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("MAP LAYERS", style = MaterialTheme.typography.labelLarge, color = Color.White.copy(alpha = 0.7f))
                    Text(
                        if (demoMode) "DEMO MODE ACTIVE - Artificial Data" else "Live GPS + Satellite active",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White
                    )
                }
                IconButton(onClick = { viewModel.toggleSatellite(!satelliteEnabled) }) {
                    Icon(
                        Icons.Default.Satellite,
                        contentDescription = "Toggle Satellite",
                        tint = if (satelliteEnabled) com.aegis.fisherman.ui.theme.AegisColors.ZoneSafe else Color.White.copy(alpha = 0.4f)
                    )
                }
            }
        }
    }
}

private fun RestrictedZone.toPolygon(mapView: MapView): Polygon =
    Polygon(mapView).also { polygon ->
        polygon.points = boundaryPolygon.map { (lat, lng) -> GeoPoint(lat, lng) }
        polygon.title = name
        // Glass-tinted fill: translucent but visible
        polygon.fillPaint.color = 0x44D32F2F // Translucent red
        polygon.outlinePaint.color = android.graphics.Color.RED
        polygon.outlinePaint.strokeWidth = 3f
    }
