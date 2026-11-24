package com.fnhook2

import java.util.concurrent.ConcurrentHashMap

/** 屏蔽命中统计 + 挂载注册表（区分"已挂但零命中=可能无效"） */
object HitLog {
    private val counts = ConcurrentHashMap<String, Int>()
    private val lastLog = ConcurrentHashMap<String, Long>()
    private val registered = ConcurrentHashMap.newKeySet<String>()

    fun hit(tag: String) {
        counts.merge(tag, 1) { a, _ -> a + 1 }
        val now = System.currentTimeMillis()
        val last = lastLog[tag] ?: 0L
        if (now - last > 60000L) {
            lastLog[tag] = now
            FnLogger.line("[生效] " + tag + " 已拦截(累计" + (counts[tag] ?: 0) + "次)")
        }
    }

    /** 挂载一个层时登记 */
    fun register(tag: String) { registered.add(tag) }

    fun snapshot(): Map<String, Int> = HashMap(counts)

    fun idleTags(): List<String> = registered.filter { !counts.containsKey(it) }.sorted()

    fun registeredCount(): Int = registered.size
}
