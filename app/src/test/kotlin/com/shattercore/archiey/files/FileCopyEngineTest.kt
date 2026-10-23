package com.shattercore.archiey.files

import org.junit.Assert.*
import org.junit.Test
import java.io.File
import java.nio.file.FileAlreadyExistsException
import java.nio.file.Files

class FileCopyEngineTest {

    // JVM-only publisher. The Android app still uses native JNI.
    // A hard link is created without replacing an existing destination.
    // FileCopyEngine's finally block removes its staging path.
    private val testPublisher = FileCopyEngine.Publisher { staging, destination ->
        try {
            // JVM-only simulation; not an atomic publisher.
            // Production continues using native RENAME_NOREPLACE.
            Files.copy(staging, destination)
            0
        } catch (_: FileAlreadyExistsException) {
            17 // EEXIST
        }
    }

    private fun copy(
        source: File,
        destination: File,
        isCancelled: () -> Boolean = { false },
        onProgress: (FileCopyEngine.Progress) -> Unit = {}
    ): Result<File> =
        FileCopyEngine.copyWithPublisher(
            source = source,
            destination = destination,
            isCancelled = isCancelled,
            onProgress = onProgress,
            publisher = testPublisher
        )


    @Test
    fun copiesFileSuccessfully() {
        val dir = Files.createTempDirectory("archiey-test")

        try {
            val source = dir.resolve("source.txt")
            val destination = dir.resolve("copy.txt")

            Files.write(source, "Hello Archiey!".toByteArray(Charsets.UTF_8))

            val result = copy(
                source.toFile(),
                destination.toFile()
            )

            assertTrue(
    "Copy failed: ${result.exceptionOrNull()}",
    result.isSuccess
)
            assertEquals(
                "Hello Archiey!",
                String(Files.readAllBytes(destination), Charsets.UTF_8)
            )
            assertEquals(
                "Hello Archiey!",
                String(Files.readAllBytes(source), Charsets.UTF_8)
            )
        } finally {
            dir.toFile().deleteRecursively()
        }
    }

    @Test
    fun refusesOverwrite() {
        val dir = Files.createTempDirectory("archiey-test")

        try {
            val source = dir.resolve("source.txt")
            val destination = dir.resolve("existing.txt")

            Files.write(source, "New content".toByteArray(Charsets.UTF_8))
            Files.write(destination, "Original content".toByteArray(Charsets.UTF_8))

            val result = copy(
                source.toFile(),
                destination.toFile()
            )

            assertTrue(result.isFailure)
            assertEquals(
                "Original content",
                String(Files.readAllBytes(destination), Charsets.UTF_8)
            )
        } finally {
            dir.toFile().deleteRecursively()
        }
    }

    @Test
    fun rejectsMissingSource() {
        val dir = Files.createTempDirectory("archiey-test")

        try {
            val result = copy(
                dir.resolve("missing.txt").toFile(),
                dir.resolve("copy.txt").toFile()
            )

            assertTrue(result.isFailure)
        } finally {
            dir.toFile().deleteRecursively()
        }
    }

    @Test
    fun reportsProgress() {
        val dir = Files.createTempDirectory("archiey-progress")
        try {
            val source = dir.resolve("source.bin")
            val destination = dir.resolve("copy.bin")
            val data = ByteArray(200_000) { (it % 256).toByte() }
            Files.write(source, data)

            var lastCopied = -1L

            val result = copy(
                source.toFile(),
                destination.toFile(),
                onProgress = { progress ->
                    lastCopied = progress.copiedBytes
                }
            )

            assertTrue(
    "Copy failed: ${result.exceptionOrNull()}",
    result.isSuccess
)
            assertEquals(data.size.toLong(), lastCopied)
            assertArrayEquals(data, Files.readAllBytes(destination))
        } finally {
            dir.toFile().deleteRecursively()
        }
    }

    @Test
    fun cancellationLeavesNoDestination() {
        val dir = Files.createTempDirectory("archiey-cancel")
        try {
            val source = dir.resolve("source.bin")
            val destination = dir.resolve("copy.bin")
            Files.write(source, ByteArray(200_000))

            var cancelled = false

            val result = copy(
                source.toFile(),
                destination.toFile(),
                isCancelled = { cancelled },
                onProgress = { progress ->
                    if (progress.copiedBytes > 0) {
                        cancelled = true
                    }
                }
            )

            assertTrue(result.isFailure)
            assertFalse(Files.exists(destination))
            assertTrue(Files.exists(source))
        } finally {
            dir.toFile().deleteRecursively()
        }
    }


    @Test
    fun copiesEmptyFile() {
        val dir = Files.createTempDirectory("archiey-empty")
        try {
            val source = dir.resolve("empty.txt")
            val destination = dir.resolve("copy.txt")
            Files.createFile(source)

            val result = copy(
                source.toFile(),
                destination.toFile()
            )

            assertTrue(
    "Copy failed: ${result.exceptionOrNull()}",
    result.isSuccess
)
            assertEquals(0L, Files.size(destination))
        } finally {
            dir.toFile().deleteRecursively()
        }
    }


    @Test
    fun existingDestinationIsNotOverwritten() {
        val dir = Files.createTempDirectory("archiey-collision")
        try {
            val source = dir.resolve("source.txt")
            val destination = dir.resolve("destination.txt")

            Files.write(source, "Source content".toByteArray())
            Files.write(destination, "Existing content".toByteArray())

            val result = copy(
                source.toFile(),
                destination.toFile()
            )

            assertTrue(result.isFailure)
            assertEquals(
                "Existing content",
                String(Files.readAllBytes(destination))
            )
        } finally {
            dir.toFile().deleteRecursively()
        }
    }


    @Test
    fun cancelledCopyPreservesExistingFiles() {
        val dir = Files.createTempDirectory("archiey-stage-test")

        try {
            val source = dir.resolve("source.bin")
            val destination = dir.resolve("copy.bin")

            Files.write(source, ByteArray(200_000) { 42 })

            var cancel = false

            val result = copy(
                source.toFile(),
                destination.toFile(),
                isCancelled = { cancel },
                onProgress = {
                    if (it.copiedBytes > 0) cancel = true
                }
            )

            assertTrue(result.isFailure)
            assertFalse(Files.exists(destination))
            assertEquals(200_000L, Files.size(source))

            val stagingFiles = Files.list(dir).use { stream ->
                stream.filter {
                    it.fileName.toString()
                        .startsWith(".archiey-copy-")
                }.count()
            }

            assertEquals(0L, stagingFiles)

        } finally {
            dir.toFile().deleteRecursively()
        }
    }

}
