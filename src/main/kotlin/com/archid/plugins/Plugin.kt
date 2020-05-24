package com.archid.plugins

import com.archid.plugins.models.Manifest

interface Plugin {
    fun load(manifest: Manifest)
    fun unload(manifest: Manifest)
    fun onUpgrade(oldManifest: Manifest, newManifest: Manifest) {}
    fun onInstall(manifest: Manifest) {}
    fun onUninstall(manifest: Manifest) {}
}