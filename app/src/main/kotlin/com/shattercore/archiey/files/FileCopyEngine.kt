package com.shattercore.archiey.files

import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.nio.file.LinkOption
import java.nio.file.StandardOpenOption
import java.util.concurrent.CancellationException

object FileCopyEngine {

    data class Progress(
        val copiedBytes: Long,
        val totalBytes: Long
    )

    fun copy(
        source: File,
        destination: File,
        isCancelled: () -> Boolean = { false },
        onProgress: (Progress) -> Unit = {}
    ): Result<File> = runCatching {

        val sourcePath = source.toPath()
        val destinationPath = destination.toPath()

        require(
            Files.isRegularFile(
                sourcePath,
                LinkOption.NOFOLLOW_LINKS
            )
        ) {
            "Source must be a regular file"
        }

        val parent = destinationPath.parent
            ?: error("Invalid destination")

        require(Files.isDirectory(parent)) {
            "Destination folder does not exist"
        }

        if (isCancelled()) {
            throw CancellationException("Copy cancelled")
        }

        val totalBytes = Files.size(sourcePath)
        var copiedBytes = 0L

        var destinationCreated = false

        try {
            Files.newOutputStream(
                destinationPath,
                StandardOpenOption.CREATE_NEW,
                StandardOpenOption.WRITE
            ).use { output ->

                destinationCreated = true

                Files.newInputStream(sourcePath).use { input ->
                    val buffer = ByteArray(64 * 1024)

                    onProgress(Progress(0L, totalBytes))

                    while (true) {
                        if (isCancelled()) {
                            throw CancellationException("Copy cancelled")
                        }

                        val count = input.read(buffer)
                        if (count == -1) break

                        output.write(buffer, 0, count)
                        copiedBytes += count

                        onProgress(
                            Progress(copiedBytes, totalBytes)
                        )
                    }
                }
            }

            if (isCancelled()) {
                throw CancellationException("Copy cancelled")
            }

            if (copiedBytes != totalBytes ||
                Files.size(destinationPath) != totalBytes
            ) {
                throw IOException("Copied file size mismatch")
            }

            destinationCreated = false
            destination

        } finally {
            if (destinationCreated) {
                Files.deleteIfExists(destinationPath)
            }
        }
    }
}
