import React from 'react';
import {
  Wind,
  Play,
  Pause,
  RotateCcw,
  Volume2,
  VolumeX,
  X,
  Sparkles,
  CheckCircle2,
  Smartphone,
  ArrowRight,
} from 'lucide-react';
import { WallpaperItem } from '../types';

interface ZenBreathingTimerProps {
  isOpen: boolean;
  onClose: () => void;
  onStartBuilding: (wallpaper?: WallpaperItem) => void;
  targetWallpaper?: WallpaperItem;
}

type BreathingPhase = 'inhale' | 'hold-in' | 'exhale' | 'rest';

// 15 seconds per cycle = exactly 4 complete cycles for 60 seconds
const CYCLE_DURATION = 15;
const INHALE_DURATION = 4;
const HOLD_DURATION = 4;
const EXHALE_DURATION = 4;
// REST_DURATION is remaining (3 seconds)

export const ZenBreathingTimer: React.FC<ZenBreathingTimerProps> = ({
  isOpen,
  onClose,
  onStartBuilding,
  targetWallpaper,
}) => {
  const [secondsLeft, setSecondsLeft] = React.useState(60);
  const [isActive, setIsActive] = React.useState(true);
  const [isCompleted, setIsCompleted] = React.useState(false);
  const [soundEnabled, setSoundEnabled] = React.useState(false);

  // Play soft gentle sine chime on phase change (optional, respectful of sound setting)
  const playChime = React.useCallback((frequency = 432) => {
    if (!soundEnabled) return;
    try {
      const AudioCtx =
        window.AudioContext ||
        (window as unknown as { webkitAudioContext: typeof AudioContext }).webkitAudioContext;
      if (!AudioCtx) return;
      const ctx = new AudioCtx();
      const osc = ctx.createOscillator();
      const gain = ctx.createGain();

      osc.type = 'sine';
      osc.frequency.setValueAtTime(frequency, ctx.currentTime);

      gain.gain.setValueAtTime(0, ctx.currentTime);
      gain.gain.linearRampToValueAtTime(0.06, ctx.currentTime + 0.1);
      gain.gain.exponentialRampToValueAtTime(0.0001, ctx.currentTime + 1.2);

      osc.connect(gain);
      gain.connect(ctx.destination);

      osc.start();
      osc.stop(ctx.currentTime + 1.3);
    } catch {
      // Audio context policy safe ignore
    }
  }, [soundEnabled]);

  // Reset when opened
  React.useEffect(() => {
    if (isOpen) {
      setSecondsLeft(60);
      setIsActive(true);
      setIsCompleted(false);
    }
  }, [isOpen]);

  // Main 1-second countdown interval
  React.useEffect(() => {
    if (!isOpen || !isActive || isCompleted) return;

    const interval = setInterval(() => {
      setSecondsLeft((prev) => {
        if (prev <= 1) {
          clearInterval(interval);
          setIsActive(false);
          setIsCompleted(true);
          playChime(528); // Gentle celebration chime
          return 0;
        }
        return prev - 1;
      });
    }, 1000);

    return () => clearInterval(interval);
  }, [isOpen, isActive, isCompleted, playChime]);

  // Calculate current cycle and phase based on elapsed time
  const elapsedTime = 60 - secondsLeft;
  const cycleIndex = Math.min(Math.floor(elapsedTime / CYCLE_DURATION) + 1, 4);
  const cycleTime = elapsedTime % CYCLE_DURATION;

  let currentPhase: BreathingPhase = 'inhale';
  let phaseInstruction = 'Breathe in slowly';
  let phaseSubtitle = 'Expand awareness & receive clarity';
  let orbScale = 1.0;
  let orbGlow = 'rgba(138, 154, 134, 0.25)';

  if (isCompleted) {
    currentPhase = 'rest';
    phaseInstruction = 'Mind Clear & Centered';
    phaseSubtitle = 'Carry this quiet focus into your wallpaper creation.';
    orbScale = 1.0;
  } else if (cycleTime < INHALE_DURATION) {
    currentPhase = 'inhale';
    phaseInstruction = 'Breathe In Slowly';
    phaseSubtitle = 'Expand awareness and invite stillness';
    // 0 to 4s: scale from 0.9 up to 1.35
    const progress = (cycleTime + 0.5) / INHALE_DURATION;
    orbScale = 0.9 + progress * 0.45;
    orbGlow = 'rgba(138, 154, 134, 0.45)';
  } else if (cycleTime < INHALE_DURATION + HOLD_DURATION) {
    currentPhase = 'hold-in';
    phaseInstruction = 'Hold in Stillness';
    phaseSubtitle = 'Rest quietly in full presence';
    orbScale = 1.35;
    orbGlow = 'rgba(187, 203, 184, 0.4)';
  } else if (cycleTime < INHALE_DURATION + HOLD_DURATION + EXHALE_DURATION) {
    currentPhase = 'exhale';
    phaseInstruction = 'Exhale Gently';
    phaseSubtitle = 'Release tension, clutter, and distractions';
    // 8 to 12s: scale down from 1.35 to 0.9
    const exhaleTime = cycleTime - (INHALE_DURATION + HOLD_DURATION);
    const progress = (exhaleTime + 0.5) / EXHALE_DURATION;
    orbScale = 1.35 - progress * 0.45;
    orbGlow = 'rgba(138, 154, 134, 0.2)';
  } else {
    currentPhase = 'rest';
    phaseInstruction = 'Pause in Quiet';
    phaseSubtitle = 'Remain centered before the next breath';
    orbScale = 0.9;
    orbGlow = 'rgba(138, 154, 134, 0.15)';
  }

  // Play subtle bell at beginning of phases
  const prevPhaseRef = React.useRef(currentPhase);
  React.useEffect(() => {
    if (isOpen && isActive && !isCompleted && prevPhaseRef.current !== currentPhase) {
      if (currentPhase === 'inhale') playChime(432);
      if (currentPhase === 'exhale') playChime(360);
      prevPhaseRef.current = currentPhase;
    }
  }, [currentPhase, isOpen, isActive, isCompleted, playChime]);

  const handleRestart = () => {
    setSecondsLeft(60);
    setIsActive(true);
    setIsCompleted(false);
  };

  const handleToggleActive = () => {
    if (isCompleted) {
      handleRestart();
    } else {
      setIsActive((prev) => !prev);
    }
  };

  const handleProceedToStudio = () => {
    onClose();
    onStartBuilding(targetWallpaper);
  };

  // SVG Progress Ring calculation
  const ringRadius = 130;
  const circumference = 2 * Math.PI * ringRadius;
  const progressRatio = (60 - secondsLeft) / 60;
  const strokeDashoffset = circumference - progressRatio * circumference;

  if (!isOpen) return null;

  return (
    <div
      role="dialog"
      aria-modal="true"
      aria-labelledby="zen-moments-title"
      className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/75 backdrop-blur-md transition-opacity duration-300 animate-in fade-in"
    >
      <div className="relative w-full max-w-lg rounded-3xl bg-[#FAF8F5] dark:bg-[#181A19] border border-[#E8E4DC] dark:border-[#2D302F] shadow-2xl overflow-hidden p-6 sm:p-8 flex flex-col items-center text-center">
        {/* Top Navigation & Controls */}
        <div className="w-full flex items-center justify-between pb-4 border-b border-[#E8E4DC]/80 dark:border-[#282A29]">
          <div className="flex items-center gap-2 text-left">
            <span className="w-2 h-2 rounded-full bg-[#556952] dark:bg-[#8A9A86] animate-pulse" />
            <h2 id="zen-moments-title" className="font-serif text-base font-semibold text-[#2C2B29] dark:text-[#F3F4F1] tracking-tight">
              Zen Moments · 60s Mindfulness
            </h2>
          </div>

          <div className="flex items-center gap-1.5">
            <button
              onClick={() => setSoundEnabled((prev) => !prev)}
              aria-label={soundEnabled ? 'Mute mindfulness chime' : 'Enable mindfulness chime'}
              title={soundEnabled ? 'Mute chime' : 'Enable soft chime'}
              className="p-2 rounded-xl text-[#726F68] dark:text-[#939188] hover:bg-[#EFECE6] dark:hover:bg-[#252827] transition-colors cursor-pointer"
            >
              {soundEnabled ? <Volume2 className="w-4 h-4 text-[#556952] dark:text-[#BBCBB8]" /> : <VolumeX className="w-4 h-4" />}
            </button>

            <button
              onClick={onClose}
              aria-label="Close Zen Moments"
              className="p-2 rounded-xl text-[#726F68] dark:text-[#939188] hover:bg-[#EFECE6] dark:hover:bg-[#252827] transition-colors cursor-pointer"
            >
              <X className="w-4 h-4" />
            </button>
          </div>
        </div>

        {/* Breathing Visual Canvas */}
        <div className="relative w-72 h-72 sm:w-80 sm:h-80 flex items-center justify-center my-6">
          {/* Circular SVG Timer Ring */}
          <svg className="absolute inset-0 w-full h-full -rotate-90 pointer-events-none" viewBox="0 0 300 300">
            {/* Background Track */}
            <circle
              cx="150"
              cy="150"
              r={ringRadius}
              fill="none"
              stroke="currentColor"
              strokeWidth="4"
              className="text-[#EFECE6] dark:text-[#252827]"
            />
            {/* Active Progress Ring */}
            <circle
              cx="150"
              cy="150"
              r={ringRadius}
              fill="none"
              stroke="currentColor"
              strokeWidth="4"
              strokeDasharray={circumference}
              strokeDashoffset={strokeDashoffset}
              strokeLinecap="round"
              className="text-[#556952] dark:text-[#8A9A86] transition-all duration-1000 ease-linear"
            />
          </svg>

          {/* Outer Soft Ambient Glow */}
          <div
            className="absolute w-52 h-52 sm:w-56 sm:h-56 rounded-full transition-all duration-1000 ease-in-out pointer-events-none"
            style={{
              transform: `scale(${orbScale * 1.15})`,
              backgroundColor: orbGlow,
              filter: 'blur(28px)',
            }}
          />

          {/* Secondary Breathing Ripple */}
          <div
            className="absolute w-44 h-44 sm:w-48 sm:h-48 rounded-full border border-[#8A9A86]/30 dark:border-[#BBCBB8]/20 transition-all duration-1000 ease-in-out pointer-events-none"
            style={{
              transform: `scale(${orbScale * 1.05})`,
            }}
          />

          {/* Core Pulsating Breathing Orb */}
          <div
            className="relative w-36 h-36 sm:w-40 sm:h-40 rounded-full flex flex-col items-center justify-center shadow-lg transition-transform duration-1000 ease-in-out bg-gradient-to-b from-[#8A9A86]/20 via-[#EFECE6]/80 to-[#556952]/30 dark:from-[#2A302B] dark:via-[#1D211E] dark:to-[#38433A] border border-[#8A9A86]/40 dark:border-[#BBCBB8]/30"
            style={{
              transform: `scale(${orbScale})`,
            }}
          >
            {isCompleted ? (
              <CheckCircle2 className="w-10 h-10 text-[#556952] dark:text-[#BBCBB8] animate-in zoom-in duration-300" />
            ) : (
              <Wind className="w-8 h-8 text-[#556952] dark:text-[#BBCBB8] opacity-80" />
            )}

            <div className="mt-1 font-mono text-2xl font-semibold tracking-tight text-[#2C2B29] dark:text-[#F3F4F1]">
              {isCompleted ? '0s' : `${secondsLeft}s`}
            </div>
          </div>
        </div>

        {/* Phase Text & Instructions */}
        <div className="space-y-1.5 max-w-sm px-2 mb-6">
          <p className="font-serif text-xl sm:text-2xl font-medium text-[#2C2B29] dark:text-[#F3F4F1] transition-all duration-500">
            {phaseInstruction}
          </p>
          <p className="text-xs sm:text-sm text-[#726F68] dark:text-[#939188] leading-relaxed">
            {phaseSubtitle}
          </p>
          {!isCompleted && (
            <p className="pt-1 text-[11px] font-mono tracking-widest text-[#556952] dark:text-[#BBCBB8] uppercase">
              Breath {cycleIndex} of 4 · Box Rhythm
            </p>
          )}
        </div>

        {/* Action Controls */}
        <div className="w-full flex flex-col gap-3 pt-2">
          {isCompleted ? (
            <div className="space-y-2.5 w-full">
              <button
                onClick={handleProceedToStudio}
                className="w-full min-h-[48px] px-6 rounded-xl bg-[#556952] hover:bg-[#435340] text-white text-sm font-medium tracking-wide flex items-center justify-center gap-2 shadow-sm transition-all hover:scale-[1.01] active:scale-[0.99] cursor-pointer"
              >
                <Sparkles className="w-4 h-4" />
                <span>Start Building Wallpaper</span>
                <ArrowRight className="w-4 h-4 ml-1" />
              </button>

              <button
                onClick={handleRestart}
                className="w-full min-h-[44px] px-4 rounded-xl border border-[#DCD7CE] dark:border-[#383A38] hover:bg-white dark:hover:bg-[#252827] text-xs font-medium text-[#2C2B29] dark:text-[#E2E3E0] flex items-center justify-center gap-2 transition-colors cursor-pointer"
              >
                <RotateCcw className="w-3.5 h-3.5" />
                <span>Breathe Again (60s)</span>
              </button>
            </div>
          ) : (
            <div className="flex flex-col sm:flex-row items-center justify-center gap-3 w-full">
              <button
                onClick={handleToggleActive}
                className="w-full sm:w-auto flex-1 min-h-[46px] px-5 rounded-xl bg-[#2C2B29] dark:bg-[#E2E3E0] text-white dark:text-[#121413] text-xs font-medium tracking-wide flex items-center justify-center gap-2 hover:opacity-90 transition-opacity cursor-pointer shadow-xs"
              >
                {isActive ? (
                  <>
                    <Pause className="w-3.5 h-3.5" />
                    <span>Pause Exercise</span>
                  </>
                ) : (
                  <>
                    <Play className="w-3.5 h-3.5 fill-current" />
                    <span>Resume Breath</span>
                  </>
                )}
              </button>

              <button
                onClick={handleRestart}
                title="Restart 60s Timer"
                aria-label="Restart 60s breathing timer"
                className="w-full sm:w-auto min-h-[46px] px-4 rounded-xl border border-[#DCD7CE] dark:border-[#383A38] hover:bg-white dark:hover:bg-[#252827] text-xs font-medium text-[#2C2B29] dark:text-[#E2E3E0] flex items-center justify-center gap-1.5 transition-colors cursor-pointer"
              >
                <RotateCcw className="w-3.5 h-3.5" />
                <span>Reset</span>
              </button>

              <button
                onClick={handleProceedToStudio}
                className="w-full sm:w-auto min-h-[46px] px-5 rounded-xl bg-[#556952] hover:bg-[#435340] text-white text-xs font-medium tracking-wide flex items-center justify-center gap-2 shadow-sm transition-all hover:scale-[1.01] active:scale-[0.99] cursor-pointer"
              >
                <Sparkles className="w-3.5 h-3.5" />
                <span>Start Building</span>
              </button>
            </div>
          )}
        </div>
      </div>
    </div>
  );
};
