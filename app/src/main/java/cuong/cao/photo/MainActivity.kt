package cuong.cao.photo

import android.app.ActivityManager
import android.app.KeyguardManager
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.WindowManager
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    private val myBroadcastReceiver = Broadcast()
    private var devicePolicyManager: DevicePolicyManager? = null
    private var activityManager: ActivityManager? = null
    private var compName: ComponentName? = null

    @RequiresApi(Build.VERSION_CODES.M)
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
        devicePolicyManager = getSystemService(DEVICE_POLICY_SERVICE) as? DevicePolicyManager
        activityManager = getSystemService(ACTIVITY_SERVICE) as ActivityManager
        compName = ComponentName(this, MyAdmin::class.java)
        tvClick.setOnClickListener {
            finish()
        }
        if (Settings.canDrawOverlays(this)) {
            startService(Intent(this, MyService::class.java))
        } else {
            val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION)
            startActivity(intent)
        }

//        if (devicePolicyManager?.isAdminActive(compName!!) == true) {
//            startService(Intent(this, MyService::class.java))
//        } else {
//            val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN)
//            intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, compName)
//            intent.putExtra(
//                DevicePolicyManager.EXTRA_ADD_EXPLANATION,
//                "Additional text explaining why we need this permission"
//            )
//            startActivityForResult(intent, 11)
//        }
    }

    override fun onPause() {
        super.onPause()
//        val activityManager = applicationContext
//            .getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
//        activityManager.moveTaskToFront(taskId, 0)
    }

    override fun onBackPressed() {

    }
}
