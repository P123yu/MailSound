package com.example.mailsound

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import com.sun.mail.imap.IMAPFolder
import java.text.DateFormat
import java.util.Date
import java.util.Properties
import javax.mail.Folder
import javax.mail.Session

class MailMonitorService : Service() {

    @Volatile private var running = false
    private var worker: Thread? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startInForeground()
        if (!running) {
            running = true
            worker = Thread { loop() }.also { it.isDaemon = true; it.start() }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        running = false
        worker?.interrupt()
        setStatus("Stopped")
        super.onDestroy()
    }

    private fun startInForeground() {
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, "Mail monitoring", NotificationManager.IMPORTANCE_LOW)
        )
        val notification: Notification = Notification.Builder(this, CHANNEL_ID)
            .setContentTitle("Mail Sound Alert")
            .setContentText("Watching your inbox for new mail")
            .setSmallIcon(android.R.drawable.ic_dialog_email)
            .setOngoing(true)
            .build()
        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(NOTIF_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(NOTIF_ID, notification)
        }
    }

    private fun loop() {
        val prefs = getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        var lastUidNext = -1L
        val pm = getSystemService(PowerManager::class.java)

        while (running) {
            val user = prefs.getString("email", "").orEmpty()
            val pass = prefs.getString("password", "").orEmpty()
            val wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "mailsound:check")
            wl.acquire(2 * 60 * 1000L)
            try {
                if (user.isEmpty() || pass.isEmpty()) {
                    setStatus("Missing email or app password")
                } else {
                    val uidNext = fetchUidNext(user, pass)
                    if (lastUidNext == -1L) {
                        setStatus("Baseline set; watching for new mail")
                    } else if (uidNext > lastUidNext) {
                        AlarmPlayer.play(this)
                        setStatus("New mail detected at " + now())
                    } else {
                        setStatus("Last check OK at " + now())
                    }
                    lastUidNext = uidNext
                }
            } catch (e: InterruptedException) {
                break
            } catch (e: Exception) {
                setStatus("Check failed: " + (e.message ?: e.javaClass.simpleName))
            } finally {
                if (wl.isHeld) wl.release()
            }
            try {
                Thread.sleep(POLL_MS)
            } catch (e: InterruptedException) {
                break
            }
        }
    }

    private fun fetchUidNext(user: String, pass: String): Long {
        val props = Properties().apply {
            put("mail.store.protocol", "imaps")
            put("mail.imaps.host", "imap.gmail.com")
            put("mail.imaps.port", "993")
            put("mail.imaps.ssl.enable", "true")
            put("mail.imaps.connectiontimeout", "15000")
            put("mail.imaps.timeout", "15000")
        }
        val store = Session.getInstance(props).getStore("imaps")
        try {
            store.connect("imap.gmail.com", 993, user, pass)
            val inbox = store.getFolder("INBOX") as IMAPFolder
            inbox.open(Folder.READ_ONLY)
            try {
                return inbox.uidNext
            } finally {
                inbox.close(false)
            }
        } finally {
            try { store.close() } catch (_: Exception) {}
        }
    }

    private fun now(): String = DateFormat.getTimeInstance().format(Date())

    private fun setStatus(text: String) {
        getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString("status", text).apply()
    }

    companion object {
        const val PREFS = "mailsound"
        private const val CHANNEL_ID = "mail_monitor"
        private const val NOTIF_ID = 1
        private const val POLL_MS = 60_000L
    }
}
