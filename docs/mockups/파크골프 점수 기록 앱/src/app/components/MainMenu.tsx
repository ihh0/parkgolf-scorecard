import { Flag, PlayCircle, ClipboardList, LayoutList, ChevronRight, Settings2 } from 'lucide-react';
import type { Screen } from '../types';

const MENU_ITEMS = [
  {
    label: '게임 시작',
    screen: 'game-start' as Screen,
    icon: PlayCircle,
    desc: '구장을 선택하고 경기를 시작하세요',
  },
  {
    label: '게임 기록',
    screen: 'game-records' as Screen,
    icon: ClipboardList,
    desc: '지난 경기 기록을 확인하세요',
  },
  {
    label: '코스 관리',
    screen: 'course-management' as Screen,
    icon: LayoutList,
    desc: '저장된 코스를 편집하거나 삭제하세요',
  },
];

export function MainMenu({ onNavigate }: { onNavigate: (screen: Screen) => void }) {
  return (
    <div className="min-h-screen flex flex-col px-5 pt-6 pb-10">
      {/* Settings button */}
      <div className="flex justify-end mb-10">
        <button
          onClick={() => onNavigate('settings')}
          className="w-12 h-12 flex items-center justify-center bg-card border-2 border-border rounded-2xl text-muted-foreground hover:text-foreground hover:bg-secondary active:scale-95 transition-all"
        >
          <Settings2 size={22} strokeWidth={1.8} />
        </button>
      </div>

      {/* App identity */}
      <div className="flex flex-col items-center gap-5 mb-14">
        <div className="w-28 h-28 rounded-3xl bg-primary flex items-center justify-center shadow-2xl shadow-primary/30">
          <Flag size={52} className="text-primary-foreground" strokeWidth={1.5} />
        </div>
        <div className="text-center">
          <h1 className="text-4xl font-bold text-foreground tracking-tight leading-tight">파크골프</h1>
          <p className="text-2xl text-muted-foreground font-medium mt-1">스코어 앱</p>
        </div>
      </div>

      {/* Menu buttons */}
      <div className="flex flex-col gap-4">
        {MENU_ITEMS.map(({ label, screen, icon: Icon, desc }) => (
          <button
            key={screen}
            onClick={() => onNavigate(screen)}
            className="flex items-center gap-5 px-5 py-5 bg-card border-2 border-border rounded-2xl text-left hover:border-primary/50 hover:bg-secondary active:scale-[0.99] transition-all shadow-sm"
          >
            <div className="w-14 h-14 rounded-2xl bg-primary/10 flex items-center justify-center shrink-0">
              <Icon size={28} className="text-primary" strokeWidth={1.5} />
            </div>
            <div className="flex-1 min-w-0">
              <p className="text-xl font-bold text-foreground mb-0.5">{label}</p>
              <p className="text-sm text-muted-foreground">{desc}</p>
            </div>
            <ChevronRight size={22} className="text-muted-foreground shrink-0" strokeWidth={2} />
          </button>
        ))}
      </div>
    </div>
  );
}
