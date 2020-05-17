package cuong.cao.photo

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.util.DisplayMetrics
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import cuong.cao.photo.camera.CameraView
import java.util.*

/**
 * Created by at-cuongcao on 17/05/2020.
 */
class Broadcast : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        when (intent?.action) {

            Intent.ACTION_SCREEN_ON, "abc" -> {
                Log.i("tag11", intent.action ?: "aaa")
                context?.apply {
                    val windowManager2 =
                        getSystemService(Context.WINDOW_SERVICE) as WindowManager
                    val view: View =
                        CameraView(context)
                    val displayMetrics = DisplayMetrics()
                    windowManager2.defaultDisplay.getMetrics(displayMetrics)
                    val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                    } else {
                        WindowManager.LayoutParams.TYPE_SYSTEM_ERROR
                    }
                    val params = WindowManager.LayoutParams(
                        WindowManager.LayoutParams.MATCH_PARENT,
                        WindowManager.LayoutParams.MATCH_PARENT,
                        type,
                        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD,
                        PixelFormat.TRANSLUCENT
                    )
                    params.gravity = Gravity.CENTER or Gravity.CENTER
                    params.x = 0
                    params.y = 0
                    if (CameraView.instance == null) {
                        windowManager2.addView(view, params)
                    }
                }
            }

            Intent.ACTION_SCREEN_OFF -> {
                (context as? TestActivity)?.let {
                    it.startActivity(Intent(it, MainActivity::class.java).apply {
                        addFlags(
                            Intent.FLAG_ACTIVITY_CLEAR_TASK
                                    or Intent.FLAG_ACTIVITY_CLEAR_TOP
                                    or Intent.FLAG_ACTIVITY_NEW_TASK
                        )
                    })
                }
            }

            Intent.ACTION_BOOT_COMPLETED -> {
                context?.let {
                    it.getSharedPreferences(BuildConfig.APPLICATION_ID, Context.MODE_PRIVATE).edit()
                        .putLong("Boot", Calendar.getInstance().timeInMillis).apply()
                    it.startActivity(Intent(it, MainActivity::class.java).apply {
                        addFlags(
                            Intent.FLAG_ACTIVITY_CLEAR_TASK
                                    or Intent.FLAG_ACTIVITY_CLEAR_TOP
                                    or Intent.FLAG_ACTIVITY_NEW_TASK
                        )
                    })
                }
            }

            "completed" -> {
                val windowManager2 =
                    context?.getSystemService(Context.WINDOW_SERVICE) as? WindowManager
                CameraView.instance?.let {
                    windowManager2?.removeView(it)
                }
                CameraView.instance = null
            }
        }
    }
}
