package com.fnhook2

import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge

/**
 * 安全屏蔽 v2（真机 10.11.1 全链）
 * 拦截规则：void → null；boolean → false；int/long/float/double → 0；对象 → 放行（防 NPE）
 * 特殊：上传 API 返回 rx.c（RxJava1 Observable）→ 返回 c.empty() 空流
 */
object SafetyHooks {

    fun install() {
        val cl = PrefsHolder.classLoader

        // ============ 1) 超速 ============
        hookAll(cl, "me.ele.punchingservice.utils.PunchingConfigUtil", "超速总开关", { it.contains("Speeding") || it == "isOpenSpeedingDetection" })
        hookMethod(cl, "me.ele.hb.ai.hbbehavior.detector.SpeedDetector\u00242", "onSuccess", "超速AI回调")
        // OverSpeedManager 全类（混淆方法名 b..w：f=位置入口 g=超速点计算 o=上报 s/t/u/v/w=上传）
        hookAll(cl, "me.ele.hb.biz.voiceservice.OverSpeedManager", "超速管理器", { true })
        hookAll(cl, "me.ele.hb.biz.voiceservice.OverSpeedManager\u0024a", "超速行为回调", { true })
        hookAll(cl, "me.ele.hb.biz.voiceservice.OverSpeedManager\u0024b", "超速检测回调", { true })
        hookAll(cl, "me.ele.hb.biz.voiceservice.OverSpeedManager\u0024c", "超速内部", { true })
        hookAll(cl, "me.ele.hb.overspeed.floatwindow.HBFloatWindowManager", "超速悬浮窗", { it.contains("Speed") })
        hookMethod(cl, "me.ele.hb.trafficsafety.model.OverSpeedConfigModel", "getOverSpeedConfigDetailModelList", "超速配置下发")
        hookClassTrue(cl, "me.ele.hb.biz.voiceservice.model.OverSpeedPointModel", "isOverSpeed", "超速点判定")
        hookClassTrue(cl, "me.ele.hb.biz.voiceservice.model.OverSpeedUploadModel", "isBroadcast", "超速播报标记")

        // ============ 2) 闯红灯（10.11.1 全链） ============
        // ★ 判定管理器（TrafficLightManagerV2）——之前的最大漏洞
        hookAll(cl, "mj0.a", "闯红灯管理器", { true })
        // 判定核心
        hookAll(cl, "jj0.c", "闯红灯判定核心", { true })
        hookAll(cl, "jj0.d", "闯红灯状态机", { true })
        hookAll(cl, "jj0.d\u0024a", "闯红灯回调", { true })
        hookAll(cl, "jj0.d\u0024b", "闯红灯数据", { true })
        hookAll(cl, "jj0.q", "闯红灯上报", { true })
        hookAll(cl, "jj0.u", "闯红灯工具", { true })
        hookAll(cl, "jj0.e", "闯红灯事件", { true })
        hookAll(cl, "jj0.s", "闯红灯状态", { true })
        hookAll(cl, "jj0.j", "闯红灯队列", { true })
        hookAll(cl, "jj0.i", "闯红灯刷新", { true })
        // 判定模型
        hookAll(cl, "me.ele.hb.trafficsafety.model.MoveStateChangeResult", "闯红灯判定结果", { true })
        hookAll(cl, "me.ele.hb.trafficsafety.model.SceneConfig4MNN", "闯红灯MNN", { true })
        hookAll(cl, "me.ele.hb.trafficsafety.model.FusedLocationResultModel", "闯红灯融合定位", { true })
        hookAll(cl, "me.ele.hb.biz.engine.navi.model.HBNaviTrafficLightState", "红绿灯状态", { true })
        hookAll(cl, "me.ele.hb.biz.order.magex.dynamicmap.workcenter.l\u0024g", "闯红灯事件", { mn ->
            mn.contains("RedLight") || mn.contains("TrafficLight") || mn.contains("GreenLight")
        })

        // ============ 3) 违规语音播报静音 ============
        hookAll(cl, "me.ele.hb.voice.spi.mist.actions.ReplayInvoker", "语音播报-违规", { true })
        hookAll(cl, "me.ele.hb.voice.spi.mist.actions.v2.ReplayInvokers", "语音播报-违规v2", { true })
        hookAll(cl, "me.ele.hb.voice.spi.mist.actions.v2.HelmetReplayInvokers", "语音播报-头盔", { true })

        // ============ 4) 头盔守护 ============
        hookMethod(cl, "id0.b", "i", "头盔采集启动")
        hookMethod(cl, "id0.b", "h", "头盔守护配置")
        hookMethod(cl, "id0.b", "onCameraOpened", "头盔摄像头打开")
        hookMethod(cl, "id0.b", "b", "头盔状态回调")
        hookMethod(cl, "id0.b", "a", "头盔停止")
        hookMethod(cl, "id0.b\u0024c", "run", "头盔采集线程")
        hookAll(cl, "id0.b\u0024b", "头盔OSS上传", { true })
        hookMethod(cl, "xy0.b\u0024b", "run", "头盔任务run")
        hookMethod(cl, "xy0.b", "f", "头盔任务f")
        hookAll(cl, "jd0.j\u0024a", "头盔抓拍上传", { it.contains("Upload") || it == "b" || it.contains("Failure") })
        hookMethod(cl, "g70.d", "o", "头盔数据上报")

        // ============ 5) 分心驾驶 ============
        hookBoolTrue(cl, "me.ele.hb.ai.hbbehavior.detector.SpeedDetector", "n", "分心驾驶总开关")
        hookMethod(cl, "me.ele.hb.ai.hbbehavior.detector.SpeedDetector\u00242", "onError", "分心驾驶判定")
        hookAll(cl, "dx0.h", "分心驾驶上报器", { it == "b" || it == "m" || it == "g" || it == "z" || it == "w" })
        hookAll(cl, "fx0.a", "分心驾驶埋点", { true })
        hookAll(cl, "me.ele.hb.trafficsafety.model.UsePhoneConfigModel", "分心驾驶配置", { it.startsWith("get") })

        // ============ 6) 上传口（返回 rx.c 的空流拦截） ============
        hookRxApi(cl, "me.ele.hb.biz.voiceservice.network.RiderSafeApi", "上传口-RiderSafe", { it == "d" || it == "c" })
        hookRxApi(cl, "me.ele.hb.biz.voiceservice.network.PizzaApi", "上传口-Pizza", { it == "c" })
        hookAll(cl, "me.ele.hb.biz.voiceservice.OverSpeedManager\u00247", "超速上报触发", { true })
        hookAll(cl, "me.ele.hb.biz.voiceservice.OverSpeedManager\u00248", "超速上报触发2", { true })
        hookAll(cl, "iv0.b\u0024d", "超速通报回调", { true })

        FnLogger.line("安全屏蔽v2: 超速全链+闯红灯全链+播报静音+头盔+分心 全挂载")
    }

    /** boolean 方法返回 true */
    private fun hookBoolTrue(cl: ClassLoader, className: String, method: String, tag: String) {
        try {
            val c = cl.loadClass(className)
            var n = 0
            for (m in c.declaredMethods) {
                if (m.name != method) continue
                XposedBridge.hookMethod(m, object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam?) {
                        if (param == null) return
                        HitLog.hit(tag)
                        param.result = true
                    }
                })
                n++
            }
            FnLogger.line("✔ " + className + "." + method + "() →true [" + tag + "] ×" + n)
        } catch (t: Throwable) { FnLogger.line("✘ " + className + "." + method + ": " + t.message) }
    }

    /** 指定方法返回 false（isOverSpeed 等） */
    private fun hookClassTrue(cl: ClassLoader, className: String, method: String, tag: String) {
        try {
            val c = cl.loadClass(className)
            var n = 0
            for (m in c.declaredMethods) {
                if (m.name != method || m.returnType != java.lang.Boolean.TYPE) continue
                XposedBridge.hookMethod(m, object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam?) {
                        if (param == null) return
                        HitLog.hit(tag)
                        param.result = false
                    }
                })
                n++
            }
            FnLogger.line("✔ " + className + "." + method + "() →false [" + tag + "] ×" + n)
        } catch (t: Throwable) { FnLogger.line("✘ " + className + "." + method + ": " + t.message) }
    }

    /** 上传 API：返回 rx.c.empty() 空流（RxJava1） */
    private fun hookRxApi(cl: ClassLoader, className: String, tag: String, pred: (String) -> Boolean) {
        try {
            val c = cl.loadClass(className)
            var empty: Any? = null
            for (cn in listOf("rx.c", "rx.Observable", "io.reactivex.Observable")) {
                try {
                    val oc = cl.loadClass(cn)
                    empty = oc.getMethod("empty").invoke(null)
                    FnLogger.line("[上传口] 空流类=" + cn)
                    break
                } catch (_: Throwable) {}
            }
            var n = 0
            for (m in c.declaredMethods) {
                if (!pred(m.name)) continue
                val e = empty
                XposedBridge.hookMethod(m, object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam?) {
                        if (param == null) return
                        HitLog.hit(tag)
                        if (e != null) param.result = e
                        // 空流不可用：只记录不拦（等价透传，绝不让调用方 NPE）
                    }
                })
                n++
            }
            FnLogger.line("✔ " + className + " 拦截 " + n + " 上传方法 [" + tag + "] 空流=" + (empty != null))
        } catch (t: Throwable) { FnLogger.line("✘ " + className + ": " + t.message) }
    }

    private fun applyResult(p: XC_MethodHook.MethodHookParam, rt: Class<*>) {
        when {
            rt == Void.TYPE -> p.result = null
            rt == java.lang.Boolean.TYPE -> p.result = false
            rt == Integer.TYPE -> p.result = 0
            rt == java.lang.Long.TYPE -> p.result = 0L
            rt == java.lang.Float.TYPE -> p.result = 0f
            rt == java.lang.Double.TYPE -> p.result = 0.0
            rt == java.lang.Short.TYPE -> p.result = 0.toShort()
            rt == java.lang.Byte.TYPE -> p.result = 0.toByte()
            rt == java.lang.Character.TYPE -> p.result = '\u0000'
            else -> return
        }
    }

    private fun hookAll(cl: ClassLoader, className: String, tag: String, pred: (String) -> Boolean) {
        try {
            val c = cl.loadClass(className)
            var n = 0
            for (m in c.declaredMethods) {
                if (!pred(m.name)) continue
                val rt = m.returnType
                if (rt != Void.TYPE && rt.isPrimitive.not()) continue
                XposedBridge.hookMethod(m, object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam?) {
                        if (param == null) return
                        HitLog.hit(tag)
                        applyResult(param, rt)
                    }
                })
                n++
            }
            HitLog.register(tag)
            FnLogger.line("✔ " + className + " 拦截 " + n + " 方法 [" + tag + "]")
        } catch (t: Throwable) { FnLogger.line("✘ " + className + ": " + t.message) }
    }

    private fun hookMethod(cl: ClassLoader, className: String, method: String, tag: String) {
        try {
            val c = cl.loadClass(className)
            var n = 0
            for (m in c.declaredMethods) {
                if (m.name != method) continue
                val rt = m.returnType
                if (rt != Void.TYPE && rt.isPrimitive.not()) continue
                XposedBridge.hookMethod(m, object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam?) {
                        if (param == null) return
                        HitLog.hit(tag)
                        applyResult(param, rt)
                    }
                })
                n++
            }
            FnLogger.line("✔ " + className + "." + method + "() 拦截 " + n + " 个")
        } catch (t: Throwable) { FnLogger.line("✘ " + className + "." + method + ": " + t.message) }
    }
}
