import { Check, User } from 'lucide-react';
import type { ThemeKey } from '../types';
import { THEME_COLORS } from '../types';

const THEME_LABELS: Record<ThemeKey, string> = {
  green: '초록',
  blue: '파랑',
  orange: '주황',
  purple: '보라',
};

export function Settings({
  theme,
  onThemeChange,
  defaultPlayerName,
  onDefaultPlayerNameChange,
}: {
  theme: ThemeKey;
  onThemeChange: (t: ThemeKey) => void;
  defaultPlayerName: string;
  onDefaultPlayerNameChange: (name: string) => void;
}) {
  return (
    <div className="flex flex-col gap-6 px-5 py-6">

      {/* Color theme */}
      <div>
        <h2 className="text-xl font-bold text-foreground mb-1">컬러 테마</h2>
        <p className="text-base text-muted-foreground mb-4">앱 전체에 적용되는 색상을 선택하세요</p>
        <div className="flex flex-col gap-3">
          {(Object.keys(THEME_COLORS) as ThemeKey[]).map((key) => {
            const active = theme === key;
            return (
              <button
                key={key}
                onClick={() => onThemeChange(key)}
                className={`flex items-center gap-4 px-5 py-4 rounded-2xl border-2 transition-all active:scale-[0.99] ${
                  active
                    ? 'border-primary bg-primary/10'
                    : 'border-border bg-card hover:bg-secondary'
                }`}
              >
                <div
                  className="w-10 h-10 rounded-full shrink-0 shadow-sm"
                  style={{ backgroundColor: THEME_COLORS[key] }}
                />
                <span className="flex-1 text-lg font-semibold text-foreground text-left">
                  {THEME_LABELS[key]}
                </span>
                {active && (
                  <div className="w-8 h-8 rounded-full bg-primary flex items-center justify-center shrink-0">
                    <Check size={16} color="#fff" strokeWidth={3} />
                  </div>
                )}
              </button>
            );
          })}
        </div>
      </div>

      {/* Divider */}
      <div className="h-0.5 bg-border rounded-full" />

      {/* Default player name */}
      <div>
        <h2 className="text-xl font-bold text-foreground mb-1">기본 이름</h2>
        <p className="text-base text-muted-foreground mb-4">
          게임 시작 시 첫 번째 플레이어로 표시되는 이름입니다
        </p>
        <div className="flex items-center gap-3 bg-card border-2 border-border rounded-2xl px-5 py-4">
          <User size={20} className="text-muted-foreground shrink-0" />
          <input
            type="text"
            value={defaultPlayerName}
            onChange={(e) => onDefaultPlayerNameChange(e.target.value)}
            placeholder="이름을 입력하세요"
            className="flex-1 bg-transparent text-lg font-semibold text-foreground placeholder:text-muted-foreground/50 focus:outline-none"
          />
        </div>
      </div>
    </div>
  );
}
