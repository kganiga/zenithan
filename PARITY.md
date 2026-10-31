# Zenithan native rewrite: parity report

Kotlin 2.2 + Jetpack Compose (BOM 2025.08), minSdk 24, target/compile 36, same applicationId `com.zennature.wallpaper`, versionCode 2 / versionName 2.0.0. No WebView, no Capacitor, no JS.

## Build status (read this first)

| Part | Status |
|---|---|
| Pure logic (models, catalog, repository, providers, storage, mesh, palette, blur, gradient, attribution) | Compiled and **19/19 JUnit tests pass** on JVM |
| Android + Compose code (renderer, export engine, all screens) | **Not compiled yet.** The cloud sandbox cannot reach Google's Maven, so AGP/AndroidX cannot be downloaded there. First real build must run on your PC: `gradlew assembleDebug` then `gradlew testDebugUnitTest`. Expect a handful of compile fixes on first build. |
| Remote providers (Wikimedia, NASA, Openverse) | Parsers unit-tested against fixtures only; live endpoints were blocked from the sandbox. |
| Visual fidelity of the render pipeline vs. the web canvas | Ported step by step but **not compared side by side** on a device. |

## Screen and feature checklist

Legend: ✓ ported, ✓+ ported and fixed/improved, ≈ ported with a deliberate deviation (see below).

### Shell
- ✓ Header: logo, "Zenithan STUDIO" badge, saved heart with count dot, light/dark toggle (persisted), Create button
- ✓ Bottom nav (Wallpapers / Discover / Studio / Saved) with favorites badge capped at 99+; hidden on wide (≥768dp) layouts where header links show instead
- ✓ System back returns to Wallpapers
- ✓ Edge-to-edge with status/nav bar colors following the theme

### Wallpapers feed
- ✓ Debounced search (350 ms), clear button
- ✓ Shuffle with "reshuffled" toast, reset filters & shuffle from empty state
- ✓ Category pills incl. "Discover Moods"; provider row (All / Curated 4K / Wikimedia / NASA)
- ✓ Filters panel: orientation (all/portrait/landscape/square), license (all / CC0-PD / commercial)
- ✓ Density toggle, refresh, import photo
- ✓ Infinite scroll with wrap-around, dedupe, loading skeletons, footer states, scroll-to-top FAB
- ✓ Tile: 4K HD / AMOLED badge, favorite, title/author/license strip, open in Studio
- ≈ Feed state (filters, scroll, loaded pages) is kept across tabs; the web build reset and reshuffled on every visit
- ≈ Tile actions are always visible (web showed them on hover, which doesn't exist on touch)

### Discover
- ✓ Hero with today's featured wallpaper, license/provider card, Set as Wallpaper / Studio / Zen Moments / Import Photo
- ✓ Phone simulator with live lock-screen clock, tap to open detail
- ✓ Zen Moments banner and 60 s box-breathing dialog: 4 cycles of 4/4/4/3, animated orb, progress ring, pause/resume/reset, optional chimes (432/360/528 Hz sine, synthesized with AudioTrack), Start Building
- ✓ Circadian Stillness card (dawn/daylight/twilight/night by hour) → jumps to recommended category
- ✓ Curated Moods grid (10 categories, icons), Browse Wallpapers
- ✓ Recently Viewed (6 most recent) with favorite + Edit
- ✓ Footer banner with Lush Moss / Mist Peaks shortcuts

### Studio
- ✓ Change Image, Import Photo, Undo / Redo / Reset, Save (with "Saved" state), Export
- ✓ Live preview (540 px wide) with composition grid overlay (thirds / golden ratio + spiral / center cross), opacity levels
- ✓ Lock / Apps / Clean preview modes (lock and home overlays only on phone ratios, as on web)
- ✓ Drag to pan (0.35 factor, clamped ±50) with "Drag to pan" hint above 1.05× zoom
- ✓ Material You (Monet) palette card: 5 swatches (tap = text color), Copy Hex, Generate Mesh
- ✓ Crop & Guides tab: 4 guide cards, golden spiral toggle, guide opacity, 5 device ratios with target pixel sizes
- ✓ Atmosphere tab: mist gradient (none/bottom/top + intensity), komorebi rays, washi texture, zoom, brightness, contrast, saturation, warmth, blur, vignette, grain (same ranges/steps as web)
- ✓ Nature Mesh tab: enable switch, root color anchor from palette, 6 presets with live mini previews, 4 blend modes, Nudge Nodes (seed +17), intensity, diffusion, derived color stops (tap to copy)
- ✓ Zen Washes tab: 6 washes + intensity
- ✓ Typography tab: enable, contemplative presets incl. today's date, custom text, palette-matched colors, subtitle, kanji, position, font
- ✓+ New: pinch to zoom on the preview
- ✓+ New: text size, text opacity and kanji-mark toggle are exposed. The web renderer supported them but the UI never offered them.

### Saved
- ✓ Favorites tab: grid, remove, download license file, edit in Studio, empty state with Explore
- ✓ Custom Projects tab: preview, ratio/zoom/date, Open (restores settings), delete
- ✓ On-device storage card (size + record count), Clear Local Cache with confirm (theme is kept, as on web)

### Detail
- ✓ Clean / Lock Screen / Home Screen preview modes, live clock, pixel dimensions tag
- ✓ Provider label, "Verified Open License", title, description
- ✓+ Apply to Device: sets the wallpaper **directly** on Home, Lock, or both via WallpaperManager (1440×2560 phone fit), or opens the system "Set as…" chooser. The web build could only open a share sheet and tell you to finish in Photos.
- ✓ Download 4K (original bytes, original format kept), platform guide (Android / iPhone / Desktop)
- ✓ Creator / license card, open original archive page
- ✓ Copy Attribution (exact same text), License File (.txt to Downloads/Zenithan)
- ✓ Favorite / Saved, Customize in Studio

### Export
- ✓ Preview at target aspect, Single vs Coordinating Duo (lock + softened home: blur +5 min 5, vignette +12 min 25, bottom mist ≥45, no text)
- ✓ QHD High / Standard FHD / Source Res, JPEG 95% / PNG
- ✓ License summary, attribution .txt download
- ✓ Share (single) and Download, success state, confetti burst
- ✓+ Files go to Pictures/Zenithan through MediaStore. Blob/anchor downloads in the old WebView build did not reliably save anything.

### Data and storage
- ✓ Local catalog (56 entries), Wikimedia / NASA / Openverse search, interleaving, fallback to local, 15 min cache, orientation + license filters, page size 20
- ✓ Favorites, recents (max 20), projects, theme persisted as JSON files in app storage
- ✓ Imported photos copied into app storage (picker grant can expire); personal-photo license metadata as on web
- ✓ Wire format of projects/settings is byte-compatible with the web JSON (tested)

## Deliberate deviations (behavior differs on purpose)

1. **Studio state survives tab switches**, held in a ViewModel. On the web it reset to defaults whenever you left.
2. **Every edit is undoable.** Discrete changes record immediately and sliders/gestures/text record on release. On the web most changes never entered history.
3. **Export uses the live studio settings.** The web export dialog rendered `editorInitialSettings || defaults`, so your edits were ignored. This was a real bug.
4. **Opening a project restores its settings.** The web build loaded the image but started from defaults.
5. **"Source Res" keeps your crop ratio** and is capped at 4096 px on the long edge. The web build exported raw source dimensions, which ignored the crop, and an uncapped 6000 px source would OOM a phone.
6. **Grain is seeded**, so the preview no longer flickers on every slider tick.
7. **Backdrop mesh actually shows.** On the web it was drawn `destination-over` onto an already opaque canvas, which made it invisible. It is now drawn as the background layer.
8. **Blend modes on Android 7 to 9 (API 24 to 28):** soft-light, overlay and screen use the closest PorterDuff modes. These look close but not identical. API 29+ uses true BlendMode.
9. **Blur** is a 3-pass box approximation of the CSS gaussian. It is visually equivalent but not pixel-identical.
10. **Palette** is always extracted from the thumbnail (the web mixed thumbnail and full image).
11. **NASA endpoint** is `images-api.nasa.gov/search`. The web used `images.nasa.gov/search`, which I believe is wrong, but I could not verify it live from the sandbox.

## Removed (dead code, dead assets, unused deps)

- `ExploreScreen.tsx` (435 lines, never routed), Header `onQuickSelectFeatured`, Collections `onOpenExportModalForProject`, storage `isFavorite` (unused on web)
- Dead settings fields: `letterSpacing`, `showClockPreview`, `showWidgetsPreview`, the `indigo` wash (no UI, never rendered). Old JSON containing them still loads (ignored).
- Dependencies that were never used even on web: `@google/genai`, `express`, `dotenv`, `motion`, `autoprefixer`. The rest (React, Vite, Tailwind, Capacitor, lucide-react, canvas-confetti) are replaced by native equivalents.
- About 13 MB of APK assets: the AI Studio zip (4.8 MB), and `logo.png`, `favicon.png` and the Gemini image, which were three identical 3.9 MB 2048² PNGs. The logo is now a 6 KB WebP. The stitch `code.html` is also gone.
- Catalog entry `zen-local-zenith`, which was a screenshot of an app UI mockup rather than a wallpaper
- Fonts: Google Fonts CDN is replaced with bundled Playfair Display and Plus Jakarta Sans TTFs (OFL), so they work offline.

## Performance changes

- The preview renders off the main thread, and `collectLatest` drops stale frames. The web build re-decoded the image and re-extracted the palette on **every slider tick**.
- Decoded source bitmaps are LRU-cached by size (96 MB) and palettes are cached per URL.
- Coil uses a memory cache of 20% of RAM and a 256 MB disk cache. Thumbnails are not re-downloaded per tab visit.
- Photos are stored as files, not base64 in localStorage (the web hit the ~5 MB quota after a few imports).
- Release builds use R8 minify plus resource shrinking. Stitch images are WebP q90, about 70% smaller than the PNGs.
- The 3 stitch images are 768×1376. Their metadata claimed 1440×3120 and has been corrected.

## Known gaps and things I could not do

- **Existing users lose their data on upgrade.** Favorites and projects lived in the WebView's localStorage, and the native app cannot read that. A one-time migration is possible (a hidden WebView reads localStorage and posts it to Kotlin), but it is not built. Tell me if you have real users on v1.
- **Not compiled on Android yet** (see top). Not run on a device, so there are no screenshots and no visual diff against the web version.
- **Catalog licensing is misrepresented in the original data:** Unsplash photos are labeled "CC0", but the Unsplash License is not CC0. I kept the data as it was and am flagging it. Fix the labels before publishing.
- **56 catalog entries cover only 45 unique images.** Duplicates are deduped at display time, but the catalog itself should be cleaned up.
- The original's `runTests.ts` was ported. Section 5 of it ("grid configs") only asserted string literals, so it was replaced with a real wire-format test.
- No Play Store assets, signing config, or CI were set up (none existed before).

## Layout

```
app/src/main/java/com/zennature/wallpaper/
  ZenApp.kt, MainActivity.kt          app container (OkHttp, Coil, store, repository, exporter, fonts)
  data/                                models, curated data, repository, user store, remote providers
  render/                              Canvas render pipeline, mesh, palette, blur, conical gradient, bitmap cache
  export/                              MediaStore save, share, Set as, WallpaperManager, attribution text
  ui/                                  theme, shared components, feed, discover, studio, saved, detail, export
app/src/test/java/...                  JUnit (CoreLogicTest, RepositoryTest, UserStoreTest)
```
