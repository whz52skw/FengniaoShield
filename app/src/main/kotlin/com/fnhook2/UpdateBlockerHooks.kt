package com.fnhook2

/** 更新屏蔽（对标 location.patch 的 UpdateBlockerHooks） */
object UpdateBlockerHooks {
    fun install() {
        val cl = PrefsHolder.classLoader
        // isForceUpgrade → false（永不强制更新）
        SmartHook.hookClass(cl, "me.ele.upgrademanager.AppVersionInfo", "更新-版本信息")
        // 更新 Activity
        SmartHook.hookClass(cl, "me.ele.lpdfoundation.ui.activity.UpdateAppActivity", "更新-页面")
        FnLogger.line("更新屏蔽层挂载完成")
    }
}
