# 아키텍처

네이티브 Android(단일 액티비티) · Kotlin · **오프라인 우선**. 화면은 View(XML) + ViewBinding, 상태는 MVVM, 영속화는 Room, 화면 전환은 Jetpack Navigation을 사용합니다. 외부 서버·네트워크 의존이 없습니다.

## 레이어

```
┌─────────────────────────────────────────────────────────────┐
│  ui/  (Fragment + ViewModel, ViewBinding)                    │
│  home · start · hole(점수 입력) · grid(전체 점수표) ·        │
│  summary(경기 완료) · history(기록) · courses(코스/마법사) · │
│  nearby(주변 구장) · settings · common                       │
└───────────────┬─────────────────────────────────────────────┘
                │ (LiveData / suspend)
┌───────────────▼──────────────┐   ┌──────────────────────────┐
│  domain/  (순수 Kotlin)       │   │  ui/nearby/  (온디바이스)  │
│  Scoring · Stats ·            │   │  NearbyGeo(거리·정렬) ·    │
│  RecordFormat · CourseFormat ·│   │  NearbyRepository(자산) ·  │
│  model(Round/HoleSpec …)      │   │  LocationProvider          │
└───────────────┬──────────────┘   └──────────────────────────┘
                │
┌───────────────▼──────────────────────────────────────────────┐
│  data/  (Room)                                                │
│  ParkGolfRepository ← RoundDao · VenueDao · CourseDao         │
│  entity: RoundEntity · VenueEntity · CourseEntity            │
└───────────────────────────────────────────────────────────────┘
```

- **domain/** — 파 대비 계산, 통계(평균/최저/최고), 점수·코스 포맷 등 **프레임워크 비의존 순수 로직**. JVM 단위 테스트 대상.
- **data/** — Room 엔티티·DAO와 이를 감싼 `ParkGolfRepository`(단일 진입점). 라운드·구장·코스 CRUD, 진행 중 라운드 조회, 완료 라운드 Flow.
- **ui/** — 기능별 Fragment + (필요 시) ViewModel. 화면 간 라운드 선택은 경량 홀더(`SelectionHolder`)로 전달. 의존성 주입은 `App`의 수동 싱글턴.
- **ui/nearby/** — 번들 데이터셋을 읽어 기기 위치 기준 거리 계산·정렬. 위치·자산 I/O를 얇은 래퍼로 격리(테스트 용이).

## 주요 흐름

- **게임**: `start`(구장/코스 선택 또는 주변 구장) → `playerSetup`(인원) → `hole`(홀별 점수, 누적·파 대비) → `grid`(전체 점수표) / `summary`(랭킹·저장). 진행 중이면 홈에서 이어하기.
- **기록**: 완료 라운드 목록(통계 카드·점수 배지) → 상세(경기 결과 점수표, 인게임 표와 렌더러 공유).
- **코스 관리**: 확장형 구장 카드 + 단계형 템플릿 마법사(홀별 파). 구장 삭제 시 코스 캐스케이드.
- **주변 구장**: 버튼 트리거 → 위치 권한 → 하버사인 거리순 → 선택 시 코스 마법사 프리필 → 저장 후 경기.

## 테마

런타임 커스텀 속성(`?attr/park*`) + `Activity.recreate()` + SharedPreferences로 4색 테마를 즉시 적용. 색상은 `:root` 토큰처럼 `attrs.xml`/테마에서 정의.

## 데이터 파이프라인 (빌드 타임, 앱과 분리)

`tools/build_venues.py` 가 대한파크골프협회 PDF(564건)를 파싱 → 정규화·멀티코스 그룹핑 → 주소를 **VWorld**로 지오코딩(+수동 좌표 보정) → 앱 자산 `app/src/main/assets/parkgolf_venues.json`(557개 구장, 좌표 100%) 생성. 런타임은 이 JSON만 **읽기 전용**으로 사용(네트워크 없음). 상세는 [README](../README.md#데이터-출처) 참고.

## 테스트

- **JVM 단위 테스트**: `domain/`(Scoring·Stats·RecordFormat·CourseFormat), `ui/nearby/`(거리·정렬·자산 파싱), 마법사 프리필, 파이프라인(`tools/test_build_venues.py`).
- **계측 테스트(Espresso)**: 주요 화면 흐름.
- 실행: `./gradlew testDebugUnitTest`.

## 기술 스택

Kotlin · AGP/Gradle · minSdk 21 / target·compile 36 · AndroidX(AppCompat, Fragment, Navigation, Lifecycle, RecyclerView) · Material Components · Room(KSP) · coroutines. 파이프라인: Python 3 + pypdf.
