package com.aegis.fisherman.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.aegis.fisherman.data.db.AegisDatabase
import com.aegis.fisherman.data.repository.WeatherRepository

/**
 * "Before You Sail" background sync. Run this once, on shore Wi-Fi/mobile data, before a trip.
 * It refreshes everything the app needs to work with zero connectivity at sea:
 *   1. Weather forecast for the fishing area (WeatherRepository -> Room cache)
 *   2. Map tiles for the operating area (TODO: wire in an osmdroid tile pre-fetch / MBTiles
 *      download here once you've picked a tile source for base map + bathymetry overlay)
 *   3. Fish species / restricted zone reference data, if your backend publishes updates
 *      beyond the bundled seed (OfflineDataRepository currently only seeds once from assets -
 *      extend it with a remote refresh call here when you have a real data source)
 *
 * Wire this up from SyncScreen via WorkManager's OneTimeWorkRequest, observing progress with
 * WorkInfo so the UI can show a per-item progress list.
 */
class OfflinePackSyncWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val db = AegisDatabase.get(applicationContext)
        val weatherRepo = WeatherRepository(db)

        val locationLabel = inputData.getString(KEY_LOCATION_LABEL) ?: return Result.failure()
        val lat = inputData.getDouble(KEY_LAT, Double.NaN)
        val lng = inputData.getDouble(KEY_LNG, Double.NaN)
        if (lat.isNaN() || lng.isNaN()) return Result.failure()

        val weatherResult = weatherRepo.fetchAndCache(locationLabel, lat, lng)

        // TODO: map tile pack + reference-data refresh go here, following the same
        // fetch-then-cache-to-Room/disk pattern as the weather call above.

        return if (weatherResult.isSuccess) Result.success() else Result.retry()
    }

    companion object {
        const val KEY_LOCATION_LABEL = "location_label"
        const val KEY_LAT = "lat"
        const val KEY_LNG = "lng"
    }
}
