package com.shattercore.archiey.search

import com.shattercore.archiey.model.FileEntry
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import java.io.File
import java.util.ArrayDeque

object DeepSearchEngine {

    suspend fun search(
        directory: File,
        query: String,
        maxResults: Int = 300
    ): List<FileEntry> {

        if (query.isBlank()) return emptyList()

        val results = mutableListOf<FileEntry>()
        val pending = ArrayDeque<File>()
        val visited = mutableSetOf<String>()

        pending.add(directory)

        while (pending.isNotEmpty()) {
            currentCoroutineContext().ensureActive()

            val folder = pending.removeFirst()

            val canonicalPath = try {
                folder.canonicalPath
            } catch (e: Exception) {
                continue
            }

            if (!visited.add(canonicalPath)) continue

            val children = folder.listFiles() ?: continue

            for (child in children) {
                currentCoroutineContext().ensureActive()

                if (child.name.contains(
                        query,
                        ignoreCase = true
                    )
                ) {
                    results.add(FileEntry(child))

                    if (results.size >= maxResults) {
                        return results
                    }
                }

                if (child.isDirectory) {
                    // Avoid following directory symlinks.
                    val isLink = try {
                        child.canonicalPath != child.absolutePath
                    } catch (e: Exception) {
                        true
                    }

                    if (!isLink) {
                        pending.add(child)
                    }
                }
            }
        }

        return results
    }
}
