# 파크골프 스코어 앱 — 경기 준비(GameStart + PlayerSetup) 재디자인 설계서

- **작성일**: 2026-08-13
- **상위 프로젝트**: 파크골프 스코어 앱 전면 개편의 **3번 서브프로젝트: 게임 플로우**, 그중 **5a 경기 준비** 단계
- **후속**: 5b 경기 진행(점수 기록 + 전체 점수 보기 + 경기 완료)은 별도 스펙
- **선행**: 테마 시스템(`?attr/park*`), 홈 재디자인. 본 문서는 그 위에서 경기 준비 두 화면을 목업대로 교체.
- **참고 목업**: `docs/mockups/파크골프 점수 기록 앱/src/app/components/GameStart.tsx`, `PlayerSetup.tsx`, `App.tsx`

---

## 1. 배경 & 목표

전체 앱 목업의 경기 준비 두 화면(`GameStart`, `PlayerSetup`)을 네이티브에서 **외형과 상호작용 규칙 모두 목업 기준**으로 구현한다. 현재 앱은 경기 준비가 3단계(구장 선택 → 코스 결합 → 인원)인데, 목업은 2단계(코스 하나 탭 → 인원)다. 코스 결합 단계를 제거한다.

**성공 기준**
1. GameStart가 목업 레이아웃(최근 사용한 구장 · 내 코스 · 주변 구장 · 하단 새 템플릿 버튼)과 일치한다.
2. 코스 하나를 탭하면 **중간 결합 단계 없이** 바로 PlayerSetup으로 이동한다.
3. PlayerSetup이 목업 레이아웃(코스 정보 카드 · 번호 인원 행 · 인원 추가 · 하단 게임 시작)과 일치한다.
4. 라운드 생성/저장/네비게이션은 기존 도메인 로직(`RoundFactory.newRound`, `repo.saveRound`, `RoundSessionViewModel.startRound`)을 그대로 사용한다.
5. 선택 테마 색을 따른다.

**범위 밖**
- 5b 화면(GamePlay/ScoreOverview/GameComplete) 재디자인.
- 주변 구장의 실제 GPS/위치 구현(플레이스홀더만).
- 코스 템플릿 작성 화면(이미 재디자인 완료).

---

## 2. 결정 사항 (브레인스토밍 확정)

| 항목 | 결정 |
|---|---|
| 분해 | 게임 플로우를 5a(경기 준비) / 5b(경기 진행)로 분할. 본 스펙은 5a. |
| 코스 결합 단계 | **제거**. 목업대로 단일 코스 선택 → PlayerSetup 직행. `CourseSetupFragment` 및 부속물 삭제. |
| 주변 구장 | **플레이스홀더 유지**("준비 중"). GPS/위치 권한 미도입. |
| 구현 방침 | 외형 + 상호작용 규칙까지 목업 기준. |
| 최대 인원 | **무제한**(기존 4명 제한 해제). |
| 인원 추가 기본값 | 목업대로 **"플레이어 N" 프리필**(포커스·전체선택). |
| 최근 카드 예외 | 최근 라운드의 구장+코스가 **현재 venues에 존재할 때만** 표시. 없으면 최근 섹션 숨김. |

---

## 3. GameStart 재디자인 (`fragment_start.xml` + `StartFragment`)

루트 `ScrollView`(배경 `?attr/parkBackground`) → 수직 `LinearLayout`(padding 20dp, 섹션 간격 ≈28dp). 최하단에 sticky 버튼 영역(ScrollView 밖, 부모를 `LinearLayout`/`FrameLayout`로 감싸 하단 고정).

상단바(`topBar` include): 제목 `title_start`="게임 시작", 뒤로가기 → `popBackStack()`.

### 3.1 섹션: 최근 사용한 구장
- 라벨 행: 시계 아이콘(`ic_clock`, tint `?attr/parkMutedForeground`) + "최근 사용한 구장"(`recent_venue_label`).
- **primary 큰 카드**(배경 `?attr/parkPrimary`, 텍스트 `@color/on_primary`, radius 16dp, elevation): 
  - 소제목: "M월 D일 마지막 사용"(`recent_last_used`, opacity 낮게).
  - 구장명(큰 bold), 코스명(중간).
- 데이터: `StartViewModel.deriveRecentCourses(repo.completedRounds(), limit=1)`의 첫 항목. 단 그 구장+코스가 현재 venues에 존재해야 표시(없으면 섹션 전체 숨김, §3.5).
- 탭 → §3.4 코스 선택 동작.

### 3.2 섹션: 내 코스
- 라벨 행: 지도핀 아이콘(`ic_map_pin`) + "내 코스"(`my_courses_label`).
- 구장별 **카드**(배경 `bg_card`, radius 16dp): 상단에 구장명(bold), 그 아래 코스 행들.
  - 코스 행: 코스명(semibold) + "N홀"(`hole_count`, muted) + 우측 chevron. 행 사이 divider.
  - 행 탭 → §3.4 코스 선택 동작.
- 데이터: `repo.observeVenues()`. 각 venue의 `coursesForVenue(venueId)`로 코스+홀수.
- 구현: `RecyclerView`(`rvVenues`) + `VenueCoursesAdapter`. 각 아이템 = 구장 1개 카드(내부에 코스 행들을 코드로 채움). 또는 flatten 없이 구장 카드 뷰타입 하나로 처리하고 내부 코스 행은 `view_start_course_row.xml`를 반복 inflate.

### 3.3 섹션: 주변 구장 (플레이스홀더)
- 라벨 행: 내비 아이콘(`ic_navigation`) + "주변 구장"(`nearby_label`).
- 카드(`bg_card`): 안내 문구 "주변 구장은 준비 중입니다"(`nearby_placeholder`, muted, 가운데 정렬). 비클릭.

### 3.4 코스 선택 동작 (공통)
최근 카드/코스 행 어디서든 코스가 선택되면:
```
SelectionHolder.reset()
SelectionHolder.venueId = <venueId>
SelectionHolder.venueName = <venueName>
SelectionHolder.chosenCourseIds.add(<courseId>)   // 단일 원소
findNavController().navigate(R.id.playerSetupFragment)
```
`PlayerSetupFragment`는 기존대로 `chosenCourseIds`(원소 1개)로 동작한다.

### 3.5 최근 카드 유효성
- `deriveRecentCourses(..., limit=1)`가 준 `RecentCourse`(구장명·코스명·날짜)에 대해, 현재 venues에서 `venueName` 일치 venue를 찾고 그 안에 `courseName` 일치 코스가 있으면 → 표시 + 그 courseId로 선택 가능.
- 못 찾으면 최근 섹션 `visibility=GONE`.

### 3.6 하단 고정 버튼
- 점선 테두리 카드형 버튼 "＋ 새 코스 템플릿 작성"(`new_template_button`, 색 `?attr/parkPrimary`). 탭 → 코스 마법사:
```
SelectionHolder.reset()
currentBackStackEntry?.savedStateHandle?.set("wizardVenueId", 0L)
currentBackStackEntry?.savedStateHandle?.set("wizardCourseId", 0L)
findNavController().navigate(R.id.courseWizardFragment)
```
(기존 `btnNewCourse` 동작 유지, 라벨/스타일만 목업화.)

---

## 4. PlayerSetup 재디자인 (`fragment_player_setup.xml` + `PlayerSetupFragment`)

루트: ScrollView + sticky 하단 버튼 구조. 상단바 제목 `title_player_setup`="경기 인원 구성".

### 4.1 코스 정보 카드
- 카드(`bg_card`): 좌측 핀 아이콘 박스(`bg_menu_icon` 재사용 또는 `?attr/parkPrimary10` 원형) + 구장명(muted, 작게) + 코스명(bold, 크게).
- 데이터: `SelectionHolder.venueName` + 선택 코스명. 코스명은 `SelectionHolder.chosenCourseIds` 첫 원소로 `coursesForVenue`에서 조회.

### 4.2 제목/부제
- "경기 인원"(`player_count_title`, bold 큼) + "함께 경기할 인원을 구성하세요"(`player_count_subtitle`, muted).

### 4.3 인원 행 목록
- 각 행: **번호 원형**(1부터, `?attr/parkPrimary10` 배경 + primary 숫자) + **이름 EditText**(`bg_input`, 큰 글꼴) + **삭제 버튼**(휴지통 `ic_trash`).
- 첫 행 기본값: 설정의 기본 이름(`App.settings(...).defaultPlayerName`, 기본 "나").
- 삭제: 행 1개만 남으면 모든 삭제 버튼 비활성(disabled + 흐리게).
- 행 재사용 레이아웃: `view_player_input_row.xml`(번호 `tvIndex`, 입력 `etName`, 삭제 `btnDelete`).
- 인원 수 **무제한**(상한 제거).

### 4.4 인원 추가
- 점선 버튼 "＋ 인원 추가"(`add_player_button`). 탭 시 새 행 추가, 기본값 "플레이어 N"(`player_default_name`, N=행번호) 프리필 + 새 행에 포커스·전체선택.

### 4.5 하단 고정: 게임 시작
- primary 버튼 "게임 시작 ›"(`start_game_button`). 모든 이름이 공백 아님일 때만 활성(`isEnabled` + alpha).
- 탭 시 기존 로직 유지:
```
val venueId = SelectionHolder.venueId ?: return
val courses = repo.coursesForVenue(venueId)
val coursePars = SelectionHolder.chosenCourseIds.mapNotNull { id ->
    courses.firstOrNull { it.id == id }?.let { RoundFactory.CoursePars(it.name, it.pars) }
}
if (coursePars.isEmpty()) return
val players = editRows.mapIndexed { i, et ->
    val t = et.text.toString().trim()
    if (t.isNotEmpty()) t else if (i == 0) defaultName else getString(R.string.companion_default, i)
}
val round = RoundFactory.newRound(venueName, players, coursePars, System.currentTimeMillis())
val id = repo.saveRound(round)
session.startRound(round.copy(id = id))
findNavController().navigate(R.id.holeInputFragment)
```
- 유효성: 이름 하나라도 공백이면 버튼 비활성(목업 `canStart`). `doAfterTextChanged`로 상태 갱신.

---

## 5. 코스 결합 단계 제거

- `nav_graph.xml`: `courseSetupFragment` 목적지 및 이를 향한 action 제거. `StartFragment` → `playerSetupFragment` 직접 이동.
- 삭제: `CourseSetupFragment.kt`, `CourseToggleAdapter.kt`, `fragment_course_setup.xml`, `view_wizard_step_holes.xml`은 **삭제하지 않음**(코스 마법사가 사용). 결합 전용만 삭제.
- `SelectionHolder`: `chosenCourseIds`는 유지(원소 1개로 사용). `players` 필드는 미사용이면 제거 검토(현재 PlayerSetup은 자체 상태 사용).
- 관련 문자열 정리(제거): `hole_config`, `total_holes`, `next_step`, `title_course_setup`, `start_where`. `total_holes_label`은 별개이므로 유지(다른 참조 확인 후).

---

## 6. 데이터/도메인 변경

### 6.1 RecentCourse에 코스명 추가
현재:
```kotlin
data class RecentCourse(val venueName: String, val holeCount: Int, val lastPlayed: Long)
```
변경:
```kotlin
data class RecentCourse(
    val venueName: String,
    val courseName: String,   // 신규: 라운드 holes의 distinct courseName 첫 값
    val holeCount: Int,
    val lastPlayed: Long,
)
```
`StartViewModel.deriveRecentCourses`는 각 라운드에서 `courseName = round.holes.map { it.courseName }.distinct().firstOrNull() ?: ""`로 채운다. distinctBy는 기존대로 venueName 기준(구장별 최근 1개). limit=1 호출 시 가장 최근 완료 라운드의 구장+코스가 나온다.

> **주의**: 레거시 결합 라운드는 holes에 코스명이 여러 개 → `firstOrNull`만 사용. 최근 카드 유효성(§3.5)에서 그 코스명이 현재 venues에 없으면 섹션 숨김되므로 안전.

### 6.2 인원 상한 해제
`Round.players` 주석("size 1..4")은 상한을 강제하지 않음(문서용). PlayerSetup에서 상한 로직만 제거. `ScoreEditor`/`Scoring`/저장 경로는 N명 배열을 이미 지원.

---

## 7. 신규/변경 리소스

### 7.1 아이콘 (`res/drawable/`, lucide stroke, tint `?attr/park*`)
- `ic_clock.xml`, `ic_map_pin.xml`, `ic_navigation.xml`, `ic_trash.xml`, `ic_user_plus.xml`(인원 추가), `ic_chevron_right.xml`(기존 재사용).

### 7.2 레이아웃
- `fragment_start.xml` — 재구성(§3).
- `view_start_course_row.xml` — 내 코스 카드 안 코스 행(코스명 + N홀 + chevron).
- `fragment_player_setup.xml` — 재구성(§4).
- `view_player_input_row.xml` — 인원 입력 행(번호/입력/삭제).

### 7.3 문자열 (`res/values/strings.xml`)
신규:
- `recent_venue_label`="최근 사용한 구장"
- `recent_last_used`="%1$s 마지막 사용" (인자: "M월 D일")
- `my_courses_label`="내 코스"
- `nearby_label`="주변 구장"
- `nearby_placeholder`="주변 구장은 준비 중입니다"
- `hole_count`="%1$d홀"
- `new_template_button`="새 코스 템플릿 작성"
- `player_count_title`="경기 인원"
- `player_count_subtitle`="함께 경기할 인원을 구성하세요"
- `add_player_button`="인원 추가"
- `player_default_name`="플레이어 %1$d"
- `player_name_hint`="이름 입력"
- `start_game_button`="게임 시작"

변경:
- `title_player_setup` 값 → "경기 인원 구성"

제거(§5): `hole_config`, `total_holes`, `next_step`, `title_course_setup`, `start_where`, `recent_courses`, `saved_presets`, `nearby_courses_soon`, `new_course`(대체됨). 각 제거 전 참조 grep 확인.

### 7.4 치수 (`res/values/dimens.xml`)
필요 시 추가: `recent_card_radius`(16dp, 기존 `radius_card` 재사용 가능), `player_index_size`(40dp), `section_gap`(28dp). 기존 값 재사용 우선.

---

## 8. 날짜 포맷

`recent_last_used`의 "M월 D일"은 `lastPlayed`(epoch millis)를 `Calendar`/`java.time`로 월·일 추출해 `getString(R.string.recent_last_used, "${month}월 ${day}일")`로 조립. minSdk 21 호환 위해 `java.util.Calendar` 사용(또는 `SimpleDateFormat("M월 d일")`).

---

## 9. 접근성 & 기기 호환

- 큰 터치 타깃(코스 행·인원 행 minHeight `touch_min`, 삭제/추가 버튼 48dp+).
- EditText 큰 글꼴, 번호 원형은 장식(`importantForAccessibility=no`; 라벨은 이름 입력).
- 초대형 글꼴 대비 ScrollView, 하단 버튼 sticky.
- primary 카드의 흰 텍스트 대비 확인.

---

## 10. 테스트 전략

### 10.1 단위 (TDD, `app/src/test`)
- `StartViewModel.deriveRecentCourses`:
  - 완료 라운드들에서 구장별 최근 1개, courseName이 holes 첫 코스명으로 채워짐.
  - limit=1 → 가장 최근 라운드의 구장+코스.
  - 빈 목록 → 빈 리스트.
- (선택) 최근 카드 유효성 판정을 순수 함수로 분리 시 그 함수 테스트(구장/코스 매칭 → 표시 여부 + courseId).

### 10.2 계측 (Espresso, `app/src/androidTest`)
- GameStart: "내 코스" 섹션 라벨 표시, 코스 행 탭 → PlayerSetup(예: `start_game_button` 표시)로 이동(중간 화면 없음).
- PlayerSetup: 첫 인원 행 표시, "인원 추가" 탭 → 두 번째 행 등장, 이름 비우면 "게임 시작" 비활성.
- 기존 `CoreFlowTest`/`CourseWizardFlowTest`에서 CourseSetup 경유하던 단정 갱신(코스 선택 → 바로 PlayerSetup).

### 10.3 회귀
- `courseSetupFragment` 제거로 깨지는 nav 참조/테스트 id 전수 확인(grep `courseSetupFragment`, `CourseToggle`, `title_course_setup`, 삭제 문자열).

---

## 11. 구현 순서(계획 단계에서 태스크화)

1. 아이콘 6종 + 문자열(신규/변경/제거) + 치수.
2. `RecentCourse`/`deriveRecentCourses` 코스명 추가(TDD) + 테스트.
3. `view_start_course_row.xml` + `fragment_start.xml` 재구성 + `VenueCoursesAdapter`.
4. `StartFragment` 재작성(세 섹션 바인딩 + 단일 코스 선택 동작 + 최근 유효성 + 새 템플릿).
5. `view_player_input_row.xml` + `fragment_player_setup.xml` 재구성.
6. `PlayerSetupFragment` 재작성(코스 정보 카드 + 무제한 인원 + 프리필 + 유효성 + 기존 시작 로직).
7. 코스 결합 제거: `nav_graph`에서 `courseSetupFragment` 삭제, `CourseSetupFragment.kt`/`CourseToggleAdapter.kt`/`fragment_course_setup.xml` 삭제, 문자열 정리.
8. 계측/기존 테스트 갱신 + 회귀 grep + 빌드.
