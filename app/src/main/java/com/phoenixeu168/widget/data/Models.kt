package com.phoenixeu168.widget.data

/** One event from events-data-game.json (times are epoch millis, exactly as published). */
data class GameEvent(
    val id: Long,
    val title: String,
    val type: String,
    val format: String?,
    val priority: String,
    val minOps: Int?,
    val maxOps: Int?,
    val startMs: Long,
    val endMs: Long,
)

/** A weekly Phx territory slot (phx_territory_schedule): weekday 0 = Sunday, start time in UTC. */
data class TerritoryRow(val system: String, val weekday: Int, val startUtc: String, val durationMin: Int)

data class TerritorySlot(val system: String, val startMs: Long, val endMs: Long)

/** User settings. */
data class Prefs(
    val ops: Int = 40,
    val maxNow: Int = 6,
    val maxNext: Int = 4,
    /** Events longer than 7 days (arc-long missions) are hidden unless this is on. */
    val includeLong: Boolean = false,
    /** Hides passes, banners, recruit and ticket events. */
    val hideMinor: Boolean = true,
    val regions: Set<String> = setOf("eu"),
    val refreshMinutes: Int = 5,
)

/** One line in the Now / Next lists (format variants of the same event are merged). */
data class EventRow(
    val title: String,
    val formats: String,
    val type: String,
    val startMs: Long,
    val endMs: Long,
    /** Ends at (or before) the coming daily reset. */
    val endsToday: Boolean,
    /** Started at or after the last daily reset. */
    val isNew: Boolean,
)

/** Incursion / tournament / maintenance headline. */
data class Spot(val title: String, val startMs: Long, val endMs: Long, val live: Boolean)

data class Snapshot(
    val nowMs: Long,
    val lastResetMs: Long,
    val nextResetMs: Long,
    /** Running right now, ending soonest first. */
    val current: List<EventRow>,
    val currentMore: Int,
    /** Starting next, soonest first. */
    val upcoming: List<EventRow>,
    val upcomingMore: Int,
    val territory: List<TerritorySlot>,
    val incursion: Spot?,
    val tournament: Spot?,
    val maintenance: Spot?,
)
