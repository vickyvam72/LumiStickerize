package com.lumi.stickerize.ui

import android.app.Activity
import android.content.ActivityNotFoundException
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lumi.stickerize.R
import com.lumi.stickerize.data.AppSettings
import com.lumi.stickerize.data.Provider
import com.lumi.stickerize.model.GridSpec
import com.lumi.stickerize.model.StickerStyle
import com.lumi.stickerize.sticker.PackStore
import com.lumi.stickerize.ui.theme.LumiBlue
import com.lumi.stickerize.ui.theme.LumiGradient
import com.lumi.stickerize.ui.theme.LumiTile
import com.lumi.stickerize.ui.theme.WhatsAppGreen
import com.lumi.stickerize.whatsapp.WhatsAppHelper
import kotlinx.coroutines.delay

private const val NEED_API_KEY = "NEED_API_KEY"

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LumiApp(vm: MainViewModel) {
    var showSettings by remember { mutableStateOf(false) }

    LaunchedEffect(vm.error) {
        if (vm.error == NEED_API_KEY) {
            vm.error = null
            showSettings = true
        }
    }

    BackHandler(enabled = vm.step != Step.PHOTOS && !vm.generating) { vm.back() }

    // Seluruh layar menyisakan ruang untuk tombol navigasi HP dan keyboard,
    // jadi tombol & kolom isian tidak pernah tertutup.
    val imeVisible = WindowInsets.isImeVisible
    Box(Modifier.fillMaxSize()) {
    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .imePadding()
    ) {
        Header(step = vm.step, compact = imeVisible, onSettings = { showSettings = true })

        Box(Modifier.weight(1f)) {
            AnimatedContent(targetState = vm.step, label = "step") { step ->
                Column(
                    Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    when (step) {
                        Step.PHOTOS -> PhotosStep(vm)
                        Step.FACES -> FacesStep(vm)
                        Step.COUNT -> CountStep(vm)
                        Step.STYLE -> StyleStep(vm)
                        Step.TEXT -> TextStep(vm)
                        Step.GENERATE -> GenerateStep(vm)
                        Step.RESULT -> ResultStep(vm)
                    }
                    Spacer(Modifier.height(12.dp))
                }
            }
        }

        BottomBar(vm)
        // Halaman hasil tidak punya tombol bawah: sisakan ruang tombol navigasi HP dengan warna latar yang sama
        if (vm.step == Step.RESULT) Spacer(Modifier.navigationBarsPadding())
    }

    // Layar penuh di jendela aplikasi sendiri (bukan Dialog), jadi latarnya menyambung
    // sampai paling bawah di belakang tombol navigasi HP tanpa garis hitam.
    if (vm.step == Step.RESULT) {
        vm.editingIndex?.let { idx ->
            val original = vm.stickerBitmaps.getOrNull(idx)
            if (original != null) {
                key(idx) {
                    EraserScreen(
                        index = idx,
                        original = original,
                        onDismiss = { vm.editingIndex = null },
                        onSave = { edited ->
                            vm.updateSticker(idx, edited)
                            vm.editingIndex = null
                        }
                    )
                }
            }
        }
        val sheet = vm.sheet
        if (vm.viewingSheet && sheet != null) {
            SheetViewer(sheet, onClose = { vm.viewingSheet = false })
        }
    }
    }

    if (showSettings) {
        SettingsDialog(settings = vm.settings, onClose = { showSettings = false })
    }

    val err = vm.error
    if (err != null && err != NEED_API_KEY) {
        AlertDialog(
            onDismissRequest = { vm.error = null },
            confirmButton = { TextButton(onClick = { vm.error = null }) { Text("Oke") } },
            title = { Text("Ups!") },
            text = { Text(err) },
        )
    }
}

/* ---------------------------------------------------------------- header & navigasi */

@Composable
private fun Header(step: Step, compact: Boolean, onSettings: () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .background(LumiGradient)
            .statusBarsPadding()
            .padding(start = 16.dp, end = 4.dp, top = 2.dp, bottom = if (compact) 6.dp else 12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color.White.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(R.drawable.ic_launcher_foreground),
                    contentDescription = null,
                    modifier = Modifier.size(46.dp),
                )
            }
            Spacer(Modifier.width(10.dp))
            Text(
                "Lumi Stickerize",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = onSettings) {
                Icon(Icons.Filled.Settings, contentDescription = "Pengaturan", tint = Color.White)
            }
        }
        // saat keyboard terbuka, header diperkecil supaya kolom isian tetap kelihatan
        if (compact) return@Column
        Row(verticalAlignment = Alignment.Bottom, modifier = Modifier.padding(end = 12.dp)) {
            Text(
                step.title,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 21.sp,
                modifier = Modifier.weight(1f)
            )
            Text(
                if (step == Step.RESULT) "Selesai" else "Langkah ${step.ordinal + 1}/${Step.entries.size - 1}",
                color = Color.White.copy(alpha = 0.85f),
                fontSize = 12.sp,
                modifier = Modifier.padding(bottom = 4.dp)
            )
        }
        Text(step.subtitle, color = Color.White.copy(alpha = 0.9f), fontSize = 13.sp)
        Spacer(Modifier.height(8.dp))
        LinearProgressIndicator(
            progress = { (step.ordinal + 1) / Step.entries.size.toFloat() },
            modifier = Modifier
                .fillMaxWidth()
                .padding(end = 12.dp)
                .clip(CircleShape),
            color = Color.White,
            trackColor = Color.White.copy(alpha = 0.3f),
        )
    }
}

@Composable
private fun BottomBar(vm: MainViewModel) {
    if (vm.step == Step.RESULT) return
    // Latar putih tombol bawah ikut memanjang sampai belakang tombol navigasi HP,
    // jadi tidak ada bayangan / warna lain di area itu.
    Surface(shadowElevation = 3.dp, color = MaterialTheme.colorScheme.surface) {
        Row(
            Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (vm.step != Step.PHOTOS) {
                OutlinedButton(
                    onClick = { vm.back() },
                    enabled = !vm.generating,
                    modifier = Modifier.height(46.dp)
                ) { Text("Kembali") }
            }
            if (vm.step == Step.GENERATE) {
                if (vm.generating) {
                    OutlinedButton(
                        onClick = { vm.cancel() },
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp)
                    ) { Text("Batalkan") }
                } else {
                    Button(
                        onClick = { vm.generate() },
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp)
                    ) { Text("✨  Buat Stiker", fontWeight = FontWeight.Bold) }
                }
            } else {
                Button(
                    onClick = { vm.next() },
                    enabled = vm.canGoNext(),
                    modifier = Modifier
                        .weight(1f)
                        .height(46.dp)
                ) { Text("Lanjut", fontWeight = FontWeight.Bold) }
            }
        }
    }
}

/* ---------------------------------------------------------------- langkah 1: foto */

@Composable
private fun PhotosStep(vm: MainViewModel) {
    var pendingIndex by remember { mutableIntStateOf(0) }
    val pickOne = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) vm.setPhoto(pendingIndex, uri)
    }
    val pickMany = rememberLauncherForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia(3)
    ) { uris ->
        if (uris.isNotEmpty()) vm.setPhotos(uris)
    }
    val imagesOnly = PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)

    InfoCard(
        "Cukup 1 foto juga bisa. Kalau ingin hasil lebih akurat dan mirip, pilih 2 atau 3 foto. " +
            "Semua orang yang mau dijadikan stiker (maks. 2) harus ada di foto utama."
    )
    Spacer(Modifier.height(8.dp))
    WarningCard(
        "Penting: outfit harus sama",
        "Kalau memilih lebih dari 1 foto, semuanya harus memakai outfit yang SAMA (baju, hijab, aksesoris)."
    )
    Spacer(Modifier.height(12.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        for (i in 0 until 3) {
            PhotoSlot(
                index = i,
                thumb = vm.photos[i]?.image,
                loading = vm.photoUris[i] != null && vm.photos[i] == null,
                onPick = {
                    // Slot kosong selalu diisi berurutan, mulai dari foto utama
                    val firstEmpty = (0..2).firstOrNull { vm.photoUris[it] == null } ?: i
                    pendingIndex = if (vm.photoUris[i] == null) minOf(i, firstEmpty) else i
                    pickOne.launch(imagesOnly)
                },
                onClear = { vm.clearPhoto(i) },
                modifier = Modifier.weight(1f)
            )
        }
    }
    Spacer(Modifier.height(10.dp))
    OutlinedButton(
        onClick = { pickMany.launch(imagesOnly) },
        modifier = Modifier
            .fillMaxWidth()
            .height(42.dp)
    ) {
        Icon(Icons.Filled.Add, contentDescription = null)
        Spacer(Modifier.width(8.dp))
        Text("Pilih 1–3 foto sekaligus")
    }
    Spacer(Modifier.height(6.dp))
    Text(
        when (vm.photoCount) {
            0 -> "Pilih minimal 1 foto untuk lanjut."
            1 -> "1 foto dipilih. Tambah foto 2 atau 3 kalau ingin hasil lebih akurat (opsional)."
            2 -> "2 foto dipilih. Tambah 1 foto lagi untuk hasil paling akurat (opsional)."
            else -> "3 foto dipilih. Hasil paling akurat 👍"
        },
        fontSize = 12.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun PhotoSlot(
    index: Int,
    thumb: ImageBitmap?,
    loading: Boolean,
    onPick: () -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier
            .aspectRatio(0.9f)
            .clip(RoundedCornerShape(16.dp))
            .background(LumiTile)
            .border(
                BorderStroke(
                    2.dp,
                    if (thumb != null) LumiBlue else MaterialTheme.colorScheme.outline
                ),
                RoundedCornerShape(18.dp)
            )
            .clickable(onClick = onPick),
        contentAlignment = Alignment.Center
    ) {
        when {
            thumb != null -> {
                Image(
                    bitmap = thumb,
                    contentDescription = "Foto ${index + 1}",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.55f))
                        .clickable(onClick = onClear),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Filled.Close,
                        contentDescription = "Hapus",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            loading -> CircularProgressIndicator(Modifier.size(28.dp), strokeWidth = 3.dp)
            else -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Filled.Add, contentDescription = null, tint = LumiBlue)
                Spacer(Modifier.height(4.dp))
                Text(
                    if (index == 0) "Foto utama" else "Foto ${index + 1}",
                    color = LumiBlue,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp
                )
                Text(
                    if (index == 0) "wajib" else "opsional",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp
                )
            }
        }
    }
}

/* ---------------------------------------------------------------- langkah 2: wajah */

private val faceColors = listOf(Color(0xFF3E84E8), Color(0xFFE8673E))

@Composable
private fun FacesStep(vm: MainViewModel) {
    InfoCard("Wajah sudah dideteksi otomatis. Ketuk kotak di wajah untuk memilih siapa yang dijadikan stiker (maks. 2).")
    Spacer(Modifier.height(10.dp))
    val info = vm.photos[0] ?: return
    val sel = vm.selectedFaces
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text("Foto utama", fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
        Text(
            if (info.faces.isEmpty()) "Tidak ada wajah" else "${info.faces.size} wajah • ${sel.size} dipilih",
            fontSize = 13.sp,
            color = if (info.faces.isEmpty()) Color(0xFFD14343) else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
    Spacer(Modifier.height(6.dp))
    FacePhoto(info = info, selected = sel, onToggle = { vm.toggleFace(it) })
    Spacer(Modifier.height(8.dp))
    when {
        info.faces.isEmpty() -> Text(
            "Wajah tidak terdeteksi. Kembali dan ganti foto utama dengan foto yang wajahnya terlihat jelas.",
            fontSize = 13.sp,
            color = Color(0xFFD14343)
        )
        info.faces.size == 2 && sel.size < 2 -> OutlinedButton(
            onClick = { vm.selectAllFaces() },
            modifier = Modifier.fillMaxWidth()
        ) { Text("Pilih keduanya (stiker berdua)") }
        info.faces.size > 2 -> Text(
            "Ada ${info.faces.size} wajah. Pilih maksimal 2.",
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        sel.isEmpty() -> Text("Ketuk wajah yang mau dijadikan stiker.", fontSize = 13.sp, color = LumiBlue)
    }
    if (sel.isNotEmpty()) {
        Spacer(Modifier.height(12.dp))
        InfoCard(
            (if (sel.size >= 2) "Stiker akan berisi 2 orang: orang bertanda biru (kiri) dan oranye (kanan)."
            else "Stiker akan berisi 1 orang.") +
                (if (vm.photoCount > 1) " Foto lainnya dipakai sebagai referensi tambahan agar lebih mirip." else "")
        )
    }
}

@Composable
private fun FacePhoto(info: PhotoInfo, selected: Set<Int>, onToggle: (Int) -> Unit) {
    val bw = info.bitmap.width
    val bh = info.bitmap.height
    // Tinggi foto dibatasi supaya seluruh langkah muat satu layar
    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
    BoxWithConstraints(
        Modifier
            .heightIn(max = 330.dp)
            .aspectRatio(bw.toFloat() / bh.toFloat(), matchHeightConstraintsFirst = true)
            .clip(RoundedCornerShape(16.dp))
            .background(LumiTile)
    ) {
        val scale = maxWidth / bw.toFloat()
        Image(
            bitmap = info.image,
            contentDescription = null,
            contentScale = ContentScale.FillBounds,
            modifier = Modifier.fillMaxSize()
        )
        info.faces.forEachIndexed { idx, face ->
            val isSel = idx in selected
            val order = selected.sorted().indexOf(idx)
            val color = if (isSel) faceColors[order.coerceIn(0, 1)] else Color.White
            Box(
                Modifier
                    .offset(x = scale * face.left.toFloat(), y = scale * face.top.toFloat())
                    .size(width = scale * face.width().toFloat(), height = scale * face.height().toFloat())
                    .border(if (isSel) 3.dp else 2.dp, color, RoundedCornerShape(8.dp))
                    .background(if (isSel) Color.Transparent else Color.Black.copy(alpha = 0.25f), RoundedCornerShape(8.dp))
                    .clickable { onToggle(idx) }
            ) {
                Box(
                    Modifier
                        .align(Alignment.TopStart)
                        .offset(x = (-6).dp, y = (-6).dp)
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(color),
                    contentAlignment = Alignment.Center
                ) {
                    if (isSel) {
                        Icon(Icons.Filled.Check, null, tint = Color.White, modifier = Modifier.size(16.dp))
                    } else {
                        Text("${idx + 1}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = LumiBlue)
                    }
                }
            }
        }
    }
    }
}

@Composable
private fun WarningCard(title: String, text: String) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFFFFF4E0))
            .border(1.dp, Color(0xFFF2B54A), RoundedCornerShape(14.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Text("⚠️", fontSize = 15.sp)
        Spacer(Modifier.width(8.dp))
        Column {
            Text(title, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF7A4B00))
            Text(text, fontSize = 12.sp, lineHeight = 16.sp, color = Color(0xFF7A4B00))
        }
    }
}

/* ---------------------------------------------------------------- langkah 3: jumlah */

@Composable
private fun CountStep(vm: MainViewModel) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            RoundButton("−", enabled = vm.count > MainViewModel.MIN_STICKERS) { vm.count-- }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("${vm.count}", fontSize = 46.sp, fontWeight = FontWeight.Black, color = LumiBlue)
                Text("stiker", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            RoundButton("+", enabled = vm.count < MainViewModel.MAX_STICKERS) { vm.count++ }
        }
    }
    Spacer(Modifier.height(12.dp))
    (MainViewModel.MIN_STICKERS..MainViewModel.MAX_STICKERS).chunked(4).forEach { row ->
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            row.forEach { n ->
                val selected = vm.count == n
                Box(
                    Modifier
                        .weight(1f)
                        .height(52.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (selected) LumiBlue else MaterialTheme.colorScheme.surface)
                        .border(1.dp, if (selected) LumiBlue else MaterialTheme.colorScheme.outline, RoundedCornerShape(14.dp))
                        .clickable { vm.count = n },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "$n",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = if (selected) Color.White else MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
        Spacer(Modifier.height(10.dp))
    }
    Spacer(Modifier.height(2.dp))
    InfoCard("Bisa 3 sampai 10 stiker. Semuanya dibuat dalam 1 gambar AI, jadi biayanya sama saja.")
}

@Composable
private fun RoundButton(label: String, enabled: Boolean, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        enabled = enabled,
        shape = CircleShape,
        modifier = Modifier.size(48.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = LumiBlue
        )
    ) { Text(label, fontSize = 26.sp, fontWeight = FontWeight.Bold) }
}

/* ---------------------------------------------------------------- langkah 3: tema */

@Composable
private fun StyleStep(vm: MainViewModel) {
    StickerStyle.entries.chunked(2).forEach { row ->
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            row.forEach { s ->
                val selected = vm.style == s
                Row(
                    Modifier
                        .weight(1f)
                        .height(54.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface)
                        .border(
                            if (selected) 2.dp else 1.dp,
                            if (selected) LumiBlue else MaterialTheme.colorScheme.outline,
                            RoundedCornerShape(14.dp)
                        )
                        .clickable { vm.style = s }
                        .padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(s.emoji, fontSize = 22.sp)
                    Spacer(Modifier.width(10.dp))
                    Text(s.label, fontWeight = FontWeight.Bold, fontSize = 15.sp, modifier = Modifier.weight(1f), maxLines = 1)
                    if (selected) {
                        Icon(Icons.Filled.Check, contentDescription = null, tint = LumiBlue, modifier = Modifier.size(18.dp))
                    }
                }
            }
            if (row.size == 1) Spacer(Modifier.weight(1f))
        }
        Spacer(Modifier.height(10.dp))
    }
}

/* ---------------------------------------------------------------- langkah 4: kata */

private val wordIdeas = listOf(
    "Makasih!", "Siap!", "Wkwkwk", "OTW", "Semangat!",
    "Maaf ya", "Mantap", "Hehe", "Gas!", "Oke deh",
)

@Composable
private fun TextStep(vm: MainViewModel) {
    var poseFor by remember { mutableStateOf<Int?>(null) }
    poseFor?.let { i ->
        PoseDialog(
            number = i + 1,
            caption = vm.words[i],
            initial = vm.poses[i],
            onDismiss = { poseFor = null },
            onSave = { vm.poses[i] = it.trim().take(120); poseFor = null }
        )
    }
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        ChoiceCard("🙂", "Tanpa kata", "Hanya ekspresi", !vm.useText, Modifier.weight(1f)) { vm.useText = false }
        ChoiceCard("💬", "Pakai kata", "${vm.count} kata", vm.useText, Modifier.weight(1f)) { vm.useText = true }
    }
    if (vm.useText) {
        Spacer(Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "Kata pendek paling rapi.\nPose opsional: ketuk + pose",
                fontSize = 12.sp,
                lineHeight = 16.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f)
            )
            FilledTonalButton(
                onClick = { vm.randomAllWords() },
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                modifier = Modifier.height(36.dp)
            ) { Text("🎲 Acak semua", fontSize = 13.sp) }
        }
        Spacer(Modifier.height(6.dp))
        // 2 kolom supaya 10 kata tetap muat satu layar
        (0 until vm.count).chunked(2).forEach { pair ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                pair.forEach { i ->
                    WordField(
                        value = vm.words[i],
                        onChange = { vm.words[i] = it.take(25) },
                        number = i + 1,
                        placeholder = wordIdeas[i % wordIdeas.size],
                        last = i == vm.count - 1,
                        onDice = { vm.randomWord(i) },
                        pose = vm.poses[i],
                        onPose = { poseFor = i },
                        modifier = Modifier.weight(1f)
                    )
                }
                if (pair.size == 1) Spacer(Modifier.weight(1f))
            }
            Spacer(Modifier.height(6.dp))
        }
    }
}

/** Kolom kata ringkas: nomor stiker, isian, dan tombol 🎲. */
@Composable
private fun WordField(
    value: String,
    onChange: (String) -> Unit,
    number: Int,
    placeholder: String,
    last: Boolean,
    onDice: () -> Unit,
    pose: String,
    onPose: () -> Unit,
    modifier: Modifier,
) {
    Row(
        modifier
            .height(52.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
            .padding(start = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("$number", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = LumiBlue)
        Spacer(Modifier.width(8.dp))
        Column(Modifier.weight(1f)) {
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterStart) {
            if (value.isEmpty()) {
                Text(placeholder, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f), maxLines = 1)
            }
            androidx.compose.foundation.text.BasicTextField(
                value = value,
                onValueChange = onChange,
                singleLine = true,
                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface),
                cursorBrush = androidx.compose.ui.graphics.SolidColor(LumiBlue),
                keyboardOptions = KeyboardOptions(imeAction = if (last) ImeAction.Done else ImeAction.Next),
                modifier = Modifier.fillMaxWidth()
            )
        }
        // baris pose opsional di bawah kata
        Text(
            if (pose.isBlank()) "+ pose" else "🧍 $pose",
            fontSize = 11.sp,
            lineHeight = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (pose.isBlank()) LumiBlue.copy(alpha = 0.75f) else LumiBlue,
            maxLines = 1,
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(6.dp))
                .clickable(onClick = onPose)
                .padding(top = 2.dp, bottom = 2.dp)
        )
        }
        Box(
            Modifier
                .size(40.dp)
                .clickable(onClick = onDice),
            contentAlignment = Alignment.Center
        ) { Text("🎲", fontSize = 17.sp) }
    }
}

private val poseIdeas = listOf(
    "Menutup hidung", "Hormat ala tentara", "Tepuk tangan", "Menutup wajah malu",
    "Menunjuk ke depan", "Memeluk erat", "Lari terburu-buru", "Rebahan santai",
    "Melipat tangan, cemberut", "Mengintip dari balik tembok",
)

/** Jendela kecil untuk mengisi pose opsional satu stiker. */
@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
private fun PoseDialog(
    number: Int,
    caption: String,
    initial: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit,
) {
    var text by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Pose stiker $number", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Text(
                    (if (caption.isNotBlank()) "Kata: \"${caption.trim()}\"\n" else "") +
                        "Tulis gaya yang kamu mau. Kosongkan kalau AI boleh memilih sendiri.",
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it.take(120) },
                    placeholder = { Text("contoh: menutup hidung sambil mengernyit", fontSize = 13.sp) },
                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 14.sp),
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(10.dp))
                Text("Ide cepat", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(6.dp))
                androidx.compose.foundation.layout.FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    poseIdeas.forEach { idea ->
                        Text(
                            idea,
                            fontSize = 12.sp,
                            color = LumiBlue,
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(LumiBlue.copy(alpha = 0.08f))
                                .border(1.dp, LumiBlue.copy(alpha = 0.3f), RoundedCornerShape(50))
                                .clickable { text = idea }
                                .padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = { onSave(text) }) { Text("Simpan", fontWeight = FontWeight.Bold) } },
        dismissButton = {
            if (initial.isNotBlank()) TextButton(onClick = { onSave("") }) { Text("Hapus pose") }
            else TextButton(onClick = onDismiss) { Text("Batal") }
        }
    )
}

@Composable
private fun ChoiceCard(
    emoji: String,
    title: String,
    caption: String,
    selected: Boolean,
    modifier: Modifier,
    onClick: () -> Unit,
) {
    Row(
        modifier
            .clip(RoundedCornerShape(14.dp))
            .background(if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface)
            .border(
                if (selected) 2.dp else 1.dp,
                if (selected) LumiBlue else MaterialTheme.colorScheme.outline,
                RoundedCornerShape(14.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(emoji, fontSize = 22.sp)
        Spacer(Modifier.width(10.dp))
        Column {
            Text(title, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            Text(caption, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/* ---------------------------------------------------------------- langkah 5: buat */

private val loadingLines = listOf(
    "Mempelajari wajah dari fotomu…",
    "Menggambar ekspresi lucu…",
    "Menyusun semua stiker dalam 1 halaman…",
    "Hampir selesai, sabar ya…",
)

@Composable
private fun GenerateStep(vm: MainViewModel) {
    if (vm.generating) {
        var line by remember { mutableIntStateOf(0) }
        LaunchedEffect(Unit) {
            while (true) {
                delay(3500)
                line = (line + 1) % loadingLines.size
            }
        }
        Column(
            Modifier
                .fillMaxWidth()
                .padding(top = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CircularProgressIndicator(Modifier.size(64.dp), strokeWidth = 5.dp)
            Spacer(Modifier.height(24.dp))
            Text(loadingLines[line], fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
            Spacer(Modifier.height(8.dp))
            Text(
                "Biasanya 15–60 detik. Kamu boleh meninggalkan aplikasi, nanti ada notifikasi saat stiker sudah jadi.",
                fontSize = 13.sp,
                lineHeight = 19.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp)
            )
        }
        return
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(horizontal = 30.dp)) {
                vm.photos.mapNotNull { it?.image }.forEach { t ->
                    Box(
                        Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(LumiTile)
                    ) {
                        Image(t, null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                    }
                }
                // jaga ukuran thumbnail tetap sama walau fotonya hanya 1 atau 2
                repeat(3 - vm.photoCount) { Spacer(Modifier.weight(1f)) }
            }
            Spacer(Modifier.height(6.dp))
            SummaryRow(
                "Wajah",
                if (vm.peopleCount >= 2) "2 orang (stiker berdua)" else "1 orang",
                Step.FACES,
                vm
            )
            HorizontalDivider()
            SummaryRow("Jumlah", "${vm.count} stiker", Step.COUNT, vm)
            HorizontalDivider()
            SummaryRow("Tema", vm.style?.let { "${it.emoji} ${it.label}" } ?: "-", Step.STYLE, vm)
            HorizontalDivider()
            SummaryRow(
                "Tulisan",
                if (vm.useText) vm.activeWords.joinToString(" • ") else "Tanpa kata",
                Step.TEXT,
                vm
            )
        }
    }
    Spacer(Modifier.height(10.dp))
    InfoCard("AI membuat semua stiker dalam 1 gambar saja (hemat token), lalu aplikasi memotongnya otomatis.")
}

@Composable
private fun SummaryRow(label: String, value: String, target: Step, vm: MainViewModel) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable { vm.goTo(target) }
            .padding(vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.width(80.dp))
        Text(value, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, maxLines = 2, modifier = Modifier.weight(1f))
        Text("Ubah", color = LumiBlue, fontSize = 13.sp)
    }
}

/* ---------------------------------------------------------------- hasil */

@Composable
private fun ResultStep(vm: MainViewModel) {
    val context = LocalContext.current
    var showRegenDialog by remember { mutableStateOf(false) }

    if (showRegenDialog) {
        RegenerateDialog(
            vm = vm,
            onDismiss = { showRegenDialog = false },
            onConfirm = { captions ->
                showRegenDialog = false
                vm.regenerateSelected(captions)
            }
        )
    }
    val waLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { res ->
        if (res.resultCode == Activity.RESULT_OK) {
            vm.notice = "Paket berhasil ditambahkan ke WhatsApp 🎉"
        } else {
            val reason = res.data?.getStringExtra("validation_error")
            if (reason != null) vm.error = "WhatsApp menolak paket ini:\n$reason"
            else if (vm.targetPackId != null) {
                vm.notice = "Stiker sudah disimpan ke paket. Kalau paket itu sudah ada di WhatsApp, " +
                    "stiker baru muncul otomatis (tutup & buka lagi WhatsApp kalau belum terlihat)."
            }
        }
    }

    // Saat mode Ganti sebagian aktif, kotak info disembunyikan agar baris tombol tambahan
    // tetap muat dalam satu layar tanpa scroll.
    if (!vm.selecting) vm.notice?.let {
        InfoCard(it)
        Spacer(Modifier.height(12.dp))
    }

    if (vm.regenerating > 0) {
        Row(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(MaterialTheme.colorScheme.primaryContainer)
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 3.dp)
            Spacer(Modifier.width(12.dp))
            Text(
                "Membuat ulang ${vm.regenerating} stiker… (15–60 detik)",
                modifier = Modifier.weight(1f),
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            TextButton(onClick = { vm.cancel() }) { Text("Batal") }
        }
        Spacer(Modifier.height(12.dp))
    }

    // ---- baris kontrol: mode centang (satu baris ringkas)
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            if (vm.selecting) "Centang yang mau diganti" else "✏️ Ketuk stiker untuk merapikan",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            modifier = Modifier.weight(1f)
        )
        if (vm.selecting) {
            SmallChip("Semua", enabled = !vm.generating) { vm.selectAll() }
            Spacer(Modifier.width(6.dp))
            SmallChip("Batal", enabled = !vm.generating) { vm.stopSelecting() }
        } else {
            SmallChip("☑ Ganti sebagian", enabled = !vm.generating, filled = true) { vm.startSelecting() }
        }
    }
    Spacer(Modifier.height(8.dp))

    // 5 stiker per baris: 10 stiker cukup 2 baris
    vm.stickers.withIndex().toList().chunked(5).forEach { row ->
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            row.forEach { (index, bmp) ->
                val checked = index in vm.selection
                Box(
                    Modifier
                        .weight(1f)
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (checked) MaterialTheme.colorScheme.primaryContainer else LumiTile)
                        .border(
                            if (checked) 2.dp else 0.dp,
                            if (checked) LumiBlue else Color.Transparent,
                            RoundedCornerShape(12.dp)
                        )
                        .clickable(enabled = !vm.generating) {
                            if (vm.selecting) vm.toggleSelection(index) else vm.editingIndex = index
                        }
                        .padding(3.dp)
                ) {
                    Image(bmp, contentDescription = "Stiker ${index + 1}", modifier = Modifier.fillMaxSize())
                    if (vm.selecting) {
                        Box(
                            Modifier
                                .align(Alignment.TopEnd)
                                .size(18.dp)
                                .clip(CircleShape)
                                .background(if (checked) LumiBlue else Color.White.copy(alpha = 0.9f))
                                .border(1.5.dp, LumiBlue, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            if (checked) {
                                Icon(Icons.Filled.Check, null, tint = Color.White, modifier = Modifier.size(12.dp))
                            }
                        }
                    }
                }
            }
            repeat(5 - row.size) { Spacer(Modifier.weight(1f)) }
        }
        Spacer(Modifier.height(6.dp))
    }

    if (vm.selecting) {
        val n = vm.selection.size
        Row(
            Modifier
                .fillMaxWidth()
                .padding(top = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                if (n == 0) "Belum dicentang" else "$n dicentang • 1x biaya",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            OutlinedButton(
                onClick = { showRegenDialog = true },
                enabled = n > 0 && !vm.generating,
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp),
                modifier = Modifier.height(38.dp)
            ) { Text("✏️ Ubah kata", maxLines = 1, fontSize = 13.sp) }
            Button(
                onClick = { vm.regenerateSelected(emptyMap()) },
                enabled = n > 0 && !vm.generating,
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp),
                modifier = Modifier.height(38.dp)
            ) { Text("🔄 Buat ulang", maxLines = 1, fontSize = 13.sp) }
        }
        Spacer(Modifier.height(4.dp))
    }

    // ---- pilih paket tujuan (dua pilihan bersebelahan)
    Spacer(Modifier.height(6.dp))
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text("Simpan ke paket", fontWeight = FontWeight.Bold, fontSize = 15.sp, modifier = Modifier.weight(1f))
        if (vm.sheet != null) {
            Text(
                "Lihat gambar asli AI",
                color = LumiBlue,
                fontSize = 12.sp,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { vm.viewingSheet = true }
                    .padding(horizontal = 6.dp, vertical = 6.dp)
            )
        }
    }
    Spacer(Modifier.height(6.dp))
    val hasPacks = vm.existingPacks.isNotEmpty()
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        PackChip(
            text = "📦 Paket baru",
            selected = vm.targetPackId == null,
            enabled = true,
            modifier = Modifier.weight(1f)
        ) { vm.targetPackId = null }
        PackChip(
            text = if (hasPacks) "➕ Paket lama" else "➕ Belum ada paket",
            selected = vm.targetPackId != null,
            enabled = hasPacks,
            modifier = Modifier.weight(1f)
        ) {
            // pilih otomatis paket lama pertama yang masih muat
            vm.targetPackId = vm.existingPacks.firstOrNull { vm.stickers.size <= vm.freeSlots(it) }?.id
                ?: vm.existingPacks.first().id
        }
    }
    Spacer(Modifier.height(8.dp))
    if (vm.targetPackId == null) {
        CompactField(
            value = vm.packName,
            onChange = { vm.packName = it.take(60) },
            label = "Nama paket"
        )
    } else {
        vm.existingPacks.forEach { pack ->
            val free = vm.freeSlots(pack)
            val fits = vm.stickers.size <= free
            val currentCount = PackStore.MAX_PER_PACK - free
            PackOption(
                selected = vm.targetPackId == pack.id,
                enabled = fits,
                title = pack.name,
                subtitle = if (fits) "$currentCount → ${currentCount + vm.stickers.size} stiker"
                else "Penuh: sisa $free tempat (maks. ${PackStore.MAX_PER_PACK})",
                onClick = { vm.targetPackId = pack.id }
            )
        }
    }

    Spacer(Modifier.height(10.dp))
    Button(
        onClick = {
            if (!WhatsAppHelper.isAnyInstalled(context)) {
                vm.error = "WhatsApp belum terpasang di HP ini."
                return@Button
            }
            vm.commitToPack { id, name ->
                try {
                    waLauncher.launch(WhatsAppHelper.addPackIntent(context, id, name))
                } catch (e: ActivityNotFoundException) {
                    vm.error = "Versi WhatsApp kamu belum mendukung stiker dari aplikasi lain. Coba perbarui WhatsApp."
                }
            }
        },
        enabled = !vm.generating,
        colors = ButtonDefaults.buttonColors(containerColor = WhatsAppGreen),
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
    ) {
        Text(
            if (vm.targetPackId == null) "Tambahkan ke WhatsApp" else "Tambahkan ke paket & WhatsApp",
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp
        )
    }
    Spacer(Modifier.height(8.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        OutlinedButton(
            onClick = {
                vm.goTo(Step.GENERATE)
                vm.generate()
            },
            enabled = !vm.generating,
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp),
            modifier = Modifier
                .weight(1f)
                .height(44.dp)
        ) {
            Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text("Ulang semua", maxLines = 1, fontSize = 14.sp)
        }
        OutlinedButton(
            onClick = { vm.restart() },
            enabled = !vm.generating,
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp),
            modifier = Modifier
                .weight(1f)
                .height(44.dp)
        ) { Text("Mulai baru", maxLines = 1, fontSize = 14.sp) }
    }
}

/** Tombol kecil berbentuk pil. */
@Composable
private fun SmallChip(text: String, enabled: Boolean, filled: Boolean = false, onClick: () -> Unit) {
    Box(
        Modifier
            .height(32.dp)
            .clip(RoundedCornerShape(50))
            .background(if (filled) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
            .border(if (filled) 0.dp else 1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(50))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.Center
    ) { Text(text, fontSize = 13.sp, color = LumiBlue, fontWeight = FontWeight.SemiBold, maxLines = 1) }
}

/** Pilihan paket (baru / lama) berbentuk tombol bersebelahan. */
@Composable
private fun PackChip(text: String, selected: Boolean, enabled: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Box(
        modifier
            .height(42.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface)
            .border(
                if (selected) 2.dp else 1.dp,
                if (selected) LumiBlue else MaterialTheme.colorScheme.outline,
                RoundedCornerShape(12.dp)
            )
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
        )
    }
}

/** Kolom isian ringkas (tinggi 46dp) dengan label di kiri. */
@Composable
private fun CompactField(value: String, onChange: (String) -> Unit, label: String) {
    Row(
        Modifier
            .fillMaxWidth()
            .height(46.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.width(10.dp))
        androidx.compose.foundation.text.BasicTextField(
            value = value,
            onValueChange = onChange,
            singleLine = true,
            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurface),
            cursorBrush = androidx.compose.ui.graphics.SolidColor(LumiBlue),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun PackOption(
    selected: Boolean,
    enabled: Boolean,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(
                when {
                    selected -> MaterialTheme.colorScheme.primaryContainer
                    else -> MaterialTheme.colorScheme.surface
                }
            )
            .border(
                if (selected) 2.dp else 1.dp,
                if (selected) LumiBlue else MaterialTheme.colorScheme.outline,
                RoundedCornerShape(14.dp)
            )
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .size(20.dp)
                .clip(CircleShape)
                .border(2.dp, if (enabled) LumiBlue else MaterialTheme.colorScheme.outline, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            if (selected) {
                Box(
                    Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(LumiBlue)
                )
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                title,
                fontWeight = FontWeight.SemiBold,
                color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(subtitle, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/** Dialog ubah kata untuk stiker yang dicentang, lalu buat ulang. */
@Composable
private fun RegenerateDialog(
    vm: MainViewModel,
    onDismiss: () -> Unit,
    onConfirm: (Map<Int, String>) -> Unit,
) {
    val targets = remember { vm.selection.sorted() }
    val values = remember {
        mutableStateMapOf<Int, String>().apply {
            targets.forEach { put(it, vm.stickerCaptions.getOrElse(it) { "" }) }
        }
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Ubah kata") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                Text(
                    "Tulis kata baru untuk tiap stiker yang dicentang. Kosongkan kalau mau tanpa kata.",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(8.dp))
                targets.forEach { i ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        vm.stickers.getOrNull(i)?.let {
                            Image(
                                it,
                                contentDescription = null,
                                modifier = Modifier
                                    .size(52.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(LumiTile)
                            )
                        }
                        Spacer(Modifier.width(10.dp))
                        OutlinedTextField(
                            value = values[i].orEmpty(),
                            onValueChange = { values[i] = it.take(25) },
                            label = { Text("Stiker ${i + 1}") },
                            singleLine = true,
                            trailingIcon = {
                                IconButton(onClick = {
                                    val others = vm.stickerCaptions.filterIndexed { j, _ -> j != i } +
                                        values.filterKeys { it != i }.values
                                    values[i] = vm.randomCaption(others)
                                }) { Text("🎲", fontSize = 20.sp) }
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(values.toMap()) }) { Text("Buat ulang ${targets.size} stiker") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Batal") } },
    )
}

/* ---------------------------------------------------------------- komponen kecil */

@Composable
private fun InfoCard(text: String) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.primaryContainer)
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Text("💡", fontSize = 14.sp)
        Spacer(Modifier.width(8.dp))
        Text(text, fontSize = 13.sp, lineHeight = 18.sp, color = MaterialTheme.colorScheme.onPrimaryContainer)
    }
}

@Composable
private fun TipCard(title: String, items: List<String>) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(16.dp)
    ) {
        Text(title, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        items.forEach {
            Row(Modifier.padding(vertical = 3.dp)) {
                Text("•  ", color = LumiBlue, fontWeight = FontWeight.Bold)
                Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun SettingsDialog(settings: AppSettings, onClose: () -> Unit) {
    var key by remember { mutableStateOf(settings.apiKey) }
    var cfAccount by remember { mutableStateOf(settings.cfAccountId) }
    var cfToken by remember { mutableStateOf(settings.cfToken) }
    var model by remember { mutableStateOf(settings.model) }
    var publisher by remember { mutableStateOf(settings.publisher) }
    var reveal by remember { mutableStateOf(false) }
    val uri = LocalUriHandler.current
    val provider = AppSettings.providerOf(model)
    val hidden = if (reveal) VisualTransformation.None else PasswordVisualTransformation()

    AlertDialog(
        onDismissRequest = onClose,
        title = { Text("Pengaturan AI") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                Text("Pilih model AI", fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(6.dp))
                AppSettings.MODELS.forEach { m ->
                    val selected = model == m.id
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant)
                            .border(
                                if (selected) 2.dp else 0.dp,
                                if (selected) LumiBlue else Color.Transparent,
                                RoundedCornerShape(12.dp)
                            )
                            .clickable { model = m.id }
                            .padding(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(m.label, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                            if (m.free) {
                                Text(
                                    "GRATIS",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(WhatsAppGreen)
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(m.note, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                OutlinedTextField(
                    value = model,
                    onValueChange = { model = it },
                    label = { Text("ID model (bisa diisi manual)") },
                    supportingText = { Text("Diawali @cf/ = Cloudflare, lainnya = OpenRouter") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(12.dp))
                HorizontalDivider()
                Spacer(Modifier.height(12.dp))

                if (provider == Provider.OPENROUTER) {
                    Text("OpenRouter (berbayar)", fontWeight = FontWeight.Bold)
                    Text(
                        "Masukkan API key OpenRouter (diawali sk-or-). Isi kredit di openrouter.ai/credits.",
                        fontSize = 13.sp
                    )
                    TextButton(onClick = { uri.openUri(AppSettings.API_KEY_URL) }) {
                        Text("Buat API key di openrouter.ai →")
                    }
                    OutlinedTextField(
                        value = key,
                        onValueChange = { key = it },
                        label = { Text("OpenRouter API key") },
                        placeholder = { Text("sk-or-v1-…") },
                        singleLine = true,
                        visualTransformation = hidden,
                        trailingIcon = {
                            TextButton(onClick = { reveal = !reveal }) { Text(if (reveal) "Tutup" else "Lihat") }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    Text("Cloudflare Workers AI (gratis)", fontWeight = FontWeight.Bold)
                    Text(
                        "1) Daftar gratis di dash.cloudflare.com\n" +
                            "2) Salin Account ID (ada di halaman Workers AI / URL dashboard)\n" +
                            "3) Buat API Token dengan template \"Workers AI\"\n" +
                            "Kuota gratis reset tiap hari jam 07.00 WIB.",
                        fontSize = 13.sp
                    )
                    TextButton(onClick = { uri.openUri(AppSettings.CF_TOKEN_URL) }) {
                        Text("Buat API Token Cloudflare →")
                    }
                    OutlinedTextField(
                        value = cfAccount,
                        onValueChange = { cfAccount = it },
                        label = { Text("Account ID") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(6.dp))
                    OutlinedTextField(
                        value = cfToken,
                        onValueChange = { cfToken = it },
                        label = { Text("API Token") },
                        singleLine = true,
                        visualTransformation = hidden,
                        trailingIcon = {
                            TextButton(onClick = { reveal = !reveal }) { Text(if (reveal) "Tutup" else "Lihat") }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = publisher,
                    onValueChange = { publisher = it.take(60) },
                    label = { Text("Nama pembuat (tampil di WhatsApp)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                settings.apiKey = key
                settings.cfAccountId = cfAccount
                settings.cfToken = cfToken
                settings.model = model
                settings.publisher = publisher
                onClose()
            }) { Text("Simpan") }
        },
        dismissButton = { TextButton(onClick = onClose) { Text("Batal") } },
    )
}
