package com.fnhook2

import android.util.Log
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage

/** 蜂鸟盾 最终版 */
class MainHook : IXposedHookLoadPackage {
    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam) {
        if (lpparam.packageName != "me.ele.hbdteam") return
        PrefsHolder.classLoader = lpparam.classLoader
        FnLogger.init()
        FnLogger.line("进程启动: " + lpparam.processName)
        initPrefs(lpparam)

        val nation = try { com.fnhook2.BuildConfig.NATION_MODE } catch (t: Throwable) { false }
        if (!nation) {
            try { IgnoreDistanceHooks.install() } catch (t: Throwable) { Log.e("FN2", "Ignore: " + t) }
            try { GpsTrustHooks.install() } catch (t: Throwable) { Log.e("FN2", "GpsTrust: " + t) }
            try { NormandyHooks.install(lpparam.classLoader) } catch (t: Throwable) { Log.e("FN2", "Normandy: " + t) }
            try { ModelHooks.install() } catch (t: Throwable) { Log.e("FN2", "Model: " + t) }
        } else FnLogger.line("整合版模式：定位相关层已停用")
        try { SafetyHooks.install() } catch (t: Throwable) { Log.e("FN2", "Safety: " + t) }
        try { SignatureBypass.install(lpparam.classLoader) } catch (t: Throwable) { Log.e("FN2", "SigBypass: " + t) }
        if (!nation) {
            try { PkgHook.install(lpparam.classLoader) } catch (t: Throwable) { Log.e("FN2", "PkgHook: " + t) }
        } else FnLogger.line("整合版：兜底层停用(不碰地图数据链)")
        try { UpdateBlockerHooks.install() } catch (t: Throwable) { Log.e("FN2", "Update: " + t) }
        if (!nation) {
            try { LocationObserver2.install(lpparam.classLoader) } catch (t: Throwable) { Log.e("FN2", "LocObs2: " + t) }
            try { LocationObserver.install(lpparam.classLoader) } catch (t: Throwable) { Log.e("FN2", "LocObs: " + t) }
        }
        try { AccsSniffer.install(lpparam.classLoader) } catch (t: Throwable) { Log.e("FN2", "Accs: " + t) }
        try { AnetworkSniffer.install(lpparam.classLoader) } catch (t: Throwable) { Log.e("FN2", "ANet: " + t) }
        try { CrashLogger.install(lpparam.classLoader) } catch (t: Throwable) { Log.e("FN2", "Crash: " + t) }
        try { OkHttpSniffer.install(lpparam.classLoader) } catch (t: Throwable) { Log.e("FN2", "OkHttp: " + t) }
        if (lpparam.processName == "me.ele.hbdteam") WatchdogHeartbeat.start()
        FnLogger.line("全部层挂载完成（最终版）")
    }

    private fun initPrefs(lpparam: XC_LoadPackage.LoadPackageParam) {
        try {
            val atCls = Class.forName("android.app.ActivityThread")
            val app = atCls.getMethod("currentApplication").invoke(null)
            if (app != null) {
                PrefsHolder.init(app as android.content.Context)
                return
            }
        } catch (t: Throwable) { }
        try {
            XposedHelpers.findAndHookMethod("android.app.Application", lpparam.classLoader, "onCreate", object : XC_MethodHook() {
                override fun afterHookedMethod(p: MethodHookParam?) {
                    val a = p?.thisObject as? android.app.Application ?: return
                    try { PrefsHolder.init(a) } catch (t: Throwable) {}
                }
            })
        } catch (t: Throwable) { }
    }
}
