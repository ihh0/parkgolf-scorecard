# 경기 진행(5b) 재디자인 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** GamePlay/ScoreOverview/GameComplete 세 화면을 목업 외형+상호작용으로 교체한다. 도메인 로직(Scoring/ScoreEditor/SummaryViewModel.ranking, 증분 저장·완료 표시·홀 점프)은 재사용하고, `Scoring.runningStrokes`만 새로 추가한다.

**Architecture:** View 시스템(XML) + ViewBinding + Navigation. 프래그먼트가 뷰를 프로그램적으로 채운다(진행 도트·플레이어 카드·표·순위 카드). 점수는 최소 1, 상한 없음(`ScoreEditor` 그대로).

**Tech Stack:** Kotlin, AndroidX, Material, JUnit+Truth(단위), Espresso(계측).

**참고 스펙:** `docs/superpowers/specs/2026-08-13-parkgolf-game-progress-redesign-design.md`

**공통 참고(기존 재사용):**
- 도메인: `Scoring.total/parTotal/relativeToPar/relationLabel/relation`, `ScoreEditor.adjust`(최소 1 보장), `SummaryViewModel.ranking(round): List<RankRow(rank,player,total,relative)>`, `ParColors.colorFor(ctx, diff)`(under/even/over_par 색).
- `HoleInputViewModel`: `holeIndex: LiveData<Int>`, `isLastHole()`, `next()`, `prev()`, `goTo(idx)`. 생성: `HoleInputViewModel(totalHoles = round.holes.size)`.
- `RoundSessionViewModel`: `round: LiveData<Round?>`, `adjust(p,h,delta)`, `startRound(r)`.
- `Round`: `venueName`, `players: List<String>`, `holes: List<HoleSpec(courseName,holeNo,par)>`, `scores: List<List<Int?>>`, `status`.
- 드로어블: `bg_card`, `bg_pill_primary`, `bg_pill_secondary`, `bg_menu_icon`. 아이콘: `ic_flag`, `ic_save`, `ic_minus`, `ic_plus`, `ic_chevron_left`, `ic_chevron_right`. 색: `@color/under_par|even_par|over_par|card|on_primary`. 속성: `?attr/parkPrimary|parkPrimary10|parkForeground|parkMutedForeground|parkBackground|parkSecondary|parkBorder`. 치수: `screen_padding`, `radius_card`, `radius_pill`, `radius_sm`, `border_width`, `touch_min`, `title_text`, `body_text`, `button_text`, `score_text`(34sp), `stepper_size`(64dp). 상단바 include `@layout/view_top_bar`(자식 `tvBarTitle`,`btnBack`).
- nav 목적지(기존): `holeInputFragment`, `gridViewFragment`, `roundSummaryFragment`, `homeFragment`. `navigate(R.id.<dest>)` 직접 호출.
- 공용 헬퍼: `com.parkgolf.score.ui.common.confirmYesNo(msgRes) { }`, `com.parkgolf.score.ui.common.onBackPressed { }`.

---

## File Structure

**신규**
- `res/drawable/ic_trophy.xml`, `ic_medal.xml`, `ic_table.xml`
- `res/drawable/bg_dot_current.xml`, `bg_dot_done.xml`, `bg_dot_future.xml`, `bg_stepper.xml`, `bg_medal_circle.xml`

**수정**
- `java/.../domain/Scoring.kt` + `test/.../ScoringTest.kt`(또는 신규)
- `res/values/colors.xml`, `strings.xml`, `dimens.xml`
- `res/layout/view_player_score_row.xml`, `fragment_hole_input.xml`, `fragment_grid_view.xml`, `fragment_round_summary.xml`, `item_rank_row.xml`
- `java/.../ui/hole/HoleInputFragment.kt`, `ui/grid/GridViewFragment.kt`, `ui/summary/RoundSummaryFragment.kt`

---

## Task 1: `Scoring.runningStrokes` (TDD)

**Files:**
- Modify: `app/src/main/java/com/parkgolf/score/domain/Scoring.kt`
- Test: `app/src/test/java/com/parkgolf/score/domain/ScoringRunningStrokesTest.kt` (new)

- [ ] **Step 1: 실패 테스트 작성** — new file:
```kotlin
package com.parkgolf.score.domain

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class ScoringRunningStrokesTest {
    @Test fun sumsUpToAndIncludingIndex() {
        val scores = listOf<Int?>(3, 4, 2, 5)
        assertThat(Scoring.runningStrokes(scores, 0)).isEqualTo(3)
        assertThat(Scoring.runningStrokes(scores, 2)).isEqualTo(9)
        assertThat(Scoring.runningStrokes(scores, 3)).isEqualTo(14)
    }

    @Test fun ignoresNulls() {
        val scores = listOf<Int?>(3, null, 4)
        assertThat(Scoring.runningStrokes(scores, 2)).isEqualTo(7)
    }

    @Test fun negativeIndexIsZero() {
        assertThat(Scoring.runningStrokes(listOf<Int?>(3, 4), -1)).isEqualTo(0)
    }

    @Test fun indexBeyondSizeCapsAtList() {
        assertThat(Scoring.runningStrokes(listOf<Int?>(3, 4), 99)).isEqualTo(7)
    }
}
```

- [ ] **Step 2: 실패 확인**

Run: `./gradlew :app:testDebugUnitTest --tests "com.parkgolf.score.domain.ScoringRunningStrokesTest" -q`
Expected: 컴파일 실패(runningStrokes 미존재).

- [ ] **Step 3: 구현** — `Scoring.kt`의 `object Scoring {` 안, `total(...)` 아래에 추가:
```kotlin
    /** 플레이어의 0..uptoHoleIndex(포함) 누적 타수. null 점수는 제외. */
    fun runningStrokes(scores: List<Int?>, uptoHoleIndex: Int): Int =
        scores.take((uptoHoleIndex + 1).coerceAtLeast(0)).filterNotNull().sum()
```

- [ ] **Step 4: 통과 확인**

Run: `./gradlew :app:testDebugUnitTest --tests "com.parkgolf.score.domain.ScoringRunningStrokesTest" -q`
Expected: PASS (4 tests). 그리고 전체: `./gradlew :app:testDebugUnitTest -q` → 통과.

- [ ] **Step 5: 커밋**
```bash
git add app/src/main/java/com/parkgolf/score/domain/Scoring.kt app/src/test/java/com/parkgolf/score/domain/ScoringRunningStrokesTest.kt
git commit -m "feat: add Scoring.runningStrokes for per-player cumulative"
```

---

## Task 2: 리소스 (아이콘·드로어블·색·문자열·치수)

**Files:**
- Create: `ic_trophy.xml`, `ic_medal.xml`, `ic_table.xml`, `bg_dot_current.xml`, `bg_dot_done.xml`, `bg_dot_future.xml`, `bg_stepper.xml`, `bg_medal_circle.xml` (all under `app/src/main/res/drawable/`)
- Modify: `app/src/main/res/values/colors.xml`, `strings.xml`, `dimens.xml`

- [ ] **Step 1: 아이콘 3종**

`ic_trophy.xml`:
```xml
<vector xmlns:android="http://schemas.android.com/apk/res/android" android:width="24dp" android:height="24dp" android:viewportWidth="24" android:viewportHeight="24">
    <path android:strokeColor="?attr/parkPrimary" android:strokeWidth="1.6" android:strokeLineCap="round" android:strokeLineJoin="round" android:pathData="M7,4 L17,4 L17,9 A5,5 0 0 1 7,9 Z" />
    <path android:strokeColor="?attr/parkPrimary" android:strokeWidth="1.6" android:strokeLineCap="round" android:strokeLineJoin="round" android:pathData="M7,5 L4,5 L4,7 A3,3 0 0 0 7,10 M17,5 L20,5 L20,7 A3,3 0 0 1 17,10" />
    <path android:strokeColor="?attr/parkPrimary" android:strokeWidth="1.6" android:strokeLineCap="round" android:strokeLineJoin="round" android:pathData="M12,14 L12,17 M9,20 L15,20 M10,17 L14,17 L15,20 L9,20 Z" />
</vector>
```

`ic_medal.xml`:
```xml
<vector xmlns:android="http://schemas.android.com/apk/res/android" android:width="24dp" android:height="24dp" android:viewportWidth="24" android:viewportHeight="24">
    <path android:strokeColor="#FFFFFF" android:strokeWidth="2" android:strokeLineCap="round" android:strokeLineJoin="round" android:pathData="M8,3 L10.5,8 M15.5,8 L13,3 M8,3 L16,3" />
    <path android:strokeColor="#FFFFFF" android:strokeWidth="2" android:strokeLineCap="round" android:strokeLineJoin="round" android:pathData="M12,20 A5,5 0 1 0 12,10 A5,5 0 0 0 12,20 Z" />
</vector>
```
(흰색 stroke — 코드에서 원형 배경을 메달색으로 tint하고 아이콘은 흰색으로 얹는다.)

`ic_table.xml`:
```xml
<vector xmlns:android="http://schemas.android.com/apk/res/android" android:width="24dp" android:height="24dp" android:viewportWidth="24" android:viewportHeight="24">
    <path android:strokeColor="?attr/parkMutedForeground" android:strokeWidth="2" android:strokeLineCap="round" android:strokeLineJoin="round" android:pathData="M4,5 L20,5 L20,19 L4,19 Z M4,10 L20,10 M4,14.5 L20,14.5 M9,10 L9,19" />
</vector>
```

- [ ] **Step 2: 드로어블**

`bg_dot_current.xml`:
```xml
<shape xmlns:android="http://schemas.android.com/apk/res/android" android:shape="oval">
    <solid android:color="?attr/parkPrimary" />
</shape>
```
`bg_dot_done.xml`:
```xml
<shape xmlns:android="http://schemas.android.com/apk/res/android" android:shape="oval">
    <solid android:color="?attr/parkPrimary10" />
</shape>
```
`bg_dot_future.xml`:
```xml
<shape xmlns:android="http://schemas.android.com/apk/res/android" android:shape="oval">
    <solid android:color="?attr/parkBorder" />
</shape>
```
`bg_stepper.xml`:
```xml
<shape xmlns:android="http://schemas.android.com/apk/res/android" android:shape="rectangle">
    <solid android:color="?attr/parkSecondary" />
    <corners android:radius="@dimen/radius_sm" />
    <stroke android:width="@dimen/border_width" android:color="?attr/parkBorder" />
</shape>
```
`bg_medal_circle.xml` (코드에서 `backgroundTintList`로 메달색 지정):
```xml
<shape xmlns:android="http://schemas.android.com/apk/res/android" android:shape="oval">
    <solid android:color="#FFFFFF" />
</shape>
```

- [ ] **Step 3: 색상** — `res/values/colors.xml`의 `<resources>`에 추가:
```xml
<color name="medal_gold">#F5A623</color>
<color name="medal_silver">#A8A8A8</color>
<color name="medal_bronze">#CD7F32</color>
```

- [ ] **Step 4: 문자열** — `strings.xml`에 추가:
```xml
<string name="title_game_play">점수 기록</string>
<string name="cumulative_strokes">누적 %1$d타</string>
<string name="hole_counter">%1$d / %2$d</string>
<string name="view_full_scores">전체 점수 보기</string>
<string name="game_complete_title">경기 완료</string>
<string name="game_finish">게임 완료</string>
<string name="prev_hole_plain">이전 홀</string>
<string name="next_hole_plain">다음 홀</string>
<string name="par_versus">파%1$d 대비 %2$s</string>
<string name="even_label">이븐</string>
<string name="save_and_home">저장하고 홈으로</string>
<string name="grid_hole_col">홀</string>
<string name="grid_par_col">파</string>
<string name="grid_total_row">합계</string>
```
그리고 **값 변경**(이름 유지):
```xml
<string name="title_grid">전체 점수 보기</string>
<string name="title_summary">경기 완료</string>
```

- [ ] **Step 5: 치수** — `dimens.xml`에 추가:
```xml
<dimen name="dot_size">9dp</dimen>
<dimen name="dot_current_width">24dp</dimen>
<dimen name="medal_size">48dp</dimen>
```

- [ ] **Step 6: 빌드**

Run: `./gradlew :app:assembleDebug -q`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 7: 커밋**
```bash
git add app/src/main/res/drawable/ic_trophy.xml app/src/main/res/drawable/ic_medal.xml app/src/main/res/drawable/ic_table.xml app/src/main/res/drawable/bg_dot_current.xml app/src/main/res/drawable/bg_dot_done.xml app/src/main/res/drawable/bg_dot_future.xml app/src/main/res/drawable/bg_stepper.xml app/src/main/res/drawable/bg_medal_circle.xml app/src/main/res/values/colors.xml app/src/main/res/values/strings.xml app/src/main/res/values/dimens.xml
git commit -m "feat: add resources for game-progress redesign (icons, dots, medals)"
```

---

## Task 3: GamePlay (레이아웃 + Fragment)

**Files:**
- Modify: `app/src/main/res/layout/view_player_score_row.xml`, `app/src/main/res/layout/fragment_hole_input.xml`, `app/src/main/java/com/parkgolf/score/ui/hole/HoleInputFragment.kt`

- [ ] **Step 1: `view_player_score_row.xml` 전체 교체** (카드형)
```xml
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:layout_width="match_parent" android:layout_height="wrap_content"
    android:orientation="horizontal" android:gravity="center_vertical"
    android:background="@drawable/bg_card" android:padding="16dp"
    android:layout_marginBottom="12dp">
    <LinearLayout android:layout_width="0dp" android:layout_height="wrap_content"
        android:layout_weight="1" android:orientation="vertical">
        <TextView android:id="@+id/tvPlayer"
            android:layout_width="wrap_content" android:layout_height="wrap_content"
            android:textSize="@dimen/body_text" android:textStyle="bold"
            android:textColor="?attr/parkForeground" />
        <TextView android:id="@+id/tvCumulative"
            android:layout_width="wrap_content" android:layout_height="wrap_content"
            android:textColor="?attr/parkMutedForeground" android:layout_marginTop="2dp" />
    </LinearLayout>
    <ImageButton android:id="@+id/btnMinus"
        android:layout_width="@dimen/touch_min" android:layout_height="@dimen/touch_min"
        android:background="@drawable/bg_stepper" android:src="@drawable/ic_minus"
        app:tint="?attr/parkForeground" android:contentDescription="@string/decrease" />
    <LinearLayout android:layout_width="56dp" android:layout_height="wrap_content"
        android:orientation="vertical" android:gravity="center"
        android:layout_marginHorizontal="8dp">
        <TextView android:id="@+id/tvScore"
            android:layout_width="wrap_content" android:layout_height="wrap_content"
            android:textSize="@dimen/score_text" android:textStyle="bold"
            android:textColor="?attr/parkForeground" />
        <TextView android:id="@+id/tvBadge"
            android:layout_width="wrap_content" android:layout_height="wrap_content"
            android:background="@drawable/bg_pill_secondary" android:paddingHorizontal="8dp"
            android:paddingVertical="1dp" android:textSize="12sp" android:textStyle="bold"
            android:layout_marginTop="2dp" />
    </LinearLayout>
    <ImageButton android:id="@+id/btnPlus"
        android:layout_width="@dimen/touch_min" android:layout_height="@dimen/touch_min"
        android:background="@drawable/bg_stepper" android:src="@drawable/ic_plus"
        app:tint="?attr/parkForeground" android:contentDescription="@string/increase" />
</LinearLayout>
```

- [ ] **Step 2: `fragment_hole_input.xml` 전체 교체**
```xml
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:layout_width="match_parent" android:layout_height="match_parent"
    android:orientation="vertical" android:background="?attr/parkBackground">

    <include android:id="@+id/topBar" layout="@layout/view_top_bar" />

    <!-- 헤더 카드 -->
    <LinearLayout android:layout_width="match_parent" android:layout_height="wrap_content"
        android:orientation="vertical" android:background="@color/card"
        android:padding="@dimen/screen_padding">
        <LinearLayout android:layout_width="wrap_content" android:layout_height="wrap_content"
            android:orientation="horizontal" android:gravity="center_vertical">
            <TextView android:id="@+id/tvVenueChip"
                android:layout_width="wrap_content" android:layout_height="wrap_content"
                android:background="@drawable/bg_pill_primary" android:paddingHorizontal="12dp"
                android:paddingVertical="3dp" android:textColor="?attr/parkPrimary"
                android:textStyle="bold" android:textSize="12sp" />
            <TextView android:layout_width="wrap_content" android:layout_height="wrap_content"
                android:text="/" android:layout_marginHorizontal="6dp"
                android:textColor="?attr/parkMutedForeground" />
            <TextView android:id="@+id/tvCourse"
                android:layout_width="wrap_content" android:layout_height="wrap_content"
                android:textColor="?attr/parkForeground" android:textStyle="bold" android:textSize="12sp" />
        </LinearLayout>

        <LinearLayout android:layout_width="match_parent" android:layout_height="wrap_content"
            android:orientation="horizontal" android:gravity="center_vertical"
            android:layout_marginTop="8dp">
            <TextView android:id="@+id/tvHole"
                android:layout_width="wrap_content" android:layout_height="wrap_content"
                android:textSize="28sp" android:textStyle="bold" android:textColor="?attr/parkForeground" />
            <ImageView android:layout_width="16dp" android:layout_height="16dp"
                android:layout_marginStart="10dp" android:src="@drawable/ic_flag"
                app:tint="?attr/parkMutedForeground" android:importantForAccessibility="no" />
            <TextView android:id="@+id/tvPar"
                android:layout_width="wrap_content" android:layout_height="wrap_content"
                android:layout_marginStart="4dp" android:textColor="?attr/parkMutedForeground"
                android:textStyle="bold" />
            <View android:layout_width="0dp" android:layout_height="1dp" android:layout_weight="1" />
            <TextView android:id="@+id/tvHoleCounter"
                android:layout_width="wrap_content" android:layout_height="wrap_content"
                android:textColor="?attr/parkMutedForeground" android:textStyle="bold" />
        </LinearLayout>

        <HorizontalScrollView android:layout_width="match_parent" android:layout_height="wrap_content"
            android:layout_marginTop="12dp" android:scrollbars="none">
            <LinearLayout android:id="@+id/dotsContainer"
                android:layout_width="wrap_content" android:layout_height="wrap_content"
                android:orientation="horizontal" android:gravity="center_vertical" />
        </HorizontalScrollView>
    </LinearLayout>

    <!-- 플레이어 카드 -->
    <ScrollView android:layout_width="match_parent" android:layout_height="0dp"
        android:layout_weight="1" android:fillViewport="true" android:padding="@dimen/screen_padding">
        <LinearLayout android:id="@+id/playerContainer"
            android:layout_width="match_parent" android:layout_height="wrap_content"
            android:orientation="vertical" />
    </ScrollView>

    <!-- 하단 고정 -->
    <LinearLayout android:layout_width="match_parent" android:layout_height="wrap_content"
        android:orientation="vertical" android:padding="@dimen/screen_padding"
        android:background="?attr/parkBackground">
        <LinearLayout android:layout_width="match_parent" android:layout_height="wrap_content"
            android:orientation="horizontal">
            <Button android:id="@+id/btnPrev" style="?attr/materialButtonOutlinedStyle"
                android:layout_width="0dp" android:layout_height="wrap_content" android:layout_weight="1"
                android:minHeight="@dimen/touch_min" android:text="@string/prev_hole_plain"
                app:icon="@drawable/ic_chevron_left" app:iconGravity="textStart" />
            <Button android:id="@+id/btnNext"
                android:layout_width="0dp" android:layout_height="wrap_content" android:layout_weight="2"
                android:layout_marginStart="8dp" android:minHeight="@dimen/touch_min"
                android:text="@string/next_hole_plain" android:textSize="@dimen/button_text" />
        </LinearLayout>
        <Button android:id="@+id/btnGrid" style="?attr/materialButtonOutlinedStyle"
            android:layout_width="match_parent" android:layout_height="wrap_content"
            android:minHeight="@dimen/touch_min" android:layout_marginTop="8dp"
            android:text="@string/view_full_scores" app:icon="@drawable/ic_table" app:iconGravity="textStart" />
    </LinearLayout>
</LinearLayout>
```

- [ ] **Step 3: `HoleInputFragment.kt` 전체 교체**
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
import com.parkgolf.score.ui.common.confirmYesNo
import com.parkgolf.score.ui.common.onBackPressed
import kotlinx.coroutines.launch

class HoleInputFragment : Fragment(R.layout.fragment_hole_input) {
    private val session: RoundSessionViewModel by activityViewModels()
    private lateinit var holeVm: HoleInputViewModel
    private val dots = mutableListOf<View>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val binding = FragmentHoleInputBinding.bind(view)
        val repo = App.repo(requireActivity().application)
        val round = session.round.value ?: run { findNavController().popBackStack(); return }
        binding.topBar.tvBarTitle.text = getString(R.string.title_game_play)
        val exitToHome = {
            confirmYesNo(R.string.confirm_exit_game) {
                findNavController().popBackStack(R.id.homeFragment, false)
            }
        }
        binding.topBar.btnBack.setOnClickListener { exitToHome() }
        onBackPressed { exitToHome() }
        holeVm = HoleInputViewModel(totalHoles = round.holes.size)

        val rows = round.players.indices.map { p ->
            val rowBinding = ViewPlayerScoreRowBinding.inflate(layoutInflater, binding.playerContainer, false)
            rowBinding.tvPlayer.text = round.players[p]
            binding.playerContainer.addView(rowBinding.root)
            rowBinding
        }
        round.players.indices.forEach { p ->
            rows[p].btnPlus.setOnClickListener {
                session.adjust(p, holeVm.holeIndex.value!!, +1); persist(repo); render(binding, rows)
            }
            rows[p].btnMinus.setOnClickListener {
                session.adjust(p, holeVm.holeIndex.value!!, -1); persist(repo); render(binding, rows)
            }
        }

        // 진행 도트 생성(한 번)
        val dotSize = resources.getDimensionPixelSize(R.dimen.dot_size)
        val dotWide = resources.getDimensionPixelSize(R.dimen.dot_current_width)
        round.holes.indices.forEach { idx ->
            val dot = View(requireContext())
            val lp = android.widget.LinearLayout.LayoutParams(dotSize, dotSize)
            lp.marginEnd = (dotSize * 0.7).toInt()
            dot.layoutParams = lp
            dot.setOnClickListener { holeVm.goTo(idx); render(binding, rows) }
            binding.dotsContainer.addView(dot)
            dots.add(dot)
        }
        // dotWide는 render에서 현재 홀에 적용
        binding.root.tag = dotWide

        binding.btnNext.setOnClickListener {
            if (holeVm.isLastHole()) findNavController().navigate(R.id.roundSummaryFragment)
            else { holeVm.next(); render(binding, rows) }
        }
        binding.btnPrev.setOnClickListener { holeVm.prev(); render(binding, rows) }
        binding.btnGrid.setOnClickListener { findNavController().navigate(R.id.gridViewFragment) }

        findNavController().currentBackStackEntry?.savedStateHandle
            ?.getLiveData<Int>("jumpToHole")?.observe(viewLifecycleOwner) { idx ->
                holeVm.goTo(idx); render(binding, rows)
            }

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
        val total = round.holes.size

        binding.tvVenueChip.text = round.venueName
        binding.tvCourse.text = hole.courseName
        binding.tvHole.text = getString(R.string.hole_label, hole.holeNo)
        binding.tvPar.text = getString(R.string.par_label, hole.par)
        binding.tvHoleCounter.text = getString(R.string.hole_counter, h + 1, total)
        binding.btnPrev.isEnabled = h > 0
        binding.btnNext.text =
            if (holeVm.isLastHole()) getString(R.string.game_finish) else getString(R.string.next_hole_plain)

        // 진행 도트 상태
        val dotSize = resources.getDimensionPixelSize(R.dimen.dot_size)
        val dotWide = (binding.root.tag as Int)
        dots.forEachIndexed { idx, dot ->
            val bg = when {
                idx == h -> R.drawable.bg_dot_current
                idx < h -> R.drawable.bg_dot_done
                else -> R.drawable.bg_dot_future
            }
            dot.setBackgroundResource(bg)
            val lp = dot.layoutParams
            lp.width = if (idx == h) dotWide else dotSize
            dot.layoutParams = lp
        }

        // 플레이어 카드
        round.players.indices.forEach { p ->
            val score = round.scores[p][h]
            rows[p].tvScore.text = score?.toString() ?: "-"
            rows[p].tvCumulative.text =
                getString(R.string.cumulative_strokes, Scoring.runningStrokes(round.scores[p], h))
            val diff = (score ?: hole.par) - hole.par
            rows[p].tvBadge.text = Scoring.relationLabel(diff)
            rows[p].tvBadge.setTextColor(ParColors.colorFor(requireContext(), diff))
            rows[p].tvBadge.setBackgroundResource(
                if (diff < 0) R.drawable.bg_pill_primary else R.drawable.bg_pill_secondary
            )
        }
    }
}
```

> 참고: `app:icon`/`app:iconGravity`는 MaterialButton 속성. `btnPrev`/`btnGrid`가 Material Button 테마를 쓰는지 확인(앱이 Material 테마면 기본 Button도 MaterialButton으로 인플레이트됨). 아니면 아이콘 속성을 제거하고 텍스트만 둔다.

- [ ] **Step 4: 빌드 & 수정**

Run: `./gradlew :app:assembleDebug -q`
Expected: BUILD SUCCESSFUL. ViewBinding 필드명(`tvVenueChip`,`tvCourse`,`tvHole`,`tvPar`,`tvHoleCounter`,`dotsContainer`,`playerContainer`,`btnPrev`,`btnNext`,`btnGrid`, row: `tvPlayer`,`tvCumulative`,`btnMinus`,`tvScore`,`tvBadge`,`btnPlus`) 일치 확인. `app:icon`이 빌드 오류를 내면(비-Material Button) 아이콘 속성 제거.

- [ ] **Step 5: 커밋**
```bash
git add app/src/main/res/layout/view_player_score_row.xml app/src/main/res/layout/fragment_hole_input.xml app/src/main/java/com/parkgolf/score/ui/hole/HoleInputFragment.kt
git commit -m "feat: redesign GamePlay (course chip, progress dots, per-player cumulative + badge)"
```

---

## Task 4: ScoreOverview (레이아웃 + Fragment)

**Files:**
- Modify: `app/src/main/res/layout/fragment_grid_view.xml`, `app/src/main/java/com/parkgolf/score/ui/grid/GridViewFragment.kt`

- [ ] **Step 1: `fragment_grid_view.xml` 전체 교체** (표를 카드로 감싸고 배경 적용)
```xml
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent" android:layout_height="match_parent"
    android:orientation="vertical" android:background="?attr/parkBackground">
    <include android:id="@+id/topBar" layout="@layout/view_top_bar" />
    <ScrollView android:layout_width="match_parent" android:layout_height="0dp"
        android:layout_weight="1" android:padding="@dimen/screen_padding">
        <HorizontalScrollView android:layout_width="match_parent" android:layout_height="wrap_content"
            android:scrollbars="none">
            <TableLayout android:id="@+id/tableGrid"
                android:layout_width="wrap_content" android:layout_height="wrap_content"
                android:background="@drawable/bg_card" />
        </HorizontalScrollView>
    </ScrollView>
</LinearLayout>
```

- [ ] **Step 2: `GridViewFragment.kt` 전체 교체**
```kotlin
package com.parkgolf.score.ui.grid

import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.TableRow
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.google.android.material.color.MaterialColors
import com.parkgolf.score.R
import com.parkgolf.score.databinding.FragmentGridViewBinding
import com.parkgolf.score.domain.Scoring
import com.parkgolf.score.ui.RoundSessionViewModel
import com.parkgolf.score.ui.hole.ParColors

class GridViewFragment : Fragment(R.layout.fragment_grid_view) {
    private val session: RoundSessionViewModel by activityViewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val binding = FragmentGridViewBinding.bind(view)
        binding.topBar.tvBarTitle.text = getString(R.string.title_grid)
        binding.topBar.btnBack.setOnClickListener { findNavController().popBackStack() }
        val round = session.round.value ?: run { findNavController().popBackStack(); return }

        val fg = MaterialColors.getColor(requireView(), R.attr.parkForeground)
        val muted = MaterialColors.getColor(requireView(), R.attr.parkMutedForeground)

        fun cell(text: String, bold: Boolean = false, color: Int = fg): TextView =
            TextView(requireContext()).apply {
                this.text = text; setPadding(28, 22, 28, 22); textSize = 15f
                gravity = Gravity.CENTER; setTextColor(color)
                if (bold) setTypeface(typeface, Typeface.BOLD)
            }

        // 헤더: 홀 | 파 | 플레이어들
        val header = TableRow(requireContext())
        header.addView(cell(getString(R.string.grid_hole_col), bold = true, color = muted))
        header.addView(cell(getString(R.string.grid_par_col), bold = true, color = muted))
        round.players.forEach { header.addView(cell(it, bold = true)) }
        binding.tableGrid.addView(header)

        // 홀별 행
        round.holes.forEachIndexed { h, hole ->
            val row = TableRow(requireContext())
            row.addView(cell(hole.holeNo.toString(), bold = true))
            row.addView(cell(hole.par.toString(), color = muted))
            round.players.indices.forEach { p ->
                val s = round.scores[p][h]
                val color = if (s != null) ParColors.colorFor(requireContext(), s - hole.par) else fg
                row.addView(cell(s?.toString() ?: "-", color = color))
            }
            row.setOnClickListener {
                findNavController().previousBackStackEntry?.savedStateHandle?.set("jumpToHole", h)
                findNavController().popBackStack()
            }
            binding.tableGrid.addView(row)
        }

        // 합계 푸터
        val totals = TableRow(requireContext())
        totals.addView(cell(getString(R.string.grid_total_row), bold = true, color = muted))
        totals.addView(cell(Scoring.parTotal(round.holes).toString(), bold = true, color = muted))
        round.players.indices.forEach { p ->
            val total = Scoring.total(round.scores[p])
            val rel = Scoring.relativeToPar(round.scores[p], round.holes)
            val container = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER
                setPadding(28, 22, 28, 22)
            }
            container.addView(TextView(requireContext()).apply {
                text = total.toString(); textSize = 16f; gravity = Gravity.CENTER
                setTypeface(typeface, Typeface.BOLD); setTextColor(fg)
            })
            container.addView(TextView(requireContext()).apply {
                text = Scoring.relationLabel(rel); textSize = 12f; gravity = Gravity.CENTER
                setTextColor(ParColors.colorFor(requireContext(), rel))
            })
            totals.addView(container)
        }
        binding.tableGrid.addView(totals)
    }
}
```

> 참고: `MaterialColors.getColor(view, R.attr.parkForeground)`는 CourseWizardFragment가 쓰는 테마 속성 resolve 패턴과 동일. 셀 색은 `ParColors.colorFor`(under/even/over_par 고정색) 사용.

- [ ] **Step 3: 빌드**

Run: `./gradlew :app:assembleDebug -q`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 4: 커밋**
```bash
git add app/src/main/res/layout/fragment_grid_view.xml app/src/main/java/com/parkgolf/score/ui/grid/GridViewFragment.kt
git commit -m "feat: redesign ScoreOverview (par column, styled cells, total diff)"
```

---

## Task 5: GameComplete (레이아웃 + item + Fragment)

**Files:**
- Modify: `app/src/main/res/layout/fragment_round_summary.xml`, `app/src/main/res/layout/item_rank_row.xml`, `app/src/main/java/com/parkgolf/score/ui/summary/RoundSummaryFragment.kt`

- [ ] **Step 1: `item_rank_row.xml` 전체 교체** (메달 카드)
```xml
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:id="@+id/rankCard"
    android:layout_width="match_parent" android:layout_height="wrap_content"
    android:orientation="horizontal" android:gravity="center_vertical"
    android:background="@drawable/bg_card" android:padding="18dp"
    android:layout_marginBottom="12dp">
    <FrameLayout android:layout_width="@dimen/medal_size" android:layout_height="@dimen/medal_size">
        <TextView android:id="@+id/tvRankNum"
            android:layout_width="match_parent" android:layout_height="match_parent"
            android:background="@drawable/bg_medal_circle" android:gravity="center"
            android:textStyle="bold" android:textColor="?attr/parkMutedForeground" />
        <ImageView android:id="@+id/ivMedal"
            android:layout_width="match_parent" android:layout_height="match_parent"
            android:padding="12dp" android:src="@drawable/ic_medal"
            android:visibility="gone" android:importantForAccessibility="no" />
    </FrameLayout>
    <LinearLayout android:layout_width="0dp" android:layout_height="wrap_content"
        android:layout_weight="1" android:orientation="vertical" android:layout_marginStart="16dp">
        <TextView android:id="@+id/tvRankLabel"
            android:layout_width="wrap_content" android:layout_height="wrap_content"
            android:textStyle="bold" android:textSize="12sp" android:visibility="gone" />
        <TextView android:id="@+id/tvPlayer"
            android:layout_width="wrap_content" android:layout_height="wrap_content"
            android:textSize="@dimen/body_text" android:textStyle="bold" android:textColor="?attr/parkForeground" />
        <TextView android:id="@+id/tvRelative"
            android:layout_width="wrap_content" android:layout_height="wrap_content"
            android:textColor="?attr/parkMutedForeground" android:layout_marginTop="2dp" />
    </LinearLayout>
    <LinearLayout android:layout_width="wrap_content" android:layout_height="wrap_content"
        android:orientation="vertical" android:gravity="end">
        <TextView android:id="@+id/tvTotal"
            android:layout_width="wrap_content" android:layout_height="wrap_content"
            android:textSize="32sp" android:textStyle="bold" android:textColor="?attr/parkForeground" />
    </LinearLayout>
</LinearLayout>
```

- [ ] **Step 2: `fragment_round_summary.xml` 전체 교체** (트로피 헤더 + 하단 버튼)
```xml
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:layout_width="match_parent" android:layout_height="match_parent"
    android:orientation="vertical" android:background="?attr/parkBackground">
    <include android:id="@+id/topBar" layout="@layout/view_top_bar" />
    <ScrollView android:layout_width="match_parent" android:layout_height="0dp"
        android:layout_weight="1" android:fillViewport="true" android:padding="@dimen/screen_padding">
        <LinearLayout android:layout_width="match_parent" android:layout_height="wrap_content"
            android:orientation="vertical">
            <LinearLayout android:layout_width="match_parent" android:layout_height="wrap_content"
                android:orientation="vertical" android:gravity="center" android:paddingVertical="16dp">
                <ImageView android:layout_width="72dp" android:layout_height="72dp"
                    android:background="@drawable/bg_menu_icon" android:padding="18dp"
                    android:src="@drawable/ic_trophy" app:tint="?attr/parkPrimary"
                    android:importantForAccessibility="no" />
                <TextView android:layout_width="wrap_content" android:layout_height="wrap_content"
                    android:text="@string/game_complete_title" android:textSize="@dimen/title_text"
                    android:textStyle="bold" android:textColor="?attr/parkForeground" android:layout_marginTop="10dp" />
                <TextView android:id="@+id/tvVenue"
                    android:layout_width="wrap_content" android:layout_height="wrap_content"
                    android:textColor="?attr/parkMutedForeground" android:layout_marginTop="2dp" />
            </LinearLayout>
            <LinearLayout android:id="@+id/rankContainer"
                android:layout_width="match_parent" android:layout_height="wrap_content"
                android:orientation="vertical" android:layout_marginTop="8dp" />
        </LinearLayout>
    </ScrollView>
    <LinearLayout android:layout_width="match_parent" android:layout_height="wrap_content"
        android:orientation="vertical" android:padding="@dimen/screen_padding"
        android:background="?attr/parkBackground">
        <Button android:id="@+id/btnDone"
            android:layout_width="match_parent" android:layout_height="wrap_content"
            android:minHeight="72dp" android:text="@string/save_and_home"
            android:textSize="@dimen/title_text" android:textStyle="bold"
            app:icon="@drawable/ic_save" app:iconGravity="textStart" />
    </LinearLayout>
</LinearLayout>
```

- [ ] **Step 3: `RoundSummaryFragment.kt` 전체 교체**
```kotlin
package com.parkgolf.score.ui.summary

import android.os.Bundle
import android.view.View
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.android.material.color.MaterialColors
import com.parkgolf.score.App
import com.parkgolf.score.R
import com.parkgolf.score.databinding.FragmentRoundSummaryBinding
import com.parkgolf.score.databinding.ItemRankRowBinding
import com.parkgolf.score.domain.Scoring
import com.parkgolf.score.domain.model.RoundStatus
import com.parkgolf.score.ui.RoundSessionViewModel
import kotlinx.coroutines.launch

class RoundSummaryFragment : Fragment(R.layout.fragment_round_summary) {
    private val session: RoundSessionViewModel by activityViewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val binding = FragmentRoundSummaryBinding.bind(view)
        binding.topBar.tvBarTitle.text = getString(R.string.title_summary)
        binding.topBar.btnBack.setOnClickListener { findNavController().popBackStack() }
        val repo = App.repo(requireActivity().application)
        val round = session.round.value ?: run { findNavController().popBackStack(); return }

        val course = round.holes.map { it.courseName }.distinct().firstOrNull() ?: ""
        binding.tvVenue.text = if (course.isBlank()) round.venueName else "${round.venueName} · $course"
        val parTotal = Scoring.parTotal(round.holes)

        val medalColors = intArrayOf(R.color.medal_gold, R.color.medal_silver, R.color.medal_bronze)
        val primary = MaterialColors.getColor(requireView(), R.attr.parkPrimary)

        SummaryViewModel.ranking(round).forEach { row ->
            val item = ItemRankRowBinding.inflate(layoutInflater, binding.rankContainer, false)
            item.tvPlayer.text = row.player
            item.tvTotal.text = row.total.toString()

            val relText = when {
                row.relative > 0 -> "+${row.relative}"
                row.relative == 0 -> getString(R.string.even_label)
                else -> row.relative.toString()
            }
            item.tvRelative.text = getString(R.string.par_versus, parTotal, relText)

            if (row.rank in 1..3) {
                val c = ContextCompat.getColor(requireContext(), medalColors[row.rank - 1])
                item.ivMedal.isVisible = true
                item.ivMedal.backgroundTintList = android.content.res.ColorStateList.valueOf(c)
                item.tvRankNum.text = ""
                item.tvRankLabel.isVisible = true
                item.tvRankLabel.text = getString(R.string.rank_suffix, row.rank)
                item.tvRankLabel.setTextColor(c)
            } else {
                item.ivMedal.isVisible = false
                item.tvRankNum.text = row.rank.toString()
                item.tvRankLabel.isVisible = false
            }
            if (row.rank == 1) {
                item.rankCard.setBackgroundResource(R.drawable.bg_pill_primary)
            }
            binding.rankContainer.addView(item.root)
        }

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

> 참고: 1위 카드 강조로 `bg_pill_primary`(primary10 라운드)를 배경에 재사용. 메달 원형은 `ivMedal.backgroundTintList`로 메달색, 아이콘(흰색 stroke)이 위에 얹힘. `tvRankNum`은 4위+에서만 숫자 표시. `ItemRankRowBinding`의 `tvRank`가 사라졌으므로(교체됨) 이전 참조 없음 확인.

- [ ] **Step 4: 빌드**

Run: `./gradlew :app:assembleDebug -q`
Expected: BUILD SUCCESSFUL. `MaterialColors.getColor(view, R.attr.parkPrimary)` 사용이 다른 프래그먼트(CourseWizard)와 동일 패턴인지 확인.

- [ ] **Step 5: 커밋**
```bash
git add app/src/main/res/layout/fragment_round_summary.xml app/src/main/res/layout/item_rank_row.xml app/src/main/java/com/parkgolf/score/ui/summary/RoundSummaryFragment.kt
git commit -m "feat: redesign GameComplete (trophy header, medal ranking cards)"
```

---

## Task 6: 테스트 갱신 + 회귀 + 빌드

**Files:**
- 확인/수정: `app/src/androidTest/...`, 필요 시 문자열 잔재 정리

- [ ] **Step 1: 제거·변경 문자열 잔재 확인**

Run: `grep -rn "cumulative_label\|round_complete\|to_home\|full_grid\|R.string.next_hole\b\|R.string.prev_hole\b\|game_recording" app/src`
- `cumulative_label`, `round_complete`, `to_home`, `full_grid`, `next_hole`, `prev_hole`, `game_recording`가 더 이상 참조되지 않으면(새 프래그먼트가 신규 문자열 사용) `strings.xml`에서 제거. 아직 참조되면 남긴다. (이 프래그먼트들은 Task 3~5에서 신규 문자열로 교체됨.)

- [ ] **Step 2: 계측 테스트 참조 확인**

Run: `grep -rn "hole_input\|HoleInput\|game_recording\|title_grid\|title_summary\|tvCumulative\|btnNext\|tvScore\|round_complete" app/src/androidTest`
- 깨지는 단정(제거된 문자열/뷰 참조)이 있으면 새 것으로 갱신. GamePlay 화면 식별은 `withText(R.string.title_game_play)` 또는 `withId(R.id.btnGrid)` 사용.

- [ ] **Step 3: androidTest 컴파일**

Run: `./gradlew :app:compileDebugAndroidTestKotlin -q`
Expected: BUILD SUCCESSFUL. 실패 시 위 참조 갱신.

- [ ] **Step 4: 전체 빌드 + 단위 테스트**

Run: `./gradlew :app:assembleDebug :app:testDebugUnitTest -q`
Expected: BUILD SUCCESSFUL (단위 전부 통과, `ScoringRunningStrokesTest` 포함).

- [ ] **Step 5: (에뮬레이터 있으면) 설치 스모크**

Run: `./gradlew :app:installDebug -q`
그 후 컨트롤러가 홈→게임 시작→코스 선택→인원→점수 기록→전체 보기→게임 완료 흐름을 캡처 확인(에뮬레이터 없으면 생략).

- [ ] **Step 6: 커밋(변경이 있으면)**
```bash
git add -A
git commit -m "test: update instrumented refs + cleanup unused strings for game-progress"
```
(변경 없으면 커밋 생략.)

---

## 완료 후

모든 태스크 완료 시 superpowers:finishing-a-development-branch로 마무리(단위 테스트 통과 확인 → 에뮬레이터 스모크 → main 병합 옵션 제시). 이후 서브프로젝트 4(기록: 목록·상세, ScoreOverview 재사용)로 이어간다.
