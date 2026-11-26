package com.fnshield

import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import java.util.concurrent.ConcurrentHashMap

/**
 * TNet(anetwork) 请求嗅探：拿到所有网络请求 URL
 * anetwork.channel.Request 是 TNet 在 Java 层的唯一入口对象
 */
object AnetworkSniffer {

    private val seen = ConcurrentHashMap<String, Int>()

    fun install(cl: ClassLoader) {
        try {
            val c = cl.loadClass("anetwork.channel.Request")
            // 1) getUrlString
            try {
                XposedBridge.hookAllMethods(c, "getUrlString", object : XC_MethodHook() {
                    override fun afterHookedMethod(p: MethodHookParam?) {
                        val u = p?.result?.toString() ?: return
                        if (u.length < 10 || u.length > 500) return
                        once("URL", u)
                    }
                })
            } catch (t: Throwable) {}
            // 2) getMethod
            try {
                XposedBridge.hookAllMethods(c, "getMethod", object : XC_MethodHook() {
                    override fun afterHookedMethod(p: MethodHookParam?) {
                        val m = p?.result?.toString() ?: return
                        once("METHOD", m)
                    }
                })
            } catch (t: Throwable) {}
            // 3) getParams（请求参数）
            try {
                XposedBridge.hookAllMethods(c, "getParams", object : XC_MethodHook() {
                    override fun afterHookedMethod(p: MethodHookParam?) {
                        val r = p?.result ?: return
                        val s = r.toString()
                        if (s.length in 3..800) once("PARAMS", s)
                    }
                })
            } catch (t: Throwable) {}
            FnLogger.line("✔ TNet anetwork.channel.Request 已嗅探")
        } catch (t: Throwable) { FnLogger.line("✘ anetwork.Request: " + t.message) }

        FnLogger.line("AnetworkSniffer 安装完成")
    }

    private fun once(kind: String, value: String) {
        val k = kind + "|" + value
        val n = seen.merge(k, 1) { a, _ -> a + 1 } ?: 1
        if (n == 1) FnLogger.line("[TNET] " + value.take(280))
    }
}