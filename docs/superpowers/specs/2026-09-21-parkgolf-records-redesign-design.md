# 파크골프 기록(목록·상세) 재디자인 — 설계

**서브프로젝트 4: 기록.** 게임 기록 목록과 경기 결과 상세 화면을 앱 전반 재디자인 언어(카드 기반, `?attr/park*` 토큰, 재사용 컴포넌트)에 맞춰 다시 만든다. 기준은 목업 `docs/mockups/파크골프 점수 기록 앱/src/app/components/GameRecords.tsx` 와 `App.tsx`(record-detail = ScoreOverview 재사용)이다.

## 목표
- 기록 목록을 목업의 카드 레이아웃으로 전환한다: 날짜·구장·코스·총타수·점수 배지·플레이어·액션 행.
- 최근 통계(평균/최저/최고)를 카드형 요약으로 **재디자인해 유지**한다(목업에는 없지만 유용해 존치).
- 기록 없음 **빈 상태**를 추가한다.
- 삭제를 **목록 카드**로 옮긴다.
- 상세("경기 결과")를 재디자인된 **ScoreOverview(전체 점수표) 읽기 전용 재사용**으로 전환한다.
- 새 도메인 로직 없음. 기존 `Scoring`·`Stats`·저장소만 사용.

## 범위 밖
- 통계 차트/그래프(목업 `ui/chart.tsx`는 도입하지 않음).
- 기록 필터·정렬·검색.
- 다중 플레이어 랭킹 카드를 상세에 추가(상세는 전체 점수표만).

## 화면 1 — 기록 목록 (`HistoryFragment`)

### 상단: 통계 요약 카드 (재디자인, 유지)
- `bg_card` 한 장 안에 3분할(평균 / 최저 / 최고). 각 칼럼: 위 muted 라벨("평균"/"최저"/"최고"), 아래 굵은 숫자 + "타". 값 없으면 "-".
- 데이터: 기존 `HistoryViewModel.computeStats(rounds, recentN = 10)` 그대로. 라벨 문구도 기존 유지("평균 %.1f타" 등은 카드 분할에 맞게 라벨/숫자 분리).
- 배경 `?attr/parkBackground`, 좌우 `@dimen/screen_padding`.

### 목록: 라운드 카드 (`item_history_round.xml`)
각 완료 라운드 = 카드(`bg_card`, 하단 마진). 구조(목업 기준):
- **상단 정보 행**(좌우 배치)
  - 좌: 날짜 `M월 d일 (요일)` (muted, 작게) / **구장명**(굵게, 큼, 한 줄 말줄임) / 코스명(muted)
  - 우: **총타수**(아주 큼, 굵게) / 그 아래 가로로 `파{합계}`(muted 작게) + **점수 배지**
- **점수 배지**(pill): `diff = 총타수 - 총파`
  - `diff < 0`: primary 색 텍스트 + primary 10% 배경 pill, 텍스트 `{diff}` (예: `-3`)
  - `diff == 0`: muted 텍스트 + muted 배경 pill, 텍스트 `E`
  - `diff > 0`: foreground 텍스트 + secondary 배경 pill, 텍스트 `+{diff}`
- **플레이어 행**: 인원 아이콘(`ic_users` 계열, muted) + 이름들 `", "` 연결(muted, 말줄임)
- **액션 행**(상단 구분선): 좌 **경기 결과 보기**(표 아이콘 `ic_table`) — 탭 시 상세 이동 / 우 **삭제**(휴지통 아이콘) — 탭 시 삭제 확인
  - 두 액션 사이 세로 구분선.
  - 최소 터치 타깃 유지(`@dimen/touch_min` 이상).

### 빈 상태
- 기록이 0개면 목록 대신: muted 원 배경 + 트로피 아이콘(`ic_trophy`, muted), "기록 없음"(굵게) / "게임을 완료하면 여기에 기록됩니다"(muted). 통계 카드는 숨기거나 "-"로 표시(빈 상태에서는 숨김).

### 데이터 도출 (라운드당)
- 날짜: `SimpleDateFormat("M월 d일 (E)", Locale.KOREA).format(Date(round.date))` → 예 "8월 13일 (수)".
- 구장명: `round.venueName`.
- 코스명: `round.holes.map { it.courseName }.distinct().firstOrNull() ?: ""` (빈 문자열이면 코스명 줄 숨김).
- 총타수: `Scoring.total(round.scores[0])` (대표 플레이어 = index 0, "나").
- 총파: `Scoring.parTotal(round.holes)`.
- 플레이어: `round.players.joinToString(", ")`.

### 삭제
- 카드 삭제 아이콘 → `MaterialAlertDialogBuilder` 확인(`R.string.delete_confirm`, 취소/삭제) → `repo.deleteRound(round.id)`. 목록은 `observeCompletedRounds()` Flow 구독으로 자동 갱신.

## 화면 2 — 경기 결과 상세 (`RoundDetailFragment`)
- 목업대로 **읽기 전용 전체 점수표**로 전환. 재디자인된 ScoreOverview(`GridViewFragment` / `fragment_grid_view.xml`)와 **동일한 표**(전체 너비, `stretchColumns="*"`, 파 색상, 가로 스크롤 + 스크롤바)를 렌더한다.
- 상단바 제목 "경기 결과"(`title_round_detail` 문구를 "경기 결과"로). 기존 raw 테이블과 하단 삭제 버튼 제거.
- `roundId`를 `previousBackStackEntry.savedStateHandle`에서 받아 `repo.getRound(id)`로 로드(현행 유지). 활성 세션(`RoundSessionViewModel`)과 무관.
- **읽기 전용**: 인게임 ScoreOverview의 홀 행 탭(→ 해당 홀 편집으로 jump)은 상세에서는 **비활성**(탭 리스너 없음).

### 공용 표 렌더러
`GridViewFragment`의 표 생성 로직(헤더/홀 행/합계 행, 셀 생성, 파 색상, 합계의 상대 라벨)을 공용 헬퍼로 추출한다.
- 시그니처(개념): `ScoreTable.render(context, table: TableLayout, round: Round, onHoleClick: ((holeIndex: Int) -> Unit)? = null)`.
- `GridViewFragment`: `onHoleClick = { h -> previousBackStackEntry.savedStateHandle.set("jumpToHole", h); popBackStack() }` 전달.
- `RoundDetailFragment`: `onHoleClick = null`(읽기 전용). 레이아웃 `fragment_round_detail.xml`을 `fragment_grid_view.xml`과 동일 구조(ScrollView → HorizontalScrollView `fillViewport` → TableLayout `stretchColumns="*"` + `bg_card`, 가로 스크롤바)로 교체.
- 색상 헬퍼 `ParColors`, 문구 `Scoring.relationLabel` 등 기존 것 재사용.

## 데이터 흐름
```
HistoryFragment → repo.observeCompletedRounds() (Flow) → 카드 목록 + 통계 카드
   카드 "경기 결과 보기" → savedStateHandle["roundId"] → navigate(roundDetailFragment)
   카드 "삭제" → 확인 → repo.deleteRound(id) → Flow가 목록 갱신
RoundDetailFragment → repo.getRound(roundId) → ScoreTable.render(read-only)
```
Nav 그래프 변경 없음(두 대상 이미 존재). ScoreOverview용 `GridViewFragment`는 인게임 경로 그대로 유지.

## 컴포넌트 경계
- `ScoreTable`(신규, `ui/common` 또는 `ui/grid`): Round → TableLayout 렌더. 세션 VM·저장소 비의존, 순수 뷰 빌더. 인게임/기록 양쪽에서 재사용.
- `HistoryAdapter`: Round 목록 → 카드 바인딩(날짜/구장/코스/점수/배지/플레이어), 클릭·삭제 콜백 노출.
- `HistoryFragment`: 저장소 구독, 통계 카드 바인딩, 어댑터·빈 상태 토글.
- `RoundDetailFragment`: id로 라운드 로드 → `ScoreTable.render` 읽기 전용.

## 변경 파일(예상)
- `app/src/main/java/com/parkgolf/score/ui/history/HistoryFragment.kt` — 통계 카드 바인딩, 빈 상태 토글, 삭제 콜백 배선.
- `app/src/main/java/com/parkgolf/score/ui/history/HistoryAdapter.kt` — 카드 바인딩(날짜/코스/배지/플레이어/액션), onDelete 콜백 추가.
- `app/src/main/java/com/parkgolf/score/ui/history/RoundDetailFragment.kt` — 공용 렌더러 사용, 삭제 제거, 읽기 전용.
- `app/src/main/java/com/parkgolf/score/ui/grid/GridViewFragment.kt` — 표 로직을 `ScoreTable`로 위임.
- **신규** `app/src/main/java/com/parkgolf/score/ui/common/ScoreTable.kt`(위치 확정은 계획 단계).
- `app/src/main/res/layout/fragment_history.xml` — 통계 카드 + 빈 상태 뷰 + RecyclerView.
- `app/src/main/res/layout/item_history_round.xml` — 카드 레이아웃.
- `app/src/main/res/layout/fragment_round_detail.xml` — ScoreOverview 스타일 표.
- 드로어블(필요 시): 점수 배지 pill 배경(언더/E/오버), 인원 아이콘(`ic_users`가 없으면 추가), 빈 상태 원 배경(기존 `bg_menu_icon` 등 재사용 검토).
- `strings.xml` — "기록 없음"/빈 상태 안내/"경기 결과 보기"/"경기 결과" 등. 미사용 문구 정리.

## 에러/엣지 케이스
- 기록 0개 → 빈 상태, 통계 카드 숨김.
- 코스명 없음(빈 문자열) → 코스명 줄 숨김.
- 플레이어 다수 → 이름 행 말줄임, 상세 표는 가로 스크롤.
- `getRound` null → 상세에서 `popBackStack`(현행 유지).
- 점수 배지: 대표 플레이어(index 0) 기준(목업과 동일한 단일 총점 표기).

## 테스트
- 도메인 로직 무변경 → 기존 `Stats`/`Scoring` 단위 테스트 유지, `./gradlew :app:testDebugUnitTest` 회귀 확인.
- 에뮬레이터 스모크: (1) 완료 라운드 여러 개에서 카드·날짜·코스·배지(언더/E/오버)·플레이어 표시, (2) 통계 카드 값, (3) 빈 상태(기록 삭제 후), (4) "경기 결과 보기" → 전체 너비 점수표(파 색상·가로 스크롤), (5) 목록 카드 삭제 → 확인 → 목록 갱신, (6) 인게임 ScoreOverview 회귀(홀 탭 편집 여전히 동작).
