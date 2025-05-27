package com.example.jellyfinnew.data

import android.util.Log
import org.jellyfin.sdk.api.client.exception.ApiClientException
import java.net.ConnectException
import java.net.UnknownHostException
import javax.net.ssl.SSLException
import java.security.cert.CertificateException

/**
 * Centralized error handling for the Jellyfin Android TV app
 */
sealed class JellyfinError {
    data class ConnectionError(val message: String, val cause: Throwable? = null) : JellyfinError()
    data class AuthenticationError(val message: String, val cause: Throwable? = null) : JellyfinError()
    data class ServerError(val message: String, val cause: Throwable? = null) : JellyfinError()
    data class NetworkError(val message: String, val cause: Throwable? = null) : JellyfinError()
    data class SSLError(val message: String, val cause: Throwable? = null) : JellyfinError()
    data class DataError(val message: String, val cause: Throwable? = null) : JellyfinError()
    data class UnknownError(val message: String, val cause: Throwable? = null) : JellyfinError()
    
    fun getUserFriendlyMessage(): String = when (this) {
        is ConnectionError -> message
        is AuthenticationError -> message
        is ServerError -> message
        is NetworkError -> message
        is SSLError -> message
        is DataError -> message
        is UnknownError -> message
    }
}

/**
 * Centralized error handler for consistent error processing
 */
object ErrorHandler {
    private const val TAG = "ErrorHandler"
    
    /**
     * Process and categorize exceptions into user-friendly errors
     */
    fun handleException(
        exception: Throwable,
        context: String = "Unknown operation"
    ): JellyfinError {
        Log.e(TAG, "Handling exception in $context", exception)
        
        return when (exception) {
            is ApiClientException -> handleApiClientException(exception)
            is SSLException -> handleSSLException(exception)
            is CertificateException -> handleCertificateException(exception)
            is ConnectException -> handleConnectionException(exception)
            is UnknownHostException -> handleUnknownHostException(exception)
            is kotlinx.coroutines.CancellationException -> {
                // Don't log cancellations as errors
                Log.d(TAG, "Operation cancelled: $context")
                JellyfinError.DataError("Operation cancelled", exception)
            }
            else -> handleGenericException(exception, context)
        }
    }
    
    private fun handleApiClientException(exception: ApiClientException): JellyfinError {
        val message = exception.message ?: "API error"
        
        return when {
            message.contains("401") -> JellyfinError.AuthenticationError(
                "Invalid username or password", exception
            )
            message.contains("403") -> JellyfinError.AuthenticationError(
                "Access denied - check user permissions", exception
            )
            message.contains("404") -> JellyfinError.ServerError(
                "Server not found - check URL", exception
            )
            message.contains("timeout") -> JellyfinError.NetworkError(
                "Connection timeout - check network", exception
            )
            message.contains("SSL") || message.contains("certificate") -> JellyfinError.SSLError(
                "SSL certificate error - For self-signed certificates behind reverse proxy: " +
                        "Install certificate in Android settings or use HTTP connection", exception
            )
            message.contains("Connection refused") -> JellyfinError.ConnectionError(
                "Connection refused - check server and port", exception
            )
            message.contains("UnknownHostException") -> JellyfinError.NetworkError(
                "Cannot resolve hostname - check URL", exception
            )
            else -> JellyfinError.ServerError("Connection failed: $message", exception)
        }
    }
    
    private fun handleSSLException(exception: SSLException): JellyfinError {
        val message = exception.message ?: "SSL error"
        
        return when {
            message.contains("certificate", ignoreCase = true) -> JellyfinError.SSLError(
                "SSL certificate error - For self-signed certificates behind reverse proxy: " +
                        "1) Install the certificate in Android's trusted certificates, or " +
                        "2) Use HTTP instead of HTTPS, or " +
                        "3) Configure your reverse proxy to use a valid certificate", exception
            )
            message.contains("handshake", ignoreCase = true) -> JellyfinError.SSLError(
                "SSL handshake failed - Check certificate configuration on your reverse proxy", exception
            )
            message.contains("hostname", ignoreCase = true) -> JellyfinError.SSLError(
                "SSL hostname verification failed - Ensure certificate matches domain name", exception
            )
            else -> JellyfinError.SSLError(
                "SSL connection error - check server certificate configuration", exception
            )
        }
    }
    
    private fun handleCertificateException(exception: CertificateException): JellyfinError {
        return JellyfinError.SSLError(
            "Certificate validation failed - For self-signed certificates: " +
                    "Install the certificate in Android's trusted store or use HTTP connection", exception
        )
    }
    
    private fun handleConnectionException(exception: ConnectException): JellyfinError {
        return JellyfinError.ConnectionError(
            "Connection refused - check server URL and port", exception
        )
    }
    
    private fun handleUnknownHostException(exception: UnknownHostException): JellyfinError {
        return JellyfinError.NetworkError(
            "Cannot resolve hostname - check server URL", exception
        )
    }
    
    private fun handleGenericException(exception: Throwable, context: String): JellyfinError {
        val message = exception.message ?: "Unknown error occurred"
        Log.w(TAG, "Unhandled exception type in $context: ${exception::class.simpleName}")
        
        return JellyfinError.UnknownError("Unexpected error: $message", exception)
    }
    
    /**
     * Convert JellyfinError to connection state error message
     */
    fun getConnectionErrorMessage(error: JellyfinError): String {
        return when (error) {
            is JellyfinError.SSLError -> {
                // For SSL errors, provide specific guidance
                error.message
            }
            is JellyfinError.AuthenticationError -> {
                "Authentication failed: ${error.message}"
            }
            is JellyfinError.NetworkError -> {
                "Network error: ${error.message}"
            }
            is JellyfinError.ConnectionError -> {
                "Connection error: ${error.message}"
            }
            else -> error.getUserFriendlyMessage()
        }
    }
}
