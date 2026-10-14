package com.lumi.stickerize.sticker

import android.content.Context
import android.graphics.Bitmap
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

data class StickerEntry(val fileName: String, val emojis: List<String>, val caption: String)

data class StickerPack(
    val id: String,
    val name: String,
    val publisher: String,
    val trayFile: String,
    val version: Int,
    val stickers: List<StickerEntry>,
)

class PackFullException(val pack: StickerPack, val free: Int) :
    Exception("Paket \"${pack.name}\" hanya muat $free stiker lagi (maksimal ${PackStore.MAX_PER_PACK} per paket).")

/**
 * Menyimpan paket stiker di penyimpanan internal aplikasi agar bisa dibaca WhatsApp.
 *
 * Satu kali pembuatan stiker = satu "sesi" (tag). File stiker sesi diberi awalan tag-nya,
 * jadi sesi yang sama bisa disimpan ulang (setelah diedit / dibuat ulang) tanpa dobel,
 * baik ke paket baru maupun ditambahkan ke paket yang sudah ada.
 */
object PackStore {

    const val TRAY_FILE = "tray.png"

    /** Batas WhatsApp: 3–30 stiker per paket. */
    const val MAX_PER_PACK = 30

    private val defaultEmojis = listOf("😀", "😂", "👍", "😍", "😮", "😭", "😠", "😴", "🤔", "🎉", "😊", "✨")

    private fun root(context: Context) = File(context.filesDir, "packs").apply { mkdirs() }

    fun packDir(context: Context, id: String): File = File(root(context), sanitize(id))

    private fun sanitize(id: String) = id.replace(Regex("[^A-Za-z0-9_.-]"), "_")

    /**
     * Simpan stiker satu sesi ke paket [id]. Kalau paket belum ada, dibuat baru dengan [newName].
     * Stiker sesi yang sama yang sudah ada di paket akan diganti (bukan ditambah lagi).
     */
    fun upsertSession(
        context: Context,
        id: String,
        newName: String?,
        publisher: String,
        tag: String,
        stickers: List<Bitmap>,
        captions: List<String>,
    ): StickerPack {
        val dir = packDir(context, id)
        val old = load(context, id)
        val prefix = "${tag}_"
        val kept = old?.stickers?.filterNot { it.fileName.startsWith(prefix) }.orEmpty()

        val free = MAX_PER_PACK - kept.size
        if (old != null && stickers.size > free) throw PackFullException(old, free)

        dir.mkdirs()
        // hapus file lama milik sesi ini; nama file baru selalu unik agar WhatsApp tidak memakai cache lama
        old?.stickers?.filter { it.fileName.startsWith(prefix) }?.forEach { File(dir, it.fileName).delete() }
        val stamp = System.currentTimeMillis().toString(36)
        val added = stickers.mapIndexed { i, bmp ->
            val file = "${prefix}${i + 1}_$stamp.webp"
            File(dir, file).writeBytes(StickerMaker.encodeWebp(bmp))
            StickerEntry(
                file,
                listOf(defaultEmojis[(kept.size + i) % defaultEmojis.size]),
                captions.getOrNull(i).orEmpty()
            )
        }
        // ikon tray: paket baru memakai stiker pertama; paket lama tetap memakai ikonnya
        if (old == null || !File(dir, TRAY_FILE).exists() || kept.isEmpty()) {
            File(dir, TRAY_FILE).writeBytes(StickerMaker.trayPng(stickers.first()))
        }

        val pack = StickerPack(
            id = id,
            name = (newName ?: old?.name ?: "Lumi Stickerize").take(128),
            publisher = (old?.publisher ?: publisher).take(128),
            trayFile = TRAY_FILE,
            version = (old?.version ?: 0) + 1,
            stickers = kept + added,
        )
        File(dir, "pack.json").writeText(toJson(pack).toString())
        return pack
    }

    /** Lepas stiker satu sesi dari paket. Paket yang jadi kosong dihapus. */
    fun removeSession(context: Context, id: String, tag: String) {
        val old = load(context, id) ?: return
        val dir = packDir(context, id)
        val prefix = "${tag}_"
        old.stickers.filter { it.fileName.startsWith(prefix) }.forEach { File(dir, it.fileName).delete() }
        val kept = old.stickers.filterNot { it.fileName.startsWith(prefix) }
        if (kept.isEmpty()) {
            dir.deleteRecursively()
            return
        }
        File(dir, "pack.json").writeText(toJson(old.copy(stickers = kept, version = old.version + 1)).toString())
    }

    fun load(context: Context, id: String): StickerPack? {
        val f = File(packDir(context, id), "pack.json")
        if (!f.exists()) return null
        return try {
            fromJson(JSONObject(f.readText()))
        } catch (e: Exception) {
            null
        }
    }

    fun all(context: Context): List<StickerPack> =
        root(context).listFiles()
            ?.filter { it.isDirectory }
            ?.mapNotNull { load(context, it.name) }
            ?.sortedByDescending { it.id }
            ?: emptyList()

    private fun toJson(p: StickerPack): JSONObject = JSONObject()
        .put("id", p.id)
        .put("name", p.name)
        .put("publisher", p.publisher)
        .put("tray", p.trayFile)
        .put("version", p.version)
        .put("stickers", JSONArray().apply {
            p.stickers.forEach { s ->
                put(
                    JSONObject()
                        .put("file", s.fileName)
                        .put("emojis", JSONArray(s.emojis))
                        .put("caption", s.caption)
                )
            }
        })

    private fun fromJson(o: JSONObject): StickerPack {
        val arr = o.getJSONArray("stickers")
        val list = (0 until arr.length()).map { i ->
            val s = arr.getJSONObject(i)
            val em = s.optJSONArray("emojis")
            StickerEntry(
                fileName = s.getString("file"),
                emojis = if (em == null) listOf("😀") else (0 until em.length()).map { em.getString(it) },
                caption = s.optString("caption"),
            )
        }
        return StickerPack(
            id = o.getString("id"),
            name = o.getString("name"),
            publisher = o.optString("publisher", "Lumi Stickerize"),
            trayFile = o.optString("tray", TRAY_FILE),
            version = o.optInt("version", 1),
            stickers = list,
        )
    }
}
