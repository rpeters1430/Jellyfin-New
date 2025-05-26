# Jellyfin Android TV Client

A modern Android TV application built with Jetpack Compose for TV that connects to your Jellyfin media server to browse and play your media collection.

## Features

- 🔐 **Server Authentication**: Connect to your Jellyfin server with username/password
- 📚 **Library Browsing**: Browse your media libraries (Movies, TV Shows, Music, etc.)
- 🎬 **Media Playback**: Stream media directly from your Jellyfin server
- 📱 **TV Optimized**: Built specifically for Android TV with D-pad navigation
- 🎨 **Modern UI**: Clean, Material Design interface optimized for large screens
- ⏯️ **Playback Controls**: Full media controls with seek, play/pause functionality
- 📊 **Progress Tracking**: Visual progress indicators for partially watched content

## Technology Stack

- **Android TV** - Target platform
- **Jetpack Compose for TV** - Modern declarative UI framework
- **Jellyfin Kotlin SDK** - Official SDK for Jellyfin server communication
- **ExoPlayer (Media3)** - Advanced media playback
- **Coil** - Efficient image loading with caching
- **Navigation Compose** - Type-safe navigation
- **Kotlin Coroutines** - Asynchronous programming

## Requirements

- Android TV device with API level 28+ (Android 9.0)
- Jellyfin media server (version 10.8.0 or later recommended)
- Network connectivity between Android TV and Jellyfin server

## Setup & Installation

### Prerequisites

1. **Jellyfin Server**: Ensure you have a Jellyfin server running and accessible on your network
2. **Android Studio**: For building and deploying the app
3. **Android TV Device or Emulator**: For testing

### Building the App

1. Clone this repository:
   ```bash
   git clone <repository-url>
   cd Jellyfin-New
   ```

2. Open the project in Android Studio

3. Sync the project and resolve dependencies

4. Build and run on your Android TV device or emulator

### Configuration

1. Launch the app on your Android TV
2. Enter your Jellyfin server details:
   - **Server URL**: Your Jellyfin server address (e.g., `http://192.168.1.100:8096`)
   - **Username**: Your Jellyfin username
   - **Password**: Your Jellyfin password
3. Connect and start browsing your media!

## Project Structure

```
app/src/main/java/com/example/jellyfinnew/
├── data/
│   └── JellyfinRepository.kt          # Data layer for API communication
├── di/
│   └── ServiceLocator.kt              # Simple dependency injection
├── navigation/
│   ├── Screen.kt                      # Navigation routes
│   └── JellyfinNavigation.kt          # Navigation setup
├── ui/
│   ├── home/
│   │   ├── HomeScreen.kt              # Media library browsing
│   │   └── HomeViewModel.kt           # Home screen state management
│   ├── login/
│   │   ├── LoginScreen.kt             # Server connection screen
│   │   └── LoginViewModel.kt          # Login state management
│   ├── player/
│   │   └── PlayerScreen.kt            # Media playback screen
│   └── theme/                         # App theming
└── MainActivity.kt                    # App entry point
```

## Usage

### Connecting to Jellyfin Server

1. Open the app
2. Enter your server URL (include `http://` or `https://`)
3. Provide your username and password
4. Tap "Connect"

### Browsing Media

- Navigate through your media libraries using the D-pad
- Select a library to view its contents
- Browse movies, TV shows, and other media types
- Use the back button to navigate up in the hierarchy

### Playing Media

- Select a movie or episode to start playback
- Use the media controls during playback:
  - **Play/Pause**: Center button
  - **Seek Backward**: ⏪ 10s button
  - **Seek Forward**: 30s ⏩ button
  - **Back**: Return to library

## Development

### Architecture

The app follows MVVM (Model-View-ViewModel) architecture:

- **Models**: Data classes representing media items and connection state
- **Views**: Composable screens optimized for TV
- **ViewModels**: State management and business logic
- **Repository**: Abstracts API communication with Jellyfin server

### Key Dependencies

```kotlin
// Jetpack Compose for TV
implementation "androidx.tv:tv-foundation:1.0.0-alpha12"
implementation "androidx.tv:tv-material:1.0.0"

// Jellyfin SDK
implementation "org.jellyfin.sdk:jellyfin-core:1.4.7"

// Media Playback
implementation "androidx.media3:media3-exoplayer:1.4.1"
implementation "androidx.media3:media3-ui:1.4.1"

// Image Loading
implementation "io.coil-kt:coil-compose:2.7.0"
```

### Building for TV

This app is specifically designed for Android TV and includes:

- TV-specific manifest declarations
- Leanback launcher support
- D-pad navigation optimization
- Landscape orientation lock
- TV-optimized UI components

## Troubleshooting

### Connection Issues

- Verify your Jellyfin server is running and accessible
- Check network connectivity between Android TV and server
- Ensure the server URL includes the protocol (`http://` or `https://`)
- Try accessing the server from a web browser on the same network

### Playback Issues

- Verify media files are accessible on the Jellyfin server
- Check server transcoding settings if direct play fails
- Ensure sufficient network bandwidth for streaming

### Performance

- Images are cached automatically by Coil
- Media libraries are loaded on-demand
- Use a wired connection for best streaming performance

## Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Test on Android TV
5. Submit a pull request

## License

This project is licensed under the MIT License - see the LICENSE file for details.

## Acknowledgments

- [Jellyfin](https://jellyfin.org/) - The amazing open-source media server
- [Jetpack Compose for TV](https://developer.android.com/jetpack/compose/tv) - Modern Android TV development
- [ExoPlayer](https://exoplayer.dev/) - Advanced media playback for Android
