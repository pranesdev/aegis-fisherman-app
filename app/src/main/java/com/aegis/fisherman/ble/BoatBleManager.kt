package com.aegis.fisherman.ble

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.util.Log
import com.aegis.fisherman.data.model.BleConnectionState
import com.aegis.fisherman.data.model.BoatPosition
import com.aegis.fisherman.data.model.ZoneStatus
import com.google.gson.Gson
import com.google.gson.JsonObject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.Instant

/**
 * Owns the BLE link to the boat unit. Call [startScan] once BLUETOOTH_SCAN / BLUETOOTH_CONNECT
 * (and, on API <31, ACCESS_FINE_LOCATION) permissions are granted.
 *
 * Speed fallback: if the ESP32 doesn't populate "speedKn", speed is derived on-device from
 * consecutive fixes using [com.aegis.fisherman.util.GeoUtils.speedKnotsBetween].
 */
class BoatBleManager(private val context: Context) {

    private val gson = Gson()
    private var gatt: BluetoothGatt? = null
    private var lastPosition: BoatPosition? = null

    private val _connectionState = MutableStateFlow(BleConnectionState.DISCONNECTED)
    val connectionState: StateFlow<BleConnectionState> = _connectionState.asStateFlow()

    private val _latestPosition = MutableStateFlow<BoatPosition?>(null)
    val latestPosition: StateFlow<BoatPosition?> = _latestPosition.asStateFlow()

    private val bluetoothAdapter: BluetoothAdapter? by lazy {
        (context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager)?.adapter
    }

    @SuppressLint("MissingPermission")
    fun startScan() {
        val adapter = bluetoothAdapter ?: run {
            _connectionState.value = BleConnectionState.FAILED
            return
        }
        val scanner = adapter.bluetoothLeScanner ?: run {
            _connectionState.value = BleConnectionState.FAILED
            return
        }

        _connectionState.value = BleConnectionState.SCANNING
        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()

        scanner.startScan(null, settings, scanCallback)
    }

    @SuppressLint("MissingPermission")
    fun stopScan() {
        bluetoothAdapter?.bluetoothLeScanner?.stopScan(scanCallback)
    }

    @SuppressLint("MissingPermission")
    fun disconnect() {
        gatt?.disconnect()
        gatt?.close()
        gatt = null
        _connectionState.value = BleConnectionState.DISCONNECTED
    }

    @SuppressLint("MissingPermission")
    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            val device: BluetoothDevice = result.device
            val name = try { device.name } catch (_: SecurityException) { null }
            if (name != null && name.startsWith(BleUuids.DEVICE_NAME_PREFIX)) {
                bluetoothAdapter?.bluetoothLeScanner?.stopScan(this)
                connectTo(device)
            }
        }

        override fun onScanFailed(errorCode: Int) {
            Log.w(TAG, "BLE scan failed: $errorCode")
            _connectionState.value = BleConnectionState.FAILED
        }
    }

    @SuppressLint("MissingPermission")
    private fun connectTo(device: BluetoothDevice) {
        _connectionState.value = BleConnectionState.CONNECTING
        gatt = device.connectGatt(context, false, gattCallback)
    }

    @SuppressLint("MissingPermission")
    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(g: BluetoothGatt, status: Int, newState: Int) {
            when (newState) {
                BluetoothProfile.STATE_CONNECTED -> {
                    _connectionState.value = BleConnectionState.CONNECTED
                    g.discoverServices()
                }
                BluetoothProfile.STATE_DISCONNECTED -> {
                    _connectionState.value = BleConnectionState.DISCONNECTED
                }
            }
        }

        override fun onServicesDiscovered(g: BluetoothGatt, status: Int) {
            val characteristic = g.getService(BleUuids.AEGIS_SERVICE_UUID)
                ?.getCharacteristic(BleUuids.POSITION_CHARACTERISTIC_UUID) ?: return

            g.setCharacteristicNotification(characteristic, true)
            val descriptor = characteristic.getDescriptor(BleUuids.CCCD_UUID) ?: return
            descriptor.value = BluetoothGattDescriptorEnableNotify
            g.writeDescriptor(descriptor)
        }

        override fun onCharacteristicChanged(
            g: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic
        ) {
            val raw = characteristic.value ?: return
            parsePacket(String(raw, Charsets.UTF_8))?.let { position ->
                _latestPosition.value = position
                lastPosition = position
            }
        }
    }

    private fun parsePacket(json: String): BoatPosition? = try {
        val obj: JsonObject = gson.fromJson(json, JsonObject::class.java)
        val lat = obj.get("lat").asDouble
        val lng = obj.get("lng").asDouble
        val ts = obj.get("ts")?.asLong ?: Instant.now().epochSecond

        val reportedSpeed = obj.get("speedKn")?.takeIf { !it.isJsonNull }?.asDouble
        val derivedSpeed = reportedSpeed ?: lastPosition?.let { prev ->
            com.aegis.fisherman.util.GeoUtils.speedKnotsBetween(
                prev.latitude, prev.longitude, prev.timestampEpochSec,
                lat, lng, ts
            )
        }

        BoatPosition(
            latitude = lat,
            longitude = lng,
            zone = ZoneStatus.fromString(obj.get("zone")?.asString),
            distanceToBoundaryMeters = obj.get("distM")?.takeIf { !it.isJsonNull }?.asDouble,
            speedKnots = derivedSpeed,
            timestampEpochSec = ts
        )
    } catch (e: Exception) {
        Log.w(TAG, "Malformed packet from boat unit: $json", e)
        null
    }

    companion object {
        private const val TAG = "BoatBleManager"
        private val BluetoothGattDescriptorEnableNotify =
            android.bluetooth.BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
    }
}
