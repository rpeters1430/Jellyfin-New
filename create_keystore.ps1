# PowerShell script to create Android keystore for signing APKs
# Run this script to create a keystore for signing release APKs

Write-Host "Creating Android Keystore for Jellyfin Android TV App..." -ForegroundColor Green

# Create keystore directory if it doesn't exist
$keystoreDir = ".\keystore"
if (!(Test-Path $keystoreDir)) {
    New-Item -ItemType Directory -Path $keystoreDir
}

# Generate keystore
$keystorePath = ".\keystore\jellyfin-android-tv.jks"
$alias = "jellyfin-key"

Write-Host "Generating keystore at: $keystorePath" -ForegroundColor Yellow
Write-Host "You will be prompted for keystore password and certificate details..." -ForegroundColor Yellow

keytool -genkey -v -keystore $keystorePath -alias $alias -keyalg RSA -keysize 2048 -validity 10000

if ($LASTEXITCODE -eq 0) {
    Write-Host "`nKeystore created successfully!" -ForegroundColor Green
    Write-Host "Location: $keystorePath" -ForegroundColor Green
    Write-Host "Alias: $alias" -ForegroundColor Green
    Write-Host "`nNext steps:" -ForegroundColor Cyan
    Write-Host "1. Update app/build.gradle.kts with signing configuration" -ForegroundColor White
    Write-Host "2. Run './gradlew assembleRelease' to build signed APK" -ForegroundColor White
} else {
    Write-Host "Failed to create keystore. Make sure Java keytool is in your PATH." -ForegroundColor Red
}
