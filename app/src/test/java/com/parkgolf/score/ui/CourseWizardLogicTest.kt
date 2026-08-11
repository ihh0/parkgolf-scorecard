package com.parkgolf.score.ui

import com.google.common.truth.Truth.assertThat
import com.parkgolf.score.data.db.entity.VenueEntity
import com.parkgolf.score.ui.courses.CourseDraft
import com.parkgolf.score.ui.courses.CourseWizardLogic
import org.junit.Test

class CourseWizardLogicTest {

    @Test fun addHole_appendsParThree() {
        assertThat(CourseWizardLogic.addHole(listOf(3, 4))).containsExactly(3, 4, 3).inOrder()
    }

    @Test fun deleteHole_removesIndex() {
        assertThat(CourseWizardLogic.deleteHole(listOf(3, 4, 5), 1)).containsExactly(3, 5).inOrder()
    }

    @Test fun deleteHole_keepsAtLeastOneHole() {
        assertThat(CourseWizardLogic.deleteHole(listOf(3), 0)).containsExactly(3).inOrder()
    }

    @Test fun clampPar_flooredAtOne_noUpperBound() {
        assertThat(CourseWizardLogic.clampPar(-2)).isEqualTo(1)
        assertThat(CourseWizardLogic.clampPar(7)).isEqualTo(7)
    }

    @Test fun isExistingVenue_exactMatchIgnoringCaseAndSpace() {
        val venues = listOf(VenueEntity(id = 1, name = "한강 파크골프장"))
        assertThat(CourseWizardLogic.isExistingVenue(" 한강 파크골프장 ", venues)).isTrue()
        assertThat(CourseWizardLogic.isExistingVenue("한강", venues)).isFalse()
    }

    @Test fun matchedVenues_partialMatchesExcludingExact() {
        val venues = listOf(VenueEntity(1, "한강 파크골프장"), VenueEntity(2, "탄천 파크골프장"))
        assertThat(CourseWizardLogic.matchedVenues("한강", venues)).containsExactly("한강 파크골프장")
        assertThat(CourseWizardLogic.matchedVenues("한강 파크골프장", venues)).isEmpty()
        assertThat(CourseWizardLogic.matchedVenues("", venues)).isEmpty()
    }

    @Test fun isDuplicateCourseName_ignoringCaseAndSpace() {
        val existing = listOf("A코스", "B코스")
        assertThat(CourseWizardLogic.isDuplicateCourseName(" a코스 ", existing)).isTrue()
        assertThat(CourseWizardLogic.isDuplicateCourseName("C코스", existing)).isFalse()
    }

    @Test fun stepValidity() {
        assertThat(CourseWizardLogic.venueStepValid(CourseDraft(venueName = " "))).isFalse()
        assertThat(CourseWizardLogic.venueStepValid(CourseDraft(venueName = "한강"))).isTrue()
        assertThat(CourseWizardLogic.courseStepValid("A코스", listOf("A코스"))).isFalse()
        assertThat(CourseWizardLogic.courseStepValid("C코스", listOf("A코스"))).isTrue()
        assertThat(CourseWizardLogic.courseStepValid("  ", emptyList())).isFalse()
        assertThat(CourseWizardLogic.holeStepValid(emptyList())).isFalse()
        assertThat(CourseWizardLogic.holeStepValid(listOf(3))).isTrue()
    }

    @Test fun resolveVenueId_matchOrNull() {
        val venues = listOf(VenueEntity(id = 7, name = "한강"))
        assertThat(CourseWizardLogic.resolveVenueId("한강", venues, null)).isEqualTo(7)
        assertThat(CourseWizardLogic.resolveVenueId("낙동강", venues, null)).isNull()
        assertThat(CourseWizardLogic.resolveVenueId("한강", venues, 99)).isEqualTo(99)
    }
}
