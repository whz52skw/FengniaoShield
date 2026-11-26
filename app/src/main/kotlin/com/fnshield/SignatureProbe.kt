package com.fnshield

import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers

/** 签名自检探针：hook PackageManager.getPackageInfo/getPackageArchiveInfo 记录调用方 */
object SignatureProbe {
    private val logged = java.util.concurrent.ConcurrentHashMap.newKeySet<String>()

    fun install(cl: ClassLoader) {
        try {
            val pm = cl.loadClass("android.app.ApplicationPackageManager")
            if (pm == null) return
            for (mn in listOf("getPackageInfo", "getPackageArchiveInfo")) {
                try {
                    val ms = pm.declaredMethods.filter { it.name == mn }
                    for (m in ms) {
                        if (m.parameterTypes.size != 2) continue
                        XposedBridge.hookMethod(m, object : XC_MethodHook() {
                            override fun beforeHookedMethod(p: MethodHookParam?) {
                                if (p == null) return
                                val pkg = p.args.getOrNull(0) ?: return
                                val flags = p.args.getOrNull(1)
                                val st = Thread.currentThread().stackTrace.drop(3).take(6)
                                    .joinToString("|") { it.className.substringAfterLast(".") + "." + it.methodName }
                                if (flags.toString().contains("64") || flags.toString().contains("134217728") || flags.toString().contains("1")) {
                                    val key = p.method.name + "|" + st
                                    if (logged.add(key)) FnLogger.line("[PM] " + mn + "(flags=" + flags + ") <= " + st)
                                }
                            }
                        })
                    }
                } catch (_: Throwable) {}
            }
            FnLogger.line("[探针] PM 签名查询探针已挂")
        } catch (t: Throwable) { FnLogger.line("[探针] 失败: " + t.message) }
    }
}
