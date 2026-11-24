package com.fnhook2

import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge

/** 无视距离：停车/落点/起点 距离校验全放行（点送达/拉回商圈不被判可疑） */
object IgnoreDistanceHooks {

    fun install() {
        val cl = PrefsHolder.classLoader

        // DistanceCheckResult：合法判定全 true + 可疑 false
        for (mn in listOf("isGpsLawful", "isWifiLawful", "isBeaconLawful", "isSupportBeacon")) {
            try {
                val c = cl.loadClass("me.ele.hb.biz.order.pipeline.util.distancecheck.DistanceCheckResult")
                for (m in c.declaredMethods) {
                    if (m.name != mn) continue
                    XposedBridge.hookMethod(m, object : XC_MethodHook() {
                        override fun beforeHookedMethod(p: MethodHookParam?) {
                            if (p == null || !PrefsHolder.ignoreDistanceEnabled) return
                            HitLog.hit("无视距离-合法")
                            p.result = true
                        }
                    })
                    FnLogger.line("✔ 无视距离 " + mn + " →true")
                }
            } catch (t: Throwable) { FnLogger.line("✘ 无视距离 " + mn + ": " + t.message) }
        }
        // DistanceCheckResult 其余 boolean（isSuspectedOperate → false）+ 数值归零
        try {
            val c = cl.loadClass("me.ele.hb.biz.order.pipeline.util.distancecheck.DistanceCheckResult")
            var n = 0
            for (m in c.declaredMethods) {
                if (m.name.startsWith("is") && m.name != "isGpsLawful" && m.name != "isWifiLawful" && m.name != "isBeaconLawful" && m.name != "isSupportBeacon") {
                    try {
                        XposedBridge.hookMethod(m, object : XC_MethodHook() {
                            override fun beforeHookedMethod(p: MethodHookParam?) {
                                if (p == null || !PrefsHolder.ignoreDistanceEnabled) return
                                HitLog.hit("无视距离-可疑")
                                if (m.returnType == java.lang.Boolean.TYPE) p.result = false else if (m.returnType != Void.TYPE && m.returnType.isPrimitive) p.result = 0
                            }
                        })
                        n++
                    } catch (_: Throwable) {}
                }
            }
            FnLogger.line("✔ 无视距离 其余判定 " + n + " 方法")
        } catch (t: Throwable) { FnLogger.line("✘ 无视距离全套: " + t.message) }

        FnLogger.line("无视距离层挂载完成 开关=" + PrefsHolder.ignoreDistanceEnabled)
    }
}
