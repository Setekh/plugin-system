package com.archid.plugins.store

import com.archid.plugins.models.Manifest
import com.archid.plugins.store.entity.PluginManifest
import com.archid.plugins.store.entity.PluginManifests
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import javax.sql.DataSource

class PluginRepository(dataSource: DataSource) {

    init {
        val db = Database.connect(dataSource)
        transaction(db) {
            // Create tables
            SchemaUtils.create(PluginManifests)
        }
    }

    fun findByName(name: String): Manifest? {
        val entity = PluginManifest.find { PluginManifests.name eq name }.firstOrNull() ?: return null

        return Manifest(entity.id.value, entity.name, entity.classPath, entity.version).apply { isEnabled = entity.isEnabled }
    }

    fun store(manifest: Manifest) {
        if (manifest.id <= 0) { // create
            PluginManifest.new {
                name = manifest.name
                classPath = manifest.classPath
                version = manifest.version
                isEnabled = manifest.isEnabled
            }
        } else {
            val entity = PluginManifest.find { PluginManifests.name eq manifest.name }.first()

            entity.version = manifest.version
            entity.isEnabled = manifest.isEnabled
            manifest.id = entity.id.value
        }
    }

}