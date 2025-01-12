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
        cacheDir = ctx.cacheDir
        cacheOutDexFile = File(cacheDir, "cacheOut.dex")
        cacheJadxOutDir = File(cacheDir, "cacheJadxOut")
    }
}