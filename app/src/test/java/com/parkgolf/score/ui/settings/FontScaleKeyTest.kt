package com.parkgolf.score.ui.settings

import org.junit.Assert.assertEquals
import org.junit.Test

class FontScaleKeyTest {
    @Test fun fromStored_valid() {
        assertEquals(FontScaleKey.LARGE, FontScaleKey.fromStored("large"))
        assertEquals(FontScaleKey.XXLARGE, FontScaleKey.fromStored("xxlarge"))
        assertEquals(FontScaleKey.XXXLARGE, FontScaleKey.fromStored("xxxlarge"))
        assertEquals(FontScaleKey.XXXXLARGE, FontScaleKey.fromStored("xxxxlarge"))
    }

    @Test fun fromStored_null_or_invalid_defaults_normal() {
        assertEquals(FontScaleKey.NORMAL, FontScaleKey.fromStored(null))
        assertEquals(FontScaleKey.NORMAL, FontScaleKey.fromStored("bogus"))
    }

    @Test fun multipliers() {
        assertEquals(1.0f, FontScaleKey.NORMAL.multiplier, 1e-6f)
        assertEquals(1.15f, FontScaleKey.LARGE.multiplier, 1e-6f)
        assertEquals(1.3f, FontScaleKey.XLARGE.multiplier, 1e-6f)
        assertEquals(1.5f, FontScaleKey.XXLARGE.multiplier, 1e-6f)
        assertEquals(1.7f, FontScaleKey.XXXLARGE.multiplier, 1e-6f)
        assertEquals(1.9f, FontScaleKey.XXXXLARGE.multiplier, 1e-6f)
    }
}
