package com.fnshield

import android.util.Log
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage

/** FnShield 最终版 */
class MainHook : IXposedHookLoadPackage {
    companion object {
        @Volatile var nationMode = false
    }
    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam) {
        if (lpparam.packageName != "me.ele.hbdteam") return
        PrefsHolder.classLoader = lpparam.classLoader
        FnLogger.init()
        FnLogger.line("进程启动: " + lpparam.processName)

        val nation = try { com.fnshield.BuildConfig.NATION_MODE } catch (t: Throwable) { false }
        nationMode = nation
        initPrefs(lpparam)
        if (!nation) {
            try { GpsTrustHooks.install() } catch (t: Throwable) { Log.e("FnShield", "GpsTrust: " + t) }
            try { NormandyHooks.install(lpparam.classLoader) } catch (t: Throwable) { Log.e("FnShield", "Normandy: " + t) }
            try { ModelHooks.install() } catch (t: Throwable) { Log.e("FnShield", "Model: " + t) }
        } else FnLogger.line("整合版模式：定位相关层已停用")
        try { SafetyHooks.install() } catch (t: Throwable) { Log.e("FnShield", "Safety: " + t) }
        try { IgnoreDistanceHooks.install() } catch (t: Throwable) { Log.e("FnShield", "Ignore: " + t) }
        if (nation) { try { ShieldFake.install() } catch (t: Throwable) { Log.e("FnShield", "Fake: " + t) } }
        try { SignatureBypass.install(lpparam.classLoader) } catch (t: Throwable) { Log.e("FnShield", "SigBypass: " + t) }
        try { ShieldProbe.install() } catch (t: Throwable) { Log.e("FnShield", "Probe: " + t) }
        if (!nation) {
            try { PkgHook.install(lpparam.classLoader) } catch (t: Throwable) { Log.e("FnShield", "PkgHook: " + t) }
        } else FnLogger.line("整合版：兜底层停用(不碰地图数据链)")
        try { UpdateBlockerHooks.install() } catch (t: Throwable) { Log.e("FnShield", "Update: " + t) }
        if (!nation) {
            try { LocationObserver2.install(lpparam.classLoader) } catch (t: Throwable) { Log.e("FnShield", "LocObs2: " + t) }
            try { LocationObserver.install(lpparam.classLoader) } catch (t: Throwable) { Log.e("FnShield", "LocObs: " + t) }
        }
        try { AccsSniffer.install(lpparam.classLoader) } catch (t: Throwable) { Log.e("FnShield", "Accs: " + t) }
        try { AnetworkSniffer.install(lpparam.classLoader) } catch (t: Throwable) { Log.e("FnShield", "ANet: " + t) }
        try { CrashLogger.install(lpparam.classLoader) } catch (t: Throwable) { Log.e("FnShield", "Crash: " + t) }
        try { OkHttpSniffer.install(lpparam.classLoader) } catch (t: Throwable) { Log.e("FnShield", "OkHttp: " + t) }
        if (lpparam.processName == "me.ele.hbdteam") WatchdogHeartbeat.start()
        FnLogger.line("全部层挂载完成（最终版）")
    }

    private fun initPrefs(lpparam: XC_LoadPackage.LoadPackageParam) {
        try {
            val atCls = Class.forName("android.app.ActivityThread")
            val app = atCls.getMethod("currentApplication").invoke(null)
            if (app != null) {
                PrefsHolder.init(app as android.content.Context)
                if (nationMode) try { ShieldFloat.show(app) } catch (t: Throwable) { FnLogger.line("[Float] 显示失败: " + t.message) }
                return
            }
        } catch (t: Throwable) { }
        try {
            XposedHelpers.findAndHookMethod("android.app.Application", lpparam.classLoader, "onCreate", object : XC_MethodHook() {
                override fun afterHookedMethod(p: MethodHookParam?) {
                    val a = p?.thisObject as? android.app.Application ?: return
                    try { PrefsHolder.init(a) } catch (t: Throwable) {}
                    if (nationMode) try { ShieldFloat.show(a) } catch (t: Throwable) {}
                }
            })
        } catch (t: Throwable) { }
    }
}
