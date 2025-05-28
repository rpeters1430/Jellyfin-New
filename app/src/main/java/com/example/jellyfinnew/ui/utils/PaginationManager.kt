package com.example.jellyfinnew.ui.utils

import android.util.Log
import com.example.jellyfinnew.data.MediaItem
import com.example.jellyfinnew.data.JellyfinConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Manages pagination for large media libraries to prevent memory issues and improve performance
 */
class PaginationManager {
    companion object {
        private const val TAG = "PaginationManager"
        private const val DEFAULT_PAGE_SIZE = JellyfinConfig.Performance.PAGE_SIZE
    }

    private val _currentPage = MutableStateFlow(0)
    val currentPage: StateFlow<Int> = _currentPage.asStateFlow()

    private val _hasNextPage = MutableStateFlow(false)
    val hasNextPage: StateFlow<Boolean> = _hasNextPage.asStateFlow()

    private val _hasPreviousPage = MutableStateFlow(false)
    val hasPreviousPage: StateFlow<Boolean> = _hasPreviousPage.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _totalItems = MutableStateFlow(0)
    val totalItems: StateFlow<Int> = _totalItems.asStateFlow()

    private val _paginatedItems = MutableStateFlow<List<MediaItem>>(emptyList())
    val paginatedItems: StateFlow<List<MediaItem>> = _paginatedItems.asStateFlow()

    private var allItems: List<MediaItem> = emptyList()
    private var pageSize: Int = DEFAULT_PAGE_SIZE

    /**
     * Initialize pagination with a list of items
     */
    fun initializePagination(
        items: List<MediaItem>,
        customPageSize: Int = DEFAULT_PAGE_SIZE
    ) {
        Log.d(TAG, "Initializing pagination with ${items.size} items, page size: $customPageSize")
        
        allItems = items
        pageSize = customPageSize
        _currentPage.value = 0
        _totalItems.value = items.size
        
        updatePaginationState()
        loadCurrentPage()
    }

    /**
     * Load the next page if available
     */
    fun loadNextPage() {
        if (_hasNextPage.value && !_isLoading.value) {
            Log.d(TAG, "Loading next page: ${_currentPage.value + 1}")
            _currentPage.value += 1
            updatePaginationState()
            loadCurrentPage()
        }
    }

    /**
     * Load the previous page if available
     */
    fun loadPreviousPage() {
        if (_hasPreviousPage.value && !_isLoading.value) {
            Log.d(TAG, "Loading previous page: ${_currentPage.value - 1}")
            _currentPage.value -= 1
            updatePaginationState()
            loadCurrentPage()
        }
    }

    /**
     * Jump to a specific page
     */
    fun goToPage(page: Int) {
        val totalPages = getTotalPages()
        if (page in 0 until totalPages && !_isLoading.value) {
            Log.d(TAG, "Jumping to page: $page")
            _currentPage.value = page
            updatePaginationState()
            loadCurrentPage()
        }
    }

    /**
     * Reset pagination to the first page
     */
    fun reset() {
        Log.d(TAG, "Resetting pagination")
        _currentPage.value = 0
        updatePaginationState()
        loadCurrentPage()
    }

    /**
     * Check if pagination is needed for the current item count
     */
    fun isPaginationNeeded(): Boolean {
        return allItems.size > pageSize
    }

    /**
     * Get total number of pages
     */
    fun getTotalPages(): Int {
        return if (allItems.isEmpty()) 0 else (allItems.size + pageSize - 1) / pageSize
    }

    /**
     * Get current page info as a formatted string
     */
    fun getPageInfo(): String {
        val totalPages = getTotalPages()
        return if (totalPages > 0) {
            "Page ${_currentPage.value + 1} of $totalPages"
        } else {
            "No items"
        }
    }

    private fun updatePaginationState() {
        val totalPages = getTotalPages()
        _hasNextPage.value = _currentPage.value < totalPages - 1
        _hasPreviousPage.value = _currentPage.value > 0
    }

    private fun loadCurrentPage() {
        _isLoading.value = true
        
        try {
            val startIndex = _currentPage.value * pageSize
            val endIndex = minOf(startIndex + pageSize, allItems.size)
            
            val pageItems = if (startIndex < allItems.size) {
                allItems.subList(startIndex, endIndex)
            } else {
                emptyList()
            }
            
            Log.d(TAG, "Loaded page ${_currentPage.value}: ${pageItems.size} items (indices $startIndex-${endIndex-1})")
            _paginatedItems.value = pageItems
            
        } catch (e: Exception) {
            Log.e(TAG, "Error loading page ${_currentPage.value}", e)
            _paginatedItems.value = emptyList()
        } finally {
            _isLoading.value = false
        }
    }

    /**
     * Search within paginated items
     */
    fun searchInCurrentPage(query: String): List<MediaItem> {
        return _paginatedItems.value.filter { item ->
            item.name.contains(query, ignoreCase = true) ||
            item.overview?.contains(query, ignoreCase = true) == true
        }
    }

    /**
     * Update page size and refresh pagination
     */
    fun updatePageSize(newPageSize: Int) {
        if (newPageSize > 0 && newPageSize != pageSize) {
            Log.d(TAG, "Updating page size from $pageSize to $newPageSize")
            pageSize = newPageSize
            
            // Try to maintain approximate position
            val currentItemIndex = _currentPage.value * pageSize
            val newPage = currentItemIndex / newPageSize
            
            _currentPage.value = newPage
            updatePaginationState()
            loadCurrentPage()
        }
    }
}
