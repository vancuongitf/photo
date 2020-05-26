package cuong.cao.photo.camera

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.*
import android.hardware.camera2.*
import android.hardware.camera2.CameraCaptureSession.CaptureCallback
import android.media.ExifInterface
import android.media.Image
import android.media.ImageReader
import android.media.ImageReader.OnImageAvailableListener
import android.os.Environment
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import android.util.Size
import android.util.SparseIntArray
import android.view.Surface
import android.view.TextureView
import android.view.View
import android.widget.RelativeLayout
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool
import com.bumptech.glide.load.resource.bitmap.BitmapTransformation
import com.bumptech.glide.load.resource.bitmap.TransformationUtils
import com.bumptech.glide.request.target.SimpleTarget
import com.bumptech.glide.request.transition.Transition
import cuong.cao.photo.Broadcast
import cuong.cao.photo.R
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.camera2_view.view.*
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStream
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.*


/**
 * Created by at-cuongcao on 23/05/2020.
 */
open class Camera2View(context: Context) : RelativeLayout(context) {

    companion object {
        internal var instances: MutableSet<Camera2View> = mutableSetOf()
    }

    private var cameraId: String? = null
    private var cameraDevice: CameraDevice? = null
    private var cameraCaptureSessions: CameraCaptureSession? = null
    private var captureRequest: CaptureRequest? = null
    private var captureRequestBuilder: CaptureRequest.Builder? = null
    private var imageDimension: Size? = null
    private val imageReader: ImageReader? = null
    private val orientations = SparseIntArray()
    private val mFlashSupported = false
    private var mBackgroundHandler: Handler? = null
    private var mBackgroundThread: HandlerThread? = null

    private val surfaceTextureListener = object : TextureView.SurfaceTextureListener {
        override fun onSurfaceTextureSizeChanged(
            surface: SurfaceTexture?,
            width: Int,
            height: Int
        ) = Unit

        override fun onSurfaceTextureUpdated(surface: SurfaceTexture?) = Unit

        override fun onSurfaceTextureDestroyed(surface: SurfaceTexture?): Boolean {
            return false
        }

        override fun onSurfaceTextureAvailable(
            surface: SurfaceTexture?,
            width: Int,
            height: Int
        ) {
            openCamera()
        }
    }
    private val stateCallback: CameraDevice.StateCallback = object : CameraDevice.StateCallback() {
        override fun onOpened(camera: CameraDevice) {
            cameraDevice = camera
            createCameraPreview()
        }

        override fun onDisconnected(camera: CameraDevice) {
            cameraDevice?.close();
        }

        override fun onError(camera: CameraDevice, error: Int) {
            cameraDevice?.close()
            cameraDevice = null
        }

    }

    init {
        View.inflate(context, R.layout.camera2_view, this)
        initData()
        Handler().postDelayed({
            startBackgroundThread()
            openCamera()
        }, 1000)
        Handler().postDelayed({
            takePicture()
        }, 3000)
        viewTreeObserver.addOnGlobalLayoutListener {
            instances.add(this)
        }
        imgClose.setOnClickListener {
            context.sendBroadcast(Intent(Broadcast.ACTION_COMPLETED))
        }
    }

    private fun initData() {
        orientations.append(Surface.ROTATION_0, 90)
        orientations.append(Surface.ROTATION_90, 0)
        orientations.append(Surface.ROTATION_180, 270)
        orientations.append(Surface.ROTATION_270, 180)
        preview.surfaceTextureListener = surfaceTextureListener
    }

    private fun openCamera() {
        (context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager)?.let { manager ->
            try {
                cameraId = manager.cameraIdList[1]
                val characteristics = manager.getCameraCharacteristics(cameraId ?: "")
                characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
                    ?.getOutputSizes(ImageFormat.JPEG)?.firstOrNull()?.let {
                        preview?.layoutParams?.height =
                            (it.width.toFloat() * preview.width / it.height).toInt()
                        preview?.requestLayout()
                    }
                val map =
                    characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)!!
                imageDimension = map.getOutputSizes(SurfaceTexture::class.java)[0]
                // Add permission for camera and let user grant the permission
                if (ActivityCompat.checkSelfPermission(
                        context,
                        Manifest.permission.CAMERA
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    return
                }
                manager.openCamera(cameraId ?: "", stateCallback, null)
            } catch (e: CameraAccessException) {
                e.printStackTrace()
            }
        }
    }

    private fun createCameraPreview() {
        try {
            preview.surfaceTexture.setDefaultBufferSize(
                imageDimension?.width ?: 640,
                imageDimension?.height ?: 480
            )
            val surface = Surface(preview.surfaceTexture)
            captureRequestBuilder =
                cameraDevice?.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
            captureRequestBuilder?.addTarget(surface)
            cameraDevice?.createCaptureSession(
                Arrays.asList(surface),
                object : CameraCaptureSession.StateCallback() {
                    override fun onConfigured(cameraCaptureSession: CameraCaptureSession) {
                        //The camera is already closed
                        if (null == cameraDevice) {
                            return
                        }
                        // When the session is ready, we start displaying the preview.
                        cameraCaptureSessions = cameraCaptureSession
                        updatePreview()
                    }

                    override fun onConfigureFailed(cameraCaptureSession: CameraCaptureSession) {
                        Toast.makeText(
                            context,
                            "Configuration change",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                },
                null
            )
        } catch (e: CameraAccessException) {
            e.printStackTrace()
        }
    }

    private fun updatePreview() {
        if (null == cameraDevice) {
            return
        }
        captureRequestBuilder?.let { captureRequestBuilder ->
            captureRequestBuilder.set(
                CaptureRequest.CONTROL_MODE,
                CameraMetadata.CONTROL_MODE_AUTO
            )
            try {
                cameraCaptureSessions?.setRepeatingRequest(
                    captureRequestBuilder.build(),
                    null,
                    mBackgroundHandler
                )
            } catch (e: CameraAccessException) {
                e.printStackTrace()
            }
        }
    }

    private fun startBackgroundThread() {
        mBackgroundThread = HandlerThread("Camera Background")
        mBackgroundThread?.let { mBackgroundThread ->
            mBackgroundThread.start()
            mBackgroundHandler = Handler(mBackgroundThread.getLooper())
        }
    }

    private fun stopBackgroundThread() {
        mBackgroundThread?.quitSafely()
        try {
            mBackgroundThread?.join()
            mBackgroundThread = null
            mBackgroundHandler = null
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }
    }

    private fun takePicture() {
        cameraDevice?.let { cameraDevice ->
            val manager = context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager
            try {
                val characteristics = manager?.getCameraCharacteristics(cameraDevice.id)
                var captureResult: CaptureResult? = null
                var jpegSizes: Array<Size>? = null
                if (characteristics != null) {
                    jpegSizes =
                        characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
                            ?.getOutputSizes(ImageFormat.JPEG)
                }
                var width = 640
                var height = 480
                if (jpegSizes != null && jpegSizes.isNotEmpty()) {
                    width = jpegSizes[0].width
                    height = jpegSizes[0].height
                }
                Log.i("tag11", "dcmm: $width --- $height")
                val reader = ImageReader.newInstance(
                    width,
                    height,
                    ImageFormat.RAW_SENSOR,
                    1
                )

                val outputSurfaces: MutableList<Surface> =
                    ArrayList(2)
                outputSurfaces.add(reader.surface)
                outputSurfaces.add(Surface(preview.surfaceTexture))
                val captureBuilder =
                    cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE)
                captureBuilder.addTarget(reader.surface)
                captureBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO)
                captureBuilder.set(CaptureRequest.JPEG_ORIENTATION, 270)
                val file =
                    File(context.cacheDir, "Photo_${Calendar.getInstance().timeInMillis}.dng")
                val readerListener: OnImageAvailableListener = object : OnImageAvailableListener {
                    override fun onImageAvailable(reader: ImageReader) {
                        Single.fromCallable {
                            1
                        }.observeOn(AndroidSchedulers.mainThread())
                            .subscribe({
                                rlProgress.visibility = View.VISIBLE
                            }) {}
                        var image: Image? = null
                        try {
                            image = reader.acquireLatestImage()
                            val dng = DngCreator(characteristics!!, captureResult!!)
                            val outputStream = FileOutputStream(file)
                            dng.writeImage(outputStream, image)
                            Glide.with(context)
                                .asBitmap()
                                .transform(MyTransformation(context, 270))
                                .load(file)
                                .into(object : SimpleTarget<Bitmap>() {
                                    override fun onResourceReady(
                                        resource: Bitmap,
                                        transition: Transition<in Bitmap>?
                                    ) {
                                        Single.fromCallable {
                                            val dir = File(
                                                Environment.getExternalStoragePublicDirectory(
                                                    Environment.DIRECTORY_DCIM
                                                ),
                                                "Photo_${Calendar.getInstance().timeInMillis}.png"
                                            )
                                            val os = dir.outputStream()
                                            mark(
                                                resource,
                                                SimpleDateFormat(
                                                    "dd-MM-YYYY HH:mm",
                                                    Locale.getDefault()
                                                ).format(
                                                    Calendar.getInstance().timeInMillis
                                                )
                                            )?.compress(Bitmap.CompressFormat.PNG, 100, os)
                                            os.flush()
                                            os.close()
                                        }.subscribeOn(Schedulers.io())
                                            .observeOn(AndroidSchedulers.mainThread())
                                            .doFinally {
                                                rlProgress.visibility = View.GONE
                                            }
                                            .subscribe({
                                                tvSuccess.visibility = View.VISIBLE
                                                Handler().postDelayed({
                                                    tvSuccess.visibility = View.GONE
                                                }, 3000)
                                            }) {

                                            }
                                    }
                                })


                        } catch (e: Exception) {
                            e.printStackTrace()
                            Single.fromCallable {
                                1
                            }.observeOn(AndroidSchedulers.mainThread())
                                .subscribe({
                                    rlProgress.visibility = View.VISIBLE
                                }) {}
                        } finally {
                            image?.close()
                        }
                    }

                    @Throws(IOException::class)
                    private fun save(bytes: ByteArray) {
                        var output: OutputStream? = null
                        try {
                            output = FileOutputStream(file)
                            output.write(bytes)
                        } finally {
                            output?.close()
                        }
                    }
                }
                reader.setOnImageAvailableListener(readerListener, mBackgroundHandler)
                val captureListener: CaptureCallback = object : CaptureCallback() {
                    override fun onCaptureCompleted(
                        session: CameraCaptureSession,
                        request: CaptureRequest,
                        result: TotalCaptureResult
                    ) {
                        super.onCaptureCompleted(session, request, result)
                        captureResult = result
                        Toast.makeText(context, "Saved:$file", Toast.LENGTH_SHORT).show()
//                        createCameraPreview()
                    }
                }
                cameraDevice.createCaptureSession(
                    outputSurfaces,
                    object : CameraCaptureSession.StateCallback() {
                        override fun onConfigured(session: CameraCaptureSession) {
                            try {
                                session.capture(
                                    captureBuilder.build(),
                                    captureListener,
                                    mBackgroundHandler
                                )
                            } catch (e: CameraAccessException) {
                                e.printStackTrace()
                            }
                        }

                        override fun onConfigureFailed(session: CameraCaptureSession) {}
                    },
                    mBackgroundHandler
                )
            } catch (e: CameraAccessException) {
                e.printStackTrace()
            }
        }
    }

    fun mark(
        bitmap: Bitmap,
        watermark: String
    ): Bitmap? {
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

    class MyTransformation(val context: Context?, val rotate: Int) : BitmapTransformation() {
        override fun updateDiskCacheKey(messageDigest: MessageDigest) = Unit
        override fun transform(
            pool: BitmapPool,
            toTransform: Bitmap,
            outWidth: Int,
            outHeight: Int
        ): Bitmap {
            val exifOrientationDegrees = getExifOrientationDegrees(rotate)
            return TransformationUtils.rotateImageExif(pool, toTransform, exifOrientationDegrees)
        }

        private fun getExifOrientationDegrees(orientation: Int): Int {
            return when (orientation) {
                90 -> ExifInterface.ORIENTATION_ROTATE_90
                270 -> ExifInterface.ORIENTATION_ROTATE_270
                else -> ExifInterface.ORIENTATION_NORMAL
            }
        }
    }
}
