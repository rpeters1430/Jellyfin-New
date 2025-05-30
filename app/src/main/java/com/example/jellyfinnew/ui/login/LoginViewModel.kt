package com.example.jellyfinnew.ui.login

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.jellyfinnew.data.ConnectionState
import com.example.jellyfinnew.data.JellyfinRepository
import com.example.jellyfinnew.data.UserPreferencesManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import com.example.jellyfinnew.di.ServiceLocator

data class LoginUiState(
    val serverUrl: String = "",
    val username: String = "",
    val password: String = "",
    val connectionState: ConnectionState = ConnectionState(),
    val isLoadingCredentials: Boolean = true,
    val hasAttemptedAutoLogin: Boolean = false
)

class LoginViewModel(
    application: Application
) : AndroidViewModel(application) {
    
    private val repository: JellyfinRepository = ServiceLocator.provideRepository(application)
    private val userPreferencesManager: UserPreferencesManager = ServiceLocator.provideUserPreferencesManager(application)
    
    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()
      init {
        viewModelScope.launch {
            repository.connectionState.collect { connectionState ->
                _uiState.value = _uiState.value.copy(connectionState = connectionState)
                
                // Clear loading state if there's an error
                if (connectionState.error != null && connectionState.isLoading) {
                    _uiState.value = _uiState.value.copy(
                        connectionState = connectionState.copy(isLoading = false)
                    )
                }
            }
        }
        
        // Load saved credentials
        loadSavedCredentials()
    }    private fun loadSavedCredentials() {
        viewModelScope.launch {
            userPreferencesManager.loginCredentials.collect { credentials ->
                val hasCredentials = credentials.serverUrl.isNotEmpty() && 
                                   credentials.username.isNotEmpty() && 
                                   credentials.password.isNotEmpty()
                
                _uiState.value = _uiState.value.copy(
                    serverUrl = credentials.serverUrl,
                    username = credentials.username,
                    password = credentials.password,
                    isLoadingCredentials = false
                )
                
                // Auto-login if credentials are available and we haven't attempted auto-login yet
                if (hasCredentials && !_uiState.value.hasAttemptedAutoLogin) {
                    _uiState.value = _uiState.value.copy(hasAttemptedAutoLogin = true)
                    login()
                }
            }
        }
    }
      fun updateServerUrl(url: String) {
        _uiState.value = _uiState.value.copy(serverUrl = url)
        clearError() // Clear any previous errors when user starts typing
    }
    
    fun updateUsername(username: String) {
        _uiState.value = _uiState.value.copy(username = username)
        clearError() // Clear any previous errors when user starts typing
    }
    
    fun updatePassword(password: String) {
        _uiState.value = _uiState.value.copy(password = password)
        clearError() // Clear any previous errors when user starts typing
    }
      fun updateRememberLogin(remember: Boolean) {
        // This function is kept for compatibility but does nothing
        // since we always save credentials on successful login
    }    fun login() {
        viewModelScope.launch {
            val state = _uiState.value
            val normalizedUrl = state.serverUrl.let { if (it.startsWith("http")) it else "http://$it" }
            
            val result = repository.connect(
                serverUrl = normalizedUrl,
                username = state.username,
                password = state.password
            )
            
            // Always save credentials on successful login for auto-login
            if (result) {
                userPreferencesManager.saveLoginCredentials(
                    serverUrl = normalizedUrl,
                    username = state.username,
                    password = state.password
                )
            }
        }
    }
    
    fun logout() {
        viewModelScope.launch {
            userPreferencesManager.clearLoginCredentials()
            repository.disconnect()
        }
    }
    
    fun clearError() {
        _uiState.value = _uiState.value.copy(
            connectionState = _uiState.value.connectionState.copy(error = null)
        )
    }
}
