package com.turc.crossovers

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CrossoversTest {
    private val h = 3_600_000L

    private fun ev(title: String, s: Long, e: Long, a: Set<String>, lo: Int? = 20, hi: Int? = 80) =
        Ev(title, s * h, e * h, lo, hi, a)

    @Test fun polaronKillCountsForFkrAndGenericHostileEvents() {
        val events = listOf(
            ev("Polaron Initiation", 0, 48, setOf("polaron")),
            ev("Faction Chase", 0, 24, setOf("fkr")),
            ev("Augmented Myths", 0, 24, setOf("hostile")),
            ev("Klingon only", 0, 24, setOf("klingon")),
        )
        val r = Crossovers.stacks(events, 1 * h, 100 * h, null)
        val top = r.first()
        assertEquals(3, top.events.size)
        assertEquals("Kill Polaron FKR hostiles", top.labels.first())
        assertTrue(top.events.none { it.title == "Klingon only" })
    }

    @Test fun windowSplitsWhenAnEventEnds() {
        val events = listOf(
            ev("A", 0, 48, setOf("fkr")),
            ev("B", 0, 24, setOf("fkr")),
            ev("C", 24, 48, setOf("fkr")),
        )
        val r = Crossovers.stacks(events, 1 * h, 100 * h, null)
        assertEquals(2, r.size)
        assertEquals(listOf("A", "B"), r[0].events.map { it.title })
        assertEquals(listOf("A", "C"), r[1].events.map { it.title })
    }

    @Test fun gradeFilterRemovesEventsOutsideTheBand() {
        val events = listOf(
            ev("Low", 0, 48, setOf("fkr"), 20, 50),
            ev("High", 0, 48, setOf("fkr"), 51, 80),
        )
        assertEquals(1, Crossovers.stacks(events, 1 * h, 100 * h, null).size)
        assertTrue(Crossovers.stacks(events, 1 * h, 100 * h, Grades.BANDS[6]).isEmpty()) // G7 only sees High
        assertTrue(Crossovers.stacks(events, 1 * h, 100 * h, Grades.BANDS[2]).isEmpty()) // G3 only sees Low
    }

    @Test fun smallerStackInsideBiggerOneIsDropped() {
        val events = listOf(
            ev("A", 0, 48, setOf("polaron")),
            ev("B", 0, 48, setOf("fkr")),
            ev("C", 0, 48, setOf("hostile")),
        )
        val r = Crossovers.stacks(events, 1 * h, 100 * h, null)
        assertEquals(1, r.size)
        assertEquals(3, r[0].events.size)
    }

    @Test fun liveStacksComeFirst() {
        val events = listOf(
            ev("Later 1", 50, 70, setOf("klingon")),
            ev("Later 2", 50, 70, setOf("klingon")),
            ev("Now 1", 0, 30, setOf("latinum")),
            ev("Now 2", 0, 30, setOf("latinum")),
        )
        val r = Crossovers.stacks(events, 1 * h, 100 * h, null)
        assertEquals("Spend Latinum", r[0].labels.first())
        assertEquals(1 * h, r[0].startMs.coerceAtLeast(1 * h))
    }

    @Test fun readsWhatEventsScoreOn() {
        assertEquals(setOf("polaron"), Crossovers.accepts("Infusion Race", "Score in this event by dealing Infusion Damage to Polaron FKR hostiles .", "hostiles"))
        assertEquals(setOf("fkr"), Crossovers.accepts("Galactic Powers", "Score in this event by defeating Federation, Klingon, and Romulan hostiles.", "hostiles"))
        assertEquals(setOf("hostile"), Crossovers.accepts("Hostile Assault", "Score in this event by destroying hostiles.", "hostiles"))
        assertEquals(setOf("borg-armada"), Crossovers.accepts("Assimilation", "Score in this event by defeating Borg Solo Armadas.", "armadas"))
        assertEquals(setOf("g3"), Crossovers.accepts("Kirk Recruit", "Score in this event by Spending G3 Uncommon or Higher Materials to earn shards of Planetfall Kirk.", "recruit"))
        assertTrue(Crossovers.accepts("Outbreak Pinnacle Meta", "Score in this event by earning Pinnacle Meta Points.", "meta").isEmpty())
        assertTrue(Crossovers.accepts("Outpost Pass", "Score by completing milestones in the Outpost Pass SMS Events.", "").isEmpty())
    }
}
