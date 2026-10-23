package com.shattercore.archiey.files

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.nio.file.Files
import java.nio.file.LinkOption

@RunWith(AndroidJUnit4::class)
class FolderCopyEngineIntegrationTest {

    @Test
    fun copiesNestedFolderAndPreservesSource() {
        val context = InstrumentationRegistry
            .getInstrumentation().targetContext

        val root = Files.createTempDirectory(
            context.cacheDir.toPath(),
            "archiey-folder-test-"
        )

        try {
            val source = Files.createDirectory(root.resolve("ChickenFarm"))
            val nested = Files.createDirectories(
                source.resolve("animals/birds")
            )
            Files.createDirectories(source.resolve("empty"))

            Files.write(
                source.resolve("readme.txt"),
                "MADERFAKIR FARM".toByteArray()
            )
            Files.write(
                nested.resolve("chicken.txt"),
                "CLUCK CLUCK".toByteArray()
            )

            val destination = root.resolve("ChickenFarmCopy")

            val result = FolderCopyEngine.copy(
                source.toFile(),
                destination.toFile()
            )

            assertTrue(
                "Folder copy failed: ${result.exceptionOrNull()}",
                result.isSuccess
            )

            assertEquals(
                "MADERFAKIR FARM",
                String(Files.readAllBytes(destination.resolve("readme.txt")))
            )
            assertEquals(
                "CLUCK CLUCK",
                String(
                    Files.readAllBytes(
                        destination.resolve("animals/birds/chicken.txt")
                    )
                )
            )

            assertTrue(
                Files.isDirectory(
                    destination.resolve("empty"),
                    LinkOption.NOFOLLOW_LINKS
                )
            )

            assertTrue(Files.exists(source.resolve("readme.txt")))
            assertTrue(Files.exists(nested.resolve("chicken.txt")))

            val stagingCount = Files.list(root).use { stream ->
                stream.filter {
                    it.fileName.toString().startsWith(".archiey-folder-")
                }.count()
            }

            assertEquals(0L, stagingCount)
        } finally {
            root.toFile().deleteRecursively()
        }
    }
}
