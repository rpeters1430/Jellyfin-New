package com.example.jellyfinnew.ui.theme

import androidx.compose.ui.graphics.Color

// Base Seed Colors
val SeedPrimaryDark = Color(0xFF3D2C8D)       // Dark Purple for Primary
val SeedSecondaryDark = Color(0xFF1B5E20)     // Dark Green for Secondary
val SeedTertiaryDark = Color(0xFF2A52BE)      // Deep Blue for Tertiary

val SeedPrimaryLight = Color(0xFF5D3FD3)      // Lighter Purple for Primary (Light Theme)
val SeedSecondaryLight = Color(0xFF2E7D32)    // Lighter Green for Secondary (Light Theme)
val SeedTertiaryLight = Color(0xFF3F51B5)     // Brighter Blue for Tertiary (Light Theme)

// Dark Theme Color Palette
val DarkPrimary = SeedPrimaryDark                 // 0xFF3D2C8D
val DarkOnPrimary = Color(0xFFFFFFFF)
val DarkPrimaryContainer = Color(0xFF2A1F68)      // Tonal variant from Primary
val DarkOnPrimaryContainer = Color(0xFFD6CFFF)    // Light text for contrast
val DarkSecondary = SeedSecondaryDark             // 0xFF1B5E20
val DarkOnSecondary = Color(0xFFFFFFFF)
val DarkSecondaryContainer = Color(0xFF123F16)    // Tonal variant from Secondary
val DarkOnSecondaryContainer = Color(0xFFB8F5BB)  // Light text for contrast
val DarkTertiary = SeedTertiaryDark               // 0xFF2A52BE
val DarkOnTertiary = Color(0xFFFFFFFF)
val DarkTertiaryContainer = Color(0xFF1A3A8A)     // Tonal variant from Tertiary
val DarkOnTertiaryContainer = Color(0xFFD1E0FF)   // Light text for contrast
val DarkError = Color(0xFFFFB4AB)                 // Standard M3 error (light tone for dark theme)
val DarkOnError = Color(0xFF690005)               // Dark text on light error color
val DarkErrorContainer = Color(0xFF93000A)        // Darker error container
val DarkOnErrorContainer = Color(0xFFFFDAD6)      // Light text on dark error container
val DarkBackground = Color(0xFF101014)            // Very dark, slightly blueish-purple
val DarkOnBackground = Color(0xFFEAEAEA)          // Off-white for text/icons
val DarkSurface = Color(0xFF18181C)               // Slightly lighter than background
val DarkOnSurface = Color(0xFFEAEAEA)             // Off-white for text/icons
val DarkSurfaceVariant = Color(0xFF2A2930)        // More distinct variant for cards, etc.
val DarkOnSurfaceVariant = Color(0xFFCAC4D0)      // Muted text/icons
val DarkOutline = Color(0xFF8A8693)               // Visible outline for components
val DarkOutlineVariant = Color(0xFF49454F)        // Subtler outline (e.g., dividers)
val DarkScrim = Color(0xE6000000)                 // High alpha for TV scrim/overlays
val DarkInversePrimary = SeedPrimaryLight         // 0xFF5D3FD3
val DarkInverseSurface = Color(0xFFEAE0F5)        // Light, slightly purplish surface
val DarkInverseOnSurface = Color(0xFF313033)      // Dark text for inverse surface

// Light Theme Color Palette
val LightPrimary = SeedPrimaryLight               // 0xFF5D3FD3
val LightOnPrimary = Color(0xFFFFFFFF)
val LightPrimaryContainer = Color(0xFFEADDFF)     // Light purple container
val LightOnPrimaryContainer = Color(0xFF21005D)   // Dark text for contrast
val LightSecondary = SeedSecondaryLight           // 0xFF2E7D32
val LightOnSecondary = Color(0xFFFFFFFF)
val LightSecondaryContainer = Color(0xFFB4F3AE)   // Light green container
val LightOnSecondaryContainer = Color(0xFF002105) // Dark text for contrast
val LightTertiary = SeedTertiaryLight             // 0xFF3F51B5
val LightOnTertiary = Color(0xFFFFFFFF)
val LightTertiaryContainer = Color(0xFFDCE1FF)    // Light blue container
val LightOnTertiaryContainer = Color(0xFF001849)  // Dark text for contrast
val LightError = Color(0xFFBA1A1A)                // Standard M3 error
val LightOnError = Color(0xFFFFFFFF)
val LightErrorContainer = Color(0xFFFFDAD6)       // Light error container
val LightOnErrorContainer = Color(0xFF410002)     // Dark text on error container
val LightBackground = Color(0xFFFEFBFF)           // Very light, almost white (neutral)
val LightOnBackground = Color(0xFF1C1B1F)         // Standard dark text
val LightSurface = Color(0xFFFEFBFF)              // Same as background for light theme
val LightOnSurface = Color(0xFF1C1B1F)            // Standard dark text
val LightSurfaceVariant = Color(0xFFE7E0EC)       // Slightly off-white/grey for variants
val LightOnSurfaceVariant = Color(0xFF49454F)     // Muted dark text
val LightOutline = Color(0xFF79747E)              // Standard outline
val LightOutlineVariant = Color(0xFFCAC4D0)       // Lighter outline variant (e.g., dividers)
val LightScrim = Color(0x99000000)                // Standard scrim alpha
val LightInversePrimary = SeedPrimaryDark         // 0xFF3D2C8D
val LightInverseSurface = Color(0xFF313033)       // Dark surface for inverse elements
val LightInverseOnSurface = Color(0xFFF2EFF4)     // Light text for inverse dark surface
