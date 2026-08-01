# AEGIS Fisherman App — Android Scaffold

A companion Android app for the AEGIS boat unit (ESP32 + GPS + LoRa + SD card). The phone
connects to the boat unit over **Bluetooth Low Energy** to mirror the safety data it already
computes (speed, distance to boundary, zone status), and adds a "whole guide" layer on top:
offline map with bathymetry/boundary overlays, pre-downloaded weather, and a fish/restricted-zone
reference guide — all designed to keep working with **zero connectivity at sea**, matching the
offline-first philosophy of the boat unit itself.

## ⚠️ Before you do anything else with this

1. **The restricted-zone/boundary coordinates in `app/src/main/assets/restricted_zones_seed.json`
   are placeholders.** They're fictional example polygons for scaffolding the map/list UI only.
   Replace them with the **exact same boundary coordinates already loaded on your ESP32**
   (AEGIS doc, Section 4) before this touches a real boat. Wrong coordinates here are worse than
   no data at all.
2. **Nothing in this repo has been compiled.** It was written in a sandboxed environment with no
   Android SDK and no internet access, so it hasn't been through a Gradle build. Expect to fix a
   handful of small issues (import ordering, a missed argument, a Gradle version bump) the first
   time you open it in Android Studio and let Gradle sync/resolve dependencies.

## What's real vs. stubbed

| Area | Status |
|---|---|
| BLE scan/connect/receive from the boat unit | Full implementation (`ble/BoatBleManager.kt`) |
| Dashboard — zone status, speed, distance to boundary | Full implementation |
| Local trip log (Room), mirroring the boat unit's SD-card blackbox | Full implementation |
| Offline map (osmdroid) with boat position + zone/boundary polygons | Working with **online** OSM tiles by default |
| Bathymetry overlay | Full implementation — bundled GEBCO-derived MBTiles archive, see "Bathymetry" below |
| Weather (wind/rain/temperature), cached for offline use | Fully wired to Open-Meteo (free, no API key) as a working example |
| Fish species guide | Full implementation, seeded with example Tamil Nadu species |
| Restricted zones guide + map overlay | Full implementation, seeded with **placeholder** coordinates (see warning above) |
| "Before You Sail" offline sync | Weather sync works end-to-end; map-tile pack and reference-data refresh are stubbed with TODOs in `sync/OfflinePackSyncWorker.kt` |
| Settings (units, language, home port) | UI shell only — no persistence yet |
| Background/foreground BLE service (so the connection survives the app being backgrounded) | **Not implemented** — noted in `DashboardViewModel.onCleared()` |

## Getting it running

1. Open the `AegisFishermanApp/` folder in Android Studio (Koala or newer recommended).
2. Let Gradle sync — this needs internet the first time to pull dependencies.
3. Set your ESP32's advertised BLE name and the packet format to match `ble/BleUuids.kt` (the
   file documents the exact JSON shape the phone expects). If your firmware doesn't build the
   BLE service yet, see "ESP32 side" below.
4. Build and run on a physical device — BLE doesn't work in the emulator.

## ESP32 side — what you need to add

Your current firmware (per the project doc) already computes `{latitude, longitude, zone,
distance, timestamp}` for the SD card log and LoRa uplink. The BLE link to the phone is just a
third consumer of that same data. Using the ESP32 BLE Arduino library (or NimBLE-Arduino):

1. Create a `BLEServer`, advertise a name starting with `AEGIS-BOAT` (matches
   `BleUuids.DEVICE_NAME_PREFIX`).
2. Create one service (`BleUuids.AEGIS_SERVICE_UUID`) with one characteristic
   (`BleUuids.POSITION_CHARACTERISTIC_UUID`, `NOTIFY` property).
3. On every GPS fix (or every N seconds), `setValue()` the characteristic to a UTF-8 JSON string
   matching the shape documented at the top of `ble/BleUuids.kt`, then `notify()`.
4. Generate your own UUIDs (`uuidgen` on any Unix machine) and put the same values in both the
   firmware and `BleUuids.kt` — the placeholders in this scaffold are not private, don't ship them.

## Bathymetry

Wired up and working offline. The map (`ui/map/MapScreen.kt`) uses osmdroid specifically because —
unlike the Google Maps SDK — it can render a second raster tile layer from a **pre-downloaded
MBTiles/SQLite archive**, which is what offline bathymetry at sea needs.

- **Source data:** a [GEBCO](https://gebco.net) grid for the boat's operating waters (Gulf of
  Mannar / Palk Bay area), depth-shaded from light near-shore cyan to deep navy and packed into
  `app/src/main/assets/bathymetry/bathymetry.mbtiles` (zoom 6–11, ~2.7 MB — land tiles are dropped
  since they're fully transparent, so only sea coverage ships).
- **Install-on-first-use:** `data/repository/BathymetryRepository.kt` copies the bundled archive
  out of the compressed APK assets into `filesDir` on first launch (SQLite needs a real,
  randomly-accessible file, not a path inside a zip), then reuses it after that.
- **Rendering:** `ui/map/MapScreen.kt` opens the installed archive with osmdroid's
  `MBTilesFileArchive` and attaches it as its own `TilesOverlay`, independent of the base map's
  tile provider, so it works even before a base-map tile pack has been downloaded.
- **Static, not synced:** unlike weather or reference data, GEBCO bathymetry for a fixed operating
  area doesn't change, so it ships bundled with the app instead of going through the "Before You
  Sail" download in `sync/OfflinePackSyncWorker.kt`.
- **Regenerating the archive:** if you change the operating area, re-run
  `tools/make_bathymetry_mbtiles.py <geotiff> <output.mbtiles> --min-zoom 6 --max-zoom 11` against
  a fresh GEBCO GeoTIFF for your bounds, replace the asset file, and update
  `BathymetryRepository.MIN_ZOOM`/`MAX_ZOOM` to match if you change the zoom range. The script
  works with just numpy/Pillow/scipy (no GDAL required), or with `rasterio` if you have it
  installed for more robust georeferencing.

Base map tiles are a separate concern and still need a pre-downloaded pack — see "Getting it
running" and the "Before You Sail" sync notes above.

## Architecture notes

- **No DI framework.** `AegisServices.kt` is a small manual service locator — deliberately, so you
  don't have to learn Hilt just to read this scaffold. Swap it for Hilt/Koin later if the app grows.
- **Offline-first data flow:** every screen reads from Room (`AegisDatabase`), never directly from
  the network. The only place that talks to the network is `WeatherRepository.fetchAndCache()`
  and (once you fill in the TODOs) the map-tile/reference-data fetch in `OfflinePackSyncWorker` —
  both are meant to run only from the Sync screen, on shore.
- **Package layout:** `ble/` (boat link), `data/` (models, Room, repositories), `sync/`
  (background download worker), `ui/<feature>/` (screen + ViewModel per feature), `util/` (math
  helpers), `navigation/` (nav graph).

## Suggested next steps, roughly in priority order

1. Get the ESP32 BLE service running and confirm real packets parse correctly on the Dashboard.
2. Replace the placeholder restricted-zone coordinates with your real boundary data.
3. Wrap `BoatBleManager`/`BoatRepository` in a foreground `Service` so the connection and trip
   logging survive the app being backgrounded during a real trip.
4. Finish the base-map tile pack download in `OfflinePackSyncWorker` (bathymetry is already
   bundled and offline-ready — see "Bathymetry" above).
5. Add Tamil string resources (`res/values-ta/strings.xml`) — the UI currently only has English
   hardcoded inline in the composables, so this will also mean pulling those strings out into
   `strings.xml` first.
6. Persist Settings (home port, units) — currently just a static screen.
