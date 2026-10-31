# Zen Nature — Minimalist Wallpaper Studio

> *"Find stillness in every frame."*

A calm, open-source, mobile-first Zen Minimalist Nature Wallpaper discovery and custom editing studio. Built with React 19, TypeScript, Tailwind CSS, and HTML5 Canvas, inspired by Japanese wabi-sabi aesthetics and Scandinavian design principles.

---

## Features

1. **Open Image Discovery**:
   - **Wikimedia Commons**: Official MediaWiki Action API integration retrieving high-resolution nature photography, author attribution, and Creative Commons / Public Domain license metadata.
   - **NASA Image and Video Library**: Official NASA API search for planetary, Earth, mountain, forest, and celestial imagery with NASA media usage guidelines.
   - **Openverse API**: Openly licensed Creative Commons nature photography.
   - **Zen Vault (Offline Ready)**: Bundled high-resolution cinematic nature photography ensuring immediate responsiveness and 100% offline functionality.

2. **Curated Zen Categories**:
   - Misty Mountains (`minimal mist mountain landscape`)
   - Japanese Gardens (`Japanese moss garden`)
   - Forests and Moss (`foggy pine forest`)
   - Ocean and Waves (`minimal ocean horizon`)
   - Lakes and Reflections (`calm lake reflection`)
   - Minimalist Deserts (`minimal sand dunes`)
   - Rain and Fog (`misty rain forest`)
   - Bamboo and Stones (`bamboo forest mist`)
   - Sunsets and Dawn (`soft sunrise mountain`)
   - Moon and Night Sky (`moon over mountains`)

3. **Contemplative Wallpaper Studio (Editor)**:
   - **Interactive Viewport**: Touch and mouse drag-to-pan with 1.0× to 2.5× zoom.
   - **Multi-Device Aspect Ratios**: Modern Android flagship 20:9, standard mobile 9:16, tablet 3:4, widescreen desktop 16:9, and square 1:1.
   - **Clock Safe-Zone Overlay**: Interactive toggle displaying live time, date, notification zones, and home bar to test real-world lock-screen ergonomics.
   - **Visual Sliders**: Brightness, Contrast, Saturation, Warmth (warm amber vs. cool cyan), Soft Focus / Blur, Vignette, and Analog Film Grain.
   - **Zen Duotone Washes**: Sumi Ink (carbon monochrome), Moss Sage, Kyoto Sand, Morning Mist, and Twilight Indigo.
   - **Minimalist Typography Engine**: Matsuo Basho haiku presets, mindfulness reminders, Japanese kanji watermarks, and custom multiline text with serif/sans typography.
   - **Edit History**: Instant Undo, Redo, Reset, and local project saving.

4. **High-Fidelity Export & License Attribution**:
   - Exports up to QHD resolution (1440×2560 or 1080×2400) directly on device via HTML5 Canvas.
   - Format selection: 95% High-Fidelity JPEG or Lossless PNG.
   - Automatic generation of `zen_nature_attribution.txt` for Creative Commons (CC-BY) compliance.
   - Web Share API integration on supported mobile devices.

5. **Privacy & On-Device Storage**:
   - Zero telemetry, analytics, cookies, or account requirements.
   - Local storage management with clear cache controls.

---

## Technology Stack

- **Framework**: React 19 + TypeScript + Vite
- **Styling**: Tailwind CSS v4 (`@import "tailwindcss";`)
- **Icons**: Lucide React
- **Animations**: Motion
- **Image Processing**: Native HTML5 Canvas 2D Context (hardware accelerated)
- **Celebration Effects**: Canvas Confetti

---

## Dependency & License Inventory

| Package | Version | License | Purpose |
|---|---|---|---|
| `react` | ^19.0.1 | MIT | UI component architecture |
| `react-dom` | ^19.0.1 | MIT | DOM rendering |
| `vite` | ^8.3.0 | MIT | Next-generation frontend tooling |
| `typescript` | ^7.0.2 | Apache-2.0 | Type safety & strict contracts |
| `tailwindcss` | ^4.3.3 | MIT | Utility-first responsive styling |
| `lucide-react` | ^0.546.0 | ISC | Clean minimalist icon set |
| `motion` | ^12.23.24 | MIT | Restrained fluid transitions |
| `canvas-confetti`| ^1.9.4 | ISC | Lightweight celebratory feedback |

---

## API Integrations & Limitations

### 1. Wikimedia Commons MediaWiki API
- **Endpoint**: `https://commons.wikimedia.org/w/api.php`
- **Method**: `action=query&generator=search&gsrnamespace=6&prop=imageinfo&iiprop=url|size|mime|extmetadata`
- **Authentication**: Keyless open public API.
- **Rate Limits**: Anonymous client requests subject to MediaWiki User-Agent etiquette. The app batches queries, debounces search, and caches responses for 30 minutes.
- **Licensing**: Explicitly parses `LicenseShortName`, `UsageTerms`, `Artist`, and `LicenseUrl`. Only CC0, Public Domain, and CC licenses are verified.

### 2. NASA Image and Video Library API
- **Endpoint**: `https://images.nasa.gov/search`
- **Method**: `GET ?q=...&media_type=image`
- **Authentication**: Keyless open public API.
- **Licensing**: Public Domain under NASA Media Usage Guidelines. Educational, personal, and informational wallpaper usage is permitted without fee.

### 3. Openverse API
- **Endpoint**: `https://api.openverse.org/v1/images/`
- **Method**: `GET ?q=...&license_type=all-cc`
- **Authentication**: Public tier open discovery.
- **Licensing**: Strict Creative Commons attribution extraction.

---

## Testing & Quality Assurance

Run the built-in unit test suite verifying dimension resolution, license parsing, and data integrity:

```bash
npm test
```

Build for production:

```bash
npm run build
```

Run linter:

```bash
npm run lint
```
