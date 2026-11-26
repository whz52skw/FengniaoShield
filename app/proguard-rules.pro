# FnShield 全包 keep（R8 只做 shrink + 代码优化；类名混淆/字符串加密交给 BLA 加固）
-keep class com.fnshield.** { *; }
-keep class com.highcapable.yukihookapi.** { *; }

# Xposed API 是 compileOnly，运行时由 LSPosed 提供
-dontwarn de.robv.android.xposed.**
