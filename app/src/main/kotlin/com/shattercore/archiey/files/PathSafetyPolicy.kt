package com.shattercore.archiey.files

import java.nio.file.Path

object PathSafetyPolicy {

    private val criticalNames = setOf(
        "Android",
        "data",
        "obb",
        "media"
    )

    fun canRename(
        root: Path,
        source: Path,
        destination: Path,
        advancedEnabled: Boolean
    ): Boolean {
        val storageRoot = root.toAbsolutePath().normalize()
        val from = source.toAbsolutePath().normalize()
        val to = destination.toAbsolutePath().normalize()

        if (!from.startsWith(storageRoot) ||
            !to.startsWith(storageRoot)) {
            return false
        }

        val android = storageRoot.resolve("Android")

        val permanent = listOf(
            storageRoot,
            android,
            android.resolve("data"),
            android.resolve("obb"),
            android.resolve("media")
        )

        if (from in permanent || to in permanent) {
            return false
        }

        if (!advancedEnabled &&
            (from.startsWith(android) || to.startsWith(android))) {
            return false
        }

        return true
    }
}
