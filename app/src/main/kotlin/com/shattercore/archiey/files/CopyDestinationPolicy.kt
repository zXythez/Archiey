package com.shattercore.archiey.files

import java.nio.file.Path

object CopyDestinationPolicy {

    fun canCopyTo(
        root: Path,
        destinationDirectory: Path
    ): Boolean {
        val storage = root.toAbsolutePath().normalize()
        val destination = destinationDirectory
            .toAbsolutePath().normalize()

        if (!destination.startsWith(storage)) return false
        if (destination == storage) return false

        val android = storage.resolve("Android")

        if (destination.startsWith(android)) return false

        return true
    }
}
