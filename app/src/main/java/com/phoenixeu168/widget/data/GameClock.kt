package com.phoenixeu168.widget.data

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset

/**
 * The game's daily reset is 09:00 Pacific time, which is 16:00 UTC in summer and 17:00 UTC in winter.
 * Events published before a clock change keep the old UTC hour, so the feed mixes 16:00 and 17:00 UTC.
 * [snap] pulls both onto the real reset so every event lines up with the same game day.
 */
object GameClock {
    val PACIFIC: ZoneId = ZoneId.of("America/Los_Angeles")
    private const val RESET_HOUR = 9
    private const val HOUR_MS = 3_600_000L

    private fun resetOn(date: LocalDate): Long =
        date.atTime(RESET_HOUR, 0).atZone(PACIFIC).toInstant().toEpochMilli()

    private fun pacificDate(ms: Long): LocalDate = Instant.ofEpochMilli(ms).atZone(PACIFIC).toLocalDate()

    /** The most recent reset at or before [ms]. */
    fun lastReset(ms: Long): Long {
        val today = resetOn(pacificDate(ms))
        return if (today <= ms) today else resetOn(pacificDate(ms).minusDays(1))
    }

    /** The first reset strictly after [ms]. */
    fun nextReset(ms: Long): Long = resetOn(pacificDate(lastReset(ms)).plusDays(1))

    /** Moves an on-the-hour 16:00 or 17:00 UTC time onto the real reset of that game day; anything else is unchanged. */
    fun snap(ms: Long): Long {
        val utc = Instant.ofEpochMilli(ms).atZone(ZoneOffset.UTC)
        if (utc.minute != 0 || utc.second != 0 || utc.nano != 0) return ms
        if (utc.hour != 16 && utc.hour != 17) return ms
        val reset = resetOn(pacificDate(ms))
        return if (kotlin.math.abs(reset - ms) <= HOUR_MS) reset else ms
    }
}
