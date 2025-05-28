package com.example.jellyfinnew.ui.utils

import android.util.Log
import com.example.jellyfinnew.data.JellyfinConfig
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Provides debounced search functionality to prevent excessive API calls
 */
class DebouncedSearchManager(
    private val debounceDelayMs: Long = JellyfinConfig.UI.DEBOUNCE_DELAY_MS
) {
    companion object {
        private const val TAG = "DebouncedSearchManager"
    }

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var searchJob: Job? = null

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()

    private val _searchResults = MutableStateFlow<List<Any>>(emptyList())
    val searchResults: StateFlow<List<Any>> = _searchResults.asStateFlow()

    /**
     * Update search query with debouncing
     */
    fun updateQuery(newQuery: String, searchAction: suspend (String) -> List<Any>) {
        _query.value = newQuery
        
        // Cancel previous search
        searchJob?.cancel()
        
        if (newQuery.isBlank()) {
            _searchResults.value = emptyList()
            _isSearching.value = false
            return
        }
        
        Log.d(TAG, "Scheduling search for query: '$newQuery'")
        
        searchJob = scope.launch {
            try {
                // Wait for debounce delay
                delay(debounceDelayMs)
                
                // Check if query is still the same (user hasn't typed more)
                if (_query.value == newQuery) {
                    Log.d(TAG, "Executing search for query: '$newQuery'")
                    _isSearching.value = true
                    
                    val results = searchAction(newQuery)
                    
                    // Check again if query is still the same
                    if (_query.value == newQuery) {
                        _searchResults.value = results
                        Log.d(TAG, "Search completed for '$newQuery': ${results.size} results")
                    }
                }
            } catch (e: CancellationException) {
                Log.d(TAG, "Search cancelled for query: '$newQuery'")
            } catch (e: Exception) {
                Log.e(TAG, "Search error for query: '$newQuery'", e)
                if (_query.value == newQuery) {
                    _searchResults.value = emptyList()
                }
            } finally {
                if (_query.value == newQuery) {
                    _isSearching.value = false
                }
            }
        }
    }

    /**
     * Clear search results and query
     */
    fun clear() {
        Log.d(TAG, "Clearing search")
        searchJob?.cancel()
        _query.value = ""
        _searchResults.value = emptyList()
        _isSearching.value = false
    }

    /**
     * Cancel current search
     */
    fun cancelSearch() {
        Log.d(TAG, "Cancelling current search")
        searchJob?.cancel()
        _isSearching.value = false
    }

    /**
     * Get search statistics
     */
    fun getSearchStats(): String {
        return buildString {
            appendLine("Search Statistics:")
            appendLine("  Current Query: '${_query.value}'")
            appendLine("  Is Searching: ${_isSearching.value}")
            appendLine("  Results Count: ${_searchResults.value.size}")
            appendLine("  Debounce Delay: ${debounceDelayMs}ms")
        }
    }

    /**
     * Cleanup resources
     */
    fun cleanup() {
        Log.d(TAG, "Cleaning up debounced search manager")
        searchJob?.cancel()
        scope.cancel()
    }
}

/**
 * Extension function for easy debounced search creation
 */
fun createDebouncedSearch(
    debounceDelayMs: Long = JellyfinConfig.UI.DEBOUNCE_DELAY_MS
): DebouncedSearchManager {
    return DebouncedSearchManager(debounceDelayMs)
}
