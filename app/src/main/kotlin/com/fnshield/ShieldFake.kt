package com.fnshield

import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import java.util.Timer
import java.util.TimerTask

/**
 * 假定位 + 路线模拟 + 遵守交规（整合版 FnShield 专用）
 * 原理：hook 高德 AMapLocation 的坐标/速度 getter，返回预设假值；
 * 路线模拟用后台定时器沿预设路线推进假坐标；遵守交规默认开（无开关），钳制速度到合法范围。
 */
object ShieldFake {
    @Volatile private var fakeLat = 0.0
    @Volatile private var fakeLng = 0.0
    private var timer: Timer? = null

    fun reload() {
        fakeLat = PrefsHolder.getFakeLat()
        fakeLng = PrefsHolder.getFakeLng()
    }

    fun install() {
        reload()
        val cl = PrefsHolder.classLoader
        // 坐标 getter（双精度）
        hookCoord(cl, "com.amap.api.location.AMapLocation", "getLatitude", true)
        hookCoord(cl, "com.amap.api.location.AMapLocation", "getLongitude", false)
        hookCoord(cl, "com.amap.api.location.AMapLocation", "getAltitude", false)
        // 速度 getter（遵守交规钳制）
        hookSpeed(cl)
        FnLogger.line("[Fake] 假定位层挂载 complete")
    }

    private fun hookCoord(cl: ClassLoader, className: String, method: String, isLat: Boolean) {
        try {
            val c = cl.loadClass(className)
            for (m in c.declaredMethods) {
                if (m.name != method || m.returnType != java.lang.Double.TYPE) continue
                XposedBridge.hookMethod(m, object : XC_MethodHook() {
                    override fun beforeHookedMethod(p: MethodHookParam?) {
                        if (p == null || !PrefsHolder.getFakeEnabled()) return
                        HitLog.hit("假定位-坐标")
                        p.result = if (isLat) PrefsHolder.getFakeLat() else PrefsHolder.getFakeLng()
                    }
                })
                FnLogger.line("✔ 假定位 " + className.substringAfterLast(".") + "." + method)
            }
        } catch (t: Throwable) { FnLogger.line("✘ 假定位 " + method + ": " + t.message) }
    }

    private fun hookSpeed(cl: ClassLoader) {
        try {
            val c = cl.loadClass("com.amap.api.location.AMapLocation")
            for (m in c.declaredMethods) {
                if (m.name != "getSpeed" || m.returnType != java.lang.Float.TYPE) continue
                XposedBridge.hookMethod(m, object : XC_MethodHook() {
                    override fun beforeHookedMethod(p: MethodHookParam?) {
                        if (p == null) return
                        if (PrefsHolder.getFakeEnabled() || PrefsHolder.getRouteSim()) {
                            HitLog.hit("假定位-速度")
                            // 遵守交规默认开：钳制到 25 km/h 内（市道合法区间）
                            p.result = if (PrefsHolder.obeyTraffic) 6.5f else 40.0f
                        }
                    }
                })
                FnLogger.line("✔ 假定位 getSpeed(遵守交规钳制)")
            }
        } catch (t: Throwable) { FnLogger.line("✘ 假定位 getSpeed: " + t.message) }
    }

    // ===== 路线模拟：按预设坐标点定时推进 =====
    fun startRouteSim(points: List<DoubleArray>, intervalMs: Long = 3000) {
        stopRouteSim()
        if (points.size < 2) return
        timer = Timer("FnShieldRouteSim", true)
        var idx = 0
        PrefsHolder.setFakeLat(points[0][0]); PrefsHolder.setFakeLng(points[0][1])
        reload()
        timer!!.scheduleAtFixedRate(object : TimerTask() {
            override fun run() {
                val q = points[idx % points.size]
                PrefsHolder.setFakeLat(q[0]); PrefsHolder.setFakeLng(q[1])
                reload()
                FnLogger.line("[Fake] 路线模拟推进 -> " + q[0] + "," + q[1])
                idx++
            }
        }, intervalMs, intervalMs)
        FnLogger.line("[Fake] 路线模拟启动 点=" + points.size + " 间隔=" + intervalMs + "ms")
    }

    fun stopRouteSim() {
        timer?.cancel(); timer = null
    }
}
