package com.archid.plugins

import com.archid.plugins.models.ActivePlugin
import com.archid.plugins.models.Manifest
import com.archid.plugins.store.PluginRepository
import com.archid.plugins.store.entity.PluginEntity
import com.google.gson.Gson
import io.objectbox.Box
import java.io.*
import java.net.URL
import java.net.URLClassLoader
import java.util.*
import java.util.logging.Level
import java.util.logging.Logger
import java.util.zip.ZipInputStream

class PluginSystem(private var pluginDir: File, boxStore: Box<PluginEntity>) {
    private val activePlugins = HashMap<String, ActivePlugin>()
    private val repository: PluginRepository = PluginRepository(boxStore)

    private var masterClassLoader = MasterClassLoader.Empty

    fun start() {
        val files = pluginDir.listFiles { file: File -> file.name.endsWith(".jar") }
                ?: throw RuntimeException("No such directory or no rights! $pluginDir")

        val urls = files.map { it.toURI().toURL() } // add filter for jars without manifest?
        masterClassLoader = MasterClassLoader(urls)

        val gson = Gson()

        for (jarUrl in urls) {
            val classLoader = URLClassLoader(arrayOf(jarUrl), null)

            try {
                val resource = classLoader.getResource("manifest.json")
                        ?: throw RuntimeException("No manifest for plugin!")

                InputStreamReader(resource.openStream()).use { reader ->
                    val manifest = gson.fromJson(reader, Manifest::class.java)
                    val activePlugin = activePlugins[manifest.name]

                    if (activePlugin != null) {
                        logger.warning("Skipping plugin " + manifest.name + " due to it being actively present! Process restart required.")
                    } else {
                        val pluginClass = Class.forName(manifest.classPath, true, masterClassLoader)
                        val plugin = pluginClass.getDeclaredConstructor().newInstance() as Plugin

                        val oldManifest = repository.findByName(manifest.name)
                        if (oldManifest != null) {
                            if (oldManifest.version > manifest.version) {
                                logger.severe("Skipping plugin " + manifest.name + ", version is lower than the installed one!")
                            } else {
                                upgradePlugin(manifest, oldManifest, plugin)
                            }
                        } else {
                            installPlugin(jarUrl, manifest, plugin)
                        }

                        if (manifest.isEnabled) {
                            loadPlugin(manifest, plugin)
                        } else {
                            logger.warning("Skipping plugin " + manifest.name + " since it's disabled.")
                        }
                    }
                }
            } catch (e: Exception) {
                logger.log(Level.SEVERE, "Failed loading plugin " + jarUrl.file + "!", e)
            } finally {
                classLoader.close()
            }
        }

        logger.info("Loaded " + activePlugins.size + " Plugin(s).")
    }

    private fun upgradePlugin(manifest: Manifest, oldManifest: Manifest, plugin: Plugin) {
        manifest.id = oldManifest.id
        repository.store(manifest)

        plugin.onUpgrade(oldManifest, manifest)
    }

    private fun loadPlugin(manifest: Manifest, plugin: Plugin) {
        repository.store(manifest)

        plugin.load(manifest)

        activePlugins[manifest.name] = ActivePlugin(manifest, plugin)
    }

    private fun installPlugin(jarUrl: URL, manifest: Manifest, plugin: Plugin) {
        manifest.workDir = File(pluginDir, manifest.name.toLowerCase().replace("""\\s""".toRegex(), "_"))
        manifest.workDir.mkdir()

        ZipInputStream(jarUrl.openStream()).use { zip ->
            var zipEntry = zip.nextEntry
            while (zipEntry != null) {
                if (!zipEntry.name.startsWith("assets") ||
                        zipEntry.name == "assets/") {
                    zipEntry = zip.nextEntry
                    continue
                }

                val name = zipEntry.name.substringAfter("assets/")
                val file = File(manifest.workDir, name)
                if (file.isDirectory)
                    file.mkdirs()

                file.createNewFile()

                FileOutputStream(file).use {
                    it.write(zip.readAllBytes())
                    it.flush()
                }
                zipEntry = zip.nextEntry
            }
        }

        repository.store(manifest)
        plugin.onInstall(manifest)
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

    fun enable(pluginName: String): Boolean {
        val manifest = repository.findByName(pluginName) ?: return let {
            logger.warning("Could not find plugin with name $pluginName")
            false
        }

        if (manifest.isEnabled)
            return true

        manifest.isEnabled = false
        repository.store(manifest)

        load(pluginName)


        return true
    }

    fun disable(pluginName: String): Boolean {
        val manifest = repository.findByName(pluginName) ?: return let {
            logger.warning("Could not find plugin with name $pluginName")
            false
        }

        if (!manifest.isEnabled)
            return true

        manifest.isEnabled = false
        repository.store(manifest)

        unload(pluginName)

        return true
    }

    fun load(pluginName: String): Boolean {
        val manifest = repository.findByName(pluginName) ?: return let {
            logger.warning("Could not find plugin with name $pluginName")
            false
        }

        try {
            val pluginClass = Class.forName(manifest.classPath, true, masterClassLoader)
            val plugin = pluginClass.getDeclaredConstructor().newInstance() as Plugin

            loadPlugin(manifest, plugin)
            logger.info("Loaded plugin ${manifest.name}")
        } catch (e: Exception) {
            logger.log(Level.SEVERE, "Failed loading plugin ${pluginName}!", e)
            return false
        }

        return true
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