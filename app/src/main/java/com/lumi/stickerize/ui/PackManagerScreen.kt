package com.lumi.stickerize.ui

import android.Manifest
import android.app.Activity
import android.content.ActivityNotFoundException
import android.graphics.BitmapFactory
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lumi.stickerize.R
import com.lumi.stickerize.sticker.PackStore
import com.lumi.stickerize.sticker.StickerPack
import com.lumi.stickerize.ui.theme.LumiBlue
import com.lumi.stickerize.ui.theme.LumiTile
import com.lumi.stickerize.ui.theme.WhatsAppGreen
import com.lumi.stickerize.util.GallerySaver
import com.lumi.stickerize.whatsapp.WhatsAppHelper

private val DangerRed = Color(0xFFD14343)

/** Layar "Paket saya": lihat isi paket, ganti nama, hapus stiker, hapus paket, simpan ke Galeri. */
@Composable
fun PackManagerScreen(vm: MainViewModel) {
    val openId = vm.managerPackId
    BackHandler {
        if (openId != null) vm.managerPackId = null else vm.managerOpen = false
    }
    LightSystemBarIcons()
    FullScreenLayer {
        Column(
            Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .imePadding()
                .padding(horizontal = 16.dp)
        ) {
            val pack = openId?.let { id -> vm.allPacks.firstOrNull { it.id == id } }
            if (pack == null) PackList(vm) else PackDetail(vm, pack)
        }
    }
}

@Composable
private fun PackList(vm: MainViewModel) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp)) {
        Text(
            "📦 " + stringResource(R.string.my_packs),
            fontWeight = FontWeight.Bold,
            fontSize = 22.sp,
            modifier = Modifier.weight(1f)
        )
        IconButton(onClick = { vm.managerOpen = false }) {
            Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.close))
        }
    }
    Text(
        stringResource(R.string.packs_help, PackStore.MAX_PER_PACK),
        fontSize = 13.sp,
        lineHeight = 18.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    vm.managerMessage?.let {
        Spacer(Modifier.height(10.dp))
        InfoCard(it)
    }
    Spacer(Modifier.height(12.dp))
    if (vm.allPacks.isEmpty()) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(top = 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("🗂️", fontSize = 44.sp)
            Spacer(Modifier.height(10.dp))
            Text(stringResource(R.string.packs_empty), fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            Text(
                stringResource(R.string.packs_empty_help),
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
        return
    }
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(bottom = 16.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        items(vm.allPacks, key = { it.id }) { pack -> PackRow(vm, pack) }
    }
}

@Composable
private fun PackRow(vm: MainViewModel, pack: StickerPack) {
    val context = LocalContext.current
    val tray = remember(pack.id, pack.version) {
        val f = PackStore.stickerFile(context, pack.id, pack.trayFile)
        BitmapFactory.decodeFile(f.absolutePath)?.asImageBitmap()
    }
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp))
            .clickable { vm.openPack(pack.id) }
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .size(52.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(LumiTile),
            contentAlignment = Alignment.Center
        ) {
            if (tray != null) Image(tray, null, modifier = Modifier.size(46.dp)) else Text("📦", fontSize = 22.sp)
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(pack.name, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                stringResource(R.string.pack_count, pack.stickers.size, PackStore.MAX_PER_PACK),
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text("›", fontSize = 26.sp, color = LumiBlue)
    }
}

@Composable
private fun PackDetail(vm: MainViewModel, pack: StickerPack) {
    val context = LocalContext.current
    var name by remember(pack.id) { mutableStateOf(pack.name) }
    var selection by remember(pack.id) { mutableStateOf(setOf<String>()) }
    var confirmDeletePack by remember { mutableStateOf(false) }
    var confirmDeleteStickers by remember { mutableStateOf(false) }

    LaunchedEffect(pack.id) { if (vm.managerStickers.isEmpty()) vm.openPack(pack.id) }

    val waLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { res ->
        vm.managerMessage = if (res.resultCode == Activity.RESULT_OK) context.getString(R.string.wa_added)
        else res.data?.getStringExtra("validation_error")?.let { context.getString(R.string.wa_rejected, it) }
            ?: context.getString(R.string.packs_wa_already)
    }
    val storagePermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
        if (ok) vm.savePackToGallery(pack.id) else vm.managerMessage = context.getString(R.string.err_gallery_permission)
    }

    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp)) {
        IconButton(onClick = { vm.managerPackId = null }) {
            Icon(Icons.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
        }
        Text(
            pack.name,
            fontWeight = FontWeight.Bold,
            fontSize = 20.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
    }
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        vm.managerMessage?.let {
            InfoCard(it)
            Spacer(Modifier.height(10.dp))
        }
        // ---- ganti nama
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.weight(1f)) {
                CompactField(value = name, onChange = { name = it.take(60) }, label = stringResource(R.string.pack_name))
            }
            Spacer(Modifier.width(8.dp))
            Button(
                onClick = { vm.renamePack(pack.id, name) },
                enabled = name.isNotBlank() && name.trim() != pack.name,
                contentPadding = PaddingValues(horizontal = 14.dp),
                modifier = Modifier.height(46.dp)
            ) { Text(stringResource(R.string.save), maxLines = 1) }
        }
        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                stringResource(R.string.pack_count, pack.stickers.size, PackStore.MAX_PER_PACK),
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                modifier = Modifier.weight(1f)
            )
            Text(
                if (selection.isEmpty()) stringResource(R.string.packs_tap_to_select)
                else stringResource(R.string.n_ticked_plain, selection.size),
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(Modifier.height(8.dp))
        // ---- isi paket, 4 per baris
        vm.managerStickers.chunked(4).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { st ->
                    val checked = st.fileName in selection
                    Box(
                        Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (checked) MaterialTheme.colorScheme.primaryContainer else LumiTile)
                            .border(if (checked) 2.dp else 0.dp, if (checked) DangerRed else Color.Transparent, RoundedCornerShape(12.dp))
                            .clickable { selection = if (checked) selection - st.fileName else selection + st.fileName }
                            .padding(4.dp)
                    ) {
                        Image(st.image, null, modifier = Modifier.fillMaxSize())
                        if (checked) {
                            Box(
                                Modifier
                                    .align(Alignment.TopEnd)
                                    .size(18.dp)
                                    .clip(CircleShape)
                                    .background(DangerRed),
                                contentAlignment = Alignment.Center
                            ) { Icon(Icons.Filled.Check, null, tint = Color.White, modifier = Modifier.size(12.dp)) }
                        }
                    }
                }
                repeat(4 - row.size) { Spacer(Modifier.weight(1f)) }
            }
            Spacer(Modifier.height(8.dp))
        }
        Spacer(Modifier.height(6.dp))
        if (selection.isNotEmpty()) {
            OutlinedButton(
                onClick = { confirmDeleteStickers = true },
                colors = ButtonDefaults.outlinedButtonColors(contentColor = DangerRed),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp)
            ) { Text("🗑 " + stringResource(R.string.packs_delete_selected, selection.size), maxLines = 1) }
            Spacer(Modifier.height(8.dp))
        }
        Button(
            onClick = {
                if (!WhatsAppHelper.isAnyInstalled(context)) {
                    vm.managerMessage = context.getString(R.string.wa_not_installed)
                    return@Button
                }
                try {
                    waLauncher.launch(WhatsAppHelper.addPackIntent(context, pack.id, pack.name))
                } catch (e: ActivityNotFoundException) {
                    vm.managerMessage = context.getString(R.string.wa_too_old)
                }
            },
            colors = ButtonDefaults.buttonColors(containerColor = WhatsAppGreen),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
        ) { Text(stringResource(R.string.add_to_whatsapp), fontWeight = FontWeight.Bold, maxLines = 1) }
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedButton(
                onClick = {
                    if (GallerySaver.needsPermission) storagePermission.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    else vm.savePackToGallery(pack.id)
                },
                contentPadding = PaddingValues(horizontal = 8.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp)
            ) { Text("⬇ " + stringResource(R.string.save_to_gallery), maxLines = 1, fontSize = 14.sp) }
            OutlinedButton(
                onClick = { confirmDeletePack = true },
                colors = ButtonDefaults.outlinedButtonColors(contentColor = DangerRed),
                contentPadding = PaddingValues(horizontal = 8.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp)
            ) { Text("🗑 " + stringResource(R.string.packs_delete_pack), maxLines = 1, fontSize = 14.sp) }
        }
        Spacer(Modifier.height(16.dp))
    }

    if (confirmDeleteStickers) {
        val remaining = pack.stickers.size - selection.size
        AlertDialog(
            onDismissRequest = { confirmDeleteStickers = false },
            title = { Text(stringResource(R.string.packs_delete_selected, selection.size)) },
            text = {
                Text(
                    if (remaining < PackStore.MIN_PER_PACK) stringResource(R.string.packs_min_three, PackStore.MIN_PER_PACK)
                    else stringResource(R.string.packs_delete_stickers_confirm, remaining)
                )
            },
            confirmButton = {
                if (remaining >= PackStore.MIN_PER_PACK) TextButton(onClick = {
                    vm.deletePackStickers(pack.id, selection)
                    selection = emptySet()
                    confirmDeleteStickers = false
                }) { Text(stringResource(R.string.delete), color = DangerRed, fontWeight = FontWeight.Bold) }
                else TextButton(onClick = { confirmDeleteStickers = false }) { Text(stringResource(R.string.ok)) }
            },
            dismissButton = {
                if (remaining >= PackStore.MIN_PER_PACK) TextButton(onClick = { confirmDeleteStickers = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
    if (confirmDeletePack) {
        AlertDialog(
            onDismissRequest = { confirmDeletePack = false },
            title = { Text(stringResource(R.string.packs_delete_pack_title, pack.name)) },
            text = { Text(stringResource(R.string.packs_delete_pack_confirm)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmDeletePack = false
                    vm.deletePack(pack.id)
                }) { Text(stringResource(R.string.delete), color = DangerRed, fontWeight = FontWeight.Bold) }
            },
            dismissButton = { TextButton(onClick = { confirmDeletePack = false }) { Text(stringResource(R.string.cancel)) } }
        )
    }
}
