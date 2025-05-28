package com.example.jellyfinnew.ui.general

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import com.example.jellyfinnew.data.MediaItem
import com.example.jellyfinnew.data.JellyfinRepository
import com.example.jellyfinnew.di.ServiceLocator
import com.example.jellyfinnew.data.ErrorHandler
import com.example.jellyfinnew.data.JellyfinConfig
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import android.util.Log

data class GeneralMediaUiState(
    val isLoading: Boolean = false,
    val error: String? = null
)

/**
 * ViewModel for handling general media content (books, photos, etc.)
 */
class GeneralMediaViewModel(application: Application) : AndroidViewModel(application) {
    
    companion object {
        private val TAG = JellyfinConfig.Logging.getTag("GeneralMedia")
    }
    
    private val repository: JellyfinRepository = ServiceLocator.provideRepository(application)
    
    private val _uiState = MutableStateFlow(GeneralMediaUiState())
    val uiState: StateFlow<GeneralMediaUiState> = _uiState.asStateFlow()
    
    private val _library = MutableStateFlow<MediaItem?>(null)
    val library: StateFlow<MediaItem?> = _library.asStateFlow()
    
    private val _items = MutableStateFlow<List<MediaItem>>(emptyList())
    val items: StateFlow<List<MediaItem>> = _items.asStateFlow()
    
    private val _focusedItem = MutableStateFlow<MediaItem?>(null)
    val focusedItem: StateFlow<MediaItem?> = _focusedItem.asStateFlow()
    
    /**
     * Load library and its items
     */
    fun loadLibrary(libraryId: String) {
        viewModelScope.launch {
            try {
                Log.d(TAG, "Loading library: $libraryId")
                _uiState.value = _uiState.value.copy(isLoading = true, error = null)
                
                // Find the library from the current media libraries
                val libraries = repository.mediaLibraries.value
                val library = libraries.find { it.id == libraryId }
                
                if (library != null) {
                    _library.value = library
                    Log.d(TAG, "Found library: ${library.name}")
                    
                    // Load library items
                    repository.loadLibraryItems(libraryId)
                    
                    // Get the items from the repository
                    val libraryItems = repository.currentLibraryItems.value
                    _items.value = libraryItems
                    
                    Log.d(TAG, "Loaded ${libraryItems.size} items for library: ${library.name}")
                    _uiState.value = _uiState.value.copy(isLoading = false)
                } else {
                    Log.w(TAG, "Library not found: $libraryId")
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "Library not found"
                    )
                }
                
            } catch (e: Exception) {
                val error = ErrorHandler.handleException(e, "Failed to load library content")
                Log.e(TAG, error.getUserFriendlyMessage(), e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = error.getUserFriendlyMessage()
                )
            }
        }
    }
    
    /**
     * Set the currently focused item for background image
     */
    fun setFocusedItem(item: MediaItem?) {
        _focusedItem.value = item
    }
    
    /**
     * Clear all data
     */
    fun clearData() {
        _library.value = null
        _items.value = emptyList()
        _focusedItem.value = null
        _uiState.value = GeneralMediaUiState()
    }
    
    override fun onCleared() {
        super.onCleared()
        clearData()
    }
}
