package com.lumi.stickerize

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import com.lumi.stickerize.ui.LumiApp
import com.lumi.stickerize.ui.MainViewModel
import com.lumi.stickerize.ui.OnboardingScreen
import com.lumi.stickerize.ui.theme.LumiTheme
import com.lumi.stickerize.work.Notifier

class MainActivity : ComponentActivity() {

    private val vm: MainViewModel by viewModels()

    // Bahasa aplikasi (bawaan English) dipakai untuk seluruh layar, terlepas dari bahasa HP.
    override fun attachBaseContext(newBase: android.content.Context) {
        super.attachBaseContext(com.lumi.stickerize.i18n.Lang.wrap(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Notifier.ensureChannels(this)
        setContent {
            // Ukuran font sistem yang sangat besar membuat tiap langkah tidak muat satu layar,
            // jadi skala font di aplikasi dibatasi maksimal normal (100%).
            val density = LocalDensity.current
            CompositionLocalProvider(
                LocalDensity provides Density(density.density, minOf(density.fontScale, 1f))
            ) {
            LumiTheme {
                var onboarded by remember { mutableStateOf(vm.settings.onboardingDone) }
                // Layar pembuka gelap → ikon bar sistem putih; layar utama terang → ikon navigasi gelap
                LaunchedEffect(onboarded) {
                    enableEdgeToEdge(
                        statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
                        navigationBarStyle = if (onboarded) SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
                        else SystemBarStyle.dark(Color.TRANSPARENT),
                    )
                }
                if (onboarded) {
                    LumiApp(vm)
                } else {
                    OnboardingScreen(vm.settings, onFinished = { onboarded = true })
                }
            }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        Notifier.appVisible = true
        Notifier.clearDone(this)
    }

    override fun onStop() {
        Notifier.appVisible = false
        super.onStop()
    }
}
