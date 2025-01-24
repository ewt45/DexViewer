package org.buildsmali.viewer

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import org.buildsmali.viewer.dex.SmaliPackageData
import org.buildsmali.viewer.ui.MyApp
import org.buildsmali.viewer.ui.theme.BuildSmaliTestTheme
import org.buildsmali.viewer.utils.Consts
import org.buildsmali.viewer.utils.Utils.getProviderApkPath

class ComposeViewerActivity : ComponentActivity() {
    private val model: MyViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        Consts.initData(this)

        setContent {
            BuildSmaliTestTheme {
                MyApp(viewModel = model)
            }
        }
    }

    override fun onResume() {
        super.onResume()

        if (!Environment.isExternalStorageManager()) {
            startActivity(
                Intent(
                    Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                    Uri.parse("package:$packageName")
                )
            )
            return
        }

        //TODO 每次进入页面都会刷新。以后也可以改成手动刷新？
        model.reset()

        val providerPkg = "org.buildsmali.provider"
        val apkPath = getProviderApkPath(this, providerPkg)
        if (apkPath.isEmpty()) {
//            findViewById<TextView>(R.id.text).text = "未找到待解析的apk: $providerPkg"
            model.infoText.value = "未找到待解析的apk: $providerPkg"
            return
        }

        model.infoText.value = "find the apk $providerPkg"

        //更新了smaliData后 会自动出发重组，重组时读取新的smaliData
        model.smaliData.value =
            SmaliPackageData.newRoot().apply { model.fillSmaliDataFromDex(apkPath, this) }
    }
}



