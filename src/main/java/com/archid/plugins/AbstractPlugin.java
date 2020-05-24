package com.archid.plugins;

import com.archid.plugins.models.Manifest;

public abstract class AbstractPlugin implements Plugin {
    @Override
    public void load(Manifest manifest) {

    }

    @Override
    public void unload(Manifest manifest) {

    }

    @Override
    public void onUpgrade(Manifest oldManifest, Manifest newManifest) {

    }

    @Override
    public void onInstall(Manifest manifest) {

    }

    @Override
    public void onUninstall(Manifest manifest) {

    }
}
