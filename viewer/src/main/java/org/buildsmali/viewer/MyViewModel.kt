package org.buildsmali.viewer

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import jadx.api.JadxArgs
import jadx.api.JadxDecompiler
import jadx.api.impl.NoOpCodeCache
import jadx.api.impl.SimpleCodeWriter
import jadx.api.security.JadxSecurityFlag
import jadx.api.security.impl.JadxSecurity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.buildsmali.viewer.dex.SmaliPackageData
import org.buildsmali.viewer.utils.Consts
import org.jf.dexlib2.DexFileFactory
import org.jf.dexlib2.dexbacked.DexBackedClassDef
import org.jf.dexlib2.dexbacked.DexBackedDexFile
import org.jf.dexlib2.iface.MultiDexContainer
import org.jf.dexlib2.writer.io.FileDataStore
import org.jf.dexlib2.writer.pool.DexPool
import java.io.File
import java.util.function.Function


class MyViewModel : ViewModel() {

    /**
     * 存储apk的dex中的所有包和类
     */
    val smaliData: MutableState<SmaliPackageData> get() = _smaliData
    private val _smaliData = mutableStateOf(SmaliPackageData.newRoot())

    /**
     * 测试文本
     */
    val infoText: MutableState<String> get() = _infoText
    private val _infoText = mutableStateOf("1111")

    /**
     * 用户勾选了的类。用来提取和解析
     */
    val checkedClassesMap get() = _checkedClassesMap
    private val _checkedClassesMap = mutableStateMapOf<DexBackedClassDef, Boolean>()


    /**
     * 提取和反编译结果。0=进行中，1=成功，2=失败
     */
    val extractedResult get() = _extractedResult
    private val _extractedResult = mutableIntStateOf(0)

    lateinit var container: MultiDexContainer<out DexBackedDexFile>

    /**
     * 读取dex,将smali类存入smaliData中
     */
    fun fillSmaliDataFromDex(apkPath: String, smaliData: SmaliPackageData): SmaliPackageData {
        container = DexFileFactory.loadDexContainer(File(apkPath), null)
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

    /**
     * 点击提取按钮后，在后台线程将用户勾选的类合并为dex,存入本地，并使用jadx反编译smali和java
     */
    fun extractAndDecompileClasses() {
        extractedResult.intValue = 0
        viewModelScope.launch {
            //后台线程提取和反编译
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    //提取为dex
                    val dexFile = Consts.cacheOutDexFile
                    val dexPool = DexPool(container.getEntry("classes.dex")!!.dexFile.opcodes)
                    checkedClassesMap.filterValues { it }.keys.forEach { dexPool.internClass(it) }
                    dexPool.writeTo(FileDataStore(Consts.cacheOutDexFile))

                    //反编译(参考jadx安卓示例 https://github.com/jadx-decompiler/jadx-lib-android-example）
                    val outDir = Consts.cacheJadxOutDir

                    val jadxArgs = JadxArgs()
                    jadxArgs.inputFiles = listOf(dexFile)
                    jadxArgs.outDir = outDir

                    jadxArgs.threadsCount = 1 // reduce memory usage
                    jadxArgs.codeCache = NoOpCodeCache.INSTANCE // code cache not needed
                    jadxArgs.codeWriterProvider = Function(::SimpleCodeWriter)
                    // code attributes not needed

                    // disable secure xml parser (some features not supported on Android)
                    val securityFlags = JadxSecurityFlag.all()
                    securityFlags.remove(JadxSecurityFlag.SECURE_XML_PARSER)
                    jadxArgs.security = JadxSecurity(securityFlags)

                    // (Optional) Class set tree loading can take too much time,
                    // but disabling it can reduce result code quality
//                    jadxArgs.isLoadJadxClsSetFile = false

                    JadxDecompiler(jadxArgs).use { jadx ->
                        jadx.load()


                        when (mode) {
                            DecompileMode.MAIN_ACTIVITY -> {
                                // search and return code of MainActivity
                                for (cls in jadx.classes) {
                                    Log.d("SmallApp", "Class: ${cls.name}")
                                    if (cls.name == "MainActivity") {
                                        return cls.code
                                    }
                                }
                            }

                            DecompileMode.SAVE_ALL -> {
                                // decompile and save all files into 'outDir'
                                jadx.save()
                                return "Saved files in $outDir:\n${listFilesInDir(outDir)}"
                            }
                        }
                    }
                }
            }

            //设置运行结果 触发重组
            extractedResult.intValue = if (result.isSuccess) 1 else 2
        }
    }
}