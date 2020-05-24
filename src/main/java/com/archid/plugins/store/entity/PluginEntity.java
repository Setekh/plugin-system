package com.archid.plugins.store.entity;

import io.objectbox.annotation.Entity;
import io.objectbox.annotation.Id;
import io.objectbox.annotation.Index;

@Entity
public class PluginEntity {
    @Id
    public long id = 0L;

    @Index
    public String name;
    public String classPath;
    public int version;
    public boolean enabled;


    public PluginEntity() {}

    public PluginEntity(long id, String name, String classPath, int version, boolean enabled) {
        this.id = id;
        this.name = name;
        this.classPath = classPath;
        this.version = version;
        this.enabled = enabled;
    }
}
