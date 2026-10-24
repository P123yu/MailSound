package com.example.mailsound

import android.content.Context
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Handler
import android.os.Looper

object AlarmPlayer {
    private val handler = Handler(Looper.getMainLooper())

    fun play(context: Context, seconds: Long = 10) {
        handler.post {
            val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            val ringtone = RingtoneManager.getRingtone(context.applicationContext, uri) ?: return@post
            ringtone.audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ALARM)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
            ringtone.play()
            handler.postDelayed({ if (ringtone.isPlaying) ringtone.stop() }, seconds * 1000)
        }
    }
}
