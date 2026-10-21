package com.lumi.stickerize.ai

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import com.lumi.stickerize.R
import java.io.InterruptedIOException
import java.net.UnknownHostException
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * Cloudflare Workers AI — model FLUX.2 Klein. Gratis 10.000 neuron per hari
 * (cukup ±80 pembuatan/hari untuk Klein 4B, ±6 pembuatan/hari untuk Klein 9B).
 * Menerima maksimal 4 foto referensi, masing-masing harus lebih kecil dari 512×512.
 */
class CloudflareClient(
    private val accountId: String,
    private val apiToken: String,
    private val model: String,
    /** Context berbahasa aplikasi, untuk pesan error. */
    private val res: Context,
) : ImageGenerator {

    private val http = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .readTimeout(180, TimeUnit.SECONDS)
        .callTimeout(240, TimeUnit.SECONDS)
        .build()

    override val maxReferences: Int = 4

    override suspend fun generateSheet(
        prompt: String,
        jpegPhotos: List<ByteArray>,
        aspectRatio: String,
    ): Bitmap = withContext(Dispatchers.IO) {
        val (w, h) = when (aspectRatio) {
            "3:2" -> 1152 to 768
            "4:3" -> 1152 to 864
            else -> 1024 to 1024
        }
        val form = MultipartBody.Builder().setType(MultipartBody.FORM)
            .addFormDataPart("prompt", prompt)
            .addFormDataPart("width", w.toString())
            .addFormDataPart("height", h.toString())
        jpegPhotos.take(maxReferences).forEachIndexed { i, bytes ->
            val small = shrink(bytes)
            form.addFormDataPart(
                "input_image_$i", "ref$i.jpg",
                small.toRequestBody("image/jpeg".toMediaType())
            )
        }

        val url = "https://api.cloudflare.com/client/v4/accounts/${accountId.trim()}/ai/run/${model.trim()}"
        val request = Request.Builder()
            .url(url)
            .header("Authorization", "Bearer ${apiToken.trim()}")
            .post(form.build())
            .build()

        val result = try {
            http.newCall(request).execute().use { resp ->
                Triple(resp.code, resp.header("Content-Type").orEmpty(), resp.body?.bytes() ?: ByteArray(0))
            }
        } catch (e: UnknownHostException) {
            throw AiException(res.getString(R.string.err_no_internet), ErrorKind.NETWORK)
        } catch (e: InterruptedIOException) {
            throw AiException(res.getString(R.string.err_timeout), ErrorKind.BUSY)
        } catch (e: IOException) {
            throw AiException(res.getString(R.string.err_connection, e.message.orEmpty()), ErrorKind.NETWORK)
        }
        val (code, type, bytes) = result

        if (type.startsWith("image/") && code in 200..299) {
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.let { return@withContext it }
        }

        val text = String(bytes)
        if (code !in 200..299) throw describeError(code, text)
        val json = try {
            JSONObject(text)
        } catch (e: Exception) {
            throw AiException(res.getString(R.string.err_bad_reply), ErrorKind.BUSY)
        }
        val b64 = json.optJSONObject("result")?.optString("image").orEmpty()
            .ifEmpty { json.optString("image") }
        if (b64.isNotEmpty()) {
            val img = Base64.decode(b64.substringAfter("base64,"), Base64.DEFAULT)
            BitmapFactory.decodeByteArray(img, 0, img.size)?.let { return@withContext it }
        }
        throw AiException(res.getString(R.string.err_no_image) + firstError(text).let { if (it.isEmpty()) "" else "\n\n$it" }, ErrorKind.BUSY)
    }

    /** Foto referensi wajib < 512×512 untuk FLUX.2 Klein. */
    private fun shrink(jpeg: ByteArray): ByteArray {
        val src = BitmapFactory.decodeByteArray(jpeg, 0, jpeg.size) ?: return jpeg
        val max = 504f
        val scale = minOf(1f, max / maxOf(src.width, src.height))
        val bmp = if (scale < 1f) {
            Bitmap.createScaledBitmap(src, (src.width * scale).toInt(), (src.height * scale).toInt(), true)
        } else src
        val bos = ByteArrayOutputStream()
        bmp.compress(Bitmap.CompressFormat.JPEG, 90, bos)
        return bos.toByteArray()
    }

    private fun firstError(body: String): String = try {
        val errs = JSONObject(body).optJSONArray("errors")
        if (errs != null && errs.length() > 0) errs.getJSONObject(0).optString("message") else ""
    } catch (e: Exception) {
        ""
    }

    private fun describeError(code: Int, body: String): AiException {
        val msg = firstError(body)
        val (friendly, kind) = when {
            code == 401 || code == 403 -> res.getString(R.string.err_cf_auth) to ErrorKind.AUTH
            code == 404 -> res.getString(R.string.err_cf_404, model) to ErrorKind.MODEL
            code == 429 || msg.contains("neuron", true) || msg.contains("limit", true) ->
                res.getString(R.string.err_cf_quota) to ErrorKind.QUOTA
            code >= 500 -> res.getString(R.string.err_cf_busy) to ErrorKind.BUSY
            else -> res.getString(R.string.err_code, code) to ErrorKind.OTHER
        }
        val text = if (msg.isNotEmpty() && !friendly.contains(msg)) "$friendly\n\n$msg" else friendly
        return AiException(text, kind)
    }
}
