<#
.SYNOPSIS
    Build, install, and launch "nono app" on an Android TV device over Wi-Fi via ADB.

.DESCRIPTION
    - Connects to the TV using ADB over TCP/IP (default port 5555).
    - Waits for the device to become authorized (accept the prompt on the TV if it appears).
    - Builds a debug APK with Gradle if one isn't present.
    - Installs/updates the APK and launches the main activity.

.PARAMETER TvIp
    IP address of the Android TV (e.g. 192.168.1.50). Prompted for if omitted.

.PARAMETER Port
    ADB TCP port. Defaults to 5555.

.EXAMPLE
    .\deploy-tv.ps1 -TvIp "192.168.1.50"

.EXAMPLE
    .\deploy-tv.ps1
    (prompts for the TV IP interactively)
#>

[CmdletBinding()]
param(
    [string]$TvIp,
    [int]$Port = 5555
)

# Stop on the first unhandled error.
$ErrorActionPreference = "Stop"

# Resolve paths relative to this script so it works from any working directory.
$ProjectRoot = $PSScriptRoot
$ApkRelPath  = "app\build\outputs\apk\debug\app-debug.apk"
$ApkPath     = Join-Path $ProjectRoot $ApkRelPath
$GradlewPath = Join-Path $ProjectRoot "gradlew.bat"
$AppId       = "com.nono.iptv"
$Activity    = "$AppId/.MainActivity"

function Write-Step($msg)  { Write-Host "`n==> $msg" -ForegroundColor Cyan }
function Write-Ok($msg)    { Write-Host "    $msg"   -ForegroundColor Green }
function Write-WarnMsg($m) { Write-Host "    $m"     -ForegroundColor Yellow }
function Fail($msg) {
    Write-Host "`nERROR: $msg" -ForegroundColor Red
    exit 1
}

# ---------------------------------------------------------------------------
# 1. Verify ADB is available on PATH.
# ---------------------------------------------------------------------------
Write-Step "Checking for ADB in PATH"
$adb = Get-Command adb -ErrorAction SilentlyContinue
if (-not $adb) {
    Fail @"
ADB was not found on your PATH.
Install the Android SDK platform-tools and add it to PATH, e.g.:
  https://developer.android.com/tools/releases/platform-tools
Then reopen PowerShell and run this script again.
"@
}
Write-Ok "Found adb at $($adb.Source)"

# ---------------------------------------------------------------------------
# 2. Resolve the TV IP (parameter or interactive prompt).
# ---------------------------------------------------------------------------
if ([string]::IsNullOrWhiteSpace($TvIp)) {
    $TvIp = Read-Host "Enter the Android TV IP address (e.g. 192.168.1.50)"
}
if ([string]::IsNullOrWhiteSpace($TvIp)) {
    Fail "No TV IP address provided."
}
$Target = "{0}:{1}" -f $TvIp, $Port
Write-Ok "Target device: $Target"

# ---------------------------------------------------------------------------
# 3. Connect to the TV over Wi-Fi.
# ---------------------------------------------------------------------------
Write-Step "Connecting to $Target"
adb connect $Target | Write-Host

# ---------------------------------------------------------------------------
# 4. Wait for the device to become authorized.
#    The TV shows an "Allow USB/network debugging?" prompt the first time;
#    poll until it reports 'device' (authorized) rather than 'unauthorized'/'offline'.
# ---------------------------------------------------------------------------
Write-Step "Waiting for device authorization"
$maxAttempts = 30          # ~60 seconds total
$attempt     = 0
$state       = ""
do {
    $attempt++
    Start-Sleep -Seconds 2

    # Find the status column for our target in `adb devices`.
    $line = (adb devices | Select-String -SimpleMatch $Target | Select-Object -First 1)
    if ($line) {
        $parts = ($line.ToString() -split "\s+")
        $state = $parts[-1]
    } else {
        $state = "missing"
    }

    switch ($state) {
        "device"       { Write-Ok "Device authorized and online." }
        "unauthorized" { Write-WarnMsg "Check the TV screen and tap 'Allow' on the debugging prompt... (attempt $attempt/$maxAttempts)" }
        "offline"      { Write-WarnMsg "Device offline, retrying... (attempt $attempt/$maxAttempts)" }
        default        { Write-WarnMsg "Waiting for device... (attempt $attempt/$maxAttempts)" }
    }
} while ($state -ne "device" -and $attempt -lt $maxAttempts)

if ($state -ne "device") {
    Fail @"
Could not get an authorized connection to $Target (last state: '$state').
Tips:
  - Enable 'Network debugging' / 'USB debugging' under Developer options on the TV.
  - Make sure the TV and this PC are on the same network.
  - Accept the 'Allow debugging' prompt on the TV, then re-run.
"@
}

# ---------------------------------------------------------------------------
# 5. Ensure the debug APK exists; build it with Gradle if not.
# ---------------------------------------------------------------------------
Write-Step "Checking for debug APK"
if (-not (Test-Path $ApkPath)) {
    Write-WarnMsg "APK not found at $ApkRelPath — building with gradlew assembleDebug..."
    if (-not (Test-Path $GradlewPath)) {
        Fail "gradlew.bat not found at $GradlewPath. Run this script from the project root."
    }
    Push-Location $ProjectRoot
    try {
        & $GradlewPath "assembleDebug"
        if ($LASTEXITCODE -ne 0) { Fail "Gradle build failed (exit code $LASTEXITCODE)." }
    } finally {
        Pop-Location
    }
    if (-not (Test-Path $ApkPath)) {
        Fail "Build finished but APK still not found at $ApkRelPath."
    }
    Write-Ok "Build complete."
} else {
    Write-Ok "Found APK at $ApkRelPath"
}

# ---------------------------------------------------------------------------
# 6. Install (or reinstall / downgrade) the APK on the TV.
#    -r keeps app data; -d allows version downgrade.
# ---------------------------------------------------------------------------
Write-Step "Installing APK on $Target"
adb -s $Target install -r -d "$ApkPath" | Write-Host
if ($LASTEXITCODE -ne 0) { Fail "adb install failed (exit code $LASTEXITCODE)." }
Write-Ok "Install successful."

# ---------------------------------------------------------------------------
# 7. Launch the main activity on the TV.
# ---------------------------------------------------------------------------
Write-Step "Launching $Activity"
adb -s $Target shell am start -n $Activity | Write-Host
if ($LASTEXITCODE -ne 0) { Fail "Failed to launch activity (exit code $LASTEXITCODE)." }

Write-Host "`nnono app deployed and launched on $Target." -ForegroundColor Green
