package com.phoenixeu168.widget.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.phoenixeu168.widget.data.Engine
import com.phoenixeu168.widget.data.EventRow
import com.phoenixeu168.widget.data.Fmt
import com.phoenixeu168.widget.data.Repo
import com.phoenixeu168.widget.data.Snapshot
import com.phoenixeu168.widget.data.Store
import java.time.ZoneId

/** Full list behind the widget's "+N more": every event running now or starting in the next 7 days, with its task. */
class EventsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val prefs = Store(this).prefs()
        val now = System.currentTimeMillis() - prefs.timeTravelDays * Engine.DAY_MS
        val snap = Engine.build(Repo(this).events(now), Repo(this).territory(), prefs, now)
        val zone = ZoneId.systemDefault()
        setContent {
            PhxTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    EventsScreen(snap, prefs.ops, zone)
                }
            }
        }
    }
}

@Composable
private fun EventsScreen(s: Snapshot, ops: Int, zone: ZoneId) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().safeDrawingPadding().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        item {
            Text("Events for ops $ops", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(top = 16.dp, bottom = 4.dp))
            Text(
                "Daily reset ${Fmt.clock(s.nextResetMs, zone)} (in ${Fmt.duration(s.nextResetMs - s.nowMs)})",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        item { Header("NOW", Color(0xFFFFB347)) }
        if (s.currentAll.isEmpty()) item { Empty("Nothing running for your ops level") }
        items(s.currentAll) { Line(it, s, zone, now = true) }
        item { Header("NEXT (starts at reset)", Color(0xFF5CE1E6)) }
        if (s.upcomingAll.isEmpty()) item { Empty("Nothing scheduled yet") }
        items(s.upcomingAll) { Line(it, s, zone, now = false) }
        item { Text("", modifier = Modifier.padding(24.dp)) }
    }
}

@Composable
private fun Header(text: String, color: Color) {
    Text(text, color = color, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 14.dp, bottom = 4.dp))
}

@Composable
private fun Empty(text: String) {
    Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun Line(r: EventRow, s: Snapshot, zone: ZoneId, now: Boolean) {
    val left = r.endMs - s.nowMs
    val time = when {
        now && r.endsToday -> "${Fmt.duration(left)} left"
        now -> "until ${Fmt.dayClock(r.endMs, zone)}"
        else -> "in ${Fmt.duration(r.startMs - s.nowMs)} · ${Fmt.dayClock(r.startMs, zone)}"
    }
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(r.title, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
            Text(time, color = if (now) Color(0xFFFFB347) else Color(0xFF5CE1E6), fontWeight = FontWeight.Bold)
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(r.task, color = Color(0xFF4ADE9A), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
            if (r.formats.isNotEmpty()) Text(r.formats, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
        }
    }
}
