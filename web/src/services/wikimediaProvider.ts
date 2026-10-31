import { WallpaperItem, LicenseInfo } from '../types';

interface WikimediaImageInfo {
  url?: string;
  thumburl?: string;
  width?: number;
  height?: number;
  mime?: string;
  descriptionurl?: string;
  extmetadata?: {
    ObjectName?: { value?: string };
    ImageDescription?: { value?: string };
    Artist?: { value?: string };
    LicenseShortName?: { value?: string };
    UsageTerms?: { value?: string };
    LicenseUrl?: { value?: string };
    DateTimeOriginal?: { value?: string };
    Categories?: { value?: string };
  };
}

interface WikimediaPage {
  pageid: number;
  title: string;
  imageinfo?: WikimediaImageInfo[];
}

interface WikimediaApiResponse {
  query?: {
    pages?: Record<string, WikimediaPage>;
  };
}

function cleanHtml(input?: string): string {
  if (!input) return '';
  return input
    .replace(/<[^>]*>/g, '')
    .replace(/&quot;/g, '"')
    .replace(/&amp;/g, '&')
    .replace(/&#039;/g, "'")
    .replace(/&lt;/g, '<')
    .replace(/&gt;/g, '>')
    .trim();
}

function parseLicense(meta?: WikimediaImageInfo['extmetadata']): LicenseInfo {
  const shortName = meta?.LicenseShortName?.value?.trim() || meta?.UsageTerms?.value?.trim() || 'Unknown';
  const licenseUrl = meta?.LicenseUrl?.value;
  const artist = cleanHtml(meta?.Artist?.value) || 'Wikimedia Commons Contributor';

  const lower = shortName.toLowerCase();
  const isPD =
    lower.includes('public domain') ||
    lower.includes('cc0') ||
    lower.includes('no rights reserved') ||
    lower.includes('pd');
  const isCCBY = lower.includes('cc by') || lower.includes('creative commons attribution');
  const isCCBYSA = lower.includes('cc by-sa') || lower.includes('sharealike');
  const isNC = lower.includes('nc') || lower.includes('noncommercial');
  const isND = lower.includes('nd') || lower.includes('noderivs');

  const verified = isPD || isCCBY || isCCBYSA || shortName !== 'Unknown';

  return {
    code: shortName,
    name: shortName,
    url: licenseUrl || (isPD ? 'https://creativecommons.org/publicdomain/zero/1.0/' : undefined),
    author: artist,
    attributionRequired: !isPD,
    commercialUseAllowed: !isNC,
    derivativeWorksAllowed: !isND,
    verified,
    rawLicenseString: shortName,
  };
}

export async function searchWikimediaCommons(
  query: string,
  categoryTag: string = 'nature',
  limit: number = 24,
  page: number = 1
): Promise<WallpaperItem[]> {
  try {
    const controller = new AbortController();
    const timeoutId = setTimeout(() => controller.abort(), 8000);

    // Enhanced query prioritizing peaceful nature photography, excluding SVGs, logos, diagrams
    const enhancedQuery = `${query} filetype:bitmap -logo -flag -map -diagram -icon`;
    const offset = Math.max(0, (page - 1) * limit);
    const endpoint = new URL('https://commons.wikimedia.org/w/api.php');
    endpoint.search = new URLSearchParams({
      origin: '*',
      action: 'query',
      generator: 'search',
      gsrnamespace: '6', // File namespace
      gsrlimit: String(limit),
      gsroffset: String(offset),
      gsrsearch: enhancedQuery,
      prop: 'imageinfo',
      iiprop: 'url|size|mime|extmetadata',
      iiurlwidth: '800', // Request high quality thumbnail width
      format: 'json',
    }).toString();

    const response = await fetch(endpoint.toString(), {
      signal: controller.signal,
      headers: {
        Accept: 'application/json',
      },
    });

    clearTimeout(timeoutId);

    if (!response.ok) {
      console.warn(`Wikimedia API responded with status ${response.status}`);
      return [];
    }

    const data: WikimediaApiResponse = await response.json();
    if (!data.query?.pages) return [];

    const items: WallpaperItem[] = [];

    for (const pageId in data.query.pages) {
      const page = data.query.pages[pageId];
      const info = page.imageinfo?.[0];
      if (!info || !info.url) continue;

      // Filter out non-photo image types or tiny badges
      const mime = info.mime?.toLowerCase() || '';
      if (!mime.includes('jpeg') && !mime.includes('jpg') && !mime.includes('png') && !mime.includes('webp')) {
        continue;
      }

      const width = info.width || 1200;
      const height = info.height || 1800;

      // Filter out micro icons (< 600px width and height)
      if (width < 600 && height < 600) continue;

      const rawTitle = page.title.replace(/^File:/i, '').replace(/\.[^/.]+$/, '');
      const cleanTitle = cleanHtml(info.extmetadata?.ObjectName?.value) || rawTitle.replace(/_/g, ' ');
      const description = cleanHtml(info.extmetadata?.ImageDescription?.value) || cleanTitle;
      const license = parseLicense(info.extmetadata);

      items.push({
        id: `wiki-${page.pageid}`,
        title: cleanTitle.length > 60 ? cleanTitle.slice(0, 57) + '...' : cleanTitle,
        description,
        thumbnailUrl: info.thumburl || info.url,
        fullUrl: info.url,
        provider: 'wikimedia',
        sourcePageUrl: info.descriptionurl || `https://commons.wikimedia.org/wiki/${encodeURIComponent(page.title)}`,
        width,
        height,
        aspectRatio: width / height,
        category: categoryTag,
        license,
        dateCreated: info.extmetadata?.DateTimeOriginal?.value,
      });
    }

    return items;
  } catch (error) {
    console.warn('Error fetching Wikimedia Commons nature images:', error);
    return [];
  }
}
