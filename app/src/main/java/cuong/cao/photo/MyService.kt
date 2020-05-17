package cuong.cao.photo

import android.app.Service
import android.content.Intent
import android.os.IBinder

/**
 * Created by at-cuongcao on 17/05/2020.
 */
class MyService : Service() {
    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        return START_STICKY
    }
}