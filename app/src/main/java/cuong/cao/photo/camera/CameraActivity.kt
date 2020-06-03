package cuong.cao.photo.camera

import android.app.ProgressDialog
import android.content.Context
import android.content.SharedPreferences
import android.graphics.*
import android.hardware.Camera
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import cuong.cao.photo.BuildConfig
import cuong.cao.photo.ImageSize
import cuong.cao.photo.R
import cuong.cao.photo.extensions.createFileToSave
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.internal.operators.flowable.FlowableInterval
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_camera.*
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

class CameraActivity : AppCompatActivity() {

    private var mCamera: Camera? = null
    private var mPreview: CameraPreview? = null
    private var progressDialog: ProgressDialog? = null
    private lateinit var sharedPreferences: SharedPreferences
    private val imageSizes = mutableListOf<ImageSize>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_camera)
        sharedPreferences = getSharedPreferences(BuildConfig.APPLICATION_ID, Context.MODE_PRIVATE)

        val groupListType = object : TypeToken<List<ImageSize>?>() {}.type
        imageSizes.addAll(
            Gson().fromJson(
                sharedPreferences.getString("images", "[]"),
                groupListType
            )
        )
        progressDialog = ProgressDialog(this)
        mCamera = getCameraInstance()
        mCamera?.setDisplayOrientation(90)

        val parameters = mCamera?.parameters
        parameters?.flashMode = Camera.Parameters.FLASH_MODE_TORCH
        parameters?.setRotation(270)
        mCamera?.parameters = parameters
        imageSizes.find { it.selected }?.let {
            parameters?.setPictureSize(it.width, it.height)
        }
        parameters?.pictureSize?.let {
            transparent?.layoutParams?.height =
                (it.width * resources.displayMetrics.widthPixels.toFloat() / it.height).toInt()
            transparent?.requestLayout()
            mCamera?.parameters = parameters
        }
        mPreview = mCamera?.let {
            // Create our Preview view
            CameraPreview(this, it)
        }
        // Set the Preview view as the content of our activity.
        mPreview?.also {
            preview.addView(it)
        }
        mCamera?.startPreview()
        val waitingTime = sharedPreferences.getLong("time", 3)
        FlowableInterval(0, 1, TimeUnit.SECONDS, AndroidSchedulers.mainThread())
            .subscribe {
                if (it <= waitingTime) {
                    Toast.makeText(this, (waitingTime - it).toString(), Toast.LENGTH_SHORT).show()
                    if (it == waitingTime) {
                        takeImage()
                    }
                }
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
            paint.color = ContextCompat.getColor(this, R.color.colorWhite50Percent)
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

    private fun takeImage() {
        try {
            mCamera?.takePicture(
                null,
                null,
                { p0, _ ->
                    mCamera?.release()
                    val currentDate = SimpleDateFormat(
                        "YYYYMMddHHmmss",
                        Locale.getDefault()
                    ).format(Calendar.getInstance().timeInMillis)
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
                            progressDialog?.show()
                        }
                        .doFinally {
                            progressDialog?.dismiss()
                            mCamera?.release()
                            finish()
                        }
                        .subscribe({
                            Toast.makeText(this, "Take Image Success!", Toast.LENGTH_LONG)
                                .show()
                        }) {
                            Toast.makeText(this, "Take Image Fail!", Toast.LENGTH_LONG).show()
                        }
                })
        } catch (e: Exception) {
            Toast.makeText(this, "Take image fail!", Toast.LENGTH_LONG).show()
            finish()
        }
    }
}