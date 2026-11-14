package com.turc.crossovers

/** One event from the feed (SMS/SLB/etc. variants already merged) with what it scores for. */
data class Ev(
    val title: String,
    val startMs: Long,
    val endMs: Long,
    val minOps: Int?,
    val maxOps: Int?,
    /** What the event scores on, e.g. "polaron", "fkr", "building". Empty = not used for crossovers. */
    val accepts: Set<String>,
)

/** Several events that one action scores in at the same time. */
data class Stack(
    val startMs: Long,
    val endMs: Long,
    /** The actions that earn in all of [events], e.g. "Kill Polaron FKR hostiles". */
    val labels: List<String>,
    val events: List<Ev>,
)

/** Player grade bands (G1 is 20 levels wide, every other band is 10). */
object Grades {
    val NAMES = listOf("G1", "G2", "G3", "G4", "G5", "G6", "G7", "G8")
    val BANDS = listOf(1..20, 21..30, 31..40, 41..50, 51..60, 61..70, 71..80, 81..90)
}

object Crossovers {
    /** An action a player can take, and the event targets it counts toward. */
    data class Action(val label: String, val attrs: Set<String>)

    val ACTIONS = listOf(
        Action("Kill Polaron FKR hostiles", setOf("polaron", "fkr", "hostile")),
        Action("Kill Federation hostiles", setOf("federation", "fkr", "hostile")),
        Action("Kill Klingon hostiles", setOf("klingon", "fkr", "hostile")),
        Action("Kill Romulan hostiles", setOf("romulan", "fkr", "hostile")),
        Action("Kill Aggregation hostiles", setOf("aggregation", "hostile")),
        Action("Kill Borg hostiles", setOf("borg", "hostile")),
        Action("Kill Xindi hostiles", setOf("xindi", "hostile")),
        Action("Kill Suliban hostiles", setOf("suliban", "hostile")),
        Action("Kill Academy drones", setOf("drone", "hostile")),
        Action("Defeat Borg armadas", setOf("borg-armada", "armada")),
        Action("Defeat FKR armadas", setOf("fkr-armada", "armada")),
        Action("Use Krenim directives", setOf("krenim", "armada")),
        Action("Upgrade a building (G3+ materials)", setOf("building", "g3")),
        Action("Upgrade research (G3+ materials)", setOf("research", "g3")),
        Action("Upgrade a ship (G3+ materials)", setOf("ship", "g3")),
        Action("Spend Latinum", setOf("latinum")),
    )

    private val CONTAINER_TYPES = setOf("meta", "battlepass", "arc_banner", "maintenance", "ticket", "waves")
    private val CONTAINER_TITLE = Regex("\\b(pass|meta)\\b")
    private val HOSTILE_WORDS = Regex("hostile|destroy|defeat")
    private val SPEND_WORDS = Regex("spending|upgrad")
    private val FKR_ALL = Regex("federation.*klingon.*romulan")
    private val FKR_WORD = Regex("\\bfkr\\b")
    private val RESEARCH = Regex("upgrading research|polaron research")
    private val BUILDING = Regex("upgrading (and spending )?(specialty )?building|station|specialty building")
    private val SHIP = Regex("ship upgrade|ship parts")
    private val G3 = Regex("spending g3")

    /**
     * Works out what an event scores on from its title and description. Meta, pass, battle-pass and similar
     * "complete other events" containers return an empty set so they never count as a crossover.
     */
    fun accepts(title: String, description: String, type: String): Set<String> {
        if (type in CONTAINER_TYPES || CONTAINER_TITLE.containsMatchIn(title.lowercase())) return emptySet()
        val t = (title + " " + description).lowercase().replace('\n', ' ')
        val a = LinkedHashSet<String>()
        if (t.contains("armada")) {
            when {
                t.contains("borg") -> a.add("borg-armada")
                t.contains("krenim") -> a.add("krenim")
                FKR_WORD.containsMatchIn(t) -> a.add("fkr-armada")
                else -> a.add("armada")
            }
        } else if (HOSTILE_WORDS.containsMatchIn(t) && !SPEND_WORDS.containsMatchIn(t)) {
            when {
                t.contains("polaron") -> a.add("polaron")
                FKR_ALL.containsMatchIn(t) || FKR_WORD.containsMatchIn(t) -> a.add("fkr")
                t.contains("romulan") -> a.add("romulan")
                t.contains("klingon") -> a.add("klingon")
                t.contains("federation") -> a.add("federation")
                t.contains("aggregation") -> a.add("aggregation")
                t.contains("borg") -> a.add("borg")
                t.contains("xindi") -> a.add("xindi")
                t.contains("suliban") -> a.add("suliban")
                t.contains("drone") -> a.add("drone")
                else -> a.add("hostile")
            }
        }
        if (RESEARCH.containsMatchIn(t)) a.add("research")
        if (BUILDING.containsMatchIn(t)) a.add("building")
        if (SHIP.containsMatchIn(t)) a.add("ship")
        if (G3.containsMatchIn(t)) a.add("g3")
        if (t.contains("latinum")) a.add("latinum")
        return a
    }

    /** True if the event can be played at some level inside the band. */
    fun inGrade(e: Ev, band: IntRange?): Boolean {
        if (band == null) return true
        val lo = e.minOps ?: 1
        val hi = e.maxOps ?: 999
        return lo <= band.last && hi >= band.first
    }

    /**
     * Finds every time window, from [nowMs] up to [horizonMs], where one action scores in two or more
     * events at once. Sorted: running now first, then by start time.
     */
    fun stacks(events: List<Ev>, nowMs: Long, horizonMs: Long, band: IntRange?): List<Stack> {
        val pool = events.filter { it.accepts.isNotEmpty() && it.endMs > nowMs && it.startMs < horizonMs && inGrade(it, band) }
        // (start, end, member indexes) -> labels of the actions that earn in all of them
        val found = LinkedHashMap<Triple<Long, Long, Set<Int>>, MutableList<String>>()
        for (action in ACTIONS) {
            val members = pool.indices.filter { i -> pool[i].accepts.any { it in action.attrs } }
            if (members.size < 2) continue
            val cuts = sortedSetOf(nowMs, horizonMs)
            for (i in members) {
                val e = pool[i]
                if (e.startMs > nowMs && e.startMs < horizonMs) cuts.add(e.startMs)
                if (e.endMs > nowMs && e.endMs < horizonMs) cuts.add(e.endMs)
            }
            val cutList = cuts.toList()
            val segs = ArrayList<Triple<Long, Long, Set<Int>>>()
            for (k in 0 until cutList.size - 1) {
                val a = cutList[k]
                val b = cutList[k + 1]
                val active = members.filter { pool[it].startMs <= a && pool[it].endMs >= b }.toSet()
                if (active.size < 2) continue
                val last = segs.lastOrNull()
                if (last != null && last.third == active && last.second == a) {
                    segs[segs.size - 1] = Triple(last.first, b, active)
                } else {
                    segs.add(Triple(a, b, active))
                }
            }
            for (s in segs) found.getOrPut(s) { ArrayList() }.add(action.label)
        }
        val keys = found.keys.toList()
        val kept = keys.filter { k ->
            // drop a window if a bigger stack covers part of the same time with a superset of its events
            keys.none { o -> o.third.size > k.third.size && o.third.containsAll(k.third) && o.first < k.second && o.second > k.first }
        }
        return kept.map { k ->
            Stack(k.first, k.second, found.getValue(k).toList(), k.third.sorted().map { pool[it] })
        }.sortedWith(
            compareBy<Stack> { if (it.startMs <= nowMs) 0 else 1 }
                .thenBy { it.startMs }
                .thenByDescending { it.events.size },
        )
    }
}
