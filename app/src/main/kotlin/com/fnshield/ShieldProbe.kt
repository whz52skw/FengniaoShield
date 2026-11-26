package com.fnshield

import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge

/**
 * 高德 key 注入（可选兜底）
 * 默认 amap_key 为空 = 不替换，走「官方 key + 官方签名伪造」通道
 * 配置里填了 key（如原作者的兜底 key）则 hook setApiKey 替换参数
 */
object ShieldProbe {
    fun install() {
        val cl = PrefsHolder.classLoader
        hookApiKey(cl, "com.amap.api.location.AMapLocationClient")
        hookApiKey(cl, "com.amap.api.services.core.ServiceSettings")
        FnLogger.line("[Probe] 高德 key 注入层已挂")
    }

    private fun hookApiKey(cl: ClassLoader, className: String) {
        try {
            val c = cl.loadClass(className)
            for (m in c.declaredMethods) {
                if (m.name != "setApiKey" || m.parameterTypes.size != 1) continue
                XposedBridge.hookMethod(m, object : XC_MethodHook() {
                    override fun beforeHookedMethod(p: MethodHookParam?) {
                        if (p == null) return
                        val k = PrefsHolder.getAmapKey()
                        if (k.isNotEmpty()) {
                            HitLog.hit("高德key注入")
                            p.args[0] = k
                        }
                    }
                })
                FnLogger.line("✔ 高德 key 注入 " + className.substringAfterLast(".") + ".setApiKey")
            }
        } catch (t: Throwable) { FnLogger.line("✘ 高德 key 注入 " + className + ": " + t.message) }
    }
}
