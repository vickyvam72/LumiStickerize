package com.lumi.stickerize.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lumi.stickerize.ui.theme.LumiBlue
import com.lumi.stickerize.ui.theme.LumiBlueLight
import com.lumi.stickerize.ui.theme.LumiInk

/* Kartu tips bergaya Lumi Signal (kartu kaca, tanda ✦, ikon dalam kotak gradien) yang mengisi
 * ruang kosong di bawah tiap langkah. Kartu hanya tampil sebanyak baris yang muat, jadi layar
 * tetap satu halaman tanpa scroll. */

private val Violet = Color(0xFF7C6CFF)
private val Pink = Color(0xFFFF6FA8)
private val Amber = Color(0xFFFFA940)
private val Green = Color(0xFF22B07D)
private val Teal = Color(0xFF14B8C4)

class Tip(val emoji: String, val title: String, val body: String, val accent: Color)
class TipSet(val title: String, val badge: String?, val tips: List<Tip>)

fun tipsFor(step: Step, useText: Boolean): TipSet? = when (step) {
    Step.PHOTOS -> TipSet(
        "Rahasia stiker paling mirip", "3 tips",
        listOf(
            Tip("📸", "Wajah terang & jelas", "Menghadap kamera, tanpa kacamata hitam atau masker.", LumiBlue),
            Tip("🪞", "Satu outfit, banyak sudut", "Foto 2 & 3 dari sisi lain bikin AI makin kenal wajahmu.", Violet),
            Tip("🔒", "Privasi tetap aman", "Wajah lain di foto disamarkan dulu sebelum dikirim ke AI.", Green),
        )
    )
    Step.FACES -> TipSet(
        "Sendiri atau berdua?", null,
        listOf(
            Tip("👯", "Pilih 2 wajah untuk stiker couple", "Kamu dan pasangan atau sahabat tampil bareng di tiap stiker.", Pink),
            Tip("🙈", "Yang tidak dipilih aman", "Wajah lain otomatis disamarkan dan tidak ikut digambar.", Teal),
        )
    )
    Step.COUNT -> TipSet(
        "Ambil banyak, tetap hemat", "1 gambar AI",
        listOf(
            Tip("💸", "Biayanya sama saja", "3 atau 10 stiker tetap cukup 1 kali gambar AI. Gas yang banyak!", Green),
            Tip("🎭", "Ekspresi beda-beda", "Tiap stiker dapat pose dan ekspresi yang berbeda.", Pink),
            Tip("✂️", "Dipotong otomatis", "Latar dihapus dan diberi garis putih khas stiker WhatsApp.", LumiBlue),
            Tip("📦", "Satu paket muat 30", "Tambahkan terus ke paket yang sama sampai 30 stiker.", Amber),
        )
    )
    Step.STYLE -> TipSet(
        "Bingung pilih tema?", "10 gaya",
        listOf(
            Tip("😄", "Sehari-hari", "Cartoon & 3D Lucu: wajah tetap mudah dikenali, cocok buat chat apa saja.", LumiBlue),
            Tip("🎀", "Super gemas", "Chibi & Kawaii: kepala besar, mata berbinar, imutnya maksimal.", Pink),
            Tip("💥", "Penuh aksi", "Komik & Anime: garis tegas dan ekspresi dramatis.", Amber),
            Tip("🎨", "Kalem & estetik", "Cat Air, Clay, Sketsa & Pixel Art buat yang suka beda.", Violet),
        )
    )
    Step.TEXT -> if (useText) TipSet(
        "Biar tulisannya makin kece", "1.647 kata",
        listOf(
            Tip("🎲", "Acak sepuasnya", "Ketuk dadu sampai ketemu kata yang pas, atau ketik sendiri.", Violet),
            Tip("✍️", "Pendek paling rapi", "1–2 kata paling jelas terbaca di stiker yang kecil.", LumiBlue),
            Tip("🔁", "Bisa diganti nanti", "Di halaman hasil, centang stiker lalu ubah katanya.", Green),
        )
    ) else TipSet(
        "Biarkan ekspresi yang bicara", null,
        listOf(
            Tip("😶", "Tanpa kata", "Stiker bersih tanpa tulisan, cocok untuk semua bahasa.", Teal),
            Tip("💬", "Pakai kata", "Tambahkan tulisan lucu dari 1.647 kata siap pakai.", Violet),
            Tip("🔁", "Bisa diganti nanti", "Di halaman hasil, centang stiker lalu ubah katanya.", Green),
        )
    )
    else -> null
}

/** Menampilkan [content], lalu kartu tips di bawahnya sebanyak baris yang masih muat di [available]. */
@Composable
fun WithTips(available: Dp, tips: TipSet?, content: @Composable () -> Unit) {
    SubcomposeLayout { c ->
        val loose = Constraints(maxWidth = c.maxWidth)
        val main = subcompose("main") { Column(Modifier.fillMaxWidth()) { content() } }.map { it.measure(loose) }
        val mainH = main.maxOfOrNull { it.height } ?: 0
        val gap = 14.dp.roundToPx()
        val room = available.roundToPx() - mainH - gap
        var card: androidx.compose.ui.layout.Placeable? = null
        if (tips != null && room > 0) {
            for (n in tips.tips.size downTo 1) {
                val p = subcompose("tips$n") { TipsCard(tips, n) }.first().measure(loose)
                if (p.height <= room) { card = p; break }
            }
        }
        val h = mainH + (card?.let { gap + it.height } ?: 0)
        layout(c.maxWidth, h) {
            main.forEach { it.place(0, 0) }
            card?.place(0, mainH + gap)
        }
    }
}

@Composable
private fun TipsCard(set: TipSet, rows: Int) {
    val shape = RoundedCornerShape(22.dp)
    Column(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Brush.verticalGradient(listOf(Color.White, Color(0xFFF7FAFF))))
            .border(
                1.dp,
                Brush.linearGradient(listOf(LumiBlueLight.copy(alpha = 0.7f), Color(0xFFD9E4F5))),
                shape
            )
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Sparkle(10.dp, LumiBlue)
            Spacer(Modifier.width(8.dp))
            Text(
                set.title,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = LumiInk,
                maxLines = 1,
                modifier = Modifier.weight(1f)
            )
            set.badge?.let {
                Text(
                    it,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = LumiBlue,
                    maxLines = 1,
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(LumiBlue.copy(alpha = 0.10f))
                        .border(1.dp, LumiBlue.copy(alpha = 0.30f), RoundedCornerShape(50))
                        .padding(horizontal = 9.dp, vertical = 3.dp)
                )
            }
        }
        set.tips.take(rows).forEach { t ->
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Brush.linearGradient(listOf(t.accent.copy(alpha = 0.95f), t.accent.copy(alpha = 0.55f)))),
                    contentAlignment = Alignment.Center
                ) { Text(t.emoji, fontSize = 17.sp) }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(t.title, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = LumiInk, maxLines = 1)
                    Text(
                        t.body,
                        fontSize = 12.sp,
                        lineHeight = 16.sp,
                        color = Color(0xFF5B6B85),
                        maxLines = 2
                    )
                }
            }
        }
    }
}

/** Tanda kilau empat sudut, aksen khas keluarga Lumi. */
@Composable
private fun Sparkle(size: Dp, color: Color) {
    Canvas(Modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val p = Path().apply {
            moveTo(w / 2, 0f)
            quadraticBezierTo(w * 0.56f, h * 0.44f, w, h / 2)
            quadraticBezierTo(w * 0.56f, h * 0.56f, w / 2, h)
            quadraticBezierTo(w * 0.44f, h * 0.56f, 0f, h / 2)
            quadraticBezierTo(w * 0.44f, h * 0.44f, w / 2, 0f)
            close()
        }
        drawPath(p, color)
    }
}
