plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.example.jellyfinnew"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.jellyfinnew"
        minSdk = 28
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

    }

    signingConfigs {
        create("release") {
            storeFile = file("../keystore/jellyfin-android-tv.jks")
            storePassword = "Sport460"
            keyAlias = "jellyfin-key"
            keyPassword = "Sport460"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("release")
            // Ensure release builds also use the network security config
            manifestPlaceholders["networkSecurityConfig"] = "@xml/network_security_config"
        }
        debug {
            isDebuggable = true
            // Add debug-specific network security config for more permissive SSL
            manifestPlaceholders["networkSecurityConfig"] = "@xml/network_security_config"
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
    }
    lint {
        baseline = file("lint-baseline.xml")
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.tv.foundation)
    implementation(libs.androidx.tv.material)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    
    // Material 3 for regular compose components
    implementation(libs.androidx.material3)
    implementation("androidx.compose.material:material-icons-extended")
    
    // Jellyfin SDK
    implementation(libs.jellyfin.sdk.android)
    
    // Image loading
    implementation(libs.coil.compose)
    implementation(libs.coil.video)
    
    // Media playback
    implementation(libs.androidx.media3.exoplayer)
    implementation(libs.androidx.media3.ui)
    implementation(libs.androidx.media3.session)
    
    // Networking
    implementation(libs.retrofit)
    implementation(libs.retrofit.moshi)
    implementation(libs.okhttp.logging)
    implementation(libs.moshi)
    implementation(libs.moshi.kotlin)
    
    // Coroutines
    implementation(libs.kotlinx.coroutines.android)
    
    // Logging
    implementation(libs.slf4j.android)
    
    // DataStore for persistent storage
    implementation(libs.androidx.datastore.preferences)
    
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}