package com.shattercore.archiey.files

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.nio.file.Files
import java.nio.file.Path

@RunWith(AndroidJUnit4::class)
class FileCopyEngineSafetyTest {

    private fun testDirectory(): Path {
        val context =
            InstrumentationRegistry.getInstrumentation().targetContext

        return Files.createTempDirectory(
            context.cacheDir.toPath(),
            "archiey-safety-"
        )
    }

    private fun stagingCount(dir: Path): Long =
        Files.list(dir).use { stream ->
            stream.filter {
                it.fileName.toString().startsWith(".archiey-copy-")
            }.count()
        }

    @Test
    fun existingDestinationIsPreserved() {
        val dir = testDirectory()

        try {
            val source = dir.resolve("source.txt")
            val destination = dir.resolve("destination.txt")

            Files.write(source, "NEW_CONTENT".toByteArray())
            Files.write(destination, "DO_NOT_TOUCH".toByteArray())

            val result = FileCopyEngine.copy(
                source.toFile(),
                destination.toFile()
            )

            assertTrue(result.isFailure)
            assertEquals(
                "DO_NOT_TOUCH",
                String(Files.readAllBytes(destination))
            )
            assertEquals(
                "NEW_CONTENT",
                String(Files.readAllBytes(source))
            )
            assertEquals(0L, stagingCount(dir))

        } finally {
            dir.toFile().deleteRecursively()
        }
    }

    @Test
    fun cancellationPreservesSourceAndCleansStaging() {
        val dir = testDirectory()

        try {
            val source = dir.resolve("source.bin")
            val destination = dir.resolve("destination.bin")

            Files.write(source, ByteArray(512 * 1024) { 42 })

            var cancelled = false

            val result = FileCopyEngine.copy(
                source.toFile(),
                destination.toFile(),
                isCancelled = { cancelled },
                onProgress = {
                    if (it.copiedBytes > 0) {
                        cancelled = true
                    }
                }
            )

            assertTrue(result.isFailure)
            assertFalse(Files.exists(destination))
            assertTrue(Files.exists(source))
            assertEquals(512L * 1024L, Files.size(source))
            assertEquals(0L, stagingCount(dir))

        } finally {
            dir.toFile().deleteRecursively()
        }
    }

    @Test
    fun cancellationBeforeCopyCreatesNothing() {
        val dir = testDirectory()

        try {
            val source = dir.resolve("source.txt")
            val destination = dir.resolve("destination.txt")

            Files.write(source, "ORIGINAL".toByteArray())

            val result = FileCopyEngine.copy(
                source.toFile(),
                destination.toFile(),
                isCancelled = { true }
            )

            assertTrue(result.isFailure)
            assertFalse(Files.exists(destination))
            assertEquals(
                "ORIGINAL",
                String(Files.readAllBytes(source))
            )
            assertEquals(0L, stagingCount(dir))

        } finally {
            dir.toFile().deleteRecursively()
        }
    }
}
