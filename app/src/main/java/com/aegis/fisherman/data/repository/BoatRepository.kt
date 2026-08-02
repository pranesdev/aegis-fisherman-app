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
import kotlinx.coroutines.flow.stateIn
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
    private val _demoMode = kotlinx.coroutines.flow.MutableStateFlow(false)
    val demoMode: StateFlow<Boolean> = _demoMode

    private val _simulatedPosition = kotlinx.coroutines.flow.MutableStateFlow<BoatPosition?>(null)

    val connectionState: StateFlow<BleConnectionState> = ble.connectionState
    
    val latestPosition: StateFlow<BoatPosition?> = kotlinx.coroutines.flow.combine(
        ble.latestPosition,
        _simulatedPosition,
        _demoMode
    ) { real, simulated, isDemo ->
        if (isDemo) simulated else real
    }.stateIn(
        CoroutineScope(Dispatchers.Main), 
        kotlinx.coroutines.flow.SharingStarted.Eagerly, 
        null
    )

    private var currentTripId: String? = null
    private val scope = CoroutineScope(Dispatchers.IO)
    private var simulationJob: kotlinx.coroutines.Job? = null

    init {
        scope.launch {
            latestPosition.collect { position ->
                if (position != null) {
                    logReading(position)
                }
            }
        }
    }

    fun toggleDemoMode(enabled: Boolean) {
        _demoMode.value = enabled
        if (enabled) {
            startSimulation()
        } else {
            simulationJob?.cancel()
            _simulatedPosition.value = null
        }
    }

    private fun startSimulation() {
        simulationJob?.cancel()
        simulationJob = scope.launch {
            val route = listOf(
                // Near shore (Safe)
                BoatPosition(9.10, 79.40, com.aegis.fisherman.data.model.ZoneStatus.SAFE, 1200.0, 5.0, Instant.now().epochSecond),
                BoatPosition(9.08, 79.42, com.aegis.fisherman.data.model.ZoneStatus.SAFE, 800.0, 8.5, Instant.now().epochSecond),
                // Approaching boundary (Warning)
                BoatPosition(9.05, 79.45, com.aegis.fisherman.data.model.ZoneStatus.WARNING, 450.0, 12.0, Instant.now().epochSecond),
                BoatPosition(9.02, 79.48, com.aegis.fisherman.data.model.ZoneStatus.WARNING, 150.0, 10.0, Instant.now().epochSecond),
                // Crossed boundary (Danger)
                BoatPosition(8.98, 79.52, com.aegis.fisherman.data.model.ZoneStatus.DANGER, 0.0, 14.5, Instant.now().epochSecond),
                BoatPosition(8.95, 79.55, com.aegis.fisherman.data.model.ZoneStatus.DANGER, 0.0, 15.0, Instant.now().epochSecond)
            )
            
            var index = 0
            while (true) {
                val basePos = route[index % route.size]
                _simulatedPosition.value = basePos.copy(timestampEpochSec = Instant.now().epochSecond)
                kotlinx.coroutines.delay(5000)
                index++
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
