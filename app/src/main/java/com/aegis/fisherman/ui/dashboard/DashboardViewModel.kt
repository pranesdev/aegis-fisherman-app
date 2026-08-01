package com.aegis.fisherman.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aegis.fisherman.AegisServices
import com.aegis.fisherman.data.model.BleConnectionState
import com.aegis.fisherman.data.model.BoatPosition
import com.aegis.fisherman.data.repository.BoatRepository
import kotlinx.coroutines.flow.StateFlow

class DashboardViewModel : ViewModel() {
    private val repository: BoatRepository = AegisServices.boatRepository

    val connectionState: StateFlow<BleConnectionState> = repository.connectionState
    val latestPosition: StateFlow<BoatPosition?> = repository.latestPosition

    private var tripStarted = false

    /** Call after BLE permissions are granted. */
    fun connectToBoatUnit() {
        if (!tripStarted) {
            repository.startNewTrip()
            tripStarted = true
        }
        repository.connect()
    }

    fun disconnectFromBoatUnit() {
        repository.disconnect()
        tripStarted = false
    }

    override fun onCleared() {
        super.onCleared()
        // Deliberately NOT disconnecting here - the BLE link and trip logging should keep
        // running in the background/foreground service even if the Dashboard screen isn't
        // visible. Wire a foreground Service around BoatRepository before shipping so the
        // connection survives the app being backgrounded during a real trip.
    }
}
