package com.shattercore.archiey.files

import org.junit.Assert.*
import org.junit.Test
import java.nio.file.Paths

class CopyDestinationPolicyTest {

    private val root = Paths.get("/storage/emulated/0")

    @Test
    fun allowsNormalFolders() {
        assertTrue(
            CopyDestinationPolicy.canCopyTo(
                root,
                root.resolve("Download")
            )
        )
    }

    @Test
    fun blocksStorageRoot() {
        assertFalse(
            CopyDestinationPolicy.canCopyTo(root, root)
        )
    }

    @Test
    fun blocksAndroidDirectories() {
        for (name in listOf(
            "Android",
            "Android/data",
            "Android/obb",
            "Android/media"
        )) {
            assertFalse(
                CopyDestinationPolicy.canCopyTo(
                    root,
                    root.resolve(name)
                )
            )
        }
    }

    @Test
    fun blocksOutsideStorage() {
        assertFalse(
            CopyDestinationPolicy.canCopyTo(
                root,
                Paths.get("/data/local/tmp")
            )
        )
    }

    @Test
    fun blocksTraversalIntoAndroid() {
        assertFalse(
            CopyDestinationPolicy.canCopyTo(
                root,
                root.resolve("Download/../Android/data")
            )
        )
    }
}
