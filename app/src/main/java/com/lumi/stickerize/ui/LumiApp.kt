package com.lumi.stickerize.ui

import android.Manifest
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
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lumi.stickerize.R
import com.lumi.stickerize.ai.ErrorKind
import com.lumi.stickerize.data.AppSettings
import com.lumi.stickerize.data.Provider
import com.lumi.stickerize.model.StickerStyle
import com.lumi.stickerize.sticker.PackStore
import com.lumi.stickerize.ui.theme.LumiBlue
import com.lumi.stickerize.ui.theme.LumiGradient
import com.lumi.stickerize.ui.theme.LumiTile
import com.lumi.stickerize.ui.theme.WhatsAppGreen
import com.lumi.stickerize.util.GallerySaver
import com.lumi.stickerize.whatsapp.WhatsAppHelper
import kotlinx.coroutines.delay

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LumiApp(vm: MainViewModel) {
    var showSettings by remember { mutableStateOf(false) }

    LaunchedEffect(vm.askForKey) {
        if (vm.askForKey) {
            vm.askForKey = false
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
            Header(
                step = vm.step,
                compact = imeVisible,
                onPacks = { vm.openManager() },
                onSettings = { showSettings = true }
            )

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
        if (vm.themePickerOpen) {
            ThemePackScreen(
                packs = remember { vm.themePacks() },
                count = vm.count,
                selected = vm.appliedTheme,
                onPick = {
                    vm.applyTheme(it)
                    vm.themePickerOpen = false
                },
                onClear = {
                    vm.clearTheme()
                    vm.themePickerOpen = false
                },
                onClose = { vm.themePickerOpen = false }
            )
        }
        if (vm.managerOpen) {
            PackManagerScreen(vm)
        }
    }

    if (showSettings) {
        SettingsDialog(settings = vm.settings, onClose = { showSettings = false })
    }

    vm.error?.let { err -> ErrorDialog(vm, err, onOpenSettings = { showSettings = true }) }
}

/** Layar error yang menjelaskan masalah dan menawarkan langkah berikutnya. */
@Composable
private fun ErrorDialog(vm: MainViewModel, err: AppError, onOpenSettings: () -> Unit) {
    val uri = LocalUriHandler.current
    val title = when (err.kind) {
        ErrorKind.AUTH -> stringResource(R.string.err_title_auth)
        ErrorKind.CREDIT -> stringResource(R.string.err_title_credit)
        ErrorKind.QUOTA -> stringResource(R.string.err_title_quota)
        ErrorKind.MODEL -> stringResource(R.string.err_title_model)
        ErrorKind.NETWORK -> stringResource(R.string.err_title_network)
        ErrorKind.BUSY -> stringResource(R.string.err_title_busy)
        ErrorKind.BLOCKED -> stringResource(R.string.err_title_blocked)
        ErrorKind.OTHER -> stringResource(R.string.err_title_other)
    }
    val emoji = when (err.kind) {
        ErrorKind.AUTH -> "🔑"
        ErrorKind.CREDIT -> "💳"
        ErrorKind.QUOTA -> "⏳"
        ErrorKind.MODEL -> "🤖"
        ErrorKind.NETWORK -> "📶"
        ErrorKind.BUSY -> "🕐"
        ErrorKind.BLOCKED -> "🛡️"
        ErrorKind.OTHER -> "😅"
    }
    AlertDialog(
        onDismissRequest = { vm.error = null },
        icon = { Text(emoji, fontSize = 30.sp) },
        title = { Text(title, textAlign = TextAlign.Center) },
        text = {
            Text(
                err.message,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                modifier = Modifier
                    .heightIn(max = 320.dp)
                    .verticalScroll(rememberScrollState())
            )
        },
        confirmButton = {
            when (err.kind) {
                ErrorKind.AUTH, ErrorKind.MODEL, ErrorKind.QUOTA -> TextButton(onClick = {
                    vm.error = null
                    onOpenSettings()
                }) { Text(stringResource(R.string.action_open_settings), fontWeight = FontWeight.Bold) }
                ErrorKind.CREDIT -> TextButton(onClick = {
                    vm.error = null
                    uri.openUri(AppSettings.CREDITS_URL)
                }) { Text(stringResource(R.string.action_top_up), fontWeight = FontWeight.Bold) }
                else -> if (err.canRetry) TextButton(onClick = { vm.retry() }) {
                    Text(stringResource(R.string.action_try_again), fontWeight = FontWeight.Bold)
                } else TextButton(onClick = { vm.error = null }) { Text(stringResource(R.string.ok)) }
            }
        },
        dismissButton = {
            val showClose = err.kind in setOf(ErrorKind.AUTH, ErrorKind.MODEL, ErrorKind.QUOTA, ErrorKind.CREDIT) || err.canRetry
            if (showClose) TextButton(onClick = { vm.error = null }) { Text(stringResource(R.string.close)) }
        },
    )
}

/* ---------------------------------------------------------------- header & navigasi */

@Composable
private fun Header(step: Step, compact: Boolean, onPacks: () -> Unit, onSettings: () -> Unit) {
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
                stringResource(R.string.app_name),
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp,
                maxLines = 1,
                modifier = Modifier.weight(1f)
            )
            // Paket saya
            Box(
                Modifier
                    .clip(RoundedCornerShape(50))
                    .background(Color.White.copy(alpha = 0.18f))
                    .clickable(onClick = onPacks)
                    .padding(horizontal = 10.dp, vertical = 5.dp)
            ) {
                Text("📦 " + stringResource(R.string.my_packs), color = Color.White, fontSize = 12.sp, maxLines = 1)
            }
            IconButton(onClick = onSettings) {
                Icon(Icons.Filled.Settings, contentDescription = stringResource(R.string.settings), tint = Color.White)
            }
        }
        // saat keyboard terbuka, header diperkecil supaya kolom isian tetap kelihatan
        if (compact) return@Column
        Row(verticalAlignment = Alignment.Bottom, modifier = Modifier.padding(end = 12.dp)) {
            Text(
                stringResource(step.title),
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 21.sp,
                modifier = Modifier.weight(1f)
            )
            Text(
                if (step == Step.RESULT) stringResource(R.string.done)
                else stringResource(R.string.step_of, step.ordinal + 1, Step.entries.size - 1),
                color = Color.White.copy(alpha = 0.85f),
                fontSize = 12.sp,
                modifier = Modifier.padding(bottom = 4.dp)
            )
        }
        Text(stringResource(step.subtitle), color = Color.White.copy(alpha = 0.9f), fontSize = 13.sp)
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
                ) { Text(stringResource(R.string.back), maxLines = 1) }
            }
            if (vm.step == Step.GENERATE) {
                if (vm.generating) {
                    OutlinedButton(
                        onClick = { vm.cancel() },
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp)
                    ) { Text(stringResource(R.string.cancel_generation), maxLines = 1) }
                } else {
                    Button(
                        onClick = { vm.generate() },
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp)
                    ) { Text("✨  " + stringResource(R.string.create_stickers), fontWeight = FontWeight.Bold, maxLines = 1) }
                }
            } else {
                Button(
                    onClick = { vm.next() },
                    enabled = vm.canGoNext(),
                    modifier = Modifier
                        .weight(1f)
                        .height(46.dp)
                ) { Text(stringResource(R.string.next), fontWeight = FontWeight.Bold, maxLines = 1) }
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

    InfoCard(stringResource(R.string.photos_info))
    Spacer(Modifier.height(8.dp))
    WarningCard(stringResource(R.string.photos_outfit_title), stringResource(R.string.photos_outfit_text))
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
        Text(stringResource(R.string.photos_pick_many), maxLines = 1)
    }
    Spacer(Modifier.height(6.dp))
    Text(
        when (vm.photoCount) {
            0 -> stringResource(R.string.photos_count_0)
            1 -> stringResource(R.string.photos_count_1)
            2 -> stringResource(R.string.photos_count_2)
            else -> stringResource(R.string.photos_count_3)
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
                    contentDescription = stringResource(R.string.photo_n, index + 1),
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
                        contentDescription = stringResource(R.string.remove),
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
                    if (index == 0) stringResource(R.string.photo_main) else stringResource(R.string.photo_n, index + 1),
                    color = LumiBlue,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    maxLines = 1,
                    textAlign = TextAlign.Center
                )
                Text(
                    if (index == 0) stringResource(R.string.required) else stringResource(R.string.optional),
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
    InfoCard(stringResource(R.string.faces_info))
    Spacer(Modifier.height(10.dp))
    val info = vm.photos[0] ?: return
    val sel = vm.selectedFaces
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(stringResource(R.string.photo_main), fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
        Text(
            if (info.faces.isEmpty()) stringResource(R.string.faces_none)
            else stringResource(R.string.faces_count, info.faces.size, sel.size),
            fontSize = 13.sp,
            color = if (info.faces.isEmpty()) Color(0xFFD14343) else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
    Spacer(Modifier.height(6.dp))
    FacePhoto(info = info, selected = sel, onToggle = { vm.toggleFace(it) })
    Spacer(Modifier.height(8.dp))
    when {
        info.faces.isEmpty() -> Text(
            stringResource(R.string.faces_not_found),
            fontSize = 13.sp,
            color = Color(0xFFD14343)
        )
        info.faces.size == 2 && sel.size < 2 -> OutlinedButton(
            onClick = { vm.selectAllFaces() },
            modifier = Modifier.fillMaxWidth()
        ) { Text(stringResource(R.string.faces_pick_both), maxLines = 1) }
        info.faces.size > 2 -> Text(
            stringResource(R.string.faces_many, info.faces.size),
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        sel.isEmpty() -> Text(stringResource(R.string.faces_tap), fontSize = 13.sp, color = LumiBlue)
    }
    if (sel.isNotEmpty()) {
        Spacer(Modifier.height(12.dp))
        InfoCard(
            (if (sel.size >= 2) stringResource(R.string.faces_two) else stringResource(R.string.faces_one)) +
                (if (vm.photoCount > 1) " " + stringResource(R.string.faces_extra_photos) else "")
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
                Text(stringResource(R.string.stickers_word), color = MaterialTheme.colorScheme.onSurfaceVariant)
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
    InfoCard(stringResource(R.string.count_info))
}

@Composable
private fun RoundButton(label: String, enabled: Boolean, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        enabled = enabled,
        shape = CircleShape,
        modifier = Modifier.size(48.dp),
        contentPadding = PaddingValues(0.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = LumiBlue
        )
    ) { Text(label, fontSize = 26.sp, fontWeight = FontWeight.Bold) }
}

/* ---------------------------------------------------------------- langkah 4: tema */

@Composable
private fun StyleStep(vm: MainViewModel) {
    // 20 gaya dalam 4 kolom × 5 baris supaya muat satu layar
    EmojiTileGrid(
        items = StickerStyle.entries.map { Triple(it.name, it.emoji, stringResource(it.label)) },
        selected = vm.style?.name,
        onPick = { name -> vm.style = StickerStyle.valueOf(name) }
    )
}

/**
 * Grid kotak kecil (emoji di atas, nama di bawah), 4 per baris.
 * Dipakai untuk 20 gaya gambar dan 20 paket tema agar muat satu layar tanpa scroll.
 */
@Composable
internal fun EmojiTileGrid(
    items: List<Triple<String, String, String>>,
    selected: String?,
    onPick: (String) -> Unit,
    columns: Int = 4,
    tileHeight: Int = 74,
) {
    items.chunked(columns).forEach { row ->
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            row.forEach { (id, emoji, label) ->
                val isSel = id == selected
                Column(
                    Modifier
                        .weight(1f)
                        .height(tileHeight.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (isSel) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface)
                        .border(
                            if (isSel) 2.dp else 1.dp,
                            if (isSel) LumiBlue else MaterialTheme.colorScheme.outline,
                            RoundedCornerShape(14.dp)
                        )
                        .clickable { onPick(id) }
                        .padding(horizontal = 4.dp, vertical = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(emoji, fontSize = 22.sp)
                    Spacer(Modifier.height(3.dp))
                    Text(
                        label,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        lineHeight = 13.sp,
                        maxLines = 2,
                        textAlign = TextAlign.Center,
                        overflow = TextOverflow.Ellipsis,
                        color = if (isSel) LumiBlue else MaterialTheme.colorScheme.onSurface
                    )
                }
            }
            repeat(columns - row.size) { Spacer(Modifier.weight(1f)) }
        }
        Spacer(Modifier.height(8.dp))
    }
}

/* ---------------------------------------------------------------- langkah 5: kata */

@Composable
private fun TextStep(vm: MainViewModel) {
    var poseFor by remember { mutableStateOf<Int?>(null) }
    val ideas = stringResource(R.string.word_placeholders).split("|")
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
        ChoiceCard("🙂", stringResource(R.string.text_none), stringResource(R.string.text_none_sub), !vm.useText, Modifier.weight(1f)) { vm.useText = false }
        ChoiceCard("💬", stringResource(R.string.text_use), stringResource(R.string.text_use_sub, vm.count), vm.useText, Modifier.weight(1f)) { vm.useText = true }
    }
    Spacer(Modifier.height(10.dp))
    // Paket tema siap pakai (kata + pose) dan acak semua
    val applied = vm.appliedTheme?.let { id -> vm.themePacks().firstOrNull { it.id == id } }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        FilledTonalButton(
            onClick = { vm.themePickerOpen = true },
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
            modifier = Modifier
                .weight(1.25f)
                .height(40.dp)
        ) {
            Text(
                if (applied != null) "${applied.emoji} ${applied.name}" else "🎁 " + stringResource(R.string.theme_packs),
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        OutlinedButton(
            onClick = { vm.useText = true; vm.randomAllWords() },
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
            modifier = Modifier
                .weight(1f)
                .height(40.dp)
        ) { Text("🎲 " + stringResource(R.string.random_all), fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis) }
    }
    if (vm.useText) {
        Spacer(Modifier.height(8.dp))
        // 2 kolom supaya 10 kata tetap muat satu layar
        (0 until vm.count).chunked(2).forEach { pair ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                pair.forEach { i ->
                    WordField(
                        value = vm.words[i],
                        onChange = { vm.words[i] = it.take(25) },
                        number = i + 1,
                        placeholder = ideas[i % ideas.size],
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
    } else {
        Spacer(Modifier.height(10.dp))
        InfoCard(stringResource(R.string.text_none_info))
    }
}

/** Kolom kata ringkas: nomor stiker, isian, baris pose, dan tombol 🎲. */
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
                BasicTextField(
                    value = value,
                    onValueChange = onChange,
                    singleLine = true,
                    textStyle = TextStyle(fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface),
                    cursorBrush = SolidColor(LumiBlue),
                    keyboardOptions = KeyboardOptions(imeAction = if (last) ImeAction.Done else ImeAction.Next),
                    modifier = Modifier.fillMaxWidth()
                )
            }
            // baris pose opsional di bawah kata
            Text(
                if (pose.isBlank()) "+ " + stringResource(R.string.pose) else "🧍 $pose",
                fontSize = 11.sp,
                lineHeight = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (pose.isBlank()) LumiBlue.copy(alpha = 0.75f) else LumiBlue,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
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

/** Jendela kecil untuk mengisi pose opsional satu stiker. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PoseDialog(
    number: Int,
    caption: String,
    initial: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit,
) {
    var text by remember { mutableStateOf(initial) }
    val ideas = stringResource(R.string.pose_ideas).split("|")
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.pose_title, number), fontWeight = FontWeight.Bold) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                Text(
                    (if (caption.isNotBlank()) stringResource(R.string.pose_caption, caption.trim()) + "\n" else "") +
                        stringResource(R.string.pose_help),
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it.take(120) },
                    placeholder = { Text(stringResource(R.string.pose_placeholder), fontSize = 13.sp) },
                    textStyle = TextStyle(fontSize = 14.sp),
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(10.dp))
                Text(stringResource(R.string.quick_ideas), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(6.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    ideas.forEach { idea ->
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
        confirmButton = { TextButton(onClick = { onSave(text) }) { Text(stringResource(R.string.save), fontWeight = FontWeight.Bold) } },
        dismissButton = {
            if (initial.isNotBlank()) TextButton(onClick = { onSave("") }) { Text(stringResource(R.string.pose_remove)) }
            else TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
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
            Text(title, fontWeight = FontWeight.Bold, fontSize = 15.sp, maxLines = 1)
            Text(caption, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
        }
    }
}

/* ---------------------------------------------------------------- langkah 6: buat */

@Composable
private fun GenerateStep(vm: MainViewModel) {
    if (vm.generating) {
        val lines = listOf(
            stringResource(R.string.loading_1),
            stringResource(R.string.loading_2),
            stringResource(R.string.loading_3),
            stringResource(R.string.loading_4),
        )
        var line by remember { mutableIntStateOf(0) }
        LaunchedEffect(Unit) {
            while (true) {
                delay(3500)
                line = (line + 1) % lines.size
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
            Text(lines[line], fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
            Spacer(Modifier.height(8.dp))
            Text(
                stringResource(R.string.loading_note),
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
                stringResource(R.string.sum_faces),
                if (vm.peopleCount >= 2) stringResource(R.string.sum_two_people) else stringResource(R.string.sum_one_person),
                Step.FACES,
                vm
            )
            HorizontalDivider()
            SummaryRow(stringResource(R.string.sum_count), stringResource(R.string.n_stickers, vm.count), Step.COUNT, vm)
            HorizontalDivider()
            SummaryRow(
                stringResource(R.string.sum_style),
                vm.style?.let { "${it.emoji} ${stringResource(it.label)}" } ?: "-",
                Step.STYLE,
                vm
            )
            HorizontalDivider()
            SummaryRow(
                stringResource(R.string.sum_text),
                if (vm.useText) vm.activeWords.joinToString(" • ") else stringResource(R.string.text_none),
                Step.TEXT,
                vm
            )
        }
    }
    Spacer(Modifier.height(10.dp))
    InfoCard(stringResource(R.string.generate_info))
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
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, modifier = Modifier.width(84.dp))
        Text(value, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, maxLines = 2, modifier = Modifier.weight(1f))
        Text(stringResource(R.string.change), color = LumiBlue, fontSize = 13.sp)
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
            onConfirm = { captions, poses ->
                showRegenDialog = false
                vm.regenerateSelected(captions, poses)
            }
        )
    }
    val waLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { res ->
        if (res.resultCode == Activity.RESULT_OK) {
            vm.notice = context.getString(R.string.wa_added)
        } else {
            val reason = res.data?.getStringExtra("validation_error")
            if (reason != null) vm.error = AppError(context.getString(R.string.wa_rejected, reason))
            else if (vm.targetPackId != null) vm.notice = context.getString(R.string.wa_saved_existing)
        }
    }
    // Android 9 ke bawah: minta izin penyimpanan dulu sebelum menyimpan ke Galeri
    val storagePermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
        if (ok) vm.saveToGallery() else vm.error = AppError(context.getString(R.string.err_gallery_permission))
    }
    val saveToGallery = {
        if (GallerySaver.needsPermission) storagePermission.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        else vm.saveToGallery()
    }

    // Saat mode Ganti sebagian aktif, kotak info disembunyikan agar baris tombol tambahan
    // tetap muat dalam satu layar tanpa scroll.
    if (!vm.selecting && vm.filling == 0 && vm.regenerating == 0) {
        if (vm.missing.isNotEmpty() && vm.stickers.size < MainViewModel.MAX_STICKERS) {
            val total = vm.stickers.size + vm.missing.size
            ActionCard(
                text = stringResource(R.string.missing_info, vm.stickers.size, total),
                action = stringResource(R.string.missing_fill, vm.missing.size),
                enabled = !vm.generating,
                onAction = { vm.fillMissing() }
            )
            Spacer(Modifier.height(10.dp))
        } else vm.notice?.let {
            InfoCard(it)
            Spacer(Modifier.height(10.dp))
        }
    }

    val busy = when {
        vm.regenerating > 0 -> stringResource(R.string.remaking_n, vm.regenerating)
        vm.filling > 0 -> stringResource(R.string.filling_n, vm.filling)
        else -> null
    }
    if (busy != null) {
        Row(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(MaterialTheme.colorScheme.primaryContainer)
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 3.dp)
            Spacer(Modifier.width(12.dp))
            Text(busy, modifier = Modifier.weight(1f), fontSize = 13.sp, color = MaterialTheme.colorScheme.onPrimaryContainer)
            TextButton(onClick = { vm.cancel() }) { Text(stringResource(R.string.cancel)) }
        }
        Spacer(Modifier.height(10.dp))
    }

    // ---- baris kontrol: mode centang (satu baris ringkas)
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            if (vm.selecting) stringResource(R.string.tick_to_replace) else "✏️ " + stringResource(R.string.tap_to_tidy),
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        if (vm.selecting) {
            SmallChip(stringResource(R.string.all), enabled = !vm.generating) { vm.selectAll() }
            Spacer(Modifier.width(6.dp))
            SmallChip(stringResource(R.string.cancel), enabled = !vm.generating) { vm.stopSelecting() }
        } else {
            SmallChip("⬇ " + stringResource(R.string.gallery), enabled = !vm.generating) { saveToGallery() }
            Spacer(Modifier.width(6.dp))
            SmallChip("☑ " + stringResource(R.string.replace_some), enabled = !vm.generating, filled = true) { vm.startSelecting() }
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
                    Image(bmp, contentDescription = stringResource(R.string.sticker_n, index + 1), modifier = Modifier.fillMaxSize())
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
                if (n == 0) stringResource(R.string.none_ticked) else stringResource(R.string.n_ticked, n),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            OutlinedButton(
                onClick = { showRegenDialog = true },
                enabled = n > 0 && !vm.generating,
                contentPadding = PaddingValues(horizontal = 10.dp),
                modifier = Modifier.height(38.dp)
            ) { Text("✏️ " + stringResource(R.string.caption_and_pose), maxLines = 1, fontSize = 13.sp) }
            Button(
                onClick = { vm.regenerateSelected(emptyMap()) },
                enabled = n > 0 && !vm.generating,
                contentPadding = PaddingValues(horizontal = 10.dp),
                modifier = Modifier.height(38.dp)
            ) { Text("🔄 " + stringResource(R.string.remake), maxLines = 1, fontSize = 13.sp) }
        }
        Spacer(Modifier.height(4.dp))
    }

    // ---- pilih paket tujuan (dua pilihan bersebelahan)
    Spacer(Modifier.height(6.dp))
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(stringResource(R.string.save_to_pack), fontWeight = FontWeight.Bold, fontSize = 15.sp, maxLines = 1, modifier = Modifier.weight(1f))
        if (vm.sheet != null) {
            Text(
                stringResource(R.string.view_ai_image),
                color = LumiBlue,
                fontSize = 12.sp,
                maxLines = 1,
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
            text = "📦 " + stringResource(R.string.new_pack),
            selected = vm.targetPackId == null,
            enabled = true,
            modifier = Modifier.weight(1f)
        ) { vm.targetPackId = null }
        PackChip(
            text = if (hasPacks) "➕ " + stringResource(R.string.existing_pack) else "➕ " + stringResource(R.string.no_packs_yet),
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
            label = stringResource(R.string.pack_name)
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
                subtitle = if (fits) stringResource(R.string.pack_after, currentCount, currentCount + vm.stickers.size)
                else stringResource(R.string.pack_full_left, free, PackStore.MAX_PER_PACK),
                onClick = { vm.targetPackId = pack.id }
            )
        }
    }

    Spacer(Modifier.height(10.dp))
    Button(
        onClick = {
            if (!WhatsAppHelper.isAnyInstalled(context)) {
                vm.error = AppError(context.getString(R.string.wa_not_installed))
                return@Button
            }
            vm.commitToPack { id, name ->
                try {
                    waLauncher.launch(WhatsAppHelper.addPackIntent(context, id, name))
                } catch (e: ActivityNotFoundException) {
                    vm.error = AppError(context.getString(R.string.wa_too_old))
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
            if (vm.targetPackId == null) stringResource(R.string.add_to_whatsapp) else stringResource(R.string.add_to_pack_whatsapp),
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            maxLines = 1
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
            contentPadding = PaddingValues(horizontal = 8.dp),
            modifier = Modifier
                .weight(1f)
                .height(44.dp)
        ) {
            Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text(stringResource(R.string.redo_all), maxLines = 1, fontSize = 14.sp)
        }
        OutlinedButton(
            onClick = { vm.restart() },
            enabled = !vm.generating,
            contentPadding = PaddingValues(horizontal = 8.dp),
            modifier = Modifier
                .weight(1f)
                .height(44.dp)
        ) { Text(stringResource(R.string.start_over), maxLines = 1, fontSize = 14.sp) }
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
            .padding(horizontal = 11.dp),
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
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
        )
    }
}

/** Kolom isian ringkas (tinggi 46dp) dengan label di kiri. */
@Composable
internal fun CompactField(value: String, onChange: (String) -> Unit, label: String) {
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
        Text(label, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
        Spacer(Modifier.width(10.dp))
        BasicTextField(
            value = value,
            onValueChange = onChange,
            singleLine = true,
            textStyle = TextStyle(fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurface),
            cursorBrush = SolidColor(LumiBlue),
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
            .background(if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface)
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
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(subtitle, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/** Dialog ubah kata & pose untuk stiker yang dicentang, lalu buat ulang. */
@Composable
private fun RegenerateDialog(
    vm: MainViewModel,
    onDismiss: () -> Unit,
    onConfirm: (Map<Int, String>, Map<Int, String>) -> Unit,
) {
    val targets = remember { vm.selection.sorted() }
    val values = remember {
        mutableStateMapOf<Int, String>().apply {
            targets.forEach { put(it, vm.stickerCaptions.getOrElse(it) { "" }) }
        }
    }
    val poseValues = remember {
        mutableStateMapOf<Int, String>().apply {
            targets.forEach { put(it, vm.stickerPoses.getOrElse(it) { "" }) }
        }
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.regen_title)) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                Text(
                    stringResource(R.string.regen_help),
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(10.dp))
                targets.forEach { i ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            vm.stickers.getOrNull(i)?.let {
                                Image(
                                    it,
                                    contentDescription = null,
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(LumiTile)
                                )
                            }
                            Text("${i + 1}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = LumiBlue)
                        }
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            DialogField(
                                value = values[i].orEmpty(),
                                onChange = { values[i] = it.take(25) },
                                prefix = "💬",
                                placeholder = stringResource(R.string.regen_caption_hint),
                                onDice = {
                                    val others = vm.stickerCaptions.filterIndexed { j, _ -> j != i } +
                                        values.filterKeys { it != i }.values
                                    values[i] = vm.randomCaption(others)
                                }
                            )
                            Spacer(Modifier.height(5.dp))
                            DialogField(
                                value = poseValues[i].orEmpty(),
                                onChange = { poseValues[i] = it.take(120) },
                                prefix = "🧍",
                                placeholder = stringResource(R.string.regen_pose_hint),
                                onDice = null
                            )
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(values.toMap(), poseValues.toMap()) }) {
                Text(stringResource(R.string.regen_confirm, targets.size))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}

/** Kolom isian ringkas (40dp) untuk jendela Ubah kata & pose. */
@Composable
private fun DialogField(
    value: String,
    onChange: (String) -> Unit,
    prefix: String,
    placeholder: String,
    onDice: (() -> Unit)?,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .height(40.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(10.dp))
            .padding(start = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(prefix, fontSize = 13.sp)
        Spacer(Modifier.width(6.dp))
        Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
            if (value.isEmpty()) {
                Text(
                    placeholder,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    maxLines = 1
                )
            }
            BasicTextField(
                value = value,
                onValueChange = onChange,
                singleLine = true,
                textStyle = TextStyle(fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface),
                cursorBrush = SolidColor(LumiBlue),
                modifier = Modifier.fillMaxWidth()
            )
        }
        if (onDice != null) {
            Box(
                Modifier
                    .size(38.dp)
                    .clickable(onClick = onDice),
                contentAlignment = Alignment.Center
            ) { Text("🎲", fontSize = 16.sp) }
        } else Spacer(Modifier.width(8.dp))
    }
}

/* ---------------------------------------------------------------- komponen kecil */

@Composable
internal fun InfoCard(text: String) {
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

/** Kotak info dengan satu tombol aksi di kanan (mis. "Lengkapi 2 stiker"). */
@Composable
private fun ActionCard(text: String, action: String, enabled: Boolean, onAction: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.primaryContainer)
            .padding(start = 12.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("💡", fontSize = 14.sp)
        Spacer(Modifier.width(8.dp))
        Text(
            text,
            fontSize = 12.sp,
            lineHeight = 16.sp,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.weight(1f)
        )
        Spacer(Modifier.width(8.dp))
        Button(
            onClick = onAction,
            enabled = enabled,
            contentPadding = PaddingValues(horizontal = 12.dp),
            modifier = Modifier.height(36.dp)
        ) { Text(action, fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 1) }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SettingsDialog(settings: AppSettings, onClose: () -> Unit) {
    var key by remember { mutableStateOf(settings.apiKey) }
    var cfAccount by remember { mutableStateOf(settings.cfAccountId) }
    var cfToken by remember { mutableStateOf(settings.cfToken) }
    var model by remember { mutableStateOf(settings.model) }
    var publisher by remember { mutableStateOf(settings.publisher) }
    var reveal by remember { mutableStateOf(false) }
    val uri = LocalUriHandler.current
    val context = LocalContext.current
    val provider = AppSettings.providerOf(model)
    val hidden = if (reveal) VisualTransformation.None else PasswordVisualTransformation()

    fun saveAll() {
        settings.apiKey = key
        settings.cfAccountId = cfAccount
        settings.cfToken = cfToken
        settings.model = model
        settings.publisher = publisher
    }

    AlertDialog(
        onDismissRequest = onClose,
        title = { Text(stringResource(R.string.settings_title)) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                Text(stringResource(R.string.choose_model), fontWeight = FontWeight.Bold)
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
                                    stringResource(R.string.free_badge),
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
                        Text(stringResource(m.note), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                OutlinedTextField(
                    value = model,
                    onValueChange = { model = it },
                    label = { Text(stringResource(R.string.model_id)) },
                    supportingText = { Text(stringResource(R.string.model_id_help)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(12.dp))
                HorizontalDivider()
                Spacer(Modifier.height(12.dp))

                if (provider == Provider.OPENROUTER) {
                    Text(stringResource(R.string.openrouter_paid), fontWeight = FontWeight.Bold)
                    Text(stringResource(R.string.openrouter_help), fontSize = 13.sp)
                    Row {
                        TextButton(onClick = { uri.openUri(AppSettings.API_KEY_URL) }) {
                            Text(stringResource(R.string.get_key) + " →")
                        }
                        TextButton(onClick = { uri.openUri(AppSettings.CREDITS_URL) }) {
                            Text(stringResource(R.string.action_top_up) + " →")
                        }
                    }
                    OutlinedTextField(
                        value = key,
                        onValueChange = { key = it },
                        label = { Text(stringResource(R.string.openrouter_key)) },
                        placeholder = { Text("sk-or-v1-…") },
                        singleLine = true,
                        visualTransformation = hidden,
                        trailingIcon = {
                            TextButton(onClick = { reveal = !reveal }) { Text(if (reveal) stringResource(R.string.hide) else stringResource(R.string.show)) }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    Text(stringResource(R.string.cloudflare_free), fontWeight = FontWeight.Bold)
                    Text(stringResource(R.string.cloudflare_help), fontSize = 13.sp)
                    TextButton(onClick = { uri.openUri(AppSettings.CF_TOKEN_URL) }) {
                        Text(stringResource(R.string.get_token) + " →")
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
                            TextButton(onClick = { reveal = !reveal }) { Text(if (reveal) stringResource(R.string.hide) else stringResource(R.string.show)) }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = publisher,
                    onValueChange = { publisher = it.take(60) },
                    label = { Text(stringResource(R.string.publisher)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                saveAll()
                onClose()
            }) { Text(stringResource(R.string.save)) }
        },
        dismissButton = { TextButton(onClick = onClose) { Text(stringResource(R.string.cancel)) } },
    )
}
