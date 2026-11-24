package com.fnhook2

import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import java.util.concurrent.ConcurrentHashMap

/**
 * OkHttp 嗅探：记录所有请求 URL（定位违规上报接口）
 * 阶段1：只记URL和方法，不碰body（零风险）
 */
object OkHttpSniffer {

    private val seen = ConcurrentHashMap<String, Int>()
    fun install(cl: ClassLoader) {
        // 1) Request.url() → 所有请求URL
        try {
            val c = cl.loadClass("okhttp3.Request")
            XposedBridge.hookAllMethods(c, "url", object : XC_MethodHook() {
                override fun afterHookedMethod(p: MethodHookParam?) {
                    val u = p?.result?.toString() ?: return
                    if (u.length < 10 || u.length > 400) return
                    once("URL", u.substringBefore("?"))
                }
            })
            FnLogger.line("✔ OkHttp Request.url()")
        } catch (t: Throwable) { FnLogger.line("✘ OkHttp.url: " + t.message) }

        // 2) Request.method() → GET/POST
        try {
            val c = cl.loadClass("okhttp3.Request")
            XposedBridge.hookAllMethods(c, "method", object : XC_MethodHook() {
                override fun afterHookedMethod(p: MethodHookParam?) {
                    val m = p?.result?.toString() ?: return
                    once("METHOD", m)
                }
            })
        } catch (t: Throwable) {}

        // 3) RealCall.execute/enqueue → 请求发出（记录调用栈首行，定位上报类）
        try {
            val c = cl.loadClass("okhttp3.RealCall")
            for (mn in listOf("execute", "enqueue")) {
                try {
                    XposedBridge.hookAllMethods(c, mn, object : XC_MethodHook() {
                        override fun beforeHookedMethod(p: MethodHookParam?) {
                            val req = p?.args?.getOrNull(0)
                            val url = try {
                                req?.javaClass?.getMethod("url")?.invoke(req)?.toString() ?: ""
                            } catch (e: Throwable) { "" }
                            if (url.isNotEmpty()) {
                                val caller = Thread.currentThread().stackTrace
                                    .firstOrNull { it.className.startsWith("me.ele") || it.className.startsWith("g70") || it.className.startsWith("g60") }
                                    ?.let { it.className + "#" + it.methodName }
                                once("CALL:" + mn, url.substringBefore("?") + " ← " + (caller ?: "?"))
                            }
                        }
                    })
                } catch (_: Throwable) {}
            }
            FnLogger.line("✔ OkHttp RealCall execute/enqueue")
        } catch (t: Throwable) { FnLogger.line("✘ RealCall: " + t.message) }

        FnLogger.line("OkHttpSniffer 安装完成")
    }

    private fun once(kind: String, value: String) {
        val k = kind + "|" + value
        val n = seen.merge(k, 1) { a, _ -> a + 1 } ?: 1
        if (n == 1) FnLogger.line("[" + kind + "] " + value.take(250))
    }
}