package com.example.jellyfinnew.data.repositories

import android.content.Context
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.jellyfin.sdk.createJellyfin
import org.jellyfin.sdk.api.client.ApiClient
import org.jellyfin.sdk.api.client.extensions.authenticateUserByName
import org.jellyfin.sdk.api.client.extensions.userApi
import org.jellyfin.sdk.model.ClientInfo
import com.example.jellyfinnew.data.ConnectionState
import com.example.jellyfinnew.data.ErrorHandler
import com.example.jellyfinnew.data.JellyfinError
import com.example.jellyfinnew.data.JellyfinConfig
import com.example.jellyfinnew.data.ImageUrlHelper

/**
 * Repository responsible for managing Jellyfin server connections and authentication
 */
class ConnectionRepository(private val context: Context) {
    
    companion object {
        private val TAG = JellyfinConfig.Logging.getTag("Connection")
    }
    
    private val jellyfin = createJellyfin {
        clientInfo = ClientInfo(JellyfinConfig.CLIENT_NAME, JellyfinConfig.CLIENT_VERSION)
        context = this@ConnectionRepository.context
    }
    
    private var _apiClient: ApiClient? = null
    val apiClient: ApiClient? get() = _apiClient
    
    private var _imageUrlHelper: ImageUrlHelper? = null
    val imageUrlHelper: ImageUrlHelper? get() = _imageUrlHelper
    
    private val _connectionState = MutableStateFlow(ConnectionState())
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()
    
    /**
     * Connect to Jellyfin server with authentication
     */
    suspend fun connect(serverUrl: String, username: String, password: String): Boolean {
        return try {
            Log.d(TAG, "Attempting to connect to $serverUrl with username: $username")
            updateConnectionState(isLoading = true, error = null)

            val client = jellyfin.createApi(baseUrl = serverUrl)
            Log.d(TAG, "Authenticating user...")

            val authResult = client.userApi.authenticateUserByName(
                username = username,
                password = password
            )

            authResult.content.accessToken?.let { token ->
                Log.d(TAG, "Authentication successful")

                val user = authResult.content.user
                Log.d(TAG, "User ID: ${user?.id}, User Name: ${user?.name}")

                val authenticatedClient = jellyfin.createApi(
                    baseUrl = serverUrl,
                    accessToken = token
                )

                _apiClient = authenticatedClient
                _imageUrlHelper = ImageUrlHelper(authenticatedClient)

                updateConnectionState(isConnected = true, isLoading = false)
                Log.d(TAG, "Connection established successfully")

                true
            } ?: run {
                Log.w(TAG, "Authentication failed - no access token received")
                val error = JellyfinError.AuthenticationError("Authentication failed - no access token received")
                updateConnectionState(error = ErrorHandler.getConnectionErrorMessage(error))
                false
            }
        } catch (e: Exception) {
            val jellyfinError = ErrorHandler.handleException(e, "Connection")
            updateConnectionState(error = ErrorHandler.getConnectionErrorMessage(jellyfinError))
            false
        }
    }
    
    /**
     * Enhanced connect method that handles self-signed certificates and reverse proxy scenarios
     */
    suspend fun connectWithSSLFallback(
        serverUrl: String,
        username: String,
        password: String
    ): Boolean {
        // First try the original URL
        val result = connect(serverUrl, username, password)
        
        if (!result && serverUrl.startsWith("https://")) {
            Log.i(TAG, "HTTPS connection failed, this may be due to self-signed certificate")
            Log.i(TAG, "Consider installing the certificate or configuring your reverse proxy with a valid certificate")
            
            // Note: We don't automatically fall back to HTTP for security reasons
            // Users should explicitly use HTTP if they want an insecure connection
        }
        
        return result
    }
    
    /**
     * Disconnect from the server and clear all connection state
     */
    fun disconnect() {
        Log.d(TAG, "Disconnecting from server")
        _apiClient = null
        _imageUrlHelper = null
        updateConnectionState(isConnected = false, isLoading = false, error = null)
    }
    
    /**
     * Check if currently connected to a server
     */
    fun isConnected(): Boolean = _connectionState.value.isConnected && _apiClient != null
    
    /**
     * Get the current server base URL
     */
    fun getServerUrl(): String? = _apiClient?.baseUrl
    
    private fun updateConnectionState(
        isConnected: Boolean = _connectionState.value.isConnected,
        isLoading: Boolean = _connectionState.value.isLoading,
        error: String? = _connectionState.value.error
    ) {
        _connectionState.value = ConnectionState(isConnected, isLoading, error)
        Log.v(TAG, "Connection state updated: connected=$isConnected, loading=$isLoading, error=$error")
    }
}
