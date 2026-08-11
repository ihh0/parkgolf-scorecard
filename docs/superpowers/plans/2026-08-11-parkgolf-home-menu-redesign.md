# Home (Main Menu) Redesign Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Rebuild the home screen to the mockup — settings gear, flag logo + app title, and three card-style menu items (게임 시작 / 게임 기록 / 코스 관리) via a reusable include — keeping navigation and the in-progress-round resume dialog unchanged.

**Architecture:** A single reusable `view_home_menu_item.xml` (icon box + title + description + chevron) is `<include>`d three times in `fragment_home.xml`. `HomeFragment` injects each item's icon/title/description and click target, and preserves the existing resume dialog. Colors come from the theme (`?attr/park*`, from sub-project 1). The three home entry screens' top-bar title strings are re-worded to match the mockup.

**Tech Stack:** Kotlin, View system (XML) + ConstraintLayout/LinearLayout + ViewBinding, Material3 themes, Jetpack Navigation, Espresso. minSdk 21.

**Design spec:** `docs/superpowers/specs/2026-08-11-parkgolf-home-menu-redesign-design.md`
**Mockup:** `docs/mockups/파크골프 점수 기록 앱/src/app/components/MainMenu.tsx`

---

## File Structure

**New:** `res/drawable/ic_flag_solid.xml`, `ic_play_circle.xml`, `ic_clipboard_list.xml`, `ic_layout_list.xml`, `bg_menu_icon.xml`; `res/layout/view_home_menu_item.xml`.
**Modified:** `res/values/dimens.xml`, `strings.xml`; `res/layout/fragment_home.xml`; `java/.../ui/home/HomeFragment.kt`; `androidTest/.../ui/CoreFlowTest.kt`; new `androidTest/.../ui/HomeMenuTest.kt`.

Existing reused: `ic_chevron_left`/`ic_chevron_right` (chevron), `ic_settings`, `bg_card`, `@color/on_primary`, theme attrs `?attr/parkPrimary`, `?attr/parkPrimary10`, `?attr/parkForeground`, `?attr/parkMutedForeground`, `?attr/parkBackground`, dimens `radius_card`, `screen_padding`, `touch_min`.

---

## Task 1: Icons, drawable, dimens, strings, title rewording

**Files:** Create 5 drawables; Modify `dimens.xml`, `strings.xml`.

- [ ] **Step 1: Create `app/src/main/res/drawable/ic_flag_solid.xml`** (filled flag, logo):
```xml
<vector xmlns:android="http://schemas.android.com/apk/res/android" android:width="24dp" android:height="24dp" android:viewportWidth="24" android:viewportHeight="24">
    <path android:fillColor="@color/on_primary" android:pathData="M5,3 C5,3 6,2 9,2 C12,2 14,4 17,4 C19,4 20,3.5 20,3.5 L20,14 C20,14 19,15 17,15 C14,15 12,13 9,13 C6.5,13 5.4,13.7 5,14 L5,22 L3,22 L3,3 Z" />
</vector>
```

- [ ] **Step 2: Create the three menu icons** (lucide stroke, default `?attr/parkPrimary`):
`ic_play_circle.xml`:
```xml
<vector xmlns:android="http://schemas.android.com/apk/res/android" android:width="24dp" android:height="24dp" android:viewportWidth="24" android:viewportHeight="24">
    <path android:strokeColor="?attr/parkPrimary" android:strokeWidth="1.7" android:strokeLineCap="round" android:strokeLineJoin="round" android:pathData="M22,12 A10,10 0 1 1 2,12 A10,10 0 0 1 22,12 Z M10,8 L16,12 L10,16 Z" />
</vector>
```
`ic_clipboard_list.xml`:
```xml
<vector xmlns:android="http://schemas.android.com/apk/res/android" android:width="24dp" android:height="24dp" android:viewportWidth="24" android:viewportHeight="24">
    <path android:strokeColor="?attr/parkPrimary" android:strokeWidth="1.7" android:strokeLineCap="round" android:strokeLineJoin="round" android:pathData="M9,2 L15,2 A1,1 0 0 1 16,3 L16,5 A1,1 0 0 1 15,6 L9,6 A1,1 0 0 1 8,5 L8,3 A1,1 0 0 1 9,2 Z M16,4 L18,4 A2,2 0 0 1 20,6 L20,20 A2,2 0 0 1 18,22 L6,22 A2,2 0 0 1 4,20 L4,6 A2,2 0 0 1 6,4 L8,4 M8,11 L8.01,11 M12,11 L16,11 M8,16 L8.01,16 M12,16 L16,16" />
</vector>
```
`ic_layout_list.xml`:
```xml
<vector xmlns:android="http://schemas.android.com/apk/res/android" android:width="24dp" android:height="24dp" android:viewportWidth="24" android:viewportHeight="24">
    <path android:strokeColor="?attr/parkPrimary" android:strokeWidth="1.7" android:strokeLineCap="round" android:strokeLineJoin="round" android:pathData="M4,4 L10,4 A1,1 0 0 1 11,5 L11,9 A1,1 0 0 1 10,10 L4,10 A1,1 0 0 1 3,9 L3,5 A1,1 0 0 1 4,4 Z M4,14 L10,14 A1,1 0 0 1 11,15 L11,19 A1,1 0 0 1 10,20 L4,20 A1,1 0 0 1 3,19 L3,15 A1,1 0 0 1 4,14 Z M15,5 L21,5 M15,9 L21,9 M15,15 L21,15 M15,19 L21,19" />
</vector>
```

- [ ] **Step 3: Create `app/src/main/res/drawable/bg_menu_icon.xml`** (rounded icon box, `rounded-2xl`):
```xml
<?xml version="1.0" encoding="utf-8"?>
<shape xmlns:android="http://schemas.android.com/apk/res/android" android:shape="rectangle">
    <solid android:color="?attr/parkPrimary10" />
    <corners android:radius="@dimen/radius_card" />
</shape>
```

- [ ] **Step 4: Append to `app/src/main/res/values/dimens.xml`** (inside `<resources>`):
```xml
    <dimen name="logo_size">112dp</dimen>
    <dimen name="logo_icon">52dp</dimen>
    <dimen name="menu_icon_box">56dp</dimen>
    <dimen name="menu_icon">28dp</dimen>
    <dimen name="app_title_text">34sp</dimen>
    <dimen name="app_subtitle_text">22sp</dimen>
    <dimen name="menu_desc_text">13sp</dimen>
```

- [ ] **Step 5: Append new strings to `app/src/main/res/values/strings.xml`** (inside `<resources>`):
```xml
    <string name="app_title">파크골프</string>
    <string name="app_subtitle">스코어 앱</string>
    <string name="menu_game_start">게임 시작</string>
    <string name="menu_game_start_desc">구장을 선택하고 경기를 시작하세요</string>
    <string name="menu_game_records">게임 기록</string>
    <string name="menu_game_records_desc">지난 경기 기록을 확인하세요</string>
    <string name="menu_course_management">코스 관리</string>
    <string name="menu_course_management_desc">저장된 코스를 편집하거나 삭제하세요</string>
```

- [ ] **Step 6: Reword the three entry-screen titles** — in `app/src/main/res/values/strings.xml`, change the VALUES (keep names):
  - `<string name="title_start">라운드 시작</string>` → `<string name="title_start">게임 시작</string>`
  - `<string name="title_history">지난 기록</string>` → `<string name="title_history">게임 기록</string>`
  - `<string name="title_my_courses">내 구장 관리</string>` → `<string name="title_my_courses">코스 관리</string>`

- [ ] **Step 7: Build** — `ANDROID_HOME=/Users/inhyo/Library/Android/sdk ./gradlew :app:processDebugResources` → BUILD SUCCESSFUL.

- [ ] **Step 8: Commit**
```bash
git add app/src/main/res/drawable/ic_flag_solid.xml app/src/main/res/drawable/ic_play_circle.xml app/src/main/res/drawable/ic_clipboard_list.xml app/src/main/res/drawable/ic_layout_list.xml app/src/main/res/drawable/bg_menu_icon.xml app/src/main/res/values/dimens.xml app/src/main/res/values/strings.xml
git commit -m "feat: add home menu icons, icon-box drawable, dimens, and strings"
```

---

## Task 2: Reusable menu-item layout + home layout rebuild

**Files:** Create `res/layout/view_home_menu_item.xml`; Replace `res/layout/fragment_home.xml`.

- [ ] **Step 1: Create `app/src/main/res/layout/view_home_menu_item.xml`**
```xml
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent" android:layout_height="wrap_content"
    android:orientation="horizontal" android:gravity="center_vertical"
    android:background="@drawable/bg_card" android:padding="20dp"
    android:minHeight="@dimen/touch_min" android:clickable="true" android:focusable="true">

    <FrameLayout
        android:layout_width="@dimen/menu_icon_box" android:layout_height="@dimen/menu_icon_box"
        android:background="@drawable/bg_menu_icon">
        <ImageView android:id="@+id/ivIcon"
            android:layout_width="@dimen/menu_icon" android:layout_height="@dimen/menu_icon"
            android:layout_gravity="center" android:importantForAccessibility="no" />
    </FrameLayout>

    <LinearLayout
        android:layout_width="0dp" android:layout_height="wrap_content" android:layout_weight="1"
        android:orientation="vertical" android:layout_marginStart="16dp">
        <TextView android:id="@+id/tvTitle"
            android:layout_width="wrap_content" android:layout_height="wrap_content"
            android:textSize="@dimen/body_text" android:textStyle="bold" android:textColor="?attr/parkForeground" />
        <TextView android:id="@+id/tvDesc"
            android:layout_width="wrap_content" android:layout_height="wrap_content"
            android:textSize="@dimen/menu_desc_text" android:textColor="?attr/parkMutedForeground"
            android:layout_marginTop="2dp" />
    </LinearLayout>

    <ImageView
        android:layout_width="22dp" android:layout_height="22dp"
        android:src="@drawable/ic_chevron_right" android:importantForAccessibility="no"
        android:layout_marginStart="8dp"
        app:tint="?attr/parkMutedForeground"
        xmlns:app="http://schemas.android.com/apk/res-auto" />
</LinearLayout>
```

- [ ] **Step 2: Replace `app/src/main/res/layout/fragment_home.xml`**
```xml
<?xml version="1.0" encoding="utf-8"?>
<ScrollView xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent" android:layout_height="match_parent"
    android:fillViewport="true" android:background="?attr/parkBackground">

    <LinearLayout
        android:layout_width="match_parent" android:layout_height="wrap_content"
        android:orientation="vertical" android:padding="20dp">

        <ImageButton
            android:id="@+id/btnSettings"
            android:layout_width="48dp" android:layout_height="48dp"
            android:layout_gravity="end"
            android:background="@drawable/bg_card"
            android:src="@drawable/ic_settings"
            android:scaleType="center"
            android:contentDescription="@string/settings_label" />

        <FrameLayout
            android:layout_width="@dimen/logo_size" android:layout_height="@dimen/logo_size"
            android:layout_gravity="center_horizontal" android:layout_marginTop="32dp"
            android:background="@drawable/bg_logo" android:elevation="8dp">
            <ImageView
                android:layout_width="@dimen/logo_icon" android:layout_height="@dimen/logo_icon"
                android:layout_gravity="center" android:src="@drawable/ic_flag_solid"
                android:importantForAccessibility="no" />
        </FrameLayout>

        <TextView
            android:layout_width="wrap_content" android:layout_height="wrap_content"
            android:layout_gravity="center_horizontal" android:layout_marginTop="20dp"
            android:text="@string/app_title" android:textSize="@dimen/app_title_text"
            android:textStyle="bold" android:textColor="?attr/parkForeground" />
        <TextView
            android:layout_width="wrap_content" android:layout_height="wrap_content"
            android:layout_gravity="center_horizontal" android:layout_marginTop="2dp"
            android:text="@string/app_subtitle" android:textSize="@dimen/app_subtitle_text"
            android:textColor="?attr/parkMutedForeground" />

        <include android:id="@+id/itemGameStart" layout="@layout/view_home_menu_item"
            android:layout_width="match_parent" android:layout_height="wrap_content"
            android:layout_marginTop="48dp" />
        <include android:id="@+id/itemGameRecords" layout="@layout/view_home_menu_item"
            android:layout_width="match_parent" android:layout_height="wrap_content"
            android:layout_marginTop="16dp" />
        <include android:id="@+id/itemCourseManagement" layout="@layout/view_home_menu_item"
            android:layout_width="match_parent" android:layout_height="wrap_content"
            android:layout_marginTop="16dp" />
    </LinearLayout>
</ScrollView>
```

- [ ] **Step 3: Create `app/src/main/res/drawable/bg_logo.xml`** (referenced above — primary rounded square):
```xml
<?xml version="1.0" encoding="utf-8"?>
<shape xmlns:android="http://schemas.android.com/apk/res/android" android:shape="rectangle">
    <solid android:color="?attr/parkPrimary" />
    <corners android:radius="24dp" />
</shape>
```

- [ ] **Step 4: Build** — `ANDROID_HOME=/Users/inhyo/Library/Android/sdk ./gradlew :app:processDebugResources` → BUILD SUCCESSFUL. (Kotlin still references old ids `btnStart` etc.; do NOT run assembleDebug yet — Task 3 updates HomeFragment. `processDebugResources` is enough to validate layouts.)

- [ ] **Step 5: Commit**
```bash
git add app/src/main/res/layout/view_home_menu_item.xml app/src/main/res/layout/fragment_home.xml app/src/main/res/drawable/bg_logo.xml
git commit -m "feat: rebuild home layout with logo and card menu items"
```

---

## Task 3: HomeFragment — inject items, preserve resume dialog

**Files:** Replace `app/src/main/java/com/parkgolf/score/ui/home/HomeFragment.kt`.

- [ ] **Step 1: Replace `HomeFragment.kt`** — each include binding exposes `ivIcon`, `tvTitle`, `tvDesc`, and `.root` (the clickable card). Set values + clicks; keep the resume-dialog block verbatim:
```kotlin
package com.parkgolf.score.ui.home

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.parkgolf.score.App
import com.parkgolf.score.R
import com.parkgolf.score.databinding.FragmentHomeBinding
import com.parkgolf.score.databinding.ViewHomeMenuItemBinding
import com.parkgolf.score.ui.RoundSessionViewModel
import kotlinx.coroutines.launch

class HomeFragment : Fragment(R.layout.fragment_home) {
    private val session: RoundSessionViewModel by activityViewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val binding = FragmentHomeBinding.bind(view)
        val repo = App.repo(requireActivity().application)

        bindItem(
            binding.itemGameStart, R.drawable.ic_play_circle,
            R.string.menu_game_start, R.string.menu_game_start_desc,
        ) { findNavController().navigate(R.id.startFragment) }

        bindItem(
            binding.itemGameRecords, R.drawable.ic_clipboard_list,
            R.string.menu_game_records, R.string.menu_game_records_desc,
        ) { findNavController().navigate(R.id.historyFragment) }

        bindItem(
            binding.itemCourseManagement, R.drawable.ic_layout_list,
            R.string.menu_course_management, R.string.menu_course_management_desc,
        ) { findNavController().navigate(R.id.myCoursesFragment) }

        binding.btnSettings.setOnClickListener {
            findNavController().navigate(R.id.settingsFragment)
        }

        viewLifecycleOwner.lifecycleScope.launch {
            val current = repo.currentInProgressRound()
            if (current != null && session.round.value == null) {
                MaterialAlertDialogBuilder(requireContext())
                    .setMessage(R.string.resume_round)
                    .setPositiveButton(R.string.resume) { _, _ ->
                        session.startRound(current)
                        findNavController().navigate(R.id.holeInputFragment)
                    }
                    .setNegativeButton(R.string.discard) { _, _ ->
                        viewLifecycleOwner.lifecycleScope.launch { repo.deleteRound(current.id) }
                    }
                    .show()
            }
        }
    }

    private fun bindItem(
        item: ViewHomeMenuItemBinding,
        iconRes: Int,
        titleRes: Int,
        descRes: Int,
        onClick: () -> Unit,
    ) {
        item.ivIcon.setImageResource(iconRes)
        item.tvTitle.setText(titleRes)
        item.tvDesc.setText(descRes)
        item.root.setOnClickListener { onClick() }
    }
}
```
> `binding.itemGameStart` resolves to `ViewHomeMenuItemBinding` because the `<include>` has an `android:id` and points at a ViewBinding-enabled layout. `RoundSessionViewModel.startRound(...)` is the existing method used by the current code — keep it as-is.

- [ ] **Step 2: Build** — `ANDROID_HOME=/Users/inhyo/Library/Android/sdk ./gradlew :app:assembleDebug` → BUILD SUCCESSFUL.

- [ ] **Step 3: Install & visually confirm** on the running emulator:
```bash
ANDROID_HOME=/Users/inhyo/Library/Android/sdk ./gradlew :app:installDebug
/Users/inhyo/Library/Android/sdk/platform-tools/adb shell am start -n com.parkgolf.score/.ui.MainActivity
```
Screenshot (`adb exec-out screencap -p > /tmp/home.png`) and confirm: settings gear top-right, primary logo box with white flag, "파크골프"/"스코어 앱", three cards (게임 시작 / 게임 기록 / 코스 관리) each with icon box + title + description + chevron, all in the active theme color.

- [ ] **Step 4: Commit**
```bash
git add app/src/main/java/com/parkgolf/score/ui/home/HomeFragment.kt
git commit -m "feat: wire home menu items and preserve resume dialog"
```

---

## Task 4: Tests

**Files:** Modify `androidTest/.../ui/CoreFlowTest.kt`; Create `androidTest/.../ui/HomeMenuTest.kt`.

- [ ] **Step 1: Update `CoreFlowTest.kt`** — the old `btnStart` id/text are gone. Replace both tests' home references:
```kotlin
package com.parkgolf.score.ui

import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.parkgolf.score.R
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CoreFlowTest {
    @Test fun home_showsStartMenu() {
        ActivityScenario.launch(MainActivity::class.java)
        onView(withText(R.string.menu_game_start)).check(matches(isDisplayed()))
    }

    @Test fun startMenu_navigatesToStartScreen() {
        ActivityScenario.launch(MainActivity::class.java)
        onView(withId(R.id.itemGameStart)).perform(click())
        onView(withId(R.id.btnNewCourse)).check(matches(isDisplayed()))
    }
}
```

- [ ] **Step 2: Create `app/src/androidTest/java/com/parkgolf/score/ui/HomeMenuTest.kt`**
```kotlin
package com.parkgolf.score.ui

import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.parkgolf.score.R
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HomeMenuTest {
    @Test fun home_showsThreeMenuItems() {
        ActivityScenario.launch(MainActivity::class.java)
        onView(withText(R.string.menu_game_start)).check(matches(isDisplayed()))
        onView(withText(R.string.menu_game_records)).check(matches(isDisplayed()))
        onView(withText(R.string.menu_course_management)).check(matches(isDisplayed()))
    }
}
```

- [ ] **Step 3: Boot check + run instrumented tests** — ensure an emulator is booted (`adb devices` shows `device`), then:
`ANDROID_HOME=/Users/inhyo/Library/Android/sdk ./gradlew :app:connectedDebugAndroidTest` → all green (`CoreFlowTest` ×2, `HomeMenuTest`, plus existing `CourseWizardFlowTest`, `SettingsFlowTest`, `RepositoryTest`). If a test flakes on a leftover resume-round dialog from stale data, `adb uninstall com.parkgolf.score` and re-run.

- [ ] **Step 4: Run full unit suite** — `./gradlew :app:testDebugUnitTest` → BUILD SUCCESSFUL.

- [ ] **Step 5: Commit**
```bash
git add app/src/androidTest/java/com/parkgolf/score/ui/CoreFlowTest.kt app/src/androidTest/java/com/parkgolf/score/ui/HomeMenuTest.kt
git commit -m "test: update home tests for card menu redesign"
```

---

## Verification Checklist (after all tasks)

- [ ] `./gradlew :app:assembleDebug` — clean build.
- [ ] `./gradlew :app:testDebugUnitTest` — all unit tests pass.
- [ ] `./gradlew :app:connectedDebugAndroidTest` — all instrumented tests pass.
- [ ] Manual: 홈이 로고 + "파크골프/스코어 앱" + 카드 3개(게임 시작/게임 기록/코스 관리) 로 표시.
- [ ] Manual: 각 카드 → 해당 화면 이동, 진입 화면 상단바 제목이 새 문구.
- [ ] Manual: 진행 중 라운드가 있으면 홈에서 복구 다이얼로그 표시.
- [ ] Manual: 테마를 파랑/주황/보라로 바꾸면 홈 로고·아이콘 박스·텍스트 색이 따라감.
