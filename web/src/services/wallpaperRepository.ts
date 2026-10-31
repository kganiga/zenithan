import { WallpaperItem, FilterOptions, ProviderType } from '../types';
import { LOCAL_WALLPAPERS } from './localProvider';
import { searchWikimediaCommons } from './wikimediaProvider';
import { searchNasaImages } from './nasaProvider';
import { searchOpenverse } from './openverseProvider';
import { CURATED_CATEGORIES } from '../data/curatedData';

const CACHE_EXPIRY_MS = 1000 * 60 * 15; // 15 minutes cache
const PAGE_SIZE = 20;

interface CacheEntry {
  timestamp: number;
  items: WallpaperItem[];
}

const memoryCache = new Map<string, CacheEntry>();

/**
 * Fisher-Yates array shuffling algorithm for true random distribution
 */
function shuffleArray<T>(array: T[]): T[] {
  const copy = [...array];
  for (let i = copy.length - 1; i > 0; i--) {
    const j = Math.floor(Math.random() * (i + 1));
    [copy[i], copy[j]] = [copy[j], copy[i]];
  }
  return copy;
}

// Session-level shuffled list initialized on application boot / reopen
let sessionShuffledWallpapers: WallpaperItem[] = shuffleArray(LOCAL_WALLPAPERS);

/**
 * Force reshuffles the session collection (e.g. user clicks "Shuffle / Surprise Me" button)
 */
export function reshuffleSession(): WallpaperItem[] {
  sessionShuffledWallpapers = shuffleArray(LOCAL_WALLPAPERS);
  memoryCache.clear();
  return sessionShuffledWallpapers;
}

/**
 * Gets a fresh, dynamic featured wallpaper from the current session shuffle
 */
export function getFeaturedWallpaper(): WallpaperItem {
  if (sessionShuffledWallpapers.length === 0) {
    sessionShuffledWallpapers = shuffleArray(LOCAL_WALLPAPERS);
  }
  return sessionShuffledWallpapers[0] || LOCAL_WALLPAPERS[0];
}

export async function fetchWallpapers(
  filters: FilterOptions,
  page: number = 1
): Promise<{
  items: WallpaperItem[];
  providersQueried: ProviderType[];
  isCached: boolean;
}> {
  const queryTrimmed = filters.query.trim().toLowerCase();
  const categoryDef = CURATED_CATEGORIES.find((c) => c.id === filters.category);
  const effectiveQuery = queryTrimmed || (categoryDef ? categoryDef.query : '');

  const cacheKey = `${filters.provider}-${filters.category}-${queryTrimmed}-${filters.orientation}-${filters.licenseOnly}-p${page}`;
  const cached = memoryCache.get(cacheKey);

  if (cached && Date.now() - cached.timestamp < CACHE_EXPIRY_MS) {
    return {
      items: cached.items,
      providersQueried: [filters.provider === 'all' ? 'wikimedia' : filters.provider],
      isCached: true,
    };
  }

  const providersQueried: ProviderType[] = [];
  let fetchedItems: WallpaperItem[] = [];

  // When no explicit external provider or search query is forced, prioritize abundant instant local catalog
  const isDefaultFeed = !queryTrimmed && (!filters.provider || filters.provider === 'all' || filters.provider === 'local');

  if (isDefaultFeed) {
    providersQueried.push('local');
    let pool = sessionShuffledWallpapers;

    if (filters.category && filters.category !== 'all') {
      pool = pool.filter((item) => item.category === filters.category);
      // Fallback if specific category has fewer items
      if (pool.length < 5) {
        pool = [...pool, ...LOCAL_WALLPAPERS.filter((item) => item.category === filters.category)];
      }
    }

    // Paginate through the shuffled pool
    const startIndex = (page - 1) * PAGE_SIZE;
    const paged = pool.slice(startIndex, startIndex + PAGE_SIZE);

    // If we reached beyond the pool, wrap around or fetch from online providers for infinite stream
    if (paged.length > 0) {
      fetchedItems = paged;
    } else if (pool.length > 0) {
      // Loop with permutation to provide endless continuous scrolling like Zedge
      const loopedIndex = startIndex % pool.length;
      fetchedItems = pool.slice(loopedIndex, loopedIndex + PAGE_SIZE);
      if (fetchedItems.length < PAGE_SIZE) {
        fetchedItems = [...fetchedItems, ...pool.slice(0, PAGE_SIZE - fetchedItems.length)];
      }
    }
  } else {
    // Search or explicit provider query
    try {
      if (filters.provider === 'local') {
        fetchedItems = [...LOCAL_WALLPAPERS];
        providersQueried.push('local');
      } else if (filters.provider === 'wikimedia') {
        providersQueried.push('wikimedia');
        const wikiItems = await searchWikimediaCommons(effectiveQuery || 'minimal nature', filters.category || 'nature', 28, page);
        fetchedItems = wikiItems;
      } else if (filters.provider === 'nasa') {
        providersQueried.push('nasa');
        const nasaItems = await searchNasaImages(effectiveQuery || 'space nebula', filters.category || 'nature', 24, page);
        fetchedItems = nasaItems;
      } else if (filters.provider === 'openverse') {
        providersQueried.push('openverse');
        const openverseItems = await searchOpenverse(effectiveQuery || 'landscape nature', filters.category || 'nature', 24, page);
        fetchedItems = openverseItems;
      } else {
        // Multi-provider search federated
        providersQueried.push('wikimedia', 'nasa', 'openverse');

        // Check local matching first for instantaneous responsiveness
        const localMatches = LOCAL_WALLPAPERS.filter((i) => {
          const matchQuery = !queryTrimmed || i.title.toLowerCase().includes(queryTrimmed) || (i.description ? i.description.toLowerCase().includes(queryTrimmed) : false);
          const matchCat = !filters.category || i.category === filters.category;
          return matchQuery && matchCat;
        });

        const [wikiResults, nasaResults, openverseResults] = await Promise.allSettled([
          searchWikimediaCommons(effectiveQuery, filters.category || 'nature', 16, page),
          searchNasaImages(effectiveQuery, filters.category || 'nature', 12, page),
          searchOpenverse(effectiveQuery, filters.category || 'nature', 12, page),
        ]);

        const wikiItems = wikiResults.status === 'fulfilled' ? wikiResults.value : [];
        const nasaItems = nasaResults.status === 'fulfilled' ? nasaResults.value : [];
        const openverseItems = openverseResults.status === 'fulfilled' ? openverseResults.value : [];

        // Interleave local + online results for rich variety
        const maxLength = Math.max(localMatches.length, wikiItems.length, nasaItems.length, openverseItems.length);
        for (let i = 0; i < maxLength; i++) {
          if (localMatches[i]) fetchedItems.push(localMatches[i]);
          if (wikiItems[i]) fetchedItems.push(wikiItems[i]);
          if (nasaItems[i]) fetchedItems.push(nasaItems[i]);
          if (openverseItems[i]) fetchedItems.push(openverseItems[i]);
        }
      }
    } catch (err) {
      console.warn('Network provider query error, falling back to local library:', err);
    }
  }

  // Fallback to local catalog if results are empty
  if (fetchedItems.length === 0) {
    let pool = sessionShuffledWallpapers;
    if (queryTrimmed) {
      pool = pool.filter((i) => i.title.toLowerCase().includes(queryTrimmed) || (i.description ? i.description.toLowerCase().includes(queryTrimmed) : false));
    }
    if (filters.category) {
      pool = pool.filter((i) => i.category === filters.category);
    }
    fetchedItems = pool.length > 0 ? pool : sessionShuffledWallpapers.slice(0, PAGE_SIZE);
  }

  // Deduplicate items by ID or URL
  const seenUrls = new Set<string>();
  const deduplicated: WallpaperItem[] = [];
  for (const item of fetchedItems) {
    const key = item.thumbnailUrl || item.fullUrl || item.id;
    if (!seenUrls.has(key)) {
      seenUrls.add(key);
      deduplicated.push(item);
    }
  }

  // Filter by orientation
  let filtered = deduplicated;
  if (filters.orientation === 'portrait') {
    filtered = filtered.filter((i) => i.height >= i.width * 1.05);
  } else if (filters.orientation === 'landscape') {
    filtered = filtered.filter((i) => i.width >= i.height * 1.05);
  } else if (filters.orientation === 'square') {
    filtered = filtered.filter((i) => Math.abs(i.width - i.height) / Math.max(i.width, i.height) < 0.2);
  }

  // Filter by license
  if (filters.licenseOnly === 'cc0_pd') {
    filtered = filtered.filter((i) => !i.license.attributionRequired || i.license.code.includes('CC0') || i.license.code.includes('Public'));
  } else if (filters.licenseOnly === 'commercial') {
    filtered = filtered.filter((i) => i.license.commercialUseAllowed);
  }

  memoryCache.set(cacheKey, {
    timestamp: Date.now(),
    items: filtered,
  });

  return {
    items: filtered,
    providersQueried,
    isCached: false,
  };
}
