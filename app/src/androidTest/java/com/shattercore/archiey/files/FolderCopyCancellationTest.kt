package com.shattercore.archiey.files

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.nio.file.Files

@RunWith(AndroidJUnit4::class)
class FolderCopyCancellationTest {

    @Test
    fun cancellationDoesNotPublishIncompleteFolder() {
        val context = InstrumentationRegistry
            .getInstrumentation().targetContext

        val root = Files.createTempDirectory(
            context.cacheDir.toPath(),
            "archiey-folder-cancel-"
        )

        try {
            val source = Files.createDirectory(root.resolve("source"))
            val destination = root.resolve("destination")

            val data = ByteArray(1024 * 1024) { 42 }

            repeat(5) { index ->
                Files.write(source.resolve("file-$index.bin"), data)
            }

            var cancelRequested = false

            val result = FolderCopyEngine.copy(
                source.toFile(),
                destination.toFile(),
                isCancelled = { cancelRequested },
                onProgress = {
                    if (it.copiedBytes > 0L) {
                        cancelRequested = true
                    }
                }
            )

            assertTrue("Copy should fail on cancellation", result.isFailure)
            assertFalse("Destination must not exist", Files.exists(destination))

            repeat(5) { index ->
                assertTrue(
                    "Source file must remain",
                    Files.exists(source.resolve("file-$index.bin"))
                )
            }

            val stagingCount = Files.list(root).use { stream ->
                stream.filter {
                    it.fileName.toString().startsWith(".archiey-folder-")
                }.count()
            }

            assertEquals(
                "Staging directory must be cleaned up",
                0L,
                stagingCount
            )
        } finally {
            root.toFile().deleteRecursively()
        }
    }
}
