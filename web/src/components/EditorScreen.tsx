import React from 'react';
import {
  Sliders,
  Type,
  Maximize2,
  Clock,
  RotateCcw,
  Undo2,
  Redo2,
  Save,
  Download,
  Palette,
  ZoomIn,
  Move,
  Eye,
  Check,
  ChevronDown,
  Grid3x3,
  Crosshair,
  Sparkles,
  LayoutGrid,
  Sun,
  CloudFog,
  FileUp,
  Copy,
  Layers,
  Shuffle,
  Wand2,
} from 'lucide-react';
import { WallpaperItem, EditorSettings, AspectRatioType, ZenWashPreset, ColorPalette, MeshPresetId, MeshBlendMode, MeshGradientSettings } from '../types';
import { ASPECT_RATIOS, ZEN_PHRASES } from '../data/curatedData';
import { renderWallpaperToCanvas, loadImage } from '../services/exportEngine';
import { saveProject } from '../services/storage';
import { GridOverlay, GridOverlayType, GridOpacity } from './GridOverlay';
import { HomeScreenSimulation } from './HomeScreenSimulation';
import { extractPaletteFromImage } from '../services/colorExtractor';
import { MESH_PRESETS, generateMeshCssGradient, generateMeshColors } from '../services/meshGradientGenerator';

interface EditorScreenProps {
  wallpaper: WallpaperItem;
  onOpenExportModal: () => void;
  onProjectSaved?: () => void;
  onSelectAnotherWallpaper: () => void;
  onImportCustomPhoto?: (wallpaper: WallpaperItem) => void;
}

const DEFAULT_SETTINGS: EditorSettings = {
  cropRatio: '20:9', // Modern tall flagship mobile
  zoom: 1.0,
  panX: 0,
  panY: 0,
  brightness: 100,
  contrast: 100,
  saturation: 100,
  warmth: 0,
  blur: 0,
  vignette: 15,
  grain: 5,
  washPreset: 'none',
  washIntensity: 35,
  meshGradient: {
    enabled: false,
    preset: 'biophilic',
    intensity: 65,
    blur: 35,
    blendMode: 'soft-light',
    seed: 42,
  },
  textOverlay: {
    enabled: false,
    text: '',
    subtext: '',
    kanji: '',
    fontStyle: 'serif',
    position: 'bottom',
    size: 'md',
    color: '#FFFFFF',
    opacity: 0.9,
    letterSpacing: 'wide',
    showJapaneseMark: true,
  },
  showClockPreview: true,
  showWidgetsPreview: false,
  gridOverlay: 'ruleOfThirds',
  uiPreviewMode: 'lockscreen',
  washiTexture: 0,
  komorebiRays: 0,
  mistGradient: 'none',
  mistIntensity: 40,
};

export const EditorScreen: React.FC<EditorScreenProps> = ({
  wallpaper,
  onOpenExportModal,
  onProjectSaved,
  onSelectAnotherWallpaper,
  onImportCustomPhoto,
}) => {
  const [settings, setSettings] = React.useState<EditorSettings>(DEFAULT_SETTINGS);
  const [history, setHistory] = React.useState<EditorSettings[]>([DEFAULT_SETTINGS]);
  const [historyIndex, setHistoryIndex] = React.useState(0);
  const [activeTab, setActiveTab] = React.useState<'crop' | 'tune' | 'mesh' | 'washes' | 'text'>('crop');
  const [saveSuccess, setSaveSuccess] = React.useState(false);
  const [isDragging, setIsDragging] = React.useState(false);

  // Visual Crop Helper Controls
  const [gridOpacity, setGridOpacity] = React.useState<GridOpacity>('medium');
  const [showGoldenSpiral, setShowGoldenSpiral] = React.useState<boolean>(true);

  // Material You Monet Extracted Palette
  const [palette, setPalette] = React.useState<ColorPalette | null>(null);
  const [paletteCopied, setPaletteCopied] = React.useState(false);

  const dragStartRef = React.useRef<{ x: number; y: number; initialPanX: number; initialPanY: number }>({
    x: 0,
    y: 0,
    initialPanX: 0,
    initialPanY: 0,
  });

  const fileInputRef = React.useRef<HTMLInputElement | null>(null);
  const canvasRef = React.useRef<HTMLCanvasElement | null>(null);

  // Clock time for simulated lock screen
  const [timeString, setTimeString] = React.useState('');
  const [dateString, setDateString] = React.useState('');

  React.useEffect(() => {
    const updateTime = () => {
      const now = new Date();
      setTimeString(
        now.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit', hour12: false })
      );
      setDateString(
        now.toLocaleDateString([], { weekday: 'short', month: 'short', day: 'numeric' })
      );
    };
    updateTime();
    const interval = setInterval(updateTime, 1000);
    return () => clearInterval(interval);
  }, []);

  // Extract Material You Monet palette whenever wallpaper changes
  React.useEffect(() => {
    let active = true;
    const extract = async () => {
      try {
        const img = await loadImage(wallpaper.thumbnailUrl || wallpaper.fullUrl);
        if (active) {
          const extracted = extractPaletteFromImage(img);
          setPalette(extracted);
        }
      } catch (err) {
        console.warn('Failed to extract Monet palette:', err);
      }
    };
    extract();
    return () => {
      active = false;
    };
  }, [wallpaper]);

  // Push to history
  const updateSettingsWithHistory = (newSettings: EditorSettings) => {
    const newHistory = history.slice(0, historyIndex + 1);
    newHistory.push(newSettings);
    setHistory(newHistory);
    setHistoryIndex(newHistory.length - 1);
    setSettings(newSettings);
  };

  const handleUndo = () => {
    if (historyIndex > 0) {
      const prev = history[historyIndex - 1];
      setHistoryIndex(historyIndex - 1);
      setSettings(prev);
    }
  };

  const handleRedo = () => {
    if (historyIndex < history.length - 1) {
      const next = history[historyIndex + 1];
      setHistoryIndex(historyIndex + 1);
      setSettings(next);
    }
  };

  const handleReset = () => {
    updateSettingsWithHistory(DEFAULT_SETTINGS);
  };

  const cycleGridOverlay = () => {
    const cycleMap: Record<GridOverlayType, GridOverlayType> = {
      none: 'ruleOfThirds',
      ruleOfThirds: 'goldenRatio',
      goldenRatio: 'centerCross',
      centerCross: 'none',
    };
    setSettings((prev) => ({ ...prev, gridOverlay: cycleMap[prev.gridOverlay] }));
  };

  // Re-render canvas preview
  React.useEffect(() => {
    let active = true;
    const render = async () => {
      if (!canvasRef.current) return;
      try {
        const ratioConfig = ASPECT_RATIOS.find((r) => r.id === settings.cropRatio);
        const wRatio = ratioConfig ? ratioConfig.widthRatio : 9;
        const hRatio = ratioConfig ? ratioConfig.heightRatio : 20;

        await renderWallpaperToCanvas(wallpaper, settings, canvasRef.current, {
          customWidth: 540,
          customHeight: Math.round(540 / (wRatio / hRatio)),
        });
      } catch (err) {
        console.warn('Live preview render error:', err);
      }
    };
    render();
    return () => {
      active = false;
    };
  }, [wallpaper, settings]);

  // Drag handlers
  const handleMouseDown = (e: React.MouseEvent) => {
    setIsDragging(true);
    dragStartRef.current = {
      x: e.clientX,
      y: e.clientY,
      initialPanX: settings.panX,
      initialPanY: settings.panY,
    };
  };

  const handleMouseMove = (e: React.MouseEvent) => {
    if (!isDragging) return;
    const dx = e.clientX - dragStartRef.current.x;
    const dy = e.clientY - dragStartRef.current.y;
    const newPanX = Math.max(-50, Math.min(50, dragStartRef.current.initialPanX + dx * 0.25));
    const newPanY = Math.max(-50, Math.min(50, dragStartRef.current.initialPanY + dy * 0.25));
    setSettings((prev) => ({ ...prev, panX: newPanX, panY: newPanY }));
  };

  const handleMouseUp = () => {
    if (isDragging) {
      setIsDragging(false);
      updateSettingsWithHistory(settings);
    }
  };

  const handleTouchStart = (e: React.TouchEvent) => {
    if (e.touches.length === 1) {
      setIsDragging(true);
      dragStartRef.current = {
        x: e.touches[0].clientX,
        y: e.touches[0].clientY,
        initialPanX: settings.panX,
        initialPanY: settings.panY,
      };
    }
  };

  const handleTouchMove = (e: React.TouchEvent) => {
    if (!isDragging || e.touches.length !== 1) return;
    const dx = e.touches[0].clientX - dragStartRef.current.x;
    const dy = e.touches[0].clientY - dragStartRef.current.y;
    const newPanX = Math.max(-50, Math.min(50, dragStartRef.current.initialPanX + dx * 0.35));
    const newPanY = Math.max(-50, Math.min(50, dragStartRef.current.initialPanY + dy * 0.35));
    setSettings((prev) => ({ ...prev, panX: newPanX, panY: newPanY }));
  };

  const handleTouchEnd = () => {
    if (isDragging) {
      setIsDragging(false);
      updateSettingsWithHistory(settings);
    }
  };

  const handleSaveProject = () => {
    const thumb = canvasRef.current?.toDataURL('image/jpeg', 0.6);
    saveProject(wallpaper, settings, `Zen ${wallpaper.title}`, thumb);
    setSaveSuccess(true);
    setTimeout(() => setSaveSuccess(false), 2500);
    onProjectSaved?.();
  };

  // Custom photo upload handler
  const handleFileUpload = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;

    const reader = new FileReader();
    reader.onload = (event) => {
      const dataUrl = event.target?.result as string;
      const img = new Image();
      img.onload = () => {
        const customItem: WallpaperItem = {
          id: `custom-${Date.now()}`,
          title: file.name.replace(/\.[^/.]+$/, '').replace(/[_-]/g, ' ') || 'Personal Nature Photo',
          description: 'Imported from personal device library for custom contemplation.',
          thumbnailUrl: dataUrl,
          fullUrl: dataUrl,
          provider: 'local',
          sourcePageUrl: 'local-device',
          width: img.width,
          height: img.height,
          aspectRatio: img.width / img.height,
          category: 'nature',
          license: {
            code: 'Personal Photo',
            name: 'Personal Creative Work (Private on device)',
            attributionRequired: false,
            commercialUseAllowed: true,
            derivativeWorksAllowed: true,
            verified: true,
            author: 'You (Device Owner)',
          },
        };
        onImportCustomPhoto?.(customItem);
      };
      img.src = dataUrl;
    };
    reader.readAsDataURL(file);
  };

  const copyMonetPalette = () => {
    if (!palette) return;
    const text = `/* Zen Nature — Android Material You (Monet) Palette */\n` +
      `--monet-dominant: ${palette.dominant};\n` +
      `--monet-secondary: ${palette.secondary};\n` +
      `--monet-muted: ${palette.muted};\n` +
      `--monet-light: ${palette.light};\n` +
      `--monet-dark: ${palette.dark};\n`;
    navigator.clipboard.writeText(text);
    setPaletteCopied(true);
    setTimeout(() => setPaletteCopied(false), 2000);
  };

  const activeRatio = ASPECT_RATIOS.find((r) => r.id === settings.cropRatio) || ASPECT_RATIOS[0];

  return (
    <div className="max-w-7xl mx-auto px-4 sm:px-6 pt-4 pb-24 md:pb-16 space-y-6">
      {/* Top Action Header */}
      <div className="flex flex-wrap items-center justify-between gap-3 pb-3 border-b border-[#E8E4DC] dark:border-[#282A29]">
        <div className="flex items-center gap-3">
          <button
            onClick={onSelectAnotherWallpaper}
            className="text-xs text-[#726F68] dark:text-[#939188] hover:text-[#2C2B29] dark:hover:text-[#F3F4F1] flex items-center gap-1 font-medium"
          >
            ← Change Image
          </button>
          <span className="text-[#E2DDD5] dark:text-[#333534]">|</span>
          <span className="text-xs font-serif font-medium text-[#2C2B29] dark:text-[#F3F4F1] truncate max-w-[150px] sm:max-w-xs">
            {wallpaper.title}
          </span>
        </div>

        {/* History, Custom Upload & Actions */}
        <div className="flex items-center gap-1.5 sm:gap-2">
          {/* Hidden File Input for Personal Photo Import */}
          <input
            type="file"
            ref={fileInputRef}
            onChange={handleFileUpload}
            accept="image/*"
            className="hidden"
          />
          <button
            onClick={() => fileInputRef.current?.click()}
            title="Import your own nature photo from camera roll / device"
            className="px-2.5 sm:px-3 py-1.5 rounded-lg border border-[#DCD7CE] dark:border-[#383A38] text-xs font-medium text-[#2C2B29] dark:text-[#E2E3E0] hover:bg-white dark:hover:bg-[#202221] flex items-center gap-1.5 transition-colors"
          >
            <FileUp className="w-3.5 h-3.5 text-[#556952] dark:text-[#BBCBB8]" />
            <span className="hidden sm:inline">Import Photo</span>
          </button>

          <button
            onClick={handleUndo}
            disabled={historyIndex <= 0}
            title="Undo"
            className="p-2 rounded-lg border border-[#E2DDD5] dark:border-[#333534] text-[#726F68] hover:bg-[#EFECE6] dark:hover:bg-[#202221] disabled:opacity-30 disabled:pointer-events-none transition-colors"
          >
            <Undo2 className="w-4 h-4" />
          </button>

          <button
            onClick={handleRedo}
            disabled={historyIndex >= history.length - 1}
            title="Redo"
            className="p-2 rounded-lg border border-[#E2DDD5] dark:border-[#333534] text-[#726F68] hover:bg-[#EFECE6] dark:hover:bg-[#202221] disabled:opacity-30 disabled:pointer-events-none transition-colors"
          >
            <Redo2 className="w-4 h-4" />
          </button>

          <button
            onClick={handleReset}
            title="Reset to default settings"
            className="p-2 rounded-lg border border-[#E2DDD5] dark:border-[#333534] text-[#726F68] hover:bg-[#EFECE6] dark:hover:bg-[#202221] transition-colors"
          >
            <RotateCcw className="w-4 h-4" />
          </button>

          <button
            onClick={handleSaveProject}
            className="px-3 py-1.5 rounded-lg border border-[#DCD7CE] dark:border-[#383A38] text-xs font-medium text-[#2C2B29] dark:text-[#E2E3E0] hover:bg-white dark:hover:bg-[#202221] flex items-center gap-1.5 transition-colors"
          >
            {saveSuccess ? (
              <>
                <Check className="w-3.5 h-3.5 text-emerald-600" />
                <span>Saved</span>
              </>
            ) : (
              <>
                <Save className="w-3.5 h-3.5" />
                <span className="hidden md:inline">Save</span>
              </>
            )}
          </button>

          <button
            onClick={onOpenExportModal}
            className="px-3.5 sm:px-4 py-1.5 rounded-lg bg-[#2C2B29] hover:bg-[#1A1918] text-[#F8F6F0] dark:bg-[#E2E3E0] dark:hover:bg-[#FFFFFF] dark:text-[#121413] text-xs font-medium tracking-wide flex items-center gap-1.5 shadow-sm transition-all hover:scale-[1.02] active:scale-[0.98]"
          >
            <Download className="w-3.5 h-3.5 text-[#8A9A86] dark:text-[#556952]" />
            <span>Export</span>
          </button>
        </div>
      </div>

      {/* Main Studio Workspace Split */}
      <div className="grid grid-cols-1 lg:grid-cols-12 gap-8 items-start">
        {/* Left Column: Realistic Device Canvas Viewport Frame */}
        <div className="lg:col-span-6 flex flex-col items-center">
          {/* Viewport Top Bar: Ratio indicator, Grid cycle, and Real-world UI Preview Selector */}
          <div className="w-full max-w-[360px] flex items-center justify-between text-xs text-[#726F68] dark:text-[#939188] mb-3 px-2">
            <span className="font-mono">{activeRatio.label}</span>

            <div className="flex items-center gap-1.5">
              {/* Quick Visual Crop Grid Toggle Button */}
              <button
                onClick={cycleGridOverlay}
                title="Cycle composition guides: Rule of Thirds (3×3), Golden Ratio (Phi), Center Cross, or None"
                className={`flex items-center gap-1.5 px-2 py-0.5 rounded-lg border transition-colors ${
                  settings.gridOverlay !== 'none'
                    ? 'border-[#8A9A86] dark:border-[#BBCBB8] bg-[#8A9A86]/15 dark:bg-[#BBCBB8]/15 text-[#3D4C3A] dark:text-[#BBCBB8] font-medium'
                    : 'border-[#E2DDD5] dark:border-[#333534] text-[#726F68] hover:text-[#2C2B29] dark:hover:text-white'
                }`}
              >
                <Grid3x3 className="w-3.5 h-3.5" />
                <span className="font-mono text-[11px]">
                  {settings.gridOverlay === 'none'
                    ? 'Grid: Off'
                    : settings.gridOverlay === 'ruleOfThirds'
                    ? '3×3'
                    : settings.gridOverlay === 'goldenRatio'
                    ? 'Golden Φ'
                    : 'Center'}
                </span>
              </button>

              {/* Real-World UI Preview Selector (Lock Screen / App Icons / Clean) */}
              <div className="flex rounded-lg border border-[#E2DDD5] dark:border-[#333534] p-0.5 bg-[#EFECE6]/60 dark:bg-[#1E201F]">
                <button
                  onClick={() => setSettings((prev) => ({ ...prev, uiPreviewMode: 'lockscreen' }))}
                  title="Simulate Android Lock Screen"
                  className={`px-2 py-0.5 rounded text-[10px] font-medium transition-colors ${
                    settings.uiPreviewMode === 'lockscreen'
                      ? 'bg-white dark:bg-[#2C2B29] text-[#2C2B29] dark:text-white font-semibold shadow-xs'
                      : 'text-[#8A8780]'
                  }`}
                >
                  Lock
                </button>
                <button
                  onClick={() => setSettings((prev) => ({ ...prev, uiPreviewMode: 'homescreen' }))}
                  title="Simulate Android Home Screen with App Icon Grid"
                  className={`px-2 py-0.5 rounded text-[10px] font-medium transition-colors ${
                    settings.uiPreviewMode === 'homescreen'
                      ? 'bg-white dark:bg-[#2C2B29] text-[#2C2B29] dark:text-white font-semibold shadow-xs'
                      : 'text-[#8A8780]'
                  }`}
                >
                  Apps
                </button>
                <button
                  onClick={() => setSettings((prev) => ({ ...prev, uiPreviewMode: 'clean' }))}
                  title="Clean Wallpaper View (No UI overlay)"
                  className={`px-2 py-0.5 rounded text-[10px] font-medium transition-colors ${
                    settings.uiPreviewMode === 'clean'
                      ? 'bg-white dark:bg-[#2C2B29] text-[#2C2B29] dark:text-white font-semibold shadow-xs'
                      : 'text-[#8A8780]'
                  }`}
                >
                  Clean
                </button>
              </div>
            </div>
          </div>

          {/* Interactive Frame Container */}
          <div
            className={`relative rounded-[32px] overflow-hidden bg-[#0D0F0E] shadow-2xl ring-1 ring-black/15 dark:ring-white/10 select-none cursor-grab active:cursor-grabbing transition-all ${
              settings.cropRatio === '9:16' || settings.cropRatio === '20:9'
                ? 'w-[280px] sm:w-[320px] aspect-[9/19]'
                : settings.cropRatio === '3:4'
                ? 'w-[320px] sm:w-[360px] aspect-[3/4]'
                : settings.cropRatio === '16:9'
                ? 'w-full max-w-[460px] aspect-[16/9]'
                : 'w-[300px] sm:w-[340px] aspect-square'
            }`}
            onMouseDown={handleMouseDown}
            onMouseMove={handleMouseMove}
            onMouseUp={handleMouseUp}
            onTouchStart={handleTouchStart}
            onTouchMove={handleTouchMove}
            onTouchEnd={handleTouchEnd}
          >
            {/* Realtime Canvas */}
            <canvas ref={canvasRef} className="w-full h-full object-cover block pointer-events-none" />

            {/* Visual Crop Helper: Persistent Grid Overlay */}
            <GridOverlay
              type={settings.gridOverlay}
              opacity={gridOpacity}
              showGoldenSpiral={showGoldenSpiral}
            />

            {/* Mode 1: Simulated Android Lock Screen */}
            {settings.uiPreviewMode === 'lockscreen' && (settings.cropRatio === '9:16' || settings.cropRatio === '20:9') && (
              <div className="absolute inset-0 pointer-events-none p-5 flex flex-col justify-between text-white select-none z-20">
                <div className="pt-6 text-center space-y-1">
                  <p className="text-xs uppercase tracking-widest opacity-85 font-medium drop-shadow">
                    {dateString || 'Mon, Oct 21'}
                  </p>
                  <h3 className="font-sans font-extralight text-6xl tracking-tight leading-none drop-shadow-md">
                    {timeString || '12:00'}
                  </h3>
                </div>
                <div className="pb-4 text-center">
                  <div className="w-24 h-1 mx-auto rounded-full bg-white/40 backdrop-blur-sm" />
                </div>
              </div>
            )}

            {/* Mode 2: Simulated Android Home Screen with App Icon Grid */}
            {settings.uiPreviewMode === 'homescreen' && (settings.cropRatio === '9:16' || settings.cropRatio === '20:9') && (
              <HomeScreenSimulation />
            )}

            {/* Drag guide indicator overlay when zooming */}
            {settings.zoom > 1.05 && (
              <div className="absolute top-3 left-3 px-2 py-1 rounded bg-black/60 text-white/90 text-[10px] font-medium flex items-center gap-1 backdrop-blur-sm pointer-events-none z-30">
                <Move className="w-3 h-3" />
                <span>Drag to pan</span>
              </div>
            )}
          </div>

          <p className="text-[11px] text-[#726F68] dark:text-[#939188] mt-3 text-center">
            Drag to pan · Toggle Apps/Lock above to test legibility · Guides & overlays are never exported
          </p>

          {/* Android Material You (Monet) Extracted Palette Card */}
          {palette && (
            <div className="w-full max-w-[360px] mt-4 p-3.5 rounded-2xl bg-white dark:bg-[#1A1C1B] border border-[#E8E4DC] dark:border-[#2D302F] shadow-xs space-y-2.5">
              <div className="flex items-center justify-between text-xs">
                <div className="flex items-center gap-1.5">
                  <Palette className="w-3.5 h-3.5 text-[#556952] dark:text-[#BBCBB8]" />
                  <span className="font-semibold text-[#2C2B29] dark:text-[#F3F4F1]">
                    Material You (Monet) Palette
                  </span>
                </div>
                <button
                  onClick={copyMonetPalette}
                  className="text-[11px] text-[#556952] dark:text-[#BBCBB8] hover:underline flex items-center gap-1 font-medium"
                >
                  {paletteCopied ? (
                    <>
                      <Check className="w-3 h-3 text-emerald-600" />
                      <span>Copied</span>
                    </>
                  ) : (
                    <>
                      <Copy className="w-3 h-3" />
                      <span>Copy Hex</span>
                    </>
                  )}
                </button>
              </div>

              {/* 5 Organic Natural Swatches */}
              <div className="flex items-center justify-between gap-2 pt-1">
                {palette.swatches.map((color, idx) => (
                  <button
                    key={idx}
                    onClick={() =>
                      setSettings((prev) => ({
                        ...prev,
                        textOverlay: { ...prev.textOverlay, color },
                      }))
                    }
                    title={`Click to set typography color to ${color}`}
                    className="flex-1 flex flex-col items-center gap-1 group"
                  >
                    <div
                      className="w-full aspect-square rounded-xl shadow-xs border border-black/10 group-hover:scale-105 transition-transform"
                      style={{ backgroundColor: color }}
                    />
                    <span className="font-mono text-[9px] text-[#726F68] dark:text-[#939188] truncate max-w-[48px]">
                      {color}
                    </span>
                  </button>
                ))}
              </div>

              {/* Quick action to launch Dynamic Nature Mesh generator */}
              <div className="pt-2 border-t border-[#E8E4DC] dark:border-[#2D302F] flex items-center justify-between text-xs">
                <span className="text-[11px] text-[#726F68] dark:text-[#939188]">
                  Dynamic Mesh Generator:
                </span>
                <button
                  onClick={() => {
                    setSettings((prev) => ({
                      ...prev,
                      meshGradient: {
                        ...(prev.meshGradient || {
                          preset: 'biophilic',
                          intensity: 65,
                          blur: 35,
                          blendMode: 'soft-light',
                          seed: 42,
                        }),
                        enabled: true,
                        customPrimaryColor: prev.meshGradient?.customPrimaryColor || palette.dominant,
                      },
                    }));
                    setActiveTab('mesh');
                  }}
                  className="px-2.5 py-1 rounded-lg bg-[#556952]/10 hover:bg-[#556952]/20 dark:bg-[#BBCBB8]/15 dark:hover:bg-[#BBCBB8]/25 text-[#3D4C3A] dark:text-[#BBCBB8] text-[11px] font-medium flex items-center gap-1.5 transition-colors"
                >
                  <Sparkles className="w-3.5 h-3.5 text-[#556952] dark:text-[#BBCBB8]" />
                  <span>Generate Mesh</span>
                </button>
              </div>
            </div>
          )}
        </div>

        {/* Right Column: Editor Controls & Tuning Panel */}
        <div className="lg:col-span-6 space-y-6">
          {/* Tab Selector Buttons */}
          <div className="flex border-b border-[#E8E4DC] dark:border-[#2D302F]">
            {[
              { id: 'crop', label: 'Crop & Guides', icon: Maximize2 },
              { id: 'tune', label: 'Atmosphere', icon: Sliders },
              { id: 'mesh', label: 'Nature Mesh', icon: Sparkles },
              { id: 'washes', label: 'Zen Washes', icon: Palette },
              { id: 'text', label: 'Typography', icon: Type },
            ].map((tab) => {
              const Icon = tab.icon;
              const isActive = activeTab === tab.id;
              return (
                <button
                  key={tab.id}
                  onClick={() => setActiveTab(tab.id as any)}
                  className={`flex-1 py-3 px-1 sm:px-2 flex items-center justify-center gap-1 text-[11px] sm:text-xs font-medium border-b-2 transition-colors ${
                    isActive
                      ? 'border-[#8A9A86] text-[#2C2B29] dark:text-[#F3F4F1] font-semibold'
                      : 'border-transparent text-[#726F68] dark:text-[#939188] hover:text-[#2C2B29] dark:hover:text-[#F3F4F1]'
                  }`}
                >
                  <Icon className="w-3.5 h-3.5 sm:w-4 sm:h-4 shrink-0" />
                  <span className="truncate">{tab.label}</span>
                </button>
              );
            })}
          </div>

          {/* TAB 1: CROP & COMPOSITION GUIDES */}
          {activeTab === 'crop' && (
            <div className="space-y-6 animate-in fade-in duration-200">
              {/* Visual Crop Helper: Composition Grid Overlay Selector */}
              <div className="space-y-3 p-4 rounded-2xl bg-white dark:bg-[#1A1C1B] border border-[#E8E4DC] dark:border-[#2D302F]">
                <div className="flex items-center justify-between">
                  <div className="flex items-center gap-2">
                    <Grid3x3 className="w-4 h-4 text-[#556952] dark:text-[#BBCBB8]" />
                    <h3 className="font-semibold text-xs text-[#2C2B29] dark:text-[#F3F4F1]">
                      Visual Crop & Composition Guides
                    </h3>
                  </div>
                  <span className="text-[10px] font-mono uppercase text-[#726F68] dark:text-[#939188] bg-[#EFECE6] dark:bg-[#282A29] px-2 py-0.5 rounded">
                    Persistent Overlay
                  </span>
                </div>

                <div className="grid grid-cols-2 gap-2.5">
                  {[
                    {
                      id: 'ruleOfThirds' as GridOverlayType,
                      label: '3×3 Rule of Thirds',
                      desc: 'Balances horizons and subject anchors on the 1/3 power points',
                    },
                    {
                      id: 'goldenRatio' as GridOverlayType,
                      label: 'Golden Ratio (Phi Φ)',
                      desc: 'Divine 1:1.618 ratio creating natural flowing organic harmony',
                    },
                    {
                      id: 'centerCross' as GridOverlayType,
                      label: 'Center & Horizon Cross',
                      desc: 'Exact 50% axes for symmetrical reflections & centered framing',
                    },
                    {
                      id: 'none' as GridOverlayType,
                      label: 'No Grid Overlay',
                      desc: 'Clean unobstructed wallpaper view without visual guides',
                    },
                  ].map((guide) => {
                    const isSelected = settings.gridOverlay === guide.id;
                    return (
                      <button
                        key={guide.id}
                        onClick={() =>
                          setSettings((prev) => ({
                            ...prev,
                            gridOverlay: guide.id,
                          }))
                        }
                        className={`p-3 rounded-xl text-left border transition-all flex flex-col justify-between h-[96px] ${
                          isSelected
                            ? 'border-[#2C2B29] dark:border-white ring-1 ring-[#2C2B29] dark:ring-white bg-[#F8F6F0] dark:bg-[#202221]'
                            : 'border-[#E2DDD5] dark:border-[#333534] bg-white dark:bg-[#1A1C1B] hover:border-[#8A9A86]'
                        }`}
                      >
                        <div className="flex items-center justify-between">
                          <span className="font-medium text-xs text-[#2C2B29] dark:text-[#F3F4F1]">
                            {guide.label}
                          </span>
                          {isSelected && <Check className="w-3.5 h-3.5 text-[#556952] dark:text-[#BBCBB8]" />}
                        </div>
                        <p className="text-[10px] text-[#726F68] dark:text-[#939188] line-clamp-2">
                          {guide.desc}
                        </p>
                      </button>
                    );
                  })}
                </div>

                {settings.gridOverlay !== 'none' && (
                  <div className="pt-3 border-t border-[#E8E4DC] dark:border-[#2D302F] space-y-3 text-xs">
                    {settings.gridOverlay === 'goldenRatio' && (
                      <div className="flex items-center justify-between p-2.5 rounded-lg bg-[#EFECE6]/60 dark:bg-[#202221]">
                        <div>
                          <p className="font-medium text-[#2C2B29] dark:text-[#F3F4F1]">
                            Show Fibonacci Golden Spiral
                          </p>
                          <p className="text-[10px] text-[#726F68] dark:text-[#939188]">
                            Natural logarithmic curve for sand waves & ridges
                          </p>
                        </div>
                        <input
                          type="checkbox"
                          checked={showGoldenSpiral}
                          onChange={(e) => setShowGoldenSpiral(e.target.checked)}
                          className="w-4 h-4 accent-[#8A9A86] cursor-pointer rounded"
                        />
                      </div>
                    )}

                    <div className="flex items-center justify-between">
                      <span className="text-[#726F68] dark:text-[#939188]">Guide Visibility / Opacity</span>
                      <div className="flex gap-1">
                        {(['subtle', 'medium', 'high'] as GridOpacity[]).map((lvl) => (
                          <button
                            key={lvl}
                            onClick={() => setGridOpacity(lvl)}
                            className={`px-2.5 py-1 rounded-md text-[11px] font-medium capitalize transition-colors ${
                              gridOpacity === lvl
                                ? 'bg-[#2C2B29] text-white dark:bg-white dark:text-black'
                                : 'border border-[#E2DDD5] dark:border-[#333534] text-[#726F68] hover:bg-[#EFECE6]'
                            }`}
                          >
                            {lvl}
                          </button>
                        ))}
                      </div>
                    </div>
                  </div>
                )}
              </div>

              {/* Device Aspect Ratio Presets */}
              <div className="space-y-3">
                <h3 className="font-semibold text-xs text-[#2C2B29] dark:text-[#F3F4F1]">
                  Target Device Framing & Ratio
                </h3>

                <div className="space-y-2">
                  {ASPECT_RATIOS.map((ratio) => {
                    const isSelected = settings.cropRatio === ratio.id;
                    return (
                      <button
                        key={ratio.id}
                        onClick={() =>
                          setSettings((prev) => ({
                            ...prev,
                            cropRatio: ratio.id,
                          }))
                        }
                        className={`w-full p-3.5 rounded-xl text-left border transition-all flex items-center justify-between ${
                          isSelected
                            ? 'border-[#2C2B29] dark:border-white ring-1 ring-[#2C2B29] dark:ring-white bg-white dark:bg-[#202221]'
                            : 'border-[#E2DDD5] dark:border-[#333534] bg-[#EFECE6]/40 dark:bg-[#1A1C1B]/40 hover:bg-white'
                        }`}
                      >
                        <div className="space-y-0.5">
                          <div className="flex items-center gap-2">
                            <span className="font-semibold text-xs text-[#2C2B29] dark:text-[#F3F4F1]">
                              {ratio.label}
                            </span>
                            <span className="text-[10px] uppercase font-mono px-1.5 py-0.5 rounded bg-[#EFECE6] dark:bg-[#2A2C2B] text-[#726F68] dark:text-[#939188]">
                              {ratio.deviceType}
                            </span>
                          </div>
                          <p className="text-[11px] text-[#726F68] dark:text-[#939188]">
                            {ratio.description} · Target: {ratio.idealWidth} × {ratio.idealHeight} px
                          </p>
                        </div>
                        {isSelected && <Check className="w-4 h-4 text-[#556952] dark:text-[#BBCBB8]" />}
                      </button>
                    );
                  })}
                </div>
              </div>
            </div>
          )}

          {/* TAB 2: ATMOSPHERIC & VISUAL ADJUSTMENTS */}
          {activeTab === 'tune' && (
            <div className="space-y-6 animate-in fade-in duration-200">
              {/* Feature 5: Advanced Atmospheric Filters */}
              <div className="p-4 rounded-2xl bg-white dark:bg-[#1A1C1B] border border-[#E8E4DC] dark:border-[#2D302F] space-y-4">
                <div className="flex items-center gap-2">
                  <Sun className="w-4 h-4 text-[#D89E6E]" />
                  <h3 className="font-semibold text-xs text-[#2C2B29] dark:text-[#F3F4F1]">
                    Atmospheric Effects (Mist, Komorebi & Washi)
                  </h3>
                </div>

                {/* Mist Gradient (Negative space for phone icons) */}
                <div className="space-y-2">
                  <div className="flex justify-between text-xs">
                    <span className="font-medium text-[#2C2B29] dark:text-[#F3F4F1]">
                      Mist Gradient (Creates clean space for phone dock/apps)
                    </span>
                    <span className="font-mono text-[#726F68] dark:text-[#939188]">
                      {settings.mistGradient === 'none' ? 'Off' : `${settings.mistIntensity}%`}
                    </span>
                  </div>
                  <div className="grid grid-cols-3 gap-1.5 text-xs">
                    {[
                      { id: 'none', label: 'None' },
                      { id: 'bottom', label: 'Bottom (Dock/Apps)' },
                      { id: 'top', label: 'Top (Clock/Widgets)' },
                    ].map((m) => (
                      <button
                        key={m.id}
                        onClick={() =>
                          setSettings((prev) => ({
                            ...prev,
                            mistGradient: m.id as any,
                          }))
                        }
                        className={`py-1.5 rounded-lg border text-[11px] font-medium transition-colors ${
                          settings.mistGradient === m.id
                            ? 'bg-[#2C2B29] text-white dark:bg-white dark:text-black'
                            : 'border-[#E2DDD5] dark:border-[#333534] text-[#726F68]'
                        }`}
                      >
                        {m.label}
                      </button>
                    ))}
                  </div>

                  {settings.mistGradient !== 'none' && (
                    <input
                      type="range"
                      min="15"
                      max="90"
                      step="5"
                      value={settings.mistIntensity}
                      onChange={(e) =>
                        setSettings((prev) => ({
                          ...prev,
                          mistIntensity: parseInt(e.target.value),
                        }))
                      }
                      className="w-full accent-[#8A9A86] cursor-pointer mt-1"
                    />
                  )}
                </div>

                {/* Komorebi (Dappled Sunlight Rays) */}
                <div className="space-y-1.5 pt-2 border-t border-[#E8E4DC] dark:border-[#2D302F]">
                  <div className="flex justify-between text-xs">
                    <span className="font-medium text-[#2C2B29] dark:text-[#F3F4F1]">
                      Komorebi (Dappled Sunlight Sunbeams)
                    </span>
                    <span className="font-mono text-[#726F68] dark:text-[#939188]">
                      {settings.komorebiRays}%
                    </span>
                  </div>
                  <input
                    type="range"
                    min="0"
                    max="80"
                    step="5"
                    value={settings.komorebiRays}
                    onChange={(e) =>
                      setSettings((prev) => ({
                        ...prev,
                        komorebiRays: parseInt(e.target.value),
                      }))
                    }
                    className="w-full accent-[#8A9A86] cursor-pointer"
                  />
                </div>

                {/* Washi Paper & Silk Texture */}
                <div className="space-y-1.5 pt-2 border-t border-[#E8E4DC] dark:border-[#2D302F]">
                  <div className="flex justify-between text-xs">
                    <span className="font-medium text-[#2C2B29] dark:text-[#F3F4F1]">
                      Washi Handmade Paper Texture
                    </span>
                    <span className="font-mono text-[#726F68] dark:text-[#939188]">
                      {settings.washiTexture}%
                    </span>
                  </div>
                  <input
                    type="range"
                    min="0"
                    max="70"
                    step="5"
                    value={settings.washiTexture}
                    onChange={(e) =>
                      setSettings((prev) => ({
                        ...prev,
                        washiTexture: parseInt(e.target.value),
                      }))
                    }
                    className="w-full accent-[#8A9A86] cursor-pointer"
                  />
                </div>
              </div>

              {/* Standard Tuning Sliders */}
              <div className="space-y-4">
                <div className="space-y-1.5">
                  <div className="flex justify-between text-xs">
                    <span className="font-medium text-[#2C2B29] dark:text-[#F3F4F1]">Scale & Zoom</span>
                    <span className="font-mono text-[#726F68] dark:text-[#939188]">
                      {settings.zoom.toFixed(2)}×
                    </span>
                  </div>
                  <input
                    type="range"
                    min="1.0"
                    max="2.5"
                    step="0.05"
                    value={settings.zoom}
                    onChange={(e) =>
                      setSettings((prev) => ({ ...prev, zoom: parseFloat(e.target.value) }))
                    }
                    className="w-full accent-[#8A9A86] cursor-pointer"
                  />
                </div>

                <div className="space-y-1.5">
                  <div className="flex justify-between text-xs">
                    <span className="font-medium text-[#2C2B29] dark:text-[#F3F4F1]">Brightness</span>
                    <span className="font-mono text-[#726F68] dark:text-[#939188]">
                      {settings.brightness}%
                    </span>
                  </div>
                  <input
                    type="range"
                    min="60"
                    max="140"
                    step="1"
                    value={settings.brightness}
                    onChange={(e) =>
                      setSettings((prev) => ({ ...prev, brightness: parseInt(e.target.value) }))
                    }
                    className="w-full accent-[#8A9A86] cursor-pointer"
                  />
                </div>

                <div className="space-y-1.5">
                  <div className="flex justify-between text-xs">
                    <span className="font-medium text-[#2C2B29] dark:text-[#F3F4F1]">Contrast</span>
                    <span className="font-mono text-[#726F68] dark:text-[#939188]">
                      {settings.contrast}%
                    </span>
                  </div>
                  <input
                    type="range"
                    min="60"
                    max="140"
                    step="1"
                    value={settings.contrast}
                    onChange={(e) =>
                      setSettings((prev) => ({ ...prev, contrast: parseInt(e.target.value) }))
                    }
                    className="w-full accent-[#8A9A86] cursor-pointer"
                  />
                </div>

                <div className="space-y-1.5">
                  <div className="flex justify-between text-xs">
                    <span className="font-medium text-[#2C2B29] dark:text-[#F3F4F1]">Saturation</span>
                    <span className="font-mono text-[#726F68] dark:text-[#939188]">
                      {settings.saturation}%
                    </span>
                  </div>
                  <input
                    type="range"
                    min="0"
                    max="140"
                    step="1"
                    value={settings.saturation}
                    onChange={(e) =>
                      setSettings((prev) => ({ ...prev, saturation: parseInt(e.target.value) }))
                    }
                    className="w-full accent-[#8A9A86] cursor-pointer"
                  />
                </div>

                <div className="space-y-1.5">
                  <div className="flex justify-between text-xs">
                    <span className="font-medium text-[#2C2B29] dark:text-[#F3F4F1]">Color Warmth</span>
                    <span className="font-mono text-[#726F68] dark:text-[#939188]">
                      {settings.warmth > 0 ? `+${settings.warmth} Warm` : settings.warmth < 0 ? `${settings.warmth} Cool` : 'Neutral'}
                    </span>
                  </div>
                  <input
                    type="range"
                    min="-40"
                    max="40"
                    step="2"
                    value={settings.warmth}
                    onChange={(e) =>
                      setSettings((prev) => ({ ...prev, warmth: parseInt(e.target.value) }))
                    }
                    className="w-full accent-[#8A9A86] cursor-pointer"
                  />
                </div>

                <div className="space-y-1.5">
                  <div className="flex justify-between text-xs">
                    <span className="font-medium text-[#2C2B29] dark:text-[#F3F4F1]">Soft Focus / Blur</span>
                    <span className="font-mono text-[#726F68] dark:text-[#939188]">
                      {settings.blur} px
                    </span>
                  </div>
                  <input
                    type="range"
                    min="0"
                    max="15"
                    step="1"
                    value={settings.blur}
                    onChange={(e) =>
                      setSettings((prev) => ({ ...prev, blur: parseInt(e.target.value) }))
                    }
                    className="w-full accent-[#8A9A86] cursor-pointer"
                  />
                </div>

                <div className="space-y-1.5">
                  <div className="flex justify-between text-xs">
                    <span className="font-medium text-[#2C2B29] dark:text-[#F3F4F1]">Vignette Edge Falloff</span>
                    <span className="font-mono text-[#726F68] dark:text-[#939188]">
                      {settings.vignette}%
                    </span>
                  </div>
                  <input
                    type="range"
                    min="0"
                    max="60"
                    step="2"
                    value={settings.vignette}
                    onChange={(e) =>
                      setSettings((prev) => ({ ...prev, vignette: parseInt(e.target.value) }))
                    }
                    className="w-full accent-[#8A9A86] cursor-pointer"
                  />
                </div>

                <div className="space-y-1.5">
                  <div className="flex justify-between text-xs">
                    <span className="font-medium text-[#2C2B29] dark:text-[#F3F4F1]">Analog Film Grain</span>
                    <span className="font-mono text-[#726F68] dark:text-[#939188]">
                      {settings.grain}%
                    </span>
                  </div>
                  <input
                    type="range"
                    min="0"
                    max="35"
                    step="1"
                    value={settings.grain}
                    onChange={(e) =>
                      setSettings((prev) => ({ ...prev, grain: parseInt(e.target.value) }))
                    }
                    className="w-full accent-[#8A9A86] cursor-pointer"
                  />
                </div>
              </div>
            </div>
          )}

          {/* TAB: DYNAMIC NATURE MESH GRADIENT BACKGROUND GENERATOR */}
          {activeTab === 'mesh' && (
            <div className="space-y-6 animate-in fade-in duration-200">
              {/* Feature Intro & Master Activation Banner */}
              <div className="p-4 sm:p-5 rounded-2xl bg-white dark:bg-[#1A1C1B] border border-[#E8E4DC] dark:border-[#2D302F] shadow-xs space-y-4">
                <div className="flex items-start justify-between gap-4">
                  <div className="space-y-1">
                    <div className="flex items-center gap-2">
                      <Sparkles className="w-4 h-4 text-[#556952] dark:text-[#BBCBB8]" />
                      <h3 className="font-semibold text-sm text-[#2C2B29] dark:text-[#F3F4F1]">
                        Dynamic Nature Mesh Background
                      </h3>
                      <span className="text-[10px] font-mono uppercase px-2 py-0.5 rounded bg-[#8A9A86]/15 text-[#3D4C3A] dark:text-[#BBCBB8] font-medium">
                        Generative
                      </span>
                    </div>
                    <p className="text-xs text-[#726F68] dark:text-[#939188] leading-relaxed">
                      Creates subtle, multi-stop organic gradients dynamically derived from the primary colors extracted from your selected wallpaper.
                    </p>
                  </div>

                  {/* Enable / Disable Toggle Switch */}
                  <button
                    onClick={() =>
                      updateSettingsWithHistory({
                        ...settings,
                        meshGradient: {
                          ...(settings.meshGradient || {
                            preset: 'biophilic',
                            intensity: 65,
                            blur: 35,
                            blendMode: 'soft-light',
                            seed: 42,
                          }),
                          enabled: !settings.meshGradient?.enabled,
                        },
                      })
                    }
                    className={`relative inline-flex h-6 w-11 shrink-0 cursor-pointer rounded-full border-2 border-transparent transition-colors duration-200 ease-in-out focus:outline-hidden ${
                      settings.meshGradient?.enabled ? 'bg-[#556952] dark:bg-[#8A9A86]' : 'bg-[#DCD7CE] dark:bg-[#383A38]'
                    }`}
                    role="switch"
                    aria-checked={Boolean(settings.meshGradient?.enabled)}
                  >
                    <span
                      aria-hidden="true"
                      className={`pointer-events-none inline-block h-5 w-5 transform rounded-full bg-white shadow-lg ring-0 transition duration-200 ease-in-out ${
                        settings.meshGradient?.enabled ? 'translate-x-5' : 'translate-x-0'
                      }`}
                    />
                  </button>
                </div>

                {/* Primary Color Root Picker */}
                <div className="pt-3 border-t border-[#E8E4DC] dark:border-[#2D302F] space-y-2.5">
                  <div className="flex items-center justify-between text-xs">
                    <span className="font-medium text-[#2C2B29] dark:text-[#F3F4F1]">
                      Root Color Anchor (Extracted from Wallpaper)
                    </span>
                    <span className="font-mono text-[11px] text-[#726F68] dark:text-[#939188]">
                      {settings.meshGradient?.customPrimaryColor || palette?.dominant || '#556952'}
                    </span>
                  </div>

                  <div className="flex items-center gap-2">
                    {(palette?.swatches || ['#556952', '#8A9A86', '#73716B', '#F8F6F0', '#121413']).map(
                      (swatch, idx) => {
                        const activeColor =
                          settings.meshGradient?.customPrimaryColor || palette?.dominant || '#556952';
                        const isSelected = activeColor.toLowerCase() === swatch.toLowerCase();
                        return (
                          <button
                            key={idx}
                            onClick={() =>
                              setSettings((prev) => ({
                                ...prev,
                                meshGradient: {
                                  ...(prev.meshGradient || {
                                    preset: 'biophilic',
                                    intensity: 65,
                                    blur: 35,
                                    blendMode: 'soft-light',
                                    seed: 42,
                                  }),
                                  enabled: true,
                                  customPrimaryColor: swatch,
                                },
                              }))
                            }
                            title={`Use ${swatch} as primary anchor for mesh`}
                            className={`flex-1 flex flex-col items-center gap-1.5 p-1 rounded-xl transition-all ${
                              isSelected
                                ? 'ring-2 ring-[#2C2B29] dark:ring-white bg-[#EFECE6]/50 dark:bg-[#282A29]'
                                : 'hover:scale-105'
                            }`}
                          >
                            <div
                              className="w-full h-8 rounded-lg shadow-2xs border border-black/10"
                              style={{ backgroundColor: swatch }}
                            />
                            <span className="font-mono text-[10px] text-[#726F68] dark:text-[#939188]">
                              {idx === 0 ? 'Dom' : idx === 1 ? 'Sec' : idx === 2 ? 'Mute' : idx === 3 ? 'Light' : 'Dark'}
                            </span>
                          </button>
                        );
                      }
                    )}
                  </div>
                </div>
              </div>

              {/* Nature-Inspired Mesh Presets */}
              <div className="space-y-3">
                <div className="flex items-center justify-between">
                  <h3 className="font-semibold text-xs text-[#2C2B29] dark:text-[#F3F4F1]">
                    Nature Mesh Presets
                  </h3>
                  <span className="text-[10px] font-mono text-[#726F68] dark:text-[#939188]">
                    6 Algorithmic Palettes
                  </span>
                </div>

                <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                  {MESH_PRESETS.map((preset) => {
                    const currentPrimary =
                      settings.meshGradient?.customPrimaryColor || palette?.dominant || '#556952';
                    const isSelected =
                      (settings.meshGradient?.preset || 'biophilic') === preset.id &&
                      Boolean(settings.meshGradient?.enabled);

                    // Dynamic CSS gradient preview generated on the fly for each card
                    const previewGradient = generateMeshCssGradient(
                      currentPrimary,
                      preset.id,
                      settings.meshGradient?.seed || 42,
                      85
                    );

                    return (
                      <button
                        key={preset.id}
                        onClick={() =>
                          updateSettingsWithHistory({
                            ...settings,
                            meshGradient: {
                              ...(settings.meshGradient || {
                                intensity: 65,
                                blur: 35,
                                blendMode: 'soft-light',
                                seed: 42,
                              }),
                              enabled: true,
                              preset: preset.id,
                              customPrimaryColor: currentPrimary,
                            },
                          })
                        }
                        className={`p-3.5 rounded-2xl text-left border transition-all flex flex-col justify-between gap-3 relative overflow-hidden group ${
                          isSelected
                            ? 'border-[#2C2B29] dark:border-white ring-2 ring-[#2C2B29] dark:ring-white bg-white dark:bg-[#1E201F] shadow-sm'
                            : 'border-[#E2DDD5] dark:border-[#333534] bg-white dark:bg-[#1A1C1B] hover:border-[#8A9A86]'
                        }`}
                      >
                        <div className="flex items-center justify-between gap-2">
                          {/* Mini Realtime Mesh Preview Bubble */}
                          <div
                            className="w-12 h-12 rounded-xl shadow-xs border border-black/10 shrink-0 group-hover:scale-105 transition-transform"
                            style={{ background: previewGradient }}
                          />
                          <div className="flex-1 min-w-0">
                            <div className="flex items-center gap-1.5">
                              <span className="font-semibold text-xs text-[#2C2B29] dark:text-[#F3F4F1] truncate">
                                {preset.name}
                              </span>
                              <span className="font-serif text-[11px] text-[#726F68] dark:text-[#939188]">
                                {preset.japaneseName}
                              </span>
                            </div>
                            <p className="text-[10px] text-[#726F68] dark:text-[#939188] truncate">
                              {preset.subtitle}
                            </p>
                          </div>
                          {isSelected && (
                            <div className="w-5 h-5 rounded-full bg-[#2C2B29] dark:bg-white text-white dark:text-black flex items-center justify-center shrink-0">
                              <Check className="w-3 h-3" />
                            </div>
                          )}
                        </div>

                        <p className="text-[11px] text-[#726F68] dark:text-[#939188] line-clamp-2">
                          {preset.description}
                        </p>

                        <div className="flex items-center justify-between pt-1 text-[10px]">
                          <span className="font-mono text-[#556952] dark:text-[#BBCBB8]">
                            {preset.moodTag}
                          </span>
                        </div>
                      </button>
                    );
                  })}
                </div>
              </div>

              {/* Application / Blend Mode Selector */}
              <div className="p-4 rounded-2xl bg-white dark:bg-[#1A1C1B] border border-[#E8E4DC] dark:border-[#2D302F] space-y-3">
                <div className="flex items-center justify-between">
                  <div className="flex items-center gap-2">
                    <Layers className="w-4 h-4 text-[#556952] dark:text-[#BBCBB8]" />
                    <h3 className="font-semibold text-xs text-[#2C2B29] dark:text-[#F3F4F1]">
                      Application & Blend Mode
                    </h3>
                  </div>
                  <span className="text-[10px] font-mono text-[#726F68] dark:text-[#939188]">
                    Canvas Integration
                  </span>
                </div>

                <div className="grid grid-cols-2 gap-2">
                  {[
                    {
                      id: 'soft-light' as MeshBlendMode,
                      label: 'Atmospheric Wash',
                      desc: 'Subtle soft light tint blending with the photo',
                    },
                    {
                      id: 'overlay' as MeshBlendMode,
                      label: 'Vibrant Harmony',
                      desc: 'Rich atmospheric contrast and color depth',
                    },
                    {
                      id: 'backdrop' as MeshBlendMode,
                      label: 'Ambient Backdrop',
                      desc: 'Background layer behind framed wallpapers',
                    },
                    {
                      id: 'pure' as MeshBlendMode,
                      label: 'Pure Mesh Wallpaper',
                      desc: 'Standalone minimalist nature gradient wallpaper',
                    },
                  ].map((mode) => {
                    const isSelected =
                      (settings.meshGradient?.blendMode || 'soft-light') === mode.id;
                    return (
                      <button
                        key={mode.id}
                        onClick={() =>
                          updateSettingsWithHistory({
                            ...settings,
                            meshGradient: {
                              ...(settings.meshGradient || {
                                preset: 'biophilic',
                                intensity: 65,
                                blur: 35,
                                seed: 42,
                              }),
                              enabled: true,
                              blendMode: mode.id,
                              customPrimaryColor:
                                settings.meshGradient?.customPrimaryColor ||
                                palette?.dominant ||
                                '#556952',
                            },
                          })
                        }
                        className={`p-2.5 rounded-xl text-left border transition-all ${
                          isSelected
                            ? 'border-[#2C2B29] dark:border-white ring-1 ring-[#2C2B29] dark:ring-white bg-[#F8F6F0] dark:bg-[#202221]'
                            : 'border-[#E2DDD5] dark:border-[#333534] bg-white dark:bg-[#1A1C1B] hover:border-[#8A9A86]'
                        }`}
                      >
                        <div className="flex items-center justify-between">
                          <span className="font-medium text-xs text-[#2C2B29] dark:text-[#F3F4F1]">
                            {mode.label}
                          </span>
                          {isSelected && <Check className="w-3.5 h-3.5 text-[#556952] dark:text-[#BBCBB8]" />}
                        </div>
                        <p className="text-[10px] text-[#726F68] dark:text-[#939188] mt-1">
                          {mode.desc}
                        </p>
                      </button>
                    );
                  })}
                </div>
              </div>

              {/* Tuning Sliders: Intensity, Blur & Seed Shuffler */}
              <div className="p-4 rounded-2xl bg-white dark:bg-[#1A1C1B] border border-[#E8E4DC] dark:border-[#2D302F] space-y-4">
                <div className="flex items-center justify-between">
                  <h3 className="font-semibold text-xs text-[#2C2B29] dark:text-[#F3F4F1]">
                    Fine-Tuning & Generative Seed
                  </h3>

                  {/* Seed Shuffle Button */}
                  <button
                    onClick={() =>
                      setSettings((prev) => ({
                        ...prev,
                        meshGradient: {
                          ...(prev.meshGradient || {
                            preset: 'biophilic',
                            intensity: 65,
                            blur: 35,
                            blendMode: 'soft-light',
                            seed: 42,
                          }),
                          enabled: true,
                          seed: ((prev.meshGradient?.seed || 42) + 17) % 1000,
                        },
                      }))
                    }
                    title="Nudge focal nodes and randomize organic positions"
                    className="px-2.5 py-1 rounded-lg border border-[#DCD7CE] dark:border-[#383A38] text-[11px] font-medium text-[#2C2B29] dark:text-[#E2E3E0] hover:bg-[#EFECE6] dark:hover:bg-[#202221] flex items-center gap-1.5 transition-colors"
                  >
                    <Shuffle className="w-3 h-3 text-[#556952] dark:text-[#BBCBB8]" />
                    <span>Nudge Nodes</span>
                  </button>
                </div>

                {/* Mesh Intensity Slider */}
                <div className="space-y-1.5">
                  <div className="flex justify-between text-xs">
                    <span className="font-medium text-[#2C2B29] dark:text-[#F3F4F1]">
                      Mesh Gradient Intensity
                    </span>
                    <span className="font-mono text-[#726F68] dark:text-[#939188]">
                      {settings.meshGradient?.intensity ?? 65}%
                    </span>
                  </div>
                  <input
                    type="range"
                    min="15"
                    max="100"
                    step="5"
                    value={settings.meshGradient?.intensity ?? 65}
                    onChange={(e) =>
                      setSettings((prev) => ({
                        ...prev,
                        meshGradient: {
                          ...(prev.meshGradient || {
                            preset: 'biophilic',
                            intensity: 65,
                            blur: 35,
                            blendMode: 'soft-light',
                            seed: 42,
                          }),
                          enabled: true,
                          intensity: parseInt(e.target.value),
                        },
                      }))
                    }
                    className="w-full accent-[#8A9A86] cursor-pointer"
                  />
                </div>

                {/* Diffusion / Soft Focus Blur Slider */}
                <div className="space-y-1.5 pt-2 border-t border-[#E8E4DC] dark:border-[#2D302F]">
                  <div className="flex justify-between text-xs">
                    <span className="font-medium text-[#2C2B29] dark:text-[#F3F4F1]">
                      Atmospheric Diffusion (Blur)
                    </span>
                    <span className="font-mono text-[#726F68] dark:text-[#939188]">
                      {settings.meshGradient?.blur ?? 35} px
                    </span>
                  </div>
                  <input
                    type="range"
                    min="5"
                    max="60"
                    step="5"
                    value={settings.meshGradient?.blur ?? 35}
                    onChange={(e) =>
                      setSettings((prev) => ({
                        ...prev,
                        meshGradient: {
                          ...(prev.meshGradient || {
                            preset: 'biophilic',
                            intensity: 65,
                            blur: 35,
                            blendMode: 'soft-light',
                            seed: 42,
                          }),
                          enabled: true,
                          blur: parseInt(e.target.value),
                        },
                      }))
                    }
                    className="w-full accent-[#8A9A86] cursor-pointer"
                  />
                </div>

                {/* Derived Harmonic Palette Stops display */}
                <div className="pt-2 border-t border-[#E8E4DC] dark:border-[#2D302F] space-y-1.5">
                  <div className="flex justify-between items-center text-xs">
                    <span className="font-medium text-[#2C2B29] dark:text-[#F3F4F1]">
                      Derived Nature Color Stops
                    </span>
                    <span className="text-[10px] text-[#726F68] dark:text-[#939188]">
                      Click stop to copy
                    </span>
                  </div>
                  <div className="flex items-center gap-1.5">
                    {generateMeshColors(
                      settings.meshGradient?.customPrimaryColor || palette?.dominant || '#556952',
                      settings.meshGradient?.preset || 'biophilic'
                    ).map((color, i) => (
                      <button
                        key={i}
                        onClick={() => navigator.clipboard.writeText(color)}
                        title={`Derived stop ${i + 1}: ${color} (Click to copy)`}
                        className="flex-1 h-7 rounded-lg border border-black/10 shadow-2xs hover:scale-105 transition-transform"
                        style={{ backgroundColor: color }}
                      />
                    ))}
                  </div>
                </div>
              </div>
            </div>
          )}

          {/* TAB 3: ZEN WASH PRESETS & MONET THEME PALETTE */}
          {activeTab === 'washes' && (
            <div className="space-y-5 animate-in fade-in duration-200">
              <p className="text-xs text-[#726F68] dark:text-[#939188] leading-relaxed">
                Apply calm, contemplative duotone tint washes inspired by traditional Japanese natural materials and pigments.
              </p>

              <div className="grid grid-cols-2 sm:grid-cols-3 gap-3">
                {[
                  { id: 'none', label: 'Natural Raw', desc: 'Original authentic spectrum', color: 'bg-transparent border border-dashed border-[#8A8780]' },
                  { id: 'sumi', label: 'Sumi Ink', desc: 'Monochrome carbon wash', color: 'bg-[#282A29]' },
                  { id: 'sage', label: 'Moss Sage', desc: 'Calming lichen green', color: 'bg-[#8A9A86]' },
                  { id: 'sand', label: 'Kyoto Sand', desc: 'Warm gravel garden', color: 'bg-[#DCD0C0]' },
                  { id: 'mist', label: 'Morning Mist', desc: 'Silver vapor glaze', color: 'bg-[#E5ECE9]' },
                  { id: 'twilight', label: 'Twilight Indigo', desc: 'Deep dusk sky', color: 'bg-[#1C2A3C]' },
                ].map((wash) => {
                  const isSelected = settings.washPreset === wash.id;
                  return (
                    <button
                      key={wash.id}
                      onClick={() =>
                        setSettings((prev) => ({
                          ...prev,
                          washPreset: wash.id as ZenWashPreset,
                        }))
                      }
                      className={`p-3.5 rounded-xl text-left border transition-all flex flex-col justify-between h-[100px] ${
                        isSelected
                          ? 'border-[#2C2B29] dark:border-white ring-1 ring-[#2C2B29] dark:ring-white bg-white dark:bg-[#202221]'
                          : 'border-[#E2DDD5] dark:border-[#333534] bg-[#EFECE6]/40 dark:bg-[#1A1C1B]/40 hover:bg-white'
                      }`}
                    >
                      <div className="flex items-center justify-between">
                        <div className={`w-4 h-4 rounded-full ${wash.color}`} />
                        {isSelected && <Check className="w-3.5 h-3.5 text-[#556952] dark:text-[#BBCBB8]" />}
                      </div>
                      <div>
                        <p className="font-medium text-xs text-[#2C2B29] dark:text-[#F3F4F1]">
                          {wash.label}
                        </p>
                        <p className="text-[10px] text-[#726F68] dark:text-[#939188] line-clamp-1">
                          {wash.desc}
                        </p>
                      </div>
                    </button>
                  );
                })}
              </div>

              {settings.washPreset !== 'none' && (
                <div className="space-y-1.5 pt-2">
                  <div className="flex justify-between text-xs">
                    <span className="font-medium text-[#2C2B29] dark:text-[#F3F4F1]">Wash Intensity</span>
                    <span className="font-mono text-[#726F68] dark:text-[#939188]">
                      {settings.washIntensity}%
                    </span>
                  </div>
                  <input
                    type="range"
                    min="10"
                    max="80"
                    step="2"
                    value={settings.washIntensity}
                    onChange={(e) =>
                      setSettings((prev) => ({
                        ...prev,
                        washIntensity: parseInt(e.target.value),
                      }))
                    }
                    className="w-full accent-[#8A9A86] cursor-pointer"
                  />
                </div>
              )}
            </div>
          )}

          {/* TAB 4: MINIMALIST TYPOGRAPHY OVERLAYS */}
          {activeTab === 'text' && (
            <div className="space-y-5 animate-in fade-in duration-200 text-xs">
              <div className="flex items-center justify-between p-3 rounded-xl bg-[#EFECE6]/60 dark:bg-[#202221] border border-[#E2DDD5] dark:border-[#333534]">
                <div>
                  <p className="font-medium text-[#2C2B29] dark:text-[#F3F4F1]">
                    Display Zen Typography
                  </p>
                  <p className="text-[11px] text-[#726F68] dark:text-[#939188]">
                    Haiku, mindfulness phrases, or personal dates
                  </p>
                </div>
                <input
                  type="checkbox"
                  checked={settings.textOverlay.enabled}
                  onChange={(e) =>
                    setSettings((prev) => ({
                      ...prev,
                      textOverlay: { ...prev.textOverlay, enabled: e.target.checked },
                    }))
                  }
                  className="w-4 h-4 accent-[#8A9A86] cursor-pointer rounded"
                />
              </div>

              {settings.textOverlay.enabled && (
                <div className="space-y-4">
                  {/* Presets */}
                  <div>
                    <label className="font-semibold text-[#2C2B29] dark:text-[#F3F4F1] block mb-1.5">
                      Contemplative Presets
                    </label>
                    <div className="grid grid-cols-2 gap-2 max-h-36 overflow-y-auto pr-1">
                      {ZEN_PHRASES.map((phrase) => (
                        <button
                          key={phrase.id}
                          onClick={() =>
                            setSettings((prev) => ({
                              ...prev,
                              textOverlay: {
                                ...prev.textOverlay,
                                text: phrase.text,
                                subtext: phrase.subtext || '',
                                kanji: phrase.kanji || '',
                              },
                            }))
                          }
                          className="p-2.5 rounded-lg border border-[#E2DDD5] dark:border-[#333534] bg-white dark:bg-[#1A1C1B] hover:border-[#8A9A86] text-left transition-colors"
                        >
                          <span className="font-serif text-[11px] font-medium text-[#2C2B29] dark:text-[#F3F4F1] truncate block">
                            {phrase.kanji ? `${phrase.kanji} · ` : ''}
                            {phrase.text.split('\n')[0]}
                          </span>
                        </button>
                      ))}
                    </div>
                  </div>

                  {/* Custom Text Area */}
                  <div>
                    <label className="font-semibold text-[#2C2B29] dark:text-[#F3F4F1] block mb-1">
                      Custom Text
                    </label>
                    <textarea
                      rows={3}
                      value={settings.textOverlay.text}
                      onChange={(e) =>
                        setSettings((prev) => ({
                          ...prev,
                          textOverlay: { ...prev.textOverlay, text: e.target.value },
                        }))
                      }
                      placeholder="Enter a calming thought, poem, or reminder..."
                      className="w-full p-2.5 rounded-lg bg-white dark:bg-[#1A1C1B] border border-[#E2DDD5] dark:border-[#333534] text-xs text-[#2C2B29] dark:text-[#F3F4F1] focus:outline-none focus:ring-1 focus:ring-[#8A9A86]"
                    />
                  </div>

                  {/* Quick Color Match from Monet Palette */}
                  {palette && (
                    <div>
                      <label className="font-semibold text-[#2C2B29] dark:text-[#F3F4F1] block mb-1">
                        Typography Color (Matched to Wallpaper Palette)
                      </label>
                      <div className="flex items-center gap-2">
                        {['#FFFFFF', '#121413', palette.dominant, palette.secondary, palette.light, palette.dark].map((col, idx) => (
                          <button
                            key={idx}
                            onClick={() =>
                              setSettings((prev) => ({
                                ...prev,
                                textOverlay: { ...prev.textOverlay, color: col },
                              }))
                            }
                            className={`w-7 h-7 rounded-full border shadow-xs transition-transform ${
                              settings.textOverlay.color === col
                                ? 'scale-115 ring-2 ring-[#8A9A86]'
                                : 'hover:scale-105'
                            }`}
                            style={{ backgroundColor: col }}
                          />
                        ))}
                      </div>
                    </div>
                  )}

                  {/* Subtext and Kanji */}
                  <div className="grid grid-cols-2 gap-3">
                    <div>
                      <label className="font-semibold text-[#2C2B29] dark:text-[#F3F4F1] block mb-1">
                        Author / Subtitle
                      </label>
                      <input
                        type="text"
                        value={settings.textOverlay.subtext || ''}
                        onChange={(e) =>
                          setSettings((prev) => ({
                            ...prev,
                            textOverlay: { ...prev.textOverlay, subtext: e.target.value },
                          }))
                        }
                        placeholder="e.g. Matsuo Basho"
                        className="w-full p-2 rounded-lg bg-white dark:bg-[#1A1C1B] border border-[#E2DDD5] dark:border-[#333534] text-xs text-[#2C2B29] dark:text-[#F3F4F1]"
                      />
                    </div>
                    <div>
                      <label className="font-semibold text-[#2C2B29] dark:text-[#F3F4F1] block mb-1">
                        Japanese Kanji Motif
                      </label>
                      <input
                        type="text"
                        value={settings.textOverlay.kanji || ''}
                        onChange={(e) =>
                          setSettings((prev) => ({
                            ...prev,
                            textOverlay: { ...prev.textOverlay, kanji: e.target.value },
                          }))
                        }
                        placeholder="e.g. 静寂"
                        className="w-full p-2 rounded-lg bg-white dark:bg-[#1A1C1B] border border-[#E2DDD5] dark:border-[#333534] text-xs text-[#2C2B29] dark:text-[#F3F4F1]"
                      />
                    </div>
                  </div>

                  {/* Position & Size */}
                  <div className="grid grid-cols-2 gap-3">
                    <div>
                      <label className="font-semibold text-[#2C2B29] dark:text-[#F3F4F1] block mb-1">
                        Vertical Position
                      </label>
                      <div className="flex gap-1">
                        {['top', 'center', 'bottom'].map((pos) => (
                          <button
                            key={pos}
                            onClick={() =>
                              setSettings((prev) => ({
                                ...prev,
                                textOverlay: { ...prev.textOverlay, position: pos as any },
                              }))
                            }
                            className={`flex-1 py-1.5 rounded border text-[11px] font-medium capitalize transition-colors ${
                              settings.textOverlay.position === pos
                                ? 'bg-[#2C2B29] text-white dark:bg-white dark:text-black border-transparent'
                                : 'border-[#E2DDD5] dark:border-[#333534] text-[#726F68]'
                            }`}
                          >
                            {pos}
                          </button>
                        ))}
                      </div>
                    </div>

                    <div>
                      <label className="font-semibold text-[#2C2B29] dark:text-[#F3F4F1] block mb-1">
                        Font Style
                      </label>
                      <div className="flex gap-1">
                        {[
                          { id: 'serif', label: 'Playfair Serif' },
                          { id: 'sans', label: 'Jakarta Sans' },
                        ].map((font) => (
                          <button
                            key={font.id}
                            onClick={() =>
                              setSettings((prev) => ({
                                ...prev,
                                textOverlay: { ...prev.textOverlay, fontStyle: font.id as any },
                              }))
                            }
                            className={`flex-1 py-1.5 rounded border text-[11px] font-medium transition-colors ${
                              settings.textOverlay.fontStyle === font.id
                                ? 'bg-[#2C2B29] text-white dark:bg-white dark:text-black border-transparent'
                                : 'border-[#E2DDD5] dark:border-[#333534] text-[#726F68]'
                            }`}
                          >
                            {font.label}
                          </button>
                        ))}
                      </div>
                    </div>
                  </div>
                </div>
              )}
            </div>
          )}
        </div>
      </div>
    </div>
  );
};
