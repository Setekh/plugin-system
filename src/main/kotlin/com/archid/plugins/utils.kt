/**
 * Copyright (c) 2013-2019 Corvus Corax Entertainment
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *  - Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 *  - Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 *
 * - Neither the name of Corvus Corax Entertainment nor the names of its
 * contributors may be used to endorse or promote products derived from
 * this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.archid.plugins

import java.net.URL
import java.util.zip.ZipInputStream

/**
 * @author Vlad Ravenholm on 6/1/2020
 */

fun readManifestInJar(url: URL): String? {
    ZipInputStream(url.openStream()).use { zip ->
        var zipEntry = zip.nextEntry
        while (zipEntry != null) {
            if (zipEntry.name.endsWith("manifest.json", true)) {
                zip.reader(Charsets.UTF_8).use {
                    return it.readText()
                }
            }
            zipEntry = zip.nextEntry
        }
    }

    return null
}

fun preloadClasses(url: URL, parentClass: Class<*>) {
    ZipInputStream(url.openStream()).use { zip ->
        var zipEntry = zip.nextEntry
        while (zipEntry != null) {
            if (zipEntry.name.endsWith(".class")) {
                if (zipEntry.name != parentClass.name) {
                    val trimLen = zipEntry.name.length - 6
                    val name = zipEntry.name.substring(0, trimLen).replace('/', '.')
                    Class.forName(name, true, parentClass.classLoader)
                }
            }
            zipEntry = zip.nextEntry
        }
    }
}

fun PluginNotInitialized(): Nothing = throw RuntimeException("Plugin not initialized!")
