package com.fnhook2

import android.util.Base64
import android.util.Log
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge

/** 签名自检绕过：getPackageInfo 查 me.ele.hbdteam 签名时覆盖官方签（相当于 NPatck sigBypass） */
object SignatureBypass {
    private val OFFICIAL_DER: String = "MIIDbTCCAlWgAwIBAgIEG9VGCDANBgkqhkiG9w0BAQsFADBmMQswCQYDVQQGEwI4NjERMA8GA1UECBMIc2hhbmdoYWkxETAPBgNVBAcTCHNoYW5naGFpMQ8wDQYDVQQKEwZlbGUubWUxDzANBgNVBAsTBmVsZS5tZTEPMA0GA1UEAxMGZWxlLm1lMCAXDTE1MDcxMDExMzA1M1oYDzIwNjUwNjI3MTEzMDUzWjBmMQswCQYDVQQGEwI4NjERMA8GA1UECBMIc2hhbmdoYWkxETAPBgNVBAcTCHNoYW5naGFpMQ8wDQYDVQQKEwZlbGUubWUxDzANBgNVBAsTBmVsZS5tZTEPMA0GA1UEAxMGZWxlLm1lMIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAtHSd3HItd4keJOZZULy5IsDr9u/GZNq+Hnit8R0wYFZ8fAVbwYdYhG0PIFCj71oXLLFUbW2IioXkFgZNMH95MtK357Kb/xO2pyvatV1kiNSHd9QkJS3RizhBIZbdVtrfVeStlIg8cAjyJHxr/vSJr6uhDGcj49mvbhYt+uKnyMRxF6IWJPkIomf3/AjlH82DgTvJE4P2fpfbweo4n92O1l9wQANr1O+QoiPBixOtC+gvt89CDR7mv9kTbkrIlDunH4bO3DV1LvrzK5VM6mDMlLEvc+PrKh12ufXT+uEeUJwCHZTzjBOh4N3SgHumr3Y/LdhmlFEy6WEiOa8nhW3d1QIDAQABoyEwHzAdBgNVHQ4EFgQUS/Vim/dCBQVklCGsWHyOiHCp4O0wDQYJKoZIhvcNAQELBQADggEBABdzM4cZxr/whNRkFKhRG/BuESNbppskJwC1wftKmKNk4MmSG5TiHt92lgbRQghKJZdO2Wep8NRNXwvy6w2hkGIPfA5VxlIVzSi9NGNz15VsTOG1Xgm5y+8PWixpfgL77eYSGMfVUXl6Ew6R+DPP72e8IuKgUaNQWazQTMPoZl25HTJjc+2K5lJPIoHMa5Cba/Yaty2a9vl6bLVj4OyXGNShNN6+S7g0k6SQvrR+ynrq7R5CoMKzNvmRWrwgCk8ljIFyhktxXVBFrKN2svk1IGTRl5dBJAVkkLmn/0X7RqSyIB+bBpb0epF/aoVe9urnZ2Q9uuto+MW7s0y4s2Ii/Fk="
    private var officialSig: Any? = null

    fun install(cl: ClassLoader) {
        try {
            officialSig = cl.loadClass("android.content.pm.Signature")
                .getConstructor(ByteArray::class.java)
                .newInstance(Base64.decode(OFFICIAL_DER, Base64.DEFAULT))
        } catch (t: Throwable) { FnLogger.line("[Sig] 官方签名对象创建失败: " + t.message) ; return }
        try {
            val pm = cl.loadClass("android.app.ApplicationPackageManager")
            for (mn in listOf("getPackageInfo", "getPackageArchiveInfo")) {
                for (m in pm.declaredMethods) {
                    if (m.name != mn || m.parameterTypes.size != 2) continue
                    XposedBridge.hookMethod(m, object : XC_MethodHook() {
                        override fun afterHookedMethod(p: MethodHookParam?) {
                            if (p == null) return
                            try {
                                val pkg = p.args.getOrNull(0) ?: return
                                if (pkg.toString() != "me.ele.hbdteam") return
                                val flags = p.args.getOrNull(1)?.toString() ?: ""
                                val info = p.result ?: return
                                val f = info.javaClass.getDeclaredField("signatures")
                                f.isAccessible = true
                                val arr = java.lang.reflect.Array.newInstance(cl.loadClass("android.content.pm.Signature"), 1)
                                java.lang.reflect.Array.set(arr, 0, officialSig)
                                f.set(info, arr)
                                FnLogger.line("[Sig] 签名已覆盖 flags=" + flags)
                            } catch (t: Throwable) {
                                FnLogger.line("[Sig] 覆盖失败: " + t.message)
                            }
                        }
                    })
                }
            }
            FnLogger.line("[Sig] 签名绕过已挂")
        } catch (t: Throwable) { FnLogger.line("[Sig] 挂载失败: " + t.message) }
    }
}
