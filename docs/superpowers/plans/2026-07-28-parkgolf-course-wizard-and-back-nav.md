# Course Registration Wizard & Back-Navigation System Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the single-screen course editor with a 4-step wizard (구장명 → 코스명 → 홀 설정 → 저장) and add an app-wide top bar with back-confirmation for the wizard and game-recording screens.

**Architecture:** A single `CourseWizardFragment` hosts a fixed top bar, a 4-step indicator, a `ViewFlipper` that swaps step content, and prev/next buttons. All wizard state lives in one `CourseDraft` held by `CourseWizardViewModel` (LiveData). Pure decision logic (venue merge-by-name, next course-letter suggestion, par resize, step validity) lives in the testable `CourseWizardLogic` object. Fragments perform Room I/O directly, matching the existing codebase pattern. Back handling routes the top-bar button and the system/gesture back through one path, showing a confirm dialog at the designated points.

**Tech Stack:** Kotlin, View system (XML) + ConstraintLayout + ViewBinding, Jetpack Navigation, Room, JUnit4 + Truth (unit), Espresso (instrumented). minSdk 21.

**Design spec:** `docs/superpowers/specs/2026-07-28-parkgolf-course-wizard-and-back-nav-design.md`

---

## File Structure

**New files:**
- `app/src/main/java/com/parkgolf/score/ui/courses/CourseWizardLogic.kt` — `CourseDraft` data class + pure `CourseWizardLogic` object.
- `app/src/main/java/com/parkgolf/score/ui/courses/CourseWizardViewModel.kt` — draft state holder.
- `app/src/main/java/com/parkgolf/score/ui/courses/CourseWizardFragment.kt` — wizard screen.
- `app/src/main/java/com/parkgolf/score/ui/courses/WizardParAdapter.kt` — par editor list driven by a callback.
- `app/src/main/java/com/parkgolf/score/ui/common/NavExt.kt` — `onBackPressed`, `confirmYesNo`, `setupTopBar` helpers.
- `app/src/main/res/layout/view_top_bar.xml` — reusable top bar include.
- `app/src/main/res/layout/view_wizard_steps.xml` — 4-item step indicator include.
- `app/src/main/res/layout/fragment_course_wizard.xml` — wizard layout with ViewFlipper.
- `app/src/main/res/drawable/ic_arrow_back.xml` — back arrow vector.
- `app/src/test/java/com/parkgolf/score/ui/CourseWizardLogicTest.kt` — pure logic tests.
- `app/src/test/java/com/parkgolf/score/ui/CourseWizardViewModelTest.kt` — VM tests.
- `app/src/androidTest/java/com/parkgolf/score/ui/CourseWizardFlowTest.kt` — instrumented flow tests.

**Modified files:**
- `app/src/main/res/values/strings.xml` — wizard + dialog strings.
- `app/src/main/res/values/colors.xml` — disabled-button color.
- `app/src/main/res/navigation/nav_graph.xml` — swap `courseEditFragment` → `courseWizardFragment`.
- `app/src/main/java/com/parkgolf/score/ui/courses/MyCoursesFragment.kt` — route to wizard (course-level edit).
- `app/src/main/java/com/parkgolf/score/ui/start/StartFragment.kt` — route "new course" to wizard.
- `app/src/main/java/com/parkgolf/score/ui/hole/HoleInputFragment.kt` — top bar + exit confirm.
- `app/src/main/res/layout/fragment_hole_input.xml` — add top bar include.

**Deleted files:**
- `app/src/main/java/com/parkgolf/score/ui/courses/CourseEditFragment.kt`
- `app/src/main/res/layout/fragment_course_edit.xml`
- (`ParEditorAdapter.kt` + `item_par_editor.xml` are kept — the item layout is reused by `WizardParAdapter`; the old adapter is removed only if unused after Task 8. See Task 8.)

---

## Task 1: Resource strings & colors

**Files:**
- Modify: `app/src/main/res/values/strings.xml`
- Modify: `app/src/main/res/values/colors.xml`

- [ ] **Step 1: Add strings**

Add these entries inside `<resources>` in `app/src/main/res/values/strings.xml` (before the closing `</resources>`):

```xml
    <!-- Course wizard -->
    <string name="wizard_title_create">코스 작성</string>
    <string name="wizard_title_edit">코스 편집</string>
    <string name="step_venue">구장명</string>
    <string name="step_course">코스명</string>
    <string name="step_holes">홀 설정</string>
    <string name="step_save">저장</string>
    <string name="venue_name_label">구장 이름</string>
    <string name="venue_name_helper">동일한 이름의 구장은 게임 시작 화면에서 통합 표시됩니다</string>
    <string name="venue_name_example">예: 한강 파크골프장</string>
    <string name="course_name_label">코스 이름</string>
    <string name="hole_setup_label">홀 설정</string>
    <string name="hole_count_label">홀 수</string>
    <string name="wizard_prev">◀ 이전</string>
    <string name="wizard_next">다음 ▶</string>
    <string name="wizard_save">저장</string>
    <string name="save_summary_venue">구장: %1$s</string>
    <string name="save_summary_course">코스: %1$s (%2$d홀)</string>
    <string name="course_saved">저장되었습니다</string>
    <string name="pick_course_to_edit">수정할 코스를 선택하세요</string>
    <!-- Top bar & back confirmation -->
    <string name="back">뒤로</string>
    <string name="game_recording">게임 기록</string>
    <string name="yes">네</string>
    <string name="no">아니오</string>
    <string name="confirm_cancel_create">코스 작성을 취소하시겠습니까?</string>
    <string name="confirm_cancel_edit">코스 편집을 취소하시겠습니까?</string>
    <string name="confirm_exit_game">현재 게임을 저장하지 않고 나가시겠습니까?</string>
```

- [ ] **Step 2: Add disabled-button color**

Add inside `<resources>` in `app/src/main/res/values/colors.xml`:

```xml
    <color name="green_disabled">#A8D5B5</color>
```

- [ ] **Step 3: Build to verify resources compile**

Run: `ANDROID_HOME=/Users/inhyo/Library/Android/sdk ./gradlew :app:processDebugResources`
Expected: BUILD SUCCESSFUL

- [ ] **Step 4: Commit**

```bash
git add app/src/main/res/values/strings.xml app/src/main/res/values/colors.xml
git commit -m "feat: add course-wizard and back-nav string/color resources"
```

---

## Task 2: CourseDraft + CourseWizardLogic (pure)

**Files:**
- Create: `app/src/main/java/com/parkgolf/score/ui/courses/CourseWizardLogic.kt`
- Test: `app/src/test/java/com/parkgolf/score/ui/CourseWizardLogicTest.kt`

- [ ] **Step 1: Write the failing test**

Create `app/src/test/java/com/parkgolf/score/ui/CourseWizardLogicTest.kt`:

```kotlin
package com.parkgolf.score.ui

import com.google.common.truth.Truth.assertThat
import com.parkgolf.score.data.db.entity.VenueEntity
import com.parkgolf.score.ui.courses.CourseDraft
import com.parkgolf.score.ui.courses.CourseWizardLogic
import org.junit.Test

class CourseWizardLogicTest {

    @Test fun resizePars_growsWithParThreeDefault() {
        val result = CourseWizardLogic.resizePars(listOf(3, 4), 4)
        assertThat(result).containsExactly(3, 4, 3, 3).inOrder()
    }

    @Test fun resizePars_shrinksKeepingLeadingValues() {
        val result = CourseWizardLogic.resizePars(listOf(3, 4, 5, 3), 2)
        assertThat(result).containsExactly(3, 4).inOrder()
    }

    @Test fun resizePars_sameSizeUnchanged() {
        val input = listOf(3, 4, 3)
        assertThat(CourseWizardLogic.resizePars(input, 3)).isEqualTo(input)
    }

    @Test fun suggestCourseName_emptyGivesA() {
        assertThat(CourseWizardLogic.suggestCourseName(emptyList())).isEqualTo("A코스")
    }

    @Test fun suggestCourseName_skipsUsedLetters() {
        assertThat(CourseWizardLogic.suggestCourseName(listOf("A코스", "B코스"))).isEqualTo("C코스")
    }

    @Test fun resolveVenueId_matchesByNameIgnoringCaseAndSpace() {
        val venues = listOf(VenueEntity(id = 7, name = "한강 파크골프장"))
        val id = CourseWizardLogic.resolveVenueId("  한강 파크골프장 ", venues, editingVenueId = null)
        assertThat(id).isEqualTo(7)
    }

    @Test fun resolveVenueId_noMatchReturnsNull() {
        val venues = listOf(VenueEntity(id = 7, name = "한강"))
        assertThat(CourseWizardLogic.resolveVenueId("낙동강", venues, null)).isNull()
    }

    @Test fun resolveVenueId_editingWins() {
        val venues = listOf(VenueEntity(id = 7, name = "한강"))
        assertThat(CourseWizardLogic.resolveVenueId("한강", venues, editingVenueId = 99)).isEqualTo(99)
    }

    @Test fun isStepValid_venueStepRequiresName() {
        val blank = CourseDraft(venueName = "  ")
        val named = CourseDraft(venueName = "한강")
        assertThat(CourseWizardLogic.isStepValid(blank, 0)).isFalse()
        assertThat(CourseWizardLogic.isStepValid(named, 0)).isTrue()
    }

    @Test fun isStepValid_holeStepRequiresMatchingPars() {
        val mismatch = CourseDraft(holeCount = 9, pars = listOf(3, 3))
        val ok = CourseDraft(holeCount = 2, pars = listOf(3, 4))
        assertThat(CourseWizardLogic.isStepValid(mismatch, 2)).isFalse()
        assertThat(CourseWizardLogic.isStepValid(ok, 2)).isTrue()
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `ANDROID_HOME=/Users/inhyo/Library/Android/sdk ./gradlew :app:testDebugUnitTest --tests "com.parkgolf.score.ui.CourseWizardLogicTest"`
Expected: FAIL / compile error — `CourseDraft` and `CourseWizardLogic` do not exist.

- [ ] **Step 3: Write minimal implementation**

Create `app/src/main/java/com/parkgolf/score/ui/courses/CourseWizardLogic.kt`:

```kotlin
package com.parkgolf.score.ui.courses

import com.parkgolf.score.data.db.entity.VenueEntity

/** All wizard state in one immutable value. step is 0..3. */
data class CourseDraft(
    val editingVenueId: Long? = null,
    val editingCourseId: Long? = null,
    val venueName: String = "",
    val courseName: String = "A코스",
    val holeCount: Int = 9,
    val pars: List<Int> = List(9) { 3 },
    val step: Int = 0,
)

/** Pure decision logic for the course wizard; no Android dependencies. */
object CourseWizardLogic {
    const val MIN_PAR = 1
    const val MAX_STEP = 3
    const val MAX_HOLES = 27

    /** Resize [current] to [holeCount], padding new holes with par 3. */
    fun resizePars(current: List<Int>, holeCount: Int): List<Int> {
        val n = holeCount.coerceAtLeast(1)
        return when {
            current.size == n -> current
            current.size < n -> current + List(n - current.size) { 3 }
            else -> current.subList(0, n).toList()
        }
    }

    /** Next unused course letter as "X코스", based on the first char of existing names. */
    fun suggestCourseName(existingCourseNames: List<String>): String {
        val used = existingCourseNames.mapNotNull { it.trim().firstOrNull() }.toSet()
        val next = ('A'..'Z').firstOrNull { it !in used } ?: 'A'
        return "${next}코스"
    }

    /** Existing venue id matching [name] (trim, case-insensitive), or [editingVenueId], else null (= create new). */
    fun resolveVenueId(name: String, existing: List<VenueEntity>, editingVenueId: Long?): Long? {
        if (editingVenueId != null) return editingVenueId
        val key = name.trim()
        return existing.firstOrNull { it.name.trim().equals(key, ignoreCase = true) }?.id
    }

    fun isStepValid(draft: CourseDraft, step: Int): Boolean = when (step) {
        0 -> draft.venueName.isNotBlank()
        1 -> draft.courseName.isNotBlank()
        2 -> draft.holeCount in 1..MAX_HOLES &&
            draft.pars.size == draft.holeCount &&
            draft.pars.all { it >= MIN_PAR }
        else -> true
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `ANDROID_HOME=/Users/inhyo/Library/Android/sdk ./gradlew :app:testDebugUnitTest --tests "com.parkgolf.score.ui.CourseWizardLogicTest"`
Expected: PASS (all tests green)

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/parkgolf/score/ui/courses/CourseWizardLogic.kt app/src/test/java/com/parkgolf/score/ui/CourseWizardLogicTest.kt
git commit -m "feat: add CourseDraft and pure CourseWizardLogic with tests"
```

---

## Task 3: CourseWizardViewModel

**Files:**
- Create: `app/src/main/java/com/parkgolf/score/ui/courses/CourseWizardViewModel.kt`
- Test: `app/src/test/java/com/parkgolf/score/ui/CourseWizardViewModelTest.kt`

- [ ] **Step 1: Write the failing test**

Create `app/src/test/java/com/parkgolf/score/ui/CourseWizardViewModelTest.kt`:

```kotlin
package com.parkgolf.score.ui

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.google.common.truth.Truth.assertThat
import com.parkgolf.score.ui.courses.CourseWizardViewModel
import org.junit.Rule
import org.junit.Test

class CourseWizardViewModelTest {
    @get:Rule val instant = InstantTaskExecutorRule()

    private fun vm() = CourseWizardViewModel().apply { initNew("A코스") }

    @Test fun initNew_setsSuggestedCourseNameAndStepZero() {
        val vm = vm()
        assertThat(vm.draft.value!!.courseName).isEqualTo("A코스")
        assertThat(vm.draft.value!!.step).isEqualTo(0)
    }

    @Test fun setHoleCount_resizesPars() {
        val vm = vm()
        vm.setHoleCount(3)
        assertThat(vm.draft.value!!.pars).hasSize(3)
        vm.setHoleCount(12)
        assertThat(vm.draft.value!!.pars).hasSize(12)
    }

    @Test fun setPar_clampsToMinimumOne() {
        val vm = vm()
        vm.setPar(0, -5)
        assertThat(vm.draft.value!!.pars[0]).isEqualTo(1)
    }

    @Test fun next_prev_clampWithinRange() {
        val vm = vm()
        vm.prev()
        assertThat(vm.draft.value!!.step).isEqualTo(0)
        repeat(10) { vm.next() }
        assertThat(vm.draft.value!!.step).isEqualTo(3)
    }

    @Test fun canAdvance_reflectsCurrentStepValidity() {
        val vm = vm()
        vm.setVenueName("")
        assertThat(vm.canAdvance()).isFalse()
        vm.setVenueName("한강")
        assertThat(vm.canAdvance()).isTrue()
    }

    @Test fun loadForEdit_populatesDraft() {
        val vm = CourseWizardViewModel()
        vm.loadForEdit(venueId = 5, courseId = 8, venueName = "한강", courseName = "B코스", pars = listOf(3, 4, 5))
        val d = vm.draft.value!!
        assertThat(d.editingVenueId).isEqualTo(5)
        assertThat(d.editingCourseId).isEqualTo(8)
        assertThat(d.holeCount).isEqualTo(3)
        assertThat(d.pars).containsExactly(3, 4, 5).inOrder()
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `ANDROID_HOME=/Users/inhyo/Library/Android/sdk ./gradlew :app:testDebugUnitTest --tests "com.parkgolf.score.ui.CourseWizardViewModelTest"`
Expected: FAIL — `CourseWizardViewModel` does not exist.

- [ ] **Step 3: Write minimal implementation**

Create `app/src/main/java/com/parkgolf/score/ui/courses/CourseWizardViewModel.kt`:

```kotlin
package com.parkgolf.score.ui.courses

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class CourseWizardViewModel : ViewModel() {
    private val _draft = MutableLiveData(CourseDraft())
    val draft: LiveData<CourseDraft> get() = _draft

    private fun cur() = _draft.value!!
    private fun update(block: (CourseDraft) -> CourseDraft) { _draft.value = block(cur()) }

    fun initNew(suggestedCourseName: String) {
        _draft.value = CourseDraft(courseName = suggestedCourseName)
    }

    fun loadForEdit(venueId: Long, courseId: Long, venueName: String, courseName: String, pars: List<Int>) {
        _draft.value = CourseDraft(
            editingVenueId = venueId,
            editingCourseId = courseId,
            venueName = venueName,
            courseName = courseName,
            holeCount = pars.size,
            pars = pars,
        )
    }

    fun setVenueName(v: String) = update { it.copy(venueName = v) }
    fun setCourseName(v: String) = update { it.copy(courseName = v) }

    fun setHoleCount(n: Int) = update {
        val clamped = n.coerceIn(1, CourseWizardLogic.MAX_HOLES)
        it.copy(holeCount = clamped, pars = CourseWizardLogic.resizePars(it.pars, clamped))
    }

    fun setPar(index: Int, value: Int) = update {
        val pars = it.pars.toMutableList()
        if (index in pars.indices) pars[index] = value.coerceAtLeast(CourseWizardLogic.MIN_PAR)
        it.copy(pars = pars)
    }

    fun next() = update { it.copy(step = (it.step + 1).coerceAtMost(CourseWizardLogic.MAX_STEP)) }
    fun prev() = update { it.copy(step = (it.step - 1).coerceAtLeast(0)) }
    fun goToStep(step: Int) = update { it.copy(step = step.coerceIn(0, CourseWizardLogic.MAX_STEP)) }

    fun canAdvance(): Boolean = CourseWizardLogic.isStepValid(cur(), cur().step)
    fun isEditing(): Boolean = cur().editingCourseId != null
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `ANDROID_HOME=/Users/inhyo/Library/Android/sdk ./gradlew :app:testDebugUnitTest --tests "com.parkgolf.score.ui.CourseWizardViewModelTest"`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/parkgolf/score/ui/courses/CourseWizardViewModel.kt app/src/test/java/com/parkgolf/score/ui/CourseWizardViewModelTest.kt
git commit -m "feat: add CourseWizardViewModel with tests"
```

---

## Task 4: Reusable top bar + back-nav helpers

**Files:**
- Create: `app/src/main/res/drawable/ic_arrow_back.xml`
- Create: `app/src/main/res/layout/view_top_bar.xml`
- Create: `app/src/main/java/com/parkgolf/score/ui/common/NavExt.kt`

- [ ] **Step 1: Create the back-arrow vector drawable**

Create `app/src/main/res/drawable/ic_arrow_back.xml`:

```xml
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp"
    android:height="24dp"
    android:viewportWidth="24"
    android:viewportHeight="24"
    android:tint="@color/text_primary">
    <path
        android:fillColor="@android:color/white"
        android:pathData="M20,11H7.83l5.59,-5.59L12,4l-8,8 8,8 1.41,-1.41L7.83,13H20v-2z" />
</vector>
```

- [ ] **Step 2: Create the reusable top bar layout**

Create `app/src/main/res/layout/view_top_bar.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<androidx.constraintlayout.widget.ConstraintLayout
    xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:background="@color/surface"
    android:minHeight="@dimen/touch_min">

    <ImageButton
        android:id="@+id/btnBack"
        android:layout_width="@dimen/touch_min"
        android:layout_height="@dimen/touch_min"
        android:background="?attr/selectableItemBackgroundBorderless"
        android:src="@drawable/ic_arrow_back"
        android:scaleType="center"
        android:contentDescription="@string/back"
        app:layout_constraintStart_toStartOf="parent"
        app:layout_constraintTop_toTopOf="parent"
        app:layout_constraintBottom_toBottomOf="parent" />

    <TextView
        android:id="@+id/tvBarTitle"
        android:layout_width="0dp"
        android:layout_height="wrap_content"
        android:layout_marginStart="8dp"
        android:layout_marginEnd="16dp"
        android:textSize="@dimen/title_text"
        android:textStyle="bold"
        android:textColor="@color/text_primary"
        android:maxLines="1"
        android:ellipsize="end"
        app:layout_constraintStart_toEndOf="@id/btnBack"
        app:layout_constraintEnd_toEndOf="parent"
        app:layout_constraintTop_toTopOf="parent"
        app:layout_constraintBottom_toBottomOf="parent" />
</androidx.constraintlayout.widget.ConstraintLayout>
```

- [ ] **Step 3: Create the nav helper extensions**

Create `app/src/main/java/com/parkgolf/score/ui/common/NavExt.kt`:

```kotlin
package com.parkgolf.score.ui.common

import androidx.activity.OnBackPressedCallback
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.parkgolf.score.R

/** Register a system/gesture back handler bound to this fragment's view lifecycle. */
fun Fragment.onBackPressed(handler: () -> Unit) {
    val callback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() = handler()
    }
    requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, callback)
}

/** Show a 네/아니오 confirmation dialog; run [onYes] only if the user taps 네. */
fun Fragment.confirmYesNo(@StringRes titleRes: Int, onYes: () -> Unit) {
    AlertDialog.Builder(requireContext())
        .setTitle(titleRes)
        .setPositiveButton(R.string.yes) { _, _ -> onYes() }
        .setNegativeButton(R.string.no, null)
        .show()
}
```

- [ ] **Step 4: Build to verify everything compiles**

Run: `ANDROID_HOME=/Users/inhyo/Library/Android/sdk ./gradlew :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL

- [ ] **Step 5: Commit**

```bash
git add app/src/main/res/drawable/ic_arrow_back.xml app/src/main/res/layout/view_top_bar.xml app/src/main/java/com/parkgolf/score/ui/common/NavExt.kt
git commit -m "feat: add reusable top bar and back-nav helpers"
```

---

## Task 5: Wizard par adapter + layouts

**Files:**
- Create: `app/src/main/java/com/parkgolf/score/ui/courses/WizardParAdapter.kt`
- Create: `app/src/main/res/layout/view_wizard_steps.xml`
- Create: `app/src/main/res/layout/fragment_course_wizard.xml`

- [ ] **Step 1: Create the par adapter driven by a callback**

Create `app/src/main/java/com/parkgolf/score/ui/courses/WizardParAdapter.kt`. It reuses the existing `item_par_editor.xml` layout (`ItemParEditorBinding`) but renders from a supplied list and reports changes via a callback so the ViewModel stays the single source of truth:

```kotlin
package com.parkgolf.score.ui.courses

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.parkgolf.score.R
import com.parkgolf.score.databinding.ItemParEditorBinding

/** Read-only view of pars; every +/- tap calls [onParChange] with the new value. */
class WizardParAdapter(private val onParChange: (index: Int, newValue: Int) -> Unit) :
    RecyclerView.Adapter<WizardParAdapter.VH>() {

    private var pars: List<Int> = emptyList()

    @SuppressLint("NotifyDataSetChanged")
    fun submit(list: List<Int>) { pars = list; notifyDataSetChanged() }

    inner class VH(val b: ItemParEditorBinding) : RecyclerView.ViewHolder(b.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VH(ItemParEditorBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun getItemCount() = pars.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val ctx = holder.itemView.context
        holder.b.tvHoleLabel.text = ctx.getString(R.string.hole_number, position + 1)
        holder.b.tvPar.text = pars[position].toString()
        holder.b.btnParMinus.setOnClickListener { onParChange(position, pars[position] - 1) }
        holder.b.btnParPlus.setOnClickListener { onParChange(position, pars[position] + 1) }
    }
}
```

- [ ] **Step 2: Create the step indicator include**

Create `app/src/main/res/layout/view_wizard_steps.xml`. Four equal-weight clickable items; the fragment styles the current step:

```xml
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:orientation="horizontal"
    android:paddingVertical="8dp">

    <LinearLayout android:id="@+id/step0"
        android:layout_width="0dp" android:layout_height="wrap_content"
        android:layout_weight="1" android:orientation="vertical" android:gravity="center"
        android:minHeight="@dimen/touch_min" android:background="?attr/selectableItemBackground">
        <TextView android:layout_width="wrap_content" android:layout_height="wrap_content"
            android:text="📍" android:textSize="22sp" />
        <TextView android:id="@+id/tvStep0Label" android:layout_width="wrap_content"
            android:layout_height="wrap_content" android:text="@string/step_venue" />
    </LinearLayout>

    <LinearLayout android:id="@+id/step1"
        android:layout_width="0dp" android:layout_height="wrap_content"
        android:layout_weight="1" android:orientation="vertical" android:gravity="center"
        android:minHeight="@dimen/touch_min" android:background="?attr/selectableItemBackground">
        <TextView android:layout_width="wrap_content" android:layout_height="wrap_content"
            android:text="🏳️" android:textSize="22sp" />
        <TextView android:id="@+id/tvStep1Label" android:layout_width="wrap_content"
            android:layout_height="wrap_content" android:text="@string/step_course" />
    </LinearLayout>

    <LinearLayout android:id="@+id/step2"
        android:layout_width="0dp" android:layout_height="wrap_content"
        android:layout_weight="1" android:orientation="vertical" android:gravity="center"
        android:minHeight="@dimen/touch_min" android:background="?attr/selectableItemBackground">
        <TextView android:layout_width="wrap_content" android:layout_height="wrap_content"
            android:text="📋" android:textSize="22sp" />
        <TextView android:id="@+id/tvStep2Label" android:layout_width="wrap_content"
            android:layout_height="wrap_content" android:text="@string/step_holes" />
    </LinearLayout>

    <LinearLayout android:id="@+id/step3"
        android:layout_width="0dp" android:layout_height="wrap_content"
        android:layout_weight="1" android:orientation="vertical" android:gravity="center"
        android:minHeight="@dimen/touch_min" android:background="?attr/selectableItemBackground">
        <TextView android:layout_width="wrap_content" android:layout_height="wrap_content"
            android:text="💾" android:textSize="22sp" />
        <TextView android:id="@+id/tvStep3Label" android:layout_width="wrap_content"
            android:layout_height="wrap_content" android:text="@string/step_save" />
    </LinearLayout>
</LinearLayout>
```

- [ ] **Step 3: Create the wizard fragment layout**

Create `app/src/main/res/layout/fragment_course_wizard.xml`. Top bar + steps + `ViewFlipper` (4 step bodies) + bottom prev/next:

```xml
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:orientation="vertical">

    <include android:id="@+id/topBar" layout="@layout/view_top_bar" />

    <include android:id="@+id/steps" layout="@layout/view_wizard_steps" />

    <View android:layout_width="match_parent" android:layout_height="1dp"
        android:background="@color/surface_muted" />

    <ViewFlipper android:id="@+id/flipper"
        android:layout_width="match_parent"
        android:layout_height="0dp"
        android:layout_weight="1">

        <!-- Step 0: venue name -->
        <ScrollView android:layout_width="match_parent" android:layout_height="match_parent"
            android:fillViewport="true" android:padding="@dimen/screen_padding">
            <LinearLayout android:layout_width="match_parent" android:layout_height="wrap_content"
                android:orientation="vertical">
                <TextView android:layout_width="wrap_content" android:layout_height="wrap_content"
                    android:text="@string/venue_name_label" android:textSize="@dimen/title_text"
                    android:textStyle="bold" />
                <TextView android:layout_width="wrap_content" android:layout_height="wrap_content"
                    android:text="@string/venue_name_helper" android:textColor="@color/text_muted"
                    android:layout_marginTop="4dp" android:layout_marginBottom="16dp" />
                <EditText android:id="@+id/etVenueName"
                    android:layout_width="match_parent" android:layout_height="wrap_content"
                    android:minHeight="@dimen/touch_min" android:hint="@string/venue_name_example"
                    android:textSize="@dimen/body_text" android:inputType="text" />
            </LinearLayout>
        </ScrollView>

        <!-- Step 1: course name -->
        <ScrollView android:layout_width="match_parent" android:layout_height="match_parent"
            android:fillViewport="true" android:padding="@dimen/screen_padding">
            <LinearLayout android:layout_width="match_parent" android:layout_height="wrap_content"
                android:orientation="vertical">
                <TextView android:layout_width="wrap_content" android:layout_height="wrap_content"
                    android:text="@string/course_name_label" android:textSize="@dimen/title_text"
                    android:textStyle="bold" android:layout_marginBottom="16dp" />
                <EditText android:id="@+id/etCourseName"
                    android:layout_width="match_parent" android:layout_height="wrap_content"
                    android:minHeight="@dimen/touch_min" android:hint="@string/course_name_hint"
                    android:textSize="@dimen/body_text" android:inputType="text" />
            </LinearLayout>
        </ScrollView>

        <!-- Step 2: hole setup -->
        <LinearLayout android:layout_width="match_parent" android:layout_height="match_parent"
            android:orientation="vertical" android:padding="@dimen/screen_padding">
            <LinearLayout android:layout_width="match_parent" android:layout_height="wrap_content"
                android:orientation="horizontal" android:gravity="center_vertical"
                android:minHeight="@dimen/touch_min">
                <TextView android:layout_width="0dp" android:layout_height="wrap_content"
                    android:layout_weight="1" android:text="@string/hole_count_label"
                    android:textSize="@dimen/body_text" android:textStyle="bold" />
                <Button android:id="@+id/btnHoleMinus"
                    android:layout_width="@dimen/stepper_size" android:layout_height="@dimen/stepper_size"
                    android:text="−" android:textSize="24sp" android:contentDescription="@string/decrease" />
                <TextView android:id="@+id/tvHoleCount"
                    android:layout_width="wrap_content" android:layout_height="wrap_content"
                    android:minWidth="48dp" android:gravity="center"
                    android:textSize="@dimen/score_text" android:textStyle="bold" />
                <Button android:id="@+id/btnHolePlus"
                    android:layout_width="@dimen/stepper_size" android:layout_height="@dimen/stepper_size"
                    android:text="＋" android:textSize="24sp" android:contentDescription="@string/increase" />
            </LinearLayout>
            <androidx.recyclerview.widget.RecyclerView android:id="@+id/rvPars"
                android:layout_width="match_parent" android:layout_height="0dp"
                android:layout_weight="1" android:layout_marginTop="8dp" />
        </LinearLayout>

        <!-- Step 3: save summary -->
        <ScrollView android:layout_width="match_parent" android:layout_height="match_parent"
            android:fillViewport="true" android:padding="@dimen/screen_padding">
            <LinearLayout android:layout_width="match_parent" android:layout_height="wrap_content"
                android:orientation="vertical">
                <TextView android:id="@+id/tvSummaryVenue"
                    android:layout_width="wrap_content" android:layout_height="wrap_content"
                    android:textSize="@dimen/body_text" android:layout_marginBottom="8dp" />
                <TextView android:id="@+id/tvSummaryCourse"
                    android:layout_width="wrap_content" android:layout_height="wrap_content"
                    android:textSize="@dimen/body_text" />
            </LinearLayout>
        </ScrollView>
    </ViewFlipper>

    <LinearLayout android:layout_width="match_parent" android:layout_height="wrap_content"
        android:orientation="horizontal" android:padding="@dimen/screen_padding">
        <Button android:id="@+id/btnPrev"
            android:layout_width="0dp" android:layout_height="wrap_content"
            android:layout_weight="1" android:minHeight="@dimen/touch_min"
            android:text="@string/wizard_prev"
            style="?attr/materialButtonOutlinedStyle" />
        <Button android:id="@+id/btnNext"
            android:layout_width="0dp" android:layout_height="wrap_content"
            android:layout_weight="2" android:minHeight="@dimen/touch_min"
            android:layout_marginStart="8dp"
            android:text="@string/wizard_next" android:textSize="@dimen/button_text" />
    </LinearLayout>
</LinearLayout>
```

- [ ] **Step 4: Build to verify layouts and adapter compile**

Run: `ANDROID_HOME=/Users/inhyo/Library/Android/sdk ./gradlew :app:assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/parkgolf/score/ui/courses/WizardParAdapter.kt app/src/main/res/layout/view_wizard_steps.xml app/src/main/res/layout/fragment_course_wizard.xml
git commit -m "feat: add wizard par adapter and step layouts"
```

---

## Task 6: CourseWizardFragment + navigation wiring

**Files:**
- Create: `app/src/main/java/com/parkgolf/score/ui/courses/CourseWizardFragment.kt`
- Modify: `app/src/main/res/navigation/nav_graph.xml`
- Modify: `app/src/main/java/com/parkgolf/score/ui/courses/MyCoursesFragment.kt`
- Modify: `app/src/main/java/com/parkgolf/score/ui/start/StartFragment.kt`
- Delete: `app/src/main/java/com/parkgolf/score/ui/courses/CourseEditFragment.kt`, `app/src/main/res/layout/fragment_course_edit.xml`

- [ ] **Step 1: Swap the nav destination**

In `app/src/main/res/navigation/nav_graph.xml`, replace the `courseEditFragment` entry:

```xml
    <fragment android:id="@+id/courseEditFragment"
        android:name="com.parkgolf.score.ui.courses.CourseEditFragment" />
```

with:

```xml
    <fragment android:id="@+id/courseWizardFragment"
        android:name="com.parkgolf.score.ui.courses.CourseWizardFragment" />
```

- [ ] **Step 2: Create the wizard fragment**

Create `app/src/main/java/com/parkgolf/score/ui/courses/CourseWizardFragment.kt`. It reads `wizardVenueId`/`wizardCourseId` from the previous back-stack entry (0 = create), loads existing data for edit, renders each step from the draft, and wires back-confirm through a single `attemptExit()`:

```kotlin
package com.parkgolf.score.ui.courses

import android.os.Bundle
import android.view.View
import androidx.core.content.ContextCompat
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import com.parkgolf.score.App
import com.parkgolf.score.R
import com.parkgolf.score.data.ParkGolfRepository
import com.parkgolf.score.data.db.entity.CourseEntity
import com.parkgolf.score.data.db.entity.VenueEntity
import com.parkgolf.score.databinding.FragmentCourseWizardBinding
import com.parkgolf.score.ui.common.confirmYesNo
import com.parkgolf.score.ui.common.onBackPressed
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class CourseWizardFragment : Fragment(R.layout.fragment_course_wizard) {

    private val vm: CourseWizardViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val binding = FragmentCourseWizardBinding.bind(view)
        val repo = App.repo(requireActivity().application)
        val parAdapter = WizardParAdapter { index, newValue -> vm.setPar(index, newValue) }
        binding.rvPars.layoutManager = LinearLayoutManager(requireContext())
        binding.rvPars.adapter = parAdapter

        // Initial state: create vs edit (only when first created).
        if (savedInstanceState == null) {
            val handle = findNavController().previousBackStackEntry?.savedStateHandle
            val venueId = handle?.get<Long>("wizardVenueId") ?: 0L
            val courseId = handle?.get<Long>("wizardCourseId") ?: 0L
            if (venueId == 0L || courseId == 0L) {
                vm.initNew("A코스")
            } else {
                viewLifecycleOwner.lifecycleScope.launch {
                    val venue = repo.observeVenues().first().firstOrNull { it.id == venueId }
                    val course = repo.coursesForVenue(venueId).firstOrNull { it.id == courseId }
                    if (venue != null && course != null) {
                        vm.loadForEdit(venue.id, course.id, venue.name, course.name, course.pars)
                    } else vm.initNew("A코스")
                }
            }
        }

        // Text inputs -> draft. guard prevents feedback loops when we set text from state.
        var binding_updating = false
        binding.etVenueName.doAfterTextChanged { if (!binding_updating) vm.setVenueName(it?.toString() ?: "") }
        binding.etCourseName.doAfterTextChanged { if (!binding_updating) vm.setCourseName(it?.toString() ?: "") }

        binding.btnHoleMinus.setOnClickListener { vm.setHoleCount((vm.draft.value?.holeCount ?: 9) - 1) }
        binding.btnHolePlus.setOnClickListener { vm.setHoleCount((vm.draft.value?.holeCount ?: 9) + 1) }

        binding.btnPrev.setOnClickListener {
            if (vm.draft.value?.step == 0) attemptExit(repo) else vm.prev()
        }
        binding.btnNext.setOnClickListener {
            val d = vm.draft.value ?: return@setOnClickListener
            if (!vm.canAdvance()) return@setOnClickListener
            if (d.step == CourseWizardLogic.MAX_STEP) save(repo, binding) else {
                if (d.step == 0 && !vm.isEditing()) maybeSuggestCourseName(repo)
                vm.next()
            }
        }

        // Step indicator taps jump to a step.
        binding.steps.step0.setOnClickListener { vm.goToStep(0) }
        binding.steps.step1.setOnClickListener { vm.goToStep(1) }
        binding.steps.step2.setOnClickListener { vm.goToStep(2) }
        binding.steps.step3.setOnClickListener { vm.goToStep(3) }

        // Top bar + system back both route to attemptExit at step 0, else previous step.
        binding.topBar.btnBack.setOnClickListener {
            if (vm.draft.value?.step == 0) attemptExit(repo) else vm.prev()
        }
        onBackPressed { if (vm.draft.value?.step == 0) attemptExit(repo) else vm.prev() }

        vm.draft.observe(viewLifecycleOwner) { draft ->
            binding_updating = true
            if (binding.etVenueName.text.toString() != draft.venueName)
                binding.etVenueName.setText(draft.venueName)
            if (binding.etCourseName.text.toString() != draft.courseName)
                binding.etCourseName.setText(draft.courseName)
            binding_updating = false

            binding.topBar.tvBarTitle.text =
                getString(if (vm.isEditing()) R.string.wizard_title_edit else R.string.wizard_title_create)
            binding.flipper.displayedChild = draft.step
            styleSteps(binding, draft.step)

            binding.tvHoleCount.text = draft.holeCount.toString()
            parAdapter.submit(draft.pars)

            binding.tvSummaryVenue.text = getString(R.string.save_summary_venue, draft.venueName)
            binding.tvSummaryCourse.text =
                getString(R.string.save_summary_course, draft.courseName, draft.holeCount)

            binding.btnPrev.text =
                if (draft.step == 0) getString(R.string.cancel) else getString(R.string.wizard_prev)
            binding.btnNext.text =
                if (draft.step == CourseWizardLogic.MAX_STEP) getString(R.string.wizard_save)
                else getString(R.string.wizard_next)
            val canAdvance = CourseWizardLogic.isStepValid(draft, draft.step)
            binding.btnNext.isEnabled = canAdvance
            binding.btnNext.alpha = if (canAdvance) 1f else 0.5f
        }
    }

    private fun styleSteps(binding: FragmentCourseWizardBinding, step: Int) {
        val labels = listOf(
            binding.steps.tvStep0Label, binding.steps.tvStep1Label,
            binding.steps.tvStep2Label, binding.steps.tvStep3Label,
        )
        labels.forEachIndexed { i, tv ->
            val active = i == step
            val colorRes = if (active) R.color.green_primary else R.color.text_muted
            tv.setTextColor(ContextCompat.getColor(requireContext(), colorRes))
            tv.setTypeface(null, if (active) android.graphics.Typeface.BOLD else android.graphics.Typeface.NORMAL)
        }
    }

    private fun maybeSuggestCourseName(repo: ParkGolfRepository) {
        val name = vm.draft.value?.venueName?.trim() ?: return
        viewLifecycleOwner.lifecycleScope.launch {
            val venues = repo.observeVenues().first()
            val existingId = CourseWizardLogic.resolveVenueId(name, venues, null)
            if (existingId != null) {
                val names = repo.coursesForVenue(existingId).map { it.name }
                vm.setCourseName(CourseWizardLogic.suggestCourseName(names))
            }
        }
    }

    private fun attemptExit(@Suppress("UNUSED_PARAMETER") repo: ParkGolfRepository) {
        val titleRes =
            if (vm.isEditing()) R.string.confirm_cancel_edit else R.string.confirm_cancel_create
        confirmYesNo(titleRes) { findNavController().popBackStack() }
    }

    private fun save(repo: ParkGolfRepository, binding: FragmentCourseWizardBinding) {
        val draft = vm.draft.value ?: return
        viewLifecycleOwner.lifecycleScope.launch {
            val venues = repo.observeVenues().first()
            val resolved = CourseWizardLogic.resolveVenueId(draft.venueName, venues, draft.editingVenueId)
            val venueId = resolved ?: repo.upsertVenue(VenueEntity(name = draft.venueName.trim()))
            if (draft.editingVenueId != null) {
                repo.upsertVenue(VenueEntity(id = venueId, name = draft.venueName.trim()))
            }
            repo.upsertCourse(
                CourseEntity(
                    id = draft.editingCourseId ?: 0L,
                    venueId = venueId,
                    name = draft.courseName.trim(),
                    pars = draft.pars,
                )
            )
            Snackbar.make(binding.root, R.string.course_saved, Snackbar.LENGTH_SHORT).show()
            findNavController().popBackStack()
        }
    }
}
```

- [ ] **Step 3: Route MyCourses to the wizard at course level**

Replace the body of `app/src/main/java/com/parkgolf/score/ui/courses/MyCoursesFragment.kt` with:

```kotlin
package com.parkgolf.score.ui.courses

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.parkgolf.score.App
import com.parkgolf.score.R
import com.parkgolf.score.data.ParkGolfRepository
import com.parkgolf.score.databinding.FragmentMyCoursesBinding
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

class MyCoursesFragment : Fragment(R.layout.fragment_my_courses) {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val binding = FragmentMyCoursesBinding.bind(view)
        val repo = App.repo(requireActivity().application)
        binding.rvVenues.layoutManager = LinearLayoutManager(requireContext())
        val adapter = VenueAdapter { venueId -> onVenueTapped(repo, venueId) }
        binding.rvVenues.adapter = adapter

        binding.btnAddVenue.setOnClickListener { openWizard(venueId = 0L, courseId = 0L) }

        viewLifecycleOwner.lifecycleScope.launch {
            combine(repo.observeVenues(), repo.observeCourses()) { venues, courses ->
                venues.map { v ->
                    val summary = courses.filter { it.venueId == v.id }.joinToString("·") { it.name }
                    VenueRow(v.id, v.name, summary)
                }
            }.collect { rows ->
                adapter.submit(rows)
                binding.tvEmpty.visibility = if (rows.isEmpty()) View.VISIBLE else View.GONE
            }
        }
    }

    private fun onVenueTapped(repo: ParkGolfRepository, venueId: Long) {
        viewLifecycleOwner.lifecycleScope.launch {
            val courses = repo.coursesForVenue(venueId)
            when {
                courses.isEmpty() -> openWizard(venueId, 0L)
                courses.size == 1 -> openWizard(venueId, courses[0].id)
                else -> {
                    val names = courses.map { it.name }.toTypedArray()
                    AlertDialog.Builder(requireContext())
                        .setTitle(R.string.pick_course_to_edit)
                        .setItems(names) { _, which -> openWizard(venueId, courses[which].id) }
                        .show()
                }
            }
        }
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

- [ ] **Step 4: Route StartFragment "new course" to the wizard**

In `app/src/main/java/com/parkgolf/score/ui/start/StartFragment.kt`, replace the `btnNewCourse` click block:

```kotlin
        binding.btnNewCourse.setOnClickListener {
            SelectionHolder.reset()
            findNavController().navigate(R.id.courseEditFragment)
        }
```

with:

```kotlin
        binding.btnNewCourse.setOnClickListener {
            SelectionHolder.reset()
            findNavController().currentBackStackEntry?.savedStateHandle?.apply {
                set("wizardVenueId", 0L)
                set("wizardCourseId", 0L)
            }
            findNavController().navigate(R.id.courseWizardFragment)
        }
```

- [ ] **Step 5: Delete the obsolete single-screen editor**

```bash
git rm app/src/main/java/com/parkgolf/score/ui/courses/CourseEditFragment.kt app/src/main/res/layout/fragment_course_edit.xml
```

- [ ] **Step 6: Build to verify everything compiles and links**

Run: `ANDROID_HOME=/Users/inhyo/Library/Android/sdk ./gradlew :app:assembleDebug`
Expected: BUILD SUCCESSFUL (no references to `courseEditFragment`/`CourseEditFragment` remain)

- [ ] **Step 7: Commit**

```bash
git add -A
git commit -m "feat: wire course wizard fragment and navigation, remove single-screen editor"
```

---

## Task 7: Game-recording exit confirmation

**Files:**
- Modify: `app/src/main/res/layout/fragment_hole_input.xml`
- Modify: `app/src/main/java/com/parkgolf/score/ui/hole/HoleInputFragment.kt`

- [ ] **Step 1: Add the top bar to the hole-input layout**

In `app/src/main/res/layout/fragment_hole_input.xml`, the root is a `ScrollView`. Wrap it so the top bar sits above the scroll area. Replace the opening `<ScrollView ...>` line and its attributes with a `LinearLayout` container holding the top bar and the existing `ScrollView`. Concretely, change the file to this structure (top bar added; existing inner content preserved):

Replace the first element:

```xml
<?xml version="1.0" encoding="utf-8"?>
<ScrollView xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:fillViewport="true"
    android:padding="@dimen/screen_padding">
```

with:

```xml
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:orientation="vertical">

    <include android:id="@+id/topBar" layout="@layout/view_top_bar" />

    <ScrollView
        android:layout_width="match_parent"
        android:layout_height="0dp"
        android:layout_weight="1"
        android:fillViewport="true"
        android:padding="@dimen/screen_padding">
```

Then at the very end of the file, after the existing closing `</ScrollView>`, add one closing tag:

```xml
</LinearLayout>
```

(The inner `<LinearLayout>...</LinearLayout>` that was the ScrollView's single child stays exactly as-is.)

- [ ] **Step 2: Wire the back button + system back to the exit confirm**

In `app/src/main/java/com/parkgolf/score/ui/hole/HoleInputFragment.kt`, add imports at the top (with the other imports):

```kotlin
import com.parkgolf.score.ui.common.confirmYesNo
import com.parkgolf.score.ui.common.onBackPressed
```

Then, inside `onViewCreated`, after the line `val round = session.round.value ?: run { findNavController().popBackStack(); return }`, add:

```kotlin
        binding.topBar.tvBarTitle.text = getString(R.string.game_recording)
        val exitToHome = {
            confirmYesNo(R.string.confirm_exit_game) {
                findNavController().popBackStack(R.id.homeFragment, false)
            }
        }
        binding.topBar.btnBack.setOnClickListener { exitToHome() }
        onBackPressed { exitToHome() }
```

- [ ] **Step 3: Build to verify it compiles**

Run: `ANDROID_HOME=/Users/inhyo/Library/Android/sdk ./gradlew :app:assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 4: Commit**

```bash
git add app/src/main/res/layout/fragment_hole_input.xml app/src/main/java/com/parkgolf/score/ui/hole/HoleInputFragment.kt
git commit -m "feat: add exit confirmation to game-recording screen"
```

---

## Task 8: Instrumented flow tests + cleanup

**Files:**
- Create: `app/src/androidTest/java/com/parkgolf/score/ui/CourseWizardFlowTest.kt`
- Delete (if now unused): `app/src/main/java/com/parkgolf/score/ui/courses/ParEditorAdapter.kt`

- [ ] **Step 1: Write the instrumented flow test**

Create `app/src/androidTest/java/com/parkgolf/score/ui/CourseWizardFlowTest.kt`. This drives the wizard end-to-end and checks the back-confirm dialog:

```kotlin
package com.parkgolf.score.ui

import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.closeSoftKeyboard
import androidx.test.espresso.action.ViewActions.replaceText
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.parkgolf.score.R
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CourseWizardFlowTest {

    @Test fun createCourse_throughAllSteps_savesAndReturns() {
        ActivityScenario.launch(MainActivity::class.java)
        // Home -> 내 구장 관리
        onView(withId(R.id.btnMyCourses)).perform(click())
        // + 새 구장 만들기 -> wizard
        onView(withId(R.id.btnAddVenue)).perform(click())
        // Step 0: venue name
        onView(withId(R.id.etVenueName)).perform(replaceText("테스트구장"), closeSoftKeyboard())
        onView(withId(R.id.btnNext)).perform(click())
        // Step 1: course name (default A코스)
        onView(withId(R.id.btnNext)).perform(click())
        // Step 2: hole setup
        onView(withId(R.id.btnNext)).perform(click())
        // Step 3: save
        onView(withId(R.id.btnNext)).perform(click())
        // Back on the list, the new venue appears
        onView(withText("테스트구장")).check(matches(isDisplayed()))
    }

    @Test fun backOnFirstStep_showsCancelDialog() {
        ActivityScenario.launch(MainActivity::class.java)
        onView(withId(R.id.btnMyCourses)).perform(click())
        onView(withId(R.id.btnAddVenue)).perform(click())
        onView(withId(R.id.etVenueName)).perform(replaceText("취소구장"), closeSoftKeyboard())
        // Tap the top-bar back button
        onView(withId(R.id.btnBack)).perform(click())
        onView(withText(R.string.confirm_cancel_create)).check(matches(isDisplayed()))
    }
}
```

- [ ] **Step 2: Boot an emulator if none is running**

Run: `/Users/inhyo/Library/Android/sdk/platform-tools/adb devices`
Expected: an `emulator-XXXX device` line. If none, start one:
`nohup /Users/inhyo/Library/Android/sdk/emulator/emulator -avd Medium_Phone_API_36.1 -no-snapshot-load >/dev/null 2>&1 &` then wait for `adb shell getprop sys.boot_completed` to return `1`.

- [ ] **Step 3: Run the instrumented tests**

Run: `ANDROID_HOME=/Users/inhyo/Library/Android/sdk ./gradlew :app:connectedDebugAndroidTest`
Expected: PASS — `CourseWizardFlowTest` (2), `CoreFlowTest` (2), `RepositoryTest` all green.

- [ ] **Step 4: Remove the now-unused old par adapter**

Confirm `ParEditorAdapter` has no remaining references (its only user, `CourseEditFragment`, was deleted in Task 6):

Run: `grep -rn "ParEditorAdapter" app/src`
Expected: no matches. If none, delete it:
`git rm app/src/main/java/com/parkgolf/score/ui/courses/ParEditorAdapter.kt`
(Keep `item_par_editor.xml` — `WizardParAdapter` uses it.)

- [ ] **Step 5: Run the full unit test suite**

Run: `ANDROID_HOME=/Users/inhyo/Library/Android/sdk ./gradlew :app:testDebugUnitTest`
Expected: BUILD SUCCESSFUL — all existing + new unit tests pass.

- [ ] **Step 6: Commit**

```bash
git add -A
git commit -m "test: add course-wizard instrumented flow tests; remove unused editor adapter"
```

---

## Verification Checklist (run after all tasks)

- [ ] `./gradlew :app:testDebugUnitTest` — all unit tests pass (includes `CourseWizardLogicTest`, `CourseWizardViewModelTest`).
- [ ] `./gradlew :app:connectedDebugAndroidTest` — all instrumented tests pass.
- [ ] `./gradlew :app:assembleDebug` — clean build.
- [ ] Manual: 내 구장 관리 → + 새 구장 → 4단계 통과 → 저장 → 목록에 표시.
- [ ] Manual: 같은 이름으로 다시 만들면 그 구장에 코스가 추가(통합)됨.
- [ ] Manual: 구장 탭(코스 여러 개면 선택) → 위저드에 기존 값 채워짐 → 수정 저장.
- [ ] Manual: 위저드 스텝1에서 뒤로(버튼/제스처) → "코스 작성을 취소하시겠습니까?" 확인창.
- [ ] Manual: 게임 기록 중 뒤로 → "현재 게임을 저장하지 않고 나가시겠습니까?" → 네 → 홈.
