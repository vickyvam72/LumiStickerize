package com.lumi.stickerize.work

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat

/**
 * Foreground service sederhana: selama AI membuat stiker, service ini menjaga aplikasi tetap
 * hidup walau user pindah ke aplikasi lain atau mematikan layar. Pekerjaannya sendiri tetap
 * berjalan di ViewModel; service hanya menampilkan notifikasi "sedang membuat stiker".
 */
class GenerationService : Service() {

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
            stopSelf()
            return START_NOT_STICKY
        }
        Notifier.ensureChannels(this)
        val text = intent?.getStringExtra(EXTRA_TEXT) ?: getString(com.lumi.stickerize.R.string.notif_working)
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
        } else 0
        try {
            ServiceCompat.startForeground(this, Notifier.ID_PROGRESS, Notifier.progress(this, text), type)
        } catch (e: Exception) {
            // sistem menolak foreground service (jarang); pembuatan tetap jalan selama aplikasi terbuka
            stopSelf()
        }
        return START_NOT_STICKY
    }

    companion object {
        private const val ACTION_STOP = "com.lumi.stickerize.STOP_GENERATION"
        private const val EXTRA_TEXT = "text"

        fun start(context: Context, text: String) {
            try {
                ContextCompat.startForegroundService(
                    context,
                    Intent(context, GenerationService::class.java).putExtra(EXTRA_TEXT, text)
                )
            } catch (e: Exception) {
                // mis. dibatasi sistem; abaikan
            }
        }

        fun stop(context: Context) {
            try {
                context.startService(Intent(context, GenerationService::class.java).setAction(ACTION_STOP))
            } catch (e: Exception) {
                context.stopService(Intent(context, GenerationService::class.java))
            }
        }
    }
}
