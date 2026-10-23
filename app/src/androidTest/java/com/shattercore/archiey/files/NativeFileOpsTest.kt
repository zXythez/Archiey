package com.shattercore.archiey.files

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.nio.file.Files

@RunWith(AndroidJUnit4::class)
class NativeFileOpsTest {

    @Test
    fun renameNoReplaceWorksInsideAndroid() {
        val context =
            InstrumentationRegistry.getInstrumentation().targetContext

        val directory = Files.createTempDirectory(
            context.cacheDir.toPath(),
            "archiey-jni-test-"
        )

        try {
            val source = directory.resolve("source.txt")
            val destination = directory.resolve("destination.txt")

            Files.write(source, "NEW".toByteArray())
            Files.write(destination, "ORIGINAL".toByteArray())

            val collision = NativeFileOps.renameNoReplaceNative(
                source.toString(),
                destination.toString()
            )

            assertEquals(17, collision) // EEXIST
            assertEquals("ORIGINAL", String(Files.readAllBytes(destination), Charsets.UTF_8))
            assertTrue(Files.exists(source))

            Files.delete(destination)

            val published = NativeFileOps.renameNoReplaceNative(
                source.toString(),
                destination.toString()
            )

            assertEquals(0, published)
            assertEquals("NEW", String(Files.readAllBytes(destination), Charsets.UTF_8))
            assertFalse(Files.exists(source))

        } finally {
            directory.toFile().deleteRecursively()
        }
    }
}
