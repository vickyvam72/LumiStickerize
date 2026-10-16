package com.lumi.stickerize.ui

import android.app.Application
import android.graphics.Bitmap
import android.graphics.Rect
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.lumi.stickerize.ai.PromptBuilder
import com.lumi.stickerize.data.AppSettings
import com.lumi.stickerize.data.WordBank
import com.lumi.stickerize.model.GridSpec
import com.lumi.stickerize.model.StickerStyle
import com.lumi.stickerize.sticker.PackFullException
import com.lumi.stickerize.sticker.PackStore
import com.lumi.stickerize.sticker.StickerPack
import com.lumi.stickerize.sticker.StickerMaker
import com.lumi.stickerize.util.FaceFinder
import com.lumi.stickerize.util.ImageUtils
import com.lumi.stickerize.work.GenerationService
import com.lumi.stickerize.work.Notifier
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class Step(val title: String, val subtitle: String) {
    PHOTOS("Pilih foto", "1 foto sudah cukup, 2–3 foto hasilnya lebih akurat"),
    FACES("Pilih wajah", "Ketuk wajah di foto pertama yang mau dijadikan stiker"),
    COUNT("Jumlah stiker", "Mau dibuatkan berapa stiker?"),
    STYLE("Pilih tema", "Gaya gambar stikermu"),
    TEXT("Tulisan", "Pakai kata di stiker atau tidak?"),
    GENERATE("Siap dibuat", "Periksa lagi sebelum AI bekerja"),
    RESULT("Stiker kamu", "Tambahkan langsung ke WhatsApp"),
}

/** Satu foto referensi beserta hasil deteksi wajahnya. */
class PhotoInfo(val bitmap: Bitmap, val image: ImageBitmap, val faces: List<Rect>)

class MainViewModel(app: Application) : AndroidViewModel(app) {

    val settings = AppSettings(app)

    var step by mutableStateOf(Step.PHOTOS)
        private set

    val photoUris = mutableStateListOf<Uri?>(null, null, null)
    val photos = mutableStateListOf<PhotoInfo?>(null, null, null)
    /** Indeks wajah yang dipilih di foto pertama (maks 2). Deteksi wajah hanya di foto 1. */
    var selectedFaces by mutableStateOf<Set<Int>>(emptySet())
        private set

    var count by mutableIntStateOf(5)
    var style by mutableStateOf<StickerStyle?>(null)
    var useText by mutableStateOf(false)
    val words = mutableStateListOf<String>().apply { repeat(MAX_STICKERS) { add("") } }
    /** Pose opsional per kata (kosong = AI memilih pose sesuai arti kata). */
    val poses = mutableStateListOf<String>().apply { repeat(MAX_STICKERS) { add("") } }

    var generating by mutableStateOf(false)
        private set
    var error by mutableStateOf<String?>(null)
    var notice by mutableStateOf<String?>(null)

    /** Layar penuh di atas halaman hasil: stiker yang sedang dirapikan & tampilan gambar asli AI. */
    var editingIndex by mutableStateOf<Int?>(null)
    var viewingSheet by mutableStateOf(false)

    var sheet by mutableStateOf<ImageBitmap?>(null)
        private set
    /** Bitmap asli tiap stiker (untuk diedit dengan penghapus). */
    var stickerBitmaps by mutableStateOf<List<Bitmap>>(emptyList())
        private set
    var stickers by mutableStateOf<List<ImageBitmap>>(emptyList())
        private set
    /** Kata yang tergambar di tiap stiker (kosong = tanpa kata). */
    var stickerCaptions by mutableStateOf<List<String>>(emptyList())
    var stickerPoses by mutableStateOf<List<String>>(emptyList())
        private set

    /** Penanda sesi pembuatan saat ini (dipakai sebagai awalan nama file di paket). */
    private var sessionTag = ""
    /** Id paket baru yang dibuat dari sesi ini (kalau user memilih "Paket baru"). */
    private var newPackId: String? = null
    /** Paket tempat stiker sesi ini sudah disimpan. */
    var committedPackId by mutableStateOf<String?>(null)
        private set
    /** null = buat paket baru, selain itu = id paket yang sudah ada. */
    var targetPackId by mutableStateOf<String?>(null)
    var existingPacks by mutableStateOf<List<StickerPack>>(emptyList())
        private set
    var packName by mutableStateOf("")

    /** Mode centang untuk mengganti sebagian stiker. */
    var selecting by mutableStateOf(false)
        private set
    var selection by mutableStateOf<Set<Int>>(emptySet())
        private set
    /** Jumlah stiker yang sedang dibuat ulang (0 = tidak sedang). */
    var regenerating by mutableIntStateOf(0)
        private set

    private var job: Job? = null

    /** Foto utama (foto 1) wajib, foto 2 & 3 opsional. Semua foto yang dipilih harus sudah selesai dimuat. */
    val photosReady: Boolean
        get() = photos[0] != null && photoUris.indices.all { photoUris[it] == null || photos[it] != null }

    /** Jumlah foto yang dipilih (1–3). */
    val photoCount: Int get() = photos.count { it != null }

    /** Foto pertama punya minimal 1 wajah terpilih. */
    val facesReady: Boolean
        get() = (photos[0]?.faces?.isNotEmpty() == true) && selectedFaces.isNotEmpty()

    /** Jumlah orang di stiker (1 atau 2). */
    val peopleCount: Int get() = selectedFaces.size.coerceIn(1, MAX_FACES)
    val activeWords: List<String> get() = words.take(count)
    val wordsReady: Boolean get() = !useText || activeWords.all { it.isNotBlank() }


    fun canGoNext(): Boolean = when (step) {
        Step.PHOTOS -> photosReady
        Step.FACES -> facesReady
        Step.COUNT -> count in MIN_STICKERS..MAX_STICKERS
        Step.STYLE -> style != null
        Step.TEXT -> wordsReady
        else -> false
    }

    fun next() {
        if (!canGoNext()) return
        step = Step.entries[step.ordinal + 1]
    }

    fun back() {
        if (generating) return
        if (step.ordinal > 0 && step != Step.RESULT) step = Step.entries[step.ordinal - 1]
        else if (step == Step.RESULT) step = Step.GENERATE
    }

    fun goTo(target: Step) {
        if (!generating) step = target
    }

    fun setPhoto(index: Int, uri: Uri) {
        photoUris[index] = uri
        photos[index] = null
        if (index == 0) selectedFaces = emptySet()
        viewModelScope.launch {
            val bmp = withContext(Dispatchers.IO) {
                runCatching { ImageUtils.loadScaled(getApplication(), uri, 1024) }.getOrNull()
            }
            if (photoUris[index] != uri) return@launch
            if (bmp == null) {
                photoUris[index] = null
                error = "Foto tidak bisa dibuka. Coba pilih foto lain."
                return@launch
            }
            // Wajah dideteksi di semua foto, supaya kalau foto utama dihapus,
            // foto berikutnya bisa langsung naik jadi foto utama. Pemilihan wajah tetap di foto 1.
            val faces = FaceFinder.detect(bmp)
            if (photoUris[index] != uri) return@launch
            photos[index] = PhotoInfo(bmp, bmp.asImageBitmap(), faces)
            // 1 wajah = otomatis terpilih; lebih dari 1 = user yang memilih
            if (index == 0) selectedFaces = if (faces.size == 1) setOf(0) else emptySet()
        }
    }

    /** Ketuk kotak wajah: pilih / batal pilih. Maksimal 2 wajah per foto. */
    fun toggleFace(face: Int) {
        val cur = selectedFaces
        selectedFaces = when {
            face in cur -> cur - face
            cur.size >= MAX_FACES -> {
                error = "Maksimal hanya 2 wajah yang bisa dijadikan stiker."
                cur
            }
            else -> cur + face
        }
    }

    /** Pilih semua wajah di foto (hanya kalau jumlahnya ≤ 2). */
    fun selectAllFaces() {
        val p = photos[0] ?: return
        if (p.faces.size > MAX_FACES) {
            error = "Foto ini punya ${p.faces.size} wajah. Maksimal hanya 2 wajah yang bisa dijadikan stiker, jadi pilih 2 saja."
            return
        }
        selectedFaces = p.faces.indices.toSet()
    }

    fun setPhotos(uris: List<Uri>) {
        // 3 foto sekaligus = ganti semua; 1–2 foto = isi slot yang masih kosong dulu (mulai dari foto utama)
        val empty = (0..2).filter { photoUris[it] == null }
        val targets = if (uris.size >= 3 || empty.size < uris.size) (0..2).toList() else empty
        uris.zip(targets).forEach { (uri, index) -> setPhoto(index, uri) }
    }

    /** Hapus foto lalu geser foto sesudahnya ke depan, jadi foto utama selalu terisi lebih dulu. */
    fun clearPhoto(index: Int) {
        for (i in index until 2) {
            photoUris[i] = photoUris[i + 1]
            photos[i] = photos[i + 1]
        }
        photoUris[2] = null
        photos[2] = null
        if (index == 0) {
            val faces = photos[0]?.faces.orEmpty()
            selectedFaces = if (faces.size == 1) setOf(0) else emptySet()
        }
    }

    /** Isi kata acak untuk satu stiker (tidak sama dengan kata di stiker lain). */
    fun randomWord(index: Int) {
        val others = words.filterIndexed { i, _ -> i != index && i < count }
        words[index] = WordBank.random(getApplication(), others)
    }

    /** Isi kata acak untuk semua stiker. */
    fun randomAllWords() {
        val used = mutableListOf<String>()
        for (i in 0 until count) {
            val w = WordBank.random(getApplication(), used)
            words[i] = w
            used.add(w)
        }
    }

    /** Apakah semua bahan untuk memanggil AI sudah lengkap. */
    private fun readyForAi(): Boolean {
        if (!settings.hasCredentials) {
            error = "NEED_API_KEY"
            return false
        }
        return style != null && photos[0] != null && facesReady
    }

    /**
     * Siapkan foto referensi untuk AI.
     *  1 orang : [foto 1 (wajah lain disamarkan), stiker contoh?, foto 2?, foto 3?]
     *  2 orang : [foto 1 berdua, foto 1 hanya A, foto 1 hanya B, stiker contoh?, foto 2?, foto 3?]
     * Foto 2 & 3 opsional dan ditaruh paling belakang, jadi kalau model membatasi jumlah
     * referensi, foto tambahan yang dibuang lebih dulu.
     * @return daftar JPEG + nomor urut (1-based) stiker contoh, kalau ada
     */
    private suspend fun buildRefs(styleRef: Bitmap?, maxRefs: Int): Pair<List<ByteArray>, Int?> =
        withContext(Dispatchers.Default) {
            val infos = photos.filterNotNull()
            val chosen = selectedFaces.sorted()
            val first = infos[0]
            val list = ArrayList<ByteArray>()
            fun add(bmp: Bitmap) {
                list.add(ImageUtils.toJpeg(bmp))
                bmp.recycle()
            }
            add(ImageUtils.buildReference(first.bitmap, first.faces, chosen.toSet()))
            if (chosen.size >= 2) {
                chosen.forEach { face -> add(ImageUtils.buildReference(first.bitmap, first.faces, setOf(face))) }
            }
            var styleIndex: Int? = null
            if (styleRef != null && list.size < maxRefs) {
                list.add(ImageUtils.toJpeg(flattenOnWhite(styleRef)))
                styleIndex = list.size
            }
            for (i in 1 until infos.size) list.add(ImageUtils.toJpeg(infos[i].bitmap))
            list.take(maxRefs) to styleIndex
        }

    /** Stiker transparan → latar putih (JPEG tidak punya transparansi). */
    private fun flattenOnWhite(src: Bitmap): Bitmap {
        val out = Bitmap.createBitmap(src.width, src.height, Bitmap.Config.ARGB_8888)
        val c = android.graphics.Canvas(out)
        c.drawColor(android.graphics.Color.WHITE)
        c.drawBitmap(src, 0f, 0f, null)
        return out
    }

    /** Panggil AI sekali dan potong hasilnya. Mengembalikan lembar asli + (nomor sel, stiker). */
    private suspend fun runAi(
        total: Int,
        captions: List<String>,
        poseOffset: Int,
        styleRef: Bitmap?,
        customPoses: List<String> = emptyList(),
    ): Pair<Bitmap, List<Pair<Int, Bitmap>>> {
        val generator = settings.generator()
        val grid = GridSpec.forCount(total)
        val (refs, styleIndex) = buildRefs(styleRef, generator.maxReferences)
        val prompt = PromptBuilder.build(
            style!!, grid, total, captions, peopleCount, photoCount, poseOffset, styleIndex, customPoses
        )
        val sheetBmp = generator.generateSheet(prompt, refs, grid.aspectRatio)
        val pieces = withContext(Dispatchers.Default) { StickerMaker.sliceIndexed(sheetBmp, grid, total) }
        return sheetBmp to pieces
    }

    fun generate() {
        if (generating || !readyForAi()) return
        val chosenStyle = style ?: return

        generating = true
        error = null
        val app = getApplication<Application>()
        Notifier.clearDone(app)
        GenerationService.start(app, "AI sedang membuat $count stiker…")
        job = viewModelScope.launch {
            try {
                val total = count
                val captions = if (useText) activeWords.map { it.trim() } else List(total) { "" }
                val chosenPoses = if (useText) poses.take(total).map { it.trim() } else List(total) { "" }
                val (sheetBmp, pieces) = runAi(total, captions, 0, null, chosenPoses)

                if (pieces.size < GridSpec.WHATSAPP_MIN) {
                    sheet = sheetBmp.asImageBitmap()
                    throw IllegalStateException(
                        "AI hanya mampu membuat ${pieces.size} stiker (WhatsApp butuh minimal 3). " +
                            "Coba buat ulang, atau ganti model AI di ⚙️ Pengaturan."
                    )
                }

                // sesi baru: belum disimpan ke paket mana pun
                sessionTag = "s" + System.currentTimeMillis().toString(36)
                committedPackId = null
                newPackId = null
                targetPackId = null
                selection = emptySet()
                selecting = false

                sheet = sheetBmp.asImageBitmap()
                stickerBitmaps = pieces.map { it.second }
                stickers = stickerBitmaps.map { it.asImageBitmap() }
                stickerCaptions = pieces.map { captions.getOrElse(it.first) { "" } }
                stickerPoses = pieces.map { chosenPoses.getOrElse(it.first) { "" } }
                packName = "Lumi ${chosenStyle.label} " +
                    SimpleDateFormat("d MMM HH.mm", Locale("id", "ID")).format(Date())
                refreshPacks()
                notice = if (pieces.size < total) {
                    "AI hanya mampu membuat ${pieces.size} dari $total stiker yang diminta, jadi hanya " +
                        "${pieces.size} stiker yang dipotong. Kalau ingin lengkap, tekan \"Buat ulang semua\" " +
                        "atau ganti model AI di ⚙️ Pengaturan."
                } else null
                step = Step.RESULT
                Notifier.done(
                    app, "Stiker sudah jadi! 🎉",
                    if (pieces.size < total) "${pieces.size} dari $total stiker berhasil dibuat. Ketuk untuk melihat."
                    else "${pieces.size} stiker siap ditambahkan ke WhatsApp. Ketuk untuk melihat."
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                error = e.message ?: "Terjadi kesalahan. Coba lagi."
                Notifier.done(app, "Pembuatan stiker gagal", error ?: "Ketuk untuk mencoba lagi.")
            } finally {
                generating = false
                GenerationService.stop(app)
            }
        }
    }

    /* ------------------------------------------------ pilih & buat ulang sebagian stiker */

    fun startSelecting() {
        selecting = true
        selection = emptySet()
    }

    fun stopSelecting() {
        selecting = false
        selection = emptySet()
    }

    fun toggleSelection(index: Int) {
        selection = if (index in selection) selection - index else selection + index
    }

    fun selectAll() {
        selection = stickers.indices.toSet()
    }

    /** Kata acak untuk satu stiker di dialog "ubah kata" (tidak sama dengan kata stiker lain). */
    fun randomCaption(exclude: Collection<String>): String = WordBank.random(getApplication(), exclude)

    /**
     * Buat ulang hanya stiker yang dicentang. [newCaptions] berisi kata baru per nomor stiker
     * (kosong = tanpa kata). Stiker lain tidak berubah.
     */
    fun regenerateSelected(newCaptions: Map<Int, String>, newPoses: Map<Int, String> = emptyMap()) {
        if (generating || !readyForAi()) return
        val targets = selection.sorted().filter { it in stickers.indices }
        if (targets.isEmpty()) return

        generating = true
        regenerating = targets.size
        error = null
        val app = getApplication<Application>()
        Notifier.clearDone(app)
        GenerationService.start(app, "AI sedang membuat ulang ${targets.size} stiker…")
        job = viewModelScope.launch {
            try {
                val captions = targets.map { (newCaptions[it] ?: stickerCaptions.getOrElse(it) { "" }).trim() }
                // stiker yang tidak dicentang dipakai sebagai contoh gaya agar hasilnya tetap senada
                val styleRef = stickerBitmaps.filterIndexed { i, _ -> i !in targets }.firstOrNull()
                // pose pilihan tetap dipakai selama katanya tidak diganti
                val regenPoses = targets.map {
                    newPoses[it]?.let { p -> return@map p.trim() }
                    val changed = newCaptions[it]?.trim()?.let { c -> c != stickerCaptions.getOrElse(it) { "" }.trim() } ?: false
                    if (changed) "" else stickerPoses.getOrElse(it) { "" }
                }
                val (sheetBmp, pieces) = runAi(targets.size, captions, (1..9).random(), styleRef, regenPoses)

                val bitmaps = stickerBitmaps.toMutableList()
                val caps = stickerCaptions.toMutableList()
                val poseList = stickerPoses.toMutableList()
                pieces.forEach { (cell, bmp) ->
                    val target = targets.getOrNull(cell) ?: return@forEach
                    bitmaps[target] = bmp
                    caps[target] = captions[cell]
                    if (target < poseList.size) poseList[target] = regenPoses[cell]
                }
                sheet = sheetBmp.asImageBitmap()
                stickerBitmaps = bitmaps
                stickers = bitmaps.map { it.asImageBitmap() }
                stickerCaptions = caps
                stickerPoses = poseList

                val missed = targets.size - pieces.size
                notice = if (missed > 0) {
                    "${pieces.size} dari ${targets.size} stiker berhasil dibuat ulang. Sisanya tidak tergambar oleh AI, coba lagi untuk stiker itu."
                } else "${targets.size} stiker berhasil dibuat ulang ✨"
                stopSelecting()
                recommitIfNeeded()
                Notifier.done(app, "Stiker sudah diperbarui ✨", notice ?: "Ketuk untuk melihat.")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                error = e.message ?: "Terjadi kesalahan. Coba lagi."
                Notifier.done(app, "Pembuatan ulang gagal", error ?: "Ketuk untuk mencoba lagi.")
            } finally {
                generating = false
                regenerating = 0
                GenerationService.stop(app)
            }
        }
    }

    fun cancel() {
        job?.cancel()
        generating = false
        regenerating = 0
        GenerationService.stop(getApplication())
    }

    override fun onCleared() {
        GenerationService.stop(getApplication())
        super.onCleared()
    }

    /* ------------------------------------------------ simpan ke paket */

    fun refreshPacks() {
        existingPacks = PackStore.all(getApplication()).filter { it.id != newPackId }
    }

    /** Sisa tempat di paket (stiker sesi ini yang sudah ada di sana tidak dihitung dua kali). */
    fun freeSlots(pack: StickerPack): Int {
        val mine = pack.stickers.count { it.fileName.startsWith("${sessionTag}_") }
        return PackStore.MAX_PER_PACK - (pack.stickers.size - mine)
    }

    /**
     * Simpan stiker ke paket tujuan (baru atau yang sudah ada), lalu panggil [onReady]
     * dengan id & nama paket untuk dikirim ke WhatsApp.
     */
    fun commitToPack(onReady: (String, String) -> Unit) {
        if (stickerBitmaps.isEmpty()) return
        viewModelScope.launch {
            try {
                val pack = withContext(Dispatchers.IO) { writeToTarget() }
                committedPackId = pack.id
                refreshPacks()
                onReady(pack.id, pack.name)
            } catch (e: PackFullException) {
                error = e.message
            } catch (e: Exception) {
                error = "Gagal menyimpan paket: ${e.message}"
            }
        }
    }

    private fun writeToTarget(): StickerPack {
        val app = getApplication<Application>()
        val existing = targetPackId
        val id = existing ?: (newPackId ?: ("lumi_" + System.currentTimeMillis()).also { newPackId = it })
        // kalau sebelumnya sudah disimpan ke paket lain, lepas dari paket itu dulu
        val previous = committedPackId
        if (previous != null && previous != id) PackStore.removeSession(app, previous, sessionTag)
        if (existing != null && newPackId != null && previous == newPackId) newPackId = null
        val name = if (existing == null) packName.trim().ifEmpty { "Lumi Stickerize" } else null
        return PackStore.upsertSession(
            app, id, name, settings.publisher, sessionTag, stickerBitmaps, stickerCaptions
        )
    }

    /** Setelah stiker diedit / dibuat ulang, perbarui juga paket yang sudah disimpan. */
    private fun recommitIfNeeded() {
        if (committedPackId == null) return
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) { writeToTarget() }
                refreshPacks()
            } catch (e: Exception) {
                error = "Gagal memperbarui paket: ${e.message}"
            }
        }
    }

    /** Simpan stiker yang sudah dihapus manual. */
    fun updateSticker(index: Int, edited: Bitmap) {
        stickerBitmaps = stickerBitmaps.toMutableList().also { it[index] = edited }
        stickers = stickers.toMutableList().also { it[index] = edited.asImageBitmap() }
        notice = "Stiker ${index + 1} diperbarui."
        recommitIfNeeded()
    }

    fun restart() {
        if (generating) return
        for (i in 0 until 3) clearPhoto(i)
        count = 5
        style = null
        useText = false
        for (i in words.indices) words[i] = ""
        for (i in poses.indices) poses[i] = ""
        sheet = null
        stickers = emptyList()
        stickerBitmaps = emptyList()
        stickerCaptions = emptyList()
        stickerPoses = emptyList()
        committedPackId = null
        newPackId = null
        targetPackId = null
        packName = ""
        stopSelecting()
        notice = null
        error = null
        step = Step.PHOTOS
    }

    companion object {
        /** WhatsApp mewajibkan minimal 3 stiker per paket. */
        const val MIN_STICKERS = GridSpec.WHATSAPP_MIN
        const val MAX_STICKERS = 10
        const val MAX_FACES = 2
    }
}
