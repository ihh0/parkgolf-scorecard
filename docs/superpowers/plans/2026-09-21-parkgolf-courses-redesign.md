# 파크골프 코스 관리 재디자인 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 코스 관리(`MyCoursesFragment`)를 목업(`CourseManagement.tsx`)에 맞춰 확장형 구장 카드 + 코스 행 + 빈 상태로 재작성한다.

**Architecture:** 구장은 보통 적으므로 RecyclerView 대신 ScrollView + 컨테이너에 구장 카드/코스 행을 프로그램적으로 빌드(기존 StartFragment 패턴). 펼침 상태는 `collapsed: Set<Long>`(비어있으면 전체 펼침)로 관리. 삭제는 확인 다이얼로그 + 수동 캐스케이드(구장 삭제 시 코스 동반, 코스 마지막 삭제 시 구장 삭제). 순수 헬퍼 `CourseFormat` 신규(TDD).

**Tech Stack:** Kotlin, Android View/XML + ViewBinding, MVVM(Fragment→Repo→Room), Jetpack Navigation.

**태스크 순서 원칙:** 리소스·헬퍼·저장소·레이아웃을 먼저 만들고, 이들을 참조하는 `MyCoursesFragment`를 마지막에 한 번에 교체(어댑터 제거 포함)해 각 태스크가 독립적으로 빌드되게 한다.

---

## File Structure
- **신규** `domain/CourseFormat.kt` + `test/.../CourseFormatTest.kt` — pars → "N홀 · 파X" 순수 포맷(TDD).
- **수정** `data/ParkGolfRepository.kt` / `ParkGolfRepositoryImpl.kt` — `deleteVenueWithCourses(venueId)`.
- **신규** `res/layout/view_course_venue_card.xml`, `res/layout/view_course_row.xml`.
- **수정** `res/layout/fragment_my_courses.xml` — 스크롤+컨테이너+점선 버튼+빈 상태.
- **수정** `ui/courses/MyCoursesFragment.kt` — 전면 재작성.
- **삭제** `ui/courses/VenueAdapter.kt`, `res/layout/item_venue.xml`.
- **신규 리소스** `ic_pencil.xml`, `ic_chevron_down.xml`, `ic_chevron_up.xml`, strings.

---

## Task 1: 리소스(문자열·아이콘)

**Files:**
- Modify: `app/src/main/res/values/strings.xml`
- Create: `app/src/main/res/drawable/ic_pencil.xml`, `ic_chevron_down.xml`, `ic_chevron_up.xml`

- [ ] **Step 1: strings.xml — `</resources>` 직전에 추가**
```xml
<!-- course management redesign -->
<string name="course_count">%1$d개 코스</string>
<string name="courses_empty_title">저장된 코스 없음</string>
<string name="courses_empty_desc">코스 템플릿을 작성해 추가하세요</string>
<string name="create_template">코스 템플릿 작성</string>
<string name="delete_venue_confirm">이 구장을 삭제할까요?</string>
<string name="delete_course_confirm">이 코스를 삭제할까요?</string>
```

- [ ] **Step 2: `ic_pencil.xml` (lucide pencil)**
```xml
<vector xmlns:android="http://schemas.android.com/apk/res/android" android:width="24dp" android:height="24dp" android:viewportWidth="24" android:viewportHeight="24">
    <path android:strokeColor="?attr/parkMutedForeground" android:strokeWidth="2" android:strokeLineCap="round" android:strokeLineJoin="round" android:pathData="M17,3 A2.85,2.83 0 1 1 21,7 L7.5,20.5 L2,22 L3.5,16.5 Z M15,5 L19,9" />
</vector>
```

- [ ] **Step 3: `ic_chevron_down.xml`**
```xml
<vector xmlns:android="http://schemas.android.com/apk/res/android" android:width="24dp" android:height="24dp" android:viewportWidth="24" android:viewportHeight="24">
    <path android:strokeColor="?attr/parkMutedForeground" android:strokeWidth="2" android:strokeLineCap="round" android:strokeLineJoin="round" android:pathData="M6,9 L12,15 L18,9" />
</vector>
```

- [ ] **Step 4: `ic_chevron_up.xml`**
```xml
<vector xmlns:android="http://schemas.android.com/apk/res/android" android:width="24dp" android:height="24dp" android:viewportWidth="24" android:viewportHeight="24">
    <path android:strokeColor="?attr/parkMutedForeground" android:strokeWidth="2" android:strokeLineCap="round" android:strokeLineJoin="round" android:pathData="M18,15 L12,9 L6,15" />
</vector>
```

- [ ] **Step 5: 빌드** — `./gradlew :app:assembleDebug` → BUILD SUCCESSFUL
- [ ] **Step 6: 커밋**
```bash
git add app/src/main/res/values/strings.xml app/src/main/res/drawable/ic_pencil.xml app/src/main/res/drawable/ic_chevron_down.xml app/src/main/res/drawable/ic_chevron_up.xml
git commit -m "chore: strings and icons for course management redesign"
```

---

## Task 2: CourseFormat 순수 헬퍼 (TDD)

**Files:**
- Create: `app/src/main/java/com/parkgolf/score/domain/CourseFormat.kt`
- Test: `app/src/test/java/com/parkgolf/score/domain/CourseFormatTest.kt`

- [ ] **Step 1: 실패 테스트**
```kotlin
package com.parkgolf.score.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class CourseFormatTest {
    @Test fun holesPar_sums_pars_and_counts_holes() {
        assertEquals("9홀 · 파27", CourseFormat.holesPar(List(9) { 3 }))
    }
    @Test fun holesPar_mixed_pars() {
        assertEquals("3홀 · 파10", CourseFormat.holesPar(listOf(3, 4, 3)))
    }
    @Test fun holesPar_empty() {
        assertEquals("0홀 · 파0", CourseFormat.holesPar(emptyList()))
    }
}
```

- [ ] **Step 2: 실패 확인** — `./gradlew :app:testDebugUnitTest --tests "com.parkgolf.score.domain.CourseFormatTest"` → FAIL
- [ ] **Step 3: 구현**
```kotlin
package com.parkgolf.score.domain

/** 코스 목록용 순수 포맷 헬퍼. */
object CourseFormat {
    /** "N홀 · 파X" (X = 파 합계). */
    fun holesPar(pars: List<Int>): String = "${pars.size}홀 · 파${pars.sum()}"
}
```

- [ ] **Step 4: 통과 확인** — 같은 명령 → PASS (3 tests)
- [ ] **Step 5: 커밋**
```bash
git add app/src/main/java/com/parkgolf/score/domain/CourseFormat.kt app/src/test/java/com/parkgolf/score/domain/CourseFormatTest.kt
git commit -m "feat: CourseFormat helper for holes/par summary"
```

---

## Task 3: 저장소 — 구장 캐스케이드 삭제

**Files:**
- Modify: `app/src/main/java/com/parkgolf/score/data/ParkGolfRepository.kt`
- Modify: `app/src/main/java/com/parkgolf/score/data/ParkGolfRepositoryImpl.kt`

- [ ] **Step 1: 인터페이스에 메서드 추가** (`deleteCourse` 선언 아래에 추가)
```kotlin
    suspend fun deleteVenueWithCourses(venueId: Long)
```

- [ ] **Step 2: 구현 추가** (`deleteVenue`/`deleteCourse` 구현 근처에 추가)
```kotlin
    override suspend fun deleteVenueWithCourses(venueId: Long) {
        courseDao.forVenue(venueId).forEach { courseDao.delete(it) }
        venueDao.byId(venueId)?.let { venueDao.delete(it) }
    }
```
참고: `courseDao.forVenue(venueId)`, `courseDao.delete(course)`, `venueDao.byId(id)`, `venueDao.delete(venue)`는 이미 존재.

- [ ] **Step 3: 빌드** — `./gradlew :app:assembleDebug` → BUILD SUCCESSFUL
- [ ] **Step 4: 커밋**
```bash
git add app/src/main/java/com/parkgolf/score/data/ParkGolfRepository.kt app/src/main/java/com/parkgolf/score/data/ParkGolfRepositoryImpl.kt
git commit -m "feat: repo deleteVenueWithCourses cascade"
```

---

## Task 4: 구장 카드 / 코스 행 레이아웃

**Files:**
- Create: `app/src/main/res/layout/view_course_venue_card.xml`
- Create: `app/src/main/res/layout/view_course_row.xml`

- [ ] **Step 1: `view_course_venue_card.xml`**
```xml
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:layout_width="match_parent" android:layout_height="wrap_content"
    android:orientation="vertical" android:background="@drawable/bg_card"
    android:layout_marginBottom="12dp">

    <!-- 헤더 -->
    <LinearLayout android:layout_width="match_parent" android:layout_height="wrap_content"
        android:orientation="horizontal" android:gravity="center_vertical"
        android:paddingHorizontal="12dp" android:paddingVertical="10dp">
        <LinearLayout android:id="@+id/headerClick"
            android:layout_width="0dp" android:layout_height="wrap_content" android:layout_weight="1"
            android:orientation="horizontal" android:gravity="center_vertical"
            android:minHeight="@dimen/touch_min"
            android:background="?attr/selectableItemBackground">
            <FrameLayout android:layout_width="44dp" android:layout_height="44dp"
                android:background="@drawable/bg_menu_icon">
                <ImageView android:layout_width="match_parent" android:layout_height="match_parent"
                    android:padding="12dp" android:src="@drawable/ic_map_pin"
                    app:tint="?attr/parkPrimary" android:importantForAccessibility="no" />
            </FrameLayout>
            <LinearLayout android:layout_width="wrap_content" android:layout_height="wrap_content"
                android:orientation="vertical" android:layout_marginStart="12dp">
                <TextView android:id="@+id/tvVenueName"
                    android:layout_width="wrap_content" android:layout_height="wrap_content"
                    android:textColor="?attr/parkForeground" android:textSize="18sp"
                    android:textStyle="bold" />
                <TextView android:id="@+id/tvCourseCount"
                    android:layout_width="wrap_content" android:layout_height="wrap_content"
                    android:textColor="?attr/parkMutedForeground" android:textSize="14sp" />
            </LinearLayout>
        </LinearLayout>
        <ImageView android:id="@+id/btnDeleteVenue"
            android:layout_width="44dp" android:layout_height="44dp"
            android:padding="12dp" android:src="@drawable/ic_trash"
            app:tint="?attr/parkMutedForeground" android:clickable="true" android:focusable="true"
            android:background="?attr/selectableItemBackgroundBorderless"
            android:contentDescription="@string/delete" />
        <ImageView android:id="@+id/btnToggle"
            android:layout_width="44dp" android:layout_height="44dp"
            android:padding="11dp" android:src="@drawable/ic_chevron_down"
            app:tint="?attr/parkMutedForeground" android:clickable="true" android:focusable="true"
            android:background="?attr/selectableItemBackgroundBorderless"
            android:importantForAccessibility="no" />
    </LinearLayout>

    <View android:id="@+id/topDivider"
        android:layout_width="match_parent" android:layout_height="2dp"
        android:background="?attr/parkBorder" />

    <LinearLayout android:id="@+id/coursesContainer"
        android:layout_width="match_parent" android:layout_height="wrap_content"
        android:orientation="vertical" />
</LinearLayout>
```

- [ ] **Step 2: `view_course_row.xml`**
```xml
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:layout_width="match_parent" android:layout_height="wrap_content"
    android:orientation="vertical">
    <LinearLayout android:layout_width="match_parent" android:layout_height="wrap_content"
        android:orientation="horizontal" android:gravity="center_vertical"
        android:minHeight="@dimen/touch_min" android:paddingHorizontal="12dp"
        android:paddingVertical="6dp">
        <LinearLayout android:layout_width="0dp" android:layout_height="wrap_content"
            android:layout_weight="1" android:orientation="vertical" android:paddingStart="8dp">
            <TextView android:id="@+id/tvCourseName"
                android:layout_width="wrap_content" android:layout_height="wrap_content"
                android:textColor="?attr/parkForeground" android:textSize="16sp"
                android:textStyle="bold" android:singleLine="true" android:ellipsize="end" />
            <TextView android:id="@+id/tvHolesPar"
                android:layout_width="wrap_content" android:layout_height="wrap_content"
                android:textColor="?attr/parkMutedForeground" android:textSize="14sp" />
        </LinearLayout>
        <ImageView android:id="@+id/btnEditCourse"
            android:layout_width="44dp" android:layout_height="44dp"
            android:padding="13dp" android:src="@drawable/ic_pencil"
            app:tint="?attr/parkMutedForeground" android:clickable="true" android:focusable="true"
            android:background="?attr/selectableItemBackgroundBorderless"
            android:contentDescription="@string/wizard_title_edit" />
        <ImageView android:id="@+id/btnDeleteCourse"
            android:layout_width="44dp" android:layout_height="44dp"
            android:padding="13dp" android:src="@drawable/ic_trash"
            app:tint="?attr/parkMutedForeground" android:clickable="true" android:focusable="true"
            android:background="?attr/selectableItemBackgroundBorderless"
            android:contentDescription="@string/delete" />
    </LinearLayout>
    <View android:id="@+id/divider"
        android:layout_width="match_parent" android:layout_height="1dp"
        android:background="?attr/parkBorder" android:layout_marginHorizontal="12dp" />
</LinearLayout>
```

- [ ] **Step 3: 빌드** — `./gradlew :app:assembleDebug` → BUILD SUCCESSFUL
- [ ] **Step 4: 커밋**
```bash
git add app/src/main/res/layout/view_course_venue_card.xml app/src/main/res/layout/view_course_row.xml
git commit -m "feat: venue card and course row layouts (mockup)"
```

---

## Task 5: 코스 관리 화면 레이아웃

**Files:**
- Modify: `app/src/main/res/layout/fragment_my_courses.xml`

- [ ] **Step 1: 전체 교체**
```xml
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:layout_width="match_parent" android:layout_height="match_parent"
    android:orientation="vertical" android:background="?attr/parkBackground">
    <include android:id="@+id/topBar" layout="@layout/view_top_bar" />

    <FrameLayout android:layout_width="match_parent" android:layout_height="0dp"
        android:layout_weight="1">

        <ScrollView android:id="@+id/scrollContent"
            android:layout_width="match_parent" android:layout_height="match_parent"
            android:fillViewport="true">
            <LinearLayout android:layout_width="match_parent" android:layout_height="wrap_content"
                android:orientation="vertical" android:padding="@dimen/screen_padding">
                <LinearLayout android:id="@+id/containerVenues"
                    android:layout_width="match_parent" android:layout_height="wrap_content"
                    android:orientation="vertical" />
                <LinearLayout android:id="@+id/btnNewTemplate"
                    android:layout_width="match_parent" android:layout_height="wrap_content"
                    android:minHeight="@dimen/touch_min" android:orientation="horizontal"
                    android:gravity="center" android:background="@drawable/bg_dashed"
                    android:paddingVertical="18dp" android:foreground="?attr/selectableItemBackground"
                    android:layout_marginTop="4dp">
                    <ImageView android:layout_width="20dp" android:layout_height="20dp"
                        android:src="@drawable/ic_plus" app:tint="?attr/parkPrimary"
                        android:importantForAccessibility="no" />
                    <TextView android:layout_width="wrap_content" android:layout_height="wrap_content"
                        android:layout_marginStart="8dp" android:text="@string/new_template_button"
                        android:textSize="@dimen/button_text" android:textStyle="bold"
                        android:textColor="?attr/parkPrimary" />
                </LinearLayout>
            </LinearLayout>
        </ScrollView>

        <!-- 빈 상태 -->
        <LinearLayout android:id="@+id/emptyView"
            android:layout_width="match_parent" android:layout_height="match_parent"
            android:orientation="vertical" android:gravity="center"
            android:padding="@dimen/screen_padding" android:visibility="gone">
            <FrameLayout android:layout_width="80dp" android:layout_height="80dp">
                <View android:layout_width="match_parent" android:layout_height="match_parent"
                    android:background="@drawable/bg_circle_muted" />
                <ImageView android:layout_width="match_parent" android:layout_height="match_parent"
                    android:src="@drawable/ic_map_pin" android:padding="22dp"
                    app:tint="?attr/parkMutedForeground" android:importantForAccessibility="no" />
            </FrameLayout>
            <TextView android:layout_width="wrap_content" android:layout_height="wrap_content"
                android:text="@string/courses_empty_title" android:textColor="?attr/parkForeground"
                android:textSize="20sp" android:textStyle="bold" android:layout_marginTop="16dp" />
            <TextView android:layout_width="wrap_content" android:layout_height="wrap_content"
                android:text="@string/courses_empty_desc" android:textColor="?attr/parkMutedForeground"
                android:textSize="16sp" android:layout_marginTop="4dp" />
            <Button android:id="@+id/btnEmptyAdd"
                android:layout_width="wrap_content" android:layout_height="wrap_content"
                android:text="@string/create_template" app:icon="@drawable/ic_plus"
                app:iconGravity="textStart" android:layout_marginTop="20dp"
                android:minHeight="@dimen/touch_min" android:textStyle="bold" />
        </LinearLayout>
    </FrameLayout>
</LinearLayout>
```

- [ ] **Step 2: 빌드** — `./gradlew :app:assembleDebug` → BUILD SUCCESSFUL (참고: 기존 `MyCoursesFragment`가 `binding.rvVenues`/`binding.btnAddVenue`/`binding.tvEmpty`를 참조하므로 이 레이아웃 변경만으로는 컴파일 에러가 날 수 있음 → Task 6에서 프래그먼트 교체와 함께 빌드 성공. 이 태스크에서 assembleDebug가 프래그먼트 참조로 실패하면 Step 3만 커밋하고 Task 6에서 함께 검증.)
- [ ] **Step 3: 커밋**
```bash
git add app/src/main/res/layout/fragment_my_courses.xml
git commit -m "feat: course management layout (venue cards, empty state)"
```

---

## Task 6: MyCoursesFragment 재작성 + 어댑터 제거

**Files:**
- Modify: `app/src/main/java/com/parkgolf/score/ui/courses/MyCoursesFragment.kt`
- Delete: `app/src/main/java/com/parkgolf/score/ui/courses/VenueAdapter.kt`
- Delete: `app/src/main/res/layout/item_venue.xml`

- [ ] **Step 1: `MyCoursesFragment.kt` 전체 교체**
```kotlin
package com.parkgolf.score.ui.courses

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.parkgolf.score.App
import com.parkgolf.score.R
import com.parkgolf.score.data.ParkGolfRepository
import com.parkgolf.score.data.db.entity.CourseEntity
import com.parkgolf.score.data.db.entity.VenueEntity
import com.parkgolf.score.databinding.FragmentMyCoursesBinding
import com.parkgolf.score.databinding.ViewCourseRowBinding
import com.parkgolf.score.databinding.ViewCourseVenueCardBinding
import com.parkgolf.score.domain.CourseFormat
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

class MyCoursesFragment : Fragment(R.layout.fragment_my_courses) {
    /** 비어있으면 전체 펼침. 접힌 구장 id만 담는다(새 구장은 기본 펼침). */
    private val collapsed = mutableSetOf<Long>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val binding = FragmentMyCoursesBinding.bind(view)
        binding.topBar.tvBarTitle.text = getString(R.string.title_my_courses)
        binding.topBar.btnBack.setOnClickListener { findNavController().popBackStack() }
        val repo = App.repo(requireActivity().application)

        binding.btnNewTemplate.setOnClickListener { openWizard(0L, 0L) }
        binding.btnEmptyAdd.setOnClickListener { openWizard(0L, 0L) }

        viewLifecycleOwner.lifecycleScope.launch {
            combine(repo.observeVenues(), repo.observeCourses()) { venues, courses ->
                venues to courses
            }.collect { (venues, courses) ->
                build(binding, repo, venues, courses)
            }
        }
    }

    private fun build(
        binding: FragmentMyCoursesBinding,
        repo: ParkGolfRepository,
        venues: List<VenueEntity>,
        courses: List<CourseEntity>,
    ) {
        val empty = venues.isEmpty()
        binding.emptyView.isVisible = empty
        binding.scrollContent.isVisible = !empty
        binding.containerVenues.removeAllViews()
        if (empty) return

        val inflater = LayoutInflater.from(requireContext())
        val byVenue = courses.groupBy { it.venueId }
        venues.forEach { venue ->
            val vCourses = byVenue[venue.id].orEmpty()
            val card = ViewCourseVenueCardBinding.inflate(inflater, binding.containerVenues, false)
            card.tvVenueName.text = venue.name
            card.tvCourseCount.text = getString(R.string.course_count, vCourses.size)

            val expanded = venue.id !in collapsed
            card.btnToggle.setImageResource(
                if (expanded) R.drawable.ic_chevron_up else R.drawable.ic_chevron_down
            )
            val toggle = {
                if (!collapsed.add(venue.id)) collapsed.remove(venue.id)
                build(binding, repo, venues, courses)
            }
            card.headerClick.setOnClickListener { toggle() }
            card.btnToggle.setOnClickListener { toggle() }
            card.btnDeleteVenue.setOnClickListener { confirmDeleteVenue(repo, venue) }

            card.topDivider.isVisible = expanded && vCourses.isNotEmpty()
            card.coursesContainer.isVisible = expanded
            card.coursesContainer.removeAllViews()
            if (expanded) {
                vCourses.forEachIndexed { i, course ->
                    val row = ViewCourseRowBinding.inflate(inflater, card.coursesContainer, false)
                    row.tvCourseName.text = course.name
                    row.tvHolesPar.text = CourseFormat.holesPar(course.pars)
                    row.btnEditCourse.setOnClickListener { openWizard(venue.id, course.id) }
                    row.btnDeleteCourse.setOnClickListener { confirmDeleteCourse(repo, venue, course) }
                    row.divider.isVisible = i < vCourses.size - 1
                    card.coursesContainer.addView(row.root)
                }
            }
            binding.containerVenues.addView(card.root)
        }
    }

    private fun confirmDeleteVenue(repo: ParkGolfRepository, venue: VenueEntity) {
        MaterialAlertDialogBuilder(requireContext())
            .setMessage(R.string.delete_venue_confirm)
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton(R.string.delete) { _, _ ->
                viewLifecycleOwner.lifecycleScope.launch { repo.deleteVenueWithCourses(venue.id) }
            }.show()
    }

    private fun confirmDeleteCourse(repo: ParkGolfRepository, venue: VenueEntity, course: CourseEntity) {
        MaterialAlertDialogBuilder(requireContext())
            .setMessage(R.string.delete_course_confirm)
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton(R.string.delete) { _, _ ->
                viewLifecycleOwner.lifecycleScope.launch {
                    repo.deleteCourse(course)
                    if (repo.coursesForVenue(venue.id).isEmpty()) repo.deleteVenue(venue)
                }
            }.show()
    }

    private fun openWizard(venueId: Long, courseId: Long) {
        findNavController().currentBackStackEntry?.savedStateHandle?.apply {
            set("wizardVenueId", venueId)
            set("wizardCourseId", courseId)
        }
        findNavController().navigate(R.id.courseWizardFragment)
    }
}
```

- [ ] **Step 2: 어댑터/미사용 레이아웃 삭제**
```bash
git rm app/src/main/java/com/parkgolf/score/ui/courses/VenueAdapter.kt app/src/main/res/layout/item_venue.xml
```

- [ ] **Step 3: 빌드** — `./gradlew :app:assembleDebug` → BUILD SUCCESSFUL. 실패 시 이 두 파일과 레이아웃 id만 점검(다른 파일이 `VenueAdapter`/`ItemVenueBinding`/`item_venue`를 참조하면 BLOCKED로 보고).
- [ ] **Step 4: 단위 테스트** — `./gradlew :app:testDebugUnitTest` → BUILD SUCCESSFUL
- [ ] **Step 5: 커밋**
```bash
git add app/src/main/java/com/parkgolf/score/ui/courses/MyCoursesFragment.kt
git commit -m "feat: course management with expandable venue cards, cascade delete"
```

---

## Task 7: 에뮬레이터 스모크 테스트

**Files:** 없음(검증). 필요 시 앞 태스크 수정.

- [ ] **Step 1: 설치** — `./gradlew :app:installDebug` (emulator-5554)
- [ ] **Step 2: 목록** — 홈 → 코스 관리. 구장 카드(핀·구장명·"N개 코스"·삭제·화살표), 펼침 시 코스 행("N홀 · 파X"·연필·휴지통) 확인. 화살표/헤더 탭으로 펼침·접힘 토글.
- [ ] **Step 3: 편집 진입** — 코스 행 연필 → 마법사 편집 화면. 뒤로 복귀.
- [ ] **Step 4: 코스 삭제** — 휴지통 → 확인 다이얼로그 → 삭제 → 목록 갱신. 구장의 마지막 코스 삭제 시 구장 카드도 사라짐.
- [ ] **Step 5: 구장 삭제** — 여러 코스 있는 구장에서 구장 삭제 → 확인 → 구장+코스 모두 제거.
- [ ] **Step 6: 빈 상태** — 전부 삭제 → 핀 원 + "저장된 코스 없음" + "코스 템플릿 작성" 버튼. 버튼 → 마법사 신규.
- [ ] **Step 7: 하단 점선 버튼** — 구장이 있을 때 하단 "새 코스 템플릿 작성" → 마법사 신규.
- [ ] **Step 8: 회귀** — 게임 시작 화면의 구장/코스 목록 정상(코스 CRUD 반영).
- [ ] **Step 9: 문제 시 수정·재검증, 이상 없으면 완료.**

---

## Self-Review 메모
- 스펙 커버리지: 확장형 구장 카드 ✔(T4/T6), 코스 행·N홀·파 ✔(T2/T4/T6), 편집/삭제/추가 ✔(T6), 캐스케이드(구장→코스, 마지막코스→구장) ✔(T3/T6), 빈 상태 ✔(T5/T6), 점선 버튼 ✔(T5/T6), 확인 다이얼로그 ✔(T6).
- 타입 일관성: `deleteVenueWithCourses(venueId: Long)` T3 정의·T6 사용 일치. 뷰 id(headerClick/btnToggle/btnDeleteVenue/tvVenueName/tvCourseCount/topDivider/coursesContainer, tvCourseName/tvHolesPar/btnEditCourse/btnDeleteCourse/divider, scrollContent/containerVenues/btnNewTemplate/emptyView/btnEmptyAdd) T4/T5 정의·T6 사용 일치.
- 빌드 독립성: 레이아웃(T4/T5)·리소스(T1)·헬퍼(T2)·저장소(T3)를 프래그먼트(T6)보다 먼저. T5 레이아웃 교체는 기존 프래그먼트의 옛 id 참조로 단독 빌드 실패 가능 → T6와 함께 성공(플랜에 명시).
- 미사용 정리: `VenueAdapter`/`item_venue.xml` 삭제. `no_courses_yet`/`pick_course_to_edit`/`new_course`는 다른 곳 참조 여부 불명 → 제거하지 않고 존치(안전).
