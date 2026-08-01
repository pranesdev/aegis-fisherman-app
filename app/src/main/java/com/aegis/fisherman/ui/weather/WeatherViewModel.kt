package com.aegis.fisherman.ui.weather

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aegis.fisherman.AegisServices
import com.aegis.fisherman.data.model.WeatherSnapshot
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

/** Change this to match how you key saved fishing areas (e.g. a home port name). */
private const val DEFAULT_LOCATION_LABEL = "Home fishing ground"

class WeatherViewModel : ViewModel() {
    private val weatherRepository = AegisServices.weatherRepository

    val cachedWeather: StateFlow<WeatherSnapshot?> =
        weatherRepository.observeCached(DEFAULT_LOCATION_LABEL)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
}
