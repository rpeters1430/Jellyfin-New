package com.example.jellyfinnew

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.navigation.compose.rememberNavController
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Surface
import com.example.jellyfinnew.navigation.JellyfinNavigation
import com.example.jellyfinnew.ui.theme.JellyfinNewTheme

class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalTvMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            JellyfinNewTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    shape = RectangleShape
                ) {
                    JellyfinApp()
                }
            }
        }
    }
}

@Composable
fun JellyfinApp() {
    val navController = rememberNavController()
    
    JellyfinNavigation(
        navController = navController,
        modifier = Modifier.fillMaxSize()
    )
}