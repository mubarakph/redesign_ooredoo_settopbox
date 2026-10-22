# Serene Home — Ooredoo SKW-HP44J-OTV

Local native Android TV launcher, installed as `local.serene.tv`. No internet permission, analytics, advertising SDK, web view, or third-party runtime. All enabled launchable apps appear under All apps. Original system components without launchable activities are not shown as app tiles.

## Use

- D-pad and OK open apps. Hold OK for dock options: add/remove, move left/right, or choose a specific position. Dock order persists locally. The All apps tile always stays at the right end and opens a separate scrollable page; Back collapses it.
- Initial dock: Netflix, YouTube, Prime Video, Apple TV, Stremio, VLC. These are editable starter choices, not inferred personal favourites.
- Ten offline scenic photographs rotate every five minutes while Home is visible. Change wallpaper advances immediately. The collection includes forests, mountains, coast, Munnar, Kerala backwaters, and Athirappilly Falls. Only the current photo is decoded. Sources and licenses are in launcher/assets/CREDITS.txt. The top-left brand and welcome slogan are hidden.
- Theme, between Settings and Change wallpaper, offers Scenic (compact dock) and Apple TV (edge-to-edge 16:9 app banners and a five-column app grid). The Apple TV layout uses rounded artwork, a short focus lift/zoom, focused-app labels, a spacious scenery area, and a minimal bottom control row. Missing app banners fall back to the app icon. Choose a layout and press Apply theme; Cancel leaves the current layout unchanged. Dock choices/order, wallpaper and dark mode are shared across layouts. The selected UI theme persists locally.
- App discovery and icon loading run off the UI thread. The first screen draws immediately with placeholders, then fills in the apps. Returning Home reuses the cached app list; package changes trigger a refresh rather than rescanning on every resume. Both themes use native controls without web views, blur effects or continuous animation.
- The top-right Dark mode toggle switches the launcher's panels, text, menus and wallpaper shading between light and dark themes. The choice is saved locally; Android's global theme is untouched.
- Wi-Fi arcs and a strength label refresh every 15 seconds from the device RSSI. Disconnected or Ethernet states are shown explicitly. No location or internet permission is required; only normal Wi-Fi/network-state permissions were added.
- Settings opens the Android TV settings app.
- Home-button helper observes only `com.ooredoo.tv.stb` window-state events and opens Serene when its MainActivity appears. It cannot retrieve window contents and has no internet access. It also opens Serene when its service connects after boot.

## Changes and limits

Disabled for user 0 (no uninstall or data clearing):

| Package | Function |
|---|---|
| `com.hoppr.androidtv.thumper.prod7` | Hoppr advertising and its accessibility service |
| `com.google.android.tvrecommendations` | Google TV recommendations |

Firmware denied disabling the Ooredoo launcher, Agama monitoring, SDT diagnostics, SDT log tools, and SDT factory tests. Those packages remain enabled. Remote management, firmware updates, remote pairing, casting, Bluetooth, Google services, voice search, and streaming apps were preserved. This is partial debloating; it does not remove ads inside streaming apps or establish that all telemetry is stopped.

The Ooredoo home intent has priority 2, above ordinary third-party home apps. The normal HOME-role change alone did not override it. Serene therefore uses the scoped Home-button helper; Ooredoo can briefly appear during a transition and remains installed and running. No root or firmware modification was attempted. No global animation, resolution, DNS, or background-process settings were changed.

## Backup and restore

The `backups` folder holds the original package dump, all three settings namespaces, original HOME component, disabled-package list, device properties, an Ooredoo APK copy, screenshots, and operation results. This is a configuration snapshot, not a full app-data backup.

Keep `restore-state.json` alongside `Restore-TV.ps1`. In PowerShell from this folder:

Device snapshots are deliberately excluded from Git. On a fresh, unmodified Ooredoo box, create one before making changes:

```powershell
.\Backup-TV.ps1 -Serial 'TV_IP:PORT' -AdbPath 'C:\path\to\adb.exe'
```

Do not run this on an already-modified box to replace its original baseline. The scripts target this specific model's package set; inspect another model before using them.

```powershell
.\Restore-TV.ps1
```

If wireless debugging uses a new connection port, reconnect using your existing paired ADB and pass the current endpoint:

```powershell
.\Restore-TV.ps1 -Serial 'TV_IP:NEW_PORT'
```

`-AdbPath` optionally overrides the saved ADB executable path. The script restores original enabled states, original accessibility settings (including Hoppr), Ooredoo HOME, then disables Serene. It never uninstalls anything. It intentionally leaves the new launcher's local preferences and APK in place, disabled.

`Apply-Debloat.ps1` reapplies the tested changes using the same snapshot; it requires Serene already installed. Firmware-protected packages are reported and left unchanged. Per-step screenshots are saved in the backup folder.

## Build

Install the build prerequisites locally (not stored in Git):

- A JDK 17 distribution under `tools/jdk/<jdk-directory>/`.
- Android platform API 35 under `tools/platform/android-35/`, including `android.jar`.
- Windows Android build-tools 35.0.0 extracted under `tools/buildtools/android-15/` (the upstream ZIP's directory name).

Run `build.ps1`. It validates or downloads the compressed wallpaper JPEGs through `Fetch-Wallpapers.ps1`, compiles Java, runs D8, packages, aligns, signs, and verifies `build/SereneHome.apk`. Preserve the generated local signing key for future updates. Keys, binaries, photos, device configuration, and screenshots are excluded from Git; only source, scripts, and photo credits are committed. Wallpaper downloads are for the local build and remain governed by their original licenses.

Before installing, pair/connect ADB and capture the original state. Then install `build/SereneHome.apk` with `adb -s TV_IP:PORT install -r build/SereneHome.apk` and run `Apply-Debloat.ps1`. Review the documented device limitations first.

## Verification

Installed and inspected actual TV screenshots; tested launching YouTube and returning Home, manual landscape changes, original launcher/package-state restoration, and reapplying the changes. Screenshot filenames ending in `checked` represent an attempted change and do not imply a protected package was disabled. Older filenames containing `disabled` may include denied attempts; `debloat-results.txt` records the actual result.

Initial reboot verification passed: Android reported boot completed, Serene was the resumed activity without a manual launch, the Home helper remained enabled, and both disabled packages retained their states. The box advertised a different ADB port after reboot; the local saved restore endpoint was updated accordingly. Version 2 was verified with TV screenshots for light/dark themes, All apps and Back, dock reordering and its position picker, and distinct photo wallpapers. The expanded ten-photo version is approximately 2 MB. Original local dock choices are preserved. Local backups contain previous APK/source and before/after screenshots. Full Ooredoo restoration remains available through Restore-TV.ps1.

The Apple TV focus fix disables ancestor clipping to padding and reserves vertical headroom for the enlarged tile and shadow. Rounded clipping remains enabled on the artwork itself.
