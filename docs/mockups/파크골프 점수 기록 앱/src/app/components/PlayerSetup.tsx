import { useState, useRef, useEffect } from 'react';
import { UserPlus, Trash2, ChevronRight, MapPin } from 'lucide-react';

export function PlayerSetup({
  venueName,
  courseName,
  defaultPlayerName,
  onStart,
}: {
  venueName: string;
  courseName: string;
  defaultPlayerName: string;
  onStart: (players: string[]) => void;
}) {
  const [players, setPlayers] = useState<string[]>([defaultPlayerName || '나']);
  const lastInputRef = useRef<HTMLInputElement>(null);

  const addPlayer = () => {
    setPlayers((prev) => [...prev, `플레이어 ${prev.length + 1}`]);
  };

  const updatePlayer = (idx: number, name: string) => {
    setPlayers((prev) => prev.map((p, i) => (i === idx ? name : p)));
  };

  const deletePlayer = (idx: number) => {
    setPlayers((prev) => prev.filter((_, i) => i !== idx));
  };

  useEffect(() => {
    if (players.length > 1) {
      lastInputRef.current?.focus();
      lastInputRef.current?.select();
    }
  }, [players.length]);

  const canStart = players.every((p) => p.trim().length > 0);

  return (
    <div className="flex flex-col pb-32">
      <div className="flex flex-col gap-6 px-5 py-5">
        {/* Course info */}
        <div className="flex items-center gap-3 bg-card border-2 border-border rounded-2xl px-5 py-4">
          <div className="w-10 h-10 rounded-xl bg-primary/10 flex items-center justify-center shrink-0">
            <MapPin size={18} className="text-primary" />
          </div>
          <div>
            <p className="text-sm text-muted-foreground font-medium">{venueName}</p>
            <p className="text-lg font-bold text-foreground">{courseName}</p>
          </div>
        </div>

        <div>
          <h2 className="text-xl font-bold text-foreground mb-1">경기 인원</h2>
          <p className="text-base text-muted-foreground leading-relaxed">
            함께 경기할 인원을 구성하세요
          </p>
        </div>

        {/* Player list */}
        <div className="flex flex-col gap-3">
          {players.map((name, idx) => (
            <div key={idx} className="flex items-center gap-3">
              <div className="w-10 h-10 rounded-full bg-primary/10 flex items-center justify-center shrink-0">
                <span className="text-base font-bold text-primary">{idx + 1}</span>
              </div>
              <input
                ref={idx === players.length - 1 ? lastInputRef : undefined}
                type="text"
                value={name}
                onChange={(e) => updatePlayer(idx, e.target.value)}
                placeholder="이름 입력"
                className="flex-1 bg-input-background border-2 border-border rounded-2xl px-4 py-4 text-lg font-semibold text-foreground placeholder:text-muted-foreground/50 focus:outline-none focus:border-primary transition-all"
              />
              <button
                onClick={() => deletePlayer(idx)}
                disabled={players.length === 1}
                className="w-12 h-12 rounded-2xl flex items-center justify-center text-muted-foreground hover:text-destructive hover:bg-red-50 disabled:opacity-20 transition-all shrink-0"
              >
                <Trash2 size={20} />
              </button>
            </div>
          ))}
        </div>

        {/* Add player */}
        <button
          onClick={addPlayer}
          className="flex items-center justify-center gap-2.5 py-4 border-2 border-dashed border-primary/40 rounded-2xl text-primary text-base font-bold hover:bg-primary/5 active:scale-[0.99] transition-all"
        >
          <UserPlus size={20} strokeWidth={2} />
          인원 추가
        </button>
      </div>

      {/* Sticky bottom */}
      <div className="sticky bottom-0 bg-background/95 backdrop-blur-sm border-t-2 border-border px-5 py-4">
        <button
          onClick={() => onStart(players)}
          disabled={!canStart}
          className="w-full bg-primary text-primary-foreground text-xl font-bold py-5 rounded-2xl flex items-center justify-center gap-2 disabled:opacity-30 hover:bg-primary/90 active:scale-[0.98] transition-all shadow-md shadow-primary/20"
        >
          게임 시작
          <ChevronRight size={22} strokeWidth={2.5} />
        </button>
      </div>
    </div>
  );
}
