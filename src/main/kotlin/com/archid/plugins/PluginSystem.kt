package com.archid.plugins

import com.archid.plugins.models.ActivePlugin
import com.archid.plugins.models.Manifest
import com.archid.plugins.store.PluginRepository
import com.archid.plugins.store.entity.PluginEntity
import com.google.gson.Gson
import io.objectbox.Box
import java.io.File
import java.io.IOException
import java.io.InputStreamReader
import java.util.*
import java.util.logging.Level
import java.util.logging.Logger

class PluginSystem(private var pluginDir: File, boxStore: Box<PluginEntity>) {
    private val activePlugins = HashMap<String, ActivePlugin>()
    private val repository: PluginRepository = PluginRepository(boxStore)

    var masterClassLoader: MasterClassLoader = MasterClassLoader.Empty
        private set

    fun start() {
        val files = pluginDir.listFiles { file: File -> file.name.endsWith(".jar") }
                ?: throw RuntimeException("No such directory or no rights! $pluginDir")

        val urls = files.map { it.toURI().toURL() }
        masterClassLoader = MasterClassLoader(urls)

        val gson = Gson()

        for (jarUrl in urls) {
            try {
                val resource = masterClassLoader.getResource("manifest.json") // This won't do if we make the processing parallel
                        ?: throw RuntimeException("No manifest for plugin!")

                InputStreamReader(resource.openStream()).use { reader ->
                    val manifest = gson.fromJson(reader, Manifest::class.java)
                    val activePlugin = activePlugins[manifest.name]

                    if (activePlugin != null) {
                        logger.warning("Skipping plugin " + manifest.name + " due to it being actively present! Process restart required.")
                    }
                    else
                        processPlugin(manifest)
                }
            } catch (e: Exception) {
                logger.log(Level.SEVERE, "Failed loading plugin " + jarUrl.file + "!", e)
            } finally {
                logger.info("Loaded " + activePlugins.size + " Plugin(s).")
            }
        }
    }

    private fun processPlugin(manifest: Manifest) {
        val pluginClass = Class.forName(manifest.classPath, true, masterClassLoader)
        val plugin = pluginClass.getDeclaredConstructor().newInstance() as Plugin

        val oldManifest = repository.findByName(manifest.name)
        if (oldManifest != null) {
            if (oldManifest.version > manifest.version) {
                logger.severe("Skipping plugin " + manifest.name + ", version is lower than the installed one!")
                return
            } else {
                manifest.id = oldManifest.id
                plugin.onUpgrade(oldManifest, manifest)
            }
        } else {
            plugin.onInstall(manifest)
        }

        repository.store(manifest)
        plugin.load(manifest)

        activePlugins[manifest.name] = ActivePlugin(manifest, plugin)
    }

    fun reload(newDir: File? = null) {
        if (newDir != null) pluginDir = newDir
        try {
            masterClassLoader.close()
        } catch (e: IOException) {
            logger.log(Level.SEVERE, "Failed closing old master handler", e)
        }

        activePlugins.forEach { (_, activePlugin) ->
            val plugin = activePlugin.plugin
            val manifest = activePlugin.manifest

            try {
                plugin.unload(manifest)
            } catch (e: Exception) {
                logger.log(Level.SEVERE, "Failed unloading plugin[" + manifest.name + "]! Reloading will be impossible for this plugin.", e)
            }
        }

        masterClassLoader = MasterClassLoader.Empty

        System.gc()
        start()
    }

    fun unload(pluginName: String): Boolean {
        val activePlugin = activePlugins[pluginName] ?: return let {
            logger.warning("Could not find plugin with name $pluginName")
            false
        }

        try {
            val (manifest, plugin) = activePlugin
            plugin.unload(manifest)
        } catch (e: Exception) {
            logger.log(Level.SEVERE, "Failed unloading plugin ${pluginName}!", e)
            return false
        } finally {
            activePlugins.remove(pluginName)
        }

        return true
    }

    companion object {
        private val logger = Logger.getLogger(PluginSystem::class.java.name)
    }

}