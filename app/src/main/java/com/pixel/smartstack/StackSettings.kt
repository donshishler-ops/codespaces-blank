package com.pixel.smartstack

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

enum class Arrangement { STACKED, OFFSET, CENTERED, ANGLED }

data class CardDef(
    val id: String,
    val title: String,
    val body: String,
    val enabled: Boolean = true
)

object StackSettings {
    private const val PREFS = "smart_stack_prefs"
    private const val KEY_ARRANGEMENT = "arrangement"
    private const val KEY_LOOP = "loop"
    private const val KEY_CARDS = "cards_json"

    fun defaultCards(): List<CardDef> = listOf(
        CardDef("clock", "CLOCK", ""),
        CardDef("weather", "☀ WEATHER", "72°F • Sunny & Clear"),
        CardDef("notes", "📝 NOTES", "⚡ Buy coffee\n📦 Pick up package\n🎯 Gym at 6 PM")
    )

    fun getArrangement(ctx: Context): Arrangement {
        val prefs = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        return Arrangement.valueOf(prefs.getString(KEY_ARRANGEMENT, Arrangement.STACKED.name)!!)
    }

    fun setArrangement(ctx: Context, a: Arrangement) {
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString(KEY_ARRANGEMENT, a.name).apply()
    }

    fun getLoop(ctx: Context): Boolean =
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(KEY_LOOP, true)

    fun setLoop(ctx: Context, loop: Boolean) {
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putBoolean(KEY_LOOP, loop).apply()
    }

    fun getCards(ctx: Context): List<CardDef> {
        val prefs = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val raw = prefs.getString(KEY_CARDS, null) ?: return defaultCards()
        return try {
            val arr = JSONArray(raw)
            (0 until arr.length()).map {
                val o = arr.getJSONObject(it)
                CardDef(o.getString("id"), o.getString("title"), o.getString("body"), o.getBoolean("enabled"))
            }
        } catch (e: Exception) {
            defaultCards()
        }
    }

    fun saveCards(ctx: Context, cards: List<CardDef>) {
        val arr = JSONArray()
        cards.forEach {
            val o = JSONObject()
            o.put("id", it.id); o.put("title", it.title); o.put("body", it.body); o.put("enabled", it.enabled)
            arr.put(o)
        }
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString(KEY_CARDS, arr.toString()).apply()
    }
}
