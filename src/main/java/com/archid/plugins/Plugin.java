package com.archid.plugins;

import com.archid.plugins.models.Manifest;

public interface Plugin {
    void load(Manifest manifest);
    void unload(Manifest manifest);

    void onUpgrade(Manifest oldManifest, Manifest newManifest);
    void onInstall(Manifest manifest);
    void onUninstall(Manifest manifest);
}
