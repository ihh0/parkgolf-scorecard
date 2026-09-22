"""파크골프 주변 구장 데이터 전처리 파이프라인.

지역별로 형식이 제각각인 공공 CSV들을 정규화·필터·지오코딩·중복제거하여
앱 자산 `app/src/main/assets/parkgolf_venues.json` 을 생성한다.

실행: python3 tools/build_venues.py   (지오코딩엔 환경변수 VWORLD_KEY 필요)
"""

import re
import csv
import json
import os

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
