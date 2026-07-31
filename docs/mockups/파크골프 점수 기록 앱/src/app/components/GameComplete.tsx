import { Trophy, Medal, Save } from 'lucide-react';
import type { GameSession } from '../types';

const RANK_COLORS = ['#F5A623', '#A8A8A8', '#CD7F32'];
const RANK_LABELS = ['1위', '2위', '3위'];

export function GameComplete({
  session,
  onSave,
}: {
  session: GameSession;
  onSave: () => void;
}) {
  const { holes, players, scores, venueName, courseName } = session;
  const totalPar = holes.reduce((s, h) => s + h.par, 0);

  const ranked = players
    .map((name, idx) => ({
      name,
      total: scores[idx].reduce((s, v) => s + v, 0),
      idx,
    }))
    .sort((a, b) => a.total - b.total);

  // Assign ranks (ties share the same rank)
  let rank = 1;
  const withRanks = ranked.map((p, i) => {
    if (i > 0 && p.total !== ranked[i - 1].total) rank = i + 1;
    return { ...p, rank };
  });

  return (
    <div className="flex flex-col pb-32">
      <div className="flex flex-col gap-6 px-5 py-5">
        {/* Header */}
        <div className="flex flex-col items-center gap-3 py-4">
          <div className="w-20 h-20 rounded-full bg-primary/15 border-4 border-primary/30 flex items-center justify-center">
            <Trophy size={38} className="text-primary" strokeWidth={1.5} />
          </div>
          <div className="text-center">
            <h2 className="text-2xl font-bold text-foreground mb-0.5">경기 완료</h2>
            <p className="text-base text-muted-foreground">
              {venueName} · {courseName}
            </p>
          </div>
        </div>

        {/* Rankings */}
        <div className="flex flex-col gap-3">
          {withRanks.map(({ name, total, rank: r }, i) => {
            const diff = total - totalPar;
            const isTop3 = r <= 3;
            return (
              <div
                key={i}
                className={`flex items-center gap-4 px-5 py-5 rounded-2xl border-2 ${
                  r === 1
                    ? 'bg-primary/10 border-primary/30'
                    : 'bg-card border-border'
                }`}
              >
                {/* Rank badge */}
                <div
                  className="w-12 h-12 rounded-full flex items-center justify-center shrink-0 font-bold text-lg"
                  style={
                    isTop3
                      ? { backgroundColor: `${RANK_COLORS[r - 1]}20`, color: RANK_COLORS[r - 1] }
                      : { backgroundColor: 'var(--muted)', color: 'var(--muted-foreground)' }
                  }
                >
                  {isTop3 ? <Medal size={22} /> : `${r}`}
                </div>

                {/* Player info */}
                <div className="flex-1 min-w-0">
                  <div className="flex items-center gap-2">
                    {isTop3 && (
                      <span
                        className="text-sm font-bold"
                        style={{ color: RANK_COLORS[r - 1] }}
                      >
                        {RANK_LABELS[r - 1]}
                      </span>
                    )}
                    <p className="text-xl font-bold text-foreground truncate">{name}</p>
                  </div>
                  <p className="text-sm text-muted-foreground font-medium">
                    파{totalPar} 대비{' '}
                    <span className={diff < 0 ? 'text-primary font-bold' : ''}>
                      {diff > 0 ? `+${diff}` : diff === 0 ? '이븐' : diff}
                    </span>
                  </p>
                </div>

                {/* Score */}
                <div className="flex flex-col items-end shrink-0">
                  <span className="text-4xl font-bold text-foreground leading-none">{total}</span>
                  <span className="text-sm text-muted-foreground mt-0.5">타</span>
                </div>
              </div>
            );
          })}
        </div>
      </div>

      {/* Sticky bottom */}
      <div className="sticky bottom-0 bg-background/95 backdrop-blur-sm border-t-2 border-border px-5 py-4">
        <button
          onClick={onSave}
          className="w-full flex items-center justify-center gap-2.5 bg-primary text-primary-foreground text-xl font-bold py-5 rounded-2xl hover:bg-primary/90 active:scale-[0.98] transition-all shadow-lg shadow-primary/25"
        >
          <Save size={22} strokeWidth={2} />
          저장하고 홈으로
        </button>
      </div>
    </div>
  );
}
