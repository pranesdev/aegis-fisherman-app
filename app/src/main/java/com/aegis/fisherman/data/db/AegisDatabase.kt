package com.aegis.fisherman.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        TripLogEntity::class,
        WeatherCacheEntity::class,
        FishSpeciesEntity::class,
        RestrictedZoneEntity::class,
        SavedSpotEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class AegisDatabase : RoomDatabase() {
    abstract fun tripLogDao(): TripLogDao
    abstract fun weatherCacheDao(): WeatherCacheDao
    abstract fun fishSpeciesDao(): FishSpeciesDao
    abstract fun restrictedZoneDao(): RestrictedZoneDao
    abstract fun savedSpotDao(): SavedSpotDao

    companion object {
        @Volatile private var instance: AegisDatabase? = null

        fun get(context: Context): AegisDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AegisDatabase::class.java,
                    "aegis.db"
                )
                .fallbackToDestructiveMigration() // scaffold only: wipe on schema change
                .build().also { instance = it }
            }
    }
}
