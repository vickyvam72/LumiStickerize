package com.lumi.stickerize.model

import androidx.annotation.StringRes
import com.lumi.stickerize.R

/** Pilihan tema stiker yang bisa dipilih user. */
enum class StickerStyle(@StringRes val label: Int, val emoji: String, val prompt: String) {
    CARTOON(
        R.string.style_cartoon, "🎨",
        "vibrant 2D cartoon illustration, thick clean outlines, flat bright colours, exaggerated friendly expressions"
    ),
    CHIBI(
        R.string.style_chibi, "🍡",
        "super-deformed chibi style: big head, tiny body (about 2.5 heads tall), huge sparkling eyes, soft pastel cel shading"
    ),
    ANIME(
        R.string.style_anime, "🌸",
        "Japanese anime style, crisp line art, expressive large eyes, cel shading with soft highlights"
    ),
    CUTE_3D(
        R.string.style_cute3d, "🧸",
        "cute stylised 3D character render, smooth rounded shapes, soft studio lighting, glossy toy-like look"
    ),
    KAWAII(
        R.string.style_kawaii, "💖",
        "kawaii doodle style, round simple shapes, rosy blush cheeks, pastel colours, very soft and adorable"
    ),
    PIXEL(
        R.string.style_pixel, "👾",
        "retro 16-bit pixel art character, crisp visible pixels, limited bright palette"
    ),
    COMIC(
        R.string.style_comic, "💥",
        "pop-art comic book style, bold black ink lines, halftone dot shading, punchy saturated colours"
    ),
    WATERCOLOR(
        R.string.style_watercolor, "🖌️",
        "soft watercolour illustration with gentle colour washes, but with a clear defined outline around the character"
    ),
    CLAY(
        R.string.style_clay, "🏺",
        "handmade clay / plasticine figure look, soft matte texture, rounded chunky shapes"
    ),
    SKETCH(
        R.string.style_sketch, "✏️",
        "hand-drawn pencil and ink sketch, bold expressive lines with light colour accents"
    );
}

/** Tata letak satu lembar gambar dari AI. Tiap sel = 1 stiker berbentuk persegi. */
data class GridSpec(val cols: Int, val rows: Int, val aspectRatio: String) {
    val cells: Int get() = cols * rows

    companion object {
        /** WhatsApp mewajibkan minimal 3 stiker per paket. */
        const val WHATSAPP_MIN = 3

        fun forCount(count: Int): GridSpec = when {
            count <= 4 -> GridSpec(2, 2, "1:1")
            count <= 6 -> GridSpec(3, 2, "3:2")
            count <= 9 -> GridSpec(3, 3, "1:1")
            else -> GridSpec(4, 3, "4:3")
        }
    }
}
