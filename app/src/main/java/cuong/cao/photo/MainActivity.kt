package cuong.cao.photo

import android.Manifest
import android.app.ActivityManager
import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.StatFs
import android.provider.Settings
import android.util.Log
import android.view.KeyEvent
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import cuong.cao.photo.extensions.getDeviceId
import kotlinx.android.synthetic.main.activity_main.*


class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
            (getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager)?.requestDismissKeyguard(
                this,
                null
            )
        } else {
            window.addFlags(
                WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD or
                        WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                        WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                        WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
            )
        }
    }

    override fun onPause() {
        super.onPause()
        val activityManager = applicationContext
            .getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        activityManager.moveTaskToFront(taskId, 0)
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        return false
    }

    override fun onResume() {
        super.onResume()
        if (Settings.canDrawOverlays(this)) {
            if (!isPermissionGranted(Manifest.permission.CAMERA)
                || !isPermissionGranted(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                || !isPermissionGranted(Manifest.permission.RECEIVE_BOOT_COMPLETED)
                || !isPermissionGranted(Manifest.permission.WAKE_LOCK)
                || !isPermissionGranted(Manifest.permission.READ_PHONE_STATE)
            ) {
                requestPermissions(
                    arrayOf(
                        Manifest.permission.CAMERA,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        Manifest.permission.WAKE_LOCK,
                        Manifest.permission.RECEIVE_BOOT_COMPLETED,
                        Manifest.permission.READ_PHONE_STATE
                    ), 1111
                )
            } else {
                tvClick.text = "Device Id: ${getDeviceId()}"
                val intent = Intent(this, MyService::class.java)
                startService(intent)
            }
        } else {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:${applicationContext.packageName}")
            )
            startActivity(intent)
        }
    }

    private fun checkMemorySize() {
        val stat = StatFs(Environment.getExternalStorageDirectory().path)
        val bytesAvailable = stat.blockSizeLong * stat.availableBlocksLong
        val megAvailable = bytesAvailable / 1048576 / 1024f
        Log.i("tag11", "Size: $megAvailable")
    }

    private fun isPermissionGranted(permission: String) =
        checkSelfPermission(permission) != PackageManager.PERMISSION_DENIED
}
