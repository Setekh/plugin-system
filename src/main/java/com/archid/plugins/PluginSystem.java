package com.archid.plugins;

import com.archid.plugins.models.ActivePlugin;
import com.archid.plugins.models.Manifest;
import com.archid.plugins.store.PluginRepository;
import com.archid.plugins.store.entity.PluginEntity;
import com.archid.utils.Utils;
import com.google.gson.Gson;
import io.objectbox.Box;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

@SuppressWarnings("unchecked")
public class PluginSystem {
    private static final Logger logger = Logger.getLogger(PluginSystem.class.getName());

    private final HashMap<String, ActivePlugin> activePlugins = new HashMap<>();
    private final PluginRepository repository;

    private File pluginDir;
    private MasterClassLoader masterClassLoader;

    public PluginSystem(File pluginDir, Box<PluginEntity> boxStore) {
        this.pluginDir = pluginDir;
        repository = new PluginRepository(boxStore);
    }

    public void start() {
        var files = pluginDir.listFiles(file -> file.getName().endsWith(".jar"));

        var gson = new Gson();
        if (files == null) {
            throw new RuntimeException("No such directory or no rights! " + pluginDir);
        }

        List<URL> urls = Arrays.stream(files).map(Utils::toURL).filter(Objects::nonNull).collect(Collectors.toList());
        masterClassLoader = new MasterClassLoader(urls);

        for (URL jarUrl : urls) {
            try {

                URL resource = masterClassLoader.getResource("manifest.json");
                if (resource == null)
                    throw new RuntimeException("No manifest for plugin!");


                try (InputStreamReader reader = new InputStreamReader(resource.openStream())) {
                    Manifest manifest = gson.fromJson(reader, Manifest.class);

                    var activePlugin = activePlugins.get(manifest.getName());
                    if (activePlugin != null) {
                        logger.warning("Skipping plugin " + manifest.getName() + " due to it being actively present! Process restart required.");
                        continue;
                    }

                    Class<? extends Plugin> pluginClass = (Class<? extends Plugin>) Class.forName(manifest.getClassPath(), true, masterClassLoader);
                    Plugin plugin = pluginClass.getDeclaredConstructor().newInstance();

                    // TODO query repo see if we need to upgrade
                    var oldManifest = repository.findByName(manifest.getName());
                    if (oldManifest != null) {
                        if (oldManifest.getVersion() > manifest.getVersion()) {
                            logger.severe("Skipping plugin " + manifest.getName() + ", version is lower than the installed one!");
                            continue;
                        } else {
                            manifest.setId(oldManifest.getId());
                            plugin.onUpgrade(oldManifest, manifest);
                        }
                    } else {
                        plugin.onInstall(manifest);
                    }

                    repository.store(manifest);

                    plugin.load(manifest);

                    activePlugins.put(manifest.getName(), new ActivePlugin(manifest, plugin));
                }
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Failed loading plugin " + jarUrl.getFile() + "!", e);
            } finally {
                logger.info("Loaded "+ activePlugins.size()+" Plugin(s).");
            }
        }
    }

    public void reload() {
        reload(null);
    }

    public void reload(File newDir) {
        if (newDir != null)
            pluginDir = newDir;

        try {
            masterClassLoader.close();
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Failed closing old master handler", e);
        }

        activePlugins.forEach((k, v) -> {
            Plugin plugin = v.getPlugin();
            Manifest manifest = v.getManifest();

            try {
                plugin.unload(manifest);
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Failed unloading plugin["+ manifest.getName()+"]! Reloading will be impossible for this plugin.", e);
            }
        });

        masterClassLoader = null;

        System.gc();

        start();
    }

    public MasterClassLoader getMasterClassLoader() {
        return masterClassLoader;
    }
}
