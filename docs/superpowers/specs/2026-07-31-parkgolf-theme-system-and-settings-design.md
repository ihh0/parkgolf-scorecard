# 파크골프 스코어 앱 — 테마 시스템 & 설정 기반 설계서

- **작성일**: 2026-07-31
- **상위 프로젝트**: 파크골프 스코어 앱 — 전면 개편(전체 흐름 목업)의 **1번 서브프로젝트: 테마 시스템 + 설정 + 저장 (기반)**
- **참고 목업**: `docs/mockups/파크골프 점수 기록 앱/` (Figma → React/shadcn 전체 앱 프로토타입). 특히 `src/app/types.ts`(THEMES), `components/Settings.tsx`, `components/MainMenu.tsx`.
- **후속 서브프로젝트(범위 밖)**: 홈 재디자인 / 게임 플로우 재디자인 / 기록 재디자인 / 코스 관리 재디자인.

---

## 1. 배경 & 목표

전체 앱 목업이 도착했고, 규모가 커 서브프로젝트로 분해했다. 그 1번인 본 문서는 **런타임 4색 테마 전환 + 설정 화면 + 영구 저장**을 구축한다. 이 기반이 완료되면 앱 전체가 선택된 테마(기본 초록)를 따르고, 이후 각 화면 재디자인은 테마 속성 위에서 진행된다.

이번 개편은 직전에 병합한 **검정(shadcn dark) 단일 팔레트를 대체**한다. 목업의 테마 목록은 초록/파랑/주황/보라 4종이며 **기본은 초록**(#2a7a3b, 틴트 배경 #f4f9f5)이다.

**성공 기준**
1. 설정에서 초록/파랑/주황/보라를 고르면 **앱 전체 색이 즉시 바뀐다**(액티비티 재생성).
2. 선택한 테마와 기본 이름이 **앱을 껐다 켜도 유지**된다.
3. 모든 기존 화면이 선택 테마의 색을 따른다(색 참조가 테마 속성 기반).
4. 홈에서 설정 화면에 진입할 수 있다.
5. 기본 이름이 게임 시작(인원 구성)의 첫 플레이어 기본값으로 쓰인다.

**범위 밖**
- 홈/게임/기록/코스관리 화면의 **레이아웃 재디자인**(색만 테마화, 구조는 이번엔 보존; 각 화면은 후속 서브프로젝트).
- 다크 모드(앱은 라이트 전용), 시스템 테마 연동.

---

## 2. 결정 사항 (브레인스토밍 확정)

| 항목 | 결정 |
|---|---|
| 테마 전환 방식 | **커스텀 테마 속성(`?attr/park*`) + `activity.recreate()`** |
| 저장 | **SharedPreferences** 래퍼 `SettingsStore` |
| background vs card | **분리** — `parkBackground`(틴트, 테마별) / `card`(흰색 고정) |
| 고정색(테마 무관) | `on_primary`(흰색), `destructive`/`on_destructive`, 파대비색(under/even/over) |
| 홈 경계 | 이번엔 **설정 진입 버튼만** 추가(홈 전체 재디자인은 서브프로젝트 2) |
| 기본 이름 | 인원 구성 첫 플레이어 기본값을 `store.defaultPlayerName`로 |

---

## 3. 테마 속성 시스템

### 3.1 커스텀 속성 (`res/values/attrs.xml`)
테마 간 바뀌는 색만 속성으로 선언(총 12개):
`parkBackground, parkForeground, parkPrimary, parkPrimary10, parkSecondary, parkMuted, parkMutedForeground, parkAccent, parkAccentForeground, parkBorder, parkInputBg` + 필요 시 `parkOnAccent`.
(각 `<attr name="..." format="color" />`)

> `secondary_foreground`는 레이아웃에서 미사용이므로 속성화하지 않고, 필요한 텍스트는 `?attr/parkForeground`를 쓴다.

### 3.2 테마 (`res/values/themes.xml`)
- `Theme.ParkGolf.Base` (부모 `Theme.Material3.DayNight.NoActionBar`): 공통 설정 + 고정색(`colorOnPrimary` 등) + `card`.
- `Theme.ParkGolf.Green / Blue / Orange / Purple` (부모 = Base): 각자 `park*` 속성값과 `colorPrimary`(= 해당 테마 primary), `android:windowBackground`(= 해당 테마 background) 지정.
- 값은 목업 `types.ts THEMES`를 그대로 사용. 예(초록): `parkBackground #F4F9F5`, `parkForeground #1A2E1D`, `parkPrimary #2A7A3B`, `parkSecondary #E6F2E8`, `parkMuted #EAF3EC`, `parkMutedForeground #4E7255`, `parkAccent #D2EBDA`, `parkAccentForeground #1A3D21`, `parkBorder #332A7A3B`(=rgba(42,122,59,0.2)), `parkInputBg #EAF3EC`, `parkPrimary10 #1A2A7A3B`(primary 10%).
- 파랑/주황/보라도 동일 구조로 `types.ts` 값 매핑. `parkPrimary10`은 각 테마 primary의 10%(ARGB alpha `1A`).

### 3.3 고정 색 (`res/values/colors.xml` 정리)
유지: `card #FFFFFF`, `on_primary #FFFFFF`, `destructive #D4183D`, `on_destructive #FFFFFF`, `under_par`, `even_par`(→ 테마 무관 고정), `over_par`.
**제거**: 기존 shadcn 토큰(primary, background, foreground, secondary, muted, accent, border, input_background, primary_10 등)과 레거시 별칭(surface, surface_muted, text_primary, text_muted, green_primary, green_light, green_disabled, stepper_bg) — 전부 속성 참조로 대체된 뒤 삭제.

### 3.4 Manifest
`application`/`activity`의 기본 테마를 `Theme.ParkGolf.Green`(기본값)으로. 실제 적용 테마는 런타임에 `setTheme`로 덮어씀(§5).

---

## 4. 색 참조 마이그레이션

모든 `res/layout/*.xml`, `res/drawable/*.xml`의 색 참조를 다음 규칙으로 스윕(약 120곳):

| 현재 `@color/...` | 변경 |
|---|---|
| `primary`, `green_primary` | `?attr/parkPrimary` |
| `primary_10`, `green_light` | `?attr/parkPrimary10` |
| `foreground`, `text_primary` | `?attr/parkForeground` |
| `muted_foreground`, `text_muted` | `?attr/parkMutedForeground` |
| `secondary`, `surface_muted` | `?attr/parkSecondary` |
| `muted`, `green_disabled` | `?attr/parkMuted` |
| `accent` | `?attr/parkAccent` |
| `accent_foreground` | `?attr/parkAccentForeground` |
| `border` | `?attr/parkBorder` |
| `input_background` | `?attr/parkInputBg` |
| `background`(화면 루트 배경), `surface` | `?attr/parkBackground` |
| `background`(카드/드로어블 solid, 흰색이어야 하는 곳) | `@color/card` |
| `on_primary` | `@color/on_primary` (유지) |
| `destructive`, `on_destructive` | 유지 |
| `under_par`/`even_par`/`over_par` | 유지 |

- **background 분기 주의**: 화면 루트/큰 영역 배경 = `parkBackground`(틴트), 카드·인풋 내부의 흰 배경 드로어블(`bg_card`, `bg_input`의 solid, `bg_step_inactive` 등) = `@color/card`(흰색).
- 드로어블은 테마 컨텍스트에서 인플레이트되므로 `?attr/...` 참조 가능. 스텝 원형·칩·인풋 드로어블 모두 속성 참조로 전환.
- 마이그레이션 후 §3.3의 제거 대상 색 정의를 삭제하고 빌드로 미참조 확인.

---

## 5. 저장 & 테마 적용

### 5.1 SettingsStore
- `data`/`enum`: `ThemeKey { GREEN, BLUE, ORANGE, PURPLE }` (문자열 직렬화 `green/blue/orange/purple`).
- `SettingsStore(context)` — SharedPreferences(`"parkgolf_settings"`) 래퍼:
  - `themeKey: ThemeKey` (get/set, 기본 GREEN)
  - `defaultPlayerName: String` (get/set, 기본 "나")
- `App`가 싱글턴 보유: `App.settings(application): SettingsStore`.

### 5.2 ThemeCatalog (순수, 테스트 대상)
```kotlin
object ThemeCatalog {
    @StyleRes fun styleFor(key: ThemeKey): Int = when (key) {
        ThemeKey.GREEN -> R.style.Theme_ParkGolf_Green
        ThemeKey.BLUE -> R.style.Theme_ParkGolf_Blue
        ThemeKey.ORANGE -> R.style.Theme_ParkGolf_Orange
        ThemeKey.PURPLE -> R.style.Theme_ParkGolf_Purple
    }
}
```
`ThemeKey.fromStored(s)`, `key.stored` 직렬화도 순수 함수로 테스트.

### 5.3 적용
- `MainActivity.onCreate`: `setTheme(ThemeCatalog.styleFor(App.settings(application).themeKey))` **전에** `super.onCreate` 후 `setContentView` 앞에서 호출.
- 테마 변경 시: `store.themeKey = newKey` 저장 → `requireActivity().recreate()`. 재생성 시 위 로직이 새 테마로 인플레이트.

---

## 6. 설정 화면 (신규)

- `SettingsFragment` + `res/layout/fragment_settings.xml`. nav_graph에 `settingsFragment` 목적지 추가. 상단바 제목 "설정"(공통 `view_top_bar`).
- **컬러 테마 섹션**: "컬러 테마" 제목 + 안내. 4개 선택 카드(가로: 색 원 + 라벨[초록/파랑/주황/보라] + 선택 시 체크 뱃지). 현재 선택은 강조(테두리 `parkPrimary` + `parkPrimary10` 배경). 탭 → 저장 + `recreate()`.
  - 색 원은 각 테마 primary(고정 4색 `@color/theme_green/blue/orange/purple` 상수) — 미리보기용이라 테마 무관.
- **기본 이름 섹션**: "기본 이름" 제목 + 안내 + (아이콘 + 입력창) 카드. 변경 시 `store.defaultPlayerName` 저장(`doAfterTextChanged` 또는 `onPause`).
- 뒤로: 단순 팝(확인창 없음).

### 6.1 홈 진입점
- `fragment_home.xml` 우상단에 **설정 기어 `ImageButton`** 추가(아이콘 `ic_settings` 신규 벡터, `contentDescription="설정"`). `HomeFragment`에서 `navigate(R.id.settingsFragment)`.
- 홈의 나머지 구조/버튼은 이번엔 유지(전체 재디자인은 서브프로젝트 2).

---

## 7. 기본 이름 연동

- `PlayerSetupFragment`에서 첫 플레이어 기본값 `getString(R.string.me)` → `App.settings(requireActivity().application).defaultPlayerName` 로 대체(행 추가 기본값과 저장 시 fallback 양쪽, 현재 코드 라인 38·52 해당).

---

## 8. 접근성 & 기기 호환
- 56dp+ 터치 타깃 유지, `sp` 폰트. 설정 카드·기어 버튼에 `contentDescription`. 테마별 primary/foreground 대비 확보(목업 값이 이미 대비 고려).
- 라이트 전용. `DayNight` 부모지만 다크 리소스는 제공하지 않음(라이트 값만).

---

## 9. 테스트 전략
- **단위**: `ThemeCatalog.styleFor(key)` 4종 매핑, `ThemeKey.fromStored`/`stored` 라운드트립(잘못된 문자열 → 기본 GREEN).
- **계측**: (a) 설정에서 "파랑" 선택 → 재진입 시 파랑이 선택 상태(저장·복원 확인). (b) 기본 이름 변경 → 게임 시작 인원 구성 첫 칸에 반영. (c) `SettingsStore` 저장/복원 왕복.
- 기존 도메인·저장소·핵심 플로우 단위/계측 테스트는 마이그레이션 후에도 통과 유지.

---

## 10. 구현 순서(계획 단계에서 태스크화)
1. `attrs.xml` + 4 테마 + 고정색 정리(`themes.xml`, `colors.xml`), manifest 기본 테마.
2. `ThemeKey`/`ThemeCatalog`/`SettingsStore` + 단위 테스트, `App` 연동, `MainActivity` setTheme.
3. 색 참조 마이그레이션(레이아웃·드로어블 `@color` → `?attr/park*`, background/card 분기), 미참조 색 제거.
4. 설정 화면(레이아웃·Fragment·nav) + 홈 기어 버튼 + `ic_settings`.
5. 기본 이름 연동(PlayerSetup).
6. 계측 테스트 + 회귀 확인.
