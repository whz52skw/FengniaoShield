package com.fnshield

import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge

/** 通用智能拦截器 + 反向开关（对标 location.patch） */
object SmartHook {
    /** 按返回类型拦截：对象返回放行，避免 NPE */
    fun apply(p: XC_MethodHook.MethodHookParam, rt: Class<*>) {
        when {
            rt == Void.TYPE -> p.result = null
            rt == java.lang.Boolean.TYPE -> p.result = false
            rt == Integer.TYPE -> p.result = 0
            rt == java.lang.Long.TYPE -> p.result = 0L
            rt == java.lang.Float.TYPE -> p.result = 0f
            rt == java.lang.Double.TYPE -> p.result = 0.0
            rt == java.lang.Short.TYPE -> p.result = 0.toShort()
            rt == java.lang.Byte.TYPE -> p.result = 0.toByte()
            else -> Unit
        }
    }

    /** 全类拦截（primitive only） */
    fun hookClass(cl: ClassLoader, cn: String, tag: String, pred: (String) -> Boolean = { true }) {
        try {
            val c = cl.loadClass(cn)
            var n = 0
            for (m in c.declaredMethods) {
                if (!pred(m.name)) continue
                val rt = m.returnType
                if (rt != Void.TYPE && !rt.isPrimitive) continue
                XposedBridge.hookMethod(m, object : XC_MethodHook() {
                    override fun beforeHookedMethod(p: MethodHookParam?) {
                        if (p == null) return
                        HitLog.hit(tag)
                        SmartHook.apply(p, rt)
                    }
                })
                n++
            }
            HitLog.register(tag)
            FnLogger.line("✔ " + cn + " 拦截 " + n + " 方法 [" + tag + "]")
        } catch (t: Throwable) { FnLogger.line("✘ " + cn + ": " + t.message) }
    }

    /** boolean 方法统一返回 true（通过/成功类） */
    fun hookTrueMethod(cl: ClassLoader, cn: String, mn: String, tag: String) {
        try {
            val c = cl.loadClass(cn)
            var n = 0
            for (m in c.declaredMethods) {
                if (m.name != mn) continue
                XposedBridge.hookMethod(m, object : XC_MethodHook() {
                    override fun beforeHookedMethod(p: MethodHookParam?) {
                        if (p == null) return
                        HitLog.hit(tag)
                        p.result = true
                    }
                })
                n++
            }
            HitLog.register(tag)
            FnLogger.line("✔ " + cn + "." + mn + " →true [" + tag + "] ×" + n)
        } catch (t: Throwable) { FnLogger.line("✘ " + cn + "." + mn + ": " + t.message) }
    }
}
