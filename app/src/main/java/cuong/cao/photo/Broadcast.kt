package cuong.cao.photo

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

/**
 * Created by at-cuongcao on 17/05/2020.
 */
class Broadcast : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        when (intent?.action) {
            Intent.ACTION_SCREEN_ON -> {
                (context as? MainActivity)?.let {
                    Log.i("tag11", "dcmm")
                    it.startActivity(Intent(it, TestActivity::class.java).apply {
                        addFlags(
                            Intent.FLAG_ACTIVITY_CLEAR_TASK
                                    or Intent.FLAG_ACTIVITY_CLEAR_TOP
                                    or Intent.FLAG_ACTIVITY_NEW_TASK
                        )
                    })
                }
            }

            Intent.ACTION_SCREEN_OFF -> {
                (context as? TestActivity)?.let {
                    Log.i("tag11", "dcmm")
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
                    Log.i("tag11", "dcmm")
                    it.startActivity(Intent(it, MainActivity::class.java).apply {
                        addFlags(
                            Intent.FLAG_ACTIVITY_CLEAR_TASK
                                    or Intent.FLAG_ACTIVITY_CLEAR_TOP
                                    or Intent.FLAG_ACTIVITY_NEW_TASK
                        )
                    })
                }
            }
        }
    }
}
