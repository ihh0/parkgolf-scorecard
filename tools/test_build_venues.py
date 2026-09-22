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


if __name__ == "__main__":
    unittest.main()
