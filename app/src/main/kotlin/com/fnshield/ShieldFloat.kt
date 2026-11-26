package com.fnshield

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Build
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Switch
import android.widget.TextView

/**
 * 整合版悬浮球：假定位 / 路线模拟 / 无视距离 开关 + 假坐标设置
 * （LSP 版不用；整合版 NPatch 嵌入时启用，manifest 需 SYSTEM_ALERT_WINDOW 权限）
 */
object ShieldFloat {
    private var wm: WindowManager? = null
    private var ball: View? = null
    private var menu: LinearLayout? = null
    private var shown = false
    private var ctx: Context? = null

    fun toggle(c: Context) { if (shown) hide() else show(c) }

    @SuppressLint("ClickableViewAccessibility", "RtlHardcoded")
    fun show(c: Context) {
        if (shown) return
        ctx = c.applicationContext
        val w = ctx!!.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        wm = w

        // 悬浮球
        val tv = TextView(ctx).apply {
            text = "盾"; textSize = 16f; setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            setBackgroundColor(Color.argb(220, 22, 122, 220))
            setTypeface(null, android.graphics.Typeface.BOLD)
        }
        val bp = WindowManager.LayoutParams(
            dp(48), dp(48),
            if (Build.VERSION.SDK_INT >= 26) WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY else WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE, PixelFormat.TRANSLUCENT
        ).apply { gravity = Gravity.TOP or Gravity.LEFT; x = 0; y = dp(200) }

        // 拖动
        var downX = 0; var downY = 0; var startX = 0; var startY = 0; var moved = false
        tv.setOnTouchListener { _, ev ->
            when (ev.action) {
                MotionEvent.ACTION_DOWN -> { downX = ev.rawX.toInt(); downY = ev.rawY.toInt(); startX = bp.x; startY = bp.y; moved = false; true }
                MotionEvent.ACTION_MOVE -> {
                    val dx = ev.rawX.toInt() - downX; val dy = ev.rawY.toInt() - downY
                    if (Math.abs(dx) > 8 || Math.abs(dy) > 8) moved = true
                    if (moved) { bp.x = startX + dx; bp.y = startY + dy; wm?.updateViewLayout(tv, bp) }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    if (!moved) toggleMenu()
                    true
                }
                else -> false
            }
        }
        w.addView(tv, bp); ball = tv; shown = true
        FnLogger.line("[Float] 悬浮球已显示")
    }

    fun hide() {
        try { ball?.let { wm?.removeView(it) }; menu?.let { wm?.removeView(it) } } catch (_: Throwable) {}
        ball = null; menu = null; shown = false
        FnLogger.line("[Float] 悬浮球隐藏")
    }

    @SuppressLint("RtlHardcoded")
    private fun toggleMenu() {
        val c = ctx ?: return
        if (menu != null) { try { wm?.removeView(menu) } catch (_: Throwable) {} ; menu = null; return }
        val lp = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= 26) WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY else WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE, PixelFormat.TRANSLUCENT
        ).apply { gravity = Gravity.TOP or Gravity.LEFT; x = dp(60); y = dp(200) }

        val root = LinearLayout(c).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.WHITE)
            setPadding(dp(16), dp(16), dp(16), dp(16))
        }
        root.addView(row(c, "假定位", PrefsHolder.getFakeEnabled()) { v -> PrefsHolder.setFakeEnabled(v); FnLogger.line("[Float] 假定位=" + v) })
        root.addView(row(c, "路线模拟", PrefsHolder.getRouteSim()) { v -> PrefsHolder.setRouteSim(v); FnLogger.line("[Float] 路线模拟=" + v); if (v) startSim() else stopSim() })
        root.addView(row(c, "无视距离", PrefsHolder.ignoreDistanceEnabled) { v ->
            if (v) {
                AlertDialog.Builder(c).setTitle("无视距离风险提示").setMessage("距离校验将被放行，可能被判异常操作（抽查算违规）。确认开启？")
                    .setPositiveButton("确认") { _, _ -> PrefsHolder.setIgnore(true) }
                    .setNegativeButton("取消") { _, _ -> menuItemRefresh() }
                    .setOnCancelListener { menuItemRefresh() }.show()
            } else PrefsHolder.setIgnore(false)
            FnLogger.line("[Float] 无视距离=" + v)
        })
        root.addView(btn(c, "设置假坐标") { askCoord(c) })
        wm?.addView(root, lp); menu = root
    }

    private fun menuItemRefresh() { /* 简化：关掉重开菜单 */ }

    private fun askCoord(c: Context) {
        val l = LinearLayout(c).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(20), dp(10), dp(20), 0) }
        val e1 = EditText(c).apply { hint = "纬度 lat（如 30.123456）" }
        val e2 = EditText(c).apply { hint = "经度 lng（如 120.123456）" }
        l.addView(e1); l.addView(e2)
        AlertDialog.Builder(c).setTitle("设置假坐标").setView(l)
            .setPositiveButton("确定") { _, _ ->
                val la = e1.text.toString().toDoubleOrNull(); val ln = e2.text.toString().toDoubleOrNull()
                if (la != null && ln != null) {
                    PrefsHolder.setFakeLat(la); PrefsHolder.setFakeLng(ln)
                    FnLogger.line("[Float] 假坐标=" + la + "," + ln)
                }
            }
            .setNegativeButton("取消", null).show()
    }

    private fun startSim() {
        // 默认演示路线：绕一个方块
        val base = PrefsHolder.getFakeLat(); val bl = PrefsHolder.getFakeLng()
        val pts = listOf(
            doubleArrayOf(base, bl), doubleArrayOf(base + 0.003, bl), doubleArrayOf(base + 0.003, bl + 0.003), doubleArrayOf(base, bl + 0.003)
        )
        ShieldFake.startRouteSim(pts)
    }
    private fun stopSim() { ShieldFake.stopRouteSim() }

    private fun row(c: Context, label: String, init: Boolean, onChanged: (Boolean) -> Unit): LinearLayout {
        val r = LinearLayout(c).apply { orientation = LinearLayout.HORIZONTAL }
        r.addView(TextView(c).apply { text = label; textSize = 15f; setTextColor(Color.BLACK) }.also { it.layoutParams = LinearLayout.LayoutParams(0, -2, 1f) })
        val sw = Switch(c).apply { isChecked = init }
        sw.setOnCheckedChangeListener { _, v -> onChanged(v) }
        r.addView(sw)
        return r
    }
    private fun btn(c: Context, label: String, onClick: () -> Unit): TextView = TextView(c).apply {
        text = label; textSize = 15f; setTextColor(Color.WHITE)
        setBackgroundColor(Color.argb(255, 22, 122, 220)); gravity = Gravity.CENTER
        setPadding(dp(12), dp(10), dp(12), dp(10))
        setOnClickListener { onClick() }
    }
    private fun dp(v: Int): Int = (v * ctx!!.resources.displayMetrics.density).toInt()
}
