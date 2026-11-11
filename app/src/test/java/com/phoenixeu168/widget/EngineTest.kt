package com.phoenixeu168.widget

import com.phoenixeu168.widget.data.Engine
import com.phoenixeu168.widget.data.GameEvent
import com.phoenixeu168.widget.data.Prefs
import com.phoenixeu168.widget.data.TerritoryRow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

private fun ms(s: String) = Instant.parse(s).toEpochMilli()

private fun ev(
    id: Long, title: String, start: String, end: String, type: String = "hostiles",
    min: Int? = null, max: Int? = null, fmt: String? = null,
) = GameEvent(id, title, type, fmt, "normal", min, max, ms(start), ms(end))

class EngineTest {
    // Wed 11 Nov 2026 18:30 UTC: the winter reset (17:00 UTC) was 1.5 h ago, the next one is tomorrow 17:00 UTC.
    private val now = ms("2026-11-11T18:30:00Z")

    @Test fun opsLevelFiltersEvents() {
        val events = listOf(
            ev(1, "Hostile Assault - SMS", "2026-11-09T17:00:00Z", "2026-11-12T17:00:00Z", min = 20, max = 25),
            ev(2, "Drone Sweep - SMS", "2026-11-09T17:00:00Z", "2026-11-12T17:00:00Z", min = 61, max = 80),
        )
        val at45 = Engine.build(events, emptyList(), Prefs(ops = 45), now)
        assertTrue(at45.current.isEmpty())
        val at22 = Engine.build(events, emptyList(), Prefs(ops = 22), now)
        assertEquals(listOf("Hostile Assault"), at22.current.map { it.title })
        val at70 = Engine.build(events, emptyList(), Prefs(ops = 70), now)
        assertEquals(listOf("Drone Sweep"), at70.current.map { it.title })
    }

    @Test fun mixed1600And1700EndsCountAsTheSameDay() {
        val events = listOf(
            ev(1, "Ends at 17", "2026-11-09T17:00:00Z", "2026-11-12T17:00:00Z"),
            ev(2, "Ends at 16", "2026-11-08T16:00:00Z", "2026-11-12T16:00:00Z"),
        )
        val s = Engine.build(events, emptyList(), Prefs(), now)
        assertEquals(2, s.current.size)
        assertTrue(s.current.all { it.endsToday })
        assertEquals(s.current[0].endMs, s.current[1].endMs)
    }

    @Test fun nextListsWhatStartsSoonestAndTicksOver() {
        val events = listOf(
            ev(1, "Later", "2026-11-13T17:00:00Z", "2026-11-14T17:00:00Z"),
            ev(2, "Soon at 16", "2026-11-12T16:00:00Z", "2026-11-13T16:00:00Z"),
            ev(3, "Running", "2026-11-09T17:00:00Z", "2026-11-15T17:00:00Z"),
        )
        val s = Engine.build(events, emptyList(), Prefs(), now)
        assertEquals(listOf("Running"), s.current.map { it.title })
        // 16:00 UTC is snapped onto the 17:00 reset, then ordered by start
        assertEquals(listOf("Soon at 16", "Later"), s.upcoming.map { it.title })
        assertEquals(ms("2026-11-12T17:00:00Z"), s.upcoming[0].startMs)
        // one day on, "Soon at 16" is running and "Later" is next
        val later = Engine.build(events, emptyList(), Prefs(), ms("2026-11-12T18:00:00Z"))
        assertEquals(setOf("Running", "Soon at 16"), later.current.map { it.title }.toSet())
        assertEquals(listOf("Later"), later.upcoming.map { it.title })
    }

    @Test fun formatVariantsAreMerged() {
        val events = listOf(
            ev(1, "Armada Assault - SMS", "2026-11-09T17:00:00Z", "2026-11-12T17:00:00Z", fmt = "SMS"),
            ev(2, "Armada Assault - SLB", "2026-11-09T17:00:00Z", "2026-11-12T17:00:00Z", fmt = "SLB"),
        )
        val s = Engine.build(events, emptyList(), Prefs(), now)
        assertEquals(1, s.current.size)
        assertEquals("Armada Assault", s.current[0].title)
        assertEquals("SLB·SMS", s.current[0].formats)
    }

    @Test fun incursionAndTournamentAreSpotlightedAndRegionFiltered() {
        val events = listOf(
            ev(1, "🇪🇺 E.U. Infinite Incursions: The Griffin Calamity ⚠️", "2026-11-28T16:00:00Z", "2026-11-29T16:00:00Z", type = ""),
            ev(2, "🇺🇸 U.S. Infinite Incursions: The Griffin Calamity", "2026-11-14T21:00:00Z", "2026-11-15T21:00:00Z", type = ""),
            ev(3, "Alliance Tournaments: Rally", "2026-11-23T16:00:00Z", "2026-11-28T16:00:00Z", type = "alliance"),
        )
        val s = Engine.build(events, emptyList(), Prefs(), now)
        assertEquals("E.U. Infinite Incursions: The Griffin Calamity", s.incursion?.title)
        assertEquals("Alliance Tournaments: Rally", s.tournament?.title)
        assertTrue(s.current.isEmpty())
        val us = Engine.build(events, emptyList(), Prefs(regions = setOf("us")), now)
        assertEquals(ms("2026-11-14T21:00:00Z"), us.incursion?.startMs)
        assertNull(Engine.build(emptyList(), emptyList(), Prefs(), now).incursion)
    }

    @Test fun passesAndLongEventsAreHiddenUnlessAsked() {
        val events = listOf(
            ev(1, "Officer Command Pass", "2026-11-04T17:00:00Z", "2026-11-12T17:00:00Z", type = ""),
            ev(2, "Mission Key Hunter - SMS", "2026-10-15T17:00:00Z", "2026-12-15T17:00:00Z"),
            ev(3, "Real event", "2026-11-10T17:00:00Z", "2026-11-12T17:00:00Z"),
        )
        assertEquals(listOf("Real event"), Engine.build(events, emptyList(), Prefs(), now).current.map { it.title })
        assertEquals(3, Engine.build(events, emptyList(), Prefs(hideMinor = false, includeLong = true), now).current.size)
    }

    @Test fun nextTwoTerritorySlots() {
        val rows = listOf(
            TerritoryRow("Adia", 3, "09:00:00", 30),   // Wednesday
            TerritoryRow("Tezera", 6, "16:00:00", 30), // Saturday
            TerritoryRow("Tholus", 0, "17:00:00", 30), // Sunday
        )
        val slots = Engine.nextTerritory(rows, now, 2)
        assertEquals(listOf("Tezera", "Tholus"), slots.map { it.system })
        assertEquals(ms("2026-11-14T16:00:00Z"), slots[0].startMs)
        // a slot that is running right now is still listed
        val live = Engine.nextTerritory(rows, ms("2026-11-11T09:10:00Z"), 2)
        assertEquals("Adia", live[0].system)
        assertNotNull(live.getOrNull(1))
    }

    @Test fun maintenanceBannerWithinThreeDays() {
        val events = listOf(ev(1, "Update 96 maintenance", "2026-11-12T09:00:00Z", "2026-11-12T11:30:00Z", type = "maintenance"))
        assertNotNull(Engine.build(events, emptyList(), Prefs(), now).maintenance)
    }
}
