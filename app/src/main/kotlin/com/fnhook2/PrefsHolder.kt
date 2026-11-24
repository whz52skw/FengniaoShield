package com.fnhook2

import android.content.Context
import android.content.SharedPreferences

/** 全局配置 */
object PrefsHolder {
    lateinit var classLoader: ClassLoader
    lateinit var sp: SharedPreferences

    @Volatile private var cachedIgnoreDistance: Boolean? = null

    fun init(ctx: Context) {
        sp = ctx.getSharedPreferences("fnhook2_prefs", Context.MODE_PRIVATE)
        try { cachedIgnoreDistance = sp.getBoolean("ignoreDistanceEnabled", true) } catch (_: Throwable) {}
    }

    fun getIgnore(ctx: Context): Boolean {
        if (!::sp.isInitialized) init(ctx)
        return try { sp.getBoolean("ignoreDistanceEnabled", false) } catch (t: Throwable) { false }
    }

    fun setIgnore(ctx: Context, v: Boolean) {
        if (!::sp.isInitialized) init(ctx)
        try { sp.edit().putBoolean("ignoreDistanceEnabled", v).apply() } catch (_: Throwable) {}
    }

    var ignoreDistanceEnabled: Boolean
        get() = cachedIgnoreDistance ?: true
        set(v) { cachedIgnoreDistance = v; try { sp.edit().putBoolean("ignoreDistanceEnabled", v).apply() } catch (_: Throwable) {} }

    fun applyFromBroadcast(ignore: Boolean) { cachedIgnoreDistance = ignore }

    /** LSP 注入心跳（蜂鸟进程发来）时间戳 */
    fun markAlive(ctx: Context, ts: Long) {
        if (!::sp.isInitialized) init(ctx)
        try { sp.edit().putLong("lsp_alive_ts", ts).apply() } catch (_: Throwable) {}
    }

    fun aliveTs(ctx: Context): Long {
        if (!::sp.isInitialized) init(ctx)
        return try { sp.getLong("lsp_alive_ts", 0L) } catch (t: Throwable) { 0L }
    }
}
