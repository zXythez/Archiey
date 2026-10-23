package com.shattercore.archiey.files

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.nio.file.Files

@RunWith(AndroidJUnit4::class)
class FileCopyRaceTest {

    @Test
    fun destinationAppearingDuringCopyIsNeverOverwritten() {
        val context =
            InstrumentationRegistry.getInstrumentation().targetContext

        val dir = Files.createTempDirectory(
            context.cacheDir.toPath(),
            "archiey-race-"
        )

        try {
            val source = dir.resolve("source.bin")
            val destination = dir.resolve("destination.bin")

            val data = ByteArray(512 * 1024) {
                (it % 251).toByte()
            }

            Files.write(source, data)

            var injected = false

            val result = FileCopyEngine.copy(
                source.toFile(),
                destination.toFile(),
                onProgress = { progress ->
                    if (!injected && progress.copiedBytes > 0L) {
                        Files.write(
                            destination,
                            "OTHER_PROCESS_DATA".toByteArray()
                        )
                        injected = true
                    }
                }
            )

            assertTrue("Collision was not injected", injected)
            assertTrue("Copy should fail", result.isFailure)

            assertEquals(
                "OTHER_PROCESS_DATA",
                String(Files.readAllBytes(destination))
            )

            assertArrayEquals(data, Files.readAllBytes(source))

            val stagingCount = Files.list(dir).use { stream ->
                stream.filter {
                    it.fileName.toString()
                        .startsWith(".archiey-copy-")
                }.count()
            }

            assertEquals(0L, stagingCount)

        } finally {
            dir.toFile().deleteRecursively()
        }
    }
}
