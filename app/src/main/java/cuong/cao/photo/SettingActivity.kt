package cuong.cao.photo

import android.content.Context
import android.content.SharedPreferences
import android.hardware.Camera
import android.os.Bundle
import android.widget.RadioButton
import androidx.appcompat.app.AppCompatActivity
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.android.synthetic.main.activity_setting.*

/**
 * Created by at-cuongcao on 28/05/2020.
 */
class SettingActivity : AppCompatActivity() {

    private lateinit var sharedPreferences: SharedPreferences
    private val imageSizes = mutableListOf<ImageSize>()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_setting)
        sharedPreferences = getSharedPreferences(BuildConfig.APPLICATION_ID, Context.MODE_PRIVATE)
        val groupListType = object : TypeToken<List<ImageSize>?>() {}.type
        imageSizes.addAll(
            Gson().fromJson(
                sharedPreferences.getString("images", "[]"),
                groupListType
            )
        )

        if (imageSizes.isEmpty()) {
            getCameraInstance()?.parameters?.supportedPictureSizes?.forEach {
                imageSizes.add(ImageSize(it.width, it.height, false))
            }
        }
        sharedPreferences.edit().putString("images", Gson().toJson(imageSizes)).apply()
        var i = 1
        imageSizes.forEach {
            val button = RadioButton(this)
            button.id = i
            i++
            button.text = "${it.height}x${it.width}"
            button.isChecked = it.selected || i == 2
            radioGroup.addView(button)
        }
        edtTime.setText(sharedPreferences.getLong("time", 3).toString())
        imgBack.setOnClickListener {
            finish()
        }
        imgSave.setOnClickListener {
            imageSizes.forEach {
                it.selected = false
            }
            imageSizes[radioGroup.checkedRadioButtonId - 1].selected = true
            sharedPreferences.edit().putString("images", Gson().toJson(imageSizes)).apply()
            var time = edtTime.text.toString().toLong()
            if (time < 3) {
                time = 3
            }
            sharedPreferences.edit().putLong("time", time).apply()
            finish()
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
}
