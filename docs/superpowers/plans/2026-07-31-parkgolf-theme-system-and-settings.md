# Theme System & Settings Foundation Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add runtime 4-color theming (green default) via custom theme attributes + activity recreate, a Settings screen (theme picker + default player name) persisted in SharedPreferences, a home settings entry point, and migrate all color references from `@color` to `?attr`.

**Architecture:** Custom color attributes (`?attr/park*`) are declared once and given values by four themes (`Theme.ParkGolf.Green/Blue/Orange/Purple`), all extending a shared Base. All layouts and drawables reference `?attr/park*` instead of `@color`, so they follow whichever theme the activity was created with. `MainActivity.setTheme(...)` picks the saved theme before `setContentView`; changing the theme in Settings saves it and calls `recreate()`. A `SettingsStore` wraps SharedPreferences; pure `ThemeKey`/`ThemeCatalog` map keys to styles.

**Tech Stack:** Kotlin, View system (XML) + ConstraintLayout + ViewBinding, Material3 themes, Jetpack Navigation, Room, SharedPreferences, JUnit4 + Truth (unit), Espresso + AndroidJUnit (instrumented). minSdk 21, light-only.

**Design spec:** `docs/superpowers/specs/2026-07-31-parkgolf-theme-system-and-settings-design.md`
**Mockup:** `docs/mockups/파크골프 점수 기록 앱/src/app/` (`types.ts` THEMES, `Settings.tsx`, `MainMenu.tsx`)

---

## Theme token values (from mockup `types.ts`)

Each theme sets these attrs (hex; `parkPrimary10` = primary at 10% alpha `1A`; `parkBorder` = primary at 20% alpha `33`):

| attr | green | blue | orange | purple |
|---|---|---|---|---|
| parkBackground | #F4F9F5 | #F0F6FB | #FDF6F0 | #F7F4FC |
| parkForeground | #1A2E1D | #1A2535 | #2D1A0E | #1E1530 |
| parkPrimary | #2A7A3B | #1A6FA4 | #C2601A | #6B3FA0 |
| parkPrimary10 | #1A2A7A3B | #1A1A6FA4 | #1AC2601A | #1A6B3FA0 |
| parkSecondary | #E6F2E8 | #E0EFF9 | #FAEADE | #EDE7F6 |
| parkMuted | #EAF3EC | #E8F3FB | #FDF0E4 | #F0EBFA |
| parkMutedForeground | #4E7255 | #3D6A8A | #8A5030 | #6B5490 |
| parkAccent | #D2EBDA | #C8E4F5 | #F5D9BC | #DDD0F0 |
| parkAccentForeground | #1A3D21 | #0D3A5C | #6B2D08 | #3A1A6B |
| parkBorder | #332A7A3B | #331A6FA4 | #33C2601A | #336B3FA0 |
| parkInputBg | #EAF3EC | #E8F3FB | #FDF0E4 | #F0EBFA |

Fixed (theme-independent): `card #FFFFFF`, `on_primary #FFFFFF`, `destructive #D4183D`, `on_destructive #FFFFFF`, `under_par #1565C0`, `even_par #252525`, `over_par #C62828`. Preview swatches: `theme_green #2A7A3B`, `theme_blue #1A6FA4`, `theme_orange #C2601A`, `theme_purple #6B3FA0`.

---

## File Structure

**New:** `res/values/attrs.xml`; `res/drawable/ic_settings.xml`, `ic_user.xml`, `circle_solid.xml`, `bg_theme_selected.xml`; `res/layout/fragment_settings.xml`; `java/.../ui/settings/ThemeKey.kt`, `ThemeCatalog.kt`, `SettingsStore.kt`, `SettingsFragment.kt`; `test/.../ui/ThemeSettingsTest.kt`; `androidTest/.../ui/SettingsFlowTest.kt`.

**Modified:** `res/values/themes.xml`, `colors.xml`, `strings.xml`; `AndroidManifest.xml`; `App.kt`, `MainActivity.kt`, `HomeFragment.kt`, `PlayerSetupFragment.kt`; `res/layout/fragment_home.xml`; `res/navigation/nav_graph.xml`; **all** `res/layout/*.xml` and `res/drawable/bg_*.xml` + icon vectors (color migration, Task 3).

---

## Task 1: Theme attributes, four themes, color cleanup, manifest

**Files:** Create `res/values/attrs.xml`; Modify `res/values/themes.xml`, `res/values/colors.xml`, `AndroidManifest.xml`.

- [ ] **Step 1: Create `app/src/main/res/values/attrs.xml`**
```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <attr name="parkBackground" format="color" />
    <attr name="parkForeground" format="color" />
    <attr name="parkPrimary" format="color" />
    <attr name="parkPrimary10" format="color" />
    <attr name="parkSecondary" format="color" />
    <attr name="parkMuted" format="color" />
    <attr name="parkMutedForeground" format="color" />
    <attr name="parkAccent" format="color" />
    <attr name="parkAccentForeground" format="color" />
    <attr name="parkBorder" format="color" />
    <attr name="parkInputBg" format="color" />
</resources>
```

- [ ] **Step 2: Replace `app/src/main/res/values/themes.xml`**
```xml
<?xml version="1.0" encoding="utf-8"?>
<resources xmlns:tools="http://schemas.android.com/tools">
    <style name="Theme.ParkGolf.Base" parent="Theme.Material3.DayNight.NoActionBar">
        <item name="colorOnPrimary">@color/on_primary</item>
        <item name="android:textColorPrimary">?attr/parkForeground</item>
    </style>

    <style name="Theme.ParkGolf.Green" parent="Theme.ParkGolf.Base">
        <item name="colorPrimary">#2A7A3B</item>
        <item name="android:windowBackground">#F4F9F5</item>
        <item name="parkBackground">#F4F9F5</item>
        <item name="parkForeground">#1A2E1D</item>
        <item name="parkPrimary">#2A7A3B</item>
        <item name="parkPrimary10">#1A2A7A3B</item>
        <item name="parkSecondary">#E6F2E8</item>
        <item name="parkMuted">#EAF3EC</item>
        <item name="parkMutedForeground">#4E7255</item>
        <item name="parkAccent">#D2EBDA</item>
        <item name="parkAccentForeground">#1A3D21</item>
        <item name="parkBorder">#332A7A3B</item>
        <item name="parkInputBg">#EAF3EC</item>
    </style>

    <style name="Theme.ParkGolf.Blue" parent="Theme.ParkGolf.Base">
        <item name="colorPrimary">#1A6FA4</item>
        <item name="android:windowBackground">#F0F6FB</item>
        <item name="parkBackground">#F0F6FB</item>
        <item name="parkForeground">#1A2535</item>
        <item name="parkPrimary">#1A6FA4</item>
        <item name="parkPrimary10">#1A1A6FA4</item>
        <item name="parkSecondary">#E0EFF9</item>
        <item name="parkMuted">#E8F3FB</item>
        <item name="parkMutedForeground">#3D6A8A</item>
        <item name="parkAccent">#C8E4F5</item>
        <item name="parkAccentForeground">#0D3A5C</item>
        <item name="parkBorder">#331A6FA4</item>
        <item name="parkInputBg">#E8F3FB</item>
    </style>

    <style name="Theme.ParkGolf.Orange" parent="Theme.ParkGolf.Base">
        <item name="colorPrimary">#C2601A</item>
        <item name="android:windowBackground">#FDF6F0</item>
        <item name="parkBackground">#FDF6F0</item>
        <item name="parkForeground">#2D1A0E</item>
        <item name="parkPrimary">#C2601A</item>
        <item name="parkPrimary10">#1AC2601A</item>
        <item name="parkSecondary">#FAEADE</item>
        <item name="parkMuted">#FDF0E4</item>
        <item name="parkMutedForeground">#8A5030</item>
        <item name="parkAccent">#F5D9BC</item>
        <item name="parkAccentForeground">#6B2D08</item>
        <item name="parkBorder">#33C2601A</item>
        <item name="parkInputBg">#FDF0E4</item>
    </style>

    <style name="Theme.ParkGolf.Purple" parent="Theme.ParkGolf.Base">
        <item name="colorPrimary">#6B3FA0</item>
        <item name="android:windowBackground">#F7F4FC</item>
        <item name="parkBackground">#F7F4FC</item>
        <item name="parkForeground">#1E1530</item>
        <item name="parkPrimary">#6B3FA0</item>
        <item name="parkPrimary10">#1A6B3FA0</item>
        <item name="parkSecondary">#EDE7F6</item>
        <item name="parkMuted">#F0EBFA</item>
        <item name="parkMutedForeground">#6B5490</item>
        <item name="parkAccent">#DDD0F0</item>
        <item name="parkAccentForeground">#3A1A6B</item>
        <item name="parkBorder">#336B3FA0</item>
        <item name="parkInputBg">#F0EBFA</item>
    </style>

    <!-- Back-compat alias so the manifest's Theme.ParkGolf keeps resolving until runtime setTheme picks one. -->
    <style name="Theme.ParkGolf" parent="Theme.ParkGolf.Green" />
</resources>
```

- [ ] **Step 3: Replace `app/src/main/res/values/colors.xml`** with fixed colors only (the shadcn tokens + aliases are removed in Task 3 after migration — but define the fixed set now; the old names stay temporarily so the app still builds until Task 3 migrates references). Set the file to:
```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <!-- theme-independent fixed colors -->
    <color name="card">#FFFFFF</color>
    <color name="on_primary">#FFFFFF</color>
    <color name="on_destructive">#FFFFFF</color>
    <color name="destructive">#D4183D</color>
    <color name="under_par">#1565C0</color>
    <color name="even_par">#252525</color>
    <color name="over_par">#C62828</color>
    <!-- theme preview swatches (fixed) -->
    <color name="theme_green">#2A7A3B</color>
    <color name="theme_blue">#1A6FA4</color>
    <color name="theme_orange">#C2601A</color>
    <color name="theme_purple">#6B3FA0</color>

    <!-- TEMP: old tokens kept only until Task 3 migrates references, then deleted -->
    <color name="primary">#030213</color>
    <color name="primary_10">#1A030213</color>
    <color name="background">#FFFFFF</color>
    <color name="foreground">#252525</color>
    <color name="secondary">#F1F1F4</color>
    <color name="muted">#ECECF0</color>
    <color name="muted_foreground">#717182</color>
    <color name="accent">#E9EBEF</color>
    <color name="accent_foreground">#030213</color>
    <color name="border">#1A000000</color>
    <color name="input_background">#F3F3F5</color>
    <color name="secondary_foreground">#030213</color>
    <color name="surface">#FFFFFF</color>
    <color name="surface_muted">#F1F1F4</color>
    <color name="text_primary">#252525</color>
    <color name="text_muted">#717182</color>
    <color name="green_primary">#030213</color>
    <color name="green_light">#1A030213</color>
    <color name="green_disabled">#ECECF0</color>
    <color name="stepper_bg">#F1F1F4</color>
</resources>
```

- [ ] **Step 4: Keep the manifest theme** — `AndroidManifest.xml` already sets `android:theme="@style/Theme.ParkGolf"` (now aliased to Green). No change needed; confirm it still references `@style/Theme.ParkGolf`.

- [ ] **Step 5: Build** — `ANDROID_HOME=/Users/inhyo/Library/Android/sdk ./gradlew :app:assembleDebug` → BUILD SUCCESSFUL (nothing references park* yet; old tokens still present).

- [ ] **Step 6: Commit**
```bash
git add app/src/main/res/values/attrs.xml app/src/main/res/values/themes.xml app/src/main/res/values/colors.xml
git commit -m "feat: add park* theme attributes and four color themes"
```

---

## Task 2: ThemeKey, ThemeCatalog, SettingsStore, App + MainActivity wiring (TDD for pure parts)

**Files:** Create `ui/settings/ThemeKey.kt`, `ThemeCatalog.kt`, `SettingsStore.kt`, `test/.../ui/ThemeSettingsTest.kt`; Modify `App.kt`, `MainActivity.kt`.

- [ ] **Step 1: Write the failing unit test** — `app/src/test/java/com/parkgolf/score/ui/ThemeSettingsTest.kt`:
```kotlin
package com.parkgolf.score.ui

import com.google.common.truth.Truth.assertThat
import com.parkgolf.score.ui.settings.ThemeKey
import org.junit.Test

class ThemeSettingsTest {
    @Test fun fromStored_roundTrips() {
        ThemeKey.entries.forEach { key ->
            assertThat(ThemeKey.fromStored(key.stored)).isEqualTo(key)
        }
    }

    @Test fun fromStored_unknownDefaultsToGreen() {
        assertThat(ThemeKey.fromStored(null)).isEqualTo(ThemeKey.GREEN)
        assertThat(ThemeKey.fromStored("teal")).isEqualTo(ThemeKey.GREEN)
    }
}
```

- [ ] **Step 2: Run test to verify it fails** — `ANDROID_HOME=/Users/inhyo/Library/Android/sdk ./gradlew :app:testDebugUnitTest --tests "com.parkgolf.score.ui.ThemeSettingsTest"` → FAIL (types missing).

- [ ] **Step 3: Create `app/src/main/java/com/parkgolf/score/ui/settings/ThemeKey.kt`**
```kotlin
package com.parkgolf.score.ui.settings

enum class ThemeKey(val stored: String) {
    GREEN("green"), BLUE("blue"), ORANGE("orange"), PURPLE("purple");

    companion object {
        fun fromStored(s: String?): ThemeKey = entries.firstOrNull { it.stored == s } ?: GREEN
    }
}
```

- [ ] **Step 4: Create `app/src/main/java/com/parkgolf/score/ui/settings/ThemeCatalog.kt`**
```kotlin
package com.parkgolf.score.ui.settings

import androidx.annotation.StyleRes
import com.parkgolf.score.R

object ThemeCatalog {
    @StyleRes
    fun styleFor(key: ThemeKey): Int = when (key) {
        ThemeKey.GREEN -> R.style.Theme_ParkGolf_Green
        ThemeKey.BLUE -> R.style.Theme_ParkGolf_Blue
        ThemeKey.ORANGE -> R.style.Theme_ParkGolf_Orange
        ThemeKey.PURPLE -> R.style.Theme_ParkGolf_Purple
    }
}
```

- [ ] **Step 5: Create `app/src/main/java/com/parkgolf/score/ui/settings/SettingsStore.kt`**
```kotlin
package com.parkgolf.score.ui.settings

import android.content.Context

class SettingsStore(context: Context) {
    private val prefs = context.applicationContext
        .getSharedPreferences("parkgolf_settings", Context.MODE_PRIVATE)

    var themeKey: ThemeKey
        get() = ThemeKey.fromStored(prefs.getString(KEY_THEME, null))
        set(value) { prefs.edit().putString(KEY_THEME, value.stored).apply() }

    var defaultPlayerName: String
        get() = prefs.getString(KEY_NAME, DEFAULT_NAME) ?: DEFAULT_NAME
        set(value) { prefs.edit().putString(KEY_NAME, value).apply() }

    companion object {
        const val DEFAULT_NAME = "나"
        private const val KEY_THEME = "theme"
        private const val KEY_NAME = "default_player_name"
    }
}
```

- [ ] **Step 6: Run unit test to verify it passes** — same command as Step 2 → PASS.

- [ ] **Step 7: Wire into `App.kt`** — add a `SettingsStore` singleton. Modify `App.kt` to:
```kotlin
package com.parkgolf.score

import android.app.Application
import androidx.room.Room
import com.parkgolf.score.data.ParkGolfRepository
import com.parkgolf.score.data.ParkGolfRepositoryImpl
import com.parkgolf.score.data.db.AppDatabase
import com.parkgolf.score.ui.settings.SettingsStore

class App : Application() {
    lateinit var repository: ParkGolfRepository
        private set
    lateinit var settings: SettingsStore
        private set

    override fun onCreate() {
        super.onCreate()
        val db = Room.databaseBuilder(this, AppDatabase::class.java, "parkgolf.db").build()
        repository = ParkGolfRepositoryImpl(db.venueDao(), db.courseDao(), db.roundDao())
        settings = SettingsStore(this)
    }

    companion object {
        fun repo(app: Application): ParkGolfRepository = (app as App).repository
        fun settings(app: Application): SettingsStore = (app as App).settings
    }
}
```

- [ ] **Step 8: Apply theme in `MainActivity.kt`**
```kotlin
package com.parkgolf.score.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.parkgolf.score.App
import com.parkgolf.score.R
import com.parkgolf.score.ui.settings.ThemeCatalog

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(ThemeCatalog.styleFor(App.settings(application).themeKey))
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }
}
```

- [ ] **Step 9: Build** — `./gradlew :app:assembleDebug` → BUILD SUCCESSFUL.

- [ ] **Step 10: Commit**
```bash
git add app/src/main/java/com/parkgolf/score/ui/settings/ThemeKey.kt app/src/main/java/com/parkgolf/score/ui/settings/ThemeCatalog.kt app/src/main/java/com/parkgolf/score/ui/settings/SettingsStore.kt app/src/main/java/com/parkgolf/score/App.kt app/src/main/java/com/parkgolf/score/ui/MainActivity.kt app/src/test/java/com/parkgolf/score/ui/ThemeSettingsTest.kt
git commit -m "feat: add ThemeKey/ThemeCatalog/SettingsStore and apply theme on launch"
```

---

## Task 3: Migrate color references `@color` → `?attr/park*`

**Files:** All `app/src/main/res/layout/*.xml` and `app/src/main/res/drawable/bg_*.xml` + icon vectors; then trim `colors.xml`.

- [ ] **Step 1: Fix the two white-surface drawables first** (these `@color/background` solids must become white `@color/card`, NOT `parkBackground`):
```bash
cd app/src/main/res/drawable
sed -i '' 's#@color/background"#@color/card"#g' bg_card.xml bg_step_inactive.xml
cd -
```

- [ ] **Step 2: Run the global attribute migration** across layouts + drawables. Each replacement is anchored to the trailing `"` so tokens never match inside longer tokens (e.g. `primary"` ≠ `primary_10"`):
```bash
cd app/src/main/res
FILES=$(grep -rl '@color/' layout drawable)
for f in $FILES; do
  sed -i '' \
    -e 's#@color/primary_10"#?attr/parkPrimary10"#g' \
    -e 's#@color/green_light"#?attr/parkPrimary10"#g' \
    -e 's#@color/muted_foreground"#?attr/parkMutedForeground"#g' \
    -e 's#@color/text_muted"#?attr/parkMutedForeground"#g' \
    -e 's#@color/input_background"#?attr/parkInputBg"#g' \
    -e 's#@color/accent_foreground"#?attr/parkAccentForeground"#g' \
    -e 's#@color/surface_muted"#?attr/parkSecondary"#g' \
    -e 's#@color/text_primary"#?attr/parkForeground"#g' \
    -e 's#@color/green_primary"#?attr/parkPrimary"#g' \
    -e 's#@color/secondary"#?attr/parkSecondary"#g' \
    -e 's#@color/accent"#?attr/parkAccent"#g' \
    -e 's#@color/muted"#?attr/parkMuted"#g' \
    -e 's#@color/border"#?attr/parkBorder"#g' \
    -e 's#@color/foreground"#?attr/parkForeground"#g' \
    -e 's#@color/primary"#?attr/parkPrimary"#g' \
    -e 's#@color/surface"#?attr/parkBackground"#g' \
    -e 's#@color/background"#?attr/parkBackground"#g' \
    "$f"
done
cd -
```

- [ ] **Step 3: Verify only allowed `@color` refs remain** in layouts/drawables:
```bash
grep -rho '@color/[a-z_]*' app/src/main/res/layout app/src/main/res/drawable | sort -u
```
Expected set is a subset of: `@color/card`, `@color/on_primary`, `@color/on_destructive`, `@color/destructive`, `@color/under_par`, `@color/even_par`, `@color/over_par`, `@color/theme_green|blue|orange|purple`. If anything else appears (e.g. `@color/stepper_bg`), migrate it to the matching `?attr/park*` by hand.

- [ ] **Step 4: Fix Kotlin references to removed colors.** `CourseWizardFragment.kt:223` (in `styleSteps`) resolves `R.color.primary` / `R.color.muted_foreground`, which no longer exist after the trim. Resolve the theme attribute instead. Add the import `import com.google.android.material.color.MaterialColors` and replace the line:
```kotlin
            tv.setTextColor(
                ContextCompat.getColor(requireContext(), if (active || done) R.color.primary else R.color.muted_foreground)
            )
```
with:
```kotlin
            val attr = if (active || done) R.attr.parkPrimary else R.attr.parkMutedForeground
            tv.setTextColor(MaterialColors.getColor(requireContext(), attr, 0))
```
(`ContextCompat` may become unused in that file — remove its import only if the build warns/errs. `ParColors.kt` uses `under_par`/`even_par`/`over_par`, which are kept — leave it unchanged.) Then confirm no stale refs remain: `grep -rn 'R\.color\.\(primary\|muted_foreground\|foreground\|background\|secondary\|accent\|border\|input_background\|green_\)' app/src/main/java` → no matches.

- [ ] **Step 5: Trim `colors.xml`** — remove the entire `<!-- TEMP: old tokens ... -->` block (everything from `<color name="primary">` through `<color name="stepper_bg">`), leaving only the fixed colors + theme swatches from Task 1 Step 3.

- [ ] **Step 6: Build** — `./gradlew :app:assembleDebug` → BUILD SUCCESSFUL (no unresolved `@color/*` or `R.color.*`; if the build reports a missing color, it is a reference the sweep missed — fix it to the right `?attr`/`R.attr` and rebuild).

- [ ] **Step 7: Install & visually confirm green theme** on the running emulator:
```bash
ANDROID_HOME=/Users/inhyo/Library/Android/sdk ./gradlew :app:installDebug
/Users/inhyo/Library/Android/sdk/platform-tools/adb shell monkey -p com.parkgolf.score -c android.intent.category.LAUNCHER 1
```
Confirm the app renders in green (primary buttons green, tinted background), not the old dark palette.

- [ ] **Step 8: Commit**
```bash
git add -A
git commit -m "refactor: migrate color references from @color to ?attr theme attributes"
```

---

## Task 4: Settings screen + home entry point

**Files:** Create `res/drawable/ic_settings.xml`, `ic_user.xml`, `circle_solid.xml`, `bg_theme_selected.xml`; `res/layout/fragment_settings.xml`; `java/.../ui/settings/SettingsFragment.kt`; Modify `res/values/strings.xml`, `res/navigation/nav_graph.xml`, `res/layout/fragment_home.xml`, `java/.../ui/home/HomeFragment.kt`.

- [ ] **Step 1: Add drawables.**
`ic_settings.xml` (lucide settings-2 style, gear-ish):
```xml
<vector xmlns:android="http://schemas.android.com/apk/res/android" android:width="24dp" android:height="24dp" android:viewportWidth="24" android:viewportHeight="24">
    <path android:strokeColor="?attr/parkMutedForeground" android:strokeWidth="1.8" android:strokeLineCap="round" android:strokeLineJoin="round" android:pathData="M12,15 A3,3 0 1 1 12,9 A3,3 0 0 1 12,15 Z M19.4,15 A1.65,1.65 0 0 0 19.73,16.82 L19.79,16.88 A2,2 0 1 1 16.96,19.71 L16.9,19.65 A1.65,1.65 0 0 0 15.09,19.32 A1.65,1.65 0 0 0 14,20.83 L14,21 A2,2 0 1 1 10,21 L10,20.91 A1.65,1.65 0 0 0 8.91,19.4 A1.65,1.65 0 0 0 7.09,19.73 L7.03,19.79 A2,2 0 1 1 4.2,16.96 L4.26,16.9 A1.65,1.65 0 0 0 4.59,15.09 A1.65,1.65 0 0 0 3.08,14 L3,14 A2,2 0 1 1 3,10 L3.09,10 A1.65,1.65 0 0 0 4.6,8.91 A1.65,1.65 0 0 0 4.27,7.09 L4.21,7.03 A2,2 0 1 1 7.04,4.2 L7.1,4.26 A1.65,1.65 0 0 0 8.91,4.59 A1.65,1.65 0 0 0 10,3.08 L10,3 A2,2 0 1 1 14,3 L14,3.09 A1.65,1.65 0 0 0 15.09,4.6 A1.65,1.65 0 0 0 16.91,4.27 L16.97,4.21 A2,2 0 1 1 19.8,7.04 L19.74,7.1 A1.65,1.65 0 0 0 19.41,8.91 A1.65,1.65 0 0 0 20.92,10 L21,10 A2,2 0 1 1 21,14 L20.91,14 A1.65,1.65 0 0 0 19.4,15 Z" />
</vector>
```
`ic_user.xml`:
```xml
<vector xmlns:android="http://schemas.android.com/apk/res/android" android:width="24dp" android:height="24dp" android:viewportWidth="24" android:viewportHeight="24">
    <path android:strokeColor="?attr/parkMutedForeground" android:strokeWidth="2" android:strokeLineCap="round" android:strokeLineJoin="round" android:pathData="M20,21 L20,19 A4,4 0 0 0 16,15 L8,15 A4,4 0 0 0 4,19 L4,21 M16,7 A4,4 0 1 1 8,7 A4,4 0 0 1 16,7 Z" />
</vector>
```
`circle_solid.xml` (white oval, tinted per row via `backgroundTint`):
```xml
<shape xmlns:android="http://schemas.android.com/apk/res/android" android:shape="oval">
    <solid android:color="#FFFFFF" />
</shape>
```
`bg_theme_selected.xml`:
```xml
<shape xmlns:android="http://schemas.android.com/apk/res/android" android:shape="rectangle">
    <solid android:color="?attr/parkPrimary10" />
    <corners android:radius="@dimen/radius_card" />
    <stroke android:width="@dimen/border_width" android:color="?attr/parkPrimary" />
</shape>
```

- [ ] **Step 2: Add strings** — append inside `<resources>` in `strings.xml`:
```xml
    <string name="title_settings">설정</string>
    <string name="settings_label">설정</string>
    <string name="theme_section_title">컬러 테마</string>
    <string name="theme_section_desc">앱 전체에 적용되는 색상을 선택하세요</string>
    <string name="default_name_title">기본 이름</string>
    <string name="default_name_desc">게임 시작 시 첫 번째 플레이어로 표시되는 이름입니다</string>
    <string name="default_name_hint">이름을 입력하세요</string>
    <string name="theme_green">초록</string>
    <string name="theme_blue">파랑</string>
    <string name="theme_orange">주황</string>
    <string name="theme_purple">보라</string>
```

- [ ] **Step 3: Create `app/src/main/res/layout/fragment_settings.xml`** — top bar + theme rows + name card. Each theme row is a horizontal container (id `rowGreen/rowBlue/rowOrange/rowPurple`) with a tinted circle, label, and a check icon (id `checkGreen/...`, `visibility=gone`):
```xml
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent" android:layout_height="match_parent"
    android:orientation="vertical" android:background="?attr/parkBackground">

    <include android:id="@+id/topBar" layout="@layout/view_top_bar" />

    <ScrollView android:layout_width="match_parent" android:layout_height="0dp"
        android:layout_weight="1" android:fillViewport="true" android:padding="20dp">
        <LinearLayout android:layout_width="match_parent" android:layout_height="wrap_content"
            android:orientation="vertical">

            <TextView android:layout_width="wrap_content" android:layout_height="wrap_content"
                android:text="@string/theme_section_title" android:textSize="@dimen/title_text"
                android:textStyle="bold" android:textColor="?attr/parkForeground" />
            <TextView android:layout_width="wrap_content" android:layout_height="wrap_content"
                android:text="@string/theme_section_desc" android:textColor="?attr/parkMutedForeground"
                android:layout_marginTop="4dp" android:layout_marginBottom="16dp" />

            <LinearLayout android:id="@+id/rowGreen" style="@style/ThemeRow">
                <View style="@style/ThemeSwatch" android:backgroundTint="@color/theme_green" />
                <TextView style="@style/ThemeRowLabel" android:text="@string/theme_green" />
                <ImageView android:id="@+id/checkGreen" style="@style/ThemeCheck" />
            </LinearLayout>
            <LinearLayout android:id="@+id/rowBlue" style="@style/ThemeRow">
                <View style="@style/ThemeSwatch" android:backgroundTint="@color/theme_blue" />
                <TextView style="@style/ThemeRowLabel" android:text="@string/theme_blue" />
                <ImageView android:id="@+id/checkBlue" style="@style/ThemeCheck" />
            </LinearLayout>
            <LinearLayout android:id="@+id/rowOrange" style="@style/ThemeRow">
                <View style="@style/ThemeSwatch" android:backgroundTint="@color/theme_orange" />
                <TextView style="@style/ThemeRowLabel" android:text="@string/theme_orange" />
                <ImageView android:id="@+id/checkOrange" style="@style/ThemeCheck" />
            </LinearLayout>
            <LinearLayout android:id="@+id/rowPurple" style="@style/ThemeRow">
                <View style="@style/ThemeSwatch" android:backgroundTint="@color/theme_purple" />
                <TextView style="@style/ThemeRowLabel" android:text="@string/theme_purple" />
                <ImageView android:id="@+id/checkPurple" style="@style/ThemeCheck" />
            </LinearLayout>

            <View android:layout_width="match_parent" android:layout_height="2dp"
                android:background="?attr/parkBorder" android:layout_marginVertical="24dp" />

            <TextView android:layout_width="wrap_content" android:layout_height="wrap_content"
                android:text="@string/default_name_title" android:textSize="@dimen/title_text"
                android:textStyle="bold" android:textColor="?attr/parkForeground" />
            <TextView android:layout_width="wrap_content" android:layout_height="wrap_content"
                android:text="@string/default_name_desc" android:textColor="?attr/parkMutedForeground"
                android:layout_marginTop="4dp" android:layout_marginBottom="16dp" />
            <LinearLayout android:layout_width="match_parent" android:layout_height="wrap_content"
                android:orientation="horizontal" android:gravity="center_vertical"
                android:background="@drawable/bg_card" android:paddingHorizontal="20dp" android:paddingVertical="16dp">
                <ImageView android:layout_width="20dp" android:layout_height="20dp"
                    android:src="@drawable/ic_user" android:importantForAccessibility="no" />
                <EditText android:id="@+id/etDefaultName"
                    android:layout_width="0dp" android:layout_height="wrap_content" android:layout_weight="1"
                    android:layout_marginStart="12dp" android:background="@android:color/transparent"
                    android:hint="@string/default_name_hint" android:textSize="@dimen/body_text"
                    android:textColor="?attr/parkForeground" android:inputType="text" />
            </LinearLayout>
        </LinearLayout>
    </ScrollView>
</LinearLayout>
```

- [ ] **Step 4: Add the referenced styles** — append to `app/src/main/res/values/styles.xml`:
```xml
    <style name="ThemeRow">
        <item name="android:layout_width">match_parent</item>
        <item name="android:layout_height">wrap_content</item>
        <item name="android:orientation">horizontal</item>
        <item name="android:gravity">center_vertical</item>
        <item name="android:background">@drawable/bg_card</item>
        <item name="android:paddingHorizontal">20dp</item>
        <item name="android:paddingVertical">16dp</item>
        <item name="android:layout_marginBottom">12dp</item>
        <item name="android:minHeight">@dimen/touch_min</item>
        <item name="android:clickable">true</item>
        <item name="android:focusable">true</item>
    </style>
    <style name="ThemeSwatch">
        <item name="android:layout_width">40dp</item>
        <item name="android:layout_height">40dp</item>
        <item name="android:background">@drawable/circle_solid</item>
    </style>
    <style name="ThemeRowLabel">
        <item name="android:layout_width">0dp</item>
        <item name="android:layout_height">wrap_content</item>
        <item name="android:layout_weight">1</item>
        <item name="android:layout_marginStart">16dp</item>
        <item name="android:textSize">@dimen/body_text</item>
        <item name="android:textStyle">bold</item>
        <item name="android:textColor">?attr/parkForeground</item>
    </style>
    <style name="ThemeCheck">
        <item name="android:layout_width">24dp</item>
        <item name="android:layout_height">24dp</item>
        <item name="android:src">@drawable/ic_check</item>
        <item name="android:visibility">gone</item>
        <item name="android:tint">?attr/parkPrimary</item>
    </style>
```

- [ ] **Step 5: Create `app/src/main/java/com/parkgolf/score/ui/settings/SettingsFragment.kt`**
```kotlin
package com.parkgolf.score.ui.settings

import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.parkgolf.score.App
import com.parkgolf.score.R
import com.parkgolf.score.databinding.FragmentSettingsBinding

class SettingsFragment : Fragment(R.layout.fragment_settings) {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val binding = FragmentSettingsBinding.bind(view)
        val store = App.settings(requireActivity().application)

        binding.topBar.tvBarTitle.text = getString(R.string.title_settings)
        binding.topBar.btnBack.setOnClickListener { findNavController().popBackStack() }

        val rows = mapOf(
            ThemeKey.GREEN to Pair(binding.rowGreen, binding.checkGreen),
            ThemeKey.BLUE to Pair(binding.rowBlue, binding.checkBlue),
            ThemeKey.ORANGE to Pair(binding.rowOrange, binding.checkOrange),
            ThemeKey.PURPLE to Pair(binding.rowPurple, binding.checkPurple),
        )
        val current = store.themeKey
        rows.forEach { (key, pair) ->
            val (row, check) = pair
            val selected = key == current
            check.isVisible = selected
            row.setBackgroundResource(
                if (selected) R.drawable.bg_theme_selected else R.drawable.bg_card
            )
            row.setOnClickListener {
                if (store.themeKey != key) {
                    store.themeKey = key
                    requireActivity().recreate()
                }
            }
        }

        binding.etDefaultName.setText(store.defaultPlayerName)
        binding.etDefaultName.doAfterTextChanged { text ->
            val name = text?.toString()?.trim().orEmpty()
            store.defaultPlayerName = if (name.isEmpty()) SettingsStore.DEFAULT_NAME else name
        }
    }
}
```

- [ ] **Step 6: Add the nav destination** — in `nav_graph.xml`, add:
```xml
    <fragment android:id="@+id/settingsFragment"
        android:name="com.parkgolf.score.ui.settings.SettingsFragment" />
```

- [ ] **Step 7: Add the home settings button** — `fragment_home.xml`'s root is a `ScrollView` wrapping a `ConstraintLayout` (children: `title`, `btnStart`, `btnHistory`, `btnMyCourses`). Add this `ImageButton` as a child of the `ConstraintLayout`, constrained to the top-right (it sits at the top-end corner; `title` stays centered with its 24dp top margin):
```xml
        <ImageButton
            android:id="@+id/btnSettings"
            android:layout_width="48dp"
            android:layout_height="48dp"
            android:background="@drawable/bg_card"
            android:src="@drawable/ic_settings"
            android:scaleType="center"
            android:contentDescription="@string/settings_label"
            app:layout_constraintTop_toTopOf="parent"
            app:layout_constraintEnd_toEndOf="parent" />
```

- [ ] **Step 8: Wire the button** — in `HomeFragment.kt`, after the binding is created, add:
```kotlin
binding.btnSettings.setOnClickListener { findNavController().navigate(R.id.settingsFragment) }
```

- [ ] **Step 9: Build** — `./gradlew :app:assembleDebug` → BUILD SUCCESSFUL.

- [ ] **Step 10: Commit**
```bash
git add -A
git commit -m "feat: add Settings screen with theme picker and default name, plus home entry"
```

---

## Task 5: Default player name wiring

**Files:** Modify `app/src/main/java/com/parkgolf/score/ui/start/PlayerSetupFragment.kt`.

- [ ] **Step 1: Replace the hardcoded first-player name** — in `PlayerSetupFragment.onViewCreated`, read the default from settings once and use it in both the initial row and the save fallback. Change:
  - The line `addPlayerRow(getString(R.string.me))` → `addPlayerRow(defaultName)`
  - In the save mapping, `else if (i == 0) getString(R.string.me) else ...` → `else if (i == 0) defaultName else ...`
  Add near the top of `onViewCreated` (after the binding/title lines):
```kotlin
val defaultName = App.settings(requireActivity().application).defaultPlayerName
```
Ensure `import com.parkgolf.score.App` is present.

- [ ] **Step 2: Build** — `./gradlew :app:assembleDebug` → BUILD SUCCESSFUL.

- [ ] **Step 3: Commit**
```bash
git add app/src/main/java/com/parkgolf/score/ui/start/PlayerSetupFragment.kt
git commit -m "feat: use configured default player name in player setup"
```

---

## Task 6: Instrumented tests + regression

**Files:** Create `app/src/androidTest/java/com/parkgolf/score/ui/SettingsFlowTest.kt`.

- [ ] **Step 1: Write the instrumented test** (emulator running & booted; `adb devices` shows `device`):
```kotlin
package com.parkgolf.score.ui

import android.content.Context
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.parkgolf.score.R
import com.parkgolf.score.ui.settings.SettingsStore
import com.parkgolf.score.ui.settings.ThemeKey
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SettingsFlowTest {
    private val ctx = ApplicationProvider.getApplicationContext<Context>()

    @After fun resetSettings() {
        SettingsStore(ctx).apply { themeKey = ThemeKey.GREEN; defaultPlayerName = SettingsStore.DEFAULT_NAME }
    }

    @Test fun store_persistsThemeAndName() {
        SettingsStore(ctx).apply { themeKey = ThemeKey.BLUE; defaultPlayerName = "홍길동" }
        val reloaded = SettingsStore(ctx)
        assertThat(reloaded.themeKey).isEqualTo(ThemeKey.BLUE)
        assertThat(reloaded.defaultPlayerName).isEqualTo("홍길동")
    }

    @Test fun homeSettingsButton_opensSettings() {
        ActivityScenario.launch(MainActivity::class.java)
        onView(withId(R.id.btnSettings)).perform(click())
        onView(withText(R.string.theme_section_title)).check(matches(isDisplayed()))
    }
}
```
> The default-player-name path (game-start → player-setup) is long and data-dependent, so it is not exercised end-to-end here; `store_persistsThemeAndName` proves persistence and Task 5's wiring is verified manually (Verification Checklist). Do not add a half-written UI test for it.

- [ ] **Step 2: Run instrumented tests** — `ANDROID_HOME=/Users/inhyo/Library/Android/sdk ./gradlew :app:connectedDebugAndroidTest` → all green (new + existing `CoreFlowTest`, `CourseWizardFlowTest`, `RepositoryTest`). If a test flakes on stale app data, `adb uninstall com.parkgolf.score` and re-run.

- [ ] **Step 3: Run full unit suite** — `./gradlew :app:testDebugUnitTest` → BUILD SUCCESSFUL.

- [ ] **Step 4: Commit**
```bash
git add app/src/androidTest/java/com/parkgolf/score/ui/SettingsFlowTest.kt
git commit -m "test: add settings persistence and navigation instrumented tests"
```

---

## Verification Checklist (after all tasks)

- [ ] `./gradlew :app:testDebugUnitTest` — all unit tests pass (includes `ThemeSettingsTest`).
- [ ] `./gradlew :app:connectedDebugAndroidTest` — all instrumented tests pass.
- [ ] `./gradlew :app:assembleDebug` — clean build; `grep -rho '@color/[a-z_]*' app/src/main/res/layout app/src/main/res/drawable | sort -u` shows only fixed colors.
- [ ] Manual: 홈 우상단 설정 버튼 → 설정 화면. 초록/파랑/주황/보라 선택 시 앱 전체 색 즉시 변경.
- [ ] Manual: 앱 재실행 후에도 선택 테마 유지.
- [ ] Manual: 기본 이름 변경 → 게임 시작 인원 구성 첫 칸에 반영.
- [ ] Manual: 모든 화면이 선택 테마 색을 따름(검정 팔레트 잔재 없음).
