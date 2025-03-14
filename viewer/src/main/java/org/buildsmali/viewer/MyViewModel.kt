package org.buildsmali.viewer

import android.util.Log
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import jadx.api.JadxArgs
import jadx.api.JadxDecompiler
import jadx.api.JavaClass
import jadx.api.impl.NoOpCodeCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.apache.commons.io.file.PathUtils
import org.buildsmali.viewer.dex.SmaliPackageData
import org.buildsmali.viewer.utils.CodeType
import org.buildsmali.viewer.utils.Consts
import org.jf.dexlib2.DexFileFactory
import org.jf.dexlib2.dexbacked.DexBackedClassDef
import org.jf.dexlib2.dexbacked.DexBackedDexFile
import org.jf.dexlib2.iface.MultiDexContainer
import org.jf.dexlib2.writer.io.FileDataStore
import org.jf.dexlib2.writer.pool.DexPool
import java.io.File
import kotlin.io.path.createDirectories


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
    private val _infoText = mutableStateOf("stub")

    /**
     * 用户勾选了的类。用来提取和解析
     */
    val checkedClassesMap get() = _checkedClassesMap
    private val _checkedClassesMap = mutableStateMapOf<DexBackedClassDef, Boolean>()


    /**
     * 提取和反编译结果。0=进行中，1=成功，2=失败
     */
    val decompiledResult get() = _decompiledResult
    private val _decompiledResult = mutableIntStateOf(0)

    /**
     * apk输入
     */
    private lateinit var container: MultiDexContainer<out DexBackedDexFile>

    /**
     * jadx解析后的类信息
     */
    val javaClasses  get() = _javaClasses
    private val _javaClasses = mutableStateListOf<JavaClass>()

    /**
     * 显示的代码是JAVA还是SMALI.
     */
    val displayCodeType get() = _displayCodeType
    private val _displayCodeType = mutableStateOf(CodeType.Java)

    /**
     * 导出dex后的对话框内容
     */
    val exportResult = mutableStateOf("")

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
     * 将选中的smali类提取为dex, 存入 [Consts.cacheOutDexFile]
     */
    suspend fun extractClassToDex() = withContext(Dispatchers.IO) {
        val dexPool = DexPool(container.getEntry("classes.dex")!!.dexFile.opcodes)
        checkedClassesMap.filterValues { it }.keys.forEach { dexPool.internClass(it) }
        dexPool.writeTo(FileDataStore(Consts.cacheOutDexFile))
    }

    /**
     * 从 [Consts.cacheOutDexFile] 读取dex,使用jadx反编译获取smali和jadx代码。
     * 结果存入 [javaClasses] 中
     *
     * 反编译(参考jadx安卓示例 https://github.com/jadx-decompiler/jadx-lib-android-example）
     */
    private suspend fun decompileWithJadx() = withContext(Dispatchers.IO) {
        val dexFile = Consts.cacheOutDexFile
        val outDir = Consts.cacheJadxOutDir

        val jadxArgs = JadxArgs()
        jadxArgs.inputFiles = listOf(dexFile)
        jadxArgs.outDir = outDir

        //清除缓存？
        val jadxCacheDir = jadxArgs.filesGetter.cacheDir
        PathUtils.deleteDirectory(jadxCacheDir)
        jadxCacheDir.createDirectories()

        jadxArgs.renameFlags.clear() //禁止重命名
        jadxArgs.isRespectBytecodeAccModifiers = true //防止protect等变public
        jadxArgs.threadsCount = 1 // reduce memory usage
        jadxArgs.codeCache = NoOpCodeCache.INSTANCE // code cache not needed
//                    jadxArgs.codeWriterProvider = Function(::SimpleCodeWriter)  // code attributes not needed

        // disable secure xml parser (some features not supported on Android)
//                    val securityFlags = JadxSecurityFlag.all()
//                    securityFlags.remove(JadxSecurityFlag.SECURE_XML_PARSER)
//                    jadxArgs.security = JadxSecurity(securityFlags)

        // (Optional) Class set tree loading can take too much time,
        // but disabling it can reduce result code quality
//                    jadxArgs.isLoadJadxClsSetFile = false

        JadxDecompiler(jadxArgs).use { jadx ->
            jadx.load()

            // decompile and save all files into 'outDir'
            jadx.save()

            //为什么保存本地文件正常，直接从java获取为null？
//                        if (jadx.classes.isNotEmpty())

            javaClasses.addAll(jadx.classes)
            Log.d("aaa", "extractAndDecompileClasses: 解析出java类："+jadx.classes.map { it.name } )

        }
    }

    /**
     * 点击提取按钮后，在后台线程将用户勾选的类合并为dex,存入本地，并使用jadx反编译smali和java
     */
    fun extractAndDecompileClasses() {
        decompiledResult.intValue = 0
        javaClasses.clear()

        viewModelScope.launch {
            //后台线程提取和反编译
            val result =runCatching {
                // 提取为dex 存入缓存文件
                extractClassToDex()
                // 读取缓存文件 使用jadx反编译 存入javaClasses
                decompileWithJadx()
            }

            //设置运行结果 触发重组
            decompiledResult.intValue = if (result.isSuccess) 1 else 2
            if (result.isFailure)
                Log.d("aaa", "extractAndDecompileClasses: "+result.exceptionOrNull())
        }
    }

    /**
     * 读取dex前，清空上一次的数据存储
     */
    fun reset() {
        _infoText.value = "stub"
        _checkedClassesMap.clear()
        _decompiledResult.intValue = 0
        _javaClasses.clear()
    }
}