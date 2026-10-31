import { CuratedCategory, ZenPhrasePreset, AspectRatioType } from '../types';

export const CURATED_CATEGORIES: CuratedCategory[] = [
  {
    id: 'misty-mountains',
    label: 'Misty Mountains',
    subtitle: 'Solitary peaks veiled in cloud',
    query: 'minimal mist mountain landscape',
    icon: 'Mountain',
    tagline: 'Quiet elevations rising through morning vapors',
  },
  {
    id: 'japanese-gardens',
    label: 'Japanese Gardens',
    subtitle: 'Raked gravel and moss stones',
    query: 'Japanese moss garden',
    icon: 'Trees',
    tagline: 'Centuries of contemplation sculpted in moss and rock',
  },
  {
    id: 'forests-moss',
    label: 'Forests & Moss',
    subtitle: 'Dappled light in ancient cedar',
    query: 'foggy pine forest',
    icon: 'TreePine',
    tagline: 'Soft evergreen ground cover absorbing all noise',
  },
  {
    id: 'ocean-waves',
    label: 'Ocean & Waves',
    subtitle: 'Endless horizon and rhythmic tide',
    query: 'minimal ocean horizon',
    icon: 'Waves',
    tagline: 'The timeless breathing of deep waters',
  },
  {
    id: 'lakes-reflections',
    label: 'Lakes & Reflections',
    subtitle: 'Mirror calm glacial waters',
    query: 'calm lake reflection',
    icon: 'Compass',
    tagline: 'Stillness that captures both sky and self',
  },
  {
    id: 'minimalist-deserts',
    label: 'Minimalist Deserts',
    subtitle: 'Wind-rippled solitary dunes',
    query: 'minimal sand dunes',
    icon: 'Sun',
    tagline: 'Pure geometry carved by wandering winds',
  },
  {
    id: 'rain-fog',
    label: 'Rain & Fog',
    subtitle: 'Soft drops and silver mists',
    query: 'misty rain forest',
    icon: 'CloudRain',
    tagline: 'The gentle sound of water renewing the earth',
  },
  {
    id: 'bamboo-stones',
    label: 'Bamboo & Stones',
    subtitle: 'Flexible stalks and grounded granite',
    query: 'bamboo forest mist',
    icon: 'Feather',
    tagline: 'Bending with the breeze, rooted deep in stone',
  },
  {
    id: 'sunsets-dawn',
    label: 'Sunsets & Dawn',
    subtitle: 'Ethereal amber and morning rose',
    query: 'soft sunrise mountain',
    icon: 'Sunset',
    tagline: 'The quiet threshold between dusk and awakening',
  },
  {
    id: 'moon-night-sky',
    label: 'Moon & Night Sky',
    subtitle: 'Celestial silver over silent earth',
    query: 'moon over mountains',
    icon: 'Moon',
    tagline: 'Infinite quietude under cold celestial light',
  },
];

export const ZEN_PHRASES: ZenPhrasePreset[] = [
  {
    id: 'basho-pond',
    category: 'haiku',
    text: 'The ancient pond;\nA frog leaps in,\nWater’s echo.',
    subtext: 'Matsuo Basho',
    kanji: '古池や',
  },
  {
    id: 'shoshin',
    category: 'calm',
    text: 'Beginner’s Mind',
    subtext: 'In the beginner’s mind there are many possibilities',
    kanji: '初心',
  },
  {
    id: 'meikyo',
    category: 'calm',
    text: 'Clear Mirror, Still Water',
    subtext: 'Mind undisturbed by passing reflections',
    kanji: '明鏡止水',
  },
  {
    id: 'breathe-stillness',
    category: 'calm',
    text: 'Breathe in stillness,\nBreathe out the storm.',
    subtext: 'Mindfulness Reminder',
    kanji: '静寂',
  },
  {
    id: 'mountain-cloud',
    category: 'nature',
    text: 'Silent mountain,\nPassing white cloud.',
    subtext: 'Non-attachment',
    kanji: '山雲',
  },
  {
    id: 'ichigo-ichie',
    category: 'calm',
    text: 'One Time, One Meeting',
    subtext: 'Cherish every singular moment',
    kanji: '一期一会',
  },
  {
    id: 'unbroken-pine',
    category: 'nature',
    text: 'The pine bends with the heavy snow,\nYet never breaks.',
    subtext: 'Resilience',
    kanji: '松雪',
  },
  {
    id: 'flowing-water',
    category: 'nature',
    text: 'Water flows without hurry,\nYet shapes the canyon.',
    subtext: 'Patience',
    kanji: '流水',
  },
  {
    id: 'today-date',
    category: 'date',
    text: new Date().toLocaleDateString('en-US', { month: 'long', day: 'numeric', year: 'numeric' }),
    subtext: 'Find stillness in this day',
    kanji: '今日',
  },
];

export const ASPECT_RATIOS: {
  id: AspectRatioType;
  label: string;
  widthRatio: number;
  heightRatio: number;
  description: string;
  idealWidth: number;
  idealHeight: number;
  deviceType: 'mobile' | 'tablet' | 'desktop' | 'square';
}[] = [
  {
    id: '9:16',
    label: '9:16 Mobile',
    widthRatio: 9,
    heightRatio: 16,
    description: 'Universal phone lock screen',
    idealWidth: 1080,
    idealHeight: 1920,
    deviceType: 'mobile',
  },
  {
    id: '20:9',
    label: '20:9 Flagship',
    widthRatio: 9,
    heightRatio: 20,
    description: 'Modern tall Android display',
    idealWidth: 1080,
    idealHeight: 2400,
    deviceType: 'mobile',
  },
  {
    id: '3:4',
    label: '3:4 Tablet',
    widthRatio: 3,
    heightRatio: 4,
    description: 'Tablet & iPad portrait',
    idealWidth: 1536,
    idealHeight: 2048,
    deviceType: 'tablet',
  },
  {
    id: '16:9',
    label: '16:9 Desktop',
    widthRatio: 16,
    heightRatio: 9,
    description: 'Laptop & widescreen monitor',
    idealWidth: 1920,
    idealHeight: 1080,
    deviceType: 'desktop',
  },
  {
    id: '1:1',
    label: '1:1 Square',
    widthRatio: 1,
    heightRatio: 1,
    description: 'Square profile & framing',
    idealWidth: 1080,
    idealHeight: 1080,
    deviceType: 'square',
  },
];

export interface CircadianSlot {
  id: 'dawn' | 'daylight' | 'twilight' | 'night';
  label: string;
  hourRange: string;
  moodDescription: string;
  recommendedCategory: string;
  japaneseTitle: string;
  accentColor: string;
  suggestedQuery: string;
}

export function getCurrentCircadianSlot(): CircadianSlot {
  const hour = new Date().getHours();

  if (hour >= 5 && hour < 9) {
    return {
      id: 'dawn',
      label: 'Morning Mist & Dew',
      hourRange: '05:00 — 09:00',
      moodDescription: 'Gentle morning radiance and silver vapors rising over tranquil mountain peaks.',
      recommendedCategory: 'sunsets-dawn',
      japaneseTitle: 'Dawn Light · Morning Mist',
      accentColor: '#D89E6E',
      suggestedQuery: 'soft sunrise mountain mist',
    };
  }

  if (hour >= 9 && hour < 17) {
    return {
      id: 'daylight',
      label: 'Clear Daylight Stillness',
      hourRange: '09:00 — 17:00',
      moodDescription: 'Lush green moss sanctuaries, sunlit forest trails, and mirror lake reflections.',
      recommendedCategory: 'japanese-gardens',
      japaneseTitle: 'Sunlit Serenity · Deep Woods',
      accentColor: '#6E8A68',
      suggestedQuery: 'Japanese moss garden lake',
    };
  }

  if (hour >= 17 && hour < 21) {
    return {
      id: 'twilight',
      label: 'Twilight & Blue Hour',
      hourRange: '17:00 — 21:00',
      moodDescription: 'Deep amber horizon and rippling desert dunes fading into peaceful dusk.',
      recommendedCategory: 'minimalist-deserts',
      japaneseTitle: 'Twilight Horizon · Blue Hour',
      accentColor: '#4A5B73',
      suggestedQuery: 'minimal sand dunes dusk twilight',
    };
  }

  return {
    id: 'night',
    label: 'Moonlit Quietude',
    hourRange: '21:00 — 05:00',
    moodDescription: 'Silver celestial moonlight resting over dark pine ridges and still alpine waters.',
    recommendedCategory: 'moon-night-sky',
    japaneseTitle: 'Night Sky · Cosmic Silence',
    accentColor: '#3A4454',
    suggestedQuery: 'moon over mountain lake night',
  };
}
