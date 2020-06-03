package cuong.cao.photo

import android.app.Application

class App : Application() {

    companion object {
        private lateinit var instance: App

        internal fun getInstance() = instance
    }

    internal var lastAction = 0L
    internal var bootime = 0L

    override fun onCreate() {
        super.onCreate()
        instance = this
    }
}