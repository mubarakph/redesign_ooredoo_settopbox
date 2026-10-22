# Serene Home — Ooredoo SKW-HP44J-OTV

Local native Android TV launcher, installed as `local.serene.tv`. No analytics, advertising SDK, web view, or third-party runtime. Internet permission is used for optional daily nature-photo downloads from Wikimedia Commons. All enabled launchable apps appear under All apps. Original system components without launchable activities are not shown as app tiles.

## Use

- D-pad and OK open apps. Hold OK for dock options: add/remove, move left/right, or choose a specific position. Dock order persists locally. The All apps tile always stays at the right end and opens a separate scrollable page; Back collapses it.
- Initial dock: Netflix, YouTube, Prime Video, Apple TV, Stremio, VLC. These are editable starter choices, not inferred personal favourites.
- Ten offline scenic photographs rotate every five minutes while Home is visible. Change wallpaper advances immediately. The collection includes forests, mountains, coast, Munnar, Kerala backwaters, and Athirappilly Falls. Only the current photo is decoded. Sources and licenses are in launcher/assets/CREDITS.txt. The top-left brand and welcome slogan are hidden.
- Daily nature sync is enabled by default. A persisted Android JobScheduler job checks approximately every 24 hours with a four-hour flexible window on unmetered Wi-Fi/Ethernet. Android may defer it when asleep, offline, or storage is low. A one-time initial sync adds the first photo. It does not depend on the PC or Codex remaining open.
- Sources are Wikimedia Commons featured forest, mountain, and waterfall categories. Only landscape JPEG/PNG photos with public-domain/CC0/CC BY licenses are accepted; obvious people, building, road, and illustration titles are excluded. Category selection is metadata-based, not image recognition. The job adds one new compressed 1280-pixel-wide JPEG per successful run and keeps at most 14 downloaded photos alongside the bundled collection. It stores the source, author and license for each photo. Network responses have size/time limits and only Commons/Wikimedia HTTPS hosts are allowed.
- Hold OK on Change wallpaper for the daily sync on/off switch, Sync now, download status and Photo credits. Downloaded photos join the same five-minute rotation in both themes. Failures preserve all offline wallpapers and retry later; new photos do not require an APK update.
- Theme, between Settings and Change wallpaper, offers Scenic (compact dock) and Apple TV (edge-to-edge 16:9 app banners and a five-column app grid). The Apple TV layout uses rounded artwork, a short focus lift/zoom, focused-app labels, a spacious scenery area, and a minimal bottom control row. Missing app banners fall back to the app icon. Choose a layout and press Apply theme; Cancel leaves the current layout unchanged. Dock choices/order, wallpaper and dark mode are shared across layouts. The selected UI theme persists locally.
- App discovery and icon loading run off the UI thread. The first screen draws immediately with placeholders, then fills in the apps. Returning Home reuses the cached app list; package changes trigger a refresh rather than rescanning on every resume. Both themes use native controls without web views, blur effects or continuous animation.
- The top-right Dark mode toggle switches the launcher's panels, text, menus and wallpaper shading between light and dark themes. The choice is saved locally; Android's global theme is untouched.
- Wi-Fi arcs and a strength label refresh every 15 seconds from the device RSSI. Disconnected or Ethernet states are shown explicitly. No location or internet permission is required; only normal Wi-Fi/network-state permissions were added.
- Settings opens the Android TV settings app.
- Home-button helper observes only `com.ooredoo.tv.stb` window-state events and opens Serene when its MainActivity appears. It cannot retrieve window contents and does not perform network requests. Wallpaper networking runs in a separate JobService. The helper also opens Serene when its service connects after boot.

## Changes and limits

Disabled for user 0 (no uninstall or data clearing):

| Package | Function |
|---|---|
| `com.hoppr.androidtv.thumper.prod7` | Hoppr advertising and its accessibility service |
| `com.google.android.tvrecommendations` | Google TV recommendations |

Firmware denied disabling the Ooredoo launcher, Agama monitoring, SDT diagnostics, SDT log tools, and SDT factory tests. Those packages remain enabled. Remote management, firmware updates, remote pairing, casting, Bluetooth, Google services, voice search, and streaming apps were preserved. This is partial debloating; it does not remove ads inside streaming apps or establish that all telemetry is stopped.

The Ooredoo home intent has priority 2, above ordinary third-party home apps. The normal HOME-role change alone did not override it. Serene therefore uses the scoped Home-button helper; Ooredoo can briefly appear during a transition and remains installed and running. No root or firmware modification was attempted. No global animation, resolution, DNS, or background-process settings were changed.

## Download ADB for Windows

[Download Android SDK Platform-Tools for Windows (official Google ZIP)](https://dl.google.com/android/repository/platform-tools-latest-windows.zip)

This package includes `adb.exe`, which is needed to pair with the TV, install the launcher, and run the backup/restore scripts. It is separate from the Android build-tools used to compile the app.

1. Download and extract the ZIP.
2. Open PowerShell inside the extracted `platform-tools` folder.
3. Check ADB with `./adb.exe version`.
4. Enable Wireless debugging on the TV. Open Pair device with pairing code and run `./adb.exe pair TV_IP:PAIRING_PORT`; enter the code when prompted.
5. Return to the TV's main Wireless debugging screen and run `./adb.exe connect TV_IP:CONNECTION_PORT`, then `./adb.exe devices -l`.

Use the ports currently displayed on the TV: the pairing port and connection port are different. Pass the full path to the extracted `adb.exe` through `-AdbPath` when creating a backup. The ZIP and extracted tools are not stored in this repository.

### Enable developer mode and test the TV connection

This is the Windows/Wi-Fi procedure used on the Ooredoo SKW-HP44J-OTV running Android TV 14. Menu names may differ on other boxes. Keep the PC and TV on the same local network.

1. On the TV, open **Settings → System → About** (sometimes **Device Preferences → About**).
2. Highlight **Android TV OS build / Build number** and press OK seven times, until the developer confirmation appears. Select the build entry, not the Android version entry.
3. Go back to **System / Device Preferences → Developer options** and enable **Wireless debugging**. Allow it on the current network if prompted.
4. Open Wireless debugging and note its **IP address & port**. This main-screen port is the connection port.
5. On the PC, open PowerShell and check whether ADB is on PATH:

```powershell
where.exe adb
```

If Windows cannot find it, extract the Platform-Tools ZIP linked above and open PowerShell in the folder containing `adb.exe`. You can run it as `./adb.exe` without adding it to PATH.

6. Test network access before attempting ADB. Replace both example values with the IP and connection port displayed on your TV:

```powershell
Test-NetConnection -ComputerName TV_IP -Port CONNECTION_PORT
```

Look for **`TcpTestSucceeded : True`**. This confirms the PC can reach that port; it does **not** mean ADB is paired or authorized. If false, recheck the current port, TV connectivity, and whether the router's guest-network/client-isolation settings separate the devices.

7. Check the downloaded ADB executable:

```powershell
./adb.exe version
```

8. On the TV, select **Pair device with pairing code** and leave that screen open. Use the IP and port on that pairing screen:

```powershell
./adb.exe pair TV_IP:PAIRING_PORT
```

Enter the current pairing code directly into PowerShell when prompted. Expected result: **`Successfully paired to ...`**. If pairing fails with a protocol/status error, close and reopen the pairing screen and retry with its current port and code. Do not use the main connection port for pairing. If necessary, restart the PC's ADB server before retrying:

```powershell
./adb.exe kill-server
./adb.exe start-server
```

9. Return to the TV's main Wireless debugging screen and use its current connection port:

```powershell
./adb.exe connect TV_IP:CONNECTION_PORT
./adb.exe devices -l
```

Expected results are **`connected to ...`** and an entry whose state is **`device`**, followed by the TV's product/model details. A `Successfully paired` message alone is not the final connection check. If ADB discovers the same box by both IP and an mDNS name, use `-s TV_IP:CONNECTION_PORT` to select it explicitly.

10. Confirm shell access with a read-only command:

```powershell
./adb.exe -s TV_IP:CONNECTION_PORT shell getprop ro.product.model
```

The tested box identifies as `SKW_HP44J_OTV`. Once connectivity is confirmed, create the original backup before installing or disabling anything.

Ports can change after toggling Wireless debugging or restarting the TV. Recheck the TV's screen before reconnecting; `./adb.exe mdns services` can also show advertised ADB services. This particular firmware advertised a different ADB port after reboot, so do not assume the initial connection endpoint is permanent.

Run the PowerShell commands one at a time in the shown order. If the prompt changes to `>>`, press **Ctrl+C** to cancel the unfinished command and return to the normal `PS ...>` prompt before retrying.

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
