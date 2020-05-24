package com.archid.plugins.store;

import com.archid.plugins.models.Manifest;
import com.archid.plugins.store.entity.PluginEntity;
import com.archid.plugins.store.entity.PluginEntity_;
import io.objectbox.Box;

import java.util.Objects;

public class PluginRepository {
    private final Box<PluginEntity> boxStore;

    public PluginRepository(Box<PluginEntity> boxStore) {
        this.boxStore = boxStore;
    }

    public Manifest findByName(String name) {
        PluginEntity entity = boxStore.query().equal(PluginEntity_.name, name).build().findFirst();

        if (entity == null)
            return null;

        return new Manifest(entity.id, entity.name, entity.classPath, entity.version, entity.enabled);
    }

    public void store(Manifest manifest) {
        if (manifest.getId() <= 0) { // crete
            var entity = new PluginEntity(0, manifest.getName(), manifest.getClassPath(), manifest.getVersion(), manifest.isEnabled());
            boxStore.put(entity);
        } else {
            PluginEntity entity = boxStore.query().equal(PluginEntity_.id, manifest.getId()).build().findFirst();

            if (entity == null)
                entity = boxStore.query().equal(PluginEntity_.name, manifest.getName()).build().findFirst();

            PluginEntity pluginEntity = Objects.requireNonNull(entity);
            pluginEntity.version = manifest.getVersion();
            pluginEntity.enabled = manifest.isEnabled();
            boxStore.put(entity);
        }
    }
}
