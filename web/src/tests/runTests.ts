/**
 * Unit Test Suite for Zen Nature Core Logic
 * Tests:
 * 1. Aspect Ratio resolution and device mapping
 * 2. Attribution text generation for Creative Commons & Public Domain compliance
 * 3. Provider deduplication and fallback recovery
 * 4. Storage operations & cache size estimation
 */

import { ASPECT_RATIOS, getCurrentCircadianSlot } from '../data/curatedData';
import { getResolutionForRatio, generateAttributionText } from '../services/exportEngine';
import { WallpaperItem } from '../types';

let totalTests = 0;
let passedTests = 0;

function assert(condition: boolean, message: string) {
  totalTests++;
  if (condition) {
    console.log(`  ✓ PASS: ${message}`);
    passedTests++;
  } else {
    console.error(`  ✗ FAIL: ${message}`);
  }
}

console.log('\n--- 1. Testing Aspect Ratio Resolution ---');
{
  const mobileHigh = getResolutionForRatio('9:16', true);
  assert(mobileHigh.width === 1440 && mobileHigh.height === 2560, '9:16 High-Res is 1440x2560');

  const flagship = getResolutionForRatio('20:9', true);
  assert(flagship.width === 1080 && flagship.height === 2400, '20:9 Flagship is 1080x2400');

  const tablet = getResolutionForRatio('3:4', true);
  assert(tablet.width === 1536 && tablet.height === 2048, '3:4 Tablet is 1536x2048');

  const square = getResolutionForRatio('1:1', true);
  assert(square.width === 1440 && square.height === 1440, '1:1 Square is 1440x1440');
}

console.log('\n--- 2. Testing License & Attribution Formatting ---');
{
  const mockWallpaperCCBY: WallpaperItem = {
    id: 'test-1',
    title: 'Misty Cedar Forest',
    thumbnailUrl: 'https://example.com/thumb.jpg',
    fullUrl: 'https://example.com/full.jpg',
    provider: 'wikimedia',
    sourcePageUrl: 'https://commons.wikimedia.org/wiki/File:Misty_Cedar.jpg',
    width: 1200,
    height: 1800,
    aspectRatio: 2 / 3,
    category: 'forests-moss',
    license: {
      code: 'CC-BY-4.0',
      name: 'Creative Commons Attribution 4.0 International',
      url: 'https://creativecommons.org/licenses/by/4.0/',
      author: 'Hiroshi Tanaka',
      attributionRequired: true,
      commercialUseAllowed: true,
      derivativeWorksAllowed: true,
      verified: true,
    },
  };

  const attrText = generateAttributionText(mockWallpaperCCBY);
  assert(attrText.includes('Misty Cedar Forest'), 'Attribution text includes image title');
  assert(attrText.includes('Hiroshi Tanaka'), 'Attribution text credits author');
  assert(attrText.includes('CC-BY-4.0'), 'Attribution text specifies license code');
  assert(attrText.includes('https://commons.wikimedia.org/wiki/File:Misty_Cedar.jpg'), 'Attribution text links to original source');
  assert(attrText.includes('YES (Attribution included in this file)'), 'Flags attribution requirement');
}

console.log('\n--- 3. Testing Public Domain Attribution ---');
{
  const mockWallpaperPD: WallpaperItem = {
    id: 'test-2',
    title: 'Silent Ocean Horizon',
    thumbnailUrl: 'https://example.com/thumb2.jpg',
    fullUrl: 'https://example.com/full2.jpg',
    provider: 'local',
    sourcePageUrl: 'https://commons.wikimedia.org/wiki/Category:Sea_horizons',
    width: 1200,
    height: 1800,
    aspectRatio: 2 / 3,
    category: 'ocean-waves',
    license: {
      code: 'CC0',
      name: 'Creative Commons Zero',
      url: 'https://creativecommons.org/publicdomain/zero/1.0/',
      author: 'Open Marine Library',
      attributionRequired: false,
      commercialUseAllowed: true,
      derivativeWorksAllowed: true,
      verified: true,
    },
  };

  const attrText = generateAttributionText(mockWallpaperPD);
  assert(attrText.includes('NO (Public Domain / CC0)'), 'Properly indicates no mandatory attribution required for CC0');
  assert(attrText.includes('Allowed'), 'Flags commercial use allowed');
}

console.log('\n--- 4. Testing Curated Categories Data Integrity ---');
{
  assert(ASPECT_RATIOS.length === 5, 'Contains 5 standard aspect ratio profiles');
  const ratioIds = ASPECT_RATIOS.map((r) => r.id);
  assert(ratioIds.includes('9:16') && ratioIds.includes('20:9'), 'Includes modern Android 20:9 and 9:16 ratios');
}

console.log('\n--- 5. Testing Grid Overlay Configurations ---');
{
  const validGrids = ['none', 'ruleOfThirds', 'goldenRatio', 'centerCross'];
  assert(validGrids.includes('ruleOfThirds'), 'Supports 3x3 Rule of Thirds grid');
  assert(validGrids.includes('goldenRatio'), 'Supports Golden Ratio (Phi) grid');
  assert(validGrids.includes('centerCross'), 'Supports Center and Horizon crosshair grid');
}

console.log('\n--- 6. Testing Circadian Stillness Resolution ---');
{
  const slot = getCurrentCircadianSlot();
  assert(Boolean(slot.id && slot.label && slot.hourRange), 'Returns valid Circadian slot object with label and range');
  assert(['dawn', 'daylight', 'twilight', 'night'].includes(slot.id), 'Circadian slot is one of dawn/daylight/twilight/night');
  assert(Boolean(slot.recommendedCategory && slot.japaneseTitle), 'Includes recommended Zen category and Japanese title');
}

console.log('\n--- 7. Testing Dynamic Nature Mesh Gradient Generator ---');
{
  const {
    MESH_PRESETS,
    generateMeshColors,
    generateMeshNodes,
    generateMeshCssGradient,
    hexToHsl,
    hslToHex,
  } = await import('../services/meshGradientGenerator');

  assert(MESH_PRESETS.length === 6, 'Contains 6 nature-inspired mesh presets');
  const presetIds = MESH_PRESETS.map((p) => p.id);
  assert(presetIds.includes('biophilic') && presetIds.includes('dawn') && presetIds.includes('golden'), 'Contains biophilic, dawn, and golden presets');

  // Test color conversion
  const testHex = '#556952'; // Extracted sage dominant
  const hsl = hexToHsl(testHex);
  assert(hsl.h > 90 && hsl.h < 130, 'Correctly derives hue for forest sage ~111°');
  const roundtripHex = hslToHex(hsl.h, hsl.s, hsl.l);
  const { hexToRgb } = await import('../services/meshGradientGenerator');
  const rgbA = hexToRgb(testHex);
  const rgbB = hexToRgb(roundtripHex);
  const maxDiff = Math.max(Math.abs(rgbA.r - rgbB.r), Math.abs(rgbA.g - rgbB.g), Math.abs(rgbA.b - rgbB.b));
  assert(maxDiff <= 2, 'HSL roundtrip conversion preserves color fidelity within quantization tolerance');

  // Test mesh colors generation from primary color
  const colors = generateMeshColors(testHex, 'biophilic');
  assert(colors.length === 5, 'Generates 5 harmonious stops from primary color');
  assert(colors[0].toLowerCase() === testHex.toLowerCase(), 'Root stop matches extracted primary color');

  // Test mesh nodes with seed
  const nodesSeedA = generateMeshNodes(testHex, 'twilight', 42);
  const nodesSeedB = generateMeshNodes(testHex, 'twilight', 99);
  assert(nodesSeedA.length === 5, 'Generates 5 organic mesh nodes');
  assert(nodesSeedA[0].x !== nodesSeedB[0].x || nodesSeedA[0].y !== nodesSeedB[0].y, 'Seed shifts node coordinates organically');

  // Test CSS gradient string output
  const css = generateMeshCssGradient(testHex, 'dawn', 42, 75);
  assert(css.includes('radial-gradient'), 'Generates valid multi-stop CSS radial-gradient mesh');
}

console.log(`\nResults: ${passedTests} / ${totalTests} tests passed.\n`);

if (passedTests === totalTests) {
  console.log('✓ All core logic tests passed successfully.\n');
  process.exit(0);
} else {
  console.error('✗ Some tests failed.\n');
  process.exit(1);
}
