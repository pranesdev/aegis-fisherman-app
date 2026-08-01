package com.aegis.fisherman.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aegis.fisherman.AegisServices
import com.aegis.fisherman.data.db.TripLogEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn

class TripHistoryViewModel : ViewModel() {
    private val boatRepository = AegisServices.boatRepository

    val tripIds: StateFlow<List<String>> =
        boatRepository.getTripIds()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedTripId = MutableStateFlow<String?>(null)
    val selectedTripId: StateFlow<String?> = _selectedTripId.asStateFlow()

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val selectedTripEntries: StateFlow<List<TripLogEntity>> =
        _selectedTripId.flatMapLatest { tripId ->
            if (tripId == null) kotlinx.coroutines.flow.flowOf(emptyList())
            else boatRepository.getTripEntries(tripId)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun selectTrip(tripId: String) {
        _selectedTripId.value = tripId
    }
}
