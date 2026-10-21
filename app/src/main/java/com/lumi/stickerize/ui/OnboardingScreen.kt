package com.lumi.stickerize.ui

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.lumi.stickerize.R
import com.lumi.stickerize.data.AppSettings
import com.lumi.stickerize.work.Notifier
import com.lumi.stickerize.i18n.Lang
import androidx.compose.ui.res.stringResource
import androidx.compose.material3.AlertDialog

// Palet gelap layar pembuka
private val NightTop = Color(0xFF0A1330)
private val NightBottom = Color(0xFF070B1C)
private val CardBg = Color(0xFF121C3D)
private val CardBorder = Color(0xFF2A3B6B)
private val IconBg = Color(0xFF1B2A55)
private val TextMain = Color(0xFFF2F5FF)
private val TextSoft = Color(0xFFA9B4D6)
private val Accent = Color(0xFF5B9BF5)
private val Success = Color(0xFF4CD08A)
private val Warn = Color(0xFFF2B54A)
private val ButtonGradient = Brush.horizontalGradient(listOf(Color(0xFF7CC0FA), Color(0xFF4F95EE), Color(0xFF3A6FEA)))
private val Background = Brush.verticalGradient(listOf(NightTop, Color(0xFF0D1838), NightBottom))
private val Glow = Brush.radialGradient(
    colors = listOf(Color(0xFF3E84E8).copy(alpha = 0.28f), Color.Transparent),
    center = androidx.compose.ui.geometry.Offset(Float.POSITIVE_INFINITY, 0f),
    radius = 1400f
)

/**
 * Layar pembuka saat aplikasi pertama kali dipasang:
 * 1. Perkenalan  2. API key (OpenRouter dan/atau Cloudflare)  3. Izin notifikasi & latar belakang
 */
@Composable
fun OnboardingScreen(settings: AppSettings, onFinished: () -> Unit) {
    var page by rememberSaveable { mutableIntStateOf(0) }
    BackHandler(enabled = page > 0) { page-- }

    Box(
        Modifier
            .fillMaxSize()
            .background(Background)
            .background(Glow)
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
    ) {
        // Ukuran font sistem yang sangat besar membuat layar pembuka tidak muat satu layar,
        // jadi di layar pembuka skala font dibatasi maksimal normal (100%).
        val density = LocalDensity.current
        CompositionLocalProvider(
            LocalDensity provides Density(density.density, minOf(density.fontScale, 1f))
        ) {
            AnimatedContent(targetState = page, label = "onboarding") { p ->
                when (p) {
                    0 -> WelcomePage(onNext = { page = 1 })
                    1 -> KeysPage(settings, onNext = { page = 2 })
                    else -> PermissionsPage(onFinished = {
                        settings.onboardingDone = true
                        onFinished()
                    })
                }
            }
        }
    }
}

/* ---------------------------------------------------------------- 1. perkenalan */

@Composable
private fun WelcomePage(onNext: () -> Unit) {
    var pickLanguage by remember { mutableStateOf(false) }
    val context = LocalContext.current
    if (pickLanguage) LanguageDialog(onDismiss = { pickLanguage = false })
    // Isi selalu di tengah layar; kalau layar kecil / font besar, halaman bisa digeser
    BoxWithConstraints(Modifier.fillMaxSize()) {
        Column(
            Modifier
                .fillMaxWidth()
                .heightIn(min = maxHeight)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // pilihan bahasa (bawaan English)
            val lang = Lang.option(Lang.current(context))
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
                Text(
                    "🌐 ${lang.flag} ${lang.label}  ▾",
                    color = TextMain,
                    fontSize = 13.sp,
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(CardBg)
                        .border(1.dp, CardBorder, RoundedCornerShape(50))
                        .clickable { pickLanguage = true }
                        .padding(horizontal = 12.dp, vertical = 7.dp)
                )
            }
            Spacer(Modifier.height(12.dp))
            AppLogo(92)
            Spacer(Modifier.height(20.dp))
            Text(stringResource(R.string.app_name), color = TextMain, fontSize = 27.sp, fontWeight = FontWeight.Light)
            Spacer(Modifier.height(8.dp))
            Text(
                stringResource(R.string.ob_tagline),
                color = TextSoft,
                fontSize = 15.sp,
                lineHeight = 21.sp,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(28.dp))
            Feature(Icons.Filled.Face, stringResource(R.string.ob_f1_title), stringResource(R.string.ob_f1_text))
            Feature(Icons.Filled.Edit, stringResource(R.string.ob_f2_title), stringResource(R.string.ob_f2_text))
            Feature(Icons.Filled.Send, stringResource(R.string.ob_f3_title), stringResource(R.string.ob_f3_text))
            Spacer(Modifier.height(28.dp))
            GradientButton(stringResource(R.string.ob_connect_ai), enabled = true, onClick = onNext, icon = Icons.Filled.Lock)
            Spacer(Modifier.height(12.dp))
            Text(
                stringResource(R.string.ob_keys_note),
                color = TextSoft.copy(alpha = 0.7f),
                fontSize = 12.sp,
                lineHeight = 17.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun Feature(icon: ImageVector, title: String, text: String) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconTile(icon)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(title, color = TextMain, fontSize = 16.sp)
            Text(text, color = TextSoft, fontSize = 13.sp, lineHeight = 18.sp)
        }
    }
}

@Composable
private fun IconTile(icon: ImageVector, tint: Color = Accent, bg: Color = IconBg, size: Int = 44) {
    Box(
        Modifier
            .size(size.dp)
            .clip(RoundedCornerShape(13.dp))
            .background(bg),
        contentAlignment = Alignment.Center
    ) { Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size((size * 0.5f).dp)) }
}

/* ---------------------------------------------------------------- 2. API key */

@Composable
private fun KeysPage(settings: AppSettings, onNext: () -> Unit) {
    var orKey by remember { mutableStateOf(settings.apiKey) }
    var cfAccount by remember { mutableStateOf(settings.cfAccountId) }
    var cfToken by remember { mutableStateOf(settings.cfToken) }
    var reveal by remember { mutableStateOf(false) }
    val uri = LocalUriHandler.current

    val hasOr = orKey.isNotBlank()
    val hasCf = cfAccount.isNotBlank() && cfToken.isNotBlank()
    val cfHalf = (cfAccount.isNotBlank()) xor (cfToken.isNotBlank())
    val hidden = if (reveal) VisualTransformation.None else PasswordVisualTransformation()

    OnePage(
        top = {
            PageHeader(
                stringResource(R.string.ob_connect_ai),
                stringResource(R.string.ob_keys_sub)
            )
            Spacer(Modifier.height(14.dp))

            // ---- OpenRouter
            DarkCard(done = hasOr) {
                CardHeader(
                    Icons.Filled.ShoppingCart, "OpenRouter", stringResource(R.string.ob_or_sub), hasOr,
                    link = stringResource(R.string.get_key), onLink = { uri.openUri(AppSettings.API_KEY_URL) }
                )
                Spacer(Modifier.height(10.dp))
                DarkField(
                    value = orKey,
                    onChange = { orKey = it.trim() },
                    label = stringResource(R.string.ob_or_label),
                    transformation = hidden,
                    onToggleReveal = { reveal = !reveal },
                    revealed = reveal
                )
            }
            Spacer(Modifier.height(10.dp))

            // ---- Cloudflare
            DarkCard(done = hasCf) {
                CardHeader(
                    Icons.Filled.Star, "Cloudflare Workers AI", stringResource(R.string.ob_cf_sub), hasCf,
                    link = stringResource(R.string.get_token), onLink = { uri.openUri(AppSettings.CF_TOKEN_URL) }
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    stringResource(R.string.ob_cf_hint),
                    color = TextSoft,
                    fontSize = 11.sp,
                    lineHeight = 15.sp
                )
                Spacer(Modifier.height(8.dp))
                DarkField(
                    value = cfAccount,
                    onChange = { cfAccount = it.trim() },
                    label = "Account ID",
                    transformation = VisualTransformation.None
                )
                Spacer(Modifier.height(6.dp))
                DarkField(
                    value = cfToken,
                    onChange = { cfToken = it.trim() },
                    label = "API Token",
                    transformation = hidden,
                    onToggleReveal = { reveal = !reveal },
                    revealed = reveal
                )
                if (cfHalf) {
                    Spacer(Modifier.height(4.dp))
                    Text(stringResource(R.string.ob_cf_both), color = Warn, fontSize = 11.sp)
                }
            }
        },
        bottom = {
            GradientButton(
                text = if (hasOr || hasCf) stringResource(R.string.next) else stringResource(R.string.ob_fill_one),
                enabled = hasOr || hasCf,
                onClick = {
                    settings.apiKey = orKey
                    settings.cfAccountId = cfAccount
                    settings.cfToken = cfToken
                    // pilih model awal yang sesuai dengan key yang diisi
                    if (!hasOr && hasCf && settings.provider != com.lumi.stickerize.data.Provider.CLOUDFLARE) {
                        settings.model = "@cf/black-forest-labs/flux-2-klein-4b"
                    } else if (hasOr && !hasCf && settings.provider == com.lumi.stickerize.data.Provider.CLOUDFLARE) {
                        settings.model = AppSettings.DEFAULT_MODEL
                    }
                    onNext()
                }
            )
            Spacer(Modifier.height(6.dp))
            Text(
                stringResource(R.string.ob_change_later),
                color = TextSoft.copy(alpha = 0.7f),
                fontSize = 11.sp,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
        }
    )
}

/** Halaman satu layar: isi di atas, tombol di bawah. Kalau layar terlalu pendek, bisa digeser. */
@Composable
private fun OnePage(top: @Composable () -> Unit, bottom: @Composable () -> Unit) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        Column(
            Modifier
                .fillMaxWidth()
                .heightIn(min = maxHeight)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column { top() }
            Column(Modifier.padding(top = 14.dp)) { bottom() }
        }
    }
}

/** Logo kecil di samping judul, lalu deskripsi singkat. */
@Composable
private fun PageHeader(title: String, subtitle: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        AppLogo(44)
        Spacer(Modifier.width(12.dp))
        Text(title, color = TextMain, fontSize = 22.sp, fontWeight = FontWeight.Light)
    }
    Spacer(Modifier.height(8.dp))
    Text(subtitle, color = TextSoft, fontSize = 13.sp, lineHeight = 18.sp)
}

@Composable
private fun CardHeader(
    icon: ImageVector,
    title: String,
    subtitle: String,
    done: Boolean,
    link: String? = null,
    onLink: () -> Unit = {},
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        if (done) IconTile(Icons.Filled.CheckCircle, tint = Success, bg = Success.copy(alpha = 0.16f), size = 36)
        else IconTile(icon, size = 36)
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(title, color = TextMain, fontSize = 15.sp, maxLines = 1)
            Text(subtitle, color = TextSoft, fontSize = 11.sp, lineHeight = 14.sp, maxLines = 1)
        }
        if (link != null) {
            Text(
                "$link →",
                color = Accent,
                fontSize = 12.sp,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(onClick = onLink)
                    .padding(horizontal = 6.dp, vertical = 8.dp)
            )
        }
    }
}

@Composable
private fun DarkField(
    value: String,
    onChange: (String) -> Unit,
    label: String,
    transformation: VisualTransformation,
    onToggleReveal: (() -> Unit)? = null,
    revealed: Boolean = false,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label, fontSize = 13.sp) },
        singleLine = true,
        textStyle = TextStyle(fontSize = 14.sp),
        visualTransformation = transformation,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
        trailingIcon = onToggleReveal?.let {
            { TextButton(onClick = it) { Text(if (revealed) stringResource(R.string.hide) else stringResource(R.string.show), color = Accent, fontSize = 13.sp) } }
        },
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = TextMain,
            unfocusedTextColor = TextMain,
            focusedBorderColor = Accent,
            unfocusedBorderColor = CardBorder,
            focusedLabelColor = Accent,
            unfocusedLabelColor = TextSoft,
            cursorColor = Accent,
            focusedPlaceholderColor = TextSoft.copy(alpha = 0.5f),
            unfocusedPlaceholderColor = TextSoft.copy(alpha = 0.5f),
        ),
        modifier = Modifier.fillMaxWidth()
    )
}

/* ---------------------------------------------------------------- 3. izin */

private fun notificationsAllowed(context: Context) = Notifier.canNotify(context)

private fun backgroundAllowed(context: Context): Boolean {
    val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
    return pm.isIgnoringBatteryOptimizations(context.packageName)
}

private fun openAppSettings(context: Context) {
    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}"))
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    context.startActivity(intent)
}

private fun openNotificationSettings(context: Context) {
    val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
        .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    try {
        context.startActivity(intent)
    } catch (e: ActivityNotFoundException) {
        openAppSettings(context)
    }
}

/**
 * Buka halaman "Mulai otomatis / Autostart" bawaan merek HP. Tiap merek punya halaman sendiri;
 * dicoba satu per satu, kalau tidak ada yang cocok dibuka pengaturan aplikasi biasa.
 */
private fun openAutostartSettings(context: Context) {
    val candidates = listOf(
        // Xiaomi / Redmi / POCO (MIUI & HyperOS)
        "com.miui.securitycenter" to "com.miui.permcenter.autostart.AutoStartManagementActivity",
        // Oppo / Realme / OnePlus (ColorOS & Realme UI baru)
        "com.oplus.battery" to "com.oplus.startupapp.view.StartupAppListActivity",
        "com.coloros.safecenter" to "com.coloros.safecenter.startupapp.StartupAppListActivity",
        "com.coloros.safecenter" to "com.coloros.safecenter.permission.startup.StartupAppListActivity",
        "com.oppo.safe" to "com.oppo.safe.permission.startup.StartupAppListActivity",
        "com.coloros.safecenter" to "com.coloros.privacypermissionsentry.PermissionTopActivity",
        // Vivo / iQOO
        "com.vivo.permissionmanager" to "com.vivo.permissionmanager.activity.BgStartUpManagerActivity",
        "com.iqoo.secure" to "com.iqoo.secure.ui.phoneoptimize.BgStartUpManager",
        "com.iqoo.secure" to "com.iqoo.secure.ui.phoneoptimize.AddWhiteListActivity",
        // Infinix / Tecno / itel (Transsion)
        "com.transsion.phonemaster" to "com.cyin.himgr.autostart.AutoStartActivity",
        // Huawei / Honor
        "com.huawei.systemmanager" to "com.huawei.systemmanager.startupmgr.ui.StartupNormalAppListActivity",
        "com.huawei.systemmanager" to "com.huawei.systemmanager.optimize.process.ProtectActivity",
        "com.hihonor.systemmanager" to "com.hihonor.systemmanager.startupmgr.ui.StartupNormalAppListActivity",
        // Asus, Letv
        "com.asus.mobilemanager" to "com.asus.mobilemanager.autostart.AutoStartActivity",
        "com.letv.android.letvsafe" to "com.letv.android.letvsafe.AutobootManageActivity",
    )
    for ((pkg, cls) in candidates) {
        try {
            context.startActivity(
                Intent().setComponent(android.content.ComponentName(pkg, cls))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
            return
        } catch (e: Exception) {
            // halaman tidak ada di HP ini, coba berikutnya
        }
    }
    openAppSettings(context)
}

@android.annotation.SuppressLint("BatteryLife")
private fun requestBackground(context: Context) {
    try {
        context.startActivity(
            Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, Uri.parse("package:${context.packageName}"))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    } catch (e: Exception) {
        try {
            context.startActivity(
                Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        } catch (e2: Exception) {
            openAppSettings(context)
        }
    }
}

@Composable
private fun PermissionsPage(onFinished: () -> Unit) {
    val context = LocalContext.current
    var notifOk by remember { mutableStateOf(notificationsAllowed(context)) }
    var bgOk by remember { mutableStateOf(backgroundAllowed(context)) }
    var askedNotif by rememberSaveable { mutableStateOf(false) }

    // cek ulang setiap kembali dari layar pengaturan HP
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        notifOk = notificationsAllowed(context)
        bgOk = backgroundAllowed(context)
    }
    val notifLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        notifOk = notificationsAllowed(context)
    }

    OnePage(
        top = {
            PageHeader(
                stringResource(R.string.ob_perm_title),
                stringResource(R.string.ob_perm_sub)
            )
            Spacer(Modifier.height(14.dp))
            PermissionCard(
                icon = Icons.Filled.Notifications,
                title = stringResource(R.string.ob_notif_title),
                text = stringResource(R.string.ob_notif_text),
                granted = notifOk,
                button = if (askedNotif) stringResource(R.string.open) else stringResource(R.string.allow),
                onClick = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !askedNotif) {
                        askedNotif = true
                        notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    } else {
                        openNotificationSettings(context)
                    }
                }
            )
            Spacer(Modifier.height(10.dp))
            PermissionCard(
                icon = Icons.Filled.Refresh,
                title = stringResource(R.string.ob_bg_title),
                text = stringResource(R.string.ob_bg_text),
                granted = bgOk,
                button = stringResource(R.string.allow),
                onClick = { requestBackground(context) }
            )
            Spacer(Modifier.height(10.dp))

            // ---- opsional untuk HP yang agresif menutup aplikasi
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(CardBg)
                    .border(1.dp, Warn.copy(alpha = 0.6f), RoundedCornerShape(18.dp))
                    .padding(14.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "Xiaomi, Oppo, Vivo, Realme & Infinix",
                        color = Warn,
                        fontSize = 14.sp,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        stringResource(R.string.optional),
                        color = Warn,
                        fontSize = 10.sp,
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Warn.copy(alpha = 0.15f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    stringResource(R.string.ob_autostart_text),
                    color = TextSoft,
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                )
                Spacer(Modifier.height(8.dp))
                Box(
                    Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .border(BorderStroke(1.dp, Warn.copy(alpha = 0.7f)), RoundedCornerShape(12.dp))
                        .clickable { openAutostartSettings(context) }
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) { Text(stringResource(R.string.ob_autostart_button), color = Warn, fontSize = 13.sp) }
            }
        },
        bottom = {
            val ready = notifOk && bgOk
            GradientButton(
                text = if (ready) stringResource(R.string.ob_start) else stringResource(R.string.ob_enable_both),
                enabled = ready,
                onClick = onFinished
            )
        }
    )
}

/** Kartu izin satu baris: ikon, teks, lalu tombol kecil (atau tanda centang) di kanan. */
@Composable
private fun PermissionCard(
    icon: ImageVector,
    title: String,
    text: String,
    granted: Boolean,
    button: String,
    onClick: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(CardBg)
            .border(1.dp, if (granted) Success.copy(alpha = 0.6f) else CardBorder, RoundedCornerShape(18.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (granted) IconTile(Icons.Filled.CheckCircle, tint = Success, bg = Success.copy(alpha = 0.16f), size = 36)
        else IconTile(icon, size = 36)
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(title, color = TextMain, fontSize = 15.sp)
            Text(
                if (granted) "✓ " + stringResource(R.string.ob_granted) else text,
                color = if (granted) Success else TextSoft,
                fontSize = 12.sp,
                lineHeight = 16.sp
            )
        }
        if (!granted) {
            Spacer(Modifier.width(8.dp))
            Box(
                Modifier
                    .clip(RoundedCornerShape(50))
                    .background(ButtonGradient)
                    .clickable(onClick = onClick)
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            ) { Text(button, color = Color.White, fontSize = 13.sp, maxLines = 1) }
        }
    }
}

/* ---------------------------------------------------------------- komponen */

@Composable
private fun DarkCard(done: Boolean, content: @Composable () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(CardBg)
            .border(1.dp, if (done) Success.copy(alpha = 0.6f) else CardBorder, RoundedCornerShape(18.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) { content() }
}

@Composable
private fun AppLogo(sizeDp: Int) {
    Box(
        Modifier
            .size(sizeDp.dp)
            .clip(CircleShape)
            .background(Brush.linearGradient(listOf(Color(0xFF74BBF9), Color(0xFF4F95EE), Color(0xFF2F6FDC)))),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(R.drawable.ic_launcher_foreground),
            contentDescription = null,
            modifier = Modifier.size((sizeDp * 1.5f).dp)
        )
    }
}

@Composable
private fun GradientButton(
    text: String,
    enabled: Boolean,
    onClick: () -> Unit,
    height: Int = 52,
    icon: ImageVector? = null,
) {
    Box(
        Modifier
            .fillMaxWidth()
            .height(height.dp)
            .clip(RoundedCornerShape(height.dp / 2))
            .background(if (enabled) ButtonGradient else Brush.horizontalGradient(listOf(IconBg, IconBg)))
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (icon != null) {
                Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(10.dp))
            }
            Text(
                text,
                color = if (enabled) Color.White else TextSoft.copy(alpha = 0.7f),
                fontSize = 16.sp,
                maxLines = 1
            )
        }
    }
}

/** Pilih bahasa aplikasi; layar dimuat ulang dengan bahasa baru. */
@Composable
fun LanguageDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val current = Lang.current(context)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("🌐 " + stringResource(R.string.language)) },
        text = {
            Column {
                Lang.OPTIONS.forEach { opt ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                onDismiss()
                                if (opt.code != current) {
                                    Lang.set(context, opt.code)
                                    (context as? android.app.Activity)?.recreate()
                                }
                            }
                            .padding(horizontal = 8.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(opt.flag, fontSize = 20.sp)
                        Spacer(Modifier.width(12.dp))
                        Text(
                            opt.label,
                            fontSize = 16.sp,
                            fontWeight = if (opt.code == current) FontWeight.Bold else FontWeight.Normal,
                            modifier = Modifier.weight(1f)
                        )
                        if (opt.code == current) Text("✓", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.close)) } },
    )
}
