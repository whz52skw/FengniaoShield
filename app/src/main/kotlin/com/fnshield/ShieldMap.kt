package com.fnshield

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedHelpers

/**
 * 地图界面（劫持 app 原生 SelectLocateActivity 叠加 FnShield UI）
 * 顶部标题 / 底部 开启·收藏·收藏夹·搜索·设置 / 版本信息
 */
object ShieldMap {
    private const val ACT = "me.ele.im.location.SelectLocateActivity"
    private const val UI_TAG = "FnShieldMapUI"

    fun install(cl: ClassLoader) {
        try {
            XposedHelpers.findAndHookMethod(ACT, cl, "onResume", object : XC_MethodHook() {
                override fun afterHookedMethod(p: MethodHookParam?) {
                    val act = p?.thisObject as? Activity ?: return
                    try { overlay(act) } catch (t: Throwable) { FnLogger.line("[Map] 叠加失败: " + t.message) }
                }
            })
            FnLogger.line("[Map] SelectLocateActivity 叠加 hook 已挂")
        } catch (t: Throwable) { FnLogger.line("[Map] 挂载失败: " + t.message) }
    }

    fun open(act: Activity) {
        try {
            act.startActivity(Intent().setClassName(act, ACT))
            FnLogger.line("[Map] 打开地图")
        } catch (t: Throwable) {
            toast(act, "打开地图失败: " + t.message); FnLogger.line("[Map] 打开失败: " + t.message)
        }
    }

    private fun overlay(act: Activity) {
        val decor = act.window?.decorView as? ViewGroup ?: return
        if (decor.findViewWithTag<View>(UI_TAG) != null) return
        val d = act.resources.displayMetrics.density
        fun dp(v: Int) = (v * d).toInt()

        val root = FrameLayout(act).apply { tag = UI_TAG; isClickable = false }

        // 顶部标题
        val top = TextView(act).apply {
            text = "FnShield，点击这里可跳转下一单"
            textSize = 16f; setTextColor(Color.parseColor("#222222")); setTypeface(typeface, Typeface.BOLD)
            setBackgroundColor(Color.parseColor("#F5F5F5")); gravity = Gravity.CENTER
            setPadding(dp(10), dp(10), dp(10), dp(10))
            setOnClickListener { toast(act, "FnShield") }
        }
        root.addView(top, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(48), Gravity.TOP))

        // 底部按钮栏
        val bottom = LinearLayout(act).apply {
            orientation = LinearLayout.HORIZONTAL; setBackgroundColor(Color.WHITE)
            setPadding(dp(4), dp(6), dp(4), dp(6))
        }
        for (label in listOf("开启", "收藏", "收藏夹", "搜索", "设置")) {
            val color = if (label == "开启") "#2FA36B" else if (label == "收藏") "#C0392B" else if (label == "收藏夹") "#E67E22" else if (label == "搜索") "#8E44AD" else "#16A085"
            bottom.addView(TextView(act).apply {
                text = label; textSize = 15f; setTextColor(Color.WHITE); gravity = Gravity.CENTER
                background = GradientDrawable().apply { cornerRadius = dp(8).toFloat(); setColor(Color.parseColor(color)) }
                layoutParams = LinearLayout.LayoutParams(0, dp(44), 1f).apply { setMargins(dp(3), 0, dp(3), 0) }
                setOnClickListener { onClickBtn(act, label) }
            })
        }
        root.addView(bottom, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.BOTTOM))

        // 版本信息
        val ver = TextView(act).apply {
            text = "Version: " + BuildConfig.VERSION_NAME + "    状态：正常"
            textSize = 13f; setTextColor(Color.parseColor("#333333")); setTypeface(typeface, Typeface.BOLD)
            gravity = Gravity.CENTER; setBackgroundColor(Color.parseColor("#E8F5E9"))
            setPadding(dp(8), dp(4), dp(8), dp(4))
        }
        root.addView(ver, FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL).apply { bottomMargin = dp(58) })

        decor.addView(root, ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
        FnLogger.line("[Map] 地图 UI 叠加完成")
    }

    private fun onClickBtn(act: Activity, label: String) {
        when (label) {
            "开启" -> {
                val lat = PrefsHolder.getFakeLat(); val lng = PrefsHolder.getFakeLng()
                if (lat == 0.0 && lng == 0.0) { toast(act, "请先在地图上选点"); return }
                PrefsHolder.setFakeEnabled(true)
                ShieldFake.applyFake(lat, lng)
                toast(act, "已开启虚拟定位")
                FnLogger.line("[Map] 开启虚拟定位 " + lat + "," + lng)
            }
            "收藏" -> { PrefsHolder.setFakeEnabled(true); toast(act, "已收藏当前点"); }
            "收藏夹" -> toast(act, "收藏夹")
            "搜索" -> toast(act, "搜索")
            "设置" -> toast(act, "设置")
        }
    }

    fun toast(c: Context, msg: String) {
        try { Handler(Looper.getMainLooper()).post { Toast.makeText(c, msg, Toast.LENGTH_SHORT).show() } } catch (_: Throwable) {}
    }
}
