# 주변 구장 데이터 전처리 파이프라인 Implementation Plan (서브프로젝트 A)

> **For agentic workers:** 이 계획은 **인라인 실행**(superpowers:executing-plans)으로 수행한다. 데이터 반복 특성상 서브에이전트 대신 컨트롤러가 직접 실행·검증한다. 스텝은 `- [ ]`로 추적.

**Goal:** 제각각 CSV 17개 → 정규화·필터·지오코딩·중복제거된 `app/src/main/assets/parkgolf_venues.json` 생성 스크립트(`tools/build_venues.py`)와 단위 테스트.

**Architecture:** 순수 함수(로더/매퍼/필터/머저/리전)로 분리해 TDD. 지오코딩만 I/O(캐시+VWorld) 격리. Python 3 표준 라이브러리만 사용(외부 pip 의존 없음). 테스트는 stdlib `unittest`.

**Tech Stack:** Python 3, urllib(HTTP), csv, json, unittest. 실행: `python3 tools/build_venues.py`.

---

## File Structure
- `tools/build_venues.py` — 파이프라인(함수 + main).
- `tools/test_build_venues.py` — 단위 테스트(unittest).
- `tools/geocode_cache.json` — 주소→좌표 캐시(커밋, 결정성).
- `app/src/main/assets/parkgolf_venues.json` — 산출물(커밋).

VWorld 키는 환경변수 `VWORLD_KEY`. 커밋 금지.

---

## Task 1: 스캐폴드 + 값/헤더/주소/리전 순수 헬퍼 (TDD)

**Files:** `tools/build_venues.py`, `tools/test_build_venues.py`

- [ ] **Step 1: 실패 테스트 작성** (`tools/test_build_venues.py`)
```python
import unittest
import build_venues as bv

class Helpers(unittest.TestCase):
    def test_normalize_header_strips_spaces(self):
        self.assertEqual(bv.norm_header("시 설 명"), "시설명")
        self.assertEqual(bv.norm_header(" 홀 수 "), "홀수")
    def test_to_int_handles_commas_and_junk(self):
        self.assertEqual(bv.to_int("17,000"), 17000)
        self.assertEqual(bv.to_int("18"), 18)
        self.assertIsNone(bv.to_int(""))
        self.assertIsNone(bv.to_int("미정"))
    def test_clean_addr_removes_ilwon_suffix(self):
        self.assertEqual(bv.clean_addr("경상북도 포항시 남구 해도동 130-1번지 일원"),
                         "경상북도 포항시 남구 해도동 130-1")
        self.assertEqual(bv.clean_addr("경북 포항시 지곡로 11 일원"), "경북 포항시 지곡로 11")
    def test_region_from_address(self):
        r = bv.region_from_address("경상남도 거창군 거창읍 심소정길 39-36")
        self.assertEqual(r, {"sido": "경상남도", "sigungu": "거창군"})
        r2 = bv.region_from_address("서울특별시 관악구 남현동 1")
        self.assertEqual(r2, {"sido": "서울특별시", "sigungu": "관악구"})

if __name__ == "__main__":
    unittest.main()
```

- [ ] **Step 2: 실패 확인** — `cd tools && python3 -m pytest test_build_venues.py -q` 또는 `python3 test_build_venues.py` → FAIL(모듈/함수 없음).

- [ ] **Step 3: 구현** (`tools/build_venues.py` 상단)
```python
import re, csv, json, os

def norm_header(s: str) -> str:
    return re.sub(r"\s+", "", s or "")

def to_int(s):
    if s is None: return None
    m = re.sub(r"[,\s]", "", str(s))
    return int(m) if re.fullmatch(r"-?\d+", m) else None

def clean_addr(s: str) -> str:
    if not s: return ""
    s = s.strip()
    s = re.sub(r"\s*번지\s*일원$", "", s)
    s = re.sub(r"\s*일원$", "", s)
    s = re.sub(r"\s*번지$", "", s)
    return s.strip()

_SIDO = ["서울특별시","부산광역시","대구광역시","인천광역시","광주광역시","대전광역시","울산광역시",
         "세종특별자치시","경기도","강원특별자치도","강원도","충청북도","충청남도","전북특별자치도","전라북도",
         "전라남도","경상북도","경상남도","제주특별자치도"]

def region_from_address(addr: str):
    if not addr: return {"sido": None, "sigungu": None}
    parts = addr.split()
    sido = next((s for s in _SIDO if addr.startswith(s)), parts[0] if parts else None)
    rest = addr[len(sido):].strip().split() if sido else parts[1:]
    sigungu = rest[0] if rest else None
    return {"sido": sido, "sigungu": sigungu}
```

- [ ] **Step 4: 통과 확인** — 같은 명령 → PASS.
- [ ] **Step 5: 커밋** — `git add tools/build_venues.py tools/test_build_venues.py && git commit -m "feat(pipeline): value/header/address/region helpers"`

---

## Task 2: 필드 매핑(별칭 사전) (TDD)

**Files:** `tools/build_venues.py`, `tools/test_build_venues.py`

- [ ] **Step 1: 테스트 추가** — 세 스키마(거창/경북현황/관악표준)의 헤더+행 dict를 넣어 `map_row`가 name/addr/lat/lng/holes/operator/phone을 올바르게 뽑고, **면적(규모(미터제곱))을 holes로 잘못 넣지 않는지** 검증.
```python
class Mapping(unittest.TestCase):
    def test_geochang_standard(self):
        row = {"시설명":"거창스포츠파크 파크골프장 1구장","소재지도로명주소":"경상남도 거창군 거창읍 심소정길 39-36",
               "소재지지번주소":"경상남도 거창군 거창읍 양평리 1160","면적(제곱미터)":"17,000","규모(홀)":"18",
               "전화번호":"055-940-8720","관리기관":"거창군","위도":"35.69513581","경도":"127.9263505"}
        m = bv.map_row(row, "거창.csv")
        self.assertEqual(m["name"], "거창스포츠파크 파크골프장 1구장")
        self.assertEqual(m["holes"], 18)
        self.assertAlmostEqual(m["lat"], 35.69513581); self.assertAlmostEqual(m["lng"], 127.9263505)
        self.assertEqual(m["roadAddress"], "경상남도 거창군 거창읍 심소정길 39-36")
    def test_gangwon_hyeonhwang_area_not_holes(self):
        row = {"시군":"춘천시","시설명":"소양강파크골프장","주소":"강원 춘천시 …","규모(미터제곱)":"20,000","홀 수":"18","연락처":"033-…"}
        m = bv.map_row(row, "강원.csv")
        self.assertEqual(m["holes"], 18)     # 규모(미터제곱)=면적, 홀수 아님
        self.assertIsNone(m["lat"])
```

- [ ] **Step 2: 실패 확인.**
- [ ] **Step 3: 구현** — 별칭 사전 + `map_row`.
```python
ALIAS = {
  "name": ["파크골프장명","시설명"],
  "roadAddress": ["소재지도로명주소"],
  "jibunAddress": ["소재지지번주소","지번주소","주소","위치"],
  "lat": ["위도"], "lng": ["경도"],
  "holes": ["홀수","규모(홀)"],            # NOTE: '규모(미터제곱)'·'면적*'은 제외(면적)
  "courseCount": ["코스수"],
  "phone": ["전화번호","연락처","운영기관연락처","예약문의전화번호","관리기관전화번호"],
  "operator": ["운영기관","관리기관","운영기관명","운영기관명(관리기관명)","관리기관명"],
  "date": ["데이터기준일자"],
  "holdings": ["보유시설"],                # 생활체육시설 필터용
}
def _first(row, keys):
    for k in keys:
        if k in row and str(row[k]).strip(): return str(row[k]).strip()
    return None
def map_row(row, filename):
    row = {norm_header(k): v for k, v in row.items()}
    aliasN = {f: [norm_header(k) for k in ks] for f, ks in ALIAS.items()}
    lat = _first(row, aliasN["lat"]); lng = _first(row, aliasN["lng"])
    def fl(x):
        try: return float(x)
        except: return None
    road = _first(row, aliasN["roadAddress"])
    jibun = _first(row, aliasN["jibunAddress"])
    addr = road or jibun or ""
    return {
        "name": _first(row, aliasN["name"]),
        "roadAddress": road, "jibunAddress": jibun,
        "lat": fl(lat), "lng": fl(lng),
        "holes": to_int(_first(row, aliasN["holes"])),
        "courseCount": to_int(_first(row, aliasN["courseCount"])),
        "phone": _first(row, aliasN["phone"]),
        "operator": _first(row, aliasN["operator"]),
        "holdings": _first(row, aliasN["holdings"]),
        "region": region_from_address(addr),
        "source": filename,
        "date": _first(row, aliasN["date"]),
    }
```
주의: `holes` 별칭에 `면적`·`규모(미터제곱)`은 넣지 않는다(정규화 후 `규모(미터제곱)`은 별칭에 없어 무시됨).

- [ ] **Step 4: 통과 확인. Step 5: 커밋** — "feat(pipeline): source-column field mapping".

---

## Task 3: 비-파크골프 필터 (TDD)

- [ ] **Step 1: 테스트** — `is_parkgolf`:
  - 생활체육시설의 축구장(`name="옻골축구장"`, `holdings="인조잔디 축구장 1면"`) → False.
  - 생활체육시설의 파크골프(`name`/`holdings`에 "파크골프") → True.
  - 파크골프 전용 파일의 일반 행(name에 파크골프 없어도 파일이 파크골프 전용) → True.
  - 제주 내장객현황은 **로더에서 파일 통째 제외**(별도 테스트: 로더가 해당 파일을 스킵).
```python
class Filter(unittest.TestCase):
    def test_exclude_soccer(self):
        e = {"name":"옻골축구장","holdings":"인조잔디 축구장 1면"}
        self.assertFalse(bv.is_parkgolf(e, "대구광역시 북구_생활체육시설_x.csv"))
    def test_include_parkgolf_in_mixed(self):
        e = {"name":"○○파크골프장","holdings":"파크골프장 9홀"}
        self.assertTrue(bv.is_parkgolf(e, "대구광역시 북구_생활체육시설_x.csv"))
    def test_parkgolf_dedicated_file_row(self):
        e = {"name":"지곡파크골프장","holdings":None}
        self.assertTrue(bv.is_parkgolf(e, "경상북도_파크골프장 현황_x.csv"))
```

- [ ] **Step 2: 실패 확인. Step 3: 구현**
```python
MIXED_FILES = ("생활체육시설",)               # 파크골프 외 시설 혼합 → 행 단위 필터
EXCLUDE_FILES = ("골프장내장객현황",)          # 파일 전체 제외(로더에서)
def _has_pg(*vals):
    return any(v and ("파크골프" in v or "파크 골프" in v) for v in vals)
def is_parkgolf(entry, filename):
    if any(k in filename for k in MIXED_FILES):
        return _has_pg(entry.get("name"), entry.get("holdings"))
    return True   # 파크골프 전용 파일은 전부 채택
```

- [ ] **Step 4/5: 통과·커밋** — "feat(pipeline): non-parkgolf row filter".

---

## Task 4: 로더 + 파일 라우팅(실데이터 통합 검증)

- [ ] **Step 1: 구현** `load_all(datadir)`:
```python
def load_csv(path):
    with open(path, encoding="cp949") as f:
        return list(csv.DictReader(f))
def load_all(datadir):
    out = []
    for fn in sorted(os.listdir(datadir)):
        if not fn.lower().endswith(".csv"): continue
        if any(k in fn for k in EXCLUDE_FILES):   # 제주 내장객현황 등 제외
            continue
        for row in load_csv(os.path.join(datadir, fn)):
            m = map_row(row, fn)
            if not m["name"]: continue
            if is_parkgolf(m, fn):
                out.append(m)
    return out
```
- [ ] **Step 2: 통합 검증**(테스트 아님, 실행): `python3 -c "import build_venues as bv; e=bv.load_all('../데이터'); print(len(e)); import collections; print(collections.Counter(x['source'] for x in e))"` → 소스별 건수 합리성(제주=0, 생활체육시설=파크골프 행만), name 누락 없음, holes 파싱 sanity를 육안 확인. 이상 있으면 Task 2/3 보정.
- [ ] **Step 3: 커밋** — "feat(pipeline): CSV loader with file routing".

---

## Task 5: 그룹핑 + 중복 제거 (TDD)

- [ ] **Step 1: 테스트** — `haversine`, `group_venues`:
  - 거창 1구장/2구장(동일 좌표·이름줄기) → 구장 1개 + courses 2개(각 18홀).
  - 좌표 근접(<150m) + 이름 유사 두 소스 → 병합(표준 우선: 좌표 original 채택).
  - 좌표 없는 두 현황 항목(다른 이름) → 분리 유지.
```python
class Grouping(unittest.TestCase):
    def test_multicourse_same_place(self):
        es = [{"name":"거창스포츠파크 파크골프장 1구장","lat":35.6951,"lng":127.9263,"holes":18,"roadAddress":"A","jibunAddress":"J","region":{},"phone":None,"operator":None,"source":"s","coordSource":"original"},
              {"name":"거창스포츠파크 파크골프장 2구장","lat":35.6951,"lng":127.9263,"holes":18,"roadAddress":"A","jibunAddress":"J","region":{},"phone":None,"operator":None,"source":"s","coordSource":"original"}]
        vs = bv.group_venues(es)
        self.assertEqual(len(vs), 1)
        self.assertEqual(sorted(c["holes"] for c in vs[0]["courses"]), [18,18])
```

- [ ] **Step 2: 실패 확인. Step 3: 구현** — `haversine(a_lat,a_lng,b_lat,b_lng)`, 이름줄기(접미 "N구장"/공백 제거) 키 + 좌표 근접으로 그룹화, `courses[]` 구성(원천 접미→코스명, 없으면 "A코스","B코스"…), 중복은 표준>시설정보>현황 우선순위로 대표 필드 선택.
- [ ] **Step 4/5: 통과·커밋** — "feat(pipeline): venue grouping and dedup".

---

## Task 6: 지오코더(캐시 + VWorld, 주입식) (TDD)

- [ ] **Step 1: 테스트** — `geocode(addr, cache, fetch)`:
  - 캐시에 있으면 fetch 미호출.
  - 캐시 미스 시 fetch 호출 결과를 캐시에 기록.
  - fetch가 실패(None) 시 좌표 None 반환.
  (fetch는 주입 가능한 콜러블; VWorld 응답 파싱은 `parse_vworld(json)` 별도 함수로 테스트.)
```python
class Geo(unittest.TestCase):
    def test_cache_hit(self):
        calls=[]; cache={"서울 A":(37.5,127.0)}
        r=bv.geocode("서울 A", cache, lambda a: calls.append(a) or (0,0))
        self.assertEqual(r,(37.5,127.0)); self.assertEqual(calls,[])
    def test_parse_vworld_ok(self):
        j={"response":{"status":"OK","result":{"point":{"x":"127.02","y":"37.53"}}}}
        self.assertEqual(bv.parse_vworld(j),(37.53,127.02))
    def test_parse_vworld_fail(self):
        self.assertIsNone(bv.parse_vworld({"response":{"status":"NOT_FOUND"}}))
```

- [ ] **Step 2: 실패 확인. Step 3: 구현**
```python
import urllib.parse, urllib.request
def parse_vworld(j):
    try:
        if j["response"]["status"] != "OK": return None
        p = j["response"]["result"]["point"]
        return (float(p["y"]), float(p["x"]))   # (lat, lng)
    except Exception:
        return None
def vworld_fetch(addr, key, addr_type="ROAD"):
    q = urllib.parse.urlencode({"service":"address","request":"getcoord","version":"2.0",
        "crs":"epsg:4326","address":addr,"refine":"true","simple":"false","format":"json",
        "type":addr_type,"key":key})
    url = "https://api.vworld.kr/req/address?" + q
    with urllib.request.urlopen(url, timeout=10) as r:
        return parse_vworld(json.load(r))
def geocode(addr, cache, fetch):
    if addr in cache: 
        v = cache[addr]; return tuple(v) if v else None
    v = fetch(addr)
    cache[addr] = list(v) if v else None
    return v
```

- [ ] **Step 4/5: 통과·커밋** — "feat(pipeline): VWorld geocoder with cache".

---

## Task 7: build() + main() + 리포트 (지오코딩 제외 1차 실행)

- [ ] **Step 1: 구현** `build(datadir, cache, key=None)`:
  - `load_all` → 각 항목 coordSource 설정(lat/lng 있으면 "original").
  - 좌표 없는 항목: key 있으면 `geocode(clean_addr(road or jibun), ...)` 도로명→실패시 지번, 성공 "geocoded" 실패 "none"; key 없으면 캐시만 조회.
  - `group_venues` → JSON 직렬화. `main()`은 `VWORLD_KEY` 읽고 캐시 로드/저장, `app/src/main/assets/parkgolf_venues.json` 기록, 리포트 출력(총 구장·소스별·좌표 original/geocoded/none 건수).
- [ ] **Step 2: 1차 실행(키 없이)** — `python3 tools/build_venues.py` → 좌표 있는 데이터로 JSON 생성 + 리포트. 좌표 없는 항목 수 확인(지오코딩 대상 규모). 출력 JSON 샘플 육안 검증(좌표 33~39/124~132, 홀수 sanity).
- [ ] **Step 3: 커밋** — "feat(pipeline): build/main with report (pre-geocoding)".

---

## Task 8: 지오코딩 실행(키 필요) → 최종 자산

- [ ] **Step 1: 사용자에게 `VWORLD_KEY` 요청**(세션에서 `! export VWORLD_KEY=…` 또는 제공).
- [ ] **Step 2: 전체 실행** — `VWORLD_KEY=… python3 tools/build_venues.py`. 리포트에서 지오코딩 성공/실패 확인.
- [ ] **Step 3: 실패 항목 점검** — 실패율 높으면 `clean_addr`/지번 폴백 보강 후 재실행(캐시 덕에 성공분 재호출 없음).
- [ ] **Step 4: 최종 검증** — 총 구장 수, 좌표 커버리지, 지역 분포, 멀티코스(거창) 확인.
- [ ] **Step 5: 커밋** — 산출 JSON + 갱신된 `geocode_cache.json` 포함: "chore(pipeline): geocode addresses and build venues asset".

---

## Self-Review 메모
- 스펙 커버리지: 인코딩/헤더 정규화 ✔(T1), 스키마 매핑·면적vs홀 ✔(T2), 비-파크골프 필터(제주 파일 제외/생활체육 행 필터) ✔(T3/T4), 지오코딩+캐시 ✔(T6/T8), 그룹핑·중복제거 ✔(T5), 출력 스키마·리포트 ✔(T7), 테스트 ✔.
- 타입 일관성: `map_row(row, filename)`·`is_parkgolf(entry, filename)`·`geocode(addr, cache, fetch)`·`group_venues(entries)`·`build(datadir, cache, key)` 시그니처 전 태스크 일관.
- 실행 방식: 인라인. T1–T7은 키 없이 완료 가능, T8만 사용자 키 필요 → 자연 분리.
- 출력 스키마는 서브프로젝트 B의 계약(스펙의 JSON과 일치).
