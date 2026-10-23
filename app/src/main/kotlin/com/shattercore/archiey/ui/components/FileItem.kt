package com.shattercore.archiey.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shattercore.archiey.model.FileEntry
import java.text.DateFormat
import java.util.Date
import java.util.Locale

@Composable
fun FileItem(
    entry: FileEntry,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onRename: (FileEntry) -> Unit = {},
    onCopy: (FileEntry) -> Unit = {},
    showParentPath: Boolean = false
) {
    var menuExpanded by remember { mutableStateOf(false) }
    var showDetails by remember { mutableStateOf(false) }

    val icon = when {
        entry.isDirectory -> Icons.Outlined.Folder
        entry.name.endsWith(".zip", true) -> Icons.Outlined.Inventory2
        entry.name.endsWith(".7z", true) -> Icons.Outlined.Inventory2
        entry.name.endsWith(".rar", true) -> Icons.Outlined.Inventory2
        entry.name.endsWith(".apk", true) -> Icons.Outlined.Android
        entry.name.endsWith(".jpg", true) -> Icons.Outlined.Image
        entry.name.endsWith(".png", true) -> Icons.Outlined.Image
        entry.name.endsWith(".mp4", true) -> Icons.Outlined.Movie
        entry.name.endsWith(".mp3", true) -> Icons.Outlined.MusicNote
        else -> Icons.Outlined.InsertDriveFile
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant,
            shape = RoundedCornerShape(12.dp)
        ) {
            Box(
                modifier = Modifier.size(46.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = entry.name,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = if (entry.isDirectory) {
                    "Folder"
                } else {
                    "${formatFileSize(entry.size)} • ${formatDate(entry.lastModified)}"
                },
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
            if (showParentPath) {
                Text(
                    text = entry.file.parentFile?.absolutePath
    ?.removePrefix("/storage/emulated/0/")
    ?: "Unknown location",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

        }

        Box {
            IconButton(onClick = { menuExpanded = true }) {
                Icon(
                    Icons.Outlined.MoreVert,
                    contentDescription = "More options",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            DropdownMenu(
                expanded = menuExpanded,
                onDismissRequest = { menuExpanded = false }
            ) {
                DropdownMenuItem(
                        text = { Text("Copy") },
                        leadingIcon = {
                            Icon(
                                Icons.Outlined.ContentCopy,
                                contentDescription = null
                            )
                        },
                        onClick = {
                            menuExpanded = false
                            onCopy(entry)
                        }
                    )

                DropdownMenuItem(
                    text = { Text("Rename") },
                    onClick = {
                        menuExpanded = false
                        onRename(entry)
                    }
                )

                DropdownMenuItem(
                    text = { Text("Details") },
                    onClick = {
                        menuExpanded = false
                        showDetails = true
                    }
                )
            }
        }
    }

    if (showDetails) {
        AlertDialog(
            onDismissRequest = { showDetails = false },
            title = { Text("File details") },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    DetailRow("Name", entry.name)
                    DetailRow(
                        "Type",
                        if (entry.isDirectory) "Folder" else "File"
                    )
                    DetailRow("Path", entry.file.absolutePath)

                    if (!entry.isDirectory) {
                        DetailRow("Size", formatFileSize(entry.size))
                    }

                    DetailRow(
                        "Modified",
                        formatDate(entry.lastModified)
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showDetails = false }) {
                    Text("Close")
                }
            }
        )
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(
            label,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.primary
        )

        Text(
            value,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

private fun formatFileSize(bytes: Long): String {
    if (bytes < 1024) return "$bytes B"

    val units = arrayOf("KB", "MB", "GB", "TB")
    var size = bytes.toDouble()
    var index = -1

    while (size >= 1024 && index < units.lastIndex) {
        size /= 1024
        index++
    }

    return String.format(Locale.US, "%.1f %s", size, units[index])
}

private fun formatDate(timestamp: Long): String {
    return DateFormat.getDateTimeInstance(
        DateFormat.MEDIUM,
        DateFormat.SHORT
    ).format(Date(timestamp))
}
