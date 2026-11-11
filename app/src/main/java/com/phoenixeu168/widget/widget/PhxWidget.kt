package com.phoenixeu168.widget.widget

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.ActionParameters
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.appWidgetBackground
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.color.ColorProvider
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.phoenixeu168.widget.R
import com.phoenixeu168.widget.data.Engine
import com.phoenixeu168.widget.data.EventRow
import com.phoenixeu168.widget.data.Fmt
import com.phoenixeu168.widget.data.Prefs
import com.phoenixeu168.widget.data.Repo
import com.phoenixeu168.widget.data.Snapshot
import com.phoenixeu168.widget.data.Spot
import com.phoenixeu168.widget.data.Store
import com.phoenixeu168.widget.data.TerritorySlot
import com.phoenixeu168.widget.ui.SettingsActivity
import com.phoenixeu168.widget.work.Scheduler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.ZoneId

/** One palette, deliberately the same in light and dark mode: the widget is always a night-sky panel. */
private fun c(argb: Long) = ColorProvider(day = Color(argb), night = Color(argb))

private object Pal {
    val card = c(0xFF171D3B)
    val cardHi = c(0xFF1F2750)
    val text = c(0xFFF1F3FF)
    val muted = c(0xFF8E96BE)
    val amber = c(0xFFFFB347)
    val cyan = c(0xFF5CE1E6)
    val pink = c(0xFFFF6FA8)
    val gold = c(0xFFFFD166)
    val green = c(0xFF4ADE9A)
    val red = c(0xFFFF6B6B)
    val onAccent = c(0xFF0B1030)
}

private const val EVENTS_PAGE = "https://stfc.phoenixeu168.space/phx-events.html"

private fun typeColor(type: String) = when (type) {
    "hostiles" -> Pal.red
    "armadas" -> Pal.cyan
    "waves" -> Pal.pink
    "meta", "alliance" -> Pal.gold
    "mining", "building", "research", "ship_upgrade" -> Pal.green
    "pvp", "domination", "vengeance", "aphelion" -> Pal.pink
    else -> Pal.amber
}

class PhxWidget : GlanceAppWidget() {
    override val sizeMode: SizeMode = SizeMode.Single

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val (snap, prefs, ok) = withContext(Dispatchers.IO) {
            val repo = Repo(context)
            val prefs = Store(context).prefs()
            val now = System.currentTimeMillis()
            Triple(Engine.build(repo.events(now), repo.territory(), prefs, now), prefs, repo.hasData())
        }
        provideContent {
            GlanceTheme { Body(snap, prefs, ok, ZoneId.systemDefault()) }
        }
    }
}

@Composable
private fun Body(s: Snapshot, prefs: Prefs, hasData: Boolean, zone: ZoneId) {
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .appWidgetBackground()
            .background(ImageProvider(R.drawable.widget_bg))
            .cornerRadius(24.dp)
            .padding(horizontal = 8.dp, vertical = 8.dp),
    ) {
        Header(s, prefs, zone)
        Spacer(GlanceModifier.height(6.dp))
        if (!hasData) {
            Card(accent = Pal.amber) {
                Text("Fetching events...", style = TextStyle(color = Pal.text, fontWeight = FontWeight.Bold, fontSize = 13.sp))
                Text("Tap refresh once you are online", style = TextStyle(color = Pal.muted, fontSize = 11.sp))
            }
        } else {
            LazyColumn(modifier = GlanceModifier.fillMaxWidth().defaultWeight()) {
                item { Block { TerritoryCard(s, zone) } }
                item { Block { SpotRow(s, zone) } }
                if (s.maintenance != null) item { Block { MaintenanceBar(s.maintenance, zone) } }
                item { Block { EventsCard("NOW", "daily reset ${Fmt.clock(s.nextResetMs, zone)}", Pal.amber, s.current, s.currentMore, s, zone, now = true) } }
                item { Block { EventsCard("NEXT", "", Pal.cyan, s.upcoming, s.upcomingMore, s, zone, now = false) } }
            }
        }
    }
}

@Composable
private fun Block(content: @Composable () -> Unit) {
    Column(modifier = GlanceModifier.fillMaxWidth().padding(bottom = 6.dp)) { content() }
}

@Composable
private fun Header(s: Snapshot, prefs: Prefs, zone: ZoneId) {
    Row(modifier = GlanceModifier.fillMaxWidth().padding(horizontal = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Text("PHX", maxLines = 1, style = TextStyle(color = Pal.amber, fontWeight = FontWeight.Bold, fontSize = 15.sp))
        Spacer(GlanceModifier.width(6.dp))
        Text("OPS ${prefs.ops}", maxLines = 1, style = TextStyle(color = Pal.muted, fontWeight = FontWeight.Bold, fontSize = 11.sp))
        Spacer(GlanceModifier.width(8.dp))
        Text(
            "reset in ${Fmt.duration(s.nextResetMs - s.nowMs)}",
            maxLines = 1,
            modifier = GlanceModifier.defaultWeight(),
            style = TextStyle(color = Pal.muted, fontSize = 11.sp),
        )
        Image(
            provider = ImageProvider(R.drawable.ic_settings),
            contentDescription = "Settings",
            modifier = GlanceModifier.size(20.dp).clickable(actionStartActivity<SettingsActivity>()),
        )
        Spacer(GlanceModifier.width(10.dp))
        Image(
            provider = ImageProvider(R.drawable.ic_refresh),
            contentDescription = "Refresh",
            modifier = GlanceModifier.size(20.dp).clickable(actionRunCallback<RefreshAction>()),
        )
    }
}

@Composable
private fun Card(accent: ColorProvider, content: @Composable () -> Unit) {
    Column(
        modifier = GlanceModifier
            .fillMaxWidth()
            .background(Pal.card)
            .cornerRadius(16.dp)
            .padding(horizontal = 10.dp, vertical = 7.dp),
    ) {
        content()
    }
}

private fun open(url: String) = actionStartActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))

@Composable
private fun Label(text: String, color: ColorProvider, right: String = "") {
    Row(modifier = GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(text, maxLines = 1, modifier = GlanceModifier.defaultWeight(), style = TextStyle(color = color, fontWeight = FontWeight.Bold, fontSize = 10.sp))
        if (right.isNotEmpty()) Text(right, maxLines = 1, style = TextStyle(color = Pal.muted, fontSize = 10.sp))
    }
}

@Composable
private fun TerritoryCard(s: Snapshot, zone: ZoneId) {
    Column(
        modifier = GlanceModifier.fillMaxWidth().background(Pal.card).cornerRadius(16.dp)
            .padding(horizontal = 10.dp, vertical = 7.dp).clickable(open(EVENTS_PAGE)),
    ) {
        Label("TERRITORY DEFENCE", Pal.cyan)
        if (s.territory.isEmpty()) {
            Text("No slots loaded yet", style = TextStyle(color = Pal.muted, fontSize = 11.sp))
        }
        s.territory.forEach { t -> TerritoryRowView(t, s.nowMs, zone) }
    }
}

@Composable
private fun TerritoryRowView(t: TerritorySlot, now: Long, zone: ZoneId) {
    val live = t.startMs <= now
    Row(modifier = GlanceModifier.fillMaxWidth().padding(top = 3.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(t.system, maxLines = 1, style = TextStyle(color = Pal.text, fontWeight = FontWeight.Bold, fontSize = 14.sp))
        Spacer(GlanceModifier.width(8.dp))
        Text(
            Fmt.dayClock(t.startMs, zone),
            maxLines = 1,
            modifier = GlanceModifier.defaultWeight(),
            style = TextStyle(color = Pal.muted, fontSize = 11.sp),
        )
        Text(
            if (live) "LIVE · ${Fmt.duration(t.endMs - now)} left" else "in ${Fmt.duration(t.startMs - now)}",
            maxLines = 1,
            style = TextStyle(color = if (live) Pal.green else Pal.cyan, fontWeight = FontWeight.Bold, fontSize = 13.sp),
        )
    }
}

@Composable
private fun SpotRow(s: Snapshot, zone: ZoneId) {
    Row(modifier = GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
        Column(modifier = GlanceModifier.defaultWeight()) { SpotCard("INCURSION", Pal.pink, s.incursion, s.nowMs, zone) }
        Spacer(GlanceModifier.width(6.dp))
        Column(modifier = GlanceModifier.defaultWeight()) { SpotCard("TOURNAMENT", Pal.gold, s.tournament, s.nowMs, zone) }
    }
}

@Composable
private fun SpotCard(label: String, color: ColorProvider, spot: Spot?, now: Long, zone: ZoneId) {
    Column(
        modifier = GlanceModifier.fillMaxWidth().background(Pal.card).cornerRadius(16.dp)
            .padding(horizontal = 10.dp, vertical = 7.dp).clickable(open(EVENTS_PAGE)),
    ) {
        Label(label, color)
        if (spot == null) {
            Text("None announced yet", maxLines = 2, style = TextStyle(color = Pal.muted, fontSize = 11.sp))
        } else {
            Text(spot.title, maxLines = 2, style = TextStyle(color = Pal.text, fontWeight = FontWeight.Medium, fontSize = 11.sp))
            if (spot.live) {
                Text("LIVE · ends in ${Fmt.duration(spot.endMs - now)}", maxLines = 1, style = TextStyle(color = Pal.green, fontWeight = FontWeight.Bold, fontSize = 13.sp))
            } else {
                Text("in ${Fmt.duration(spot.startMs - now)}", maxLines = 1, style = TextStyle(color = color, fontWeight = FontWeight.Bold, fontSize = 15.sp))
                Text(Fmt.dayDateClock(spot.startMs, zone), maxLines = 1, style = TextStyle(color = Pal.muted, fontSize = 10.sp))
            }
        }
    }
}

@Composable
private fun MaintenanceBar(m: Spot, zone: ZoneId) {
    Row(
        modifier = GlanceModifier.fillMaxWidth().background(Pal.cardHi).cornerRadius(12.dp).padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("MAINTENANCE", maxLines = 1, style = TextStyle(color = Pal.red, fontWeight = FontWeight.Bold, fontSize = 10.sp))
        Spacer(GlanceModifier.width(8.dp))
        Text(
            if (m.live) "in progress" else Fmt.dayClock(m.startMs, zone),
            maxLines = 1,
            modifier = GlanceModifier.defaultWeight(),
            style = TextStyle(color = Pal.text, fontSize = 11.sp),
        )
    }
}

@Composable
private fun EventsCard(label: String, right: String, color: ColorProvider, rows: List<EventRow>, more: Int, s: Snapshot, zone: ZoneId, now: Boolean) {
    Column(modifier = GlanceModifier.fillMaxWidth().background(Pal.card).cornerRadius(16.dp).padding(horizontal = 10.dp, vertical = 7.dp)) {
        Label(label, color, right)
        if (rows.isEmpty()) {
            Text(if (now) "Nothing running for your ops level" else "Nothing scheduled yet", style = TextStyle(color = Pal.muted, fontSize = 11.sp))
        }
        rows.forEach { r -> EventLine(r, s, zone, now) }
        if (more > 0) Text("+$more more", maxLines = 1, style = TextStyle(color = Pal.muted, fontSize = 10.sp))
    }
}

@Composable
private fun EventLine(r: EventRow, s: Snapshot, zone: ZoneId, now: Boolean) {
    val left = r.endMs - s.nowMs
    val urgent = now && r.endsToday && left < 3 * 3_600_000L
    Row(modifier = GlanceModifier.fillMaxWidth().padding(top = 3.dp), verticalAlignment = Alignment.CenterVertically) {
        Text("●", maxLines = 1, style = TextStyle(color = typeColor(r.type), fontSize = 9.sp))
        Spacer(GlanceModifier.width(6.dp))
        Text(r.title, maxLines = 1, modifier = GlanceModifier.defaultWeight(), style = TextStyle(color = Pal.text, fontWeight = FontWeight.Medium, fontSize = 12.sp))
        if (r.formats.isNotEmpty()) {
            Spacer(GlanceModifier.width(4.dp))
            Text(r.formats, maxLines = 1, style = TextStyle(color = Pal.muted, fontSize = 9.sp))
        }
        Spacer(GlanceModifier.width(6.dp))
        val (text, color) = when {
            now && r.endsToday -> "${Fmt.duration(left)} left" to (if (urgent) Pal.red else Pal.amber)
            now && r.isNew -> "NEW \u00B7 until ${Fmt.day(r.endMs, zone)}" to Pal.green
            now -> "until ${Fmt.day(r.endMs, zone)}" to Pal.muted
            else -> "in ${Fmt.duration(r.startMs - s.nowMs)}" to Pal.cyan
        }
        Text(text, maxLines = 1, style = TextStyle(color = color, fontWeight = FontWeight.Bold, fontSize = 11.sp))
    }
}

/** Tapped from the widget's refresh button. */
class RefreshAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        Scheduler.refreshNow(context, fetch = true)
    }
}
