package com.aegis.fisherman.data.repository

import android.content.Context
import com.aegis.fisherman.data.db.AegisDatabase
import com.aegis.fisherman.data.db.FishSpeciesEntity
import com.aegis.fisherman.data.db.RestrictedZoneEntity
import com.aegis.fisherman.data.model.FishSpecies
import com.aegis.fisherman.data.model.RestrictedZone
import com.aegis.fisherman.data.model.RestrictedZoneType
import com.aegis.fisherman.util.GeoUtils
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

/**
 * Fish species and restricted-zone reference data. Ships with a small bundled seed (assets/)
 * so the guide isn't empty on first install, and can be refreshed from a backend during the
 * "Before You Sail" sync once your team has a real data source (fisheries department feed, etc).
 */
class OfflineDataRepository(
    private val context: Context,
    private val db: AegisDatabase
) {
    private val gson = Gson()

    fun observeFishSpecies(): Flow<List<FishSpecies>> =
        db.fishSpeciesDao().getAll().map { list -> list.map { it.toModel() } }

    fun observeRestrictedZones(): Flow<List<RestrictedZone>> =
        db.restrictedZoneDao().getAll().map { list -> list.map { it.toModel() } }

    /** Seeds the local DB from bundled assets on first run. Safe to call every launch. */
    suspend fun seedIfEmpty() = withContext(Dispatchers.IO) {
        if (db.fishSpeciesDao().count() == 0) {
            val json = context.assets.open("fish_species_seed.json").bufferedReader().use { it.readText() }
            val type = object : TypeToken<List<FishSpeciesSeed>>() {}.type
            val seed: List<FishSpeciesSeed> = gson.fromJson(json, type)
            db.fishSpeciesDao().upsertAll(seed.map { it.toEntity() })
        }
        if (db.restrictedZoneDao().count() == 0) {
            val json = context.assets.open("restricted_zones_seed.json").bufferedReader().use { it.readText() }
            val type = object : TypeToken<List<RestrictedZoneSeed>>() {}.type
            val seed: List<RestrictedZoneSeed> = gson.fromJson(json, type)
            db.restrictedZoneDao().upsertAll(seed.map { it.toEntity() })
        }
    }

    // --- seed JSON shapes (kept separate from the DB entities so the bundled file format can
    // evolve independently of the schema) ---

    private data class FishSpeciesSeed(
        val id: String,
        val commonName: String,
        val localName: String?,
        val scientificName: String,
        val typicalDepthRangeM: String,
        val season: String,
        val notes: String?,
        val isRestrictedOrBanned: Boolean
    ) {
        fun toEntity() = FishSpeciesEntity(
            id, commonName, localName, scientificName, typicalDepthRangeM, season, notes, isRestrictedOrBanned
        )
    }

    private data class RestrictedZoneSeed(
        val id: String,
        val name: String,
        val type: String,
        val description: String,
        val boundaryPolygon: List<List<Double>>
    ) {
        fun toEntity() = RestrictedZoneEntity(
            id, name, type, description, GeoUtils.polygonToJson(boundaryPolygon.map { it[0] to it[1] })
        )
    }

    private fun FishSpeciesEntity.toModel() = FishSpecies(
        id, commonName, localName, scientificName, typicalDepthRangeM, season, notes, isRestrictedOrBanned
    )

    private fun RestrictedZoneEntity.toModel() = RestrictedZone(
        id, name,
        RestrictedZoneType.valueOf(type),
        description,
        GeoUtils.polygonFromJson(polygonJson)
    )
}
