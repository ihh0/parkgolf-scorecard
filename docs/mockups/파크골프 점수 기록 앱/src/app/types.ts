export type ThemeKey = 'green' | 'blue' | 'orange' | 'purple';
export type Screen =
  | 'main'
  | 'settings'
  | 'game-start'
  | 'game-records'
  | 'record-detail'
  | 'course-management'
  | 'template-create'
  | 'player-setup'
  | 'game-play'
  | 'score-overview'
  | 'game-complete';

export type Hole = { par: number };
export type Course = { name: string; holes: Hole[] };
export type Venue = { id: string; name: string; courses: Course[] };
export type GameRecord = {
  id: number;
  date: string;
  venueName: string;
  courseName: string;
  score: number;
  par: number;
  players: string[];
  holes?: Hole[];
  scores?: number[][];
};

export type GameSession = {
  venueName: string;
  courseName: string;
  holes: Hole[];
  players: string[];
  scores: number[][]; // scores[playerIdx][holeIdx]
  currentHole: number; // 0-indexed
};

export const THEMES: Record<ThemeKey, Record<string, string>> = {
  green: {
    '--background': '#f4f9f5',
    '--foreground': '#1a2e1d',
    '--card': '#ffffff',
    '--card-foreground': '#1a2e1d',
    '--primary': '#2a7a3b',
    '--primary-foreground': '#ffffff',
    '--secondary': '#e6f2e8',
    '--secondary-foreground': '#1a2e1d',
    '--muted': '#eaf3ec',
    '--muted-foreground': '#4e7255',
    '--accent': '#d2ebda',
    '--accent-foreground': '#1a3d21',
    '--border': 'rgba(42,122,59,0.2)',
    '--input-background': '#eaf3ec',
    '--ring': '#2a7a3b',
  },
  blue: {
    '--background': '#f0f6fb',
    '--foreground': '#1a2535',
    '--card': '#ffffff',
    '--card-foreground': '#1a2535',
    '--primary': '#1a6fa4',
    '--primary-foreground': '#ffffff',
    '--secondary': '#e0eff9',
    '--secondary-foreground': '#1a2535',
    '--muted': '#e8f3fb',
    '--muted-foreground': '#3d6a8a',
    '--accent': '#c8e4f5',
    '--accent-foreground': '#0d3a5c',
    '--border': 'rgba(26,111,164,0.2)',
    '--input-background': '#e8f3fb',
    '--ring': '#1a6fa4',
  },
  orange: {
    '--background': '#fdf6f0',
    '--foreground': '#2d1a0e',
    '--card': '#ffffff',
    '--card-foreground': '#2d1a0e',
    '--primary': '#c2601a',
    '--primary-foreground': '#ffffff',
    '--secondary': '#faeade',
    '--secondary-foreground': '#2d1a0e',
    '--muted': '#fdf0e4',
    '--muted-foreground': '#8a5030',
    '--accent': '#f5d9bc',
    '--accent-foreground': '#6b2d08',
    '--border': 'rgba(194,96,26,0.2)',
    '--input-background': '#fdf0e4',
    '--ring': '#c2601a',
  },
  purple: {
    '--background': '#f7f4fc',
    '--foreground': '#1e1530',
    '--card': '#ffffff',
    '--card-foreground': '#1e1530',
    '--primary': '#6b3fa0',
    '--primary-foreground': '#ffffff',
    '--secondary': '#ede7f6',
    '--secondary-foreground': '#1e1530',
    '--muted': '#f0ebfa',
    '--muted-foreground': '#6b5490',
    '--accent': '#ddd0f0',
    '--accent-foreground': '#3a1a6b',
    '--border': 'rgba(107,63,160,0.2)',
    '--input-background': '#f0ebfa',
    '--ring': '#6b3fa0',
  },
};

export const THEME_COLORS: Record<ThemeKey, string> = {
  green: '#2a7a3b',
  blue: '#1a6fa4',
  orange: '#c2601a',
  purple: '#6b3fa0',
};
