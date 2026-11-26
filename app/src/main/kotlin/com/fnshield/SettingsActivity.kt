package com.fnshield

import android.app.Activity
import android.app.AlertDialog
import android.graphics.Color
import android.os.Bundle
import android.util.TypedValue
import android.widget.LinearLayout
import android.widget.Switch
import android.widget.TextView

/** FnShield 模块设置页（LSP 版） */
class SettingsActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        PrefsHolder.init(this)
        CrashLogger.installCrashHandler()

        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(28), dp(44), dp(28), dp(28)) }
        root.addView(tv("FnShield", 24f, Color.BLACK, true))
        root.addView(tv("蜂鸟骑士 违规抑制 · 定位可信 · 无视距离", 13f, Color.GRAY, false))
        root.addView(tv("", 12f, Color.BLACK, false))
        root.addView(tv("已内置", 15f, Color.DKGRAY, true))
        root.addView(tv("· 超速 / 逆行 / 闯红灯 / 分心 / 未戴头盔 / 路口超速 屏蔽", 15f, Color.DKGRAY, false))
        root.addView(tv("· 违规语音播报静音", 15f, Color.DKGRAY, false))
        root.addView(tv("· GPS 可信（假定位不被识破）", 15f, Color.DKGRAY, false))
        root.addView(tv("· 遵守交规（默认开启，无开关）", 15f, Color.DKGRAY, false))
        root.addView(tv("", 14f, Color.BLACK, false))

        // 无视距离开关（默认关，开启先弹风险提示）
        val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        row.addView(tv("无视距离", 18f, Color.BLACK, true).apply { layoutParams = LinearLayout.LayoutParams(0, -2, 1f) })
        val sw = Switch(this)
        sw.isChecked = PrefsHolder.ignoreDistanceEnabled
        sw.setOnCheckedChangeListener { _, checked ->
            if (checked) {
                AlertDialog.Builder(this)
                    .setTitle("无视距离 · 风险提示")
                    .setMessage("开启后：终点/停车/拉回商圈等距离校验将被放行，可能被平台判定为异常操作（抽查到算违规）。\n\n确认开启吗？")
                    .setPositiveButton("确认开启") { _, _ ->
                        PrefsHolder.setIgnore(true)
                        FnLogger.line("[UI] 无视距离 已开启（用户确认）")
                    }
                    .setNegativeButton("取消") { _, _ -> sw.isChecked = false }
                    .setOnCancelListener { sw.isChecked = false }
                    .show()
            } else {
                PrefsHolder.setIgnore(false)
                FnLogger.line("[UI] 无视距离 已关闭")
            }
        }
        row.addView(sw)
        root.addView(row)

        root.addView(tv("", 16f, Color.BLACK, false))
        root.addView(tv("日志目录：/sdcard/Android/data/me.ele.hbdteam/files/fnshield_logs/", 12f, Color.GRAY, false))
        root.addView(tv("全量日志：hook命中 / 签名 / 测点 / 开关 全程落盘", 12f, Color.GRAY, false))
        setContentView(root)
    }

    private fun tv(s: String, size: Float, color: Int, bold: Boolean): TextView = TextView(this).apply {
        text = s; textSize = size; setTextColor(color)
        if (bold) setTypeface(typeface, android.graphics.Typeface.BOLD)
    }
    private fun dp(v: Int): Int = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v.toFloat(), resources.displayMetrics).toInt()
}
