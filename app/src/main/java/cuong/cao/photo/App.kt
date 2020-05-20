package cuong.cao.photo

import android.app.Activity
import android.app.Application
import android.content.Context
import android.os.Bundle
import android.util.Log

class App : Application(), Application.ActivityLifecycleCallbacks {

    companion object {
        private lateinit var instance: App

        internal fun getInstance() = instance
    }

    internal var isTop = false
    internal var contextDecor: Context? = null
    internal var lastAction = 0L
    internal var bootime = 0L

    override fun onCreate() {
        super.onCreate()
        instance = this
//        registerActivityLifecycleCallbacks(this)
    }

    override fun onActivityPaused(p0: Activity) {
        Log.i("tag11", "onActivityPaused: $p0")

    }

    override fun onActivityStarted(p0: Activity) {
        Log.i("tag11", "onActivityStarted: $p0")
    }

    override fun onActivityDestroyed(p0: Activity) {

    }

    override fun onActivitySaveInstanceState(p0: Activity, p1: Bundle) {

    }

    override fun onActivityStopped(p0: Activity) {
    }

    override fun onActivityCreated(p0: Activity, p1: Bundle?) {

    }

    override fun onActivityResumed(p0: Activity) {
        Log.i("tag11", "$isTop")
    }
}