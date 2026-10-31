# Zen Nature — Native Android Project

This directory contains the complete native Android project for **Zen Nature — Minimalist Wallpaper Studio** (package `com.zennature.wallpaper`).

---

## Architecture

- **Project Root**: `/android`
- **Application Module**: `/android/app`
- **Manifest**: `/android/app/src/main/AndroidManifest.xml`
- **Application ID**: `com.zennature.wallpaper`
- **Build System**: Gradle 8.x + Android Gradle Plugin
- **Web Runtime Assets**: `/android/app/src/main/assets/public/`

---

## How to Build the Native Android APK

### Option 1: Using Android Studio (Recommended GUI)

1. Open **Android Studio**.
2. Select **File > Open...** and navigate to this project's `android` folder.
3. Allow Gradle to sync dependencies.
4. Connect an Android phone via USB (with Developer Mode & USB Debugging enabled) or start an Android Virtual Device (AVD).
5. Click the green **Run (▶)** button or press `Shift + F10` to build and install directly on your device.

### Option 2: Using the Command Line (Gradle)

From the project root:

```bash
# 1. Build and sync web assets to the native Android directory
npm run cap:sync

# 2. Build the Debug APK using the Gradle Wrapper
cd android
./gradlew assembleDebug
```

The compiled APK will be generated at:
```
android/app/build/outputs/apk/debug/app-debug.apk
```

You can install this APK on any connected Android device using ADB:
```bash
adb install android/app/build/outputs/apk/debug/app-debug.apk
```

### Option 3: Building a Signed Release APK / AAB for Google Play

In Android Studio:
1. Select **Build > Generate Signed Bundle / APK...**
2. Choose **Android App Bundle** (for Google Play) or **APK**.
3. Create or select your release keystore.
4. Select destination folder and click **Finish**.

---

## Permissions Configured

In `android/app/src/main/AndroidManifest.xml`:
- `android.permission.INTERNET`: For querying Wikimedia Commons, NASA, and Openverse nature repositories.
- `android.permission.SET_WALLPAPER`: For direct Android system wallpaper setting.
- `android.permission.READ_MEDIA_IMAGES`: For importing personal nature photos from the camera roll on Android 13+.
- `android.permission.WRITE_EXTERNAL_STORAGE`: For saving exported wallpapers to the Android `Pictures` / `Wallpapers` directory on Android 9 and lower.
