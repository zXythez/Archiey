package com.shattercore.archiey.files

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.nio.file.Files
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
class NativeConcurrentPublishTest {

    @Test
    fun onlyOnePublisherCanWin() {
        val context =
            InstrumentationRegistry.getInstrumentation().targetContext

        val dir = Files.createTempDirectory(
            context.cacheDir.toPath(),
            "archiey-concurrent-"
        )

        val executor = Executors.newFixedThreadPool(2)

        try {
            repeat(100) { round ->
                val first = dir.resolve("first-$round.tmp")
                val second = dir.resolve("second-$round.tmp")
                val destination = dir.resolve("result-$round.bin")

                Files.write(first, "PUBLISHER_A".toByteArray())
                Files.write(second, "PUBLISHER_B".toByteArray())

                val ready = CountDownLatch(2)
                val start = CountDownLatch(1)

                val futureA = executor.submit<Int> {
                    ready.countDown()
                    check(start.await(5, TimeUnit.SECONDS))
                    NativeFileOps.renameNoReplaceNative(
                        first.toString(),
                        destination.toString()
                    )
                }

                val futureB = executor.submit<Int> {
                    ready.countDown()
                    check(start.await(5, TimeUnit.SECONDS))
                    NativeFileOps.renameNoReplaceNative(
                        second.toString(),
                        destination.toString()
                    )
                }

                assertTrue(
                    "Threads did not become ready",
                    ready.await(5, TimeUnit.SECONDS)
                )

                start.countDown()

                val resultA = futureA.get(5, TimeUnit.SECONDS)
                val resultB = futureB.get(5, TimeUnit.SECONDS)

                assertTrue(
                    "Round $round: expected one success and one EEXIST",
                    (resultA == 0 && resultB == 17) ||
                    (resultA == 17 && resultB == 0)
                )

                val content =
                    String(Files.readAllBytes(destination))

                val expected =
                    if (resultA == 0) "PUBLISHER_A"
                    else "PUBLISHER_B"

                assertEquals(expected, content)

                val loser =
                    if (resultA == 17) first else second

                assertTrue(
                    "Losing source must remain intact",
                    Files.exists(loser)
                )
            }
        } finally {
            executor.shutdownNow()
            dir.toFile().deleteRecursively()
        }
    }
}
