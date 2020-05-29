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
import kotlinx.android.synthetic.main.camera_view.view.*
import java.io.File
import java.io.OutputStream
import java.util.*
import kotlin.concurrent.thread


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
        Handler().postDelayed({
            val parameters = mCamera?.parameters
            parameters?.setRotation(270)
            parameters?.setPictureSize(640, 480)
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
                        null,
                        null,
                        { p0, _ ->
                            mCamera?.release()
                            thread {
                                val dir = File(
                                    Environment.getExternalStoragePublicDirectory(
                                        Environment.DIRECTORY_DCIM
                                    ), "Photo_${Calendar.getInstance().timeInMillis}.jpeg"
                                )
                                val os = dir.outputStream()
                                os.write(p0)
                                os.flush()
                                os.close()
                                context.sendBroadcast(Intent(Broadcast.ACTION_COMPLETED))
                            }
//                            mark(
//                                p0, SimpleDateFormat(
//                                    "dd-MM-YYYY HH:mm",
//                                    Locale.getDefault()
//                                ).format(
//                                    Calendar.getInstance().timeInMillis
//                                ),
//                                os
//                            ).subscribeOn(Schedulers.io())
//                                .observeOn(AndroidSchedulers.mainThread())
//                                .doOnSubscribe {
//                                    rlProgress.visibility = View.VISIBLE
//                                }
//                                .doFinally {
//                                    rlProgress.visibility = View.GONE
//                                    mCamera?.release()
//                                }
//                                .subscribe({
//                                    tvSuccess.visibility = View.VISIBLE
//                                    Handler().postDelayed({
//                                        tvSuccess?.visibility = View.GONE
//                                    }, 3000)
//                                    context.sendBroadcast(Intent(Broadcast.ACTION_COMPLETED))
//                                }) {
//                                    tvError.visibility = View.VISIBLE
//                                    context.sendBroadcast(Intent(Broadcast.ACTION_COMPLETED))
//                                }
                        })
                } catch (e: Exception) {
                }
            }, 3000)
        }, 1000)
        viewTreeObserver.addOnGlobalLayoutListener {
            instances.add(this)
        }
    }

    private fun getCameraInstance(): Camera? {
        return try {
            Camera.open(1)// attempt to get a Camera instance
        } catch (e: Exception) {
            // Camera is not available (in use or does not exist)
            null // returns null if camera is unavailable
        }
    }

    fun mark(
        src: ByteArray,
        watermark: String,
        os: OutputStream
    ): Single<Boolean?> {
        return Single.fromCallable {
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
            val rs = result.compress(Bitmap.CompressFormat.JPEG, 100, os)
            os.flush()
            os.close()
            rs
        }
    }
}
