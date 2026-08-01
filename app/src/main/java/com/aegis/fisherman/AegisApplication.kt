package com.aegis.fisherman

import android.app.Application
import org.osmdroid.config.Configuration

class AegisApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        AegisServices.init(this)

        // osmdroid needs a user agent + a writable cache dir for downloaded/pre-loaded map tiles.
        // Tile packs (base map + bathymetry overlay) live under filesDir so they survive
        // without needing external storage permissions.
        Configuration.getInstance().apply {
            userAgentValue = packageName
            osmdroidBasePath = getExternalFilesDir(null) ?: filesDir
            osmdroidTileCache = java.io.File(osmdroidBasePath, "tiles")
        }
    }
}
