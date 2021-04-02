package com.archid.plugins

import com.archid.plugins.dependency.Dependency
import com.archid.plugins.models.Manifest

abstract class AdvancedPlugin : Plugin {
    private var pluginSystem: PluginSystem? = null
    private var manifest: Manifest? = null

    fun onInitialize(pluginSystem: PluginSystem, manifest: Manifest) {
        this.pluginSystem = pluginSystem
        this.manifest = manifest
        load(manifest)
    }

    fun createDependency(name: String): Dependency {
        val pluginSystem = this.pluginSystem ?: PluginNotInitialized()
        val manifest = this.manifest ?: PluginNotInitialized()

        return pluginSystem.dependencies.createDependency(name, manifest.name)
    }

    fun indexOfDependency(name: String): Int {
        val pluginSystem = this.pluginSystem ?: PluginNotInitialized()
        val manifest = this.manifest ?: PluginNotInitialized()
        return pluginSystem.dependencies.indexOfDependency(name)
    }

    fun getDependency(name: String): Dependency? {
        val pluginSystem = this.pluginSystem ?: PluginNotInitialized()
        val manifest = this.manifest ?: PluginNotInitialized()
        return pluginSystem.dependencies.getDependency(name)
    }

    fun dispose() {
        val oldManifest = manifest
        if (oldManifest != null)
            unload(oldManifest)

        pluginSystem = null
        manifest = null
    }
}
