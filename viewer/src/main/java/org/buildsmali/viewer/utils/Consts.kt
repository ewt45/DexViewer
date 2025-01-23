package org.buildsmali.viewer.utils

import android.content.Context
import java.io.File

object Consts {
    lateinit var cacheDir: File
    lateinit var cacheOutDexFile: File
    lateinit var cacheJadxOutDir: File

    /**
     * 初始化一些常量数据
     */
    fun initData(ctx: Context) {
//        cacheDir = ctx.cacheDir
        //TODO 改为内部路径？
        cacheDir = File("/sdcard/Download/Jadx")
        cacheDir.mkdirs()
        cacheOutDexFile = File(cacheDir, "cacheOut.dex")
        cacheJadxOutDir = File(cacheDir, "cacheJadxOut")
        cacheJadxOutDir.mkdirs()
    }
}

enum class CodeType(val value:String) {
    Java("Java"),
    Smali("Smali"),
}