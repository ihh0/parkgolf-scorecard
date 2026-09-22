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


if __name__ == "__main__":
    unittest.main()
