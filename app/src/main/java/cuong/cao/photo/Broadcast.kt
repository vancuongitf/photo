package cuong.cao.photo

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.SystemClock
import android.util.DisplayMetrics
import android.view.Gravity
import android.view.View
import android.view.WindowManager
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
        internal const val PERIOD_ACTION_TIME = 10000 // Thoi gian giua hai lan chup
        internal const val DELAY_AFTER_BOOT = 120000 // Thoi gian cho sau khi nhan su kien khoi dong xong
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        when (intent?.action) {
            Intent.ACTION_SCREEN_ON, ACTION_VOLUME_PRESSED -> {
                if (SystemClock.elapsedRealtime() - App.getInstance().lastAction > PERIOD_ACTION_TIME && Calendar.getInstance().timeInMillis - DELAY_AFTER_BOOT > App.getInstance().bootime) {
                    App.getInstance().lastAction = SystemClock.elapsedRealtime()
                    val forMobell = false // TODO: doi thanh false cho dien thoai khac
                    if (forMobell) {
                        val intent = Intent(context, CameraActivity::class.java).apply {
                            addFlags(Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT or Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        context?.startActivity(intent)
                    } else {
                        context?.apply {
                            (getSystemService(Context.WINDOW_SERVICE) as? WindowManager)?.let { windowManager ->
                                val view: View = CameraView(context)
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
            }

            ACTION_COMPLETED -> {
                removeViews(context)
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
    }
}
