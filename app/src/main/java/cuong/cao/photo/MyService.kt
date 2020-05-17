package cuong.cao.photo

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.*
import android.content.pm.ResolveInfo
import android.graphics.PixelFormat
import android.media.AudioManager
import android.os.Build
import android.os.IBinder
import android.os.SystemClock
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.core.app.NotificationCompat
import androidx.media.VolumeProviderCompat


/**
 * Created by at-cuongcao on 17/05/2020.
 */
class MyService : Service() {
    companion object {
        private const val NOTIFICATION_ID = 888
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
            override fun onAdjustVolume(direction: Int) {
                if (SystemClock.elapsedRealtime() - latBroadcastSend > 500) {
                    sendBroadcast(Intent("abc"))
                    latBroadcastSend = SystemClock.elapsedRealtime()
                } else {
                    Log.i(
                        "tag11",
                        "vcl: " + latBroadcastSend + "---" + SystemClock.elapsedRealtime()
                    )
                }
            }
        }

        mediaSession?.setPlaybackToRemote(myVolumeProvider)
        mediaSession?.isActive = true
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        registerReceiver(broadcast, IntentFilter().apply {
            addAction("abc")
            addAction(Intent.ACTION_SCREEN_ON)
            addAction("completed")
        })
        val audio = (getSystemService(Context.AUDIO_SERVICE) as? AudioManager)
        val rec = ComponentName(
            packageName,
            Broadcast::javaClass.name
        )
        audio?.registerMediaButtonEventReceiver(rec)
        sendNotification()
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
            .setContentTitle("Foreground Service")
            .setContentText("abcd")
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

    fun startApplication(application_name: String) {
        try {
            val intent = Intent("android.intent.action.MAIN")
            intent.addCategory("android.intent.category.LAUNCHER")
            intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
            val resolveinfo_list: List<ResolveInfo> =
                getPackageManager().queryIntentActivities(intent, 0)
            for (info in resolveinfo_list) {
                Log.i("tag11", info.activityInfo.packageName + "---" + info.activityInfo.name)
            }
        } catch (e: ActivityNotFoundException) {

        }
    }

    private fun launchComponent(packageName: String, name: String) {
        val launch_intent = Intent("android.intent.action.MAIN")
        launch_intent.addCategory("android.intent.category.LAUNCHER")
        launch_intent.component = ComponentName(packageName, name)
        launch_intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        applicationContext.startActivity(launch_intent)
    }

    fun getLayoutParams(): WindowManager.LayoutParams? {
        return WindowManager.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT,
            0,
            0,
            WindowManager.LayoutParams.TYPE_APPLICATION,
            WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                    or WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD,
            PixelFormat.TRANSPARENT
        )
    }

    fun getSystemUiVisibility(): Int {
        return (View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY)
    }
}