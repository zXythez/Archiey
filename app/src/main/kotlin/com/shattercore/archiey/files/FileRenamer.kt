package com.shattercore.archiey.files

import java.io.File
import android.content.Context

object FileRenamer {

    fun rename(context: Context, source: File, newName: String): Result<File> =
        runCatching {
            require(newName.isNotBlank()) {
                "Filename cannot be empty"
            }

            require(
                newName != "." &&
                newName != ".." &&
                '/' !in newName &&
                '\\' !in newName &&
                '\u0000' !in newName
            ) {
                "Invalid filename"
            }

            require(newName == newName.trim()) {
                "Remove leading or trailing spaces"
            }

            require(source.exists()) {
                "Original file no longer exists"
            }

            val parent = source.parentFile
                ?: error("Cannot rename this location")

            val destination = File(parent, newName)

            require(ProtectedPaths.canRename(context, source, destination)) {
                "Protected Android directory. Rename blocked."
            }

            if (source.absolutePath == destination.absolutePath) {
                return@runCatching source
            }

            require(!destination.exists()) {
                "A file with this name already exists"
            }

            check(source.renameTo(destination)) {
                "Rename failed. Check storage permissions."
            }

            destination
        }
}
