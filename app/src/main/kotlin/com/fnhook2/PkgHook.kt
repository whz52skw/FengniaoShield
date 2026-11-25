package com.fnhook2

import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge

/** 全国通用兜底层：包级清单 hook（类名已修正） */
object PkgHook {

    private val A: Array<String> = arrayOf(
            "me.ele.hb.ai.hbbehavior.detector.PeriodSpeedModel", "me.ele.hb.ai.hbbehavior.detector.SpeedDetector", "me.ele.hb.ai.hbbehavior.detector.SpeedDetector\u00241", "me.ele.hb.ai.hbbehavior.detector.SpeedDetector\u00242", "me.ele.hb.ai.hbbehavior.detector.SpeedDetector\u00242\u00241", "me.ele.hb.ai.hbbehavior.detector.SpeedDetector\u00242\u00242",
            "me.ele.hb.ai.hbbehavior.detector.SpeedDetector\u00242\u00243", "me.ele.hb.ai.hbbehavior.detector.SpeedDetector\u0024a", "me.ele.hb.ai.hbbehavior.detector.SpeedDetector\u0024b", "me.ele.hb.ai.hbbehavior.detector.SpeedDetector\u0024c", "me.ele.hb.ai.hbbehavior.detector.SpeedPointModel", "me.ele.hb.ai.hbbehavior.detector.a",
            "me.ele.hb.ai.hbbehavior.detector.a\u0024a", "me.ele.hb.ai.hbbehavior.detector.a\u0024b", "me.ele.hb.ai.hbbehavior.model.LimitQueue", "me.ele.hb.ai.hbbehavior.model.OverSpeedConfigModel", "me.ele.hb.ai.hbbehavior.model.OverSpeedConfigRequestParam", "me.ele.hb.ai.hbbehavior.model.OverSpeedPointModel",
            "me.ele.hb.ai.hbbehavior.model.OverSpeedUploadModel", "me.ele.hb.ai.hbbehavior.model.OverSpeedUploadRequestParam", "me.ele.hb.ai.hbbehavior.model.RetrogradeLinkListModel", "me.ele.hb.ai.hbbehavior.model.RetrogradeLinkModel", "me.ele.hb.ai.hbbehavior.model.RetrogradeLinkResponse", "me.ele.hb.ai.hbbehavior.model.RetrogradeRules",
            "me.ele.hb.ai.hbbehavior.network.PizzaApi\u00241", "me.ele.hb.ai.hbbehavior.network.a", "me.ele.hb.ai.hbbehavior.network.a\u0024a", "me.ele.hb.ai.hbbehavior.network.b", "me.ele.hb.ai.hbbehavior.network.c", "me.ele.hb.ai.hbbehavior.overspeed.OverSpeedManager\u00241\u00241",
            "me.ele.hb.ai.hbbehavior.overspeed.OverSpeedManager\u00244\u00241", "me.ele.hb.ai.hbbehavior.overspeed.OverSpeedManager\u00244\u00242", "me.ele.hb.ai.hbbehavior.overspeed.OverSpeedManager\u00244\u00243", "me.ele.hb.ai.hbbehavior.overspeed.OverSpeedManager\u00244\u00244", "me.ele.hb.ai.hbbehavior.overspeed.a", "me.ele.hb.ai.hbbehavior.overspeed.b",
            "me.ele.hb.ai.hbbehavior.retrograde.FileModel", "me.ele.hb.ai.hbbehavior.retrograde.RetrogradeManager", "me.ele.hb.ai.hbbehavior.retrograde.RetrogradeManager\u00242", "me.ele.hb.ai.hbbehavior.retrograde.RetrogradeManager\u00242\u00241", "me.ele.hb.ai.hbbehavior.retrograde.RetrogradeManager\u00242\u00242", "me.ele.hb.ai.hbbehavior.retrograde.RetrogradeManager\u00242\u00243",
            "me.ele.hb.ai.hbbehavior.retrograde.RetrogradeManager\u00242\u00243\u00241", "me.ele.hb.ai.hbbehavior.retrograde.RetrogradeManager\u00242\u00243\u00242", "me.ele.hb.ai.hbbehavior.retrograde.RetrogradeManager\u0024a", "me.ele.hb.ai.hbbehavior.retrograde.RetrogradeManager\u0024b", "me.ele.hb.ai.hbbehavior.retrograde.RetrogradeManager\u0024c", "me.ele.hb.ai.hbbehavior.retrograde.RetrogradeManager\u0024d",
            "me.ele.hb.ai.hbbehavior.retrograde.RetrogradeResult", "me.ele.hb.ai.hbbehavior.retrograde.a", "me.ele.hb.ai.hbbehavior.retrograde.a\u0024a", "me.ele.hb.ai.hbbehavior.retrograde.a\u0024b", "me.ele.hb.ai.hbbehavior.retrograde.a\u0024c", "me.ele.hb.ai.hbbehavior.retrograde.a\u0024d",
            "me.ele.hb.ai.hbbehavior.retrograde.a\u0024e", "me.ele.hb.ai.hbbehavior.retrograde.a\u0024f", "me.ele.hb.biz.engine.navi.model.HBNaviEndWaitForGreenLightReason", "me.ele.hb.biz.engine.navi.model.HBNaviGpsSignalStrength", "me.ele.hb.biz.engine.navi.model.HBNaviInfo", "me.ele.hb.biz.engine.navi.model.HBNaviInputLocation",
            "me.ele.hb.biz.engine.navi.model.HBNaviMode", "me.ele.hb.biz.engine.navi.model.HBNaviOutputLocation", "me.ele.hb.biz.engine.navi.model.HBNaviPedalPoint", "me.ele.hb.biz.engine.navi.model.HBNaviPoint", "me.ele.hb.biz.engine.navi.model.HBNaviRoute", "me.ele.hb.biz.engine.navi.model.HBNaviRouteDiffInfo",
            "me.ele.hb.biz.engine.navi.model.HBNaviRoutePlanType", "me.ele.hb.biz.engine.navi.model.HBNaviRoutePoint", "me.ele.hb.biz.engine.navi.model.HBNaviTTSInfo", "me.ele.hb.biz.engine.navi.model.HBNaviTagSegment", "me.ele.hb.biz.engine.navi.model.HBNaviTrafficLightMarkerInfo", "me.ele.hb.biz.engine.navi.model.HBNaviTrafficLightState",
            "me.ele.hb.biz.engine.navi.model.HBNaviWaitForGreenLightNotifyInfo", "me.ele.hb.biz.voiceservice.OverSpeedManager", "me.ele.hb.biz.voiceservice.OverSpeedManager\u00241", "me.ele.hb.biz.voiceservice.OverSpeedManager\u00241\u00241", "me.ele.hb.biz.voiceservice.OverSpeedManager\u00242", "me.ele.hb.biz.voiceservice.OverSpeedManager\u00242\u00241",
            "me.ele.hb.biz.voiceservice.OverSpeedManager\u00243", "me.ele.hb.biz.voiceservice.OverSpeedManager\u00243\u00241", "me.ele.hb.biz.voiceservice.OverSpeedManager\u00246", "me.ele.hb.biz.voiceservice.OverSpeedManager\u00247", "me.ele.hb.biz.voiceservice.OverSpeedManager\u00247\u00241", "me.ele.hb.biz.voiceservice.OverSpeedManager\u00247\u00242",
            "me.ele.hb.biz.voiceservice.OverSpeedManager\u00247\u00243", "me.ele.hb.biz.voiceservice.OverSpeedManager\u00247\u00244", "me.ele.hb.biz.voiceservice.OverSpeedManager\u00247\u00245", "me.ele.hb.biz.voiceservice.OverSpeedManager\u00248", "me.ele.hb.biz.voiceservice.OverSpeedManager\u00248\u00241", "me.ele.hb.biz.voiceservice.OverSpeedManager\u00248\u00242",
            "me.ele.hb.biz.voiceservice.OverSpeedManager\u00248\u00243", "me.ele.hb.biz.voiceservice.OverSpeedManager\u00248\u00244", "me.ele.hb.biz.voiceservice.OverSpeedManager\u00248\u00245", "me.ele.hb.biz.voiceservice.OverSpeedManager\u0024a", "me.ele.hb.biz.voiceservice.OverSpeedManager\u0024b", "me.ele.hb.biz.voiceservice.OverSpeedManager\u0024c",
            "me.ele.hb.biz.voiceservice.a", "me.ele.hb.biz.voiceservice.b", "me.ele.hb.biz.voiceservice.c", "me.ele.hb.biz.voiceservice.c\u0024a", "me.ele.hb.biz.voiceservice.c\u0024b", "me.ele.hb.biz.voiceservice.d",
            "me.ele.hb.biz.voiceservice.d\u0024a", "me.ele.hb.biz.voiceservice.model.OverSpeedConfigModel", "me.ele.hb.biz.voiceservice.model.OverSpeedConfigRequestParam", "me.ele.hb.biz.voiceservice.model.OverSpeedPointModel", "me.ele.hb.biz.voiceservice.model.OverSpeedUploadModel", "me.ele.hb.biz.voiceservice.model.OverSpeedUploadRequestParam",
            "me.ele.hb.biz.voiceservice.model.RiskWay", "me.ele.hb.biz.voiceservice.model.RiskWaysInfo", "me.ele.hb.biz.voiceservice.model.RiskWaysModel", "me.ele.hb.biz.voiceservice.network.PizzaApi", "me.ele.hb.biz.voiceservice.network.PizzaApi\u00241", "me.ele.hb.biz.voiceservice.network.PizzaApi\u0024a",
            "me.ele.hb.biz.voiceservice.network.RiderSafeApi", "me.ele.hb.biz.voiceservice.network.RiderSafeApi\u00241", "me.ele.hb.biz.voiceservice.network.a", "me.ele.hb.biz.voiceservice.network.b", "me.ele.hb.biz.voiceservice.network.c", "me.ele.hb.biz.voiceservice.network.d",
            "me.ele.hb.overspeed.floatwindow.HBFloatWindowManager", "me.ele.hb.overspeed.floatwindow.HBFloatWindowManager\u0024SpeedStatus", "me.ele.hb.overspeed.floatwindow.HBFloatWindowManager\u0024a", "me.ele.hb.overspeed.floatwindow.HBFloatWindowManager\u0024b", "me.ele.hb.overspeed.floatwindow.HBFloatWindowManager\u0024c", "me.ele.hb.overspeed.floatwindow.HBFloatWindowManager\u0024d",
            "me.ele.hb.overspeed.floatwindow.HBFloatWindowManager\u0024e", "me.ele.hb.overspeed.floatwindow.HBFloatWindowManager\u0024f", "me.ele.hb.overspeed.floatwindow.a", "me.ele.hb.overspeed.floatwindow.a\u0024a", "me.ele.hb.overspeed.floatwindow.a\u0024b", "me.ele.hb.trafficsafety.api.IJson",
            "me.ele.hb.trafficsafety.model.BaseConfigModel", "me.ele.hb.trafficsafety.model.FixedSizeList", "me.ele.hb.trafficsafety.model.FusedLocationResultModel", "me.ele.hb.trafficsafety.model.MobilePhonePDRModel", "me.ele.hb.trafficsafety.model.MoveStateChangeResult", "me.ele.hb.trafficsafety.model.OverSpeedConfigDetailModel",
            "me.ele.hb.trafficsafety.model.OverSpeedConfigModel", "me.ele.hb.trafficsafety.model.PointModel", "me.ele.hb.trafficsafety.model.SceneConfig", "me.ele.hb.trafficsafety.model.SceneConfig4MNN", "me.ele.hb.trafficsafety.model.SensorDataModel", "me.ele.hb.trafficsafety.model.UTTrackerInfo",
            "me.ele.hb.trafficsafety.model.UsePhoneConfigModel", "me.ele.hb.trafficsafety.model.UsePhoneRecordModel", "me.ele.qc.v3.model.AppSubLog", "me.ele.qc.v3.model.JudgeRule", "me.ele.qc.v3.model.QcHitInfo", "me.ele.qc.v3.model.QcHitInfo\u0024RenderProp",
            "me.ele.qc.v3.model.QcJudgeModel", "me.ele.qc.v3.model.QcModel", "me.ele.qc.v3.model.QcModel\u0024QcItem", "me.ele.qc.v3.model.QcSubmitInfo", "me.ele.qc.v3.model.QcSubmitResponse", "me.ele.qc.v3.model.QcV3Event"
    )

    private val B: Array<String> = arrayOf(
            "me.ele.hb.ai.punch.DetectRequest", "me.ele.hb.ai.punch.DetectResponse", "me.ele.hb.ai.punch.DetectResponse\u0024Data", "me.ele.hb.ai.punch.a", "me.ele.hb.ai.punch.b", "me.ele.hb.ai.punch.c",
            "me.ele.hb.ai.punch.d", "me.ele.hb.ai.punch.db.DetectDatabase", "me.ele.hb.ai.punch.db.DetectDatabase_Impl", "me.ele.hb.ai.punch.db.DetectDatabase_Impl\u0024a", "me.ele.hb.ai.punch.e", "me.ele.hb.ai.punch.f",
            "me.ele.hb.ai.punch.f\u0024a", "me.ele.hb.ai.punch.f\u0024b", "me.ele.hb.ai.punch.model.LimitedLinkList", "me.ele.hb.ai.punch.model.PointModel", "me.ele.hb.ai.punch.utils.DetectPunchConfigUtils", "me.ele.hb.ai.punch.utils.DetectPunchConfigUtils\u0024CityGray",
            "me.ele.hb.location.v2.WiFiLocationManagerV2", "me.ele.hb.location.v2.WiFiLocationManagerV2\u00241", "me.ele.hb.location.v2.WiFiLocationManagerV2\u00242", "me.ele.hb.location.v2.WiFiLocationManagerV2\u00242\u00241", "me.ele.hb.location.v2.WiFiLocationManagerV2\u00242\u00242", "me.ele.hb.location.v2.WiFiLocationManagerV2\u00242\u00243",
            "me.ele.hb.location.v2.WiFiLocationManagerV2\u00242\u00244", "me.ele.hb.location.v2.WiFiLocationManagerV2\u00242\u00245", "me.ele.hb.location.v2.WiFiLocationManagerV2\u0024Inner", "me.ele.hb.location.v2.alg.AlgParams", "me.ele.hb.location.v2.alg.AlgParams\u00241", "me.ele.hb.location.v2.alg.LocationDetectorV2",
            "me.ele.hb.location.v2.alg.LocationDetectorV2\u00241", "me.ele.hb.location.v2.alg.LocationDetectorV2\u0024InstanceHolder", "me.ele.hb.location.v2.data.Beacon", "me.ele.hb.location.v2.data.DataModelBean", "me.ele.hb.location.v2.data.DataModelInfo", "me.ele.hb.location.v2.data.IOData",
            "me.ele.hb.location.v2.data.TrackingModelMapInfo", "me.ele.hb.location.v2.data.UploadInfo", "me.ele.hb.location.v2.db.dao.WalleCommonDataModelDao", "me.ele.hb.location.v2.db.dao.WalleCommonDataModelDao\u00241", "me.ele.hb.location.v2.db.dao.WalleCommonDataModelDao\u00242", "me.ele.hb.location.v2.db.dao.WalleCommonDataModelDao\u00242\u00241",
            "me.ele.hb.location.v2.db.dao.WalleCommonDataModelDao\u00243", "me.ele.hb.location.v2.db.dao.WalleCommonDataModelDao\u00244", "me.ele.hb.location.v2.db.dao.WalleCommonDataModelDao\u00245", "me.ele.hb.location.v2.db.dao.WalleCommonDataModelDao\u0024Inner", "me.ele.hb.location.v2.db.dbmodel.CommonDataModel", "me.ele.hb.location.v2.model.WiFiAlgModelManager",
            "me.ele.hb.location.v2.model.WiFiDataModelManager", "me.ele.hb.location.v2.model.WiFiDataModelManager\u00241", "me.ele.hb.location.v2.model.WiFiDataModelManager\u0024Inner", "me.ele.hb.location.v2.model.WiFiDataModelV2", "me.ele.hb.location.v2.model.WiFiDataModelV2\u00241", "me.ele.hb.location.v2.model.WiFiDataModelV2\u00242",
            "me.ele.hb.location.v2.model.WiFiDataModelV2\u00242\u00241", "me.ele.hb.location.v2.model.WiFiModelFileV2", "me.ele.hb.location.v2.model.WiFiModelFileV2\u00241", "me.ele.hb.location.v2.model.WiFiModelFileV2\u00241\u00241", "me.ele.hb.location.v2.model.WiFiModelFileV2\u00242", "me.ele.hb.location.v2.model.WiFiModelFileV2\u00242\u00241",
            "me.ele.hb.location.v2.model.WiFiRequestV2", "me.ele.hb.location.v2.model.WiFiRequestV2\u00241", "me.ele.hb.location.v2.net.DataModelApi", "me.ele.hb.location.v2.net.DataModelApi\u00241", "me.ele.hb.location.v2.net.DataModelApi\u00242", "me.ele.hb.location.v2.net.DataModelApi\u00243",
            "me.ele.hb.location.v2.net.DataModelApi\u0024Inner", "me.ele.hb.location.v2.net.DataModelService", "me.ele.hb.location.v2.net.a", "me.ele.hb.location.v2.net.b", "me.ele.hb.location.v2.net.c", "me.ele.hb.location.v2.net.xtop.Result",
            "me.ele.hb.location.v2.net.xtop.XTopParams", "me.ele.hb.location.v2.net.xtop.XTopResult"
    )

    fun install(cl: ClassLoader) {
        val t = Thread({
            var aN = 0; var bN = 0; var aH = 0; var bH = 0
            for (cn in A) {
                try { val c = cl.loadClass(cn); aN++; aH += hookClass(c, "pkgA") } catch (_: Throwable) {}
            }
            for (cn in B) {
                try { val c = cl.loadClass(cn); bN++; bH += hookClass(c, "pkgB") } catch (_: Throwable) {}
            }
            FnLogger.line("[兜底层] A包 " + aN + " 类/" + aH + " 方法 | B包 " + bN + " 类/" + bH + " 方法")
        }, "fn2-pkghook")
        t.isDaemon = true
        t.start()
    }

    private val SKIP = arrayOf(
        "me.ele.hb.ai.hbbehavior.detector.SpeedDetector",
        "me.ele.hb.biz.voiceservice.OverSpeedManager"
    )

    private fun hookClass(c: Class<*>, tag: String): Int {
        val name = c.name
        if (SKIP.any { n -> name == n || name.startsWith(n + "$") }) return 0
        var n = 0
        val flat = tag == "pkgA"
        for (m in c.declaredMethods) {
            val rt = m.returnType
            if (rt != Void.TYPE && !rt.isPrimitive) continue
            if (!flat && rt != Void.TYPE && rt != java.lang.Boolean.TYPE) continue
            try {
                XposedBridge.hookMethod(m, object : XC_MethodHook() {
                    override fun beforeHookedMethod(p: MethodHookParam?) {
                        if (p == null) return
                        HitLog.hit("兜底-" + c.simpleName)
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
                        }
                    }
                })
                n++
            } catch (_: Throwable) {}
        }
        return n
    }
}
