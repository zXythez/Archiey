package com.shattercore.archiey.browser

import com.shattercore.archiey.model.FileEntry

object FileSorter {

    fun sort(
        entries: List<FileEntry>,
        mode: SortMode,
        descending: Boolean
    ): List<FileEntry> {

        val comparator = when (mode) {
            SortMode.NAME -> compareBy<FileEntry> {
                it.name.lowercase()
            }

            SortMode.SIZE -> compareBy<FileEntry> {
                it.size
            }.thenBy {
                it.name.lowercase()
            }

            SortMode.DATE -> compareBy<FileEntry> {
                it.lastModified
            }.thenBy {
                it.name.lowercase()
            }
        }

        val directionComparator =
            if (descending) comparator.reversed()
            else comparator

        return entries.sortedWith(
            compareByDescending<FileEntry> {
                it.isDirectory
            }.then(directionComparator)
        )
    }
}
