import { WallpaperItem, LicenseInfo } from '../types';

interface OpenverseImageResult {
  id: string;
  title?: string;
  creator?: string;
  creator_url?: string;
  url: string;
  thumbnail?: string;
  foreign_landing_url?: string;
  license?: string;
  license_version?: string;
  license_url?: string;
  width?: number;
  height?: number;
  attribution?: string;
}

interface OpenverseApiResponse {
  result_count?: number;
  page_count?: number;
  page_size?: number;
  results?: OpenverseImageResult[];
}

function parseOpenverseLicense(result: OpenverseImageResult): LicenseInfo {
  const lic = (result.license || '').toLowerCase();
  const version = result.license_version || '4.0';
  const isPD = lic === 'cc0' || lic === 'pdm';
  const isNC = lic.includes('nc');
  const isND = lic.includes('nd');

  const code = isPD ? (lic === 'cc0' ? 'CC0' : 'Public Domain') : `CC-${lic.toUpperCase()}-${version}`;

  return {
    code,
    name: code,
    url: result.license_url || (isPD ? 'https://creativecommons.org/publicdomain/zero/1.0/' : undefined),
    author: result.creator || 'Openverse Contributor',
    authorUrl: result.creator_url,
    attributionRequired: !isPD,
    commercialUseAllowed: !isNC,
    derivativeWorksAllowed: !isND,
    verified: true,
  };
}

export async function searchOpenverse(
  query: string,
  categoryTag: string = 'nature',
  pageSize: number = 20,
  page: number = 1
): Promise<WallpaperItem[]> {
  try {
    const controller = new AbortController();
    const timeoutId = setTimeout(() => controller.abort(), 7000);

    const endpoint = new URL('https://api.openverse.org/v1/images/');
    endpoint.search = new URLSearchParams({
      q: query,
      page_size: String(pageSize),
      page: String(page),
      license_type: 'all-cc',
    }).toString();

    const response = await fetch(endpoint.toString(), {
      signal: controller.signal,
      headers: {
        Accept: 'application/json',
      },
    });

    clearTimeout(timeoutId);

    if (!response.ok) {
      console.warn(`Openverse API returned status ${response.status}`);
      return [];
    }

    const data: OpenverseApiResponse = await response.json();
    const results: WallpaperItem[] = [];

    for (const item of data.results || []) {
      if (!item.url) continue;

      const width = item.width || 1200;
      const height = item.height || 1800;

      results.push({
        id: `openverse-${item.id}`,
        title: item.title || 'Nature Landscape',
        description: item.attribution || item.title,
        thumbnailUrl: item.thumbnail || item.url,
        fullUrl: item.url,
        provider: 'openverse',
        sourcePageUrl: item.foreign_landing_url || item.url,
        width,
        height,
        aspectRatio: width / height,
        category: categoryTag,
        license: parseOpenverseLicense(item),
      });
    }

    return results;
  } catch (error) {
    console.warn('Error fetching Openverse nature images:', error);
    return [];
  }
}
