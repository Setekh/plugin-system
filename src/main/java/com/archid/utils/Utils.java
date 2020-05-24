package com.archid.utils;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;

public class Utils {

    public static URL toURL(File file) {
        try {
            return file.toURI().toURL();
        } catch (MalformedURLException e) {
            return null;
        }
    }
}
