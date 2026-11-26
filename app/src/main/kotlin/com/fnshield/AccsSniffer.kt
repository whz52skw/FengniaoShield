package com.fnshield

import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import java.util.concurrent.ConcurrentHashMap

/**
 * ACCS 数据嗅探（读 Message.data 字段）
 * Message 没有 getData()，直接反射读字段
 */
object AccsSniffer {

    private val seen = ConcurrentHashMap<String, Int>()

    fun install(cl: ClassLoader) {
        try {
            val msgCls = cl.loadClass("com.taobao.accs.data.Message")
            // hook buildData（构造完读 data 字段）
            var n = 0
            for (mn in listOf("buildData", "compressData")) {
                try {
                    XposedBridge.hookAllMethods(msgCls, mn, object : XC_MethodHook() {
                        override fun afterHookedMethod(p: MethodHookParam?) {
                            val msg = p?.thisObject ?: return
                            dumpMessage(msg)
                        }
                    })
                    n++
                } catch (_: Throwable) {}
            }
            FnLogger.line("✔ ACCS buildData/compressData(" + n + ")")

            // hook build(Context,int) 返回值（最终序列化字节）
            try {
                XposedBridge.hookAllMethods(msgCls, "build", object : XC_MethodHook() {
                    override fun afterHookedMethod(p: MethodHookParam?) {
                        val r = p?.result as? ByteArray ?: return
                        if (r.size == 0 || r.size > 20000) return
                        val msg = p?.thisObject
                        val tag = msg?.let { safeField(it, "tag") } ?: ""
                        val sid = msg?.let { safeField(it, "serviceId") } ?: ""
                        val key = "BUILD:" + tag + ":" + sid + ":" + r.size
                        val c = seen.merge(key, 1) { x, _ -> x + 1 } ?: 1
                        if (c == 1) {
                            val preview = String(r, Charsets.UTF_8).replace(Regex("[^\\x20-\\x7e]"), ".").take(200)
                            FnLogger.line("[ACCS] build tag=" + tag + " sid=" + sid + " len=" + r.size + " data=" + preview)
                        }
                    }
                })
                FnLogger.line("✔ ACCS Message.build()")
            } catch (t: Throwable) { FnLogger.line("✘ build: " + t.message) }

        } catch (t: Throwable) { FnLogger.line("✘ ACCS: " + t.message) }

        FnLogger.line("AccsSniffer(数据版) 安装完成")
    }

    private fun safeField(obj: Any, name: String): String {
        return try { (XposedHelpers.getObjectField(obj, name) as? String) ?: "" } catch (t: Throwable) { "" }
    }

    private fun dumpMessage(msg: Any) {
        try {
            val data = XposedHelpers.getObjectField(msg, "data") as? ByteArray ?: return
            if (data.isEmpty()) return
            val tag = safeField(msg, "tag")
            val sid = safeField(msg, "serviceId")
            val type = try { XposedHelpers.getIntField(msg, "type") } catch (t: Throwable) { -1 }
            val key = "DATA:" + tag + ":" + sid + ":" + data.size
            val c = seen.merge(key, 1) { x, _ -> x + 1 } ?: 1
            if (c == 1) {
                val preview = String(data, Charsets.UTF_8).replace(Regex("[^\\x20-\\x7e]"), ".").take(240)
                FnLogger.line("[ACCS] data tag=" + tag + " sid=" + sid + " type=" + type + " len=" + data.size + " => " + preview)
            }
        } catch (t: Throwable) {}
    }
}