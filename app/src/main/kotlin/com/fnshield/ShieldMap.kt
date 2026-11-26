package com.fnshield

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Handler
import android.os.Looper
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedHelpers
import org.json.JSONArray
import org.json.JSONObject

/**
 * 地图（借 app 自带 SelectLocateActivity 叠加 FnShield UI）
 * 功能：开启 / 收藏 / 收藏夹 / 搜索 / 设置（速度滑块）
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
        } catch (t: Throwable) { toast(act, "打开地图失败: " + t.message); FnLogger.line("[Map] 打开失败: " + t.message) }
    }

    private fun dp(c: Context, v: Int): Int = (v * c.resources.displayMetrics.density).toInt()

    private fun overlay(act: Activity) {
        val decor = act.window?.decorView as? ViewGroup ?: return
        if (decor.findViewWithTag<View>(UI_TAG) != null) return
        val root = FrameLayout(act).apply { tag = UI_TAG; isClickable = false }

        val top = TextView(act).apply {
            text = "FnShield，点击这里可跳转下一单"
            textSize = 16f; setTextColor(Color.parseColor("#222222")); setTypeface(typeface, Typeface.BOLD)
            setBackgroundColor(Color.parseColor("#F5F5F5")); gravity = Gravity.CENTER
            setPadding(dp(act,10), dp(act,10), dp(act,10), dp(act,10))
        }
        root.addView(top, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(act,48), Gravity.TOP))

        val bottom = LinearLayout(act).apply {
            orientation = LinearLayout.HORIZONTAL; setBackgroundColor(Color.WHITE)
            setPadding(dp(act,4), dp(act,6), dp(act,4), dp(act,6))
        }
        for (label in listOf("开启", "收藏", "收藏夹", "搜索", "设置")) {
            val color = when (label) {
                "开启" -> "#2FA36B"; "收藏" -> "#C0392B"; "收藏夹" -> "#E67E22"; "搜索" -> "#8E44AD"; else -> "#16A085"
            }
            bottom.addView(TextView(act).apply {
                text = label; textSize = 15f; setTextColor(Color.WHITE); gravity = Gravity.CENTER
                background = GradientDrawable().apply { cornerRadius = dp(act,8).toFloat(); setColor(Color.parseColor(color)) }
                layoutParams = LinearLayout.LayoutParams(0, dp(act,44), 1f).apply { setMargins(dp(act,3), 0, dp(act,3), 0) }
                setOnClickListener { onClickBtn(act, label) }
            })
        }
        root.addView(bottom, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.BOTTOM))

        val ver = TextView(act).apply {
            text = "Version: " + BuildConfig.VERSION_NAME + "    速度: " + PrefsHolder.simSpeed + " km/h"
            textSize = 13f; setTextColor(Color.parseColor("#333333")); setTypeface(typeface, Typeface.BOLD)
            gravity = Gravity.CENTER; setBackgroundColor(Color.parseColor("#E8F5E9"))
            setPadding(dp(act,8), dp(act,4), dp(act,8), dp(act,4))
        }
        root.addView(ver, FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL).apply { bottomMargin = dp(act,58) })

        decor.addView(root, ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
        FnLogger.line("[Map] 地图 UI 叠加完成")
    }

    private fun onClickBtn(act: Activity, label: String) {
        when (label) {
            "开启" -> doOpen(act)
            "收藏" -> askFavorite(act)
            "收藏夹" -> showFavorites(act)
            "搜索" -> askSearch(act)
            "设置" -> askSpeed(act)
        }
    }

    // ============ 速度滑块 ============
    private fun askSpeed(act: Activity) {
        try {
            val box = LinearLayout(act).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(act,16), dp(act,12), dp(act,16), dp(act,12)) }
            val label = TextView(act).apply { text = "模拟速度：" + PrefsHolder.simSpeed + " km/h"; textSize = 16f; setTextColor(Color.BLACK) }
            val bar = SeekBar(act).apply {
                max = 149; progress = PrefsHolder.simSpeed - 1
                setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                    override fun onProgressChanged(sb: SeekBar?, progress: Int, fromUser: Boolean) { label.text = "模拟速度：" + (progress + 1) + " km/h" }
                    override fun onStartTrackingTouch(sb: SeekBar?) {}
                    override fun onStopTrackingTouch(sb: SeekBar?) {}
                })
            }
            box.addView(TextView(act).apply { text = "模拟路线速度设置"; textSize = 18f; setTypeface(typeface, Typeface.BOLD); setTextColor(Color.BLACK) })
            box.addView(TextView(act).apply { text = "默认 100 km/h，请勿设置过大速度值"; textSize = 12f; setTextColor(Color.GRAY) })
            box.addView(label); box.addView(bar)
            AlertDialog.Builder(act).setView(box)
                .setNegativeButton("取消", null)
                .setPositiveButton("确定") { _, _ ->
                    val v = bar.progress + 1
                    PrefsHolder.setSimSpeed(v)
                    ShieldFake.setSimSpeed(v)
                    toast(act, "速度已设置为 " + v + " km/h")
                    FnLogger.line("[Map] 速度=" + v)
                }.show()
        } catch (t: Throwable) { toast(act, "设置失败: " + t.message) }
    }

    // ============ 收藏 ============
    private fun askFavorite(act: Activity) {
        try {
            val et = EditText(act).apply { hint = "请输入备注（可选）"; inputType = InputType.TYPE_CLASS_TEXT }
            AlertDialog.Builder(act)
                .setTitle("收藏位置")
                .setMessage("是否收藏当前位置？")
                .setView(et)
                .setNegativeButton("取消", null)
                .setPositiveButton("确定收藏") { _, _ ->
                    val lat = PrefsHolder.getFakeLat(); val lng = PrefsHolder.getFakeLng()
                    if (lat == 0.0 && lng == 0.0) { toast(act, "请先在地图上选点"); return@setPositiveButton }
                    addFavorite(et.text.toString().ifEmpty { "未命名" }, lat, lng)
                    toast(act, "收藏成功！")
                }.show()
        } catch (t: Throwable) { toast(act, "收藏失败: " + t.message) }
    }

    private fun addFavorite(note: String, lat: Double, lng: Double) {
        val arr = JSONArray(PrefsHolder.getFavorites())
        arr.put(JSONObject().put("note", note).put("lat", lat).put("lng", lng))
        PrefsHolder.setFavorites(arr.toString())
        FnLogger.line("[Map] 收藏 " + note + " " + lat + "," + lng)
    }

    private fun showFavorites(act: Activity) {
        try {
            val arr = JSONArray(PrefsHolder.getFavorites())
            if (arr.length() == 0) { toast(act, "收藏夹是空的"); return }
            val box = LinearLayout(act).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(act,12), dp(act,8), dp(act,12), dp(act,8)) }
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                val note = o.optString("note", "未命名")
                val la = o.optDouble("lat", 0.0); val ln = o.optDouble("lng", 0.0)
                val row = LinearLayout(act).apply { orientation = LinearLayout.HORIZONTAL; setPadding(0, dp(act,8), 0, dp(act,8)) }
                row.addView(TextView(act).apply {
                    text = note + "\n" + String.format("%.6f,%.6f", la, ln)
                    textSize = 14f; setTextColor(Color.BLACK)
                    layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                    setOnClickListener { doOpenAt(act, la, ln); }
                })
                row.addView(TextView(act).apply {
                    text = "删除"; textSize = 13f; setTextColor(Color.RED); setPadding(dp(act,10), 0, 0, 0)
                    setOnClickListener { arr.remove(i); PrefsHolder.setFavorites(arr.toString()); toast(act, "已删除"); }
                })
                box.addView(row)
            }
            val sv = ScrollView(act).apply { addView(box) }
            AlertDialog.Builder(act).setTitle("收藏夹 (" + arr.length() + "个)").setView(sv).setPositiveButton("关闭", null).show()
        } catch (t: Throwable) { toast(act, "读取收藏夹失败: " + t.message) }
    }

    // ============ 搜索（反射 app 自带高德 PoiSearch） ============
    private fun askSearch(act: Activity) {
        try {
            val et = EditText(act).apply { hint = "输入地址或地点名称"; inputType = InputType.TYPE_CLASS_TEXT }
            AlertDialog.Builder(act)
                .setTitle("搜索位置")
                .setView(et)
                .setNegativeButton("取消", null)
                .setPositiveButton("搜索") { _, _ ->
                    val kw = et.text.toString().trim()
                    if (kw.isEmpty()) { toast(act, "请输入关键词"); return@setPositiveButton }
                    poiSearch(act, kw)
                }.show()
        } catch (t: Throwable) { toast(act, "搜索失败: " + t.message) }
    }

    private fun poiSearch(act: Activity, keyword: String) {
        try {
            val cl = act.javaClass.classLoader
            val poiCls = Class.forName("com.amap.api.services.poisearch.PoiSearch", false, cl)
            val queryCls = Class.forName("com.amap.api.services.poisearch.PoiSearch\$Query", false, cl)
            val query = queryCls.getConstructor(String::class.java, String::class.java).newInstance(keyword, "")
            try { queryCls.getMethod("setPageSize", Int::class.javaPrimitiveType).invoke(query, 20) } catch (_: Throwable) {}
            val search = poiCls.getConstructor(Context::class.java, queryCls).newInstance(act, query)
            val lisCls = Class.forName("com.amap.api.services.poisearch.PoiSearch\$OnPoiSearchListener", false, cl)
            val proxy = java.lang.reflect.Proxy.newProxyInstance(cl, arrayOf(lisCls)) { _, m, args ->
                if (m.name == "onPoiSearched" && args != null && args.isNotEmpty()) {
                    try {
                        val result = args[0]
                        val pois = result.javaClass.getMethod("getPois").invoke(result) as List<*>
                        val list = ArrayList<Array<Any>>()
                        for (poi in pois) {
                            if (poi == null) continue
                            try {
                                val title = poi.javaClass.getMethod("getTitle").invoke(poi) as String
                                val llp = poi.javaClass.getMethod("getLatLonPoint").invoke(poi)
                                val la = llp.javaClass.getMethod("getLatitude").invoke(llp) as Double
                                val ln = llp.javaClass.getMethod("getLongitude").invoke(llp) as Double
                                list.add(arrayOf(title, la, ln))
                            } catch (_: Throwable) {}
                        }
                        Handler(Looper.getMainLooper()).post { showSearchResult(act, keyword, list) }
                    } catch (t: Throwable) { FnLogger.line("[Map] 解析搜索结果失败: " + t.message) }
                }
                null
            }
            poiCls.getMethod("setOnPoiSearchListener", lisCls).invoke(search, proxy)
            poiCls.getMethod("searchPOIAsyn").invoke(search)
            toast(act, "搜索中…")
            FnLogger.line("[Map] 搜索 " + keyword)
        } catch (t: Throwable) {
            toast(act, "搜索失败(需网络): " + t.message)
            FnLogger.line("[Map] 搜索异常: " + t.message)
        }
    }

    private fun showSearchResult(act: Activity, keyword: String, list: List<Array<Any>>) {
        try {
            if (list.isEmpty()) { toast(act, "没搜到「" + keyword + "」"); return }
            val box = LinearLayout(act).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(act,12), dp(act,8), dp(act,12), dp(act,8)) }
            for (item in list) {
                val title = item[0] as String; val la = item[1] as Double; val ln = item[2] as Double
                box.addView(TextView(act).apply {
                    text = title + "\n" + String.format("%.6f,%.6f", la, ln)
                    textSize = 14f; setTextColor(Color.BLACK); setPadding(0, dp(act,8), 0, dp(act,8))
                    setOnClickListener { doOpenAt(act, la, ln) }
                })
            }
            val sv = ScrollView(act).apply { addView(box) }
            AlertDialog.Builder(act).setTitle("搜索「" + keyword + "」").setView(sv).setPositiveButton("关闭", null).show()
        } catch (t: Throwable) { toast(act, "显示结果失败: " + t.message) }
    }

    // ============ 开启 / 指定点 ============
    private fun doOpen(act: Activity) {
        val lat = PrefsHolder.getFakeLat(); val lng = PrefsHolder.getFakeLng()
        if (lat == 0.0 && lng == 0.0) { toast(act, "请先在地图上选点"); return }
        PrefsHolder.setFakeEnabled(true)
        ShieldFake.applyFake(lat, lng)
        toast(act, "已开启虚拟定位")
        FnLogger.line("[Map] 开启虚拟定位 " + lat + "," + lng)
    }

    private fun doOpenAt(act: Activity, lat: Double, lng: Double) {
        PrefsHolder.setFakeLat(lat); PrefsHolder.setFakeLng(lng)
        PrefsHolder.setFakeEnabled(true)
        ShieldFake.applyFake(lat, lng)
        toast(act, "已设为定位点")
        FnLogger.line("[Map] 设为定位点 " + lat + "," + lng)
    }

    fun toast(c: Context, msg: String) {
        try { Handler(Looper.getMainLooper()).post { Toast.makeText(c, msg, Toast.LENGTH_SHORT).show() } } catch (_: Throwable) {}
    }
}
