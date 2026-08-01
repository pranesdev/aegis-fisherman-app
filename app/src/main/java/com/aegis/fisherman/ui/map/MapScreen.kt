package com.aegis.fisherman.ui.map

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aegis.fisherman.data.model.RestrictedZone
import com.aegis.fisherman.data.repository.BathymetryRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.osmdroid.tileprovider.MapTileProviderArray
import org.osmdroid.tileprovider.modules.MBTilesFileArchive
import org.osmdroid.tileprovider.modules.MapTileFileArchiveProvider
import org.osmdroid.tileprovider.util.SimpleRegisterReceiver
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.tileprovider.tilesource.XYTileSource
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polygon
import org.osmdroid.views.overlay.TilesOverlay

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
    val context = LocalContext.current

    val mapView = remember {
        MapView(context).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            controller.setZoom(9.0)
            controller.setCenter(GeoPoint(9.0, 79.5)) // rough Gulf of Mannar / Palk Bay area
        }
    }

    val boatMarker = remember { Marker(mapView) }

    // Bathymetry overlay: install the bundled MBTiles archive (first launch only - a no-op copy
    // check after that) and attach it as its own TilesOverlay, independent of the base map's
    // tile provider. Runs once per MapView instance; inserted at index 0 so it sits above the
    // base map but below the boundary polygons and boat marker added by the effects below.
    LaunchedEffect(Unit) {
        val archiveFile = withContext(Dispatchers.IO) { viewModel.ensureBathymetryArchive() }
        val archive = MBTilesFileArchive.getDatabaseFileArchive(archiveFile)
        val bathymetryTileSource = XYTileSource(
            "AegisBathymetry",
            BathymetryRepository.MIN_ZOOM,
            BathymetryRepository.MAX_ZOOM,
            256,
            ".png",
            arrayOf() // archive-only: no remote base URL, MBTilesFileArchive serves every tile
        )
        val archiveProvider = MapTileFileArchiveProvider(
            SimpleRegisterReceiver(context),
            bathymetryTileSource,
            arrayOf(archive)
        )
        val bathymetryTileProvider = MapTileProviderArray(bathymetryTileSource, null, arrayOf(archiveProvider))
        val bathymetryOverlay = TilesOverlay(bathymetryTileProvider, context).apply {
            loadingBackgroundColor = android.graphics.Color.TRANSPARENT
            loadingLineColor = android.graphics.Color.TRANSPARENT
        }
        mapView.overlays.add(0, bathymetryOverlay)
        mapView.invalidate()
    }

    LaunchedEffect(restrictedZones) {
        mapView.overlays.removeAll { it is Polygon }
        restrictedZones.forEach { zone -> mapView.overlays.add(zone.toPolygon(mapView)) }
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

        Surface(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(12.dp),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
            shape = MaterialTheme.shapes.medium
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text("Boundary & zone map", style = MaterialTheme.typography.titleLarge)
                Text(
                    "Depth shading is bundled and works offline. Base map tiles still need a " +
                        "pre-downloaded pack via \"Before You Sail\" sync before use at sea.",
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }
    }
}

private fun RestrictedZone.toPolygon(mapView: MapView): Polygon =
    Polygon(mapView).also { polygon ->
        polygon.points = boundaryPolygon.map { (lat, lng) -> GeoPoint(lat, lng) }
        polygon.title = name
        polygon.fillPaint.color = 0x33D32F2F // translucent red fill regardless of zone type,
        polygon.outlinePaint.color = android.graphics.Color.RED
        polygon.outlinePaint.strokeWidth = 4f
    }
