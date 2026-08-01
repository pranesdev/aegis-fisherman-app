package com.aegis.fisherman.data.repository

import com.aegis.fisherman.data.db.AegisDatabase
import com.aegis.fisherman.data.db.WeatherCacheEntity
import com.aegis.fisherman.data.model.WeatherSnapshot
import com.google.gson.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import java.time.Instant
import java.time.LocalDate

/**
 * Uses Open-Meteo (open, no API key required) as the default forecast + marine data source.
 * Swap [fetchAndCache] for whatever provider your team prefers (INCOIS PFZ advisories, IMD, etc.)
 * - the important part is the caching contract, not the specific vendor.
 */
class WeatherRepository(private val db: AegisDatabase) {

    fun observeCached(locationLabel: String): Flow<WeatherSnapshot?> =
        db.weatherCacheDao().getAll().map { list ->
            list.firstOrNull { it.locationLabel == locationLabel }?.toModel()
        }

    suspend fun getCached(locationLabel: String): WeatherSnapshot? =
        db.weatherCacheDao().get(locationLabel)?.toModel()

    /** Call only while on shore Wi-Fi/mobile data, e.g. from the "Before You Sail" sync screen. */
    suspend fun fetchAndCache(locationLabel: String, latitude: Double, longitude: Double): Result<WeatherSnapshot> =
        withContext(Dispatchers.IO) {
            try {
                val url = "https://api.open-meteo.com/v1/forecast" +
                    "?latitude=$latitude&longitude=$longitude" +
                    "&current=temperature_2m,wind_speed_10m,wind_direction_10m,precipitation" +
                    "&daily=precipitation_probability_max,precipitation_sum" +
                    "&timezone=auto"

                val connection = (URL(url).openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    connectTimeout = 10_000
                    readTimeout = 10_000
                }
                val body = connection.inputStream.bufferedReader().use { it.readText() }
                val root = JsonParser.parseString(body).asJsonObject
                val current = root.getAsJsonObject("current")
                val daily = root.getAsJsonObject("daily")

                val snapshot = WeatherSnapshot(
                    fetchedAtEpochSec = Instant.now().epochSecond,
                    validForDate = LocalDate.now().toString(),
                    locationLabel = locationLabel,
                    windSpeedKmh = current.get("wind_speed_10m").asDouble,
                    windDirectionDeg = current.get("wind_direction_10m").asInt,
                    rainChancePercent = daily.getAsJsonArray("precipitation_probability_max")
                        .firstOrNull()?.asInt ?: 0,
                    rainfallMm = daily.getAsJsonArray("precipitation_sum").firstOrNull()?.asDouble ?: 0.0,
                    temperatureC = current.get("temperature_2m").asDouble,
                    // Open-Meteo's separate Marine API covers sea temp / wave height; wire that in
                    // alongside this call if your deployment needs it. Left null in this scaffold.
                    seaSurfaceTempC = null,
                    waveHeightM = null,
                    advisory = null
                )

                db.weatherCacheDao().upsert(snapshot.toEntity())
                Result.success(snapshot)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    private fun WeatherCacheEntity.toModel() = WeatherSnapshot(
        fetchedAtEpochSec, validForDate, locationLabel, windSpeedKmh, windDirectionDeg,
        rainChancePercent, rainfallMm, temperatureC, seaSurfaceTempC, waveHeightM, advisory
    )

    private fun WeatherSnapshot.toEntity() = WeatherCacheEntity(
        locationLabel, fetchedAtEpochSec, validForDate, windSpeedKmh, windDirectionDeg,
        rainChancePercent, rainfallMm, temperatureC, seaSurfaceTempC, waveHeightM, advisory
    )
}
