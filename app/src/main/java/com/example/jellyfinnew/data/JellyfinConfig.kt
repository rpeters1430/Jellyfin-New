package com.example.jellyfinnew.data

/**
 * Centralized configuration for the Jellyfin Android TV app
 */
object JellyfinConfig {
    
    // Client Information
    const val CLIENT_NAME = "Jellyfin Android TV"
    const val CLIENT_VERSION = "1.0.0"
    
    // Image Configuration
    object Images {
        const val DEFAULT_QUALITY = 85
        const val POSTER_WIDTH = 400
        const val POSTER_HEIGHT = 600
        const val BACKDROP_WIDTH = 1920
        const val BACKDROP_HEIGHT = 1080
        const val THUMB_WIDTH = 800
        const val THUMB_HEIGHT = 450
        const val LIBRARY_BACKDROP_WIDTH = 1280
        const val LIBRARY_BACKDROP_HEIGHT = 720
    }
    
    // API Configuration
    object Api {
        const val DEFAULT_TIMEOUT_MS = 30_000L
        const val RETRY_DELAY_MS = 1_000L
        const val MAX_RETRIES = 3
    }
    
    // UI Configuration
    object UI {
        const val LIST_SIZE_LIMIT = 200
        const val FEATURED_ITEMS_LIMIT = 10
        const val RECENTLY_ADDED_LIMIT = 10
        const val AUTO_ROTATE_INTERVAL_MS = 20_000L
        const val DEBOUNCE_DELAY_MS = 300L
        const val FOCUS_DEBOUNCE_MS = 100L
        
        // Navigation
        const val BACK_GESTURE_THRESHOLD = 100f
        
        // Animation
        const val ANIMATION_DURATION_MS = 150
        const val FOCUS_SCALE_FACTOR = 1.1f
        const val SELECTED_SCALE_FACTOR = 1.05f
    }
      // Performance Configuration
    object Performance {
        const val IMAGE_CACHE_SIZE_MB = 200
        const val MEMORY_CACHE_PERCENT = 0.30
        const val MEMORY_CLEANUP_INTERVAL_MS = 30_000L
        const val GC_THRESHOLD_MB = 50
        
        // Image loading optimizations
        const val ENABLE_HARDWARE_ACCELERATION = true
        const val PRELOAD_ADJACENT_IMAGES = true
        const val MAX_PRELOAD_COUNT = 10
    }
    
    // Logging Configuration
    object Logging {
        const val ENABLE_DEBUG_LOGS = false
        const val ENABLE_PERFORMANCE_LOGS = false
        const val LOG_TAG_PREFIX = "JellyfinTV"
        
        fun getTag(component: String) = "${LOG_TAG_PREFIX}_$component"
    }
    
    // Feature Flags
    object Features {
        const val ENABLE_AUTO_ROTATION = true
        const val ENABLE_BACKGROUND_IMAGES = true
        const val ENABLE_IMAGE_CACHING = true
        const val ENABLE_OFFLINE_MODE = false
        const val ENABLE_EXPERIMENTAL_UI = false
    }
    
    // Network Security
    object Security {
        const val ALLOW_SELF_SIGNED_CERTIFICATES = true
        const val ENABLE_CERTIFICATE_PINNING = false
        const val FORCE_HTTPS = false
    }
    
    // Streaming Configuration
    object Streaming {
        const val DEFAULT_MAX_BITRATE = 20_000_000 // 20 Mbps
        const val DEFAULT_AUDIO_CODEC = "aac"
        const val DEFAULT_VIDEO_CODEC = "h264"
        const val DEFAULT_CONTAINER = "mkv"
        
        val SUPPORTED_CONTAINERS = setOf("mp4", "mkv", "webm", "avi", "mov")
        val SUPPORTED_VIDEO_CODECS = setOf("h264", "h265", "vp8", "vp9", "av1")
        val SUPPORTED_AUDIO_CODECS = setOf("aac", "mp3", "opus", "vorbis", "ac3", "eac3")
        
        const val HLS_SEGMENT_LENGTH = 6
        const val TRANSCODING_TIMEOUT_MS = 60_000L
    }
}
