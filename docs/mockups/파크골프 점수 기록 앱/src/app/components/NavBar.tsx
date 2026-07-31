import { ChevronRight } from 'lucide-react';

export function NavBar({ title, onBack }: { title: string; onBack: () => void }) {
  return (
    <div className="sticky top-0 z-10 bg-background/95 backdrop-blur-sm border-b-2 border-border">
      <div className="relative flex items-center justify-center h-16 px-3">
        <button
          onClick={onBack}
          className="absolute left-3 flex items-center gap-1 px-2 h-11 rounded-xl hover:bg-secondary active:scale-95 transition-all text-foreground"
        >
          <ChevronRight size={22} strokeWidth={2.5} className="rotate-180" />
          <span className="text-base font-semibold">뒤로</span>
        </button>
        <h1 className="text-lg font-bold text-foreground">{title}</h1>
      </div>
    </div>
  );
}
