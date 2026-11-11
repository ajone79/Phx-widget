package com.phoenixeu168.widget.ui

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.phoenixeu168.widget.data.Fmt
import com.phoenixeu168.widget.data.Prefs
import com.phoenixeu168.widget.data.Store
import com.phoenixeu168.widget.work.Scheduler
import java.time.ZoneId

/** Opens from the app icon, from the widget's gear, and as the widget's configure screen. */
class SettingsActivity : ComponentActivity() {
    private lateinit var store: Store

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        store = Store(this)
        // Adding the widget never fails if you just back out: the defaults (or your saved settings) apply.
        val id = intent?.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
            ?: AppWidgetManager.INVALID_APPWIDGET_ID
        if (id != AppWidgetManager.INVALID_APPWIDGET_ID) {
            setResult(RESULT_OK, Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id))
        }
        setContent {
            PhxTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    SettingsScreen(store, onDone = { finish() })
                }
            }
        }
    }

    override fun onStop() {
        super.onStop()
        // Settings are saved as you change them; redraw the widget with them.
        Scheduler.start(this)
        Scheduler.refreshNow(this, fetch = false)
    }
}

@Composable
private fun SettingsScreen(store: Store, onDone: () -> Unit) {
    var p by remember { mutableStateOf(store.prefs()) }
    var opsText by remember { mutableStateOf(p.ops.toString()) }
    fun update(n: Prefs) { p = n; store.save(n) }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text("Phoenix EU168 widget", style = MaterialTheme.typography.headlineSmall)
        Hint("Build ${Store.buildNumber(androidx.compose.ui.platform.LocalContext.current)}")

        Section("Your ops level")
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedButton(onClick = { val v = (p.ops - 1).coerceAtLeast(1); opsText = v.toString(); update(p.copy(ops = v)) }) { Text("−") }
            Spacer(Modifier.width(10.dp))
            OutlinedTextField(
                value = opsText,
                onValueChange = { t ->
                    val digits = t.filter { it.isDigit() }.take(2)
                    opsText = digits
                    digits.toIntOrNull()?.let { update(p.copy(ops = it.coerceIn(1, 80))) }
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.width(96.dp),
            )
            Spacer(Modifier.width(10.dp))
            OutlinedButton(onClick = { val v = (p.ops + 1).coerceAtMost(80); opsText = v.toString(); update(p.copy(ops = v)) }) { Text("+") }
        }
        Hint("Only events your ops level can take part in are shown.")

        Section("Raid territories")
        ToggleRow("Show raid timers (at war)", "Adds the enemy raid territory times to the widget", p.showRaids) { update(p.copy(showRaids = it)) }

        Section("Filters")
        ToggleRow("Hide passes, banners and recruit events", null, p.hideMinor) { update(p.copy(hideMinor = it)) }
        ToggleRow("Show long events (over 7 days)", "Arc-long missions and similar", p.includeLong) { update(p.copy(includeLong = it)) }

        Section("Incursion region")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("eu" to "EU", "us" to "US", "apac" to "APAC").forEach { (key, label) ->
                FilterChip(
                    selected = key in p.regions,
                    onClick = {
                        val next = if (key in p.regions) p.regions - key else p.regions + key
                        if (next.isNotEmpty()) update(p.copy(regions = next))
                    },
                    label = { Text(label) },
                )
            }
        }

        Section("Rows on the widget")
        Text("Now", style = MaterialTheme.typography.labelLarge)
        Chips(listOf(4, 6, 8, 10, 15), p.maxNow, { "$it" }) { update(p.copy(maxNow = it)) }
        Text("Next", style = MaterialTheme.typography.labelLarge)
        Chips(listOf(2, 4, 6, 8, 10), p.maxNext, { "$it" }) { update(p.copy(maxNext = it)) }

        Section("Countdown refresh")
        Chips(listOf(1, 5, 15), p.refreshMinutes, { "$it min" }) { update(p.copy(refreshMinutes = it)) }
        Hint("How often the widget redraws its countdowns. 1 minute uses a little more battery.")

        Section("Time travel (demo)")
        Chips(listOf(0, 1, 3, 7, 14), p.timeTravelDays, { if (it == 0) "Off" else "-${it}d" }) { update(p.copy(timeTravelDays = it)) }
        Hint("Shows the widget as it would have looked that many days ago, so you can see real events moving through Now and Next.")

        Section("Data")
        val last = store.lastFetchMs
        Text(
            if (last == 0L) "Not downloaded yet" else "Events last downloaded ${Fmt.dayClock(last, ZoneId.systemDefault())}",
            style = MaterialTheme.typography.bodyMedium,
        )
        if (store.lastError.isNotEmpty()) Text("Last problem: ${store.lastError}", color = MaterialTheme.colorScheme.error)
        val ctx = androidx.compose.ui.platform.LocalContext.current
        OutlinedButton(onClick = { Scheduler.start(ctx); Scheduler.refreshNow(ctx, fetch = true) }) { Text("Download events now") }
        Hint("Game events refresh every 4 hours; territory times are live. Reset (16:00 / 17:00) is treated as one daily time.")

        Spacer(Modifier.height(4.dp))
        Button(onClick = onDone, modifier = Modifier.fillMaxWidth()) { Text("Done") }
    }
}

@Composable
private fun Section(text: String) {
    Text(text, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
}

@Composable
private fun Hint(text: String) {
    Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun ToggleRow(title: String, sub: String?, value: Boolean, onChange: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            if (sub != null) Hint(sub)
        }
        Switch(checked = value, onCheckedChange = onChange)
    }
}

@Composable
private fun Chips(options: List<Int>, selected: Int, label: (Int) -> String, onPick: (Int) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { o -> FilterChip(selected = o == selected, onClick = { onPick(o) }, label = { Text(label(o)) }) }
    }
}
