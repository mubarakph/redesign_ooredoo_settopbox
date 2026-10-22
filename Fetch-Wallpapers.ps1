$ErrorActionPreference = 'Stop'
$ProgressPreference = 'SilentlyContinue'
$assets = Join-Path $PSScriptRoot 'launcher\assets'
New-Item -ItemType Directory -Path $assets -Force | Out-Null
$photos = @{
    'alpine'='https://images.unsplash.com/photo-1470770841072-f978cf4d019e?fit=crop&w=1280&h=720&q=70&fm=jpg'
    'forest'='https://images.unsplash.com/photo-1441974231531-c6227db76b6e?fit=crop&w=1280&h=720&q=70&fm=jpg'
    'peaks'='https://images.unsplash.com/photo-1464822759023-fed622ff2c3b?fit=crop&w=1280&h=720&q=70&fm=jpg'
    'coast'='https://images.unsplash.com/photo-1507525428034-b723cf961d3e?fit=crop&w=1280&h=720&q=70&fm=jpg'
    'kerala-munnar'='https://images.unsplash.com/photo-1628080657485-a45999f8a3cb?fit=crop&w=1280&h=720&q=65&fm=jpg'
    'pine-forest'='https://images.unsplash.com/photo-1473448912268-2022ce9509d8?fit=crop&w=1280&h=720&q=65&fm=jpg'
    'woodland'='https://images.unsplash.com/photo-1448375240586-882707db888b?fit=crop&w=1280&h=720&q=65&fm=jpg'
    'kerala-backwaters'='https://images.pexels.com/photos/27869350/pexels-photo-27869350.jpeg?auto=compress&cs=tinysrgb&fit=crop&w=1280&h=720'
    'kerala-waterfalls'='https://images.pexels.com/photos/14847155/pexels-photo-14847155.jpeg?auto=compress&cs=tinysrgb&fit=crop&w=1280&h=720'
    'kerala-palms'='https://images.pexels.com/photos/29988900/pexels-photo-29988900.jpeg?auto=compress&cs=tinysrgb&fit=crop&w=1280&h=720'
}
foreach ($entry in $photos.GetEnumerator()) {
    $path = Join-Path $assets ($entry.Key+'.jpg')
    if (!(Test-Path -LiteralPath $path)) { Invoke-WebRequest $entry.Value -OutFile $path }
    $bytes = [IO.File]::ReadAllBytes($path)
    if ($bytes.Length -lt 3 -or $bytes[0] -ne 255 -or $bytes[1] -ne 216 -or $bytes[2] -ne 255) { throw "Invalid JPEG: $path" }
    Write-Host "Ready: $($entry.Key)"
}
