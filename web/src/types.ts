export type ProviderType = 'wikimedia' | 'nasa' | 'openverse' | 'local';

export type AspectRatioType = '9:16' | '20:9' | '3:4' | '16:9' | '1:1';

export interface LicenseInfo {
  code: string; // 'CC0', 'Public Domain', 'CC-BY-4.0', 'NASA Public Domain', etc.
  name: string;
  url?: string;
  author?: string;
  authorUrl?: string;
  attributionRequired: boolean;
  commercialUseAllowed: boolean;
  derivativeWorksAllowed: boolean;
  verified: boolean;
  rawLicenseString?: string;
}

export interface WallpaperItem {
  id: string;
  title: string;
  description?: string;
  thumbnailUrl: string;
  fullUrl: string;
  provider: ProviderType;
  sourcePageUrl: string;
  width: number;
  height: number;
  aspectRatio: number;
  category: string;
  license: LicenseInfo;
  dateCreated?: string;
  blurHashPlaceholder?: string;
}

export type ZenWashPreset = 'none' | 'sage' | 'sumi' | 'sand' | 'mist' | 'twilight' | 'indigo';

export type MeshPresetId = 'biophilic' | 'dawn' | 'twilight' | 'river' | 'golden' | 'zen';
export type MeshBlendMode = 'overlay' | 'soft-light' | 'backdrop' | 'pure';

export interface MeshNode {
  x: number; // 0 to 100 percentage
  y: number; // 0 to 100 percentage
  radius: number; // percentage radius
  color: string;
  opacity: number;
}

export interface MeshGradientSettings {
  enabled: boolean;
  preset: MeshPresetId;
  intensity: number; // 0 to 100%
  blur: number; // 0 to 60px
  blendMode: MeshBlendMode;
  seed: number;
  customPrimaryColor?: string;
}

export interface TextOverlaySettings {
  enabled: boolean;
  text: string;
  subtext?: string;
  kanji?: string;
  fontStyle: 'serif' | 'sans';
  position: 'top' | 'center' | 'bottom';
  size: 'sm' | 'md' | 'lg' | 'xl';
  color: string;
  opacity: number;
  letterSpacing: 'normal' | 'wide' | 'widest';
  showJapaneseMark: boolean;
}

export interface EditorSettings {
  cropRatio: AspectRatioType;
  zoom: number; // 1.0 to 3.0
  panX: number; // percentage offset -50 to 50
  panY: number; // percentage offset -50 to 50
  brightness: number; // 50 to 150 (100 = default)
  contrast: number; // 50 to 150 (100 = default)
  saturation: number; // 0 to 150 (100 = default)
  warmth: number; // -50 to +50 (0 = default)
  blur: number; // 0 to 20 px (0 = default)
  vignette: number; // 0 to 100% (0 = default)
  grain: number; // 0 to 50% (0 = default)
  washPreset: ZenWashPreset;
  washIntensity: number; // 0 to 100% (40 = default when wash active)
  textOverlay: TextOverlaySettings;
  showClockPreview: boolean;
  showWidgetsPreview: boolean;
  gridOverlay: 'none' | 'ruleOfThirds' | 'goldenRatio' | 'centerCross';
  uiPreviewMode: 'lockscreen' | 'homescreen' | 'clean';
  washiTexture: number; // 0 to 100%
  komorebiRays: number; // 0 to 100%
  mistGradient: 'none' | 'bottom' | 'top';
  mistIntensity: number; // 0 to 100%
  meshGradient?: MeshGradientSettings;
}

export interface ColorPalette {
  dominant: string;
  secondary: string;
  muted: string;
  light: string;
  dark: string;
  swatches: string[];
}

export interface SavedProject {
  id: string;
  title: string;
  createdAt: number;
  updatedAt: number;
  wallpaper: WallpaperItem;
  settings: EditorSettings;
  previewThumbnail?: string;
}

export interface CuratedCategory {
  id: string;
  label: string;
  subtitle: string;
  query: string;
  icon: string;
  tagline: string;
}

export interface ZenPhrasePreset {
  id: string;
  category: 'haiku' | 'calm' | 'nature' | 'date';
  text: string;
  subtext?: string;
  kanji?: string;
}

export interface FilterOptions {
  query: string;
  category: string;
  provider: 'all' | ProviderType;
  orientation: 'all' | 'portrait' | 'landscape' | 'square';
  licenseOnly: 'all' | 'cc0_pd' | 'commercial';
}
