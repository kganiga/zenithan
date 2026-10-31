import { WallpaperItem, LicenseInfo } from '../types';

interface NasaItem {
  data?: Array<{
    nasa_id: string;
    title: string;
    description?: string;
    center?: string;
    date_created?: string;
    photographer?: string;
    secondary_creator?: string;
  }>;
  links?: Array<{
    href: string;
    rel?: string;
    render?: string;
  }>;
}

interface NasaApiResponse {
  collection?: {
    items?: NasaItem[];
  };
}

export async function searchNasaImages(
  query: string,
  categoryTag: string = 'nature',
  limit: number = 20,
  page: number = 1
): Promise<WallpaperItem[]> {
  try {
    const controller = new AbortController();
    const timeoutId = setTimeout(() => controller.abort(), 8000);

    const endpoint = new URL('https://images.nasa.gov/search');
    endpoint.search = new URLSearchParams({
      q: query,
      media_type: 'image',
      page: String(page),
    }).toString();

    const response = await fetch(endpoint.toString(), {
      signal: controller.signal,
      headers: {
        Accept: 'application/json',
      },
    });

    clearTimeout(timeoutId);

    if (!response.ok) {
      console.warn(`NASA API responded with status ${response.status}`);
      return [];
    }

    const data: NasaApiResponse = await response.json();
    const items = data.collection?.items || [];
    const results: WallpaperItem[] = [];

    const defaultNasaLicense: LicenseInfo = {
      code: 'NASA Public Domain',
      name: 'NASA Media Usage Guidelines (Public Domain)',
      url: 'https://www.nasa.gov/multimedia/guidelines/index.html',
      author: 'NASA',
      attributionRequired: false,
      commercialUseAllowed: true,
      derivativeWorksAllowed: true,
      verified: true,
    };

    for (const item of items.slice(0, limit)) {
      const datum = item.data?.[0];
      const link = item.links?.[0];
      if (!datum || !link?.href) continue;

      const thumbUrl = link.href;
      // NASA asset convention: replace ~thumb.jpg with ~medium.jpg or ~orig.jpg for high resolution
      const fullUrl = thumbUrl.replace(/~thumb\.jpg$/i, '~orig.jpg').replace(/~thumb\.png$/i, '~orig.png');

      const author = datum.photographer || datum.secondary_creator || (datum.center ? `NASA / ${datum.center}` : 'NASA');

      results.push({
        id: `nasa-${datum.nasa_id}`,
        title: datum.title || 'Earth and Space Nature',
        description: datum.description || datum.title,
        thumbnailUrl: thumbUrl,
        fullUrl: fullUrl || thumbUrl,
        provider: 'nasa',
        sourcePageUrl: `https://images.nasa.gov/details-${datum.nasa_id}`,
        width: 1920,
        height: 1080,
        aspectRatio: 16 / 9,
        category: categoryTag,
        license: {
          ...defaultNasaLicense,
          author,
        },
        dateCreated: datum.date_created,
      });
    }

    return results;
  } catch (error) {
    console.warn('Error fetching NASA nature images:', error);
    return [];
  }
}
