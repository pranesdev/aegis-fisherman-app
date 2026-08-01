package com.aegis.fisherman.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "trip_log")
data class TripLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val tripId: String,
    val latitude: Double,
    val longitude: Double,
    val zone: String,
    val distanceToBoundaryMeters: Double?,
    val speedKnots: Double?,
    val timestampEpochSec: Long
)

@Entity(tableName = "weather_cache")
data class WeatherCacheEntity(
    @PrimaryKey val locationLabel: String, // one cached forecast per saved fishing area
    val fetchedAtEpochSec: Long,
    val validForDate: String,
    val windSpeedKmh: Double,
    val windDirectionDeg: Int,
    val rainChancePercent: Int,
    val rainfallMm: Double,
    val temperatureC: Double,
    val seaSurfaceTempC: Double?,
    val waveHeightM: Double?,
    val advisory: String?
)

@Entity(tableName = "fish_species")
data class FishSpeciesEntity(
    @PrimaryKey val id: String,
    val commonName: String,
    val localName: String?,
    val scientificName: String,
    val typicalDepthRangeM: String,
    val season: String,
    val notes: String?,
    val isRestrictedOrBanned: Boolean
)

@Entity(tableName = "restricted_zone")
data class RestrictedZoneEntity(
    @PrimaryKey val id: String,
    val name: String,
    val type: String,
    val description: String,
    val polygonJson: String // list of [lat,lng] pairs, serialized - see GeoUtils for (de)serialization
)
