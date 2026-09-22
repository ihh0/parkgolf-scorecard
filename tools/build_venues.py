"""파크골프 주변 구장 데이터 전처리 파이프라인.

지역별로 형식이 제각각인 공공 CSV들을 정규화·필터·지오코딩·중복제거하여
앱 자산 `app/src/main/assets/parkgolf_venues.json` 을 생성한다.

실행: python3 tools/build_venues.py   (지오코딩엔 환경변수 VWORLD_KEY 필요)
"""

import re
import csv
import json
import os
import math
import time
import urllib.parse
import urllib.request

# ---------------------------------------------------------------------------
# 값/헤더/주소/리전 순수 헬퍼
# ---------------------------------------------------------------------------


def norm_header(s: str) -> str:
    """헤더의 모든 공백 제거. '시 설 명' -> '시설명'."""
    return re.sub(r"\s+", "", s or "")


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
    sido = next((s for s in _SIDO if addr.startswith(s)), parts[0] if parts else None)
    if sido and addr.startswith(sido):
        rest = addr[len(sido):].strip().split()
    else:
        rest = parts[1:]
    sigungu = rest[0] if rest else None
    return {"sido": sido, "sigungu": sigungu}


# ---------------------------------------------------------------------------
# 스키마 라우팅: 원천 컬럼 -> 공통 필드 (별칭 사전)
# ---------------------------------------------------------------------------

ALIAS = {
    "name": ["파크골프장명", "시설명"],
    "roadAddress": ["소재지도로명주소"],
    "jibunAddress": ["소재지지번주소", "지번주소", "주소", "위치"],
    "lat": ["위도"],
    "lng": ["경도"],
    # NOTE: '규모(미터제곱)'·'면적*'은 면적이므로 홀수 별칭에서 제외.
    "holes": ["홀수", "규모(홀)"],
    "courseCount": ["코스수"],
    "phone": ["전화번호", "연락처", "운영기관연락처", "예약문의전화번호", "관리기관전화번호"],
    "operator": ["운영기관", "관리기관", "운영기관명", "운영기관명(관리기관명)", "관리기관명"],
    "date": ["데이터기준일자"],
    "holdings": ["보유시설"],   # 생활체육시설 필터용
}


def holes_from_holdings(s):
    """'정규홀(27홀)' 같은 보유시설 문구에서 홀수(최댓값=정규홀)를 추출."""
    if not s:
        return None
    nums = [int(n) for n in re.findall(r"(\d+)\s*홀", s)]
    return max(nums) if nums else None


def _first(row, keys):
    for k in keys:
        if k in row and str(row[k]).strip():
            return str(row[k]).strip()
    return None


def _to_float(x):
    try:
        return float(x)
    except (TypeError, ValueError):
        return None


# ---------------------------------------------------------------------------
# 비-파크골프 필터
# ---------------------------------------------------------------------------

MIXED_FILES = ("생활체육시설",)        # 파크골프 외 시설 혼합 -> 행 단위 필터
EXCLUDE_FILES = ("골프장내장객현황",)   # 파일 전체 제외(로더에서 스킵)


def _has_pg(*vals):
    return any(v and ("파크골프" in v or "파크 골프" in v) for v in vals)


def is_parkgolf(entry, filename):
    """혼합 파일은 name/보유시설에 '파크골프' 있는 행만, 전용 파일은 전부 채택."""
    if any(k in filename for k in MIXED_FILES):
        return _has_pg(entry.get("name"), entry.get("holdings"))
    return True


def map_row(row, filename):
    """원천 CSV 한 행(dict) -> 정규화된 공통 필드 dict."""
    row = {norm_header(k): v for k, v in row.items()}
    aliasN = {f: [norm_header(k) for k in ks] for f, ks in ALIAS.items()}
    road = _first(row, aliasN["roadAddress"])
    jibun = _first(row, aliasN["jibunAddress"])
    addr = road or jibun or ""
    return {
        "name": _first(row, aliasN["name"]),
        "roadAddress": road,
        "jibunAddress": jibun,
        "lat": _to_float(_first(row, aliasN["lat"])),
        "lng": _to_float(_first(row, aliasN["lng"])),
        "holes": to_int(_first(row, aliasN["holes"]))
        or holes_from_holdings(_first(row, aliasN["holdings"])),
        "courseCount": to_int(_first(row, aliasN["courseCount"])),
        "phone": _first(row, aliasN["phone"]),
        "operator": _first(row, aliasN["operator"]),
        "holdings": _first(row, aliasN["holdings"]),
        "region": region_from_address(addr),
        "source": filename,
        "date": _first(row, aliasN["date"]),
    }


# ---------------------------------------------------------------------------
# 로더 + 파일 라우팅
# ---------------------------------------------------------------------------


def load_csv(path):
    with open(path, encoding="cp949") as f:
        return list(csv.DictReader(f))


def load_all(datadir):
    """데이터 디렉터리의 모든 CSV -> 정규화·필터된 항목 리스트."""
    out = []
    for fn in sorted(os.listdir(datadir)):
        if not fn.lower().endswith(".csv"):
            continue
        if any(k in fn for k in EXCLUDE_FILES):   # 제주 내장객현황 등 파일 통째 제외
            continue
        for row in load_csv(os.path.join(datadir, fn)):
            m = map_row(row, fn)
            if not m["name"]:
                continue
            if is_parkgolf(m, fn):
                out.append(m)
    return out


# ---------------------------------------------------------------------------
# 그룹핑 + 중복 제거
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


def source_priority(filename):
    """대표 필드 선택 우선순위: 표준데이터 > 시설 > 현황."""
    if "표준" in filename:
        return 3
    if "시설" in filename:
        return 2
    return 1


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

        def rank(e):
            return (source_priority(e["source"]), 1 if e["lat"] is not None else 0)

        rep = max(ents, key=rank)
        coord_ents = [e for e in ents if e["lat"] is not None]
        coord = max(coord_ents, key=lambda e: source_priority(e["source"])) if coord_ents else None

        def pick(field):
            if rep.get(field):
                return rep[field]
            for e in ents:
                if e.get(field):
                    return e[field]
            return None

        # courses: 접미 있는 항목은 각각 코스, 접미 없는 항목은 홀수로 중복 제거
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

        # 이름 없는 코스가 여러 개면 A코스/B코스… 부여
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
            "coordSource": (coord.get("coordSource", "original") if coord else "none"),
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
    """캐시 우선 조회, 미스 시 fetch 호출 후 캐시에 기록. (lat, lng) 또는 None."""
    if addr in cache:
        v = cache[addr]
        return tuple(v) if v else None
    v = fetch(addr)
    cache[addr] = list(v) if v else None
    return v
