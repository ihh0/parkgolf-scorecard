# 파크골프 스코어 앱 — shadcn 리테마 & 코스 작성 위저드 재디자인 설계서

- **작성일**: 2026-07-30
- **상위 프로젝트**: 파크골프 스코어 앱 (M1 위에 얹는 UI/UX 2차 개편)
- **선행**: `2026-07-28-parkgolf-course-wizard-and-back-nav-design.md` (1차 위저드) — 본 문서가 이를 시각·동작 면에서 대체·확장
- **참고 목업**: `docs/mockups/코스 작성/` (Figma → React/Vite/shadcn 프로토타입, 핵심은 `src/app/App.tsx`)

---

## 1. 배경 & 목표

1차 위저드(4단계 + 공통 상단바 + 뒤로 확인)를 구현·병합한 뒤, 사용자가 **코스 작성 화면을 shadcn 스타일로 정교하게 다시 목업**했다. 이 목업의 외형을 네이티브 View 시스템에서 **그대로 재현**하고, 목업이 정의한 새 동작(홀 추가/삭제, 코스명 중복 검증, 저장 완료 화면, 기존 구장 감지)을 반영한다. 더불어 목업의 테마(검정 primary + shadcn 토큰)를 **앱 전체에 리테마**한다.

**성공 기준**
1. 코스 작성 위저드 4단계 + 저장 완료 화면이 목업의 레이아웃·컴포넌트·동작과 일치한다(네이티브 근사).
2. 검정 primary + shadcn 토큰이 앱 전역에 적용되어 색 정체성이 통일된다.
3. 공통 상단바가 **홈을 제외한 모든 화면**에 표시된다.
4. 새 동작(홀 추가/삭제, 코스명 중복 차단, 기존 구장 감지, 저장 완료 화면)이 동작한다.
5. 구형·저사양 기기와 다양한 화면 비율에서 안정적으로 렌더된다.

**범위 밖**
- 홈·홀입력·그리드·요약·기록·시작 등 **목업이 없는 화면의 카드 스타일 전면 재설계**(이번엔 색·상단바만 적용, 구조 보존).
- 엔티티/DB 스키마 변경, 다크 모드(앱은 라이트 전용 유지).
- 파 3~5 상·하한 제한(아래 결정대로 미적용).

---

## 2. 결정 사항 (브레인스토밍 확정)

| 항목 | 결정 |
|---|---|
| 접근 방식 | **A**: 디자인시스템 토대 신설 → 코스 작성 위저드 풀 재구현 → 나머지 화면은 색·상단바만 적용(구조 유지) |
| 테마 색 | **목업 그대로 — 검정 primary(#030213) + shadcn 토큰**, 앱 전체 리테마 |
| 상단바 | **홈(메인 메뉴) 제외 전 화면 공통 레이아웃**. 편집 시 제목 "코스 편집" |
| 위저드 동작 | 홀 추가/삭제 방식 ✅, 코스명 중복 검증 ✅, 저장 완료 화면 ✅, 기존 구장 감지(배지·제안·배너) ✅ |
| 파 범위 | **미적용** — 파 1 이상 자유(3~5 제한 없음) |
| 데이터 모델 | `CourseDraft`에서 `holeCount` 제거(=`pars.size`), 홀은 add/delete로 관리 |

---

## 3. 디자인시스템 토대 (재사용 리소스)

### 3.1 컬러 토큰 (`res/values/colors.xml` 신설/매핑)
shadcn 라이트 테마를 근사한 값:

| 토큰 | 값 | 용도 |
|---|---|---|
| `primary` | `#030213` | 주요 버튼·강조·활성 |
| `on_primary` | `#FFFFFF` | primary 위 텍스트/아이콘 |
| `background` | `#FFFFFF` | 화면 배경 |
| `foreground` | `#252525` | 본문 텍스트 |
| `secondary` | `#F1F1F5` | 보조 버튼/칩 배경 |
| `muted` | `#ECECF0` | 약한 배경 |
| `muted_foreground` | `#717182` | 보조 텍스트 |
| `accent` | `#E9EBEF` | 파5 미리보기 등 |
| `border` | `#1A000000` | 카드·인풋 2dp 보더(검정 10%) |
| `destructive` | `#D4183D` | 오류(중복 코스명) |
| `on_destructive` | `#FFFFFF` | destructive 위 텍스트 |
| `input_background` | `#F3F3F5` | 입력창 배경 |
| `primary_10` | `#1A030213` | primary 10%(칩·배너·활성 스텝) |

- **유지**: 점수 파대비색(`under_par` 파랑, `over_par` 빨강, `even_par`)은 의미색이므로 그대로 둔다.
- 기존 초록 계열(`green_primary`, `green_light`, `green_disabled`)은 제거하고, 참조처를 새 토큰으로 스윕한다(§5).

### 3.2 반경·컴포넌트 드로어블 (`res/drawable/`)
- `bg_card.xml` — 흰 배경 + 2dp `border` + 16dp radius.
- `bg_input.xml` — `input_background` + 2dp `border` + 16dp radius. 포커스 상태(state_focused)에서 보더 `primary` (selector).
- `bg_input_error.xml` — 위와 동일하나 보더 `destructive`(코스명 중복 시).
- `bg_pill_primary.xml` — `primary_10` 배경 + full radius(구장 칩·배지·배너).
- `bg_pill_secondary.xml` — `secondary` 배경 + full radius(코스 칩).
- `bg_dashed.xml` — 점선 `primary` 40% 보더 + 16dp radius(홀 추가 버튼).
- `bg_step_circle_active/done/inactive.xml` — 원형: active=`primary_10`+`primary` 보더, done=`primary` 채움, inactive=흰 배경+`border`.
- `bg_number_badge.xml` — 10dp radius `secondary` 배경(홀 번호 배지).
- 기본 radius 리소스 `dimen`: `radius_card=16dp`, `radius_pill`(=높이의 절반, 실무상 999dp), `radius_sm=10dp`.

### 3.3 버튼 스타일 (`res/values/styles.xml`)
- `Widget.App.Button.Primary` — `primary` 채움, `on_primary` 텍스트, 16dp corner, minHeight 56dp+, bold.
- `Widget.App.Button.Outline` — 투명 배경, 2dp `border`, `foreground` 텍스트, 16dp corner.
- 기존 Material 버튼 사용처는 이 스타일로 교체.

### 3.4 아이콘 벡터 (`res/drawable/`, lucide 기반 24dp)
`ic_map_pin`, `ic_flag`, `ic_list_ordered`, `ic_save`, `ic_check`, `ic_plus`, `ic_minus`, `ic_trash`, `ic_chevron_left`(뒤로), `ic_alert_circle`, `ic_chevron_right`(다음). tint는 사용처에서 지정.

---

## 4. 공통 상단바 (전 화면, 홈 제외)

### 4.1 레이아웃 (`view_top_bar.xml` 갱신)
목업 형태로 재구성: 좌측 `‹ 뒤로`(chevron-left 아이콘 + "뒤로" 텍스트, 56dp+ 터치 타깃), 가운데 **제목**(굵게), 우측 동일 폭 스페이서로 제목 중앙 정렬. 배경 `background`, 하단 2dp `border`.

### 4.2 적용 범위 & 제목
홈(`homeFragment`)을 **제외한 모든 목적지**에 상단바를 표시한다:

| 화면 | 제목 | 뒤로 동작 |
|---|---|---|
| 라운드 시작(start) | 라운드 시작 | 팝(홈) |
| 코스 선택(courseSetup) | 코스 선택 | 팝 |
| 플레이어(playerSetup) | 함께 하는 사람 | 팝 |
| 홀 입력(holeInput) | 게임 기록 | **확인창** → 홈 |
| 그리드(grid) | 전체 보기 | 팝 |
| 요약(summary) | 라운드 완료 | 팝(홈) |
| 기록(history) | 지난 기록 | 팝(홈) |
| 기록 상세(roundDetail) | 기록 상세 | 팝 |
| 내 구장(myCourses) | 내 구장 관리 | 팝 |
| 코스 작성/편집(wizard) | **코스 템플릿 작성 / 코스 편집** | 스텝1에서 **확인창**, 그 외 이전 스텝 |

### 4.3 뒤로 단일 경로
1차에서 도입한 `Fragment.onBackPressed`(시스템/제스처) + 상단바 버튼을 **하나의 핸들러**로 통일하는 원칙을 전 화면에 유지한다. 확인창이 필요한 화면(홀 입력, 위저드 스텝1)만 `confirmYesNo`, 나머지는 단순 팝.

---

## 5. 나머지 화면 recolor + 상단바 적용

- 홈 제외 각 Fragment 레이아웃에서 기존 초록·회색 하드 참조를 새 토큰으로 스윕(레이아웃·위젯 구조는 보존).
- 각 화면에 `view_top_bar` include 추가 + 제목/뒤로 배선(§4.2).
- 홈 화면: 상단바 없음. 단, 색 토큰은 새 팔레트 적용(버튼 primary 검정 등).
- 카드/라운드 스타일의 전면 도입은 하지 않음(해당 화면 목업 확정 시 별도 사이클).

---

## 6. 코스 작성 위저드 재디자인 (목업대로)

단일 `CourseWizardFragment` + `ViewFlipper`(5개 자식: 4단계 + 저장 완료) 구조 유지. 상단바·스텝 인디케이터는 완료 화면에서 숨김.

### 6.1 스텝 인디케이터
원(48dp) + 라벨, 원 사이 연결선. **완료 단계=`primary` 채움+체크✓**, 현재=`primary_10`+`primary` 보더+아이콘, 미도달=흰 배경+`border`. 아이콘: map-pin/flag/list-ordered/save. (1차의 스텝 탭→점프는 편집 편의로 유지.)

### 6.2 단계별 상세
1. **구장명(Step1)**: "구장 이름" 제목 + 안내("동일한 이름의 구장은 통합 표시"). 라운드 입력.
   - **정확히 일치하는 기존 구장** 입력 시: 입력창 우측 "✓ 기존 구장" 배지 + 하단 배너("이미 저장된 구장입니다. 코스를 추가하면 통합됩니다.").
   - **부분 일치**(정확일치 아님)일 때: "저장된 구장" 제안 목록(map-pin + 이름, 탭하면 입력 채움).
   - `다음`(full-width primary), 구장명 공백이면 비활성(투명도 30%).
2. **코스명(Step2)**: 상단 구장 칩. "코스 이름" 제목 + 안내("같은 구장 내 중복 코스명 불가"). 입력.
   - **중복 검증**: 같은 구장에 이미 있는 코스명이면 입력창 보더 `destructive` + "이미 존재하는 코스명입니다"(alert-circle). `다음` 비활성.
   - "이 구장의 기존 코스" 칩 목록(있을 때).
   - `이전`(outline) / `다음`(primary) = 1:2.
3. **홀 설정(Step3)**: 구장/코스 칩. "홀 설정" 제목 + 안내.
   - **총 홀 수 / 총 파** 요약 카드(가운데 세로 구분선).
   - 홀별 카드: 번호 배지 + "N번 홀" + `파 − [값] ＋` + 삭제🗑. **파 하한 1**(상한 없음). 홀이 1개면 삭제 비활성.
   - **`＋ 홀 추가`**(점선 카드) — 파3 홀 1개 추가.
   - 하단 sticky `이전`/`다음`(1:2). 홀 0개면 `다음` 비활성(최소 1개 보장).
4. **저장 확인(Step4)**: "저장 확인" 제목.
   - 요약 카드: 구장명 / 코스명 / 홀 수 / **총 파(강조)** / 파 구성("파3 — N홀" 오름차순).
   - **홀 파 미리보기 칩**: 홀마다 번호+파, 파별 색(파3=`secondary`, 파4=`primary_10`, 그 외=`accent`).
   - `이전`(outline) / `저장하기`(primary, save 아이콘).
5. **저장 완료(Success)**: 큰 체크 원(`primary_10`), "저장 완료!", "○○의 △△이 저장되었습니다". `새 템플릿 작성`(primary, 초기화 후 Step1) / `목록으로`(outline, 내 구장으로 팝).

### 6.3 제목
create=**"코스 템플릿 작성"**, edit=**"코스 편집"** (`editingCourseId != null`로 판별).

---

## 7. 데이터 모델 & 로직

### 7.1 CourseDraft (holeCount 제거)
```kotlin
data class CourseDraft(
    val editingVenueId: Long? = null,
    val editingCourseId: Long? = null,
    val venueName: String = "",
    val courseName: String = "A코스",
    val pars: List<Int> = List(9) { 3 },
    val step: Int = 0,
)
```
- 홀 수 = `pars.size`. `resizePars`/`holeCount` 관련 로직 제거.

### 7.2 CourseWizardLogic (순수) 변경
- `addHole(pars): List<Int>` = `pars + 3`.
- `deleteHole(pars, index): List<Int>` — `pars.size <= 1`이면 그대로, 아니면 index 제거.
- `clampPar(v): Int` = `max(1, v)` (상한 없음).
- `isExistingVenue(name, venues): Boolean` — trim·대소문자 무시 정확 일치.
- `matchedVenues(name, venues): List<String>` — 부분 일치(name 비공백, 정확일치 제외), 중복 제거.
- `isDuplicateCourseName(name, existingCourseNames): Boolean` — trim·대소문자 무시 포함.
- `resolveVenueId(...)`, `suggestCourseName(...)` — 1차 그대로 유지.
- 단계 유효성: `venueStepValid(draft)`=구장명 비공백; `courseStepValid(name, existingCourseNames)`=비공백 && 중복 아님; `holeStepValid(pars)`=`pars.isNotEmpty()`.

### 7.3 CourseWizardViewModel 변경
- `addHole()`, `deleteHole(index)`, `setPar(index, value)`(clampPar) 추가; `setHoleCount` 제거.
- 기존 구장의 코스명 목록을 UI가 로드해 넘길 수 있도록 `existingCourseNames`를 보유(중복 검증·기존 코스 칩용). Fragment가 repo에서 로드해 주입.
- `next/prev/goToStep`, `loadForEdit`(pars만, holeCount 제거), `initNew` 유지.

### 7.4 저장 로직
1차와 동일: `resolveVenueId`로 같은 이름 구장 통합/신규 결정 → `editingCourseId`면 update, 아니면 insert. 엔티티·DB 불변, 과거 라운드 스냅샷 영향 없음.

---

## 8. 접근성 & 기기 호환
- 56dp+ 터치 타깃, `sp` 폰트, 초대형 글꼴 대응(ConstraintLayout·스크롤). 아이콘 버튼 `contentDescription`(뒤로/추가/삭제/−/＋). 검정 primary + 흰 텍스트로 명도 대비 확보. 중복 오류는 색+텍스트+아이콘 삼중 표기.

---

## 9. 테스트 전략
- **단위**: `CourseWizardLogic`(addHole, deleteHole 최소1, clampPar 하한1, isExistingVenue, matchedVenues, isDuplicateCourseName, courseStepValid, holeStepValid, suggestCourseName, resolveVenueId), `CourseWizardViewModel`(홀 add/delete, setPar, 단계 이동).
- **계측**: 전 흐름(구장명→코스명→홀 추가/삭제→저장→**완료 화면** 표시), 코스명 중복 시 `다음` 차단, 기존 구장명 입력 시 배너 표시.
- 기존 테스트(도메인·저장소·핵심 플로우)는 새 색/상단바 반영 후에도 통과 유지.

---

## 10. 구현 순서(계획 단계에서 태스크화)
1. 디자인시스템 토대: 토큰·드로어블·아이콘·버튼 스타일.
2. 공통 상단바 레이아웃 갱신(‹ 뒤로 + 중앙 제목).
3. 위저드 로직·VM 변경(홀 add/delete, 중복 검증) + 단위 테스트.
4. 위저드 5개 화면 레이아웃(카드 스타일) + 단계별 동작.
5. 저장 완료 화면 + 네비게이션.
6. 나머지 화면 recolor + 상단바 적용.
7. 계측 테스트 + 회귀 확인.
