package com.fnhook2

import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedHelpers

/** 全问题日志：崩溃/自杀/杀后台重启/生命周期 */
object CrashLogger {

    private var installed = false

    fun install(cl: ClassLoader) {
        installCrashHandler()
        if (installed) return
        try {
            // 进程自杀点记录（杀后台后重启可对时间线）
            XposedHelpers.findAndHookMethod("android.os.Process", cl, "killProcess", Int::class.javaPrimitiveType, object : XC_MethodHook() {
                override fun beforeHookedMethod(p: MethodHookParam?) {
                    FnLogger.line("[自杀] Process.killProcess(" + p?.args?.getOrNull(0) + ") " + stack(3))
                }
            })
            XposedHelpers.findAndHookMethod("java.lang.System", cl, "exit", Int::class.javaPrimitiveType, object : XC_MethodHook() {
                override fun beforeHookedMethod(p: MethodHookParam?) {
                    FnLogger.line("[自杀] System.exit(" + p?.args?.getOrNull(0) + ") " + stack(3))
                }
            })
            FnLogger.line("[CrashLogger] 自杀点已挂")
        } catch (t: Throwable) { FnLogger.line("[CrashLogger] 自杀点挂载失败: " + t.message) }

        try {
            XposedHelpers.findAndHookMethod("android.app.Application", cl, "onLowMemory", object : XC_MethodHook() {
                override fun afterHookedMethod(p: MethodHookParam?) { FnLogger.line("[内存] onLowMemory（可能即将被杀）") }
            })
        } catch (t: Throwable) { FnLogger.line("[CrashLogger] onLowMemory: " + t.message) }

        installed = true
    }

    /** UncaughtExceptionHandler：捕获所有未处理崩溃 */
    fun installCrashHandler() {
        val old = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { t, e ->
            try {
                FnLogger.line("=================================================================")
                FnLogger.line("[崩溃] 线程=" + t.name + " 时间=" + java.util.Date())
                FnLogger.line("[崩溃] 异常: " + e.javaClass.name + ": " + e.message)
                for (s in e.stackTrace.take(40)) FnLogger.line("[崩溃]   at " + s.toString())
                var c = e.cause; var depth = 0
                while (c != null && depth < 5) {
                    FnLogger.line("[崩溃] Caused by: " + c.javaClass.name + ": " + c.message)
                    for (s in c.stackTrace.take(15)) FnLogger.line("[崩溃]     at " + s.toString())
                    c = c.cause; depth++
                }
                FnLogger.line("=================================================================")
            } catch (_: Throwable) {}
            if (old != null) old.uncaughtException(t, e)
            else android.os.Process.killProcess(android.os.Process.myPid())
        }
        FnLogger.line("[CrashLogger] 崩溃捕获已安装")
    }

    private fun stack(depth: Int): String {
        return try {
            Thread.currentThread().stackTrace.drop(4).take(depth).joinToString("|") { it.className.substringAfterLast(".") + "." + it.methodName }
        } catch (t: Throwable) { "?" }
    }
}
