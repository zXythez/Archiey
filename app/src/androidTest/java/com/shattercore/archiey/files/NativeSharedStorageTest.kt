package com.shattercore.archiey.files

import android.os.Environment
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.nio.file.Files
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class NativeSharedStorageTest {

    @Test
    fun renameNoReplaceOnSharedStorage() {
        val context =
            InstrumentationRegistry.getInstrumentation().targetContext

        val downloads = requireNotNull(
            context.getExternalFilesDir(null)
        ).toPath()

        val directory = downloads.resolve(
            "archiey-jni-test-" + UUID.randomUUID()
        )

        // Never touch an existing directory.
        Files.createDirectory(directory)

        try {
            val source = directory.resolve("source.txt")
            val destination = directory.resolve("destination.txt")

            Files.write(source, "NEW".toByteArray())
            Files.write(destination, "ORIGINAL".toByteArray())

            val collision = NativeFileOps.renameNoReplaceNative(
                source.toString(),
                destination.toString()
            )

            assertEquals(17, collision)
            assertEquals(
                "ORIGINAL",
                String(Files.readAllBytes(destination), Charsets.UTF_8)
            )
            assertTrue(Files.exists(source))

            Files.delete(destination)

            val published = NativeFileOps.renameNoReplaceNative(
                source.toString(),
                destination.toString()
            )

            assertEquals(0, published)
            assertEquals(
                "NEW",
                String(Files.readAllBytes(destination), Charsets.UTF_8)
            )
            assertFalse(Files.exists(source))

        } finally {
            // Only delete files inside our uniquely created test folder.
            directory.toFile().deleteRecursively()
        }
    }
}
