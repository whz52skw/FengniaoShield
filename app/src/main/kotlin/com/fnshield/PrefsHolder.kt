package com.fnshield

import android.content.Context
import android.content.SharedPreferences
import de.robv.android.xposed.XSharedPreferences

/**
 * FnShield 全局配置（跨进程）
 * LSP 版: 模块 UI 进程(com.fnshield)写自身 sp; 目标 app 进程(me.ele.hbdteam) 用 XSharedPreferences 跨进程读
 * 整合版: 浮球与 hook 同进程, 直接读写自身 sp
 */
object PrefsHolder {
    lateinit var classLoader: ClassLoader
    private var sp: SharedPreferences? = null   // 当前进程写入口
    private var xsp: XSharedPreferences? = null // 跨进程读(LSP版)
    @Volatile private var lastReload = 0L

    @Volatile var ignoreDistanceEnabled = false; private set
    @Volatile private var _fakeEnabled = false
    @Volatile private var _fakeLat = 0.0
    @Volatile private var _fakeLng = 0.0
    @Volatile private var _routeSim = false
    @Volatile private var _amapKey = ""

    /** 遵守交规: 默认开启, 无开关 */
    val obeyTraffic: Boolean get() = true

    fun init(ctx: Context) {
        sp = ctx.getSharedPreferences("fnshield_prefs", Context.MODE_PRIVATE)
        try { xsp = XSharedPreferences("com.fnshield", "fnshield_prefs") } catch (_: Throwable) { xsp = null }
        reload()
    }

    fun reload() {
        val src: SharedPreferences? = try {
            xsp?.reload()
            if (xsp != null && xsp?.file?.canRead() == true) xsp else sp
        } catch (_: Throwable) { sp }
        if (src == null) return
        try {
            ignoreDistanceEnabled = src.getBoolean("ignoreDistanceEnabled", false)
            _fakeEnabled = src.getBoolean("fake_enabled", false)
            _fakeLat = src.getString("fake_lat", "0")?.toDouble() ?: 0.0
            _fakeLng = src.getString("fake_lng", "0")?.toDouble() ?: 0.0
            _routeSim = src.getBoolean("route_sim_enabled", false)
            _amapKey = src.getString("amap_key", "") ?: ""
        } catch (_: Throwable) {}
        lastReload = System.currentTimeMillis()
    }

    /** hook 热路径调它: 每 5 秒刷新一次跨进程配置 */
    fun ensureFresh() { if (System.currentTimeMillis() - lastReload > 5000L) reload() }

    fun getFakeEnabled(): Boolean { ensureFresh(); return _fakeEnabled }
    fun getFakeLat(): Double { ensureFresh(); return _fakeLat }
    fun getFakeLng(): Double { ensureFresh(); return _fakeLng }
    fun getRouteSim(): Boolean { ensureFresh(); return _routeSim }
    fun getAmapKey(): String { ensureFresh(); return _amapKey }

    // ===== 写入（模块 UI / 浮球进程） =====
    fun setIgnore(v: Boolean) { ignoreDistanceEnabled = v; sp?.edit()?.putBoolean("ignoreDistanceEnabled", v)?.apply() }
    fun setFakeEnabled(v: Boolean) { _fakeEnabled = v; sp?.edit()?.putBoolean("fake_enabled", v)?.apply() }
    fun setFakeLat(v: Double) { _fakeLat = v; sp?.edit()?.putString("fake_lat", v.toString())?.apply() }
    fun setFakeLng(v: Double) { _fakeLng = v; sp?.edit()?.putString("fake_lng", v.toString())?.apply() }
    fun setRouteSim(v: Boolean) { _routeSim = v; sp?.edit()?.putBoolean("route_sim_enabled", v)?.apply() }
    fun setAmapKey(v: String) { _amapKey = v; sp?.edit()?.putString("amap_key", v)?.apply() }

    // ===== 心跳 =====
    fun markAlive(ctx: Context, ts: Long) { sp?.edit()?.putLong("lsp_alive_ts", ts)?.apply() }
    fun aliveTs(ctx: Context): Long = try { sp?.getLong("lsp_alive_ts", 0L) ?: 0L } catch (_: Throwable) { 0L }
}
