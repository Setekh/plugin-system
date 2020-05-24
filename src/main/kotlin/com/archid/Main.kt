package com.archid

import com.archid.plugins.PluginSystem
import com.archid.plugins.store.entity.MyObjectBox
import com.archid.plugins.store.entity.PluginEntity
import io.objectbox.kotlin.boxFor
import java.io.File

fun main() {
    val boxStore = MyObjectBox
            .builder()
            .name("plugins-db")
            .build()

    val pluginEntityBox = boxStore.boxFor(PluginEntity::class)
    val ps = PluginSystem(File("./test.dir"), pluginEntityBox)
    ps.start()

    ps.unload("Test Plugin")
}
