package com.archid.plugins.dependency

/*
 * Couldn't think of a better name
 */
class PluginDependency {
    private val dependencyList = arrayListOf<Dependency>()
    private var isLocked = true

    fun createDependency(name: String, pluginName: String): Dependency {
        if (isLocked) {
            throw RuntimeException("Tried to create a new dependency when the system is locked!")
        }

        val cachedDependency = dependencyList.find { it.name == name }

        if (cachedDependency != null) {
            return cachedDependency
        }

        val nextId = dependencyList.lastIndex + 1
        val dependency = Dependency(nextId, name, pluginName)
        dependencyList.add(dependency)

        return dependency
    }

    fun indexOfDependency(name: String) = dependencyList.indexOfFirst { it.name == name }
    fun getDependency(name: String) = dependencyList.find { it.name == name }

    fun dependencySize() = dependencyList.size
    fun dependencySizeForPlugin(pluginName: String) = dependencyList.filter { it.pluginName == pluginName }

    internal fun lock() {
        isLocked = true
    }

    internal fun unlock() {
        isLocked = false
    }
}
