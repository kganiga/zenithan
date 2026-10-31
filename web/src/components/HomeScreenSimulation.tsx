import React from 'react';
import {
  Phone,
  MessageSquare,
  Camera,
  Image as ImageIcon,
  Compass,
  Music,
  Settings,
  Clock,
  CloudSun,
  Folder,
  Search,
  Mic,
  Calendar,
} from 'lucide-react';

export const HomeScreenSimulation: React.FC = () => {
  const mainIcons = [
    { label: 'Photos', icon: ImageIcon, color: 'bg-emerald-500/80 text-white' },
    { label: 'Notes', icon: MessageSquare, color: 'bg-amber-500/80 text-white' },
    { label: 'Music', icon: Music, color: 'bg-indigo-500/80 text-white' },
    { label: 'Clock', icon: Clock, color: 'bg-slate-700/80 text-white' },

    { label: 'Maps', icon: Compass, color: 'bg-teal-500/80 text-white' },
    { label: 'Calendar', icon: Calendar, color: 'bg-rose-500/80 text-white' },
    { label: 'Weather', icon: CloudSun, color: 'bg-sky-500/80 text-white' },
    { label: 'Files', icon: Folder, color: 'bg-blue-600/80 text-white' },
  ];

  const dockIcons = [
    { label: 'Phone', icon: Phone, color: 'bg-emerald-600/90 text-white' },
    { label: 'Messages', icon: MessageSquare, color: 'bg-blue-500/90 text-white' },
    { label: 'Camera', icon: Camera, color: 'bg-slate-800/90 text-white' },
    { label: 'Settings', icon: Settings, color: 'bg-stone-600/90 text-white' },
  ];

  return (
    <div className="absolute inset-0 pointer-events-none select-none z-20 p-4 flex flex-col justify-between text-white font-sans">
      {/* Top At-A-Glance Widget Pill */}
      <div className="pt-6 px-1 flex items-center justify-between text-xs">
        <div className="flex items-center gap-1.5 px-3 py-1.5 rounded-full bg-black/40 backdrop-blur-md border border-white/10 shadow-sm">
          <CloudSun className="w-4 h-4 text-amber-300" />
          <span className="font-medium tracking-tight text-white/95">21°C · Zen Calm</span>
        </div>
        <div className="text-[11px] font-mono px-2 py-1 rounded-full bg-black/30 backdrop-blur-md text-white/80">
          Mon, Oct 21
        </div>
      </div>

      {/* Middle 4-Column App Icons Grid */}
      <div className="my-auto py-2">
        <div className="grid grid-cols-4 gap-y-5 gap-x-2 text-center">
          {mainIcons.map((app, idx) => {
            const Icon = app.icon;
            return (
              <div key={idx} className="flex flex-col items-center gap-1">
                <div
                  className={`w-11 h-11 rounded-2xl flex items-center justify-center shadow-lg ring-1 ring-white/15 backdrop-blur-sm ${app.color}`}
                >
                  <Icon className="w-5 h-5 drop-shadow-sm" />
                </div>
                <span className="text-[10px] font-medium tracking-tight text-white drop-shadow-[0_1px_2px_rgba(0,0,0,0.8)] truncate max-w-[56px]">
                  {app.label}
                </span>
              </div>
            );
          })}
        </div>
      </div>

      {/* Bottom Area: Search Pill + Dock */}
      <div className="space-y-3 pb-3">
        {/* Android Google-style Search Pill */}
        <div className="h-10 px-3.5 rounded-full bg-white/75 dark:bg-black/60 backdrop-blur-md border border-white/20 flex items-center justify-between shadow-lg text-black/80 dark:text-white/80">
          <div className="flex items-center gap-2">
            <span className="font-bold text-xs text-sky-600 dark:text-sky-400">G</span>
            <span className="text-xs text-neutral-600 dark:text-neutral-300 font-normal">Search...</span>
          </div>
          <div className="flex items-center gap-2 text-neutral-500 dark:text-neutral-400">
            <Mic className="w-3.5 h-3.5" />
            <Search className="w-3.5 h-3.5" />
          </div>
        </div>

        {/* Bottom Dock Icons */}
        <div className="grid grid-cols-4 gap-2 pt-1 text-center">
          {dockIcons.map((app, idx) => {
            const Icon = app.icon;
            return (
              <div key={idx} className="flex flex-col items-center">
                <div
                  className={`w-11 h-11 rounded-2xl flex items-center justify-center shadow-lg ring-1 ring-white/15 backdrop-blur-sm ${app.color}`}
                >
                  <Icon className="w-5 h-5 drop-shadow-sm" />
                </div>
              </div>
            );
          })}
        </div>

        {/* Android Gesture Bar */}
        <div className="w-24 h-1 mx-auto rounded-full bg-white/60 shadow-sm" />
      </div>
    </div>
  );
};
