param([string]$Serial, [string]$AdbPath)
$ErrorActionPreference = 'Stop'
$state = Get-Content (Join-Path $PSScriptRoot 'restore-state.json') -Raw | ConvertFrom-Json
if (!$Serial) { $Serial = $state.serial }
if (!$AdbPath) { $AdbPath = $state.adb }
function Invoke-Adb([string[]]$Arguments) {
    $output = & $AdbPath -s $Serial @Arguments 2>&1
    $output | Write-Host
    if ($LASTEXITCODE -ne 0 -or ($output -join "`n") -match 'Error:|Exception|SecurityException') { throw 'ADB operation failed. Restore stopped; launcher remains available.' }
}
Invoke-Adb @('get-state')
# Remove only the helper setting we added, restoring both original secure values.
foreach ($key in @('enabled_accessibility_services','accessibility_enabled')) {
    $value=$state.accessibility.$key
    if ($value -eq 'null' -or $null -eq $value) {
        Invoke-Adb @('shell','settings','delete','secure',$key)
    } else {
        Invoke-Adb @('shell','settings','put','secure',$key,[string]$value)
    }
}
# Restore the old home first. Do not disable Serene until the old launcher works.
$old = $state.packages | Where-Object package -eq 'com.ooredoo.tv.stb'
$commands = @{'0'='default-state'; '1'='enable'; '2'='disable'; '3'='disable-user'; '4'='disable-until-used'}
function Restore-Entry($entry) {
    $dump=(& $AdbPath -s $Serial shell dumpsys package $entry.package) -join "`n"
    if ($dump -notmatch '(?m)^\s+User 0: ceDataInode=.*enabled=(\d+)') { throw "Cannot read state: $($entry.package)" }
    if ([int]$Matches[1] -eq [int]$entry.enabled) { Write-Host "Already original: $($entry.package)"; return }
    Invoke-Adb @('shell','pm',$commands[[string]$entry.enabled],'--user','0',$entry.package)
}
Restore-Entry $old
Invoke-Adb @('shell','cmd','package','set-home-activity','--user','0',$state.originalHome)
Invoke-Adb @('shell','am','start','-n',$state.originalHome)
foreach ($entry in $state.packages) {
    if ($entry.package -eq $old.package) { continue }
    Restore-Entry $entry
}
# Reapply after enabling packages, so the original advertising service setting is
# restored exactly even if Android pruned disabled services during the transition.
foreach ($key in @('enabled_accessibility_services','accessibility_enabled')) {
    $value=$state.accessibility.$key
    if ($value -eq 'null' -or $null -eq $value) { Invoke-Adb @('shell','settings','delete','secure',$key) }
    else { Invoke-Adb @('shell','settings','put','secure',$key,[string]$value) }
}
$resolvedHome = & $AdbPath -s $Serial shell cmd package resolve-activity --brief --user 0 -a android.intent.action.MAIN -c android.intent.category.HOME
if (($resolvedHome -join "`n") -notmatch 'com.ooredoo.tv.stb/com.ifeelsmart.VoltStb.MainActivity') { throw 'Old home not confirmed. Serene stays enabled as a fallback.' }
Invoke-Adb @('shell','pm','disable-user','--user','0','local.serene.tv')
Invoke-Adb @('shell','input','keyevent','KEYCODE_HOME')
Write-Host 'Original home and package enabled states restored. Nothing was uninstalled or cleared.'
