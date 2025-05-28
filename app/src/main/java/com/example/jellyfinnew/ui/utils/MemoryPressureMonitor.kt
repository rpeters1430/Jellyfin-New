package com.example.jellyfinnew.ui.utils

import android.app.ActivityManager
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.example.jellyfinnew.data.JellyfinConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Monitors memory pressure and helps prevent crashes on older Android TV devices
 */
class MemoryPressureMonitor(private val context: Context) {
    
    companion object {
        private const val TAG = "MemoryPressureMonitor"
        private const val LOW_MEMORY_THRESHOLD_MB = 100
        private const val CRITICAL_MEMORY_THRESHOLD_MB = 50
    }

    data class MemoryInfo(
        val availableMemoryMB: Int,
        val totalMemoryMB: Int,
        val usedMemoryMB: Int,
        val usagePercentage: Float,
        val isLowMemory: Boolean,
        val isCriticalMemory: Boolean
    )

    sealed class MemoryPressureLevel {
        object Normal : MemoryPressureLevel()
        object Low : MemoryPressureLevel()
        object Critical : MemoryPressureLevel()
    }

    private val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
    private val handler = Handler(Looper.getMainLooper())
    private var monitoringRunnable: Runnable? = null

    private val _memoryInfo = MutableStateFlow(
        MemoryInfo(
            availableMemoryMB = 0,
            totalMemoryMB = 0,
            usedMemoryMB = 0,
            usagePercentage = 0f,
            isLowMemory = false,
            isCriticalMemory = false
        )
    )
    val memoryInfo: StateFlow<MemoryInfo> = _memoryInfo.asStateFlow()

    private val _pressureLevel = MutableStateFlow<MemoryPressureLevel>(MemoryPressureLevel.Normal)
    val pressureLevel: StateFlow<MemoryPressureLevel> = _pressureLevel.asStateFlow()

    private val _cleanupRequests = MutableStateFlow(0)
    val cleanupRequests: StateFlow<Int> = _cleanupRequests.asStateFlow()

    private var isMonitoring = false
    private var cleanupCallbacks = mutableListOf<() -> Unit>()

    /**
     * Start monitoring memory pressure
     */
    fun startMonitoring() {
        if (isMonitoring) return
        
        Log.d(TAG, "Starting memory pressure monitoring")
        isMonitoring = true
        
        monitoringRunnable = object : Runnable {
            override fun run() {
                updateMemoryInfo()
                
                // Schedule next check
                handler.postDelayed(this, JellyfinConfig.Performance.MEMORY_CLEANUP_INTERVAL_MS)
            }
        }
        
        monitoringRunnable?.let { handler.post(it) }
    }

    /**
     * Stop monitoring memory pressure
     */
    fun stopMonitoring() {
        if (!isMonitoring) return
        
        Log.d(TAG, "Stopping memory pressure monitoring")
        isMonitoring = false
        
        monitoringRunnable?.let { handler.removeCallbacks(it) }
        monitoringRunnable = null
    }

    /**
     * Register a callback to be called when memory cleanup is needed
     */
    fun registerCleanupCallback(callback: () -> Unit) {
        cleanupCallbacks.add(callback)
        Log.d(TAG, "Registered cleanup callback, total: ${cleanupCallbacks.size}")
    }

    /**
     * Force a memory cleanup
     */
    fun forceCleanup() {
        Log.i(TAG, "Force cleanup requested")
        triggerCleanup()
    }

    /**
     * Get current memory usage as a percentage
     */
    fun getMemoryUsagePercentage(): Float {
        return _memoryInfo.value.usagePercentage
    }

    /**
     * Check if the device is in low memory state
     */
    fun isLowMemory(): Boolean {
        return _memoryInfo.value.isLowMemory
    }

    private fun updateMemoryInfo() {
        try {
            val memInfo = ActivityManager.MemoryInfo()
            activityManager.getMemoryInfo(memInfo)
            
            val availableMemoryMB = (memInfo.availMem / (1024 * 1024)).toInt()
            val totalMemoryMB = (memInfo.totalMem / (1024 * 1024)).toInt()
            val usedMemoryMB = totalMemoryMB - availableMemoryMB
            val usagePercentage = (usedMemoryMB.toFloat() / totalMemoryMB.toFloat()) * 100f
            
            val isLowMemory = availableMemoryMB <= LOW_MEMORY_THRESHOLD_MB || memInfo.lowMemory
            val isCriticalMemory = availableMemoryMB <= CRITICAL_MEMORY_THRESHOLD_MB
            
            val newMemoryInfo = MemoryInfo(
                availableMemoryMB = availableMemoryMB,
                totalMemoryMB = totalMemoryMB,
                usedMemoryMB = usedMemoryMB,
                usagePercentage = usagePercentage,
                isLowMemory = isLowMemory,
                isCriticalMemory = isCriticalMemory
            )
            
            _memoryInfo.value = newMemoryInfo
            
            // Update pressure level and trigger cleanup if needed
            val newPressureLevel = when {
                isCriticalMemory -> MemoryPressureLevel.Critical
                isLowMemory -> MemoryPressureLevel.Low
                else -> MemoryPressureLevel.Normal
            }
            
            if (newPressureLevel != _pressureLevel.value) {
                _pressureLevel.value = newPressureLevel
                
                when (newPressureLevel) {
                    is MemoryPressureLevel.Critical -> {
                        Log.w(TAG, "Critical memory pressure detected: ${availableMemoryMB}MB available")
                        triggerCleanup()
                    }
                    is MemoryPressureLevel.Low -> {
                        Log.w(TAG, "Low memory pressure detected: ${availableMemoryMB}MB available")
                        triggerCleanup()
                    }
                    is MemoryPressureLevel.Normal -> {
                        Log.d(TAG, "Memory pressure normalized: ${availableMemoryMB}MB available")
                    }
                }
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error updating memory info", e)
        }
    }

    private fun triggerCleanup() {
        Log.i(TAG, "Triggering memory cleanup, ${cleanupCallbacks.size} callbacks registered")
        
        // Increment cleanup request counter
        _cleanupRequests.value += 1
        
        // Call all registered cleanup callbacks
        cleanupCallbacks.forEach { callback ->
            try {
                callback()
            } catch (e: Exception) {
                Log.e(TAG, "Error in cleanup callback", e)
            }
        }
        
        // Force garbage collection
        System.gc()
        
        Log.i(TAG, "Memory cleanup completed")
    }
}