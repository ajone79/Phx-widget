package com.phoenixeu168.widget.data

/**
 * Turns an event's long description into the one-line job you actually need ("Kill FKR hostiles",
 * "Mine isogenite & geodes", "Spend G3 U+ mats"), and sorts events into a few broad categories.
 * Pure rules on the published text, so it needs no extra data and works offline.
 */
object Tasks {
    const val HOSTILES = "Hostiles"
    const val ARMADAS = "Armadas"
    const val WAVES = "Waves"
    const val SPEND = "Spend & upgrade"
    const val MINING = "Mining"
    const val META = "Alliance & meta"
    const val OTHER = "Other"

    val CATEGORIES = listOf(HOSTILES, ARMADAS, WAVES, SPEND, MINING, META, OTHER)

    private const val MAX_LEN = 38

    /** First sentence, lower case, with "U.S.S." style dots removed so they do not end the sentence. */
    private fun sentence(desc: String): String {
        val s = desc.replace("\n", " ").trim()
            .replace("U.S.S.", "USS").replace("U.S.", "US").replace("E.U.", "EU")
            .replace("’", "'")
        val cut = s.indexOfFirst { it == '.' || it == '!' }
        val one = if (cut in 0 until s.length) s.substring(0, cut) else s
        return one.replace(Regex("^score (in this event )?by\\s+", RegexOption.IGNORE_CASE), "").trim()
    }

    private fun cap(s: String) = s.replaceFirstChar { it.uppercase() }

    private val HOSTILE_TARGETS = listOf(
        Regex("elite assassin") to "FKR elite assassins",
        Regex("academy drone|venari ral") to "Academy drones",
        Regex("borg probe") to "Borg probes",
        Regex("dominion|jem'?hadar") to "Dominion",
        Regex("v'?ger") to "V'ger",
        Regex("swarm") to "Swarm",
        Regex("suliban") to "Suliban",
        Regex("xindi") to "Xindi",
        Regex("hirogen") to "Hirogen",
        Regex("aggregation") to "Aggregation",
        Regex("augment") to "Augment",
        Regex("mirror") to "Mirror universe",
        Regex("quantum") to "Quantum",
        Regex("breen") to "Breen",
        Regex("el-aurian") to "El-Aurian",
        Regex("federation,? klingon,? (and )?romulan|\\bfkr\\b") to "FKR",
    )

    /** One short line saying what to do in this event. */
    fun short(title: String, description: String, type: String): String {
        val o = sentence(description) // original capitalisation, for names we quote back
        val t = o.lowercase()
        val ti = title.lowercase()
        if (t.isBlank()) return when (type) {
            "mining" -> "Mine"
            "hostiles" -> "Kill hostiles"
            "armadas" -> "Armadas"
            "waves" -> "Wave defense"
            else -> ""
        }

        // Faction hunts: "Invade Klingon space and destroy as many enemy ships as possible"
        Regex("invade (\\w+) space").find(description.lowercase())?.let { return "Kill ${cap(it.groupValues[1])} ships in their space" }
        if (ti.contains("faction lb")) return "Kill enemy faction ships"

        Regex("\\bmining (.+)", RegexOption.IGNORE_CASE).find(o)?.let { return trim("Mine " + it.groupValues[1]) }

        if (t.contains("wave")) return "Complete wave defense waves"

        if (t.contains("armada") && !t.contains("research")) {
            return when {
                t.contains("directive") && t.contains("krenim") -> "Spend Krenim armada directives"
                t.contains("directive") -> "Use armada directives"
                t.contains("group armada") -> "Start group armadas"
                t.contains("fkr") -> "Kill FKR armadas"
                t.contains("borg sphere") -> "Kill Borg Sphere solo armadas"
                t.contains("conqueror") -> "Kill Conqueror Borg solo armadas"
                t.contains("open armada") -> "Kill open armadas"
                t.contains("borg") -> "Kill Borg solo armadas"
                else -> "Kill solo armadas"
            }
        }

        if (t.contains("outpost retaliation")) return "Defeat outpost attackers"

        if (t.contains("stronghold")) {
            val f = listOf("federation", "klingon", "romulan").firstOrNull { t.contains(it) }
            return if (f != null) "Defeat ${cap(f)} strongholds" else "Defeat FKR strongholds"
        }

        if (Regex("(destroying|defeating|destroy|defeat|dealing)").containsMatchIn(t) && !t.contains("complet")) {
            val anomaly = if (t.contains("anomal")) " in anomaly systems" else ""
            if (t.contains("powerful hostiles")) return "Kill powerful hostiles"
            val hit = HOSTILE_TARGETS.firstOrNull { it.first.containsMatchIn(t) }?.second
            if (hit != null) return trim("Kill $hit$anomaly")
            val faction = listOf("federation", "klingon", "romulan").firstOrNull { Regex("$it hostiles").containsMatchIn(t) }
            if (faction != null) return trim("Kill ${cap(faction)} hostiles$anomaly")
            return trim("Kill any hostiles$anomaly")
        }

        // Spending and upgrading
        if (Regex("g\\d u(ncommon)? or higher").containsMatchIn(t)) return "Spend G3 U+ mats"
        if (t.contains("spending like")) return "Spend ship parts & upgrade ships"
        if (t.contains("upgrading research")) return "Spend mats on research & station"
        if (t.contains("gaining power")) return "Gain power: research, buildings, drydocks"
        Regex("spending (.+?) to upgrade", RegexOption.IGNORE_CASE).find(o)?.let { return trim("Spend " + it.groupValues[1]) }
        Regex("upgrading the ([\\w.' ]+?)( or |$)", RegexOption.IGNORE_CASE).find(o)?.let { return trim("Upgrade " + it.groupValues[1]) }

        if (t.contains("sub-event") || t.contains("sub event") || t.contains("sub events") ||
            Regex("completing (daily|regular|this weekend)").containsMatchIn(t) || type == "meta"
        ) return "Complete the daily events"
        if (t.contains("mission key")) return "Gain Mission Key tokens"
        if (t.contains("multiphasic")) return "Gain Multiphasic credits"
        if (t.contains("away team")) return "Complete away team missions"
        if (ti.contains("trivia")) return "Answer the daily trivia"

        // Anything else: turn "spending X" into "Spend X" and keep it short.
        val verbs = mapOf(
            "spending" to "Spend", "completing" to "Complete", "gaining" to "Gain", "earning" to "Earn",
            "upgrading" to "Upgrade", "using" to "Use", "starting" to "Start", "collecting" to "Collect",
            "claiming" to "Claim", "defeating" to "Defeat", "destroying" to "Kill", "mining" to "Mine",
        )
        val m = Regex("^(\\w+)\\s+(.*)$").find(o)
        if (m != null) {
            val verb = verbs[m.groupValues[1].lowercase()]
            if (verb != null) return trim("$verb ${m.groupValues[2]}")
        }
        // Story text with no clear job in it: better to show nothing than a quote from the flavour text.
        return ""
    }

    private fun trim(s: String): String {
        val one = s.replace(Regex("\\s+"), " ").trim()
        if (one.length <= MAX_LEN) return one
        val cut = one.lastIndexOf(' ', MAX_LEN - 1).let { if (it < 12) MAX_LEN - 1 else it }
        return one.substring(0, cut).trimEnd(',', ' ', '&') + "…"
    }

    /** Which broad group an event belongs to (used by the "show only these" settings). */
    fun category(task: String, type: String): String {
        val lower = task.lowercase()
        return when {
            lower.contains("armada") -> ARMADAS
            lower.contains("wave") -> WAVES
            lower.startsWith("kill") || lower.startsWith("defeat") -> HOSTILES
            lower.startsWith("mine") -> MINING
            lower.startsWith("spend") || lower.startsWith("upgrade") || lower.startsWith("gain power") -> SPEND
            lower.startsWith("complete the daily") || lower.startsWith("gain mission") -> META
            else -> when (type) {
                "hostiles" -> HOSTILES
                "armadas" -> ARMADAS
                "waves" -> WAVES
                "mining" -> MINING
                "ship_upgrade", "building", "research", "recruit", "domination", "vengeance" -> SPEND
                "meta", "alliance" -> META
                else -> OTHER
            }
        }
    }
}
