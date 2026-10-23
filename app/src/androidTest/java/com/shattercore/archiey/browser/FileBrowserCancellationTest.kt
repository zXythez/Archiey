package com.shattercore.archiey.browser

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.shattercore.archiey.model.FileEntry
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.delay
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

@RunWith(AndroidJUnit4::class)
class FileBrowserCancellationTest {

    @Test
    fun cancelActiveCopyResetsState() = runBlocking {
        val context = InstrumentationRegistry
            .getInstrumentation().targetContext

        val root = File(
            context.cacheDir,
            "archiey-cancel-test-" + System.nanoTime()
        )
        assertTrue(root.mkdirs())

        val source = File(root, "source.txt")
        source.writeText("CHICKEN MADERFAKIR")

        val started = CountDownLatch(1)
        val release = CountDownLatch(1)
        val observedCancellation = AtomicBoolean(false)

        val vm = FileBrowserViewModel(
            storageRoot = root,
            copyOperation = { _, _, isCancelled ->
                started.countDown()
                try {
                    check(release.await(10, TimeUnit.SECONDS)) {
                        "Test never released the fake copy"
                    }
                    observedCancellation.set(isCancelled())
                    Result.failure(
                        java.util.concurrent.CancellationException(
                            "Copy cancelled"
                        )
                    )
                } catch (e: InterruptedException) {
                    Result.failure(e)
                }
            }
        )

        try {
            vm.selectForCopy(FileEntry(source))
            assertNotNull(vm.copySource.value)

            var resultMessage: String? = null
            vm.pasteCopiedFile { resultMessage = it }

            assertTrue(
                "Copy did not start",
                started.await(5, TimeUnit.SECONDS)
            )

            vm.clearCopySelection()

            assertTrue(vm.pasteCancelling.value)
            assertTrue(vm.pasteBusy.value)

            release.countDown()

            var attempts = 0
            while (vm.pasteBusy.value && attempts < 100) {
                delay(20)
                attempts++
            }

            assertFalse("Copy remained busy", vm.pasteBusy.value)
            assertFalse(vm.pasteCancelling.value)
            assertTrue(observedCancellation.get())
            assertNotNull(resultMessage)
            assertNotNull(vm.copySource.value)
            assertEquals("CHICKEN MADERFAKIR", source.readText())
        } finally {
            release.countDown()
            root.deleteRecursively()
        }
    }
}
