package com.aegis.fisherman.util

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

object GeoUtils {
    private const val EARTH_RADIUS_M = 6_371_000.0
    private const val METERS_PER_SEC_TO_KNOTS = 1.9438445

    /** Great-circle distance between two points, in metres. */
    fun distanceMeters(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double {
        val phi1 = Math.toRadians(lat1)
        val phi2 = Math.toRadians(lat2)
        val dPhi = Math.toRadians(lat2 - lat1)
        val dLambda = Math.toRadians(lng2 - lng1)

        val a = sin(dPhi / 2) * sin(dPhi / 2) +
            cos(phi1) * cos(phi2) * sin(dLambda / 2) * sin(dLambda / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return EARTH_RADIUS_M * c
    }

    /**
     * Fallback speed estimate from two consecutive GPS fixes, used only when the ESP32 packet
     * doesn't include "speedKn" directly (see BleUuids.kt payload spec).
     */
    fun speedKnotsBetween(
        lat1: Double, lng1: Double, tsSec1: Long,
        lat2: Double, lng2: Double, tsSec2: Long
    ): Double? {
        val dtSec = tsSec2 - tsSec1
        if (dtSec <= 0) return null
        val distance = distanceMeters(lat1, lng1, lat2, lng2)
        return (distance / dtSec) * METERS_PER_SEC_TO_KNOTS
    }

    /** Serialize a lat/lng polygon (as used by RestrictedZone) to a compact JSON string for Room. */
    fun polygonToJson(points: List<Pair<Double, Double>>): String =
        Gson().toJson(points.map { listOf(it.first, it.second) })

    fun polygonFromJson(json: String): List<Pair<Double, Double>> {
        val type = object : TypeToken<List<List<Double>>>() {}.type
        val raw: List<List<Double>> = try {
            Gson().fromJson(json, type)
        } catch (_: Exception) {
            // Fallback for the broken [{"first":..., "second":...}] format from previous versions
            val legacyType = object : TypeToken<List<Map<String, Double>>>() {}.type
            val legacyRaw: List<Map<String, Double>>? = try {
                Gson().fromJson(json, legacyType)
            } catch (_: Exception) {
                null
            }
            legacyRaw?.map { listOf(it["first"] ?: 0.0, it["second"] ?: 0.0) } ?: emptyList()
        } ?: emptyList()

        return raw.map { it[0] to it[1] }
    }
}
