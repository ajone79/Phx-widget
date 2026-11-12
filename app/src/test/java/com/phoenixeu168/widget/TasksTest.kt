package com.phoenixeu168.widget

import com.phoenixeu168.widget.data.Engine
import com.phoenixeu168.widget.data.GameEvent
import com.phoenixeu168.widget.data.Prefs
import com.phoenixeu168.widget.data.Tasks
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class TasksTest {
    private fun t(title: String, desc: String, type: String = "hostiles") = Tasks.short(title, desc, type)

    @Test fun hostileTargets() {
        assertEquals("Kill FKR", t("Division 12 - FKR", "Score in this event by defeating Federation, Klingon, and Romulan hostiles."))
        assertEquals("Kill Academy drones", t("Drone Sweep", "Score in this event by destroying Academy Drone Hostiles ."))
        assertEquals("Kill Academy drones", t("Haven", "Score in this event by destroying Academy Drone and Venari Ral Hostiles."))
        assertEquals("Kill Borg probes", t("x", "Score in this event by destroying Borg Probes."))
        assertEquals("Kill Dominion", t("x", "Score in this event by destroying Dominion hostiles. Earn Bonus points for defeating hostiles while using U.S.S. Defiant."))
        assertEquals("Kill any hostiles", t("Hostile Offensive", "Score in this event by destroying hostiles."))
        assertEquals("Kill FKR in anomaly systems", t("x", "Score in this event by defeating Federation, Klingon, and Romulan hostiles in a system that is affected by a Galactic Anomaly."))
        assertEquals("Kill Klingon hostiles", t("x", "Score in this event by defeating Klingon hostiles."))
    }

    @Test fun armadasWavesMining() {
        assertEquals("Kill Borg Sphere solo armadas", t("x", "Score in this event by defeating Borg Sphere Solo Armadas.", "armadas"))
        assertEquals("Use armada directives", t("x", "Score in this event by using Uncommon and Rare Formation Armada Directives.", "armadas"))
        assertEquals("Complete wave defense waves", t("x", "Score in this event by defeating waves in Duo Wave Defense.", "waves"))
        assertEquals("Mine Isogenite & Geodes", t("x", "Score in this event by mining Isogenite & Geodes.", "mining"))
    }

    @Test fun spendingAndUpgrading() {
        assertEquals("Spend G3 U+ mats", t("x", "Score in this event by Spending G3 U or Higher Materials to earn epic officer Shards.", "recruit"))
        assertEquals("Spend ship parts & upgrade ships", t("Starship Armament", "Score in this event by spending like Ra'ul Reaver, Athena, Borg Sphere parts and spending materials on ship upgrades.", "ship_upgrade"))
        assertEquals("Upgrade USS Kelcie Mae", t("x", "Score in this event by upgrading the U.S.S. Kelcie Mae.", "ship_upgrade"))
        assertEquals("Complete the daily events", t("x Meta", "Score in this event by completing daily Division 12: Classified Orders SMS Events.", "meta"))
    }

    @Test fun neverLongerThanAWidgetLine() {
        val long = t("x", "Score in this event by spending an extremely long list of different materials and parts and tokens and credits.")
        assertTrue(long.length <= 40)
    }

    @Test fun categories() {
        assertEquals(Tasks.HOSTILES, Tasks.category("Kill FKR", "hostiles"))
        assertEquals(Tasks.ARMADAS, Tasks.category("Kill Borg solo armadas", "armadas"))
        assertEquals(Tasks.SPEND, Tasks.category("Spend G3 U+ mats", "recruit"))
        assertEquals(Tasks.META, Tasks.category("Complete the daily events", "meta"))
    }

    @Test fun hiddenCategoriesDisappearFromTheWidget() {
        val ms = { s: String -> Instant.parse(s).toEpochMilli() }
        val now = ms("2026-11-11T18:30:00Z")
        val events = listOf(
            GameEvent(1, "Drone Sweep - SMS", "hostiles", "SMS", "normal", null, null, ms("2026-11-10T17:00:00Z"), ms("2026-11-12T17:00:00Z"),
                "Score in this event by destroying Academy Drone Hostiles."),
            GameEvent(2, "Isogenite Rush - SMS", "mining", "SMS", "normal", null, null, ms("2026-11-10T17:00:00Z"), ms("2026-11-12T17:00:00Z"),
                "Score in this event by mining Isogenite & Geodes."),
        )
        val all = Engine.build(events, emptyList(), Prefs(), now)
        assertEquals(setOf("Drone Sweep", "Isogenite Rush"), all.current.map { it.title }.toSet())
        assertEquals("Kill Academy drones", all.current.first { it.title == "Drone Sweep" }.task)
        val noMining = Engine.build(events, emptyList(), Prefs(hiddenCats = setOf(Tasks.MINING)), now)
        assertEquals(listOf("Drone Sweep"), noMining.current.map { it.title })
        assertEquals(2, all.currentAll.size)
    }
}
