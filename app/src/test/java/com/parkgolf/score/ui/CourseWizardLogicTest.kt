package com.parkgolf.score.ui

import com.google.common.truth.Truth.assertThat
import com.parkgolf.score.data.db.entity.VenueEntity
import com.parkgolf.score.ui.courses.CourseDraft
import com.parkgolf.score.ui.courses.CourseWizardLogic
import org.junit.Test

class CourseWizardLogicTest {

    @Test fun resizePars_growsWithParThreeDefault() {
        val result = CourseWizardLogic.resizePars(listOf(3, 4), 4)
        assertThat(result).containsExactly(3, 4, 3, 3).inOrder()
    }

    @Test fun resizePars_shrinksKeepingLeadingValues() {
        val result = CourseWizardLogic.resizePars(listOf(3, 4, 5, 3), 2)
        assertThat(result).containsExactly(3, 4).inOrder()
    }

    @Test fun resizePars_sameSizeUnchanged() {
        val input = listOf(3, 4, 3)
        assertThat(CourseWizardLogic.resizePars(input, 3)).isEqualTo(input)
    }

    @Test fun suggestCourseName_emptyGivesA() {
        assertThat(CourseWizardLogic.suggestCourseName(emptyList())).isEqualTo("A코스")
    }

    @Test fun suggestCourseName_skipsUsedLetters() {
        assertThat(CourseWizardLogic.suggestCourseName(listOf("A코스", "B코스"))).isEqualTo("C코스")
    }

    @Test fun resolveVenueId_matchesByNameIgnoringCaseAndSpace() {
        val venues = listOf(VenueEntity(id = 7, name = "한강 파크골프장"))
        val id = CourseWizardLogic.resolveVenueId("  한강 파크골프장 ", venues, editingVenueId = null)
        assertThat(id).isEqualTo(7)
    }

    @Test fun resolveVenueId_noMatchReturnsNull() {
        val venues = listOf(VenueEntity(id = 7, name = "한강"))
        assertThat(CourseWizardLogic.resolveVenueId("낙동강", venues, null)).isNull()
    }

    @Test fun resolveVenueId_editingWins() {
        val venues = listOf(VenueEntity(id = 7, name = "한강"))
        assertThat(CourseWizardLogic.resolveVenueId("한강", venues, editingVenueId = 99)).isEqualTo(99)
    }

    @Test fun isStepValid_venueStepRequiresName() {
        val blank = CourseDraft(venueName = "  ")
        val named = CourseDraft(venueName = "한강")
        assertThat(CourseWizardLogic.isStepValid(blank, 0)).isFalse()
        assertThat(CourseWizardLogic.isStepValid(named, 0)).isTrue()
    }

    @Test fun isStepValid_holeStepRequiresMatchingPars() {
        val mismatch = CourseDraft(holeCount = 9, pars = listOf(3, 3))
        val ok = CourseDraft(holeCount = 2, pars = listOf(3, 4))
        assertThat(CourseWizardLogic.isStepValid(mismatch, 2)).isFalse()
        assertThat(CourseWizardLogic.isStepValid(ok, 2)).isTrue()
    }
}
