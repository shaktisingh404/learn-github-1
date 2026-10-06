package com.brainpal.counter

import android.content.Context
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class Source(val key: String, val label: String) {
    INSTAGRAM("ig", "Reels"),
    YOUTUBE("yt", "Shorts"),
}

/** Per-day counters kept in SharedPreferences; a new day starts from zero. */
class Store(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("counts", Context.MODE_PRIVATE)

    private fun today() = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

    fun get(source: Source): Int = prefs.getInt("${today()}_${source.key}", 0)

    fun increment(source: Source): Int {
        val next = get(source) + 1
        prefs.edit().putInt("${today()}_${source.key}", next).apply()
        return next
    }

    fun resetToday() {
        prefs.edit().apply { Source.values().forEach { remove("${today()}_${it.key}") } }.apply()
    }

    var notchEnabled: Boolean
        get() = prefs.getBoolean("notch", true)
        set(v) = prefs.edit().putBoolean("notch", v).apply()
}
