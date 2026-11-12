package com.phoenixeu168.widget.data

import android.content.Context

/** Settings, kept in SharedPreferences. */
class Store(context: Context) {
    private val sp = context.applicationContext.getSharedPreferences("phx_widget", Context.MODE_PRIVATE)

    fun prefs(): Prefs {
        val d = Prefs()
        return Prefs(
            ops = sp.getInt("ops", d.ops).coerceIn(1, 80),
            maxNow = sp.getInt("maxNow", d.maxNow),
            maxNext = sp.getInt("maxNext", d.maxNext),
            includeLong = sp.getBoolean("includeLong", d.includeLong),
            hideMinor = sp.getBoolean("hideMinor", d.hideMinor),
            regions = sp.getStringSet("regions", d.regions)?.toSet() ?: d.regions,
            refreshMinutes = sp.getInt("refreshMinutes", d.refreshMinutes),
            showRaids = sp.getBoolean("showRaids", d.showRaids),
            timeTravelDays = sp.getInt("timeTravelDays", d.timeTravelDays),
            hiddenCats = sp.getStringSet("hiddenCats", d.hiddenCats)?.toSet() ?: d.hiddenCats,
        )
    }

    fun save(p: Prefs) {
        sp.edit()
            .putInt("ops", p.ops)
            .putInt("maxNow", p.maxNow)
            .putInt("maxNext", p.maxNext)
            .putBoolean("includeLong", p.includeLong)
            .putBoolean("hideMinor", p.hideMinor)
            .putStringSet("regions", p.regions)
            .putInt("refreshMinutes", p.refreshMinutes)
            .putBoolean("showRaids", p.showRaids)
            .putInt("timeTravelDays", p.timeTravelDays)
            .putStringSet("hiddenCats", p.hiddenCats)
            .apply()
    }

    companion object {
        /** "3" for "build 3" (set by the GitHub run number), or "dev" for local builds. */
        fun buildNumber(context: Context): String = try {
            val v = context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: ""
            v.removePrefix("build ").ifBlank { "dev" }
        } catch (e: Exception) {
            "dev"
        }
    }

    var lastFetchMs: Long
        get() = sp.getLong("lastFetchMs", 0L)
        set(v) { sp.edit().putLong("lastFetchMs", v).apply() }

    var lastError: String
        get() = sp.getString("lastError", "") ?: ""
        set(v) { sp.edit().putString("lastError", v).apply() }

    var eventsEtag: String
        get() = sp.getString("eventsEtag", "") ?: ""
        set(v) { sp.edit().putString("eventsEtag", v).apply() }
}
