import { WallpaperItem, EditorSettings, AspectRatioType } from '../types';
import { ASPECT_RATIOS } from '../data/curatedData';
import { drawMeshToCanvas } from './meshGradientGenerator';
import { extractPaletteFromImage } from './colorExtractor';

export interface RenderOptions {
  customWidth?: number;
  customHeight?: number;
  format?: 'image/jpeg' | 'image/png';
  quality?: number; // 0.95
}

// Helper to load an HTMLImageElement safely with crossOrigin
export function loadImage(src: string): Promise<HTMLImageElement> {
  return new Promise((resolve, reject) => {
    const img = new Image();
    img.crossOrigin = 'anonymous';
    img.onload = () => resolve(img);
    img.onerror = () => {
      // If anonymous fails due to CORS, retry without crossOrigin
      const fallbackImg = new Image();
      fallbackImg.onload = () => resolve(fallbackImg);
      fallbackImg.onerror = (err) => reject(err);
      fallbackImg.src = src;
    };
    img.src = src;
  });
}

export function getResolutionForRatio(
  ratio: AspectRatioType,
  highRes: boolean = true
): { width: number; height: number } {
  const match = ASPECT_RATIOS.find((r) => r.id === ratio);
  if (!match) return { width: 1080, height: 1920 };

  if (highRes) {
    if (ratio === '9:16') return { width: 1440, height: 2560 };
    if (ratio === '20:9') return { width: 1080, height: 2400 };
    if (ratio === '3:4') return { width: 1536, height: 2048 };
    if (ratio === '16:9') return { width: 2560, height: 1440 };
    if (ratio === '1:1') return { width: 1440, height: 1440 };
  }

  return { width: match.idealWidth, height: match.idealHeight };
}

export async function renderWallpaperToCanvas(
  wallpaper: WallpaperItem,
  settings: EditorSettings,
  canvas: HTMLCanvasElement,
  options?: RenderOptions
): Promise<void> {
  const { width: targetWidth, height: targetHeight } = options?.customWidth && options?.customHeight
    ? { width: options.customWidth, height: options.customHeight }
    : getResolutionForRatio(settings.cropRatio, true);

  canvas.width = targetWidth;
  canvas.height = targetHeight;

  const ctx = canvas.getContext('2d');
  if (!ctx) throw new Error('Canvas 2D context unavailable');

  ctx.imageSmoothingEnabled = true;
  ctx.imageSmoothingQuality = 'high';

  // Load image
  const img = await loadImage(wallpaper.fullUrl || wallpaper.thumbnailUrl);

  // Extract primary dominant color if not explicitly provided
  const primaryColor =
    settings.meshGradient?.customPrimaryColor ||
    extractPaletteFromImage(img).dominant;

  // Clear canvas
  ctx.fillStyle = '#121413';
  ctx.fillRect(0, 0, targetWidth, targetHeight);

  // 1. Pure Mesh Gradient Wallpaper Mode (standalone organic mesh background)
  if (settings.meshGradient?.enabled && settings.meshGradient.blendMode === 'pure') {
    drawMeshToCanvas(ctx, targetWidth, targetHeight, settings.meshGradient, primaryColor);
  } else {
    // 2. Backdrop Mode (draw mesh behind the wallpaper photo)
    if (settings.meshGradient?.enabled && settings.meshGradient.blendMode === 'backdrop') {
      drawMeshToCanvas(ctx, targetWidth, targetHeight, settings.meshGradient, primaryColor);
    }

    // Compute cover dimensions with zoom and pan
    const imgAspect = img.width / img.height;
    const targetAspect = targetWidth / targetHeight;

    let baseWidth: number;
    let baseHeight: number;

    if (imgAspect > targetAspect) {
      baseHeight = targetHeight;
      baseWidth = targetHeight * imgAspect;
    } else {
      baseWidth = targetWidth;
      baseHeight = targetWidth / imgAspect;
    }

    const zoomFactor = settings.zoom;
    const drawWidth = baseWidth * zoomFactor;
    const drawHeight = baseHeight * zoomFactor;

    // Pan offsets (-50% to 50%)
    const maxPanX = (drawWidth - targetWidth) / 2;
    const maxPanY = (drawHeight - targetHeight) / 2;

    const panOffsetX = (settings.panX / 50) * Math.max(0, maxPanX);
    const panOffsetY = (settings.panY / 50) * Math.max(0, maxPanY);

    const drawX = (targetWidth - drawWidth) / 2 + panOffsetX;
    const drawY = (targetHeight - drawHeight) / 2 + panOffsetY;

    // Apply basic visual adjustments (Brightness, Contrast, Saturation, Blur)
    const brightness = settings.brightness / 100;
    const contrast = settings.contrast / 100;
    const saturation = settings.saturation / 100;
    const blurPx = (settings.blur / 10) * (targetWidth / 800); // Scale blur to resolution

    ctx.save();
    ctx.filter = `brightness(${brightness}) contrast(${contrast}) saturate(${saturation}) blur(${blurPx}px)`;
    ctx.drawImage(img, drawX, drawY, drawWidth, drawHeight);
    ctx.restore();
  }

  // Warmth filter overlay
  if (settings.warmth !== 0) {
    ctx.save();
    if (settings.warmth > 0) {
      // Warm amber/gold tint
      ctx.fillStyle = `rgba(240, 160, 80, ${(Math.abs(settings.warmth) / 100) * 0.35})`;
      ctx.globalCompositeOperation = 'soft-light';
    } else {
      // Cool cyan/blue tint
      ctx.fillStyle = `rgba(80, 160, 240, ${(Math.abs(settings.warmth) / 100) * 0.35})`;
      ctx.globalCompositeOperation = 'soft-light';
    }
    ctx.fillRect(0, 0, targetWidth, targetHeight);
    ctx.restore();
  }

  // Zen Color Wash Presets
  if (settings.washPreset !== 'none' && settings.washIntensity > 0) {
    const alpha = (settings.washIntensity / 100) * 0.45;
    ctx.save();
    switch (settings.washPreset) {
      case 'sage':
        ctx.fillStyle = `rgba(138, 154, 134, ${alpha})`;
        ctx.globalCompositeOperation = 'color';
        ctx.fillRect(0, 0, targetWidth, targetHeight);
        break;
      case 'sumi':
        ctx.fillStyle = `rgba(40, 42, 40, ${alpha * 1.3})`;
        ctx.globalCompositeOperation = 'luminosity';
        ctx.fillRect(0, 0, targetWidth, targetHeight);
        break;
      case 'sand':
        ctx.fillStyle = `rgba(224, 214, 196, ${alpha})`;
        ctx.globalCompositeOperation = 'soft-light';
        ctx.fillRect(0, 0, targetWidth, targetHeight);
        break;
      case 'mist':
        ctx.fillStyle = `rgba(235, 240, 238, ${alpha})`;
        ctx.globalCompositeOperation = 'lighten';
        ctx.fillRect(0, 0, targetWidth, targetHeight);
        break;
      case 'twilight':
        ctx.fillStyle = `rgba(45, 55, 75, ${alpha})`;
        ctx.globalCompositeOperation = 'color-burn';
        ctx.fillRect(0, 0, targetWidth, targetHeight);
        break;
      case 'indigo':
        ctx.fillStyle = `rgba(28, 42, 60, ${alpha})`;
        ctx.globalCompositeOperation = 'multiply';
        ctx.fillRect(0, 0, targetWidth, targetHeight);
        break;
    }
    ctx.restore();
  }

  // Dynamic Nature Mesh Gradient Overlay (when blending over the photo)
  if (
    settings.meshGradient?.enabled &&
    (settings.meshGradient.blendMode === 'soft-light' ||
      settings.meshGradient.blendMode === 'overlay')
  ) {
    drawMeshToCanvas(ctx, targetWidth, targetHeight, settings.meshGradient, primaryColor);
  }

  // Vignette overlay
  if (settings.vignette > 0) {
    ctx.save();
    const radius = Math.max(targetWidth, targetHeight) * 0.75;
    const vignetteGrad = ctx.createRadialGradient(
      targetWidth / 2,
      targetHeight / 2,
      radius * 0.3,
      targetWidth / 2,
      targetHeight / 2,
      radius
    );
    const vignetteAlpha = (settings.vignette / 100) * 0.85;
    vignetteGrad.addColorStop(0, 'rgba(0,0,0,0)');
    vignetteGrad.addColorStop(1, `rgba(0,0,0,${vignetteAlpha})`);
    ctx.fillStyle = vignetteGrad;
    ctx.fillRect(0, 0, targetWidth, targetHeight);
    ctx.restore();
  }

  // Film Grain overlay
  if (settings.grain > 0) {
    ctx.save();
    const grainCanvas = document.createElement('canvas');
    grainCanvas.width = 128;
    grainCanvas.height = 128;
    const gCtx = grainCanvas.getContext('2d');
    if (gCtx) {
      const gData = gCtx.createImageData(128, 128);
      const intensity = (settings.grain / 100) * 45;
      for (let i = 0; i < gData.data.length; i += 4) {
        const val = (Math.random() - 0.5) * intensity;
        gData.data[i] = 128 + val;
        gData.data[i + 1] = 128 + val;
        gData.data[i + 2] = 128 + val;
        gData.data[i + 3] = (settings.grain / 100) * 50;
      }
      gCtx.putImageData(gData, 0, 0);
      const pattern = ctx.createPattern(grainCanvas, 'repeat');
      if (pattern) {
        ctx.globalCompositeOperation = 'overlay';
        ctx.fillStyle = pattern;
        ctx.fillRect(0, 0, targetWidth, targetHeight);
      }
    }
    ctx.restore();
  }

  // Atmospheric Filter 1: Komorebi Sunbeam Rays
  if (settings.komorebiRays && settings.komorebiRays > 0) {
    ctx.save();
    const rayAlpha = (settings.komorebiRays / 100) * 0.45;
    const originX = targetWidth * 0.15;
    const originY = -targetHeight * 0.1;
    const rayGrad = ctx.createRadialGradient(
      originX,
      originY,
      targetWidth * 0.1,
      targetWidth * 0.6,
      targetHeight * 0.8,
      targetWidth * 1.5
    );
    rayGrad.addColorStop(0, `rgba(255, 245, 215, ${rayAlpha * 1.2})`);
    rayGrad.addColorStop(0.35, `rgba(255, 235, 195, ${rayAlpha * 0.7})`);
    rayGrad.addColorStop(0.7, `rgba(255, 240, 220, ${rayAlpha * 0.25})`);
    rayGrad.addColorStop(1, 'rgba(255, 255, 255, 0)');

    ctx.globalCompositeOperation = 'screen';
    ctx.fillStyle = rayGrad;
    ctx.fillRect(0, 0, targetWidth, targetHeight);
    ctx.restore();
  }

  // Atmospheric Filter 2: Mist Gradient (Negative space for phone icons & widgets)
  if (settings.mistGradient && settings.mistGradient !== 'none' && settings.mistIntensity > 0) {
    ctx.save();
    const mistAlpha = (settings.mistIntensity / 100) * 0.75;

    if (settings.mistGradient === 'bottom') {
      const mistGrad = ctx.createLinearGradient(0, targetHeight * 0.45, 0, targetHeight);
      mistGrad.addColorStop(0, 'rgba(240, 245, 242, 0)');
      mistGrad.addColorStop(0.4, `rgba(240, 245, 242, ${mistAlpha * 0.4})`);
      mistGrad.addColorStop(1, `rgba(235, 242, 238, ${mistAlpha})`);
      ctx.fillStyle = mistGrad;
      ctx.fillRect(0, 0, targetWidth, targetHeight);
    } else if (settings.mistGradient === 'top') {
      const mistGrad = ctx.createLinearGradient(0, targetHeight * 0.55, 0, 0);
      mistGrad.addColorStop(0, 'rgba(240, 245, 242, 0)');
      mistGrad.addColorStop(0.4, `rgba(240, 245, 242, ${mistAlpha * 0.4})`);
      mistGrad.addColorStop(1, `rgba(235, 242, 238, ${mistAlpha})`);
      ctx.fillStyle = mistGrad;
      ctx.fillRect(0, 0, targetWidth, targetHeight);
    }
    ctx.restore();
  }

  // Atmospheric Filter 3: Japanese Washi Paper Texture Overlay
  if (settings.washiTexture && settings.washiTexture > 0) {
    ctx.save();
    const washiCanvas = document.createElement('canvas');
    washiCanvas.width = 256;
    washiCanvas.height = 256;
    const wCtx = washiCanvas.getContext('2d');
    if (wCtx) {
      wCtx.fillStyle = '#F8F6F0';
      wCtx.fillRect(0, 0, 256, 256);

      // Draw subtle random washi fiber strands
      wCtx.strokeStyle = 'rgba(160, 150, 135, 0.25)';
      wCtx.lineWidth = 0.5;
      for (let f = 0; f < 35; f++) {
        wCtx.beginPath();
        const startX = Math.random() * 256;
        const startY = Math.random() * 256;
        wCtx.moveTo(startX, startY);
        wCtx.bezierCurveTo(
          startX + (Math.random() - 0.5) * 40,
          startY + (Math.random() - 0.5) * 40,
          startX + (Math.random() - 0.5) * 60,
          startY + (Math.random() - 0.5) * 60,
          startX + (Math.random() - 0.5) * 80,
          startY + (Math.random() - 0.5) * 80
        );
        wCtx.stroke();
      }

      const pattern = ctx.createPattern(washiCanvas, 'repeat');
      if (pattern) {
        ctx.globalCompositeOperation = 'soft-light';
        ctx.globalAlpha = (settings.washiTexture / 100) * 0.55;
        ctx.fillStyle = pattern;
        ctx.fillRect(0, 0, targetWidth, targetHeight);
      }
    }
    ctx.restore();
  }

  // Minimalist Typography & Text Overlay
  if (settings.textOverlay.enabled && (settings.textOverlay.text || settings.textOverlay.kanji)) {
    ctx.save();

    const scale = targetWidth / 1080;
    const { text, subtext, kanji, fontStyle, position, size, color, opacity, showJapaneseMark } =
      settings.textOverlay;

    ctx.globalAlpha = opacity;
    ctx.fillStyle = color || '#FFFFFF';
    ctx.textAlign = 'center';
    ctx.textBaseline = 'middle';

    // Drop shadow for legibility over both dark and bright backgrounds
    ctx.shadowColor = 'rgba(0, 0, 0, 0.45)';
    ctx.shadowBlur = 12 * scale;
    ctx.shadowOffsetX = 0;
    ctx.shadowOffsetY = 2 * scale;

    // Font size sizing
    let baseFontSize = 28 * scale;
    if (size === 'sm') baseFontSize = 20 * scale;
    if (size === 'md') baseFontSize = 28 * scale;
    if (size === 'lg') baseFontSize = 38 * scale;
    if (size === 'xl') baseFontSize = 52 * scale;

    const fontFamily = fontStyle === 'serif' ? 'Playfair Display, Georgia, serif' : 'Plus Jakarta Sans, sans-serif';

    // Vertical Position
    let centerY = targetHeight * 0.5;
    if (position === 'top') {
      // Below phone clock safe area
      centerY = targetHeight * 0.28;
    } else if (position === 'bottom') {
      centerY = targetHeight * 0.78;
    }

    // Draw Japanese Kanji motif if enabled
    if (showJapaneseMark && kanji) {
      ctx.font = `300 ${baseFontSize * 1.6}px ${fontFamily}`;
      ctx.fillText(kanji, targetWidth / 2, centerY - baseFontSize * 1.4);
    }

    // Draw main text (supports multiline haiku)
    ctx.font = `400 ${baseFontSize}px ${fontFamily}`;
    const lines = text.split('\n');
    const lineHeight = baseFontSize * 1.45;
    const startY = centerY - ((lines.length - 1) * lineHeight) / 2;

    lines.forEach((line, index) => {
      ctx.fillText(line.trim(), targetWidth / 2, startY + index * lineHeight);
    });

    // Draw subtext
    if (subtext) {
      const subFontSize = baseFontSize * 0.55;
      ctx.font = `300 ${subFontSize}px Plus Jakarta Sans, sans-serif`;
      ctx.shadowBlur = 8 * scale;
      const subY = startY + lines.length * lineHeight + 4 * scale;
      ctx.fillText(subtext.toUpperCase(), targetWidth / 2, subY);
    }

    ctx.restore();
  }
}

export function generateAttributionText(wallpaper: WallpaperItem): string {
  const timestamp = new Date().toISOString().split('T')[0];
  return `ZEN NATURE — WALLPAPER SOURCE & ATTRIBUTION
Generated: ${timestamp}
-----------------------------------------------------------
Image Title: ${wallpaper.title}
Creator / Author: ${wallpaper.license.author || 'Unknown / Public Archives'}
${wallpaper.license.authorUrl ? `Author Profile: ${wallpaper.license.authorUrl}\n` : ''}Source Provider: ${wallpaper.provider.toUpperCase()}
Original Archive Page: ${wallpaper.sourcePageUrl}

LICENSE & USAGE TERMS:
-----------------------------------------------------------
License: ${wallpaper.license.name} (${wallpaper.license.code})
License URL: ${wallpaper.license.url || 'Not provided'}
Attribution Required: ${wallpaper.license.attributionRequired ? 'YES (Attribution included in this file)' : 'NO (Public Domain / CC0)'}
Commercial Use: ${wallpaper.license.commercialUseAllowed ? 'Allowed' : 'Restricted / Non-Commercial'}
Derivative Works: ${wallpaper.license.derivativeWorksAllowed ? 'Allowed' : 'Restricted'}
Verified License Status: ${wallpaper.license.verified ? 'Verified via Official API' : 'Unverified provisional metadata'}

-----------------------------------------------------------
Exported from Zen Nature — Minimalist Wallpaper Studio.
Open-source nature wallpaper builder honoring Creative Commons and Public Domain imagery.`;
}

export function downloadFile(blob: Blob, filename: string): void {
  const url = URL.createObjectURL(blob);
  const a = document.createElement('a');
  a.href = url;
  a.download = filename;
  document.body.appendChild(a);
  a.click();
  document.body.removeChild(a);
  setTimeout(() => URL.revokeObjectURL(url), 1000);
}

export function downloadAttribution(wallpaper: WallpaperItem): void {
  const text = generateAttributionText(wallpaper);
  const blob = new Blob([text], { type: 'text/plain;charset=utf-8' });
  downloadFile(blob, `zenithan_studio_${wallpaper.id}_attribution.txt`);
}

export async function downloadWallpaperDirect(
  wallpaper: WallpaperItem,
  mode: 'phone' | 'original' = 'phone'
): Promise<{ success: boolean; filename: string }> {
  const sanitizedTitle = wallpaper.title
    .toLowerCase()
    .replace(/[^a-z0-9]/g, '-')
    .replace(/-+/g, '-')
    .slice(0, 32);

  const filename =
    mode === 'phone'
      ? `zenithan-studio-${sanitizedTitle}-1440x2560.jpg`
      : `zenithan-studio-${sanitizedTitle}-fullres.jpg`;

  const imageUrl = wallpaper.fullUrl || wallpaper.thumbnailUrl;

  try {
    if (mode === 'phone') {
      const canvas = document.createElement('canvas');
      canvas.width = 1440;
      canvas.height = 2560;
      const ctx = canvas.getContext('2d');
      if (ctx) {
        ctx.imageSmoothingEnabled = true;
        ctx.imageSmoothingQuality = 'high';
        const img = await loadImage(imageUrl);

        // Aspect fill into 1440x2560
        const imgAspect = img.width / img.height;
        const targetAspect = 1440 / 2560;
        let drawW: number;
        let drawH: number;
        if (imgAspect > targetAspect) {
          drawH = 2560;
          drawW = 2560 * imgAspect;
        } else {
          drawW = 1440;
          drawH = 1440 / imgAspect;
        }
        const drawX = (1440 - drawW) / 2;
        const drawY = (2560 - drawH) / 2;

        ctx.drawImage(img, drawX, drawY, drawW, drawH);

        const blob = await new Promise<Blob | null>((res) =>
          canvas.toBlob((b) => res(b), 'image/jpeg', 0.96)
        );
        if (blob) {
          downloadFile(blob, filename);
          return { success: true, filename };
        }
      }
    }

    // Direct blob download fallback or original mode
    const response = await fetch(imageUrl, { mode: 'cors' });
    if (response.ok) {
      const blob = await response.blob();
      downloadFile(blob, filename);
      return { success: true, filename };
    }
  } catch (err) {
    console.warn('Direct canvas/fetch export had CORS limitation, using fallback anchor:', err);
  }

  // Anchor fallback
  const link = document.createElement('a');
  link.href = imageUrl;
  link.download = filename;
  link.target = '_blank';
  link.rel = 'noopener noreferrer';
  document.body.appendChild(link);
  link.click();
  document.body.removeChild(link);
  return { success: true, filename };
}

export async function shareWallpaperDirect(wallpaper: WallpaperItem): Promise<boolean> {
  const imageUrl = wallpaper.fullUrl || wallpaper.thumbnailUrl;
  if (!navigator.share) return false;

  try {
    const res = await fetch(imageUrl, { mode: 'cors' });
    if (res.ok) {
      const blob = await res.blob();
      const file = new File(
        [blob],
        `zenithan-studio-${wallpaper.title.toLowerCase().replace(/[^a-z0-9]/g, '-')}.jpg`,
        { type: blob.type || 'image/jpeg' }
      );
      if (navigator.canShare && navigator.canShare({ files: [file] })) {
        await navigator.share({
          files: [file],
          title: `${wallpaper.title} — Zenithan Studio`,
          text: `Apply "${wallpaper.title}" as your wallpaper.`,
        });
        return true;
      }
    }

    await navigator.share({
      title: `${wallpaper.title} — Zenithan Studio`,
      text: `Apply "${wallpaper.title}" as your wallpaper.`,
      url: window.location.href,
    });
    return true;
  } catch (err) {
    if ((err as Error)?.name !== 'AbortError') {
      console.warn('Share error:', err);
    }
    return false;
  }
}

