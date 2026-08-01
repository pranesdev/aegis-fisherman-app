package com.aegis.fisherman.ui.map

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aegis.fisherman.AegisServices
import com.aegis.fisherman.data.model.BoatPosition
import com.aegis.fisherman.data.model.RestrictedZone
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import java.io.File

class MapViewModel : ViewModel() {
    private val boatRepository = AegisServices.boatRepository
    private val offlineDataRepository = AegisServices.offlineDataRepository
    private val bathymetryRepository = AegisServices.bathymetryRepository

    val latestPosition: StateFlow<BoatPosition?> = boatRepository.latestPosition

    val restrictedZones: StateFlow<List<RestrictedZone>> =
        offlineDataRepository.observeRestrictedZones()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** Copies the bundled GEBCO bathymetry MBTiles archive to app storage (first call only). */
    suspend fun ensureBathymetryArchive(): File = bathymetryRepository.ensureInstalled()
}
