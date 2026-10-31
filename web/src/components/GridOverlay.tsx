import React from 'react';

export type GridOverlayType = 'none' | 'ruleOfThirds' | 'goldenRatio' | 'centerCross';
export type GridOpacity = 'subtle' | 'medium' | 'high';

interface GridOverlayProps {
  type: GridOverlayType;
  opacity?: GridOpacity;
  showGoldenSpiral?: boolean;
}

export const GridOverlay: React.FC<GridOverlayProps> = ({
  type,
  opacity = 'medium',
  showGoldenSpiral = false,
}) => {
  if (type === 'none') return null;

  const alpha = opacity === 'subtle' ? 0.35 : opacity === 'high' ? 0.85 : 0.6;
  const strokeColor = `rgba(255, 255, 255, ${alpha})`;
  const shadowColor = `rgba(0, 0, 0, ${alpha * 0.9})`;

  return (
    <div className="absolute inset-0 pointer-events-none select-none z-10 overflow-hidden">
      {/* 1. RULE OF THIRDS (3x3 Grid) */}
      {type === 'ruleOfThirds' && (
        <svg
          className="w-full h-full"
          xmlns="http://www.w3.org/2000/svg"
          preserveAspectRatio="none"
          viewBox="0 0 300 300"
        >
          <defs>
            <filter id="shadow-filter" x="-20%" y="-20%" width="140%" height="140%">
              <feDropShadow dx="0" dy="0" stdDeviation="0.75" floodColor={shadowColor} />
            </filter>
          </defs>

          {/* Vertical Lines at 33.33% and 66.67% */}
          <line
            x1="100"
            y1="0"
            x2="100"
            y2="300"
            stroke={strokeColor}
            strokeWidth="0.8"
            strokeDasharray="4 2"
            filter="url(#shadow-filter)"
          />
          <line
            x1="200"
            y1="0"
            x2="200"
            y2="300"
            stroke={strokeColor}
            strokeWidth="0.8"
            strokeDasharray="4 2"
            filter="url(#shadow-filter)"
          />

          {/* Horizontal Lines at 33.33% and 66.67% */}
          <line
            x1="0"
            y1="100"
            x2="300"
            y2="100"
            stroke={strokeColor}
            strokeWidth="0.8"
            strokeDasharray="4 2"
            filter="url(#shadow-filter)"
          />
          <line
            x1="0"
            y1="200"
            x2="300"
            y2="200"
            stroke={strokeColor}
            strokeWidth="0.8"
            strokeDasharray="4 2"
            filter="url(#shadow-filter)"
          />

          {/* 4 Intersection Power Points */}
          {[
            { cx: 100, cy: 100 },
            { cx: 200, cy: 100 },
            { cx: 100, cy: 200 },
            { cx: 200, cy: 200 },
          ].map((pt, i) => (
            <g key={i} filter="url(#shadow-filter)">
              {/* Outer ring */}
              <circle
                cx={pt.cx}
                cy={pt.cy}
                r="4.5"
                fill="none"
                stroke={strokeColor}
                strokeWidth="0.8"
              />
              {/* Center point */}
              <circle cx={pt.cx} cy={pt.cy} r="1.5" fill={strokeColor} />
            </g>
          ))}
        </svg>
      )}

      {/* 2. GOLDEN RATIO (Phi Grid: 38.2% and 61.8%) */}
      {type === 'goldenRatio' && (
        <svg
          className="w-full h-full"
          xmlns="http://www.w3.org/2000/svg"
          preserveAspectRatio="none"
          viewBox="0 0 1000 1000"
        >
          <defs>
            <filter id="shadow-phi" x="-20%" y="-20%" width="140%" height="140%">
              <feDropShadow dx="0" dy="0" stdDeviation="1" floodColor={shadowColor} />
            </filter>
          </defs>

          {/* Vertical Lines at 38.2% (382) and 61.8% (618) */}
          <line
            x1="382"
            y1="0"
            x2="382"
            y2="1000"
            stroke={strokeColor}
            strokeWidth="2.5"
            strokeDasharray="8 4"
            filter="url(#shadow-phi)"
          />
          <line
            x1="618"
            y1="0"
            x2="618"
            y2="1000"
            stroke={strokeColor}
            strokeWidth="2.5"
            strokeDasharray="8 4"
            filter="url(#shadow-phi)"
          />

          {/* Horizontal Lines at 38.2% and 61.8% */}
          <line
            x1="0"
            y1="382"
            x2="1000"
            y2="382"
            stroke={strokeColor}
            strokeWidth="2.5"
            strokeDasharray="8 4"
            filter="url(#shadow-phi)"
          />
          <line
            x1="0"
            y1="618"
            x2="1000"
            y2="618"
            stroke={strokeColor}
            strokeWidth="2.5"
            strokeDasharray="8 4"
            filter="url(#shadow-phi)"
          />

          {/* 4 Golden Ratio Intersections (Diamond / Rhombus Anchors) */}
          {[
            { cx: 382, cy: 382 },
            { cx: 618, cy: 382 },
            { cx: 382, cy: 618 },
            { cx: 618, cy: 618 },
          ].map((pt, i) => (
            <g key={i} filter="url(#shadow-phi)">
              <polygon
                points={`${pt.cx},${pt.cy - 12} ${pt.cx + 12},${pt.cy} ${pt.cx},${pt.cy + 12} ${pt.cx - 12},${pt.cy}`}
                fill="none"
                stroke={strokeColor}
                strokeWidth="2"
              />
              <circle cx={pt.cx} cy={pt.cy} r="3" fill={strokeColor} />
            </g>
          ))}

          {/* Golden Spiral / Fibonacci Curve Overlay (Optional) */}
          {showGoldenSpiral && (
            <path
              d="M 1000,1000 A 1000,1000 0 0,1 0,0 A 618,618 0 0,1 618,618 A 382,382 0 0,1 236,236 A 236,236 0 0,1 472,472 A 146,146 0 0,1 326,326 A 90,90 0 0,1 416,416"
              fill="none"
              stroke={strokeColor}
              strokeWidth="2.5"
              strokeDasharray="6 3"
              filter="url(#shadow-phi)"
            />
          )}
        </svg>
      )}

      {/* 3. CENTER CROSSHAIR & HORIZON GUIDE */}
      {type === 'centerCross' && (
        <svg
          className="w-full h-full"
          xmlns="http://www.w3.org/2000/svg"
          preserveAspectRatio="none"
          viewBox="0 0 200 200"
        >
          <defs>
            <filter id="shadow-cross" x="-20%" y="-20%" width="140%" height="140%">
              <feDropShadow dx="0" dy="0" stdDeviation="0.75" floodColor={shadowColor} />
            </filter>
          </defs>

          {/* Center Vertical Axis (50%) */}
          <line
            x1="100"
            y1="0"
            x2="100"
            y2="200"
            stroke={strokeColor}
            strokeWidth="0.8"
            strokeDasharray="4 2"
            filter="url(#shadow-cross)"
          />

          {/* Center Horizontal Horizon Line (50%) */}
          <line
            x1="0"
            y1="100"
            x2="200"
            y2="100"
            stroke={strokeColor}
            strokeWidth="0.8"
            strokeDasharray="4 2"
            filter="url(#shadow-cross)"
          />

          {/* Center Focus Reticle */}
          <g filter="url(#shadow-cross)">
            <circle cx="100" cy="100" r="12" fill="none" stroke={strokeColor} strokeWidth="0.8" />
            <circle cx="100" cy="100" r="2" fill={strokeColor} />
          </g>
        </svg>
      )}

      {/* Guide Label Pill in bottom-right corner */}
      <div className="absolute bottom-2.5 right-2.5 px-2 py-0.5 rounded bg-black/60 backdrop-blur-md text-[10px] font-mono text-white/90 border border-white/10 flex items-center gap-1.5 shadow-sm">
        <span className="w-1.5 h-1.5 rounded-full bg-[#8A9A86]" />
        <span>
          {type === 'ruleOfThirds'
            ? '3×3 Rule of Thirds'
            : type === 'goldenRatio'
            ? showGoldenSpiral
              ? 'Golden Ratio + Spiral (1:1.618)'
              : 'Golden Ratio / Phi (1:1.618)'
            : 'Center & Horizon Cross'}
        </span>
      </div>
    </div>
  );
};
