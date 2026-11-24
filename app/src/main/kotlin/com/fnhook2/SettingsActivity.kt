package com.fnhook2

import android.app.Activity
import android.graphics.Color
import android.os.Bundle
import android.util.TypedValue
import android.widget.LinearLayout
import android.widget.TextView

/** 模块页：软件简介 */
class SettingsActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        PrefsHolder.init(this)
        CrashLogger.installCrashHandler()

        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(28), dp(44), dp(28), dp(28)) }
        root.addView(tv("蜂鸟盾", 24f, Color.BLACK, true))
        root.addView(tv("", 12f, Color.BLACK, false))
        root.addView(tv("· 超速 / 路口超速 / 闯红灯 / 分心驾驶 / 头盔 屏蔽", 16f, Color.DKGRAY, false))
        root.addView(tv("· GPS 可信（假定位不被识破，兼容 LocationMax）", 16f, Color.DKGRAY, false))
        root.addView(tv("· 无视距离（默认开启）", 16f, Color.DKGRAY, false))
        root.addView(tv("· 采样截断 / 违规模型 / 更新屏蔽", 16f, Color.DKGRAY, false))
        root.addView(tv("", 16f, Color.BLACK, false))
        root.addView(tv("日志目录：/sdcard/Android/data/me.ele.hbdteam/files/fnhook2_logs/", 12f, Color.GRAY, false))
        root.addView(tv("崩溃/闪退/杀后台重启 全部自动记录", 12f, Color.GRAY, false))
        setContentView(root)
    }

    private fun tv(s: String, size: Float, color: Int, bold: Boolean): TextView = TextView(this).apply {
        text = s; textSize = size; setTextColor(color)
        if (bold) setTypeface(typeface, android.graphics.Typeface.BOLD)
    }

    private fun dp(v: Int): Int = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v.toFloat(), resources.displayMetrics).toInt()
}
