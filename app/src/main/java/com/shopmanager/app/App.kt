package com.shopmanager.app

import android.app.Application
import android.content.Context
import com.shopmanager.app.data.AppDatabase
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class App : Application() {

    @Volatile
    private var _database: AppDatabase? = null

    val database: AppDatabase
        get() = _database ?: synchronized(this) {
            _database ?: AppDatabase.getInstance(applicationContext).also { _database = it }
        }

    override fun onCreate() {
        super.onCreate()
        instance = this
        // 最先设置崩溃处理器，确保任何后续崩溃都能被捕获
        installCrashHandler()
        // 数据库懒加载，不在 onCreate 中初始化，避免阻塞或崩溃
    }

    private fun installCrashHandler() {
        val prev = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                val sw = StringWriter()
                throwable.printStackTrace(PrintWriter(sw))
                val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())
                val log = buildString {
                    appendLine("===== CRASH LOG =====")
                    appendLine("Time: $timestamp")
                    appendLine("Thread: ${thread.name}")
                    appendLine("Exception: ${throwable.javaClass.name}")
                    appendLine("Message: ${throwable.message}")
                    appendLine("StackTrace:")
                    appendLine(sw.toString())
                    appendLine("=====================")
                }
                // 直接用绝对路径写入，不依赖任何 Android API
                val logFile = File(filesDir, "crash_log.txt")
                logFile.appendText("\n$log\n")
            } catch (_: Throwable) {
                // 忽略写入失败
            }
            prev?.uncaughtException(thread, throwable)
        }
    }

    companion object {
        lateinit var instance: App
            private set

        fun readCrashLog(context: Context): String? {
            return try {
                val logFile = File(context.filesDir, "crash_log.txt")
                if (logFile.exists()) logFile.readText() else null
            } catch (_: Exception) {
                null
            }
        }

        fun clearCrashLog(context: Context) {
            try {
                File(context.filesDir, "crash_log.txt").delete()
            } catch (_: Exception) {
            }
        }
    }
}
