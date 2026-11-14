package com.turc.crossovers

import android.content.Context
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.time.Instant

/** Downloads the public event feed and keeps a copy so the app still opens offline. */
object Feed {
    const val URL_EVENTS =
        "https://raw.githubusercontent.com/ajone79/Phoenix-tracker/refs/heads/main/events-data-game.json"

    private val FORMAT_SUFFIX = Regex("\\s*-\\s*(SMS|ALB|SLB|AMS|RLB|XALB|IMS)\\s*$", RegexOption.IGNORE_CASE)

    fun parse(json: String, nowMs: Long): List<Ev> {
        val arr = JSONObject(json).optJSONArray("events") ?: return emptyList()
        val seen = HashSet<String>()
        val out = ArrayList<Ev>()
        for (i in 0 until arr.length()) {
            val o = arr.optJSONObject(i) ?: continue
            val start = runCatching { Instant.parse(o.optString("startUTC")).toEpochMilli() }.getOrNull() ?: continue
            val end = runCatching { Instant.parse(o.optString("endUTC")).toEpochMilli() }.getOrNull() ?: continue
            if (end < nowMs - 86_400_000L) continue
            val base = o.optString("title").replace(FORMAT_SUFFIX, "").trim()
            if (!seen.add("$base|$start|$end")) continue
            val accepts = Crossovers.accepts(base, o.optString("description"), o.optString("eventType"))
            if (accepts.isEmpty()) continue
            out.add(
                Ev(
                    title = base,
                    startMs = start,
                    endMs = end,
                    minOps = if (o.isNull("minOpsLevel")) null else o.optInt("minOpsLevel"),
                    maxOps = if (o.isNull("maxOpsLevel")) null else o.optInt("maxOpsLevel"),
                    accepts = accepts,
                ),
            )
        }
        return out
    }

    private fun cacheFile(ctx: Context) = File(ctx.filesDir, "events.json")

    /** Cached copy, or an empty list if there isn't one yet. */
    fun cached(ctx: Context, nowMs: Long): List<Ev> =
        runCatching { cacheFile(ctx).takeIf { it.exists() }?.let { parse(it.readText(), nowMs) } }.getOrNull() ?: emptyList()

    /** Fetches a fresh copy. Returns null on success, or a short error message. */
    fun refresh(ctx: Context): String? = try {
        val c = URL(URL_EVENTS).openConnection() as HttpURLConnection
        c.connectTimeout = 15_000
        c.readTimeout = 30_000
        c.setRequestProperty("User-Agent", "TurcCrossovers")
        if (c.responseCode != 200) {
            "Feed returned HTTP ${c.responseCode}"
        } else {
            val body = c.inputStream.bufferedReader().use { it.readText() }
            parse(body, System.currentTimeMillis()) // make sure it parses before replacing the cached copy
            cacheFile(ctx).writeText(body)
            null
        }
    } catch (e: Exception) {
        "Couldn't reach the feed (${e.message})"
    }
}
