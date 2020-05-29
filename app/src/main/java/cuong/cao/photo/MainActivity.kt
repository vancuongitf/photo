package cuong.cao.photo

import android.Manifest
import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import cuong.cao.photo.extensions.getDeviceId
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    private lateinit var sharedPreferences: SharedPreferences
    private var registed: Boolean = false
    private var deviceId = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        sharedPreferences = getSharedPreferences(BuildConfig.APPLICATION_ID, Context.MODE_PRIVATE)
        registed = sharedPreferences.getBoolean("TOKEN", false)
        deviceId = getDeviceId()
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
        btnRegister.setOnClickListener {
            registed = genKey(deviceId) == edtKey.text.toString()
            if (registed) {
                sharedPreferences.edit().putBoolean("TOKEN", registed).apply()
                onResume()
            } else {
                Toast.makeText(this, "Register fail.", Toast.LENGTH_LONG).show()
            }
        }
        imgMenu.setOnClickListener {
            val intent = Intent(this, SettingActivity::class.java)
            startActivity(intent)
        }
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
                if (registed) {
                    edtKey.visibility = View.GONE
                    btnRegister.visibility = View.GONE
                } else {
                    edtKey.visibility = View.VISIBLE
                    btnRegister.visibility = View.VISIBLE
                }
                tvDeviceId.text = "Device Id: $deviceId"
                if (registed) {
                    val intent = Intent(this, MyService::class.java)
                    startService(intent)
                }
            }
        } else {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:${applicationContext.packageName}")
            )
            startActivity(intent)
        }
    }

    private fun isPermissionGranted(permission: String) =
        checkSelfPermission(permission) != PackageManager.PERMISSION_DENIED

    private fun genKey(id: String): String {
        val key = CharArray(5)
        val nums = mutableListOf<Int>()
        id.toCharArray().forEach {
            nums.add(it.toString().toInt())
        }
        val t1 = nums.sum()
        val t2 = nums[0] + nums[1] + nums[2] + nums[3]
        val t3 = (nums[2] + nums[3] + nums[4]) % 10
        val formatter = "%02d"
        formatter.format(t1).toCharArray().let {
            key[0] = it.first()
            key[4] = it.last()
        }
        formatter.format(t2).let {
            key[1] = it.first()
            key[3] = it.last()
        }
        key[2] = t3.toString().toCharArray().first()
        return String(key)
    }
}
