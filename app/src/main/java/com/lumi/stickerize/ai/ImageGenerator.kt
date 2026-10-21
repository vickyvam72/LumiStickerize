package com.lumi.stickerize.ai

import android.graphics.Bitmap

/** Jenis masalah, supaya layar error bisa menawarkan tombol yang tepat (Pengaturan, isi saldo, coba lagi). */
enum class ErrorKind {
    /** API key / token belum diisi atau salah → buka Pengaturan. */
    AUTH,
    /** Saldo OpenRouter habis → isi saldo. */
    CREDIT,
    /** Kuota gratis harian habis → ganti model. */
    QUOTA,
    /** Model tidak ada / tidak mendukung permintaan → ganti model. */
    MODEL,
    /** Tidak ada internet / koneksi putus → coba lagi. */
    NETWORK,
    /** Server sibuk, terlalu banyak permintaan, atau terlalu lama → coba lagi. */
    BUSY,
    /** Ditolak filter keamanan → ganti foto / kata. */
    BLOCKED,
    OTHER,
}

class AiException(message: String, val kind: ErrorKind = ErrorKind.OTHER) : Exception(message)

/** Penyedia AI gambar (OpenRouter berbayar atau Cloudflare gratis). */
interface ImageGenerator {
    /** Jumlah foto referensi maksimal yang diterima model. */
    val maxReferences: Int

    suspend fun generateSheet(prompt: String, jpegPhotos: List<ByteArray>, aspectRatio: String): Bitmap
}
