import React from 'react';
import { X, Download, Share2, Check, FileText, Sparkles, ShieldCheck, AlertCircle, Layers, Smartphone } from 'lucide-react';
import confetti from 'canvas-confetti';
import { WallpaperItem, EditorSettings, AspectRatioType } from '../types';
import { renderWallpaperToCanvas, getResolutionForRatio, downloadFile, downloadAttribution } from '../services/exportEngine';

interface ExportModalProps {
  wallpaper: WallpaperItem;
  settings: EditorSettings;
  onClose: () => void;
}

export const ExportModal: React.FC<ExportModalProps> = ({ wallpaper, settings, onClose }) => {
  const [format, setFormat] = React.useState<'image/jpeg' | 'image/png'>('image/jpeg');
  const [resolutionPreset, setResolutionPreset] = React.useState<'native' | 'high' | 'original'>('high');
  const [exportMode, setExportMode] = React.useState<'single' | 'duo'>('single');
  const [isExporting, setIsExporting] = React.useState(false);
  const [exportedSuccess, setExportedSuccess] = React.useState(false);
  const [previewBlobUrl, setPreviewBlobUrl] = React.useState<string | null>(null);

  // Compute dimensions
  const dims = React.useMemo(() => {
    if (resolutionPreset === 'original' && wallpaper.width && wallpaper.height) {
      return { width: wallpaper.width, height: wallpaper.height };
    }
    return getResolutionForRatio(settings.cropRatio, resolutionPreset === 'high');
  }, [settings.cropRatio, resolutionPreset, wallpaper]);

  // Generate preview when modal opens
  React.useEffect(() => {
    let active = true;
    const generatePreview = async () => {
      const canvas = document.createElement('canvas');
      try {
        await renderWallpaperToCanvas(wallpaper, settings, canvas, {
          customWidth: 540,
          customHeight: Math.round(540 / (dims.width / dims.height)),
        });
        if (active) {
          canvas.toBlob((blob) => {
            if (blob && active) {
              const url = URL.createObjectURL(blob);
              setPreviewBlobUrl(url);
            }
          }, 'image/jpeg', 0.85);
        }
      } catch (err) {
        console.warn('Preview render error:', err);
      }
    };

    generatePreview();
    return () => {
      active = false;
      if (previewBlobUrl) URL.revokeObjectURL(previewBlobUrl);
    };
  }, [wallpaper, settings, dims]);

  // Handle actual full-res export (supports single or coordinating duo)
  const handleExport = async (action: 'download' | 'share') => {
    setIsExporting(true);
    try {
      const ext = format === 'image/jpeg' ? 'jpg' : 'png';
      const timestamp = Date.now();

      // 1. Render Lock Screen Wallpaper (sharp as configured)
      const lockCanvas = document.createElement('canvas');
      await renderWallpaperToCanvas(wallpaper, settings, lockCanvas, {
        customWidth: dims.width,
        customHeight: dims.height,
        format,
        quality: 0.95,
      });

      const lockBlob = await new Promise<Blob | null>((resolve) =>
        lockCanvas.toBlob(resolve, format, 0.95)
      );

      if (!lockBlob) {
        setIsExporting(false);
        return;
      }

      if (exportMode === 'duo') {
        // 2. Render Coordinating Home Screen Wallpaper (softened depth & icon negative space)
        const homeSettings: EditorSettings = {
          ...settings,
          blur: Math.max(5, settings.blur + 5),
          vignette: Math.max(25, settings.vignette + 12),
          mistGradient: 'bottom',
          mistIntensity: Math.max(45, settings.mistIntensity || 45),
          textOverlay: { ...settings.textOverlay, enabled: false }, // clean behind icons
        };

        const homeCanvas = document.createElement('canvas');
        await renderWallpaperToCanvas(wallpaper, homeSettings, homeCanvas, {
          customWidth: dims.width,
          customHeight: dims.height,
          format,
          quality: 0.95,
        });

        const homeBlob = await new Promise<Blob | null>((resolve) =>
          homeCanvas.toBlob(resolve, format, 0.95)
        );

        const lockFilename = `zen_LOCK_SCREEN_${settings.cropRatio.replace(':', 'x')}_${timestamp}.${ext}`;
        const homeFilename = `zen_HOME_SCREEN_SOFT_${settings.cropRatio.replace(':', 'x')}_${timestamp}.${ext}`;

        downloadFile(lockBlob, lockFilename);
        if (homeBlob) {
          setTimeout(() => downloadFile(homeBlob, homeFilename), 400);
        }
      } else {
        // Single export
        const filename = `zen_nature_${settings.cropRatio.replace(':', 'x')}_${timestamp}.${ext}`;

        if (action === 'share' && navigator.canShare && navigator.canShare({ files: [new File([lockBlob], filename, { type: format })] })) {
          try {
            await navigator.share({
              title: wallpaper.title,
              text: `Zen Nature Wallpaper: "${wallpaper.title}"`,
              files: [new File([lockBlob], filename, { type: format })],
            });
          } catch {
            downloadFile(lockBlob, filename);
          }
        } else {
          downloadFile(lockBlob, filename);
        }
      }

      setIsExporting(false);
      setExportedSuccess(true);

      // Confetti celebration
      confetti({
        particleCount: 50,
        spread: 60,
        origin: { y: 0.7 },
        colors: ['#8A9A86', '#BBCBB8', '#EFECE6', '#2C2B29'],
      });
    } catch (err) {
      console.error('Export error:', err);
      setIsExporting(false);
    }
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-3 sm:p-6 bg-black/65 backdrop-blur-md animate-in fade-in duration-200">
      <div
        className="relative w-full max-w-2xl bg-[#F8F6F0] dark:bg-[#1A1C1B] rounded-2xl md:rounded-3xl border border-[#E8E4DC] dark:border-[#333534] shadow-2xl overflow-hidden p-6 sm:p-8 space-y-6"
        onClick={(e) => e.stopPropagation()}
      >
        {/* Header */}
        <div className="flex items-center justify-between pb-3 border-b border-[#E8E4DC] dark:border-[#2D302F]">
          <div>
            <h2 className="font-serif text-2xl font-semibold text-[#2C2B29] dark:text-[#F3F4F1]">
              Export Zen Wallpaper
            </h2>
            <p className="text-xs text-[#726F68] dark:text-[#939188] mt-0.5">
              High-resolution export with verified license attribution
            </p>
          </div>
          <button
            onClick={onClose}
            aria-label="Close export modal"
            className="p-2 rounded-full hover:bg-[#EFECE6] dark:hover:bg-[#262827] text-[#726F68]"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Preview & Settings Grid */}
        <div className="grid grid-cols-1 sm:grid-cols-12 gap-6 items-center">
          {/* Small Simulated Preview */}
          <div className="sm:col-span-5 flex justify-center">
            <div className="relative w-[150px] aspect-[9/19] rounded-[24px] overflow-hidden bg-black shadow-lg ring-1 ring-black/10">
              {previewBlobUrl ? (
                <img
                  src={previewBlobUrl}
                  alt="Export preview"
                  className="w-full h-full object-cover"
                />
              ) : (
                <div className="w-full h-full bg-[#202221] animate-pulse flex items-center justify-center">
                  <span className="text-[10px] text-[#726F68]">Rendering...</span>
                </div>
              )}
              {/* Overlay simulation */}
              <div className="absolute inset-0 bg-gradient-to-b from-black/30 via-transparent to-black/50 p-2.5 flex flex-col justify-between text-white pointer-events-none">
                <div className="pt-4 text-center">
                  <p className="font-light text-2xl leading-none font-sans drop-shadow">12:00</p>
                </div>
              </div>
            </div>
          </div>

          {/* Configuration Options */}
          <div className="sm:col-span-7 space-y-4 text-xs">
            {/* Export Mode: Single vs Coordinating Duo */}
            <div>
              <label className="font-semibold text-[#2C2B29] dark:text-[#F3F4F1] block mb-1.5">
                Wallpaper Bundle
              </label>
              <div className="grid grid-cols-2 gap-2">
                <button
                  onClick={() => setExportMode('single')}
                  className={`py-2 px-2.5 rounded-lg font-medium border text-left transition-colors flex items-center gap-2 ${
                    exportMode === 'single'
                      ? 'bg-[#2C2B29] text-white dark:bg-white dark:text-black border-transparent'
                      : 'border-[#E2DDD5] dark:border-[#333534] text-[#726F68] hover:bg-[#EFECE6]'
                  }`}
                >
                  <Smartphone className="w-4 h-4 shrink-0" />
                  <div>
                    <p className="font-semibold text-[11px]">Single Wallpaper</p>
                    <p className="text-[10px] opacity-80">Current custom framing</p>
                  </div>
                </button>

                <button
                  onClick={() => setExportMode('duo')}
                  className={`py-2 px-2.5 rounded-lg font-medium border text-left transition-colors flex items-center gap-2 ${
                    exportMode === 'duo'
                      ? 'bg-[#2C2B29] text-white dark:bg-white dark:text-black border-transparent'
                      : 'border-[#E2DDD5] dark:border-[#333534] text-[#726F68] hover:bg-[#EFECE6]'
                  }`}
                >
                  <Layers className="w-4 h-4 shrink-0" />
                  <div>
                    <p className="font-semibold text-[11px]">Coordinating Duo</p>
                    <p className="text-[10px] opacity-80">Lock + Softened Home</p>
                  </div>
                </button>
              </div>
            </div>

            {/* Resolution Preset */}
            <div>
              <label className="font-semibold text-[#2C2B29] dark:text-[#F3F4F1] block mb-1.5">
                Output Resolution ({dims.width} × {dims.height} px)
              </label>
              <div className="grid grid-cols-3 gap-2">
                {[
                  { id: 'high', label: 'QHD High' },
                  { id: 'native', label: 'Standard FHD' },
                  { id: 'original', label: 'Source Res' },
                ].map((opt) => (
                  <button
                    key={opt.id}
                    onClick={() => setResolutionPreset(opt.id as any)}
                    className={`py-2 px-2 rounded-lg font-medium border text-center transition-colors ${
                      resolutionPreset === opt.id
                        ? 'bg-[#2C2B29] text-white dark:bg-white dark:text-black border-transparent'
                        : 'border-[#E2DDD5] dark:border-[#333534] text-[#726F68] hover:bg-[#EFECE6]'
                    }`}
                  >
                    {opt.label}
                  </button>
                ))}
              </div>
            </div>

            {/* Format Selection */}
            <div>
              <label className="font-semibold text-[#2C2B29] dark:text-[#F3F4F1] block mb-1.5">
                File Format
              </label>
              <div className="grid grid-cols-2 gap-2">
                <button
                  onClick={() => setFormat('image/jpeg')}
                  className={`py-2 px-3 rounded-lg font-medium border text-center transition-colors ${
                    format === 'image/jpeg'
                      ? 'bg-[#2C2B29] text-white dark:bg-white dark:text-black border-transparent'
                      : 'border-[#E2DDD5] dark:border-[#333534] text-[#726F68] hover:bg-[#EFECE6]'
                  }`}
                >
                  High-Fidelity JPEG (95%)
                </button>
                <button
                  onClick={() => setFormat('image/png')}
                  className={`py-2 px-3 rounded-lg font-medium border text-center transition-colors ${
                    format === 'image/png'
                      ? 'bg-[#2C2B29] text-white dark:bg-white dark:text-black border-transparent'
                      : 'border-[#E2DDD5] dark:border-[#333534] text-[#726F68] hover:bg-[#EFECE6]'
                  }`}
                >
                  Lossless PNG
                </button>
              </div>
            </div>

            {/* Verified License Summary */}
            <div className="p-3 rounded-xl bg-[#EFECE6]/70 dark:bg-[#202221] border border-[#E2DDD5] dark:border-[#333534] space-y-1 text-[11px]">
              <div className="flex items-center gap-1.5 text-[#556952] dark:text-[#BBCBB8] font-medium">
                <ShieldCheck className="w-3.5 h-3.5" />
                <span>License: {wallpaper.license.name} ({wallpaper.license.code})</span>
              </div>
              <p className="text-[#726F68] dark:text-[#939188]">
                {wallpaper.license.attributionRequired
                  ? 'Attribution is required for distribution. You can download the attribution text file below.'
                  : 'Public Domain / CC0 Dedication — Free for personal and commercial wallpaper use.'}
              </p>
            </div>
          </div>
        </div>

        {/* Actions & Export Buttons */}
        <div className="pt-4 border-t border-[#E8E4DC] dark:border-[#2D302F] flex flex-col sm:flex-row items-center justify-between gap-3">
          <button
            onClick={() => downloadAttribution(wallpaper)}
            className="text-xs text-[#726F68] dark:text-[#939188] hover:text-[#2C2B29] dark:hover:text-[#F3F4F1] flex items-center gap-1.5 underline"
          >
            <FileText className="w-3.5 h-3.5" />
            <span>Download Attribution File (TXT)</span>
          </button>

          <div className="flex items-center gap-2 w-full sm:w-auto">
            {exportMode === 'single' && typeof navigator !== 'undefined' && 'share' in navigator && (
              <button
                onClick={() => handleExport('share')}
                disabled={isExporting}
                className="flex-1 sm:flex-none min-h-[46px] px-4 rounded-xl border border-[#DCD7CE] dark:border-[#383A38] hover:bg-white dark:hover:bg-[#262827] text-xs font-medium text-[#2C2B29] dark:text-[#E2E3E0] flex items-center justify-center gap-2 transition-colors disabled:opacity-50"
              >
                <Share2 className="w-4 h-4" />
                <span>Share</span>
              </button>
            )}

            <button
              onClick={() => handleExport('download')}
              disabled={isExporting}
              className="flex-1 sm:flex-none min-h-[46px] px-6 rounded-xl bg-[#2C2B29] hover:bg-[#1A1918] text-[#F8F6F0] dark:bg-[#E2E3E0] dark:hover:bg-[#FFFFFF] dark:text-[#121413] text-sm font-medium tracking-wide flex items-center justify-center gap-2 shadow-sm transition-all hover:scale-[1.02] active:scale-[0.98] disabled:opacity-50"
            >
              {isExporting ? (
                <>
                  <div className="w-4 h-4 border-2 border-white/30 border-t-white rounded-full animate-spin" />
                  <span>Processing {exportMode === 'duo' ? 'Duo Set...' : 'Wallpaper...'}</span>
                </>
              ) : exportedSuccess ? (
                <>
                  <Check className="w-4 h-4 text-emerald-500" />
                  <span>{exportMode === 'duo' ? 'Duo Saved to Device!' : 'Saved to Device!'}</span>
                </>
              ) : (
                <>
                  <Download className="w-4 h-4 text-[#8A9A86] dark:text-[#556952]" />
                  <span>{exportMode === 'duo' ? 'Download Duo (Lock + Home)' : 'Download Wallpaper'}</span>
                </>
              )}
            </button>
          </div>
        </div>
      </div>
    </div>
  );
};
