# shadcn Retheme & Course-Wizard Redesign Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Retheme the whole app to the mockup's shadcn tokens (dark primary), show the shared top bar on every non-home screen, and rebuild the course-registration wizard to match the new Figma mockup (card styling, hole add/delete, course-name duplicate validation, existing-venue detection, save-success screen).

**Architecture:** A design-system foundation (color tokens + reusable drawables + icon vectors + button styles) is added first. Legacy color names become aliases remapped to the new palette, so untouched screens recolor with zero per-widget edits. The course wizard keeps its single-Fragment + `ViewFlipper` shape but grows to 5 children (4 steps + success) with card-styled `<include>` layouts; all wizard state stays in one `CourseDraft` (now without `holeCount`) held by `CourseWizardViewModel`, and pure decisions live in `CourseWizardLogic`. Other non-home screens get the shared top bar added and inherit the new palette via aliases.

**Tech Stack:** Kotlin, View system (XML) + ConstraintLayout + ViewBinding, Jetpack Navigation, Room, Material Components, JUnit4 + Truth (unit), Espresso (instrumented). minSdk 21, light theme only.

**Design spec:** `docs/superpowers/specs/2026-07-30-parkgolf-shadcn-retheme-and-course-wizard-redesign-design.md`
**Mockup:** `docs/mockups/코스 작성/src/app/App.tsx`

---

## Conventions for every task

- Working dir is the repo root (a worktree if one was created). Prefix gradle with `ANDROID_HOME=/Users/inhyo/Library/Android/sdk`.
- Color hex approximations of the mockup's oklch/rgba values are fixed in Task 1 — always reference tokens, never raw hex, in later tasks.
- Icons are lucide stroke vectors: they carry `android:strokeColor="@color/foreground"` by default; at a usage that needs another color (e.g. on a dark button) set `app:tint` on the `ImageView`/`ImageButton`.

---

## File Structure

**New — design system:**
- `res/drawable/bg_card.xml`, `bg_input.xml`, `bg_input_error.xml`, `bg_pill_primary.xml`, `bg_pill_secondary.xml`, `bg_dashed.xml`, `bg_step_active.xml`, `bg_step_done.xml`, `bg_step_inactive.xml`, `bg_number_badge.xml`, `bg_success_circle.xml`
- `res/drawable/ic_map_pin.xml`, `ic_flag.xml`, `ic_list_ordered.xml`, `ic_save.xml`, `ic_check.xml`, `ic_plus.xml`, `ic_minus.xml`, `ic_trash.xml`, `ic_chevron_left.xml`, `ic_chevron_right.xml`, `ic_alert_circle.xml`
- `res/values/styles.xml`

**New — wizard:**
- `res/layout/view_wizard_step_venue.xml`, `view_wizard_step_course.xml`, `view_wizard_step_holes.xml`, `view_wizard_step_save.xml`, `view_wizard_success.xml`
- `res/layout/item_hole_card.xml`, `item_venue_suggestion.xml`, `item_par_preview.xml`
- `java/.../ui/courses/HoleCardAdapter.kt`, `VenueSuggestionAdapter.kt`, `ParPreviewAdapter.kt`

**Modified:**
- `res/values/colors.xml`, `res/values/themes.xml`, `res/values/dimens.xml`
- `res/layout/view_top_bar.xml`
- `res/layout/fragment_course_wizard.xml`
- `java/.../ui/courses/CourseWizardLogic.kt`, `CourseWizardViewModel.kt`, `CourseWizardFragment.kt`
- `test/.../ui/CourseWizardLogicTest.kt`, `CourseWizardViewModelTest.kt`
- `androidTest/.../ui/CourseWizardFlowTest.kt`
- Non-home screens (Task 8): `fragment_start.xml`, `fragment_course_setup.xml`, `fragment_player_setup.xml`, `fragment_grid_view.xml`, `fragment_round_summary.xml`, `fragment_history.xml`, `fragment_round_detail.xml`, `fragment_my_courses.xml` + their fragments (title/back wiring); `item_recent_course.xml`, `fragment_hole_input.xml` (already has bar) recolor via aliases.

**Removed:** `res/layout/view_wizard_steps.xml` and `WizardParAdapter.kt` (replaced), if unused after Task 7.

---

## Task 1: Color tokens, theme, dimens, button styles

**Files:**
- Modify: `app/src/main/res/values/colors.xml`
- Modify: `app/src/main/res/values/themes.xml`
- Modify: `app/src/main/res/values/dimens.xml`
- Create: `app/src/main/res/values/styles.xml`

- [ ] **Step 1: Replace colors.xml** with the new palette plus legacy aliases:

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <!-- shadcn tokens (light) -->
    <color name="primary">#030213</color>
    <color name="on_primary">#FFFFFF</color>
    <color name="primary_10">#1A030213</color>
    <color name="background">#FFFFFF</color>
    <color name="foreground">#252525</color>
    <color name="secondary">#F1F1F4</color>
    <color name="secondary_foreground">#030213</color>
    <color name="muted">#ECECF0</color>
    <color name="muted_foreground">#717182</color>
    <color name="accent">#E9EBEF</color>
    <color name="accent_foreground">#030213</color>
    <color name="border">#1A000000</color>
    <color name="destructive">#D4183D</color>
    <color name="on_destructive">#FFFFFF</color>
    <color name="input_background">#F3F3F5</color>

    <!-- semantic par-relation colors (unchanged) -->
    <color name="under_par">#1565C0</color>
    <color name="even_par">#252525</color>
    <color name="over_par">#C62828</color>

    <!-- legacy aliases → remapped to new palette so existing layouts recolor for free -->
    <color name="surface">@color/background</color>
    <color name="surface_muted">@color/secondary</color>
    <color name="text_primary">@color/foreground</color>
    <color name="text_muted">@color/muted_foreground</color>
    <color name="green_primary">@color/primary</color>
    <color name="green_light">@color/primary_10</color>
    <color name="green_disabled">@color/muted</color>
    <color name="stepper_bg">@color/secondary</color>
</resources>
```

- [ ] **Step 2: Update themes.xml** to the new tokens:

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources xmlns:tools="http://schemas.android.com/tools">
    <style name="Theme.ParkGolf" parent="Theme.Material3.DayNight.NoActionBar">
        <item name="colorPrimary">@color/primary</item>
        <item name="colorOnPrimary">@color/on_primary</item>
        <item name="android:windowBackground">@color/background</item>
        <item name="android:textColorPrimary">@color/foreground</item>
    </style>
</resources>
```

- [ ] **Step 3: Add radii to dimens.xml** (append inside `<resources>`):

```xml
    <dimen name="radius_card">16dp</dimen>
    <dimen name="radius_sm">10dp</dimen>
    <dimen name="radius_pill">999dp</dimen>
    <dimen name="border_width">2dp</dimen>
```

- [ ] **Step 4: Create styles.xml**:

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <style name="Widget.App.Button.Primary" parent="Widget.Material3.Button">
        <item name="backgroundTint">@color/primary</item>
        <item name="android:textColor">@color/on_primary</item>
        <item name="cornerRadius">@dimen/radius_card</item>
        <item name="android:minHeight">@dimen/touch_min</item>
        <item name="android:textSize">@dimen/button_text</item>
        <item name="android:textStyle">bold</item>
    </style>

    <style name="Widget.App.Button.Outline" parent="Widget.Material3.Button.OutlinedButton">
        <item name="strokeColor">@color/border</item>
        <item name="strokeWidth">@dimen/border_width</item>
        <item name="android:textColor">@color/foreground</item>
        <item name="cornerRadius">@dimen/radius_card</item>
        <item name="android:minHeight">@dimen/touch_min</item>
        <item name="android:textSize">@dimen/button_text</item>
        <item name="android:textStyle">bold</item>
    </style>
</resources>
```

- [ ] **Step 5: Build** — `ANDROID_HOME=/Users/inhyo/Library/Android/sdk ./gradlew :app:processDebugResources` → BUILD SUCCESSFUL.

- [ ] **Step 6: Commit**
```bash
git add app/src/main/res/values/colors.xml app/src/main/res/values/themes.xml app/src/main/res/values/dimens.xml app/src/main/res/values/styles.xml
git commit -m "feat: add shadcn color tokens, theme, radii, and button styles"
```

---

## Task 2: Reusable drawables

**Files (all Create under `app/src/main/res/drawable/`):**

- [ ] **Step 1: bg_card.xml**
```xml
<?xml version="1.0" encoding="utf-8"?>
<shape xmlns:android="http://schemas.android.com/apk/res/android" android:shape="rectangle">
    <solid android:color="@color/background" />
    <corners android:radius="@dimen/radius_card" />
    <stroke android:width="@dimen/border_width" android:color="@color/border" />
</shape>
```

- [ ] **Step 2: bg_input.xml** (focus-aware selector)
```xml
<?xml version="1.0" encoding="utf-8"?>
<selector xmlns:android="http://schemas.android.com/apk/res/android">
    <item android:state_focused="true">
        <shape android:shape="rectangle">
            <solid android:color="@color/input_background" />
            <corners android:radius="@dimen/radius_card" />
            <stroke android:width="@dimen/border_width" android:color="@color/primary" />
        </shape>
    </item>
    <item>
        <shape android:shape="rectangle">
            <solid android:color="@color/input_background" />
            <corners android:radius="@dimen/radius_card" />
            <stroke android:width="@dimen/border_width" android:color="@color/border" />
        </shape>
    </item>
</selector>
```

- [ ] **Step 3: bg_input_error.xml**
```xml
<?xml version="1.0" encoding="utf-8"?>
<shape xmlns:android="http://schemas.android.com/apk/res/android" android:shape="rectangle">
    <solid android:color="@color/input_background" />
    <corners android:radius="@dimen/radius_card" />
    <stroke android:width="@dimen/border_width" android:color="@color/destructive" />
</shape>
```

- [ ] **Step 4: bg_pill_primary.xml**
```xml
<?xml version="1.0" encoding="utf-8"?>
<shape xmlns:android="http://schemas.android.com/apk/res/android" android:shape="rectangle">
    <solid android:color="@color/primary_10" />
    <corners android:radius="@dimen/radius_pill" />
</shape>
```

- [ ] **Step 5: bg_pill_secondary.xml**
```xml
<?xml version="1.0" encoding="utf-8"?>
<shape xmlns:android="http://schemas.android.com/apk/res/android" android:shape="rectangle">
    <solid android:color="@color/secondary" />
    <corners android:radius="@dimen/radius_pill" />
</shape>
```

- [ ] **Step 6: bg_dashed.xml**
```xml
<?xml version="1.0" encoding="utf-8"?>
<shape xmlns:android="http://schemas.android.com/apk/res/android" android:shape="rectangle">
    <solid android:color="@android:color/transparent" />
    <corners android:radius="@dimen/radius_card" />
    <stroke android:width="@dimen/border_width" android:color="@color/primary"
        android:dashWidth="6dp" android:dashGap="4dp" />
</shape>
```

- [ ] **Step 7: bg_step_active.xml**
```xml
<?xml version="1.0" encoding="utf-8"?>
<shape xmlns:android="http://schemas.android.com/apk/res/android" android:shape="oval">
    <solid android:color="@color/primary_10" />
    <stroke android:width="@dimen/border_width" android:color="@color/primary" />
</shape>
```

- [ ] **Step 8: bg_step_done.xml**
```xml
<?xml version="1.0" encoding="utf-8"?>
<shape xmlns:android="http://schemas.android.com/apk/res/android" android:shape="oval">
    <solid android:color="@color/primary" />
</shape>
```

- [ ] **Step 9: bg_step_inactive.xml**
```xml
<?xml version="1.0" encoding="utf-8"?>
<shape xmlns:android="http://schemas.android.com/apk/res/android" android:shape="oval">
    <solid android:color="@color/background" />
    <stroke android:width="@dimen/border_width" android:color="@color/border" />
</shape>
```

- [ ] **Step 10: bg_number_badge.xml**
```xml
<?xml version="1.0" encoding="utf-8"?>
<shape xmlns:android="http://schemas.android.com/apk/res/android" android:shape="rectangle">
    <solid android:color="@color/secondary" />
    <corners android:radius="@dimen/radius_sm" />
</shape>
```

- [ ] **Step 11: bg_success_circle.xml**
```xml
<?xml version="1.0" encoding="utf-8"?>
<shape xmlns:android="http://schemas.android.com/apk/res/android" android:shape="oval">
    <solid android:color="@color/primary_10" />
    <stroke android:width="3dp" android:color="@color/primary" />
</shape>
```

- [ ] **Step 12: Build** — `./gradlew :app:processDebugResources` → BUILD SUCCESSFUL.

- [ ] **Step 13: Commit**
```bash
git add app/src/main/res/drawable/bg_*.xml
git commit -m "feat: add shadcn-style card/input/pill/step drawables"
```

---

## Task 3: Icon vectors (lucide)

**Files (all Create under `app/src/main/res/drawable/`).** Each is a 24dp stroke vector. Template — strokeColor defaults to `@color/foreground`; usages needing another color set `app:tint`.

- [ ] **Step 1: Create the eleven icons.**

`ic_chevron_left.xml`
```xml
<vector xmlns:android="http://schemas.android.com/apk/res/android" android:width="24dp" android:height="24dp" android:viewportWidth="24" android:viewportHeight="24">
    <path android:strokeColor="@color/foreground" android:strokeWidth="2" android:strokeLineCap="round" android:strokeLineJoin="round" android:pathData="M15,18 L9,12 L15,6" />
</vector>
```
`ic_chevron_right.xml`
```xml
<vector xmlns:android="http://schemas.android.com/apk/res/android" android:width="24dp" android:height="24dp" android:viewportWidth="24" android:viewportHeight="24">
    <path android:strokeColor="@color/foreground" android:strokeWidth="2" android:strokeLineCap="round" android:strokeLineJoin="round" android:pathData="M9,18 L15,12 L9,6" />
</vector>
```
`ic_plus.xml`
```xml
<vector xmlns:android="http://schemas.android.com/apk/res/android" android:width="24dp" android:height="24dp" android:viewportWidth="24" android:viewportHeight="24">
    <path android:strokeColor="@color/foreground" android:strokeWidth="2" android:strokeLineCap="round" android:pathData="M12,5 L12,19 M5,12 L19,12" />
</vector>
```
`ic_minus.xml`
```xml
<vector xmlns:android="http://schemas.android.com/apk/res/android" android:width="24dp" android:height="24dp" android:viewportWidth="24" android:viewportHeight="24">
    <path android:strokeColor="@color/foreground" android:strokeWidth="2" android:strokeLineCap="round" android:pathData="M5,12 L19,12" />
</vector>
```
`ic_check.xml`
```xml
<vector xmlns:android="http://schemas.android.com/apk/res/android" android:width="24dp" android:height="24dp" android:viewportWidth="24" android:viewportHeight="24">
    <path android:strokeColor="@color/foreground" android:strokeWidth="2.5" android:strokeLineCap="round" android:strokeLineJoin="round" android:pathData="M20,6 L9,17 L4,12" />
</vector>
```
`ic_trash.xml`
```xml
<vector xmlns:android="http://schemas.android.com/apk/res/android" android:width="24dp" android:height="24dp" android:viewportWidth="24" android:viewportHeight="24">
    <path android:strokeColor="@color/foreground" android:strokeWidth="2" android:strokeLineCap="round" android:strokeLineJoin="round" android:pathData="M3,6 L21,6 M19,6 L19,20 A2,2 0 0 1 17,22 L7,22 A2,2 0 0 1 5,20 L5,6 M8,6 L8,4 A2,2 0 0 1 10,2 L14,2 A2,2 0 0 1 16,4 L16,6" />
</vector>
```
`ic_map_pin.xml`
```xml
<vector xmlns:android="http://schemas.android.com/apk/res/android" android:width="24dp" android:height="24dp" android:viewportWidth="24" android:viewportHeight="24">
    <path android:strokeColor="@color/foreground" android:strokeWidth="2" android:strokeLineCap="round" android:strokeLineJoin="round" android:pathData="M21,10 C21,17 12,23 12,23 C12,23 3,17 3,10 A9,9 0 0 1 21,10 Z" />
    <path android:strokeColor="@color/foreground" android:strokeWidth="2" android:pathData="M15,10 A3,3 0 1 1 9,10 A3,3 0 0 1 15,10 Z" />
</vector>
```
`ic_flag.xml`
```xml
<vector xmlns:android="http://schemas.android.com/apk/res/android" android:width="24dp" android:height="24dp" android:viewportWidth="24" android:viewportHeight="24">
    <path android:strokeColor="@color/foreground" android:strokeWidth="2" android:strokeLineCap="round" android:strokeLineJoin="round" android:pathData="M4,15 C4,15 5,14 8,14 C11,14 13,16 16,16 C19,16 20,15 20,15 L20,3 C20,3 19,4 16,4 C13,4 11,2 8,2 C5,2 4,3 4,3 Z M4,22 L4,15" />
</vector>
```
`ic_list_ordered.xml`
```xml
<vector xmlns:android="http://schemas.android.com/apk/res/android" android:width="24dp" android:height="24dp" android:viewportWidth="24" android:viewportHeight="24">
    <path android:strokeColor="@color/foreground" android:strokeWidth="2" android:strokeLineCap="round" android:strokeLineJoin="round" android:pathData="M10,6 L21,6 M10,12 L21,12 M10,18 L21,18 M4,6 L5,6 L5,10 M4,10 L6,10 M6,18 L4,18 C4,17 6,16 6,15 C6,14 5,13.5 4,14" />
</vector>
```
`ic_save.xml`
```xml
<vector xmlns:android="http://schemas.android.com/apk/res/android" android:width="24dp" android:height="24dp" android:viewportWidth="24" android:viewportHeight="24">
    <path android:strokeColor="@color/foreground" android:strokeWidth="2" android:strokeLineCap="round" android:strokeLineJoin="round" android:pathData="M19,21 L5,21 A2,2 0 0 1 3,19 L3,5 A2,2 0 0 1 5,3 L16,3 L21,8 L21,19 A2,2 0 0 1 19,21 Z M17,21 L17,13 L7,13 L7,21 M7,3 L7,8 L15,8" />
</vector>
```
`ic_alert_circle.xml`
```xml
<vector xmlns:android="http://schemas.android.com/apk/res/android" android:width="24dp" android:height="24dp" android:viewportWidth="24" android:viewportHeight="24">
    <path android:strokeColor="@color/destructive" android:strokeWidth="2" android:pathData="M22,12 A10,10 0 1 1 2,12 A10,10 0 0 1 22,12 Z" />
    <path android:strokeColor="@color/destructive" android:strokeWidth="2" android:strokeLineCap="round" android:pathData="M12,8 L12,12 M12,16 L12.01,16" />
</vector>
```

- [ ] **Step 2: Build** — `./gradlew :app:processDebugResources` → BUILD SUCCESSFUL.

- [ ] **Step 3: Commit**
```bash
git add app/src/main/res/drawable/ic_*.xml
git commit -m "feat: add lucide-style icon vectors for wizard and top bar"
```

---

## Task 4: Common top bar redesign

**Files:**
- Modify: `app/src/main/res/layout/view_top_bar.xml`

- [ ] **Step 1: Replace view_top_bar.xml** — left `‹ 뒤로` (chevron + text), centered title, right spacer:

```xml
<?xml version="1.0" encoding="utf-8"?>
<androidx.constraintlayout.widget.ConstraintLayout
    xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:background="@color/background"
    android:minHeight="64dp"
    android:paddingHorizontal="4dp">

    <LinearLayout
        android:id="@+id/btnBack"
        android:layout_width="wrap_content"
        android:layout_height="@dimen/touch_min"
        android:orientation="horizontal"
        android:gravity="center_vertical"
        android:paddingHorizontal="8dp"
        android:background="?attr/selectableItemBackground"
        android:clickable="true"
        android:focusable="true"
        android:contentDescription="@string/back"
        app:layout_constraintStart_toStartOf="parent"
        app:layout_constraintTop_toTopOf="parent"
        app:layout_constraintBottom_toBottomOf="parent">
        <ImageView
            android:layout_width="22dp" android:layout_height="22dp"
            android:src="@drawable/ic_chevron_left"
            android:importantForAccessibility="no" />
        <TextView
            android:layout_width="wrap_content" android:layout_height="wrap_content"
            android:layout_marginStart="2dp"
            android:text="@string/back"
            android:textColor="@color/foreground"
            android:textSize="@dimen/body_text"
            android:textStyle="bold" />
    </LinearLayout>

    <TextView
        android:id="@+id/tvBarTitle"
        android:layout_width="0dp"
        android:layout_height="wrap_content"
        android:gravity="center"
        android:textSize="@dimen/body_text"
        android:textStyle="bold"
        android:textColor="@color/foreground"
        android:maxLines="1"
        android:ellipsize="end"
        app:layout_constraintStart_toEndOf="@id/btnBack"
        app:layout_constraintEnd_toStartOf="@id/barSpacer"
        app:layout_constraintTop_toTopOf="parent"
        app:layout_constraintBottom_toBottomOf="parent" />

    <Space
        android:id="@+id/barSpacer"
        android:layout_width="72dp"
        android:layout_height="1dp"
        app:layout_constraintEnd_toEndOf="parent"
        app:layout_constraintTop_toTopOf="parent" />

    <View android:layout_width="0dp" android:layout_height="@dimen/border_width"
        android:background="@color/border"
        app:layout_constraintStart_toStartOf="parent"
        app:layout_constraintEnd_toEndOf="parent"
        app:layout_constraintBottom_toBottomOf="parent" />
</androidx.constraintlayout.widget.ConstraintLayout>
```

Note: `btnBack` changed from `ImageButton` to a clickable `LinearLayout` — its `setOnClickListener` in `HoleInputFragment` (Task 8 verifies) and `CourseWizardFragment` (Task 7) still works unchanged.

- [ ] **Step 2: Build** — `./gradlew :app:assembleDebug` → BUILD SUCCESSFUL (existing top-bar users — hole input, wizard — still compile since `btnBack`/`tvBarTitle` ids persist).

- [ ] **Step 3: Commit**
```bash
git add app/src/main/res/layout/view_top_bar.xml
git commit -m "feat: redesign top bar to chevron + 뒤로 text with centered title"
```

---

## Task 5: Wizard logic + ViewModel changes (TDD)

**Files:**
- Modify: `app/src/main/java/com/parkgolf/score/ui/courses/CourseWizardLogic.kt`
- Modify: `app/src/main/java/com/parkgolf/score/ui/courses/CourseWizardViewModel.kt`
- Modify: `app/src/test/java/com/parkgolf/score/ui/CourseWizardLogicTest.kt`
- Modify: `app/src/test/java/com/parkgolf/score/ui/CourseWizardViewModelTest.kt`

- [ ] **Step 1: Replace CourseWizardLogicTest.kt** with tests for the new behavior:

```kotlin
package com.parkgolf.score.ui

import com.google.common.truth.Truth.assertThat
import com.parkgolf.score.data.db.entity.VenueEntity
import com.parkgolf.score.ui.courses.CourseDraft
import com.parkgolf.score.ui.courses.CourseWizardLogic
import org.junit.Test

class CourseWizardLogicTest {

    @Test fun addHole_appendsParThree() {
        assertThat(CourseWizardLogic.addHole(listOf(3, 4))).containsExactly(3, 4, 3).inOrder()
    }

    @Test fun deleteHole_removesIndex() {
        assertThat(CourseWizardLogic.deleteHole(listOf(3, 4, 5), 1)).containsExactly(3, 5).inOrder()
    }

    @Test fun deleteHole_keepsAtLeastOneHole() {
        assertThat(CourseWizardLogic.deleteHole(listOf(3), 0)).containsExactly(3).inOrder()
    }

    @Test fun clampPar_flooredAtOne_noUpperBound() {
        assertThat(CourseWizardLogic.clampPar(-2)).isEqualTo(1)
        assertThat(CourseWizardLogic.clampPar(7)).isEqualTo(7)
    }

    @Test fun isExistingVenue_exactMatchIgnoringCaseAndSpace() {
        val venues = listOf(VenueEntity(id = 1, name = "한강 파크골프장"))
        assertThat(CourseWizardLogic.isExistingVenue(" 한강 파크골프장 ", venues)).isTrue()
        assertThat(CourseWizardLogic.isExistingVenue("한강", venues)).isFalse()
    }

    @Test fun matchedVenues_partialMatchesExcludingExact() {
        val venues = listOf(VenueEntity(1, "한강 파크골프장"), VenueEntity(2, "탄천 파크골프장"))
        assertThat(CourseWizardLogic.matchedVenues("한강", venues)).containsExactly("한강 파크골프장")
        // exact match returns empty (badge path handles it, not suggestion list)
        assertThat(CourseWizardLogic.matchedVenues("한강 파크골프장", venues)).isEmpty()
        assertThat(CourseWizardLogic.matchedVenues("", venues)).isEmpty()
    }

    @Test fun isDuplicateCourseName_ignoringCaseAndSpace() {
        val existing = listOf("A코스", "B코스")
        assertThat(CourseWizardLogic.isDuplicateCourseName(" a코스 ", existing)).isTrue()
        assertThat(CourseWizardLogic.isDuplicateCourseName("C코스", existing)).isFalse()
    }

    @Test fun stepValidity() {
        assertThat(CourseWizardLogic.venueStepValid(CourseDraft(venueName = " "))).isFalse()
        assertThat(CourseWizardLogic.venueStepValid(CourseDraft(venueName = "한강"))).isTrue()
        assertThat(CourseWizardLogic.courseStepValid("A코스", listOf("A코스"))).isFalse()
        assertThat(CourseWizardLogic.courseStepValid("C코스", listOf("A코스"))).isTrue()
        assertThat(CourseWizardLogic.courseStepValid("  ", emptyList())).isFalse()
        assertThat(CourseWizardLogic.holeStepValid(emptyList())).isFalse()
        assertThat(CourseWizardLogic.holeStepValid(listOf(3))).isTrue()
    }

    @Test fun suggestCourseName_skipsUsedLetters() {
        assertThat(CourseWizardLogic.suggestCourseName(listOf("A코스", "B코스"))).isEqualTo("C코스")
    }

    @Test fun resolveVenueId_matchOrNull() {
        val venues = listOf(VenueEntity(id = 7, name = "한강"))
        assertThat(CourseWizardLogic.resolveVenueId("한강", venues, null)).isEqualTo(7)
        assertThat(CourseWizardLogic.resolveVenueId("낙동강", venues, null)).isNull()
        assertThat(CourseWizardLogic.resolveVenueId("한강", venues, 99)).isEqualTo(99)
    }
}
```

- [ ] **Step 2: Run tests to verify they fail** — `./gradlew :app:testDebugUnitTest --tests "com.parkgolf.score.ui.CourseWizardLogicTest"` → FAIL (new functions / `holeCount` removal not yet done).

- [ ] **Step 3: Replace CourseWizardLogic.kt**:

```kotlin
package com.parkgolf.score.ui.courses

import com.parkgolf.score.data.db.entity.VenueEntity

/** All wizard state in one immutable value. step is 0..3 (4 = success handled by fragment). */
data class CourseDraft(
    val editingVenueId: Long? = null,
    val editingCourseId: Long? = null,
    val venueName: String = "",
    val courseName: String = "A코스",
    val pars: List<Int> = List(9) { 3 },
    val step: Int = 0,
)

/** Pure decision logic for the course wizard; no Android dependencies. */
object CourseWizardLogic {
    const val MIN_PAR = 1
    const val MAX_STEP = 3

    fun addHole(pars: List<Int>): List<Int> = pars + 3

    fun deleteHole(pars: List<Int>, index: Int): List<Int> =
        if (pars.size <= 1 || index !in pars.indices) pars
        else pars.toMutableList().apply { removeAt(index) }

    /** Par floored at 1, no upper bound. */
    fun clampPar(value: Int): Int = value.coerceAtLeast(MIN_PAR)

    /** Exact venue-name match (trim, case-insensitive). */
    fun isExistingVenue(name: String, venues: List<VenueEntity>): Boolean {
        val key = name.trim()
        return key.isNotEmpty() && venues.any { it.name.trim().equals(key, ignoreCase = true) }
    }

    /** Partial matches (venue name contains input), excluding exact matches. name must be non-blank. */
    fun matchedVenues(name: String, venues: List<VenueEntity>): List<String> {
        val key = name.trim()
        if (key.isEmpty()) return emptyList()
        return venues.map { it.name }
            .filter { it.contains(key, ignoreCase = true) && !it.trim().equals(key, ignoreCase = true) }
            .distinct()
    }

    fun isDuplicateCourseName(name: String, existingCourseNames: List<String>): Boolean {
        val key = name.trim()
        return existingCourseNames.any { it.trim().equals(key, ignoreCase = true) }
    }

    fun venueStepValid(draft: CourseDraft): Boolean = draft.venueName.isNotBlank()

    fun courseStepValid(name: String, existingCourseNames: List<String>): Boolean =
        name.isNotBlank() && !isDuplicateCourseName(name, existingCourseNames)

    fun holeStepValid(pars: List<Int>): Boolean = pars.isNotEmpty()

    /** Next unused course letter as "X코스". */
    fun suggestCourseName(existingCourseNames: List<String>): String {
        val used = existingCourseNames.mapNotNull { it.trim().firstOrNull() }.toSet()
        val next = ('A'..'Z').firstOrNull { it !in used } ?: 'A'
        return "${next}코스"
    }

    /** Existing venue id matching [name], or [editingVenueId], else null. */
    fun resolveVenueId(name: String, existing: List<VenueEntity>, editingVenueId: Long?): Long? {
        if (editingVenueId != null) return editingVenueId
        val key = name.trim()
        return existing.firstOrNull { it.name.trim().equals(key, ignoreCase = true) }?.id
    }
}
```

- [ ] **Step 4: Replace CourseWizardViewModelTest.kt**:

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

    @Test fun initNew_setsCourseNameStepZeroNineHoles() {
        val vm = vm()
        assertThat(vm.draft.value!!.courseName).isEqualTo("A코스")
        assertThat(vm.draft.value!!.step).isEqualTo(0)
        assertThat(vm.draft.value!!.pars).hasSize(9)
    }

    @Test fun addHole_and_deleteHole() {
        val vm = vm()
        vm.addHole()
        assertThat(vm.draft.value!!.pars).hasSize(10)
        vm.deleteHole(0)
        assertThat(vm.draft.value!!.pars).hasSize(9)
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

    @Test fun loadForEdit_populatesDraftWithPars() {
        val vm = CourseWizardViewModel()
        vm.loadForEdit(venueId = 5, courseId = 8, venueName = "한강", courseName = "B코스", pars = listOf(3, 4, 5))
        val d = vm.draft.value!!
        assertThat(d.editingVenueId).isEqualTo(5)
        assertThat(d.editingCourseId).isEqualTo(8)
        assertThat(d.pars).containsExactly(3, 4, 5).inOrder()
    }

    @Test fun isEditing_reflectsEditingCourseId() {
        assertThat(vm().isEditing()).isFalse()
        val vm = CourseWizardViewModel()
        vm.loadForEdit(1, 2, "v", "c", listOf(3))
        assertThat(vm.isEditing()).isTrue()
    }
}
```

- [ ] **Step 5: Replace CourseWizardViewModel.kt**:

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

    fun initNew(suggestedCourseName: String) { _draft.value = CourseDraft(courseName = suggestedCourseName) }

    fun loadForEdit(venueId: Long, courseId: Long, venueName: String, courseName: String, pars: List<Int>) {
        _draft.value = CourseDraft(
            editingVenueId = venueId, editingCourseId = courseId,
            venueName = venueName, courseName = courseName, pars = pars,
        )
    }

    fun setVenueName(v: String) = update { it.copy(venueName = v) }
    fun setCourseName(v: String) = update { it.copy(courseName = v) }

    fun addHole() = update { it.copy(pars = CourseWizardLogic.addHole(it.pars)) }
    fun deleteHole(index: Int) = update { it.copy(pars = CourseWizardLogic.deleteHole(it.pars, index)) }
    fun setPar(index: Int, value: Int) = update {
        val pars = it.pars.toMutableList()
        if (index in pars.indices) pars[index] = CourseWizardLogic.clampPar(value)
        it.copy(pars = pars)
    }

    fun next() = update { it.copy(step = (it.step + 1).coerceAtMost(CourseWizardLogic.MAX_STEP)) }
    fun prev() = update { it.copy(step = (it.step - 1).coerceAtLeast(0)) }
    fun goToStep(step: Int) = update { it.copy(step = step.coerceIn(0, CourseWizardLogic.MAX_STEP)) }

    fun isEditing(): Boolean = cur().editingCourseId != null
}
```

- [ ] **Step 6: Run unit tests** — `./gradlew :app:testDebugUnitTest --tests "com.parkgolf.score.ui.CourseWizardLogicTest" --tests "com.parkgolf.score.ui.CourseWizardViewModelTest"` → PASS.

- [ ] **Step 7: Commit**
```bash
git add app/src/main/java/com/parkgolf/score/ui/courses/CourseWizardLogic.kt app/src/main/java/com/parkgolf/score/ui/courses/CourseWizardViewModel.kt app/src/test/java/com/parkgolf/score/ui/CourseWizardLogicTest.kt app/src/test/java/com/parkgolf/score/ui/CourseWizardViewModelTest.kt
git commit -m "feat: rework wizard logic/VM for hole add-delete and duplicate validation"
```

Note: `CourseWizardFragment.kt` will not compile until Task 7 (it still calls `setHoleCount`/`holeCount`). That is expected — Task 6 adds layouts (no fragment change), Task 7 rewrites the fragment. Do NOT run `assembleDebug` at the end of Tasks 5–6; use `:app:testDebugUnitTest` and `:app:processDebugResources` respectively. `assembleDebug` returns green again at Task 7.

---

## Task 6: Wizard item layouts, step layouts, adapters

**Files:**
- Create item layouts, step layouts, and three adapters (list below).

- [ ] **Step 1: item_hole_card.xml**
```xml
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent" android:layout_height="wrap_content"
    android:orientation="horizontal" android:gravity="center_vertical"
    android:background="@drawable/bg_card" android:padding="12dp"
    android:layout_marginBottom="10dp" android:minHeight="@dimen/touch_min">
    <TextView android:id="@+id/tvHoleBadge"
        android:layout_width="40dp" android:layout_height="40dp"
        android:background="@drawable/bg_number_badge" android:gravity="center"
        android:textColor="@color/primary" android:textStyle="bold" android:textSize="@dimen/body_text" />
    <TextView android:id="@+id/tvHoleName"
        android:layout_width="0dp" android:layout_height="wrap_content" android:layout_weight="1"
        android:layout_marginStart="12dp" android:textColor="@color/foreground"
        android:textStyle="bold" android:textSize="@dimen/body_text" />
    <TextView android:layout_width="wrap_content" android:layout_height="wrap_content"
        android:text="파" android:textColor="@color/muted_foreground" android:layout_marginEnd="4dp" />
    <ImageButton android:id="@+id/btnParMinus"
        android:layout_width="40dp" android:layout_height="40dp"
        android:background="@drawable/bg_number_badge" android:src="@drawable/ic_minus"
        android:scaleType="center" android:contentDescription="@string/decrease" />
    <TextView android:id="@+id/tvPar"
        android:layout_width="32dp" android:layout_height="wrap_content" android:gravity="center"
        android:textColor="@color/foreground" android:textStyle="bold" android:textSize="@dimen/title_text" />
    <ImageButton android:id="@+id/btnParPlus"
        android:layout_width="40dp" android:layout_height="40dp"
        android:background="@drawable/bg_number_badge" android:src="@drawable/ic_plus"
        android:scaleType="center" android:contentDescription="@string/increase" />
    <ImageButton android:id="@+id/btnDeleteHole"
        android:layout_width="40dp" android:layout_height="40dp" android:layout_marginStart="4dp"
        android:background="?attr/selectableItemBackgroundBorderless" android:src="@drawable/ic_trash"
        android:scaleType="center" android:contentDescription="@string/delete" />
</LinearLayout>
```

- [ ] **Step 2: item_venue_suggestion.xml**
```xml
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent" android:layout_height="wrap_content"
    android:orientation="horizontal" android:gravity="center_vertical"
    android:background="@drawable/bg_card" android:padding="16dp"
    android:layout_marginBottom="8dp" android:minHeight="@dimen/touch_min"
    android:clickable="true" android:focusable="true">
    <ImageView android:layout_width="18dp" android:layout_height="18dp"
        android:src="@drawable/ic_map_pin" android:importantForAccessibility="no" />
    <TextView android:id="@+id/tvVenueName"
        android:layout_width="0dp" android:layout_height="wrap_content" android:layout_weight="1"
        android:layout_marginStart="12dp" android:textColor="@color/foreground"
        android:textStyle="bold" android:textSize="@dimen/body_text" />
</LinearLayout>
```

- [ ] **Step 3: item_par_preview.xml**
```xml
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="48dp" android:layout_height="48dp"
    android:orientation="vertical" android:gravity="center"
    android:background="@drawable/bg_number_badge" android:layout_margin="3dp">
    <TextView android:id="@+id/tvIndex" android:layout_width="wrap_content" android:layout_height="wrap_content"
        android:textSize="10sp" android:textColor="@color/muted_foreground" />
    <TextView android:id="@+id/tvPar" android:layout_width="wrap_content" android:layout_height="wrap_content"
        android:textStyle="bold" android:textColor="@color/foreground" android:textSize="@dimen/body_text" />
</LinearLayout>
```

- [ ] **Step 4: HoleCardAdapter.kt**
```kotlin
package com.parkgolf.score.ui.courses

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.parkgolf.score.R
import com.parkgolf.score.databinding.ItemHoleCardBinding

class HoleCardAdapter(
    private val onPar: (index: Int, newValue: Int) -> Unit,
    private val onDelete: (index: Int) -> Unit,
) : RecyclerView.Adapter<HoleCardAdapter.VH>() {
    private var pars: List<Int> = emptyList()

    @SuppressLint("NotifyDataSetChanged")
    fun submit(list: List<Int>) { pars = list; notifyDataSetChanged() }

    inner class VH(val b: ItemHoleCardBinding) : RecyclerView.ViewHolder(b.root)
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VH(ItemHoleCardBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    override fun getItemCount() = pars.size
    override fun onBindViewHolder(holder: VH, position: Int) {
        val ctx = holder.itemView.context
        holder.b.tvHoleBadge.text = (position + 1).toString()
        holder.b.tvHoleName.text = ctx.getString(R.string.hole_label, position + 1)
        holder.b.tvPar.text = pars[position].toString()
        holder.b.btnParMinus.setOnClickListener { onPar(position, pars[position] - 1) }
        holder.b.btnParPlus.setOnClickListener { onPar(position, pars[position] + 1) }
        holder.b.btnDeleteHole.isEnabled = pars.size > 1
        holder.b.btnDeleteHole.alpha = if (pars.size > 1) 1f else 0.3f
        holder.b.btnDeleteHole.setOnClickListener { onDelete(position) }
    }
}
```

- [ ] **Step 5: VenueSuggestionAdapter.kt**
```kotlin
package com.parkgolf.score.ui.courses

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.parkgolf.score.databinding.ItemVenueSuggestionBinding

class VenueSuggestionAdapter(private val onPick: (String) -> Unit) :
    RecyclerView.Adapter<VenueSuggestionAdapter.VH>() {
    private var names: List<String> = emptyList()

    @SuppressLint("NotifyDataSetChanged")
    fun submit(list: List<String>) { names = list; notifyDataSetChanged() }

    inner class VH(val b: ItemVenueSuggestionBinding) : RecyclerView.ViewHolder(b.root)
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VH(ItemVenueSuggestionBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    override fun getItemCount() = names.size
    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.b.tvVenueName.text = names[position]
        holder.b.root.setOnClickListener { onPick(names[position]) }
    }
}
```

- [ ] **Step 6: ParPreviewAdapter.kt**
```kotlin
package com.parkgolf.score.ui.courses

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.parkgolf.score.databinding.ItemParPreviewBinding

class ParPreviewAdapter : RecyclerView.Adapter<ParPreviewAdapter.VH>() {
    private var pars: List<Int> = emptyList()

    @SuppressLint("NotifyDataSetChanged")
    fun submit(list: List<Int>) { pars = list; notifyDataSetChanged() }

    inner class VH(val b: ItemParPreviewBinding) : RecyclerView.ViewHolder(b.root)
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VH(ItemParPreviewBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    override fun getItemCount() = pars.size
    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.b.tvIndex.text = (position + 1).toString()
        holder.b.tvPar.text = pars[position].toString()
    }
}
```

- [ ] **Step 7: Create the four step layouts + success layout.**

`view_wizard_step_venue.xml`
```xml
<?xml version="1.0" encoding="utf-8"?>
<ScrollView xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent" android:layout_height="match_parent"
    android:fillViewport="true" android:padding="20dp">
    <LinearLayout android:layout_width="match_parent" android:layout_height="wrap_content"
        android:orientation="vertical">
        <TextView android:layout_width="wrap_content" android:layout_height="wrap_content"
            android:text="@string/venue_name_label" android:textSize="@dimen/title_text"
            android:textStyle="bold" android:textColor="@color/foreground" />
        <TextView android:layout_width="wrap_content" android:layout_height="wrap_content"
            android:text="@string/venue_name_helper" android:textColor="@color/muted_foreground"
            android:layout_marginTop="4dp" android:layout_marginBottom="20dp" />
        <FrameLayout android:layout_width="match_parent" android:layout_height="wrap_content">
            <EditText android:id="@+id/etVenueName"
                android:layout_width="match_parent" android:layout_height="wrap_content"
                android:background="@drawable/bg_input" android:paddingHorizontal="20dp"
                android:paddingVertical="16dp" android:minHeight="@dimen/touch_min"
                android:hint="@string/venue_name_example" android:textSize="@dimen/body_text"
                android:inputType="text" android:textColor="@color/foreground" />
            <LinearLayout android:id="@+id/badgeExisting"
                android:layout_width="wrap_content" android:layout_height="wrap_content"
                android:layout_gravity="end|center_vertical" android:layout_marginEnd="16dp"
                android:orientation="horizontal" android:gravity="center_vertical" android:visibility="gone">
                <ImageView android:layout_width="16dp" android:layout_height="16dp"
                    android:src="@drawable/ic_check" android:importantForAccessibility="no"
                    app:tint="@color/primary" xmlns:app="http://schemas.android.com/apk/res-auto" />
                <TextView android:layout_width="wrap_content" android:layout_height="wrap_content"
                    android:layout_marginStart="4dp" android:text="@string/existing_venue"
                    android:textColor="@color/primary" android:textStyle="bold" android:textSize="14sp" />
            </LinearLayout>
        </FrameLayout>

        <TextView android:id="@+id/tvSuggestLabel"
            android:layout_width="wrap_content" android:layout_height="wrap_content"
            android:text="@string/saved_venues" android:textColor="@color/muted_foreground"
            android:textStyle="bold" android:layout_marginTop="20dp" android:visibility="gone" />
        <androidx.recyclerview.widget.RecyclerView android:id="@+id/rvSuggestions"
            android:layout_width="match_parent" android:layout_height="wrap_content"
            android:nestedScrollingEnabled="false" android:layout_marginTop="8dp" android:visibility="gone" />

        <LinearLayout android:id="@+id/bannerExisting"
            android:layout_width="match_parent" android:layout_height="wrap_content"
            android:orientation="horizontal" android:background="@drawable/bg_pill_primary"
            android:padding="16dp" android:layout_marginTop="20dp" android:visibility="gone">
            <ImageView android:layout_width="20dp" android:layout_height="20dp"
                android:src="@drawable/ic_alert_circle" android:importantForAccessibility="no"
                app:tint="@color/primary" xmlns:app="http://schemas.android.com/apk/res-auto" />
            <TextView android:layout_width="0dp" android:layout_height="wrap_content" android:layout_weight="1"
                android:layout_marginStart="12dp" android:text="@string/existing_venue_banner"
                android:textColor="@color/primary" />
        </LinearLayout>
    </LinearLayout>
</ScrollView>
```

`view_wizard_step_course.xml`
```xml
<?xml version="1.0" encoding="utf-8"?>
<ScrollView xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent" android:layout_height="match_parent"
    android:fillViewport="true" android:padding="20dp">
    <LinearLayout android:layout_width="match_parent" android:layout_height="wrap_content"
        android:orientation="vertical">
        <TextView android:id="@+id/chipVenue"
            android:layout_width="wrap_content" android:layout_height="wrap_content"
            android:background="@drawable/bg_pill_primary" android:paddingHorizontal="12dp"
            android:paddingVertical="6dp" android:textColor="@color/primary" android:textStyle="bold"
            android:textSize="14sp" android:layout_marginBottom="12dp" />
        <TextView android:layout_width="wrap_content" android:layout_height="wrap_content"
            android:text="@string/course_name_label" android:textSize="@dimen/title_text"
            android:textStyle="bold" android:textColor="@color/foreground" />
        <TextView android:layout_width="wrap_content" android:layout_height="wrap_content"
            android:text="@string/course_name_helper" android:textColor="@color/muted_foreground"
            android:layout_marginTop="4dp" android:layout_marginBottom="20dp" />
        <EditText android:id="@+id/etCourseName"
            android:layout_width="match_parent" android:layout_height="wrap_content"
            android:background="@drawable/bg_input" android:paddingHorizontal="20dp"
            android:paddingVertical="16dp" android:minHeight="@dimen/touch_min"
            android:hint="@string/course_name_example" android:textSize="@dimen/body_text"
            android:inputType="text" android:textColor="@color/foreground" />
        <LinearLayout android:id="@+id/errorDuplicate"
            android:layout_width="wrap_content" android:layout_height="wrap_content"
            android:orientation="horizontal" android:gravity="center_vertical"
            android:layout_marginTop="10dp" android:visibility="gone">
            <ImageView android:layout_width="17dp" android:layout_height="17dp"
                android:src="@drawable/ic_alert_circle" android:importantForAccessibility="no" />
            <TextView android:layout_width="wrap_content" android:layout_height="wrap_content"
                android:layout_marginStart="8dp" android:text="@string/course_name_duplicate"
                android:textColor="@color/destructive" android:textStyle="bold" />
        </LinearLayout>
        <TextView android:id="@+id/tvExistingLabel"
            android:layout_width="wrap_content" android:layout_height="wrap_content"
            android:text="@string/existing_courses_label" android:textColor="@color/muted_foreground"
            android:textStyle="bold" android:layout_marginTop="20dp" android:visibility="gone" />
        <com.google.android.flexbox.FlexboxLayout android:id="@+id/flexExistingCourses"
            android:layout_width="match_parent" android:layout_height="wrap_content"
            android:layout_marginTop="8dp"
            xmlns:app="http://schemas.android.com/apk/res-auto"
            app:flexWrap="wrap" />
    </LinearLayout>
</ScrollView>
```
> Uses `com.google.android.flexbox.FlexboxLayout`. Add the dependency in Step 8 before building.

`view_wizard_step_holes.xml`
```xml
<?xml version="1.0" encoding="utf-8"?>
<androidx.core.widget.NestedScrollView xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent" android:layout_height="match_parent"
    android:fillViewport="true" android:padding="20dp">
    <LinearLayout android:layout_width="match_parent" android:layout_height="wrap_content"
        android:orientation="vertical">
        <LinearLayout android:layout_width="wrap_content" android:layout_height="wrap_content"
            android:orientation="horizontal" android:gravity="center_vertical" android:layout_marginBottom="12dp">
            <TextView android:id="@+id/chipVenue"
                android:layout_width="wrap_content" android:layout_height="wrap_content"
                android:background="@drawable/bg_pill_primary" android:paddingHorizontal="12dp"
                android:paddingVertical="6dp" android:textColor="@color/primary" android:textStyle="bold" android:textSize="14sp" />
            <TextView android:layout_width="wrap_content" android:layout_height="wrap_content"
                android:text=" / " android:textColor="@color/muted_foreground" />
            <TextView android:id="@+id/chipCourse"
                android:layout_width="wrap_content" android:layout_height="wrap_content"
                android:background="@drawable/bg_pill_secondary" android:paddingHorizontal="12dp"
                android:paddingVertical="6dp" android:textColor="@color/foreground" android:textStyle="bold" android:textSize="14sp" />
        </LinearLayout>
        <TextView android:layout_width="wrap_content" android:layout_height="wrap_content"
            android:text="@string/hole_setup_label" android:textSize="@dimen/title_text"
            android:textStyle="bold" android:textColor="@color/foreground" />
        <TextView android:layout_width="wrap_content" android:layout_height="wrap_content"
            android:text="@string/hole_setup_helper" android:textColor="@color/muted_foreground"
            android:layout_marginTop="4dp" android:layout_marginBottom="16dp" />

        <LinearLayout android:layout_width="match_parent" android:layout_height="wrap_content"
            android:orientation="horizontal" android:background="@drawable/bg_card" android:padding="16dp"
            android:gravity="center_vertical" android:layout_marginBottom="16dp">
            <TextView android:layout_width="wrap_content" android:layout_height="wrap_content"
                android:text="@string/total_holes_label" android:textColor="@color/muted_foreground" />
            <TextView android:id="@+id/tvTotalHoles"
                android:layout_width="0dp" android:layout_height="wrap_content" android:layout_weight="1"
                android:layout_marginStart="8dp" android:textColor="@color/foreground"
                android:textStyle="bold" android:textSize="@dimen/title_text" />
            <TextView android:layout_width="wrap_content" android:layout_height="wrap_content"
                android:text="@string/total_par_label" android:textColor="@color/muted_foreground" />
            <TextView android:id="@+id/tvTotalPar"
                android:layout_width="wrap_content" android:layout_height="wrap_content"
                android:layout_marginStart="8dp" android:textColor="@color/primary"
                android:textStyle="bold" android:textSize="@dimen/title_text" />
        </LinearLayout>

        <androidx.recyclerview.widget.RecyclerView android:id="@+id/rvHoles"
            android:layout_width="match_parent" android:layout_height="wrap_content"
            android:nestedScrollingEnabled="false" />

        <TextView android:id="@+id/btnAddHole"
            android:layout_width="match_parent" android:layout_height="wrap_content"
            android:background="@drawable/bg_dashed" android:gravity="center" android:padding="16dp"
            android:text="@string/add_hole" android:textColor="@color/primary" android:textStyle="bold"
            android:drawablePadding="8dp" android:minHeight="@dimen/touch_min"
            android:clickable="true" android:focusable="true" android:layout_marginTop="4dp" />
    </LinearLayout>
</androidx.core.widget.NestedScrollView>
```

`view_wizard_step_save.xml`
```xml
<?xml version="1.0" encoding="utf-8"?>
<ScrollView xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:layout_width="match_parent" android:layout_height="match_parent"
    android:fillViewport="true" android:padding="20dp">
    <LinearLayout android:layout_width="match_parent" android:layout_height="wrap_content"
        android:orientation="vertical">
        <TextView android:layout_width="wrap_content" android:layout_height="wrap_content"
            android:text="@string/save_confirm_title" android:textSize="@dimen/title_text"
            android:textStyle="bold" android:textColor="@color/foreground" />
        <TextView android:layout_width="wrap_content" android:layout_height="wrap_content"
            android:text="@string/save_confirm_helper" android:textColor="@color/muted_foreground"
            android:layout_marginTop="4dp" android:layout_marginBottom="20dp" />
        <LinearLayout android:layout_width="match_parent" android:layout_height="wrap_content"
            android:orientation="vertical" android:background="@drawable/bg_card" android:padding="20dp">
            <TextView android:id="@+id/tvSumVenue" android:layout_width="match_parent" android:layout_height="wrap_content"
                android:textColor="@color/foreground" android:textSize="@dimen/body_text" android:layout_marginBottom="10dp" />
            <TextView android:id="@+id/tvSumCourse" android:layout_width="match_parent" android:layout_height="wrap_content"
                android:textColor="@color/foreground" android:textSize="@dimen/body_text" android:layout_marginBottom="10dp" />
            <TextView android:id="@+id/tvSumHoles" android:layout_width="match_parent" android:layout_height="wrap_content"
                android:textColor="@color/foreground" android:textSize="@dimen/body_text" android:layout_marginBottom="10dp" />
            <TextView android:id="@+id/tvSumTotalPar" android:layout_width="match_parent" android:layout_height="wrap_content"
                android:textColor="@color/primary" android:textStyle="bold" android:textSize="@dimen/body_text" />
        </LinearLayout>
        <TextView android:layout_width="wrap_content" android:layout_height="wrap_content"
            android:text="@string/par_preview_label" android:textColor="@color/muted_foreground"
            android:textStyle="bold" android:layout_marginTop="20dp" android:layout_marginBottom="8dp" />
        <androidx.recyclerview.widget.RecyclerView android:id="@+id/rvParPreview"
            android:layout_width="match_parent" android:layout_height="wrap_content"
            android:nestedScrollingEnabled="false" />
    </LinearLayout>
</ScrollView>
```

`view_wizard_success.xml`
```xml
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:layout_width="match_parent" android:layout_height="match_parent"
    android:orientation="vertical" android:gravity="center" android:padding="24dp">
    <FrameLayout android:layout_width="96dp" android:layout_height="96dp"
        android:background="@drawable/bg_success_circle">
        <ImageView android:layout_width="44dp" android:layout_height="44dp" android:layout_gravity="center"
            android:src="@drawable/ic_check" android:importantForAccessibility="no" app:tint="@color/primary" />
    </FrameLayout>
    <TextView android:layout_width="wrap_content" android:layout_height="wrap_content"
        android:text="@string/save_done_title" android:textSize="@dimen/score_text" android:textStyle="bold"
        android:textColor="@color/foreground" android:layout_marginTop="24dp" />
    <TextView android:id="@+id/tvSavedInfo" android:layout_width="wrap_content" android:layout_height="wrap_content"
        android:textColor="@color/muted_foreground" android:textSize="@dimen/body_text" android:gravity="center"
        android:layout_marginTop="8dp" />
    <Button android:id="@+id/btnNewTemplate" style="@style/Widget.App.Button.Primary"
        android:layout_width="match_parent" android:layout_height="wrap_content"
        android:text="@string/new_template" android:layout_marginTop="32dp" />
    <Button android:id="@+id/btnToList" style="@style/Widget.App.Button.Outline"
        android:layout_width="match_parent" android:layout_height="wrap_content"
        android:text="@string/to_course_list" android:layout_marginTop="12dp" />
</LinearLayout>
```

- [ ] **Step 8: Add the flexbox dependency** — in `app/build.gradle.kts` dependencies block add:
```kotlin
    implementation("com.google.android.flexbox:flexbox:3.0.0")
```

- [ ] **Step 9: Add strings** — append inside `<resources>` in `app/src/main/res/values/strings.xml`:
```xml
    <string name="wizard_full_title">코스 템플릿 작성</string>
    <string name="existing_venue">기존 구장</string>
    <string name="saved_venues">저장된 구장</string>
    <string name="existing_venue_banner">이미 저장된 구장입니다. 코스를 추가하면 해당 구장에 통합됩니다.</string>
    <string name="course_name_helper">같은 구장 내 중복된 코스명은 사용할 수 없습니다</string>
    <string name="course_name_example">예: A코스, 챔피언코스</string>
    <string name="course_name_duplicate">이미 존재하는 코스명입니다</string>
    <string name="existing_courses_label">이 구장의 기존 코스</string>
    <string name="hole_setup_helper">홀별 파를 조정하고 홀을 추가/삭제하세요</string>
    <string name="total_holes_label">총 홀 수</string>
    <string name="total_par_label">총 파</string>
    <string name="add_hole">＋ 홀 추가</string>
    <string name="save_confirm_title">저장 확인</string>
    <string name="save_confirm_helper">아래 내용으로 템플릿을 저장합니다</string>
    <string name="par_preview_label">홀 파 미리보기</string>
    <string name="save_done_title">저장 완료!</string>
    <string name="saved_info">%1$s의 %2$s이 저장되었습니다</string>
    <string name="new_template">새 템플릿 작성</string>
    <string name="to_course_list">목록으로</string>
    <string name="hole_setup_label">홀 설정</string>
    <string name="save_now">저장하기</string>
```
> If any of these names already exist (e.g. `hole_setup_label` from the prior spec), keep a single definition — remove the duplicate so the build doesn't fail on redefinition.

- [ ] **Step 10: Build resources + compile** — `./gradlew :app:processDebugResources :app:compileDebugKotlin` → BUILD SUCCESSFUL (adapters compile; ViewBinding classes generated). Note `CourseWizardFragment.kt` may still fail to compile here because it references the OLD binding ids; if so, that failure is resolved in Task 7 — run only `:app:processDebugResources` in that case and defer full compile to Task 7.

- [ ] **Step 11: Commit**
```bash
git add app/src/main/res/layout/item_hole_card.xml app/src/main/res/layout/item_venue_suggestion.xml app/src/main/res/layout/item_par_preview.xml app/src/main/res/layout/view_wizard_step_*.xml app/src/main/res/layout/view_wizard_success.xml app/src/main/java/com/parkgolf/score/ui/courses/HoleCardAdapter.kt app/src/main/java/com/parkgolf/score/ui/courses/VenueSuggestionAdapter.kt app/src/main/java/com/parkgolf/score/ui/courses/ParPreviewAdapter.kt app/src/main/res/values/strings.xml app/build.gradle.kts
git commit -m "feat: add wizard step layouts, item layouts, adapters, and strings"
```

---

## Task 7: Wizard fragment layout + fragment rewrite

**Files:**
- Modify: `app/src/main/res/layout/fragment_course_wizard.xml`
- Modify: `app/src/main/java/com/parkgolf/score/ui/courses/CourseWizardFragment.kt`
- Delete: `app/src/main/res/layout/view_wizard_steps.xml`, `app/src/main/java/com/parkgolf/score/ui/courses/WizardParAdapter.kt`

- [ ] **Step 1: Replace fragment_course_wizard.xml** — top bar + custom step indicator + ViewFlipper(5) + bottom buttons:

```xml
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent" android:layout_height="match_parent"
    android:orientation="vertical" android:background="@color/background">

    <include android:id="@+id/topBar" layout="@layout/view_top_bar" />

    <LinearLayout android:id="@+id/stepBar"
        android:layout_width="match_parent" android:layout_height="wrap_content"
        android:orientation="horizontal" android:gravity="center"
        android:paddingHorizontal="16dp" android:paddingVertical="12dp">
        <!-- Four step cells; each: circle (FrameLayout w/ icon + check) + label. Connectors between. -->
        <LinearLayout android:id="@+id/step0" android:layout_width="wrap_content" android:layout_height="wrap_content"
            android:orientation="vertical" android:gravity="center">
            <FrameLayout android:layout_width="48dp" android:layout_height="48dp">
                <ImageView android:id="@+id/stepIcon0" android:layout_width="20dp" android:layout_height="20dp"
                    android:layout_gravity="center" android:src="@drawable/ic_map_pin" android:importantForAccessibility="no" />
                <ImageView android:id="@+id/stepCheck0" android:layout_width="20dp" android:layout_height="20dp"
                    android:layout_gravity="center" android:src="@drawable/ic_check" android:visibility="gone"
                    android:importantForAccessibility="no" xmlns:app="http://schemas.android.com/apk/res-auto" app:tint="@color/on_primary" />
            </FrameLayout>
            <TextView android:id="@+id/stepLabel0" android:layout_width="wrap_content" android:layout_height="wrap_content"
                android:text="@string/step_venue" android:textSize="12sp" android:textStyle="bold" android:layout_marginTop="4dp" />
        </LinearLayout>
        <View android:id="@+id/conn0" android:layout_width="24dp" android:layout_height="2dp"
            android:layout_marginBottom="18dp" android:background="@color/border" />
        <LinearLayout android:id="@+id/step1" android:layout_width="wrap_content" android:layout_height="wrap_content"
            android:orientation="vertical" android:gravity="center">
            <FrameLayout android:layout_width="48dp" android:layout_height="48dp">
                <ImageView android:id="@+id/stepIcon1" android:layout_width="20dp" android:layout_height="20dp"
                    android:layout_gravity="center" android:src="@drawable/ic_flag" android:importantForAccessibility="no" />
                <ImageView android:id="@+id/stepCheck1" android:layout_width="20dp" android:layout_height="20dp"
                    android:layout_gravity="center" android:src="@drawable/ic_check" android:visibility="gone"
                    android:importantForAccessibility="no" xmlns:app="http://schemas.android.com/apk/res-auto" app:tint="@color/on_primary" />
            </FrameLayout>
            <TextView android:id="@+id/stepLabel1" android:layout_width="wrap_content" android:layout_height="wrap_content"
                android:text="@string/step_course" android:textSize="12sp" android:textStyle="bold" android:layout_marginTop="4dp" />
        </LinearLayout>
        <View android:id="@+id/conn1" android:layout_width="24dp" android:layout_height="2dp"
            android:layout_marginBottom="18dp" android:background="@color/border" />
        <LinearLayout android:id="@+id/step2" android:layout_width="wrap_content" android:layout_height="wrap_content"
            android:orientation="vertical" android:gravity="center">
            <FrameLayout android:layout_width="48dp" android:layout_height="48dp">
                <ImageView android:id="@+id/stepIcon2" android:layout_width="20dp" android:layout_height="20dp"
                    android:layout_gravity="center" android:src="@drawable/ic_list_ordered" android:importantForAccessibility="no" />
                <ImageView android:id="@+id/stepCheck2" android:layout_width="20dp" android:layout_height="20dp"
                    android:layout_gravity="center" android:src="@drawable/ic_check" android:visibility="gone"
                    android:importantForAccessibility="no" xmlns:app="http://schemas.android.com/apk/res-auto" app:tint="@color/on_primary" />
            </FrameLayout>
            <TextView android:id="@+id/stepLabel2" android:layout_width="wrap_content" android:layout_height="wrap_content"
                android:text="@string/step_holes" android:textSize="12sp" android:textStyle="bold" android:layout_marginTop="4dp" />
        </LinearLayout>
        <View android:id="@+id/conn2" android:layout_width="24dp" android:layout_height="2dp"
            android:layout_marginBottom="18dp" android:background="@color/border" />
        <LinearLayout android:id="@+id/step3" android:layout_width="wrap_content" android:layout_height="wrap_content"
            android:orientation="vertical" android:gravity="center">
            <FrameLayout android:layout_width="48dp" android:layout_height="48dp">
                <ImageView android:id="@+id/stepIcon3" android:layout_width="20dp" android:layout_height="20dp"
                    android:layout_gravity="center" android:src="@drawable/ic_save" android:importantForAccessibility="no" />
                <ImageView android:id="@+id/stepCheck3" android:layout_width="20dp" android:layout_height="20dp"
                    android:layout_gravity="center" android:src="@drawable/ic_check" android:visibility="gone"
                    android:importantForAccessibility="no" xmlns:app="http://schemas.android.com/apk/res-auto" app:tint="@color/on_primary" />
            </FrameLayout>
            <TextView android:id="@+id/stepLabel3" android:layout_width="wrap_content" android:layout_height="wrap_content"
                android:text="@string/step_save" android:textSize="12sp" android:textStyle="bold" android:layout_marginTop="4dp" />
        </LinearLayout>
    </LinearLayout>

    <View android:layout_width="match_parent" android:layout_height="@dimen/border_width" android:background="@color/border" />

    <ViewFlipper android:id="@+id/flipper"
        android:layout_width="match_parent" android:layout_height="0dp" android:layout_weight="1">
        <include android:id="@+id/stepVenue" layout="@layout/view_wizard_step_venue" />
        <include android:id="@+id/stepCourse" layout="@layout/view_wizard_step_course" />
        <include android:id="@+id/stepHoles" layout="@layout/view_wizard_step_holes" />
        <include android:id="@+id/stepSave" layout="@layout/view_wizard_step_save" />
        <include android:id="@+id/stepSuccess" layout="@layout/view_wizard_success" />
    </ViewFlipper>

    <LinearLayout android:id="@+id/bottomBar"
        android:layout_width="match_parent" android:layout_height="wrap_content"
        android:orientation="horizontal" android:padding="20dp">
        <Button android:id="@+id/btnPrev" style="@style/Widget.App.Button.Outline"
            android:layout_width="0dp" android:layout_height="wrap_content" android:layout_weight="1"
            android:text="@string/wizard_prev" />
        <Button android:id="@+id/btnNext" style="@style/Widget.App.Button.Primary"
            android:layout_width="0dp" android:layout_height="wrap_content" android:layout_weight="2"
            android:layout_marginStart="12dp" android:text="@string/wizard_next" />
    </LinearLayout>
</LinearLayout>
```
> ViewBinding note: the step-indicator views (`stepBar`, `step0..3`, `stepIcon0..3`, `stepCheck0..3`, `stepLabel0..3`) live directly in this layout, so they are **flat** fields — access as `binding.step0`, `binding.stepIcon0`, etc. The five `<include>`s carry ids (`stepVenue`, `stepCourse`, `stepHoles`, `stepSave`, `stepSuccess`), so their contents are **nested** — access as `binding.stepVenue.etVenueName`, `binding.stepHoles.rvHoles`, etc. The fragment in Step 2 already uses exactly these paths.

- [ ] **Step 2: Replace CourseWizardFragment.kt**:

```kotlin
package com.parkgolf.score.ui.courses

import android.os.Bundle
import android.view.View
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
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
    private var venues: List<VenueEntity> = emptyList()
    private var existingCourseNames: List<String> = emptyList()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val binding = FragmentCourseWizardBinding.bind(view)
        val repo = App.repo(requireActivity().application)

        val holeAdapter = HoleCardAdapter(
            onPar = { i, v -> vm.setPar(i, v) },
            onDelete = { i -> vm.deleteHole(i) },
        )
        binding.stepHoles.rvHoles.layoutManager = LinearLayoutManager(requireContext())
        binding.stepHoles.rvHoles.adapter = holeAdapter

        val suggestAdapter = VenueSuggestionAdapter { picked -> vm.setVenueName(picked) }
        binding.stepVenue.rvSuggestions.layoutManager = LinearLayoutManager(requireContext())
        binding.stepVenue.rvSuggestions.adapter = suggestAdapter

        val previewAdapter = ParPreviewAdapter()
        binding.stepSave.rvParPreview.layoutManager =
            androidx.recyclerview.widget.GridLayoutManager(requireContext(), 6)
        binding.stepSave.rvParPreview.adapter = previewAdapter

        // Load venues (for existing-venue detection + suggestions).
        viewLifecycleOwner.lifecycleScope.launch {
            repo.observeVenues().collect { venues = it; renderStep1(binding, suggestAdapter) }
        }

        // Initial create/edit.
        if (savedInstanceState == null) {
            val handle = findNavController().previousBackStackEntry?.savedStateHandle
            val venueId = handle?.get<Long>("wizardVenueId") ?: 0L
            val courseId = handle?.get<Long>("wizardCourseId") ?: 0L
            if (venueId == 0L || courseId == 0L) vm.initNew("A코스")
            else viewLifecycleOwner.lifecycleScope.launch {
                val venue = repo.observeVenues().first().firstOrNull { it.id == venueId }
                val course = repo.coursesForVenue(venueId).firstOrNull { it.id == courseId }
                if (venue != null && course != null)
                    vm.loadForEdit(venue.id, course.id, venue.name, course.name, course.pars)
                else vm.initNew("A코스")
            }
        }

        var settingText = false
        binding.stepVenue.etVenueName.doAfterTextChanged {
            if (!settingText) vm.setVenueName(it?.toString() ?: "")
        }
        binding.stepCourse.etCourseName.doAfterTextChanged {
            if (!settingText) vm.setCourseName(it?.toString() ?: "")
        }
        binding.stepHoles.btnAddHole.setOnClickListener { vm.addHole() }

        binding.btnPrev.setOnClickListener {
            if (vm.draft.value?.step == 0) attemptExit() else vm.prev()
        }
        binding.btnNext.setOnClickListener { onNext(binding, repo) }

        binding.step0.setOnClickListener { vm.goToStep(0) }
        binding.step1.setOnClickListener { vm.goToStep(1) }
        binding.step2.setOnClickListener { vm.goToStep(2) }
        binding.step3.setOnClickListener { vm.goToStep(3) }

        binding.topBar.btnBack.setOnClickListener {
            if (vm.draft.value?.step == 0) attemptExit() else vm.prev()
        }
        onBackPressed { if (vm.draft.value?.step == 0) attemptExit() else vm.prev() }

        binding.stepSuccess.btnNewTemplate.setOnClickListener {
            vm.initNew("A코스"); showSuccess(binding, false)
        }
        binding.stepSuccess.btnToList.setOnClickListener { findNavController().popBackStack() }

        vm.draft.observe(viewLifecycleOwner) { draft ->
            settingText = true
            if (binding.stepVenue.etVenueName.text.toString() != draft.venueName)
                binding.stepVenue.etVenueName.setText(draft.venueName)
            if (binding.stepCourse.etCourseName.text.toString() != draft.courseName)
                binding.stepCourse.etCourseName.setText(draft.courseName)
            settingText = false

            binding.topBar.tvBarTitle.text = getString(
                if (vm.isEditing()) R.string.wizard_title_edit else R.string.wizard_full_title
            )
            binding.flipper.displayedChild = draft.step
            styleSteps(binding, draft.step)

            // Load existing courses of the (matched) venue for duplicate check + chips.
            refreshExistingCourses(binding, repo, draft.venueName)

            renderStep1(binding, suggestAdapter)
            renderStep2(binding)
            renderStep3(binding, holeAdapter)
            renderStep4(binding, previewAdapter)

            binding.btnNext.text = when (draft.step) {
                CourseWizardLogic.MAX_STEP -> getString(R.string.save_now)
                else -> getString(R.string.wizard_next)
            }
            binding.btnPrev.text =
                if (draft.step == 0) getString(R.string.cancel) else getString(R.string.wizard_prev)
            val enabled = isStepValid(draft.step, draft)
            binding.btnNext.isEnabled = enabled
            binding.btnNext.alpha = if (enabled) 1f else 0.3f
        }
    }

    private fun isStepValid(step: Int, draft: CourseDraft): Boolean = when (step) {
        0 -> CourseWizardLogic.venueStepValid(draft)
        1 -> CourseWizardLogic.courseStepValid(draft.courseName, existingCourseNames)
        2 -> CourseWizardLogic.holeStepValid(draft.pars)
        else -> true
    }

    private fun onNext(binding: FragmentCourseWizardBinding, repo: ParkGolfRepository) {
        val d = vm.draft.value ?: return
        if (!isStepValid(d.step, d)) return
        if (d.step == CourseWizardLogic.MAX_STEP) save(binding, repo)
        else {
            if (d.step == 0 && !vm.isEditing()) maybeSuggestCourseName()
            vm.next()
        }
    }

    private fun refreshExistingCourses(
        binding: FragmentCourseWizardBinding, repo: ParkGolfRepository, venueName: String,
    ) {
        val vid = CourseWizardLogic.resolveVenueId(venueName, venues, vm.draft.value?.editingVenueId)
        if (vid == null) { existingCourseNames = emptyList(); return }
        viewLifecycleOwner.lifecycleScope.launch {
            val editingId = vm.draft.value?.editingCourseId
            existingCourseNames = repo.coursesForVenue(vid)
                .filter { it.id != editingId }.map { it.name }
        }
    }

    private fun maybeSuggestCourseName() {
        vm.setCourseName(CourseWizardLogic.suggestCourseName(existingCourseNames))
    }

    private fun renderStep1(binding: FragmentCourseWizardBinding, adapter: VenueSuggestionAdapter) {
        val name = vm.draft.value?.venueName ?: ""
        val existing = CourseWizardLogic.isExistingVenue(name, venues)
        val matches = CourseWizardLogic.matchedVenues(name, venues)
        binding.stepVenue.badgeExisting.isVisible = existing
        binding.stepVenue.bannerExisting.isVisible = existing
        val showSuggest = matches.isNotEmpty() && !existing
        binding.stepVenue.tvSuggestLabel.isVisible = showSuggest
        binding.stepVenue.rvSuggestions.isVisible = showSuggest
        adapter.submit(matches)
    }

    private fun renderStep2(binding: FragmentCourseWizardBinding) {
        val d = vm.draft.value ?: return
        binding.stepCourse.chipVenue.text = d.venueName
        val dup = CourseWizardLogic.isDuplicateCourseName(d.courseName, existingCourseNames)
        binding.stepCourse.errorDuplicate.isVisible = dup
        binding.stepCourse.etCourseName.setBackgroundResource(
            if (dup) R.drawable.bg_input_error else R.drawable.bg_input
        )
        binding.stepCourse.tvExistingLabel.isVisible = existingCourseNames.isNotEmpty()
        val flex = binding.stepCourse.flexExistingCourses
        flex.removeAllViews()
        existingCourseNames.forEach { c ->
            val tv = layoutInflater.inflate(R.layout.item_course_chip, flex, false) as android.widget.TextView
            tv.text = c
            flex.addView(tv)
        }
    }

    private fun renderStep3(binding: FragmentCourseWizardBinding, adapter: HoleCardAdapter) {
        val d = vm.draft.value ?: return
        binding.stepHoles.chipVenue.text = d.venueName
        binding.stepHoles.chipCourse.text = d.courseName
        binding.stepHoles.tvTotalHoles.text = getString(R.string.total_holes, d.pars.size)
        binding.stepHoles.tvTotalPar.text = d.pars.sum().toString()
        adapter.submit(d.pars)
    }

    private fun renderStep4(binding: FragmentCourseWizardBinding, adapter: ParPreviewAdapter) {
        val d = vm.draft.value ?: return
        binding.stepSave.tvSumVenue.text = getString(R.string.save_summary_venue, d.venueName)
        binding.stepSave.tvSumCourse.text = getString(R.string.save_summary_course, d.courseName, d.pars.size)
        binding.stepSave.tvSumHoles.text = getString(R.string.total_holes, d.pars.size)
        binding.stepSave.tvSumTotalPar.text = getString(R.string.total_par_value, d.pars.sum())
        adapter.submit(d.pars)
    }

    private fun styleSteps(binding: FragmentCourseWizardBinding, step: Int) {
        val icons = listOf(binding.stepIcon0, binding.stepIcon1, binding.stepIcon2, binding.stepIcon3)
        val checks = listOf(binding.stepCheck0, binding.stepCheck1, binding.stepCheck2, binding.stepCheck3)
        val labels = listOf(binding.stepLabel0, binding.stepLabel1, binding.stepLabel2, binding.stepLabel3)
        for (i in 0..3) {
            val done = step > i
            val active = step == i
            val circle = (icons[i].parent as View)
            circle.setBackgroundResource(
                when { done -> R.drawable.bg_step_done; active -> R.drawable.bg_step_active; else -> R.drawable.bg_step_inactive }
            )
            checks[i].isVisible = done
            icons[i].isVisible = !done
            labels[i].setTextColor(
                ContextCompat.getColor(requireContext(), if (active || done) R.color.primary else R.color.muted_foreground)
            )
        }
    }

    private fun attemptExit() {
        val titleRes = if (vm.isEditing()) R.string.confirm_cancel_edit else R.string.confirm_cancel_create
        confirmYesNo(titleRes) { findNavController().popBackStack() }
    }

    private fun showSuccess(binding: FragmentCourseWizardBinding, saved: Boolean) {
        if (saved) {
            val d = vm.draft.value ?: return
            binding.stepSuccess.tvSavedInfo.text = getString(R.string.saved_info, d.venueName, d.courseName)
            binding.flipper.displayedChild = 4
            binding.stepBar.visibility = View.GONE
            binding.bottomBar.visibility = View.GONE
        } else {
            binding.flipper.displayedChild = 0
            binding.stepBar.visibility = View.VISIBLE
            binding.bottomBar.visibility = View.VISIBLE
        }
    }

    private fun save(binding: FragmentCourseWizardBinding, repo: ParkGolfRepository) {
        val draft = vm.draft.value ?: return
        viewLifecycleOwner.lifecycleScope.launch {
            val allVenues = repo.observeVenues().first()
            val resolved = CourseWizardLogic.resolveVenueId(draft.venueName, allVenues, draft.editingVenueId)
            val venueId = resolved ?: repo.upsertVenue(VenueEntity(name = draft.venueName.trim()))
            if (draft.editingVenueId != null)
                repo.upsertVenue(VenueEntity(id = venueId, name = draft.venueName.trim()))
            repo.upsertCourse(
                CourseEntity(
                    id = draft.editingCourseId ?: 0L, venueId = venueId,
                    name = draft.courseName.trim(), pars = draft.pars,
                )
            )
            binding.stepSuccess.tvSavedInfo.text =
                getString(R.string.saved_info, draft.venueName, draft.courseName)
            binding.flipper.displayedChild = 4
            binding.stepBar.visibility = View.GONE
            binding.bottomBar.visibility = View.GONE
        }
    }
}
```

- [ ] **Step 3: Add remaining strings & the course chip item.** Append to `strings.xml`:
```xml
    <string name="total_par_value">총 파 %1$d</string>
```
Create `app/src/main/res/layout/item_course_chip.xml`:
```xml
<?xml version="1.0" encoding="utf-8"?>
<TextView xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="wrap_content" android:layout_height="wrap_content"
    android:background="@drawable/bg_card" android:paddingHorizontal="16dp" android:paddingVertical="8dp"
    android:layout_margin="4dp" android:textColor="@color/muted_foreground" android:textStyle="bold" />
```
(The `total_holes` and `save_summary_venue`/`save_summary_course` strings already exist from the prior wizard; reuse them.)

- [ ] **Step 4: Delete the obsolete step layout + adapter**
```bash
git rm app/src/main/res/layout/view_wizard_steps.xml app/src/main/java/com/parkgolf/score/ui/courses/WizardParAdapter.kt
```

- [ ] **Step 5: Build** — `./gradlew :app:assembleDebug` → BUILD SUCCESSFUL. Also `grep -rn "WizardParAdapter\|view_wizard_steps\|holeCount\|setHoleCount" app/src` → no matches.

- [ ] **Step 6: Commit**
```bash
git add -A
git commit -m "feat: rebuild course wizard UI with card steps, hole add/delete, and success screen"
```

---

## Task 8: Top bar on all non-home screens + recolor verification

**Files:** each non-home fragment layout + fragment (add top bar include, wire title/back). Screens: `start`, `course_setup`, `player_setup`, `grid_view`, `round_summary`, `history`, `round_detail`, `my_courses`. (`hole_input` already has the bar; `course_wizard` handled in Task 7.)

- [ ] **Step 1: Add top-bar strings.** Append to `strings.xml`:
```xml
    <string name="title_start">라운드 시작</string>
    <string name="title_course_setup">코스 선택</string>
    <string name="title_player_setup">함께 하는 사람</string>
    <string name="title_grid">전체 보기</string>
    <string name="title_summary">라운드 완료</string>
    <string name="title_history">지난 기록</string>
    <string name="title_round_detail">기록 상세</string>
    <string name="title_my_courses">내 구장 관리</string>
```

- [ ] **Step 2: For EACH of the eight layouts**, wrap the existing root in a vertical `LinearLayout` whose first child is `<include android:id="@+id/topBar" layout="@layout/view_top_bar" />`, exactly as was done for `fragment_hole_input.xml` (root becomes `LinearLayout`, original root becomes the weighted second child with `layout_height="0dp"` + `layout_weight="1"`; move the `xmlns:android` to the new root). Do this one file at a time and build after each to catch nesting errors early.

Concretely, for a file whose current root is `<X ...>...</X>`, transform to:
```xml
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent" android:layout_height="match_parent"
    android:orientation="vertical">
    <include android:id="@+id/topBar" layout="@layout/view_top_bar" />
    <X ... android:layout_height="0dp" ... [add] android:layout_weight="1">
        ...original children...
    </X>
</LinearLayout>
```
(Keep any `xmlns:app`/`xmlns:tools` on the inner element or move to root as needed; remove `xmlns:android` from the inner element.)

- [ ] **Step 3: For EACH of the eight fragments**, in `onViewCreated` after binding, set the title and wire back to a simple pop (these screens have no unsaved-work confirm). Add:
```kotlin
binding.topBar.tvBarTitle.text = getString(R.string.<title_key>)
binding.topBar.btnBack.setOnClickListener { findNavController().popBackStack() }
```
Titles per screen (Task-8 table in the spec §4.2): start→`title_start`, courseSetup→`title_course_setup`, playerSetup→`title_player_setup`, gridView→`title_grid`, roundSummary→`title_summary`, history→`title_history`, roundDetail→`title_round_detail`, myCourses→`title_my_courses`. Each fragment already creates its binding (`FragmentXBinding.bind(view)`); reuse it.

- [ ] **Step 4: Build** — `./gradlew :app:assembleDebug` → BUILD SUCCESSFUL after all eight are done.

- [ ] **Step 5: Visual recolor check.** Install and screenshot home + one inner screen to confirm the dark-primary palette applied via aliases:
```bash
ANDROID_HOME=/Users/inhyo/Library/Android/sdk ./gradlew :app:installDebug
/Users/inhyo/Library/Android/sdk/platform-tools/adb shell monkey -p com.parkgolf.score -c android.intent.category.LAUNCHER 1
```
Confirm buttons render dark (not green). (No automated assertion; a build-green + manual glance suffices.)

- [ ] **Step 6: Commit**
```bash
git add -A
git commit -m "feat: add shared top bar to all non-home screens; app-wide recolor via aliases"
```

---

## Task 9: Instrumented flow tests + regression

**Files:**
- Modify: `app/src/androidTest/java/com/parkgolf/score/ui/CourseWizardFlowTest.kt`

- [ ] **Step 1: Replace CourseWizardFlowTest.kt** with tests for the redesigned flow (an emulator must be running; `adb devices` shows `device`):

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

    @Test fun createCourse_throughAllSteps_showsSuccessScreen() {
        ActivityScenario.launch(MainActivity::class.java)
        onView(withId(R.id.btnMyCourses)).perform(click())
        onView(withId(R.id.btnAddVenue)).perform(click())
        // Step 1: venue name
        onView(withId(R.id.etVenueName)).perform(replaceText("테스트구장"), closeSoftKeyboard())
        onView(withId(R.id.btnNext)).perform(click())
        // Step 2: course name (default filled)
        onView(withId(R.id.btnNext)).perform(click())
        // Step 3: holes (9 default) -> add one
        onView(withId(R.id.btnAddHole)).perform(click())
        onView(withId(R.id.btnNext)).perform(click())
        // Step 4: save
        onView(withId(R.id.btnNext)).perform(click())
        // Success screen
        onView(withText(R.string.save_done_title)).check(matches(isDisplayed()))
    }

    @Test fun backOnFirstStep_showsCancelDialog() {
        ActivityScenario.launch(MainActivity::class.java)
        onView(withId(R.id.btnMyCourses)).perform(click())
        onView(withId(R.id.btnAddVenue)).perform(click())
        onView(withId(R.id.etVenueName)).perform(replaceText("취소구장"), closeSoftKeyboard())
        onView(withId(R.id.btnBack)).perform(click())
        onView(withText(R.string.confirm_cancel_create)).check(matches(isDisplayed()))
    }
}
```
> `btnBack` is now a `LinearLayout` (Task 4) — `withId(R.id.btnBack)` + `click()` still works.

- [ ] **Step 2: Run instrumented tests** — `./gradlew :app:connectedDebugAndroidTest` → all green (`CourseWizardFlowTest` ×2, `CoreFlowTest` ×2, `RepositoryTest`). If a wizard test flakes on stale app data (a leftover in-progress round intercepting Home), uninstall first: `adb uninstall com.parkgolf.score`, then re-run.

- [ ] **Step 3: Run full unit suite** — `./gradlew :app:testDebugUnitTest` → BUILD SUCCESSFUL.

- [ ] **Step 4: Commit**
```bash
git add app/src/androidTest/java/com/parkgolf/score/ui/CourseWizardFlowTest.kt
git commit -m "test: update wizard instrumented tests for redesigned flow + success screen"
```

---

## Verification Checklist (after all tasks)

- [ ] `./gradlew :app:testDebugUnitTest` — all unit tests pass.
- [ ] `./gradlew :app:connectedDebugAndroidTest` — all instrumented tests pass.
- [ ] `./gradlew :app:assembleDebug` — clean build.
- [ ] Manual: 코스 작성 4단계 카드 UI + 저장 완료 화면이 목업과 일치.
- [ ] Manual: 기존 구장명 입력 → "기존 구장" 배지 + 통합 배너; 부분 입력 → 제안 목록.
- [ ] Manual: 같은 구장에 중복 코스명 → 빨강 오류 + 다음 비활성.
- [ ] Manual: 홀 추가/삭제(최소 1홀), 파 1 이상 자유.
- [ ] Manual: 모든 비홈 화면 상단바 표시(편집 제목 "코스 편집"), 앱 전체 검정 primary.
