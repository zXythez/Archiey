package com.shattercore.archiey.files

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.nio.file.Files

@RunWith(AndroidJUnit4::class)
class FileCopyEngineIntegrationTest {

    @Test
    fun copiesFileUsingNativePublisher() {
        val context =
            InstrumentationRegistry.getInstrumentation().targetContext

        val dir = Files.createTempDirectory(
            context.cacheDir.toPath(),
            "archiey-engine-test-"
        )

        try {
            val source = dir.resolve("source.txt")
            val destination = dir.resolve("copy.txt")

            Files.write(source, "ARCHIEY_NATIVE_COPY".toByteArray())

            val result = FileCopyEngine.copy(
                source.toFile(),
                destination.toFile()
            )

            assertTrue(
                "Copy failed: ${result.exceptionOrNull()}",
                result.isSuccess
            )

            assertEquals(
                "ARCHIEY_NATIVE_COPY",
                String(Files.readAllBytes(destination))
            )

            assertEquals(
                "ARCHIEY_NATIVE_COPY",
                String(Files.readAllBytes(source))
            )

            val stagingCount = Files.list(dir).use { stream ->
                stream.filter {
                    it.fileName.toString().startsWith(".archiey-copy-")
                }.count()
            }

            assertEquals(0L, stagingCount)

        } finally {
            dir.toFile().deleteRecursively()
        }
    }
}
