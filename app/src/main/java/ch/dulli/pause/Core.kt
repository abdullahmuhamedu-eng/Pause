package ch.dulli.pause

import android.content.Context
import java.time.LocalDate
import java.time.LocalTime

data class Rule(
    val key: String,
    val pkgs: List<String>,
    val title: String,
    val sub: String,
    val ids: List<String>,   // View-IDs, die den Feed verraten
    val tab: String?,        // Name des Tabs (wenn ausgewählt -> Feed offen)
    val whole: Boolean       // ganze App sperren
)

object Rules {
    private const val IG = "com.instagram.android"
    private const val YT = "com.google.android.youtube"
    private const val SC = "com.snapchat.android"
    private val TT = listOf("com.zhiliaoapp.musically", "com.ss.android.ugc.trill")

    val all = listOf(
        Rule("ig_reels", listOf(IG), "Instagram Reels", "Chats & Feed bleiben offen", listOf("clips_viewer"), "Reels", false),
        Rule("yt_shorts", listOf(YT), "YouTube Shorts", "Normale Videos bleiben offen", listOf("reel_recycler", "reel_player_page"), "Shorts", false),
        Rule("sc_spotlight", listOf(SC), "Snapchat Spotlight", "Chats & Kamera bleiben offen", emptyList(), "Spotlight", false),
        Rule("tt_app", TT, "TikTok", "Komplett gesperrt", emptyList(), null, true),
        Rule("ig_app", listOf(IG), "Instagram", "Komplett gesperrt", emptyList(), null, true),
        Rule("yt_app", listOf(YT), "YouTube", "Komplett gesperrt", emptyList(), null, true),
        Rule("sc_app", listOf(SC), "Snapchat", "Komplett gesperrt", emptyList(), null, true),
    )
    val defaults = setOf("ig_reels", "yt_shorts", "sc_spotlight", "tt_app")
}

class Prefs(ctx: Context) {
    private val p = ctx.getSharedPreferences("pause", Context.MODE_PRIVATE)
    private fun ed() = p.edit()

    fun on(key: String) = p.getBoolean("r_$key", key in Rules.defaults)
    fun set(key: String, v: Boolean) = ed().putBoolean("r_$key", v).apply()

    /** 0 = 30 s warten, 1 = 60 s warten, 2 = Rechenaufgabe */
    var mode: Int
        get() = p.getInt("mode", 0)
        set(v) = ed().putInt("mode", v).apply()
    val waitSec get() = if (mode == 1) 60 else 30
    var maxMin: Int
        get() = p.getInt("maxMin", 10)
        set(v) = ed().putInt("maxMin", v).apply()
    var dailyLimit: Int
        get() = p.getInt("limit", 3)
        set(v) = ed().putInt("limit", v).apply()
    var night: Boolean
        get() = p.getBoolean("night", true)
        set(v) = ed().putBoolean("night", v).apply()

    fun isNight(): Boolean {
        if (!night) return false
        val h = LocalTime.now().hour
        return h >= 23 || h < 7
    }

    fun isUnlocked(key: String) = System.currentTimeMillis() < p.getLong("u_$key", 0)
    fun unlock(key: String, min: Int) {
        roll()
        ed().putLong("u_$key", System.currentTimeMillis() + min * 60_000L)
            .putInt("unlocks", p.getInt("unlocks", 0) + 1).apply()
    }

    private fun roll() {
        val d = LocalDate.now().toString()
        if (p.getString("day", "") != d) ed().putString("day", d).putInt("blocks", 0).putInt("unlocks", 0).apply()
    }
    val blocks: Int get() { roll(); return p.getInt("blocks", 0) }
    val unlocks: Int get() { roll(); return p.getInt("unlocks", 0) }
    fun addBlock() { roll(); ed().putInt("blocks", p.getInt("blocks", 0) + 1).apply() }
}
