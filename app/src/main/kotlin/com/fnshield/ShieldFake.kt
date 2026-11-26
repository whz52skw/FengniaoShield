package com.fnshield

import android.location.Location
import android.location.LocationListener
import android.os.Handler
import android.os.Looper
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import java.util.Timer
import java.util.TimerTask
import java.util.concurrent.CopyOnWriteArrayList

/**
 * 虚拟定位（系统 LocationManager 层）+ 模拟路线 + 遵守交规控速
 * 照 FengGPS 的做法：hook getLastKnownLocation / requestLocationUpdates
 */
object ShieldFake {
    @Volatile private var fakeLat = 0.0
    @Volatile private var fakeLng = 0.0
    @Volatile private var fakeSpeed = 6.5f
    private val listeners = CopyOnWriteArrayList<LocationListener>()
    private var simTimer: Timer? = null
    private var step = 0

    fun install(cl: ClassLoader) {
        reload()
        hookLastKnown()
        hookRequestUpdates()
        hookAMapGetter(cl)
        FnLogger.line("[Fake] 虚拟定位层已挂")
    }

    fun reload() {
        fakeLat = PrefsHolder.getFakeLat()
        fakeLng = PrefsHolder.getFakeLng()
    }

    fun applyFake(lat: Double, lng: Double) {
        fakeLat = lat; fakeLng = lng
        PrefsHolder.setFakeLat(lat); PrefsHolder.setFakeLng(lng)
        pushFake()
        FnLogger.line("[Fake] 应用假坐标 " + lat + "," + lng)
    }

    private fun makeLoc(): Location {
        val l = Location("gps")
        l.latitude = fakeLat; l.longitude = fakeLng
        l.speed = fakeSpeed; l.accuracy = 5f
        l.time = System.currentTimeMillis(); l.provider = "gps"
        return l
    }

    private fun pushFake() {
        val loc = makeLoc()
        for (lis in listeners) {
            try { Handler(Looper.getMainLooper()).post { lis.onLocationChanged(loc) } } catch (_: Throwable) {}
        }
    }

    private fun hookLastKnown() {
        try {
            val c = Class.forName("android.location.LocationManager")
            for (m in c.declaredMethods) {
                if (m.name != "getLastKnownLocation") continue
                XposedBridge.hookMethod(m, object : XC_MethodHook() {
                    override fun beforeHookedMethod(p: MethodHookParam?) {
                        if (p == null || !PrefsHolder.getFakeEnabled()) return
                        HitLog.hit("假定位-last"); p.result = makeLoc()
                    }
                })
                FnLogger.line("✔ 假定位 getLastKnownLocation")
            }
        } catch (t: Throwable) { FnLogger.line("✘ 假定位 last: " + t.message) }
    }

    private fun hookRequestUpdates() {
        try {
            val c = Class.forName("android.location.LocationManager")
            for (m in c.declaredMethods) {
                if (m.name != "requestLocationUpdates") continue
                XposedBridge.hookMethod(m, object : XC_MethodHook() {
                    override fun beforeHookedMethod(p: MethodHookParam?) {
                        if (p == null || !PrefsHolder.getFakeEnabled()) return
                        for (a in p.args) if (a is LocationListener) {
                            if (!listeners.contains(a)) listeners.add(a)
                            HitLog.hit("假定位-updates")
                        }
                    }
                })
                FnLogger.line("✔ 假定位 requestLocationUpdates")
            }
        } catch (t: Throwable) { FnLogger.line("✘ 假定位 updates: " + t.message) }
    }

    /** 双保险：高德 AMapLocation 坐标 getter 也拦 */
    private fun hookAMapGetter(cl: ClassLoader) {
        try {
            val c = cl.loadClass("com.amap.api.location.AMapLocation")
            for (m in c.declaredMethods) {
                if (m.name != "getLatitude" && m.name != "getLongitude" && m.name != "getSpeed") continue
                val isLat = m.name == "getLatitude"
                val isLng = m.name == "getLongitude"
                XposedBridge.hookMethod(m, object : XC_MethodHook() {
                    override fun beforeHookedMethod(p: MethodHookParam?) {
                        if (p == null || !PrefsHolder.getFakeEnabled()) return
                        HitLog.hit("假定位-amap")
                        when {
                            isLat -> p.result = fakeLat
                            isLng -> p.result = fakeLng
                            else -> p.result = if (PrefsHolder.obeyTraffic) fakeSpeed else 40f
                        }
                    }
                })
                FnLogger.line("✔ 假定位 AMapLocation." + m.name)
            }
        } catch (t: Throwable) { FnLogger.line("✘ 假定位 amap: " + t.message) }
    }

    fun setRouteSim(on: Boolean) {
        if (on) startSim() else stopSim()
    }

    private fun startSim() {
        stopSim(); step = 0
        if (fakeLat == 0.0 && fakeLng == 0.0) { FnLogger.line("[Fake] 路线模拟需先设坐标"); return }
        simTimer = Timer("FnShieldSim", true)
        simTimer?.scheduleAtFixedRate(object : TimerTask() {
            override fun run() {
                try {
                    step++
                    fakeLat += 0.00009
                    PrefsHolder.setFakeLat(fakeLat)
                    pushFake()
                    if (step % 10 == 0) FnLogger.line("[Fake] 路线推进 step=" + step)
                } catch (_: Throwable) {}
            }
        }, 3000, 3000)
        FnLogger.line("[Fake] 模拟路线启动（遵守交规控速=" + PrefsHolder.obeyTraffic + "）")
    }

    private fun stopSim() { try { simTimer?.cancel() } catch (_: Throwable) {}; simTimer = null }
}
