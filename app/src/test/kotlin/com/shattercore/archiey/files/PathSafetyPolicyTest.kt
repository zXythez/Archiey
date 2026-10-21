package com.shattercore.archiey.files

import org.junit.Assert.*
import org.junit.Test
import java.nio.file.Paths

class PathSafetyPolicyTest {

    private val root = Paths.get("/storage/emulated/0")

    private fun allowed(
        source: String,
        destination: String,
        advanced: Boolean = false
    ): Boolean {
        return PathSafetyPolicy.canRename(
            root,
            root.resolve(source),
            root.resolve(destination),
            advanced
        )
    }

    @Test
    fun normalRenameAllowed() {
        assertTrue(
            allowed("Download/old.txt", "Download/new.txt")
        )
    }

    @Test
    fun androidRootAlwaysBlocked() {
        assertFalse(allowed("Android", "lol"))
        assertFalse(allowed("Android", "lol", true))
    }

    @Test
    fun managedDirectoriesAlwaysBlocked() {
        for (name in listOf("data", "obb", "media")) {
            assertFalse(allowed("Android/$name", "Android/lol"))
            assertFalse(allowed("Android/$name", "Android/lol", true))
        }
    }

    @Test
    fun contentsBlockedByDefault() {
        assertFalse(
            allowed(
                "Android/data/com.example",
                "Android/data/com.renamed"
            )
        )
    }

    @Test
    fun advancedModeAllowsContents() {
        assertTrue(
            allowed(
                "Android/data/com.example",
                "Android/data/com.renamed",
                true
            )
        )
    }

    @Test
    fun storageRootAlwaysBlocked() {
        assertFalse(
            PathSafetyPolicy.canRename(
                root,
                root,
                root.resolve("renamed"),
                true
            )
        )
    }

    @Test
    fun outsideStorageRejected() {
        assertFalse(
            PathSafetyPolicy.canRename(
                root,
                Paths.get("/data/local/tmp/test"),
                root.resolve("Download/test"),
                true
            )
        )
    }
}
