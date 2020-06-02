package cuong.cao.photo

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.SystemClock
import android.util.DisplayMetrics
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import cuong.cao.photo.camera.Camera2View
import cuong.cao.photo.camera.CameraActivity
import cuong.cao.photo.camera.CameraView
import java.util.*

/**
 * Created by at-cuongcao on 17/05/2020.
 */
class Broadcast : BroadcastReceiver() {

    companion object {
        internal const val ACTION_VOLUME_PRESSED = "action_volume_pressed"
        internal const val ACTION_COMPLETED = "action_completed"
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        when (intent?.action) {
            Intent.ACTION_SCREEN_ON, ACTION_VOLUME_PRESSED -> {
                val intent = Intent(context, CameraActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT or Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context?.startActivity(intent)

                // TODO: Code dung cho may moi.
                return
                if (SystemClock.elapsedRealtime() - App.getInstance().lastAction > 10000 && Calendar.getInstance().timeInMillis - 120000 > App.getInstance().bootime) {
                    App.getInstance().lastAction = SystemClock.elapsedRealtime()
                    context?.apply {
                        (getSystemService(Context.WINDOW_SERVICE) as? WindowManager)?.let { windowManager ->
                            val view: View = Camera2View(context)
                            val displayMetrics = DisplayMetrics()
                            windowManager.defaultDisplay.getMetrics(displayMetrics)
                            removeViews(context)
                            val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                            } else {
                                WindowManager.LayoutParams.TYPE_SYSTEM_ERROR
                            }
                            val params = WindowManager.LayoutParams(
                                WindowManager.LayoutParams.MATCH_PARENT,
                                view.context.resources.displayMetrics.heightPixels,
                                type,
                                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD,
                                PixelFormat.TRANSLUCENT
                            )
                            params.gravity = Gravity.CENTER
                            params.x = 0
                            params.y = 0
                            windowManager.addView(view, params)
                        }
                    }
                }
            }

            Intent.ACTION_BOOT_COMPLETED, "android.intent.action.QUICKBOOT_POWERON", Intent.ACTION_LOCKED_BOOT_COMPLETED, Intent.ACTION_REBOOT, "android.intent.action.USER_PRESENT" -> {
                App.getInstance().bootime = Calendar.getInstance().timeInMillis
                context?.getSharedPreferences(BuildConfig.APPLICATION_ID, Context.MODE_PRIVATE)
                    ?.edit()?.putLong("xxx", App.getInstance().bootime)?.apply()
                Handler().postDelayed({
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        context?.startForegroundService(Intent(context, MyService::class.java))
                    } else {
                        context?.startService(Intent(context, MyService::class.java))
                    }
                }, 120000)
            }

            ACTION_COMPLETED -> {
                removeViews(context)
            }

            else -> {
                context?.getSharedPreferences(BuildConfig.APPLICATION_ID, Context.MODE_PRIVATE)
                    ?.edit()?.putString("xxx", intent?.action ?: "asdasd")?.apply()
            }
        }
    }

    private fun removeViews(context: Context?) {
        val windowManager2 = context?.getSystemService(Context.WINDOW_SERVICE) as? WindowManager
        CameraView.instances.forEach {
            try {
                windowManager2?.removeView(it)
            } catch (e: Exception) {
            }
        }
        Camera2View.instances.forEach {
            try {
                windowManager2?.removeView(it)
            } catch (e: Exception) {
            }
        }
    }
}
