$ErrorActionPreference = 'Stop'
$root = $PSScriptRoot
& (Join-Path $root 'Fetch-Wallpapers.ps1')
$jdk = (Get-ChildItem "$root\tools\jdk" -Directory | Select-Object -First 1).FullName
$bt = "$root\tools\buildtools\android-15"
$android = "$root\tools\platform\android-35\android.jar"
$env:JAVA_HOME = $jdk
$env:PATH = "$jdk\bin;$env:PATH"
New-Item -ItemType Directory "$root\build\classes", "$root\build\dex" -Force | Out-Null
function Check { if ($LASTEXITCODE -ne 0) { throw "Build failed: $LASTEXITCODE" } }
$sources = @(Get-ChildItem "$root\launcher\src" -Filter '*.java' -Recurse | ForEach-Object FullName)
& "$jdk\bin\javac.exe" -encoding UTF-8 --release 8 -classpath $android -d "$root\build\classes" @sources
Check
& "$jdk\bin\jar.exe" cf "$root\build\classes.jar" -C "$root\build\classes" .
Check
& "$bt\d8.bat" --lib $android --min-api 26 --output "$root\build\dex" "$root\build\classes.jar"
Check
& "$bt\aapt2.exe" compile --dir "$root\launcher\res" -o "$root\build\resources.zip"
Check
& "$bt\aapt2.exe" link -I $android --manifest "$root\launcher\AndroidManifest.xml" -A "$root\launcher\assets" -o "$root\build\unsigned.apk" "$root\build\resources.zip"
Check
& "$jdk\bin\jar.exe" uf "$root\build\unsigned.apk" -C "$root\build\dex" classes.dex
Check
& "$bt\zipalign.exe" -f 4 "$root\build\unsigned.apk" "$root\build\aligned.apk"
Check
if (!(Test-Path "$root\launcher-signing.p12")) {
  & "$jdk\bin\keytool.exe" -genkeypair -keystore "$root\launcher-signing.p12" -storepass serene-local -keypass serene-local -alias serene -keyalg RSA -keysize 2048 -validity 10000 -dname "CN=Serene Local Home"
  Check
}
& "$bt\apksigner.bat" sign --ks "$root\launcher-signing.p12" --ks-pass pass:serene-local --out "$root\build\SereneHome.apk" "$root\build\aligned.apk"
Check
& "$bt\apksigner.bat" verify "$root\build\SereneHome.apk"
Check
Get-Item "$root\build\SereneHome.apk" | Select-Object FullName,Length
