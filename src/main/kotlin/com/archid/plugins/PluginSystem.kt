package com.archid.plugins

import com.archid.plugins.dependency.PluginDependency
import com.archid.plugins.models.ActivePlugin
import com.archid.plugins.models.Manifest
import com.archid.plugins.store.PluginRepository
import com.archid.plugins.store.entity.PluginEntity
import com.google.gson.Gson
import io.objectbox.Box
import java.io.File
import java.io.FileOutputStream
import java.net.URL
import java.util.*
import java.util.logging.Level
import java.util.logging.Logger
import java.util.zip.ZipInputStream

class PluginSystem(private var pluginDir: File, boxStore: Box<PluginEntity>) {
    private val activePlugins = HashMap<String, ActivePlugin>()
    private val repository: PluginRepository = PluginRepository(boxStore)

    internal val dependencies: PluginDependency = PluginDependency()

    init {
        pluginDir.mkdirs()
    }

    fun start() {
        val files = pluginDir.listFiles { file: File -> file.name.endsWith(".jar") }
                ?: throw RuntimeException("No such directory or no rights! $pluginDir")

        val urls = files.map { it.toURI().toURL() }
        val masterClassLoader = MasterClassLoader(emptyList(), javaClass.classLoader)
        masterClassLoader.addUrls(urls)

        val gson = Gson()
        dependencies.unlock()
        for (jarUrl in urls) {
            try {
                val resource = readManifestInJar(jarUrl)
                        ?: throw RuntimeException("No manifest for plugin!")

                val manifest = gson.fromJson(resource, Manifest::class.java)
                val activePlugin = activePlugins[manifest.name]

                if (activePlugin != null) {
                    logger.warning("Skipping plugin " + manifest.name + " due to it being actively present! Process restart required.")
                } else {
                    val pluginClass = Class.forName(manifest.classPath, true, masterClassLoader)
                    preloadClasses(jarUrl, pluginClass)


                    val plugin = pluginClass.getDeclaredConstructor().newInstance() as Plugin

                    assignWorkDirectory(manifest)

                    val oldManifest = repository.findByName(manifest.name)
                    manifest.id = oldManifest?.id ?: 0

                    if (oldManifest != null) {
                        if (oldManifest.version > manifest.version) {
                            logger.severe("Skipping plugin " + manifest.name + ", version is lower than the installed one!")
                        } else if (manifest.version > oldManifest.version){
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

            } catch (e: Exception) {
                logger.log(Level.SEVERE, "Failed loading plugin " + jarUrl.file + "!", e)
            }
        }

        dependencies.lock()
        masterClassLoader.close()
        System.gc()

        logger.info("Loaded " + activePlugins.size + " Plugin(s).")
    }

    private fun upgradePlugin(manifest: Manifest, oldManifest: Manifest, plugin: Plugin) {
        repository.store(manifest)

        plugin.onUpgrade(oldManifest, manifest)
    }

    private fun loadPlugin(manifest: Manifest, plugin: Plugin) {
        repository.store(manifest)

        if (plugin is AdvancedPlugin) {
            plugin.onInitialize(this, manifest)
        } else {
            plugin.load(manifest)
        }

        activePlugins[manifest.name] = ActivePlugin(manifest, plugin)
    }

    private fun unloadPlugin(activePlugin: ActivePlugin) {
        val (manifest, plugin) = activePlugin
        if (plugin is AdvancedPlugin) {
            plugin.dispose()
        } else {
            plugin.unload(manifest)
        }
    }

    private fun installPlugin(jarUrl: URL, manifest: Manifest, plugin: Plugin) {
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
                    it.write(zip.readBytes())
                    it.flush()
                }
                zipEntry = zip.nextEntry
            }
        }

        repository.store(manifest)
        plugin.onInstall(manifest)
    }

    private fun assignWorkDirectory(manifest: Manifest) {
        manifest.workDir = File(pluginDir, manifest.name.toLowerCase().replace("""\s""".toRegex(), "_"))
        manifest.workDir.mkdir()
    }

    @JvmOverloads
    fun reload(newDir: File? = null) {
        if (newDir != null) pluginDir = newDir

        activePlugins.forEach { (_, activePlugin) ->
            val manifest = activePlugin.manifest

            try {
                unloadPlugin(activePlugin)
            } catch (e: Exception) {
                logger.log(Level.SEVERE, "Failed unloading plugin[" + manifest.name + "]! Reloading will be impossible for this plugin.", e)
            }
        }

        System.gc()

        activePlugins.clear()
        start()
    }

    fun enable(pluginName: String): Boolean {
        val manifest = repository.findByName(pluginName) ?: return let {
            logger.warning("Could not find plugin with name $pluginName")
            false
        }

        if (manifest.isEnabled)
            return true

        manifest.isEnabled = true
        repository.store(manifest)

        reload()
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

    fun unload(pluginName: String): Boolean {
        val activePlugin = activePlugins[pluginName] ?: return let {
            logger.warning("Could not find plugin with name $pluginName")
            false
        }

        try {
            unloadPlugin(activePlugin)
        } catch (e: Exception) {
            logger.log(Level.SEVERE, "Failed unloading plugin ${pluginName}!", e)
            return false
        } finally {
            activePlugins.remove(pluginName)
        }

        return true
    }

    fun shutdown() {
        val activePlugins = activePlugins.values.toList()
        for (activePlugin in activePlugins) {
            try {
                val (manifest) = activePlugin
                unloadPlugin(activePlugin)

                repository.store(manifest)
            } catch (e: Exception) {
                logger.log(Level.SEVERE, "Failed saving plugin[${activePlugin.manifest.name}]!")
            }
        }

        this.activePlugins.clear()
    }

    companion object {
        @JvmStatic
        private val logger = Logger.getLogger(PluginSystem::class.java.name)
    }

}