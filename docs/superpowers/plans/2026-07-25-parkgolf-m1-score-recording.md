# 파크골프 M1 (스코어 기록 코어) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a fully offline Android app that lets senior park-golfers record hole-by-hole scores for up to 4 players, view/edit history, and see minimal stats — no backend, no login.

**Architecture:** MVVM with the Android View system (XML + ConstraintLayout + ViewBinding). Pure-Kotlin domain logic (scoring/stats) sits under a Repository backed by Room. Each screen has a ViewModel exposing state via StateFlow. Rounds snapshot their own pars/players/holes so later edits to saved courses never mutate history.

**Tech Stack:** Kotlin, Android View system, ConstraintLayout, ViewBinding, AndroidX, Jetpack Navigation Component (single-Activity), Room, Gson (Room TypeConverters), Coroutines/Flow. minSdk 21, targetSdk 34. Tests: JUnit4 + Truth (unit), Robolectric-free in-memory Room (instrumented `androidTest`), Espresso (one core flow).

---

## Conventions used in this plan

- Package root: `com.parkgolf.score`
- Unit tests live in `app/src/test/...`; instrumented tests (Room, Espresso) in `app/src/androidTest/...`.
- Run unit tests: `./gradlew testDebugUnitTest`
- Run instrumented tests: `./gradlew connectedDebugAndroidTest` (needs an emulator/device; API 21+).
- Commit after every task's final step. Commit messages use Conventional Commits.
- "Expected: FAIL/PASS" lines tell you what a correct run looks like before moving on.

---

## File Structure (locked-in decomposition)

```
app/
  build.gradle.kts
  src/main/
    AndroidManifest.xml
    java/com/parkgolf/score/
      domain/model/            # pure Kotlin, no Android imports
        HoleSpec.kt
        Round.kt               # domain Round, Player, ParRelation
      domain/
        Scoring.kt             # total, parTotal, relativeToPar, labels
        Stats.kt               # recent average, best, worst
      data/db/
        entity/VenueEntity.kt
        entity/CourseEntity.kt
        entity/RoundEntity.kt
        Converters.kt          # Gson list converters
        VenueDao.kt
        CourseDao.kt
        RoundDao.kt
        AppDatabase.kt
      data/
        ParkGolfRepository.kt          # interface
        ParkGolfRepositoryImpl.kt      # Room-backed
        Mappers.kt                     # entity <-> domain
      ui/
        MainActivity.kt
        home/HomeFragment.kt / HomeViewModel.kt
        start/StartFragment.kt / StartViewModel.kt
        start/CourseSetupFragment.kt / PlayerSetupFragment.kt
        hole/HoleInputFragment.kt / HoleInputViewModel.kt
        grid/GridViewFragment.kt / GridViewModel.kt
        summary/RoundSummaryFragment.kt / SummaryViewModel.kt
        history/HistoryFragment.kt / HistoryViewModel.kt
        history/RoundDetailFragment.kt
        courses/MyCoursesFragment.kt / CourseEditFragment.kt / CoursesViewModel.kt
      App.kt                    # Application: DB + repository singletons (manual DI)
    res/layout/...              # one XML per screen/item
    res/values/                 # colors.xml, dimens.xml, strings.xml, themes
  src/test/java/com/parkgolf/score/
    domain/ScoringTest.kt
    domain/StatsTest.kt
    ui/*ViewModelTest.kt
  src/androidTest/java/com/parkgolf/score/
    data/RepositoryTest.kt
    ui/CoreFlowTest.kt
```

Manual DI (an `App` holding singletons) is used instead of Hilt to keep old-device footprint small and the plan dependency-light.

---

## Phase 0 — Project setup

### Task 0.1: Create the Android project skeleton

**Files:**
- Create: `settings.gradle.kts`, `build.gradle.kts` (root), `app/build.gradle.kts`, `gradle.properties`, `app/src/main/AndroidManifest.xml`

- [ ] **Step 1: Root Gradle files**

`settings.gradle.kts`:
```kotlin
pluginManagement {
    repositories { google(); mavenCentral(); gradlePluginPortal() }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories { google(); mavenCentral() }
}
rootProject.name = "ParkGolfScore"
include(":app")
```

`build.gradle.kts` (root):
```kotlin
plugins {
    id("com.android.application") version "8.5.2" apply false
    id("org.jetbrains.kotlin.android") version "1.9.24" apply false
    id("com.google.devtools.ksp") version "1.9.24-1.0.20" apply false
}
```

`gradle.properties`:
```
org.gradle.jvmargs=-Xmx2048m
android.useAndroidX=true
kotlin.code.style=official
```

- [ ] **Step 2: App module Gradle**

`app/build.gradle.kts`:
```kotlin
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.devtools.ksp")
}

android {
    namespace = "com.parkgolf.score"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.parkgolf.score"
        minSdk = 21
        targetSdk = 34
        versionCode = 1
        versionName = "0.1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables { useSupportLibrary = true }
    }
    buildFeatures { viewBinding = true }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
        isCoreLibraryDesugaringEnabled = true
    }
    kotlinOptions { jvmTarget = "1.8" }
    buildTypes {
        release { isMinifyEnabled = false }
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.fragment:fragment-ktx:1.8.2")
    implementation("androidx.navigation:navigation-fragment-ktx:2.7.7")
    implementation("androidx.navigation:navigation-ui-ktx:2.7.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.4")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.4")
    implementation("androidx.recyclerview:recyclerview:1.3.2")

    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")

    implementation("com.google.code.gson:gson:2.11.0")
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.0.4")

    testImplementation("junit:junit:4.13.2")
    testImplementation("com.google.truth:truth:1.4.4")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.1")
    testImplementation("androidx.arch.core:core-testing:2.2.0")   // InstantTaskExecutorRule for LiveData

    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test:runner:1.6.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
    androidTestImplementation("androidx.room:room-testing:2.6.1")
    androidTestImplementation("com.google.truth:truth:1.4.4")
    androidTestImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.1")
}
```

- [ ] **Step 3: Minimal manifest**

`app/src/main/AndroidManifest.xml`:
```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">
    <application
        android:name=".App"
        android:allowBackup="true"
        android:label="파크골프 스코어"
        android:supportsRtl="true"
        android:theme="@style/Theme.ParkGolf">
        <activity
            android:name=".ui.MainActivity"
            android:exported="true">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>
    </application>
</manifest>
```

- [ ] **Step 4: Verify it configures**

Run: `./gradlew help`
Expected: `BUILD SUCCESSFUL` (dependencies resolve; no compile yet).

- [ ] **Step 5: Commit**

```bash
git add settings.gradle.kts build.gradle.kts gradle.properties app/
git commit -m "chore: scaffold Android app module (minSdk 21, viewBinding, Room)"
```

### Task 0.2: Theme, colors, dimens, strings (senior-friendly defaults)

**Files:**
- Create: `app/src/main/res/values/colors.xml`, `dimens.xml`, `strings.xml`, `themes.xml`

- [ ] **Step 1: colors.xml (high-contrast, color+text pairing)**
```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <color name="green_primary">#0A7D2C</color>
    <color name="green_light">#EAFBE7</color>
    <color name="surface">#FFFFFF</color>
    <color name="surface_muted">#F4F6F8</color>
    <color name="text_primary">#1A1A1A</color>
    <color name="text_muted">#5A5A5A</color>
    <color name="under_par">#1565C0</color>   <!-- blue -->
    <color name="even_par">#1A1A1A</color>     <!-- black -->
    <color name="over_par">#C62828</color>     <!-- red -->
    <color name="stepper_bg">#E0E0E0</color>
</resources>
```

- [ ] **Step 2: dimens.xml (large touch targets/fonts)**
```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <dimen name="touch_min">56dp</dimen>
    <dimen name="stepper_size">64dp</dimen>
    <dimen name="score_text">34sp</dimen>
    <dimen name="button_text">20sp</dimen>
    <dimen name="body_text">18sp</dimen>
    <dimen name="title_text">24sp</dimen>
    <dimen name="screen_padding">16dp</dimen>
</resources>
```

- [ ] **Step 3: strings.xml (all user-facing text; no hardcoded strings in layouts)**
```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <string name="app_name">파크골프 스코어</string>
    <string name="start_round">＋ 라운드 시작</string>
    <string name="past_records">📋 지난 기록</string>
    <string name="my_courses">🏳️ 내 구장 관리</string>
    <string name="recent_courses">최근 구장</string>
    <string name="saved_presets">내 구장 (저장한 프리셋)</string>
    <string name="nearby_courses_soon">📍 주변 구장 — 다음 업데이트에서 추가</string>
    <string name="new_course">＋ 새 구장 만들기</string>
    <string name="next_hole">다음 홀 ▶</string>
    <string name="prev_hole">◀ 이전 홀</string>
    <string name="full_grid">📊 전체 보기</string>
    <string name="hole_label">%1$d번 홀</string>
    <string name="par_label">파 %1$d</string>
    <string name="cumulative_label">누적 %1$s</string>
    <string name="even_par_label">E</string>
    <string name="decrease">한 타 줄이기</string>
    <string name="increase">한 타 늘리기</string>
    <string name="delete_round">기록 삭제</string>
    <string name="delete_confirm">이 기록을 삭제할까요?</string>
    <string name="cancel">취소</string>
    <string name="delete">삭제</string>
    <string name="save">저장</string>
    <string name="me">나</string>
    <string name="resume_round">진행 중인 라운드를 이어서 할까요?</string>
    <string name="resume">이어하기</string>
    <string name="discard">새로 시작</string>
</resources>
```

- [ ] **Step 4: themes.xml**
```xml
<?xml version="1.0" encoding="utf-8"?>
<resources xmlns:tools="http://schemas.android.com/tools">
    <style name="Theme.ParkGolf" parent="Theme.Material3.DayNight.NoActionBar">
        <item name="colorPrimary">@color/green_primary</item>
        <item name="android:windowBackground">@color/surface</item>
        <item name="android:textColorPrimary">@color/text_primary</item>
    </style>
</resources>
```

- [ ] **Step 5: Commit**
```bash
git add app/src/main/res/values/
git commit -m "chore: add senior-friendly theme, colors, dimens, strings"
```

---

## Phase 1 — Domain models & scoring (pure Kotlin, TDD)

### Task 1.1: Domain models

**Files:**
- Create: `app/src/main/java/com/parkgolf/score/domain/model/HoleSpec.kt`
- Create: `app/src/main/java/com/parkgolf/score/domain/model/Round.kt`

- [ ] **Step 1: Write the models (no test needed — plain data)**

`HoleSpec.kt`:
```kotlin
package com.parkgolf.score.domain.model

/** A single hole as snapshotted into a round: its origin course, hole number, and par. */
data class HoleSpec(
    val courseName: String,
    val holeNo: Int,
    val par: Int
)
```

`Round.kt`:
```kotlin
package com.parkgolf.score.domain.model

enum class ParRelation { UNDER, EVEN, OVER }

enum class RoundStatus { IN_PROGRESS, COMPLETED }

/**
 * A round snapshots everything it needs: player names, the ordered holes (with pars),
 * and a scores matrix indexed [playerIndex][holeIndex]. A null score means "not entered yet".
 */
data class Round(
    val id: Long = 0,
    val date: Long,                       // epoch millis
    val venueName: String,
    val players: List<String>,            // size 1..4
    val holes: List<HoleSpec>,
    val scores: List<List<Int?>>,         // [player][hole]
    val status: RoundStatus
)
```

- [ ] **Step 2: Verify it compiles**

Run: `./gradlew :app:compileDebugKotlin`
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 3: Commit**
```bash
git add app/src/main/java/com/parkgolf/score/domain/model/
git commit -m "feat: add domain models (HoleSpec, Round, ParRelation)"
```

### Task 1.2: Scoring logic

**Files:**
- Create: `app/src/main/java/com/parkgolf/score/domain/Scoring.kt`
- Test: `app/src/test/java/com/parkgolf/score/domain/ScoringTest.kt`

- [ ] **Step 1: Write the failing test**

`ScoringTest.kt`:
```kotlin
package com.parkgolf.score.domain

import com.google.common.truth.Truth.assertThat
import com.parkgolf.score.domain.model.HoleSpec
import com.parkgolf.score.domain.model.ParRelation
import org.junit.Test

class ScoringTest {
    private val holes = listOf(
        HoleSpec("A", 1, 3),
        HoleSpec("A", 2, 3),
        HoleSpec("A", 3, 4)
    )

    @Test fun total_ignoresNulls() {
        assertThat(Scoring.total(listOf(3, null, 5))).isEqualTo(8)
    }

    @Test fun parTotal_sumsPars() {
        assertThat(Scoring.parTotal(holes)).isEqualTo(10)
    }

    @Test fun relativeToPar_countsOnlyPlayedHoles() {
        // hole1: 4 vs par3 = +1, hole2: not played, hole3: 3 vs par4 = -1 -> 0
        assertThat(Scoring.relativeToPar(listOf(4, null, 3), holes)).isEqualTo(0)
    }

    @Test fun label_formatsUnderEvenOver() {
        assertThat(Scoring.relationLabel(-2)).isEqualTo("-2")
        assertThat(Scoring.relationLabel(0)).isEqualTo("E")
        assertThat(Scoring.relationLabel(3)).isEqualTo("+3")
    }

    @Test fun relation_classifies() {
        assertThat(Scoring.relation(-1)).isEqualTo(ParRelation.UNDER)
        assertThat(Scoring.relation(0)).isEqualTo(ParRelation.EVEN)
        assertThat(Scoring.relation(2)).isEqualTo(ParRelation.OVER)
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew testDebugUnitTest --tests "*.ScoringTest"`
Expected: FAIL — `Scoring` unresolved.

- [ ] **Step 3: Write minimal implementation**

`Scoring.kt`:
```kotlin
package com.parkgolf.score.domain

import com.parkgolf.score.domain.model.HoleSpec
import com.parkgolf.score.domain.model.ParRelation

object Scoring {
    fun total(scores: List<Int?>): Int = scores.filterNotNull().sum()

    fun parTotal(holes: List<HoleSpec>): Int = holes.sumOf { it.par }

    fun relativeToPar(scores: List<Int?>, holes: List<HoleSpec>): Int =
        scores.indices
            .filter { scores[it] != null }
            .sumOf { scores[it]!! - holes[it].par }

    fun relationLabel(diff: Int): String = when {
        diff < 0 -> diff.toString()
        diff == 0 -> "E"
        else -> "+$diff"
    }

    fun relation(diff: Int): ParRelation = when {
        diff < 0 -> ParRelation.UNDER
        diff == 0 -> ParRelation.EVEN
        else -> ParRelation.OVER
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew testDebugUnitTest --tests "*.ScoringTest"`
Expected: PASS (5 tests).

- [ ] **Step 5: Commit**
```bash
git add app/src/main/java/com/parkgolf/score/domain/Scoring.kt app/src/test/java/com/parkgolf/score/domain/ScoringTest.kt
git commit -m "feat: add scoring logic (total, par-relative, labels)"
```

### Task 1.3: Stats logic

**Files:**
- Create: `app/src/main/java/com/parkgolf/score/domain/Stats.kt`
- Test: `app/src/test/java/com/parkgolf/score/domain/StatsTest.kt`

- [ ] **Step 1: Write the failing test**

`StatsTest.kt`:
```kotlin
package com.parkgolf.score.domain

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class StatsTest {
    // totals are the player's own total strokes per completed round, newest first
    private val totals = listOf(41, 39, 45, 38, 40, 42, 44, 37, 43, 40, 99)

    @Test fun recentAverage_usesLastN() {
        // last 10 (excludes the trailing 99): average of first 10 entries
        val avg = Stats.recentAverageStrokes(totals, 10)
        assertThat(avg).isWithin(0.01).of(40.9)
    }

    @Test fun recentAverage_handlesFewerThanN() {
        assertThat(Stats.recentAverageStrokes(listOf(40, 42), 10)).isWithin(0.01).of(41.0)
    }

    @Test fun recentAverage_emptyIsNull() {
        assertThat(Stats.recentAverageStrokes(emptyList(), 10)).isNull()
    }

    @Test fun best_isLowest_worst_isHighest() {
        assertThat(Stats.best(totals)).isEqualTo(37)
        assertThat(Stats.worst(totals)).isEqualTo(99)
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew testDebugUnitTest --tests "*.StatsTest"`
Expected: FAIL — `Stats` unresolved.

- [ ] **Step 3: Write minimal implementation**

`Stats.kt`:
```kotlin
package com.parkgolf.score.domain

object Stats {
    /** Average of the most recent [n] totals (list is newest-first). Null if empty. */
    fun recentAverageStrokes(totals: List<Int>, n: Int): Double? {
        if (totals.isEmpty()) return null
        val window = totals.take(n)
        return window.sum().toDouble() / window.size
    }

    fun best(totals: List<Int>): Int? = totals.minOrNull()
    fun worst(totals: List<Int>): Int? = totals.maxOrNull()
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew testDebugUnitTest --tests "*.StatsTest"`
Expected: PASS (4 tests).

- [ ] **Step 5: Commit**
```bash
git add app/src/main/java/com/parkgolf/score/domain/Stats.kt app/src/test/java/com/parkgolf/score/domain/StatsTest.kt
git commit -m "feat: add minimal stats (recent average, best, worst)"
```

---

## Phase 2 — Room persistence

### Task 2.1: Gson TypeConverters

**Files:**
- Create: `app/src/main/java/com/parkgolf/score/data/db/Converters.kt`
- Test: `app/src/test/java/com/parkgolf/score/data/ConvertersTest.kt`

- [ ] **Step 1: Write the failing test**

`ConvertersTest.kt`:
```kotlin
package com.parkgolf.score.data

import com.google.common.truth.Truth.assertThat
import com.parkgolf.score.data.db.Converters
import com.parkgolf.score.domain.model.HoleSpec
import org.junit.Test

class ConvertersTest {
    private val c = Converters()

    @Test fun intList_roundTrips() {
        val list = listOf(3, 3, 4, 5)
        assertThat(c.toIntList(c.fromIntList(list))).isEqualTo(list)
    }

    @Test fun stringList_roundTrips() {
        val list = listOf("나", "김철수")
        assertThat(c.toStringList(c.fromStringList(list))).isEqualTo(list)
    }

    @Test fun holeList_roundTrips() {
        val list = listOf(HoleSpec("A", 1, 3), HoleSpec("B", 2, 4))
        assertThat(c.toHoleList(c.fromHoleList(list))).isEqualTo(list)
    }

    @Test fun scoreMatrix_roundTrips_withNulls() {
        val m = listOf(listOf(3, null, 5), listOf(null, 4, 4))
        assertThat(c.toScoreMatrix(c.fromScoreMatrix(m))).isEqualTo(m)
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew testDebugUnitTest --tests "*.ConvertersTest"`
Expected: FAIL — `Converters` unresolved.

- [ ] **Step 3: Write minimal implementation**

`Converters.kt`:
```kotlin
package com.parkgolf.score.data.db

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.parkgolf.score.domain.model.HoleSpec

class Converters {
    private val gson = Gson()

    @TypeConverter fun fromIntList(v: List<Int>): String = gson.toJson(v)
    @TypeConverter fun toIntList(v: String): List<Int> =
        gson.fromJson(v, object : TypeToken<List<Int>>() {}.type)

    @TypeConverter fun fromStringList(v: List<String>): String = gson.toJson(v)
    @TypeConverter fun toStringList(v: String): List<String> =
        gson.fromJson(v, object : TypeToken<List<String>>() {}.type)

    @TypeConverter fun fromHoleList(v: List<HoleSpec>): String = gson.toJson(v)
    @TypeConverter fun toHoleList(v: String): List<HoleSpec> =
        gson.fromJson(v, object : TypeToken<List<HoleSpec>>() {}.type)

    @TypeConverter fun fromScoreMatrix(v: List<List<Int?>>): String = gson.toJson(v)
    @TypeConverter fun toScoreMatrix(v: String): List<List<Int?>> =
        gson.fromJson(v, object : TypeToken<List<List<Int?>>>() {}.type)
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew testDebugUnitTest --tests "*.ConvertersTest"`
Expected: PASS (4 tests).

- [ ] **Step 5: Commit**
```bash
git add app/src/main/java/com/parkgolf/score/data/db/Converters.kt app/src/test/java/com/parkgolf/score/data/ConvertersTest.kt
git commit -m "feat: add Room Gson type converters"
```

### Task 2.2: Entities

**Files:**
- Create: `entity/VenueEntity.kt`, `entity/CourseEntity.kt`, `entity/RoundEntity.kt`

- [ ] **Step 1: Write entities**

`VenueEntity.kt`:
```kotlin
package com.parkgolf.score.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "venues")
data class VenueEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val memo: String? = null
)
```

`CourseEntity.kt`:
```kotlin
package com.parkgolf.score.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/** pars stored as JSON via Converters (List<Int>). A course is usually 9 holes. */
@Entity(tableName = "courses")
data class CourseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val venueId: Long,
    val name: String,
    val pars: List<Int>
)
```

`RoundEntity.kt`:
```kotlin
package com.parkgolf.score.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.parkgolf.score.domain.model.HoleSpec

@Entity(tableName = "rounds")
data class RoundEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: Long,
    val venueName: String,
    val players: List<String>,
    val holes: List<HoleSpec>,
    val scores: List<List<Int?>>,
    val status: String   // "IN_PROGRESS" | "COMPLETED"
)
```

- [ ] **Step 2: Verify compiles**

Run: `./gradlew :app:compileDebugKotlin`
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 3: Commit**
```bash
git add app/src/main/java/com/parkgolf/score/data/db/entity/
git commit -m "feat: add Room entities (venue, course, round)"
```

### Task 2.3: DAOs and database

**Files:**
- Create: `VenueDao.kt`, `CourseDao.kt`, `RoundDao.kt`, `AppDatabase.kt`

- [ ] **Step 1: Write the DAOs**

`VenueDao.kt`:
```kotlin
package com.parkgolf.score.data.db

import androidx.room.*
import com.parkgolf.score.data.db.entity.VenueEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface VenueDao {
    @Insert suspend fun insert(venue: VenueEntity): Long
    @Update suspend fun update(venue: VenueEntity)
    @Delete suspend fun delete(venue: VenueEntity)
    @Query("SELECT * FROM venues ORDER BY name") fun observeAll(): Flow<List<VenueEntity>>
    @Query("SELECT * FROM venues WHERE id = :id") suspend fun byId(id: Long): VenueEntity?
}
```

`CourseDao.kt`:
```kotlin
package com.parkgolf.score.data.db

import androidx.room.*
import com.parkgolf.score.data.db.entity.CourseEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CourseDao {
    @Insert suspend fun insert(course: CourseEntity): Long
    @Update suspend fun update(course: CourseEntity)
    @Delete suspend fun delete(course: CourseEntity)
    @Query("SELECT * FROM courses WHERE venueId = :venueId ORDER BY name")
    suspend fun forVenue(venueId: Long): List<CourseEntity>
    @Query("SELECT * FROM courses ORDER BY name") fun observeAll(): Flow<List<CourseEntity>>
}
```

`RoundDao.kt`:
```kotlin
package com.parkgolf.score.data.db

import androidx.room.*
import com.parkgolf.score.data.db.entity.RoundEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RoundDao {
    @Insert suspend fun insert(round: RoundEntity): Long
    @Update suspend fun update(round: RoundEntity)
    @Delete suspend fun delete(round: RoundEntity)
    @Query("SELECT * FROM rounds WHERE id = :id") suspend fun byId(id: Long): RoundEntity?
    @Query("SELECT * FROM rounds WHERE status = 'COMPLETED' ORDER BY date DESC")
    fun observeCompleted(): Flow<List<RoundEntity>>
    @Query("SELECT * FROM rounds WHERE status = 'IN_PROGRESS' ORDER BY date DESC LIMIT 1")
    suspend fun currentInProgress(): RoundEntity?
    @Query("SELECT * FROM rounds WHERE status = 'COMPLETED' ORDER BY date DESC")
    suspend fun completedList(): List<RoundEntity>
}
```

`AppDatabase.kt`:
```kotlin
package com.parkgolf.score.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.parkgolf.score.data.db.entity.CourseEntity
import com.parkgolf.score.data.db.entity.RoundEntity
import com.parkgolf.score.data.db.entity.VenueEntity

@Database(
    entities = [VenueEntity::class, CourseEntity::class, RoundEntity::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun venueDao(): VenueDao
    abstract fun courseDao(): CourseDao
    abstract fun roundDao(): RoundDao
}
```

- [ ] **Step 2: Verify compiles (Room codegen runs)**

Run: `./gradlew :app:compileDebugKotlin`
Expected: `BUILD SUCCESSFUL` — Room generates `AppDatabase_Impl`.

- [ ] **Step 3: Commit**
```bash
git add app/src/main/java/com/parkgolf/score/data/db/
git commit -m "feat: add Room DAOs and database"
```

---

## Phase 3 — Repository

### Task 3.1: Mappers and repository interface

**Files:**
- Create: `data/Mappers.kt`, `data/ParkGolfRepository.kt`

- [ ] **Step 1: Write mappers**

`Mappers.kt`:
```kotlin
package com.parkgolf.score.data

import com.parkgolf.score.data.db.entity.RoundEntity
import com.parkgolf.score.domain.model.Round
import com.parkgolf.score.domain.model.RoundStatus

fun RoundEntity.toDomain(): Round = Round(
    id = id,
    date = date,
    venueName = venueName,
    players = players,
    holes = holes,
    scores = scores,
    status = RoundStatus.valueOf(status)
)

fun Round.toEntity(): RoundEntity = RoundEntity(
    id = id,
    date = date,
    venueName = venueName,
    players = players,
    holes = holes,
    scores = scores,
    status = status.name
)
```

- [ ] **Step 2: Write the interface**

`ParkGolfRepository.kt`:
```kotlin
package com.parkgolf.score.data

import com.parkgolf.score.data.db.entity.CourseEntity
import com.parkgolf.score.data.db.entity.VenueEntity
import com.parkgolf.score.domain.model.Round
import kotlinx.coroutines.flow.Flow

interface ParkGolfRepository {
    // Rounds
    suspend fun saveRound(round: Round): Long   // insert or update, returns id
    suspend fun getRound(id: Long): Round?
    fun observeCompletedRounds(): Flow<List<Round>>
    suspend fun completedRounds(): List<Round>
    suspend fun currentInProgressRound(): Round?
    suspend fun deleteRound(id: Long)

    // Venues & courses
    fun observeVenues(): Flow<List<VenueEntity>>
    suspend fun coursesForVenue(venueId: Long): List<CourseEntity>
    suspend fun upsertVenue(venue: VenueEntity): Long
    suspend fun upsertCourse(course: CourseEntity): Long
    suspend fun deleteVenue(venue: VenueEntity)
    suspend fun deleteCourse(course: CourseEntity)
    fun observeCourses(): Flow<List<CourseEntity>>
}
```

- [ ] **Step 3: Verify compiles**

Run: `./gradlew :app:compileDebugKotlin`
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 4: Commit**
```bash
git add app/src/main/java/com/parkgolf/score/data/Mappers.kt app/src/main/java/com/parkgolf/score/data/ParkGolfRepository.kt
git commit -m "feat: add repository interface and round mappers"
```

### Task 3.2: Repository implementation + instrumented test

**Files:**
- Create: `data/ParkGolfRepositoryImpl.kt`
- Test: `app/src/androidTest/java/com/parkgolf/score/data/RepositoryTest.kt`

- [ ] **Step 1: Write the failing instrumented test**

`RepositoryTest.kt`:
```kotlin
package com.parkgolf.score.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.parkgolf.score.data.db.AppDatabase
import com.parkgolf.score.domain.model.HoleSpec
import com.parkgolf.score.domain.model.Round
import com.parkgolf.score.domain.model.RoundStatus
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RepositoryTest {
    private lateinit var db: AppDatabase
    private lateinit var repo: ParkGolfRepository

    @Before fun setup() {
        val ctx = ApplicationProvider.getApplicationContext<android.content.Context>()
        db = Room.inMemoryDatabaseBuilder(ctx, AppDatabase::class.java).build()
        repo = ParkGolfRepositoryImpl(db.venueDao(), db.courseDao(), db.roundDao())
    }

    @After fun teardown() = db.close()

    private fun sampleRound(status: RoundStatus) = Round(
        date = 1000L,
        venueName = "○○파크골프장",
        players = listOf("나", "김철수"),
        holes = listOf(HoleSpec("A", 1, 3), HoleSpec("A", 2, 3)),
        scores = listOf(listOf(3, 4), listOf(3, 3)),
        status = status
    )

    @Test fun saveAndGetRound_roundTrips() = runTest {
        val id = repo.saveRound(sampleRound(RoundStatus.COMPLETED))
        val loaded = repo.getRound(id)
        assertThat(loaded).isNotNull()
        assertThat(loaded!!.players).containsExactly("나", "김철수").inOrder()
        assertThat(loaded.scores[0]).containsExactly(3, 4).inOrder()
        assertThat(loaded.status).isEqualTo(RoundStatus.COMPLETED)
    }

    @Test fun currentInProgress_returnsOnlyInProgress() = runTest {
        repo.saveRound(sampleRound(RoundStatus.COMPLETED))
        val id = repo.saveRound(sampleRound(RoundStatus.IN_PROGRESS))
        val current = repo.currentInProgressRound()
        assertThat(current?.id).isEqualTo(id)
    }

    @Test fun deleteRound_removesIt() = runTest {
        val id = repo.saveRound(sampleRound(RoundStatus.COMPLETED))
        repo.deleteRound(id)
        assertThat(repo.getRound(id)).isNull()
    }

    @Test fun completedRounds_excludesInProgress() = runTest {
        repo.saveRound(sampleRound(RoundStatus.COMPLETED))
        repo.saveRound(sampleRound(RoundStatus.IN_PROGRESS))
        assertThat(repo.completedRounds()).hasSize(1)
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew connectedDebugAndroidTest --tests "*.RepositoryTest"`
Expected: FAIL — `ParkGolfRepositoryImpl` unresolved. (Requires a running emulator/device.)

- [ ] **Step 3: Write minimal implementation**

`ParkGolfRepositoryImpl.kt`:
```kotlin
package com.parkgolf.score.data

import com.parkgolf.score.data.db.CourseDao
import com.parkgolf.score.data.db.RoundDao
import com.parkgolf.score.data.db.VenueDao
import com.parkgolf.score.data.db.entity.CourseEntity
import com.parkgolf.score.data.db.entity.VenueEntity
import com.parkgolf.score.domain.model.Round
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ParkGolfRepositoryImpl(
    private val venueDao: VenueDao,
    private val courseDao: CourseDao,
    private val roundDao: RoundDao
) : ParkGolfRepository {

    override suspend fun saveRound(round: Round): Long {
        val entity = round.toEntity()
        return if (round.id == 0L) roundDao.insert(entity)
        else { roundDao.update(entity); round.id }
    }

    override suspend fun getRound(id: Long): Round? = roundDao.byId(id)?.toDomain()

    override fun observeCompletedRounds(): Flow<List<Round>> =
        roundDao.observeCompleted().map { list -> list.map { it.toDomain() } }

    override suspend fun completedRounds(): List<Round> =
        roundDao.completedList().map { it.toDomain() }

    override suspend fun currentInProgressRound(): Round? =
        roundDao.currentInProgress()?.toDomain()

    override suspend fun deleteRound(id: Long) {
        roundDao.byId(id)?.let { roundDao.delete(it) }
    }

    override fun observeVenues(): Flow<List<VenueEntity>> = venueDao.observeAll()
    override suspend fun coursesForVenue(venueId: Long): List<CourseEntity> =
        courseDao.forVenue(venueId)

    override suspend fun upsertVenue(venue: VenueEntity): Long =
        if (venue.id == 0L) venueDao.insert(venue) else { venueDao.update(venue); venue.id }

    override suspend fun upsertCourse(course: CourseEntity): Long =
        if (course.id == 0L) courseDao.insert(course) else { courseDao.update(course); course.id }

    override suspend fun deleteVenue(venue: VenueEntity) = venueDao.delete(venue)
    override suspend fun deleteCourse(course: CourseEntity) = courseDao.delete(course)
    override fun observeCourses(): Flow<List<CourseEntity>> = courseDao.observeAll()
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew connectedDebugAndroidTest --tests "*.RepositoryTest"`
Expected: PASS (4 tests).

- [ ] **Step 5: Commit**
```bash
git add app/src/main/java/com/parkgolf/score/data/ParkGolfRepositoryImpl.kt app/src/androidTest/java/com/parkgolf/score/data/RepositoryTest.kt
git commit -m "feat: add Room-backed repository implementation"
```

### Task 3.3: Application + manual DI

**Files:**
- Create: `app/src/main/java/com/parkgolf/score/App.kt`

- [ ] **Step 1: Write App**

`App.kt`:
```kotlin
package com.parkgolf.score

import android.app.Application
import androidx.room.Room
import com.parkgolf.score.data.ParkGolfRepository
import com.parkgolf.score.data.ParkGolfRepositoryImpl
import com.parkgolf.score.data.db.AppDatabase

class App : Application() {
    lateinit var repository: ParkGolfRepository
        private set

    override fun onCreate() {
        super.onCreate()
        val db = Room.databaseBuilder(this, AppDatabase::class.java, "parkgolf.db").build()
        repository = ParkGolfRepositoryImpl(db.venueDao(), db.courseDao(), db.roundDao())
    }

    companion object {
        fun repo(app: Application): ParkGolfRepository = (app as App).repository
    }
}
```

- [ ] **Step 2: Verify compiles**

Run: `./gradlew :app:compileDebugKotlin`
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 3: Commit**
```bash
git add app/src/main/java/com/parkgolf/score/App.kt
git commit -m "feat: add Application with manual DI singletons"
```

---

## Phase 4 — Round construction logic (TDD, pre-UI)

Before wiring screens, build the pure logic that assembles a `Round` from a course selection, so the UI layer stays thin.

### Task 4.1: RoundFactory

**Files:**
- Create: `app/src/main/java/com/parkgolf/score/domain/RoundFactory.kt`
- Test: `app/src/test/java/com/parkgolf/score/domain/RoundFactoryTest.kt`

- [ ] **Step 1: Write the failing test**

`RoundFactoryTest.kt`:
```kotlin
package com.parkgolf.score.domain

import com.google.common.truth.Truth.assertThat
import com.parkgolf.score.domain.model.RoundStatus
import org.junit.Test

class RoundFactoryTest {
    // Two 9-hole course units combined into an 18-hole round.
    private val courseA = RoundFactory.CoursePars("A코스", List(9) { 3 })
    private val courseB = RoundFactory.CoursePars("B코스", listOf(3,3,4,3,3,4,3,3,5))

    @Test fun buildsHoleSequence_withRenumberedHoles() {
        val round = RoundFactory.newRound(
            venueName = "○○파크골프장",
            players = listOf("나", "김철수"),
            courses = listOf(courseA, courseB),
            date = 5L
        )
        assertThat(round.holes).hasSize(18)
        assertThat(round.holes[0].holeNo).isEqualTo(1)
        assertThat(round.holes[9].holeNo).isEqualTo(10)
        assertThat(round.holes[9].courseName).isEqualTo("B코스")
        assertThat(round.holes[17].par).isEqualTo(5)
    }

    @Test fun initialScores_startAtPar_forEveryPlayer() {
        val round = RoundFactory.newRound("V", listOf("나", "철수"), listOf(courseA), 1L)
        assertThat(round.scores).hasSize(2)            // 2 players
        assertThat(round.scores[0]).hasSize(9)         // 9 holes
        assertThat(round.scores[0]).containsExactlyElementsIn(List(9) { 3 })
        assertThat(round.status).isEqualTo(RoundStatus.IN_PROGRESS)
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew testDebugUnitTest --tests "*.RoundFactoryTest"`
Expected: FAIL — `RoundFactory` unresolved.

- [ ] **Step 3: Write minimal implementation**

`RoundFactory.kt`:
```kotlin
package com.parkgolf.score.domain

import com.parkgolf.score.domain.model.HoleSpec
import com.parkgolf.score.domain.model.Round
import com.parkgolf.score.domain.model.RoundStatus

object RoundFactory {
    data class CoursePars(val courseName: String, val pars: List<Int>)

    fun newRound(
        venueName: String,
        players: List<String>,
        courses: List<CoursePars>,
        date: Long
    ): Round {
        val holes = mutableListOf<HoleSpec>()
        var holeNo = 1
        for (course in courses) {
            for (par in course.pars) {
                holes += HoleSpec(course.courseName, holeNo, par)
                holeNo++
            }
        }
        // Every score starts at that hole's par.
        val scores = players.map { holes.map { h -> h.par as Int? } }
        return Round(
            date = date,
            venueName = venueName,
            players = players,
            holes = holes,
            scores = scores,
            status = RoundStatus.IN_PROGRESS
        )
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew testDebugUnitTest --tests "*.RoundFactoryTest"`
Expected: PASS (2 tests).

- [ ] **Step 5: Commit**
```bash
git add app/src/main/java/com/parkgolf/score/domain/RoundFactory.kt app/src/test/java/com/parkgolf/score/domain/RoundFactoryTest.kt
git commit -m "feat: add RoundFactory that builds a round from course units"
```

### Task 4.2: ScoreEditor — immutable score updates

**Files:**
- Create: `app/src/main/java/com/parkgolf/score/domain/ScoreEditor.kt`
- Test: `app/src/test/java/com/parkgolf/score/domain/ScoreEditorTest.kt`

- [ ] **Step 1: Write the failing test**

`ScoreEditorTest.kt`:
```kotlin
package com.parkgolf.score.domain

import com.google.common.truth.Truth.assertThat
import com.parkgolf.score.domain.model.HoleSpec
import com.parkgolf.score.domain.model.Round
import com.parkgolf.score.domain.model.RoundStatus
import org.junit.Test

class ScoreEditorTest {
    private val round = Round(
        date = 1L, venueName = "V", players = listOf("나", "철수"),
        holes = listOf(HoleSpec("A", 1, 3), HoleSpec("A", 2, 3)),
        scores = listOf(mutableListOf<Int?>(3, 3), mutableListOf<Int?>(3, 3)),
        status = RoundStatus.IN_PROGRESS
    )

    @Test fun increment_addsOne() {
        val r = ScoreEditor.adjust(round, playerIndex = 0, holeIndex = 1, delta = +1)
        assertThat(r.scores[0][1]).isEqualTo(4)
        assertThat(r.scores[1][1]).isEqualTo(3) // untouched
    }

    @Test fun decrement_clampsAtOne() {
        var r = round
        repeat(5) { r = ScoreEditor.adjust(r, 0, 0, -1) }
        assertThat(r.scores[0][0]).isEqualTo(1)
    }

    @Test fun setScore_setsExactValue() {
        val r = ScoreEditor.set(round, 1, 0, 6)
        assertThat(r.scores[1][0]).isEqualTo(6)
    }

    @Test fun adjust_doesNotMutateOriginal() {
        ScoreEditor.adjust(round, 0, 0, +1)
        assertThat(round.scores[0][0]).isEqualTo(3)
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew testDebugUnitTest --tests "*.ScoreEditorTest"`
Expected: FAIL — `ScoreEditor` unresolved.

- [ ] **Step 3: Write minimal implementation**

`ScoreEditor.kt`:
```kotlin
package com.parkgolf.score.domain

import com.parkgolf.score.domain.model.Round

object ScoreEditor {
    private const val MIN_STROKES = 1

    fun set(round: Round, playerIndex: Int, holeIndex: Int, value: Int): Round {
        val newScores = round.scores.map { it.toMutableList() }
        newScores[playerIndex][holeIndex] = value.coerceAtLeast(MIN_STROKES)
        return round.copy(scores = newScores)
    }

    fun adjust(round: Round, playerIndex: Int, holeIndex: Int, delta: Int): Round {
        val current = round.scores[playerIndex][holeIndex] ?: 0
        return set(round, playerIndex, holeIndex, current + delta)
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew testDebugUnitTest --tests "*.ScoreEditorTest"`
Expected: PASS (4 tests).

- [ ] **Step 5: Commit**
```bash
git add app/src/main/java/com/parkgolf/score/domain/ScoreEditor.kt app/src/test/java/com/parkgolf/score/domain/ScoreEditorTest.kt
git commit -m "feat: add immutable ScoreEditor (adjust/set with clamp)"
```

---

## Phase 5 — Navigation host & Home

### Task 5.1: Navigation graph + MainActivity

**Files:**
- Create: `res/navigation/nav_graph.xml`, `res/layout/activity_main.xml`, `ui/MainActivity.kt`

- [ ] **Step 1: activity_main.xml**
```xml
<?xml version="1.0" encoding="utf-8"?>
<androidx.fragment.app.FragmentContainerView xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:id="@+id/nav_host"
    android:name="androidx.navigation.fragment.NavHostFragment"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    app:defaultNavHost="true"
    app:navGraph="@navigation/nav_graph" />
```

- [ ] **Step 2: nav_graph.xml (all destinations declared up front)**
```xml
<?xml version="1.0" encoding="utf-8"?>
<navigation xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:id="@+id/nav_graph"
    app:startDestination="@id/homeFragment">

    <fragment android:id="@+id/homeFragment"
        android:name="com.parkgolf.score.ui.home.HomeFragment" />
    <fragment android:id="@+id/startFragment"
        android:name="com.parkgolf.score.ui.start.StartFragment" />
    <fragment android:id="@+id/courseSetupFragment"
        android:name="com.parkgolf.score.ui.start.CourseSetupFragment" />
    <fragment android:id="@+id/playerSetupFragment"
        android:name="com.parkgolf.score.ui.start.PlayerSetupFragment" />
    <fragment android:id="@+id/holeInputFragment"
        android:name="com.parkgolf.score.ui.hole.HoleInputFragment" />
    <fragment android:id="@+id/gridViewFragment"
        android:name="com.parkgolf.score.ui.grid.GridViewFragment" />
    <fragment android:id="@+id/roundSummaryFragment"
        android:name="com.parkgolf.score.ui.summary.RoundSummaryFragment" />
    <fragment android:id="@+id/historyFragment"
        android:name="com.parkgolf.score.ui.history.HistoryFragment" />
    <fragment android:id="@+id/roundDetailFragment"
        android:name="com.parkgolf.score.ui.history.RoundDetailFragment" />
    <fragment android:id="@+id/myCoursesFragment"
        android:name="com.parkgolf.score.ui.courses.MyCoursesFragment" />
    <fragment android:id="@+id/courseEditFragment"
        android:name="com.parkgolf.score.ui.courses.CourseEditFragment" />
</navigation>
```

Note: This plan passes data between fragments via a **shared `RoundSessionViewModel`** (activity-scoped) rather than Safe Args, to keep the round-in-progress in one place. IDs above are referenced by `findNavController().navigate(R.id.<dest>)`.

- [ ] **Step 3: MainActivity.kt**
```kotlin
package com.parkgolf.score.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.parkgolf.score.R

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }
}
```

- [ ] **Step 4: Verify build**

Run: `./gradlew :app:assembleDebug`
Expected: FAIL — referenced fragment classes don't exist yet. That's expected; the next task adds Home and a temporary stub for the rest. To keep the build green, first create empty stub fragments for every destination.

- [ ] **Step 5: Create stub fragments for every destination**

For EACH of the 10 non-home destinations, create a file like:
```kotlin
package com.parkgolf.score.ui.start   // adjust package per file

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import com.parkgolf.score.R

class StartFragment : Fragment(R.layout.fragment_placeholder) {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {}
}
```
Create the matching class for each: `CourseSetupFragment`, `PlayerSetupFragment` (package `ui.start`); `HoleInputFragment` (`ui.hole`); `GridViewFragment` (`ui.grid`); `RoundSummaryFragment` (`ui.summary`); `HistoryFragment`, `RoundDetailFragment` (`ui.history`); `MyCoursesFragment`, `CourseEditFragment` (`ui.courses`). Each uses `R.layout.fragment_placeholder`.

Also create `res/layout/fragment_placeholder.xml`:
```xml
<?xml version="1.0" encoding="utf-8"?>
<androidx.constraintlayout.widget.ConstraintLayout
    xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="match_parent" />
```

- [ ] **Step 6: Build succeeds**

Run: `./gradlew :app:assembleDebug`
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 7: Commit**
```bash
git add app/src/main/java/com/parkgolf/score/ui/ app/src/main/res/navigation/ app/src/main/res/layout/activity_main.xml app/src/main/res/layout/fragment_placeholder.xml
git commit -m "feat: add single-activity nav host with destination stubs"
```

### Task 5.2: Shared RoundSession ViewModel

**Files:**
- Create: `app/src/main/java/com/parkgolf/score/ui/RoundSessionViewModel.kt`
- Test: `app/src/test/java/com/parkgolf/score/ui/RoundSessionViewModelTest.kt`

- [ ] **Step 1: Write the failing test**

`RoundSessionViewModelTest.kt`:
```kotlin
package com.parkgolf.score.ui

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.google.common.truth.Truth.assertThat
import com.parkgolf.score.domain.RoundFactory
import org.junit.Rule
import org.junit.Test

class RoundSessionViewModelTest {
    @get:Rule val instant = InstantTaskExecutorRule()

    private fun vmWithRound(): RoundSessionViewModel {
        val vm = RoundSessionViewModel()
        vm.startRound(
            RoundFactory.newRound(
                "V", listOf("나", "철수"),
                listOf(RoundFactory.CoursePars("A", List(9) { 3 })), 1L
            )
        )
        return vm
    }

    @Test fun adjustScore_updatesState() {
        val vm = vmWithRound()
        vm.adjust(playerIndex = 0, holeIndex = 0, delta = +1)
        assertThat(vm.round.value!!.scores[0][0]).isEqualTo(4)
    }

    @Test fun cumulativeRelative_reflectsScores() {
        val vm = vmWithRound()
        vm.adjust(0, 0, +1) // +1 vs par for player 0
        assertThat(vm.cumulativeRelative(0)).isEqualTo(1)
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew testDebugUnitTest --tests "*.RoundSessionViewModelTest"`
Expected: FAIL — class unresolved.

- [ ] **Step 3: Write minimal implementation**

`RoundSessionViewModel.kt`:
```kotlin
package com.parkgolf.score.ui

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.parkgolf.score.domain.ScoreEditor
import com.parkgolf.score.domain.Scoring
import com.parkgolf.score.domain.model.Round

/** Activity-scoped holder for the round currently being played/edited. */
class RoundSessionViewModel : ViewModel() {
    val round = MutableLiveData<Round?>(null)

    fun startRound(newRound: Round) { round.value = newRound }

    fun adjust(playerIndex: Int, holeIndex: Int, delta: Int) {
        val r = round.value ?: return
        round.value = ScoreEditor.adjust(r, playerIndex, holeIndex, delta)
    }

    fun cumulativeRelative(playerIndex: Int): Int {
        val r = round.value ?: return 0
        return Scoring.relativeToPar(r.scores[playerIndex], r.holes)
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew testDebugUnitTest --tests "*.RoundSessionViewModelTest"`
Expected: PASS (2 tests).

- [ ] **Step 5: Commit**
```bash
git add app/src/main/java/com/parkgolf/score/ui/RoundSessionViewModel.kt app/src/test/java/com/parkgolf/score/ui/RoundSessionViewModelTest.kt
git commit -m "feat: add activity-scoped RoundSessionViewModel"
```

### Task 5.3: Home screen

**Files:**
- Create: `res/layout/fragment_home.xml`
- Modify: `ui/home/HomeFragment.kt`

- [ ] **Step 1: fragment_home.xml (ConstraintLayout, large buttons, scrollable for big fonts)**
```xml
<?xml version="1.0" encoding="utf-8"?>
<ScrollView xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:fillViewport="true"
    android:padding="@dimen/screen_padding">

    <androidx.constraintlayout.widget.ConstraintLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content">

        <TextView
            android:id="@+id/title"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:text="@string/app_name"
            android:textSize="@dimen/title_text"
            android:textStyle="bold"
            android:layout_marginTop="24dp"
            app:layout_constraintTop_toTopOf="parent"
            app:layout_constraintStart_toStartOf="parent"
            app:layout_constraintEnd_toEndOf="parent" />

        <Button
            android:id="@+id/btnStart"
            android:layout_width="0dp"
            android:layout_height="wrap_content"
            android:minHeight="88dp"
            android:text="@string/start_round"
            android:textSize="@dimen/title_text"
            android:layout_marginTop="40dp"
            app:layout_constraintTop_toBottomOf="@id/title"
            app:layout_constraintStart_toStartOf="parent"
            app:layout_constraintEnd_toEndOf="parent" />

        <Button
            android:id="@+id/btnHistory"
            style="?attr/materialButtonOutlinedStyle"
            android:layout_width="0dp"
            android:layout_height="wrap_content"
            android:minHeight="@dimen/touch_min"
            android:text="@string/past_records"
            android:textSize="@dimen/button_text"
            android:layout_marginTop="16dp"
            app:layout_constraintTop_toBottomOf="@id/btnStart"
            app:layout_constraintStart_toStartOf="parent"
            app:layout_constraintEnd_toEndOf="parent" />

        <Button
            android:id="@+id/btnMyCourses"
            style="?attr/materialButtonOutlinedStyle"
            android:layout_width="0dp"
            android:layout_height="wrap_content"
            android:minHeight="@dimen/touch_min"
            android:text="@string/my_courses"
            android:textSize="@dimen/button_text"
            android:layout_marginTop="16dp"
            app:layout_constraintTop_toBottomOf="@id/btnHistory"
            app:layout_constraintStart_toStartOf="parent"
            app:layout_constraintEnd_toEndOf="parent" />

    </androidx.constraintlayout.widget.ConstraintLayout>
</ScrollView>
```

- [ ] **Step 2: HomeFragment.kt with in-progress resume check**
```kotlin
package com.parkgolf.score.ui.home

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.parkgolf.score.App
import com.parkgolf.score.R
import com.parkgolf.score.databinding.FragmentHomeBinding
import com.parkgolf.score.ui.RoundSessionViewModel
import androidx.fragment.app.activityViewModels
import kotlinx.coroutines.launch

class HomeFragment : Fragment(R.layout.fragment_home) {
    private val session: RoundSessionViewModel by activityViewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val binding = FragmentHomeBinding.bind(view)
        val repo = App.repo(requireActivity().application)

        binding.btnStart.setOnClickListener {
            findNavController().navigate(R.id.startFragment)
        }
        binding.btnHistory.setOnClickListener {
            findNavController().navigate(R.id.historyFragment)
        }
        binding.btnMyCourses.setOnClickListener {
            findNavController().navigate(R.id.myCoursesFragment)
        }

        // Offer to resume an in-progress round (crash/exit recovery).
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
}
```

- [ ] **Step 3: Build & manual smoke**

Run: `./gradlew :app:assembleDebug`
Expected: `BUILD SUCCESSFUL`. Install on a device (`./gradlew installDebug`) and confirm Home shows three large buttons; tapping navigates to (still placeholder) screens.

- [ ] **Step 4: Commit**
```bash
git add app/src/main/res/layout/fragment_home.xml app/src/main/java/com/parkgolf/score/ui/home/HomeFragment.kt
git commit -m "feat: add Home screen with resume-round prompt"
```

---

## Phase 6 — Round start flow

### Task 6.1: StartViewModel — recent courses + presets

**Files:**
- Create: `ui/start/StartViewModel.kt`, `ui/start/RecentCourse.kt`
- Test: `app/src/test/java/com/parkgolf/score/ui/StartViewModelTest.kt`

- [ ] **Step 1: Write RecentCourse model + failing test**

`RecentCourse.kt`:
```kotlin
package com.parkgolf.score.ui.start

data class RecentCourse(val venueName: String, val holeCount: Int, val lastPlayed: Long)
```

`StartViewModelTest.kt`:
```kotlin
package com.parkgolf.score.ui

import com.google.common.truth.Truth.assertThat
import com.parkgolf.score.domain.model.HoleSpec
import com.parkgolf.score.domain.model.Round
import com.parkgolf.score.domain.model.RoundStatus
import com.parkgolf.score.ui.start.StartViewModel
import org.junit.Test

class StartViewModelTest {
    private fun round(venue: String, holes: Int, date: Long) = Round(
        id = 0, date = date, venueName = venue, players = listOf("나"),
        holes = List(holes) { HoleSpec("A", it + 1, 3) },
        scores = listOf(List(holes) { 3 as Int? }), status = RoundStatus.COMPLETED
    )

    @Test fun recentCourses_areDistinctByVenue_newestFirst() {
        val completed = listOf(
            round("○○", 18, 300),
            round("△△", 9, 200),
            round("○○", 9, 100)   // older duplicate venue -> dropped
        )
        val recents = StartViewModel.deriveRecentCourses(completed, limit = 5)
        assertThat(recents.map { it.venueName }).containsExactly("○○", "△△").inOrder()
        assertThat(recents[0].holeCount).isEqualTo(18) // from newest ○○ round
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew testDebugUnitTest --tests "*.StartViewModelTest"`
Expected: FAIL — `StartViewModel` unresolved.

- [ ] **Step 3: Write StartViewModel**

`StartViewModel.kt`:
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
                .map { RecentCourse(it.venueName, it.holes.size, it.date) }
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew testDebugUnitTest --tests "*.StartViewModelTest"`
Expected: PASS (1 test).

- [ ] **Step 5: Commit**
```bash
git add app/src/main/java/com/parkgolf/score/ui/start/
git commit -m "feat: derive recent-course list for start screen"
```

### Task 6.2: Start / CourseSetup / PlayerSetup screens

**Files:**
- Create layouts: `fragment_start.xml`, `item_recent_course.xml`, `item_preset_course.xml`, `fragment_course_setup.xml`, `fragment_player_setup.xml`
- Modify fragments: `StartFragment.kt`, `CourseSetupFragment.kt`, `PlayerSetupFragment.kt`

This task wires the selection → hole-count → players → `session.startRound(...)` → navigate to hole input. It is UI-heavy; implement it in these sub-steps, committing once at the end.

- [ ] **Step 1: fragment_start.xml**

A `ScrollView` → `ConstraintLayout` containing: section header "최근 구장" (`@string/recent_courses`), a `RecyclerView` `rvRecent`; section header "내 구장" (`@string/saved_presets`), a `RecyclerView` `rvPresets`; a disabled `TextView` `@string/nearby_courses_soon` with dashed background; a `Button` `btnNewCourse` (`@string/new_course`). Use `android:minHeight="@dimen/touch_min"` on interactive rows and `wrap_content` heights so large system fonts expand rows.

- [ ] **Step 2: item_recent_course.xml / item_preset_course.xml**

`item_recent_course.xml`: a `MaterialCardView` with green stroke containing two `TextView`s — `tvVenue` (venue + "· N홀", `textSize=@dimen/body_text`, bold) and `tvSub` (date + players, `text_muted`). `item_preset_course.xml`: a single-line row `TextView` `tvPreset` with `minHeight=@dimen/touch_min`.

- [ ] **Step 3: StartFragment.kt**
```kotlin
package com.parkgolf.score.ui.start

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.parkgolf.score.App
import com.parkgolf.score.R
import com.parkgolf.score.databinding.FragmentStartBinding
import kotlinx.coroutines.launch

class StartFragment : Fragment(R.layout.fragment_start) {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val binding = FragmentStartBinding.bind(view)
        val repo = App.repo(requireActivity().application)

        binding.rvRecent.layoutManager = LinearLayoutManager(requireContext())
        binding.rvPresets.layoutManager = LinearLayoutManager(requireContext())

        val recentAdapter = RecentCourseAdapter { recent ->
            // Quick resume path: prefill setup with this venue's most recent config.
            SelectionHolder.venueName = recent.venueName
            findNavController().navigate(R.id.courseSetupFragment)
        }
        binding.rvRecent.adapter = recentAdapter

        val presetAdapter = PresetCourseAdapter { venueId ->
            SelectionHolder.venueId = venueId
            findNavController().navigate(R.id.courseSetupFragment)
        }
        binding.rvPresets.adapter = presetAdapter

        binding.btnNewCourse.setOnClickListener {
            SelectionHolder.reset()
            findNavController().navigate(R.id.courseEditFragment)
        }

        viewLifecycleOwner.lifecycleScope.launch {
            val recents = StartViewModel.deriveRecentCourses(repo.completedRounds(), limit = 5)
            recentAdapter.submit(recents)
        }
        // presets: observe venues+courses
        viewLifecycleOwner.lifecycleScope.launch {
            repo.observeVenues().collect { venues -> presetAdapter.submit(venues) }
        }
    }
}
```

Create the two `RecyclerView.Adapter` classes (`RecentCourseAdapter`, `PresetCourseAdapter`) in the same package, each a straightforward `ListAdapter`/simple adapter binding the item layouts above; the recent adapter's click lambda passes the `RecentCourse`, the preset adapter's passes `venueId: Long`.

Create `SelectionHolder.kt` (in `ui.start`) — a plain object carrying the in-flight selection between the three setup fragments:
```kotlin
package com.parkgolf.score.ui.start

object SelectionHolder {
    var venueId: Long? = null
    var venueName: String? = null
    var chosenCourseIds: MutableList<Long> = mutableListOf() // ordered courses to play
    var players: MutableList<String> = mutableListOf("나")
    fun reset() { venueId = null; venueName = null; chosenCourseIds.clear(); players = mutableListOf("나") }
}
```

- [ ] **Step 4: fragment_course_setup.xml + CourseSetupFragment.kt**

Layout: title "홀 구성", a `RecyclerView` `rvCourses` listing that venue's 9-hole courses each as a toggle chip/checkbox (tap to add to `chosenCourseIds` in order; show running total holes), a large `TextView` `tvTotalHoles` ("총 18홀"), and a `Button` `btnNext` → PlayerSetup.

`CourseSetupFragment.kt` core logic:
```kotlin
// on create: load courses for SelectionHolder.venueId (if null, this is a "recent" quick path
// — resolve venueId by matching SelectionHolder.venueName via repo.observeVenues()).
// Each course toggle appends/removes course.id in SelectionHolder.chosenCourseIds (preserve order).
// tvTotalHoles = sum of pars sizes of chosen courses.
// btnNext enabled only when chosenCourseIds is non-empty; navigate(R.id.playerSetupFragment).
```

- [ ] **Step 5: fragment_player_setup.xml + PlayerSetupFragment.kt**

Layout: title "함께 하는 사람", up to 4 rows of `EditText` (first prefilled "나"), a `btnAddPlayer` (disabled at 4), a large `btnStartPlay`.

`PlayerSetupFragment.kt` on start tap:
```kotlin
// Build RoundFactory.CoursePars list from SelectionHolder.chosenCourseIds:
//   for each id -> find CourseEntity -> CoursePars(course.name, course.pars)
// venueName = SelectionHolder.venueName ?: venue looked up by venueId.
// players = non-blank EditText values (fallback "동반자N" for blanks after the first).
val round = RoundFactory.newRound(venueName, players, coursePars, System.currentTimeMillis())
// persist immediately so it becomes the recoverable in-progress round:
val id = repo.saveRound(round)
session.startRound(round.copy(id = id))
findNavController().navigate(R.id.holeInputFragment)
```

- [ ] **Step 6: Build**

Run: `./gradlew :app:assembleDebug`
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 7: Commit**
```bash
git add app/src/main/java/com/parkgolf/score/ui/start/ app/src/main/res/layout/fragment_start.xml app/src/main/res/layout/fragment_course_setup.xml app/src/main/res/layout/fragment_player_setup.xml app/src/main/res/layout/item_recent_course.xml app/src/main/res/layout/item_preset_course.xml
git commit -m "feat: add round start flow (select course, holes, players)"
```

---

## Phase 7 — Hole input (core screen)

### Task 7.1: Per-player row view + HoleInput layout

**Files:**
- Create: `res/layout/fragment_hole_input.xml`, `res/layout/view_player_score_row.xml`

- [ ] **Step 1: view_player_score_row.xml (the − [score] + control)**
```xml
<?xml version="1.0" encoding="utf-8"?>
<androidx.constraintlayout.widget.ConstraintLayout
    xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:background="@color/surface_muted"
    android:padding="12dp"
    android:layout_marginBottom="10dp">

    <TextView
        android:id="@+id/tvPlayer"
        android:layout_width="0dp"
        android:layout_height="wrap_content"
        android:textSize="@dimen/body_text"
        android:textStyle="bold"
        app:layout_constraintStart_toStartOf="parent"
        app:layout_constraintTop_toTopOf="parent"
        app:layout_constraintBottom_toBottomOf="parent"
        app:layout_constraintEnd_toStartOf="@id/btnMinus" />

    <Button
        android:id="@+id/btnMinus"
        android:layout_width="@dimen/stepper_size"
        android:layout_height="@dimen/stepper_size"
        android:text="−"
        android:textSize="28sp"
        android:contentDescription="@string/decrease"
        app:layout_constraintEnd_toStartOf="@id/tvScore"
        app:layout_constraintTop_toTopOf="parent" />

    <TextView
        android:id="@+id/tvScore"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:minWidth="48dp"
        android:gravity="center"
        android:textSize="@dimen/score_text"
        android:textStyle="bold"
        app:layout_constraintEnd_toStartOf="@id/btnPlus"
        app:layout_constraintTop_toTopOf="@id/btnMinus"
        app:layout_constraintBottom_toBottomOf="@id/btnMinus" />

    <Button
        android:id="@+id/btnPlus"
        android:layout_width="@dimen/stepper_size"
        android:layout_height="@dimen/stepper_size"
        android:text="＋"
        android:textSize="28sp"
        android:contentDescription="@string/increase"
        app:layout_constraintEnd_toEndOf="parent"
        app:layout_constraintTop_toTopOf="parent" />
</androidx.constraintlayout.widget.ConstraintLayout>
```

- [ ] **Step 2: fragment_hole_input.xml**

`ScrollView` → `ConstraintLayout` with: top bar (`tvVenue` start, `tvCumulative` end), `tvHole` ("N번 홀", large bold), `tvPar` ("파 P", green), a vertical `LinearLayout` `playerContainer` (rows injected at runtime), a bottom bar with `btnPrev` (weight 1) and `btnNext` (weight 2), and a text button `btnGrid` (`@string/full_grid`). Give `btnNext` `minHeight="@dimen/touch_min"`.

- [ ] **Step 3: Commit**
```bash
git add app/src/main/res/layout/view_player_score_row.xml app/src/main/res/layout/fragment_hole_input.xml
git commit -m "feat: add hole input layouts (player score row + screen)"
```

### Task 7.2: HoleInputViewModel

**Files:**
- Create: `ui/hole/HoleInputViewModel.kt`
- Test: `app/src/test/java/com/parkgolf/score/ui/HoleInputViewModelTest.kt`

- [ ] **Step 1: Write the failing test**

`HoleInputViewModelTest.kt`:
```kotlin
package com.parkgolf.score.ui

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.google.common.truth.Truth.assertThat
import com.parkgolf.score.ui.hole.HoleInputViewModel
import org.junit.Rule
import org.junit.Test

class HoleInputViewModelTest {
    @get:Rule val instant = InstantTaskExecutorRule()

    @Test fun navigation_clampsToHoleRange() {
        val vm = HoleInputViewModel(totalHoles = 9)
        assertThat(vm.holeIndex.value).isEqualTo(0)
        vm.prev() // can't go below 0
        assertThat(vm.holeIndex.value).isEqualTo(0)
        repeat(20) { vm.next() }
        assertThat(vm.holeIndex.value).isEqualTo(8) // last hole (0-based)
    }

    @Test fun isLastHole_trueOnFinalHole() {
        val vm = HoleInputViewModel(totalHoles = 3)
        assertThat(vm.isLastHole()).isFalse()
        vm.next(); vm.next()
        assertThat(vm.isLastHole()).isTrue()
    }

    @Test fun goTo_jumpsToIndex() {
        val vm = HoleInputViewModel(totalHoles = 9)
        vm.goTo(5)
        assertThat(vm.holeIndex.value).isEqualTo(5)
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew testDebugUnitTest --tests "*.HoleInputViewModelTest"`
Expected: FAIL — class unresolved.

- [ ] **Step 3: Write minimal implementation**

`HoleInputViewModel.kt`:
```kotlin
package com.parkgolf.score.ui.hole

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class HoleInputViewModel(private val totalHoles: Int) : ViewModel() {
    val holeIndex = MutableLiveData(0)

    fun next() { holeIndex.value = (holeIndex.value!! + 1).coerceAtMost(totalHoles - 1) }
    fun prev() { holeIndex.value = (holeIndex.value!! - 1).coerceAtLeast(0) }
    fun goTo(index: Int) { holeIndex.value = index.coerceIn(0, totalHoles - 1) }
    fun isLastHole(): Boolean = holeIndex.value == totalHoles - 1
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew testDebugUnitTest --tests "*.HoleInputViewModelTest"`
Expected: PASS (3 tests).

- [ ] **Step 5: Commit**
```bash
git add app/src/main/java/com/parkgolf/score/ui/hole/HoleInputViewModel.kt app/src/test/java/com/parkgolf/score/ui/HoleInputViewModelTest.kt
git commit -m "feat: add HoleInputViewModel with clamped hole navigation"
```

### Task 7.3: HoleInputFragment — wire it all together

**Files:**
- Modify: `ui/hole/HoleInputFragment.kt`
- Create: `ui/hole/ParColors.kt`

- [ ] **Step 1: ParColors helper (color+text pairing)**
```kotlin
package com.parkgolf.score.ui.hole

import android.content.Context
import androidx.core.content.ContextCompat
import com.parkgolf.score.R
import com.parkgolf.score.domain.ParRelation
import com.parkgolf.score.domain.Scoring

object ParColors {
    fun colorFor(ctx: Context, diff: Int): Int {
        val res = when (Scoring.relation(diff)) {
            ParRelation.UNDER -> R.color.under_par
            ParRelation.EVEN -> R.color.even_par
            ParRelation.OVER -> R.color.over_par
        }
        return ContextCompat.getColor(ctx, res)
    }
    fun label(diff: Int): String = Scoring.relationLabel(diff)
}
```
(Note: `Scoring.relation`/`relationLabel` are the functions defined in Task 1.2.)

- [ ] **Step 2: HoleInputFragment.kt**
```kotlin
package com.parkgolf.score.ui.hole

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.parkgolf.score.App
import com.parkgolf.score.R
import com.parkgolf.score.databinding.FragmentHoleInputBinding
import com.parkgolf.score.databinding.ViewPlayerScoreRowBinding
import com.parkgolf.score.domain.Scoring
import com.parkgolf.score.ui.RoundSessionViewModel
import kotlinx.coroutines.launch

class HoleInputFragment : Fragment(R.layout.fragment_hole_input) {
    private val session: RoundSessionViewModel by activityViewModels()
    private lateinit var holeVm: HoleInputViewModel

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val binding = FragmentHoleInputBinding.bind(view)
        val repo = App.repo(requireActivity().application)
        val round = session.round.value ?: run { findNavController().popBackStack(); return }
        holeVm = HoleInputViewModel(totalHoles = round.holes.size)

        // Build one row per player once.
        val rows = round.players.indices.map { p ->
            val rowBinding = ViewPlayerScoreRowBinding.inflate(layoutInflater, binding.playerContainer, false)
            rowBinding.tvPlayer.text = round.players[p]
            rowBinding.btnPlus.setOnClickListener {
                session.adjust(p, holeVm.holeIndex.value!!, +1); persist(repo); render(binding, rows)
            }
            rowBinding.btnMinus.setOnClickListener {
                session.adjust(p, holeVm.holeIndex.value!!, -1); persist(repo); render(binding, rows)
            }
            binding.playerContainer.addView(rowBinding.root)
            rowBinding
        }

        binding.btnNext.setOnClickListener {
            if (holeVm.isLastHole()) {
                findNavController().navigate(R.id.roundSummaryFragment)
            } else { holeVm.next(); render(binding, rows) }
        }
        binding.btnPrev.setOnClickListener { holeVm.prev(); render(binding, rows) }
        binding.btnGrid.setOnClickListener { findNavController().navigate(R.id.gridViewFragment) }

        render(binding, rows)
    }

    private fun persist(repo: com.parkgolf.score.data.ParkGolfRepository) {
        val r = session.round.value ?: return
        viewLifecycleOwner.lifecycleScope.launch { repo.saveRound(r) }
    }

    private fun render(binding: FragmentHoleInputBinding, rows: List<ViewPlayerScoreRowBinding>) {
        val round = session.round.value ?: return
        val h = holeVm.holeIndex.value!!
        val hole = round.holes[h]
        binding.tvVenue.text = round.venueName
        binding.tvHole.text = getString(R.string.hole_label, hole.holeNo)
        binding.tvPar.text = getString(R.string.par_label, hole.par)
        binding.btnNext.text = if (holeVm.isLastHole()) getString(R.string.save) else getString(R.string.next_hole)

        round.players.indices.forEach { p ->
            val score = round.scores[p][h]
            rows[p].tvScore.text = score?.toString() ?: "-"
            val cumulative = Scoring.relativeToPar(round.scores[p], round.holes)
            // cumulative label shown only on first row's top bar for the active player set:
        }
        // Cumulative for player 0 in the top bar (primary user "나").
        val cum0 = Scoring.relativeToPar(round.scores[0], round.holes)
        binding.tvCumulative.text = getString(R.string.cumulative_label, Scoring.relationLabel(cum0))
        binding.tvCumulative.setTextColor(ParColors.colorFor(requireContext(), cum0))
    }
}
```

- [ ] **Step 3: Build & manual smoke**

Run: `./gradlew :app:assembleDebug && ./gradlew installDebug`
Expected: BUILD SUCCESSFUL. On device: start a round, verify each hole shows par-initialized scores, ＋/− change values and persist, "다음 홀" advances, last hole button reads "저장" and goes to summary.

- [ ] **Step 4: Commit**
```bash
git add app/src/main/java/com/parkgolf/score/ui/hole/
git commit -m "feat: wire hole input screen (stepper, par colors, persistence)"
```

---

## Phase 8 — Grid view (read-only, tap-to-edit)

### Task 8.1: GridView screen

**Files:**
- Create: `res/layout/fragment_grid_view.xml`, `ui/grid/GridViewFragment.kt`

- [ ] **Step 1: fragment_grid_view.xml**

Horizontal + vertical scrolling table. Use a `HorizontalScrollView` → `TableLayout` `tableGrid` inside a vertical `ScrollView`. Header row: "홀" + one column per player. One `TableRow` per hole showing "N(파P)" then each player's score; tap a row navigates back to that hole. A final totals `TableRow` shows each player's total via `Scoring.total`.

- [ ] **Step 2: GridViewFragment.kt**
```kotlin
package com.parkgolf.score.ui.grid

import android.os.Bundle
import android.view.View
import android.widget.TableRow
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.parkgolf.score.R
import com.parkgolf.score.databinding.FragmentGridViewBinding
import com.parkgolf.score.domain.Scoring
import com.parkgolf.score.ui.RoundSessionViewModel

class GridViewFragment : Fragment(R.layout.fragment_grid_view) {
    private val session: RoundSessionViewModel by activityViewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val binding = FragmentGridViewBinding.bind(view)
        val round = session.round.value ?: run { findNavController().popBackStack(); return }

        fun cell(text: String) = TextView(requireContext()).apply {
            this.text = text; setPadding(24, 20, 24, 20); textSize = 16f
        }

        // Header
        val header = TableRow(requireContext())
        header.addView(cell("홀"))
        round.players.forEach { header.addView(cell(it)) }
        binding.tableGrid.addView(header)

        // Hole rows
        round.holes.forEachIndexed { h, hole ->
            val row = TableRow(requireContext())
            row.addView(cell("${hole.holeNo}(파${hole.par})"))
            round.players.indices.forEach { p ->
                row.addView(cell(round.scores[p][h]?.toString() ?: "-"))
            }
            row.setOnClickListener {
                // Jump back to hole input at this hole.
                findNavController().previousBackStackEntry
                    ?.savedStateHandle?.set("jumpToHole", h)
                findNavController().popBackStack()
            }
            binding.tableGrid.addView(row)
        }

        // Totals
        val totals = TableRow(requireContext())
        totals.addView(cell("합계"))
        round.players.indices.forEach { p ->
            totals.addView(cell(Scoring.total(round.scores[p]).toString()))
        }
        binding.tableGrid.addView(totals)
    }
}
```

In `HoleInputFragment.onViewCreated`, after building rows, observe the jump request:
```kotlin
findNavController().currentBackStackEntry?.savedStateHandle
    ?.getLiveData<Int>("jumpToHole")?.observe(viewLifecycleOwner) { idx ->
        holeVm.goTo(idx); render(binding, rows)
    }
```

- [ ] **Step 3: Build & smoke**

Run: `./gradlew :app:assembleDebug`
Expected: BUILD SUCCESSFUL. On device: "전체 보기" shows all holes/players + totals; tapping a hole row returns to that hole in the input screen.

- [ ] **Step 4: Commit**
```bash
git add app/src/main/res/layout/fragment_grid_view.xml app/src/main/java/com/parkgolf/score/ui/grid/GridViewFragment.kt app/src/main/java/com/parkgolf/score/ui/hole/HoleInputFragment.kt
git commit -m "feat: add read-only grid view with tap-to-edit jump"
```

---

## Phase 9 — Round summary & save

### Task 9.1: SummaryViewModel — ranking

**Files:**
- Create: `ui/summary/SummaryViewModel.kt`
- Test: `app/src/test/java/com/parkgolf/score/ui/SummaryViewModelTest.kt`

- [ ] **Step 1: Write the failing test**

`SummaryViewModelTest.kt`:
```kotlin
package com.parkgolf.score.ui

import com.google.common.truth.Truth.assertThat
import com.parkgolf.score.domain.model.HoleSpec
import com.parkgolf.score.domain.model.Round
import com.parkgolf.score.domain.model.RoundStatus
import com.parkgolf.score.ui.summary.SummaryViewModel
import org.junit.Test

class SummaryViewModelTest {
    private val round = Round(
        date = 1L, venueName = "V", players = listOf("나", "철수", "영희"),
        holes = listOf(HoleSpec("A", 1, 3), HoleSpec("A", 2, 3)),
        scores = listOf(listOf(3, 4), listOf(3, 3), listOf(5, 5)),
        status = RoundStatus.IN_PROGRESS
    )

    @Test fun ranking_ordersByTotalAscending() {
        val rows = SummaryViewModel.ranking(round)
        assertThat(rows.map { it.player }).containsExactly("철수", "나", "영희").inOrder()
        assertThat(rows[0].total).isEqualTo(6)
        assertThat(rows[0].rank).isEqualTo(1)
    }

    @Test fun ranking_tiesShareRank() {
        val tie = round.copy(scores = listOf(listOf(3, 3), listOf(3, 3), listOf(5, 5)))
        val rows = SummaryViewModel.ranking(tie)
        assertThat(rows[0].rank).isEqualTo(1)
        assertThat(rows[1].rank).isEqualTo(1)
        assertThat(rows[2].rank).isEqualTo(3)
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew testDebugUnitTest --tests "*.SummaryViewModelTest"`
Expected: FAIL — class unresolved.

- [ ] **Step 3: Write minimal implementation**

`SummaryViewModel.kt`:
```kotlin
package com.parkgolf.score.ui.summary

import androidx.lifecycle.ViewModel
import com.parkgolf.score.domain.Scoring
import com.parkgolf.score.domain.model.Round

data class RankRow(val rank: Int, val player: String, val total: Int, val relative: Int)

class SummaryViewModel : ViewModel() {
    companion object {
        fun ranking(round: Round): List<RankRow> {
            val base = round.players.indices.map { p ->
                Triple(
                    round.players[p],
                    Scoring.total(round.scores[p]),
                    Scoring.relativeToPar(round.scores[p], round.holes)
                )
            }.sortedBy { it.second }

            var lastTotal = Int.MIN_VALUE
            var lastRank = 0
            return base.mapIndexed { i, (name, total, rel) ->
                val rank = if (total == lastTotal) lastRank else (i + 1).also { lastRank = it; }
                lastTotal = total
                RankRow(rank, name, total, rel)
            }
        }
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew testDebugUnitTest --tests "*.SummaryViewModelTest"`
Expected: PASS (2 tests).

- [ ] **Step 5: Commit**
```bash
git add app/src/main/java/com/parkgolf/score/ui/summary/SummaryViewModel.kt app/src/test/java/com/parkgolf/score/ui/SummaryViewModelTest.kt
git commit -m "feat: add round summary ranking (ties share rank)"
```

### Task 9.2: RoundSummaryFragment — mark completed & save

**Files:**
- Create: `res/layout/fragment_round_summary.xml`, `res/layout/item_rank_row.xml`
- Modify: `ui/summary/RoundSummaryFragment.kt`

- [ ] **Step 1: Layouts**

`fragment_round_summary.xml`: title "라운드 완료", `tvVenue`, a `RecyclerView`/`LinearLayout` `rankContainer`, a `btnDone` ("홈으로"). `item_rank_row.xml`: `tvRank`, `tvPlayer`, `tvTotal`, `tvRelative` (colored via ParColors).

- [ ] **Step 2: RoundSummaryFragment.kt**
```kotlin
package com.parkgolf.score.ui.summary

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.parkgolf.score.App
import com.parkgolf.score.R
import com.parkgolf.score.databinding.FragmentRoundSummaryBinding
import com.parkgolf.score.domain.model.RoundStatus
import com.parkgolf.score.ui.RoundSessionViewModel
import kotlinx.coroutines.launch

class RoundSummaryFragment : Fragment(R.layout.fragment_round_summary) {
    private val session: RoundSessionViewModel by activityViewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val binding = FragmentRoundSummaryBinding.bind(view)
        val repo = App.repo(requireActivity().application)
        val round = session.round.value ?: run { findNavController().popBackStack(); return }

        binding.tvVenue.text = round.venueName
        val rows = SummaryViewModel.ranking(round)
        // inflate item_rank_row per row into rankContainer, coloring tvRelative with ParColors.

        // Mark completed and persist.
        val completed = round.copy(status = RoundStatus.COMPLETED)
        viewLifecycleOwner.lifecycleScope.launch { repo.saveRound(completed) }
        session.startRound(completed)

        binding.btnDone.setOnClickListener {
            session.round.value = null
            findNavController().popBackStack(R.id.homeFragment, false)
        }
    }
}
```

- [ ] **Step 3: Build & smoke**

Run: `./gradlew :app:assembleDebug`
Expected: BUILD SUCCESSFUL. On device: finishing the last hole shows ranked totals; "홈으로" clears session and the round now appears in history.

- [ ] **Step 4: Commit**
```bash
git add app/src/main/java/com/parkgolf/score/ui/summary/RoundSummaryFragment.kt app/src/main/res/layout/fragment_round_summary.xml app/src/main/res/layout/item_rank_row.xml
git commit -m "feat: add round summary screen; mark round completed on finish"
```

---

## Phase 10 — History & stats

### Task 10.1: HistoryViewModel — list rows + stats bar

**Files:**
- Create: `ui/history/HistoryViewModel.kt`
- Test: `app/src/test/java/com/parkgolf/score/ui/HistoryViewModelTest.kt`

- [ ] **Step 1: Write the failing test**

`HistoryViewModelTest.kt`:
```kotlin
package com.parkgolf.score.ui

import com.google.common.truth.Truth.assertThat
import com.parkgolf.score.domain.model.HoleSpec
import com.parkgolf.score.domain.model.Round
import com.parkgolf.score.domain.model.RoundStatus
import com.parkgolf.score.ui.history.HistoryViewModel
import org.junit.Test

class HistoryViewModelTest {
    private fun round(total: Int, date: Long): Round {
        // player "나" total == `total` across 2 par-3 holes
        val a = total / 2
        val b = total - a
        return Round(
            date = date, venueName = "V", players = listOf("나"),
            holes = listOf(HoleSpec("A", 1, 3), HoleSpec("A", 2, 3)),
            scores = listOf(listOf(a, b)), status = RoundStatus.COMPLETED
        )
    }

    @Test fun stats_computeFromPrimaryPlayerTotals() {
        val rounds = listOf(round(38, 300), round(44, 200), round(40, 100))
        val stats = HistoryViewModel.computeStats(rounds, recentN = 10)
        assertThat(stats.recentAverage).isWithin(0.01).of(40.67)
        assertThat(stats.best).isEqualTo(38)
        assertThat(stats.worst).isEqualTo(44)
    }

    @Test fun stats_emptyIsNulls() {
        val stats = HistoryViewModel.computeStats(emptyList(), recentN = 10)
        assertThat(stats.recentAverage).isNull()
        assertThat(stats.best).isNull()
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew testDebugUnitTest --tests "*.HistoryViewModelTest"`
Expected: FAIL — class unresolved.

- [ ] **Step 3: Write minimal implementation**

`HistoryViewModel.kt`:
```kotlin
package com.parkgolf.score.ui.history

import androidx.lifecycle.ViewModel
import com.parkgolf.score.domain.Scoring
import com.parkgolf.score.domain.Stats
import com.parkgolf.score.domain.model.Round

data class HistoryStats(val recentAverage: Double?, val best: Int?, val worst: Int?)

class HistoryViewModel : ViewModel() {
    companion object {
        /** Stats use the primary player's (index 0, "나") total per round. */
        fun computeStats(rounds: List<Round>, recentN: Int): HistoryStats {
            val totals = rounds.sortedByDescending { it.date }
                .map { Scoring.total(it.scores[0]) }
            return HistoryStats(
                recentAverage = Stats.recentAverageStrokes(totals, recentN),
                best = Stats.best(totals),
                worst = Stats.worst(totals)
            )
        }
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew testDebugUnitTest --tests "*.HistoryViewModelTest"`
Expected: PASS (2 tests).

- [ ] **Step 5: Commit**
```bash
git add app/src/main/java/com/parkgolf/score/ui/history/HistoryViewModel.kt app/src/test/java/com/parkgolf/score/ui/HistoryViewModelTest.kt
git commit -m "feat: add history stats computation (recent avg, best, worst)"
```

### Task 10.2: History list + detail + delete

**Files:**
- Create: `res/layout/fragment_history.xml`, `res/layout/item_history_round.xml`, `res/layout/fragment_round_detail.xml`
- Modify: `ui/history/HistoryFragment.kt`, `ui/history/RoundDetailFragment.kt`

- [ ] **Step 1: fragment_history.xml + item + HistoryFragment**

`fragment_history.xml`: a stats bar at top (`tvAvg`, `tvBest`, `tvWorst`) + `RecyclerView` `rvRounds`. `item_history_round.xml`: `tvDate` (format with `java.text.SimpleDateFormat("yyyy.MM.dd", Locale.KOREA)`), `tvVenue`, `tvTotal`.

`HistoryFragment.kt`:
```kotlin
package com.parkgolf.score.ui.history

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.parkgolf.score.App
import com.parkgolf.score.R
import com.parkgolf.score.databinding.FragmentHistoryBinding
import kotlinx.coroutines.launch

class HistoryFragment : Fragment(R.layout.fragment_history) {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val binding = FragmentHistoryBinding.bind(view)
        val repo = App.repo(requireActivity().application)
        binding.rvRounds.layoutManager = LinearLayoutManager(requireContext())
        val adapter = HistoryAdapter { roundId ->
            findNavController().currentBackStackEntry?.savedStateHandle?.set("roundId", roundId)
            findNavController().navigate(R.id.roundDetailFragment)
        }
        binding.rvRounds.adapter = adapter

        viewLifecycleOwner.lifecycleScope.launch {
            repo.observeCompletedRounds().collect { rounds ->
                adapter.submit(rounds)
                val stats = HistoryViewModel.computeStats(rounds, recentN = 10)
                binding.tvAvg.text = stats.recentAverage?.let { "평균 %.1f타".format(it) } ?: "평균 -"
                binding.tvBest.text = stats.best?.let { "최저 ${it}타" } ?: "최저 -"
                binding.tvWorst.text = stats.worst?.let { "최고 ${it}타" } ?: "최고 -"
            }
        }
    }
}
```
Create `HistoryAdapter` (ListAdapter) binding `item_history_round.xml`; click lambda passes `round.id`.

- [ ] **Step 2: RoundDetailFragment — reuse grid + delete**

`fragment_round_detail.xml`: a `TableLayout` `tableGrid` (same structure as grid view) + a `btnDelete` (`@string/delete_round`).

`RoundDetailFragment.kt`:
```kotlin
package com.parkgolf.score.ui.history

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.parkgolf.score.App
import com.parkgolf.score.R
import com.parkgolf.score.databinding.FragmentRoundDetailBinding
import kotlinx.coroutines.launch

class RoundDetailFragment : Fragment(R.layout.fragment_round_detail) {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val binding = FragmentRoundDetailBinding.bind(view)
        val repo = App.repo(requireActivity().application)
        val roundId = findNavController().previousBackStackEntry
            ?.savedStateHandle?.get<Long>("roundId") ?: run { findNavController().popBackStack(); return }

        viewLifecycleOwner.lifecycleScope.launch {
            val round = repo.getRound(roundId) ?: return@launch
            // populate binding.tableGrid exactly like GridViewFragment (header, hole rows, totals) — no tap-to-edit.
            binding.btnDelete.setOnClickListener {
                MaterialAlertDialogBuilder(requireContext())
                    .setMessage(R.string.delete_confirm)
                    .setNegativeButton(R.string.cancel, null)
                    .setPositiveButton(R.string.delete) { _, _ ->
                        viewLifecycleOwner.lifecycleScope.launch {
                            repo.deleteRound(roundId)
                            findNavController().popBackStack()
                        }
                    }.show()
            }
        }
    }
}
```

- [ ] **Step 3: Build & smoke**

Run: `./gradlew :app:assembleDebug`
Expected: BUILD SUCCESSFUL. On device: history shows saved rounds + stats bar; opening one shows the grid; delete asks for confirmation then removes it.

- [ ] **Step 4: Commit**
```bash
git add app/src/main/java/com/parkgolf/score/ui/history/ app/src/main/res/layout/fragment_history.xml app/src/main/res/layout/item_history_round.xml app/src/main/res/layout/fragment_round_detail.xml
git commit -m "feat: add history list, stats bar, round detail with delete"
```

---

## Phase 11 — My courses management

### Task 11.1: CoursesViewModel + screens

**Files:**
- Create: `ui/courses/CoursesViewModel.kt`
- Create layouts: `fragment_my_courses.xml`, `item_venue.xml`, `fragment_course_edit.xml`, `item_par_editor.xml`
- Modify: `ui/courses/MyCoursesFragment.kt`, `ui/courses/CourseEditFragment.kt`

- [ ] **Step 1: MyCourses list**

`fragment_my_courses.xml`: `RecyclerView` `rvVenues` + `btnAddVenue`. `item_venue.xml`: `tvVenueName`, `tvCourseSummary` ("A코스·B코스"), tap → CourseEdit for that venue.

`MyCoursesFragment.kt`: observe `repo.observeVenues()`, list them; `btnAddVenue`/row tap navigates to `courseEditFragment`, passing `venueId` (or null for new) via `savedStateHandle`.

- [ ] **Step 2: CourseEdit — create/edit a venue + one 9-hole course with per-hole pars**

`fragment_course_edit.xml`: `etVenueName`, `etCourseName` (default "A코스"), a numeric `etHoleCount` (default 9), a `RecyclerView` `rvPars` of per-hole par steppers (reuse the −/＋ pattern; default par 3), `btnSave`.

`CourseEditFragment.kt` save logic:
```kotlin
// Build pars: List<Int> from the par editors (size == holeCount, default 3).
// venueId = existing or repo.upsertVenue(VenueEntity(name = venueName))
// repo.upsertCourse(CourseEntity(venueId = venueId, name = courseName, pars = pars))
// findNavController().popBackStack()
```

Par editor list adapts its item count when `etHoleCount` changes (clamp 1..27). Each item is `item_par_editor.xml`: "N홀" label + −/＋ around a par value (min 1).

- [ ] **Step 3: Build & smoke**

Run: `./gradlew :app:assembleDebug`
Expected: BUILD SUCCESSFUL. On device: create a venue + course, set per-hole pars, save; it appears in "내 구장" on the start screen and in My Courses.

- [ ] **Step 4: Commit**
```bash
git add app/src/main/java/com/parkgolf/score/ui/courses/ app/src/main/res/layout/fragment_my_courses.xml app/src/main/res/layout/item_venue.xml app/src/main/res/layout/fragment_course_edit.xml app/src/main/res/layout/item_par_editor.xml
git commit -m "feat: add my-courses management (create/edit venue, courses, pars)"
```

---

## Phase 12 — Core-flow instrumented test & accessibility pass

### Task 12.1: End-to-end Espresso test of the core flow

**Files:**
- Create: `app/src/androidTest/java/com/parkgolf/score/ui/CoreFlowTest.kt`

- [ ] **Step 1: Write the test**

`CoreFlowTest.kt`:
```kotlin
package com.parkgolf.score.ui

import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.parkgolf.score.R
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CoreFlowTest {
    @Test fun home_showsStartButton() {
        ActivityScenario.launch(MainActivity::class.java)
        onView(withId(R.id.btnStart))
            .check(matches(isDisplayed()))
            .check(matches(withText(R.string.start_round)))
    }

    @Test fun startButton_navigatesToStartScreen() {
        ActivityScenario.launch(MainActivity::class.java)
        onView(withId(R.id.btnStart)).perform(click())
        // Start screen shows the "새 구장 만들기" button
        onView(withId(R.id.btnNewCourse)).check(matches(isDisplayed()))
    }
}
```

- [ ] **Step 2: Run it**

Run: `./gradlew connectedDebugAndroidTest --tests "*.CoreFlowTest"`
Expected: PASS (2 tests) on an emulator/device.

- [ ] **Step 3: Commit**
```bash
git add app/src/main/java app/src/androidTest/java/com/parkgolf/score/ui/CoreFlowTest.kt
git commit -m "test: add Espresso core-flow smoke test"
```

### Task 12.2: Accessibility & large-font verification pass

**Files:**
- Modify: layouts as needed (no new files expected).

- [ ] **Step 1: Set device font to largest and check every screen**

On device: Settings → Display → Font size = largest. Walk Home → Start → Course setup → Player setup → Hole input → Grid → Summary → History → Detail → My courses. Confirm: no clipped text, no overlapping views, all buttons reachable (screens scroll when needed).

- [ ] **Step 2: Fix any clipping**

For any screen that clips: ensure the root is a `ScrollView`/`NestedScrollView` with `fillViewport=true`, replace fixed heights with `wrap_content` + `minHeight`, and add `app:layout_constrainedWidth="true"` to text views that can overflow. Re-check.

- [ ] **Step 3: TalkBack labels**

Enable TalkBack; confirm ＋/− announce "한 타 늘리기/줄이기" (contentDescription set in Task 7.1), and every actionable control has a spoken label. Add `android:contentDescription` where missing.

- [ ] **Step 4: Verify all unit tests still pass**

Run: `./gradlew testDebugUnitTest`
Expected: PASS (all suites).

- [ ] **Step 5: Commit**
```bash
git add -A
git commit -m "chore: accessibility & large-font layout fixes"
```

---

## Self-Review notes (spec coverage)

- Spec §3 UI stack (View + ConstraintLayout + ViewBinding, minSdk 21) → Tasks 0.1–0.2, all layouts.
- Spec §4 data model with **par snapshot in Round** → Task 2.2 (`RoundEntity` carries its own `holes`/`scores`), Task 4.1 (factory snapshots pars). Venue/Course edits never touch saved rounds (they're separate tables; rounds store `venueName` + `holes`).
- Spec §5 screens (home, start w/ recent+presets+nearby-placeholder, hole input, grid, summary, history, my courses) → Phases 5–11.
- Spec §6 input (score starts at par, `+`/`-`, color+text par relation) → Tasks 4.1, 7.1, 7.3 (`ParColors`).
- Spec §7 stats (recent avg, best, worst only) → Tasks 1.3, 10.1.
- Spec §8 error tolerance (edit any hole via grid jump, delete with confirm, in-progress recovery) → Tasks 8.1, 10.2, 5.3 (resume prompt) + persistence on every adjust (7.3) and on start (6.2 step 5).
- Spec §9 accessibility (large targets/fonts, high contrast, color+text, TalkBack) → Tasks 0.2, 7.1, 12.2.
- Spec §10 testing (domain unit TDD, in-memory Room, one core UI flow) → Phases 1/4 (unit), Task 3.2 (Room), Task 12.1 (Espresso).

Open items from spec §11 resolved here: minSdk = 21; navigation = single-Activity + Navigation Component; recent-stats N = 10.
```
