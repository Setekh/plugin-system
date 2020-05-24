package com.archid.plugins.models;

import com.archid.plugins.Plugin;

public class ActivePlugin {
    private final Manifest manifest;
    private final Plugin plugin;

    public ActivePlugin(Manifest manifest, Plugin plugin) {
        this.manifest = manifest;
        this.plugin = plugin;
    }

    public Manifest getManifest() {
        return manifest;
    }

    public Plugin getPlugin() {
        return plugin;
    }

    @Override
    public String toString() {
        return "ActivePlugin{" +
                "manifest=" + manifest +
                ", plugin=" + plugin +
                '}';
    }
}
