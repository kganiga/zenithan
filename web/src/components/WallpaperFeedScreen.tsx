import React from 'react';
import {
  Search,
  X,
  Filter,
  Sparkles,
  Heart,
  RefreshCw,
  Eye,
  FileUp,
  SlidersHorizontal,
  Compass,
  ArrowUp,
  Image as ImageIcon,
  Shuffle,
  LayoutGrid,
  Grid2X2,
  Smartphone,
  Download,
} from 'lucide-react';
import { WallpaperItem, FilterOptions, ProviderType } from '../types';
import { CURATED_CATEGORIES } from '../data/curatedData';
import { fetchWallpapers, reshuffleSession } from '../services/wallpaperRepository';

interface WallpaperFeedScreenProps {
  onSelectWallpaper: (wallpaper: WallpaperItem) => void;
  onEditInStudio: (wallpaper: WallpaperItem) => void;
  isFavorited: (id: string) => boolean;
  onToggleFavorite: (item: WallpaperItem) => void;
  onImportCustomPhoto?: (wallpaper: WallpaperItem) => void;
  onNavigateCurated: () => void;
  initialCategory?: string;
}

export const WallpaperFeedScreen: React.FC<WallpaperFeedScreenProps> = ({
  onSelectWallpaper,
  onEditInStudio,
  isFavorited,
  onToggleFavorite,
  onImportCustomPhoto,
  onNavigateCurated,
  initialCategory,
}) => {
  const [filters, setFilters] = React.useState<FilterOptions>({
    query: '',
    category: initialCategory || '',
    provider: 'all',
    orientation: 'all',
    licenseOnly: 'all',
  });

  // Sync if initialCategory prop changes
  React.useEffect(() => {
    if (initialCategory !== undefined) {
      setFilters((prev) => ({ ...prev, category: initialCategory }));
      setPage(1);
    }
  }, [initialCategory]);

  const [searchInput, setSearchInput] = React.useState('');
  const [items, setItems] = React.useState<WallpaperItem[]>([]);
  const [page, setPage] = React.useState(1);
  const [hasMore, setHasMore] = React.useState(true);
  const [loadingInitial, setLoadingInitial] = React.useState(true);
  const [loadingMore, setLoadingMore] = React.useState(false);
  const [showFiltersModal, setShowFiltersModal] = React.useState(false);
  const [showScrollTop, setShowScrollTop] = React.useState(false);
  const [gridDensity, setGridDensity] = React.useState<'standard' | 'dense'>('standard');
  const [shuffleToast, setShuffleToast] = React.useState(false);

  const sentinelRef = React.useRef<HTMLDivElement | null>(null);

  // Debounced search input
  React.useEffect(() => {
    const handler = setTimeout(() => {
      setFilters((prev) => ({ ...prev, query: searchInput }));
      setPage(1);
      setItems([]);
    }, 350);
    return () => clearTimeout(handler);
  }, [searchInput]);

  // Load initial page whenever filters change
  const loadInitial = React.useCallback(async () => {
    setLoadingInitial(true);
    setPage(1);
    try {
      const result = await fetchWallpapers(filters, 1);
      setItems(result.items);
      setHasMore(result.items.length >= 10);
    } catch (err) {
      console.error('Failed to load initial wallpapers:', err);
    } finally {
      setLoadingInitial(false);
    }
  }, [filters]);

  // Re-shuffle on every component mount or browser reload to ensure a fresh set of images on each visit
  const initialMountRef = React.useRef(true);

  React.useEffect(() => {
    if (initialMountRef.current) {
      initialMountRef.current = false;
      reshuffleSession();
    }
    loadInitial();
  }, [loadInitial]);

  // Handle explicit Shuffle trigger
  const handleShuffle = () => {
    reshuffleSession();
    setShuffleToast(true);
    setTimeout(() => setShuffleToast(false), 2000);
    loadInitial();
    window.scrollTo({ top: 0, behavior: 'smooth' });
  };

  // Load more function for continuous infinite scroll
  const loadMore = React.useCallback(async () => {
    if (loadingMore || !hasMore || loadingInitial) return;

    setLoadingMore(true);
    const nextPage = page + 1;
    try {
      const result = await fetchWallpapers(filters, nextPage);
      if (result.items.length === 0) {
        setHasMore(false);
      } else {
        setItems((prev) => {
          const existingKeys = new Set(prev.map((i) => i.id + (i.thumbnailUrl || '')));
          const newUnique = result.items.filter((i) => !existingKeys.has(i.id + (i.thumbnailUrl || '')));
          if (newUnique.length === 0) {
            // Keep stream rolling with fresh permutation
            return [...prev, ...result.items.map((it, idx) => ({ ...it, id: `${it.id}-p${nextPage}-${idx}` }))];
          }
          return [...prev, ...newUnique];
        });
        setPage(nextPage);
      }
    } catch (err) {
      console.warn('Error fetching more wallpapers:', err);
      setHasMore(false);
    } finally {
      setLoadingMore(false);
    }
  }, [filters, page, hasMore, loadingMore, loadingInitial]);

  // Infinite Scroll IntersectionObserver
  React.useEffect(() => {
    if (!sentinelRef.current || !hasMore || loadingInitial) return;

    const observer = new IntersectionObserver(
      (entries) => {
        if (entries[0].isIntersecting) {
          loadMore();
        }
      },
      {
        rootMargin: '500px', // Pre-fetch smoothly before hitting the bottom
      }
    );

    observer.observe(sentinelRef.current);
    return () => observer.disconnect();
  }, [hasMore, loadMore, loadingInitial]);

  // Scroll to top listener
  React.useEffect(() => {
    const checkScroll = () => {
      setShowScrollTop(window.scrollY > 600);
    };
    window.addEventListener('scroll', checkScroll, { passive: true });
    return () => window.removeEventListener('scroll', checkScroll);
  }, []);

  const scrollToTop = () => {
    window.scrollTo({ top: 0, behavior: 'smooth' });
  };

  return (
    <div className="max-w-7xl mx-auto px-4 sm:px-6 pt-5 pb-24 md:pb-16 space-y-5">
      {/* Search & Top Action Bar */}
      <div className="space-y-3">
        <div className="flex flex-col sm:flex-row gap-2.5 items-stretch sm:items-center justify-between">
          {/* Search Box */}
          <div className="relative flex-1 max-w-xl">
            <Search className="absolute left-3.5 top-1/2 -translate-y-1/2 w-4 h-4 text-[#726F68] dark:text-[#939188]" />
            <input
              type="text"
              value={searchInput}
              onChange={(e) => setSearchInput(e.target.value)}
              placeholder="Search 100+ wallpapers: mountains, AMOLED dark, ocean, neon..."
              className="w-full pl-10 pr-9 py-2.5 rounded-xl bg-white dark:bg-[#1E201F] border border-[#E8E4DC] dark:border-[#2D302F] text-sm text-[#2C2B29] dark:text-[#F3F4F1] placeholder-[#9E9B93] dark:placeholder-[#6C6E6D] focus:outline-none focus:ring-1 focus:ring-[#8A9A86] transition-all shadow-xs"
            />
            {searchInput && (
              <button
                onClick={() => {
                  setSearchInput('');
                  setFilters((prev) => ({ ...prev, query: '' }));
                }}
                className="absolute right-3 top-1/2 -translate-y-1/2 p-1 text-[#726F68] hover:text-[#2C2B29] dark:hover:text-white"
              >
                <X className="w-3.5 h-3.5" />
              </button>
            )}
          </div>

          {/* Quick Actions Bar */}
          <div className="flex items-center gap-2">
            {/* 1-Tap Shuffle / Randomize Button (Zedge Discovery) */}
            <button
              onClick={handleShuffle}
              title="Shuffle wallpapers to discover fresh sets"
              className="min-h-[42px] px-3.5 rounded-xl bg-[#2C2B29] text-white dark:bg-[#E2E3E0] dark:text-[#121413] hover:scale-105 active:scale-95 text-xs font-semibold flex items-center gap-2 transition-all shadow-xs shrink-0"
            >
              <Shuffle className="w-4 h-4 text-[#8A9A86] dark:text-[#556952]" />
              <span>Shuffle</span>
            </button>

            {/* Density Toggle (Standard vs Dense) */}
            <button
              onClick={() => setGridDensity((prev) => (prev === 'standard' ? 'dense' : 'standard'))}
              title={gridDensity === 'standard' ? 'Switch to compact view' : 'Switch to standard view'}
              className="min-h-[42px] px-3 rounded-xl bg-white dark:bg-[#1E201F] border border-[#E8E4DC] dark:border-[#2D302F] text-[#726F68] dark:text-[#939188] hover:text-[#2C2B29] dark:hover:text-[#F3F4F1] flex items-center gap-1.5 text-xs transition-colors shadow-xs"
            >
              {gridDensity === 'standard' ? (
                <LayoutGrid className="w-4 h-4" />
              ) : (
                <Grid2X2 className="w-4 h-4" />
              )}
              <span className="hidden md:inline capitalize">{gridDensity}</span>
            </button>

            {/* Import Personal Photo */}
            <input
              type="file"
              id="feed-file-import"
              onChange={(e) => {
                const file = e.target.files?.[0];
                if (!file) return;
                const reader = new FileReader();
                reader.onload = (event) => {
                  const dataUrl = event.target?.result as string;
                  const img = new Image();
                  img.onload = () => {
                    const customItem: WallpaperItem = {
                      id: `custom-${Date.now()}`,
                      title: file.name.replace(/\.[^/.]+$/, '').replace(/[_-]/g, ' ') || 'Personal Photo',
                      description: 'Imported from personal device library.',
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
              }}
              accept="image/*"
              className="hidden"
            />
            <label
              htmlFor="feed-file-import"
              title="Import your personal wallpaper or photo"
              className="min-h-[42px] px-3.5 rounded-xl border border-[#DCD7CE] dark:border-[#383A38] bg-white dark:bg-[#1E201F] text-xs font-medium text-[#2C2B29] dark:text-[#E2E3E0] hover:border-[#8A9A86] flex items-center gap-1.5 cursor-pointer transition-colors shadow-xs"
            >
              <FileUp className="w-4 h-4 text-[#556952] dark:text-[#BBCBB8]" />
              <span className="hidden sm:inline">Import</span>
            </label>

            {/* Filter Drawer Toggle */}
            <button
              onClick={() => setShowFiltersModal(!showFiltersModal)}
              className={`min-h-[42px] px-3.5 rounded-xl border text-xs font-medium flex items-center gap-2 transition-colors ${
                filters.orientation !== 'all' || filters.licenseOnly !== 'all'
                  ? 'bg-[#8A9A86]/20 border-[#8A9A86] text-[#3D4C3A] dark:text-[#BBCBB8]'
                  : 'bg-white dark:bg-[#1E201F] border-[#E8E4DC] dark:border-[#2D302F] text-[#726F68] dark:text-[#939188] hover:text-[#2C2B29]'
              }`}
            >
              <SlidersHorizontal className="w-4 h-4" />
              <span className="hidden sm:inline">Filters</span>
              {(filters.orientation !== 'all' || filters.licenseOnly !== 'all') && (
                <span className="w-2 h-2 rounded-full bg-[#8A9A86]" />
              )}
            </button>

            {/* Refresh */}
            <button
              onClick={loadInitial}
              title="Refresh wallpapers"
              className="min-h-[42px] min-w-[42px] rounded-xl bg-white dark:bg-[#1E201F] border border-[#E8E4DC] dark:border-[#2D302F] text-[#726F68] dark:text-[#939188] hover:text-[#2C2B29] flex items-center justify-center transition-colors shadow-xs"
            >
              <RefreshCw className={`w-4 h-4 ${loadingInitial ? 'animate-spin' : ''}`} />
            </button>
          </div>
        </div>

        {/* Categories Horizontal Ribbons */}
        <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-2.5 pt-1">
          {/* Quick Category Chips */}
          <div className="flex items-center gap-1.5 overflow-x-auto pb-1 scrollbar-none text-xs">
            <button
              onClick={onNavigateCurated}
              title="View today's curated mood guide & lockscreen preview"
              className="px-3 py-1.5 rounded-full whitespace-nowrap transition-colors text-[11px] font-medium border border-[#8A9A86]/40 text-[#4A5D47] dark:text-[#BBCBB8] hover:bg-[#8A9A86]/10 flex items-center gap-1 shrink-0"
            >
              <Sparkles className="w-3 h-3 text-[#556952] dark:text-[#BBCBB8]" />
              <span>Discover Moods</span>
            </button>

            <button
              onClick={() => {
                setFilters((prev) => ({ ...prev, category: '' }));
                setPage(1);
              }}
              className={`px-3 py-1.5 rounded-full whitespace-nowrap transition-colors font-semibold text-[11px] ${
                !filters.category
                  ? 'bg-[#2C2B29] text-white dark:bg-[#E2E3E0] dark:text-[#121413]'
                  : 'bg-[#EFECE6] dark:bg-[#202221] text-[#726F68] dark:text-[#939188] hover:text-[#2C2B29]'
              }`}
            >
              All Wallpapers
            </button>

            {CURATED_CATEGORIES.map((cat) => {
              const isCatActive = filters.category === cat.id;
              return (
                <button
                  key={cat.id}
                  onClick={() => {
                    setFilters((prev) => ({
                      ...prev,
                      category: isCatActive ? '' : cat.id,
                    }));
                    setPage(1);
                  }}
                  className={`px-3 py-1.5 rounded-full whitespace-nowrap transition-colors text-[11px] font-medium ${
                    isCatActive
                      ? 'bg-[#8A9A86] text-white shadow-xs font-semibold'
                      : 'bg-[#EFECE6] dark:bg-[#202221] text-[#726F68] dark:text-[#939188] hover:text-[#2C2B29]'
                  }`}
                >
                  {cat.label}
                </button>
              );
            })}
          </div>

          {/* Provider selector */}
          <div className="flex items-center gap-1 text-[11px] overflow-x-auto pb-1 scrollbar-none shrink-0">
            {[
              { id: 'all', label: 'All Archives' },
              { id: 'local', label: 'Curated 4K' },
              { id: 'wikimedia', label: 'Wikimedia' },
              { id: 'nasa', label: 'NASA Space' },
            ].map((p) => (
              <button
                key={p.id}
                onClick={() => {
                  setFilters((prev) => ({ ...prev, provider: p.id as any }));
                  setPage(1);
                }}
                className={`px-2 py-0.5 rounded-md border transition-colors ${
                  filters.provider === p.id
                    ? 'border-[#8A9A86] text-[#3D4C3A] dark:text-[#BBCBB8] font-semibold bg-[#8A9A86]/10'
                    : 'border-transparent text-[#726F68] hover:text-[#2C2B29]'
                }`}
              >
                {p.label}
              </button>
            ))}
          </div>
        </div>

        {/* Shuffle Toast Notification */}
        {shuffleToast && (
          <div className="p-2.5 rounded-xl bg-[#8A9A86]/15 dark:bg-[#BBCBB8]/15 border border-[#8A9A86]/30 text-xs text-[#3D4C3A] dark:text-[#BBCBB8] flex items-center justify-between animate-in fade-in slide-in-from-top-1 duration-200">
            <span className="font-medium flex items-center gap-1.5">
              <Shuffle className="w-3.5 h-3.5" />
              Wallpapers reshuffled! New discover stream loaded.
            </span>
          </div>
        )}

        {/* Filter Drawer */}
        {showFiltersModal && (
          <div className="p-4 rounded-xl bg-white dark:bg-[#1E201F] border border-[#E8E4DC] dark:border-[#2D302F] space-y-4 animate-in fade-in duration-150 shadow-sm">
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
              <div>
                <label className="text-xs font-semibold text-[#2C2B29] dark:text-[#F3F4F1] block mb-2">
                  Image Orientation
                </label>
                <div className="flex gap-2">
                  {[
                    { id: 'all', label: 'Any' },
                    { id: 'portrait', label: 'Phone Portrait' },
                    { id: 'landscape', label: 'Landscape' },
                    { id: 'square', label: 'Square' },
                  ].map((opt) => (
                    <button
                      key={opt.id}
                      onClick={() => setFilters((prev) => ({ ...prev, orientation: opt.id as any }))}
                      className={`flex-1 py-1.5 px-2 rounded-lg text-xs font-medium border transition-colors ${
                        filters.orientation === opt.id
                          ? 'bg-[#2C2B29] text-white dark:bg-white dark:text-black border-transparent'
                          : 'border-[#E2DDD5] dark:border-[#333534] text-[#726F68] hover:bg-[#F8F6F0]'
                      }`}
                    >
                      {opt.label}
                    </button>
                  ))}
                </div>
              </div>

              <div>
                <label className="text-xs font-semibold text-[#2C2B29] dark:text-[#F3F4F1] block mb-2">
                  License & Copyright
                </label>
                <div className="flex gap-2">
                  {[
                    { id: 'all', label: 'All Verified' },
                    { id: 'cc0_pd', label: 'Public Domain / CC0' },
                    { id: 'commercial', label: 'Commercial Friendly' },
                  ].map((opt) => (
                    <button
                      key={opt.id}
                      onClick={() => setFilters((prev) => ({ ...prev, licenseOnly: opt.id as any }))}
                      className={`flex-1 py-1.5 px-2 rounded-lg text-xs font-medium border transition-colors ${
                        filters.licenseOnly === opt.id
                          ? 'bg-[#2C2B29] text-white dark:bg-white dark:text-black border-transparent'
                          : 'border-[#E2DDD5] dark:border-[#333534] text-[#726F68] hover:bg-[#F8F6F0]'
                      }`}
                    >
                      {opt.label}
                    </button>
                  ))}
                </div>
              </div>
            </div>
          </div>
        )}
      </div>

      {/* Infinite Wallpaper Grid */}
      {loadingInitial ? (
        <div
          className={
            gridDensity === 'standard'
              ? 'grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-4 gap-3 sm:gap-4'
              : 'grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-5 xl:grid-cols-6 gap-2.5 sm:gap-3'
          }
        >
          {Array.from({ length: 16 }).map((_, idx) => (
            <div
              key={idx}
              className="rounded-2xl overflow-hidden aspect-[9/16] bg-[#EFECE6] dark:bg-[#1E201F] animate-pulse"
            />
          ))}
        </div>
      ) : items.length === 0 ? (
        <div className="p-12 text-center rounded-2xl border border-dashed border-[#DCD7CE] dark:border-[#333534] space-y-4">
          <div className="w-12 h-12 mx-auto rounded-full bg-[#EFECE6] dark:bg-[#202221] flex items-center justify-center text-[#726F68]">
            <Compass className="w-6 h-6" />
          </div>
          <div className="space-y-1">
            <h3 className="font-serif text-lg font-medium text-[#2C2B29] dark:text-[#F3F4F1]">
              No Wallpapers in Current Stream
            </h3>
            <p className="text-xs text-[#726F68] dark:text-[#939188] max-w-sm mx-auto">
              No matching imagery found. Try clearing your search keywords or tapping Shuffle to see a fresh selection.
            </p>
          </div>
          <button
            onClick={() => {
              setSearchInput('');
              setFilters({
                query: '',
                category: '',
                provider: 'all',
                orientation: 'all',
                licenseOnly: 'all',
              });
              handleShuffle();
            }}
            className="px-4 py-2 rounded-lg bg-[#2C2B29] text-white dark:bg-[#E2E3E0] dark:text-[#121413] text-xs font-medium"
          >
            Reset Filters & Shuffle
          </button>
        </div>
      ) : (
        <div className="space-y-6">
          {/* Main Grid */}
          <div
            className={
              gridDensity === 'standard'
                ? 'grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-4 gap-3 sm:gap-4'
                : 'grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-5 xl:grid-cols-6 gap-2.5 sm:gap-3'
            }
          >
            {items.map((item) => {
              const favorited = isFavorited(item.id);
              const isDarkTheme = item.category === 'moon-night-sky' || item.title.toLowerCase().includes('dark') || item.title.toLowerCase().includes('obsidian');

              return (
                <div
                  key={item.id}
                  onClick={() => onSelectWallpaper(item)}
                  className="group relative rounded-2xl overflow-hidden aspect-[9/16] bg-[#0D0F0E] border border-[#E8E4DC] dark:border-[#282A29] shadow-xs hover:shadow-lg transition-all cursor-pointer"
                >
                  <img
                    src={item.thumbnailUrl}
                    alt={item.title}
                    className="w-full h-full object-cover transition-transform duration-700 group-hover:scale-105"
                    loading="lazy"
                  />

                  {/* Top Bar with Quality Badge & Favorite */}
                  <div className="absolute top-2.5 left-2.5 right-2.5 flex items-center justify-between pointer-events-none z-10">
                    <span className="text-[9px] font-mono font-semibold uppercase tracking-wider px-2 py-0.5 rounded-full bg-black/60 backdrop-blur-md text-white/90">
                      {isDarkTheme ? 'AMOLED' : '4K HD'}
                    </span>

                    <button
                      onClick={(e) => {
                        e.stopPropagation();
                        onToggleFavorite(item);
                      }}
                      aria-label="Save to Favorites"
                      className="pointer-events-auto p-1.5 rounded-full bg-black/40 hover:bg-black/60 text-white backdrop-blur-md transition-colors"
                    >
                      <Heart className={`w-3.5 h-3.5 ${favorited ? 'fill-rose-500 text-rose-500' : ''}`} />
                    </button>
                  </div>

                  {/* Bottom Hover Card Details */}
                  <div className="absolute inset-0 bg-gradient-to-t from-black/90 via-black/30 to-transparent opacity-0 group-hover:opacity-100 transition-opacity p-3 flex flex-col justify-end text-white">
                    <p className="font-serif text-xs font-semibold line-clamp-1 leading-snug">
                      {item.title}
                    </p>
                    <p className="text-[10px] text-white/70 line-clamp-1 mt-0.5">
                      {item.license.author || 'Open Creator'} · {item.license.code}
                    </p>

                    <div className="flex items-center gap-1.5 mt-2.5">
                      <button
                        onClick={(e) => {
                          e.stopPropagation();
                          onSelectWallpaper(item);
                        }}
                        className="flex-1 py-1.5 px-2 rounded-lg bg-[#556952] hover:bg-[#435340] text-white text-[11px] font-semibold flex items-center justify-center gap-1 shadow-sm transition-all"
                      >
                        <Smartphone className="w-3 h-3" />
                        <span>Preview & Apply</span>
                      </button>
                      <button
                        onClick={(e) => {
                          e.stopPropagation();
                          onEditInStudio(item);
                        }}
                        className="py-1.5 px-2.5 rounded-lg bg-white/20 hover:bg-white/30 text-white text-[11px] font-medium flex items-center justify-center gap-1 backdrop-blur-sm"
                        title="Customize in Studio"
                      >
                        <Sparkles className="w-3 h-3" />
                        <span className="hidden sm:inline">Studio</span>
                      </button>
                    </div>
                  </div>
                </div>
              );
            })}
          </div>

          {/* Sentinel Element for Continuous Infinite Scroll */}
          <div ref={sentinelRef} className="py-8 flex flex-col items-center justify-center text-xs text-[#726F68] dark:text-[#939188]">
            {loadingMore ? (
              <div className="flex items-center gap-2.5 p-3 rounded-full bg-white dark:bg-[#1E201F] border border-[#E8E4DC] dark:border-[#2D302F] shadow-sm">
                <div className="w-4 h-4 border-2 border-[#8A9A86] border-t-transparent rounded-full animate-spin" />
                <span className="font-medium text-xs">Loading more stunning wallpapers...</span>
              </div>
            ) : hasMore ? (
              <div className="flex flex-col items-center gap-1 text-[11px] opacity-70">
                <span className="font-mono">Scroll down for continuous discovery</span>
              </div>
            ) : (
              <div className="text-center space-y-1">
                <p className="font-serif italic text-sm">"Endless horizon."</p>
                <p className="text-[11px] opacity-70">You have explored hundreds of wallpapers. Tap Shuffle above for a new stream!</p>
              </div>
            )}
          </div>
        </div>
      )}

      {/* Floating Scroll to Top Button */}
      {showScrollTop && (
        <button
          onClick={scrollToTop}
          aria-label="Scroll to top"
          className="fixed bottom-20 right-5 md:bottom-8 md:right-8 z-30 p-3 rounded-full bg-[#2C2B29] text-white dark:bg-white dark:text-black shadow-lg hover:scale-105 active:scale-95 transition-all"
        >
          <ArrowUp className="w-4 h-4" />
        </button>
      )}
    </div>
  );
};
