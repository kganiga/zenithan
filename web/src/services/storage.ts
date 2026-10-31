import { WallpaperItem, SavedProject, EditorSettings } from '../types';

const FAVORITES_KEY = 'zen_nature_favorites_v1';
const RECENTS_KEY = 'zen_nature_recents_v1';
const PROJECTS_KEY = 'zen_nature_projects_v1';
const THEME_KEY = 'zen_nature_theme_v1';

export function getFavorites(): WallpaperItem[] {
  try {
    const raw = localStorage.getItem(FAVORITES_KEY);
    return raw ? JSON.parse(raw) : [];
  } catch {
    return [];
  }
}

export function isFavorite(wallpaperId: string): boolean {
  const favs = getFavorites();
  return favs.some((item) => item.id === wallpaperId);
}

export function toggleFavorite(item: WallpaperItem): boolean {
  const favs = getFavorites();
  const existsIndex = favs.findIndex((f) => f.id === item.id);
  let updated: WallpaperItem[];
  let isNowFav = false;

  if (existsIndex >= 0) {
    updated = favs.filter((f) => f.id !== item.id);
    isNowFav = false;
  } else {
    updated = [item, ...favs];
    isNowFav = true;
  }

  try {
    localStorage.setItem(FAVORITES_KEY, JSON.stringify(updated));
  } catch (err) {
    console.warn('LocalStorage save error:', err);
  }

  return isNowFav;
}

export function getRecents(): WallpaperItem[] {
  try {
    const raw = localStorage.getItem(RECENTS_KEY);
    return raw ? JSON.parse(raw) : [];
  } catch {
    return [];
  }
}

export function addRecent(item: WallpaperItem): void {
  const recents = getRecents();
  const filtered = recents.filter((r) => r.id !== item.id);
  const updated = [item, ...filtered].slice(0, 20); // Keep 20 most recent
  try {
    localStorage.setItem(RECENTS_KEY, JSON.stringify(updated));
  } catch (err) {
    console.warn('LocalStorage recents error:', err);
  }
}

export function getSavedProjects(): SavedProject[] {
  try {
    const raw = localStorage.getItem(PROJECTS_KEY);
    return raw ? JSON.parse(raw) : [];
  } catch {
    return [];
  }
}

export function saveProject(
  wallpaper: WallpaperItem,
  settings: EditorSettings,
  title?: string,
  previewThumbnail?: string
): SavedProject {
  const projects = getSavedProjects();
  const now = Date.now();
  const projectTitle = title || `Zen Project ${new Date(now).toLocaleDateString()}`;

  const newProject: SavedProject = {
    id: `project-${now}-${Math.random().toString(36).substr(2, 6)}`,
    title: projectTitle,
    createdAt: now,
    updatedAt: now,
    wallpaper,
    settings,
    previewThumbnail,
  };

  const updated = [newProject, ...projects];
  try {
    localStorage.setItem(PROJECTS_KEY, JSON.stringify(updated));
  } catch (err) {
    console.warn('LocalStorage project save error:', err);
  }

  return newProject;
}

export function deleteProject(projectId: string): void {
  const projects = getSavedProjects();
  const updated = projects.filter((p) => p.id !== projectId);
  try {
    localStorage.setItem(PROJECTS_KEY, JSON.stringify(updated));
  } catch (err) {
    console.warn('LocalStorage project delete error:', err);
  }
}

export function getStoredTheme(): 'light' | 'dark' {
  try {
    const raw = localStorage.getItem(THEME_KEY);
    if (raw === 'dark' || raw === 'light') return raw;
  } catch {
    // fallback
  }
  return 'light'; // Default Warm Mist White as confirmed in user interview
}

export function setStoredTheme(theme: 'light' | 'dark'): void {
  try {
    localStorage.setItem(THEME_KEY, theme);
  } catch {
    // ignore
  }
}

export function getEstimatedStorageSize(): { sizeKb: number; count: number } {
  let totalLength = 0;
  let count = 0;
  for (let i = 0; i < localStorage.length; i++) {
    const key = localStorage.key(i);
    if (key && key.startsWith('zen_nature_')) {
      const val = localStorage.getItem(key) || '';
      totalLength += key.length + val.length;
      count++;
    }
  }
  return {
    sizeKb: Math.round((totalLength * 2) / 1024), // UTF-16 approximate
    count,
  };
}

export function clearCache(): void {
  const keysToRemove: string[] = [];
  for (let i = 0; i < localStorage.length; i++) {
    const key = localStorage.key(i);
    if (key && key.startsWith('zen_nature_') && key !== THEME_KEY) {
      keysToRemove.push(key);
    }
  }
  keysToRemove.forEach((k) => localStorage.removeItem(k));
}
