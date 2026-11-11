package com.phoenixeu168.widget.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.time.Instant

/** Downloads the public event data and keeps a copy on the phone so the widget works offline. */
class Repo(context: Context) {
    private val ctx = context.applicationContext
    private val store = Store(ctx)
    private val eventsFile = File(ctx.filesDir, "events.json")
    private val territoryFile = File(ctx.filesDir, "territory.json")

    companion object {
        const val EVENTS_URL =
            "https://raw.githubusercontent.com/ajone79/Phoenix-tracker/refs/heads/main/events-data-game.json"
        const val SB_URL = "https://mmzizgsanwqjpiumpqay.supabase.co"
        // Public anon key (the territory table is public read-only, same key the calendar feed uses).
        const val SB_ANON =
            "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6Im1teml6Z3NhbndxanBpdW1wcWF5Iiwicm9sZSI6ImFub24iLCJpYXQiOjE3ODYwMjk5MzksImV4cCI6MjEwMTYwNTkzOX0.KqvY2Ib33J8h8ztEi8qxtfutSdVIPAaJRtj7cSUSKFM"
        const val STALE_MS = 25 * 60_000L

        fun parseEvents(json: String, nowMs: Long): List<GameEvent> {
            val arr = JSONObject(json).optJSONArray("events") ?: JSONArray()
            val keepAfter = nowMs - 3 * Engine.DAY_MS
            val out = ArrayList<GameEvent>()
            for (i in 0 until arr.length()) {
                val o = arr.optJSONObject(i) ?: continue
                val start = runCatching { Instant.parse(o.optString("startUTC")).toEpochMilli() }.getOrNull() ?: continue
                val end = runCatching { Instant.parse(o.optString("endUTC")).toEpochMilli() }.getOrNull() ?: continue
                if (end < keepAfter) continue
                out.add(
                    GameEvent(
                        id = o.optLong("id"),
                        title = o.optString("title"),
                        type = o.optString("eventType"),
                        format = if (o.isNull("eventFormat")) null else o.optString("eventFormat"),
                        priority = o.optString("priority"),
                        minOps = if (o.isNull("minOpsLevel")) null else o.optInt("minOpsLevel"),
                        maxOps = if (o.isNull("maxOpsLevel")) null else o.optInt("maxOpsLevel"),
                        startMs = start,
                        endMs = end,
                    ),
                )
            }
            return out
        }

        fun parseTerritory(json: String): List<TerritoryRow> {
            val arr = JSONArray(json)
            val out = ArrayList<TerritoryRow>()
            for (i in 0 until arr.length()) {
                val o = arr.optJSONObject(i) ?: continue
                out.add(TerritoryRow(o.optString("system"), o.optInt("weekday"), o.optString("start_utc"), o.optInt("duration_min", 30)))
            }
            return out
        }
    }

    fun events(now: Long = System.currentTimeMillis()): List<GameEvent> =
        runCatching { if (eventsFile.exists()) parseEvents(eventsFile.readText(), now) else emptyList() }.getOrDefault(emptyList())

    fun territory(): List<TerritoryRow> =
        runCatching { if (territoryFile.exists()) parseTerritory(territoryFile.readText()) else emptyList() }.getOrDefault(emptyList())

    fun hasData() = eventsFile.exists()

    fun isStale(now: Long = System.currentTimeMillis()) = !hasData() || now - store.lastFetchMs > STALE_MS

    /** Downloads both sources. Failures keep the old copy and are remembered in [Store.lastError]. */
    fun refresh(): Boolean {
        val errors = ArrayList<String>()
        try {
            val c = open(EVENTS_URL)
            val etag = store.eventsEtag
            if (etag.isNotEmpty() && eventsFile.exists()) c.setRequestProperty("If-None-Match", etag)
            when (c.responseCode) {
                200 -> {
                    val body = c.inputStream.bufferedReader().use { it.readText() }
                    parseEvents(body, System.currentTimeMillis()) // make sure it parses before replacing the copy
                    eventsFile.writeText(body)
                    store.eventsEtag = c.getHeaderField("ETag") ?: ""
                }
                304 -> Unit
                else -> errors.add("events HTTP ${c.responseCode}")
            }
        } catch (e: Exception) {
            errors.add("events: ${e.message}")
        }
        try {
            val c = open("$SB_URL/rest/v1/phx_territory_schedule?select=system,weekday,start_utc,duration_min")
            c.setRequestProperty("apikey", SB_ANON)
            c.setRequestProperty("Authorization", "Bearer $SB_ANON")
            if (c.responseCode == 200) {
                val body = c.inputStream.bufferedReader().use { it.readText() }
                parseTerritory(body)
                territoryFile.writeText(body)
            } else {
                errors.add("territory HTTP ${c.responseCode}")
            }
        } catch (e: Exception) {
            errors.add("territory: ${e.message}")
        }
        store.lastError = errors.joinToString("; ")
        if (errors.isEmpty()) store.lastFetchMs = System.currentTimeMillis()
        return errors.isEmpty()
    }

    private fun open(url: String): HttpURLConnection {
        val c = URL(url).openConnection() as HttpURLConnection
        c.connectTimeout = 15_000
        c.readTimeout = 30_000
        c.setRequestProperty("User-Agent", "PhxWidget")
        return c
    }
}
