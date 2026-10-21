package com.shattercore.archiey.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Sort
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import com.shattercore.archiey.browser.SortMode

@Composable
fun SortMenu(
    sortMode: SortMode,
    descending: Boolean,
    onSortModeChange: (SortMode) -> Unit,
    onDescendingChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    Box(modifier = modifier) {
        IconButton(onClick = {
            focusManager.clearFocus()
            keyboardController?.hide()
            expanded = true
        }) {
            Icon(
                imageVector = Icons.Outlined.Sort,
                contentDescription = "Sort files",
                tint = MaterialTheme.colorScheme.primary
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            Text(
                text = "Sort by",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary
            )

            SortMode.entries.forEach { mode ->
                DropdownMenuItem(
                    text = {
                        Text(
                            when (mode) {
                                SortMode.NAME -> "Name"
                                SortMode.SIZE -> "Size"
                                SortMode.DATE -> "Date modified"
                            }
                        )
                    },
                    trailingIcon = {
                        if (sortMode == mode) {
                            Text("✓")
                        }
                    },
                    onClick = {
                        onSortModeChange(mode)
                        expanded = false
                    }
                )
            }

            HorizontalDivider()

            DropdownMenuItem(
                text = {
                    Text(
                        if (descending) {
                            "Descending ↓"
                        } else {
                            "Ascending ↑"
                        }
                    )
                },
                onClick = {
                    onDescendingChange(!descending)
                    expanded = false
                }
            )
        }
    }
}
