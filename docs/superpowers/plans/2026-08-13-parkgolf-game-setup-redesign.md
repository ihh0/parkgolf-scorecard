# 경기 준비(5a) 재디자인 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** GameStart와 PlayerSetup 두 화면을 목업대로 재디자인하고, 코스 결합 단계(CourseSetupFragment)를 제거해 코스 하나 탭 → 인원 구성 → 게임 시작의 2단계 흐름으로 만든다.

**Architecture:** View 시스템(XML) + ViewBinding + Jetpack Navigation. GameStart/PlayerSetup은 프래그먼트가 뷰를 프로그램적으로 채운다(구장 카드·인원 행). 라운드 생성은 기존 `RoundFactory.newRound` + `repo.saveRound` + `RoundSessionViewModel.startRound`를 그대로 사용. 테마 색은 `?attr/park*`.

**Tech Stack:** Kotlin, AndroidX, Material, ConstraintLayout/LinearLayout, Room, Navigation, JUnit + Truth(단위), Espresso(계측).

**참고 스펙:** `docs/superpowers/specs/2026-08-13-parkgolf-game-setup-redesign-design.md`

**공통 참고(기존 재사용):**
- 드로어블: `bg_card`(카드), `bg_dashed`(점선 primary 테두리), `bg_menu_icon`(primary10 라운드 박스), `bg_pill_primary`(primary10 pill). 색: `@color/on_primary`(#FFF), `@color/card`. 속성: `?attr/parkPrimary`, `?attr/parkPrimary10`, `?attr/parkForeground`, `?attr/parkMutedForeground`, `?attr/parkBorder`, `?attr/parkBackground`, `?attr/parkInputBg`.
- 상단바 include: `@layout/view_top_bar` (id `topBar`, 자식 `tvBarTitle`·`btnBack`).
- 치수: `screen_padding`, `radius_card`, `radius_pill`, `radius_sm`, `border_width`, `touch_min`, `title_text`, `body_text`, `button_text`.
- 아이콘 기존: `ic_map_pin`, `ic_trash`, `ic_chevron_right`, `ic_plus`.
- 도메인: `RoundFactory.CoursePars(courseName, pars)`, `HoleSpec(courseName, holeNo, par)`. `CourseEntity`는 `id`,`name`,`pars:List<Int>`. `VenueEntity`는 `id`,`name`. `repo.observeVenues():Flow<List<VenueEntity>>`, `repo.coursesForVenue(id):List<CourseEntity>`(suspend), `repo.completedRounds():List<Round>`(suspend), `repo.saveRound(round):Long`(suspend).
- nav_graph는 명시적 action 없이 `findNavController().navigate(R.id.<dest>)` 직접 호출.

---

## File Structure

**신규**
- `res/drawable/ic_clock.xml`, `res/drawable/ic_navigation.xml`, `res/drawable/ic_user_plus.xml`
- `res/drawable/bg_card_primary.xml`, `res/drawable/bg_index_circle.xml`
- `res/layout/view_start_venue_card.xml`, `res/layout/view_start_course_row.xml`
- `res/layout/view_player_input_row.xml`

**수정**
- `res/values/strings.xml`, `res/values/dimens.xml`
- `res/layout/fragment_start.xml`, `res/layout/fragment_player_setup.xml`
- `java/.../ui/start/StartFragment.kt`, `java/.../ui/start/PlayerSetupFragment.kt`
- `java/.../ui/start/RecentCourse.kt`, `java/.../ui/start/StartViewModel.kt`
- `java/.../ui/start/SelectionHolder.kt`
- `res/navigation/nav_graph.xml`
- `src/test/.../ui/StartViewModelTest.kt`
- 계측: `src/androidTest/.../CoreFlowTest.kt`, `CourseWizardFlowTest.kt`(경유 지점 있을 때)

**삭제**
- `java/.../ui/start/CourseSetupFragment.kt`, `java/.../ui/start/CourseToggleAdapter.kt`
- `res/layout/fragment_course_setup.xml`
- 미사용 문자열(참조 grep 후)

---

## Task 1: 리소스 (아이콘·드로어블·문자열·치수)

**Files:**
- Create: `app/src/main/res/drawable/ic_clock.xml`, `ic_navigation.xml`, `ic_user_plus.xml`, `bg_card_primary.xml`, `bg_index_circle.xml`
- Modify: `app/src/main/res/values/strings.xml`, `app/src/main/res/values/dimens.xml`

- [ ] **Step 1: 아이콘 3종 생성**

`ic_clock.xml`:
```xml
<vector xmlns:android="http://schemas.android.com/apk/res/android" android:width="24dp" android:height="24dp" android:viewportWidth="24" android:viewportHeight="24">
    <path android:strokeColor="?attr/parkMutedForeground" android:strokeWidth="2" android:strokeLineCap="round" android:strokeLineJoin="round" android:pathData="M12,3 A9,9 0 1 0 12,21 A9,9 0 0 0 12,3 Z" />
    <path android:strokeColor="?attr/parkMutedForeground" android:strokeWidth="2" android:strokeLineCap="round" android:strokeLineJoin="round" android:pathData="M12,7 L12,12 L15.5,14" />
</vector>
```

`ic_navigation.xml`:
```xml
<vector xmlns:android="http://schemas.android.com/apk/res/android" android:width="24dp" android:height="24dp" android:viewportWidth="24" android:viewportHeight="24">
    <path android:strokeColor="?attr/parkMutedForeground" android:strokeWidth="2" android:strokeLineCap="round" android:strokeLineJoin="round" android:pathData="M3,11 L21,3 L13,21 L11,13 Z" />
</vector>
```

`ic_user_plus.xml`:
```xml
<vector xmlns:android="http://schemas.android.com/apk/res/android" android:width="24dp" android:height="24dp" android:viewportWidth="24" android:viewportHeight="24">
    <path android:strokeColor="?attr/parkPrimary" android:strokeWidth="2" android:strokeLineCap="round" android:strokeLineJoin="round" android:pathData="M9,11 A4,4 0 1 0 9,3 A4,4 0 0 0 9,11 Z" />
    <path android:strokeColor="?attr/parkPrimary" android:strokeWidth="2" android:strokeLineCap="round" android:strokeLineJoin="round" android:pathData="M3,21 C3,17 5.5,15 9,15 C11,15 12.8,15.7 14,16.9" />
    <path android:strokeColor="?attr/parkPrimary" android:strokeWidth="2" android:strokeLineCap="round" android:strokeLineJoin="round" android:pathData="M18,14 L18,20 M15,17 L21,17" />
</vector>
```

- [ ] **Step 2: 드로어블 2종 생성**

`bg_card_primary.xml`:
```xml
<?xml version="1.0" encoding="utf-8"?>
<shape xmlns:android="http://schemas.android.com/apk/res/android" android:shape="rectangle">
    <solid android:color="?attr/parkPrimary" />
    <corners android:radius="@dimen/radius_card" />
</shape>
```

`bg_index_circle.xml`:
```xml
<?xml version="1.0" encoding="utf-8"?>
<shape xmlns:android="http://schemas.android.com/apk/res/android" android:shape="oval">
    <solid android:color="?attr/parkPrimary10" />
</shape>
```

- [ ] **Step 3: 문자열 추가** — `res/values/strings.xml`의 `<resources>` 안에 추가:
```xml
<string name="recent_venue_label">최근 사용한 구장</string>
<string name="recent_last_used">%1$s 마지막 사용</string>
<string name="my_courses_label">내 코스</string>
<string name="nearby_label">주변 구장</string>
<string name="nearby_placeholder">주변 구장은 준비 중입니다</string>
<string name="hole_count">%1$d홀</string>
<string name="new_template_button">새 코스 템플릿 작성</string>
<string name="player_count_title">경기 인원</string>
<string name="player_count_subtitle">함께 경기할 인원을 구성하세요</string>
<string name="add_player_button">인원 추가</string>
<string name="player_default_name">플레이어 %1$d</string>
<string name="player_name_hint">이름 입력</string>
<string name="start_game_button">게임 시작</string>
<string name="player_delete">인원 삭제</string>
```

- [ ] **Step 4: 문자열 값 변경** — 기존 `title_player_setup` 값을 교체:
```xml
<string name="title_player_setup">경기 인원 구성</string>
```
(이름 유지, 값만 변경. `title_start`는 이미 "게임 시작"이므로 그대로.)

- [ ] **Step 5: 치수 추가** — `res/values/dimens.xml`에 추가:
```xml
<dimen name="player_index_size">40dp</dimen>
<dimen name="section_gap">28dp</dimen>
<dimen name="recent_card_pad">20dp</dimen>
```

- [ ] **Step 6: 빌드 확인**

Run: `./gradlew :app:assembleDebug -q`
Expected: BUILD SUCCESSFUL (리소스 컴파일 통과)

- [ ] **Step 7: 커밋**
```bash
git add app/src/main/res/drawable/ic_clock.xml app/src/main/res/drawable/ic_navigation.xml app/src/main/res/drawable/ic_user_plus.xml app/src/main/res/drawable/bg_card_primary.xml app/src/main/res/drawable/bg_index_circle.xml app/src/main/res/values/strings.xml app/src/main/res/values/dimens.xml
git commit -m "feat: add resources for game-setup redesign (icons, bg, strings)"
```

---

## Task 2: RecentCourse에 코스명 추가 (TDD)

**Files:**
- Modify: `app/src/main/java/com/parkgolf/score/ui/start/RecentCourse.kt`
- Modify: `app/src/main/java/com/parkgolf/score/ui/start/StartViewModel.kt`
- Test: `app/src/test/java/com/parkgolf/score/ui/StartViewModelTest.kt`

- [ ] **Step 1: 실패 테스트 작성** — `StartViewModelTest.kt`를 아래로 교체:
```kotlin
package com.parkgolf.score.ui

import com.google.common.truth.Truth.assertThat
import com.parkgolf.score.domain.model.HoleSpec
import com.parkgolf.score.domain.model.Round
import com.parkgolf.score.domain.model.RoundStatus
import com.parkgolf.score.ui.start.StartViewModel
import org.junit.Test

class StartViewModelTest {
    private fun round(venue: String, course: String, holes: Int, date: Long) = Round(
        id = 0, date = date, venueName = venue, players = listOf("나"),
        holes = List(holes) { HoleSpec(course, it + 1, 3) },
        scores = listOf(List(holes) { 3 as Int? }), status = RoundStatus.COMPLETED
    )

    @Test fun recentCourses_areDistinctByVenue_newestFirst() {
        val completed = listOf(
            round("○○", "A코스", 18, 300),
            round("△△", "메인코스", 9, 200),
            round("○○", "B코스", 9, 100)
        )
        val recents = StartViewModel.deriveRecentCourses(completed, limit = 5)
        assertThat(recents.map { it.venueName }).containsExactly("○○", "△△").inOrder()
        assertThat(recents[0].holeCount).isEqualTo(18)
    }

    @Test fun recentCourses_carryCourseName_fromFirstHole() {
        val completed = listOf(round("○○", "챔피언코스", 9, 300))
        val recents = StartViewModel.deriveRecentCourses(completed, limit = 1)
        assertThat(recents).hasSize(1)
        assertThat(recents[0].courseName).isEqualTo("챔피언코스")
        assertThat(recents[0].venueName).isEqualTo("○○")
    }

    @Test fun recentCourses_empty_whenNoCompletedRounds() {
        assertThat(StartViewModel.deriveRecentCourses(emptyList(), limit = 1)).isEmpty()
    }
}
```

- [ ] **Step 2: 실패 확인**

Run: `./gradlew :app:testDebugUnitTest --tests "com.parkgolf.score.ui.StartViewModelTest" -q`
Expected: 컴파일 실패 또는 FAIL (`courseName` 미존재)

- [ ] **Step 3: RecentCourse에 courseName 추가** — `RecentCourse.kt` 전체 교체:
```kotlin
package com.parkgolf.score.ui.start

data class RecentCourse(
    val venueName: String,
    val courseName: String,
    val holeCount: Int,
    val lastPlayed: Long,
)
```

- [ ] **Step 4: deriveRecentCourses 수정** — `StartViewModel.kt` 전체 교체:
```kotlin
package com.parkgolf.score.ui.start

import androidx.lifecycle.ViewModel
import com.parkgolf.score.domain.model.Round

class StartViewModel : ViewModel() {
    companion object {
        /** Most-recent completed round per venue, newest first, up to [limit]. */
        fun deriveRecentCourses(completed: List<Round>, limit: Int): List<RecentCourse> =
            completed.sortedByDescending { it.date }
                .distinctBy { it.venueName }
                .take(limit)
                .map {
                    RecentCourse(
                        venueName = it.venueName,
                        courseName = it.holes.map { h -> h.courseName }.distinct().firstOrNull() ?: "",
                        holeCount = it.holes.size,
                        lastPlayed = it.date,
                    )
                }
    }
}
```

- [ ] **Step 5: 테스트 통과 확인**

Run: `./gradlew :app:testDebugUnitTest --tests "com.parkgolf.score.ui.StartViewModelTest" -q`
Expected: PASS (3 tests)

- [ ] **Step 6: 커밋**
```bash
git add app/src/main/java/com/parkgolf/score/ui/start/RecentCourse.kt app/src/main/java/com/parkgolf/score/ui/start/StartViewModel.kt app/src/test/java/com/parkgolf/score/ui/StartViewModelTest.kt
git commit -m "feat: carry courseName in RecentCourse for game-start recent card"
```

---

## Task 3: GameStart 레이아웃 (구장 카드/코스 행 + fragment_start.xml)

**Files:**
- Create: `app/src/main/res/layout/view_start_venue_card.xml`, `app/src/main/res/layout/view_start_course_row.xml`
- Modify: `app/src/main/res/layout/fragment_start.xml`

- [ ] **Step 1: `view_start_course_row.xml` 생성** (내 코스 카드 안의 코스 1행)
```xml
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent" android:layout_height="wrap_content"
    android:orientation="vertical">
    <LinearLayout android:id="@+id/rowCourse"
        android:layout_width="match_parent" android:layout_height="wrap_content"
        android:minHeight="@dimen/touch_min" android:gravity="center_vertical"
        android:orientation="horizontal" android:background="?attr/selectableItemBackground"
        android:paddingHorizontal="20dp" android:paddingVertical="14dp">
        <TextView android:id="@+id/tvCourseName"
            android:layout_width="wrap_content" android:layout_height="wrap_content"
            android:textSize="@dimen/body_text" android:textStyle="bold"
            android:textColor="?attr/parkForeground" />
        <TextView android:id="@+id/tvHoleCount"
            android:layout_width="wrap_content" android:layout_height="wrap_content"
            android:layout_marginStart="8dp" android:textColor="?attr/parkMutedForeground" />
        <View android:layout_width="0dp" android:layout_height="1dp" android:layout_weight="1" />
        <ImageView android:layout_width="18dp" android:layout_height="18dp"
            android:src="@drawable/ic_chevron_right" app:tint="?attr/parkMutedForeground"
            android:importantForAccessibility="no"
            xmlns:app="http://schemas.android.com/apk/res-auto" />
    </LinearLayout>
    <View android:id="@+id/divider"
        android:layout_width="match_parent" android:layout_height="1dp"
        android:background="?attr/parkBorder" android:layout_marginHorizontal="20dp" />
</LinearLayout>
```

- [ ] **Step 2: `view_start_venue_card.xml` 생성** (구장 1개 카드)
```xml
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent" android:layout_height="wrap_content"
    android:orientation="vertical" android:background="@drawable/bg_card"
    android:layout_marginBottom="12dp">
    <TextView android:id="@+id/tvVenueName"
        android:layout_width="match_parent" android:layout_height="wrap_content"
        android:paddingHorizontal="20dp" android:paddingTop="16dp" android:paddingBottom="6dp"
        android:textSize="@dimen/body_text" android:textStyle="bold"
        android:textColor="?attr/parkForeground" />
    <LinearLayout android:id="@+id/coursesContainer"
        android:layout_width="match_parent" android:layout_height="wrap_content"
        android:orientation="vertical" android:paddingBottom="4dp" />
</LinearLayout>
```

- [ ] **Step 3: `fragment_start.xml` 전체 교체**
```xml
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:layout_width="match_parent" android:layout_height="match_parent"
    android:orientation="vertical" android:background="?attr/parkBackground">

    <include android:id="@+id/topBar" layout="@layout/view_top_bar" />

    <ScrollView
        android:layout_width="match_parent" android:layout_height="0dp" android:layout_weight="1"
        android:fillViewport="true" android:padding="@dimen/screen_padding">
        <LinearLayout android:layout_width="match_parent" android:layout_height="wrap_content"
            android:orientation="vertical">

            <!-- 최근 사용한 구장 -->
            <LinearLayout android:id="@+id/sectionRecent"
                android:layout_width="match_parent" android:layout_height="wrap_content"
                android:orientation="vertical" android:layout_marginBottom="@dimen/section_gap">
                <LinearLayout android:layout_width="wrap_content" android:layout_height="wrap_content"
                    android:orientation="horizontal" android:gravity="center_vertical"
                    android:layout_marginBottom="10dp">
                    <ImageView android:layout_width="16dp" android:layout_height="16dp"
                        android:src="@drawable/ic_clock" android:importantForAccessibility="no" />
                    <TextView android:layout_width="wrap_content" android:layout_height="wrap_content"
                        android:layout_marginStart="8dp" android:text="@string/recent_venue_label"
                        android:textStyle="bold" android:textColor="?attr/parkMutedForeground" />
                </LinearLayout>
                <LinearLayout android:id="@+id/cardRecent"
                    android:layout_width="match_parent" android:layout_height="wrap_content"
                    android:orientation="vertical" android:background="@drawable/bg_card_primary"
                    android:padding="@dimen/recent_card_pad" android:elevation="4dp"
                    android:foreground="?attr/selectableItemBackground">
                    <TextView android:id="@+id/tvRecentDate"
                        android:layout_width="wrap_content" android:layout_height="wrap_content"
                        android:textColor="@color/on_primary" android:alpha="0.75"
                        android:layout_marginBottom="4dp" />
                    <TextView android:id="@+id/tvRecentVenue"
                        android:layout_width="wrap_content" android:layout_height="wrap_content"
                        android:textSize="@dimen/title_text" android:textStyle="bold"
                        android:textColor="@color/on_primary" />
                    <TextView android:id="@+id/tvRecentCourse"
                        android:layout_width="wrap_content" android:layout_height="wrap_content"
                        android:textColor="@color/on_primary" android:alpha="0.9"
                        android:layout_marginTop="2dp" />
                </LinearLayout>
            </LinearLayout>

            <!-- 내 코스 -->
            <LinearLayout android:layout_width="wrap_content" android:layout_height="wrap_content"
                android:orientation="horizontal" android:gravity="center_vertical"
                android:layout_marginBottom="10dp">
                <ImageView android:layout_width="16dp" android:layout_height="16dp"
                    android:src="@drawable/ic_map_pin" app:tint="?attr/parkMutedForeground"
                    android:importantForAccessibility="no" />
                <TextView android:layout_width="wrap_content" android:layout_height="wrap_content"
                    android:layout_marginStart="8dp" android:text="@string/my_courses_label"
                    android:textStyle="bold" android:textColor="?attr/parkMutedForeground" />
            </LinearLayout>
            <LinearLayout android:id="@+id/containerVenues"
                android:layout_width="match_parent" android:layout_height="wrap_content"
                android:orientation="vertical" android:layout_marginBottom="@dimen/section_gap" />

            <!-- 주변 구장 (플레이스홀더) -->
            <LinearLayout android:layout_width="wrap_content" android:layout_height="wrap_content"
                android:orientation="horizontal" android:gravity="center_vertical"
                android:layout_marginBottom="10dp">
                <ImageView android:layout_width="16dp" android:layout_height="16dp"
                    android:src="@drawable/ic_navigation" android:importantForAccessibility="no" />
                <TextView android:layout_width="wrap_content" android:layout_height="wrap_content"
                    android:layout_marginStart="8dp" android:text="@string/nearby_label"
                    android:textStyle="bold" android:textColor="?attr/parkMutedForeground" />
            </LinearLayout>
            <TextView android:layout_width="match_parent" android:layout_height="wrap_content"
                android:background="@drawable/bg_card" android:gravity="center"
                android:padding="20dp" android:text="@string/nearby_placeholder"
                android:textColor="?attr/parkMutedForeground" />
        </LinearLayout>
    </ScrollView>

    <!-- 하단 고정: 새 코스 템플릿 작성 -->
    <LinearLayout android:layout_width="match_parent" android:layout_height="wrap_content"
        android:orientation="vertical" android:background="?attr/parkBackground"
        android:padding="@dimen/screen_padding">
        <LinearLayout android:id="@+id/btnNewCourse"
            android:layout_width="match_parent" android:layout_height="wrap_content"
            android:minHeight="@dimen/touch_min" android:orientation="horizontal"
            android:gravity="center" android:background="@drawable/bg_dashed"
            android:paddingVertical="18dp" android:foreground="?attr/selectableItemBackground">
            <ImageView android:layout_width="20dp" android:layout_height="20dp"
                android:src="@drawable/ic_plus" app:tint="?attr/parkPrimary"
                android:importantForAccessibility="no" />
            <TextView android:layout_width="wrap_content" android:layout_height="wrap_content"
                android:layout_marginStart="8dp" android:text="@string/new_template_button"
                android:textSize="@dimen/button_text" android:textStyle="bold"
                android:textColor="?attr/parkPrimary" />
        </LinearLayout>
    </LinearLayout>
</LinearLayout>
```

- [ ] **Step 4: 빌드 확인**

Run: `./gradlew :app:assembleDebug -q`
Expected: BUILD SUCCESSFUL (아직 StartFragment는 구 뷰 id 참조로 실패할 수 있음 → Task 4에서 함께 통과. 이 단계에서 실패하면 Task 4까지 진행 후 빌드)

> 주의: `fragment_start.xml`에서 `rvRecent`/`rvPresets`/구 문자열 id가 사라지므로 `StartFragment.kt`가 컴파일 실패한다. Task 4와 한 커밋으로 묶어 진행할 것.

- [ ] **Step 5: (Task 4와 공동 커밋)** — 이 태스크만으로는 커밋하지 않고 Task 4 완료 후 함께 커밋.

---

## Task 4: StartFragment 재작성

**Files:**
- Modify: `app/src/main/java/com/parkgolf/score/ui/start/StartFragment.kt`

- [ ] **Step 1: `StartFragment.kt` 전체 교체**
```kotlin
package com.parkgolf.score.ui.start

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import androidx.core.view.isGone
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.parkgolf.score.App
import com.parkgolf.score.R
import com.parkgolf.score.data.db.entity.CourseEntity
import com.parkgolf.score.data.db.entity.VenueEntity
import com.parkgolf.score.databinding.FragmentStartBinding
import com.parkgolf.score.databinding.ViewStartCourseRowBinding
import com.parkgolf.score.databinding.ViewStartVenueCardBinding
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class StartFragment : Fragment(R.layout.fragment_start) {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val binding = FragmentStartBinding.bind(view)
        binding.topBar.tvBarTitle.text = getString(R.string.title_start)
        binding.topBar.btnBack.setOnClickListener { findNavController().popBackStack() }
        val repo = App.repo(requireActivity().application)

        binding.btnNewCourse.setOnClickListener {
            SelectionHolder.reset()
            findNavController().currentBackStackEntry?.savedStateHandle?.apply {
                set("wizardVenueId", 0L)
                set("wizardCourseId", 0L)
            }
            findNavController().navigate(R.id.courseWizardFragment)
        }

        // 최근 사용한 구장 (해당 코스가 현재 존재할 때만)
        viewLifecycleOwner.lifecycleScope.launch {
            val recents = StartViewModel.deriveRecentCourses(repo.completedRounds(), limit = 1)
            val recent = recents.firstOrNull()
            val venues = repo.observeVenues().first()
            val resolved = recent?.let { r ->
                val venue = venues.firstOrNull { it.name == r.venueName }
                val course = venue?.let { repo.coursesForVenue(it.id).firstOrNull { c -> c.name == r.courseName } }
                if (venue != null && course != null) Triple(venue, course, r) else null
            }
            if (resolved == null) {
                binding.sectionRecent.isGone = true
            } else {
                val (venue, course, r) = resolved
                binding.sectionRecent.isGone = false
                val fmt = SimpleDateFormat("M월 d일", Locale.KOREA)
                binding.tvRecentDate.text =
                    getString(R.string.recent_last_used, fmt.format(Date(r.lastPlayed)))
                binding.tvRecentVenue.text = venue.name
                binding.tvRecentCourse.text = course.name
                binding.cardRecent.setOnClickListener {
                    selectCourse(venue.id, venue.name, course.id)
                }
            }
        }

        // 내 코스 (구장별 카드)
        viewLifecycleOwner.lifecycleScope.launch {
            repo.observeVenues().collect { venues ->
                binding.containerVenues.removeAllViews()
                val inflater = LayoutInflater.from(requireContext())
                for (venue in venues) {
                    val courses = repo.coursesForVenue(venue.id)
                    if (courses.isEmpty()) continue
                    addVenueCard(inflater, binding, venue, courses)
                }
            }
        }
    }

    private fun addVenueCard(
        inflater: LayoutInflater,
        binding: FragmentStartBinding,
        venue: VenueEntity,
        courses: List<CourseEntity>,
    ) {
        val card = ViewStartVenueCardBinding.inflate(inflater, binding.containerVenues, false)
        card.tvVenueName.text = venue.name
        courses.forEachIndexed { idx, course ->
            val row = ViewStartCourseRowBinding.inflate(inflater, card.coursesContainer, false)
            row.tvCourseName.text = course.name
            row.tvHoleCount.text = getString(R.string.hole_count, course.pars.size)
            row.divider.isGone = idx == courses.lastIndex
            row.rowCourse.setOnClickListener { selectCourse(venue.id, venue.name, course.id) }
            card.coursesContainer.addView(row.root)
        }
        binding.containerVenues.addView(card.root)
    }

    private fun selectCourse(venueId: Long, venueName: String, courseId: Long) {
        SelectionHolder.reset()
        SelectionHolder.venueId = venueId
        SelectionHolder.venueName = venueName
        SelectionHolder.chosenCourseIds.add(courseId)
        findNavController().navigate(R.id.playerSetupFragment)
    }
}
```

> 참고: `CourseEntity`/`VenueEntity`의 실제 패키지가 다르면 import를 맞춘다(`repo.coursesForVenue` 반환 타입으로 확인). `pars` 프로퍼티명이 다르면 홀 수 계산부를 맞춘다.

- [ ] **Step 2: 빌드 확인**

Run: `./gradlew :app:assembleDebug -q`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: 커밋 (Task 3 + 4)**
```bash
git add app/src/main/res/layout/view_start_venue_card.xml app/src/main/res/layout/view_start_course_row.xml app/src/main/res/layout/fragment_start.xml app/src/main/java/com/parkgolf/score/ui/start/StartFragment.kt
git commit -m "feat: redesign GameStart (recent card, my-courses, direct course select)"
```

---

## Task 5: PlayerSetup 레이아웃 (인원 행 + fragment_player_setup.xml)

**Files:**
- Create: `app/src/main/res/layout/view_player_input_row.xml`
- Modify: `app/src/main/res/layout/fragment_player_setup.xml`

- [ ] **Step 1: `view_player_input_row.xml` 생성**
```xml
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent" android:layout_height="wrap_content"
    android:orientation="horizontal" android:gravity="center_vertical"
    android:layout_marginBottom="12dp">
    <TextView android:id="@+id/tvIndex"
        android:layout_width="@dimen/player_index_size" android:layout_height="@dimen/player_index_size"
        android:background="@drawable/bg_index_circle" android:gravity="center"
        android:textStyle="bold" android:textColor="?attr/parkPrimary"
        android:importantForAccessibility="no" />
    <EditText android:id="@+id/etName"
        android:layout_width="0dp" android:layout_height="wrap_content" android:layout_weight="1"
        android:layout_marginStart="12dp" android:background="@drawable/bg_input"
        android:minHeight="@dimen/touch_min" android:paddingHorizontal="16dp"
        android:textSize="@dimen/body_text" android:textStyle="bold"
        android:textColor="?attr/parkForeground" android:hint="@string/player_name_hint"
        android:inputType="text" android:maxLines="1" />
    <ImageButton android:id="@+id/btnDelete"
        android:layout_width="48dp" android:layout_height="48dp"
        android:layout_marginStart="8dp" android:background="?attr/selectableItemBackgroundBorderless"
        android:src="@drawable/ic_trash" android:contentDescription="@string/player_delete"
        xmlns:app="http://schemas.android.com/apk/res-auto"
        app:tint="?attr/parkMutedForeground" />
</LinearLayout>
```

- [ ] **Step 2: `fragment_player_setup.xml` 전체 교체**
```xml
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:layout_width="match_parent" android:layout_height="match_parent"
    android:orientation="vertical" android:background="?attr/parkBackground">

    <include android:id="@+id/topBar" layout="@layout/view_top_bar" />

    <ScrollView
        android:layout_width="match_parent" android:layout_height="0dp" android:layout_weight="1"
        android:fillViewport="true" android:padding="@dimen/screen_padding">
        <LinearLayout android:layout_width="match_parent" android:layout_height="wrap_content"
            android:orientation="vertical">

            <!-- 코스 정보 카드 -->
            <LinearLayout android:layout_width="match_parent" android:layout_height="wrap_content"
                android:orientation="horizontal" android:gravity="center_vertical"
                android:background="@drawable/bg_card" android:padding="16dp"
                android:layout_marginBottom="24dp">
                <ImageView android:layout_width="40dp" android:layout_height="40dp"
                    android:background="@drawable/bg_menu_icon" android:padding="10dp"
                    android:src="@drawable/ic_map_pin" app:tint="?attr/parkPrimary"
                    android:importantForAccessibility="no" />
                <LinearLayout android:layout_width="wrap_content" android:layout_height="wrap_content"
                    android:orientation="vertical" android:layout_marginStart="12dp">
                    <TextView android:id="@+id/tvVenueName"
                        android:layout_width="wrap_content" android:layout_height="wrap_content"
                        android:textColor="?attr/parkMutedForeground" />
                    <TextView android:id="@+id/tvCourseName"
                        android:layout_width="wrap_content" android:layout_height="wrap_content"
                        android:textSize="@dimen/title_text" android:textStyle="bold"
                        android:textColor="?attr/parkForeground" />
                </LinearLayout>
            </LinearLayout>

            <!-- 제목/부제 -->
            <TextView android:layout_width="wrap_content" android:layout_height="wrap_content"
                android:text="@string/player_count_title" android:textSize="@dimen/title_text"
                android:textStyle="bold" android:textColor="?attr/parkForeground" />
            <TextView android:layout_width="wrap_content" android:layout_height="wrap_content"
                android:text="@string/player_count_subtitle" android:textColor="?attr/parkMutedForeground"
                android:layout_marginTop="2dp" android:layout_marginBottom="20dp" />

            <!-- 인원 행 목록 -->
            <LinearLayout android:id="@+id/playersContainer"
                android:layout_width="match_parent" android:layout_height="wrap_content"
                android:orientation="vertical" />

            <!-- 인원 추가 -->
            <LinearLayout android:id="@+id/btnAddPlayer"
                android:layout_width="match_parent" android:layout_height="wrap_content"
                android:minHeight="@dimen/touch_min" android:orientation="horizontal"
                android:gravity="center" android:background="@drawable/bg_dashed"
                android:paddingVertical="14dp" android:layout_marginTop="4dp"
                android:foreground="?attr/selectableItemBackground">
                <ImageView android:layout_width="20dp" android:layout_height="20dp"
                    android:src="@drawable/ic_user_plus" android:importantForAccessibility="no" />
                <TextView android:layout_width="wrap_content" android:layout_height="wrap_content"
                    android:layout_marginStart="8dp" android:text="@string/add_player_button"
                    android:textStyle="bold" android:textColor="?attr/parkPrimary" />
            </LinearLayout>
        </LinearLayout>
    </ScrollView>

    <!-- 하단 고정: 게임 시작 -->
    <LinearLayout android:layout_width="match_parent" android:layout_height="wrap_content"
        android:orientation="vertical" android:background="?attr/parkBackground"
        android:padding="@dimen/screen_padding">
        <Button android:id="@+id/btnStartPlay"
            android:layout_width="match_parent" android:layout_height="wrap_content"
            android:minHeight="72dp" android:text="@string/start_game_button"
            android:textSize="@dimen/title_text" android:textStyle="bold" />
    </LinearLayout>
</LinearLayout>
```

- [ ] **Step 3: (Task 6와 공동 커밋)** — `PlayerSetupFragment.kt`가 구 로직으로 컴파일 실패하므로 Task 6과 함께 커밋.

---

## Task 6: PlayerSetupFragment 재작성 (무제한 인원 + 프리필 + 유효성)

**Files:**
- Modify: `app/src/main/java/com/parkgolf/score/ui/start/PlayerSetupFragment.kt`

- [ ] **Step 1: `PlayerSetupFragment.kt` 전체 교체**
```kotlin
package com.parkgolf.score.ui.start

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.parkgolf.score.App
import com.parkgolf.score.R
import com.parkgolf.score.databinding.FragmentPlayerSetupBinding
import com.parkgolf.score.databinding.ViewPlayerInputRowBinding
import com.parkgolf.score.domain.RoundFactory
import com.parkgolf.score.ui.RoundSessionViewModel
import kotlinx.coroutines.launch

class PlayerSetupFragment : Fragment(R.layout.fragment_player_setup) {
    private val session: RoundSessionViewModel by activityViewModels()
    private val rows = mutableListOf<ViewPlayerInputRowBinding>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val binding = FragmentPlayerSetupBinding.bind(view)
        binding.topBar.tvBarTitle.text = getString(R.string.title_player_setup)
        binding.topBar.btnBack.setOnClickListener { findNavController().popBackStack() }
        val repo = App.repo(requireActivity().application)
        val defaultName = App.settings(requireActivity().application).defaultPlayerName

        // 코스 정보 카드
        binding.tvVenueName.text = SelectionHolder.venueName ?: ""
        viewLifecycleOwner.lifecycleScope.launch {
            val venueId = SelectionHolder.venueId
            val courseId = SelectionHolder.chosenCourseIds.firstOrNull()
            if (venueId != null && courseId != null) {
                val course = repo.coursesForVenue(venueId).firstOrNull { it.id == courseId }
                binding.tvCourseName.text = course?.name ?: ""
            }
        }

        val inflater = LayoutInflater.from(requireContext())
        fun refresh() {
            rows.forEachIndexed { i, row ->
                row.tvIndex.text = (i + 1).toString()
                row.btnDelete.isEnabled = rows.size > 1
                row.btnDelete.alpha = if (rows.size > 1) 1f else 0.2f
            }
            val canStart = rows.all { it.etName.text.toString().trim().isNotEmpty() }
            binding.btnStartPlay.isEnabled = canStart
            binding.btnStartPlay.alpha = if (canStart) 1f else 0.3f
        }
        fun addRow(initial: String, focus: Boolean) {
            val row = ViewPlayerInputRowBinding.inflate(inflater, binding.playersContainer, false)
            row.etName.setText(initial)
            row.etName.doAfterTextChanged { refresh() }
            row.btnDelete.setOnClickListener {
                if (rows.size <= 1) return@setOnClickListener
                binding.playersContainer.removeView(row.root)
                rows.remove(row)
                refresh()
            }
            rows.add(row)
            binding.playersContainer.addView(row.root)
            refresh()
            if (focus) { row.etName.requestFocus(); row.etName.selectAll() }
        }

        addRow(defaultName, focus = false)
        binding.btnAddPlayer.setOnClickListener {
            addRow(getString(R.string.player_default_name, rows.size + 1), focus = true)
        }

        binding.btnStartPlay.setOnClickListener {
            val venueId = SelectionHolder.venueId ?: return@setOnClickListener
            viewLifecycleOwner.lifecycleScope.launch {
                val courses = repo.coursesForVenue(venueId)
                val coursePars = SelectionHolder.chosenCourseIds.mapNotNull { id ->
                    courses.firstOrNull { it.id == id }?.let { RoundFactory.CoursePars(it.name, it.pars) }
                }
                if (coursePars.isEmpty()) return@launch
                val players = rows.mapIndexed { i, row ->
                    val t = row.etName.text.toString().trim()
                    if (t.isNotEmpty()) t
                    else if (i == 0) defaultName else getString(R.string.companion_default, i)
                }
                val venueName = SelectionHolder.venueName ?: ""
                val round = RoundFactory.newRound(venueName, players, coursePars, System.currentTimeMillis())
                val id = repo.saveRound(round)
                session.startRound(round.copy(id = id))
                findNavController().navigate(R.id.holeInputFragment)
            }
        }
    }
}
```

- [ ] **Step 2: 빌드 확인**

Run: `./gradlew :app:assembleDebug -q`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: 커밋 (Task 5 + 6)**
```bash
git add app/src/main/res/layout/view_player_input_row.xml app/src/main/res/layout/fragment_player_setup.xml app/src/main/java/com/parkgolf/score/ui/start/PlayerSetupFragment.kt
git commit -m "feat: redesign PlayerSetup (course card, numbered rows, unlimited players)"
```

---

## Task 7: 코스 결합 단계 제거

**Files:**
- Modify: `app/src/main/res/navigation/nav_graph.xml`
- Delete: `CourseSetupFragment.kt`, `CourseToggleAdapter.kt`, `fragment_course_setup.xml`
- Modify: `app/src/main/res/values/strings.xml`, `SelectionHolder.kt`(선택)

- [ ] **Step 1: nav_graph에서 courseSetupFragment 목적지 제거** — `nav_graph.xml`에서 아래 두 줄 삭제:
```xml
    <fragment android:id="@+id/courseSetupFragment"
        android:name="com.parkgolf.score.ui.start.CourseSetupFragment" />
```

- [ ] **Step 2: 파일 삭제**
```bash
git rm app/src/main/java/com/parkgolf/score/ui/start/CourseSetupFragment.kt \
       app/src/main/java/com/parkgolf/score/ui/start/CourseToggleAdapter.kt \
       app/src/main/res/layout/fragment_course_setup.xml
```

- [ ] **Step 3: 미사용 참조 grep**

Run: `grep -rn "courseSetupFragment\|CourseSetupFragment\|CourseToggleAdapter\|fragment_course_setup" app/src`
Expected: 결과 없음 (없으면 통과)

- [ ] **Step 4: 미사용 문자열 제거 (grep 확인 후)** — 각 문자열이 `app/src`에서 더 이상 참조되지 않는지 확인 후 `strings.xml`에서 제거:

Run 예: `grep -rn "start_where\|recent_courses\|saved_presets\|nearby_courses_soon\|hole_config\|total_holes\b\|next_step\|title_course_setup\|players_title\|add_player\b\|start_play\|new_course\b" app/src`

- 참조 0인 것만 제거: `start_where`, `recent_courses`, `saved_presets`, `nearby_courses_soon`, `hole_config`, `total_holes`, `next_step`, `title_course_setup`, `players_title`, `add_player`, `start_play`, `new_course`.
- `companion_default`, `total_holes_label`은 유지(다른 참조 존재 가능 — grep로 확인).

> 주의: `total_holes`와 `total_holes_label`은 다른 문자열이다. `grep "total_holes\b"`로 정확히 구분해 `total_holes`만 제거.

- [ ] **Step 5: SelectionHolder 정리(선택)** — `players` 필드가 `app/src`에서 미참조면 제거:

Run: `grep -rn "SelectionHolder.players" app/src`
- 결과 0이면 `SelectionHolder.kt`의 `players` 필드와 `reset()`의 `players = ...` 라인 제거. 참조 있으면 그대로 둔다.

- [ ] **Step 6: 빌드 + 단위 테스트**

Run: `./gradlew :app:assembleDebug :app:testDebugUnitTest -q`
Expected: BUILD SUCCESSFUL

- [ ] **Step 7: 커밋**
```bash
git add -A
git commit -m "refactor: remove course-combine step (CourseSetup) from game flow"
```

---

## Task 8: 계측 테스트 갱신 + 회귀 확인

**Files:**
- Modify: `app/src/androidTest/java/com/parkgolf/score/CoreFlowTest.kt` (경로는 실제 위치 확인)
- Modify: `app/src/androidTest/.../CourseWizardFlowTest.kt` (필요 시)

- [ ] **Step 1: 계측 테스트에서 CourseSetup 경유 지점 확인**

Run: `grep -rn "courseSetupFragment\|title_course_setup\|hole_config\|next_step\|btnStartPlay\|start_play\|rvPresets\|rvRecent\|btnNewCourse" app/src/androidTest`
Expected: 목록 확인. 아래 대응 규칙으로 각 단정 갱신.

- [ ] **Step 2: 흐름 단정 갱신**

CourseSetup을 거치던 단정을 새 흐름으로 교체:
- 코스 선택 후 곧바로 PlayerSetup 화면이 뜨는지 확인. PlayerSetup 식별자로 `withText(R.string.player_count_title)`(="경기 인원") 또는 시작 버튼 `withId(R.id.btnStartPlay)` 사용.
- 게임 시작 버튼 텍스트 단정이 있으면 `R.string.start_play` → `R.string.start_game_button`.
- 시작 화면에서 코스 목록 진입 단정이 `rvPresets`/`rvRecent`에 의존했다면, 새 레이아웃은 `containerVenues` 안에 동적 뷰이므로 `withText(<코스명>)` 기반 클릭으로 교체.

예(패턴, 실제 파일에 맞게 조정):
```kotlin
// 코스 선택 → 인원 구성 화면 진입
onView(withText("A코스")).perform(click())
onView(withText(R.string.player_count_title)).check(matches(isDisplayed()))
```

- [ ] **Step 3: (에뮬레이터 있으면) 계측 실행**

Run: `./gradlew :app:connectedDebugAndroidTest`
Expected: BUILD SUCCESSFUL (전체 계측 통과). 실패 시 stale DB면 `adb uninstall com.parkgolf.score` 후 재실행.

> 에뮬레이터가 없으면 이 단계는 컨트롤러가 수동 확인으로 대체하고, 최소한 `:app:compileDebugAndroidTestKotlin`으로 계측 컴파일만이라도 통과시킨다:
> Run: `./gradlew :app:compileDebugAndroidTestKotlin -q` → BUILD SUCCESSFUL

- [ ] **Step 4: 전체 회귀 grep**

Run: `grep -rn "CourseSetup\|courseSetupFragment\|chosenCourseIds" app/src/main | grep -v "chosenCourseIds.add\|chosenCourseIds.firstOrNull\|chosenCourseIds.mapNotNull\|chosenCourseIds.clear\|chosenCourseIds:"`
Expected: 남은 결합 관련 잔재 없음(정상적 단일 사용만 남음).

- [ ] **Step 5: 최종 빌드**

Run: `./gradlew :app:assembleDebug :app:testDebugUnitTest -q`
Expected: BUILD SUCCESSFUL

- [ ] **Step 6: 커밋**
```bash
git add -A
git commit -m "test: update instrumented flow for single-course game start"
```

---

## 완료 후

모든 태스크 완료 시 superpowers:finishing-a-development-branch로 마무리(단위 테스트 통과 확인 → main 병합 옵션 제시). 이후 5b(경기 진행: GamePlay·ScoreOverview·GameComplete) 브레인스토밍으로 이어간다.
