package com.shattercore.archiey.browser

import android.os.Environment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shattercore.archiey.model.FileEntry
import com.shattercore.archiey.files.FileRenamer
import com.shattercore.archiey.files.FileCopyEngine
import com.shattercore.archiey.files.FolderCopyEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean
import android.content.Context

class FileBrowserViewModel(
    private val storageRoot: File = Environment.getExternalStorageDirectory(),
    private val copyOperation: (
        source: File,
        destination: File,
        isCancelled: () -> Boolean
    ) -> Result<File> = { source, destination, isCancelled ->
        if (source.isDirectory) {
            FolderCopyEngine.copy(
                source = source,
                destination = destination,
                isCancelled = isCancelled
            )
        } else {
            FileCopyEngine.copy(
                source = source,
                destination = destination,
                isCancelled = isCancelled
            )
        }
    }
) : ViewModel() {

    private val root = storageRoot

    private val _currentDirectory = MutableStateFlow(root)
    val currentDirectory: StateFlow<File> = _currentDirectory

    private val _files = MutableStateFlow<List<FileEntry>>(emptyList())
    val files: StateFlow<List<FileEntry>> = _files

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private val _sortMode = MutableStateFlow(SortMode.NAME)
    val sortMode: StateFlow<SortMode> = _sortMode

    private val _sortDescending = MutableStateFlow(false)
    val sortDescending: StateFlow<Boolean> = _sortDescending


    private val _copySource = MutableStateFlow<File?>(null)
    val copySource: StateFlow<File?> = _copySource

    fun selectForCopy(entry: FileEntry) {
        if (_pasteBusy.value) return
        _copySource.value = entry.file
    }

    fun clearCopySelection() {
        if (_pasteBusy.value) {
            pasteCancelRequested.set(true)
            _pasteCancelling.value = true
        } else {
            _copySource.value = null
        }
    }

    private val pasteCancelRequested = AtomicBoolean(false)
    private val _pasteCancelling = MutableStateFlow(false)
    val pasteCancelling: StateFlow<Boolean> = _pasteCancelling
    private val _pasteBusy = MutableStateFlow(false)
    val pasteBusy: StateFlow<Boolean> = _pasteBusy

    fun pasteCopiedFile(onResult: (String?) -> Unit) {
        if (_pasteBusy.value) return

        val source = _copySource.value ?: return
        val targetDirectory = _currentDirectory.value
        val destination = File(targetDirectory, source.name)

        pasteCancelRequested.set(false)
        _pasteCancelling.value = false
        _pasteBusy.value = true

        viewModelScope.launch {
            try {
                val copyJob = kotlinx.coroutines.currentCoroutineContext()
                val result = withContext(Dispatchers.IO) {
                    copyOperation(
                        source,
                        destination,
                        { pasteCancelRequested.get() || !copyJob.isActive }
                    )
                }

                result.onSuccess {
                    if (_copySource.value == source) {
                        _copySource.value = null
                    }
                    if (_currentDirectory.value == targetDirectory) {
                        refresh()
                    }
                    onResult(null)
                }.onFailure {
                    onResult(it.message ?: "Copy failed")
                }
            } finally {
                _pasteCancelling.value = false
                _pasteBusy.value = false
            }
        }
    }

    private var loadJob: Job? = null

    init {
        refresh()
    }

    private fun sortEntries(entries: List<FileEntry>): List<FileEntry> =
        FileSorter.sort(entries, _sortMode.value, _sortDescending.value)

    fun setSortMode(mode: SortMode) {
        _sortMode.value = mode
        _files.value = sortEntries(_files.value)
    }

    fun setSortDescending(descending: Boolean) {
        _sortDescending.value = descending
        _files.value = sortEntries(_files.value)
    }

    fun refresh() {
        val directory = _currentDirectory.value

        loadJob?.cancel()

        loadJob = viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    val children = directory.listFiles()
                        ?: throw IllegalStateException(
                            "Cannot read this directory"
                        )

                    children.map { FileEntry(it) }
                }
            }

            result.onSuccess {
                _files.value = sortEntries(it)
                _error.value = null
            }.onFailure {
                _files.value = emptyList()
                _error.value = it.message ?: "Unable to open folder"
            }
        }
    }

    fun renameFile(
        context: Context,
        entry: FileEntry,
        newName: String,
        onResult: (String?) -> Unit
    ) {
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                FileRenamer.rename(context.applicationContext, entry.file, newName)
            }
            result.onSuccess {
                refresh()
                onResult(null)
            }.onFailure {
                onResult(it.message ?: "Rename failed")
            }
        }
    }

    fun openFolder(entry: FileEntry) {
        if (!entry.isDirectory) return

        _currentDirectory.value = entry.file
        _files.value = emptyList()
        refresh()
    }

    fun goBack(): Boolean {
        val current = _currentDirectory.value

        if (current == root) return false

        val parent = current.parentFile ?: return false

        if (!parent.toPath().normalize()
                .startsWith(root.toPath().normalize())) {
            return false
        }

        _currentDirectory.value = parent
        _files.value = emptyList()
        refresh()
        return true
    }
}
