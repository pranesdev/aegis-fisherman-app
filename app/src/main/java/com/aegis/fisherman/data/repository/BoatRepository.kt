package com.aegis.fisherman.data.repository

import com.aegis.fisherman.ble.BoatBleManager
import com.aegis.fisherman.data.db.AegisDatabase
import com.aegis.fisherman.data.db.TripLogEntity
import com.aegis.fisherman.data.model.BleConnectionState
import com.aegis.fisherman.data.model.BoatPosition
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.time.ZoneOffset

/**
 * Single source of truth for "where is the boat right now". Wraps the BLE manager and mirrors
 * every reading into the local trip_log table, matching the boat unit's own SD-card blackbox
 * approach (AEGIS doc, Section 5.1) - this phone-side log is a convenience layer for the
 * fisherman, not a replacement for the boat unit's own record.
 */
class BoatRepository(
    private val ble: BoatBleManager,
    private val db: AegisDatabase
) {
    val connectionState: StateFlow<BleConnectionState> = ble.connectionState
    val latestPosition: StateFlow<BoatPosition?> = ble.latestPosition

    private var currentTripId: String? = null
    private val scope = CoroutineScope(Dispatchers.IO)

    init {
        scope.launch {
            ble.latestPosition.collect { position ->
                if (position != null) {
                    logReading(position)
                }
            }
        }
    }

    fun connect() = ble.startScan()
    fun disconnect() {
        ble.stopScan()
        ble.disconnect()
        currentTripId = null
    }

    /** Call when the fisherman confirms they're heading out - starts a fresh trip id. */
    fun startNewTrip() {
        currentTripId = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")
            .withZone(ZoneOffset.UTC)
            .format(Instant.now())
    }

    fun getTripIds(): Flow<List<String>> = db.tripLogDao().getTripIds()
    fun getTripEntries(tripId: String) = db.tripLogDao().getTripEntries(tripId)

    private suspend fun logReading(position: BoatPosition) {
        val tripId = currentTripId ?: return // no active trip - don't log stray readings
        db.tripLogDao().insert(
            TripLogEntity(
                tripId = tripId,
                latitude = position.latitude,
                longitude = position.longitude,
                zone = position.zone.name,
                distanceToBoundaryMeters = position.distanceToBoundaryMeters,
                speedKnots = position.speedKnots,
                timestampEpochSec = position.timestampEpochSec
            )
        )
    }
}
