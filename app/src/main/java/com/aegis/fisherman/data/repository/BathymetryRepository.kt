package com.aegis.fisherman.data.repository

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Installs the bundled GEBCO-derived bathymetry tile archive
 * (`assets/bathymetry/bathymetry.mbtiles`) into app-private storage on first use.
 *
 * SQLite (used by osmdroid's `MBTilesFileArchive`) needs a real filesystem path with random
 * access - it can't open a database packed inside the APK's compressed assets zip directly - so
 * this copies the archive out to `filesDir` once and reuses it after that.
 *
 * The archive is deliberately small (a few MB, zoom levels [MIN_ZOOM]-[MAX_ZOOM], land tiles
 * dropped since they're fully transparent) because it only needs to cover the boat's operating
 * waters. Unlike the base map tiles or weather, GEBCO bathymetry doesn't change, so it ships
 * bundled with the app instead of going through the "Before You Sail" download in
 * `sync/OfflinePackSyncWorker.kt` - there's no benefit to re-fetching static seabed data every
 * trip. If you regenerate the archive for a different operating area, just replace the asset file
 * and bump [MIN_ZOOM]/[MAX_ZOOM] here to match.
 */
class BathymetryRepository(private val context: Context) {

    suspend fun ensureInstalled(): File = withContext(Dispatchers.IO) {
        val outFile = File(context.filesDir, "bathymetry/bathymetry.mbtiles")
        if (!outFile.exists() || outFile.length() == 0L) {
            outFile.parentFile?.mkdirs()
            context.assets.open(ASSET_PATH).use { input ->
                outFile.outputStream().use { output -> input.copyTo(output) }
            }
        }
        outFile
    }

    companion object {
        private const val ASSET_PATH = "bathymetry/bathymetry.mbtiles"

        // Must match the zoom range the archive was generated with (see tools/make_bathymetry_mbtiles.py).
        const val MIN_ZOOM = 6
        const val MAX_ZOOM = 11
    }
}
