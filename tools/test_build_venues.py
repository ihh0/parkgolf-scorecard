import unittest
import build_venues as bv


class Helpers(unittest.TestCase):
    def test_to_int_handles_commas_and_junk(self):
        self.assertEqual(bv.to_int("17,000"), 17000)
        self.assertEqual(bv.to_int("18"), 18)
        self.assertIsNone(bv.to_int(""))
        self.assertIsNone(bv.to_int("미정"))
        self.assertIsNone(bv.to_int(None))

    def test_clean_addr_removes_ilwon_suffix(self):
        self.assertEqual(
            bv.clean_addr("경상북도 포항시 남구 해도동 130-1번지 일원"),
            "경상북도 포항시 남구 해도동 130-1",
        )
        self.assertEqual(bv.clean_addr("경북 포항시 지곡로 11 일원"), "경북 포항시 지곡로 11")

    def test_region_from_address(self):
        self.assertEqual(bv.region_from_address("경상남도 거창군 거창읍 심소정길 39-36"),
                         {"sido": "경상남도", "sigungu": "거창군"})
        self.assertEqual(bv.region_from_address("서울특별시 관악구 남현동 1"),
                         {"sido": "서울특별시", "sigungu": "관악구"})

    def test_region_missing_province_prefix(self):
        self.assertEqual(bv.region_from_address("가평군 청평면 대성리 388-13"),
                         {"sido": None, "sigungu": "가평군"})


class ParseRecord(unittest.TestCase):
    def test_normal_line(self):
        r = bv.parse_record("1 강원특별자치도 1 삼척시셍활체육공원파크골프장 강원특별자치도 강릉시 교동 262-4 9")
        self.assertEqual(r["name"], "삼척시셍활체육공원파크골프장")
        self.assertEqual(r["sido"], "강원특별자치도")
        self.assertEqual(r["address"], "강원특별자치도 강릉시 교동 262-4")
        self.assertEqual(r["holes"], 9)

    def test_name_contains_sido(self):
        r = bv.parse_record("552 충청북도 22 충청북도 도립파크골프장 충청북도 청주시 청원구 내수읍 45")
        self.assertEqual(r["name"], "충청북도 도립파크골프장")
        self.assertEqual(r["address"], "충청북도 청주시 청원구 내수읍")
        self.assertEqual(r["holes"], 45)

    def test_missing_address(self):
        r = bv.parse_record("397 울산광역시 8 알프스파크골프장 9")
        self.assertEqual(r["name"], "알프스파크골프장")
        self.assertEqual(r["address"], "")
        self.assertEqual(r["holes"], 9)

    def test_address_with_paren(self):
        r = bv.parse_record("7 강원특별자치도 7 동해망상파크골프장 강원특별자치도 동해시 동해대로 6314 (망상컨벤션센터 옆) 27")
        self.assertEqual(r["name"], "동해망상파크골프장")
        self.assertEqual(r["address"], "강원특별자치도 동해시 동해대로 6314 (망상컨벤션센터 옆)")
        self.assertEqual(r["holes"], 27)

    def test_header_line_is_none(self):
        self.assertIsNone(bv.parse_record("연번 지역 지역번호 파크골프장명 주소 홀수"))


def _entry(name, lat=None, lng=None, holes=18, sigungu="거창군", sido="경상남도"):
    return {
        "name": name, "roadAddress": None, "jibunAddress": "지번",
        "lat": lat, "lng": lng, "holes": holes, "courseCount": None,
        "phone": None, "operator": None,
        "region": {"sido": sido, "sigungu": sigungu},
        "source": bv.SOURCE_NAME, "date": bv.DATA_DATE,
        "coordSource": "geocoded" if lat is not None else "none",
    }


class Grouping(unittest.TestCase):
    def test_haversine_zero_and_known(self):
        self.assertAlmostEqual(bv.haversine(35.0, 127.0, 35.0, 127.0), 0.0, places=6)
        d = bv.haversine(37.5665, 126.9780, 35.1796, 129.0756)
        self.assertTrue(300 < d < 340)

    def test_multicourse_same_place(self):
        es = [_entry("거창스포츠파크 파크골프장 1구장", 35.6951, 127.9263, 18),
              _entry("거창스포츠파크 파크골프장 2구장", 35.6951, 127.9263, 18)]
        vs = bv.group_venues(es)
        self.assertEqual(len(vs), 1)
        self.assertEqual(vs[0]["name"], "거창스포츠파크 파크골프장")
        self.assertEqual(sorted(c["holes"] for c in vs[0]["courses"]), [18, 18])

    def test_dedup_same_name_same_sigungu(self):
        es = [_entry("지곡파크골프장", None, None, 18, "포항시", "경상북도"),
              _entry("지곡파크골프장", 36.01, 129.34, 18, "포항시", "경상북도")]
        vs = bv.group_venues(es)
        self.assertEqual(len(vs), 1)
        self.assertEqual(len(vs[0]["courses"]), 1)
        self.assertAlmostEqual(vs[0]["lat"], 36.01)
        self.assertEqual(vs[0]["coordSource"], "geocoded")

    def test_different_names_stay_separate(self):
        es = [_entry("주상면 파크골프장", None, None, 9),
              _entry("웅양면 파크골프장", None, None, 18)]
        self.assertEqual(len(bv.group_venues(es)), 2)


class Geo(unittest.TestCase):
    def test_cache_hit_no_fetch(self):
        calls = []
        cache = {"서울 A": [37.5, 127.0]}
        r = bv.geocode("서울 A", cache, lambda a: calls.append(a) or (0, 0))
        self.assertEqual(r, (37.5, 127.0))
        self.assertEqual(calls, [])

    def test_cache_miss_calls_and_stores(self):
        calls = []
        cache = {}
        r = bv.geocode("서울 B", cache, lambda a: calls.append(a) or (37.1, 127.1))
        self.assertEqual(r, (37.1, 127.1))
        self.assertEqual(cache["서울 B"], [37.1, 127.1])

    def test_fetch_failure_not_cached(self):
        cache = {}
        r = bv.geocode("없는주소", cache, lambda a: None)
        self.assertIsNone(r)
        self.assertNotIn("없는주소", cache)

    def test_parse_vworld_ok(self):
        j = {"response": {"status": "OK", "result": {"point": {"x": "127.02", "y": "37.53"}}}}
        self.assertEqual(bv.parse_vworld(j), (37.53, 127.02))

    def test_parse_vworld_fail(self):
        self.assertIsNone(bv.parse_vworld({"response": {"status": "NOT_FOUND"}}))

    def test_extract_paren(self):
        self.assertEqual(bv.extract_paren("동해대로 6314 (망상컨벤션센터 옆)"), "망상컨벤션센터 옆")
        self.assertIsNone(bv.extract_paren("금천교 ~ 철산교 사이"))

    def test_address_candidates_spacing_and_truncation(self):
        c = bv.address_candidates("경상북도 구미시 신평동 구미시산업로193-105 체육공원")
        # 한글-숫자 공백 정규화된 후보 포함
        self.assertTrue(any("산업로 193-105" in x for x in c))
        # 랜드마크 제거된 짧은 접두(동 단위) 포함
        self.assertIn("경상북도 구미시 신평동", c)
        # 원본이 첫 후보(정밀 우선)
        self.assertEqual(c[0], "경상북도 구미시 신평동 구미시산업로193-105 체육공원")


if __name__ == "__main__":
    unittest.main()
