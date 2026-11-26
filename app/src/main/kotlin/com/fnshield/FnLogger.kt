package com.fnshield

import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 日志落盘：按当前进程包名分目录
 *   蜂鸟进程  → /sdcard/Android/data/me.ele.hbdteam/files/fnshield_logs/
 *   模块进程  → /sdcard/Android/data/com.fnshield/files/fnshield_logs/
 * （无障碍服务在模块进程，必须分开写，否则跨包无权限）
 */
object FnLogger {

    @Volatile private var dir: File? = null
    @Volatile private var tag = ""

    fun init() {
        try {
            val raw = try { File("/proc/self/cmdline").readText() } catch (t: Throwable) { "" }
            val pkg = raw.substringBefore(":").trim().trimEnd('\u0000')
            val base = if (pkg.contains(".")) pkg else "com.fnshield"
            val d = File("/sdcard/Android/data/" + base + "/files/fnshield_logs")
            d.mkdirs()
            dir = d
            tag = "[" + base + "]"
            write("模块已加载 pid=" + android.os.Process.myPid() + " 目录=" + d.absolutePath)
        } catch (t: Throwable) {
            Log.e("FnShield", "日志初始化失败: " + t)
        }
    }

    private val lock = Any()

    fun write(msg: String) {
        synchronized(lock) {
        try {
            val d = dir ?: return
            val stamp = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
            val time = SimpleDateFormat("HH:mm:ss", Locale.US).format(Date())
            val f = File(d, "fnshield_" + stamp + ".log")
            val line = "[" + time + "] " + msg + "\n"
            FileOutputStream(f, true).use { it.write(line.toByteArray(Charsets.UTF_8)) }
        } catch (t: Throwable) {
            Log.d("FnShield", "日志写盘失败: " + t)
        }
        }
    }
    fun line(msg: String) {
        Log.d("FnShield", msg)
        write(msg)
    }
}
