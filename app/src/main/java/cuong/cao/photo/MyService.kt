package cuong.cao.photo

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.os.SystemClock
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.core.app.NotificationCompat
import androidx.media.VolumeProviderCompat


/**
 * Created by at-cuongcao on 17/05/2020.
 */
class MyService : Service() {
    companion object {
        const val CHANNEL_ID = "ForegroundServiceChannel"
    }

    private val broadcast = Broadcast()
    var mediaSession: MediaSessionCompat? = null
    private var latBroadcastSend = 0L

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()
        mediaSession = MediaSessionCompat(this, "PlayerService")
        mediaSession?.setFlags(
            MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS or
                    MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS
        )
        mediaSession?.setPlaybackState(
            PlaybackStateCompat.Builder()
                .setState(
                    PlaybackStateCompat.STATE_PLAYING,
                    0,
                    0f
                ) //you simulate a player which plays something.
                .build()
        )

        //this will only work on Lollipop and up, see https://code.google.com/p/android/issues/detail?id=224134

        //this will only work on Lollipop and up, see https://code.google.com/p/android/issues/detail?id=224134
        val myVolumeProvider: VolumeProviderCompat = object : VolumeProviderCompat(
            VOLUME_CONTROL_RELATIVE,  /*max volume*/
            100,  /*initial volume level*/
            50
        ) {
            @SuppressLint("InvalidWakeLockTag")
            override fun onAdjustVolume(direction: Int) {
                if (SystemClock.elapsedRealtime() - latBroadcastSend > 500) {
                    sendBroadcast(Intent(Broadcast.ACTION_VOLUME_PRESSED))
                    latBroadcastSend = SystemClock.elapsedRealtime()
                    val screenLock =
                        (getSystemService(Context.POWER_SERVICE) as? PowerManager)?.newWakeLock(
                            PowerManager.SCREEN_BRIGHT_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP,
                            BuildConfig.APPLICATION_ID
                        )
                    screenLock?.acquire(1000)
                }
            }
        }

        mediaSession?.setPlaybackToRemote(myVolumeProvider)
        mediaSession?.isActive = true
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        try {
            registerReceiver(broadcast, IntentFilter().apply {
                addAction(Broadcast.ACTION_VOLUME_PRESSED)
                addAction(Intent.ACTION_SCREEN_ON)
                addAction(Broadcast.ACTION_COMPLETED)
            })
            val audio = (getSystemService(Context.AUDIO_SERVICE) as? AudioManager)
            val rec = ComponentName(
                packageName,
                Broadcast::javaClass.name
            )
            audio?.registerMediaButtonEventReceiver(rec)
            sendNotification()
        } catch (e: Exception) {

        }
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(broadcast)
    }

    private fun sendNotification() {
        createNotificationChannel()
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0, notificationIntent, 0
        )
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Photo Service")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .build()
        startForeground(1, notification)

    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                CHANNEL_ID,
                "Foreground Service Channel",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            val manager = getSystemService(
                NotificationManager::class.java
            )
            manager.createNotificationChannel(serviceChannel)
        }
    }
}