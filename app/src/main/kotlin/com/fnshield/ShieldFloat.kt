package com.fnshield

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.Switch
import android.widget.TextView
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedHelpers

/**
 * 内嵌式悬浮球（免悬浮窗权限）：hook Activity.onResume 往 DecorView 加球
 * 点击 → 打开地图（SelectLocateActivity）；长按 → 扩展功能弹窗（无视距离 / 模拟路线）
 */
object ShieldFloat {
    private const val TAG = "FnShieldBall"
    // 隐藏状态：只在内存，app 重启即恢复显示
    @Volatile private var hidden = false
    private val balls = java.util.Collections.synchronizedSet(java.util.HashSet<View>())
    @Volatile private var lastKeyCode = -1
    @Volatile private var lastKeyTime = 0L

    fun install(cl: ClassLoader) {
        try {
            XposedHelpers.findAndHookMethod("android.app.Activity", cl, "onResume", object : XC_MethodHook() {
                override fun afterHookedMethod(p: MethodHookParam?) {
                    val act = p?.thisObject as? Activity ?: return
                    try {
                        if (act.packageName != "me.ele.hbdteam") return
                        addBall(act)
                    } catch (_: Throwable) {}
                }
            })
            FnLogger.line("[Float] 内嵌悬浮球 hook 已挂")
        } catch (t: Throwable) { FnLogger.line("[Float] 挂载失败: " + t.message) }

        // 音量键双击：隐藏/显示悬浮球（防误触：同键 + 200~600ms 间隔）
        try {
            XposedHelpers.findAndHookMethod("android.app.Activity", cl, "dispatchKeyEvent", android.view.KeyEvent::class.java, object : XC_MethodHook() {
                override fun beforeHookedMethod(p: MethodHookParam?) {
                    val e = p?.args?.get(0) as? android.view.KeyEvent ?: return
                    if (e.action != android.view.KeyEvent.ACTION_DOWN) return
                    val code = e.keyCode
                    if (code != android.view.KeyEvent.KEYCODE_VOLUME_UP && code != android.view.KeyEvent.KEYCODE_VOLUME_DOWN) return
                    val now = android.os.SystemClock.uptimeMillis()
                    val gap = now - lastKeyTime
                    if (code == lastKeyCode && gap in 200..600) {
                        // 双击成功：切换显隐，并吞掉这次按键（避免再调音量）
                        toggle()
                        p.result = true
                        lastKeyCode = -1
                        lastKeyTime = 0L
                        FnLogger.line("[Float] 音量双击 → " + (if (hidden) "隐藏" else "显示"))
                    } else {
                        lastKeyCode = code
                        lastKeyTime = now
                    }
                }
            })
            FnLogger.line("[Float] 音量双击隐藏已挂（200-600ms 同键）")
        } catch (t: Throwable) { FnLogger.line("[Float] 音量键挂载失败: " + t.message) }
    }

    /** 切换显隐（所有已挂的球） */
    private fun toggle() {
        hidden = !hidden
        val snapshot = balls.toList()
        for (v in snapshot) {
            try {
                v.post { v.visibility = if (hidden) View.GONE else View.VISIBLE }
            } catch (_: Throwable) {}
        }
    }

    private fun dp(c: Context, v: Int): Int = (v * c.resources.displayMetrics.density).toInt()

    private fun addBall(act: Activity) {
        val decor = act.window?.decorView as? ViewGroup ?: return
        if (decor.findViewWithTag<View>(TAG) != null) return
        val size = dp(act, 52)
        val ball = TextView(act).apply {
            tag = TAG
            text = "盾"
            textSize = 16f
            setTextColor(Color.WHITE)
            setTypeface(typeface, Typeface.BOLD)
            gravity = Gravity.CENTER
            background = GradientDrawable().apply { shape = GradientDrawable.OVAL; setColor(Color.parseColor("#F2711C")) }
            setOnClickListener { ShieldMap.open(act) }
            setOnLongClickListener { showMenu(act); true }
        }
        val lp = FrameLayout.LayoutParams(size, size).apply {
            gravity = Gravity.TOP or Gravity.LEFT
            leftMargin = dp(act, 8)
            topMargin = dp(act, 130)
        }
        ball.visibility = if (hidden) View.GONE else View.VISIBLE
        balls.add(ball)
        decor.addView(ball, lp)
        FnLogger.line("[Float] 球已加到 " + act.javaClass.simpleName)
    }

    private fun showMenu(act: Activity) {
        try {
            val root = LinearLayout(act).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(act, 20), dp(act, 16), dp(act, 20), dp(act, 16))
                background = GradientDrawable().apply { cornerRadius = dp(act, 14).toFloat(); setColor(Color.parseColor("#2B2B2B")) }
            }
            root.addView(TextView(act).apply {
                text = "扩展功能"; textSize = 18f; setTextColor(Color.WHITE)
                setTypeface(typeface, Typeface.BOLD); setPadding(0, 0, 0, dp(act, 12))
            })
            root.addView(menuRow(act, "无视距离", PrefsHolder.ignoreDistanceEnabled) { v ->
                PrefsHolder.setIgnore(v)
                FnLogger.line("[Float] 无视距离=" + v)
                if (v) ShieldMap.toast(act, "无视距离已开启：距离校验放行，抽查可能算违规")
            })
            root.addView(menuRow(act, "模拟路线", PrefsHolder.getRouteSim()) { v ->
                PrefsHolder.setRouteSim(v)
                ShieldFake.setRouteSim(v)
                FnLogger.line("[Float] 模拟路线=" + v)
            })
            val dlg = AlertDialog.Builder(act, android.R.style.Theme_DeviceDefault_Dialog_NoActionBar).setView(root).create()
            dlg.window?.setBackgroundDrawableResource(android.R.color.transparent)
            dlg.show()
        } catch (t: Throwable) { FnLogger.line("[Float] 菜单失败: " + t.message) }
    }

    private fun menuRow(act: Activity, label: String, init: Boolean, onChange: (Boolean) -> Unit): LinearLayout {
        val row = LinearLayout(act).apply { orientation = LinearLayout.HORIZONTAL; setPadding(0, dp(act, 10), 0, dp(act, 10)) }
        row.addView(TextView(act).apply {
            text = label; textSize = 16f; setTextColor(Color.WHITE)
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        })
        val sw = Switch(act).apply {
            isChecked = init
            thumbTintList = ColorStateList.valueOf(Color.parseColor("#F2711C"))
            trackTintList = ColorStateList(arrayOf(intArrayOf(android.R.attr.state_checked), intArrayOf()), intArrayOf(Color.parseColor("#F2711C"), Color.parseColor("#555555")))
        }
        sw.setOnCheckedChangeListener { _, v -> onChange(v) }
        row.addView(sw)
        return row
    }
}
