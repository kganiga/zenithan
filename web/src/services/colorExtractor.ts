import { ColorPalette } from '../types';

function rgbToHex(r: number, g: number, b: number): string {
  const toHex = (n: number) => {
    const hex = Math.max(0, Math.min(255, Math.round(n))).toString(16);
    return hex.length === 1 ? '0' + hex : hex;
  };
  return `#${toHex(r)}${toHex(g)}${toHex(b)}`;
}

function hexToRgb(hex: string): { r: number; g: number; b: number } {
  const clean = hex.replace('#', '');
  const num = parseInt(clean, 16);
  return {
    r: (num >> 16) & 255,
    g: (num >> 8) & 255,
    b: num & 255,
  };
}

export function extractPaletteFromImage(image: HTMLImageElement): ColorPalette {
  const canvas = document.createElement('canvas');
  const size = 64; // Downsample for fast sampling
  canvas.width = size;
  canvas.height = size;
  const ctx = canvas.getContext('2d');

  const defaultPalette: ColorPalette = {
    dominant: '#556952',
    secondary: '#8A9A86',
    muted: '#73716B',
    light: '#F8F6F0',
    dark: '#121413',
    swatches: ['#556952', '#8A9A86', '#73716B', '#F8F6F0', '#121413'],
  };

  if (!ctx) return defaultPalette;

  try {
    ctx.drawImage(image, 0, 0, size, size);
    const imgData = ctx.getImageData(0, 0, size, size).data;

    const colors: { r: number; g: number; b: number; count: number }[] = [];
    const colorMap = new Map<string, { r: number; g: number; b: number; count: number }>();

    for (let i = 0; i < imgData.length; i += 16) {
      // Step by 4 pixels (16 bytes)
      const r = imgData[i];
      const g = imgData[i + 1];
      const b = imgData[i + 2];
      const a = imgData[i + 3];

      if (a < 128) continue; // Skip translucent

      // Quantize to 24-step bins for clustering
      const qr = Math.round(r / 24) * 24;
      const qg = Math.round(g / 24) * 24;
      const qb = Math.round(b / 24) * 24;
      const key = `${qr},${qg},${qb}`;

      if (colorMap.has(key)) {
        colorMap.get(key)!.count++;
      } else {
        const item = { r: qr, g: qg, b: qb, count: 1 };
        colorMap.set(key, item);
        colors.push(item);
      }
    }

    colors.sort((a, b) => b.count - a.count);

    if (colors.length < 5) return defaultPalette;

    // Pick diverse swatches based on luminance and saturation
    const sortedByLuminance = [...colors].sort((a, b) => {
      const lumA = 0.299 * a.r + 0.587 * a.g + 0.114 * a.b;
      const lumB = 0.299 * b.r + 0.587 * b.g + 0.114 * b.b;
      return lumA - lumB;
    });

    const darkest = sortedByLuminance[0];
    const lightest = sortedByLuminance[sortedByLuminance.length - 1];
    const dominant = colors[0];
    const secondary = colors.length > 2 ? colors[2] : colors[1];
    const midIdx = Math.floor(sortedByLuminance.length / 2);
    const muted = sortedByLuminance[midIdx];

    const swatches = [
      rgbToHex(dominant.r, dominant.g, dominant.b),
      rgbToHex(secondary.r, secondary.g, secondary.b),
      rgbToHex(muted.r, muted.g, muted.b),
      rgbToHex(lightest.r, lightest.g, lightest.b),
      rgbToHex(darkest.r, darkest.g, darkest.b),
    ];

    return {
      dominant: swatches[0],
      secondary: swatches[1],
      muted: swatches[2],
      light: swatches[3],
      dark: swatches[4],
      swatches,
    };
  } catch (err) {
    console.warn('Canvas color extraction error (CORS or canvas tainted):', err);
    return defaultPalette;
  }
}
