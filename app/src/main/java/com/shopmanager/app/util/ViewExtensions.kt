package com.shopmanager.app.util

import android.view.MotionEvent
import android.view.View
import android.content.res.Resources

/**
 * View 扩展函数
 * - 点击缩放反馈
 * - 可见性切换
 * - dp 转换
 */

/**
 * 按钮 / 卡片点击缩放反馈：按下时缩放到 0.97，松开或取消时还原。
 * 通过 OnTouchListener 实现，不消费事件，因此不影响原有 OnClickListener。
 */
fun View.addClickScaleAnimation() {
    isClickable = true
    setOnTouchListener { v, event ->
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                v.animate()
                    .scaleX(0.97f)
                    .scaleY(0.97f)
                    .setDuration(80)
                    .withEndAction {
                        v.animate().scaleX(1f).scaleY(1f).setDuration(80).start()
                    }
                    .start()
            }
            MotionEvent.ACTION_UP,
            MotionEvent.ACTION_CANCEL -> {
                v.animate().scaleX(1f).scaleY(1f).setDuration(80).start()
            }
        }
        false // 不消费事件，保证 click 正常触发
    }
}

/** 设置为可见 */
fun View.visible() {
    visibility = View.VISIBLE
}

/** 设置为隐藏（不占位） */
fun View.gone() {
    visibility = View.GONE
}

/** 设置为不可见（保留占位） */
fun View.invisible() {
    visibility = View.INVISIBLE
}

/** dp 转 px（基于系统屏幕密度） */
val Int.dp: Int
    get() = (this * Resources.getSystem().displayMetrics.density).toInt()
