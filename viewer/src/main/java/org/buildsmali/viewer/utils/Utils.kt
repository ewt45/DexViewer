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

    /**
     * 读取dex,将smali类存入smaliData中
     */
    fun fillSmaliDataFromDex(apkPath: String, smaliData: SmaliPackageData): SmaliPackageData {
        val container = DexFileFactory.loadDexContainer(File(apkPath), null)
        return container.dexEntryNames
            .flatMap { name: String -> container.getEntry(name)!!.dexFile!!.classes }
            .sorted()
            .fold(smaliData) { rootPkg, def ->
                //将每个类放到对应包下
                val type = def.type
                var currPkg = rootPkg
                val splits = type.substring(1, type.length - 1).split("/".toRegex())
                splits.forEachIndexed { idx, split ->
                    when (idx) {
                        splits.size - 1 -> currPkg.addClassDef(split, def) // 将类添加到对应包下
                        else -> currPkg = currPkg.getSubPackage(split) // 寻找包名
                    }
                }
                rootPkg
            }
    }
}