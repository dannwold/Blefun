package com.example.blecontroller

import android.app.Application
import android.content.Intent
import kotlin.system.exitProcess

class BleApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, exception ->
            val stackTrace = exception.stackTraceToString()

            val intent = Intent(applicationContext, CrashReporterActivity::class.java).apply {
                putExtra(CrashReporterActivity.EXTRA_CRASH_INFO, stackTrace)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            startActivity(intent)

            defaultHandler?.uncaughtException(thread, exception)
            exitProcess(1)
        }
    }
}
