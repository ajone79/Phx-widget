package com.phoenixeu168.widget.data

import java.time.Instant
import java.time.LocalTime
import java.time.ZoneOffset

/** Pure logic: turns the raw event list + settings + "now" into what the widget draws. No Android APIs. */
object Engine {
    const val DAY_MS = 86_400_000L
    private const val LONG_EVENT_MS = 7 * DAY_MS

    private val TOURNAMENT = Regex("alliance\\s+tournament", RegexOption.IGNORE_CASE)
    private val INCURSION = Regex("incursion", RegexOption.IGNORE_CASE)
    private val FORMAT_SUFFIX = Regex("\\s*[-\u2013]\\s*(SMS|SLB|AMS|ALB|RLB|XALB|XLB)\\s*$", RegexOption.IGNORE_CASE)
    private val MINOR_TITLE = Regex("\\b(pass|banner)\\b", RegexOption.IGNORE_CASE)
    private val MINOR_TYPES = setOf("battlepass", "arc_banner", "recruit", "ticket")
    private val JUNK = Regex("[\\p{So}\\p{Cs}\\p{Mn}]")
    private val SPACES = Regex("\\s+")

    /** Removes flags, emoji and warning signs from a title. */
    fun clean(title: String): String = JUNK.replace(title, "").replace(SPACES, " ").trim()

    private fun baseTitle(title: String): String = FORMAT_SUFFIX.replace(clean(title), "").trim()

    private fun formatOf(e: GameEvent): String? =
        e.format?.takeIf { it.isNotBlank() }?.uppercase()
            ?: FORMAT_SUFFIX.find(clean(e.title))?.groupValues?.get(1)?.uppercase()

    /** Same rules as the calendar feed: E.U. / U.S. / APAC in the title; no region means everyone. */
    fun regionOf(title: String): String? = when {
        Regex("\\be\\.?u\\.?(\\s|$)", RegexOption.IGNORE_CASE).containsMatchIn(title) -> "eu"
        Regex("\\bu\\.?s\\.?(\\s|$)", RegexOption.IGNORE_CASE).containsMatchIn(title) -> "us"
        Regex("apac", RegexOption.IGNORE_CASE).containsMatchIn(title) -> "apac"
        else -> null
    }

    private fun opsOk(e: GameEvent, ops: Int) =
        (e.minOps == null || ops >= e.minOps) && (e.maxOps == null || ops <= e.maxOps)

    private fun isMinor(e: GameEvent) = e.type in MINOR_TYPES || MINOR_TITLE.containsMatchIn(e.title)

    private fun isSpecial(e: GameEvent) = TOURNAMENT.containsMatchIn(e.title) || INCURSION.containsMatchIn(e.title)

    private class Norm(val ev: GameEvent, val s: Long, val e: Long) {
        val task: String = Tasks.short(baseTitle(ev.title), ev.description, ev.type)
        val category: String = Tasks.category(task, ev.type)
    }

    private fun spot(list: List<GameEvent>, now: Long): Spot? =
        list.filter { it.endMs > now }.minByOrNull { it.startMs }
            ?.let { Spot(clean(it.title), it.startMs, it.endMs, it.startMs <= now) }

    private fun group(list: List<Norm>, lastReset: Long, nextReset: Long): List<EventRow> =
        list.groupBy { Triple(baseTitle(it.ev.title), it.s, it.e) }
            .map { (k, v) ->
                EventRow(
                    title = k.first,
                    formats = v.mapNotNull { formatOf(it.ev) }.distinct().sorted().joinToString("\u00B7"),
                    type = v.first().ev.type,
                    startMs = k.second,
                    endMs = k.third,
                    endsToday = k.third <= nextReset,
                    isNew = k.second >= lastReset,
                    task = v.first().task,
                    category = v.first().category,
                )
            }
            .sortedWith(compareBy<EventRow>({ it.endMs }, { it.title }))

    fun build(events: List<GameEvent>, territory: List<TerritoryRow>, prefs: Prefs, now: Long, raidRows: List<TerritoryRow> = emptyList()): Snapshot {
        val lr = GameClock.lastReset(now)
        val nr = GameClock.nextReset(now)

        val tournaments = events.filter { TOURNAMENT.containsMatchIn(it.title) }
        val incursions = events.filter {
            INCURSION.containsMatchIn(it.title) && !TOURNAMENT.containsMatchIn(it.title) &&
                (regionOf(it.title)?.let { r -> r in prefs.regions } ?: true)
        }
        val maintenance = events.filter { it.type == "maintenance" && it.endMs > now && it.startMs < now + 3 * DAY_MS }

        val normal = events.asSequence()
            .filter { !isSpecial(it) && it.type != "maintenance" }
            .filter { opsOk(it, prefs.ops) }
            .filter { !prefs.hideMinor || !isMinor(it) }
            .map { Norm(it, GameClock.snap(it.startMs), GameClock.snap(it.endMs)) }
            .filter { it.category !in prefs.hiddenCats }
            .filter { prefs.includeLong || it.e - it.s <= LONG_EVENT_MS }
            .filter { it.e > now }
            .toList()

        // NOW = running right now (ending soonest first); NEXT = what starts next (soonest first).
        // Both tick over by themselves because they are recomputed against the clock on every redraw.
        val nowAll = group(normal.filter { it.s <= now && it.e > now }, lr, nr)
        val nextAll = group(normal.filter { it.s > now && it.s <= now + 7 * DAY_MS }, lr, nr)
            .sortedWith(compareBy<EventRow>({ it.startMs }, { it.endMs }, { it.title }))

        return Snapshot(
            nowMs = now,
            lastResetMs = lr,
            nextResetMs = nr,
            current = nowAll.take(prefs.maxNow),
            currentMore = (nowAll.size - prefs.maxNow).coerceAtLeast(0),
            upcoming = nextAll.take(prefs.maxNext),
            upcomingMore = (nextAll.size - prefs.maxNext).coerceAtLeast(0),
            currentAll = nowAll,
            upcomingAll = nextAll,
            territory = nextTerritory(territory, now, 2),
            raids = if (prefs.showRaids) nextTerritory(raidRows, now, 2) else emptyList(),
            incursion = spot(incursions, now),
            tournament = spot(tournaments, now),
            maintenance = spot(maintenance, now),
        )
    }

    /** The next [count] Phx territory slots (one that is running right now counts), soonest first. */
    fun nextTerritory(rows: List<TerritoryRow>, now: Long, count: Int): List<TerritorySlot> {
        val today = Instant.ofEpochMilli(now).atZone(ZoneOffset.UTC).toLocalDate()
        val out = ArrayList<TerritorySlot>()
        for (r in rows) {
            val time = runCatching { LocalTime.parse(r.startUtc.take(5)) }.getOrNull() ?: continue
            val dur = r.durationMin * 60_000L
            for (back in -1..8) {
                val d = today.plusDays(back.toLong())
                if (d.dayOfWeek.value % 7 != r.weekday) continue
                val start = d.atTime(time).toInstant(ZoneOffset.UTC).toEpochMilli()
                if (start + dur > now) {
                    out.add(TerritorySlot(r.system, start, start + dur))
                    break
                }
            }
        }
        return out.sortedBy { it.startMs }.take(count)
    }
}
