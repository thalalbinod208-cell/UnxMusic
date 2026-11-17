package com.maxrave.simpmusic

import android.content.Context
import android.os.Build
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter

/** Stores a small, credential-free crash snapshot for import on the next process start. */
internal class UnxCrashHandler(context: Context) : Thread.UncaughtExceptionHandler {
    private val previous = Thread.getDefaultUncaughtExceptionHandler()
    private val crashFile = File(context.filesDir, "pending_unx_crash.txt")

    override fun uncaughtException(thread: Thread, throwable: Throwable) {
        runCatching {
            val trace = StringWriter().also { throwable.printStackTrace(PrintWriter(it)) }.toString().take(48_000)
            crashFile.writeText(
                "thread=${thread.name}\nandroid=${Build.VERSION.SDK_INT}\ndevice=${Build.MANUFACTURER} ${Build.MODEL}\n" +
                    "app=${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})\n${throwable.javaClass.name}\n$trace",
            )
        }
        previous?.uncaughtException(thread, throwable)
    }
}
