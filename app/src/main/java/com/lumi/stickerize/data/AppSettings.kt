package com.lumi.stickerize.data

import android.content.Context
import androidx.annotation.StringRes
import com.lumi.stickerize.R
import com.lumi.stickerize.ai.CloudflareClient
import com.lumi.stickerize.ai.ImageGenerator
import com.lumi.stickerize.ai.OpenRouterClient
import com.lumi.stickerize.i18n.Lang

enum class Provider { OPENROUTER, CLOUDFLARE }

/** Pilihan model gambar yang sudah dicek cocok untuk stiker. */
data class AiModel(
    val id: String,
    val label: String,
    @StringRes val note: Int,
    val provider: Provider,
    val free: Boolean = false,
    /** Resolusi yang dikirim ke OpenRouter (null = jangan kirim, model memakai bawaannya). */
    val resolution: String? = "1K",
    /** Rasio gambar yang didukung (null = semua rasio umum). */
    val aspects: List<String>? = null,
    /** Kualitas (khusus model yang punya pilihan ini). */
    val quality: String? = null,
    /** Jumlah foto referensi maksimal yang diterima model. */
    val maxRefs: Int = 10,
)

/** Pengaturan sederhana yang disimpan di HP (key hanya dikirim ke penyedia AI-nya). */
class AppSettings(context: Context) {
    private val appContext = context.applicationContext
    private val prefs = context.getSharedPreferences("lumi_settings", Context.MODE_PRIVATE)

    var apiKey: String
        get() = prefs.getString("openrouter_key", "").orEmpty()
        set(v) = prefs.edit().putString("openrouter_key", v.trim()).apply()

    var cfAccountId: String
        get() = prefs.getString("cf_account", "").orEmpty()
        set(v) = prefs.edit().putString("cf_account", v.trim()).apply()

    var cfToken: String
        get() = prefs.getString("cf_token", "").orEmpty()
        set(v) = prefs.edit().putString("cf_token", v.trim()).apply()

    var model: String
        get() = prefs.getString("openrouter_model", DEFAULT_MODEL).orEmpty().ifBlank { DEFAULT_MODEL }
        set(v) = prefs.edit().putString("openrouter_model", v.trim()).apply()

    var publisher: String
        get() = prefs.getString("publisher", "Lumi Stickerize").orEmpty().ifBlank { "Lumi Stickerize" }
        set(v) = prefs.edit().putString("publisher", v.trim()).apply()

    /** Layar pembuka (kenalan, API key, izin) sudah dilewati. */
    var onboardingDone: Boolean
        get() = prefs.getBoolean("onboarding_done", false)
        set(v) = prefs.edit().putBoolean("onboarding_done", v).apply()

    val hasOpenRouter: Boolean get() = apiKey.isNotBlank()
    val hasCloudflare: Boolean get() = cfAccountId.isNotBlank() && cfToken.isNotBlank()

    val provider: Provider get() = providerOf(model)

    /** Kunci untuk penyedia yang dipakai sudah diisi? */
    val hasCredentials: Boolean
        get() = when (provider) {
            Provider.OPENROUTER -> apiKey.isNotBlank()
            Provider.CLOUDFLARE -> cfAccountId.isNotBlank() && cfToken.isNotBlank()
        }

    fun generator(): ImageGenerator = when (provider) {
        Provider.OPENROUTER -> OpenRouterClient(apiKey, model, MODELS.firstOrNull { it.id == model.trim() }, Lang.wrap(appContext))
        Provider.CLOUDFLARE -> CloudflareClient(cfAccountId, cfToken, model, Lang.wrap(appContext))
    }

    companion object {
        const val DEFAULT_MODEL = "google/gemini-3.1-flash-lite-image"
        const val API_KEY_URL = "https://openrouter.ai/keys"
        const val CREDITS_URL = "https://openrouter.ai/settings/credits"
        const val CF_TOKEN_URL = "https://dash.cloudflare.com/profile/api-tokens"

        /** Model Cloudflare diawali "@cf/", selain itu dianggap model OpenRouter. */
        fun providerOf(model: String): Provider =
            if (model.trim().startsWith("@cf/")) Provider.CLOUDFLARE else Provider.OPENROUTER

        val MODELS = listOf(
            AiModel(
                DEFAULT_MODEL,
                "Nano Banana 2 Lite",
                R.string.model_note_nb2lite,
                Provider.OPENROUTER,
                maxRefs = 14
            ),
            AiModel(
                "openai/gpt-image-1-mini",
                "GPT Image 1 Mini",
                R.string.model_note_gptmini,
                Provider.OPENROUTER,
                resolution = null,
                aspects = listOf("1:1", "3:2", "2:3"),
                quality = "medium",
                maxRefs = 16
            ),
            AiModel(
                "black-forest-labs/flux.2-klein-4b",
                "FLUX.2 Klein 4B",
                R.string.model_note_klein4b_or,
                Provider.OPENROUTER,
                resolution = null,
                aspects = listOf("1:1", "4:3", "3:4", "3:2", "2:3", "16:9", "9:16", "21:9"),
                maxRefs = 4
            ),
            AiModel(
                "bytedance-seed/seedream-5-0-flash",
                "Seedream 5.0 Flash",
                R.string.model_note_seedream_flash,
                Provider.OPENROUTER,
                maxRefs = 14
            ),
            AiModel(
                "google/gemini-nano-banana-2.1",
                "Nano Banana 2.1",
                R.string.model_note_nb21,
                Provider.OPENROUTER,
                maxRefs = 14
            ),
            AiModel(
                "bytedance-seed/seedream-5-0-lite",
                "Seedream 5.0 Lite",
                R.string.model_note_seedream_lite,
                Provider.OPENROUTER,
                resolution = "2K",
                maxRefs = 14
            ),
            AiModel(
                "qwen/qwen-image-3",
                "Qwen Image 3",
                R.string.model_note_qwen,
                Provider.OPENROUTER,
                aspects = listOf("1:1", "2:3", "3:2", "3:4", "4:3", "4:5", "5:4", "9:16", "16:9"),
                maxRefs = 4
            ),
            AiModel(
                "google/gemini-3.1-flash-image",
                "Nano Banana 2",
                R.string.model_note_nb2,
                Provider.OPENROUTER,
                maxRefs = 14
            ),
            AiModel(
                "@cf/black-forest-labs/flux-2-klein-9b",
                "FLUX.2 Klein 9B",
                R.string.model_note_cf9b,
                Provider.CLOUDFLARE,
                free = true
            ),
            AiModel(
                "@cf/black-forest-labs/flux-2-klein-4b",
                "FLUX.2 Klein 4B",
                R.string.model_note_cf4b,
                Provider.CLOUDFLARE,
                free = true
            ),
        )
    }
}
