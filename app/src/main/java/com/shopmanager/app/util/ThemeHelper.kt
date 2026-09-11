package com.shopmanager.app.util

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import com.shopmanager.app.App

/**
 * 深色模式 / 主题切换工具
 * - 持久化：SharedPreferences("settings", MODE_PRIVATE)，key = "dark_mode"
 * - applyTheme() 在 Application.onCreate（或 MainActivity.onCreate）调用
 */
object ThemeHelper {

    private const val PREFS_NAME = "settings"
    private const val KEY_DARK_MODE = "dark_mode"

    /** 当前是否为深色模式 */
    fun isDarkMode(context: Context): Boolean {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_DARK_MODE, false)
    }

    /** 切换深色模式并立即应用，结果持久化到 SharedPreferences */
    fun setDarkMode(enabled: Boolean) {
        App.instance.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_DARK_MODE, enabled)
            .apply()
        applyTheme()
    }

    /** 根据持久化设置应用主题，在 Application 或 MainActivity 启动时调用 */
    fun applyTheme() {
        val dark = isDarkMode(App.instance)
        AppCompatDelegate.setDefaultNightMode(
            if (dark) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
        )
    }
}
