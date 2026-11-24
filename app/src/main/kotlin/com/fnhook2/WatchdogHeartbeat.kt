package com.fnhook2

/** 每分钟自检：心跳 + 各层命中快照 + 零命中层（可能无效）清单 */
object WatchdogHeartbeat {

    @Volatile private var started = false

    fun start() {
        if (started) return
        started = true
        val t = Thread({
            var tick = 0
            while (true) {
                try {
                    tick++
                    val snap = HitLog.snapshot()
                    val on = PrefsHolder.ignoreDistanceEnabled
                    val alive = android.os.Process.myPid()
                    if (snap.isNotEmpty()) {
                        val top = snap.entries.sortedByDescending { it.value }.take(8).joinToString(",") { it.key + "=" + it.value }
                        FnLogger.line("[自检] " + tick + " 心跳ok pid=" + alive + " 无视距离=" + (if (on) "开" else "关") + "| Top: " + top)
                    } else {
                        FnLogger.line("[自检] " + tick + " 心跳ok pid=" + alive + " 无视距离=" + (if (on) "开" else "关") + "| 尚无命中")
                    }
                    if (tick % 5 == 0) {
                        val idle = HitLog.idleTags()
                        if (idle.isNotEmpty()) FnLogger.line("[自检-零命中] " + idle.joinToString(","))
                    }
                } catch (t: Throwable) { FnLogger.line("[自检] 异常: " + t.message) }
                try { Thread.sleep(60000) } catch (_: InterruptedException) { break }
            }
        }, "fn2-watchdog")
        t.isDaemon = true
        t.start()
        FnLogger.line("[自检] 看门狗已启动（每60秒一条）")
    }
}
