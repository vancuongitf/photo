package cuong.cao.photo

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import java.util.*

/**
 * Created by at-cuongcao on 27/05/2020.
 */
class BootCompleteBroadCast : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        App.getInstance().bootime = Calendar.getInstance().timeInMillis
        context?.getSharedPreferences(BuildConfig.APPLICATION_ID, Context.MODE_PRIVATE)
            ?.edit()?.putLong("xxx", App.getInstance().bootime)?.apply()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context?.startForegroundService(Intent(context, MyService::class.java))
        } else {
            context?.startService(Intent(context, MyService::class.java))
        }
    }
}