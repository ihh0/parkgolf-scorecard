# 파크골프 스코어 앱 — 경기 진행(GamePlay + ScoreOverview + GameComplete) 재디자인 설계서

- **작성일**: 2026-08-13
- **상위 프로젝트**: 파크골프 스코어 앱 전면 개편의 **3번 서브프로젝트: 게임 플로우**, 그중 **5b 경기 진행** 단계
- **선행**: 5a 경기 준비(GameStart + PlayerSetup) 완료. 테마 시스템·홈 재디자인 위에서 진행.
- **참고 목업**: `docs/mockups/파크골프 점수 기록 앱/src/app/components/GamePlay.tsx`, `ScoreOverview.tsx`, `GameComplete.tsx`

---

## 1. 배경 & 목표

경기 진행 3개 화면을 목업의 **외형과 상호작용 규칙** 기준으로 교체한다. 도메인 로직(`Scoring`, `ScoreEditor`, `SummaryViewModel.ranking`, 증분 저장·완료 표시·홀 점프)은 그대로 재사용하고 뷰 계층만 목업화한다.

**성공 기준**
1. GamePlay가 목업 레이아웃(구장/코스 칩 · "N번 홀 파X" · N/전체 · 진행 도트 · 플레이어별 스테퍼 카드 · 하단 이전/다음(완료) + 전체 보기)과 일치한다.
2. GamePlay가 **플레이어 전원**에 대해 "누적 N타"(현재 홀까지)와 이번 홀 파대비 뱃지를 보여준다(현재는 플레이어 0만 표시).
3. ScoreOverview가 가로 스크롤 표(홀·파·플레이어 열 + 합계·파대비 푸터)로 표시된다.
4. GameComplete가 트로피 헤더 + 메달 순위 카드(파대비, 총타)로 표시된다.
5. 점수 입력은 **최소 1, 상한 없음**(기존 유지). 선택 테마 색을 따른다.
6. 기존 네비게이션/저장/홀 점프/뒤로가기 확인 동작이 유지된다.

**범위 밖**
- 기록 목록/상세(서브프로젝트 4). 단 ScoreOverview는 향후 record-detail에서 재사용 가능하도록 구성한다(이번엔 게임 흐름에만 연결).
- 점수 상한 도입(목업의 1~9 상한은 **적용하지 않음** — 결정).

---

## 2. 결정 사항 (브레인스토밍 확정)

| 항목 | 결정 |
|---|---|
| 분해 | 3개 화면(GamePlay/ScoreOverview/GameComplete)을 **한 스펙**으로. |
| 점수 상한 | **상한 없음 유지**(최소 1만). 목업의 1~9 상한 미적용. |
| GamePlay 플레이어 표시 | **목업대로**: 카드마다 "누적 N타"(현재 홀까지 누적 타수) + 이번 홀 파대비 뱃지, 전원 표시. |
| 구현 방침 | 외형 + 상호작용 규칙 목업 기준. 도메인 로직 재사용. |
| 저장 시점 | 기존 유지(증분 저장 + Summary 진입 시 COMPLETED 저장). "저장하고 홈으로"는 홈 이동 라벨. |

---

## 3. GamePlay 재디자인 (`fragment_hole_input.xml` + `view_player_score_row.xml` + `HoleInputFragment`)

루트: `LinearLayout`(vertical, 배경 `?attr/parkBackground`) → topBar include → 헤더 카드 → ScrollView(플레이어 카드들) → 하단 고정 영역.

### 3.1 헤더 카드 (`bg_card` 하단 강조, 상단 고정 느낌)
- 상단 행: 구장 칩(`bg_pill_primary`, primary 텍스트) + "/" + 코스명(muted). 코스명은 `round.holes[h].courseName`.
- 중단 행: "N번 홀"(대형 bold, `holeNo`) + 깃발 아이콘(`ic_flag`) + "파X" / 우측 "N / 전체"(muted).
- 하단: **진행 도트 컨테이너**(가로 wrap, 홀 수만큼 `View` 생성). 상태별 배경:
  - 현재 홀: primary, 넓게(width 24dp).
  - 완료(현재보다 앞): `?attr/parkPrimary` 40% 알파, 8dp.
  - 예정(현재보다 뒤): `?attr/parkBorder`, 8dp.
  - **탭 시** `holeVm.goTo(idx)` + 재렌더.

### 3.2 플레이어 카드 (`view_player_score_row.xml`, 전원)
- 카드(`bg_card`, padding): 좌측 이름(bold) + "누적 N타"(muted) / 우측 `−`(원형 버튼) `점수`(대형) `+`(원형 버튼).
- 점수 아래: **이번 홀 파대비 뱃지** — `score - hole.par`를 `Scoring.relationLabel`로(E/-n/+n), 색 `ParColors.colorFor`. 뱃지 배경: under=`bg_pill_primary`, even/over=`bg_pill_secondary`.
- "누적 N타" = `Scoring.runningStrokes(round.scores[p], h)`(§6).
- 버튼 동작: `session.adjust(p, h, +1/-1)` → `persist(repo)` → 재렌더. `ScoreEditor`가 최소 1 보장, 상한 없음.

### 3.3 하단 고정
- 행: `이전 홀`(outline, 첫 홀에서 비활성) / 우측 넓게 `다음 홀 ›`(primary) — 마지막 홀에서는 `게임 완료`(primary).
- 그 아래: `전체 점수 보기`(outline, 표 아이콘 `ic_table`).
- 네비: 다음(마지막) → `roundSummaryFragment`, 전체 보기 → `gridViewFragment`. (기존 id 유지.)

### 3.4 유지 동작
- 뒤로가기/상단 뒤로: `confirm_exit_game` 확인 → 홈으로 pop.
- `jumpToHole` savedStateHandle 관찰 유지(ScoreOverview 행 탭에서 점프).

---

## 4. ScoreOverview 재디자인 (`fragment_grid_view.xml` + `GridViewFragment`)

루트: topBar + `HorizontalScrollView` > `TableLayout`(카드 배경 `bg_card`, radius). 프로그램적 표 생성 유지, 스타일만 목업화.

### 4.1 표 구성
- 헤더 행(`?attr/parkSecondary` 배경): `홀` · `파` · 플레이어명들.
- 홀 행마다: `N`(bold) · 홀 파(muted) · 플레이어별 점수. 점수 셀 색 = 파대비(under=primary, even=muted, over=foreground) via `ParColors`.
- **합계 푸터 행**(`?attr/parkSecondary`): `합계` · 파 합계(`Scoring.parTotal`) · 플레이어별 총타(`Scoring.total`) + 그 아래 작은 파대비(+n/E/-n, `Scoring.relativeToPar`+`relationLabel`).
- 짝수/홀수 행 배경 교차(선택).

### 4.2 유지
- 홀 행 탭 → `previousBackStackEntry.savedStateHandle.set("jumpToHole", h)` → `popBackStack()`.
- 상단바 제목 `title_grid`(="전체 점수 보기"로 값 통일, §7).

### 4.3 재사용 고려
표 생성 로직을 `GridViewFragment` 내부 헬퍼로 두되, 입력을 `Round`(또는 holes/players/scores)만 받게 해 향후 record-detail에서 동일 뷰를 재사용할 수 있는 형태로 작성. (이번 스펙에서 별도 재사용 컴포넌트 추출은 필수 아님 — 최소 결합만.)

---

## 5. GameComplete 재디자인 (`fragment_round_summary.xml` + `item_rank_row.xml` + `RoundSummaryFragment`)

루트: topBar + ScrollView(헤더 + 순위 카드) + 하단 고정 버튼.

### 5.1 트로피 헤더
- 원형 배경(`?attr/parkPrimary` 15% 유사, 테두리) + `ic_trophy`(primary). "경기 완료"(bold) + "구장 · 코스"(muted). 구장=`round.venueName`, 코스=`round.holes` distinct courseName(첫 값 또는 " · " 결합).

### 5.2 순위 카드 (`SummaryViewModel.ranking(round)` 재사용, 전원)
`item_rank_row.xml`를 카드형으로 재작성:
- 좌측 **메달/순위 배지**: 1~3위는 `ic_medal`(색=금/은/동), 4위+는 숫자 원형(muted).
- 가운데: (1~3위) "N위" 색 라벨 + 이름(bold) / 아래 "파X 대비 [rel]"(rel: `relative`>0 → "+n", ==0 → "이븐", <0 → "n"; under면 primary 강조).
- 우측: 대형 총타 + "타".
- 1위 카드: `?attr/parkPrimary` 10% 배경 + primary 테두리. 나머지: `bg_card`.

`RankRow`(rank, player, total, relative)는 그대로 사용. 파 합계는 `Scoring.parTotal(round.holes)`.

### 5.3 유지
- 진입 시 `round.copy(status=COMPLETED)` 저장 + `session.startRound(completed)`(기존).
- 하단 `저장하고 홈으로`(`ic_save`) → `session.round.value=null` → 홈으로 pop.

---

## 6. 도메인 추가 (TDD)

`Scoring`에 순수 함수 추가:
```kotlin
/** 플레이어의 0..uptoHoleIndex(포함) 누적 타수. null 점수는 제외. */
fun runningStrokes(scores: List<Int?>, uptoHoleIndex: Int): Int =
    scores.take((uptoHoleIndex + 1).coerceAtLeast(0)).filterNotNull().sum()
```
경계: `take(n)`은 n이 음수면 예외를 던지므로 `coerceAtLeast(0)`로 방어. 범위를 넘는 큰 index는 `take`가 리스트 크기까지만. 기존 함수 변경 없음.

---

## 7. 신규/변경 리소스

### 7.1 아이콘 (`res/drawable/`, lucide)
- `ic_trophy.xml`, `ic_medal.xml`, `ic_table.xml`. (`ic_flag`, `ic_plus`, `ic_minus`, `ic_chevron_left`, `ic_chevron_right`, `ic_save` 기존 재사용.)

### 7.2 드로어블
- 진행 도트: `bg_dot_current.xml`(primary), `bg_dot_done.xml`(primary 40%), `bg_dot_future.xml`(border). 또는 단일 oval + tint 코드 지정.
- 메달 원형 배경: 코드에서 색 지정(고정색 참조) — 별도 드로어블 불필요 시 생략.
- 뱃지: 기존 `bg_pill_primary`/`bg_pill_secondary` 재사용.
- 스테퍼 버튼 원형: 기존 있으면 재사용, 없으면 `bg_stepper.xml`(secondary, 원형/라운드).

### 7.3 색상 (`res/values/colors.xml`, 고정색)
- `medal_gold`=#F5A623, `medal_silver`=#A8A8A8, `medal_bronze`=#CD7F32.

### 7.4 문자열 (`res/values/strings.xml`)
- 신규: `cumulative_strokes`="누적 %1$d타", `hole_counter`="%1$d / %2$d", `view_full_scores`="전체 점수 보기", `game_complete_title`="경기 완료", `par_versus`="파%1$d 대비 %2$s", `even_label`="이븐", `save_and_home`="저장하고 홈으로", `game_finish`="게임 완료".
- 변경(값 통일): `title_grid`="전체 점수 보기", `title_summary`="경기 완료", `game_recording`(상단바)="점수 기록".
- 기존 재사용: `hole_label`, `par_label`, `prev_hole`, `next_hole`, `rank_suffix`, `strokes_suffix`, `confirm_exit_game`.
- 정리(참조 0 확인 후): `cumulative_label`(플레이어0 전용 → 미사용 시 제거).

### 7.5 치수 (`res/values/dimens.xml`)
필요 시: `dot_size`(8dp), `dot_current_width`(24dp), `medal_size`(48dp), `stepper_size`(48dp, 기존 있으면 재사용), `score_big`(기존 `score_text` 재사용).

---

## 8. 무제한 인원 대응

5a에서 인원 상한을 없앴으므로:
- GamePlay: 플레이어 카드가 세로 스택(ScrollView) → N명 자연 확장.
- ScoreOverview: 플레이어당 열 추가 → `HorizontalScrollView`로 가로 스크롤.
- GameComplete: 순위 카드 세로 스택.
하드코딩 인원 가정 없이 `round.players.indices`로 순회.

---

## 9. 접근성 & 기기 호환

- 스테퍼 버튼 48dp+, 큰 점수 글꼴. 진행 도트는 장식이지만 탭 타깃이므로 최소 높이 확보(도트 주변 패딩) 또는 표/이전·다음으로도 홀 이동 가능(대체 경로 유지).
- 카드/표 큰 글꼴, 초대형 글꼴 대비 ScrollView/HorizontalScrollView.
- 메달 색 대비 확인, 색만으로 순위 구분하지 않도록 "N위" 텍스트 병기.

---

## 10. 테스트 전략

### 10.1 단위 (TDD, `app/src/test`)
- `Scoring.runningStrokes`: 현재 홀까지 합, null 제외, 경계(index 0, 마지막, 범위 밖).
- 기존 `SummaryViewModel.ranking` 테스트 유지(동점 공동 순위).

### 10.2 계측 (Espresso, `app/src/androidTest`)
- GamePlay: `+` 탭 → 점수 증가 반영, "다음 홀" → 홀 카운터 증가, 마지막 홀에서 "게임 완료" 표시. (라운드 시딩 필요 — 기존 계측 패턴 따르거나 스모크로 대체.)
- 회귀: `title_grid`/`title_summary`/`game_recording` 문자열 값 변경으로 깨지는 계측 단정 확인.

### 10.3 회귀
- `cumulative_label` 등 제거 문자열 참조 grep.
- 기존 `HoleInputViewModelTest`/`SummaryViewModelTest` 통과 확인.

---

## 11. 구현 순서(계획 단계에서 태스크화)

1. `Scoring.runningStrokes`(TDD) + 아이콘 3종 + 드로어블(도트/스테퍼) + 색상(메달) + 문자열/치수.
2. GamePlay 레이아웃(`view_player_score_row.xml` 카드화 + `fragment_hole_input.xml` 헤더/도트/하단) .
3. `HoleInputFragment` 재작성(전원 누적·뱃지, 진행 도트 생성/탭, 하단 버튼, 기존 저장/확인/점프 유지).
4. ScoreOverview 레이아웃(`fragment_grid_view.xml`) + `GridViewFragment` 표 스타일(파 열·합계 파대비·가로 스크롤).
5. GameComplete 레이아웃(`fragment_round_summary.xml` + `item_rank_row.xml` 메달 카드) + `RoundSummaryFragment` 재작성(트로피 헤더·메달·파대비, 기존 저장 유지).
6. 계측/기존 테스트 갱신 + 회귀 grep + 빌드 + (에뮬레이터 시 스모크).
