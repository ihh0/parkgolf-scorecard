# 파크골프 스코어 앱 — 홈(메인 메뉴) 재디자인 설계서

- **작성일**: 2026-08-11
- **상위 프로젝트**: 파크골프 스코어 앱 — 전면 개편의 **2번 서브프로젝트: 홈(메인 메뉴) 재디자인**
- **선행**: `2026-07-31-parkgolf-theme-system-and-settings-design.md` (테마 속성 `?attr/park*` + 설정 + 홈 설정 기어). 본 문서는 그 위에서 홈 화면 외형을 목업대로 교체.
- **참고 목업**: `docs/mockups/파크골프 점수 기록 앱/src/app/components/MainMenu.tsx`

---

## 1. 배경 & 목표

전체 앱 목업의 홈 화면(`MainMenu`)을 네이티브에서 **최대한 그대로** 구현한다. 현재 홈은 텍스트 타이틀 + 큰 "＋ 라운드 시작" 버튼 + 아웃라인 버튼 2개 구조인데, 목업은 **로고 아이덴티티 + 카드형 메뉴 3개** 구조다. 테마 시스템(서브프로젝트 1)이 이미 깔려 있어 색은 `?attr/park*`를 따른다.

**성공 기준**
1. 홈이 목업의 레이아웃(설정 기어 · 로고+타이틀 · 카드 메뉴 3개)과 일치(네이티브 근사)한다.
2. 메뉴 문구가 목업 그대로다(게임 시작 · 게임 기록 · 코스 관리 + 각 설명).
3. 세 카드가 각각 기존 목적지(시작/기록/내 구장)로 이동한다.
4. 진행 중 라운드 자동 복구 다이얼로그가 그대로 동작한다.
5. 선택된 테마(초록/파랑/주황/보라) 색을 따른다.

**범위 밖**
- 게임 플로우 / 기록 / 코스 관리 **화면 자체의 재디자인**(각 후속 서브프로젝트). 이번엔 그 화면들의 **상단바 제목 문자열만** 목업 문구로 맞춘다(§5).
- 네비게이션 구조/목적지 변경(외형만 교체).

---

## 2. 결정 사항 (브레인스토밍 확정)

| 항목 | 결정 |
|---|---|
| 메뉴 문구 | **목업 그대로** (게임 시작 · 게임 기록 · 코스 관리 + 설명) |
| 진입 화면 제목 | 홈 직결 3개 화면의 상단바 제목도 목업 문구로 통일 |
| 로고 | **목업대로 깃발 아이콘**(채워진 flag, primary 배경 라운드 박스) |
| 메뉴 카드 구현 | **`<include>` 3개**(어댑터 대신, 항목 고정) |
| 네비게이션 | 변경 없음(기존 목적지 재사용) |
| 기존 테스트 | `CoreFlowTest`를 새 id로 갱신 |

---

## 3. 홈 레이아웃 (목업대로)

`fragment_home.xml`을 재구성. 루트 `ScrollView`(배경 `?attr/parkBackground`) → `LinearLayout`(vertical, padding 20dp):

1. **설정 기어 행** — 우측 정렬. 기존 `btnSettings`(48dp, `bg_card`, `ic_settings`, `contentDescription=설정`)를 최상단 우측에 배치. 아래 넉넉한 마진(≈40dp).
2. **앱 아이덴티티**(가운데 정렬, 아래 마진 ≈56dp):
   - 로고 박스: 112dp 정사각, 배경 `?attr/parkPrimary`, 라운드 24dp(그림자 elevation), 안에 흰색 깃발 아이콘 52dp(`ic_flag_solid`, tint `@color/on_primary`).
   - "파크골프": 대형 bold, `?attr/parkForeground`.
   - "스코어 앱": 중형, `?attr/parkMutedForeground`.
3. **메뉴 카드 3개**(세로 스택). 각 카드는 `view_home_menu_item.xml` include:
   - 컨테이너: `bg_card`, 패딩 20dp, minHeight 56dp, 클릭 가능, 하단 마진 16dp.
   - 좌측 아이콘 박스: 56dp, 배경 `bg_pill_primary`(=`?attr/parkPrimary10`), 아이콘 28dp tint `?attr/parkPrimary`.
   - 가운데(weight=1): 제목(bold, `?attr/parkForeground`) + 설명(작은, `?attr/parkMutedForeground`).
   - 우측: `ic_chevron_right` 22dp, tint `?attr/parkMutedForeground`.
   - 순서·내용:
     - **게임 시작** — "구장을 선택하고 경기를 시작하세요" — `ic_play_circle`
     - **게임 기록** — "지난 경기 기록을 확인하세요" — `ic_clipboard_list`
     - **코스 관리** — "저장된 코스를 편집하거나 삭제하세요" — `ic_layout_list`

### 3.1 재사용 include 규칙
`view_home_menu_item.xml`의 뷰에 id 부여: `ivIcon`, `tvTitle`, `tvDesc`(우측 chevron은 정적). 홈 레이아웃에서 세 개를 `<include android:id="@+id/itemGameStart|itemGameRecords|itemCourseManagement" .../>`로 배치. Fragment가 각 include 바인딩에 아이콘/제목/설명/클릭을 주입.

---

## 4. 신규 리소스

### 4.1 아이콘 벡터 (lucide, `res/drawable/`)
- `ic_flag_solid.xml` — 채워진 깃발(로고용, `fillColor` 흰색; 사용처에서 tint 불필요하나 `@color/on_primary` 고정).
- `ic_play_circle.xml`, `ic_clipboard_list.xml`, `ic_layout_list.xml` — stroke 벡터(strokeColor 기본 `?attr/parkPrimary`; 사용처에서 tint 지정 가능). (`ic_chevron_right.xml`는 기존 재사용.)

### 4.2 치수 (`res/values/dimens.xml`)
- `logo_size 112dp`, `logo_icon 52dp`, `menu_icon_box 56dp`, `app_title_text 34sp`, `app_subtitle_text 22sp`, `menu_desc_text 13sp`.

### 4.3 문자열 (`res/values/strings.xml`)
- 신규: `app_title`="파크골프", `app_subtitle`="스코어 앱".
- 신규 메뉴: `menu_game_start`="게임 시작", `menu_game_start_desc`="구장을 선택하고 경기를 시작하세요"; `menu_game_records`="게임 기록", `menu_game_records_desc`="지난 경기 기록을 확인하세요"; `menu_course_management`="코스 관리", `menu_course_management_desc`="저장된 코스를 편집하거나 삭제하세요".
- 기존 `start_round`/`past_records`/`my_courses`(이모지 포함 버튼 라벨)는 새 홈에서 미사용 → 남겨두되 참조 제거(제거는 후속 정리로 미룸).

---

## 5. 진입 화면 상단바 제목 통일

홈에서 직접 진입하는 3개 화면의 상단바 제목 문자열 **값**을 목업 문구로 갱신(문자열 이름은 유지, 값만 변경):
- `title_start` "라운드 시작" → **"게임 시작"**
- `title_history` "지난 기록" → **"게임 기록"**
- `title_my_courses` "내 구장 관리" → **"코스 관리"**

(해당 Fragment 코드는 이미 이 문자열을 참조하므로 코드 변경 없음.)

---

## 6. Fragment 동작

`HomeFragment` 갱신:
- 기존 4개 `setOnClickListener`(btnStart/btnHistory/btnMyCourses/btnSettings)를 **`itemGameStart.root`/`itemGameRecords.root`/`itemCourseManagement.root` + `btnSettings`** 로 대체. 목적지는 동일(`startFragment`/`historyFragment`/`myCoursesFragment`/`settingsFragment`).
- 각 include 바인딩에 아이콘·제목·설명 세팅(`itemGameStart.ivIcon.setImageResource(...)`, `tvTitle.text=...`, `tvDesc.text=...`). 또는 정적으로 include별 `android:src`/`android:text`를 레이아웃에 직접 지정하는 방식도 허용(그 경우 Fragment는 클릭만 주입). **정적 지정 선호**(레이아웃에 값, Fragment는 클릭만) — 단순하고 오류 적음.
- **진행 중 라운드 복구 다이얼로그 로직은 그대로 유지**(현재 코드의 `currentInProgressRound` 블록 보존).

> 구현 참고: 정적 지정을 택하면 include에 `<include>` 태그로는 자식 속성 오버라이드가 제한적이므로, 세 항목을 각각 별도 레이아웃(`view_home_item_game_start.xml` 등)으로 두거나, 공통 include + Fragment에서 값 주입 중 하나를 계획 단계에서 확정한다. 본 설계는 **공통 include 1개 + Fragment 주입**을 기본으로 한다(중복 최소화).

---

## 7. 접근성 & 기기 호환
- 카드 최소 터치 타깃 충분(패딩 20dp + 아이콘 56dp로 56dp+ 확보), 큰 글꼴. 설정 기어 `contentDescription` 유지, 로고/메뉴 아이콘은 장식이면 `importantForAccessibility=no`(제목 텍스트가 라벨 역할).
- 초대형 글꼴에서 카드 세로 확장 대응(ScrollView), 하드코딩 절대위치 지양.

---

## 8. 테스트 전략
- **계측**: (a) 홈에 3개 메뉴 제목 표시 확인. (b) "게임 시작" 카드 탭 → 시작 화면(예: `btnNewCourse` 표시) 이동.
- **기존 `CoreFlowTest` 갱신**: `btnStart`→`itemGameStart`(카드 루트), `withText(R.string.start_round)`→`withText(R.string.menu_game_start)`. 나머지 단정은 유지.
- 홈에 비즈니스 로직이 없어 단위 테스트는 불필요.

---

## 9. 구현 순서(계획 단계에서 태스크화)
1. 아이콘 벡터 4종 + 치수 + 문자열(신규 메뉴/타이틀) + 진입 화면 제목값 갱신.
2. `view_home_menu_item.xml`(재사용 카드) + `fragment_home.xml` 재구성.
3. `HomeFragment` 갱신(항목 값 주입 + 클릭, 복구 다이얼로그 보존).
4. `CoreFlowTest` 갱신 + 홈 메뉴 계측 테스트 + 회귀 확인.
