package com.aegis.fisherman

import android.content.Context
import com.aegis.fisherman.ble.BoatBleManager
import com.aegis.fisherman.data.db.AegisDatabase
import com.aegis.fisherman.data.repository.BathymetryRepository
import com.aegis.fisherman.data.repository.BoatRepository
import com.aegis.fisherman.data.repository.OfflineDataRepository
import com.aegis.fisherman.data.repository.WeatherRepository

/**
 * Deliberately simple manual DI. Swap for Hilt/Koin once the app grows past this scaffold -
 * nothing here depends on a particular DI approach, so the migration is mechanical.
 */
object AegisServices {
    private var applicationContext: Context? = null

    private val database: AegisDatabase by lazy {
        AegisDatabase.get(requireContext())
    }

    val boatBleManager: BoatBleManager by lazy {
        BoatBleManager(requireContext())
    }

    val boatRepository: BoatRepository by lazy {
        BoatRepository(boatBleManager, database)
    }

    val weatherRepository: WeatherRepository by lazy {
        WeatherRepository(database)
    }

    val offlineDataRepository: OfflineDataRepository by lazy {
        OfflineDataRepository(requireContext(), database)
    }

    val bathymetryRepository: BathymetryRepository by lazy {
        BathymetryRepository(requireContext())
    }

    fun init(context: Context) {
        applicationContext = context.applicationContext
    }

    private fun requireContext(): Context =
        applicationContext ?: error("AegisServices.init(context) must be called before use - call it from AegisApplication.onCreate()")
}
