package com.fnshield

import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import java.util.concurrent.ConcurrentHashMap

/**
 * 双路位置观察器：同时抓高德定位 + 系统定位
 * 目的：确认 App 实际收到的位置（验证 LocationMax 是否生效）
 */
object LocationObserver2 {

    private val lastLog = ConcurrentHashMap<String, Long>()

    fun install(cl: ClassLoader) {
        // ===== 1) 高德定位 =====
        try {
            val c = cl.loadClass("com.amap.api.location.AMapLocation")
            var n = 0
            for (mn in listOf("getLatitude", "getLongitude", "getSpeed", "getAccuracy", "getProvider", "getTime")) {
                try {
                    XposedBridge.hookAllMethods(c, mn, object : XC_MethodHook() {
                        override fun afterHookedMethod(p: MethodHookParam?) {
                            val v = p?.result ?: return
                            record("AMap." + (p.method?.name ?: "?"), v.toString())
                        }
                    })
                    n++
                } catch (_: Throwable) {}
            }
            FnLogger.line("✔ AMapLocation 观察(" + n + ")")
        } catch (t: Throwable) { FnLogger.line("✘ AMapLocation: " + t.message) }

        // ===== 2) 系统定位 =====
        try {
            val c = cl.loadClass("android.location.Location")
            var n = 0
            for (mn in listOf("getLatitude", "getLongitude", "getSpeed", "getAccuracy")) {
                try {
                    XposedBridge.hookAllMethods(c, mn, object : XC_MethodHook() {
                        override fun afterHookedMethod(p: MethodHookParam?) {
                            val v = p?.result ?: return
                            // 只记录调用方是蜂鸟的
                            val stack = Thread.currentThread().stackTrace
                            val caller = stack.firstOrNull { it.className.startsWith("me.ele") }
                            if (caller == null) return
                            record("Sys." + (p.method?.name ?: "?"), v.toString(), caller.className + "#" + caller.methodName)
                        }
                    })
                    n++
                } catch (_: Throwable) {}
            }
            FnLogger.line("✔ android.location.Location 观察(" + n + ")")
        } catch (t: Throwable) { FnLogger.line("✘ Location: " + t.message) }

        FnLogger.line("LocationObserver2 安装完成")
    }

    private fun record(kind: String, value: String, caller: String? = null) {
        val now = System.currentTimeMillis()
        val last = lastLog[kind] ?: 0L
        if (now - last < 5000) return   // 同名方法5秒内只记一次
        lastLog[kind] = now
        val extra = if (caller != null) " ← " + caller else ""
        FnLogger.line("[位置] " + kind + " = " + value.take(30) + extra)
    }
}