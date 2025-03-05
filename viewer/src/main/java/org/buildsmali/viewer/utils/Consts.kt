package org.buildsmali.viewer.utils

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
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

/**
 * 存储键值对
 */
val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "default")

/**
 * data store的 key, 代表导出dex时的默认文件名称
 */
val DSKEY_SAVE_DEX_NAME = stringPreferencesKey("save_dex_name")