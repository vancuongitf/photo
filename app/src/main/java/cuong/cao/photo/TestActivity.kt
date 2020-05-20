package cuong.cao.photo

import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity


/**
 * Created by at-cuongcao on 17/05/2020.
 */
class TestActivity : AppCompatActivity() {

    companion object {
        private var instance: TestActivity? = null

        internal fun start(context: Context) {
            if (instance != null) {
                if (instance?.isPause == true) {
                    instance?.finish()
                    instance = null
                    Log.i("tag11", "dcmm")
                    context.startActivity(Intent(context, TestActivity::class.java).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    })
                }
            } else {
                Log.i("tag11","asdsad")
                context.startActivity(Intent(context, TestActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                })
            }
        }
    }

    private var isPause = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        instance = this
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
    }

    override fun onResume() {
        super.onResume()
        App.getInstance().isTop = true
        isPause = false
    }

    override fun onDestroy() {
        super.onDestroy()
    }

    override fun onPause() {
        super.onPause()
        isPause = true
        App.getInstance().isTop = false
    }
}
