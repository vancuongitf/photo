package cuong.cao.photo.camera

import android.content.Context
import android.content.Intent
import android.graphics.*
import android.hardware.Camera
import android.os.Environment
import android.os.Handler
import android.util.Log
import android.view.View
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
import cuong.cao.photo.Broadcast
import cuong.cao.photo.R
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.camera_view.view.*
import java.io.File
import java.text.SimpleDateFormat
import java.util.*


class CameraView(context: Context) : LinearLayout(context) {

    companion object {
        internal var instances: MutableSet<CameraView> = mutableSetOf()
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
                        Single.fromCallable {
                            val dir = File(
                                Environment.getExternalStoragePublicDirectory(
                                    Environment.DIRECTORY_DCIM
                                ), "Photo_${Calendar.getInstance().timeInMillis}.png"
                            )
                            val os = dir.outputStream()
                            mark(
                                p0,
                                SimpleDateFormat("dd-MM-YYYY HH:mm", Locale.getDefault()).format(
                                    Calendar.getInstance().timeInMillis
                                )
                            )?.compress(Bitmap.CompressFormat.PNG, 100, os)
                            os.flush()
                            os.close()
                        }.subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .doOnSubscribe {
                                rlProgress.visibility = View.VISIBLE
                            }
                            .doFinally {
                                rlProgress.visibility = View.GONE
                            }
                            .subscribe({
                                tvSuccess.visibility = View.VISIBLE
                                Handler().postDelayed({
                                    tvSuccess?.visibility = View.GONE
                                    mCamera?.startPreview()
                                }, 3000)
                            }) {
                                tvError.visibility = View.VISIBLE
                                Handler().postDelayed({
                                    tvError?.visibility = View.GONE
                                    mCamera?.startPreview()
                                }, 3000)
                            }
                    })
            } catch (e: Exception) {

            }
        }, 3000)
        viewTreeObserver.addOnGlobalLayoutListener {
            instances.add(this)
        }
        imgClose.setOnClickListener {
            mCamera?.release()
            context.sendBroadcast(Intent(Broadcast.ACTION_COMPLETED))
        }
    }

    private fun getCameraInstance(): Camera? {
        return try {
            Camera.open(1)// attempt to get a Camera instance
        } catch (e: Exception) {
            Log.i("tag11", e.message)
            // Camera is not available (in use or does not exist)
            null // returns null if camera is unavailable
        }
    }

    fun mark(
        src: ByteArray,
        watermark: String
    ): Bitmap? {
        val bitmap = BitmapFactory.decodeByteArray(src, 0, src.size)
        val w = bitmap.width
        val h = bitmap.height
        val result = Bitmap.createBitmap(w, h, bitmap.config)
        val canvas = Canvas(result)
        canvas.drawBitmap(bitmap, 0f, 0f, null)
        val paint = Paint()
        val textSize = bitmap.height * 0.04f
        paint.textSize = textSize
        paint.isAntiAlias = true
        paint.style = Paint.Style.FILL_AND_STROKE
        paint.color = ContextCompat.getColor(context, R.color.colorWhite50Percent)
        val textBound = Rect()
        paint.getTextBounds(watermark, 0, watermark.length, textBound)
        val bgRect = RectF(
            bitmap.width.toFloat() * 0.91f - textBound.width(),
            bitmap.height * 0.94f - textBound.height(),
            bitmap.width.toFloat() * 0.97f,
            bitmap.height.toFloat() * 0.98f
        )
        canvas.drawRect(
            bgRect, paint
        )
        paint.color = Color.BLACK
        canvas.drawText(
            watermark,
            bgRect.left + bitmap.width * 0.03f,
            bitmap.height * 0.96f,
            paint
        )
        return result
    }
}
