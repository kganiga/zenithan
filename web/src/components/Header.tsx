import React from 'react';
import { Sun, Moon, Sparkles, Image as ImageIcon, Heart } from 'lucide-react';
import { WallpaperItem } from '../types';

interface HeaderProps {
  theme: 'light' | 'dark';
  onToggleTheme: () => void;
  favoritesCount: number;
  onNavigate: (tab: 'home' | 'curated' | 'studio' | 'saved') => void;
  currentTab: 'home' | 'curated' | 'studio' | 'saved';
  onQuickSelectFeatured: (item: WallpaperItem) => void;
}

export const Header: React.FC<HeaderProps> = ({
  theme,
  onToggleTheme,
  favoritesCount,
  onNavigate,
  currentTab,
}) => {
  return (
    <header className="sticky top-0 z-30 w-full border-b border-[#E8E4DC] dark:border-[#262827] bg-[#F8F6F0]/90 dark:bg-[#121413]/90 zen-glass transition-colors">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 h-16 flex items-center justify-between">
        {/* Brand identity */}
        <div
          onClick={() => onNavigate('home')}
          className="flex items-center gap-3 cursor-pointer group select-none"
        >
          <div className="w-9 h-9 rounded-xl overflow-hidden shadow-sm transition-transform group-hover:scale-105 border border-[#E8E4DC] dark:border-[#333534] bg-[#2C2B29] flex-shrink-0">
            <img
              src="/logo.png"
              alt="Zenithan Studio Logo"
              className="w-full h-full object-cover"
            />
          </div>
          <div className="flex flex-col">
            <div className="flex items-center gap-2">
              <span className="font-serif font-bold text-xl tracking-tight text-[#2C2B29] dark:text-[#F3F4F1]">
                Zenithan
              </span>
              <span className="text-[10px] tracking-widest uppercase font-semibold px-1.5 py-0.5 rounded text-[#556952] dark:text-[#BBCBB8] bg-[#8A9A86]/20">
                Studio
              </span>
            </div>
            <span className="text-xs text-[#726F68] dark:text-[#939188] hidden sm:inline tracking-normal font-sans">
              High-resolution wallpapers & creative studio
            </span>
          </div>
        </div>

        {/* Desktop Quick Nav Links (clean typography, zero-pill discipline) */}
        <nav className="hidden md:flex items-center gap-6 text-sm font-medium">
          <button
            onClick={() => onNavigate('home')}
            className={`transition-colors py-1 ${
              currentTab === 'home'
                ? 'text-[#2C2B29] dark:text-[#F3F4F1] font-semibold border-b-2 border-[#8A9A86] dark:border-[#BBCBB8]'
                : 'text-[#726F68] dark:text-[#939188] hover:text-[#2C2B29] dark:hover:text-[#F3F4F1]'
            }`}
          >
            Explore Wallpapers
          </button>
          <button
            onClick={() => onNavigate('curated')}
            className={`transition-colors py-1 ${
              currentTab === 'curated'
                ? 'text-[#2C2B29] dark:text-[#F3F4F1] font-semibold border-b-2 border-[#8A9A86] dark:border-[#BBCBB8]'
                : 'text-[#726F68] dark:text-[#939188] hover:text-[#2C2B29] dark:hover:text-[#F3F4F1]'
            }`}
          >
            Curated Moods
          </button>
          <button
            onClick={() => onNavigate('studio')}
            className={`transition-colors py-1 ${
              currentTab === 'studio'
                ? 'text-[#2C2B29] dark:text-[#F3F4F1] font-semibold border-b-2 border-[#8A9A86] dark:border-[#BBCBB8]'
                : 'text-[#726F68] dark:text-[#939188] hover:text-[#2C2B29] dark:hover:text-[#F3F4F1]'
            }`}
          >
            Wallpaper Studio
          </button>
          <button
            onClick={() => onNavigate('saved')}
            className={`transition-colors py-1 flex items-center gap-1.5 ${
              currentTab === 'saved'
                ? 'text-[#2C2B29] dark:text-[#F3F4F1] font-semibold border-b-2 border-[#8A9A86] dark:border-[#BBCBB8]'
                : 'text-[#726F68] dark:text-[#939188] hover:text-[#2C2B29] dark:hover:text-[#F3F4F1]'
            }`}
          >
            <span>Saved</span>
            {favoritesCount > 0 && (
              <span className="text-[11px] px-1.5 py-0.2 rounded-full bg-[#8A9A86]/20 text-[#475744] dark:text-[#BBCBB8] font-mono">
                {favoritesCount}
              </span>
            )}
          </button>
        </nav>

        {/* Right actions */}
        <div className="flex items-center gap-2">
          {/* Quick Saved icon trigger on mobile */}
          <button
            onClick={() => onNavigate('saved')}
            aria-label="Saved Wallpapers"
            className="md:hidden relative min-h-[44px] min-w-[44px] flex items-center justify-center rounded-lg text-[#726F68] dark:text-[#939188] hover:text-[#2C2B29] dark:hover:text-[#F3F4F1] hover:bg-[#EFECE6] dark:hover:bg-[#202221] transition-colors"
          >
            <Heart className="w-5 h-5" />
            {favoritesCount > 0 && (
              <span className="absolute top-2 right-2 w-2 h-2 rounded-full bg-[#8A9A86] dark:bg-[#BBCBB8]" />
            )}
          </button>

          {/* Theme Switcher */}
          <button
            onClick={onToggleTheme}
            aria-label={`Switch to ${theme === 'light' ? 'Zen Charcoal dark' : 'Warm Mist white'} theme`}
            title={`Switch to ${theme === 'light' ? 'Zen Charcoal dark' : 'Warm Mist white'}`}
            className="min-h-[44px] min-w-[44px] px-2.5 flex items-center gap-2 rounded-lg text-[#726F68] dark:text-[#939188] hover:text-[#2C2B29] dark:hover:text-[#F3F4F1] hover:bg-[#EFECE6] dark:hover:bg-[#202221] transition-colors text-xs font-medium"
          >
            {theme === 'light' ? (
              <>
                <Moon className="w-4 h-4 text-[#726F68]" />
                <span className="hidden sm:inline">Charcoal</span>
              </>
            ) : (
              <>
                <Sun className="w-4 h-4 text-[#BBCBB8]" />
                <span className="hidden sm:inline">Mist White</span>
              </>
            )}
          </button>

          {/* Studio Action Button */}
          <button
            onClick={() => onNavigate('studio')}
            className="min-h-[40px] px-3.5 py-2 rounded-lg bg-[#2C2B29] hover:bg-[#1A1918] text-[#F8F6F0] dark:bg-[#E2E3E0] dark:hover:bg-[#FFFFFF] dark:text-[#121413] text-xs font-medium tracking-wide flex items-center gap-1.5 shadow-sm transition-all hover:scale-[1.02] active:scale-[0.98]"
          >
            <Sparkles className="w-3.5 h-3.5" />
            <span>Create</span>
          </button>
        </div>
      </div>
    </header>
  );
};
