import type { CapacitorConfig } from '@capacitor/cli';

const config: CapacitorConfig = {
  appId: 'com.zennature.wallpaper',
  appName: 'Zen Nature',
  webDir: 'dist',
  server: {
    androidScheme: 'https',
  },
};

export default config;
