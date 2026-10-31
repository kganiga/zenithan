import React from 'react';
import { Image as ImageIcon, Sun, Sliders, Bookmark } from 'lucide-react';

interface NavbarProps {
  currentTab: 'home' | 'curated' | 'studio' | 'saved';
  onNavigate: (tab: 'home' | 'curated' | 'studio' | 'saved') => void;
  favoritesCount: number;
}

export const Navbar: React.FC<NavbarProps> = ({ currentTab, onNavigate, favoritesCount }) => {
  const tabs = [
    { id: 'home' as const, label: 'Wallpapers', icon: ImageIcon },
    { id: 'curated' as const, label: 'Discover', icon: Sun },
    { id: 'studio' as const, label: 'Studio', icon: Sliders },
    { id: 'saved' as const, label: 'Saved', icon: Bookmark, badge: favoritesCount },
  ];

  return (
    <nav
      aria-label="Mobile Navigation"
      className="md:hidden fixed bottom-0 left-0 right-0 z-40 bg-[#F8F6F0]/95 dark:bg-[#121413]/95 zen-glass border-t border-[#E8E4DC] dark:border-[#262827] pb-safe"
    >
      <div className="grid grid-cols-4 h-16 max-w-lg mx-auto">
        {tabs.map((tab) => {
          const Icon = tab.icon;
          const isActive = currentTab === tab.id;

          return (
            <button
              key={tab.id}
              onClick={() => onNavigate(tab.id)}
              className="min-h-[44px] min-w-[44px] flex flex-col items-center justify-center relative group transition-colors"
            >
              <div className="relative">
                <Icon
                  className={`w-5 h-5 transition-transform duration-200 ${
                    isActive
                      ? 'text-[#4A5D47] dark:text-[#BBCBB8] scale-110 stroke-[2.2]'
                      : 'text-[#8A8780] dark:text-[#888A87] group-hover:text-[#2C2B29] dark:group-hover:text-[#F3F4F1] stroke-[1.7]'
                  }`}
                />
                {tab.badge !== undefined && tab.badge > 0 && (
                  <span className="absolute -top-1 -right-2 min-w-[15px] h-[15px] px-1 bg-[#8A9A86] dark:bg-[#BBCBB8] text-white dark:text-[#121413] text-[9px] font-bold rounded-full flex items-center justify-center">
                    {tab.badge > 99 ? '99+' : tab.badge}
                  </span>
                )}
              </div>
              <span
                className={`text-[10px] font-medium tracking-tight mt-1 transition-colors ${
                  isActive
                    ? 'text-[#2C2B29] dark:text-[#F3F4F1] font-semibold'
                    : 'text-[#8A8780] dark:text-[#888A87]'
                }`}
              >
                {tab.label}
              </span>
              {isActive && (
                <span className="absolute bottom-1 w-1 h-1 rounded-full bg-[#8A9A86] dark:bg-[#BBCBB8]" />
              )}
            </button>
          );
        })}
      </div>
    </nav>
  );
};
