package com.archid.plugins.models

import com.google.gson.annotations.SerializedName
import java.io.File

data class Manifest(
    var id: Long = 0,
    var name: String = "",
    var classPath: String = "",

    @SerializedName(value = "versionCode", alternate = ["version"])
    var version: Int = 0,

    var isEnabled: Boolean = true
) {

    @Transient
    lateinit var workDir: File
        internal set
}