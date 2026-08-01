package com.aegis.fisherman.data.model

/** Mirrors the ESP32's own three-zone classification (AEGIS doc, Section 4). */
enum class ZoneStatus {
    SAFE, WARNING, DANGER, UNKNOWN;

    companion object {
        fun fromString(value: String?): ZoneStatus = when (value?.uppercase()) {
            "SAFE" -> SAFE
            "WARNING" -> WARNING
            "DANGER" -> DANGER
            else -> UNKNOWN
        }
    }
}

/** A single live reading received over BLE from the boat unit. */
data class BoatPosition(
    val latitude: Double,
    val longitude: Double,
    val zone: ZoneStatus,
    val distanceToBoundaryMeters: Double?,
    val speedKnots: Double?,
    val timestampEpochSec: Long
)

enum class BleConnectionState {
    DISCONNECTED, SCANNING, CONNECTING, CONNECTED, FAILED
}

/** One row of the on-phone trip log, built from every BLE reading received during a trip. */
data class TripLogEntry(
    val id: Long = 0,
    val tripId: String,
    val latitude: Double,
    val longitude: Double,
    val zone: ZoneStatus,
    val distanceToBoundaryMeters: Double?,
    val speedKnots: Double?,
    val timestampEpochSec: Long
)

/** Cached weather forecast, downloaded on shore before departure. */
data class WeatherSnapshot(
    val fetchedAtEpochSec: Long,
    val validForDate: String,          // e.g. "2026-08-01"
    val locationLabel: String,         // e.g. "Chennai Coast"
    val windSpeedKmh: Double,
    val windDirectionDeg: Int,
    val rainChancePercent: Int,
    val rainfallMm: Double,
    val temperatureC: Double,
    val seaSurfaceTempC: Double?,
    val waveHeightM: Double?,
    val advisory: String?              // e.g. "Small craft advisory" if issued
)

/** Bundled/downloadable species reference so fishermen can identify a legal, in-season catch. */
data class FishSpecies(
    val id: String,
    val commonName: String,
    val localName: String?,            // e.g. Tamil name
    val scientificName: String,
    val typicalDepthRangeM: String,    // e.g. "10-40 m"
    val season: String,                // e.g. "Nov - Feb"
    val notes: String?,
    val isRestrictedOrBanned: Boolean
)

/** A restricted or bordering maritime zone shown on the map and in the guide list. */
data class RestrictedZone(
    val id: String,
    val name: String,
    val type: RestrictedZoneType,
    val description: String,
    val boundaryPolygon: List<Pair<Double, Double>> // lat, lng vertices
)

enum class RestrictedZoneType {
    INTERNATIONAL_BOUNDARY, MARINE_PROTECTED_AREA, FISHING_BAN_SEASONAL, NAVAL_EXERCISE_AREA
}
