package com.fnhook2

import android.util.Log
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage

/** 蜂鸟盾 最终版（五层+无视距离，无平移） */
class MainHook : IXposedHookLoadPackage {
    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam) {
        if (lpparam.packageName != "me.ele.hbdteam") return
        PrefsHolder.classLoader = lpparam.classLoader
        FnLogger.init()
        FnLogger.line("进程启动: " + lpparam.processName)
        initPrefs(lpparam)

        try { IgnoreDistanceHooks.install() } catch (t: Throwable) { Log.e("FN2", "Ignore: " + t) }
        try { GpsTrustHooks.install() } catch (t: Throwable) { Log.e("FN2", "GpsTrust: " + t) }
        try { NormandyHooks.install(lpparam.classLoader) } catch (t: Throwable) { Log.e("FN2", "Normandy: " + t) }
        try { ModelHooks.install() } catch (t: Throwable) { Log.e("FN2", "Model: " + t) }
        try { SafetyHooks.install() } catch (t: Throwable) { Log.e("FN2", "Safety: " + t) }
        try { PkgHook.install(lpparam.classLoader) } catch (t: Throwable) { Log.e("FN2", "PkgHook: " + t) } catch (t: Throwable) { Log.e("FN2", "Safety: " + t) }
        try { UpdateBlockerHooks.install() } catch (t: Throwable) { Log.e("FN2", "Update: " + t) }
        try { LocationObserver2.install(lpparam.classLoader) } catch (t: Throwable) { Log.e("FN2", "LocObs2: " + t) }
        try { LocationObserver.install(lpparam.classLoader) } catch (t: Throwable) { Log.e("FN2", "LocObs: " + t) }
        try { AccsSniffer.install(lpparam.classLoader) } catch (t: Throwable) { Log.e("FN2", "Accs: " + t) }
        try { AnetworkSniffer.install(lpparam.classLoader) } catch (t: Throwable) { Log.e("FN2", "ANet: " + t) }
        try { CrashLogger.install(lpparam.classLoader) } catch (t: Throwable) { Log.e("FN2", "Crash: " + t) }
        try { OkHttpSniffer.install(lpparam.classLoader) } catch (t: Throwable) { Log.e("FN2", "OkHttp: " + t) }
        FnLogger.line("全部层挂载完成（最终版）")
        if (lpparam.processName == "me.ele.hbdteam") WatchdogHeartbeat.start()
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

    private fun registerCfg(ctx: android.content.Context, pn: String) {
        // 主进程 → 通知模块：悬浮球自动出现 + LSP 注入心跳
        if (pn == "me.ele.hbdteam") {
            try {
                val i = android.content.Intent("com.fnhook2.ALIVE").apply { setPackage("com.fnhook2"); putExtra("ts", System.currentTimeMillis()) }
                ctx.sendBroadcast(i)
                FnLogger.line("[ALIVE] 已发送")}
            catch (t: Throwable) { FnLogger.line("[ALIVE] 发送失败: " + t.message) }
        }
        try {
            val receiver = object : android.content.BroadcastReceiver() {
                override fun onReceive(c: android.content.Context?, i: android.content.Intent?) {
                    val v = i?.getBooleanExtra("ignoreDistanceEnabled", false) ?: return
                    PrefsHolder.applyFromBroadcast(v)
                    FnLogger.line("[配置] 广播更新 无视距离=" + v)
                }
            }
            if (android.os.Build.VERSION.SDK_INT >= 33) ctx.registerReceiver(receiver, android.content.IntentFilter("com.fnhook2.CFG"), 2)
            else ctx.registerReceiver(receiver, android.content.IntentFilter("com.fnhook2.CFG"))
            FnLogger.line("[配置] 广播接收器已注册 无视距离=" + PrefsHolder.ignoreDistanceEnabled)
        } catch (t: Throwable) { FnLogger.line("[配置] 广播注册失败: " + t.message) }
    }
}
