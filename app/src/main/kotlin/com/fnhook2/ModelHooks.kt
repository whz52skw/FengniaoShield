package com.fnhook2

/** 违规判定模型层（对标 location.patch 的 QC/打卡/逆行/安全判定） */
object ModelHooks {
    fun install() {
        val cl = PrefsHolder.classLoader
        // ① 头盔/工服/口罩 QC 判定模型
        SmartHook.hookClass(cl, "me.ele.qc.v3.model.QcJudgeModel", "合规QC判定")
        // ② 打卡检测请求：isBeaconSuccess → true（通过检测）
        SmartHook.hookTrueMethod(cl, "me.ele.hb.ai.punch.DetectRequest", "isBeaconSuccess", "打卡检测-成功")
        SmartHook.hookClass(cl, "me.ele.hb.ai.punch.DetectRequest", "打卡检测")
        // ③ 逆行规则模型（getMonitorPeriod/Rules 数值归零 = 无逆行判定）
        SmartHook.hookClass(cl, "me.ele.hb.ai.hbbehavior.model.RetrogradeRules", "逆行规则")
        // ④ 阿里安全检测结果：isHit → true（表示已命中/通过，阻断安全处置链）
        SmartHook.hookTrueMethod(cl, "com.alibaba.security.client.smart.core.model.EvalResult", "isHit", "安全检测-命中")
        SmartHook.hookClass(cl, "com.alibaba.security.client.smart.core.model.EvalResult", "安全检测结果")
        // ⑤⑥ 地图模型类摘除（会导致地图空白）：HBMistLocation/HBDyMapCoordinate 不再无脑拦截
        // 替代：仅拦 is 判定方法（两个都拦 boolean）
        SmartHook.hookClass(cl, "me.ele.hb.biz.order.magex.dynamicmap.model.HBMistLocation", "迷雾定位校验", { it.startsWith("is") })
        FnLogger.line("违规模型层挂载完成")
    }
}
