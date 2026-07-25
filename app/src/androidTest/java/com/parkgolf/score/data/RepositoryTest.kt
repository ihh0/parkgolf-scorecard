package com.parkgolf.score.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.parkgolf.score.data.db.AppDatabase
import com.parkgolf.score.domain.model.HoleSpec
import com.parkgolf.score.domain.model.Round
import com.parkgolf.score.domain.model.RoundStatus
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RepositoryTest {
    private lateinit var db: AppDatabase
    private lateinit var repo: ParkGolfRepository

    @Before fun setup() {
        val ctx = ApplicationProvider.getApplicationContext<android.content.Context>()
        db = Room.inMemoryDatabaseBuilder(ctx, AppDatabase::class.java).build()
        repo = ParkGolfRepositoryImpl(db.venueDao(), db.courseDao(), db.roundDao())
    }

    @After fun teardown() = db.close()

    private fun sampleRound(status: RoundStatus) = Round(
        date = 1000L,
        venueName = "○○파크골프장",
        players = listOf("나", "김철수"),
        holes = listOf(HoleSpec("A", 1, 3), HoleSpec("A", 2, 3)),
        scores = listOf(listOf(3, 4), listOf(3, 3)),
        status = status
    )

    @Test fun saveAndGetRound_roundTrips() = runTest {
        val id = repo.saveRound(sampleRound(RoundStatus.COMPLETED))
        val loaded = repo.getRound(id)
        assertThat(loaded).isNotNull()
        assertThat(loaded!!.players).containsExactly("나", "김철수").inOrder()
        assertThat(loaded.scores[0]).containsExactly(3, 4).inOrder()
        assertThat(loaded.status).isEqualTo(RoundStatus.COMPLETED)
    }

    @Test fun currentInProgress_returnsOnlyInProgress() = runTest {
        repo.saveRound(sampleRound(RoundStatus.COMPLETED))
        val id = repo.saveRound(sampleRound(RoundStatus.IN_PROGRESS))
        val current = repo.currentInProgressRound()
        assertThat(current?.id).isEqualTo(id)
    }

    @Test fun deleteRound_removesIt() = runTest {
        val id = repo.saveRound(sampleRound(RoundStatus.COMPLETED))
        repo.deleteRound(id)
        assertThat(repo.getRound(id)).isNull()
    }

    @Test fun completedRounds_excludesInProgress() = runTest {
        repo.saveRound(sampleRound(RoundStatus.COMPLETED))
        repo.saveRound(sampleRound(RoundStatus.IN_PROGRESS))
        assertThat(repo.completedRounds()).hasSize(1)
    }
}
