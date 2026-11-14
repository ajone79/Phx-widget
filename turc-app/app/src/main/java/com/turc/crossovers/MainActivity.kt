package com.turc.crossovers

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val Night = darkColorScheme(
    primary = Color(0xFFFFB347),
    onPrimary = Color(0xFF0B1030),
    background = Color(0xFF0B1026),
    surface = Color(0xFF0B1026),
    onBackground = Color(0xFFF1F3FF),
    onSurface = Color(0xFFF1F3FF),
    onSurfaceVariant = Color(0xFF8E96BE),
    secondaryContainer = Color(0xFF2A3470),
    onSecondaryContainer = Color(0xFFFFE2B3),
)
private val Teal = Color(0xFF45C4B0)
private val CardBg = Color(0xFF141B3D)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { MaterialTheme(colorScheme = Night) { Screen() } }
    }
}

private val TIME_FMT: DateTimeFormatter = DateTimeFormatter.ofPattern("EEE d MMM HH:mm", Locale.UK)

private fun fmt(ms: Long): String = TIME_FMT.format(Instant.ofEpochMilli(ms).atZone(ZoneId.systemDefault()))

private fun timeLeft(ms: Long, now: Long): String {
    val mins = ((ms - now) / 60_000L).coerceAtLeast(0)
    return when {
        mins < 60 -> "${mins}m"
        mins < 48 * 60 -> "${mins / 60}h ${mins % 60}m"
        else -> "${mins / (24 * 60)}d ${(mins / 60) % 24}h"
    }
}

@Composable
private fun Screen() {
    val ctx = androidx.compose.ui.platform.LocalContext.current
    val prefs = remember { ctx.getSharedPreferences("turc", Context.MODE_PRIVATE) }
    var grade by remember { mutableIntStateOf(prefs.getInt("grade", -1)) }
    var days by remember { mutableIntStateOf(prefs.getInt("days", 7)) }
    var threePlus by remember { mutableStateOf(prefs.getBoolean("threePlus", false)) }
    var events by remember { mutableStateOf(Feed.cached(ctx, System.currentTimeMillis())) }
    var error by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(false) }
    var reload by remember { mutableIntStateOf(0) }

    LaunchedEffect(reload) {
        loading = true
        val err = withContext(Dispatchers.IO) { Feed.refresh(ctx) }
        error = err
        if (err == null) events = withContext(Dispatchers.IO) { Feed.cached(ctx, System.currentTimeMillis()) }
        loading = false
    }

    val now = System.currentTimeMillis()
    val band = if (grade in Grades.BANDS.indices) Grades.BANDS[grade] else null
    val all = remember(events, grade, days) {
        Crossovers.stacks(events, now, now + days * 86_400_000L, band)
    }
    val shown = if (threePlus) all.filter { it.events.size >= 3 } else all
    val groups = groupStacks(shown, now)

    Column(Modifier.fillMaxSize().statusBarsPadding().padding(horizontal = 14.dp)) {
        Row(Modifier.fillMaxWidth().padding(top = 10.dp), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            Text("Crossovers", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, modifier = Modifier.weight(1f))
            TextButton(onClick = { reload++ }) { Text(if (loading) "Updating…" else "Refresh") }
        }
        Text("Events you can score in with the same action, at the same time.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)

        Row(Modifier.horizontalScroll(rememberScrollState()).padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            FilterChip(selected = grade == -1, onClick = { grade = -1; prefs.edit().putInt("grade", -1).apply() }, label = { Text("All") })
            Grades.NAMES.forEachIndexed { i, n ->
                FilterChip(selected = grade == i, onClick = { grade = i; prefs.edit().putInt("grade", i).apply() }, label = { Text(n) })
            }
        }
        Row(Modifier.padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf(3, 7, 14).forEach { d ->
                FilterChip(selected = days == d, onClick = { days = d; prefs.edit().putInt("days", d).apply() }, label = { Text("$d days") })
            }
            FilterChip(selected = threePlus, onClick = { threePlus = !threePlus; prefs.edit().putBoolean("threePlus", threePlus).apply() }, label = { Text("3+ only") })
        }
        if (error != null) Text(error ?: "", color = Color(0xFFFF8A80), fontSize = 12.sp, modifier = Modifier.padding(top = 6.dp))

        if (groups.isEmpty()) {
            Text(
                if (events.isEmpty()) (if (loading) "Loading the event feed…" else "No event data yet. Tap Refresh when you're online.")
                else "No crossovers in this window for this grade. Try more days or another grade.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 24.dp),
            )
        }
        LazyColumn(contentPadding = PaddingValues(vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(groups) { g -> GroupCard(g, now) }
            item {
                Text(
                    "Matched from the event descriptions in the feed, so treat it as a guide. Passes and metas are left out. Times are your phone's time.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp, modifier = Modifier.padding(vertical = 8.dp),
                )
            }
        }
    }
}

private fun shortTitle(t: String): String = t.removePrefix("Outbreak: Battlelines ").removePrefix("Outbreak: ")

/** One action with every window it stacks in, so the same events aren't repeated across cards. */
private data class Group(val label: String, val extra: Int, val stacks: List<Stack>) {
    val peak: Int get() = stacks.maxOf { it.events.size }
}

private fun groupStacks(stacks: List<Stack>, now: Long): List<Group> =
    stacks.groupBy { it.labels.first() }
        .map { (label, list) -> Group(label, (list.maxOf { it.labels.size } - 1), list) }
        .sortedWith(
            compareBy<Group> { g -> if (g.stacks.any { it.startMs <= now }) 0 else 1 }
                .thenByDescending { it.peak }
                .thenBy { it.stacks.first().startMs },
        )

/** Why this window is the one to use, in a sentence or two. */
private fun reasonFor(best: Stack, others: List<Stack>, now: Long): String {
    val n = best.events.size
    val parts = ArrayList<String>()
    parts.add("One action here scores in $n events at once.")
    val bestOther = others.maxOfOrNull { it.events.size }
    when {
        bestOther == null -> parts.add("It's the only window in this period.")
        bestOther < n -> parts.add("Other windows only reach ×$bestOther, so this is the most you can get from it.")
        else -> parts.add("Later windows give the same ×$n, so use whichever suits you.")
    }
    if (best.startMs <= now) parts.add("It ends in ${timeLeft(best.endMs, now)}, so use it before then.")
    return parts.joinToString(" ")
}

@Composable
private fun GroupCard(g: Group, now: Long) {
    var open by remember(g.label) { mutableStateOf(false) }
    val best = g.stacks.sortedWith(compareByDescending<Stack> { it.events.size }.thenBy { it.startMs }).first()
    val others = g.stacks.filter { it !== best }
    val big = best.events.size >= 4
    val live = best.startMs <= now
    Card(
        colors = CardDefaults.cardColors(containerColor = CardBg),
        border = if (big) BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else null,
    ) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                Text(
                    "×${best.events.size}",
                    fontSize = 22.sp, fontWeight = FontWeight.Bold,
                    color = if (big) MaterialTheme.colorScheme.primary else Teal,
                    modifier = Modifier.padding(end = 10.dp),
                )
                Text(g.label + if (g.extra > 0) " (+${g.extra} more)" else "", fontWeight = FontWeight.SemiBold, fontSize = 15.sp, modifier = Modifier.weight(1f))
            }
            Text("BEST TIME", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 10.dp))
            Text(
                if (live) "Now until ${fmt(best.endMs)}" else "${fmt(best.startMs)} → ${fmt(best.endMs)}",
                color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold, fontSize = 14.sp,
            )
            Text(reasonFor(best, others, now), fontSize = 13.sp, modifier = Modifier.padding(top = 4.dp))
            Text("Counts in:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 8.dp))
            best.events.forEach { e -> Text("• ${shortTitle(e.title)}", fontSize = 13.sp, modifier = Modifier.padding(start = 4.dp, top = 2.dp)) }
            if (others.isNotEmpty()) {
                TextButton(onClick = { open = !open }, modifier = Modifier.padding(top = 4.dp)) {
                    Text(if (open) "Hide other windows" else "Other windows (${others.size})")
                }
                if (open) others.forEach { s ->
                    Text(
                        (if (s.startMs <= now) "Now → ${fmt(s.endMs)}" else "${fmt(s.startMs)} → ${fmt(s.endMs)}") + "  ·  ×${s.events.size}",
                        fontSize = 12.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 6.dp),
                    )
                    Text(s.events.joinToString(" · ") { shortTitle(it.title) }, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}
