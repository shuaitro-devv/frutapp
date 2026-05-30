package cl.frutapp.app.ui

import android.util.Log

private const val TAG = "FrutApp-Error"

internal actual fun log(message: String, throwable: Throwable?) {
    // Tag "FrutApp-Error" filtrable en logcat. Cuando metamos Sentry, el SDK
    // captura Log.e automáticamente — o llamamos a Sentry.captureException acá.
    Log.e(TAG, message, throwable)
}
