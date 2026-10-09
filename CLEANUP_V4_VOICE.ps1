# Run from the Android Studio project root with:
# powershell -ExecutionPolicy Bypass -File .\CLEANUP_V4_VOICE.ps1
$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $MyInvocation.MyCommand.Path
$tracking = Join-Path $root 'app\src\main\java\com\nirmalamgroup\nirmalamchant\tracking'
$obsolete = @('LocalChantClassifier.kt', 'PersonalChantMatcher.kt', 'ChantTrackingService.kt', 'VoiceChantDetector.kt')
foreach ($name in $obsolete) {
  $path = Join-Path $tracking $name
  if (Test-Path -LiteralPath $path) {
    Remove-Item -LiteralPath $path -Force
    Write-Host "Removed obsolete V4 file: $name"
  }
}
Write-Host 'Remaining source references to voice classes:'
$src = Join-Path $root 'app\src\main'
Get-ChildItem -LiteralPath $src -Recurse -File -Include '*.kt','*.xml' | Select-String -Pattern 'LocalChantClassifier|PersonalChantMatcher|ChantTrackingService|VoiceChantDetector' | ForEach-Object { Write-Warning "$($_.Path):$($_.LineNumber) $($_.Line.Trim())" }
Write-Host 'Done. Now run Gradle clean and assembleDebug.'
