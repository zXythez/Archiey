package com.shattercore.archiey.files

import org.junit.Assert.*
import org.junit.Test
import java.nio.file.Files

class FileCopyEngineTest {

    @Test
    fun copiesFileSuccessfully() {
        val dir = Files.createTempDirectory("archiey-test")

        try {
            val source = dir.resolve("source.txt")
            val destination = dir.resolve("copy.txt")

            Files.write(source, "Hello Archiey!".toByteArray(Charsets.UTF_8))

            val result = FileCopyEngine.copy(
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

            val result = FileCopyEngine.copy(
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
            val result = FileCopyEngine.copy(
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

            val result = FileCopyEngine.copy(
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

            val result = FileCopyEngine.copy(
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

            val result = FileCopyEngine.copy(
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

            val result = FileCopyEngine.copy(
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

}
