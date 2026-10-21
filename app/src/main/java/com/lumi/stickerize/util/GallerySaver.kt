package com.lumi.stickerize.util

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.media.MediaScannerConnection
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import java.io.File
import java.io.FileOutputStream

/**
 * Simpan stiker ke Galeri sebagai PNG transparan di folder "Pictures/Lumi Stickerize",
 * supaya bisa dipakai di aplikasi lain (Instagram Story, Telegram, dll.).
 */
object GallerySaver {

    const val FOLDER = "Lumi Stickerize"

    /** Android 9 ke bawah butuh izin tulis penyimpanan; Android 10+ tidak. */
    val needsPermission: Boolean get() = Build.VERSION.SDK_INT < Build.VERSION_CODES.Q

    /** @return jumlah gambar yang berhasil disimpan */
    fun save(context: Context, bitmaps: List<Bitmap>, baseName: String): Int {
        val stamp = System.currentTimeMillis()
        var ok = 0
        bitmaps.forEachIndexed { i, bmp ->
            val name = "${baseName}_${stamp}_${i + 1}.png"
            if (runCatching { saveOne(context, bmp, name) }.getOrDefault(false)) ok++
        }
        return ok
    }

    private fun saveOne(context: Context, bmp: Bitmap, name: String): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val resolver = context.contentResolver
            val values = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, name)
                put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/$FOLDER")
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
            val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values) ?: return false
            val written = resolver.openOutputStream(uri)?.use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) } ?: false
            values.clear()
            values.put(MediaStore.Images.Media.IS_PENDING, 0)
            resolver.update(uri, values, null, null)
            if (!written) resolver.delete(uri, null, null)
            return written
        }
        @Suppress("DEPRECATION")
        val dir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), FOLDER)
        dir.mkdirs()
        val file = File(dir, name)
        val written = FileOutputStream(file).use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
        MediaScannerConnection.scanFile(context, arrayOf(file.absolutePath), arrayOf("image/png"), null)
        return written
    }
}
