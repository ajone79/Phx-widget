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
            .apply()
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
