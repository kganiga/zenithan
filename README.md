# Zen Nature — Minimalist Wallpaper Studio

> *"Find stillness in every frame."*

A calm, open-source, mobile-first Zen Minimalist Nature Wallpaper discovery and custom editing studio. Rewritten as a native Android application using Kotlin and Jetpack Compose, inspired by Japanese wabi-sabi aesthetics and Scandinavian design principles.

---

## Features

1. **Open Image Discovery**:
   - **Wikimedia Commons**: Retrieves high-resolution nature photography, author attribution, and Creative Commons / Public Domain license metadata.
   - **NASA Image and Video Library**: Search for planetary, Earth, mountain, forest, and celestial imagery.
   - **Openverse API**: Openly licensed Creative Commons nature photography.
   - **Zen Vault (Offline Ready)**: Bundled high-resolution cinematic nature photography ensuring immediate responsiveness and offline functionality.

2. **Curated Zen Categories**:
   - Misty Mountains, Japanese Gardens, Forests and Moss, Ocean and Waves, Lakes and Reflections, Minimalist Deserts, Rain and Fog, Bamboo and Stones, Sunsets and Dawn, Moon and Night Sky.

3. **Contemplative Wallpaper Studio (Editor)**:
   - **Interactive Viewport**: Pinch-to-zoom and drag-to-pan.
   - **Multi-Device Aspect Ratios**: Overlays and crop guides tailored to modern phone layouts.
   - **Clock Safe-Zone Overlay**: Interactive toggle displaying live time, date, notification zones, and home bar to test real-world lock-screen ergonomics.
   - **Visual Sliders**: Brightness, Contrast, Saturation, Warmth, Soft Focus / Blur, Vignette, and Seeded Analog Film Grain.
   - **Zen Duotone Washes**: Curated washes including Sumi Ink, Moss Sage, Morning Mist, etc.
   - **Nature Mesh**: Procedural mesh gradients rooted in the wallpaper's palette.
   - **Minimalist Typography Engine**: Matsuo Basho haiku presets, mindfulness reminders, Japanese kanji watermarks, and custom multiline text with bundled Playfair Display and Plus Jakarta Sans typography.
   - **Edit History**: Instant Undo, Redo, Reset, and local project saving.

4. **High-Fidelity Export & System Integration**:
   - **Apply directly to device**: Set as Home Screen, Lock Screen, or both using Android's WallpaperManager.
   - Exports up to source resolution or QHD directly on device via native rendering pipeline.
   - Saves into the `Pictures/Zenithan` directory via MediaStore.
   - Generates and downloads license file `zen_nature_attribution.txt` for compliance.

5. **Zen Moments**:
   - Box-breathing exercises with animated orbs and synthesized chimes to center your mind.

6. **Privacy & On-Device Storage**:
   - Zero telemetry, analytics, cookies, or account requirements.
   - Local storage management via app-specific storage.

---

## Technology Stack

- **Platform**: Android (minSdk 24, targetSdk 36)
- **Language**: Kotlin 2.2
- **UI Framework**: Jetpack Compose (BOM 2025.08)
- **Image Loading**: Coil
- **Rendering**: Hardware-accelerated Canvas / Compose Graphics

---

## Build & Test Instructions

### Building the App

This project uses Gradle. First-time builds will require an active internet connection to download dependencies from Google's Maven repository and other remote sources.

To build the debug APK:
```bash
./gradlew assembleDebug
```

To build a release APK (requires keystore configuration, see `.github/workflows/android.yml`):
```bash
./gradlew assembleRelease
```

### Running Tests

The pure business logic, repositories, and models are fully unit-tested on the JVM.

To execute the unit test suite:
```bash
./gradlew testDebugUnitTest
```

---

## API Integrations

- **Wikimedia Commons**: `https://commons.wikimedia.org/w/api.php`
- **NASA Image and Video Library**: `https://images-api.nasa.gov/search`
- **Openverse API**: `https://api.openverse.org/v1/images/`
