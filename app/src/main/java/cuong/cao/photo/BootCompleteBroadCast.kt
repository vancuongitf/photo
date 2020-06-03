package cuong.cao.photo

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import java.util.*

class BootCompleteBroadCast : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        App.getInstance().bootime = Calendar.getInstance().timeInMillis
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context?.startForegroundService(Intent(context, MyService::class.java))
        } else {
            context?.startService(Intent(context, MyService::class.java))
        }
    }
}
