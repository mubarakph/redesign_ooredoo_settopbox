param([string]$Serial, [string]$AdbPath)
$ErrorActionPreference='Stop'
$state=Get-Content (Join-Path $PSScriptRoot 'restore-state.json') -Raw | ConvertFrom-Json
if(!$Serial){$Serial=$state.serial}; if(!$AdbPath){$AdbPath=$state.adb}
function Run([string[]]$Arguments){
    $out=& $AdbPath -s $Serial @Arguments 2>&1
    $out | Write-Host
    if($LASTEXITCODE -ne 0 -or ($out -join "`n") -match 'Error:|Exception|SecurityException'){throw 'ADB failed. Run Restore-TV.ps1 to roll back.'}
}
function Capture([string]$Name){
    Run @('shell','screencap','-p','/data/local/tmp/serene-check.png')
    Run @('pull','/data/local/tmp/serene-check.png',(Join-Path $state.backup "$Name.png"))
}
Run @('get-state')
Run @('shell','pm','enable','--user','0','local.serene.tv')
Run @('shell','am','start','-W','-n','local.serene.tv/.HomeActivity')
Capture '01-serene-before-debloat'
# Ooredoo is firmware-protected. Use the scoped accessibility helper instead.
$helper='local.serene.tv/.HomeBridge'
$existing=$state.accessibility.enabled_accessibility_services
$preserved=@($existing -split ':' | Where-Object { $_ -and $_ -ne 'null' -and $_ -notlike 'com.hoppr.androidtv.thumper.prod7/*' -and $_ -notlike 'local.serene.tv/*' })
$services=(@($preserved)+@($helper)) -join ':'
Run @('shell','settings','put','secure','enabled_accessibility_services',$services)
Run @('shell','settings','put','secure','accessibility_enabled','1')
Run @('shell','input','keyevent','KEYCODE_HOME')
Run @('shell','sleep','1')
Capture '02-home-helper-enabled'
$index=3
foreach($entry in $state.packages){
    if($entry.package -eq 'com.ooredoo.tv.stb'){continue}
    $out=& $AdbPath -s $Serial shell pm disable-user --user 0 $entry.package 2>&1
    $out | Tee-Object -FilePath (Join-Path $state.backup 'debloat-results.txt') -Append | Write-Host
    if(($out -join "`n") -notmatch 'new state: disabled-user'){Write-Warning "Not disabled: $($entry.package). Preserving firmware restrictions."}
    Capture ('{0:00}-{1}-checked' -f $index,$entry.package)
    $index++
}
# Refresh the app list after the advertisement and factory-tool packages disappear.
Run @('shell','input','keyevent','KEYCODE_HOME')
Run @('shell','am','start','-W','-n','local.serene.tv/.HomeActivity')
Run @('shell','sleep','1')
Capture '09-final-home'
Run @('shell','pm','list','packages','-d')
