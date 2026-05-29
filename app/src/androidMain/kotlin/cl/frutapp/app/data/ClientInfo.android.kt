package cl.frutapp.app.data

import android.os.Build
import cl.frutapp.app.BuildConfig
import java.util.Locale

actual object ClientInfo {
    actual val channel: String = "APP_ANDROID"
    actual val appVersion: String = BuildConfig.VERSION_NAME
    actual val deviceModel: String = "${Build.MANUFACTURER} ${Build.MODEL}"
    actual val osVersion: String = "Android ${Build.VERSION.RELEASE} (SDK ${Build.VERSION.SDK_INT})"
    actual val locale: String = Locale.getDefault().toLanguageTag()
}
