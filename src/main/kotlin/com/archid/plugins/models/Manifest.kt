package com.archid.plugins.models

import com.google.gson.annotations.SerializedName

data class Manifest(
    var id: Long = 0,
    var name: String,
    var classPath: String,

    @SerializedName(value = "versionCode", alternate = ["version"])
    var version: Int = 0,

    @SerializedName(value = "enabled", alternate = ["isEnabled"])
    var isEnabled: Boolean = true
)