import { MeshPresetId, MeshGradientSettings, MeshNode } from '../types';

export interface MeshPresetDefinition {
  id: MeshPresetId;
  name: string;
  japaneseName: string;
  subtitle: string;
  description: string;
  moodTag: string;
}

export const MESH_PRESETS: MeshPresetDefinition[] = [
  {
    id: 'biophilic',
    name: 'Biophilic Flora',
    japaneseName: '木漏れ日',
    subtitle: 'Moss, fern canopy & fertile earth',
    description: 'Blends your image primary tone with forest moss greens, damp bark, and soft leaf canopy highlights.',
    moodTag: 'Calming · Organic',
  },
  {
    id: 'dawn',
    name: 'Dawn Mist',
    japaneseName: '朝霧',
    subtitle: 'Morning dew & luminous horizon',
    description: 'Bathes your wallpaper in ethereal morning mist, gentle pearl highlights, and faint amber sunrise vapor.',
    moodTag: 'Fresh · Awakening',
  },
  {
    id: 'twilight',
    name: 'Alpine Twilight',
    japaneseName: '黄昏',
    subtitle: 'Dusk indigo & mountain ridge glow',
    description: 'Harmonizes with twilight shadows, evening violet, and a warm fading horizon dusk ember.',
    moodTag: 'Serene · Reflective',
  },
  {
    id: 'river',
    name: 'River Stone',
    japaneseName: '清流',
    subtitle: 'Wet slate, pebble & water ripples',
    description: 'Grounds your wallpaper with smooth river stone gray, calm stream tones, and mineral reflections.',
    moodTag: 'Steady · Centering',
  },
  {
    id: 'golden',
    name: 'Golden Hour',
    japaneseName: '秋麗',
    subtitle: 'Warm cedar & amber sunbeams',
    description: 'Infuses radiant afternoon sun warmth, honey amber, and sunset copper derived from your wallpaper.',
    moodTag: 'Inviting · Radiant',
  },
  {
    id: 'zen',
    name: 'Zen Monolith',
    japaneseName: '墨絵',
    subtitle: 'Deep sumi ink & muted jade',
    description: 'Deep minimalist contemplation with rich charcoal ink undertones and subtle weathered stone jade.',
    moodTag: 'Deep · Minimalist',
  },
];

// Color conversions
export function hexToRgb(hex: string): { r: number; g: number; b: number } {
  let clean = hex.replace('#', '').trim();
  if (clean.length === 3) {
    clean = clean.split('').map((c) => c + c).join('');
  }
  const num = parseInt(clean, 16) || 0;
  return {
    r: (num >> 16) & 255,
    g: (num >> 8) & 255,
    b: num & 255,
  };
}

export function rgbToHex(r: number, g: number, b: number): string {
  const toHex = (n: number) => {
    const clamped = Math.max(0, Math.min(255, Math.round(n)));
    const hex = clamped.toString(16);
    return hex.length === 1 ? '0' + hex : hex;
  };
  return `#${toHex(r)}${toHex(g)}${toHex(b)}`;
}

export function hexToHsl(hex: string): { h: number; s: number; l: number } {
  const { r, g, b } = hexToRgb(hex);
  const rNorm = r / 255;
  const gNorm = g / 255;
  const bNorm = b / 255;

  const max = Math.max(rNorm, gNorm, bNorm);
  const min = Math.min(rNorm, gNorm, bNorm);
  let h = 0;
  let s = 0;
  const l = (max + min) / 2;

  if (max !== min) {
    const d = max - min;
    s = l > 0.5 ? d / (2 - max - min) : d / (max + min);
    switch (max) {
      case rNorm:
        h = (gNorm - bNorm) / d + (gNorm < bNorm ? 6 : 0);
        break;
      case gNorm:
        h = (bNorm - rNorm) / d + 2;
        break;
      case bNorm:
        h = (rNorm - gNorm) / d + 4;
        break;
    }
    h = Math.round(h * 60);
  }

  return { h, s: Math.round(s * 100), l: Math.round(l * 100) };
}

export function hslToHex(h: number, s: number, l: number): string {
  const hNorm = ((h % 360) + 360) % 360;
  const sNorm = Math.max(0, Math.min(100, s)) / 100;
  const lNorm = Math.max(0, Math.min(100, l)) / 100;

  const c = (1 - Math.abs(2 * lNorm - 1)) * sNorm;
  const x = c * (1 - Math.abs(((hNorm / 60) % 2) - 1));
  const m = lNorm - c / 2;

  let r = 0;
  let g = 0;
  let b = 0;

  if (hNorm >= 0 && hNorm < 60) {
    r = c;
    g = x;
    b = 0;
  } else if (hNorm >= 60 && hNorm < 120) {
    r = x;
    g = c;
    b = 0;
  } else if (hNorm >= 120 && hNorm < 180) {
    r = 0;
    g = c;
    b = x;
  } else if (hNorm >= 180 && hNorm < 240) {
    r = 0;
    g = x;
    b = c;
  } else if (hNorm >= 240 && hNorm < 300) {
    r = x;
    g = 0;
    b = c;
  } else {
    r = c;
    g = 0;
    b = x;
  }

  return rgbToHex((r + m) * 255, (g + m) * 255, (b + m) * 255);
}

// Simple deterministic pseudo-random number generator for seed
function pseudoRandom(seed: number, index: number): number {
  const x = Math.sin(seed * 9973 + index * 1013) * 10000;
  return x - Math.floor(x);
}

/**
 * Generate 5 nature-inspired harmonious color stops based on the primary wallpaper color and preset
 */
export function generateMeshColors(primaryHex: string, preset: MeshPresetId): string[] {
  const { h, s, l } = hexToHsl(primaryHex);

  switch (preset) {
    case 'biophilic': {
      // Moss green, damp bark, fern highlight, forest mist, primary
      const mossGreen = hslToHex(115, Math.min(48, Math.max(25, s * 0.85)), Math.min(48, Math.max(30, l * 0.8)));
      const fernHighlight = hslToHex(88, Math.min(55, Math.max(30, s)), 72);
      const barkLoam = hslToHex(32, Math.min(40, s * 0.7), 24);
      const sageMist = hslToHex(135, 22, 68);
      return [primaryHex, mossGreen, fernHighlight, barkLoam, sageMist];
    }

    case 'dawn': {
      // Morning dew, pale amber horizon, soft sky pastel, airy mist
      const dawnSky = hslToHex((h + 20) % 360, Math.min(40, s * 0.6), 84);
      const dawnAmber = hslToHex(38, Math.min(65, s * 0.9), 78);
      const dewVapor = hslToHex(170, 18, 92);
      const horizonGlow = hslToHex(22, 50, 75);
      const softenedPrimary = hslToHex(h, Math.min(45, s * 0.7), Math.max(50, Math.min(75, l)));
      return [softenedPrimary, dawnSky, dawnAmber, dewVapor, horizonGlow];
    }

    case 'twilight': {
      // Evening indigo, dusk horizon copper, pine twilight, starry slate
      const eveningViolet = hslToHex(255, Math.min(50, s * 0.8), 28);
      const duskCopper = hslToHex(18, Math.min(65, s * 1.1), 48);
      const pineShadow = hslToHex(195, 25, 20);
      const alpineHaze = hslToHex(230, 30, 42);
      const deepenedPrimary = hslToHex(h, Math.min(55, s), Math.max(25, Math.min(45, l * 0.8)));
      return [deepenedPrimary, eveningViolet, duskCopper, pineShadow, alpineHaze];
    }

    case 'river': {
      // River slate, pebble ochre, cool stream water, stone shadow
      const riverSlate = hslToHex(210, 18, 42);
      const pebbleOchre = hslToHex(38, 28, 58);
      const waterRipple = hslToHex(190, 24, 76);
      const darkPebble = hslToHex(220, 15, 18);
      const groundedPrimary = hslToHex(h, Math.min(30, s * 0.6), Math.max(35, Math.min(55, l)));
      return [groundedPrimary, riverSlate, pebbleOchre, waterRipple, darkPebble];
    }

    case 'golden': {
      // Honey amber, warm cedar, sunset bronze, radiant core
      const honeyAmber = hslToHex(42, Math.min(85, Math.max(55, s * 1.2)), 58);
      const warmCedar = hslToHex(25, 60, 44);
      const sunsetBronze = hslToHex(12, 65, 48);
      const radiantCore = hslToHex(48, 80, 78);
      const warmedPrimary = hslToHex((h + 15) % 360, Math.min(70, s * 1.1), Math.max(45, l));
      return [warmedPrimary, honeyAmber, warmCedar, sunsetBronze, radiantCore];
    }

    case 'zen':
    default: {
      // Deep sumi ink, weathered jade, charcoal void, misted mineral
      const sumiCharcoal = hslToHex(h, 12, 14);
      const weatheredJade = hslToHex(155, 20, 32);
      const inkWash = hslToHex(210, 10, 24);
      const paleMineral = hslToHex(60, 8, 70);
      const mutedPrimary = hslToHex(h, Math.min(25, s * 0.5), Math.max(20, Math.min(45, l * 0.7)));
      return [mutedPrimary, sumiCharcoal, weatheredJade, inkWash, paleMineral];
    }
  }
}

/**
 * Generate 5 dynamic mesh focal nodes with organic positions modulated by the seed
 */
export function generateMeshNodes(
  primaryHex: string,
  preset: MeshPresetId,
  seed: number = 42
): MeshNode[] {
  const colors = generateMeshColors(primaryHex, preset);

  // Base natural positions (Corners & centers)
  const baseLayout = [
    { x: 18, y: 22, r: 85, opacity: 0.9 }, // Top-left anchor
    { x: 82, y: 18, r: 90, opacity: 0.85 }, // Top-right aura
    { x: 50, y: 55, r: 105, opacity: 0.75 }, // Center blend
    { x: 15, y: 82, r: 95, opacity: 0.9 }, // Bottom-left base
    { x: 85, y: 85, r: 80, opacity: 0.85 }, // Bottom-right anchor
  ];

  return baseLayout.map((layout, i) => {
    // Subtle organic perturbation from seed (-12% to +12%)
    const shiftX = (pseudoRandom(seed, i * 2) - 0.5) * 24;
    const shiftY = (pseudoRandom(seed, i * 2 + 1) - 0.5) * 24;
    const radiusShift = (pseudoRandom(seed, i + 10) - 0.5) * 20;

    const clampedX = Math.max(5, Math.min(95, Math.round(layout.x + shiftX)));
    const clampedY = Math.max(5, Math.min(95, Math.round(layout.y + shiftY)));
    const clampedR = Math.max(50, Math.min(130, Math.round(layout.r + radiusShift)));

    return {
      x: clampedX,
      y: clampedY,
      radius: clampedR,
      color: colors[i % colors.length],
      opacity: layout.opacity,
    };
  });
}

/**
 * Generate high-performance CSS radial-gradient mesh string for DOM previews and CSS styling
 */
export function generateMeshCssGradient(
  primaryHex: string,
  preset: MeshPresetId,
  seed: number = 42,
  intensity: number = 75
): string {
  const nodes = generateMeshNodes(primaryHex, preset, seed);
  const alphaFactor = (intensity / 100);

  const gradients = nodes.map((node) => {
    const { r, g, b } = hexToRgb(node.color);
    const stopAlpha = Math.min(1, Math.max(0, node.opacity * alphaFactor));
    return `radial-gradient(circle at ${node.x}% ${node.y}%, rgba(${r}, ${g}, ${b}, ${stopAlpha.toFixed(2)}) 0%, rgba(${r}, ${g}, ${b}, 0) ${node.radius}%)`;
  });

  // Base background under the mesh
  const baseColor = nodes[nodes.length - 1].color;
  return `${gradients.join(', ')}, ${baseColor}`;
}

/**
 * Render the subtle nature mesh gradient onto an HTML5 Canvas context
 */
export function drawMeshToCanvas(
  ctx: CanvasRenderingContext2D,
  width: number,
  height: number,
  settings: MeshGradientSettings,
  primaryHex: string
): void {
  if (!settings.enabled && settings.blendMode !== 'pure') return;

  const { preset, intensity, blur, blendMode, seed } = settings;
  const nodes = generateMeshNodes(primaryHex, preset, seed);
  const intensityAlpha = Math.max(0.05, Math.min(1, intensity / 100));

  ctx.save();

  // If pure mode, fill base color first
  if (blendMode === 'pure') {
    const baseRgb = hexToRgb(nodes[nodes.length - 1].color);
    ctx.fillStyle = `rgb(${baseRgb.r}, ${baseRgb.g}, ${baseRgb.b})`;
    ctx.fillRect(0, 0, width, height);
  } else if (blendMode === 'soft-light') {
    ctx.globalCompositeOperation = 'soft-light';
  } else if (blendMode === 'overlay') {
    ctx.globalCompositeOperation = 'overlay';
  } else if (blendMode === 'backdrop') {
    ctx.globalCompositeOperation = 'destination-over'; // Underneath
  }

  // Optional blur filter for organic diffusion
  if (blur > 0) {
    const scaledBlur = (blur / 35) * (width / 500) * 12;
    ctx.filter = `blur(${scaledBlur.toFixed(1)}px)`;
  }

  // Draw each radial gradient node
  for (const node of nodes) {
    const centerX = (node.x / 100) * width;
    const centerY = (node.y / 100) * height;
    const maxDimension = Math.max(width, height);
    const gradRadius = (node.radius / 100) * maxDimension * 0.75;

    const { r, g, b } = hexToRgb(node.color);
    const nodeAlpha = node.opacity * intensityAlpha;

    const grad = ctx.createRadialGradient(
      centerX,
      centerY,
      0,
      centerX,
      centerY,
      gradRadius
    );

    grad.addColorStop(0, `rgba(${r}, ${g}, ${b}, ${nodeAlpha.toFixed(3)})`);
    grad.addColorStop(0.5, `rgba(${r}, ${g}, ${b}, ${(nodeAlpha * 0.55).toFixed(3)})`);
    grad.addColorStop(1, `rgba(${r}, ${g}, ${b}, 0)`);

    ctx.fillStyle = grad;
    ctx.fillRect(0, 0, width, height);
  }

  ctx.restore();
}
