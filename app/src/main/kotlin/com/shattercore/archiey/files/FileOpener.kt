package com.shattercore.archiey.files

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.webkit.MimeTypeMap
import android.widget.Toast
import androidx.core.content.FileProvider
import java.io.File
import java.util.Locale

object FileOpener {

    fun open(context: Context, file: File) {
        if (!file.isFile || !file.canRead()) {
            Toast.makeText(
                context,
                "Cannot read this file",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        try {
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val extension = file.extension.lowercase(Locale.ROOT)

            val mimeType = when (extension) {
                "apk" -> "application/vnd.android.package-archive"
                "pdf" -> "application/pdf"
                "zip" -> "application/zip"
                "7z" -> "application/x-7z-compressed"
                "rar" -> "application/vnd.rar"
                "iso" -> "application/x-iso9660-image"
                else -> MimeTypeMap.getSingleton()
                    .getMimeTypeFromExtension(extension)
                    ?: "application/octet-stream"
            }

            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, mimeType)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                clipData = android.content.ClipData.newUri(
                    context.contentResolver,
                    file.name,
                    uri
                )
            }

            context.startActivity(
                Intent.createChooser(intent, "Open with")
                    .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            )

        } catch (e: ActivityNotFoundException) {
            Toast.makeText(
                context,
                "No compatible app found",
                Toast.LENGTH_LONG
            ).show()
        } catch (e: Exception) {
            Toast.makeText(
                context,
                "Unable to open file: ${e.message}",
                Toast.LENGTH_LONG
            ).show()
        }
    }
}
