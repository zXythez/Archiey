# Archiey

**A minimal Android file manager.**

Archiey is an open-source Android file manager developed under **Shattercore (Xythe)**, built with **Kotlin**, **Jetpack Compose**, and **Material 3**.

> **Development status:** Early development. Features and compatibility are still being tested. The source code is available in this repository. This is an early development build, not a stable release.

## Goals

- Fast, clean, lightweight file browsing
- Practical file operations with predictable behavior
- Deep file search without blocking the interface
- Reliable archive browsing and extraction
- Compatibility across a wide range of Android devices
- Respect Android storage permissions and user data

## Current development areas

- File browser and navigation
- File operation handling
- Deep search
- Settings and application lifecycle behavior

The items above describe development work, **not a claim that every feature is complete or production-ready**.

## Planned work

- Archive extraction engine (ZIP, 7z, TAR and other formats as feasibility permits)
- Extraction progress, cancellation and robust error handling
- Performance and reliability testing on real Android devices
- Improved handling of scoped storage and permission edge cases

## Technology

- Kotlin
- Jetpack Compose
- Material 3
- Android Gradle build system

## Getting started

### Requirements

- A compatible Android development environment (Android Studio or an Android IDE with Gradle support)
- JDK 17
- Android SDK Platform 36 and compatible build tools
- Internet access for the first Gradle dependency sync
- An Android device or emulator for testing (minimum API 26 / Android 8.0)

The project currently compiles with SDK 36 and targets API 34. These values are not a guarantee of complete compatibility across all Android versions.

### Clone the source

```bash
git clone https://github.com/zXythez/Archiey.git
cd Archiey
```

### Build a debug APK

Open the repository root in your Android IDE and let Gradle synchronize. Alternatively, from a terminal with a properly configured Android SDK and JDK 17:

**Linux / macOS / Termux-compatible shell:**

```bash
chmod +x gradlew
./gradlew assembleDebug
```

**Windows:**

```bat
gradlew.bat assembleDebug
```

If successful, the debug APK should be at:

```text
app/build/outputs/apk/debug/app-debug.apk
```

You can install the debug APK using your IDE, or via ADB:

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

### Troubleshooting

- **SDK not found:** Configure your SDK path in your IDE or in a local, untracked `local.properties` file using `sdk.dir`.
- **JDK mismatch:** Ensure Gradle is running on JDK 17.
- **Gradle sync fails:** Check network access, SDK Platform 36 installation and Gradle error output.
- **Android-on-device IDEs:** Build tools and AAPT2 support vary; use the IDE's supported SDK/build configuration.

### Important

Archiey is under active development. Back up important files before testing file operations. Do not commit `local.properties`, credentials, signing keys, build outputs, or private device data.

## Contributing

Bug reports, reproducible test cases and constructive suggestions are welcome. Contribution guidelines will be added as the codebase stabilizes.

## License

**Not yet selected.** Public visibility does not by itself grant permission to redistribute or modify the source. A license will be added before broader open-source distribution.

---

Developed by **Shattercore (Xythe)**.
