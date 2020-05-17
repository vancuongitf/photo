package cuong.cao.photo.camera

import android.content.Context
import android.content.Intent
import android.hardware.Camera
import android.os.Handler
import android.util.Log
import android.view.View
import android.widget.LinearLayout
import android.widget.Toast
import cuong.cao.photo.R
import kotlinx.android.synthetic.main.camera_view.view.*
import java.io.File
import java.util.*

class CameraView(context: Context) : LinearLayout(context) {

    companion object {
        internal var instance: CameraView? = null
    }

    private var mCamera: Camera? = null
    private var mPreview: CameraPreview? = null

    init {
        View.inflate(context, R.layout.camera_view, this)
        // Create an instance of Camera
        mCamera = getCameraInstance()
        mCamera?.setDisplayOrientation(90)
        val parameters = mCamera?.parameters
        parameters?.setRotation(270)
        parameters?.setPictureSize(3200, 1800)
        mCamera?.parameters = parameters
        mPreview = mCamera?.let {
            // Create our Preview view
            CameraPreview(context, it)
        }

        // Set the Preview view as the content of our activity.
        mPreview?.also {
            preview.addView(it)
        }
        mCamera?.stopPreview()
        mCamera?.startPreview()

        Handler().postDelayed({
            try {
                mCamera?.takePicture(
                    { },
                    { _, _ -> },
                    { p0, _ ->
                        try {
                            val file =
                                File(context.filesDir, "${Calendar.getInstance().timeInMillis}.png")
                            val os = file.outputStream()
                            os.write(p0)
                            os.flush()
                            os.close()
                            Toast.makeText(context, "Take Image Success", Toast.LENGTH_LONG).show()
                            Handler().postDelayed({
                                context.sendBroadcast(Intent("completed"))
                            }, 1000)
                        } catch (e: java.lang.Exception) {
                            Log.i("tag11", e.message)
                            context.sendBroadcast(Intent("completed"))
                        }
                    })
            } catch (e: Exception) {
                context.sendBroadcast(Intent("completed"))
            }
        }, 3000)
        viewTreeObserver.addOnGlobalLayoutListener {
            if (instance == null) {
                instance = this
            }
        }
    }

    private fun getCameraInstance(): Camera? {
        return try {
            Camera.open(1)// attempt to get a Camera instance
        } catch (e: Exception) {
            Log.i("tag11", e.message)
            context.sendBroadcast(Intent("completed"))
            // Camera is not available (in use or does not exist)
            null // returns null if camera is unavailable
        }
    }
}
