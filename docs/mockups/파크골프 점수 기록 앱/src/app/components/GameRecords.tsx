import { Trophy, Users, TableProperties, Trash2 } from 'lucide-react';
import type { GameRecord } from '../types';

function formatDate(dateStr: string) {
  const d = new Date(dateStr);
  const days = ['일', '월', '화', '수', '목', '금', '토'];
  return `${d.getMonth() + 1}월 ${d.getDate()}일 (${days[d.getDay()]})`;
}

function ScoreBadge({ score, par }: { score: number; par: number }) {
  const diff = score - par;
  if (diff < 0)
    return (
      <span className="text-sm font-bold text-primary bg-primary/10 px-2.5 py-1 rounded-full">
        {diff}
      </span>
    );
  if (diff === 0)
    return (
      <span className="text-sm font-bold text-muted-foreground bg-muted px-2.5 py-1 rounded-full">
        E
      </span>
    );
  return (
    <span className="text-sm font-bold text-foreground bg-secondary px-2.5 py-1 rounded-full">
      +{diff}
    </span>
  );
}

export function GameRecords({
  gameRecords,
  onViewDetail,
  onDelete,
}: {
  gameRecords: GameRecord[];
  onViewDetail: (record: GameRecord) => void;
  onDelete: (id: number) => void;
}) {
  if (gameRecords.length === 0) {
    return (
      <div className="flex flex-col items-center justify-center gap-4 px-5 py-24 text-center">
        <div className="w-20 h-20 rounded-full bg-muted flex items-center justify-center">
          <Trophy size={36} className="text-muted-foreground" strokeWidth={1.5} />
        </div>
        <div>
          <p className="text-xl font-bold text-foreground mb-1">기록 없음</p>
          <p className="text-base text-muted-foreground">게임을 완료하면 여기에 기록됩니다</p>
        </div>
      </div>
    );
  }

  return (
    <div className="flex flex-col gap-3 px-5 py-5">
      {gameRecords.map((record) => (
        <div
          key={record.id}
          className="bg-card border-2 border-border rounded-2xl overflow-hidden"
        >
          {/* Main info row */}
          <div className="flex items-start gap-4 px-5 pt-5 pb-4">
            <div className="flex-1 min-w-0">
              <p className="text-sm text-muted-foreground font-medium mb-0.5">
                {formatDate(record.date)}
              </p>
              <p className="text-xl font-bold text-foreground truncate">{record.venueName}</p>
              <p className="text-base text-muted-foreground font-medium">{record.courseName}</p>
            </div>
            <div className="flex flex-col items-end shrink-0">
              <span className="text-4xl font-bold text-foreground leading-none">{record.score}</span>
              <div className="flex items-center gap-1.5 mt-1">
                <span className="text-sm text-muted-foreground">파{record.par}</span>
                <ScoreBadge score={record.score} par={record.par} />
              </div>
            </div>
          </div>

          {/* Players row */}
          <div className="flex items-center gap-2 px-5 pb-4">
            <Users size={15} className="text-muted-foreground shrink-0" />
            <span className="text-sm text-muted-foreground font-medium truncate">
              {record.players.join(', ')}
            </span>
          </div>

          {/* Action buttons */}
          <div className="flex border-t-2 border-border">
            <button
              onClick={() => onViewDetail(record)}
              disabled={!record.scores || !record.holes}
              className="flex-1 flex items-center justify-center gap-2 py-4 text-base font-semibold text-muted-foreground hover:text-primary hover:bg-primary/5 disabled:opacity-30 transition-all"
            >
              <TableProperties size={17} />
              경기 결과 보기
            </button>
            <div className="w-0.5 bg-border" />
            <button
              onClick={() => onDelete(record.id)}
              className="flex items-center justify-center gap-2 px-6 py-4 text-base font-semibold text-muted-foreground hover:text-destructive hover:bg-red-50 transition-all"
            >
              <Trash2 size={17} />
            </button>
          </div>
        </div>
      ))}
    </div>
  );
}
