package com.lumi.stickerize.ui

import android.app.Application
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Rect
import android.net.Uri
import androidx.annotation.StringRes
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.lumi.stickerize.R
import com.lumi.stickerize.ai.AiException
import com.lumi.stickerize.ai.ErrorKind
import com.lumi.stickerize.ai.PromptBuilder
import com.lumi.stickerize.data.AppSettings
import com.lumi.stickerize.data.ThemePack
import com.lumi.stickerize.data.ThemePacks
import com.lumi.stickerize.data.WordBank
import com.lumi.stickerize.model.GridSpec
import com.lumi.stickerize.model.StickerStyle
import com.lumi.stickerize.sticker.PackFullException
import com.lumi.stickerize.sticker.PackStore
import com.lumi.stickerize.sticker.StickerMaker
import com.lumi.stickerize.sticker.StickerPack
import com.lumi.stickerize.util.FaceFinder
import com.lumi.stickerize.util.GallerySaver
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

enum class Step(@StringRes val title: Int, @StringRes val subtitle: Int) {
    PHOTOS(R.string.step_photos, R.string.step_photos_sub),
    FACES(R.string.step_faces, R.string.step_faces_sub),
    COUNT(R.string.step_count, R.string.step_count_sub),
    STYLE(R.string.step_style, R.string.step_style_sub),
    TEXT(R.string.step_text, R.string.step_text_sub),
    GENERATE(R.string.step_generate, R.string.step_generate_sub),
    RESULT(R.string.step_result, R.string.step_result_sub),
}

/** Satu foto referensi beserta hasil deteksi wajahnya. */
class PhotoInfo(val bitmap: Bitmap, val image: ImageBitmap, val faces: List<Rect>)

/** Pesan error beserta jenisnya, supaya layar error bisa menawarkan tombol yang tepat. */
data class AppError(val message: String, val kind: ErrorKind = ErrorKind.OTHER, val canRetry: Boolean = false)

/** Stiker di paket (untuk layar Paket saya). */
class PackSticker(val fileName: String, val image: ImageBitmap)

class MainViewModel(app: Application) : AndroidViewModel(app) {

    val settings = AppSettings(app)

    /** Teks sesuai bahasa aplikasi. */
    private fun s(@StringRes id: Int, vararg args: Any): String = getApplication<Application>().getString(id, *args)

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
    /** Paket tema yang terakhir dipakai (untuk ditandai di daftar). */
    var appliedTheme by mutableStateOf<String?>(null)
        private set

    var generating by mutableStateOf(false)
        private set
    var error by mutableStateOf<AppError?>(null)
    /** true = buka Pengaturan karena API key belum diisi. */
    var askForKey by mutableStateOf(false)
    var notice by mutableStateOf<String?>(null)

    /** Layar penuh di atas halaman hasil: stiker yang sedang dirapikan & tampilan gambar asli AI. */
    var editingIndex by mutableStateOf<Int?>(null)
    var viewingSheet by mutableStateOf(false)
    /** Layar penuh daftar paket tema (dibuka dari langkah Tulisan). */
    var themePickerOpen by mutableStateOf(false)

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

    /** Stiker yang diminta tapi tidak tergambar oleh AI (kata, pose) — bisa dilengkapi. */
    var missing by mutableStateOf<List<Pair<String, String>>>(emptyList())
        private set
    /** Jumlah stiker yang sedang dilengkapi (0 = tidak sedang). */
    var filling by mutableIntStateOf(0)
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
    /** Aksi AI terakhir, untuk tombol "Coba lagi" di layar error. */
    private var lastAction: (() -> Unit)? = null

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

    private fun fail(message: String, kind: ErrorKind = ErrorKind.OTHER, canRetry: Boolean = false) {
        error = AppError(message, kind, canRetry)
    }

    fun retry() {
        val action = lastAction ?: return
        error = null
        action()
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
                fail(s(R.string.err_photo_open))
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
                fail(s(R.string.err_max_faces))
                cur
            }
            else -> cur + face
        }
    }

    /** Pilih semua wajah di foto (hanya kalau jumlahnya ≤ 2). */
    fun selectAllFaces() {
        val p = photos[0] ?: return
        if (p.faces.size > MAX_FACES) {
            fail(s(R.string.err_too_many_faces, p.faces.size))
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

    /* ------------------------------------------------ kata, pose & paket tema */

    private fun currentTheme(): ThemePack? = appliedTheme?.let { id -> themePacks().firstOrNull { it.id == id } }

    /**
     * Isi kata acak untuk satu stiker (tidak sama dengan kata di stiker lain).
     * Kalau sedang memakai paket tema, kata & pose diambil dari tema itu.
     */
    fun randomWord(index: Int) {
        val others = words.filterIndexed { i, _ -> i != index && i < count }
        val theme = currentTheme()
        if (theme != null) {
            val used = others.map { it.trim().lowercase() }.toSet() + words[index].trim().lowercase()
            val item = theme.items.filter { it.caption.lowercase() !in used }.randomOrNull() ?: theme.items.random()
            words[index] = item.caption
            poses[index] = item.pose
            return
        }
        words[index] = WordBank.random(getApplication(), others)
    }

    /** Isi kata acak untuk semua stiker (dari paket tema yang dipakai, kalau ada). */
    fun randomAllWords() {
        currentTheme()?.let { applyTheme(it); return }
        val used = mutableListOf<String>()
        for (i in 0 until count) {
            val w = WordBank.random(getApplication(), used)
            words[i] = w
            poses[i] = ""
            used.add(w)
        }
    }

    fun themePacks(): List<ThemePack> = ThemePacks.all(getApplication())

    /**
     * Pakai paket tema: kata dan pose tiap stiker langsung terisi (tetap bisa diubah).
     * Tiap tema punya 20 kata & pose; yang dipakai dipilih acak, jadi ketuk lagi = kombinasi baru.
     */
    fun applyTheme(pack: ThemePack) {
        useText = true
        val picked = pack.items.shuffled()
        for (i in 0 until MAX_STICKERS) {
            val item = picked.getOrNull(i)
            words[i] = item?.caption.orEmpty()
            poses[i] = item?.pose.orEmpty()
        }
        appliedTheme = pack.id
    }

    /** Berhenti memakai paket tema (kata & pose yang sudah terisi tetap ada). */
    fun clearTheme() {
        appliedTheme = null
    }

    /* ------------------------------------------------ AI */

    /** Apakah semua bahan untuk memanggil AI sudah lengkap. */
    private fun readyForAi(): Boolean {
        if (!settings.hasCredentials) {
            askForKey = true
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

    /** Pesan error yang ramah + jenisnya, dari exception apa pun. */
    private fun handleFailure(e: Exception, @StringRes notifTitle: Int) {
        val app = getApplication<Application>()
        if (e is AiException) {
            val retry = e.kind == ErrorKind.NETWORK || e.kind == ErrorKind.BUSY || e.kind == ErrorKind.OTHER
            fail(e.message ?: s(R.string.err_generic), e.kind, retry)
        } else {
            fail(e.message ?: s(R.string.err_generic), ErrorKind.OTHER, canRetry = true)
        }
        Notifier.done(app, s(notifTitle), error?.message ?: s(R.string.notif_tap_retry))
    }

    fun generate() {
        if (generating || !readyForAi()) return
        val chosenStyle = style ?: return
        lastAction = { generate() }

        generating = true
        error = null
        val app = getApplication<Application>()
        Notifier.clearDone(app)
        GenerationService.start(app, s(R.string.notif_making, count))
        job = viewModelScope.launch {
            try {
                val total = count
                val captions = if (useText) activeWords.map { it.trim() } else List(total) { "" }
                val chosenPoses = if (useText) poses.take(total).map { it.trim() } else List(total) { "" }
                val (sheetBmp, pieces) = runAi(total, captions, 0, null, chosenPoses)

                if (pieces.size < GridSpec.WHATSAPP_MIN) {
                    sheet = sheetBmp.asImageBitmap()
                    throw AiException(s(R.string.err_too_few, pieces.size), ErrorKind.BUSY)
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
                val drawn = pieces.map { it.first }.toSet()
                missing = (0 until total).filter { it !in drawn }.map { captions[it] to chosenPoses[it] }
                packName = "Lumi ${s(chosenStyle.label)} " +
                    SimpleDateFormat("d MMM HH.mm", java.util.Locale("in", "ID")).format(Date())
                refreshPacks()
                notice = null
                step = Step.RESULT
                Notifier.done(
                    app, s(R.string.notif_done_title),
                    if (pieces.size < total) s(R.string.notif_done_partial, pieces.size, total)
                    else s(R.string.notif_done_text, pieces.size)
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                handleFailure(e, R.string.notif_failed)
            } finally {
                generating = false
                GenerationService.stop(app)
            }
        }
    }

    /** Buat hanya stiker yang tidak tergambar oleh AI, lalu tambahkan ke hasil. */
    fun fillMissing() {
        if (generating || missing.isEmpty() || !readyForAi()) return
        val wanted = missing.take(MAX_STICKERS - stickers.size)
        if (wanted.isEmpty()) return
        lastAction = { fillMissing() }
        generating = true
        filling = wanted.size
        error = null
        val app = getApplication<Application>()
        Notifier.clearDone(app)
        GenerationService.start(app, s(R.string.notif_filling, wanted.size))
        job = viewModelScope.launch {
            try {
                val captions = wanted.map { it.first }
                val customPoses = wanted.map { it.second }
                // stiker yang sudah jadi dipakai sebagai contoh gaya agar senada
                val styleRef = stickerBitmaps.firstOrNull()
                val (sheetBmp, pieces) = runAi(wanted.size, captions, (1..9).random(), styleRef, customPoses)
                if (pieces.isEmpty()) throw AiException(s(R.string.err_fill_none), ErrorKind.BUSY)

                sheet = sheetBmp.asImageBitmap()
                val drawn = pieces.map { it.first }.toSet()
                stickerBitmaps = stickerBitmaps + pieces.map { it.second }
                stickers = stickerBitmaps.map { it.asImageBitmap() }
                stickerCaptions = stickerCaptions + pieces.map { captions.getOrElse(it.first) { "" } }
                stickerPoses = stickerPoses + pieces.map { customPoses.getOrElse(it.first) { "" } }
                missing = wanted.filterIndexed { i, _ -> i !in drawn }
                notice = if (missing.isEmpty()) s(R.string.notice_filled, pieces.size)
                else s(R.string.notice_filled_partial, pieces.size, wanted.size)
                recommitIfNeeded()
                Notifier.done(app, s(R.string.notif_updated_title), notice ?: "")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                handleFailure(e, R.string.notif_failed)
            } finally {
                generating = false
                filling = 0
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
        lastAction = { regenerateSelected(newCaptions, newPoses) }

        generating = true
        regenerating = targets.size
        error = null
        val app = getApplication<Application>()
        Notifier.clearDone(app)
        GenerationService.start(app, s(R.string.notif_remaking, targets.size))
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
                notice = if (missed > 0) s(R.string.notice_remade_partial, pieces.size, targets.size)
                else s(R.string.notice_remade, targets.size)
                stopSelecting()
                recommitIfNeeded()
                Notifier.done(app, s(R.string.notif_updated_title), notice ?: "")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                handleFailure(e, R.string.notif_remake_failed)
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
        filling = 0
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
                fail(s(R.string.err_pack_full, e.pack.name, e.free, PackStore.MAX_PER_PACK))
            } catch (e: Exception) {
                fail(s(R.string.err_pack_save, e.message.orEmpty()))
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
            } catch (e: PackFullException) {
                fail(s(R.string.err_pack_full, e.pack.name, e.free, PackStore.MAX_PER_PACK))
            } catch (e: Exception) {
                fail(s(R.string.err_pack_update, e.message.orEmpty()))
            }
        }
    }

    /** Simpan stiker yang sudah dihapus manual. */
    fun updateSticker(index: Int, edited: Bitmap) {
        stickerBitmaps = stickerBitmaps.toMutableList().also { it[index] = edited }
        stickers = stickers.toMutableList().also { it[index] = edited.asImageBitmap() }
        notice = s(R.string.notice_sticker_updated, index + 1)
        recommitIfNeeded()
    }

    /* ------------------------------------------------ simpan ke Galeri */

    fun saveToGallery(bitmaps: List<Bitmap> = stickerBitmaps, name: String = "lumi") {
        if (bitmaps.isEmpty()) return
        val app = getApplication<Application>()
        viewModelScope.launch {
            val ok = withContext(Dispatchers.IO) { GallerySaver.save(app, bitmaps, name) }
            if (ok > 0) notice = s(R.string.notice_gallery, ok, GallerySaver.FOLDER)
            else fail(s(R.string.err_gallery))
        }
    }

    /* ------------------------------------------------ layar "Paket saya" */

    var managerOpen by mutableStateOf(false)
    var managerPackId by mutableStateOf<String?>(null)
    var managerStickers by mutableStateOf<List<PackSticker>>(emptyList())
        private set
    var managerMessage by mutableStateOf<String?>(null)

    fun openManager() {
        refreshAllPacks()
        managerPackId = null
        managerMessage = null
        managerOpen = true
    }

    /** Semua paket (termasuk paket baru dari sesi ini). */
    var allPacks by mutableStateOf<List<StickerPack>>(emptyList())
        private set

    private fun refreshAllPacks() {
        allPacks = PackStore.all(getApplication())
        refreshPacks()
        // paket sesi ini dihapus dari layar Paket saya → sesi dianggap belum disimpan
        val committed = committedPackId
        if (committed != null && allPacks.none { it.id == committed }) {
            committedPackId = null
            if (newPackId == committed) newPackId = null
            if (targetPackId == committed) targetPackId = null
        }
        if (targetPackId != null && existingPacks.none { it.id == targetPackId }) targetPackId = null
    }

    fun openPack(id: String) {
        managerPackId = id
        managerStickers = emptyList()
        managerMessage = null
        loadManagerStickers()
    }

    private fun loadManagerStickers() {
        val id = managerPackId ?: return
        val app = getApplication<Application>()
        viewModelScope.launch {
            managerStickers = withContext(Dispatchers.IO) {
                val pack = PackStore.load(app, id) ?: return@withContext emptyList()
                pack.stickers.mapNotNull { st ->
                    val f = PackStore.stickerFile(app, id, st.fileName)
                    BitmapFactory.decodeFile(f.absolutePath)?.let { PackSticker(st.fileName, it.asImageBitmap()) }
                }
            }
        }
    }

    fun renamePack(id: String, name: String) {
        if (name.isBlank()) return
        PackStore.rename(getApplication(), id, name)
        refreshAllPacks()
        managerMessage = s(R.string.packs_renamed)
    }

    /** @return false kalau sisa stiker kurang dari 3 (tidak ada yang dihapus) */
    fun deletePackStickers(id: String, files: Set<String>): Boolean {
        val result = PackStore.deleteStickers(getApplication(), id, files) ?: return false
        refreshAllPacks()
        loadManagerStickers()
        managerMessage = s(R.string.packs_deleted_stickers, files.size, result.stickers.size)
        return true
    }

    fun deletePack(id: String) {
        PackStore.deletePack(getApplication(), id)
        managerPackId = null
        managerStickers = emptyList()
        refreshAllPacks()
        managerMessage = s(R.string.packs_deleted_pack)
    }

    fun savePackToGallery(id: String) {
        val app = getApplication<Application>()
        viewModelScope.launch {
            val ok = withContext(Dispatchers.IO) {
                val pack = PackStore.load(app, id) ?: return@withContext 0
                val bitmaps = pack.stickers.mapNotNull {
                    BitmapFactory.decodeFile(PackStore.stickerFile(app, id, it.fileName).absolutePath)
                }
                GallerySaver.save(app, bitmaps, "lumi_pack")
            }
            managerMessage = if (ok > 0) s(R.string.notice_gallery, ok, GallerySaver.FOLDER) else s(R.string.err_gallery)
        }
    }

    /* ------------------------------------------------ edit stiker di paket lama */

    /** Stiker paket yang sedang dirapikan dengan penghapus (layar Paket saya). */
    var managerEditFile by mutableStateOf<String?>(null)
        private set
    var managerEditBitmap by mutableStateOf<Bitmap?>(null)
        private set
    /** Jumlah stiker paket yang sedang dibuat ulang (0 = tidak sedang). */
    var managerBusy by mutableIntStateOf(0)
        private set
    private var managerJob: Job? = null

    fun openPackEraser(id: String, fileName: String) {
        val app = getApplication<Application>()
        viewModelScope.launch {
            val bmp = withContext(Dispatchers.IO) {
                BitmapFactory.decodeFile(PackStore.stickerFile(app, id, fileName).absolutePath)
            } ?: return@launch
            managerEditBitmap = bmp
            managerEditFile = fileName
        }
    }

    fun closePackEraser() {
        managerEditFile = null
        managerEditBitmap = null
    }

    fun savePackEraser(id: String, edited: Bitmap) {
        val file = managerEditFile ?: return
        val app = getApplication<Application>()
        closePackEraser()
        viewModelScope.launch {
            val ok = withContext(Dispatchers.IO) { PackStore.replaceSticker(app, id, file, edited) } != null
            refreshAllPacks()
            loadManagerStickers()
            managerMessage = if (ok) s(R.string.packs_edit_saved) else s(R.string.err_generic)
        }
    }

    /**
     * Buat ulang beberapa stiker di paket yang sudah jadi. Foto asli tidak disimpan, jadi
     * stiker lain di paket itu dipakai sebagai contoh wajah & gaya.
     * @param captions kata baru per nama file, @param newPoses pose baru per nama file (opsional)
     */
    fun regeneratePackStickers(id: String, files: List<String>, captions: Map<String, String>, newPoses: Map<String, String>) {
        if (files.isEmpty() || generating || managerBusy > 0) return
        if (!settings.hasCredentials) {
            askForKey = true
            return
        }
        val app = getApplication<Application>()
        val pack = PackStore.load(app, id) ?: return
        val targets = files.filter { f -> pack.stickers.any { it.fileName == f } }.take(MAX_STICKERS)
        if (targets.isEmpty()) return
        lastAction = { regeneratePackStickers(id, files, captions, newPoses) }

        managerBusy = targets.size
        error = null
        Notifier.clearDone(app)
        GenerationService.start(app, s(R.string.notif_remaking, targets.size))
        managerJob = viewModelScope.launch {
            try {
                val generator = settings.generator()
                // contoh: utamakan stiker yang tidak dibuat ulang, lalu stiker yang dibuat ulang
                val refFiles = (pack.stickers.map { it.fileName }.filterNot { it in targets } + targets)
                    .take(generator.maxReferences.coerceIn(1, 3))
                val refs = withContext(Dispatchers.IO) {
                    refFiles.mapNotNull { f ->
                        BitmapFactory.decodeFile(PackStore.stickerFile(app, id, f).absolutePath)?.let { bmp ->
                            ImageUtils.toJpeg(flattenOnWhite(bmp)).also { bmp.recycle() }
                        }
                    }
                }
                if (refs.isEmpty()) throw AiException(s(R.string.err_generic), ErrorKind.OTHER)
                val caps = targets.map { f -> (captions[f] ?: pack.stickers.first { it.fileName == f }.caption).trim() }
                val posesList = targets.map { newPoses[it].orEmpty().trim() }
                val grid = GridSpec.forCount(targets.size)
                val prompt = PromptBuilder.buildFromStickers(grid, targets.size, caps, posesList, refs.size)
                val sheetBmp = generator.generateSheet(prompt, refs, grid.aspectRatio)
                val pieces = withContext(Dispatchers.Default) { StickerMaker.sliceIndexed(sheetBmp, grid, targets.size) }
                if (pieces.isEmpty()) throw AiException(s(R.string.err_fill_none), ErrorKind.BUSY)
                withContext(Dispatchers.IO) {
                    pieces.forEach { (cell, bmp) ->
                        val f = targets.getOrNull(cell) ?: return@forEach
                        PackStore.replaceSticker(app, id, f, bmp, caps[cell])
                    }
                }
                refreshAllPacks()
                if (managerPackId == id) loadManagerStickers()
                managerMessage = if (pieces.size < targets.size) s(R.string.notice_remade_partial, pieces.size, targets.size)
                else s(R.string.packs_remade, pieces.size)
                Notifier.done(app, s(R.string.notif_updated_title), managerMessage ?: "")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                handleFailure(e, R.string.notif_remake_failed)
            } finally {
                managerBusy = 0
                GenerationService.stop(app)
            }
        }
    }

    fun cancelPackRegenerate() {
        managerJob?.cancel()
        managerBusy = 0
        GenerationService.stop(getApplication())
    }

    fun restart() {
        if (generating) return
        for (i in 0 until 3) clearPhoto(i)
        count = 5
        style = null
        useText = false
        for (i in words.indices) words[i] = ""
        for (i in poses.indices) poses[i] = ""
        appliedTheme = null
        sheet = null
        stickers = emptyList()
        stickerBitmaps = emptyList()
        stickerCaptions = emptyList()
        stickerPoses = emptyList()
        missing = emptyList()
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
