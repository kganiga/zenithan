import React from 'react';
import { Heart, Sliders, Trash2, Download, HardDrive, Compass, Sparkles, Check, FileText } from 'lucide-react';
import { WallpaperItem, SavedProject } from '../types';
import { getFavorites, toggleFavorite, getSavedProjects, deleteProject, getEstimatedStorageSize, clearCache } from '../services/storage';
import { downloadAttribution } from '../services/exportEngine';

interface CollectionsScreenProps {
  onSelectWallpaper: (wallpaper: WallpaperItem) => void;
  onEditInStudio: (wallpaper: WallpaperItem, project?: SavedProject) => void;
  onNavigateExplore: () => void;
  onOpenExportModalForProject?: (project: SavedProject) => void;
}

export const CollectionsScreen: React.FC<CollectionsScreenProps> = ({
  onSelectWallpaper,
  onEditInStudio,
  onNavigateExplore,
}) => {
  const [activeTab, setActiveTab] = React.useState<'favorites' | 'projects'>('favorites');
  const [favorites, setFavorites] = React.useState<WallpaperItem[]>([]);
  const [projects, setProjects] = React.useState<SavedProject[]>([]);
  const [storageInfo, setStorageInfo] = React.useState({ sizeKb: 0, count: 0 });
  const [confirmClear, setConfirmClear] = React.useState(false);

  const refreshData = React.useCallback(() => {
    setFavorites(getFavorites());
    setProjects(getSavedProjects());
    setStorageInfo(getEstimatedStorageSize());
  }, []);

  React.useEffect(() => {
    refreshData();
  }, [refreshData]);

  const handleRemoveFavorite = (item: WallpaperItem) => {
    toggleFavorite(item);
    refreshData();
  };

  const handleDeleteProject = (projectId: string) => {
    deleteProject(projectId);
    refreshData();
  };

  const handleClearAll = () => {
    clearCache();
    setConfirmClear(false);
    refreshData();
  };

  return (
    <div className="max-w-7xl mx-auto px-4 sm:px-6 pt-6 pb-24 md:pb-16 space-y-8">
      {/* Page Header */}
      <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4 pb-4 border-b border-[#E8E4DC] dark:border-[#282A29]">
        <div>
          <h1 className="font-serif text-3xl font-semibold text-[#2C2B29] dark:text-[#F3F4F1] tracking-tight">
            Saved Collections
          </h1>
          <p className="text-xs sm:text-sm text-[#726F68] dark:text-[#939188] mt-1">
            Your favorited nature photography and custom wallpaper projects stored locally on this device.
          </p>
        </div>

        {/* Tab Switcher */}
        <div className="flex p-1 bg-[#EFECE6] dark:bg-[#1E201F] rounded-xl text-xs font-medium border border-[#E2DDD5] dark:border-[#2D302F]">
          <button
            onClick={() => setActiveTab('favorites')}
            className={`px-4 py-2 rounded-lg transition-colors flex items-center gap-1.5 ${
              activeTab === 'favorites'
                ? 'bg-white dark:bg-[#2C2B29] text-[#2C2B29] dark:text-white font-semibold shadow-sm'
                : 'text-[#726F68] dark:text-[#939188]'
            }`}
          >
            <Heart className="w-3.5 h-3.5" />
            <span>Favorites ({favorites.length})</span>
          </button>
          <button
            onClick={() => setActiveTab('projects')}
            className={`px-4 py-2 rounded-lg transition-colors flex items-center gap-1.5 ${
              activeTab === 'projects'
                ? 'bg-white dark:bg-[#2C2B29] text-[#2C2B29] dark:text-white font-semibold shadow-sm'
                : 'text-[#726F68] dark:text-[#939188]'
            }`}
          >
            <Sliders className="w-3.5 h-3.5" />
            <span>Custom Projects ({projects.length})</span>
          </button>
        </div>
      </div>

      {/* TAB 1: FAVORITES */}
      {activeTab === 'favorites' && (
        <div className="space-y-6">
          {favorites.length === 0 ? (
            <div className="p-12 text-center rounded-2xl border border-dashed border-[#DCD7CE] dark:border-[#333534] space-y-4">
              <div className="w-12 h-12 mx-auto rounded-full bg-[#EFECE6] dark:bg-[#202221] flex items-center justify-center text-[#726F68]">
                <Heart className="w-6 h-6" />
              </div>
              <div className="space-y-1">
                <h3 className="font-serif text-lg font-medium text-[#2C2B29] dark:text-[#F3F4F1]">
                  No Favorites Yet
                </h3>
                <p className="text-xs text-[#726F68] dark:text-[#939188] max-w-sm mx-auto">
                  Click the heart icon on any nature photograph in Explore to save it for offline contemplation.
                </p>
              </div>
              <button
                onClick={onNavigateExplore}
                className="px-4 py-2 rounded-lg bg-[#2C2B29] text-white text-xs font-medium inline-flex items-center gap-1.5"
              >
                <Compass className="w-3.5 h-3.5" />
                <span>Explore Nature Archives</span>
              </button>
            </div>
          ) : (
            <div className="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-5 gap-4">
              {favorites.map((item) => (
                <div
                  key={item.id}
                  className="group relative rounded-xl overflow-hidden aspect-[9/16] bg-[#0D0F0E] border border-[#E8E4DC] dark:border-[#2D302F] shadow-sm cursor-pointer"
                  onClick={() => onSelectWallpaper(item)}
                >
                  <img
                    src={item.thumbnailUrl}
                    alt={item.title}
                    className="w-full h-full object-cover transition-transform duration-500 group-hover:scale-105"
                    loading="lazy"
                  />

                  {/* Top Bar with delete */}
                  <div className="absolute top-2 right-2 z-10">
                    <button
                      onClick={(e) => {
                        e.stopPropagation();
                        handleRemoveFavorite(item);
                      }}
                      title="Remove from favorites"
                      className="p-1.5 rounded-full bg-black/50 hover:bg-black/70 text-white backdrop-blur-sm"
                    >
                      <Trash2 className="w-3.5 h-3.5 text-rose-300" />
                    </button>
                  </div>

                  {/* Hover Actions */}
                  <div className="absolute inset-0 bg-gradient-to-t from-black/85 via-black/20 to-transparent opacity-0 group-hover:opacity-100 transition-opacity p-3 flex flex-col justify-end text-white">
                    <p className="font-serif text-xs font-medium line-clamp-1">{item.title}</p>
                    <p className="text-[11px] text-white/70 line-clamp-1">
                      {item.license.author || 'Open Creator'}
                    </p>

                    <div className="grid grid-cols-2 gap-1.5 mt-2">
                      <button
                        onClick={(e) => {
                          e.stopPropagation();
                          downloadAttribution(item);
                        }}
                        title="Download license attribution file"
                        className="py-1 rounded bg-white/20 hover:bg-white/30 text-white text-[10px] font-medium flex items-center justify-center gap-1 backdrop-blur-sm"
                      >
                        <FileText className="w-3 h-3" />
                        <span>License</span>
                      </button>
                      <button
                        onClick={(e) => {
                          e.stopPropagation();
                          onEditInStudio(item);
                        }}
                        className="py-1 rounded bg-white text-black text-[10px] font-semibold flex items-center justify-center gap-1"
                      >
                        <Sparkles className="w-3 h-3 text-[#556952]" />
                        <span>Edit</span>
                      </button>
                    </div>
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>
      )}

      {/* TAB 2: SAVED CUSTOM PROJECTS */}
      {activeTab === 'projects' && (
        <div className="space-y-6">
          {projects.length === 0 ? (
            <div className="p-12 text-center rounded-2xl border border-dashed border-[#DCD7CE] dark:border-[#333534] space-y-4">
              <div className="w-12 h-12 mx-auto rounded-full bg-[#EFECE6] dark:bg-[#202221] flex items-center justify-center text-[#726F68]">
                <Sliders className="w-6 h-6" />
              </div>
              <div className="space-y-1">
                <h3 className="font-serif text-lg font-medium text-[#2C2B29] dark:text-[#F3F4F1]">
                  No Saved Projects
                </h3>
                <p className="text-xs text-[#726F68] dark:text-[#939188] max-w-sm mx-auto">
                  When you customize adjustments or typography in Studio, click "Save Project" to keep your editable setup here.
                </p>
              </div>
            </div>
          ) : (
            <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
              {projects.map((proj) => (
                <div
                  key={proj.id}
                  className="p-4 rounded-xl bg-white dark:bg-[#1A1C1B] border border-[#E8E4DC] dark:border-[#2D302F] shadow-sm flex gap-4 items-center justify-between group"
                >
                  <div className="flex items-center gap-3">
                    <div className="w-14 aspect-[9/16] rounded-lg overflow-hidden bg-black shrink-0 border border-[#E2DDD5] dark:border-[#333534]">
                      <img
                        src={proj.previewThumbnail || proj.wallpaper.thumbnailUrl}
                        alt={proj.title}
                        className="w-full h-full object-cover"
                      />
                    </div>
                    <div className="space-y-0.5">
                      <h4 className="font-medium text-xs sm:text-sm text-[#2C2B29] dark:text-[#F3F4F1] line-clamp-1">
                        {proj.title}
                      </h4>
                      <p className="text-[11px] text-[#726F68] dark:text-[#939188]">
                        Ratio: {proj.settings.cropRatio} · Zoom: {proj.settings.zoom.toFixed(1)}×
                      </p>
                      <p className="text-[10px] text-[#9E9B93] dark:text-[#6C6E6D]">
                        {new Date(proj.updatedAt).toLocaleDateString()}
                      </p>
                    </div>
                  </div>

                  <div className="flex items-center gap-1.5">
                    <button
                      onClick={() => onEditInStudio(proj.wallpaper, proj)}
                      className="px-3 py-1.5 rounded-lg bg-[#2C2B29] hover:bg-black text-white text-xs font-medium"
                    >
                      Open
                    </button>
                    <button
                      onClick={() => handleDeleteProject(proj.id)}
                      title="Delete project"
                      className="p-2 rounded-lg text-[#726F68] hover:text-rose-600 hover:bg-rose-50 dark:hover:bg-rose-950/20 transition-colors"
                    >
                      <Trash2 className="w-4 h-4" />
                    </button>
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>
      )}

      {/* On-Device Privacy & Storage Management Card */}
      <div className="p-5 rounded-2xl bg-[#EFECE6]/50 dark:bg-[#1E201F]/50 border border-[#E8E4DC] dark:border-[#2D302F] flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4 text-xs">
        <div className="flex items-start gap-3">
          <div className="p-2.5 rounded-xl bg-white dark:bg-[#282A29] text-[#556952] dark:text-[#BBCBB8] shrink-0">
            <HardDrive className="w-4 h-4" />
          </div>
          <div>
            <p className="font-medium text-[#2C2B29] dark:text-[#F3F4F1]">
              On-Device Storage: ~{storageInfo.sizeKb} KB ({storageInfo.count} stored records)
            </p>
            <p className="text-[#726F68] dark:text-[#939188] mt-0.5">
              100% Client-side. No user data, text edits, or custom wallpapers leave your device.
            </p>
          </div>
        </div>

        <div>
          {confirmClear ? (
            <div className="flex items-center gap-2">
              <span className="text-rose-600 font-medium">Clear all local data?</span>
              <button
                onClick={handleClearAll}
                className="px-3 py-1.5 rounded-lg bg-rose-600 text-white font-medium hover:bg-rose-700"
              >
                Confirm
              </button>
              <button
                onClick={() => setConfirmClear(false)}
                className="px-3 py-1.5 rounded-lg border border-[#DCD7CE] text-[#726F68]"
              >
                Cancel
              </button>
            </div>
          ) : (
            <button
              onClick={() => setConfirmClear(true)}
              className="px-3 py-1.5 rounded-lg border border-[#DCD7CE] dark:border-[#383A38] text-[#726F68] dark:text-[#939188] hover:text-rose-600 hover:bg-white dark:hover:bg-[#282A29] transition-colors"
            >
              Clear Local Cache
            </button>
          )}
        </div>
      </div>
    </div>
  );
};
