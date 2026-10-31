import React from 'react';
import {
  X,
  Sparkles,
  Heart,
  ExternalLink,
  ShieldCheck,
  Download,
  FileText,
  Check,
  Smartphone,
  Share2,
  Lock,
  LayoutGrid,
  Image as ImageIcon,
  ChevronDown,
  ChevronUp,
  Info,
  Flashlight,
  Camera,
} from 'lucide-react';
import { WallpaperItem } from '../types';
import { downloadAttribution, downloadWallpaperDirect, shareWallpaperDirect } from '../services/exportEngine';
import { HomeScreenSimulation } from './HomeScreenSimulation';

interface DetailModalProps {
  wallpaper: WallpaperItem | null;
  onClose: () => void;
  onEditInStudio: (wallpaper: WallpaperItem) => void;
  isFavorited: boolean;
  onToggleFavorite: (wallpaper: WallpaperItem) => void;
}

export const DetailModal: React.FC<DetailModalProps> = ({
  wallpaper,
  onClose,
  onEditInStudio,
  isFavorited,
  onToggleFavorite,
}) => {
  const [attributionCopied, setAttributionCopied] = React.useState(false);
  const [previewMode, setPreviewMode] = React.useState<'clean' | 'lockscreen' | 'homescreen'>('clean');
  const [downloadTarget, setDownloadTarget] = React.useState<'phone' | 'original'>('phone');
  const [downloading, setDownloading] = React.useState(false);
  const [successBanner, setSuccessBanner] = React.useState<string | null>(null);
  const [showGuide, setShowGuide] = React.useState(false);

  const [guidePlatform, setGuidePlatform] = React.useState<'android' | 'iphone' | 'desktop'>('android');

  // Detect client platform
  React.useEffect(() => {
    const ua = navigator.userAgent || '';
    if (/iPad|iPhone|iPod/.test(ua)) {
      setGuidePlatform('iphone');
    } else if (/Android/.test(ua)) {
      setGuidePlatform('android');
    } else {
      setGuidePlatform('desktop');
    }
  }, []);

  // Live time for lockscreen simulation
  const [currentTime, setCurrentTime] = React.useState('12:00');
  const [currentDate, setCurrentDate] = React.useState('Monday, Oct 21');

  React.useEffect(() => {
    const updateTime = () => {
      const now = new Date();
      setCurrentTime(
        now.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit', hour12: false })
      );
      setCurrentDate(
        now.toLocaleDateString([], { weekday: 'long', month: 'short', day: 'numeric' })
      );
    };
    updateTime();
    const timer = setInterval(updateTime, 1000);
    return () => clearInterval(timer);
  }, []);

  if (!wallpaper) return null;

  const handleCopyAttribution = () => {
    const text = `Image: "${wallpaper.title}" by ${wallpaper.license.author || 'Unknown'} via ${wallpaper.provider.toUpperCase()} (${wallpaper.license.name}). ${wallpaper.sourcePageUrl}`;
    navigator.clipboard.writeText(text);
    setAttributionCopied(true);
    setTimeout(() => setAttributionCopied(false), 2500);
  };

  const handleApplyToDevice = async () => {
    setDownloading(true);
    try {
      // 1. Try mobile native share sheet first (triggers native "Set as wallpaper" / "Use as Wallpaper" on Android & iOS)
      const shared = await shareWallpaperDirect(wallpaper);
      if (shared) {
        setSuccessBanner('Device sheet opened! Select "Use as Wallpaper" or "Set as wallpaper" from the system sheet.');
        setShowGuide(true);
        return;
      }

      // 2. If share isn't supported or cancelled, download optimized 9:16 phone fit
      const result = await downloadWallpaperDirect(wallpaper, 'phone');
      if (result.success) {
        setSuccessBanner('Wallpaper saved to Photos! Follow the 2-step guide below to apply.');
        setShowGuide(true);
      }
    } catch (err) {
      console.error('Failed to apply wallpaper:', err);
      await downloadWallpaperDirect(wallpaper, 'phone');
      setSuccessBanner('Wallpaper saved! Open Photos to set as wallpaper.');
      setShowGuide(true);
    } finally {
      setDownloading(false);
    }
  };

  const handleDownloadDirect = async (mode: 'phone' | 'original') => {
    setDownloading(true);
    try {
      const result = await downloadWallpaperDirect(wallpaper, mode);
      if (result.success) {
        setSuccessBanner(`Downloaded ${mode === 'phone' ? 'Phone 9:16' : '4K Original'} file to device.`);
      }
    } catch (err) {
      console.error('Failed to download:', err);
    } finally {
      setDownloading(false);
    }
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-3 sm:p-6 bg-black/70 backdrop-blur-md animate-in fade-in duration-200">
      <div
        className="relative w-full max-w-4xl max-h-[94vh] flex flex-col md:flex-row bg-[#F8F6F0] dark:bg-[#1A1C1B] rounded-2xl md:rounded-3xl border border-[#E8E4DC] dark:border-[#333534] shadow-2xl overflow-hidden"
        onClick={(e) => e.stopPropagation()}
      >
        {/* Close Button */}
        <button
          onClick={onClose}
          aria-label="Close details"
          className="absolute top-3 right-3 z-30 min-h-[44px] min-w-[44px] flex items-center justify-center rounded-full bg-black/50 hover:bg-black/75 text-white backdrop-blur-md transition-colors"
        >
          <X className="w-5 h-5" />
        </button>

        {/* Media Preview Column with Interactive Lock/Home Simulator */}
        <div className="md:w-1/2 relative bg-[#0D0F0E] flex flex-col items-center justify-center min-h-[300px] md:min-h-[520px] overflow-hidden group select-none">
          {/* Wallpaper Image */}
          <img
            src={wallpaper.fullUrl || wallpaper.thumbnailUrl}
            alt={wallpaper.title}
            className="w-full h-full object-cover max-h-[44vh] md:max-h-full transition-transform duration-700 group-hover:scale-102"
            loading="eager"
          />

          {/* Top Preview Controls Pill */}
          <div className="absolute top-3 left-3 z-20 flex items-center gap-1 p-1 rounded-full bg-black/60 backdrop-blur-md border border-white/10 text-white text-[11px]">
            <button
              onClick={() => setPreviewMode('clean')}
              className={`px-2.5 py-1 rounded-full flex items-center gap-1 font-medium transition-colors ${
                previewMode === 'clean' ? 'bg-white text-black shadow-xs' : 'text-white/80 hover:text-white'
              }`}
            >
              <ImageIcon className="w-3 h-3" />
              <span>Clean</span>
            </button>
            <button
              onClick={() => setPreviewMode('lockscreen')}
              className={`px-2.5 py-1 rounded-full flex items-center gap-1 font-medium transition-colors ${
                previewMode === 'lockscreen' ? 'bg-white text-black shadow-xs' : 'text-white/80 hover:text-white'
              }`}
            >
              <Lock className="w-3 h-3" />
              <span>Lock Screen</span>
            </button>
            <button
              onClick={() => setPreviewMode('homescreen')}
              className={`px-2.5 py-1 rounded-full flex items-center gap-1 font-medium transition-colors ${
                previewMode === 'homescreen' ? 'bg-white text-black shadow-xs' : 'text-white/80 hover:text-white'
              }`}
            >
              <LayoutGrid className="w-3 h-3" />
              <span>Home Screen</span>
            </button>
          </div>

          {/* Simulated Lock Screen Overlay */}
          {previewMode === 'lockscreen' && (
            <div className="absolute inset-0 bg-gradient-to-b from-black/45 via-transparent to-black/65 p-6 flex flex-col justify-between text-white pointer-events-none animate-in fade-in duration-200">
              <div className="pt-10 text-center space-y-1">
                <div className="flex items-center justify-center gap-1 text-[11px] font-medium tracking-wide uppercase opacity-85">
                  <Lock className="w-3 h-3" />
                  <span>Swipe up to unlock</span>
                </div>
                <h3 className="font-sans font-extralight text-6xl tracking-tight leading-none drop-shadow-md">
                  {currentTime}
                </h3>
                <p className="text-xs font-medium tracking-wide opacity-90 drop-shadow-sm">
                  {currentDate}
                </p>
              </div>

              {/* Bottom Lock Screen Torch / Camera Shortcuts */}
              <div className="flex items-center justify-between pb-3 px-2">
                <div className="w-10 h-10 rounded-full bg-black/40 backdrop-blur-md flex items-center justify-center border border-white/20">
                  <Flashlight className="w-4 h-4 text-white" />
                </div>
                <div className="w-10 h-10 rounded-full bg-black/40 backdrop-blur-md flex items-center justify-center border border-white/20">
                  <Camera className="w-4 h-4 text-white" />
                </div>
              </div>
            </div>
          )}

          {/* Simulated Home Screen Overlay */}
          {previewMode === 'homescreen' && (
            <div className="absolute inset-0 animate-in fade-in duration-200">
              <HomeScreenSimulation />
            </div>
          )}

          {/* Dimension Tag */}
          <div className="absolute bottom-3 left-3 z-10 text-white/90 text-[11px] font-mono px-2 py-0.5 rounded-md bg-black/50 backdrop-blur-xs">
            {wallpaper.width} × {wallpaper.height} px
          </div>
        </div>

        {/* Metadata & Actions Column */}
        <div className="md:w-1/2 p-5 sm:p-7 flex flex-col justify-between overflow-y-auto max-h-[50vh] md:max-h-full">
          <div className="space-y-4">
            {/* Source & Verified Status */}
            <div className="flex items-center justify-between gap-2 text-xs">
              <span className="text-[#726F68] dark:text-[#939188] uppercase tracking-widest font-semibold flex items-center gap-1.5">
                <span className="w-2 h-2 rounded-full bg-[#8A9A86]" />
                {wallpaper.provider === 'wikimedia'
                  ? 'Wikimedia Commons'
                  : wallpaper.provider === 'nasa'
                  ? 'NASA Earth & Space'
                  : wallpaper.provider === 'openverse'
                  ? 'Openverse Public Archive'
                  : 'Zenithan Studio Curated'}
              </span>
              <span className="flex items-center gap-1 text-[#556952] dark:text-[#BBCBB8] font-medium">
                <ShieldCheck className="w-4 h-4" />
                Verified Open License
              </span>
            </div>

            {/* Title & Description */}
            <div>
              <h2 className="font-serif text-2xl font-semibold text-[#2C2B29] dark:text-[#F3F4F1] leading-tight">
                {wallpaper.title}
              </h2>
              {wallpaper.description && (
                <p className="mt-1 text-xs sm:text-sm text-[#726F68] dark:text-[#939188] leading-relaxed line-clamp-2">
                  {wallpaper.description}
                </p>
              )}
            </div>

            {/* Success Toast / Instruction Banner */}
            {successBanner && (
              <div className="p-3.5 rounded-xl bg-emerald-500/10 border border-emerald-500/30 text-emerald-800 dark:text-emerald-300 text-xs space-y-1.5 animate-in fade-in duration-200">
                <div className="flex items-start gap-2">
                  <Check className="w-4 h-4 mt-0.5 shrink-0 text-emerald-600 dark:text-emerald-400" />
                  <p className="font-medium leading-relaxed">{successBanner}</p>
                </div>
              </div>
            )}

            {/* 🌟 Dedicated "Apply to Device" Action Box 🌟 */}
            <div className="p-4 rounded-2xl bg-white dark:bg-[#202221] border border-[#8A9A86]/30 dark:border-[#8A9A86]/20 shadow-sm space-y-3.5">
              <div className="flex items-center justify-between">
                <div className="flex items-center gap-2">
                  <Smartphone className="w-4 h-4 text-[#556952] dark:text-[#BBCBB8]" />
                  <span className="text-xs font-semibold uppercase tracking-wider text-[#2C2B29] dark:text-[#F3F4F1]">
                    Device Setup
                  </span>
                </div>

                <span className="text-[11px] font-mono font-medium px-2 py-0.5 rounded bg-[#EFECE6] dark:bg-[#151716] text-[#726F68] dark:text-[#939188]">
                  Phone Fit (9:16)
                </span>
              </div>

              {/* Action Buttons: Apply to Device + Download 4K */}
              <div className="flex flex-col sm:flex-row items-stretch gap-2">
                <button
                  onClick={handleApplyToDevice}
                  disabled={downloading}
                  className="flex-1 min-h-[46px] px-4 rounded-xl bg-[#556952] hover:bg-[#435340] text-white font-medium text-sm flex items-center justify-center gap-2 shadow-sm transition-all hover:scale-[1.01] active:scale-[0.99] disabled:opacity-75 cursor-pointer"
                >
                  <Smartphone className={`w-4 h-4 ${downloading ? 'animate-pulse' : ''}`} />
                  <span>
                    {downloading ? 'Preparing Wallpaper...' : 'Apply to Device'}
                  </span>
                </button>

                <button
                  onClick={() => handleDownloadDirect('original')}
                  disabled={downloading}
                  title="Download original 4K file"
                  className="min-h-[46px] px-4 rounded-xl border border-[#DCD7CE] dark:border-[#383A38] bg-[#F8F6F0] dark:bg-[#262827] hover:bg-white dark:hover:bg-[#303331] text-[#2C2B29] dark:text-[#E2E3E0] text-xs font-medium flex items-center justify-center gap-2 transition-colors cursor-pointer shrink-0"
                >
                  <Download className="w-3.5 h-3.5 text-[#726F68] dark:text-[#939188]" />
                  <span>Download 4K</span>
                </button>
              </div>

              {/* Step-by-Step Device Guide Accordion with Platform Tabs */}
              <div className="pt-2 border-t border-[#E8E4DC] dark:border-[#2D302F]">
                <div className="flex items-center justify-between">
                  <button
                    onClick={() => setShowGuide(!showGuide)}
                    className="flex items-center gap-1.5 text-[11px] font-medium text-[#726F68] dark:text-[#939188] hover:text-[#2C2B29] dark:hover:text-white"
                  >
                    <Info className="w-3.5 h-3.5 text-[#556952] dark:text-[#BBCBB8]" />
                    <span>How to set on your device</span>
                    {showGuide ? <ChevronUp className="w-3.5 h-3.5" /> : <ChevronDown className="w-3.5 h-3.5" />}
                  </button>

                  {showGuide && (
                    <div className="flex items-center gap-1 text-[10px]">
                      {(['android', 'iphone', 'desktop'] as const).map((p) => (
                        <button
                          key={p}
                          onClick={() => setGuidePlatform(p)}
                          className={`px-2 py-0.5 rounded-md capitalize font-medium transition-colors ${
                            guidePlatform === p
                              ? 'bg-[#2C2B29] text-white dark:bg-white dark:text-black shadow-2xs'
                              : 'text-[#726F68] dark:text-[#939188] hover:bg-[#EFECE6] dark:hover:bg-[#202221]'
                          }`}
                        >
                          {p === 'iphone' ? 'iPhone' : p}
                        </button>
                      ))}
                    </div>
                  )}
                </div>

                {showGuide && (
                  <div className="mt-2.5 p-3 rounded-xl bg-[#F8F6F0] dark:bg-[#151716] text-[11px] text-[#726F68] dark:text-[#939188] space-y-2 border border-[#E8E4DC] dark:border-[#2D302F] animate-in fade-in duration-200">
                    {guidePlatform === 'android' && (
                      <div className="space-y-1.5 leading-relaxed">
                        <div className="flex items-start gap-2">
                          <span className="w-4 h-4 rounded-full bg-[#556952] text-white flex items-center justify-center text-[10px] font-bold shrink-0 mt-0.5">1</span>
                          <span>Tap <strong>Apply to Device</strong> above to open the native share sheet or save directly.</span>
                        </div>
                        <div className="flex items-start gap-2">
                          <span className="w-4 h-4 rounded-full bg-[#556952] text-white flex items-center justify-center text-[10px] font-bold shrink-0 mt-0.5">2</span>
                          <span>In Photos or Gallery, tap the <strong>⋮ Menu</strong> &gt; select <strong className="text-[#556952] dark:text-[#BBCBB8]">"Set as wallpaper"</strong> (Home, Lock, or Both).</span>
                        </div>
                      </div>
                    )}
                    {guidePlatform === 'iphone' && (
                      <div className="space-y-1.5 leading-relaxed">
                        <div className="flex items-start gap-2">
                          <span className="w-4 h-4 rounded-full bg-[#556952] text-white flex items-center justify-center text-[10px] font-bold shrink-0 mt-0.5">1</span>
                          <span>Tap <strong>Apply to Device</strong> above to launch the system sheet or save to Photos.</span>
                        </div>
                        <div className="flex items-start gap-2">
                          <span className="w-4 h-4 rounded-full bg-[#556952] text-white flex items-center justify-center text-[10px] font-bold shrink-0 mt-0.5">2</span>
                          <span>In Photos, tap the <strong>Share icon [↑]</strong> &gt; select <strong className="text-[#556952] dark:text-[#BBCBB8]">"Use as Wallpaper"</strong> &gt; tap Add.</span>
                        </div>
                      </div>
                    )}
                    {guidePlatform === 'desktop' && (
                      <div className="space-y-1.5 leading-relaxed">
                        <div className="flex items-start gap-2">
                          <span className="w-4 h-4 rounded-full bg-[#556952] text-white flex items-center justify-center text-[10px] font-bold shrink-0 mt-0.5">1</span>
                          <span>Tap <strong>Download 4K</strong> to save the full original image.</span>
                        </div>
                        <div className="flex items-start gap-2">
                          <span className="w-4 h-4 rounded-full bg-[#556952] text-white flex items-center justify-center text-[10px] font-bold shrink-0 mt-0.5">2</span>
                          <span>Right-click the image file on your desktop and choose <strong className="text-[#556952] dark:text-[#BBCBB8]">"Set desktop background"</strong>.</span>
                        </div>
                      </div>
                    )}
                  </div>
                )}
              </div>
            </div>

            {/* Creator & License Information Card */}
            <div className="p-3.5 rounded-xl bg-[#EFECE6]/70 dark:bg-[#262827] border border-[#E2DDD5] dark:border-[#333534] space-y-2 text-xs">
              <div className="flex items-center justify-between">
                <span className="text-[#726F68] dark:text-[#939188]">Creator / Artist</span>
                <span className="font-medium text-[#2C2B29] dark:text-[#E2E3E0] truncate max-w-[180px]">
                  {wallpaper.license.author || 'Open Contributor'}
                </span>
              </div>

              <div className="flex items-center justify-between">
                <span className="text-[#726F68] dark:text-[#939188]">License Code</span>
                <span className="font-mono font-medium text-[#2C2B29] dark:text-[#E2E3E0] px-2 py-0.5 rounded bg-white/60 dark:bg-black/30 border border-[#E2DDD5] dark:border-[#3a3c3b]">
                  {wallpaper.license.code}
                </span>
              </div>

              <div className="flex items-center gap-2 pt-1 border-t border-[#E2DDD5] dark:border-[#333534]">
                <a
                  href={wallpaper.sourcePageUrl}
                  target="_blank"
                  rel="noopener noreferrer"
                  className="text-[11px] text-[#556952] dark:text-[#BBCBB8] hover:underline flex items-center gap-1"
                >
                  <span>View original archive entry</span>
                  <ExternalLink className="w-3 h-3" />
                </a>
              </div>
            </div>

            {/* Attribution Actions */}
            <div className="flex items-center gap-2">
              <button
                onClick={handleCopyAttribution}
                className="flex-1 py-2 px-3 rounded-lg border border-[#E2DDD5] dark:border-[#333534] hover:bg-[#EFECE6] dark:hover:bg-[#262827] text-xs text-[#2C2B29] dark:text-[#E2E3E0] font-medium flex items-center justify-center gap-1.5 transition-colors cursor-pointer"
              >
                {attributionCopied ? (
                  <>
                    <Check className="w-3.5 h-3.5 text-emerald-600" />
                    <span>Copied Attribution</span>
                  </>
                ) : (
                  <>
                    <FileText className="w-3.5 h-3.5" />
                    <span>Copy Attribution</span>
                  </>
                )}
              </button>

              <button
                onClick={() => downloadAttribution(wallpaper)}
                title="Download attribution text file"
                className="py-2 px-3 rounded-lg border border-[#E2DDD5] dark:border-[#333534] hover:bg-[#EFECE6] dark:hover:bg-[#262827] text-xs text-[#2C2B29] dark:text-[#E2E3E0] font-medium flex items-center justify-center gap-1.5 transition-colors cursor-pointer"
              >
                <Download className="w-3.5 h-3.5" />
                <span className="hidden sm:inline">License File</span>
              </button>
            </div>
          </div>

          {/* Secondary Action Buttons Footer */}
          <div className="pt-4 mt-4 border-t border-[#E8E4DC] dark:border-[#333534] flex items-center gap-3">
            <button
              onClick={() => onToggleFavorite(wallpaper)}
              className={`min-h-[44px] px-4 rounded-xl border transition-colors flex items-center justify-center gap-2 cursor-pointer ${
                isFavorited
                  ? 'bg-rose-50 border-rose-200 text-rose-600 dark:bg-rose-950/30 dark:border-rose-900/40 dark:text-rose-400'
                  : 'border-[#E2DDD5] dark:border-[#333534] text-[#726F68] dark:text-[#939188] hover:bg-[#EFECE6] dark:hover:bg-[#262827]'
              }`}
            >
              <Heart className={`w-4 h-4 ${isFavorited ? 'fill-current' : ''}`} />
              <span className="text-xs font-medium">
                {isFavorited ? 'Saved' : 'Favorite'}
              </span>
            </button>

            <button
              onClick={() => {
                onEditInStudio(wallpaper);
                onClose();
              }}
              className="flex-1 min-h-[44px] px-4 rounded-xl border border-[#2C2B29] dark:border-[#E2E3E0] hover:bg-[#2C2B29] hover:text-white dark:hover:bg-white dark:hover:text-black text-xs font-medium tracking-wide flex items-center justify-center gap-2 transition-colors cursor-pointer"
            >
              <Sparkles className="w-4 h-4 text-[#8A9A86] dark:text-[#556952]" />
              <span>Customize in Studio</span>
            </button>
          </div>
        </div>
      </div>
    </div>
  );
};
