import { useState, useEffect } from 'react';
import { NavBar } from './components/NavBar';
import { MainMenu } from './components/MainMenu';
import { Settings } from './components/Settings';
import { GameStart } from './components/GameStart';
import { GameRecords } from './components/GameRecords';
import { CourseManagement } from './components/CourseManagement';
import { TemplateCreate } from './components/TemplateCreate';
import type { EditCourseKey } from './components/TemplateCreate';
import { PlayerSetup } from './components/PlayerSetup';
import { GamePlay } from './components/GamePlay';
import { ScoreOverview } from './components/ScoreOverview';
import { GameComplete } from './components/GameComplete';
import type { ThemeKey, Screen, Venue, GameRecord, GameSession, Hole } from './types';
import { THEMES } from './types';

const SCREEN_TITLES: Partial<Record<Screen, string>> = {
  settings: '설정',
  'game-start': '게임 시작',
  'game-records': '게임 기록',
  'record-detail': '경기 결과',
  'course-management': '코스 관리',
  'template-create': '코스 템플릿 작성',
  'player-setup': '경기 인원 구성',
  'game-play': '점수 기록',
  'score-overview': '전체 점수 보기',
  'game-complete': '경기 완료',
};

const H9 = (scores: number[]): number[][] => [scores];
const P3x9 = Array(9).fill(null).map(() => ({ par: 3 }));

const INITIAL_VENUES: Venue[] = [
  {
    id: 'v1',
    name: '한강 파크골프장',
    courses: [
      { name: 'A코스', holes: P3x9 },
      { name: 'B코스', holes: P3x9 },
    ],
  },
  {
    id: 'v2',
    name: '탄천 파크골프장',
    courses: [{ name: '메인코스', holes: P3x9 }],
  },
];

const INITIAL_GAME_RECORDS: GameRecord[] = [
  {
    id: 1, date: '2026-07-28', venueName: '한강 파크골프장', courseName: 'A코스',
    score: 32, par: 27, players: ['김철수', '이영희'],
    holes: P3x9,
    scores: [[3,4,3,4,3,3,4,4,4], [3,3,4,3,3,4,3,3,4]],
  },
  {
    id: 2, date: '2026-07-25', venueName: '탄천 파크골프장', courseName: '메인코스',
    score: 28, par: 27, players: ['김철수'],
    holes: P3x9,
    scores: [[3,3,3,3,2,3,4,3,4]],
  },
  {
    id: 3, date: '2026-07-20', venueName: '한강 파크골프장', courseName: 'B코스',
    score: 30, par: 27, players: ['김철수', '박민준', '최수진'],
    holes: P3x9,
    scores: [[3,3,4,3,3,3,4,3,4], [4,4,3,4,4,3,3,4,4], [3,4,3,3,4,3,4,3,4]],
  },
  {
    id: 4, date: '2026-07-15', venueName: '양재천 파크골프장', courseName: '동코스',
    score: 27, par: 27, players: ['김철수', '이영희'],
    holes: P3x9,
    scores: [[3,3,3,3,3,3,3,3,3], [3,4,3,3,3,3,4,3,4]],
  },
  {
    id: 5, date: '2026-07-10', venueName: '올림픽공원 파크골프장', courseName: '챔피언코스',
    score: 31, par: 27, players: ['김철수'],
    holes: P3x9,
    scores: [[4,4,3,3,4,3,4,3,3]],
  },
  {
    id: 6, date: '2026-07-05', venueName: '한강 파크골프장', courseName: 'A코스',
    score: 26, par: 27, players: ['김철수', '이영희', '박민준'],
    holes: P3x9,
    scores: [[3,3,2,3,3,3,3,3,3], [3,4,3,3,3,4,3,4,3], [4,3,3,4,3,3,4,3,3]],
  },
];

// suppress unused warning for H9 helper
void H9;

export default function App() {
  const [theme, setTheme] = useState<ThemeKey>('green');
  const [defaultPlayerName, setDefaultPlayerName] = useState('나');
  const [screenStack, setScreenStack] = useState<Screen[]>(['main']);
  const [venues, setVenues] = useState<Venue[]>(INITIAL_VENUES);
  const [gameRecords, setGameRecords] = useState<GameRecord[]>(INITIAL_GAME_RECORDS);
  const [pendingCourse, setPendingCourse] = useState<{ venueName: string; courseName: string; holes: Hole[] } | null>(null);
  const [gameSession, setGameSession] = useState<GameSession | null>(null);
  const [selectedRecord, setSelectedRecord] = useState<GameRecord | null>(null);
  const [editCourse, setEditCourse] = useState<{ venueId: string; venueName: string; courseName: string; holes: Hole[] } | null>(null);

  const currentScreen = screenStack[screenStack.length - 1];
  const navigate = (screen: Screen) => setScreenStack((prev) => [...prev, screen]);

  const goBack = () => {
    if (currentScreen === 'game-play') {
      setScreenStack((prev) => {
        const idx = [...prev].lastIndexOf('game-start' as Screen);
        return idx !== -1 ? prev.slice(0, idx + 1) : ['main'];
      });
      return;
    }
    if (currentScreen === 'game-complete') {
      setScreenStack(['main']);
      setGameSession(null);
      setPendingCourse(null);
      return;
    }
    setScreenStack((prev) => (prev.length > 1 ? prev.slice(0, -1) : prev));
  };

  useEffect(() => {
    const vars = THEMES[theme];
    Object.entries(vars).forEach(([prop, value]) => {
      document.documentElement.style.setProperty(prop, value);
    });
  }, [theme]);

  const handleCourseSelect = (venueName: string, courseName: string, holes: Hole[]) => {
    setPendingCourse({ venueName, courseName, holes });
  };

  const handleGameStart = (players: string[]) => {
    if (!pendingCourse) return;
    const scores = players.map(() => pendingCourse.holes.map((h) => h.par));
    setGameSession({ ...pendingCourse, players, scores, currentHole: 0 });
    navigate('game-play');
  };

  const handleScoreChange = (playerIdx: number, holeIdx: number, score: number) => {
    setGameSession((prev) =>
      prev
        ? {
            ...prev,
            scores: prev.scores.map((row, pi) =>
              pi === playerIdx ? row.map((s, hi) => (hi === holeIdx ? score : s)) : row
            ),
          }
        : prev
    );
  };

  const handleHoleChange = (holeIdx: number) => {
    setGameSession((prev) => (prev ? { ...prev, currentHole: holeIdx } : prev));
  };

  const handleGameSave = () => {
    if (!gameSession) return;
    const totalPar = gameSession.holes.reduce((s, h) => s + h.par, 0);
    const winnerScore = Math.min(...gameSession.scores.map((row) => row.reduce((s, v) => s + v, 0)));
    const newRecord: GameRecord = {
      id: Date.now(),
      date: new Date().toISOString().split('T')[0],
      venueName: gameSession.venueName,
      courseName: gameSession.courseName,
      score: winnerScore,
      par: totalPar,
      players: gameSession.players,
      holes: gameSession.holes,
      scores: gameSession.scores,
    };
    setGameRecords((prev) => [newRecord, ...prev]);
    setGameSession(null);
    setPendingCourse(null);
    setScreenStack(['main']);
  };

  const handleTemplateSave = (venueName: string, courseName: string, holes: Hole[], editKey?: EditCourseKey) => {
    setVenues((prev) => {
      if (editKey) {
        // Remove the original course from its venue
        const withoutOld = prev
          .map((v) =>
            v.id === editKey.venueId
              ? { ...v, courses: v.courses.filter((c) => c.name !== editKey.originalCourseName) }
              : v
          )
          .filter((v) => v.courses.length > 0);
        // Add updated course to matching venue (by name) or create new
        const target = withoutOld.find((v) => v.name === venueName);
        if (target) {
          return withoutOld.map((v) =>
            v.name === venueName ? { ...v, courses: [...v.courses, { name: courseName, holes }] } : v
          );
        }
        return [...withoutOld, { id: `v${Date.now()}`, name: venueName, courses: [{ name: courseName, holes }] }];
      }
      // New course
      const existing = prev.find((v) => v.name === venueName);
      if (existing) {
        return prev.map((v) =>
          v.name === venueName ? { ...v, courses: [...v.courses, { name: courseName, holes }] } : v
        );
      }
      return [...prev, { id: `v${Date.now()}`, name: venueName, courses: [{ name: courseName, holes }] }];
    });
    setEditCourse(null);
  };

  const handleEditCourse = (venueId: string, courseName: string) => {
    const venue = venues.find((v) => v.id === venueId);
    const course = venue?.courses.find((c) => c.name === courseName);
    if (!venue || !course) return;
    setEditCourse({ venueId, venueName: venue.name, courseName, holes: course.holes });
    navigate('template-create');
  };

  return (
    <div className="min-h-screen bg-background" style={{ fontFamily: "'Noto Sans KR', sans-serif" }}>
      <div className="w-full max-w-md mx-auto min-h-screen bg-background">
        {currentScreen !== 'main' && (
          <NavBar title={SCREEN_TITLES[currentScreen] ?? ''} onBack={goBack} />
        )}

        {currentScreen === 'main' && <MainMenu onNavigate={navigate} />}

        {currentScreen === 'settings' && (
          <Settings
            theme={theme}
            onThemeChange={setTheme}
            defaultPlayerName={defaultPlayerName}
            onDefaultPlayerNameChange={setDefaultPlayerName}
          />
        )}

        {currentScreen === 'game-start' && (
          <GameStart
            venues={venues}
            gameRecords={gameRecords}
            onNavigate={navigate}
            onCourseSelect={handleCourseSelect}
          />
        )}

        {currentScreen === 'game-records' && (
          <GameRecords
            gameRecords={gameRecords}
            onViewDetail={(record) => {
              setSelectedRecord(record);
              navigate('record-detail');
            }}
            onDelete={(id) => setGameRecords((prev) => prev.filter((r) => r.id !== id))}
          />
        )}

        {currentScreen === 'record-detail' && selectedRecord?.holes && selectedRecord?.scores && (
          <ScoreOverview
            session={{
              venueName: selectedRecord.venueName,
              courseName: selectedRecord.courseName,
              holes: selectedRecord.holes,
              players: selectedRecord.players,
              scores: selectedRecord.scores,
            }}
          />
        )}

        {currentScreen === 'course-management' && (
          <CourseManagement
            venues={venues}
            onVenuesChange={setVenues}
            onNavigate={navigate}
            onEditCourse={handleEditCourse}
          />
        )}

        {currentScreen === 'template-create' && (
          <TemplateCreate
            existingVenues={venues}
            onSave={handleTemplateSave}
            onExit={goBack}
            editCourse={editCourse ?? undefined}
          />
        )}

        {currentScreen === 'player-setup' && pendingCourse && (
          <PlayerSetup
            venueName={pendingCourse.venueName}
            courseName={pendingCourse.courseName}
            defaultPlayerName={defaultPlayerName}
            onStart={handleGameStart}
          />
        )}

        {currentScreen === 'game-play' && gameSession && (
          <GamePlay
            session={gameSession}
            onScoreChange={handleScoreChange}
            onHoleChange={handleHoleChange}
            onComplete={() => navigate('game-complete')}
            onViewAll={() => navigate('score-overview')}
          />
        )}

        {currentScreen === 'score-overview' && gameSession && (
          <ScoreOverview session={gameSession} />
        )}

        {currentScreen === 'game-complete' && gameSession && (
          <GameComplete session={gameSession} onSave={handleGameSave} />
        )}
      </div>
    </div>
  );
}
