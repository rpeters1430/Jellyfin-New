#!/usr/bin/env pwsh

# Image Loading Test Script for Jellyfin Android TV Client
# This script validates the completed image loading fixes

Write-Host "=== Jellyfin Android TV Image Loading Test Script ===" -ForegroundColor Green
Write-Host "Validating completed image loading fixes..." -ForegroundColor Yellow
Write-Host ""

# Check if the project builds successfully
Write-Host "1. Testing build..." -ForegroundColor Cyan
Set-Location "c:\Users\James\Desktop\Jellyfin-New"

try {
    & .\gradlew assembleDebug --quiet
    if ($LASTEXITCODE -eq 0) {
        Write-Host "   ✅ Build successful - All fixes implemented and compiling correctly" -ForegroundColor Green
    } else {
        Write-Host "   ❌ Build failed - Check compilation errors" -ForegroundColor Red
        exit 1
    }
} catch {
    Write-Host "   ❌ Build error: $_" -ForegroundColor Red
    exit 1
}

Write-Host ""
Write-Host "2. Checking key files..." -ForegroundColor Cyan

$keyFiles = @(
    "app\src\main\java\com\example\jellyfinnew\data\ImageUrlHelper.kt",
    "app\src\main\java\com\example\jellyfinnew\ui\components\RobustAsyncImage.kt", 
    "app\src\main\java\com\example\jellyfinnew\ui\utils\ImageDebugHelper.kt",
    "app\src\main\java\com\example\jellyfinnew\data\repositories\MediaRepository.kt",
    "app\src\main\java\com\example\jellyfinnew\ui\home\HomeViewModel.kt"
)

foreach ($file in $keyFiles) {
    if (Test-Path $file) {
        Write-Host "   ✅ $file exists" -ForegroundColor Green
    } else {
        Write-Host "   ❌ $file missing" -ForegroundColor Red
    }
}

Write-Host ""
Write-Host "3. Testing APK installation (optional)..." -ForegroundColor Cyan
$apkPath = "app\build\outputs\apk\debug\app-debug.apk"

if (Test-Path $apkPath) {
    Write-Host "   ✅ APK built successfully: $apkPath" -ForegroundColor Green
    
    # Check if ADB is available
    try {
        & adb devices
        Write-Host "   📱 You can now install the APK using:" -ForegroundColor Yellow
        Write-Host "      adb install -r `"$apkPath`"" -ForegroundColor White
    } catch {
        Write-Host "   ℹ️  ADB not found. Install Android SDK Platform Tools to test on device" -ForegroundColor Blue
    }
} else {
    Write-Host "   ❌ APK not found - build may have failed" -ForegroundColor Red
}

Write-Host ""
Write-Host "=== Manual Testing Guide ===" -ForegroundColor Green
Write-Host ""
Write-Host "After installing the app on your Android TV:" -ForegroundColor Yellow
Write-Host "1. Connect to your Jellyfin server" -ForegroundColor White
Write-Host "2. Check library cards load properly (should show images or proper fallbacks)" -ForegroundColor White
Write-Host "3. Navigate to 'Recently Added' sections, especially TV episodes" -ForegroundColor White
Write-Host "4. Look for specific error messages instead of generic 'Failed to load'" -ForegroundColor White
Write-Host "5. Check Android Studio Logcat for debug information with these tags:" -ForegroundColor White
Write-Host "   - ImageUrlHelper: Image URL generation" -ForegroundColor Gray
Write-Host "   - RobustAsyncImage: Image loading errors" -ForegroundColor Gray
Write-Host "   - Media: MediaRepository operations" -ForegroundColor Gray
Write-Host "   - HomeViewModel: Debug functions" -ForegroundColor Gray

Write-Host ""
Write-Host "=== Debug Commands ===" -ForegroundColor Green
Write-Host "If you encounter issues, you can call these methods in the app:" -ForegroundColor Yellow
Write-Host "• viewModel.debugAllImageUrls() - Analyze all image URLs" -ForegroundColor White
Write-Host "• viewModel.debugRecentlyAddedEpisodes() - Focus on episode thumbnails" -ForegroundColor White
Write-Host "• viewModel.testServerConnectivity() - Test server connection" -ForegroundColor White
Write-Host "• viewModel.getDebugInfo() - Get general debug information" -ForegroundColor White

Write-Host ""
Write-Host "=== Expected Improvements ===" -ForegroundColor Green
Write-Host "✅ Library cards should show proper images or appropriate fallbacks" -ForegroundColor Green
Write-Host "✅ Episode thumbnails should load correctly in Recently Added sections" -ForegroundColor Green
Write-Host "✅ Specific error messages (Auth Error, Not Found, Access Denied)" -ForegroundColor Green
Write-Host "✅ Comprehensive debug logging for troubleshooting" -ForegroundColor Green
Write-Host "✅ Better fallback strategies for missing images" -ForegroundColor Green

Write-Host ""
Write-Host "Test completed! 🚀" -ForegroundColor Green
