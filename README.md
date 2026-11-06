# MHOutlanders-Spoof

LSPosed module that forces device tier `7` (max) in **Monster Hunter Outlanders** (`com.garena.game.mho`) to unlock locked graphics presets and high framerate settings.

Hooks Java methods at runtime inside the game process. No APK modifying, repacking, or signature bypass needed.

## Why not just edit model or apm_cfg?

Spoofing `ro.product.model` or editing `apm_cfg.xml` / save keys `1200` & `1203` only changes telemetry and the in-game "Device Load" indicator. 

The actual graphics menu lock evaluates the Java-side device class returned by `GPMAgent` to the Unreal Engine bridge (`GameActivity`). This module hooks those calls directly and forces tier `7`.

## Hooks

Forces return value or setter argument to `7`:
- `com.perfsight.gpm.portal.GPMAgent` → `checkDCLSByQcc`, `checkDCLSByQccSync`, `checkDCLSByString`, `setDefinedDeviceClass`
- `com.epicgames.unreal.GameActivity` → `AndroidThunkJava_getDeviceLevel*`, `AndroidThunkJava_setDeviceLevel`

Also logs `setQuality`, `GPMAgent.setQulaity` *(game typo)*, and `setTargetFramerate` for debugging.

## Target

- **Game**: `com.garena.game.mho` (tested on `v1.0.11.2`)
- **Environment**: Android 12–16 (arm64-v8a)
- **Framework**: KernelSU / APatch / Magisk + LSPosed (Zygisk)

## Install

Grab the APK from [Releases](https://github.com/fortifying/MHOutlanders-Spoof/releases) or the GitHub Actions build artifact:

```bash
adb install -r MHOutlanders-Spoof.apk
```

1. Open **LSPosed Manager** and enable **MHOutlanders Spoof**.
2. Check the scope for `com.garena.game.mho`.
3. Force stop the game (or reboot).
4. Launch the game → **Settings** → **Graphics**.

## Verification

Check logcat while launching the game:

```bash
adb logcat -s LSPosed Xposed | grep "MHO Hook"
```

Expected output:
```text
MHO Hook: loaded into com.garena.game.mho
MHO Hook: installed result hook com.perfsight.gpm.portal.GPMAgent.checkDCLSByQcc
MHO Hook: com.perfsight.gpm.portal.GPMAgent.checkDCLSByQcc -> 7
MHO Hook: com.epicgames.unreal.GameActivity.AndroidThunkJava_setDeviceLevel 2 -> 7
```

## Build

Requirements: JDK 17+, `aapt2`, `apksigner`, `curl`, `unzip`.

```bash
chmod +x scripts/build.sh
./scripts/build.sh
```

The script automatically downloads API 35 `android.jar` and Google R8 into `.deps/` if not present. Output is generated at `build/MHOutlanders-Spoof.apk`.

## Notes

- Missing hooks log as `FAIL` instead of crashing the game.
- Unlocks the settings gate; actual in-game performance still depends on your GPU/SoC.
- Hooking memory/game processes can be flagged by anti-cheat. Use at your own risk.

## License

[MIT](LICENSE)
