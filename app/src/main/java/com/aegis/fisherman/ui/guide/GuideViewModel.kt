package com.aegis.fisherman.ui.guide

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aegis.fisherman.AegisServices
import com.aegis.fisherman.data.model.FishSpecies
import com.aegis.fisherman.data.model.RestrictedZone
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class GuideViewModel : ViewModel() {
    private val offlineDataRepository = AegisServices.offlineDataRepository

    val fishSpecies: StateFlow<List<FishSpecies>> =
        offlineDataRepository.observeFishSpecies()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val restrictedZones: StateFlow<List<RestrictedZone>> =
        offlineDataRepository.observeRestrictedZones()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        viewModelScope.launch { offlineDataRepository.seedIfEmpty() }
    }
}
