package com.example.jellyfinnew.data.repositories

import android.util.Log
import org.jellyfin.sdk.api.client.ApiClient
import com.example.jellyfinnew.data.ImageUrlHelper
import com.example.jellyfinnew.data.ErrorHandler
import com.example.jellyfinnew.data.JellyfinConfig

/**
 * Repository responsible for managing media streaming URLs and playback-related functions
 */
class StreamingRepository {
    
    companion object {
        private val TAG = JellyfinConfig.Logging.getTag("Streaming")
    }
    
    /**
     * Get the stream URL for a media item
     */    fun getStreamUrl(itemId: String, imageUrlHelper: ImageUrlHelper?): String? {
        return try {
            val streamUrl = imageUrlHelper?.buildStreamUrl(itemId)
            Log.d(TAG, "Generated stream URL for item $itemId: $streamUrl")
            streamUrl
        } catch (e: Exception) {
            val error = ErrorHandler.handleException(e, "Failed to generate stream URL")
            Log.e(TAG, error.getUserFriendlyMessage(), e)
            null
        }
    }
    
    /**
     * Get the direct playback URL for a media item with optional parameters
     */
    fun getDirectPlayUrl(
        itemId: String, 
        apiClient: ApiClient?,
        maxBitrate: Int? = null,
        audioCodec: String? = null,
        videoCodec: String? = null
    ): String? {
        return try {
            val baseUrl = apiClient?.baseUrl ?: return null
            
            val urlBuilder = StringBuilder("$baseUrl/Videos/$itemId/stream")
            val params = mutableListOf<String>()
            
            // Add optional streaming parameters
            maxBitrate?.let { params.add("maxStreamingBitrate=$it") }
            audioCodec?.let { params.add("audioCodec=$it") }
            videoCodec?.let { params.add("videoCodec=$it") }
            
            // Add default parameters for better compatibility
            params.add("container=mkv,mp4,webm")
            params.add("audioSampleRate=48000")
            params.add("audioBitRate=128000")
            
            if (params.isNotEmpty()) {
                urlBuilder.append("?").append(params.joinToString("&"))
            }
              val directPlayUrl = urlBuilder.toString()
            Log.d(TAG, "Generated direct play URL for item $itemId: $directPlayUrl")
            directPlayUrl
        } catch (e: Exception) {
            val error = ErrorHandler.handleException(e, "Failed to generate direct play URL")
            Log.e(TAG, error.getUserFriendlyMessage(), e)
            null
        }
    }
    
    /**
     * Get transcoding URL for media that needs server-side processing
     */
    fun getTranscodingUrl(
        itemId: String,
        apiClient: ApiClient?,
        maxBitrate: Int = JellyfinConfig.Streaming.DEFAULT_MAX_BITRATE,
        audioCodec: String = JellyfinConfig.Streaming.DEFAULT_AUDIO_CODEC,
        videoCodec: String = JellyfinConfig.Streaming.DEFAULT_VIDEO_CODEC,
        container: String = JellyfinConfig.Streaming.DEFAULT_CONTAINER
    ): String? {
        return try {
            val baseUrl = apiClient?.baseUrl ?: return null
            
            val transcodingUrl = buildString {
                append("$baseUrl/Videos/$itemId/stream")
                append("?container=$container")
                append("&videoCodec=$videoCodec")
                append("&audioCodec=$audioCodec")
                append("&maxStreamingBitrate=$maxBitrate")
                append("&breakOnNonKeyFrames=true")
                append("&transcodingReasons=VideoCodecNotSupported,AudioCodecNotSupported")            }
            
            Log.d(TAG, "Generated transcoding URL for item $itemId: $transcodingUrl")
            transcodingUrl
        } catch (e: Exception) {
            val error = ErrorHandler.handleException(e, "Failed to generate transcoding URL")
            Log.e(TAG, error.getUserFriendlyMessage(), e)
            null
        }
    }
    
    /**
     * Get HLS (HTTP Live Streaming) URL for adaptive streaming
     */
    fun getHlsUrl(
        itemId: String,
        apiClient: ApiClient?,
        maxBitrate: Int = JellyfinConfig.Streaming.DEFAULT_MAX_BITRATE
    ): String? {
        return try {
            val baseUrl = apiClient?.baseUrl ?: return null
            
            val hlsUrl = buildString {
                append("$baseUrl/Videos/$itemId/stream.m3u8")
                append("?container=ts")
                append("&videoCodec=h264")
                append("&audioCodec=aac")
                append("&maxStreamingBitrate=$maxBitrate")
                append("&segmentLength=6")
                append("&breakOnNonKeyFrames=true")
            }
            
            Log.d(TAG, "Generated HLS URL for item $itemId: $hlsUrl")
            hlsUrl        } catch (e: Exception) {
            val error = ErrorHandler.handleException(e, "Failed to generate HLS URL")
            Log.e(TAG, error.getUserFriendlyMessage(), e)
            null
        }
    }
    
    /**
     * Get subtitle URL for a media item
     */
    fun getSubtitleUrl(
        itemId: String,
        apiClient: ApiClient?,
        subtitleStreamIndex: Int,
        format: String = "vtt"
    ): String? {
        return try {
            val baseUrl = apiClient?.baseUrl ?: return null
            
            val subtitleUrl = "$baseUrl/Videos/$itemId/$subtitleStreamIndex/Subtitles.$format"
            Log.d(TAG, "Generated subtitle URL for item $itemId, stream $subtitleStreamIndex: $subtitleUrl")
            subtitleUrl
        } catch (e: Exception) {            val error = ErrorHandler.handleException(e, "Failed to generate subtitle URL")
            Log.e(TAG, error.getUserFriendlyMessage(), e)
            null
        }
    }
    
    /**
     * Check if direct play is supported for a media item
     * This is a simple heuristic - in practice you'd check media info
     */
    fun supportsDirectPlay(
        container: String?,
        videoCodec: String?,
        audioCodec: String?
    ): Boolean {
        return try {
            val supportedContainers = JellyfinConfig.Streaming.SUPPORTED_CONTAINERS
            val supportedVideoCodecs = JellyfinConfig.Streaming.SUPPORTED_VIDEO_CODECS
            val supportedAudioCodecs = JellyfinConfig.Streaming.SUPPORTED_AUDIO_CODECS
            
            val containerSupported = container?.lowercase() in supportedContainers
            val videoSupported = videoCodec?.lowercase() in supportedVideoCodecs
            val audioSupported = audioCodec?.lowercase() in supportedAudioCodecs
            
            val directPlaySupported = containerSupported && videoSupported && audioSupported
            
            Log.d(TAG, "Direct play check - Container: $container ($containerSupported), " +
                    "Video: $videoCodec ($videoSupported), Audio: $audioCodec ($audioSupported), " +
                    "Result: $directPlaySupported")
            
            directPlaySupported
        } catch (e: Exception) {
            Log.w(TAG, "Error checking direct play support, defaulting to false", e)
            false
        }
    }
}
