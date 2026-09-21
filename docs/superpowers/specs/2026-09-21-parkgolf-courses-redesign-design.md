# 파크골프 코스 관리 재디자인 — 설계

**서브프로젝트 5: 코스 관리.** 코스 관리 화면(`MyCoursesFragment`)을 목업 `docs/mockups/파크골프 점수 기록 앱/src/app/components/CourseManagement.tsx`에 맞춰 확장형 구장 카드로 다시 만든다. 현재는 구식(단순 구장 목록 + 탭 시 편집 다이얼로그).

## 목표
- 목업의 **확장형 구장 카드**: 헤더(핀 아이콘·구장명·N개 코스·구장 삭제·펼침 화살표) + 펼침 시 코스 행 목록.
- 코스 행: 코스명 · "N홀 · 파X" · 편집(연필) · 삭제(휴지통).
- **빈 상태**: 핀 아이콘 + "저장된 코스 없음" + 안내 + "코스 템플릿 작성" 버튼.
- 하단 점선 **"새 코스 템플릿 작성"** 버튼.
- 삭제 시 **확인 다이얼로그**(목업은 즉시 삭제지만 시니어 안전성·앱 관행 위해 확인 유지 — 사용자 결정).
- 데이터 모델상 **수동 캐스케이드**: 구장 삭제 → 그 구장의 코스도 모두 삭제. 코스 삭제로 구장의 마지막 코스가 사라지면 구장도 삭제(목업의 `filter(courses.length>0)`와 동일).

## 범위 밖
- 코스 재정렬/드래그, 구장 메모 편집, 검색.
- 마법사(TemplateCreate) 자체 변경(이미 목업 기준). 편집/추가 진입만 연결.

## 화면 (`MyCoursesFragment`)

### 구조
- 상단바(제목 "코스 관리") + 본문.
- 본문: 구장이 있으면 스크롤 영역(구장 카드들 + 하단 점선 버튼), 없으면 빈 상태.
- 구장 수는 보통 적으므로 `RecyclerView` 대신 **ScrollView + 컨테이너에 카드 프로그램적으로 빌드**(기존 `StartFragment`의 구장 카드 패턴과 동일: 컨테이너에 `view_course_venue_card` 인플레이트, 그 안 `coursesContainer`에 `view_course_row` 추가). 기존 `VenueAdapter`/`VenueRow`/`item_venue.xml`은 제거.

### 구장 카드 (`view_course_venue_card.xml`)
`bg_card`. 헤더 행:
- 좌: 핀 아이콘(`ic_map_pin`, primary, `bg_menu_icon`류 primary/10 배경 사각) + 구장명(굵게) + "N개 코스"(muted). 이 좌측 영역 탭 → 펼침/접힘 토글.
- 우: 구장 삭제(`ic_trash`, muted) · 펼침 화살표(`ic_chevron_down`/`ic_chevron_up`).
펼침 시(coursesContainer 표시): 상단 구분선 + 코스 행들.

### 코스 행 (`view_course_row.xml`)
- 코스명(semibold) + "N홀 · 파X"(muted).
- 우: 편집(`ic_pencil`, muted → 탭 시 마법사 편집) · 삭제(`ic_trash`, muted → 확인 후 코스 삭제).
- 마지막 행 제외 하단 구분선.

### 빈 상태
- muted 원 + `ic_map_pin`(muted), "저장된 코스 없음"(굵게), "코스 템플릿을 작성해 추가하세요"(muted), primary 버튼 "코스 템플릿 작성" → 마법사(신규).

### 펼침 상태
- `Set<Long>`(구장 id) 유지. 기본값: 전체 펼침(목업 동일). 토글 시 카드 다시 빌드. 데이터(Flow) 변경 시에도 현재 펼침 상태 유지.

### 데이터 도출
- "N개 코스": 해당 구장 코스 수.
- "N홀 · 파X": `pars.size`홀 · 파`pars.sum()`. → 순수 헬퍼 `CourseFormat.holesPar(pars)`.

## 동작
- 헤더 좌측 탭 → 펼침 토글(화살표 방향 전환).
- 구장 삭제(휴지통) → 확인("이 구장을 삭제할까요?") → 그 구장 코스 전부 + 구장 삭제(`deleteVenueWithCourses`).
- 코스 편집(연필) → `openWizard(venueId, courseId)`.
- 코스 삭제(휴지통) → 확인("이 코스를 삭제할까요?") → `deleteCourse`; 그 구장의 마지막 코스였으면 구장도 삭제.
- 하단 점선 버튼 / 빈 상태 버튼 → `openWizard(0, 0)`(신규).
- 목록은 `observeVenues` + `observeCourses` combine Flow 구독으로 자동 갱신.

## 저장소 변경
- **추가** `ParkGolfRepository.deleteVenueWithCourses(venueId: Long)` + Impl: `coursesForVenue(venueId).forEach { deleteCourse(it) }; venueById(venueId)?.let { deleteVenue(it) }`. (구장 조회용 `venueById`가 저장소에 없으면 함께 추가; `VenueDao.byId` 이미 존재.)
- 코스 마지막 삭제 후 구장 삭제는 프래그먼트에서 처리(현재 구장 엔티티 보유).

## 컴포넌트 경계
- `CourseFormat`(신규, domain): pars → "N홀 · 파X" 순수 포맷. TDD.
- `MyCoursesFragment`: Flow 구독, 카드 프로그램적 빌드, 펼침 상태, 삭제/편집/추가 배선.
- `view_course_venue_card` / `view_course_row`: 표시 전용 레이아웃.

## 변경 파일(예상)
- `MyCoursesFragment.kt` — 전면 재작성(어댑터 제거, 카드 빌드).
- **삭제** `VenueAdapter.kt`(+ `VenueRow`), `res/layout/item_venue.xml`.
- **신규** `domain/CourseFormat.kt` + 테스트.
- **신규** `res/layout/view_course_venue_card.xml`, `res/layout/view_course_row.xml`.
- `res/layout/fragment_my_courses.xml` — 재구성(스크롤+컨테이너+점선 버튼+빈 상태).
- `data/ParkGolfRepository.kt` / `ParkGolfRepositoryImpl.kt` — `deleteVenueWithCourses`(+ 필요 시 `venueById`).
- **신규 아이콘** `ic_pencil.xml`, `ic_chevron_down.xml`, `ic_chevron_up.xml`.
- `res/values/strings.xml` — `course_count`("%1$d개 코스"), `courses_empty_title`("저장된 코스 없음"), `courses_empty_desc`("코스 템플릿을 작성해 추가하세요"), `create_template`("코스 템플릿 작성"), `delete_venue_confirm`("이 구장을 삭제할까요?"), `delete_course_confirm`("이 코스를 삭제할까요?"), `course_holes_par`("%1$d홀 · 파%2$d"). 기존 `new_template_button`("새 코스 템플릿 작성"), `title_my_courses`, `cancel`, `delete` 재사용. 미사용 `no_courses_yet`/`pick_course_to_edit`/`new_course` 정리 검토.

## 에러/엣지
- 구장 0개 → 빈 상태.
- 펼침 상태에서 데이터 갱신 시 상태 유지(재빌드 시 Set 기준).
- 삭제 취소 시 아무 동작 없음.
- 코스 다수/이름 김 → 행 말줄임.

## 테스트
- `CourseFormat.holesPar` 단위 테스트(TDD).
- 나머지 도메인/저장소 로직 무변경(캐스케이드는 기존 DAO 조합).
- 에뮬레이터 스모크: (1) 구장 카드·핀·N개 코스·펼침/접힘·코스 행("N홀·파X")·편집/삭제 아이콘, (2) 코스 편집 진입, (3) 코스 삭제(확인)→갱신, 마지막 코스 삭제 시 구장 제거, (4) 구장 삭제(확인)→코스까지 제거, (5) 빈 상태 + "코스 템플릿 작성", (6) 하단 점선 버튼 → 마법사 신규, (7) 게임 시작 화면 구장/코스 목록 회귀 없음.
