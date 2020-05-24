package com.archid.plugins.store.entity

import io.objectbox.annotation.Entity
import io.objectbox.annotation.Id
import io.objectbox.annotation.Index

@Entity
data class PluginEntity(
        @Id var id: Long = 0,
        @Index var name: String,
        var classPath: String,
        var version: Int,
        var isEnabled: Boolean)