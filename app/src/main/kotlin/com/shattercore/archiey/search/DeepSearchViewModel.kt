package com.shattercore.archiey.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shattercore.archiey.model.FileEntry
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class DeepSearchViewModel : ViewModel() {

    private val _results =
        MutableStateFlow<List<FileEntry>>(emptyList())
    val results: StateFlow<List<FileEntry>> = _results

    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private var searchJob: Job? = null
    private var generation = 0L

    fun search(directory: File, query: String) {
        searchJob?.cancel()
        generation++

        val requestId = generation

        _results.value = emptyList()
        _error.value = null
        _isSearching.value = false

        if (query.isBlank()) return

        searchJob = viewModelScope.launch {
            delay(350)

            if (requestId != generation) return@launch

            _isSearching.value = true

            try {
                val matches = withContext(Dispatchers.IO) {
                    DeepSearchEngine.search(
                        directory = directory,
                        query = query,
                        maxResults = 300
                    )
                }

                if (requestId == generation) {
                    _results.value = matches
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                if (requestId == generation) {
                    _error.value =
                        e.message ?: "Search failed"
                }
            } finally {
                if (requestId == generation) {
                    _isSearching.value = false
                }
            }
        }
    }

    fun cancelSearch() {
        searchJob?.cancel()
        searchJob = null
        generation++

        _results.value = emptyList()
        _error.value = null
        _isSearching.value = false
    }
}
