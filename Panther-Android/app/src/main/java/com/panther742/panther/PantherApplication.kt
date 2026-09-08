package com.panther742.panther

import android.app.Application
import android.os.Build
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Writes any uncaught crash to a small file so that on the NEXT launch
 * Panther can show the error text (helps debugging on the user's phone).
 */
class PantherApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        val prev = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                val sw = StringWriter()
                throwable.printStackTrace(PrintWriter(sw))
                val sb = StringBuilder()
                sb.append("Time: ")
                    .append(SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date()))
                    .append('\n')
                sb.append("Thread: ").append(thread.name).append('\n')
                sb.append("Android: ").append(Build.VERSION.RELEASE)
                    .append(" (API ").append(Build.VERSION.SDK_INT).append(")\n")
                sb.append("Device: ").append(Build.MANUFACTURER).append(' ').append(Build.MODEL).append('\n')
                sb.append(sw)
                val out = File(filesDir, "panther_crash.txt")
                out.writeText(sb.toString())
            } catch (_: Throwable) {
                // Never let the crash-logger itself crash.
            }
            prev?.uncaughtException(thread, throwable)
        }
    }

    /** Read + delete a previously recorded crash (called on next launch). */
    fun readCrashReport(): String? {
        val f = File(filesDir, "panther_crash.txt")
        if (!f.exists()) return null
        val text = runCatching { f.readText() }.getOrNull() ?: return null
        runCatching { f.delete() }
        return text
    }
}
