/**
 * @license
 * SPDX-License-Identifier: Apache-2.0
 */

import React from 'react';
import { Header } from './components/Header';
import { Navbar } from './components/Navbar';
import { WallpaperFeedScreen } from './components/WallpaperFeedScreen';
import { HomeScreen } from './components/HomeScreen';
import { EditorScreen } from './components/EditorScreen';
import { CollectionsScreen } from './components/CollectionsScreen';
import { DetailModal } from './components/DetailModal';
import { ExportModal } from './components/ExportModal';
import { WallpaperItem, SavedProject, EditorSettings } from './types';
import { getFeaturedWallpaper } from './services/wallpaperRepository';
import {
  getFavorites,
  toggleFavorite,
  getRecents,
  addRecent,
  getStoredTheme,
  setStoredTheme,
} from './services/storage';

export default function App() {
  const [currentTab, setCurrentTab] = React.useState<'home' | 'curated' | 'studio' | 'saved'>('home');
  const [theme, setTheme] = React.useState<'light' | 'dark'>('light');

  // Featured & active wallpaper for the studio
  const [featuredWallpaper] = React.useState<WallpaperItem>(getFeaturedWallpaper);
  const [activeWallpaper, setActiveWallpaper] = React.useState<WallpaperItem>(getFeaturedWallpaper);
  const [editorInitialSettings, setEditorInitialSettings] = React.useState<EditorSettings | undefined>(undefined);

  // Modal states
  const [detailWallpaper, setDetailWallpaper] = React.useState<WallpaperItem | null>(null);
  const [isExportModalOpen, setIsExportModalOpen] = React.useState(false);

  // Category filter state when navigating from Curated Moods to Home
  const [selectedCategory, setSelectedCategory] = React.useState<string | undefined>(undefined);

  // Local storage reactive states
  const [favorites, setFavorites] = React.useState<WallpaperItem[]>([]);
  const [recents, setRecents] = React.useState<WallpaperItem[]>([]);

  // Initialize theme and storage on mount
  React.useEffect(() => {
    const storedTheme = getStoredTheme();
    setTheme(storedTheme);
    if (storedTheme === 'dark') {
      document.documentElement.classList.add('dark');
    } else {
      document.documentElement.classList.remove('dark');
    }

    setFavorites(getFavorites());
    setRecents(getRecents());
  }, []);

  const handleToggleTheme = () => {
    const nextTheme = theme === 'light' ? 'dark' : 'light';
    setTheme(nextTheme);
    setStoredTheme(nextTheme);
    if (nextTheme === 'dark') {
      document.documentElement.classList.add('dark');
    } else {
      document.documentElement.classList.remove('dark');
    }
  };

  const handleSelectWallpaperForDetail = (wallpaper: WallpaperItem) => {
    setDetailWallpaper(wallpaper);
    addRecent(wallpaper);
    setRecents(getRecents());
  };

  const handleEditInStudio = (wallpaper: WallpaperItem, project?: SavedProject) => {
    setActiveWallpaper(wallpaper);
    if (project) {
      setEditorInitialSettings(project.settings);
    } else {
      setEditorInitialSettings(undefined);
    }
    addRecent(wallpaper);
    setRecents(getRecents());
    setCurrentTab('studio');
  };

  const handleToggleFavorite = (wallpaper: WallpaperItem) => {
    toggleFavorite(wallpaper);
    setFavorites(getFavorites());
  };

  const isFavorited = (id: string) => {
    return favorites.some((f) => f.id === id);
  };

  const handleSelectCategoryFromCurated = (categoryId: string) => {
    setSelectedCategory(categoryId);
    setCurrentTab('home');
    window.scrollTo({ top: 0, behavior: 'smooth' });
  };

  const handleImportCustomPhoto = (customWallpaper: WallpaperItem) => {
    setActiveWallpaper(customWallpaper);
    setEditorInitialSettings(undefined);
    addRecent(customWallpaper);
    setRecents(getRecents());
    setCurrentTab('studio');
  };

  return (
    <div className="min-h-screen flex flex-col bg-[#F8F6F0] dark:bg-[#121413] text-[#2C2B29] dark:text-[#E2E3E0] font-sans transition-colors duration-200">
      {/* Top Header */}
      <Header
        theme={theme}
        onToggleTheme={handleToggleTheme}
        favoritesCount={favorites.length}
        onNavigate={(tab) => {
          if (tab === 'home') setSelectedCategory(undefined);
          setCurrentTab(tab);
        }}
        currentTab={currentTab}
        onQuickSelectFeatured={handleEditInStudio}
      />

      {/* Main View Port */}
      <main className="flex-1">
        {/* Screen 1: Home - Continuous Infinite Scroll Wallpaper Stream */}
        {currentTab === 'home' && (
          <WallpaperFeedScreen
            initialCategory={selectedCategory}
            onSelectWallpaper={handleSelectWallpaperForDetail}
            onEditInStudio={handleEditInStudio}
            isFavorited={isFavorited}
            onToggleFavorite={handleToggleFavorite}
            onImportCustomPhoto={handleImportCustomPhoto}
            onNavigateCurated={() => setCurrentTab('curated')}
          />
        )}

        {/* Screen 2: Daily Zen & Moods - Featured Hero Lockscreen Clock Preview, Circadian Stillness & 10 Curated Moods */}
        {currentTab === 'curated' && (
          <HomeScreen
            featuredWallpaper={featuredWallpaper}
            recentWallpapers={recents}
            onSelectCategory={handleSelectCategoryFromCurated}
            onSelectWallpaper={handleSelectWallpaperForDetail}
            onEditInStudio={handleEditInStudio}
            onNavigateExplore={() => {
              setSelectedCategory(undefined);
              setCurrentTab('home');
            }}
            isFavorited={isFavorited}
            onToggleFavorite={handleToggleFavorite}
            onImportCustomPhoto={handleImportCustomPhoto}
          />
        )}

        {/* Screen 3: Wallpaper Studio */}
        {currentTab === 'studio' && (
          <EditorScreen
            wallpaper={activeWallpaper}
            onOpenExportModal={() => setIsExportModalOpen(true)}
            onSelectAnotherWallpaper={() => setCurrentTab('home')}
            onProjectSaved={() => setFavorites(getFavorites())}
            onImportCustomPhoto={handleImportCustomPhoto}
          />
        )}

        {/* Screen 4: Saved Collections */}
        {currentTab === 'saved' && (
          <CollectionsScreen
            onSelectWallpaper={handleSelectWallpaperForDetail}
            onEditInStudio={handleEditInStudio}
            onNavigateExplore={() => {
              setSelectedCategory(undefined);
              setCurrentTab('home');
            }}
          />
        )}
      </main>

      {/* Mobile Bottom Tab Bar */}
      <Navbar
        currentTab={currentTab}
        onNavigate={(tab) => {
          if (tab === 'home') setSelectedCategory(undefined);
          setCurrentTab(tab);
        }}
        favoritesCount={favorites.length}
      />

      {/* Wallpaper Detail & License Inspection Modal */}
      {detailWallpaper && (
        <DetailModal
          wallpaper={detailWallpaper}
          onClose={() => setDetailWallpaper(null)}
          onEditInStudio={handleEditInStudio}
          isFavorited={isFavorited(detailWallpaper.id)}
          onToggleFavorite={handleToggleFavorite}
        />
      )}

      {/* Wallpaper High-Resolution Export Modal */}
      {isExportModalOpen && (
        <ExportModal
          wallpaper={activeWallpaper}
          settings={
            editorInitialSettings || {
              cropRatio: '20:9',
              zoom: 1.0,
              panX: 0,
              panY: 0,
              brightness: 100,
              contrast: 100,
              saturation: 100,
              warmth: 0,
              blur: 0,
              vignette: 15,
              grain: 5,
              washPreset: 'none',
              washIntensity: 35,
              textOverlay: {
                enabled: false,
                text: '',
                subtext: '',
                kanji: '',
                fontStyle: 'serif',
                position: 'bottom',
                size: 'md',
                color: '#FFFFFF',
                opacity: 0.9,
                letterSpacing: 'wide',
                showJapaneseMark: true,
              },
              showClockPreview: true,
              showWidgetsPreview: false,
              gridOverlay: 'none',
              uiPreviewMode: 'lockscreen',
              washiTexture: 0,
              komorebiRays: 0,
              mistGradient: 'none',
              mistIntensity: 40,
            }
          }
          onClose={() => setIsExportModalOpen(false)}
        />
      )}
    </div>
  );
}
