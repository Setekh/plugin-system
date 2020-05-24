package com.archid;

import com.archid.plugins.PluginSystem;
import com.archid.plugins.store.entity.MyObjectBox;
import com.archid.plugins.store.entity.PluginEntity;
import io.objectbox.Box;
import io.objectbox.BoxStore;

import java.io.File;

public class Main {
    public static void main(String[] args) {
        BoxStore boxStore = MyObjectBox.builder().name("core").build();
        boxStore.startObjectBrowser();

        Box<PluginEntity> pluginEntityBox = boxStore.boxFor(PluginEntity.class);

        PluginSystem ps = new PluginSystem(new File("./test.dir"), pluginEntityBox);
        ps.start();
    }
}
