# MHOutlanders-Spoof v1.0

LSPosed module to bypass the minimum hardware to unlock max graphics presets and framerate options in **Monster Hunter Outlanders**.

Works via runtime method hooking inside the game process without modifying, repacking, or resigning the game APK.

## Compatible Targets

- **Garena Version**: `com.garena.game.mho`
- **Level Infinite Version**: `com.tencent.mhadv`
- **Tested Environment**: Android 12–16 (arm64-v8a), game client `v1.0.11.2+`
- **Root / Hooking Framework**: KernelSU / APatch / Magisk + LSPosed (Zygisk)


This module uses context-aware hook resolution alongside flagship identity and memory spoofing to satisfy every layer of verification.

## What It Hooks

### 1. Identity & Memory Spoofing
- **`android.os.Build`**: Spoofed to Samsung Galaxy S24 Ultra (`SM-S928B`, Snapdragon 8 Gen 3 `SM8650`, platform `pineapple`).
- **`android.os.SystemProperties`**: Intercepts `ro.product.model`, `ro.product.manufacturer`, `ro.product.brand`, `ro.board.platform`, `ro.hardware`, and `ro.soc.model`.
- **`android.app.ActivityManager`**: Intercepts `getMemoryInfo` to report 16 GB total RAM and prevent low-memory gating.

### 2. GPM & Unreal Engine Graphics Tiers
- **`com.perfsight.gpm.portal.GPMAgent`**:
  - `checkDCLSByQcc`, `checkDCLSByQccSync`, `checkDCLSByString`: context-aware returns (`android`/`ios` &rarr; `2`, `blacklist` &rarr; `1`, `default` &rarr; `7`).
  - `setDefinedDeviceClass`: forced to `6`.
- **`com.epicgames.unreal.GameActivity`**:
  - `AndroidThunkJava_getDeviceLevel*`: context-aware returns.
  - `AndroidThunkJava_setDeviceLevel`: forced to `6`.
- **`com.intlgame.api.analytics.INTLAnalytics`**:
  - `getDeviceLevel` &rarr; forced to `6`.
  - `setDeviceLevel` &rarr; argument forced to `6`.

## Installation

1. Grab the APK from [Releases](https://github.com/fortifying/MHOutlanders-Spoof/releases) or your build artifact:
   ```bash
   adb install -r MHOutlanders-Spoof.apk
   ```
2. Open **LSPosed Manager** and enable **MHOutlanders Spoof**.
3. Check the scope for your game package (`com.garena.game.mho` and/or `com.tencent.mhadv`).
4. Force-stop the game:
   ```bash
   adb shell am force-stop com.garena.game.mho
   # or for Level Infinite:
   adb shell am force-stop com.tencent.mhadv
   ```
5. Launch the game &rarr; **Settings** &rarr; **Graphics**.

## Verification & Logs

Check LSPosed module output during game launch:

```bash
adb logcat -s LSPosed Xposed | grep "MHO Hook"
```

Expected log output:
```text
MHO Hook: loaded into com.garena.game.mho [process: com.garena.game.mho]
MHO Hook: com.epicgames.unreal.GameActivity.AndroidThunkJava_setDeviceLevel 2 -> 6
MHO Hook: com.perfsight.gpm.portal.GPMAgent.setDefinedDeviceClass 2 -> 6
MHO Hook: INTLAnalytics.getDeviceLevel -> 6
MHO Hook: INTLAnalytics.setDeviceLevel -> 6
MHO Hook: com.perfsight.gpm.portal.GPMAgent.checkDCLSByQccSync(android, ...) -> 2
MHO Hook: com.perfsight.gpm.portal.GPMAgent.checkDCLSByQccSync(blacklist, ...) -> 1
MHO Hook: com.perfsight.gpm.portal.GPMAgent.checkDCLSByQccSync(default, ...) -> 7
MHO Hook: com.epicgames.unreal.GameActivity.AndroidThunkJava_setQuality(0, 0)
MHO Hook: com.epicgames.unreal.GameActivity.AndroidThunkJava_setTargetFramerate(60)
```

## Build

Requires JDK 17+, `aapt2`, and `apksigner`:

```bash
chmod +x scripts/build.sh
./scripts/build.sh
```

Output binary: `build/MHOutlanders-Spoof.apk`.

## License

[MIT](LICENSE)
