package com.shattercore.archiey.files

import android.content.Context
import android.os.Environment
import java.io.File

object ProtectedPaths {

    private fun storageRoots(): List<File> {
        val roots = mutableListOf(
            Environment.getExternalStorageDirectory().canonicalFile
        )

        File("/storage").listFiles()?.forEach { volume ->
            if (volume.name != "emulated" &&
                volume.name != "self" &&
                volume.isDirectory
            ) {
                runCatching {
                    roots.add(volume.canonicalFile)
                }
            }
        }

        return roots.distinct()
    }

    fun canRename(
        context: Context,
        source: File,
        destination: File
    ): Boolean = runCatching {

        val from = source.canonicalFile.toPath()
        val to = destination.canonicalFile.toPath()

        val advancedEnabled = context.getSharedPreferences(
            "archiey_settings",
            Context.MODE_PRIVATE
        ).getBoolean("advanced_file_operations", false)

        storageRoots().any { root ->
            PathSafetyPolicy.canRename(
                root = root.toPath(),
                source = from,
                destination = to,
                advancedEnabled = advancedEnabled
            )
        }

    }.getOrDefault(false)

    fun requiresConfirmation(source: File): Boolean =
        runCatching {
            val path = source.canonicalFile.toPath()

            storageRoots().any { root ->
                val android = File(root, "Android")
                    .canonicalFile.toPath()

                path.startsWith(android)
            }
        }.getOrDefault(true)
}
