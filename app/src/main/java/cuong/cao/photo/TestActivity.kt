package cuong.cao.photo

import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity


/**
 * Created by at-cuongcao on 17/05/2020.
 */
class TestActivity : AppCompatActivity() {

    private val myBroadcastReceiver = Broadcast()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_test1)
        Log.i("tag11", this.toString())
        val mKeyGuardManager = getSystemService(KEYGUARD_SERVICE) as? KeyguardManager
        val mLock = mKeyGuardManager?.newKeyguardLock(TestActivity::class.java.simpleName)
        mLock?.disableKeyguard();
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
        registerReceiver(myBroadcastReceiver, IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_OFF)
        })
    }

    override fun onResume() {
        super.onResume()
        App.getInstance().isTop = true
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(myBroadcastReceiver)
    }

    override fun onPause() {
        super.onPause()
        App.getInstance().isTop = false
    }
}
