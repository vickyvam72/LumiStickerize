package com.lumi.stickerize.ui

import android.graphics.Bitmap
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.lumi.stickerize.R
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/** Penghapus manual: gosok bagian stiker yang tidak terpotong rapi. */
@Composable
fun EraserScreen(
    index: Int,
    original: Bitmap,
    onDismiss: () -> Unit,
    onSave: (Bitmap) -> Unit,
) {
    val working = remember { original.copy(Bitmap.Config.ARGB_8888, true) }
    val canvas = remember { android.graphics.Canvas(working) }
    val history = remember { ArrayList<Bitmap>() }
    var version by remember { mutableIntStateOf(0) }
    var brush by remember { mutableFloatStateOf(30f) }
    var boxSize by remember { mutableStateOf(IntSize.Zero) }
    var edited by remember { mutableStateOf(false) }
    val image = remember(version) { working.asImageBitmap() }

    val paint = remember {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }
    }

    fun toBitmap(p: Offset): Offset {
        if (boxSize.width == 0) return p
        val s = working.width.toFloat() / boxSize.width
        return Offset(p.x * s, p.y * s)
    }

    fun pushHistory() {
        history.add(working.copy(Bitmap.Config.ARGB_8888, false))
        if (history.size > 15) history.removeAt(0).recycle()
    }

    fun restore(from: Bitmap) {
        canvas.drawColor(0, PorterDuff.Mode.CLEAR)
        canvas.drawBitmap(from, 0f, 0f, null)
        version++
    }

    fun erase(a: Offset, b: Offset) {
        paint.strokeWidth = brush
        val p1 = toBitmap(a)
        val p2 = toBitmap(b)
        canvas.drawLine(p1.x, p1.y, p2.x, p2.y, paint)
        edited = true
        version++
    }

    BackHandler(onBack = onDismiss)
    LightSystemBarIcons()
    // Layar penuh di jendela aplikasi (bukan Dialog): latar menyambung sampai paling bawah,
    // di belakang tombol navigasi HP, tanpa garis hitam.
    FullScreenLayer {
        run {
            Column(
                Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .padding(20.dp)
            ) {
                Text(stringResource(R.string.tidy_title, index + 1), fontWeight = FontWeight.Bold, fontSize = 22.sp)
                Spacer(Modifier.height(4.dp))
                Text(
                    stringResource(R.string.tidy_help),
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(16.dp))

                Box(
                    Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(16.dp))
                        .drawBehind {
                            val cell = 14.dp.toPx()
                            var y = 0f
                            var row = 0
                            while (y < size.height) {
                                var x = 0f
                                var col = 0
                                while (x < size.width) {
                                    drawRect(
                                        if ((row + col) % 2 == 0) Color(0xFFE9EDF3) else Color(0xFFFFFFFF),
                                        topLeft = Offset(x, y),
                                        size = Size(cell, cell)
                                    )
                                    x += cell
                                    col++
                                }
                                y += cell
                                row++
                            }
                        }
                        .onSizeChanged { boxSize = it }
                        .pointerInput(Unit) {
                            detectTapGestures(onTap = { pos ->
                                pushHistory()
                                erase(pos, pos)
                            })
                        }
                        .pointerInput(Unit) {
                            detectDragGestures(
                                onDragStart = { pos ->
                                    pushHistory()
                                    erase(pos, pos)
                                },
                                onDrag = { change, drag ->
                                    change.consume()
                                    erase(change.position - drag, change.position)
                                }
                            )
                        }
                ) {
                    Image(image, contentDescription = null, modifier = Modifier.fillMaxSize())
                }

                Spacer(Modifier.height(16.dp))
                Text(stringResource(R.string.tidy_brush, brush.toInt()), fontSize = 14.sp)
                Slider(value = brush, onValueChange = { brush = it }, valueRange = 6f..90f)

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(
                        onClick = {
                            if (history.isNotEmpty()) {
                                val prev = history.removeAt(history.size - 1)
                                restore(prev)
                                prev.recycle()
                            }
                        },
                        modifier = Modifier.weight(1f)
                    ) { Text("↶ " + stringResource(R.string.undo), maxLines = 1) }
                    OutlinedButton(
                        onClick = {
                            pushHistory()
                            restore(original)
                            edited = false
                        },
                        modifier = Modifier.weight(1f)
                    ) { Text(stringResource(R.string.reset), maxLines = 1) }
                }
                // tombol langsung di bawah kontrol (bukan di dasar layar) supaya tidak tertutup tombol HP
                Spacer(Modifier.height(20.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                    ) { Text(stringResource(R.string.cancel), fontWeight = FontWeight.Bold) }
                    Button(
                        onClick = { onSave(working.copy(Bitmap.Config.ARGB_8888, false)) },
                        enabled = edited || history.isNotEmpty(),
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                    ) { Text(stringResource(R.string.save), fontWeight = FontWeight.Bold) }
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

/** Lapisan layar penuh berlatar terang yang menahan sentuhan agar tidak tembus ke halaman di bawahnya. */
@Composable
internal fun FullScreenLayer(content: @Composable () -> Unit) {
    Box(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .pointerInput(Unit) { detectTapGestures { } }
    ) { content() }
}

/** Selama layar terang tampil, ikon jam/baterai dibuat gelap supaya tetap terbaca. */
@Composable
internal fun LightSystemBarIcons() {
    val view = LocalView.current
    DisposableEffect(view) {
        val window = (view.context as? Activity)?.window
        val ctrl = window?.let { WindowCompat.getInsetsController(it, view) }
        val before = ctrl?.isAppearanceLightStatusBars ?: false
        ctrl?.isAppearanceLightStatusBars = true
        onDispose { ctrl?.isAppearanceLightStatusBars = before }
    }
}

/** Gambar asli dari AI, diperkecil agar pas satu layar. */
@Composable
fun SheetViewer(sheet: ImageBitmap, onClose: () -> Unit) {
    BackHandler(onBack = onClose)
    LightSystemBarIcons()
    FullScreenLayer {
        Column(
            Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .padding(20.dp)
        ) {
            Text(stringResource(R.string.sheet_title), fontWeight = FontWeight.Bold, fontSize = 22.sp)
            Spacer(Modifier.height(4.dp))
            Text(
                stringResource(R.string.sheet_help),
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(16.dp))
            Box(
                Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    sheet,
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(16.dp))
                )
            }
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = onClose,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) { Text(stringResource(R.string.close), fontWeight = FontWeight.Bold) }
        }
    }
}
