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
        self.assertIsNone(bv.to_int(None))

    def test_clean_addr_removes_ilwon_suffix(self):
        self.assertEqual(
            bv.clean_addr("경상북도 포항시 남구 해도동 130-1번지 일원"),
            "경상북도 포항시 남구 해도동 130-1",
        )
        self.assertEqual(bv.clean_addr("경북 포항시 지곡로 11 일원"), "경북 포항시 지곡로 11")

    def test_region_from_address(self):
        r = bv.region_from_address("경상남도 거창군 거창읍 심소정길 39-36")
        self.assertEqual(r, {"sido": "경상남도", "sigungu": "거창군"})
        r2 = bv.region_from_address("서울특별시 관악구 남현동 1")
        self.assertEqual(r2, {"sido": "서울특별시", "sigungu": "관악구"})


class Mapping(unittest.TestCase):
    def test_geochang_standard(self):
        row = {
            "시설명": "거창스포츠파크 파크골프장 1구장",
            "소재지도로명주소": "경상남도 거창군 거창읍 심소정길 39-36",
            "소재지지번주소": "경상남도 거창군 거창읍 양평리 1160",
            "면적(제곱미터)": "17,000", "규모(홀)": "18",
            "전화번호": "055-940-8720", "관리기관": "거창군",
            "위도": "35.69513581", "경도": "127.9263505",
        }
        m = bv.map_row(row, "거창.csv")
        self.assertEqual(m["name"], "거창스포츠파크 파크골프장 1구장")
        self.assertEqual(m["holes"], 18)
        self.assertAlmostEqual(m["lat"], 35.69513581)
        self.assertAlmostEqual(m["lng"], 127.9263505)
        self.assertEqual(m["roadAddress"], "경상남도 거창군 거창읍 심소정길 39-36")
        self.assertEqual(m["operator"], "거창군")
        self.assertEqual(m["region"], {"sido": "경상남도", "sigungu": "거창군"})

    def test_gangwon_hyeonhwang_area_not_holes(self):
        row = {
            "시군": "춘천시", "시설명": "소양강파크골프장",
            "주소": "강원특별자치도 춘천시 근화동 1",
            "규모(미터제곱)": "20,000", "홀 수": "18", "연락처": "033-250-0000",
        }
        m = bv.map_row(row, "강원.csv")
        self.assertEqual(m["name"], "소양강파크골프장")
        self.assertEqual(m["holes"], 18)   # 규모(미터제곱)=면적, 홀 아님
        self.assertIsNone(m["lat"])
        self.assertEqual(m["jibunAddress"], "강원특별자치도 춘천시 근화동 1")
        self.assertEqual(m["phone"], "033-250-0000")

    def test_gwanak_standard_spaced_and_position(self):
        row = {"파크골프장명": "관악파크골프장", "소재지지번주소": "서울특별시 관악구 남현동 1",
               "위도": "37.47", "경도": "126.98", "홀수": "9", "코스수": "1"}
        m = bv.map_row(row, "관악표준.csv")
        self.assertEqual(m["name"], "관악파크골프장")
        self.assertEqual(m["holes"], 9)
        self.assertEqual(m["courseCount"], 1)
        self.assertEqual(m["region"]["sigungu"], "관악구")


class HolesFromHoldings(unittest.TestCase):
    def test_regular_holes(self):
        self.assertEqual(bv.holes_from_holdings("정규홀(27홀)"), 27)
        self.assertEqual(bv.holes_from_holdings("정규홀(18홀)"), 18)

    def test_takes_max_regulation(self):
        self.assertEqual(bv.holes_from_holdings("정규홀(36홀)연습홀(1홀)"), 36)

    def test_none_when_no_holes(self):
        self.assertIsNone(bv.holes_from_holdings("인조잔디 축구장 1면"))
        self.assertIsNone(bv.holes_from_holdings(None))

    def test_map_row_holes_fallback_to_holdings(self):
        row = {"시설명": "무태파크골프장", "보유시설": "정규홀(18홀)",
               "위도": "35.9", "경도": "128.6", "소재지지번주소": "대구 북구 x"}
        m = bv.map_row(row, "대구광역시 북구_생활체육시설_x.csv")
        self.assertEqual(m["holes"], 18)


class Filter(unittest.TestCase):
    def test_exclude_soccer_in_mixed_file(self):
        e = {"name": "옻골축구장", "holdings": "인조잔디 축구장 1면"}
        self.assertFalse(bv.is_parkgolf(e, "대구광역시 북구_생활체육시설_20260212.csv"))

    def test_include_parkgolf_in_mixed_file(self):
        e = {"name": "○○파크골프장", "holdings": "파크골프장 9홀"}
        self.assertTrue(bv.is_parkgolf(e, "대구광역시 북구_생활체육시설_20260212.csv"))

    def test_include_by_holdings_only(self):
        e = {"name": "산격체육공원", "holdings": "파크골프 9홀 + 산책로"}
        self.assertTrue(bv.is_parkgolf(e, "대구광역시 북구_생활체육시설_20260212.csv"))

    def test_parkgolf_dedicated_file_row_always_true(self):
        e = {"name": "지곡파크골프장", "holdings": None}
        self.assertTrue(bv.is_parkgolf(e, "경상북도_파크골프장 현황_20250310.csv"))


def _entry(name, lat=None, lng=None, holes=18, sigungu="거창군",
           source="경상남도 거창군_파크골프장_20260811.csv"):
    return {
        "name": name, "roadAddress": "도로명", "jibunAddress": "지번",
        "lat": lat, "lng": lng, "holes": holes, "courseCount": None,
        "phone": None, "operator": None, "holdings": None,
        "region": {"sido": "경상남도", "sigungu": sigungu},
        "source": source, "date": None,
        "coordSource": "original" if lat is not None else "none",
    }


class Grouping(unittest.TestCase):
    def test_haversine_zero_and_known(self):
        self.assertAlmostEqual(bv.haversine(35.0, 127.0, 35.0, 127.0), 0.0, places=6)
        d = bv.haversine(37.5665, 126.9780, 35.1796, 129.0756)  # 서울-부산 ≈ 325km
        self.assertTrue(300 < d < 340)

    def test_multicourse_same_place(self):
        es = [_entry("거창스포츠파크 파크골프장 1구장", 35.6951, 127.9263, 18),
              _entry("거창스포츠파크 파크골프장 2구장", 35.6951, 127.9263, 18)]
        vs = bv.group_venues(es)
        self.assertEqual(len(vs), 1)
        self.assertEqual(vs[0]["name"], "거창스포츠파크 파크골프장")
        self.assertEqual(sorted(c["holes"] for c in vs[0]["courses"]), [18, 18])

    def test_dedup_across_sources_prefers_standard_coords(self):
        es = [_entry("지곡파크골프장", None, None, 18, "포항시",
                     source="경상북도_파크골프장 현황_20250310.csv"),
              _entry("지곡파크골프장", 36.01, 129.34, 18, "포항시",
                     source="경주시시설관리공단_파크골프장 표준데이터_x.csv")]
        vs = bv.group_venues(es)
        self.assertEqual(len(vs), 1)
        self.assertEqual(len(vs[0]["courses"]), 1)
        self.assertAlmostEqual(vs[0]["lat"], 36.01)   # 표준 좌표 채택
        self.assertEqual(vs[0]["coordSource"], "original")

    def test_coordless_different_names_stay_separate(self):
        es = [_entry("주상면 파크골프장", None, None, 9),
              _entry("웅양면 파크골프장", None, None, 18)]
        vs = bv.group_venues(es)
        self.assertEqual(len(vs), 2)


if __name__ == "__main__":
    unittest.main()
