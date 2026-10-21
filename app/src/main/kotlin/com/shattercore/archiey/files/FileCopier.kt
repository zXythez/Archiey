package com.shattercore.archiey.files

import java.io.File

object FileCopier {

    fun copy(source: File, destinationDirectory: File): Result<File> =
        runCatching {
            require(source.isFile) {
                "Source must be a file"
            }

            require(destinationDirectory.isDirectory) {
                "Destination must be a folder"
            }

            val destination = File(destinationDirectory, source.name)

            require(!destination.exists()) {
                "A file with this name already exists"
            }

            source.inputStream().use { input ->
                destination.outputStream().use { output ->
                    input.copyTo(output, bufferSize = 64 * 1024)
                }
            }

            destination
        }
}
