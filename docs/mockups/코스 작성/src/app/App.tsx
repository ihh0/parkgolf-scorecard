import { useState } from "react";
import { Plus, Minus, Trash2, ChevronRight, Check, MapPin, Flag, ListOrdered, Save, AlertCircle } from "lucide-react";

type Hole = { par: number };

type Template = {
  venueName: string;
  courseName: string;
  holes: Hole[];
};

const SAVED_TEMPLATES: Template[] = [
  { venueName: "한강 파크골프장", courseName: "A코스", holes: Array(9).fill({ par: 3 }) },
  { venueName: "한강 파크골프장", courseName: "B코스", holes: Array(9).fill({ par: 3 }) },
  { venueName: "탄천 파크골프장", courseName: "메인코스", holes: Array(9).fill({ par: 3 }) },
];

const STEPS = [
  { id: 1, label: "구장명", icon: MapPin },
  { id: 2, label: "코스명", icon: Flag },
  { id: 3, label: "홀 설정", icon: ListOrdered },
  { id: 4, label: "저장", icon: Save },
];

function StepIndicator({ current }: { current: number }) {
  return (
    <div className="flex items-center justify-center gap-0 px-4 pt-5 pb-3">
      {STEPS.map((step, idx) => {
        const done = current > step.id;
        const active = current === step.id;
        const Icon = step.icon;
        return (
          <div key={step.id} className="flex items-center">
            <div className="flex flex-col items-center gap-1.5">
              <div
                className={`w-12 h-12 rounded-full flex items-center justify-center border-2 transition-all duration-200 ${
                  done
                    ? "bg-primary border-primary text-primary-foreground"
                    : active
                    ? "bg-primary/10 border-primary text-primary"
                    : "bg-white border-border text-muted-foreground"
                }`}
              >
                {done ? <Check size={20} strokeWidth={2.5} /> : <Icon size={18} strokeWidth={active ? 2.2 : 1.5} />}
              </div>
              <span
                className={`text-xs font-semibold tracking-wide ${
                  active ? "text-primary" : done ? "text-primary/70" : "text-muted-foreground"
                }`}
              >
                {step.label}
              </span>
            </div>
            {idx < STEPS.length - 1 && (
              <div
                className={`w-8 h-0.5 mb-5 mx-1 rounded-full transition-colors duration-300 ${
                  current > step.id ? "bg-primary/50" : "bg-border"
                }`}
              />
            )}
          </div>
        );
      })}
    </div>
  );
}

function Step1({
  venueName,
  onChange,
  onNext,
}: {
  venueName: string;
  onChange: (v: string) => void;
  onNext: () => void;
}) {
  const existingVenues = [...new Set(SAVED_TEMPLATES.map((t) => t.venueName))];
  const matched = existingVenues.filter(
    (v) => v.includes(venueName) && venueName.length > 0
  );
  const isExisting = existingVenues.includes(venueName);

  return (
    <div className="flex flex-col gap-6 px-5 py-5">
      <div>
        <h2 className="text-xl font-bold text-foreground mb-1">구장 이름</h2>
        <p className="text-base text-muted-foreground leading-relaxed">
          동일한 이름의 구장은 게임 시작 화면에서 통합 표시됩니다
        </p>
      </div>

      <div className="relative">
        <input
          type="text"
          value={venueName}
          onChange={(e) => onChange(e.target.value)}
          placeholder="예: 한강 파크골프장"
          className="w-full bg-input-background border-2 border-border rounded-2xl px-5 py-4 text-lg text-foreground placeholder:text-muted-foreground/50 focus:outline-none focus:border-primary transition-all"
        />
        {isExisting && (
          <div className="absolute right-4 top-1/2 -translate-y-1/2 flex items-center gap-1.5 text-primary text-sm font-semibold">
            <Check size={16} strokeWidth={2.5} />
            기존 구장
          </div>
        )}
      </div>

      {matched.length > 0 && !isExisting && (
        <div className="flex flex-col gap-2">
          <p className="text-sm text-muted-foreground font-semibold px-1">저장된 구장</p>
          {matched.map((v) => (
            <button
              key={v}
              onClick={() => onChange(v)}
              className="flex items-center gap-4 px-5 py-4 bg-white rounded-2xl border-2 border-border text-left hover:border-primary/50 hover:bg-secondary active:scale-[0.99] transition-all"
            >
              <MapPin size={18} className="text-primary shrink-0" />
              <span className="text-base font-semibold text-foreground">{v}</span>
            </button>
          ))}
        </div>
      )}

      {isExisting && (
        <div className="flex items-start gap-3 bg-primary/10 border-2 border-primary/25 rounded-2xl px-5 py-4">
          <AlertCircle size={20} className="text-primary shrink-0 mt-0.5" />
          <p className="text-base text-primary font-medium leading-relaxed">
            이미 저장된 구장입니다. 코스를 추가하면 해당 구장에 통합됩니다.
          </p>
        </div>
      )}

      <div className="mt-1">
        <button
          onClick={onNext}
          disabled={!venueName.trim()}
          className="w-full bg-primary text-primary-foreground text-lg font-bold py-5 rounded-2xl flex items-center justify-center gap-2 disabled:opacity-30 hover:bg-primary/90 active:scale-[0.98] transition-all shadow-md shadow-primary/20"
        >
          다음
          <ChevronRight size={20} strokeWidth={2.5} />
        </button>
      </div>
    </div>
  );
}

function Step2({
  venueName,
  courseName,
  onChange,
  onNext,
  onBack,
}: {
  venueName: string;
  courseName: string;
  onChange: (v: string) => void;
  onNext: () => void;
  onBack: () => void;
}) {
  const existingCourses = SAVED_TEMPLATES.filter((t) => t.venueName === venueName).map((t) => t.courseName);
  const isDuplicate = existingCourses.includes(courseName.trim());

  return (
    <div className="flex flex-col gap-6 px-5 py-5">
      <div>
        <div className="flex items-center gap-2 mb-2">
          <span className="text-sm font-bold text-primary bg-primary/10 px-3 py-1.5 rounded-full">{venueName}</span>
        </div>
        <h2 className="text-xl font-bold text-foreground mb-1">코스 이름</h2>
        <p className="text-base text-muted-foreground leading-relaxed">같은 구장 내 중복된 코스명은 사용할 수 없습니다</p>
      </div>

      <div>
        <input
          type="text"
          value={courseName}
          onChange={(e) => onChange(e.target.value)}
          placeholder="예: A코스, 챔피언코스"
          className={`w-full bg-input-background border-2 rounded-2xl px-5 py-4 text-lg text-foreground placeholder:text-muted-foreground/50 focus:outline-none transition-all ${
            isDuplicate
              ? "border-destructive focus:border-destructive"
              : "border-border focus:border-primary"
          }`}
        />
        {isDuplicate && (
          <div className="flex items-center gap-2 mt-2.5 px-1">
            <AlertCircle size={17} className="text-destructive" />
            <span className="text-base text-destructive font-semibold">이미 존재하는 코스명입니다</span>
          </div>
        )}
      </div>

      {existingCourses.length > 0 && (
        <div className="flex flex-col gap-2">
          <p className="text-sm text-muted-foreground font-semibold px-1">이 구장의 기존 코스</p>
          <div className="flex flex-wrap gap-2">
            {existingCourses.map((c) => (
              <span
                key={c}
                className="text-sm px-4 py-2 bg-white border-2 border-border rounded-xl text-muted-foreground font-medium"
              >
                {c}
              </span>
            ))}
          </div>
        </div>
      )}

      <div className="flex gap-3 mt-1">
        <button
          onClick={onBack}
          className="flex-1 py-5 rounded-2xl border-2 border-border text-foreground text-lg font-bold hover:bg-secondary active:scale-[0.98] transition-all"
        >
          이전
        </button>
        <button
          onClick={onNext}
          disabled={!courseName.trim() || isDuplicate}
          className="flex-[2] bg-primary text-primary-foreground text-lg font-bold py-5 rounded-2xl flex items-center justify-center gap-2 disabled:opacity-30 hover:bg-primary/90 active:scale-[0.98] transition-all shadow-md shadow-primary/20"
        >
          다음
          <ChevronRight size={20} strokeWidth={2.5} />
        </button>
      </div>
    </div>
  );
}

function HoleRow({
  index,
  hole,
  onParChange,
  onDelete,
  canDelete,
}: {
  index: number;
  hole: Hole;
  onParChange: (delta: number) => void;
  onDelete: () => void;
  canDelete: boolean;
}) {
  return (
    <div className="flex items-center gap-3 px-4 py-3.5 bg-white border-2 border-border rounded-2xl">
      <div className="w-10 h-10 rounded-xl bg-secondary flex items-center justify-center shrink-0">
        <span className="text-sm font-bold text-primary">{index + 1}</span>
      </div>

      <div className="flex-1">
        <span className="text-base font-semibold text-foreground">{index + 1}번 홀</span>
      </div>

      <div className="flex items-center gap-2">
        <span className="text-sm text-muted-foreground font-medium mr-0.5">파</span>
        <button
          onClick={() => onParChange(-1)}
          disabled={hole.par <= 3}
          className="w-10 h-10 rounded-xl bg-secondary border-2 border-border flex items-center justify-center disabled:opacity-30 hover:bg-accent active:scale-95 transition-all"
        >
          <Minus size={16} strokeWidth={2.5} />
        </button>
        <span className="w-8 text-center text-xl font-bold text-foreground">{hole.par}</span>
        <button
          onClick={() => onParChange(1)}
          disabled={hole.par >= 5}
          className="w-10 h-10 rounded-xl bg-secondary border-2 border-border flex items-center justify-center disabled:opacity-30 hover:bg-accent active:scale-95 transition-all"
        >
          <Plus size={16} strokeWidth={2.5} />
        </button>
      </div>

      <button
        onClick={onDelete}
        disabled={!canDelete}
        className="w-10 h-10 rounded-xl flex items-center justify-center text-muted-foreground hover:text-destructive hover:bg-red-50 disabled:opacity-20 transition-all ml-1"
      >
        <Trash2 size={18} />
      </button>
    </div>
  );
}

function Step3({
  venueName,
  courseName,
  holes,
  onHolesChange,
  onNext,
  onBack,
}: {
  venueName: string;
  courseName: string;
  holes: Hole[];
  onHolesChange: (h: Hole[]) => void;
  onNext: () => void;
  onBack: () => void;
}) {
  const totalPar = holes.reduce((sum, h) => sum + h.par, 0);

  const updatePar = (idx: number, delta: number) => {
    const next = holes.map((h, i) =>
      i === idx ? { par: Math.min(5, Math.max(3, h.par + delta)) } : h
    );
    onHolesChange(next);
  };

  const deleteHole = (idx: number) => {
    onHolesChange(holes.filter((_, i) => i !== idx));
  };

  const addHole = () => {
    onHolesChange([...holes, { par: 3 }]);
  };

  return (
    <div className="flex flex-col">
      <div className="flex flex-col gap-4 px-5 py-5">
        <div>
          <div className="flex items-center gap-2 mb-2">
            <span className="text-sm font-bold text-primary bg-primary/10 px-3 py-1.5 rounded-full">{venueName}</span>
            <span className="text-sm text-muted-foreground">/</span>
            <span className="text-sm font-bold text-foreground bg-secondary px-3 py-1.5 rounded-full">{courseName}</span>
          </div>
          <h2 className="text-xl font-bold text-foreground mb-1">홀 설정</h2>
          <p className="text-base text-muted-foreground leading-relaxed">홀별 파를 조정하고 홀을 추가/삭제하세요</p>
        </div>

        <div className="flex items-center justify-between bg-white border-2 border-border rounded-2xl px-5 py-4">
          <div className="flex items-center gap-3">
            <span className="text-base text-muted-foreground font-medium">총 홀 수</span>
            <span className="text-2xl font-bold text-foreground">{holes.length}홀</span>
          </div>
          <div className="w-px h-6 bg-border" />
          <div className="flex items-center gap-3">
            <span className="text-base text-muted-foreground font-medium">총 파</span>
            <span className="text-2xl font-bold text-primary">{totalPar}</span>
          </div>
        </div>

        <div className="flex flex-col gap-2.5">
          {holes.map((hole, idx) => (
            <HoleRow
              key={idx}
              index={idx}
              hole={hole}
              onParChange={(d) => updatePar(idx, d)}
              onDelete={() => deleteHole(idx)}
              canDelete={holes.length > 1}
            />
          ))}
        </div>

        <button
          onClick={addHole}
          className="flex items-center justify-center gap-2.5 py-4 border-2 border-dashed border-primary/40 rounded-2xl text-primary text-base font-bold hover:bg-primary/5 active:scale-[0.99] transition-all"
        >
          <Plus size={18} strokeWidth={2.5} />
          홀 추가
        </button>

        <div className="h-28" />
      </div>

      <div className="sticky bottom-0 bg-background/95 backdrop-blur-sm border-t-2 border-border px-5 py-4">
        <div className="flex gap-3">
          <button
            onClick={onBack}
            className="flex-1 py-5 rounded-2xl border-2 border-border text-foreground text-lg font-bold hover:bg-secondary active:scale-[0.98] transition-all"
          >
            이전
          </button>
          <button
            onClick={onNext}
            disabled={holes.length === 0}
            className="flex-[2] bg-primary text-primary-foreground text-lg font-bold py-5 rounded-2xl flex items-center justify-center gap-2 disabled:opacity-30 hover:bg-primary/90 active:scale-[0.98] transition-all shadow-md shadow-primary/20"
          >
            다음
            <ChevronRight size={20} strokeWidth={2.5} />
          </button>
        </div>
      </div>
    </div>
  );
}

function Step4({
  venueName,
  courseName,
  holes,
  onSave,
  onBack,
}: {
  venueName: string;
  courseName: string;
  holes: Hole[];
  onSave: () => void;
  onBack: () => void;
}) {
  const totalPar = holes.reduce((sum, h) => sum + h.par, 0);
  const parGroups: Record<number, number> = {};
  holes.forEach((h) => {
    parGroups[h.par] = (parGroups[h.par] || 0) + 1;
  });

  return (
    <div className="flex flex-col gap-5 px-5 py-5">
      <div>
        <h2 className="text-xl font-bold text-foreground mb-1">저장 확인</h2>
        <p className="text-base text-muted-foreground leading-relaxed">아래 내용으로 템플릿을 저장합니다</p>
      </div>

      <div className="flex flex-col bg-white border-2 border-border rounded-2xl overflow-hidden">
        {[
          { label: "구장명", value: venueName },
          { label: "코스명", value: courseName },
          { label: "홀 수", value: `${holes.length}홀` },
          { label: "총 파", value: String(totalPar), highlight: true },
        ].map((row, idx, arr) => (
          <div
            key={row.label}
            className={`flex items-center justify-between px-5 py-4 ${idx < arr.length - 1 ? "border-b-2 border-border" : ""}`}
          >
            <span className="text-base text-muted-foreground font-medium">{row.label}</span>
            <span className={`text-lg font-bold ${row.highlight ? "text-primary" : "text-foreground"}`}>
              {row.value}
            </span>
          </div>
        ))}
        <div className="flex items-start justify-between px-5 py-4 border-t-2 border-border">
          <span className="text-base text-muted-foreground font-medium">파 구성</span>
          <div className="flex flex-col items-end gap-1">
            {Object.entries(parGroups)
              .sort(([a], [b]) => Number(a) - Number(b))
              .map(([par, count]) => (
                <span key={par} className="text-base font-semibold text-foreground">
                  파{par} — {count}홀
                </span>
              ))}
          </div>
        </div>
      </div>

      <div className="flex flex-col gap-2">
        <p className="text-sm text-muted-foreground font-semibold px-1">홀 파 미리보기</p>
        <div className="flex flex-wrap gap-2">
          {holes.map((h, idx) => (
            <div
              key={idx}
              className={`flex flex-col items-center justify-center w-12 h-12 rounded-xl border-2 ${
                h.par === 3
                  ? "bg-secondary border-border text-foreground"
                  : h.par === 4
                  ? "bg-primary/15 border-primary/40 text-primary"
                  : "bg-accent border-accent-foreground/30 text-accent-foreground"
              }`}
            >
              <span className="text-[10px] font-semibold opacity-60 leading-none">{idx + 1}</span>
              <span className="text-base font-bold leading-tight">{h.par}</span>
            </div>
          ))}
        </div>
      </div>

      <div className="flex gap-3 mt-1">
        <button
          onClick={onBack}
          className="flex-1 py-5 rounded-2xl border-2 border-border text-foreground text-lg font-bold hover:bg-secondary active:scale-[0.98] transition-all"
        >
          이전
        </button>
        <button
          onClick={onSave}
          className="flex-[2] bg-primary text-primary-foreground text-lg font-bold py-5 rounded-2xl flex items-center justify-center gap-2.5 hover:bg-primary/90 active:scale-[0.98] transition-all shadow-lg shadow-primary/25"
        >
          <Save size={20} strokeWidth={2.5} />
          저장하기
        </button>
      </div>
    </div>
  );
}

function SavedScreen({ template, onReset }: { template: Template; onReset: () => void }) {
  return (
    <div className="flex flex-col items-center justify-center gap-7 px-5 py-12 min-h-[60vh]">
      <div className="w-24 h-24 rounded-full bg-primary/15 border-4 border-primary/30 flex items-center justify-center">
        <Check size={44} className="text-primary" strokeWidth={2.5} />
      </div>
      <div className="text-center">
        <h2 className="text-2xl font-bold text-foreground mb-2">저장 완료!</h2>
        <p className="text-base text-muted-foreground leading-relaxed">
          <span className="text-foreground font-bold">{template.venueName}</span>의{" "}
          <span className="text-primary font-bold">{template.courseName}</span>이 저장되었습니다
        </p>
      </div>
      <div className="flex flex-col gap-3 w-full">
        <button
          onClick={onReset}
          className="w-full bg-primary text-primary-foreground text-lg font-bold py-5 rounded-2xl hover:bg-primary/90 active:scale-[0.98] transition-all shadow-md shadow-primary/20"
        >
          새 템플릿 작성
        </button>
        <button className="w-full py-5 rounded-2xl border-2 border-border text-foreground text-lg font-bold hover:bg-secondary active:scale-[0.98] transition-all">
          목록으로
        </button>
      </div>
    </div>
  );
}

export default function App() {
  const [step, setStep] = useState(1);
  const [venueName, setVenueName] = useState("");
  const [courseName, setCourseName] = useState("");
  const [holes, setHoles] = useState<Hole[]>(Array(9).fill(null).map(() => ({ par: 3 })));
  const [saved, setSaved] = useState<Template | null>(null);

  const handleSave = () => {
    setSaved({ venueName, courseName, holes });
  };

  const handleReset = () => {
    setStep(1);
    setVenueName("");
    setCourseName("");
    setHoles(Array(9).fill(null).map(() => ({ par: 3 })));
    setSaved(null);
  };

  return (
    <div className="min-h-screen bg-background flex flex-col items-center" style={{ fontFamily: "'Noto Sans KR', sans-serif" }}>
      <div className="w-full max-w-md min-h-screen flex flex-col bg-background">
        <div className="sticky top-0 z-10 bg-background/95 backdrop-blur-sm border-b-2 border-border">
          <div className="flex items-center px-3 h-16">
            <button
              onClick={() => {}}
              className="flex items-center gap-1 px-2 h-11 rounded-xl hover:bg-secondary active:scale-95 transition-all text-foreground"
            >
              <ChevronRight size={22} strokeWidth={2.5} className="rotate-180" />
              <span className="text-base font-semibold">뒤로</span>
            </button>
            <h1 className="flex-1 text-center text-lg font-bold text-foreground">코스 템플릿 작성</h1>
            <div className="w-11" />
          </div>
          {!saved && <StepIndicator current={step} />}
        </div>

        <div className="flex-1 overflow-y-auto">
          {saved ? (
            <SavedScreen template={saved} onReset={handleReset} />
          ) : (
            <>
              {step === 1 && (
                <Step1 venueName={venueName} onChange={setVenueName} onNext={() => setStep(2)} />
              )}
              {step === 2 && (
                <Step2
                  venueName={venueName}
                  courseName={courseName}
                  onChange={setCourseName}
                  onNext={() => setStep(3)}
                  onBack={() => setStep(1)}
                />
              )}
              {step === 3 && (
                <Step3
                  venueName={venueName}
                  courseName={courseName}
                  holes={holes}
                  onHolesChange={setHoles}
                  onNext={() => setStep(4)}
                  onBack={() => setStep(2)}
                />
              )}
              {step === 4 && (
                <Step4
                  venueName={venueName}
                  courseName={courseName}
                  holes={holes}
                  onSave={handleSave}
                  onBack={() => setStep(3)}
                />
              )}
            </>
          )}
        </div>
      </div>
    </div>
  );
}
