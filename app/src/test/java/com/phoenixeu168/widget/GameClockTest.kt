package com.phoenixeu168.widget

import com.phoenixeu168.widget.data.GameClock
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant

private fun ms(s: String) = Instant.parse(s).toEpochMilli()

class GameClockTest {
    @Test fun winterResetIs1700Utc() {
        assertEquals(ms("2026-11-11T17:00:00Z"), GameClock.lastReset(ms("2026-11-11T20:00:00Z")))
        assertEquals(ms("2026-11-10T17:00:00Z"), GameClock.lastReset(ms("2026-11-11T16:59:00Z")))
        assertEquals(ms("2026-11-12T17:00:00Z"), GameClock.nextReset(ms("2026-11-11T20:00:00Z")))
    }

    @Test fun summerResetIs1600Utc() {
        assertEquals(ms("2026-07-01T16:00:00Z"), GameClock.lastReset(ms("2026-07-01T18:00:00Z")))
        assertEquals(ms("2026-07-02T16:00:00Z"), GameClock.nextReset(ms("2026-07-01T18:00:00Z")))
    }

    @Test fun oldSummerTimeInWinterSnapsToRealReset() {
        // published before the clocks changed: 16:00 UTC, but the winter reset is 17:00 UTC
        assertEquals(ms("2026-11-12T17:00:00Z"), GameClock.snap(ms("2026-11-12T16:00:00Z")))
        assertEquals(ms("2026-11-12T17:00:00Z"), GameClock.snap(ms("2026-11-12T17:00:00Z")))
    }

    @Test fun winterTimeInSummerSnapsToRealReset() {
        assertEquals(ms("2026-07-01T16:00:00Z"), GameClock.snap(ms("2026-07-01T17:00:00Z")))
    }

    @Test fun otherTimesAreUntouched() {
        assertEquals(ms("2026-11-12T09:00:00Z"), GameClock.snap(ms("2026-11-12T09:00:00Z")))
        assertEquals(ms("2026-11-12T16:30:00Z"), GameClock.snap(ms("2026-11-12T16:30:00Z")))
        assertEquals(ms("2026-11-12T15:00:00Z"), GameClock.snap(ms("2026-11-12T15:00:00Z")))
    }
}
