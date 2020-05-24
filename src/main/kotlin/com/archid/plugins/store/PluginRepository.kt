package com.archid.plugins.store

import com.archid.plugins.models.Manifest
import com.archid.plugins.store.entity.PluginEntity
import com.archid.plugins.store.entity.PluginEntity_
import io.objectbox.Box
import java.util.*

class PluginRepository(private val boxStore: Box<PluginEntity>) {

    fun findByName(name: String): Manifest? {
        val entity = boxStore.query().equal(PluginEntity_.name, name).build().findFirst() ?: return null
        return Manifest(entity.id, entity.name, entity.classPath, entity.version, entity.isEnabled)
    }

    fun store(manifest: Manifest) {
        if (manifest.id <= 0) { // crete
            val entity = PluginEntity(0, manifest.name, manifest.classPath, manifest.version, manifest.isEnabled)
            boxStore.put(entity)
        } else {
            val entity = boxStore.query().equal(PluginEntity_.id, manifest.id).build().findFirst() ?:
                boxStore.query().equal(PluginEntity_.name, manifest.name).build().findFirst()!!

            entity.version = manifest.version
            entity.isEnabled = manifest.isEnabled

            val id = boxStore.put(entity)
            manifest.id = id
        }
    }

}