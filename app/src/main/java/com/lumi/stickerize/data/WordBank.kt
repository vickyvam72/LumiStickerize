package com.lumi.stickerize.data

import android.content.Context
import org.json.JSONArray

/** 1.600+ pilihan kata stiker dari assets/kata_stiker.txt, untuk tombol 🎲. */
object WordBank {

    @Volatile
    private var words: List<String>? = null

    fun all(context: Context): List<String> = words ?: synchronized(this) {
        words ?: context.assets.open("kata_stiker.txt").bufferedReader().useLines { lines ->
            lines.map { it.trim() }
                .filter { it.isNotEmpty() && !it.startsWith("#") && it.length <= 25 }
                .distinct()
                .toList()
        }.also { words = it }
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

/** Paket tema dari assets/tema.json (dibuat oleh tools/themes/build.py). */
object ThemePacks {

    @Volatile
    private var packs: List<ThemePack>? = null

    fun all(context: Context): List<ThemePack> = packs ?: synchronized(this) {
        packs ?: run {
            val text = context.assets.open("tema.json").bufferedReader().use { it.readText() }
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
        }.also { packs = it }
    }
}
