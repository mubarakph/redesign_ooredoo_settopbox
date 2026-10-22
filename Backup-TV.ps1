param([Parameter(Mandatory=$true)][string]$Serial,[string]$AdbPath='adb')
$ErrorActionPreference='Stop'
$statePath=Join-Path $PSScriptRoot 'restore-state.json'
if(Test-Path -LiteralPath $statePath){throw 'An original snapshot already exists. Preserve it; do not replace the original baseline after debloating.'}
$resolvedAdb=(Get-Command $AdbPath -ErrorAction Stop).Source
function Read-Adb([string[]]$Arguments){$result=& $resolvedAdb -s $Serial @Arguments;if($LASTEXITCODE -ne 0){throw "ADB failed: $($Arguments -join ' ')"};return $result}
if((Read-Adb @('get-state')) -ne 'device'){throw 'Connect and authorize the intended TV first.'}
$backup=Join-Path $PSScriptRoot ('backups\'+(Get-Date -Format 'yyyyMMdd-HHmmss'))
New-Item -ItemType Directory $backup -Force | Out-Null
Read-Adb @('shell','getprop') | Set-Content "$backup\device.txt"
Read-Adb @('shell','dumpsys','package') | Set-Content "$backup\package-state.txt"
Read-Adb @('shell','pm','list','packages','-d') | Set-Content "$backup\disabled-packages.txt"
foreach($scope in @('system','secure','global')){Read-Adb @('shell','settings','list',$scope) | Set-Content "$backup\settings-$scope.txt"}
$originalHome=Read-Adb @('shell','cmd','package','resolve-activity','--brief','--user','0','-a','android.intent.action.MAIN','-c','android.intent.category.HOME') | Where-Object {$_ -match '^\S+/\S+$'} | Select-Object -Last 1
if(!$originalHome -or $originalHome -like 'local.serene.tv/*'){throw 'Capture the original manufacturer home before installing or applying Serene.'}
$packages=@();foreach($pkg in @('com.hoppr.androidtv.thumper.prod7','tv.sdt.agama.empclient','com.sdt.diagnose','com.sdt.logtools','com.sdt.producttest','com.google.android.tvrecommendations','com.ooredoo.tv.stb')){
 $dump=(Read-Adb @('shell','dumpsys','package',$pkg)) -join "`n"
 if($dump -notmatch '(?m)^\s+User 0: ceDataInode=.*enabled=(\d+)'){throw "Expected package not found: $pkg. Review this device before applying these scripts."}
 $packages+=@{package=$pkg;enabled=[int]$Matches[1]}
}
$accessibility=@{};foreach($key in @('enabled_accessibility_services','accessibility_enabled')){$accessibility[$key]=((Read-Adb @('shell','settings','get','secure',$key)) -join "`n").Trim()}
Read-Adb @('shell','screencap','-p','/data/local/tmp/serene-before.png') | Out-Null
Read-Adb @('pull','/data/local/tmp/serene-before.png',"$backup\before.png") | Out-Null
@{adb=$resolvedAdb;serial=$Serial;backup=$backup;originalHome=$originalHome;packages=$packages;accessibility=$accessibility} | ConvertTo-Json -Depth 8 | Set-Content $statePath
Write-Host "Configuration snapshot saved to $backup. This is not an app-data backup."
