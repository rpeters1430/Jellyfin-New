package com.example.jellyfinnew.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes // Ensure this is the M3 import, not androidx.compose.material.Shapes
import androidx.compose.ui.unit.dp

val JellyfinShapes = Shapes(
    small = RoundedCornerShape(8.dp),   // Slightly more rounded than M3 default (4.dp)
    medium = RoundedCornerShape(12.dp), // More rounded than M3 default (8.dp), good for cards on TV
    large = RoundedCornerShape(16.dp)   // Good for larger containers or dialogs on TV
)
