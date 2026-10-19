# FORGE AI LAB

**سامانه هوشمند تحلیل، سفارشی‌سازی و مهندسی نرم‌افزار اندروید**

Professional Android toolkit for real APK analysis, resource browsing, customization studio, rebuild pipeline, signature inspection, and a Persian AI agent powered by Gemini.

## Features

### Offline APK Analysis Engine
- Package name, version, SDK levels from binary `AndroidManifest.xml`
- Permissions, Activities, Services, Receivers, Providers
- MD5 / SHA-1 / SHA-256
- Resource & asset inventory
- Native library (ABI) listing
- DEX string extraction
- Known ads / analytics / network SDK detection (heuristic)
- JSON + HTML technical reports
- APK comparison

### Workspace
- Independent work directory per project
- Original APK never modified by default
- File browser, text preview, search
- Splash-screen resource discovery
- Change log & backups

### Customization Studio
- UI string listing (from DEX)
- Splash resource finder
- Manifest text view

### Rebuild & Sign
- Staged rebuild pipeline (extract → validate → re-package)
- Signature certificate inspection
- Documented production signing with `apksigner`

### AI Agent (Persian)
- Rule-based offline tools for common queries
- Optional Gemini API (user-supplied key, stored only on device)
- Maps natural language to real analysis tools

## Stack
- Kotlin 2.0
- Jetpack Compose + Material 3
- MVVM-style app container
- Coroutines / StateFlow
- DataStore
- OkHttp (Gemini)
- Pure-Kotlin AXML + DEX readers (no root / no external native tools required for analysis)

## Requirements
- Android Studio Ladybug+ / AGP 8.7
- JDK 17
- minSdk 26 / targetSdk 35

## Build

```bash
./gradlew assembleDebug
```

APK output: `app/build/outputs/apk/debug/app-debug.apk`

## Gemini API Key
1. Open **Settings** in the app
2. Paste your key from [Google AI Studio](https://aistudio.google.com/apikey)
3. Key is stored in DataStore on device only — never committed to git

## Limitations (honest)
| Capability | Status |
|---|---|
| Manifest / permissions / components | Real binary AXML parse |
| Resource listing & extract | Real (ZIP) |
| DEX strings | Real string table read |
| SDK detection | Heuristic patterns |
| Full resource decompile (arsc) | Not in v1 — paths listed |
| aapt2 / apktool rebuild | Packaging of extracted tree; full resource rebuild needs host tools |
| APK Signature v2/v3 generate | Inspect only; sign via `apksigner` CLI |
| Device install automation | Manual / system installer |

## License
Educational / research use. Only analyze and modify software you own or have explicit permission to modify.

## Repository
https://github.com/aamoozaa20-coder/ForgeAiLab
