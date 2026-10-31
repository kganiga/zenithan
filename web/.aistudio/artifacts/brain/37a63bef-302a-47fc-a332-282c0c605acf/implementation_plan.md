# Zen Nature — Minimalist Wallpaper Builder

An open-source, mobile-first Zen minimalist nature wallpaper discovery, editing, and export studio. Users can search and explore high-resolution nature photography across verified public domain and Creative Commons sources (Wikimedia Commons, NASA Image Library, and Openverse), customize them with meditative adjustments and minimalist typography overlays, and export wallpaper files for mobile, tablet, or desktop devices.

---

## User Review & Critical Decisions

> [!IMPORTANT]
> The following product and design decisions have been confirmed through user feedback and will guide the build:

- **Confirmed Provider Strategy**: All 3 public open image repositories (**Wikimedia Commons**, **NASA Image and Video Library**, and **Openverse**) will be integrated into a unified, modular provider architecture with **Wikimedia Commons** as the default primary provider. All providers operate with zero mandatory API keys, strict license extraction (CC-0, CC-BY, Public Domain), and graceful offline fallback to bundled high-fidelity photography assets.
- **Confirmed Text Overlay & Clock Safe-Area**: Balanced minimalist typography overlays offering short Zen Haiku, poetic nature reminders, custom phrases, and Japanese aesthetic kanji/date marks, paired with an interactive Android lock-screen clock & widget safe-zone overlay for realistic framing.
- **Confirmed Aspect Ratio Support**: Multi-device presets including mobile phone portrait (**9:16** and modern tall **20:9**), tablet portrait (**3:4** / **4:3**), desktop (**16:9**), and square (**1:1**) with freeform pan, zoom, and orientation toggling.
- **Confirmed Default Aesthetic Theme**: **Warm Mist White** (natural Japanese paper and Scandinavian light atmosphere: off-white `#F9F8F6`, stone `#73716B`, sage green `#8A9A86`, and warm sand `#EFECE6`), with a 1-tap toggle to **Zen Charcoal** (deep dark stone `#121413`).
- **No Paid APIs or Backend Tracking**: 100% client-side privacy, local IndexedDB/localStorage persistence for favorites and custom projects, canvas-based high-resolution exports (PNG and high-quality JPEG), and standard Web Share API / File Download support.

---

## 1. Overview & Core Concept

- **What It Does**: Zen Nature is a calm, contemplative wallpaper studio designed as a mobile-first Progressive Web App. It allows users to explore curated nature categories (Misty Mountains, Japanese Gardens, Forests & Moss, Ocean & Waves, Lakes & Reflections, Sand Dunes, Rain & Fog, Bamboo & Stones, Sunsets & Dawn, Moon & Night Sky), preview how wallpapers look on actual phone lock and home screens, tune aesthetic parameters (warmth, brightness, contrast, blur, grain, vignette, muted duotone washes), and download full-resolution images with verified license attribution.
- **Target Persona**: Nature lovers, mindfulness practitioners, minimalists, and phone personalization enthusiasts seeking intentional, calm visuals without ad interruptions, paywalls, or accounts.
- **Core Value**: Serene discovery and zero-friction wallpaper creation, respecting open-source licenses, providing transparent attribution, and working smoothly even when offline.

---

## 2. User Experience & Visual Design

### A. Key User Flows

1. **Discovery & Curation (Home View)**:
   - Ambient, distraction-free hero wallpaper preview displaying today's featured calming nature capture.
   - Curated Zen category ribbon (horizontal scrolling with tactile feedback).
   - Instant search with debounced typing and provider selector (All, Wikimedia Commons, NASA, Openverse).
   - "Recent Views" and "Saved Collections" accessible at a glance.
2. **Deep Exploration & Search (Explore View)**:
   - Responsive multi-column masonry gallery with lazy-loaded thumbnails.
   - Granular filters: Provider source, orientation (portrait/landscape/square), and verified license type (CC0 / Public Domain / Attribution Required).
   - Image Detail sheet: high-res preview, title, creator, original source page link, license badge, and usage rights.
3. **Contemplative Wallpaper Editor (Studio View)**:
   - Realistic mobile phone viewport frame with toggleable clock & system status safe-area bar.
   - Interactive canvas pan & zoom with touch/mouse drag.
   - Aspect ratio switcher: 9:16 (standard mobile), 20:9 (modern flagship), 3:4 (tablet), 16:9 (desktop), 1:1 (square).
   - Visual adjustment sliders: Brightness, Contrast, Warmth/Cool, Subtle Blur, Film Grain, and Vignette.
   - Zen Color Washes: Muted Sage, Japanese Ink, Warm Sand, Morning Mist, Twilight Indigo, and Pure Monochrome.
   - Minimalist Text & Motifs: Optional Haiku presets, personal affirmations, date stamps, and custom text with adjustable font weight, letter spacing, and vertical alignment.
   - Action controls: Undo, Redo, Reset, and Save Project.
4. **Export & Attribution (Export View / Sheet)**:
   - Preview with simulated Android lock screen clock and icons.
   - Resolution selector: Device Native (e.g., 1080×2400 for 20:9 or 1440×2560 for 9:16) or Original Resolution.
   - Format: High-fidelity JPEG (95% quality) or lossless PNG.
   - Download to device Storage / Pictures, or trigger native Web Share sheet.
   - Copyable / downloadable license attribution text file (`zen_nature_attribution.txt`) ensuring CC-BY compliance.
5. **Set as Wallpaper Action & Live Simulator (Detail Inspection Sheet)**:
   - Direct prominent "Set as Wallpaper" CTA on every wallpaper card and detail view.
   - 1-tap phone portrait optimization (1440 × 2560) and 4K original download.
   - Native device share integration (`navigator.share`) invoking native Android/iOS wallpaper assignment.
   - Live interactive device preview toggles: Clean view, Lock Screen with real-time clock and widgets, and Home Screen with app icons and search dock.
   - Step-by-step guidance for setting wallpaper on Android and iPhone.
6. **Saved Collections & Offline Library (Collections View)**:
   - Offline-ready gallery of favorited items and saved custom editor projects.
   - Storage manager showing cached image size with 1-click cache purge.

### B. Visual Identity & Design Tokens

- **Aesthetic Direction**: Wabi-sabi minimalism, Japanese Zen garden tranquility, and Scandinavian functional clarity. Zero flashy neon gradients, zero decorative static pills, and generous breathing whitespace.
- **Color Palette (Theme Tokens)**:
  - *Light Theme (Warm Mist White - Default)*:
    - Canvas Background: `#F8F6F0` (warm washi paper)
    - Card Surface: `#FFFFFF`
    - Subtle Surface: `#EFECE6`
    - Muted Accent: `#8A9A86` (sage moss)
    - Deep Text: `#2C2B29` (sumi charcoal)
    - Secondary Text: `#726F68` (stone gray)
    - Border Tint: `#E2DDD5`
  - *Dark Theme (Zen Charcoal - Toggleable)*:
    - Canvas Background: `#121413` (kuro charcoal)
    - Card Surface: `#1A1C1B`
    - Surface Elevation: `#262927`
    - Accent: `#BBCBB8` (soft lichen)
    - Foreground Text: `#E2E3E0`
    - Border Tint: `#333534`
- **Typography**:
  - Headings & Editorial Display: `Playfair Display`, serif (elevated, serene, bookish elegance).
  - Body, Metadata & Controls: `Plus Jakarta Sans`, sans-serif (crisp, modern, legible at all sizes).
  - Clean unboxed metadata with bullet separators (`·`) in accordance with the Zero-Pill discipline.

---

## 3. Key Product Decisions & Trade-Offs

### Decision 1: Image Provider Integration Architecture
- **Chosen Approach**: Three independent provider adapters unified by a common TypeScript interface `ImageProvider` with normalized models (`WallpaperItem`, `LicenseInfo`).
  - *Wikimedia Commons*: MediaWiki Action API (`action=query&generator=search&prop=imageinfo...`) retrieving original URLs, authors, descriptions, and license codes.
  - *NASA Image and Video Library*: Official free search API (`https://images.nasa.gov/api`) querying Earth, mountain, and space views.
  - *Openverse API*: Public openverse API (`https://api.openverse.org/v1/images/`) filtering for CC licenses.
  - *Local Zen Fallbacks*: Bundled high-resolution cinematic nature photography (pine emerging from deep mist, glacial fjord lake reflection, dusk wind dunes) so the app works instantaneously without waiting for network calls, and functions 100% offline.
- **Why**: Protects against provider rate limits, network timeouts, or intermittent API outages while strictly preserving creator attributions and verified license terms.

### Decision 2: Pure Client-Side Image Processing & Canvas Export
- **Chosen Approach**: Full HTML5 Canvas 2D engine rendering at native high resolution (e.g. 1080×2400 or 1440×2560 pixels) with image smoothing, matrix transforms, CSS-style filter matrix calculations, and offscreen canvas export.
- **Why**: Guarantees zero personal image data leaves the device, eliminates expensive backend GPU hosting, and delivers instantaneous 60fps adjustments without lag.

### Decision 3: Local Persistence & Cache Control
- **Chosen Approach**: Browser `localStorage` for UI preferences and favorited metadata, with IndexedDB / Cache API for downloaded high-resolution images, plus an explicit "Manage Storage" panel.
- **Why**: Adheres strictly to the prompt rule: "No login, registration, analytics, advertising SDKs, or telemetry. Keep user data and preferences on the device."

---

## 4. Technical Architecture & Data Strategy

```
┌────────────────────────────────────────────────────────────────────────┐
│                        ZEN NATURE CLIENT APP                           │
├────────────────────────────────────────────────────────────────────────┤
│  Top Bar: Brand Identity "Zen Nature" · Theme Switcher · Quick Search   │
├────────────────────────────────────────────────────────────────────────┤
│  NAVIGATION TABS: [1. Home]  [2. Explore]  [3. Studio]  [4. Saved]     │
├────────────────────────────────────────────────────────────────────────┤
│                                                                        │
│  ┌───────────────────────┐  ┌────────────────────────────────────────┐ │
│  │ 1. Home Screen        │  │ 2. Explore & Search                    │ │
│  │ - Featured Wallpaper  │  │ - Multi-source Filter (Wikimedia/NASA) │ │
│  │ - Curated Zen Moods   │  │ - Orientation & License Filters        │ │
│  │ - Recent Wallpapers   │  │ - Infinite Masonry Nature Grid         │ │
│  │ - "Create" Hero CTA   │  │ - Detail & License Verification Modal  │ │
│  └───────────────────────┘  └────────────────────────────────────────┘ │
│                                                                        │
│  ┌───────────────────────────────────────────────────────────────────┐ │
│  │ 3. Wallpaper Studio & Editor Canvas                               │ │
│  │ - Live Mobile / Multi-Ratio Viewport Simulation (9:16, 20:9, etc.)│ │
│  │ - Pan, Zoom, Rotate, and Drag Canvas Engine                       │ │
│  │ - Clock & Lockscreen Safe-Zone Overlay Toggle                     │ │
│  │ - Visual Sliders: Brightness · Contrast · Warmth · Blur · Vignette│ │
│  │ - Zen Duotone Color Washes & Film Grain Overlay                   │ │
│  │ - Minimalist Typography Engine (Haiku, Zen Phrases, Date Stamps)  │ │
│  │ - Undo / Redo / Reset / Local Project Save                        │ │
│  │ - High-Res Native Resolution Export (PNG / JPG / Share)           │ │
│  └───────────────────────────────────────────────────────────────────┘ │
│                                                                        │
│  ┌───────────────────────────────────────────────────────────────────┐ │
│  │ 4. Collections & Offline Library                                  │ │
│  │ - Favorited Nature Images · Saved Editor Projects                 │ │
│  │ - Offline Cache Manager with Storage Purge Controls               │ │
│  │ - CC-BY Attribution Generator (`zen_nature_attribution.txt`)      │ │
│  └───────────────────────────────────────────────────────────────────┘ │
│                                                                        │
├────────────────────────────────────────────────────────────────────────┤
│                      UNIFIED PROVIDER REPOSITORY                       │
│  ┌───────────────────────┐  ┌────────────────┐  ┌──────────────────┐  │
│  │ Wikimedia Commons API │  │ NASA Images API│  │ Openverse API    │  │
│  └───────────────────────┘  └────────────────┘  └──────────────────┘  │
│  ┌─────────────────────────────────────────────────────────────────┐  │
│  │ Fallback Offline Cache: Bundled Cinematic Zen Photography Assets│  │
│  └─────────────────────────────────────────────────────────────────┘  │
└────────────────────────────────────────────────────────────────────────┘
```

### Data Models

```typescript
export interface LicenseInfo {
  code: string; // e.g. 'CC0', 'Public Domain', 'CC-BY-4.0', 'NASA-Public'
  name: string;
  url?: string;
  author?: string;
  attributionRequired: boolean;
  commercialUseAllowed: boolean;
  derivativeWorksAllowed: boolean;
  verified: boolean;
}

export interface WallpaperItem {
  id: string;
  title: string;
  description?: string;
  thumbnailUrl: string;
  fullUrl: string;
  provider: 'wikimedia' | 'nasa' | 'openverse' | 'local';
  sourcePageUrl: string;
  width: number;
  height: number;
  aspectRatio: number;
  category: string;
  license: LicenseInfo;
  createdAt?: string;
}

export interface EditorSettings {
  cropRatio: '9:16' | '20:9' | '3:4' | '16:9' | '1:1';
  zoom: number; // 1.0 to 3.0
  panX: number;
  panY: number;
  brightness: number; // 50 to 150
  contrast: number; // 50 to 150
  warmth: number; // -50 to +50
  blur: number; // 0 to 20 px
  vignette: number; // 0 to 100%
  grain: number; // 0 to 50%
  washPreset: 'none' | 'sage' | 'sumi' | 'sand' | 'mist' | 'twilight';
  washIntensity: number; // 0 to 100%
  textOverlay: {
    enabled: boolean;
    text: string;
    subtext?: string;
    fontStyle: 'serif' | 'sans' | 'mono';
    position: 'top' | 'center' | 'bottom';
    size: 'sm' | 'md' | 'lg';
    color: string;
    opacity: number;
  };
  showClockPreview: boolean;
}
```

---

## 5. Implementation Sequence

1. **Setup & Dependencies**:
   - Install `canvas-confetti` (for celebratory export experience if needed) and verify `lucide-react`, `motion`, `@tailwindcss/vite`.
   - Update `index.html` with title "Zen Nature — Minimalist Wallpaper Builder", meta tags, and Google Fonts (`Playfair Display` and `Plus Jakarta Sans`).
   - Sync `metadata.json` with app title and description.
2. **Provider Layer**:
   - Implement Wikimedia Commons MediaWiki API client with query builder for Zen categories.
   - Implement NASA Image and Video Library search client.
   - Implement Openverse API search client.
   - Implement offline nature cache and bundled fallback items using the extracted high-res photo assets.
3. **Core State & Persistence**:
   - Create local persistence hook for favorites, saved projects, and recent views.
   - Create theme provider (Warm Mist White vs. Zen Charcoal).
4. **Main Views**:
   - **Home Screen**: Featured wallpaper showcase, curated Zen categories, recent picks, and quick create action.
   - **Explore Screen**: Live search, provider selector, orientation filter, masonry grid, image detail sheet with license verification.
   - **Wallpaper Editor Screen**: Interactive canvas with pan/zoom, aspect ratio selector (9:16, 20:9, 3:4, 16:9, 1:1), visual adjustment sliders, Zen washes, typography overlays, safe-area preview, and history (undo/redo).
   - **Export Modal**: High-res canvas render, JPEG/PNG format toggle, resolution selection, attribution download, and device save/share.
   - **Collections Screen**: Saved wallpapers, project re-editing, and cache management.
5. **Validation & Verification**:
   - Run compilation via `compile_applet`.
   - Test offline mode, provider failover, canvas rendering, text rendering, and exports.
