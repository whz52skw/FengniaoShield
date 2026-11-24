package com.fnhook2

import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import java.util.concurrent.ConcurrentHashMap

/**
 * HBLocation 观察器：定位「上报」与「导航」用位置的差异
 * 记录 getTimestamp / getLatitude / getLongitude 的调用方
 */
object LocationObserver {

    private val seen = ConcurrentHashMap<String, Int>()

    fun install(cl: ClassLoader) {
        try {
            val c = cl.loadClass("me.ele.hb.location.model.HBLocation")
            val getters = listOf("getTimestamp", "getLatitude", "getLongitude", "getAccuracy", "getLocationTypeMask", "getLocationSourceMask")
            for (g in getters) {
                try {
                    XposedBridge.hookAllMethods(c, g, object : XC_MethodHook() {
                        override fun afterHookedMethod(p: MethodHookParam?) {
                            val r = p?.result ?: return
                            observe(g, r.toString())
                        }
                    })
                } catch (_: Throwable) {}
            }
            FnLogger.line("✔ HBLocation 观察器 已挂")
        } catch (t: Throwable) { FnLogger.line("✘ HBLocation 观察: " + t.message) }

        // 位置分发点 messages.d.H(HBLocation)
        try {
            val c = cl.loadClass("me.ele.hb.biz.order.magex.messages.d")
            for (m in c.declaredMethods) {
                if (m.parameterTypes.firstOrNull()?.name == "me.ele.hb.location.model.HBLocation") {
                    XposedBridge.hookMethod(m, object : XC_MethodHook() {
                        override fun afterHookedMethod(p: MethodHookParam?) {
                            val loc = p?.args?.getOrNull(0) ?: return
                            try {
                                val lat = loc.javaClass.getMethod("getLatitude").invoke(loc)
                                val lng = loc.javaClass.getMethod("getLongitude").invoke(loc)
                                val ts = loc.javaClass.getMethod("getTimestamp").invoke(loc)
                                val acc = loc.javaClass.getMethod("getAccuracy").invoke(loc)
                                val key = "DISPATCH|H"
                                val n = seen.merge(key, 1) { a, _ -> a + 1 } ?: 1
                                if (n <= 3 || n % 300 == 0) {
                                    FnLogger.line("[定位分发] H(" + m.name + ") lat=" + lat + " lng=" + lng + " ts=" + ts + " acc=" + acc + " (第" + n + "次)")
                                }
                            } catch (_: Throwable) {}
                        }
                    })
                    FnLogger.line("✔ 定位分发点 messages.d." + m.name + "(HBLocation) 已观察")
                    break
                }
            }
        } catch (t: Throwable) { FnLogger.line("✘ 定位分发: " + t.message) }

        FnLogger.line("LocationObserver 安装完成")
    }

    private fun observe(method: String, value: String) {
        // 只统计调用来源（去重），避免刷屏
        val stack = Thread.currentThread().stackTrace
        val caller = stack.firstOrNull { it.className.startsWith("me.ele") || it.className.startsWith("g70") || it.className.startsWith("g60") || it.className.startsWith("g1") || it.className.startsWith("anetwork") }
        val key = method + "<-" + (caller?.let { it.className + "#" + it.methodName } ?: "?")
        val n = seen.merge(key, 1) { a, _ -> a + 1 } ?: 1
        if (n == 1) FnLogger.line("[定位调用] " + key + " = " + value.take(30))
    }
}