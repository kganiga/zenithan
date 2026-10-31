import React from 'react';
import {
  Sparkles,
  ArrowRight,
  Compass,
  ShieldCheck,
  Heart,
  Sliders,
  Trees,
  Mountain,
  TreePine,
  Waves,
  Sun,
  CloudRain,
  Feather,
  Sunset,
  Moon,
  FileUp,
  Clock,
  Sparkle,
  Smartphone,
  Wind,
} from 'lucide-react';
import { WallpaperItem, CuratedCategory } from '../types';
import { CURATED_CATEGORIES, getCurrentCircadianSlot } from '../data/curatedData';
import { ZenBreathingTimer } from './ZenBreathingTimer';

interface HomeScreenProps {
  featuredWallpaper: WallpaperItem;
  recentWallpapers: WallpaperItem[];
  onSelectCategory: (categoryId: string) => void;
  onSelectWallpaper: (wallpaper: WallpaperItem) => void;
  onEditInStudio: (wallpaper: WallpaperItem) => void;
  onNavigateExplore: () => void;
  isFavorited: (id: string) => boolean;
  onToggleFavorite: (item: WallpaperItem) => void;
  onImportCustomPhoto?: (wallpaper: WallpaperItem) => void;
}

const CATEGORY_ICONS: Record<string, React.ElementType> = {
  Mountain: Mountain,
  Trees: Trees,
  TreePine: TreePine,
  Waves: Waves,
  Compass: Compass,
  Sun: Sun,
  CloudRain: CloudRain,
  Feather: Feather,
  Sunset: Sunset,
  Moon: Moon,
};

export const HomeScreen: React.FC<HomeScreenProps> = ({
  featuredWallpaper,
  recentWallpapers,
  onSelectCategory,
  onSelectWallpaper,
  onEditInStudio,
  onNavigateExplore,
  isFavorited,
  onToggleFavorite,
  onImportCustomPhoto,
}) => {
  const [timeString, setTimeString] = React.useState('');
  const [dateString, setDateString] = React.useState('');
  const [isZenBreathingOpen, setIsZenBreathingOpen] = React.useState(false);
  const circadianSlot = React.useMemo(() => getCurrentCircadianSlot(), []);
  const fileInputRef = React.useRef<HTMLInputElement | null>(null);

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

  return (
    <div className="space-y-12 pb-24 md:pb-16 max-w-7xl mx-auto px-4 sm:px-6 pt-6">
      {/* Hidden File Input for Personal Photo Import */}
      <input
        type="file"
        ref={fileInputRef}
        onChange={handleFileUpload}
        accept="image/*"
        className="hidden"
      />

      {/* Hero Featured Wallpaper Preview */}
      <section className="relative overflow-hidden rounded-2xl md:rounded-3xl border border-[#E8E4DC] dark:border-[#282A29] bg-[#EFECE6]/60 dark:bg-[#1A1C1B]/80 p-6 md:p-10 shadow-sm transition-all">
        <div className="grid grid-cols-1 lg:grid-cols-12 gap-8 items-center">
          {/* Left Text & Value Prop */}
          <div className="lg:col-span-7 space-y-6">
            <div className="inline-flex items-center gap-2 px-3 py-1 rounded-full bg-[#8A9A86]/15 dark:bg-[#BBCBB8]/15 border border-[#8A9A86]/25 dark:border-[#BBCBB8]/25 text-[#4A5D47] dark:text-[#BBCBB8] text-xs font-medium">
              <span className="w-1.5 h-1.5 rounded-full bg-[#8A9A86] animate-pulse" />
              <span>Today’s Contemplative Discovery</span>
            </div>

            <div className="space-y-2">
              <h1 className="font-serif text-3xl sm:text-4xl md:text-5xl font-semibold text-[#2C2B29] dark:text-[#F3F4F1] leading-[1.15] tracking-tight">
                Find stillness in every frame.
              </h1>
              <p className="text-sm sm:text-base text-[#726F68] dark:text-[#939188] max-w-xl leading-relaxed">
                Discover serene public domain and verified high-resolution photography. Customize with elegant typography, clock-safe ratios, and clean aesthetic washes.
              </p>
            </div>

            {/* Featured Image Details */}
            <div className="p-4 rounded-xl bg-white/70 dark:bg-[#202221] border border-[#E2DDD5] dark:border-[#333534] space-y-2 max-w-lg">
              <div className="flex items-center justify-between text-xs">
                <span className="font-medium text-[#2C2B29] dark:text-[#F3F4F1] truncate max-w-[240px]">
                  {featuredWallpaper.title}
                </span>
                <span className="font-mono text-[11px] text-[#726F68] dark:text-[#939188]">
                  {featuredWallpaper.license.code}
                </span>
              </div>
              <div className="text-xs text-[#726F68] dark:text-[#939188] flex items-center gap-3">
                <span>By {featuredWallpaper.license.author || 'Open Archive'}</span>
                <span>·</span>
                <span className="uppercase text-[10px] tracking-wider font-semibold text-[#556952] dark:text-[#BBCBB8]">
                  {featuredWallpaper.provider}
                </span>
              </div>
            </div>

            {/* Actions */}
            <div className="flex flex-wrap items-center gap-2.5 sm:gap-3 pt-2">
              <button
                onClick={() => onSelectWallpaper(featuredWallpaper)}
                className="min-h-[48px] px-6 rounded-xl bg-[#556952] hover:bg-[#435340] text-white text-sm font-medium tracking-wide flex items-center gap-2.5 shadow-sm transition-all hover:scale-[1.02] active:scale-[0.98] cursor-pointer"
              >
                <Smartphone className="w-4 h-4" />
                <span>Set as Wallpaper</span>
              </button>

              <button
                onClick={() => onEditInStudio(featuredWallpaper)}
                className="min-h-[48px] px-5 rounded-xl border border-[#2C2B29] dark:border-[#E2E3E0] hover:bg-[#2C2B29] hover:text-white dark:hover:bg-white dark:hover:text-black text-xs font-medium tracking-wide flex items-center gap-2 transition-colors cursor-pointer"
              >
                <Sparkles className="w-4 h-4 text-[#8A9A86] dark:text-[#556952]" />
                <span>Studio</span>
              </button>

              <button
                onClick={() => setIsZenBreathingOpen(true)}
                className="min-h-[48px] px-4 rounded-xl border border-[#8A9A86] bg-[#8A9A86]/10 hover:bg-[#8A9A86]/20 text-[#3A4B37] dark:text-[#BBCBB8] text-xs font-medium tracking-wide flex items-center gap-2 transition-colors cursor-pointer"
              >
                <Wind className="w-4 h-4 text-[#556952] dark:text-[#BBCBB8] animate-pulse" />
                <span>Zen Moments (60s)</span>
              </button>

              <button
                onClick={() => fileInputRef.current?.click()}
                className="min-h-[48px] px-4 rounded-xl border border-[#DCD7CE] dark:border-[#383A38] hover:bg-white dark:hover:bg-[#262827] text-xs font-medium text-[#2C2B29] dark:text-[#E2E3E0] flex items-center gap-1.5 transition-colors cursor-pointer"
              >
                <FileUp className="w-4 h-4 text-[#556952] dark:text-[#BBCBB8]" />
                <span>Import Photo</span>
              </button>
            </div>
          </div>

          {/* Right Phone Simulator Frame */}
          <div className="lg:col-span-5 flex justify-center">
            <div
              className="relative w-[240px] sm:w-[260px] aspect-[9/19] rounded-[36px] p-2.5 bg-[#2C2B29] dark:bg-[#333534] shadow-2xl ring-1 ring-black/10 dark:ring-white/10 group cursor-pointer"
              onClick={() => onSelectWallpaper(featuredWallpaper)}
              title="Click to inspect and set as wallpaper"
            >
              {/* Speaker notch */}
              <div className="absolute top-4 left-1/2 -translate-x-1/2 w-16 h-3.5 bg-black/60 rounded-full z-20 flex items-center justify-center">
                <div className="w-2.5 h-2.5 rounded-full bg-black/80" />
              </div>

              {/* Phone display viewport */}
              <div className="relative w-full h-full rounded-[28px] overflow-hidden bg-black">
                <img
                  src={featuredWallpaper.fullUrl || featuredWallpaper.thumbnailUrl}
                  alt={featuredWallpaper.title}
                  className="w-full h-full object-cover transition-transform duration-700 group-hover:scale-105"
                />

                {/* Simulated lock screen overlay */}
                <div className="absolute inset-0 bg-gradient-to-b from-black/40 via-transparent to-black/60 p-4 flex flex-col justify-between text-white select-none pointer-events-none">
                  <div className="pt-8 text-center space-y-0.5">
                    <p className="text-[11px] font-medium tracking-wide uppercase opacity-90 drop-shadow-sm">
                      {dateString || 'Mon, Oct 21'}
                    </p>
                    <h3 className="font-sans font-extralight text-5xl tracking-tight leading-none drop-shadow-md">
                      {timeString || '12:00'}
                    </h3>
                  </div>

                  <div className="pb-6 text-center space-y-1">
                    <p className="font-serif italic text-xs tracking-wider opacity-90 drop-shadow-sm">
                      "Find stillness in every frame."
                    </p>
                    <p className="text-[9px] uppercase tracking-widest text-[#BBCBB8] font-mono">
                      ZENITHAN STUDIO · TAP TO EDIT
                    </p>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </section>

      {/* Zen Moments: 60-Second Mindfulness Breathing Guide */}
      <section className="relative overflow-hidden rounded-2xl bg-gradient-to-r from-[#8A9A86]/15 via-white/70 to-[#8A9A86]/10 dark:from-[#252E27] dark:via-[#1D221F] dark:to-[#222923] border border-[#8A9A86]/30 dark:border-[#3D4D3E] p-6 shadow-xs">
        <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-5">
          <div className="flex items-start gap-4">
            <div className="w-12 h-12 rounded-2xl bg-[#556952] text-white flex items-center justify-center shrink-0 shadow-sm relative">
              <span className="absolute inset-0 rounded-2xl bg-[#556952] animate-ping opacity-20" />
              <Wind className="w-6 h-6 relative z-10" />
            </div>
            <div className="space-y-1">
              <div className="flex items-center gap-2">
                <span className="text-[11px] font-mono font-medium tracking-wider uppercase text-[#556952] dark:text-[#BBCBB8]">
                  Mindfulness Exercise · 60 Seconds
                </span>
              </div>
              <h2 className="font-serif text-xl sm:text-2xl font-semibold text-[#2C2B29] dark:text-[#F3F4F1]">
                Zen Moments Breathing Guide
              </h2>
              <p className="text-xs sm:text-sm text-[#726F68] dark:text-[#939188] max-w-xl">
                Center your thoughts with 60 seconds of gentle guided breathwork and soft pulsating rhythms before entering the wallpaper studio.
              </p>
            </div>
          </div>

          <button
            onClick={() => setIsZenBreathingOpen(true)}
            className="w-full sm:w-auto px-5 py-3 rounded-xl text-xs font-semibold bg-[#556952] hover:bg-[#435340] text-white hover:scale-[1.02] active:scale-[0.98] transition-all flex items-center justify-center gap-2.5 shadow-sm shrink-0 cursor-pointer"
          >
            <Wind className="w-4 h-4 animate-pulse" />
            <span>Begin 60s Breathwork</span>
          </button>
        </div>
      </section>

      {/* Feature 6: Circadian Stillness (Time-of-Day Adaptive Curation) */}
      <section className="p-6 rounded-2xl bg-gradient-to-r from-[#EFECE6]/80 via-white/60 to-[#EFECE6]/80 dark:from-[#1A1C1B] dark:via-[#222423] dark:to-[#1A1C1B] border border-[#E8E4DC] dark:border-[#2D302F] shadow-xs">
        <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4">
          <div className="space-y-1">
            <div className="flex items-center gap-2">
              <span className="w-2 h-2 rounded-full" style={{ backgroundColor: circadianSlot.accentColor }} />
              <span className="text-[11px] font-mono font-medium tracking-wider uppercase text-[#726F68] dark:text-[#939188]">
                Atmospheric Stillness · {circadianSlot.hourRange}
              </span>
            </div>
            <h2 className="font-serif text-xl sm:text-2xl font-semibold text-[#2C2B29] dark:text-[#F3F4F1]">
              {circadianSlot.japaneseTitle}
            </h2>
            <p className="text-xs sm:text-sm text-[#726F68] dark:text-[#939188] max-w-xl">
              {circadianSlot.moodDescription}
            </p>
          </div>

          <button
            onClick={() => onSelectCategory(circadianSlot.recommendedCategory)}
            className="px-4 py-2.5 rounded-xl text-xs font-semibold bg-[#2C2B29] text-white dark:bg-[#E2E3E0] dark:text-[#121413] hover:scale-[1.02] transition-transform flex items-center gap-2 shadow-xs shrink-0"
          >
            <span>Immerse in {circadianSlot.label}</span>
            <ArrowRight className="w-3.5 h-3.5" />
          </button>
        </div>
      </section>

      {/* Curated Nature Categories Section */}
      <section className="space-y-4">
        <div className="flex items-center justify-between">
          <div>
            <h2 className="font-serif text-2xl font-semibold text-[#2C2B29] dark:text-[#F3F4F1] tracking-tight">
              Curated Moods & Aesthetics
            </h2>
            <p className="text-xs sm:text-sm text-[#726F68] dark:text-[#939188]">
              Select a calm natural aesthetic to discover peaceful public domain photography
            </p>
          </div>
          <button
            onClick={onNavigateExplore}
            className="text-xs font-medium text-[#556952] dark:text-[#BBCBB8] hover:underline flex items-center gap-1"
          >
            <span>Browse Wallpapers</span>
            <ArrowRight className="w-3.5 h-3.5" />
          </button>
        </div>

        {/* Categories Grid */}
        <div className="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-5 gap-3 sm:gap-4">
          {CURATED_CATEGORIES.map((cat: CuratedCategory) => {
            const Icon = CATEGORY_ICONS[cat.icon] || Mountain;
            return (
              <button
                key={cat.id}
                onClick={() => onSelectCategory(cat.id)}
                className="p-4 rounded-xl text-left bg-white dark:bg-[#1E201F] border border-[#E8E4DC] dark:border-[#2D302F] hover:border-[#8A9A86] dark:hover:border-[#BBCBB8] hover:shadow-md transition-all group flex flex-col justify-between h-[120px]"
              >
                <div className="w-8 h-8 rounded-lg bg-[#EFECE6] dark:bg-[#282A29] flex items-center justify-center text-[#556952] dark:text-[#BBCBB8] group-hover:scale-110 transition-transform">
                  <Icon className="w-4 h-4" />
                </div>
                <div>
                  <h3 className="font-medium text-xs sm:text-sm text-[#2C2B29] dark:text-[#F3F4F1] line-clamp-1">
                    {cat.label}
                  </h3>
                  <p className="text-[11px] text-[#726F68] dark:text-[#939188] line-clamp-1">
                    {cat.subtitle}
                  </p>
                </div>
              </button>
            );
          })}
        </div>
      </section>

      {/* Recent Wallpapers (if available) */}
      {recentWallpapers.length > 0 && (
        <section className="space-y-4">
          <div className="flex items-center justify-between">
            <div>
              <h2 className="font-serif text-2xl font-semibold text-[#2C2B29] dark:text-[#F3F4F1] tracking-tight">
                Recently Viewed Stillness
              </h2>
              <p className="text-xs sm:text-sm text-[#726F68] dark:text-[#939188]">
                Quickly return to wallpapers you’ve inspected or edited
              </p>
            </div>
          </div>

          <div className="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-6 gap-3 sm:gap-4">
            {recentWallpapers.slice(0, 6).map((item) => {
              const favorited = isFavorited(item.id);
              return (
                <div
                  key={item.id}
                  className="group relative rounded-xl overflow-hidden aspect-[9/16] bg-[#0D0F0E] border border-[#E8E4DC] dark:border-[#2D302F] shadow-sm cursor-pointer"
                  onClick={() => onSelectWallpaper(item)}
                >
                  <img
                    src={item.thumbnailUrl}
                    alt={item.title}
                    className="w-full h-full object-cover transition-transform duration-500 group-hover:scale-105"
                    loading="lazy"
                  />
                  <div className="absolute inset-0 bg-gradient-to-t from-black/80 via-black/20 to-transparent opacity-0 group-hover:opacity-100 transition-opacity p-2.5 flex flex-col justify-between">
                    <div className="flex justify-end">
                      <button
                        onClick={(e) => {
                          e.stopPropagation();
                          onToggleFavorite(item);
                        }}
                        className="p-1.5 rounded-full bg-black/40 hover:bg-black/60 text-white backdrop-blur-sm"
                      >
                        <Heart className={`w-3.5 h-3.5 ${favorited ? 'fill-rose-500 text-rose-500' : ''}`} />
                      </button>
                    </div>
                    <div>
                      <p className="text-white text-xs font-medium line-clamp-1">{item.title}</p>
                      <button
                        onClick={(e) => {
                          e.stopPropagation();
                          onEditInStudio(item);
                        }}
                        className="mt-1 w-full py-1 rounded bg-white/90 text-black text-[10px] font-semibold flex items-center justify-center gap-1"
                      >
                        <Sliders className="w-3 h-3" />
                        <span>Edit</span>
                      </button>
                    </div>
                  </div>
                </div>
              );
            })}
          </div>
        </section>
      )}

      {/* Mindful Philosophy Footer Banner */}
      <section className="p-6 rounded-2xl bg-[#EFECE6]/40 dark:bg-[#1A1C1B]/40 border border-[#E8E4DC] dark:border-[#262827] flex flex-col sm:flex-row items-center justify-between gap-4 text-center sm:text-left">
        <div className="flex flex-col sm:flex-row items-center gap-3.5">
          <img
            src="/logo.png"
            alt="Zenithan Studio Logo"
            className="w-11 h-11 rounded-xl object-cover shadow-sm border border-[#E8E4DC] dark:border-[#383A38] shrink-0"
          />
          <div className="space-y-1">
            <p className="font-serif italic text-sm text-[#2C2B29] dark:text-[#E2E3E0]">
              "Nothing missing, nothing to add. Nature finishes without hurrying."
            </p>
            <p className="text-xs text-[#726F68] dark:text-[#939188]">
              Zenithan Studio · 100% Client-side · Free & verified open-licensed photography.
            </p>
          </div>
        </div>
        <div className="flex items-center gap-2">
          <button
            onClick={() => onSelectCategory('japanese-gardens')}
            className="px-3 py-1.5 rounded-lg border border-[#DCD7CE] dark:border-[#383A38] text-xs font-medium text-[#2C2B29] dark:text-[#E2E3E0] hover:bg-white dark:hover:bg-[#202221] transition-colors"
          >
            Lush Moss
          </button>
          <button
            onClick={() => onSelectCategory('misty-mountains')}
            className="px-3 py-1.5 rounded-lg border border-[#DCD7CE] dark:border-[#383A38] text-xs font-medium text-[#2C2B29] dark:text-[#E2E3E0] hover:bg-white dark:hover:bg-[#202221] transition-colors"
          >
            Mist Peaks
          </button>
        </div>
      </section>

      {/* Zen Moments 60-Second Breathing Modal */}
      <ZenBreathingTimer
        isOpen={isZenBreathingOpen}
        onClose={() => setIsZenBreathingOpen(false)}
        onStartBuilding={(item) => onEditInStudio(item || featuredWallpaper)}
        targetWallpaper={featuredWallpaper}
      />
    </div>
  );
};
