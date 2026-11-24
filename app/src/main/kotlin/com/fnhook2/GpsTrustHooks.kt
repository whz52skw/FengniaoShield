package com.fnhook2

/** GPS 可信度族（对标 location.patch）- 只拦 is should 判定开关，不碰地图数据 getter */
object GpsTrustHooks {
    fun install() {
        val cl = PrefsHolder.classLoader
        val pred: (String) -> Boolean = { mn ->
            mn.contains("Check") || mn.contains("Mock") || mn.contains("Filter") ||
            mn.startsWith("should") || mn == "isGpsTooFar" || mn == "isGpsTrustLevelUseful" ||
            mn == "isOnlyUseGpsLoc"
        }
        // ① GPSCheckUtils 判定类
        SmartHook.hookClass(cl, "me.ele.location.newcustomlocation.GPSCheckUtils", "GPS可信-核心", pred)
        // ② LocationConfig 族：只拦 is should
        SmartHook.hookClass(cl, "me.ele.location.LocationConfig", "GPS可信-配置", pred)
        SmartHook.hookClass(cl, "me.ele.location.LocationConfig\u0024Builder", "GPS可信-配置构建", pred)
        // ③ LocationConfigUtils：只拦 is should
        SmartHook.hookClass(cl, "me.ele.location.utils.LocationConfigUtils", "GPS可信-环境检测", pred)
        // ④ VirtualUtils：只拦 is should
        SmartHook.hookClass(cl, "me.ele.location.utils.virtual.VirtualUtils", "GPS可信-虚拟检测", pred)
        // ⑤ NLP / WiFi 检查：同样收紧
        SmartHook.hookClass(cl, "me.ele.location.newcustomlocation.utils.NLPCheckUtils", "GPS可信-NLP", pred)
        SmartHook.hookClass(cl, "me.ele.talariskernel.location.WifiLocationCheckManager", "GPS可信-WiFi检查", pred)
        FnLogger.line("GpsTrust 层挂载完成（仅判定开关）")
    }
}
