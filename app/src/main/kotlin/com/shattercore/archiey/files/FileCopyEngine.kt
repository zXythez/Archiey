package com.shattercore.archiey.files

import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.nio.file.LinkOption
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import java.util.concurrent.CancellationException

object FileCopyEngine {

    internal fun interface Publisher {
        fun publish(source: Path, destination: Path): Int
    }

    private val nativePublisher = Publisher { source, destination ->
        NativeFileOps.renameNoReplaceNative(
            source.toString(), destination.toString()
        )
    }

    data class Progress(
        val copiedBytes: Long,
        val totalBytes: Long
    )

    fun copy(
        source: File,
        destination: File,
        isCancelled: () -> Boolean = { false },
        onProgress: (Progress) -> Unit = {}
    ): Result<File> = copyWithPublisher(
        source, destination, isCancelled, onProgress, nativePublisher
    )

    internal fun copyWithPublisher(
        source: File,
        destination: File,
        isCancelled: () -> Boolean = { false },
        onProgress: (Progress) -> Unit = {},
        publisher: Publisher
    ): Result<File> = runCatching {

        val src = source.toPath()
        val dst = destination.toPath()

        require(Files.isRegularFile(src, LinkOption.NOFOLLOW_LINKS)) {
            "Source must be a regular file"
        }

        val parent = dst.parent
            ?: throw IOException("Invalid destination")

        require(Files.isDirectory(parent, LinkOption.NOFOLLOW_LINKS)) {
            "Destination folder does not exist"
        }

        require(!Files.exists(dst, LinkOption.NOFOLLOW_LINKS)) {
            "Destination already exists"
        }

        if (isCancelled()) {
            throw CancellationException("Copy cancelled")
        }

        val total = Files.size(src)
        var copied = 0L

        val temp: Path = Files.createTempFile(
            parent,
            ".archiey-copy-",
            ".tmp"
        )

        try {
            Files.newInputStream(src).use { input ->
                Files.newOutputStream(
                    temp,
                    StandardOpenOption.WRITE,
                    StandardOpenOption.TRUNCATE_EXISTING
                ).use { output ->

                    val buffer = ByteArray(64 * 1024)
                    onProgress(Progress(0L, total))

                    while (true) {
                        if (isCancelled()) {
                            throw CancellationException("Copy cancelled")
                        }

                        val count = input.read(buffer)
                        if (count == -1) break

                        output.write(buffer, 0, count)
                        copied += count
                        onProgress(Progress(copied, total))
                    }
                }
            }

            if (isCancelled()) {
                throw CancellationException("Copy cancelled")
            }

            if (copied != total || Files.size(temp) != total) {
                throw IOException("Copied file size mismatch")
            }

            if (Files.exists(dst, LinkOption.NOFOLLOW_LINKS)) {
                throw IOException("Destination already exists")
            }

            // Experimental: filesystem support and collision
            // semantics must be verified before production use.
            val errno = publisher.publish(temp, dst)

            if (errno != 0) {
                throw IOException(
                    "Native no-replace publish failed (errno=$errno)"
                )
            }

            dst.toFile()

        } finally {
            // Only clean up our uniquely created staging path.
            // Never delete the final destination here.
            Files.deleteIfExists(temp)
        }
    }
}
