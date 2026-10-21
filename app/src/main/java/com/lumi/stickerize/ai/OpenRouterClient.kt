package com.lumi.stickerize.ai

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import com.lumi.stickerize.R
import java.io.IOException
import java.io.InterruptedIOException
import java.net.UnknownHostException
import java.util.concurrent.TimeUnit

/**
 * Memanggil Image API OpenRouter (POST /api/v1/images) sekali saja per pembuatan:
 * 3 foto referensi + prompt  ->  1 lembar gambar berisi semua stiker.
 */
class OpenRouterClient(
    private val apiKey: String,
    private val model: String,
    /** Batas tiap model (null = model diisi manual, pakai pengaturan aman). */
    private val spec: com.lumi.stickerize.data.AiModel? = null,
    /** Context berbahasa aplikasi, untuk pesan error. */
    private val res: Context,
) : ImageGenerator {

    override val maxReferences: Int = spec?.maxRefs ?: 4

    /** Rasio paling dekat yang didukung model (AI tetap bebas menyusun grid; pemotong mendeteksinya). */
    private fun pickAspect(wanted: String): String {
        val supported = spec?.aspects ?: return wanted
        if (wanted in supported) return wanted
        fun ratio(a: String): Double {
            val p = a.split(":")
            return p[0].toDouble() / p[1].toDouble()
        }
        val target = ratio(wanted)
        return supported.minByOrNull { kotlin.math.abs(kotlin.math.ln(ratio(it) / target)) } ?: wanted
    }

    private val http = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .readTimeout(240, TimeUnit.SECONDS)
        .callTimeout(300, TimeUnit.SECONDS)
        .build()

    override suspend fun generateSheet(
        prompt: String,
        jpegPhotos: List<ByteArray>,
        aspectRatio: String,
    ): Bitmap = withContext(Dispatchers.IO) {
        val refs = JSONArray()
        jpegPhotos.forEach { bytes ->
            val dataUrl = "data:image/jpeg;base64," + Base64.encodeToString(bytes, Base64.NO_WRAP)
            refs.put(
                JSONObject()
                    .put("type", "image_url")
                    .put("image_url", JSONObject().put("url", dataUrl))
            )
        }

        val body = JSONObject()
            .put("model", model.trim())
            .put("prompt", prompt)
            .put("n", 1)
            .put("aspect_ratio", pickAspect(aspectRatio))
            .put("input_references", refs)
        // hanya kirim parameter yang memang didukung model itu
        val resolution = if (spec == null) "1K" else spec.resolution
        if (resolution != null) body.put("resolution", resolution)
        spec?.quality?.let { body.put("quality", it) }

        val request = Request.Builder()
            .url("https://openrouter.ai/api/v1/images")
            .header("Authorization", "Bearer ${apiKey.trim()}")
            .header("HTTP-Referer", "https://github.com/vickyvam72/LumiStickerize")
            .header("X-Title", "Lumi Stickerize")
            .post(body.toString().toRequestBody("application/json".toMediaType()))
            .build()

        val (code, text) = try {
            http.newCall(request).execute().use { resp ->
                Pair(resp.code, resp.body?.string().orEmpty())
            }
        } catch (e: UnknownHostException) {
            throw AiException(res.getString(R.string.err_no_internet), ErrorKind.NETWORK)
        } catch (e: InterruptedIOException) {
            throw AiException(res.getString(R.string.err_timeout), ErrorKind.BUSY)
        } catch (e: IOException) {
            throw AiException(res.getString(R.string.err_connection, e.message.orEmpty()), ErrorKind.NETWORK)
        }

        if (code !in 200..299) throw describeHttpError(code, text)

        val json = try {
            JSONObject(text)
        } catch (e: Exception) {
            throw AiException(res.getString(R.string.err_bad_reply), ErrorKind.BUSY)
        }

        json.optJSONObject("error")?.let { err ->
            throw AiException(res.getString(R.string.err_ai_failed, err.optString("message")), ErrorKind.BUSY)
        }

        val data = json.optJSONArray("data")
        if (data != null) {
            for (i in 0 until data.length()) {
                val item = data.optJSONObject(i) ?: continue
                val b64 = item.optString("b64_json")
                if (b64.isNotEmpty()) {
                    val bytes = Base64.decode(b64.substringAfter("base64,"), Base64.DEFAULT)
                    BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.let { return@withContext it }
                }
            }
        }
        throw AiException(res.getString(R.string.err_no_image), ErrorKind.BUSY)
    }

    private fun describeHttpError(code: Int, body: String): AiException {
        val apiMsg = try {
            JSONObject(body).optJSONObject("error")?.optString("message").orEmpty()
        } catch (e: Exception) {
            ""
        }
        val (friendly, kind) = when (code) {
            400 -> res.getString(R.string.err_or_400) to ErrorKind.MODEL
            401 -> res.getString(R.string.err_or_401) to ErrorKind.AUTH
            402 -> res.getString(R.string.err_or_402) to ErrorKind.CREDIT
            403 -> res.getString(R.string.err_or_403) to ErrorKind.BLOCKED
            404 -> res.getString(R.string.err_or_404, model) to ErrorKind.MODEL
            408 -> res.getString(R.string.err_timeout) to ErrorKind.BUSY
            429 -> res.getString(R.string.err_or_429) to ErrorKind.BUSY
            500, 502, 503, 504 -> res.getString(R.string.err_or_5xx) to ErrorKind.BUSY
            else -> res.getString(R.string.err_code, code) to ErrorKind.OTHER
        }
        val msg = if (apiMsg.isNotEmpty() && apiMsg != friendly) "$friendly\n\n$apiMsg" else friendly
        return AiException(msg, kind)
    }
}
