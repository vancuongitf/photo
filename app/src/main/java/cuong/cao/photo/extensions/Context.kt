package cuong.cao.photo.extensions

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.os.Environment
import android.telephony.TelephonyManager
import java.io.File
import java.net.NetworkInterface
import java.util.*

@SuppressLint("MissingPermission")
internal fun Context.getDeviceId(): String {
    var id = ""
    val telephonyManager =
        getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
    if (telephonyManager != null) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            id = telephonyManager.deviceId
        } else {
            if (telephonyManager.imei != null) {
                id = telephonyManager.imei
            }
        }
    }
    if (id.isEmpty()) {
        id = getMacAdd().toUpperCase(Locale.getDefault())
    }
    if (id.length > 5) {
        id = id.substring(id.length - 5)
    }
    return id
}

private fun getMacAdd(): String {
    try {
        val all = Collections.list(NetworkInterface.getNetworkInterfaces())
        for (nif in all) {
            if (nif.name.toLowerCase(Locale.getDefault()) != "wlan0") continue

            val macBytes = nif.hardwareAddress ?: return ""

            val res1 = StringBuilder()
            for (b in macBytes) {
                var hexStr = Integer.toHexString((b.toInt() and 0xFF))
                if (hexStr.length < 2) {
                    hexStr = "0$hexStr"
                }
                res1.append("$hexStr:")
            }

            if (res1.isNotEmpty()) {
                res1.deleteCharAt(res1.length - 1)
            }
            return res1.toString()
        }
    } catch (ex: Exception) {
        //handle exception
    }
    return ""
}

internal fun createFileToSave(): File {
    Environment.getExternalStorageDirectory().parentFile?.parentFile?.listFiles()?.find {
        it.name != "self" && it.name != "emulated"
    }?.let {
        val parent = File(it, Environment.DIRECTORY_DCIM)
        if (!parent.exists()) {
            parent.mkdir()
        }
        return File(parent, "Photo_${Calendar.getInstance().timeInMillis}.jpeg")
    }
    return File(
        Environment.getExternalStoragePublicDirectory(
            Environment.DIRECTORY_DCIM
        ), "Photo_${Calendar.getInstance().timeInMillis}.jpeg"
    )
}