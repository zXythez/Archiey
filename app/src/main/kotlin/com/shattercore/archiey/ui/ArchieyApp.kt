package com.shattercore.archiey.ui

import android.os.Environment
import androidx.compose.ui.platform.LocalContext
import com.shattercore.archiey.files.FileOpener
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.shattercore.archiey.browser.FileBrowserViewModel
import com.shattercore.archiey.browser.FileSorter
import com.shattercore.archiey.search.DeepSearchViewModel
import com.shattercore.archiey.ui.components.*
import com.shattercore.archiey.files.ProtectedPaths

@Composable
fun ArchieyApp(
    browser: FileBrowserViewModel = viewModel(),
    deepSearch: DeepSearchViewModel = viewModel()
) {
    val context = LocalContext.current
    var renameTarget by remember { mutableStateOf<com.shattercore.archiey.model.FileEntry?>(null) }
    var renameText by remember { mutableStateOf("") }
    var renameError by remember { mutableStateOf<String?>(null) }
    var renameBusy by remember { mutableStateOf(false) }
    var protectedWarning by remember { mutableStateOf(false) }
    var confirmDangerousRename by remember { mutableStateOf(false) }
    var pasteError by remember { mutableStateOf<String?>(null) }
    val pasteBusy by browser.pasteBusy.collectAsStateWithLifecycle()
    val pasteCancelling by browser.pasteCancelling.collectAsStateWithLifecycle()
    val copySource by browser.copySource.collectAsStateWithLifecycle()
    val directory by browser.currentDirectory.collectAsStateWithLifecycle()
    val files by browser.files.collectAsStateWithLifecycle()
   val sortMode by browser.sortMode.collectAsStateWithLifecycle()
val sortDescending by browser.sortDescending.collectAsStateWithLifecycle()
    val error by browser.error.collectAsStateWithLifecycle()

    var searchQuery by remember { mutableStateOf("") }
    LaunchedEffect(directory) {
        searchQuery = ""
    }

    var searchSubfolders by remember { mutableStateOf(false) }
    val deepResults by deepSearch.results.collectAsStateWithLifecycle()
    val isSearching by deepSearch.isSearching.collectAsStateWithLifecycle()
    val searchError by deepSearch.error.collectAsStateWithLifecycle()

    LaunchedEffect(directory, searchQuery, searchSubfolders) {
        if (searchSubfolders) {
            deepSearch.search(directory, searchQuery)
        } else {
            deepSearch.cancelSearch()
        }
    }


    val filteredFiles = remember(files, searchQuery, searchSubfolders, deepResults, sortMode, sortDescending) {
        if (searchSubfolders && searchQuery.isNotBlank()) {
            FileSorter.sort(deepResults, sortMode, sortDescending)
        } else {
        if (searchQuery.isBlank()) {
            files
        } else {
            files.filter { entry ->
                entry.name.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    }

    val rootPath = remember {
        Environment.getExternalStorageDirectory().canonicalPath
    }

    val isRoot = directory.canonicalPath == rootPath

    var selectedTab by remember {
        mutableStateOf(ArchieyTab.HOME)
    }

    BackHandler(
        enabled = selectedTab != ArchieyTab.HOME || !isRoot
    ) {
        if (selectedTab != ArchieyTab.HOME) {
            selectedTab = ArchieyTab.HOME
        } else {
            browser.goBack()
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            Column {
                if (copySource != null && selectedTab == ArchieyTab.HOME) {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceContainer,
                        tonalElevation = 3.dp
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                Icons.Outlined.ContentCopy,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )

                            Text(
                                text = copySource?.name ?: "",
                                modifier = Modifier.weight(1f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                fontSize = 13.sp
                            )

                            TextButton(
                                onClick = { browser.clearCopySelection() },
                                enabled = !pasteCancelling
                            ) {
                                Text(if (pasteCancelling) "Cancelling..." else "Cancel")
                            }

                            Button(
    onClick = {
        pasteError = null
        browser.pasteCopiedFile { resultError ->
            pasteError = resultError
        }
    },
    enabled = !pasteBusy && copySource != null
) {
    Text(if (pasteCancelling) "Stopping..." else if (pasteBusy) "Copying..." else "Paste")
}
                        }
                    }
                }

                BottomNavBar(
                    selectedTab = selectedTab,
                    onTabSelected = { selectedTab = it }
                )
            }
        }
    ) { padding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Text(
                text = "Archiey",
                modifier = Modifier.padding(
                    start = 20.dp,
                    top = 18.dp,
                    bottom = 12.dp
                ),
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            when (selectedTab) {
                ArchieyTab.HOME -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(
                            horizontal = 16.dp,
                            vertical = 8.dp
                        ),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (isRoot) {
                            item {
                                StorageCard()
                            }
                        }

                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (!isRoot) {
                                    IconButton(
                                        onClick = { browser.goBack() }
                                    ) {
                                        Icon(
                                            Icons.Outlined.ArrowBack,
                                            contentDescription = "Go back",
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                } else {
                                    Icon(
                                        Icons.Outlined.FolderOpen,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(
                                            horizontal = 12.dp
                                        )
                                    )
                                }

                                Column(
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(
                                        text = if (isRoot) {
                                            "Internal storage"
                                        } else {
                                            directory.name
                                        },
                                        fontSize = 17.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onBackground
                                    )

                                    Text(
                                        text = directory.absolutePath,
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                          SortMenu(
    sortMode = sortMode,
    descending = sortDescending,
    onSortModeChange = browser::setSortMode,
    onDescendingChange = browser::setSortDescending
)
                            }
                        }

                        item {
                            OutlinedTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                modifier = Modifier.fillMaxWidth(),
                                placeholder = { Text("Search this folder...") },
                                leadingIcon = {
                                    Icon(
                                        Icons.Outlined.Search,
                                        contentDescription = null
                                    )
                                },
                                singleLine = true,
                                shape = MaterialTheme.shapes.large
                            )
                        }

                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Search subfolders",
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontSize = 14.sp
                                )
                                Switch(
                                    checked = searchSubfolders,
                                    onCheckedChange = { searchSubfolders = it }
                                )
                            }
                        }

                        if (searchSubfolders && searchQuery.isNotBlank() && isSearching) {
                            item {
                                LinearProgressIndicator(
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }

                        if (searchSubfolders && searchError != null) {
                            item {
                                Text(
                                    text = searchError ?: "Search failed",
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }

                        if (error != null) {
                            item {
                                Text(
                                    text = error ?: "Unable to open folder",
                                    color = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.padding(12.dp)
                                )
                            }
                        }

                        items(
                            items = filteredFiles,
                            key = { it.file.absolutePath }
                        ) { entry ->
                            FileItem(
                                entry = entry,
                                showParentPath = searchSubfolders && searchQuery.isNotBlank(),
                                onCopy = { selected ->
                                    browser.selectForCopy(selected)
                                },
                                onRename = { selected ->
                                    if (!ProtectedPaths.canRename(context, 
                                            selected.file,
                                            java.io.File(selected.file.parentFile, selected.name + "_test")
                                        )) {
                                        protectedWarning = true
                                    } else {
                                        renameTarget = selected
                                        renameText = selected.name
                                        renameError = null
                                    }
                                },
                                onClick = {
                                    if (entry.isDirectory) {
                                        browser.openFolder(entry)
                                    } else {
                                        FileOpener.open(context, entry.file)
                                    }
                                }
                            )
                        }

                        if (filteredFiles.isEmpty() && error == null && !isSearching && searchError == null) {
                            item {
                                Text(
                                    if (searchQuery.isNotBlank()) "No matching files" else "No files to display",
                                    modifier = Modifier.padding(16.dp),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                ArchieyTab.SETTINGS -> {
                    SettingsScreen()
                }

                else -> {
                    val title = when (selectedTab) {
                        ArchieyTab.ARCHIVES -> "Archives"
                        ArchieyTab.RECENT -> "Recent"
                        ArchieyTab.SETTINGS -> "Settings"
                        else -> ""
                    }

                    Text(
                        text = title,
                        modifier = Modifier.padding(20.dp),
                        fontSize = 22.sp,
                        color = MaterialTheme.colorScheme.onBackground
                    )

                    Text(
                        text = "Coming soon",
                        modifier = Modifier.padding(horizontal = 20.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }

    if (pasteError != null) {
        AlertDialog(
            onDismissRequest = { pasteError = null },
            title = { Text("Copy failed") },
            text = {
                Text(pasteError ?: "Unable to copy file")
            },
            confirmButton = {
                TextButton(onClick = { pasteError = null }) {
                    Text("OK")
                }
            }
        )
    }

    if (protectedWarning) {
        AlertDialog(
            onDismissRequest = { protectedWarning = false },
            title = { Text("Protected directory") },
            text = {
                Text("This Android directory is protected to prevent accidental app data loss.")
            },
            confirmButton = {
                TextButton(onClick = { protectedWarning = false }) {
                    Text("OK")
                }
            }
        )
    }

    if (confirmDangerousRename) {
        AlertDialog(
            onDismissRequest = { confirmDangerousRename = false },
            title = { Text("Confirm dangerous rename") },
            text = {
                Text("Renaming Android-managed files or directories can break apps or cause data loss. Continue?")
            },
            confirmButton = {
                TextButton(onClick = {
                    val target = renameTarget
                    if (target != null && !renameBusy) {
                        confirmDangerousRename = false
                        renameBusy = true
                        browser.renameFile(context, target, renameText) { error ->
                            renameBusy = false
                            if (error == null) renameTarget = null
                            else renameError = error
                        }
                    }
                }) { Text("Rename anyway") }
            },
            dismissButton = {
                TextButton(onClick = { confirmDangerousRename = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (renameTarget != null && !confirmDangerousRename) {
        AlertDialog(
            onDismissRequest = {
                if (!renameBusy) renameTarget = null
            },
            title = { Text("Rename") },
            text = {
                Column {
                    OutlinedTextField(
                        value = renameText,
                        onValueChange = {
                            renameText = it
                            renameError = null
                        },
                        label = { Text("New name") },
                        singleLine = true,
                        enabled = !renameBusy
                    )

                    if (renameError != null) {
                        Text(
                            text = renameError ?: "",
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    enabled = !renameBusy && renameText.isNotBlank(),
                    onClick = {
                        val target = renameTarget
                        if (target != null && !renameBusy) {
                            if (renameText != target.name &&
                                ProtectedPaths.requiresConfirmation(target.file)) {
                                confirmDangerousRename = true
                            } else {
                                renameBusy = true
                                browser.renameFile(context, target, renameText) { error ->
                                    renameBusy = false
                                    if (error == null) {
                                        renameTarget = null
                                    } else {
                                        renameError = error
                                    }
                                }
                            }
                        }
                    }
                ) {
                    Text(if (renameBusy) "Renaming..." else "Rename")
                }
            },
            dismissButton = {
                TextButton(
                    enabled = !renameBusy,
                    onClick = { renameTarget = null }
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}
