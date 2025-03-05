package org.buildsmali.viewer

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.datastore.preferences.core.edit
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.buildsmali.viewer.dex.SmaliPackageData
import org.buildsmali.viewer.ui.MyApp
import org.buildsmali.viewer.ui.theme.BuildSmaliTestTheme
import org.buildsmali.viewer.utils.Consts
import org.buildsmali.viewer.utils.DSKEY_SAVE_DEX_NAME
import org.buildsmali.viewer.utils.Utils
import org.buildsmali.viewer.utils.Utils.getProviderApkPath
import org.buildsmali.viewer.utils.dataStore

class ComposeViewerActivity : ComponentActivity() {
    private val viewModel: MyViewModel by viewModels()

    /**
     * 一个launcher,用于导出dex时发起intent，选择文件存储位置
     */
    private lateinit var exportDexLauncher: ActivityResultLauncher<String>

    /**
     * 是否已经读取了要解析的apk
     */
    private var providerLoaded = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        Consts.initData(this)

        setContent {
            BuildSmaliTestTheme {
                MyApp(viewModel = viewModel)
            }
        }

        exportDexLauncher = registerExportDexLauncher()

        if (checkAndRequestPermissionIfNeeded()) {
            readDexFromApk()
        }


    }

    override fun onResume() {
        super.onResume()

        if (!checkAndRequestPermissionIfNeeded()) {
            return
        }

        //TODO 如果此时还没解析apk,就解析。否则检查当前apk是否有变化
        if (!providerLoaded) {
            readDexFromApk()
        }
    }

    /**
     * 检查是否需要申请存储权限，有必要时申请。
     * @return 拥有权限时为true,没有权限时为false
     */
    private fun checkAndRequestPermissionIfNeeded(): Boolean {
        if (Environment.isExternalStorageManager())
            return true;
        startActivity(
            Intent(
                Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                Uri.parse("package:$packageName")
            )
        )
        return false;
    }


    /**
     * 读取apk中的dex,并将smali包和类显示出来。
     *
     * 此函数会将[providerLoaded]置为true
     */
    fun readDexFromApk() {
        //先重置数据
        viewModel.reset()

        val providerPkg = "org.buildsmali.provider"
        val apkPath = getProviderApkPath(this, providerPkg)
        if (apkPath.isEmpty()) {
//            findViewById<TextView>(R.id.text).text = "未找到待解析的apk: $providerPkg"
            viewModel.infoText.value = "寻找包名为 $providerPkg 的apk：未找到"
            return
        }

        viewModel.infoText.value = "寻找包名为 $providerPkg 的apk：已找到"

        //更新了smaliData后 会自动出发重组，重组时读取新的smaliData
        viewModel.smaliData.value =
            SmaliPackageData.newRoot().apply { viewModel.fillSmaliDataFromDex(apkPath, this) }

        providerLoaded = true
    }

    /**
     * 点击导出按钮后，启动file picker, 选择dex存储位置，然后导出dex。
     */
    fun exportDex() {
        lifecycleScope.launch {
            exportDexLauncher.launch(dataStore.data.map { it[DSKEY_SAVE_DEX_NAME] ?: "" }.first())
        }
    }

    /**
     * 注册一个launcher,用于导出dex时发起intent，选择文件存储位置
     */
    private fun registerExportDexLauncher() = registerForActivityResult(
        ActivityResultContracts.CreateDocument("application/octet-stream")
    ) { uri ->
        if (uri == null) {
            return@registerForActivityResult
        }

        lifecycleScope.launch {
            val result = runCatching {
                viewModel.extractClassToDex() // 生成缓存dex
                val activity = this@ComposeViewerActivity
                Utils.copyCachedOutDexToTarget(activity, uri) //复制缓存文件到指定路径
                //本次名称存起来 作为下一次默认名称
                DocumentFile.fromSingleUri(activity, uri)?.name?.let { name ->
                    activity.dataStore.edit { it[DSKEY_SAVE_DEX_NAME] = name }
                }
            }
            // 完成时显示对话框
            viewModel.exportResult.value = if (result.isSuccess) "导出成功!" else "导出失败!"
        }
    }


}



