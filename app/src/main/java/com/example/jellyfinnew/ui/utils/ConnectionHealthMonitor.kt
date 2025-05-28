package com.example.jellyfinnew.ui.utils

import android.util.Log
import com.example.jellyfinnew.data.JellyfinRepository
import com.example.jellyfinnew.data.JellyfinConfig
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.net.InetSocketAddress
import java.net.Socket

/**
 * Monitors connection health and provides network quality metrics
 */
class ConnectionHealthMonitor(
    private val repository: JellyfinRepository
) {
    companion object {
        private const val TAG = "ConnectionHealthMonitor"
        private const val PING_TIMEOUT_MS = 5000
        private const val GOOD_PING_THRESHOLD_MS = 100
        private const val POOR_PING_THRESHOLD_MS = 500
    }

    data class ConnectionHealth(
        val isConnected: Boolean,
        val pingMs: Long,
        val quality: ConnectionQuality,
        val lastChecked: Long,
        val consecutiveFailures: Int,
        val uptime: Long
    )

    enum class ConnectionQuality {
        EXCELLENT,  // < 100ms
        GOOD,       // 100-250ms
        FAIR,       // 250-500ms
        POOR,       // > 500ms
        UNKNOWN     // Failed to measure
    }

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    private val _connectionHealth = MutableStateFlow(
        ConnectionHealth(
            isConnected = false,
            pingMs = -1,
            quality = ConnectionQuality.UNKNOWN,
            lastChecked = 0,
            consecutiveFailures = 0,
            uptime = 0
        )
    )
    val connectionHealth: StateFlow<ConnectionHealth> = _connectionHealth.asStateFlow()

    private val _isMonitoring = MutableStateFlow(false)
    val isMonitoring: StateFlow<Boolean> = _isMonitoring.asStateFlow()

    private var monitoringJob: Job? = null
    private var startTime = 0L

    /**
     * Start monitoring connection health
     */
    fun startMonitoring() {
        if (_isMonitoring.value) return
        
        Log.d(TAG, "Starting connection health monitoring")
        _isMonitoring.value = true
        startTime = System.currentTimeMillis()
        
        monitoringJob = scope.launch {
            while (isActive && _isMonitoring.value) {
                try {
                    checkConnectionHealth()
                    delay(JellyfinConfig.Performance.HEALTH_CHECK_INTERVAL_MS)
                } catch (e: CancellationException) {
                    break
                } catch (e: Exception) {
                    Log.e(TAG, "Error in health monitoring", e)
                    delay(JellyfinConfig.Performance.HEALTH_CHECK_INTERVAL_MS)
                }
            }
        }
    }

    /**
     * Stop monitoring connection health
     */
    fun stopMonitoring() {
        if (!_isMonitoring.value) return
        
        Log.d(TAG, "Stopping connection health monitoring")
        _isMonitoring.value = false
        monitoringJob?.cancel()
        monitoringJob = null
    }

    /**
     * Force a health check
     */
    suspend fun forceHealthCheck(): ConnectionHealth {
        return checkConnectionHealth()
    }

    /**
     * Get connection quality description
     */
    fun getQualityDescription(quality: ConnectionQuality): String {
        return when (quality) {
            ConnectionQuality.EXCELLENT -> "Excellent"
            ConnectionQuality.GOOD -> "Good"
            ConnectionQuality.FAIR -> "Fair"
            ConnectionQuality.POOR -> "Poor"
            ConnectionQuality.UNKNOWN -> "Unknown"
        }
    }

    /**
     * Get connection health summary for debugging
     */
    fun getHealthSummary(): String {
        val health = _connectionHealth.value
        return buildString {
            appendLine("Connection Health:")
            appendLine("  Status: ${if (health.isConnected) "Connected" else "Disconnected"}")
            appendLine("  Ping: ${if (health.pingMs >= 0) "${health.pingMs}ms" else "N/A"}")
            appendLine("  Quality: ${getQualityDescription(health.quality)}")
            appendLine("  Failures: ${health.consecutiveFailures}")
            appendLine("  Uptime: ${formatUptime(health.uptime)}")
            appendLine("  Last Check: ${formatLastChecked(health.lastChecked)}")
        }
    }

    private suspend fun checkConnectionHealth(): ConnectionHealth {
        val currentTime = System.currentTimeMillis()
        val currentHealth = _connectionHealth.value
        
        try {
            // Check basic connection state first
            val connectionState = repository.connectionState.value
            if (!connectionState.isConnected) {
                val newHealth = currentHealth.copy(
                    isConnected = false,
                    pingMs = -1,
                    quality = ConnectionQuality.UNKNOWN,
                    lastChecked = currentTime,
                    consecutiveFailures = currentHealth.consecutiveFailures + 1,
                    uptime = if (startTime > 0) currentTime - startTime else 0
                )
                _connectionHealth.value = newHealth
                return newHealth
            }

            // Try to ping the server
            val pingMs = measurePing()
            val quality = determinePingQuality(pingMs)
            
            val newHealth = ConnectionHealth(
                isConnected = true,
                pingMs = pingMs,
                quality = quality,
                lastChecked = currentTime,
                consecutiveFailures = 0, // Reset on successful check
                uptime = if (startTime > 0) currentTime - startTime else 0
            )
            
            _connectionHealth.value = newHealth
            
            Log.v(TAG, "Health check: ${pingMs}ms, ${getQualityDescription(quality)}")
            return newHealth
            
        } catch (e: Exception) {
            Log.w(TAG, "Health check failed", e)
            
            val newHealth = currentHealth.copy(
                isConnected = false,
                pingMs = -1,
                quality = ConnectionQuality.UNKNOWN,
                lastChecked = currentTime,
                consecutiveFailures = currentHealth.consecutiveFailures + 1,
                uptime = if (startTime > 0) currentTime - startTime else 0
            )
            _connectionHealth.value = newHealth
            return newHealth
        }
    }    private suspend fun measurePing(): Long = withContext(Dispatchers.IO) {
        // For now, use a simplified approach that just measures basic connectivity
        // In a real implementation, you might want to ping a known endpoint
        val startTime = System.currentTimeMillis()
          try {
            // Use a basic connectivity check instead of trying to access server URL
            // This is a simplified approach that measures general network responsiveness
            val testHost = "8.8.8.8" // Google DNS as a connectivity test
            val testPort = 53 // DNS port
            
            Socket().use { socket ->
                socket.connect(InetSocketAddress(testHost, testPort), PING_TIMEOUT_MS)
                val endTime = System.currentTimeMillis()
                endTime - startTime
            }
        } catch (e: Exception) {
            Log.w(TAG, "Network connectivity test failed", e)
            throw e
        }
    }

    private fun determinePingQuality(pingMs: Long): ConnectionQuality {
        return when {
            pingMs < 0 -> ConnectionQuality.UNKNOWN
            pingMs <= GOOD_PING_THRESHOLD_MS -> ConnectionQuality.EXCELLENT
            pingMs <= 250 -> ConnectionQuality.GOOD
            pingMs <= POOR_PING_THRESHOLD_MS -> ConnectionQuality.FAIR
            else -> ConnectionQuality.POOR
        }
    }

    private fun formatUptime(uptimeMs: Long): String {
        val seconds = uptimeMs / 1000
        val minutes = seconds / 60
        val hours = minutes / 60
        
        return when {
            hours > 0 -> "${hours}h ${minutes % 60}m"
            minutes > 0 -> "${minutes}m ${seconds % 60}s"
            else -> "${seconds}s"
        }
    }

    private fun formatLastChecked(timestamp: Long): String {
        val now = System.currentTimeMillis()
        val diffMs = now - timestamp
        val diffSeconds = diffMs / 1000
        
        return when {
            diffSeconds < 60 -> "${diffSeconds}s ago"
            diffSeconds < 3600 -> "${diffSeconds / 60}m ago"
            else -> "${diffSeconds / 3600}h ago"
        }
    }

    /**
     * Check if connection quality is sufficient for streaming
     */
    fun isQualitySufficientForStreaming(): Boolean {
        val health = _connectionHealth.value
        return health.isConnected && health.quality != ConnectionQuality.POOR
    }

    /**
     * Get recommended actions based on connection health
     */
    fun getRecommendedActions(): List<String> {
        val health = _connectionHealth.value
        val actions = mutableListOf<String>()
        
        if (!health.isConnected) {
            actions.add("Check network connection")
            actions.add("Verify server is running")
        } else {
            when (health.quality) {
                ConnectionQuality.POOR -> {
                    actions.add("Poor connection detected")
                    actions.add("Consider reducing video quality")
                    actions.add("Check WiFi signal strength")
                }
                ConnectionQuality.FAIR -> {
                    actions.add("Fair connection quality")
                    actions.add("May experience buffering with high quality streams")
                }
                else -> {
                    // Good connection, no actions needed
                }
            }
        }
        
        if (health.consecutiveFailures > 3) {
            actions.add("Multiple connection failures detected")
            actions.add("Server may be unstable")
        }
        
        return actions
    }

    /**
     * Cleanup resources
     */
    fun cleanup() {
        Log.d(TAG, "Cleaning up connection health monitor")
        stopMonitoring()
        scope.cancel()
    }
}
