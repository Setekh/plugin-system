package com.archid.plugins;

import java.net.URL;
import java.net.URLClassLoader;
import java.util.List;

public class MasterClassLoader extends URLClassLoader {
    public MasterClassLoader() {
        super(new URL[0]);
    }

    public MasterClassLoader(List<URL> urls) {
        super(urls.toArray(new URL[0]));
    }

    void addUrls(List<URL> urls) {
        for (URL u : urls) {
            addURL(u);
        }

    }
}
