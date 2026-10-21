package com.lumi.stickerize.data

import android.content.Context
import com.lumi.stickerize.i18n.Lang
import org.json.JSONArray

/** Kata stiker siap pakai per bahasa (assets/words/<bahasa>.txt), untuk tombol 🎲. */
object WordBank {

    private val cache = HashMap<String, List<String>>()

    fun all(context: Context, lang: String = Lang.current(context)): List<String> = synchronized(cache) {
        cache.getOrPut(lang) {
            val file = "words/$lang.txt"
            val name = runCatching { context.assets.open(file).close(); file }.getOrDefault("words/en.txt")
            context.assets.open(name).bufferedReader().useLines { lines ->
                lines.map { it.trim() }
                    .filter { it.isNotEmpty() && !it.startsWith("#") && it.length <= 25 }
                    .distinct()
                    .toList()
            }
        }
    }

    /** Ambil satu kata acak yang belum dipakai di stiker lain. */
    fun random(context: Context, exclude: Collection<String> = emptyList()): String {
        val list = all(context)
        val used = exclude.map { it.trim().lowercase() }.toSet()
        val pool = list.filter { it.lowercase() !in used }
        return (pool.ifEmpty { list }).random()
    }
}

/** Satu stiker dalam paket tema: kata + pose. */
data class ThemeItem(val caption: String, val pose: String)

/** Paket tema siap pakai, mis. Lebaran, Bucin, Kantor. */
data class ThemePack(val id: String, val emoji: String, val name: String, val items: List<ThemeItem>)

/** Paket tema per bahasa dari assets/themes/<bahasa>.json (dibuat oleh tools/themes/build.py). */
object ThemePacks {

    private val cache = HashMap<String, List<ThemePack>>()

    fun all(context: Context, lang: String = Lang.current(context)): List<ThemePack> = synchronized(cache) {
        cache.getOrPut(lang) {
            val text = runCatching { context.assets.open("themes/$lang.json") }
                .getOrElse { context.assets.open("themes/en.json") }
                .bufferedReader().use { it.readText() }
            val arr = JSONArray(text)
            (0 until arr.length()).map { i ->
                val o = arr.getJSONObject(i)
                val items = o.getJSONArray("items")
                ThemePack(
                    id = o.getString("id"),
                    emoji = o.getString("emoji"),
                    name = o.getString("name"),
                    items = (0 until items.length()).map { j ->
                        val it = items.getJSONObject(j)
                        ThemeItem(it.getString("c"), it.optString("p"))
                    }
                )
            }
        }
    }
}
