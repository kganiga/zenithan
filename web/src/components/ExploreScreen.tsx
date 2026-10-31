import React from 'react';
import { Search, X, Filter, Sparkles, Heart, RefreshCw, Eye, ShieldCheck, Compass, Image as ImageIcon, FileUp } from 'lucide-react';
import { WallpaperItem, FilterOptions, ProviderType } from '../types';
import { CURATED_CATEGORIES } from '../data/curatedData';
import { fetchWallpapers } from '../services/wallpaperRepository';

interface ExploreScreenProps {
  initialCategory?: string;
  onSelectWallpaper: (wallpaper: WallpaperItem) => void;
  onEditInStudio: (wallpaper: WallpaperItem) => void;
  isFavorited: (id: string) => boolean;
  onToggleFavorite: (item: WallpaperItem) => void;
  onImportCustomPhoto?: (wallpaper: WallpaperItem) => void;
}

export const ExploreScreen: React.FC<ExploreScreenProps> = ({
  initialCategory,
  onSelectWallpaper,
  onEditInStudio,
  isFavorited,
  onToggleFavorite,
  onImportCustomPhoto,
}) => {
  const [filters, setFilters] = React.useState<FilterOptions>({
    query: '',
    category: initialCategory || '',
    provider: 'all',
    orientation: 'all',
    licenseOnly: 'all',
  });

  const [searchInput, setSearchInput] = React.useState('');
  const [items, setItems] = React.useState<WallpaperItem[]>([]);
  const [loading, setLoading] = React.useState(true);
  const [activeProviders, setActiveProviders] = React.useState<ProviderType[]>([]);
  const [showFiltersModal, setShowFiltersModal] = React.useState(false);

  // Sync category if passed from parent
  React.useEffect(() => {
    if (initialCategory !== undefined) {
      setFilters((prev) => ({ ...prev, category: initialCategory }));
    }
  }, [initialCategory]);

  // Debounced search input
  React.useEffect(() => {
    const handler = setTimeout(() => {
      setFilters((prev) => ({ ...prev, query: searchInput }));
    }, 450);
    return () => clearTimeout(handler);
  }, [searchInput]);

  // Fetch wallpapers when filters change
  const loadData = React.useCallback(async () => {
    setLoading(true);
    try {
      const result = await fetchWallpapers(filters);
      setItems(result.items);
      setActiveProviders(result.providersQueried);
    } catch (err) {
      console.error('Failed to fetch wallpapers:', err);
    } finally {
      setLoading(false);
    }
  }, [filters]);

  React.useEffect(() => {
    loadData();
  }, [loadData]);

  const activeCategory = CURATED_CATEGORIES.find((c) => c.id === filters.category);

  return (
    <div className="max-w-7xl mx-auto px-4 sm:px-6 pt-6 pb-24 md:pb-16 space-y-6">
      {/* Top Search & Filter Bar */}
      <div className="flex flex-col sm:flex-row gap-3 items-stretch sm:items-center justify-between">
        {/* Search Input Box */}
        <div className="relative flex-1 max-w-xl">
          <Search className="absolute left-3.5 top-1/2 -translate-y-1/2 w-4 h-4 text-[#726F68] dark:text-[#939188]" />
          <input
            type="text"
            value={searchInput}
            onChange={(e) => setSearchInput(e.target.value)}
            placeholder="Search misty forest, calm ocean, Japanese stone garden..."
            className="w-full pl-10 pr-9 py-2.5 rounded-xl bg-white dark:bg-[#1E201F] border border-[#E8E4DC] dark:border-[#2D302F] text-sm text-[#2C2B29] dark:text-[#F3F4F1] placeholder-[#9E9B93] dark:placeholder-[#6C6E6D] focus:outline-none focus:ring-1 focus:ring-[#8A9A86] transition-all shadow-sm"
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

        {/* Filter Toggle, Import Photo & Refresh */}
        <div className="flex items-center gap-2">
          <input
            type="file"
            id="explore-file-import"
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
            }}
            accept="image/*"
            className="hidden"
          />
          <label
            htmlFor="explore-file-import"
            title="Import your personal nature photo"
            className="min-h-[42px] px-3.5 rounded-xl border border-[#DCD7CE] dark:border-[#383A38] bg-white dark:bg-[#1E201F] text-xs font-medium text-[#2C2B29] dark:text-[#E2E3E0] hover:border-[#8A9A86] flex items-center gap-1.5 cursor-pointer transition-colors shadow-xs"
          >
            <FileUp className="w-4 h-4 text-[#556952] dark:text-[#BBCBB8]" />
            <span className="hidden sm:inline">Import Photo</span>
          </label>

          <button
            onClick={() => setShowFiltersModal(!showFiltersModal)}
            className={`min-h-[42px] px-3.5 rounded-xl border text-xs font-medium flex items-center gap-2 transition-colors ${
              filters.orientation !== 'all' || filters.licenseOnly !== 'all'
                ? 'bg-[#8A9A86]/20 border-[#8A9A86] text-[#3D4C3A] dark:text-[#BBCBB8]'
                : 'bg-white dark:bg-[#1E201F] border-[#E8E4DC] dark:border-[#2D302F] text-[#726F68] dark:text-[#939188] hover:text-[#2C2B29]'
            }`}
          >
            <Filter className="w-4 h-4" />
            <span>Filters</span>
            {(filters.orientation !== 'all' || filters.licenseOnly !== 'all') && (
              <span className="w-2 h-2 rounded-full bg-[#8A9A86]" />
            )}
          </button>

          <button
            onClick={loadData}
            title="Refresh repository results"
            className="min-h-[42px] min-w-[42px] rounded-xl bg-white dark:bg-[#1E201F] border border-[#E8E4DC] dark:border-[#2D302F] text-[#726F68] dark:text-[#939188] hover:text-[#2C2B29] flex items-center justify-center transition-colors"
          >
            <RefreshCw className={`w-4 h-4 ${loading ? 'animate-spin' : ''}`} />
          </button>
        </div>
      </div>

      {/* Provider Selector Tabs (Functional Segmented Control) */}
      <div className="flex items-center gap-1.5 overflow-x-auto pb-1 scrollbar-none text-xs font-medium">
        <span className="text-[#939188] text-[11px] uppercase tracking-wider pr-1 hidden sm:inline">
          Provider:
        </span>
        {[
          { id: 'all', label: 'All Sources' },
          { id: 'wikimedia', label: 'Wikimedia Commons' },
          { id: 'nasa', label: 'NASA Space & Earth' },
          { id: 'openverse', label: 'Openverse' },
          { id: 'local', label: 'Zen Vault (Offline)' },
        ].map((prov) => {
          const isActive = filters.provider === prov.id;
          return (
            <button
              key={prov.id}
              onClick={() => setFilters((prev) => ({ ...prev, provider: prov.id as any }))}
              className={`px-3 py-1.5 rounded-lg whitespace-nowrap transition-colors ${
                isActive
                  ? 'bg-[#2C2B29] text-white dark:bg-[#E2E3E0] dark:text-[#121413] font-semibold'
                  : 'bg-white/80 dark:bg-[#1E201F] text-[#726F68] dark:text-[#939188] hover:text-[#2C2B29] dark:hover:text-[#F3F4F1] border border-[#E8E4DC] dark:border-[#2D302F]'
              }`}
            >
              {prov.label}
            </button>
          );
        })}
      </div>

      {/* Categories Horizontal Quick Bar */}
      <div className="flex items-center gap-1.5 overflow-x-auto pb-1 scrollbar-none text-xs">
        <button
          onClick={() => setFilters((prev) => ({ ...prev, category: '' }))}
          className={`px-3 py-1.5 rounded-full whitespace-nowrap transition-colors ${
            !filters.category
              ? 'bg-[#8A9A86] text-white font-medium'
              : 'bg-[#EFECE6] dark:bg-[#202221] text-[#726F68] dark:text-[#939188] hover:text-[#2C2B29]'
          }`}
        >
          All Moods
        </button>
        {CURATED_CATEGORIES.map((cat) => {
          const isCatActive = filters.category === cat.id;
          return (
            <button
              key={cat.id}
              onClick={() =>
                setFilters((prev) => ({
                  ...prev,
                  category: isCatActive ? '' : cat.id,
                }))
              }
              className={`px-3 py-1.5 rounded-full whitespace-nowrap transition-colors ${
                isCatActive
                  ? 'bg-[#8A9A86] text-white font-medium shadow-sm'
                  : 'bg-[#EFECE6] dark:bg-[#202221] text-[#726F68] dark:text-[#939188] hover:text-[#2C2B29] dark:hover:text-[#F3F4F1]'
              }`}
            >
              {cat.label}
            </button>
          );
        })}
      </div>

      {/* Expanded Granular Filters Tray */}
      {showFiltersModal && (
        <div className="p-4 rounded-xl bg-white dark:bg-[#1E201F] border border-[#E8E4DC] dark:border-[#2D302F] space-y-4 animate-in fade-in duration-200">
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            {/* Orientation Filter */}
            <div>
              <label className="text-xs font-semibold text-[#2C2B29] dark:text-[#F3F4F1] block mb-2">
                Image Orientation
              </label>
              <div className="flex gap-2">
                {[
                  { id: 'all', label: 'Any' },
                  { id: 'portrait', label: 'Portrait (Phone)' },
                  { id: 'landscape', label: 'Landscape' },
                  { id: 'square', label: 'Square' },
                ].map((opt) => (
                  <button
                    key={opt.id}
                    onClick={() => setFilters((prev) => ({ ...prev, orientation: opt.id as any }))}
                    className={`flex-1 py-1.5 px-2 rounded-lg text-xs font-medium border transition-colors ${
                      filters.orientation === opt.id
                        ? 'bg-[#2C2B29] text-white dark:bg-white dark:text-black border-transparent'
                        : 'border-[#E2DDD5] dark:border-[#333534] text-[#726F68] dark:text-[#939188] hover:bg-[#F8F6F0]'
                    }`}
                  >
                    {opt.label}
                  </button>
                ))}
              </div>
            </div>

            {/* License Filter */}
            <div>
              <label className="text-xs font-semibold text-[#2C2B29] dark:text-[#F3F4F1] block mb-2">
                Verified License Filter
              </label>
              <div className="flex gap-2">
                {[
                  { id: 'all', label: 'All Open Licenses' },
                  { id: 'cc0_pd', label: 'Public Domain / CC0' },
                  { id: 'commercial', label: 'Commercial Friendly' },
                ].map((opt) => (
                  <button
                    key={opt.id}
                    onClick={() => setFilters((prev) => ({ ...prev, licenseOnly: opt.id as any }))}
                    className={`flex-1 py-1.5 px-2 rounded-lg text-xs font-medium border transition-colors ${
                      filters.licenseOnly === opt.id
                        ? 'bg-[#2C2B29] text-white dark:bg-white dark:text-black border-transparent'
                        : 'border-[#E2DDD5] dark:border-[#333534] text-[#726F68] dark:text-[#939188] hover:bg-[#F8F6F0]'
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

      {/* Category banner header if category active */}
      {activeCategory && !filters.query && (
        <div className="p-4 rounded-xl bg-[#EFECE6]/50 dark:bg-[#1A1C1B]/50 border border-[#E8E4DC] dark:border-[#282A29] flex items-center justify-between">
          <div>
            <h3 className="font-serif text-lg font-semibold text-[#2C2B29] dark:text-[#F3F4F1]">
              {activeCategory.label}
            </h3>
            <p className="text-xs text-[#726F68] dark:text-[#939188]">
              {activeCategory.tagline}
            </p>
          </div>
          <button
            onClick={() => setFilters((prev) => ({ ...prev, category: '' }))}
            className="text-xs text-[#726F68] hover:text-[#2C2B29] underline"
          >
            Clear
          </button>
        </div>
      )}

      {/* Wallpapers Masonry Grid */}
      {loading ? (
        <div className="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-4 gap-4">
          {Array.from({ length: 8 }).map((_, idx) => (
            <div
              key={idx}
              className="rounded-xl overflow-hidden aspect-[9/16] bg-[#EFECE6] dark:bg-[#202221] animate-pulse"
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
              No Stillness Found
            </h3>
            <p className="text-xs text-[#726F68] dark:text-[#939188] max-w-sm mx-auto">
              We couldn't locate nature imagery matching "{filters.query || filters.category}". Try broadening your search or resetting filters.
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
            }}
            className="px-4 py-2 rounded-lg bg-[#2C2B29] text-white text-xs font-medium"
          >
            Reset Filters
          </button>
        </div>
      ) : (
        <div className="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-4 gap-3 sm:gap-4">
          {items.map((item) => {
            const favorited = isFavorited(item.id);
            return (
              <div
                key={item.id}
                onClick={() => onSelectWallpaper(item)}
                className="group relative rounded-xl overflow-hidden aspect-[9/16] bg-[#0D0F0E] border border-[#E8E4DC] dark:border-[#2A2C2B] shadow-sm hover:shadow-md transition-all cursor-pointer"
              >
                <img
                  src={item.thumbnailUrl}
                  alt={item.title}
                  className="w-full h-full object-cover transition-transform duration-700 group-hover:scale-105"
                  loading="lazy"
                />

                {/* Top Overlay Badges */}
                <div className="absolute top-2.5 left-2.5 right-2.5 flex items-center justify-between pointer-events-none z-10">
                  <span className="text-[10px] font-mono uppercase tracking-wider px-2 py-0.5 rounded bg-black/60 backdrop-blur-md text-white/90">
                    {item.provider}
                  </span>

                  <button
                    onClick={(e) => {
                      e.stopPropagation();
                      onToggleFavorite(item);
                    }}
                    aria-label="Toggle Favorite"
                    className="pointer-events-auto p-1.5 rounded-full bg-black/40 hover:bg-black/60 text-white backdrop-blur-md transition-colors"
                  >
                    <Heart className={`w-3.5 h-3.5 ${favorited ? 'fill-rose-500 text-rose-500' : ''}`} />
                  </button>
                </div>

                {/* Bottom Card Hover Overlay */}
                <div className="absolute inset-0 bg-gradient-to-t from-black/85 via-black/20 to-transparent opacity-0 group-hover:opacity-100 transition-opacity p-3 flex flex-col justify-end text-white">
                  <p className="font-serif text-xs font-medium line-clamp-1 leading-snug">
                    {item.title}
                  </p>
                  <p className="text-[11px] text-white/70 line-clamp-1 mt-0.5">
                    {item.license.author || 'Open Contributor'} · {item.license.code}
                  </p>

                  <div className="grid grid-cols-2 gap-1.5 mt-2.5">
                    <button
                      onClick={(e) => {
                        e.stopPropagation();
                        onSelectWallpaper(item);
                      }}
                      className="py-1.5 rounded bg-white/20 hover:bg-white/30 text-white text-[11px] font-medium flex items-center justify-center gap-1 backdrop-blur-sm"
                    >
                      <Eye className="w-3 h-3" />
                      <span>Details</span>
                    </button>
                    <button
                      onClick={(e) => {
                        e.stopPropagation();
                        onEditInStudio(item);
                      }}
                      className="py-1.5 rounded bg-white text-black text-[11px] font-semibold flex items-center justify-center gap-1 shadow-sm"
                    >
                      <Sparkles className="w-3 h-3 text-[#556952]" />
                      <span>Customize</span>
                    </button>
                  </div>
                </div>
              </div>
            );
          })}
        </div>
      )}
    </div>
  );
};
