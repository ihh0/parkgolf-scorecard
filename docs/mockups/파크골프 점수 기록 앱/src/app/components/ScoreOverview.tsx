import type { Hole } from '../types';

interface SessionLike {
  venueName: string;
  courseName: string;
  holes: Hole[];
  players: string[];
  scores: number[][];
}

function ScoreCell({ score, par }: { score: number; par: number }) {
  const diff = score - par;
  return (
    <td
      className={`text-center py-3 px-2 text-base font-bold border-b border-border ${
        diff < 0
          ? 'text-primary'
          : diff === 0
          ? 'text-muted-foreground'
          : 'text-foreground'
      }`}
    >
      {score}
    </td>
  );
}

export function ScoreOverview({ session }: { session: SessionLike }) {
  const { holes, players, scores } = session;
  const totalPar = holes.reduce((s, h) => s + h.par, 0);
  const playerTotals = players.map((_, pIdx) =>
    scores[pIdx].reduce((s, v) => s + v, 0)
  );

  return (
    <div className="px-4 py-5">
      <div className="bg-card border-2 border-border rounded-2xl overflow-hidden">
        <div className="overflow-x-auto" style={{ scrollbarWidth: 'none' }}>
          <table className="w-full min-w-max border-collapse">
            <thead>
              <tr className="bg-secondary">
                <th className="text-left py-3 px-4 text-sm font-bold text-muted-foreground border-b-2 border-border sticky left-0 bg-secondary z-10 whitespace-nowrap">
                  홀
                </th>
                <th className="text-center py-3 px-3 text-sm font-bold text-muted-foreground border-b-2 border-border whitespace-nowrap">
                  파
                </th>
                {players.map((player, pIdx) => (
                  <th
                    key={pIdx}
                    className="text-center py-3 px-3 text-sm font-bold text-foreground border-b-2 border-border whitespace-nowrap min-w-[64px]"
                  >
                    {player}
                  </th>
                ))}
              </tr>
            </thead>
            <tbody>
              {holes.map((hole, hIdx) => (
                <tr key={hIdx} className={hIdx % 2 === 0 ? 'bg-card' : 'bg-background'}>
                  <td className="py-3 px-4 text-base font-bold text-foreground border-b border-border sticky left-0 bg-inherit z-10">
                    {hIdx + 1}
                  </td>
                  <td className="text-center py-3 px-2 text-base text-muted-foreground border-b border-border">
                    {hole.par}
                  </td>
                  {players.map((_, pIdx) => (
                    <ScoreCell key={pIdx} score={scores[pIdx][hIdx]} par={hole.par} />
                  ))}
                </tr>
              ))}
            </tbody>
            <tfoot>
              <tr className="bg-secondary">
                <td className="py-4 px-4 text-base font-bold text-foreground border-t-2 border-border sticky left-0 bg-secondary z-10">
                  합계
                </td>
                <td className="text-center py-4 px-2 text-base font-bold text-muted-foreground border-t-2 border-border">
                  {totalPar}
                </td>
                {playerTotals.map((total, pIdx) => {
                  const diff = total - totalPar;
                  return (
                    <td
                      key={pIdx}
                      className="text-center py-4 px-2 border-t-2 border-border"
                    >
                      <p className={`text-lg font-bold ${diff < 0 ? 'text-primary' : 'text-foreground'}`}>
                        {total}
                      </p>
                      <p className="text-xs font-semibold text-muted-foreground">
                        {diff > 0 ? `+${diff}` : diff === 0 ? 'E' : diff}
                      </p>
                    </td>
                  );
                })}
              </tr>
            </tfoot>
          </table>
        </div>
      </div>
    </div>
  );
}
