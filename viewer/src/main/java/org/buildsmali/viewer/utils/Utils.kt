package org.buildsmali.viewer.utils

import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import jadx.api.JavaClass
import jadx.api.impl.InMemoryCodeCache
import org.buildsmali.viewer.dex.SmaliPackageData
import org.jf.dexlib2.DexFileFactory
import org.jf.dexlib2.dexbacked.DexBackedClassDef
import java.io.File

object Utils {
    /**
     * 获取 provider的apk的本地路径。
     *
     * @return 返回apk本地路径。找不到时返回空字符串
     */
    fun getProviderApkPath(ctx: Context, pkg: String): String {
        try {
            //需要在manifest指定对应包名的query,才能获取到info
            return ctx.packageManager.getApplicationInfo(pkg, 0).sourceDir
        } catch (e: PackageManager.NameNotFoundException) {
            e.printStackTrace()
            return "" // 如果包名不存在
        }
    }

    //
    //
    //
    /**
     * 为啥codecache是null呢？？？导致抛出异常了
     * 在viewModel里新建args时,生成decompiler时, .load()时的args都是同一个。但是compose时args就变了，codecache为null
     * 而且只用设置一次，之后再次新建args和decompiler,这里的codecache也不为null
     */
    fun fixCodeCacheIsNull(javaClass:JavaClass) {
        val jadxArgs = javaClass.classNode.root().args
        if (jadxArgs.codeCache == null) {
            jadxArgs.codeCache = InMemoryCodeCache()
            Log.w("aaa", "TextContentScreen: codeCache为null,设为InMemoryCodeCache")
        }
    }


}