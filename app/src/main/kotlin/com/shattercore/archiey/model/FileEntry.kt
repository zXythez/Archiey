package com.shattercore.archiey.model

import java.io.File

data class FileEntry(
    val file: File,
    val name: String = file.name,
    val isDirectory: Boolean = file.isDirectory,
    val size: Long = file.length(),
    val lastModified: Long = file.lastModified()
)
