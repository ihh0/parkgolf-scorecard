# 주변 구장 앱 기능 Implementation Plan (서브프로젝트 B)

> **For agentic workers:** REQUIRED SUB-SKILL: superpowers:subagent-driven-development(권장) 또는 executing-plans. 스텝은 `- [ ]`.

**Goal:** 게임 시작 화면 "주변 구장" 섹션을 번들 자산(`assets/parkgolf_venues.json`) + 기기 위치로 거리순 표시하고, 선택 시 코스 마법사를 프리필한다.

**Architecture:** 순수 로직(거리/정렬/포맷)·모델·자산 파서는 테스트 가능하게 분리. 위치는 `LocationManager` 얇은 래퍼. UI는 버튼 트리거 상태형 섹션. 선택 → 마법사 프리필 채널.

**Tech Stack:** Kotlin, Android View/XML+ViewBinding, org.json(자산 파싱, 새 의존성 없음), LocationManager, coroutines. minSdk 21.

**태스크 순서 원칙:** 리소스·순수로직·모델·저장소·위치·마법사 프리필을 먼저, 이들을 소비하는 `StartFragment`(레이아웃+와이어링)를 마지막에.

---

## File Structure
- 신규 `ui/nearby/NearbyModels.kt`, `NearbyGeo.kt`, `NearbyRepository.kt`, `LocationProvider.kt`
- 신규 `test/.../NearbyGeoTest.kt`, `NearbyParseTest.kt`, `CourseWizardPrefillTest.kt`
- 신규 `res/layout/view_nearby_venue_card.xml`
- 수정 `res/layout/fragment_start.xml`, `ui/start/StartFragment.kt`
- 수정 `ui/courses/CourseWizardViewModel.kt`, `CourseWizardFragment.kt`
- 수정 `AndroidManifest.xml`, `res/values/strings.xml`

---

## Task 1: 리소스(권한·문자열)

**Files:** `AndroidManifest.xml`, `res/values/strings.xml`

- [ ] **Step 1: 매니페스트 위치 권한 추가** — `<manifest>` 안, `<application>` 위:
```xml
<uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
```
- [ ] **Step 2: strings 추가** (`</resources>` 직전):
```xml
<!-- nearby -->
<string name="nearby_find">주변 구장 찾기</string>
<string name="nearby_loading">주변 구장을 찾는 중…</string>
<string name="nearby_denied">주변 구장을 보려면 위치 권한이 필요합니다</string>
<string name="nearby_error">위치를 확인하지 못했습니다</string>
<string name="nearby_empty">주변에 표시할 구장이 없습니다</string>
<string name="nearby_retry">다시 시도</string>
<string name="nearby_open_settings">설정 열기</string>
<string name="nearby_distance_m">%1$dm</string>
<string name="nearby_distance_km">%1$.1fkm</string>
```
- [ ] **Step 3: 빌드** `./gradlew :app:assembleDebug` → BUILD SUCCESSFUL.
- [ ] **Step 4: 커밋** — "chore(nearby): location permissions and strings".

---

## Task 2: 모델 + 거리 로직 (TDD)

**Files:** `ui/nearby/NearbyModels.kt`, `ui/nearby/NearbyGeo.kt`, `test/.../NearbyGeoTest.kt`

- [ ] **Step 1: 실패 테스트** `NearbyGeoTest.kt`
```kotlin
package com.parkgolf.score.ui.nearby

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NearbyGeoTest {
    private fun v(name: String, lat: Double?, lng: Double?) =
        NearbyVenue(name, lat, lng, listOf(NearbyCourse("A", 9)), null, null, null, null, null)

    @Test fun haversine_zero() {
        assertEquals(0.0, NearbyGeo.haversineKm(35.0, 127.0, 35.0, 127.0), 1e-6)
    }
    @Test fun haversine_seoul_busan() {
        val d = NearbyGeo.haversineKm(37.5665, 126.9780, 35.1796, 129.0756)
        assertTrue("$d", d in 300.0..340.0)
    }
    @Test fun nearest_sorts_and_limits_and_skips_null() {
        val vs = listOf(
            v("far", 35.9, 127.9), v("near", 37.57, 126.98),
            v("mid", 36.5, 127.5), v("nocoord", null, null),
        )
        val r = NearbyGeo.nearest(vs, 37.5665, 126.9780, limit = 2)
        assertEquals(listOf("near", "mid"), r.map { it.first.name })
        assertTrue(r[0].second < r[1].second)
    }
    @Test fun format_distance_m_and_km() {
        assertEquals("850m", NearbyGeo.formatDistance(0.85, mFmt = "%1\$dm", kmFmt = "%1$.1fkm"))
        assertEquals("1.2km", NearbyGeo.formatDistance(1.23, mFmt = "%1\$dm", kmFmt = "%1$.1fkm"))
    }
}
```

- [ ] **Step 2: 실패 확인** — `./gradlew :app:testDebugUnitTest --tests "*NearbyGeoTest"` → FAIL.

- [ ] **Step 3: 구현** `NearbyModels.kt`
```kotlin
package com.parkgolf.score.ui.nearby

data class NearbyCourse(val name: String, val holes: Int?)

data class NearbyVenue(
    val name: String,
    val lat: Double?,
    val lng: Double?,
    val courses: List<NearbyCourse>,
    val sido: String?,
    val sigungu: String?,
    val roadAddress: String?,
    val jibunAddress: String?,
    val phone: String?,
)
```
`NearbyGeo.kt`
```kotlin
package com.parkgolf.score.ui.nearby

import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

object NearbyGeo {
    fun haversineKm(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double {
        val r = 6371.0
        val p1 = Math.toRadians(lat1); val p2 = Math.toRadians(lat2)
        val dPhi = Math.toRadians(lat2 - lat1); val dL = Math.toRadians(lng2 - lng1)
        val a = sin(dPhi / 2) * sin(dPhi / 2) + cos(p1) * cos(p2) * sin(dL / 2) * sin(dL / 2)
        return 2 * r * asin(sqrt(a))
    }

    /** 좌표 있는 구장만, 거리 오름차순 상위 limit개. */
    fun nearest(venues: List<NearbyVenue>, myLat: Double, myLng: Double, limit: Int = 10):
        List<Pair<NearbyVenue, Double>> =
        venues.filter { it.lat != null && it.lng != null }
            .map { it to haversineKm(myLat, myLng, it.lat!!, it.lng!!) }
            .sortedBy { it.second }
            .take(limit)

    /** 1km 미만은 m(10 단위 반올림), 이상은 km(소수1). */
    fun formatDistance(km: Double, mFmt: String, kmFmt: String): String =
        if (km < 1.0) String.format(mFmt, (Math.round(km * 1000 / 10.0) * 10).toInt())
        else String.format(kmFmt, km)
}
```

- [ ] **Step 4: 통과 확인. Step 5: 커밋** — "feat(nearby): models and distance logic".

---

## Task 3: 자산 파서 (`NearbyRepository`) (TDD)

**Files:** `ui/nearby/NearbyRepository.kt`, `test/.../NearbyParseTest.kt`

- [ ] **Step 1: 실패 테스트** `NearbyParseTest.kt` — `parseVenues(json: String)` 순수 함수 검증(좌표 null 처리, courses 매핑).
```kotlin
package com.parkgolf.score.ui.nearby

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class NearbyParseTest {
    private val json = """
      [{"name":"거창스포츠파크 파크골프장","region":{"sido":"경상남도","sigungu":"거창군"},
        "roadAddress":"경남 거창읍 심소정길 39","jibunAddress":"경남 거창읍 양평리 1160",
        "lat":35.6951,"lng":127.9263,"coordSource":"original",
        "courses":[{"name":"1구장","holes":18},{"name":"2구장","holes":18}],
        "phone":"055-940-8720","operator":"거창군","source":"x.csv"},
       {"name":"좌표없음","region":{"sido":null,"sigungu":"어딘가"},"roadAddress":null,
        "jibunAddress":"주소","lat":null,"lng":null,"coordSource":"none",
        "courses":[{"name":"","holes":null}],"phone":null,"operator":null,"source":"y.csv"}]
    """.trimIndent()

    @Test fun parses_fields_and_nulls() {
        val vs = NearbyRepository.parseVenues(json)
        assertEquals(2, vs.size)
        assertEquals("거창스포츠파크 파크골프장", vs[0].name)
        assertEquals(35.6951, vs[0].lat!!, 1e-6)
        assertEquals(listOf(18, 18), vs[0].courses.map { it.holes })
        assertEquals("거창군", vs[0].sigungu)
        assertNull(vs[1].lat)
        assertNull(vs[1].courses[0].holes)
    }
}
```

- [ ] **Step 2: 실패 확인.**
- [ ] **Step 3: 구현** `NearbyRepository.kt`
```kotlin
package com.parkgolf.score.ui.nearby

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

object NearbyRepository {
    @Volatile private var cache: List<NearbyVenue>? = null

    fun parseVenues(json: String): List<NearbyVenue> {
        val arr = JSONArray(json)
        val out = ArrayList<NearbyVenue>(arr.length())
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            val region = o.optJSONObject("region")
            val cArr = o.optJSONArray("courses") ?: JSONArray()
            val courses = (0 until cArr.length()).map { j ->
                val c = cArr.getJSONObject(j)
                NearbyCourse(c.optString("name", ""), c.optIntOrNull("holes"))
            }
            out.add(
                NearbyVenue(
                    name = o.optString("name", ""),
                    lat = o.optDoubleOrNull("lat"),
                    lng = o.optDoubleOrNull("lng"),
                    courses = courses,
                    sido = region?.optStringOrNull("sido"),
                    sigungu = region?.optStringOrNull("sigungu"),
                    roadAddress = o.optStringOrNull("roadAddress"),
                    jibunAddress = o.optStringOrNull("jibunAddress"),
                    phone = o.optStringOrNull("phone"),
                )
            )
        }
        return out
    }

    suspend fun load(context: Context): List<NearbyVenue> {
        cache?.let { return it }
        val json = context.assets.open("parkgolf_venues.json")
            .bufferedReader().use { it.readText() }
        return parseVenues(json).also { cache = it }
    }
}

private fun JSONObject.optDoubleOrNull(k: String): Double? =
    if (isNull(k) || !has(k)) null else optDouble(k).takeIf { !it.isNaN() }
private fun JSONObject.optIntOrNull(k: String): Int? =
    if (isNull(k) || !has(k)) null else optInt(k)
private fun JSONObject.optStringOrNull(k: String): String? =
    if (isNull(k) || !has(k)) null else optString(k).takeIf { it.isNotEmpty() }
```
참고: `load`는 IO 디스패처에서 호출(StartFragment에서 `withContext(Dispatchers.IO)`).

- [ ] **Step 4: 통과 확인. Step 5: 커밋** — "feat(nearby): asset repository and JSON parser".

---

## Task 4: 위치 래퍼 (`LocationProvider`)

**Files:** `ui/nearby/LocationProvider.kt`

- [ ] **Step 1: 구현**(프레임워크 격리, 권한은 호출자가 보장)
```kotlin
package com.parkgolf.score.ui.nearby

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Looper
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume

object LocationProvider {
    @SuppressLint("MissingPermission")
    suspend fun current(context: Context, timeoutMs: Long = 8000): Location? {
        val lm = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return null
        val providers = listOf(
            LocationManager.GPS_PROVIDER,
            LocationManager.NETWORK_PROVIDER,
            LocationManager.PASSIVE_PROVIDER,
        )
        val last = providers.mapNotNull { p ->
            runCatching { lm.getLastKnownLocation(p) }.getOrNull()
        }.maxByOrNull { it.time }
        if (last != null && System.currentTimeMillis() - last.time < 2 * 60 * 1000) return last

        val provider = when {
            lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER) -> LocationManager.NETWORK_PROVIDER
            lm.isProviderEnabled(LocationManager.GPS_PROVIDER) -> LocationManager.GPS_PROVIDER
            else -> return last
        }
        val fresh = withTimeoutOrNull(timeoutMs) {
            suspendCancellableCoroutine<Location?> { cont ->
                val listener = object : LocationListener {
                    override fun onLocationChanged(location: Location) {
                        lm.removeUpdates(this)
                        if (cont.isActive) cont.resume(location)
                    }
                    @Deprecated("") override fun onStatusChanged(p: String?, s: Int, e: android.os.Bundle?) {}
                    override fun onProviderEnabled(p: String) {}
                    override fun onProviderDisabled(p: String) {}
                }
                runCatching {
                    lm.requestLocationUpdates(provider, 0L, 0f, listener, Looper.getMainLooper())
                }.onFailure { if (cont.isActive) cont.resume(null) }
                cont.invokeOnCancellation { runCatching { lm.removeUpdates(listener) } }
            }
        }
        return fresh ?: last
    }
}
```
- [ ] **Step 2: 빌드** → BUILD SUCCESSFUL. **Step 3: 커밋** — "feat(nearby): LocationManager wrapper".

---

## Task 5: 마법사 프리필 채널 (TDD + 배선)

**Files:** `ui/courses/CourseWizardViewModel.kt`, `ui/courses/CourseWizardFragment.kt`, `test/.../CourseWizardPrefillTest.kt`

- [ ] **Step 1: 실패 테스트** `CourseWizardPrefillTest.kt`
```kotlin
package com.parkgolf.score.ui.courses

import org.junit.Assert.assertEquals
import org.junit.Test

class CourseWizardPrefillTest {
    @Test fun initPrefill_sets_name_course_and_pars() {
        val vm = CourseWizardViewModel()
        vm.initPrefill("양재천 파크골프장", "동코스", 18)
        val d = vm.draft.value!!
        assertEquals("양재천 파크골프장", d.venueName)
        assertEquals("동코스", d.courseName)
        assertEquals(18, d.pars.size)
        assertEquals(List(18) { 3 }, d.pars)
        assertEquals(0, d.step)
    }
    @Test fun initPrefill_min_one_hole() {
        val vm = CourseWizardViewModel()
        vm.initPrefill("V", "C", 0)
        assertEquals(1, vm.draft.value!!.pars.size)
    }
}
```
(LiveData는 즉시 값 설정이므로 InstantTaskExecutorRule 없이도 `_draft.value` 접근 가능. 필요 시 `androidx.arch.core:core-testing` 규칙 추가.)

- [ ] **Step 2: 실패 확인.**
- [ ] **Step 3: VM 구현** — `CourseWizardViewModel`에 추가:
```kotlin
    fun initPrefill(venueName: String, courseName: String, holeCount: Int) {
        _draft.value = CourseDraft(
            venueName = venueName,
            courseName = courseName,
            pars = List(holeCount.coerceAtLeast(1)) { 3 },
        )
    }
```
- [ ] **Step 4: 통과 확인.**
- [ ] **Step 5: Fragment 프리필 분기** — `CourseWizardFragment`의 `if (savedInstanceState == null) { ... }` 블록 시작부에서 프리필 우선 처리:
```kotlin
        if (savedInstanceState == null) {
            val handle = findNavController().previousBackStackEntry?.savedStateHandle
            val prefillVenue = handle?.get<String>("prefillVenueName")
            if (prefillVenue != null) {
                vm.initPrefill(
                    prefillVenue,
                    handle.get<String>("prefillCourseName") ?: "",
                    handle.get<Int>("prefillHoles") ?: 9,
                )
            } else {
                val venueId = handle?.get<Long>("wizardVenueId") ?: 0L
                val courseId = handle?.get<Long>("wizardCourseId") ?: 0L
                // ...기존 initNew/loadForEdit 로직 그대로...
            }
        }
```
(기존 venueId/courseId 로직은 `else` 안으로 이동. 나머지 파일 변경 없음.)
- [ ] **Step 6: 빌드 + 단위테스트** → BUILD SUCCESSFUL. **Step 7: 커밋** — "feat(nearby): course wizard prefill channel".

---

## Task 6: 주변 구장 카드 레이아웃 + 시작 화면 섹션

**Files:** `res/layout/view_nearby_venue_card.xml`, `res/layout/fragment_start.xml`

- [ ] **Step 1: `view_nearby_venue_card.xml`**(구장명 + 거리 배지 + coursesContainer; `view_start_course_row` 재사용)
```xml
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent" android:layout_height="wrap_content"
    android:orientation="vertical" android:background="@drawable/bg_card"
    android:layout_marginBottom="8dp">
    <LinearLayout android:layout_width="match_parent" android:layout_height="wrap_content"
        android:orientation="horizontal" android:gravity="center_vertical"
        android:paddingHorizontal="20dp" android:paddingTop="16dp" android:paddingBottom="6dp">
        <TextView android:id="@+id/tvVenueName"
            android:layout_width="0dp" android:layout_height="wrap_content" android:layout_weight="1"
            android:textSize="@dimen/body_text" android:textStyle="bold"
            android:textColor="?attr/parkForeground" android:singleLine="true" android:ellipsize="end" />
        <TextView android:id="@+id/tvDistance"
            android:layout_width="wrap_content" android:layout_height="wrap_content"
            android:background="@drawable/bg_pill_primary" android:paddingHorizontal="12dp"
            android:paddingVertical="4dp" android:textSize="14sp" android:textStyle="bold"
            android:layout_marginStart="8dp" />
    </LinearLayout>
    <LinearLayout android:id="@+id/coursesContainer"
        android:layout_width="match_parent" android:layout_height="wrap_content"
        android:orientation="vertical" android:paddingBottom="4dp" />
</LinearLayout>
```
(거리 배지 텍스트색은 코드에서 `?attr/parkPrimary`로 설정.)

- [ ] **Step 2: `fragment_start.xml`** — 기존 "주변 구장" 플레이스홀더 TextView(약 70–73행)를 아래 섹션으로 교체(섹션 라벨은 유지):
```xml
            <LinearLayout android:id="@+id/nearbySection"
                android:layout_width="match_parent" android:layout_height="wrap_content"
                android:orientation="vertical">
                <LinearLayout android:id="@+id/btnFindNearby"
                    android:layout_width="match_parent" android:layout_height="wrap_content"
                    android:minHeight="@dimen/touch_min" android:orientation="horizontal"
                    android:gravity="center" android:background="@drawable/bg_dashed"
                    android:paddingVertical="18dp" android:foreground="?attr/selectableItemBackground">
                    <ImageView android:layout_width="20dp" android:layout_height="20dp"
                        android:src="@drawable/ic_navigation" app:tint="?attr/parkPrimary"
                        android:importantForAccessibility="no" />
                    <TextView android:layout_width="wrap_content" android:layout_height="wrap_content"
                        android:layout_marginStart="8dp" android:text="@string/nearby_find"
                        android:textSize="@dimen/button_text" android:textStyle="bold"
                        android:textColor="?attr/parkPrimary" />
                </LinearLayout>
                <TextView android:id="@+id/tvNearbyStatus"
                    android:layout_width="match_parent" android:layout_height="wrap_content"
                    android:background="@drawable/bg_card" android:gravity="center" android:padding="20dp"
                    android:textColor="?attr/parkMutedForeground" android:visibility="gone" />
                <LinearLayout android:layout_width="match_parent" android:layout_height="wrap_content"
                    android:orientation="horizontal" android:gravity="center" android:visibility="gone"
                    android:id="@+id/nearbyActions" android:layout_marginTop="8dp">
                    <Button android:id="@+id/btnNearbyRetry"
                        android:layout_width="wrap_content" android:layout_height="wrap_content"
                        android:text="@string/nearby_retry" android:minHeight="@dimen/touch_min" />
                    <Button android:id="@+id/btnNearbySettings"
                        android:layout_width="wrap_content" android:layout_height="wrap_content"
                        android:text="@string/nearby_open_settings" android:layout_marginStart="8dp"
                        android:minHeight="@dimen/touch_min" android:visibility="gone" />
                </LinearLayout>
                <LinearLayout android:id="@+id/nearbyContainer"
                    android:layout_width="match_parent" android:layout_height="wrap_content"
                    android:orientation="vertical" />
            </LinearLayout>
```
(섹션 라벨 `ic_navigation`+`nearby_label`은 기존 그대로 위에 둔다.)

- [ ] **Step 3: 빌드** → BUILD SUCCESSFUL(StartFragment는 Task 7에서 새 id 참조 → 이 태스크만으론 컴파일 에러 가능. Task 7과 함께 성공. 이 태스크는 레이아웃만 커밋).
- [ ] **Step 4: 커밋** — "feat(nearby): start-screen nearby section and venue card layout".

---

## Task 7: StartFragment 주변 구장 배선

**Files:** `ui/start/StartFragment.kt`

기존 최근/내 코스 로직은 유지하고 주변 구장 상태머신을 추가한다. `onViewCreated` 끝에 `setupNearby(binding, repo)` 호출을 추가하고 아래 메서드들을 클래스에 추가.

- [ ] **Step 1: import 추가**
```kotlin
import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import com.google.android.material.color.MaterialColors
import com.parkgolf.score.databinding.ViewNearbyVenueCardBinding
import com.parkgolf.score.databinding.ViewStartCourseRowBinding
import com.parkgolf.score.ui.nearby.LocationProvider
import com.parkgolf.score.ui.nearby.NearbyGeo
import com.parkgolf.score.ui.nearby.NearbyRepository
import com.parkgolf.score.ui.nearby.NearbyVenue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
```

- [ ] **Step 2: 권한 런처 + setupNearby** (클래스 멤버/메서드 추가)
```kotlin
    private lateinit var bindingRef: FragmentStartBinding
    private val locPerm = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        if (result.values.any { it }) loadNearby()
        else showNearbyDenied(permanent = !shouldShowRequestPermissionRationale(
            Manifest.permission.ACCESS_FINE_LOCATION))
    }

    private fun setupNearby(binding: FragmentStartBinding) {
        bindingRef = binding
        binding.btnFindNearby.setOnClickListener { requestNearby() }
        binding.btnNearbyRetry.setOnClickListener { requestNearby() }
        binding.btnNearbySettings.setOnClickListener {
            startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.fromParts("package", requireContext().packageName, null)))
        }
    }

    private fun hasLocationPerm(): Boolean =
        ContextCompat.checkSelfPermission(requireContext(),
            Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
        ContextCompat.checkSelfPermission(requireContext(),
            Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED

    private fun requestNearby() {
        if (hasLocationPerm()) loadNearby()
        else locPerm.launch(arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION))
    }
```

- [ ] **Step 3: 상태 렌더 + 로딩** (메서드 추가)
```kotlin
    private fun nearbyState(loading: Boolean, statusText: String?, showRetry: Boolean, showSettings: Boolean) {
        val b = bindingRef
        b.btnFindNearby.isVisible = false
        b.tvNearbyStatus.isVisible = statusText != null
        b.tvNearbyStatus.text = statusText ?: ""
        b.nearbyActions.isVisible = showRetry || showSettings
        b.btnNearbyRetry.isVisible = showRetry
        b.btnNearbySettings.isVisible = showSettings
    }

    private fun showNearbyDenied(permanent: Boolean) {
        bindingRef.nearbyContainer.removeAllViews()
        nearbyState(false, getString(R.string.nearby_denied), showRetry = !permanent, showSettings = permanent)
    }

    private fun loadNearby() {
        nearbyState(true, getString(R.string.nearby_loading), showRetry = false, showSettings = false)
        bindingRef.nearbyContainer.removeAllViews()
        viewLifecycleOwner.lifecycleScope.launch {
            val loc = LocationProvider.current(requireContext().applicationContext)
            if (loc == null) {
                nearbyState(false, getString(R.string.nearby_error), showRetry = true, showSettings = false)
                return@launch
            }
            val venues = withContext(Dispatchers.IO) {
                NearbyRepository.load(requireContext().applicationContext)
            }
            val results = NearbyGeo.nearest(venues, loc.latitude, loc.longitude, limit = 10)
            if (results.isEmpty()) {
                nearbyState(false, getString(R.string.nearby_empty), showRetry = true, showSettings = false)
                return@launch
            }
            nearbyState(false, null, showRetry = false, showSettings = false)
            renderNearby(results)
        }
    }
```

- [ ] **Step 4: 목록 렌더 + 선택→마법사 프리필** (메서드 추가)
```kotlin
    private fun renderNearby(results: List<Pair<NearbyVenue, Double>>) {
        val b = bindingRef
        val inflater = LayoutInflater.from(requireContext())
        val primary = MaterialColors.getColor(requireView(), R.attr.parkPrimary)
        for ((venue, km) in results) {
            val card = ViewNearbyVenueCardBinding.inflate(inflater, b.nearbyContainer, false)
            card.tvVenueName.text = venue.name
            card.tvDistance.text = NearbyGeo.formatDistance(
                km, getString(R.string.nearby_distance_m), getString(R.string.nearby_distance_km))
            card.tvDistance.setTextColor(primary)
            venue.courses.forEachIndexed { idx, course ->
                val row = ViewStartCourseRowBinding.inflate(inflater, card.coursesContainer, false)
                row.tvCourseName.text = if (course.name.isNotBlank()) course.name else venue.name
                row.tvHoleCount.text = getString(R.string.hole_count, course.holes ?: 9)
                row.divider.isGone = idx == venue.courses.lastIndex
                row.rowCourse.setOnClickListener {
                    prefillWizard(venue.name, course.name, course.holes ?: 9)
                }
                card.coursesContainer.addView(row.root)
            }
            b.nearbyContainer.addView(card.root)
        }
    }

    private fun prefillWizard(venueName: String, courseName: String, holes: Int) {
        findNavController().currentBackStackEntry?.savedStateHandle?.apply {
            set("prefillVenueName", venueName)
            set("prefillCourseName", if (courseName.isNotBlank()) courseName else "A코스")
            set("prefillHoles", holes)
        }
        findNavController().navigate(R.id.courseWizardFragment)
    }
```

- [ ] **Step 5: onViewCreated에서 `setupNearby(binding)` 호출 추가**(기존 코드 끝에 한 줄).

- [ ] **Step 6: 빌드 + 단위테스트** → BUILD SUCCESSFUL. **Step 7: 커밋** — "feat(nearby): wire nearby section (permission, location, list, prefill)".

---

## Task 8: 에뮬레이터 스모크

- [ ] **Step 1: 설치** `./gradlew :app:installDebug`.
- [ ] **Step 2: 모의 위치 주입** — 권한 부여 후 adb로 위치 설정:
  `adb emu geo fix 127.0276 37.4979`(서울 강남 부근). 또는 설정에서 권한 허용 후 `adb shell appops set com.parkgolf.score android:mock_location allow`는 불필요(emu geo fix 사용).
- [ ] **Step 3: 흐름 확인** — 홈 → 게임 시작 → "주변 구장 찾기" → 권한 허용 → 거리순 목록(구장명 + 거리 배지 + 코스). 좌표 순서·거리 표기 sanity.
- [ ] **Step 4: 선택 → 마법사 프리필** — 코스 탭 → 마법사에 구장명·코스명·홀수(파3) 채워짐 → 저장 → 내 코스 등장 → 게임 시작 가능.
- [ ] **Step 5: 권한 거부 상태** — 권한 거부 시 안내 + 다시 시도/설정 버튼.
- [ ] **Step 6: 회귀** — 최근/내 코스 섹션 정상.
- [ ] **Step 7: 이상 시 수정·재검증, 완료.**

---

## Self-Review 메모
- 스펙 커버리지: 자산 로딩/파싱 ✔(T3), 거리·정렬·포맷 ✔(T2), 상태형 섹션(버튼/로딩/목록/거부/빈) ✔(T6/T7), 위치 권한·획득 ✔(T1/T4/T7), 선택→마법사 프리필 ✔(T5/T7).
- 타입 일관성: `NearbyGeo.nearest/formatDistance`, `NearbyRepository.parseVenues/load`, `LocationProvider.current`, `CourseWizardViewModel.initPrefill(venueName,courseName,holeCount)`, savedStateHandle 키(prefillVenueName/prefillCourseName/prefillHoles) 정의·사용 일치.
- 빌드 독립성: 레이아웃(T6)·리소스(T1)·로직(T2–T5)을 StartFragment(T7)보다 먼저. T6 레이아웃 단독 빌드는 StartFragment 참조로 실패 가능 → T7과 함께 성공(플랜 명시).
- 홀수 미상 코스 프리필 기본 9홀. 좌표 없는 15곳은 거리 목록 제외.
