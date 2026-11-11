package com.phoenixeu168.widget.data

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

object Fmt {
    /** "now", "42m", "3h 12m", "2d 3h". Rounds partial minutes up so a countdown never shows 0m early. */
    fun duration(ms: Long): String {
        val m = (ms + 59_999L) / 60_000L
        if (m <= 0) return "now"
        if (m < 60) return "${m}m"
        val h = m / 60
        if (h < 24) return "${h}h ${m % 60}m"
        return "${h / 24}d ${h % 24}h"
    }

    /** Whole length of an event: "6h", "1d", "5d". */
    fun length(ms: Long): String {
        val h = (ms + 1_800_000L) / 3_600_000L
        if (h < 24) return "${h}h"
        val d = (h + 12) / 24
        return "${d}d"
    }

    private fun f(p: String) = DateTimeFormatter.ofPattern(p, Locale.UK)
    private val CLOCK = f("HH:mm")
    private val DAY_CLOCK = f("EEE HH:mm")
    private val DAY_DATE_CLOCK = f("EEE d MMM HH:mm")
    private val DAY = f("EEE")

    fun clock(ms: Long, zone: ZoneId): String = CLOCK.format(Instant.ofEpochMilli(ms).atZone(zone))
    fun dayClock(ms: Long, zone: ZoneId): String = DAY_CLOCK.format(Instant.ofEpochMilli(ms).atZone(zone))
    fun dayDateClock(ms: Long, zone: ZoneId): String = DAY_DATE_CLOCK.format(Instant.ofEpochMilli(ms).atZone(zone))
    fun day(ms: Long, zone: ZoneId): String = DAY.format(Instant.ofEpochMilli(ms).atZone(zone))
}
