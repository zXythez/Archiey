package com.shattercore.archiey.files

import java.io.IOException
import java.nio.file.Files
import java.nio.file.LinkOption
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import java.util.concurrent.CancellationException

object FolderCopyEngine {

    data class Progress(
        val copiedBytes: Long,
        val totalBytes: Long
    )

    fun copy(
        source: java.io.File,
        destination: java.io.File,
        isCancelled: () -> Boolean = { false },
        onProgress: (Progress) -> Unit = {}
    ): Result<java.io.File> = runCatching {
        val src = source.toPath().toAbsolutePath().normalize()
        val dst = destination.toPath().toAbsolutePath().normalize()

        require(Files.isDirectory(src, LinkOption.NOFOLLOW_LINKS)) {
            "Source must be a directory"
        }

        val parent = dst.parent
            ?: throw IOException("Invalid destination")

        require(Files.isDirectory(parent, LinkOption.NOFOLLOW_LINKS)) {
            "Destination parent does not exist"
        }

        require(!dst.startsWith(src)) {
            "Cannot copy a folder into itself"
        }

        require(!Files.exists(dst, LinkOption.NOFOLLOW_LINKS)) {
            "Destination already exists"
        }

        fun checkCancelled() {
            if (isCancelled()) {
                throw CancellationException("Folder copy cancelled")
            }
        }

        checkCancelled()

        val sourceReal = src.toRealPath()
        val parentReal = parent.toRealPath()

        require(!parentReal.startsWith(sourceReal)) {
            "Cannot copy a folder into its own descendant"
        }

        val entries = mutableListOf<Path>()
        var totalBytes = 0L

        // Snapshot the initial tree structure without following symlinks.
        Files.walk(src).use { stream ->
            val iterator = stream.iterator()
            while (iterator.hasNext()) {
                checkCancelled()
                val entry = iterator.next()

                when {
                    Files.isSymbolicLink(entry) ->
                        throw IOException("Symbolic links are not supported: $entry")

                    Files.isDirectory(entry, LinkOption.NOFOLLOW_LINKS) ->
                        Unit

                    Files.isRegularFile(entry, LinkOption.NOFOLLOW_LINKS) -> {
                        totalBytes = Math.addExact(
                            totalBytes,
                            Files.size(entry)
                        )
                    }

                    else ->
                        throw IOException("Unsupported filesystem entry: $entry")
                }

                entries.add(entry)
            }
        }

        checkCancelled()

        // The staging directory is created beside the final destination.
        val staging = Files.createTempDirectory(
            parent,
            ".archiey-folder-"
        )

        var published = false

        try {
            var copiedBytes = 0L
            onProgress(Progress(0L, totalBytes))

            for (entry in entries) {
                checkCancelled()

                val relative = src.relativize(entry)
                val target = staging.resolve(relative)

                if (relative.toString().isEmpty()) {
                    continue
                }

                if (Files.isSymbolicLink(entry)) {
                    throw IOException("Symbolic link detected during copy")
                }

                if (Files.isDirectory(entry, LinkOption.NOFOLLOW_LINKS)) {
                    Files.createDirectory(target)
                } else if (
                    Files.isRegularFile(entry, LinkOption.NOFOLLOW_LINKS)
                ) {
                    Files.newInputStream(entry).use { input ->
                        Files.newOutputStream(
                            target,
                            StandardOpenOption.CREATE_NEW,
                            StandardOpenOption.WRITE
                        ).use { output ->
                            val buffer = ByteArray(64 * 1024)

                            while (true) {
                                checkCancelled()
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

                    if (Files.size(entry) != Files.size(target)) {
                        throw IOException("Copied file size mismatch")
                    }
                } else {
                    throw IOException("Source entry changed during copy")
                }
            }

            checkCancelled()

            if (copiedBytes != totalBytes) {
                throw IOException("Folder byte count mismatch")
            }

            if (Files.exists(dst, LinkOption.NOFOLLOW_LINKS)) {
                throw IOException("Destination already exists")
            }

            val errno = NativeFileOps.renameNoReplaceNative(
                staging.toString(),
                dst.toString()
            )

            if (errno != 0) {
                throw IOException(
                    "Native folder publish failed (errno=$errno)"
                )
            }

            published = true
            dst.toFile()
        } finally {
            if (!published) {
                // Only delete the uniquely created staging directory.
                // Never delete the published destination.
                staging.toFile().deleteRecursively()
            }
        }
    }
}
