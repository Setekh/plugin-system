package com.archid.plugins.models;

public class Manifest {
    private long id = 0; // internal

    private String name;
    private String classPath;
    private int version = 0;
    private boolean enabled = true;

    public Manifest() {}

    public Manifest(long id, String name, String classPath, int version, boolean enabled) {
        this.id = id;
        this.name = name;
        this.classPath = classPath;
        this.version = version;
        this.enabled = enabled;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getClassPath() {
        return classPath;
    }

    public void setClassPath(String classPath) {
        this.classPath = classPath;
    }

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    @Override
    public String toString() {
        return "Manifest{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", classPath='" + classPath + '\'' +
                ", version=" + version +
                ", enabled=" + enabled +
                '}';
    }
}
