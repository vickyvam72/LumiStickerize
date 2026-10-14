<div align="center">

# ✦ Lumi Stickerize ✦

### An AI-powered WhatsApp sticker maker for Android

*3 photos · 1 AI image · 3–10 stickers · straight into WhatsApp*

[![Release](https://img.shields.io/github/v/release/vickyvam72/LumiStickerize?label=release&color=3E84E8&style=for-the-badge)](https://github.com/vickyvam72/LumiStickerize/releases/latest)
[![Android](https://img.shields.io/badge/Android-8.0%2B-3DDC84?style=for-the-badge&logo=android&logoColor=white)](#-requirements)
[![Kotlin](https://img.shields.io/badge/Kotlin-Jetpack%20Compose-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white)](#%EF%B8%8F-tech-stack)
[![WhatsApp](https://img.shields.io/badge/WhatsApp-Stickers-25D366?style=for-the-badge&logo=whatsapp&logoColor=white)](#-features)
[![License](https://img.shields.io/badge/License-GPL--3.0-F57C00?style=for-the-badge)](LICENSE)

**🌐 Bahasa / Language:** &nbsp; [🇮🇩 Bahasa Indonesia](README.md) &nbsp;|&nbsp; **🇬🇧 English**

[⬇️ Download](#%EF%B8%8F-download--install) · [✨ Features](#-features) · [📖 User Guide](#-user-guide) · [❓ FAQ](#-faq--troubleshooting) · [📝 Changelog](CHANGELOG.md)

</div>

---

## 🌟 Why Lumi Stickerize?

> Pick 3 photos, choose a style and your captions, and **Lumi Stickerize** creates a WhatsApp sticker pack that looks like you — neatly cut out and added in **one tap**.

| | |
|---|---|
| 🤖 **Stickers that look like you** | The AI learns your face, hair or hijab, and outfit from your 3 photos. |
| 💑 **Couple stickers** | Automatic face detection. Pick 1 person, or 2 people for **couple stickers**. |
| 💸 **Token-saving** | All stickers are drawn in **a single image**, so 3 or 10 stickers cost the same. |
| ✂️ **Auto cut-out** | Background removed, stray frames and lines cleaned up, and a white sticker border added. |
| 🎨 **10 styles** | Cartoon, Chibi, Anime, Cute 3D, Kawaii, Pixel Art, Comic, Watercolor, Clay and Sketch. |
| 💬 **1,600+ ready-made captions** | Write your own, or tap 🎲 for a random caption on each sticker. |
| 🧽 **Manual eraser** | Something still not clean? Just rub it away with your finger. |
| 🆓 **Free option** | Use free daily models from Cloudflare, or OpenRouter models for the best results. |

---

## ✨ Features

**🖼️ Photos & faces**
- Pick **3 photos** one by one or **all at once**
- An upfront reminder that all 3 photos should show the **same outfit**
- **Automatic face detection** on the first photo (on-device, no internet needed)
- Tap the box on a face to select it, **up to 2 people**
- Unselected faces are **pixelated** before anything is sent to the AI

**🎨 Sticker settings**
- **3 to 10** stickers (WhatsApp requires at least 3)
- **10** art styles
- **No text** (expressions only) or **with captions**, one per sticker
- A 🎲 button on every field plus **🎲 Acak semua** (Randomize all), drawing from **1,647 Indonesian captions** in 33 categories: greetings, thanks, apologies, laughter, romance, work, school, food, Islamic phrases, slang, regional languages and more

**✂️ Results**
- 512×512 WebP stickers under 100 KB, meeting WhatsApp's requirements
- Automatically removes white backgrounds, **boxes/frames**, **stray line fragments** and specks
- **Rapikan stiker** (Tidy up sticker): an eraser with adjustable brush, Undo and Start over
- View the original AI image for comparison
- **Buat ulang** (Regenerate) with the same settings

**📲 Add to WhatsApp**
- The official **Tambahkan ke WhatsApp** (Add to WhatsApp) flow, supporting WhatsApp and WhatsApp Business
- Rename the sticker pack
- Stickers edited after adding are updated in WhatsApp too

---

## 🤖 AI Options

| Model | Provider | Cost | Notes |
|---|---|---|---|
| **Nano Banana 2 Lite** *(default)* | OpenRouter | ~$0.034 / generation | Best likeness, clean text, fast |
| **Seedream 5.0 Flash** | OpenRouter | ~$0.018 / generation | Cheaper |
| **Nano Banana 2** | OpenRouter | ~2× Lite | Highest quality |
| **FLUX.2 Klein 9B** | Cloudflare | 🆓 ~6× per day | Free, lower likeness |
| **FLUX.2 Klein 4B** | Cloudflare | 🆓 ~80× per day | Free, fast, standard quality |

> 💡 One generation = one AI image, no matter how many stickers. Cloudflare's free quota resets every day at **00:00 UTC**.

---

## ⬇️ Download & Install

1. Go to the **[Releases](https://github.com/vickyvam72/LumiStickerize/releases/latest)** page.
2. Under **Assets**, download `LumiStickerize-v2.0.apk`.
3. Open the downloaded file. If Android asks, allow **"Install unknown apps"** for your browser or file manager.
4. Tap **Install**. Done! 🎉

> 💡 **Updating:** Just install the new APK over the old one. Your settings and sticker packs are kept.

### 📋 Requirements
- Android **8.0 (API 26)** or newer on an ARM processor (nearly every Android phone)
- About **80 MB** of free space
- **WhatsApp** or **WhatsApp Business** installed
- An internet connection and an **API key** from OpenRouter or Cloudflare

---

## 📖 User Guide

> 🗣️ The app interface is in **Indonesian**. Button names are written below exactly as they appear in the app, with an English translation in brackets.

### 1️⃣ Add your API key (once)
Tap the ⚙️ icon at the top right → **Pengaturan AI** (AI settings), then pick a model:
- **OpenRouter (paid):** create a key at [openrouter.ai/keys](https://openrouter.ai/keys), add credit at [openrouter.ai/credits](https://openrouter.ai/credits), then paste the key (starts with `sk-or-`).
- **Cloudflare (free):** sign up at [dash.cloudflare.com](https://dash.cloudflare.com), copy your **Account ID**, create an **API Token** with the *Workers AI* template, and fill in both.

Keys are stored **only on your device**.

### 2️⃣ Pick 3 photos
Use photos with the **same outfit** and clearly visible faces. Make sure everyone you want on the stickers appears in the **first photo**.

### 3️⃣ Choose faces
Tap the box on a face in the first photo. For couple stickers, tap **Pilih keduanya (stiker berdua)** (Select both — couple stickers).

### 4️⃣ Count, style and captions
Choose 3–10 stickers, pick a style, then choose **Tanpa kata** (No text) or **Pakai kata** (With captions). Tap 🎲 for random captions.

### 5️⃣ Generate and tidy up
Tap **✨ Buat Stiker** (Create stickers) and wait about 15–60 seconds. If anything looks untidy, tap that sticker to open **Rapikan stiker** (Tidy up sticker).

### 6️⃣ Add to WhatsApp
Rename the pack if you like, then tap **Tambahkan ke WhatsApp** (Add to WhatsApp).

---

## ❓ FAQ & Troubleshooting

<details>
<summary><b>In couple stickers, both faces look the same</b></summary>

Tap **Buat ulang** (Regenerate). The app already sends each person's photo separately to the AI, but it can still slip occasionally. **Nano Banana 2** is usually the most accurate for couple stickers.
</details>

<details>
<summary><b>There's still a line or box around a sticker</b></summary>

Tap that sticker, rub the area away with the eraser on the **Rapikan stiker** screen, and tap **Simpan** (Save).
</details>

<details>
<summary><b>No face is detected</b></summary>

Replace the first photo with one where the face is larger, well lit and facing the camera.
</details>

<details>
<summary><b>"Out of credit" or "quota used up"</b></summary>

For OpenRouter, add credit at openrouter.ai/credits. For Cloudflare, wait for the daily reset at 00:00 UTC, or switch to another model in ⚙️ Settings.
</details>

<details>
<summary><b>Why at least 3 stickers?</b></summary>

WhatsApp requires every sticker pack to contain at least 3 stickers.
</details>

<details>
<summary><b>WhatsApp rejects the sticker pack</b></summary>

Update WhatsApp to the latest version. If it still fails, the app shows WhatsApp's error message.
</details>

---

## 🔒 Privacy

- ✅ **No** ads, analytics or trackers
- ✅ Face detection runs **on your device**, offline
- ✅ Photos are sent only to **the AI provider you choose**, when you generate stickers
- ✅ Unselected faces are **pixelated** before sending
- ✅ API keys and sticker packs stay **on your device**

---

## 🛠️ Tech Stack

| Area | Technology |
|---|---|
| Language | **Kotlin** (+ Java for the sticker cutter) |
| UI | **Jetpack Compose**, Material 3 |
| AI images | **OpenRouter Image API** and **Cloudflare Workers AI** |
| Face detection | **Google ML Kit Face Detection** (on-device) |
| Networking | OkHttp |
| Stickers | ContentProvider following WhatsApp's sticker contract |
| CI/CD | GitHub Actions — every push builds and publishes the APK |

### 📁 Project structure
```
app/src/main/java/com/lumi/stickerize/
├── ai/          # PromptBuilder, OpenRouter & Cloudflare clients
├── data/        # Settings, model list, caption bank
├── model/       # Sticker styles and grid layout
├── sticker/     # Sticker cutter, pack storage, WhatsApp ContentProvider
├── util/        # Face detection, photo processing
├── whatsapp/    # "Add to WhatsApp" intent
└── ui/          # Compose screens: wizard steps, results, eraser
app/src/main/assets/kata_stiker.txt   # 1,647 captions for the 🎲 button
```

### 🏗️ Building from source
```bash
git clone https://github.com/vickyvam72/LumiStickerize.git
cd LumiStickerize
gradle :app:assembleRelease   # Gradle 8.11.1, JDK 17
```
The APK is written to `app/build/outputs/apk/release/`.

> 🔐 Release builds are signed with a private key stored in **GitHub Actions Secrets** (`LUMI_KEYSTORE_BASE64`, `LUMI_KEYSTORE_PASSWORD`). The key is never committed to the repository. For a local build, place your own keystore at `keystore/lumistickerize.jks` and set `LUMI_KEYSTORE_PASSWORD`.

---

## 🙏 Acknowledgements

Lumi Stickerize stands on the shoulders of these amazing projects, models and services. A huge **thank you** to their authors and contributors! 💖

### 🧩 Code, techniques and ideas
| Project | What Lumi Stickerize learned from it |
|---|---|
| [**WhatsApp Stickers**](https://github.com/WhatsApp/stickers) by WhatsApp | The ContentProvider contract, the *Add to WhatsApp* intent and sticker file requirements |

### 🤖 AI models
[Nano Banana (Gemini Image)](https://deepmind.google/models/gemini-image/) by Google · [Seedream](https://seed.bytedance.com/) by ByteDance Seed · [FLUX.2 Klein](https://bfl.ai/) by Black Forest Labs

### ☁️ Services
[OpenRouter](https://openrouter.ai) · [Cloudflare Workers AI](https://developers.cloudflare.com/workers-ai/)

### 📚 Libraries
[Jetpack Compose](https://developer.android.com/jetpack/compose) · [ML Kit Face Detection](https://developers.google.com/ml-kit/vision/face-detection) · [OkHttp](https://github.com/square/okhttp) · [Kotlin Coroutines](https://github.com/Kotlin/kotlinx.coroutines) · [AndroidX ExifInterface](https://developer.android.com/jetpack/androidx/releases/exifinterface)

> These projects and services are not affiliated with Lumi Stickerize. All credit for their work belongs to their respective authors.

---

## ⚖️ Disclaimer

Lumi Stickerize is an independent project for **personal and educational use**.
It is **not affiliated with, endorsed by, or sponsored by WhatsApp, Meta, Google, OpenRouter, Cloudflare, ByteDance or Black Forest Labs**. All trademarks belong to their respective owners.
Only use photos of yourself or of people who have given their permission. AI usage costs follow the terms of the provider you choose.

## 📄 License

Copyright © 2026 **vickyvam72**.

Lumi Stickerize is free software: you can redistribute it and/or modify it under the terms of the **[GNU General Public License v3.0](LICENSE)** as published by the Free Software Foundation. It is distributed in the hope that it will be useful, but **WITHOUT ANY WARRANTY**. See the [LICENSE](LICENSE) file for details.

<div align="center">

---

Made with ❤️ in Indonesia · If you like Lumi Stickerize, leave a ⭐

</div>
