package com.archid.plugins

import java.net.URL
import java.net.URLClassLoader

open class MasterClassLoader(urls: List<URL>) : URLClassLoader(urls.toTypedArray<URL>()) {
    companion object {
        val Empty = object : MasterClassLoader(emptyList()) {
            override fun addURL(url: URL?) {}
        }
    }

    fun addUrl(url: URL) = super.addURL(url)

    fun addUrls(urls: List<URL?>) {
        for (u in urls) {
            addURL(u)
        }
    }
}