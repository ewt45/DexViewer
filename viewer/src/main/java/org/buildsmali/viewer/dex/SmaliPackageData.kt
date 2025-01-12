package org.buildsmali.viewer.dex

import org.jf.dexlib2.dexbacked.DexBackedClassDef
import java.util.Stack

/**
 * @param fullPkgName
 * @param name
 */
class SmaliPackageData(
    /**
     * 完整包名 e.g. Lcom/example/
     */
    val fullPkgName: String,

    /**
     * 当前包的名称 e.g. example
     */
    var name: String
) {

    /**
     * 此包中所有直接子包
     */
    val subPackages: LinkedHashMap<String, SmaliPackageData> = LinkedHashMap()
//    val subPackages: LinkedHashMap<String, SmaliPackageData> by lazy {
//        val a = LinkedHashMap<String, SmaliPackageData>()
//        if (fullPkgName == "L") a["aaa"] = SmaliPackageData(fullPkgName + "aaa/", "aaa")
//        a
//    }

    /**
     * 此包中所有直接子类
     */
    val classes: LinkedHashMap<String, DexBackedClassDef> = LinkedHashMap()

    /**
     * 此包下的所有子类（包括子包中的）
     * 因为会在首次调用时生成缓存结果，所以请勿在全部包结构构建完成之前调用此函数
     */
    val allSubClasses: MutableList<DexBackedClassDef> by lazy {
        val allClasses = ArrayList(classes.values)
        val stack = Stack<SmaliPackageData>()
        stack.addAll(subPackages.values)
        while (!stack.empty()) {
            val pkg = stack.pop()
            allClasses.addAll(pkg.classes.values)
            stack.addAll(pkg.subPackages.values)
        }
        allClasses
    }

    /**
     * 给定子包名（e.g. example) ，返回对应的实例。要求子包名必须是此包的直接子包
     */
    fun getSubPackage(subPkgName: String): SmaliPackageData {
        return subPackages.getOrPut(subPkgName) {
            SmaliPackageData(
                "$fullPkgName$subPkgName/",
                subPkgName
            )
        }
    }

    /**
     * 添加一个类到当前包下，要求包名必须等于此包
     * @param name 对应类名 e.g. Example
     */
    fun addClassDef(name: String, d: DexBackedClassDef) {
        classes[name] = d
    }

    /**
     * 给定类名（e.g. Example) ，返回对应的实例。要求此类必须是此包的直接所属类
     */
    fun getClassDef(clsName: String): DexBackedClassDef? {
        return classes[clsName]
    }

    companion object {
        fun newRoot(): SmaliPackageData {
            return SmaliPackageData("L", "");
        }
    }


}
