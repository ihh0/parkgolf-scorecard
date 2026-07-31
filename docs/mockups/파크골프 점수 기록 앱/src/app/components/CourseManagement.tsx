import { Pencil, Trash2, ChevronDown, ChevronUp, MapPin, Plus } from 'lucide-react';
import { useState } from 'react';
import type { Venue, Screen } from '../types';

export function CourseManagement({
  venues,
  onVenuesChange,
  onNavigate,
  onEditCourse,
}: {
  venues: Venue[];
  onVenuesChange: (v: Venue[]) => void;
  onNavigate: (screen: Screen) => void;
  onEditCourse: (venueId: string, courseName: string) => void;
}) {
  const [expanded, setExpanded] = useState<Set<string>>(
    () => new Set(venues.map((v) => v.id))
  );

  const toggle = (id: string) =>
    setExpanded((prev) => {
      const next = new Set(prev);
      next.has(id) ? next.delete(id) : next.add(id);
      return next;
    });

  const deleteCourse = (venueId: string, courseName: string) => {
    const updated = venues
      .map((v) =>
        v.id === venueId
          ? { ...v, courses: v.courses.filter((c) => c.name !== courseName) }
          : v
      )
      .filter((v) => v.courses.length > 0);
    onVenuesChange(updated);
  };

  const deleteVenue = (venueId: string) =>
    onVenuesChange(venues.filter((v) => v.id !== venueId));

  if (venues.length === 0) {
    return (
      <div className="flex flex-col items-center justify-center gap-5 px-5 py-24 text-center">
        <div className="w-20 h-20 rounded-full bg-muted flex items-center justify-center">
          <MapPin size={36} className="text-muted-foreground" strokeWidth={1.5} />
        </div>
        <div>
          <p className="text-xl font-bold text-foreground mb-1">저장된 코스 없음</p>
          <p className="text-base text-muted-foreground">코스 템플릿을 작성해 추가하세요</p>
        </div>
        <button
          onClick={() => onNavigate('template-create')}
          className="flex items-center gap-2 bg-primary text-primary-foreground px-6 py-4 rounded-2xl text-lg font-bold hover:bg-primary/90 active:scale-[0.99] transition-all"
        >
          <Plus size={20} strokeWidth={2.5} />
          코스 템플릿 작성
        </button>
      </div>
    );
  }

  return (
    <div className="flex flex-col gap-4 px-5 py-5">
      {venues.map((venue) => {
        const isExpanded = expanded.has(venue.id);
        return (
          <div key={venue.id} className="bg-card border-2 border-border rounded-2xl overflow-hidden">
            {/* Venue header */}
            <div className="flex items-center gap-2 px-4 py-4">
              <button
                onClick={() => toggle(venue.id)}
                className="flex-1 flex items-center gap-3 text-left"
              >
                <div className="w-11 h-11 rounded-xl bg-primary/10 flex items-center justify-center shrink-0">
                  <MapPin size={20} className="text-primary" />
                </div>
                <div>
                  <p className="text-lg font-bold text-foreground">{venue.name}</p>
                  <p className="text-sm text-muted-foreground">{venue.courses.length}개 코스</p>
                </div>
              </button>
              <button
                onClick={() => deleteVenue(venue.id)}
                className="w-11 h-11 flex items-center justify-center rounded-xl text-muted-foreground hover:text-destructive hover:bg-red-50 transition-all"
              >
                <Trash2 size={19} />
              </button>
              <button
                onClick={() => toggle(venue.id)}
                className="w-11 h-11 flex items-center justify-center rounded-xl text-muted-foreground hover:bg-secondary transition-all"
              >
                {isExpanded ? <ChevronUp size={21} /> : <ChevronDown size={21} />}
              </button>
            </div>

            {/* Courses */}
            {isExpanded && (
              <div className="border-t-2 border-border">
                {venue.courses.map((course, idx) => (
                  <div
                    key={course.name}
                    className={`flex items-center gap-2 px-4 py-4 ${
                      idx < venue.courses.length - 1 ? 'border-b border-border' : ''
                    }`}
                  >
                    <div className="flex-1 pl-2">
                      <p className="text-base font-semibold text-foreground">{course.name}</p>
                      <p className="text-sm text-muted-foreground">
                        {course.holes.length}홀 · 파{course.holes.reduce((s, h) => s + h.par, 0)}
                      </p>
                    </div>
                    <button
                      onClick={() => onEditCourse(venue.id, course.name)}
                      className="w-11 h-11 flex items-center justify-center rounded-xl text-muted-foreground hover:text-primary hover:bg-primary/10 transition-all"
                    >
                      <Pencil size={17} />
                    </button>
                    <button
                      onClick={() => deleteCourse(venue.id, course.name)}
                      className="w-11 h-11 flex items-center justify-center rounded-xl text-muted-foreground hover:text-destructive hover:bg-red-50 transition-all"
                    >
                      <Trash2 size={17} />
                    </button>
                  </div>
                ))}
              </div>
            )}
          </div>
        );
      })}

      <button
        onClick={() => onNavigate('template-create')}
        className="flex items-center justify-center gap-2.5 py-5 border-2 border-dashed border-primary/40 rounded-2xl text-primary text-lg font-bold hover:bg-primary/5 active:scale-[0.99] transition-all"
      >
        <Plus size={20} strokeWidth={2.5} />
        새 코스 템플릿 작성
      </button>
    </div>
  );
}
