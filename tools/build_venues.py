"""파크골프 주변 구장 데이터 전처리 파이프라인.

대한파크골프협회 "전국 파크골프장 현황" PDF(구장명·주소·홀수)를 파싱하고
주소를 VWorld로 지오코딩하여 앱 자산 `app/src/main/assets/parkgolf_venues.json` 을 생성한다.

실행: python3 tools/build_venues.py   (지오코딩엔 환경변수 VWORLD_KEY 필요)
"""

import re
import json
import os
import math
import time
import urllib.parse
import urllib.request

SOURCE_NAME = "대한파크골프협회 2026 상반기 현황"
DATA_DATE = "2026-01-31"

# 지오코딩 실패 구장의 수동 좌표(구장명 -> (위도, 경도)). 지오코딩이 실패해도 이 값으로 채운다.
MANUAL_COORDS = {
    "군포시파크골프장": (37.3503197570378, 126.920132192209),
    "아양그린파크골프장": (37.003199, 127.256648),
    "동탄제2신도시파크골프장": (37.1815404, 127.0904292),
    "조만강파크골프장": (35.1872578145169, 128.836417284216),
    "조만강 장애인 파크골프장": (35.1887114, 128.8354529),
    "삼랑진파크골프장": (35.391698941161, 128.840212904304),
    "삼신교통부지 파크골프장": (35.4262381582601, 129.183121055012),
    "웅상파크골프장": (35.4112788, 129.1633769),
    "문경파크골프장": (36.6935691, 128.1250454),
    "달맞이파크골프장": (35.6563679295613, 128.635020792401),
    "원오교파크골프장": (35.6917323157497, 128.418973550198),
    "사암파크골프장": (35.0917499092036, 128.887140453233),
    "신호파크골프장": (35.1837104, 128.9829143),
    "중앙공원파크골프장": (36.5192737, 127.2778286),
    "알프스파크골프장": (35.5555611646063, 129.080117350245),
    "미호파크골프장": (36.6807485956748, 127.437555925126),
    "계양파크골프장": (37.5305703, 126.7336201),
    "아시아드파크골프장": (37.5452067885602, 126.665099463955),
    "공촌유수지파크골프장": (37.5432183993832, 126.619927318745),
    "연수파크골프장": (37.4063664536846, 126.68610672938),
    "영종미단시티파크골프장": (37.5182379982779, 126.535961787632),
    "오곡파크골프장": (35.2699389613742, 127.317284734004),
    "남악파크골프장": (34.8134182, 126.479036),
    "남원파크골프장": (35.4308207145339, 127.405414681414),
    "수안보온천 파크골프장": (36.8396532136071, 128.006316796562),
}


def apply_manual_overrides(entries, manual):
    """지오코딩 실패(좌표 없음) 항목을 구장명 기준 수동 좌표로 채운다. 채운 개수 반환."""
    n = 0
    for e in entries:
        if e["lat"] is None and e["name"] in manual:
            e["lat"], e["lng"] = manual[e["name"]]
            e["coordSource"] = "manual"
            n += 1
    return n

# ---------------------------------------------------------------------------
# 값/주소/리전 순수 헬퍼
# ---------------------------------------------------------------------------


def to_int(s):
    """콤마/공백을 제거하고 정수 파싱. 실패 시 None."""
    if s is None:
        return None
    m = re.sub(r"[,\s]", "", str(s))
    return int(m) if re.fullmatch(r"-?\d+", m) else None


def clean_addr(s: str) -> str:
    """지오코딩 실패를 줄이기 위한 최소 주소 정제('…번지 일원' 등 제거)."""
    if not s:
        return ""
    s = s.strip()
    s = re.sub(r"\s*번지\s*일원$", "", s)
    s = re.sub(r"\s*일원$", "", s)
    s = re.sub(r"\s*번지$", "", s)
    return s.strip()


_SIDO = [
    "서울특별시", "부산광역시", "대구광역시", "인천광역시", "광주광역시", "대전광역시", "울산광역시",
    "세종특별자치시", "경기도", "강원특별자치도", "강원도", "충청북도", "충청남도",
    "전북특별자치도", "전라북도", "전라남도", "경상북도", "경상남도", "제주특별자치도",
]


def region_from_address(addr: str):
    """주소 문자열에서 시/도와 시군구를 추출."""
    if not addr:
        return {"sido": None, "sigungu": None}
    parts = addr.split()
    sido = next((s for s in _SIDO if addr.startswith(s)), None)
    if sido:
        rest = addr[len(sido):].strip().split()
        sigungu = rest[0] if rest else None
    else:
        # 시/도 접두가 없는 주소: 첫 토큰을 시군구로 본다.
        sigungu = parts[0] if parts else None
    return {"sido": sido, "sigungu": sigungu}


# ---------------------------------------------------------------------------
# PDF 파싱 (연번 · 시도 · 지역번호 · 구장명 · 주소 · 홀수)
# ---------------------------------------------------------------------------

_RECORD_RE = re.compile(r"^\s*\d+\s+\S+\s+\d+\s+.+\s+\d+\s*$")


def parse_record(line):
    """PDF 표 한 줄 -> {name, sido, address, holes}. 형식 불일치면 None.

    토큰: 연번 sido 지역번호 <구장명…> [sido <주소…>] 홀수
    주소는 항상 sido로 시작하므로, 인덱스3 이후 sido의 '마지막' 등장을 주소 시작으로 본다
    (구장명에 sido가 들어가는 예: '충청북도 도립파크골프장'도 올바르게 분리).
    주소가 없는 행(울산 알프스/미호)은 address="".
    """
    t = line.split()
    if len(t) < 5 or not t[0].isdigit() or not t[2].isdigit() or not t[-1].isdigit():
        return None
    sido = t[1]
    holes = int(t[-1])
    addr_i = None
    for i in range(len(t) - 2, 2, -1):
        if t[i] == sido:
            addr_i = i
            break
    if addr_i is not None:
        name = " ".join(t[3:addr_i])
        address = " ".join(t[addr_i:-1])
    else:
        name = " ".join(t[3:-1])
        address = ""
    if not name:
        return None
    return {"name": name, "sido": sido, "address": address, "holes": holes}


def _to_entry(rec):
    address = rec["address"]
    return {
        "name": rec["name"],
        "roadAddress": None,
        "jibunAddress": address or None,
        "lat": None,
        "lng": None,
        "holes": rec["holes"],
        "courseCount": None,
        "phone": None,
        "operator": None,
        "region": {"sido": rec["sido"], "sigungu": region_from_address(address)["sigungu"]},
        "source": SOURCE_NAME,
        "date": DATA_DATE,
    }


def extract_pdf(pdf_path):
    """PDF 전체 -> 정규화 항목 리스트."""
    from pypdf import PdfReader

    reader = PdfReader(pdf_path)
    entries = []
    for page in reader.pages:
        for ln in (page.extract_text() or "").splitlines():
            if _RECORD_RE.match(ln):
                rec = parse_record(ln)
                if rec:
                    entries.append(_to_entry(rec))
    return entries


# ---------------------------------------------------------------------------
# 그룹핑 + 중복 제거 (멀티코스 '1구장/2구장' 병합)
# ---------------------------------------------------------------------------


def haversine(lat1, lng1, lat2, lng2):
    """두 좌표 간 거리(km)."""
    r = 6371.0
    p1, p2 = math.radians(lat1), math.radians(lat2)
    dphi = math.radians(lat2 - lat1)
    dl = math.radians(lng2 - lng1)
    a = math.sin(dphi / 2) ** 2 + math.cos(p1) * math.cos(p2) * math.sin(dl / 2) ** 2
    return 2 * r * math.asin(math.sqrt(a))


_SUFFIX_RE = re.compile(r"\s*(제?\s*\d+\s*구장|[A-Za-z]코스)$")


def split_suffix(name):
    """'…파크골프장 1구장' -> (stem, '1구장'). 접미 없으면 (name, None)."""
    m = _SUFFIX_RE.search(name or "")
    if m:
        return name[:m.start()].strip(), re.sub(r"\s+", "", m.group(1))
    return (name or "").strip(), None


def group_venues(entries):
    """같은 구장(이름줄기 + 시군구)끼리 묶어 구장 단위 + courses[]로 정규화."""
    groups = {}
    for e in entries:
        stem, suf = split_suffix(e["name"])
        key = (stem, e["region"].get("sigungu") or e["region"].get("sido"))
        groups.setdefault(key, []).append((suf, e))

    venues = []
    for (stem, _reg), items in groups.items():
        ents = [e for _, e in items]
        rep = max(ents, key=lambda e: 1 if e["lat"] is not None else 0)
        coord = next((e for e in ents if e["lat"] is not None), None)

        def pick(field):
            if rep.get(field):
                return rep[field]
            for e in ents:
                if e.get(field):
                    return e[field]
            return None

        courses = []
        for suf, e in items:
            if suf:
                courses.append({"name": suf, "holes": e["holes"]})
        seen_holes = set()
        for suf, e in items:
            if suf:
                continue
            h = e["holes"]
            if h in seen_holes:
                continue
            seen_holes.add(h)
            courses.append({"name": "", "holes": h})

        blanks = [c for c in courses if c["name"] == ""]
        if len(courses) > 1 and blanks:
            used = {c["name"] for c in courses if c["name"]}
            letters = (ch + "코스" for ch in "ABCDEFGHIJ")
            for c in blanks:
                for L in letters:
                    if L not in used:
                        c["name"] = L
                        used.add(L)
                        break

        venues.append({
            "name": stem,
            "region": rep["region"],
            "roadAddress": pick("roadAddress"),
            "jibunAddress": pick("jibunAddress"),
            "lat": coord["lat"] if coord else None,
            "lng": coord["lng"] if coord else None,
            "coordSource": (coord.get("coordSource", "geocoded") if coord else "none"),
            "courses": courses,
            "phone": pick("phone"),
            "operator": pick("operator"),
            "source": rep["source"],
        })
    return venues


# ---------------------------------------------------------------------------
# 지오코더 (VWorld + 캐시, I/O 주입식)
# ---------------------------------------------------------------------------


def parse_vworld(j):
    """VWorld getcoord 응답 -> (lat, lng) 또는 None."""
    try:
        if j["response"]["status"] != "OK":
            return None
        p = j["response"]["result"]["point"]
        return (float(p["y"]), float(p["x"]))   # y=위도, x=경도
    except (KeyError, TypeError, ValueError):
        return None


def vworld_fetch(addr, key, addr_type="ROAD"):
    """VWorld Geocoder 2.0 호출. 실패 시 None."""
    q = urllib.parse.urlencode({
        "service": "address", "request": "getcoord", "version": "2.0",
        "crs": "epsg:4326", "address": addr, "refine": "true", "simple": "false",
        "format": "json", "type": addr_type, "key": key,
    })
    url = "https://api.vworld.kr/req/address?" + q
    try:
        with urllib.request.urlopen(url, timeout=10) as r:
            return parse_vworld(json.load(r))
    except Exception:
        return None


def geocode(addr, cache, fetch):
    """캐시(성공분)만 신뢰. 미스 시 fetch. 실패는 캐시하지 않아 다음 실행에 재시도."""
    cached = cache.get(addr)
    if cached:
        return tuple(cached)
    v = fetch(addr)
    if v:
        cache[addr] = list(v)
    return v


def extract_paren(s):
    """'… (망상컨벤션센터 옆)' 안의 내용을 반환. 없으면 None."""
    if not s:
        return None
    m = re.search(r"\(([^)]+)\)", s)
    return m.group(1).strip() if m else None


def _strip_paren(s):
    return re.sub(r"\s*\([^)]*\)", "", s).strip()


def _space_digits(s):
    """한글 바로 뒤 숫자 사이에 공백('관광로363번길92' -> '관광로 363번길 92')."""
    return re.sub(r"([가-힣])(\d)", r"\1 \2", s)


def address_candidates(addr):
    """지오코딩 후보 주소들을 정밀→개략 순서로 생성.

    원본 → 한글/숫자 공백 정규화 → 괄호 제거 → 괄호 내부 →
    (실패 대비) 꼬리 토큰을 하나씩 줄인 접두(랜드마크 제거, 동/리 단위까지).
    """
    addr = clean_addr(addr)
    if not addr:
        return []
    cands = []

    def add(x):
        x = (x or "").strip()
        if x and x not in cands:
            cands.append(x)

    spaced = _space_digits(addr)
    add(addr)
    add(spaced)
    add(_strip_paren(spaced))
    inner = extract_paren(addr)
    if inner:
        add(inner)
    # 꼬리 토큰 절삭 폴백(랜드마크·불완전 번지 제거). 최소 3토큰(시도·시군구·동/리).
    toks = _strip_paren(spaced).split()
    for n in range(len(toks) - 1, 2, -1):
        add(" ".join(toks[:n]))
    return cands


def _geocode_entry(entry, cache, fetch):
    """주소 후보를 정밀→개략 순으로 지오코딩 시도."""
    for raw in (entry.get("roadAddress"), entry.get("jibunAddress")):
        for cand in address_candidates(raw):
            r = geocode(cand, cache, fetch)
            if r:
                return r
    return None


# ---------------------------------------------------------------------------
# build + main
# ---------------------------------------------------------------------------

HERE = os.path.dirname(os.path.abspath(__file__))
REPO = os.path.dirname(HERE)
PDF_PATH = os.path.join(REPO, "data", "2026년 전국 파크골프장 현황(2026년 상반기).pdf")
CACHE_PATH = os.path.join(HERE, "geocode_cache.json")
OUT_PATH = os.path.join(REPO, "app", "src", "main", "assets", "parkgolf_venues.json")


def build(pdf_path, cache, key=None, sleep=0.0, log=print):
    entries = extract_pdf(pdf_path)
    for e in entries:
        e["coordSource"] = "none"

    def fetch(addr):
        if not key:
            return None
        r = vworld_fetch(addr, key, "ROAD") or vworld_fetch(addr, key, "PARCEL")
        if sleep:
            time.sleep(sleep)
        return r

    geocoded = failed = 0
    for e in entries:
        r = _geocode_entry(e, cache, fetch)
        if r:
            e["lat"], e["lng"] = r
            e["coordSource"] = "geocoded"
            geocoded += 1
        else:
            failed += 1

    manual = apply_manual_overrides(entries, MANUAL_COORDS)

    venues = group_venues(entries)

    log(f"entries: {len(entries)}  ->  venues: {len(venues)}")
    log(f"coords  geocoded: {sum(1 for v in venues if v['coordSource']=='geocoded')}"
        f"  manual: {sum(1 for v in venues if v['coordSource']=='manual')}"
        f"  none: {sum(1 for v in venues if v['coordSource']=='none')}")
    log(f"geocoding this run  success: {geocoded}  failed: {failed}"
        f"  manual-filled: {manual}  (key {'set' if key else 'MISSING'})")
    return venues


def main():
    cache = {}
    if os.path.exists(CACHE_PATH):
        with open(CACHE_PATH, encoding="utf-8") as f:
            cache = json.load(f)
    key = os.environ.get("VWORLD_KEY")
    venues = build(PDF_PATH, cache, key=key, sleep=0.1)

    os.makedirs(os.path.dirname(OUT_PATH), exist_ok=True)
    with open(OUT_PATH, "w", encoding="utf-8") as f:
        json.dump(venues, f, ensure_ascii=False, indent=2)
    with open(CACHE_PATH, "w", encoding="utf-8") as f:
        json.dump(cache, f, ensure_ascii=False, indent=2, sort_keys=True)
    print(f"wrote {OUT_PATH} ({len(venues)} venues)")


if __name__ == "__main__":
    main()
