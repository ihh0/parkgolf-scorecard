import { ChevronRight, Clock, MapPin, Navigation, Plus } from 'lucide-react';
import type { Venue, GameRecord, Screen, Hole } from '../types';

const NEARBY_VENUES = [
  { name: '양재천 파크골프장', distance: '1.2km', courses: ['동코스', '서코스'] },
  { name: '올림픽공원 파크골프장', distance: '3.5km', courses: ['챔피언코스'] },
  { name: '보라매공원 파크골프장', distance: '4.1km', courses: ['A코스', 'B코스', 'C코스'] },
  { name: '서울숲 파크골프장', distance: '5.8km', courses: ['숲속코스'] },
];

const DEFAULT_HOLES: Hole[] = Array(9).fill(null).map(() => ({ par: 3 }));

function formatDate(dateStr: string) {
  const d = new Date(dateStr);
  return `${d.getMonth() + 1}월 ${d.getDate()}일`;
}

function SectionLabel({ icon: Icon, label }: { icon: React.ElementType; label: string }) {
  return (
    <div className="flex items-center gap-2 mb-3">
      <Icon size={16} className="text-muted-foreground" />
      <span className="text-base font-semibold text-muted-foreground">{label}</span>
    </div>
  );
}

export function GameStart({
  venues,
  gameRecords,
  onNavigate,
  onCourseSelect,
}: {
  venues: Venue[];
  gameRecords: GameRecord[];
  onNavigate: (screen: Screen) => void;
  onCourseSelect: (venueName: string, courseName: string, holes: Hole[]) => void;
}) {
  const recentRecord = gameRecords[0];

  const handleSelect = (venueName: string, courseName: string, holes: Hole[]) => {
    onCourseSelect(venueName, courseName, holes);
    onNavigate('player-setup');
  };

  return (
    <div className="flex flex-col pb-32">
      <div className="flex flex-col gap-7 px-5 py-5">

        {/* Recent venue */}
        {recentRecord && (
          <div>
            <SectionLabel icon={Clock} label="최근 사용한 구장" />
            <button
              onClick={() => {
                const venue = venues.find((v) => v.name === recentRecord.venueName);
                const course = venue?.courses.find((c) => c.name === recentRecord.courseName);
                handleSelect(
                  recentRecord.venueName,
                  recentRecord.courseName,
                  course?.holes ?? DEFAULT_HOLES
                );
              }}
              className="w-full bg-primary text-primary-foreground rounded-2xl px-5 py-5 text-left active:scale-[0.99] transition-all shadow-md shadow-primary/20 hover:bg-primary/90"
            >
              <p className="text-sm font-medium opacity-75 mb-1">
                {formatDate(recentRecord.date)} 마지막 사용
              </p>
              <p className="text-xl font-bold mb-0.5">{recentRecord.venueName}</p>
              <p className="text-base font-medium opacity-90">{recentRecord.courseName}</p>
            </button>
          </div>
        )}

        {/* My courses */}
        {venues.length > 0 && (
          <div>
            <SectionLabel icon={MapPin} label="내 코스" />
            <div className="flex flex-col gap-2">
              {venues.map((venue) => (
                <div key={venue.id} className="bg-card border-2 border-border rounded-2xl overflow-hidden">
                  <div className="px-5 pt-4 pb-2">
                    <span className="text-base font-bold text-foreground">{venue.name}</span>
                  </div>
                  <div className="flex flex-col">
                    {venue.courses.map((course, idx) => (
                      <button
                        key={course.name}
                        onClick={() => handleSelect(venue.name, course.name, course.holes)}
                        className={`flex items-center justify-between px-5 py-4 hover:bg-secondary active:bg-accent transition-colors ${
                          idx < venue.courses.length - 1 ? 'border-b border-border' : ''
                        }`}
                      >
                        <div className="flex items-baseline gap-2">
                          <span className="text-base font-semibold text-foreground">{course.name}</span>
                          <span className="text-sm text-muted-foreground">{course.holes.length}홀</span>
                        </div>
                        <ChevronRight size={18} className="text-muted-foreground" strokeWidth={2} />
                      </button>
                    ))}
                  </div>
                </div>
              ))}
            </div>
          </div>
        )}

        {/* Nearby venues */}
        <div>
          <SectionLabel icon={Navigation} label="주변 구장" />
          <div className="flex flex-col gap-2">
            {NEARBY_VENUES.map((venue) => (
              <div key={venue.name} className="bg-card border-2 border-border rounded-2xl overflow-hidden">
                <div className="flex items-center justify-between px-5 pt-4 pb-2">
                  <span className="text-base font-bold text-foreground">{venue.name}</span>
                  <span className="text-sm font-bold text-primary bg-primary/10 px-3 py-1 rounded-full">
                    {venue.distance}
                  </span>
                </div>
                <div className="flex flex-col">
                  {venue.courses.map((course, idx) => (
                    <button
                      key={course}
                      onClick={() => handleSelect(venue.name, course, DEFAULT_HOLES)}
                      className={`flex items-center justify-between px-5 py-4 hover:bg-secondary active:bg-accent transition-colors ${
                        idx < venue.courses.length - 1 ? 'border-b border-border' : ''
                      }`}
                    >
                      <span className="text-base font-semibold text-foreground">{course}</span>
                      <ChevronRight size={18} className="text-muted-foreground" strokeWidth={2} />
                    </button>
                  ))}
                </div>
              </div>
            ))}
          </div>
        </div>
      </div>

      {/* Sticky bottom button */}
      <div className="sticky bottom-0 bg-background/95 backdrop-blur-sm border-t-2 border-border px-5 py-4">
        <button
          onClick={() => onNavigate('template-create')}
          className="w-full flex items-center justify-center gap-2.5 py-5 border-2 border-dashed border-primary/50 rounded-2xl text-primary text-lg font-bold hover:bg-primary/5 active:scale-[0.99] transition-all"
        >
          <Plus size={20} strokeWidth={2.5} />
          새 코스 템플릿 작성
        </button>
      </div>
    </div>
  );
}
