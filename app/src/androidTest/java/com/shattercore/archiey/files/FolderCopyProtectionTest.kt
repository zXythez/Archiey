package com.shattercore.archiey.files

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.nio.file.Files

@RunWith(AndroidJUnit4::class)
class FolderCopyProtectionTest {

    @Test
    fun refusesExistingDestinationWithoutChangingIt() {
        withTemporaryRoot { root ->
            val source = Files.createDirectory(root.resolve("source"))
            val destination = Files.createDirectory(
                root.resolve("destination")
            )

            Files.write(
                source.resolve("chicken.txt"),
                "SOURCE".toByteArray()
            )
            Files.write(
                destination.resolve("chicken.txt"),
                "DO NOT OVERWRITE".toByteArray()
            )

            val result = FolderCopyEngine.copy(
                source.toFile(),
                destination.toFile()
            )

            assertTrue(result.isFailure)
            assertEquals(
                "DO NOT OVERWRITE",
                String(Files.readAllBytes(
                    destination.resolve("chicken.txt")
                ))
            )
            assertEquals(
                "SOURCE",
                String(Files.readAllBytes(
                    source.resolve("chicken.txt")
                ))
            )

            assertNoStaging(root)
        }
    }

    @Test
    fun refusesCopyIntoOwnDescendant() {
        withTemporaryRoot { root ->
            val source = Files.createDirectory(root.resolve("source"))
            val nested = Files.createDirectories(
                source.resolve("nested")
            )

            Files.write(
                source.resolve("chicken.txt"),
                "SAFE".toByteArray()
            )

            val destination = nested.resolve("recursive-copy")

            val result = FolderCopyEngine.copy(
                source.toFile(),
                destination.toFile()
            )

            assertTrue(result.isFailure)
            assertFalse(Files.exists(destination))
            assertEquals(
                "SAFE",
                String(Files.readAllBytes(
                    source.resolve("chicken.txt")
                ))
            )

            assertNoStaging(nested)
        }
    }

    private fun withTemporaryRoot(
        action: (java.nio.file.Path) -> Unit
    ) {
        val context = InstrumentationRegistry
            .getInstrumentation().targetContext

        val root = Files.createTempDirectory(
            context.cacheDir.toPath(),
            "archiey-folder-protection-"
        )

        try {
            action(root)
        } finally {
            root.toFile().deleteRecursively()
        }
    }

    private fun assertNoStaging(directory: java.nio.file.Path) {
        Files.list(directory).use { stream ->
            assertEquals(
                0L,
                stream.filter {
                    it.fileName.toString()
                        .startsWith(".archiey-folder-")
                }.count()
            )
        }
    }
}
