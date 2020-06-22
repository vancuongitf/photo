package cuong.cao.photo.camera

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.*
import android.hardware.Camera
import android.hardware.camera2.CameraManager
import android.os.Handler
import android.view.View
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import cuong.cao.photo.Broadcast
import cuong.cao.photo.BuildConfig
import cuong.cao.photo.ImageSize
import cuong.cao.photo.R
import cuong.cao.photo.extensions.createFileToSave
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.internal.operators.flowable.FlowableInterval
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.camera_view.view.*
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

class CameraView(context: Context) : LinearLayout(context) {

    companion object {
        internal var instances: MutableSet<CameraView> = mutableSetOf()
    }

    private var mCamera: Camera? = null
    private var mPreview: CameraPreview? = null
    private lateinit var sharedPreferences: SharedPreferences
    private val imageSize: ImageSize = ImageSize(640, 480, false)
    private var waitingTime = 3L // TODO: Thoi gian mac dinh cho khi chup anh.

    init {
        View.inflate(context, R.layout.camera_view, this)
        initData()

        // Create an instance of Camera
        mCamera = getCameraInstance()
        mCamera?.setDisplayOrientation(90)
        val parameters = mCamera?.parameters
        parameters?.setRotation(270)
        parameters?.setPictureSize(imageSize.width, imageSize.height)
        parameters?.let {
            mCamera?.parameters = it
            mCamera?.parameters?.flashMode = Camera.Parameters.FLASH_MODE_TORCH
        }
        mPreview = mCamera?.let {
            // Create our Preview view
            CameraPreview(context, it)
        }
        // Set the Preview view as the content of our activity.
        mPreview?.also {
            preview.addView(it)
            turnOnFlash()
        }
        mCamera?.stopPreview()
        mCamera?.startPreview()
        FlowableInterval.interval(0, 1, TimeUnit.SECONDS, Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe {
                if (it <= waitingTime) {
                    tvTime.text = (waitingTime - it).toString()
                    tvTime.visibility = View.VISIBLE
                    if (it == waitingTime) {
                        tvTime.visibility = View.GONE
                        try {
                            mCamera?.takePicture(
                                null,
                                null,
                                { p0, _ ->
                                    mCamera?.release()
                                    val dir = createFileToSave()
                                    val os = dir.outputStream()
                                    mark(
                                        p0, SimpleDateFormat(
                                            "dd-MM-YYYY HH:mm",
                                            Locale.getDefault()
                                        ).format(
                                            Calendar.getInstance().timeInMillis
                                        ),
                                        os
                                    ).subscribeOn(Schedulers.io())
                                        .observeOn(AndroidSchedulers.mainThread())
                                        .doOnSubscribe {
                                            rlProgress.visibility = View.VISIBLE
                                        }
                                        .doFinally {
                                            rlProgress.visibility = View.GONE
                                            mCamera?.release()
                                            turnOffFlash()
                                        }
                                        .subscribe({
                                            tvSuccess.visibility = View.VISIBLE
                                            Handler().postDelayed({
                                                tvSuccess?.visibility = View.GONE
                                                context.sendBroadcast(Intent(Broadcast.ACTION_COMPLETED))
                                            }, 2000)
                                        }) {
                                            tvError.visibility = View.VISIBLE
                                            context.sendBroadcast(Intent(Broadcast.ACTION_COMPLETED))
                                        }
                                })
                        } catch (e: Exception) {
                        }
                    }
                } else {
                    tvTime.visibility = View.GONE
                }
            }
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

    private fun initData() {
        sharedPreferences =
            context.getSharedPreferences(BuildConfig.APPLICATION_ID, Context.MODE_PRIVATE)

        val groupListType = object : TypeToken<List<ImageSize>?>() {}.type
        Gson().fromJson<List<ImageSize>>(
            sharedPreferences.getString("images", "[]"),
            groupListType
        ).find {
            it.selected
        }?.let {
            this.imageSize.width = it.width
            this.imageSize.height = it.height
        }
        waitingTime = sharedPreferences.getLong("time", 3) // TODO: Thoi gian min khi chup anh.
    }

    private fun turnOnFlash() {
        (context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager)?.let {
            try {
                it.setTorchMode(1.toString(), true)
            } catch (e: Exception) {
                try {
                    it.setTorchMode(0.toString(), true)
                } catch (e: Exception) {
                }
            }
        }
    }

    private fun turnOffFlash() {
        (context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager)?.let {
            try {
                it.setTorchMode(1.toString(), false)
            } catch (e: Exception) {
                try {
                    it.setTorchMode(0.toString(), false)
                } catch (e: Exception) {

                }
            }
        }
    }
}
