package com.fnhook2

import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge

/**
 * Normandy 安全采样系统拦截（闯红灯/路口超速/行为违规的数据源）
 * ★修复版：按返回类型智能拦截（防 NPE）
 */
object NormandyHooks {

    fun install(cl: ClassLoader) {
        // 第一层：采集源头
        hookAll(cl, "me.ele.normandie.predict.gather.DataGatheringManager", "采样-启动采集",
            listOf("startAllDataGathering", "startGatheringForIoDetector", "startGatheringForModelPredict"))

        // 第二层：上传出口
        hookAll(cl, "me.ele.normandie.sampling.api.WebServer", "采样-数据上传",
            listOf("uploadSamplingSceneData", "uploadSamplingLocationData", "uploadShippingOrders", "downloadConfigSync", "downloadExceptionDataConfigSync"))
        hookAll(cl, "me.ele.normandie.sampling.api.WebServer", "采样-传感器上传",
            listOf("uploadSensorData"))

        // 第三层：检测/推理（只拦 void/boolean，对象返回放行）
        hookAll(cl, "me.ele.normandie.predict.iodetector.DetectorManager", "采样-IO检测",
            listOf("startDetector", "initDetector"))
        hookAll(cl, "me.ele.normandie.predict.inference.InferenceManager", "采样-推理",
            listOf("inferenceResult"))

        // 采样入口（samplingWithSceneId 是 void）
        hookAll(cl, "me.ele.normandie.sampling.NormandyApi", "采样-入口",
            listOf("samplingWithSceneId"))

        FnLogger.line("Normandy 三层拦截(智能类型版) 全部挂载")
    }

    private fun applyResult(p: XC_MethodHook.MethodHookParam, rt: Class<*>) {
        when {
            rt == Void.TYPE -> p.result = null
            rt == java.lang.Boolean.TYPE -> p.result = false
            rt == Integer.TYPE -> p.result = 0
            rt == java.lang.Long.TYPE -> p.result = 0L
            rt == java.lang.Float.TYPE -> p.result = 0f
            rt == java.lang.Double.TYPE -> p.result = 0.0
            else -> return
        }
    }

    private fun hookAll(cl: ClassLoader, className: String, tag: String, methods: List<String>) {
        try {
            val c = cl.loadClass(className)
            var n = 0
            for (mn in methods) {
                for (m in c.declaredMethods) {
                    if (m.name != mn) continue
                    val rt = m.returnType
                    if (rt != Void.TYPE && rt.isPrimitive.not()) continue   // 对象返回放行
                    XposedBridge.hookMethod(m, object : XC_MethodHook() {
                        override fun beforeHookedMethod(p: MethodHookParam?) {
                            if (p == null) return
                            HitLog.hit(tag)
                            applyResult(p, rt)
                        }
                    })
                    n++
                }
            }
    
        // ===== 对标 location.patch 的采样全链（10.11.1 真身） =====
        SmartHook.hookClass(cl, "me.ele.normandie.sampling.collector.DataCollectorManager", "采样-采集器", { mn ->
            mn.startsWith("start") || mn.startsWith("stop") || mn.startsWith("sampling") ||
            mn.contains("Sampling") || mn.contains("Predict") || mn.contains("Upload") ||
            mn.contains("Gather") || mn.contains("Gnss") || mn.contains("Nmea") ||
            mn == "getGpsLocations" || mn == "getHorizontalBehaviorStateList4OverSpeed" ||
            mn == "getIoDetectorStateList4OverSpeed" || mn == "addBehaviorStateCallback" ||
            mn == "registerNormandieDataCall" || mn == "saveInfoOpr" || mn == "initNormandie" ||
            mn == "updateMetaData" || mn == "sendPredictDataToCallback" ||
            mn == "sendIODetectorResultToCallback" || mn == "addNormandieDataCallback" ||
            mn == "addNormandieIoTDataCallback" || mn == "removeAllNormandieDataCallback"
        })
        SmartHook.hookClass(cl, "me.ele.normandie.sampling.NormandyApi", "采样-API")
        SmartHook.hookClass(cl, "me.ele.normandie.datagathering.sensor.ChangedSensorListener", "采样-传感器")
        SmartHook.hookClass(cl, "me.ele.normandie.datagathering.location.GpsSensorCallback", "采样-GPS回调")
        SmartHook.hookClass(cl, "me.ele.normandie.datagathering.location.GnssNemaCallback", "采样-GNSS回调")
        SmartHook.hookClass(cl, "me.ele.normandie.datagathering.sensor.SensorChangeCallback", "采样-传感回调")
        // PredictedDataListener 摘除（其 onLocationChange 断跑单中心位置链，patch 也不拦此接口）

        FnLogger.line("✔ " + className + " 拦截 " + n + " 方法 [" + tag + "]")
        } catch (t: Throwable) { FnLogger.line("✘ " + className + ": " + t.message) }
    }
}