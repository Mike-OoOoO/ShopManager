package com.shopmanager.app.util

import java.util.Locale

/**
 * 数字 / 金额格式化工具
 * - 价格：¥ + 两位小数
 * - 百分比：一位小数 + %
 * - 价格输入：限制为最多两位小数
 */
object NumberUtils {

    /** 价格格式化："¥12.50" */
    fun formatPrice(value: Double): String =
        "¥" + String.format(Locale.getDefault(), "%.2f", value)

    /** 利润额格式化：与价格一致，"¥12.50" */
    fun formatProfit(value: Double): String = formatPrice(value)

    /** 百分比格式化："50.5%" */
    fun formatPercent(value: Double): String =
        String.format(Locale.getDefault(), "%.1f%%", value)

    /** 数量格式化：直接转字符串，"123" */
    fun formatCount(value: Int): String = value.toString()

    /**
     * 价格输入格式化：
     * - 只保留数字与小数点
     * - 只允许一个小数点
     * - 小数点后最多两位小数
     */
    fun formatPriceInput(text: String): String {
        if (text.isEmpty()) return ""
        // 1) 过滤掉数字和小数点以外的字符
        val filtered = StringBuilder()
        var dotCount = 0
        for (c in text) {
            when {
                c.isDigit() -> filtered.append(c)
                c == '.' && dotCount == 0 -> {
                    dotCount++
                    filtered.append(c)
                }
            }
        }
        var result = filtered.toString()
        // 2) 若出现多个小数点（理论上已被过滤），只保留第一个
        val firstDot = result.indexOf('.')
        if (firstDot >= 0 && result.indexOf('.', firstDot + 1) >= 0) {
            val head = result.substring(0, firstDot + 1)
            val tail = result.substring(firstDot + 1).replace(".", "")
            result = head + tail
        }
        // 3) 小数点后最多两位小数
        val dotIndex = result.indexOf('.')
        if (dotIndex >= 0 && result.length - dotIndex - 1 > 2) {
            result = result.substring(0, dotIndex + 3)
        }
        return result
    }
}
