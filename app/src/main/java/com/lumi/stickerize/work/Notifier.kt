package com.lumi.stickerize.work

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.lumi.stickerize.R

/** Notifikasi proses pembuatan stiker & "stiker sudah jadi". */
object Notifier {

    const val CHANNEL_PROGRESS = "lumi_progress"
    const val CHANNEL_DONE = "lumi_done"
    const val ID_PROGRESS = 1001
    private const val ID_DONE = 1002

    /** true saat layar aplikasi sedang terlihat (diatur oleh MainActivity). */
    @Volatile
    var appVisible = false

    fun ensureChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val nm = context.getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_PROGRESS, "Proses pembuatan stiker", NotificationManager.IMPORTANCE_LOW)
                .apply { description = "Muncul selama AI sedang membuat stiker" }
        )
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_DONE, "Stiker sudah jadi", NotificationManager.IMPORTANCE_HIGH)
                .apply { description = "Memberi tahu saat stiker selesai dibuat" }
        )
    }

    /** Buka aplikasi yang sudah berjalan (tidak membuat layar baru, jadi hasilnya tidak hilang). */
    private fun openAppIntent(context: Context): PendingIntent {
        val intent = (context.packageManager.getLaunchIntentForPackage(context.packageName) ?: Intent())
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
        return PendingIntent.getActivity(
            context, 0, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    fun progress(context: Context, text: String) = NotificationCompat.Builder(context, CHANNEL_PROGRESS)
        .setSmallIcon(R.drawable.ic_notification)
        .setContentTitle("Lumi Stickerize")
        .setContentText(text)
        .setProgress(0, 0, true)
        .setOngoing(true)
        .setOnlyAlertOnce(true)
        .setContentIntent(openAppIntent(context))
        .build()

    fun canNotify(context: Context): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) return false
        return NotificationManagerCompat.from(context).areNotificationsEnabled()
    }

    /** Beri tahu hasil pembuatan. Hanya muncul kalau aplikasi sedang tidak dibuka. */
    fun done(context: Context, title: String, text: String) {
        if (appVisible || !canNotify(context)) return
        ensureChannels(context)
        val n = NotificationCompat.Builder(context, CHANNEL_DONE)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(openAppIntent(context))
            .build()
        try {
            NotificationManagerCompat.from(context).notify(ID_DONE, n)
        } catch (e: SecurityException) {
            // izin dicabut di tengah jalan
        }
    }

    fun clearDone(context: Context) {
        NotificationManagerCompat.from(context).cancel(ID_DONE)
    }
}
