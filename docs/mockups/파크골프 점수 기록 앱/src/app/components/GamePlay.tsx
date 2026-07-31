import { Minus, Plus, ChevronLeft, ChevronRight, TableProperties, Flag } from 'lucide-react';
import type { GameSession } from '../types';

function ScoreDiffBadge({ score, par }: { score: number; par: number }) {
  const diff = score - par;
  if (diff === 0)
    return <span className="text-xs font-bold text-muted-foreground bg-muted px-2 py-0.5 rounded-full">E</span>;
  if (diff < 0)
    return <span className="text-xs font-bold text-primary bg-primary/15 px-2 py-0.5 rounded-full">{diff}</span>;
  return <span className="text-xs font-semibold text-muted-foreground bg-secondary px-2 py-0.5 rounded-full">+{diff}</span>;
}

export function GamePlay({
  session,
  onScoreChange,
  onHoleChange,
  onComplete,
  onViewAll,
}: {
  session: GameSession;
  onScoreChange: (playerIdx: number, holeIdx: number, score: number) => void;
  onHoleChange: (holeIdx: number) => void;
  onComplete: () => void;
  onViewAll: () => void;
}) {
  const { holes, players, scores, currentHole, venueName, courseName } = session;
  const hole = holes[currentHole];
  const isFirst = currentHole === 0;
  const isLast = currentHole === holes.length - 1;
  const totalHoles = holes.length;

  const runningTotal = (playerIdx: number) =>
    scores[playerIdx].slice(0, currentHole + 1).reduce((s, v) => s + v, 0);

  return (
    <div className="flex flex-col pb-44">
      {/* Course + hole header */}
      <div className="px-5 pt-5 pb-4 border-b-2 border-border bg-card">
        <div className="flex items-center gap-2 mb-2">
          <span className="text-sm font-bold text-primary bg-primary/10 px-3 py-1 rounded-full">{venueName}</span>
          <span className="text-sm text-muted-foreground">/</span>
          <span className="text-sm font-semibold text-foreground">{courseName}</span>
        </div>
        <div className="flex items-center justify-between">
          <div className="flex items-baseline gap-3">
            <h2 className="text-3xl font-bold text-foreground">{currentHole + 1}번 홀</h2>
            <div className="flex items-center gap-1.5">
              <Flag size={15} className="text-muted-foreground" />
              <span className="text-lg font-semibold text-muted-foreground">파{hole.par}</span>
            </div>
          </div>
          <span className="text-base font-semibold text-muted-foreground">
            {currentHole + 1} / {totalHoles}
          </span>
        </div>

        {/* Hole progress dots */}
        <div className="flex gap-1.5 mt-3 flex-wrap">
          {holes.map((_, idx) => (
            <button
              key={idx}
              onClick={() => onHoleChange(idx)}
              className={`h-2.5 rounded-full transition-all ${
                idx === currentHole
                  ? 'bg-primary w-6'
                  : idx < currentHole
                  ? 'bg-primary/40 w-2.5'
                  : 'bg-border w-2.5'
              }`}
            />
          ))}
        </div>
      </div>

      {/* Player score rows */}
      <div className="flex flex-col gap-3 px-5 py-5">
        {players.map((player, pIdx) => {
          const score = scores[pIdx][currentHole];
          return (
            <div key={pIdx} className="bg-card border-2 border-border rounded-2xl px-5 py-4">
              <div className="flex items-center justify-between">
                <div>
                  <p className="text-lg font-bold text-foreground">{player}</p>
                  <p className="text-sm text-muted-foreground font-medium mt-0.5">
                    누적 {runningTotal(pIdx)}타
                  </p>
                </div>
                <div className="flex items-center gap-3">
                  <button
                    onClick={() => onScoreChange(pIdx, currentHole, Math.max(1, score - 1))}
                    className="w-12 h-12 rounded-2xl bg-secondary border-2 border-border flex items-center justify-center hover:bg-accent active:scale-95 transition-all"
                  >
                    <Minus size={20} strokeWidth={2.5} />
                  </button>
                  <div className="flex flex-col items-center w-12">
                    <span className="text-3xl font-bold text-foreground leading-none">{score}</span>
                    <div className="mt-1">
                      <ScoreDiffBadge score={score} par={hole.par} />
                    </div>
                  </div>
                  <button
                    onClick={() => onScoreChange(pIdx, currentHole, Math.min(9, score + 1))}
                    className="w-12 h-12 rounded-2xl bg-secondary border-2 border-border flex items-center justify-center hover:bg-accent active:scale-95 transition-all"
                  >
                    <Plus size={20} strokeWidth={2.5} />
                  </button>
                </div>
              </div>
            </div>
          );
        })}
      </div>

      {/* Sticky bottom */}
      <div className="sticky bottom-0 bg-background/95 backdrop-blur-sm border-t-2 border-border px-5 py-4">
        <div className="flex gap-3 mb-3">
          <button
            onClick={() => onHoleChange(currentHole - 1)}
            disabled={isFirst}
            className="flex-1 flex items-center justify-center gap-1.5 py-5 rounded-2xl border-2 border-border text-foreground text-lg font-bold disabled:opacity-25 hover:bg-secondary active:scale-[0.98] transition-all"
          >
            <ChevronLeft size={20} strokeWidth={2.5} />
            이전 홀
          </button>
          {isLast ? (
            <button
              onClick={onComplete}
              className="flex-[2] py-5 rounded-2xl bg-primary text-primary-foreground text-lg font-bold hover:bg-primary/90 active:scale-[0.98] transition-all shadow-md shadow-primary/20"
            >
              게임 완료
            </button>
          ) : (
            <button
              onClick={() => onHoleChange(currentHole + 1)}
              className="flex-[2] flex items-center justify-center gap-1.5 py-5 rounded-2xl bg-primary text-primary-foreground text-lg font-bold hover:bg-primary/90 active:scale-[0.98] transition-all shadow-md shadow-primary/20"
            >
              다음 홀
              <ChevronRight size={20} strokeWidth={2.5} />
            </button>
          )}
        </div>
        <button
          onClick={onViewAll}
          className="w-full flex items-center justify-center gap-2 py-4 rounded-2xl border-2 border-border text-base font-bold text-muted-foreground hover:text-foreground hover:bg-secondary active:scale-[0.99] transition-all"
        >
          <TableProperties size={18} />
          전체 점수 보기
        </button>
      </div>
    </div>
  );
}
