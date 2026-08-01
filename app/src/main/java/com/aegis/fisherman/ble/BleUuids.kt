package com.aegis.fisherman.ble

import java.util.UUID

/**
 * BLE contract between the phone and the boat unit's ESP32.
 *
 * IMPORTANT: These UUIDs are placeholders. Generate your own with `uuidgen` and use the
 * SAME values in the ESP32 firmware's BLE server (e.g. via the ESP32 BLE Arduino library's
 * BLEServer / BLECharacteristic setup). They just need to match on both sides.
 *
 * Payload format (sent as UTF-8 JSON bytes on POSITION_CHARACTERISTIC_UUID, via notify):
 * {
 *   "lat": 8.083200,          // decimal degrees, boat's current latitude
 *   "lng": 77.549500,         // decimal degrees, boat's current longitude
 *   "zone": "SAFE",           // "SAFE" | "WARNING" | "DANGER" - mirrors the ESP32's own LED/buzzer state
 *   "distM": 4200.0,          // ESP32's own computed distance to the boundary, in metres
 *   "speedKn": 6.4,           // optional: speed over ground in knots, from the NEO-6M's VTG/RMC sentence.
 *                              // Omit or send null if the firmware doesn't compute this yet - the phone
 *                              // will derive speed itself from consecutive GPS fixes as a fallback.
 *   "ts": 1735689600          // unix epoch seconds, boat unit's own clock
 * }
 *
 * This deliberately mirrors the {latitude, longitude, zone, timestamp} record the boat unit
 * already builds for its SD-card log and LoRa uplink (see AEGIS doc, Section 5.1) - the BLE
 * link to the phone is just a third consumer of the same data the ESP32 is already producing.
 */
object BleUuids {
    val AEGIS_SERVICE_UUID: UUID = UUID.fromString("6e400001-b5a3-f393-e0a9-e50e24dcca9e")
    val POSITION_CHARACTERISTIC_UUID: UUID = UUID.fromString("6e400002-b5a3-f393-e0a9-e50e24dcca9e")

    // Standard Client Characteristic Configuration Descriptor - needed to enable notifications.
    val CCCD_UUID: UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")

    // Advertised local name to filter for during scanning. Set this to whatever
    // BLEDevice::setName(...) or NimBLE equivalent uses on the ESP32.
    const val DEVICE_NAME_PREFIX = "AEGIS-BOAT"
}
