package com.lumi.stickerize.i18n

import android.content.Context
import android.content.res.Configuration
import androidx.annotation.StringRes
import java.util.Locale

/**
 * Bahasa aplikasi. Bawaan English; bisa diganti di layar pembuka atau ⚙️ Settings
 * tanpa mengikuti bahasa HP.
 */
object Lang {

    data class Option(val code: String, val label: String, val flag: String)

    const val DEFAULT = "en"

    val OPTIONS = listOf(
        Option("en", "English", "🇬🇧"),
        Option("id", "Bahasa Indonesia", "🇮🇩"),
        Option("ms", "Bahasa Melayu", "🇲🇾"),
        Option("es", "Español", "🇪🇸"),
        Option("pt", "Português", "🇧🇷"),
    )

    private fun prefs(context: Context) =
        context.getSharedPreferences("lumi_settings", Context.MODE_PRIVATE)

    fun current(context: Context): String =
        prefs(context).getString("language", DEFAULT)?.takeIf { c -> OPTIONS.any { it.code == c } } ?: DEFAULT

    fun set(context: Context, code: String) {
        prefs(context).edit().putString("language", code).commit()
    }

    fun option(code: String): Option = OPTIONS.firstOrNull { it.code == code } ?: OPTIONS.first()

    /** Kode "id" disimpan di Android sebagai "in" (folder values-in). */
    fun locale(code: String): Locale = when (code) {
        "id" -> Locale("in", "ID")
        "ms" -> Locale("ms", "MY")
        "es" -> Locale("es")
        "pt" -> Locale("pt", "BR")
        else -> Locale("en")
    }

    /** Context dengan bahasa aplikasi (untuk Activity, ViewModel, notifikasi, pesan error). */
    fun wrap(context: Context): Context {
        val loc = locale(current(context))
        val cfg = Configuration(context.resources.configuration)
        cfg.setLocale(loc)
        cfg.setLayoutDirection(loc)
        return context.createConfigurationContext(cfg)
    }

    fun str(context: Context, @StringRes id: Int, vararg args: Any): String =
        wrap(context).getString(id, *args)
}
