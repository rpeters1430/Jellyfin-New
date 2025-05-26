<!-- Use this file to provide workspace-specific custom instructions to Copilot. For more details, visit https://code.visualstudio.com/docs/copilot/copilot-customization#_use-a-githubcopilotinstructionsmd-file -->

# Jellyfin Android TV Client

This is an Android TV application built with Jetpack Compose for TV that connects to a Jellyfin media server to browse and play media content.

## Technology Stack
- Android TV with Jetpack Compose for TV
- Jellyfin Kotlin SDK for server communication
- ExoPlayer (Media3) for video playback
- Coil for image loading
- Navigation Compose for screen navigation
- Kotlin Coroutines for asynchronous operations

## Architecture
- MVVM pattern with ViewModels
- Repository pattern for data management
- Simple service locator for dependency injection
- State management with StateFlow and Compose

## Key Components
- **LoginScreen**: Handles server connection and authentication
- **HomeScreen**: Displays media libraries and content browsing
- **PlayerScreen**: Media playback with ExoPlayer
- **JellyfinRepository**: Manages API communication with Jellyfin server
- **Navigation**: Compose Navigation for screen transitions

## Development Guidelines
- Use TV-optimized Compose components from androidx.tv.material3
- Focus on D-pad navigation and TV UX patterns
- Handle focus management properly for TV interfaces
- Use AsyncImage for loading server images with proper error handling
- Implement proper lifecycle management for media players
- Follow Material Design guidelines adapted for TV

## API Integration
- Uses Jellyfin SDK for authentication and media retrieval
- Supports browsing libraries, movies, TV shows, and other media types
- Handles streaming URLs for direct media playback
- Manages user data like watch progress and play counts
