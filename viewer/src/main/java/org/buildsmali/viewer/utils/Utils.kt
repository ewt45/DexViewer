package org.buildsmali.viewer.utils

import android.content.Context
import android.content.pm.PackageManager
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




}