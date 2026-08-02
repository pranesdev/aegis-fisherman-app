package com.aegis.fisherman.ui.map

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aegis.fisherman.AegisServices
import com.aegis.fisherman.data.model.BoatPosition
import com.aegis.fisherman.data.model.RestrictedZone
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File

class MapViewModel : ViewModel() {
    private val boatRepository = AegisServices.boatRepository
    private val offlineDataRepository = AegisServices.offlineDataRepository
    private val bathymetryRepository = AegisServices.bathymetryRepository

    val latestPosition: StateFlow<BoatPosition?> = boatRepository.latestPosition
    val demoMode: StateFlow<Boolean> = boatRepository.demoMode

    val restrictedZones: StateFlow<List<RestrictedZone>> =
        offlineDataRepository.observeRestrictedZones()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val savedSpots: StateFlow<List<com.aegis.fisherman.data.db.SavedSpotEntity>> =
        offlineDataRepository.observeSavedSpots()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _breadcrumbTrail = kotlinx.coroutines.flow.MutableStateFlow<List<BoatPosition>>(emptyList())
    val breadcrumbTrail: StateFlow<List<BoatPosition>> = _breadcrumbTrail

    init {
        viewModelScope.launch {
            latestPosition.collect { position ->
                if (position != null) {
                    val currentTrail = _breadcrumbTrail.value.toMutableList()
                    currentTrail.add(position)
                    if (currentTrail.size > 200) currentTrail.removeAt(0) // limit trail length
                    _breadcrumbTrail.value = currentTrail
                }
            }
        }
    }

    private val _satelliteEnabled = kotlinx.coroutines.flow.MutableStateFlow(false)
    val satelliteEnabled: StateFlow<Boolean> = _satelliteEnabled

    fun toggleSatellite(enabled: Boolean) {
        _satelliteEnabled.value = enabled
    }

    fun toggleDemoMode(enabled: Boolean) {
        boatRepository.toggleDemoMode(enabled)
        if (!enabled) _breadcrumbTrail.value = emptyList()
    }

    fun saveCurrentSpot(name: String) {
        val pos = latestPosition.value ?: return
        viewModelScope.launch {
            offlineDataRepository.saveSpot(name, pos.latitude, pos.longitude, null)
        }
    }

    /** Copies the bundled GEBCO bathymetry MBTiles archive to app storage (first call only). */
    suspend fun ensureBathymetryArchive(): File = bathymetryRepository.ensureInstalled()
}
