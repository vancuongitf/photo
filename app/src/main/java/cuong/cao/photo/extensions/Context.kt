package cuong.cao.photo.extensions

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.telephony.TelephonyManager
import java.net.NetworkInterface
import java.util.*

/**
 * Created by at-cuongcao on 27/05/2020.
 * ScreenId:xxx
 */

@SuppressLint("MissingPermission")
internal fun Context.getDeviceId(): String {
    val telephonyManager =
        getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
    if (telephonyManager != null) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return telephonyManager.deviceId
        } else {
            if (telephonyManager.imei != null) {
                return telephonyManager.imei
            }
        }
    }
    return getMacAdd().toUpperCase(Locale.getDefault())
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