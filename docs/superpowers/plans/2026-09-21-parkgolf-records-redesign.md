# 파크골프 기록(목록·상세) 재디자인 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 게임 기록 목록·상세 화면을 목업(`GameRecords.tsx`, `App.tsx`의 record-detail = ScoreOverview 재사용)에 맞춰 카드 기반으로 재작성한다.

**Architecture:** 목록은 카드형(날짜·구장·코스·총타수·점수배지·플레이어·액션행) + 재디자인 통계 요약 카드 + 빈 상태. 상세는 인게임 ScoreOverview와 공유하는 읽기 전용 점수표. 점수표 렌더 로직을 `ScoreTable` 공용 객체로 추출해 `GridViewFragment`(인게임, 홀 탭→편집)와 `RoundDetailFragment`(기록, 읽기 전용)가 함께 사용한다.

**Tech Stack:** Kotlin, Android View/XML + ViewBinding, MVVM(Fragment→Repo→Room), Jetpack Navigation. 새 도메인 로직 없음(기존 `Scoring`/`Stats` 재사용) + 순수 포맷 헬퍼 1개 신규(TDD).

**태스크 순서 원칙:** 레이아웃(뷰 id)을 그것을 참조하는 코틀린 코드보다 먼저 둔다. 상호 의존하는 어댑터·프래그먼트는 한 태스크로 묶어 각 태스크가 독립적으로 빌드되게 한다.

---

## File Structure

- **신규** `app/src/main/java/com/parkgolf/score/domain/RecordFormat.kt` — 순수 포맷 헬퍼(점수 배지 텍스트, 코스명 도출). TDD.
- **신규** `app/src/test/java/com/parkgolf/score/domain/RecordFormatTest.kt` — 위 단위 테스트.
- **신규** `app/src/main/java/com/parkgolf/score/ui/common/ScoreTable.kt` — Round→TableLayout 공용 렌더러(세션/저장소 비의존).
- **수정** `app/src/main/java/com/parkgolf/score/ui/grid/GridViewFragment.kt` — 표 로직을 `ScoreTable`에 위임.
- **수정** `app/src/main/java/com/parkgolf/score/ui/history/RoundDetailFragment.kt` — id로 로드 후 `ScoreTable` 읽기 전용, 삭제 제거.
- **수정** `app/src/main/res/layout/fragment_round_detail.xml` — ScoreOverview 스타일 표.
- **수정** `app/src/main/res/layout/item_history_round.xml` — 카드 레이아웃.
- **수정** `app/src/main/res/layout/fragment_history.xml` — 통계 요약 카드 + 빈 상태 + RecyclerView.
- **수정** `app/src/main/java/com/parkgolf/score/ui/history/HistoryAdapter.kt` — 카드 바인딩 + onDelete 콜백.
- **수정** `app/src/main/java/com/parkgolf/score/ui/history/HistoryFragment.kt` — 통계 카드 바인딩, 빈 상태 토글, 삭제 배선.
- **신규 리소스** `ic_users.xml`, `bg_pill_muted.xml`, `bg_circle_muted.xml`, `strings.xml` 추가 문구.

---

## Task 1: 리소스(문자열·드로어블) 추가

**Files:**
- Modify: `app/src/main/res/values/strings.xml`
- Create: `app/src/main/res/drawable/ic_users.xml`
- Create: `app/src/main/res/drawable/bg_pill_muted.xml`
- Create: `app/src/main/res/drawable/bg_circle_muted.xml`

- [ ] **Step 1: strings.xml 수정/추가**

`title_round_detail` 문구를 "경기 결과"로 바꾸고 아래 문자열을 `</resources>` 직전에 추가한다.

기존 라인 변경:
```xml
<string name="title_round_detail">경기 결과</string>
```

추가:
```xml
<!-- records redesign -->
<string name="records_empty_title">기록 없음</string>
<string name="records_empty_desc">게임을 완료하면 여기에 기록됩니다</string>
<string name="view_result">경기 결과 보기</string>
<string name="stat_avg">평균</string>
<string name="stat_best">최저</string>
<string name="stat_worst">최고</string>
```

- [ ] **Step 2: ic_users.xml 생성 (lucide users)**

```xml
<vector xmlns:android="http://schemas.android.com/apk/res/android" android:width="24dp" android:height="24dp" android:viewportWidth="24" android:viewportHeight="24">
    <path android:strokeColor="?attr/parkMutedForeground" android:strokeWidth="2" android:strokeLineCap="round" android:strokeLineJoin="round" android:pathData="M16,21 L16,19 A4,4 0 0 0 12,15 L6,15 A4,4 0 0 0 2,19 L2,21 M13,7 A4,4 0 1 1 5,7 A4,4 0 0 1 13,7 Z M22,21 L22,19 A4,4 0 0 0 19,15.13 M16,3.13 A4,4 0 0 1 16,10.88" />
</vector>
```

- [ ] **Step 3: bg_pill_muted.xml 생성**

```xml
<?xml version="1.0" encoding="utf-8"?>
<shape xmlns:android="http://schemas.android.com/apk/res/android" android:shape="rectangle">
    <solid android:color="?attr/parkMuted" />
    <corners android:radius="@dimen/radius_pill" />
</shape>
```

- [ ] **Step 4: bg_circle_muted.xml 생성 (빈 상태 원)**

```xml
<?xml version="1.0" encoding="utf-8"?>
<shape xmlns:android="http://schemas.android.com/apk/res/android" android:shape="oval">
    <solid android:color="?attr/parkMuted" />
</shape>
```

- [ ] **Step 5: 빌드 확인**

Run: `./gradlew :app:assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 6: 커밋**

```bash
git add app/src/main/res/values/strings.xml app/src/main/res/drawable/ic_users.xml app/src/main/res/drawable/bg_pill_muted.xml app/src/main/res/drawable/bg_circle_muted.xml
git commit -m "chore: strings and drawables for records redesign"
```

---

## Task 2: RecordFormat 순수 헬퍼 (TDD)

목록 카드의 점수 배지 텍스트와 코스명 도출을 순수 함수로 만들어 테스트한다. 배지 색상은 목업 기준(언더=primary, E=muted, 오버=secondary)이며 색상 결정은 뷰 계층에서 처리하므로 여기서는 **텍스트만** 담당한다.

**Files:**
- Create: `app/src/main/java/com/parkgolf/score/domain/RecordFormat.kt`
- Test: `app/src/test/java/com/parkgolf/score/domain/RecordFormatTest.kt`

- [ ] **Step 1: 실패하는 테스트 작성**

```kotlin
package com.parkgolf.score.domain

import com.parkgolf.score.domain.model.HoleSpec
import com.parkgolf.score.domain.model.Round
import com.parkgolf.score.domain.model.RoundStatus
import org.junit.Assert.assertEquals
import org.junit.Test

class RecordFormatTest {
    private fun round(courseNames: List<String>): Round {
        val holes = courseNames.mapIndexed { i, c -> HoleSpec(c, i + 1, 3) }
        return Round(
            id = 1, date = 0L, venueName = "V",
            players = listOf("나"), holes = holes,
            scores = listOf(List(holes.size) { 3 }), status = RoundStatus.COMPLETED
        )
    }

    @Test fun badge_under_shows_signed_negative() {
        assertEquals("-3", RecordFormat.badgeText(-3))
    }

    @Test fun badge_even_shows_E() {
        assertEquals("E", RecordFormat.badgeText(0))
    }

    @Test fun badge_over_shows_plus() {
        assertEquals("+2", RecordFormat.badgeText(2))
    }

    @Test fun courseName_takes_first_non_blank() {
        assertEquals("A코스", RecordFormat.courseName(round(listOf("", "A코스", "A코스"))))
    }

    @Test fun courseName_blank_when_none() {
        assertEquals("", RecordFormat.courseName(round(listOf("", ""))))
    }
}
```

- [ ] **Step 2: 테스트 실패 확인**

Run: `./gradlew :app:testDebugUnitTest --tests "com.parkgolf.score.domain.RecordFormatTest"`
Expected: FAIL (RecordFormat 미정의로 컴파일 에러)

- [ ] **Step 3: 최소 구현**

```kotlin
package com.parkgolf.score.domain

import com.parkgolf.score.domain.model.Round

/** 기록 목록 카드용 순수 포맷 헬퍼. 색상은 뷰 계층에서 결정. */
object RecordFormat {
    /** 파 대비 차이를 배지 텍스트로. 언더=음수 그대로, 이븐="E", 오버="+n". */
    fun badgeText(diff: Int): String = when {
        diff < 0 -> diff.toString()
        diff == 0 -> "E"
        else -> "+$diff"
    }

    /** 라운드 홀들의 코스명 중 첫 비어있지 않은 값. 없으면 "". */
    fun courseName(round: Round): String =
        round.holes.map { it.courseName }.firstOrNull { it.isNotBlank() } ?: ""
}
```

- [ ] **Step 4: 테스트 통과 확인**

Run: `./gradlew :app:testDebugUnitTest --tests "com.parkgolf.score.domain.RecordFormatTest"`
Expected: PASS (5 tests)

- [ ] **Step 5: 커밋**

```bash
git add app/src/main/java/com/parkgolf/score/domain/RecordFormat.kt app/src/test/java/com/parkgolf/score/domain/RecordFormatTest.kt
git commit -m "feat: RecordFormat helper for score badge and course name"
```

---

## Task 3: ScoreTable 공용 렌더러 + GridViewFragment 위임

인게임 전체 점수표(`GridViewFragment`)의 표 생성 로직을 `ScoreTable.render`로 옮기고, 홀 행 탭 콜백을 파라미터화한다. 인게임 동작(홀 탭→해당 홀 편집)은 그대로 유지한다.

**Files:**
- Create: `app/src/main/java/com/parkgolf/score/ui/common/ScoreTable.kt`
- Modify: `app/src/main/java/com/parkgolf/score/ui/grid/GridViewFragment.kt`

- [ ] **Step 1: ScoreTable.kt 생성**

기존 `GridViewFragment`의 셀/헤더/행/합계 로직을 그대로 옮기되, 뷰 대신 `TableLayout`을 받고 `onHoleClick`이 null이면 행 클릭 리스너를 달지 않는다.

```kotlin
package com.parkgolf.score.ui.common

import android.graphics.Typeface
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.TableLayout
import android.widget.TableRow
import android.widget.TextView
import com.google.android.material.color.MaterialColors
import com.parkgolf.score.R
import com.parkgolf.score.domain.Scoring
import com.parkgolf.score.domain.model.Round
import com.parkgolf.score.ui.hole.ParColors

/**
 * 라운드를 전체 점수표로 렌더한다. 인게임(홀 탭→편집)과 기록 상세(읽기 전용)가 공유.
 * @param onHoleClick null이면 읽기 전용(행 탭 없음).
 */
object ScoreTable {
    fun render(table: TableLayout, round: Round, onHoleClick: ((holeIndex: Int) -> Unit)? = null) {
        val ctx = table.context
        table.removeAllViews()
        val fg = MaterialColors.getColor(table, R.attr.parkForeground)
        val muted = MaterialColors.getColor(table, R.attr.parkMutedForeground)

        fun cell(text: String, bold: Boolean = false, color: Int = fg): TextView =
            TextView(ctx).apply {
                this.text = text; setPadding(28, 22, 28, 22); textSize = 15f
                gravity = Gravity.CENTER; setTextColor(color)
                if (bold) setTypeface(typeface, Typeface.BOLD)
            }

        val header = TableRow(ctx)
        header.addView(cell(ctx.getString(R.string.grid_hole_col), bold = true, color = muted))
        header.addView(cell(ctx.getString(R.string.grid_par_col), bold = true, color = muted))
        round.players.forEach { header.addView(cell(it, bold = true)) }
        table.addView(header)

        round.holes.forEachIndexed { h, hole ->
            val row = TableRow(ctx)
            row.addView(cell(hole.holeNo.toString(), bold = true))
            row.addView(cell(hole.par.toString(), color = muted))
            round.players.indices.forEach { p ->
                val s = round.scores[p][h]
                val color = if (s != null) ParColors.colorFor(ctx, s - hole.par) else fg
                row.addView(cell(s?.toString() ?: "-", color = color))
            }
            if (onHoleClick != null) row.setOnClickListener { onHoleClick(h) }
            table.addView(row)
        }

        val totals = TableRow(ctx)
        totals.addView(cell(ctx.getString(R.string.grid_total_row), bold = true, color = muted))
        totals.addView(cell(Scoring.parTotal(round.holes).toString(), bold = true, color = muted))
        round.players.indices.forEach { p ->
            val total = Scoring.total(round.scores[p])
            val rel = Scoring.relativeToPar(round.scores[p], round.holes)
            val container = LinearLayout(ctx).apply {
                orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER
                setPadding(28, 22, 28, 22)
            }
            container.addView(TextView(ctx).apply {
                text = total.toString(); textSize = 16f; gravity = Gravity.CENTER
                setTypeface(typeface, Typeface.BOLD); setTextColor(fg)
            })
            container.addView(TextView(ctx).apply {
                text = Scoring.relationLabel(rel); textSize = 12f; gravity = Gravity.CENTER
                setTextColor(ParColors.colorFor(ctx, rel))
            })
            totals.addView(container)
        }
        table.addView(totals)
    }
}
```

- [ ] **Step 2: GridViewFragment.kt를 위임으로 교체**

`onViewCreated` 전체를 아래로 교체한다(표 생성 코드 제거, `ScoreTable.render` 호출).

```kotlin
package com.parkgolf.score.ui.grid

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.parkgolf.score.R
import com.parkgolf.score.databinding.FragmentGridViewBinding
import com.parkgolf.score.ui.RoundSessionViewModel
import com.parkgolf.score.ui.common.ScoreTable

class GridViewFragment : Fragment(R.layout.fragment_grid_view) {
    private val session: RoundSessionViewModel by activityViewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val binding = FragmentGridViewBinding.bind(view)
        binding.topBar.tvBarTitle.text = getString(R.string.title_grid)
        binding.topBar.btnBack.setOnClickListener { findNavController().popBackStack() }
        val round = session.round.value ?: run { findNavController().popBackStack(); return }

        ScoreTable.render(binding.tableGrid, round) { h ->
            findNavController().previousBackStackEntry?.savedStateHandle?.set("jumpToHole", h)
            findNavController().popBackStack()
        }
    }
}
```

- [ ] **Step 3: 빌드 확인**

Run: `./gradlew :app:assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 4: 커밋**

```bash
git add app/src/main/java/com/parkgolf/score/ui/common/ScoreTable.kt app/src/main/java/com/parkgolf/score/ui/grid/GridViewFragment.kt
git commit -m "refactor: extract ScoreTable renderer shared by grid and detail"
```

---

## Task 4: 상세 화면 읽기 전용 점수표

`RoundDetailFragment`를 id로 라운드를 로드해 `ScoreTable` 읽기 전용으로 렌더하도록 바꾸고, 하단 삭제 버튼과 raw 테이블을 제거한다. 레이아웃을 ScoreOverview(`fragment_grid_view.xml`)와 동일 구조로 교체한다.

**Files:**
- Modify: `app/src/main/res/layout/fragment_round_detail.xml`
- Modify: `app/src/main/java/com/parkgolf/score/ui/history/RoundDetailFragment.kt`

- [ ] **Step 1: fragment_round_detail.xml 교체**

```xml
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent" android:layout_height="match_parent"
    android:orientation="vertical" android:background="?attr/parkBackground">
    <include android:id="@+id/topBar" layout="@layout/view_top_bar" />
    <ScrollView android:layout_width="match_parent" android:layout_height="0dp"
        android:layout_weight="1" android:padding="@dimen/screen_padding">
        <HorizontalScrollView android:layout_width="match_parent" android:layout_height="wrap_content"
            android:fillViewport="true" android:scrollbars="horizontal">
            <TableLayout android:id="@+id/tableGrid"
                android:layout_width="wrap_content" android:layout_height="wrap_content"
                android:stretchColumns="*"
                android:background="@drawable/bg_card" />
        </HorizontalScrollView>
    </ScrollView>
</LinearLayout>
```

- [ ] **Step 2: RoundDetailFragment.kt 교체**

```kotlin
package com.parkgolf.score.ui.history

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.parkgolf.score.App
import com.parkgolf.score.R
import com.parkgolf.score.databinding.FragmentRoundDetailBinding
import com.parkgolf.score.ui.common.ScoreTable
import kotlinx.coroutines.launch

class RoundDetailFragment : Fragment(R.layout.fragment_round_detail) {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val binding = FragmentRoundDetailBinding.bind(view)
        binding.topBar.tvBarTitle.text = getString(R.string.title_round_detail)
        binding.topBar.btnBack.setOnClickListener { findNavController().popBackStack() }
        val repo = App.repo(requireActivity().application)
        val roundId = findNavController().previousBackStackEntry
            ?.savedStateHandle?.get<Long>("roundId") ?: run { findNavController().popBackStack(); return }

        viewLifecycleOwner.lifecycleScope.launch {
            val round = repo.getRound(roundId) ?: run { findNavController().popBackStack(); return@launch }
            ScoreTable.render(binding.tableGrid, round)  // 읽기 전용(onHoleClick 없음)
        }
    }
}
```

- [ ] **Step 3: 빌드 확인**

Run: `./gradlew :app:assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 4: 커밋**

```bash
git add app/src/main/res/layout/fragment_round_detail.xml app/src/main/java/com/parkgolf/score/ui/history/RoundDetailFragment.kt
git commit -m "feat: record detail reuses read-only ScoreTable (경기 결과)"
```

---

## Task 5: 기록 카드 레이아웃

목업 카드 구조로 `item_history_round.xml`을 교체한다: 상단 정보 행(날짜/구장/코스 + 총타수/파/배지), 플레이어 행, 구분선, 액션 행(경기 결과 보기 / 삭제). 이 태스크는 뷰만 추가하므로 어댑터(Task 7)보다 먼저 두어 id를 제공한다.

**Files:**
- Modify: `app/src/main/res/layout/item_history_round.xml`

- [ ] **Step 1: item_history_round.xml 교체**

```xml
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent" android:layout_height="wrap_content"
    android:orientation="vertical" android:background="@drawable/bg_card"
    android:layout_marginBottom="12dp">

    <!-- 상단 정보 행 -->
    <LinearLayout android:layout_width="match_parent" android:layout_height="wrap_content"
        android:orientation="horizontal" android:paddingHorizontal="20dp"
        android:paddingTop="18dp" android:paddingBottom="12dp">
        <LinearLayout android:layout_width="0dp" android:layout_height="wrap_content"
            android:layout_weight="1" android:orientation="vertical">
            <TextView android:id="@+id/tvDate"
                android:layout_width="wrap_content" android:layout_height="wrap_content"
                android:textColor="?attr/parkMutedForeground" android:textSize="14sp" />
            <TextView android:id="@+id/tvVenue"
                android:layout_width="match_parent" android:layout_height="wrap_content"
                android:textColor="?attr/parkForeground" android:textSize="20sp"
                android:textStyle="bold" android:singleLine="true" android:ellipsize="end"
                android:layout_marginTop="1dp" />
            <TextView android:id="@+id/tvCourse"
                android:layout_width="wrap_content" android:layout_height="wrap_content"
                android:textColor="?attr/parkMutedForeground" android:textSize="16sp" />
        </LinearLayout>
        <LinearLayout android:layout_width="wrap_content" android:layout_height="wrap_content"
            android:orientation="vertical" android:gravity="end"
            android:layout_marginStart="12dp">
            <TextView android:id="@+id/tvTotal"
                android:layout_width="wrap_content" android:layout_height="wrap_content"
                android:textColor="?attr/parkForeground" android:textSize="34sp"
                android:textStyle="bold" />
            <LinearLayout android:layout_width="wrap_content" android:layout_height="wrap_content"
                android:orientation="horizontal" android:gravity="center_vertical"
                android:layout_marginTop="2dp">
                <TextView android:id="@+id/tvPar"
                    android:layout_width="wrap_content" android:layout_height="wrap_content"
                    android:textColor="?attr/parkMutedForeground" android:textSize="14sp"
                    android:layout_marginEnd="6dp" />
                <TextView android:id="@+id/tvBadge"
                    android:layout_width="wrap_content" android:layout_height="wrap_content"
                    android:paddingHorizontal="10dp" android:paddingVertical="3dp"
                    android:textSize="14sp" android:textStyle="bold" />
            </LinearLayout>
        </LinearLayout>
    </LinearLayout>

    <!-- 플레이어 행 -->
    <LinearLayout android:layout_width="match_parent" android:layout_height="wrap_content"
        android:orientation="horizontal" android:gravity="center_vertical"
        android:paddingHorizontal="20dp" android:paddingBottom="14dp">
        <ImageView android:layout_width="16dp" android:layout_height="16dp"
            android:src="@drawable/ic_users" android:importantForAccessibility="no" />
        <TextView android:id="@+id/tvPlayers"
            android:layout_width="0dp" android:layout_height="wrap_content" android:layout_weight="1"
            android:layout_marginStart="8dp" android:textColor="?attr/parkMutedForeground"
            android:textSize="14sp" android:singleLine="true" android:ellipsize="end" />
    </LinearLayout>

    <!-- 구분선 -->
    <View android:layout_width="match_parent" android:layout_height="2dp"
        android:background="?attr/parkBorder" />

    <!-- 액션 행 -->
    <LinearLayout android:layout_width="match_parent" android:layout_height="wrap_content"
        android:orientation="horizontal">
        <LinearLayout android:id="@+id/btnViewResult"
            android:layout_width="0dp" android:layout_height="wrap_content" android:layout_weight="1"
            android:orientation="horizontal" android:gravity="center"
            android:minHeight="@dimen/touch_min" android:clickable="true" android:focusable="true"
            android:background="?attr/selectableItemBackground">
            <ImageView android:layout_width="18dp" android:layout_height="18dp"
                android:src="@drawable/ic_table" android:importantForAccessibility="no" />
            <TextView android:layout_width="wrap_content" android:layout_height="wrap_content"
                android:text="@string/view_result" android:textColor="?attr/parkMutedForeground"
                android:textSize="16sp" android:textStyle="bold" android:layout_marginStart="8dp" />
        </LinearLayout>
        <View android:layout_width="2dp" android:layout_height="match_parent"
            android:background="?attr/parkBorder" />
        <LinearLayout android:id="@+id/btnDelete"
            android:layout_width="wrap_content" android:layout_height="match_parent"
            android:orientation="horizontal" android:gravity="center"
            android:minWidth="72dp" android:minHeight="@dimen/touch_min"
            android:clickable="true" android:focusable="true"
            android:background="?attr/selectableItemBackground">
            <ImageView android:layout_width="18dp" android:layout_height="18dp"
                android:src="@drawable/ic_trash" android:importantForAccessibility="no" />
        </LinearLayout>
    </LinearLayout>
</LinearLayout>
```

- [ ] **Step 2: 빌드 확인**

Run: `./gradlew :app:assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: 커밋**

```bash
git add app/src/main/res/layout/item_history_round.xml
git commit -m "feat: record list card layout (mockup)"
```

---

## Task 6: 목록 레이아웃 — 통계 카드 + 빈 상태

`fragment_history.xml`에 재디자인된 통계 요약 카드와 빈 상태 뷰를 추가한다. 프래그먼트(Task 7)가 `statsCard`/`emptyView` id를 참조하므로 먼저 둔다.

**Files:**
- Modify: `app/src/main/res/layout/fragment_history.xml`

- [ ] **Step 1: fragment_history.xml 교체**

```xml
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:layout_width="match_parent" android:layout_height="match_parent"
    android:orientation="vertical" android:background="?attr/parkBackground">
    <include android:id="@+id/topBar" layout="@layout/view_top_bar" />

    <!-- 통계 요약 카드 -->
    <LinearLayout android:id="@+id/statsCard"
        android:layout_width="match_parent" android:layout_height="wrap_content"
        android:orientation="horizontal" android:background="@drawable/bg_card"
        android:layout_marginHorizontal="@dimen/screen_padding" android:layout_marginTop="12dp"
        android:paddingVertical="16dp">
        <LinearLayout android:layout_width="0dp" android:layout_height="wrap_content"
            android:layout_weight="1" android:orientation="vertical" android:gravity="center">
            <TextView android:layout_width="wrap_content" android:layout_height="wrap_content"
                android:text="@string/stat_avg" android:textColor="?attr/parkMutedForeground"
                android:textSize="14sp" />
            <TextView android:id="@+id/tvAvg"
                android:layout_width="wrap_content" android:layout_height="wrap_content"
                android:textColor="?attr/parkForeground" android:textSize="22sp"
                android:textStyle="bold" android:layout_marginTop="2dp" />
        </LinearLayout>
        <LinearLayout android:layout_width="0dp" android:layout_height="wrap_content"
            android:layout_weight="1" android:orientation="vertical" android:gravity="center">
            <TextView android:layout_width="wrap_content" android:layout_height="wrap_content"
                android:text="@string/stat_best" android:textColor="?attr/parkMutedForeground"
                android:textSize="14sp" />
            <TextView android:id="@+id/tvBest"
                android:layout_width="wrap_content" android:layout_height="wrap_content"
                android:textColor="?attr/parkForeground" android:textSize="22sp"
                android:textStyle="bold" android:layout_marginTop="2dp" />
        </LinearLayout>
        <LinearLayout android:layout_width="0dp" android:layout_height="wrap_content"
            android:layout_weight="1" android:orientation="vertical" android:gravity="center">
            <TextView android:layout_width="wrap_content" android:layout_height="wrap_content"
                android:text="@string/stat_worst" android:textColor="?attr/parkMutedForeground"
                android:textSize="14sp" />
            <TextView android:id="@+id/tvWorst"
                android:layout_width="wrap_content" android:layout_height="wrap_content"
                android:textColor="?attr/parkForeground" android:textSize="22sp"
                android:textStyle="bold" android:layout_marginTop="2dp" />
        </LinearLayout>
    </LinearLayout>

    <FrameLayout android:layout_width="match_parent" android:layout_height="0dp"
        android:layout_weight="1">
        <androidx.recyclerview.widget.RecyclerView android:id="@+id/rvRounds"
            android:layout_width="match_parent" android:layout_height="match_parent"
            android:paddingHorizontal="@dimen/screen_padding" android:paddingTop="12dp"
            android:paddingBottom="12dp" android:clipToPadding="false" />

        <!-- 빈 상태 -->
        <LinearLayout android:id="@+id/emptyView"
            android:layout_width="match_parent" android:layout_height="match_parent"
            android:orientation="vertical" android:gravity="center" android:visibility="gone">
            <FrameLayout android:layout_width="80dp" android:layout_height="80dp">
                <View android:layout_width="match_parent" android:layout_height="match_parent"
                    android:background="@drawable/bg_circle_muted" />
                <ImageView android:layout_width="match_parent" android:layout_height="match_parent"
                    android:src="@drawable/ic_trophy" android:padding="22dp"
                    app:tint="?attr/parkMutedForeground" android:importantForAccessibility="no" />
            </FrameLayout>
            <TextView android:layout_width="wrap_content" android:layout_height="wrap_content"
                android:text="@string/records_empty_title" android:textColor="?attr/parkForeground"
                android:textSize="20sp" android:textStyle="bold" android:layout_marginTop="16dp" />
            <TextView android:layout_width="wrap_content" android:layout_height="wrap_content"
                android:text="@string/records_empty_desc" android:textColor="?attr/parkMutedForeground"
                android:textSize="16sp" android:layout_marginTop="4dp" />
        </LinearLayout>
    </FrameLayout>
</LinearLayout>
```

- [ ] **Step 2: 빌드 확인**

Run: `./gradlew :app:assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: 커밋**

```bash
git add app/src/main/res/layout/fragment_history.xml
git commit -m "feat: history stats summary card and empty state layout"
```

---

## Task 7: HistoryAdapter + HistoryFragment 배선 (통합)

어댑터와 프래그먼트는 서로의 시그니처에 의존하므로 한 태스크로 묶는다. 카드 바인딩(날짜/구장/코스/총타수/파/배지/플레이어), 상세 이동·삭제 콜백, 통계 카드 값, 빈 상태 토글을 배선한다. 배지 색상은 목업 기준(언더=primary/parkPrimary10 pill, E=muted/parkMuted pill, 오버=foreground/parkSecondary pill).

**Files:**
- Modify: `app/src/main/java/com/parkgolf/score/ui/history/HistoryAdapter.kt`
- Modify: `app/src/main/java/com/parkgolf/score/ui/history/HistoryFragment.kt`

- [ ] **Step 1: HistoryAdapter.kt 교체**

```kotlin
package com.parkgolf.score.ui.history

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.color.MaterialColors
import com.parkgolf.score.R
import com.parkgolf.score.databinding.ItemHistoryRoundBinding
import com.parkgolf.score.domain.RecordFormat
import com.parkgolf.score.domain.Scoring
import com.parkgolf.score.domain.model.Round
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class HistoryAdapter(
    private val onView: (Long) -> Unit,
    private val onDelete: (Round) -> Unit,
) : RecyclerView.Adapter<HistoryAdapter.VH>() {
    private val items = mutableListOf<Round>()
    @SuppressLint("NotifyDataSetChanged")
    fun submit(list: List<Round>) { items.clear(); items.addAll(list); notifyDataSetChanged() }
    inner class VH(val b: ItemHistoryRoundBinding) : RecyclerView.ViewHolder(b.root)
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VH(ItemHistoryRoundBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val r = items[position]
        val ctx = holder.b.root.context
        holder.b.tvDate.text = SimpleDateFormat("M월 d일 (E)", Locale.KOREA).format(Date(r.date))
        holder.b.tvVenue.text = r.venueName

        val course = RecordFormat.courseName(r)
        holder.b.tvCourse.isVisible = course.isNotBlank()
        holder.b.tvCourse.text = course

        val total = Scoring.total(r.scores[0])
        val par = Scoring.parTotal(r.holes)
        val diff = total - par
        holder.b.tvTotal.text = total.toString()
        holder.b.tvPar.text = ctx.getString(R.string.grid_par_col) + par  // "파27"
        holder.b.tvBadge.text = RecordFormat.badgeText(diff)

        // 배지 색상/배경 (목업: 언더=primary, E=muted, 오버=foreground/secondary)
        when {
            diff < 0 -> {
                holder.b.tvBadge.setBackgroundResource(R.drawable.bg_pill_primary)
                holder.b.tvBadge.setTextColor(MaterialColors.getColor(holder.b.tvBadge, R.attr.parkPrimary))
            }
            diff == 0 -> {
                holder.b.tvBadge.setBackgroundResource(R.drawable.bg_pill_muted)
                holder.b.tvBadge.setTextColor(MaterialColors.getColor(holder.b.tvBadge, R.attr.parkMutedForeground))
            }
            else -> {
                holder.b.tvBadge.setBackgroundResource(R.drawable.bg_pill_secondary)
                holder.b.tvBadge.setTextColor(MaterialColors.getColor(holder.b.tvBadge, R.attr.parkForeground))
            }
        }

        holder.b.tvPlayers.text = r.players.joinToString(", ")
        holder.b.btnViewResult.setOnClickListener { onView(r.id) }
        holder.b.btnDelete.setOnClickListener { onDelete(r) }
    }
}
```

참고: `tvPar` 텍스트는 "파27" 형태(목업의 `파{par}`). `getString(grid_par_col)`="파"에 파 합계를 붙인다.

- [ ] **Step 2: HistoryFragment.kt 교체**

```kotlin
package com.parkgolf.score.ui.history

import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.parkgolf.score.App
import com.parkgolf.score.R
import com.parkgolf.score.data.ParkGolfRepository
import com.parkgolf.score.databinding.FragmentHistoryBinding
import com.parkgolf.score.domain.model.Round
import kotlinx.coroutines.launch

class HistoryFragment : Fragment(R.layout.fragment_history) {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val binding = FragmentHistoryBinding.bind(view)
        binding.topBar.tvBarTitle.text = getString(R.string.title_history)
        binding.topBar.btnBack.setOnClickListener { findNavController().popBackStack() }
        val repo = App.repo(requireActivity().application)
        binding.rvRounds.layoutManager = LinearLayoutManager(requireContext())
        val adapter = HistoryAdapter(
            onView = { roundId ->
                findNavController().currentBackStackEntry?.savedStateHandle?.set("roundId", roundId)
                findNavController().navigate(R.id.roundDetailFragment)
            },
            onDelete = { round -> confirmDelete(repo, round) },
        )
        binding.rvRounds.adapter = adapter

        viewLifecycleOwner.lifecycleScope.launch {
            repo.observeCompletedRounds().collect { rounds ->
                adapter.submit(rounds)
                val empty = rounds.isEmpty()
                binding.emptyView.isVisible = empty
                binding.rvRounds.isVisible = !empty
                binding.statsCard.isVisible = !empty

                val stats = HistoryViewModel.computeStats(rounds, recentN = 10)
                binding.tvAvg.text = stats.recentAverage?.let { "%.1f".format(it) } ?: "-"
                binding.tvBest.text = stats.best?.toString() ?: "-"
                binding.tvWorst.text = stats.worst?.toString() ?: "-"
            }
        }
    }

    private fun confirmDelete(repo: ParkGolfRepository, round: Round) {
        MaterialAlertDialogBuilder(requireContext())
            .setMessage(R.string.delete_confirm)
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton(R.string.delete) { _, _ ->
                viewLifecycleOwner.lifecycleScope.launch { repo.deleteRound(round.id) }
            }.show()
    }
}
```

참고: 통계 숫자는 카드 라벨과 분리(숫자만 표기). 평균은 소수 1자리, 값 없으면 "-". 삭제 후 `observeCompletedRounds` Flow가 목록·통계·빈 상태를 자동 갱신.

- [ ] **Step 3: 빌드 확인**

Run: `./gradlew :app:assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 4: 단위 테스트**

Run: `./gradlew :app:testDebugUnitTest`
Expected: BUILD SUCCESSFUL (RecordFormat 5 + 기존 테스트)

- [ ] **Step 5: 커밋**

```bash
git add app/src/main/java/com/parkgolf/score/ui/history/HistoryAdapter.kt app/src/main/java/com/parkgolf/score/ui/history/HistoryFragment.kt
git commit -m "feat: history card binding, stats card, empty state, card delete"
```

---

## Task 8: 에뮬레이터 스모크 테스트

**Files:** 없음(검증만). 필요 시 앞 태스크 수정.

- [ ] **Step 1: 설치**

Run: `./gradlew :app:installDebug` (에뮬레이터 `emulator-5554`)
Expected: Installed on 1 device.

- [ ] **Step 2: 목록 카드 확인**

완료 라운드가 있는 상태로 홈 → 게임 기록. 확인:
- 카드에 날짜 "M월 d일 (요일)", 구장명(굵게), 코스명(있으면), 우측 큰 총타수 + "파{합계}" + 배지.
- 배지 색: 언더=primary pill / E=muted pill / 오버=secondary pill. (여러 점수의 라운드로 3종 확인)
- 플레이어 행에 인원 아이콘 + 이름들.
- 상단 통계 카드 평균/최저/최고 값.
- adb 캡처: `$HOME/Library/Android/sdk/platform-tools/adb exec-out screencap -p > /tmp/rec.png`

- [ ] **Step 3: 상세(경기 결과) 확인**

카드의 "경기 결과 보기" → 상단바 "경기 결과", 전체 너비 점수표(파 색상, 인원 많으면 가로 스크롤). 홀 행 탭이 **아무 동작 안 함**(읽기 전용) 확인.

- [ ] **Step 4: 삭제 확인**

카드의 삭제(휴지통) → 확인 다이얼로그 → 삭제 → 목록/통계 갱신.

- [ ] **Step 5: 빈 상태 확인**

모든 기록 삭제 → 트로피 원 + "기록 없음"/"게임을 완료하면 여기에 기록됩니다", 통계 카드 숨김.

- [ ] **Step 6: 인게임 회귀**

게임 진행 중 "전체 점수 보기"에서 홀 행 탭 → 해당 홀 편집으로 이동(기존 동작 유지) 확인.

- [ ] **Step 7: 문제 발견 시 수정 후 재검증, 이상 없으면 완료(추가 커밋이 있으면 커밋).**

---

## Self-Review 메모
- 스펙 커버리지: 통계 카드(유지·재디자인) ✔(T6/T7), 카드 목록·배지·플레이어·액션 ✔(T5/T7), 빈 상태 ✔(T6/T7), 삭제 목록 이동 ✔(T7), 상세=읽기전용 ScoreTable ✔(T3/T4), 공용 렌더러 ✔(T3), 도메인 무변경 ✔(T2만 순수 헬퍼 추가).
- 타입 일관성: `HistoryAdapter(onView, onDelete)` T7 어댑터 정의·T7 프래그먼트 사용 일치. `ScoreTable.render(table, round, onHoleClick?)` T3 정의·T4 사용 일치.
- 빌드 독립성: 레이아웃(T5 카드, T6 목록)을 코드(T7)보다 먼저 배치, 상호 의존 어댑터·프래그먼트를 T7로 통합 → 각 태스크 assembleDebug 성공.
- 미사용 정리: 기존 `RoundDetailFragment`의 삭제 UI 제거로 `delete_round` 문자열이 기록 화면에서 미사용이 될 수 있으나 코스 관리(#5)에서도 삭제 UI가 쓰이므로 존치.
