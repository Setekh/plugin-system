package com.archid.plugins.store.entity

import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable

object PluginManifests : IntIdTable("plugins_manifests") {
    var name = varchar("name", 50).uniqueIndex()

    var class_path = varchar("class_path", 255)
    var version = integer("version")
    var enabled = bool("enabled")
}

class PluginManifest(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<PluginManifest>(PluginManifests)

    var name by PluginManifests.name
    var classPath by PluginManifests.class_path
    var version by PluginManifests.version
    var isEnabled by PluginManifests.enabled
}
