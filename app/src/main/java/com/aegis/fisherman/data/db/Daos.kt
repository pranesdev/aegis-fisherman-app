package com.aegis.fisherman.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface TripLogDao {
    @Insert
    suspend fun insert(entry: TripLogEntity)

    @Query("SELECT DISTINCT tripId FROM trip_log ORDER BY timestampEpochSec DESC")
    fun getTripIds(): Flow<List<String>>

    @Query("SELECT * FROM trip_log WHERE tripId = :tripId ORDER BY timestampEpochSec ASC")
    fun getTripEntries(tripId: String): Flow<List<TripLogEntity>>

    @Query("SELECT * FROM trip_log ORDER BY timestampEpochSec DESC LIMIT 1")
    suspend fun getLatestEntry(): TripLogEntity?

    @Query("DELETE FROM trip_log WHERE tripId = :tripId")
    suspend fun deleteTrip(tripId: String)
}

@Dao
interface WeatherCacheDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: WeatherCacheEntity)

    @Query("SELECT * FROM weather_cache WHERE locationLabel = :locationLabel LIMIT 1")
    suspend fun get(locationLabel: String): WeatherCacheEntity?

    @Query("SELECT * FROM weather_cache")
    fun getAll(): Flow<List<WeatherCacheEntity>>
}

@Dao
interface FishSpeciesDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(species: List<FishSpeciesEntity>)

    @Query("SELECT * FROM fish_species ORDER BY commonName ASC")
    fun getAll(): Flow<List<FishSpeciesEntity>>

    @Query("SELECT COUNT(*) FROM fish_species")
    suspend fun count(): Int
}

@Dao
interface RestrictedZoneDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(zones: List<RestrictedZoneEntity>)

    @Query("SELECT * FROM restricted_zone")
    fun getAll(): Flow<List<RestrictedZoneEntity>>

    @Query("SELECT COUNT(*) FROM restricted_zone")
    suspend fun count(): Int
}

@Dao
interface SavedSpotDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(spot: SavedSpotEntity)

    @Query("SELECT * FROM saved_spots ORDER BY timestampEpochSec DESC")
    fun getAll(): Flow<List<SavedSpotEntity>>

    @Query("DELETE FROM saved_spots WHERE id = :id")
    suspend fun delete(id: Long)
}
