# 파크골프 주변 구장 앱 기능 — 설계 (서브프로젝트 B)

**목표.** 게임 시작 화면의 "주변 구장" 섹션을, 번들 데이터셋(`app/src/main/assets/parkgolf_venues.json`, 서브프로젝트 A 산출)과 기기 위치를 이용해 **가까운 구장을 거리순으로** 보여주고, 선택 시 **코스 마법사를 프리필**하도록 구현한다. 오프라인 우선(런타임 네트워크 없음).

## 결정(확정됨)
- 좌표 없는 "현황" 구장은 A에서 지오코딩 완료(204/219). 좌표 없는 15곳은 거리 목록에서 제외.
- **선택 동작 = 코스 마법사 프리필**(목업의 "바로 게임 시작"과 다름, 사용자 확정): 선택 → 구장명·코스명·홀수(파3) 프리필 → 사용자 확인/수정 후 저장 → 내 코스 편입 → 정상 게임 시작.
- **위치 트리거 = 버튼**("주변 구장 찾기"): 탭 시 권한 요청 → 목록. 갑작스런 자동 팝업 없음(시니어 배려).
- 위치 획득 = 프레임워크 `LocationManager`(새 라이브러리 없음).

## 범위
- 포함: 자산 로딩/파싱, 거리 계산·정렬·포맷(순수·테스트), "주변 구장" 섹션 상태 UI(버튼/로딩/목록/거부/빈), 위치 권한·획득, 선택→마법사 프리필.
- 제외: A 데이터 재생성, 지도 표시, 검색/필터, 반경 슬라이더, 즐겨찾기.

## 데이터 로딩 (`NearbyRepository` 신규)
- `assets/parkgolf_venues.json`을 1회 파싱(`org.json`, 새 의존성 없음) → `NearbyVenue` 목록.
- 모델(순수 data class):
  ```
  NearbyVenue(name, lat: Double?, lng: Double?, courses: List<NearbyCourse>,
              sido: String?, sigungu: String?, roadAddress: String?, jibunAddress: String?, phone: String?)
  NearbyCourse(name: String, holes: Int?)
  ```
- 거리 계산 대상은 `lat != null` 구장만.
- 파싱은 IO 디스패처에서 1회, 메모리 캐시.

## 거리 로직 (순수, 단위 테스트)
- `haversineKm(lat1, lng1, lat2, lng2): Double`.
- `nearest(venues, myLat, myLng, limit=10): List<Pair<NearbyVenue, Double>>` — 좌표 있는 구장만, 거리 오름차순, 상위 N.
- `formatDistance(km): String` — 1km 미만 "800m"(십의 자리 반올림), 이상 "1.2km"(소수 1자리).
- 홀수 미상 코스(holes=null)는 프리필 시 기본 9홀로 처리(마법사에서 수정 가능).

## UI — "주변 구장" 섹션 (`fragment_start.xml` / `StartFragment`)
기존 플레이스홀더(`nearby_placeholder` TextView)를 상태형 컨테이너로 교체. 상태:
1. **초기(idle)**: "📍 주변 구장 찾기" 버튼(점선/기본). 이미 권한 허용 상태면 진입 시 자동 로드해도 됨(선택 구현).
2. **로딩**: "주변 구장을 찾는 중…"(스피너/텍스트).
3. **목록**: 가장 가까운 10곳을 목업 카드로. 카드 = 구장명 + **거리 배지**(primary pill) + 코스 행들(코스명 + N홀 + chevron). 코스 행 탭 → 마법사 프리필.
   - 코스 행은 기존 `view_start_course_row`(코스명·홀수·chevron) 재사용 가능(홀수 표기 포함).
4. **권한 거부**: 안내("위치 권한이 필요합니다") + "다시 시도"(권한 재요청) / "설정 열기"(앱 설정) 버튼. 영구 거부 시 설정 유도.
5. **빈 결과**: 좌표 있는 구장이 없거나(이론상 없음) 위치 획득 실패 시 안내 + 다시 시도.

거리 배지 pill·아이콘은 기존 리소스(`bg_pill_primary`, `ic_navigation`, `ic_chevron_right`) 재사용.

## 위치 권한·획득
- 매니페스트: `ACCESS_COARSE_LOCATION`, `ACCESS_FINE_LOCATION` 추가.
- 런타임 요청: `ActivityResultContracts.RequestMultiplePermissions`(FINE+COARSE). COARSE만 허용돼도 동작(정밀도 낮아도 거리순 충분).
- 획득: `LocationManager`
  - `getLastKnownLocation`(GPS→NETWORK→PASSIVE 순)로 최근값 우선.
  - 최근값이 없거나 오래되면 1회 업데이트 요청(`getCurrentLocation`(API 30+) 또는 `requestSingleUpdate`/`requestLocationUpdates` 후 첫 콜백) + 타임아웃(예: 8초) → 실패 시 최근값 폴백 → 그래도 없으면 빈/에러 상태.
- 권한 거부/위치 실패는 위 UI 상태로 처리.

## 선택 → 마법사 프리필
- 마법사 진입 채널 확장: `savedStateHandle`에 `prefillVenueName`, `prefillCourseName`, `prefillHoles`(Int), (선택) `prefillAddress`.
- `CourseWizardFragment` 초기화: 기존 `wizardVenueId/CourseId` 분기 앞/뒤에 프리필 분기 추가 — 프리필 키가 있으면 `vm.initPrefill(venueName, courseName, holes)`.
- `CourseWizardViewModel.initPrefill(venueName, courseName, holeCount)`(신규): `CourseDraft(venueName=…, courseName=…, pars=List(holeCount){3}, step=0)`로 초기화. 이후 사용자가 확인/수정 후 저장(기존 저장 경로 재사용) → 내 코스 편입.
- 주소는 마법사에 필드가 없으므로 메모로만(선택) 또는 미사용. 최소 구현은 이름·코스·홀수만 프리필.

## 컴포넌트 경계
- `NearbyRepository`(신규): 자산 파싱 + 캐시. IO 격리.
- `NearbyGeo`(신규, 순수): haversine/nearest/formatDistance. 단위 테스트.
- `LocationProvider`(신규, 얇은 래퍼): 권한 가정 하에 위치 1회 획득(콜백/코루틴). 프레임워크 격리.
- `StartFragment`: 섹션 상태 관리·버튼·목록 렌더·선택 배선.
- `CourseWizardViewModel.initPrefill` + `CourseWizardFragment` 프리필 분기.

## 변경 파일(예상)
- 신규 `ui/nearby/NearbyRepository.kt`, `NearbyModels.kt`(NearbyVenue/Course), `NearbyGeo.kt`, `LocationProvider.kt`.
- 신규 테스트 `test/.../NearbyGeoTest.kt`(+ 파싱 테스트).
- 수정 `res/layout/fragment_start.xml`(플레이스홀더 → 상태형 섹션; 필요 시 `view_nearby_venue_card.xml` 신규).
- 수정 `ui/start/StartFragment.kt`(위치·목록·선택).
- 수정 `ui/courses/CourseWizardViewModel.kt`(initPrefill), `CourseWizardFragment.kt`(프리필 분기).
- 수정 `AndroidManifest.xml`(위치 권한), `res/values/strings.xml`(버튼/상태 문구).

## 에러/엣지
- 권한 거부(1회/영구): 안내 + 재시도/설정. 앱은 위치 없이도 나머지 기능 정상.
- 위치 획득 타임아웃: 에러 상태 + 다시 시도.
- 좌표 없는 구장: 거리 목록 제외(데이터엔 존재).
- 홀수 미상: 프리필 기본 9홀.
- 자산 파싱 실패: 섹션에 조용한 에러 상태(앱 크래시 없음).

## 테스트
- 순수 로직 Kotlin 단위 테스트: haversine(서울-부산 ≈325km, 동일점 0), nearest(정렬·상위 N·좌표 없는 항목 제외), formatDistance(800m/1.2km 경계).
- 자산 파싱: 샘플 JSON → NearbyVenue 리스트(필드 매핑, 좌표 null 처리).
- 에뮬레이터 스모크: adb로 모의 위치 주입 → "주변 구장 찾기" → 권한 허용 → 거리순 목록 → 코스 탭 → 마법사 프리필(이름·홀수) 확인 → 저장 → 내 코스·게임 시작. 권한 거부 상태 UI도 확인.
