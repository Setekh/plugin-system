package com.archid.plugins

import java.net.URL
import java.net.URLClassLoader


open class MasterClassLoader(urls: List<URL>, parent: ClassLoader? = null) : URLClassLoader(urls.toTypedArray<URL>(), parent) {

    fun addUrl(url: URL) = super.addURL(url)

    fun addUrls(urls: List<URL?>) {
        for (u in urls) {
            addURL(u)
        }
    }
}