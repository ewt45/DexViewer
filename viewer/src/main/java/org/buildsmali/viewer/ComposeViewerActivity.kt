package org.buildsmali.viewer

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.widget.Space
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import org.buildsmali.viewer.dex.SmaliPackageData
import org.buildsmali.viewer.ui.HomeScreen
import org.buildsmali.viewer.ui.theme.BuildSmaliTestTheme
import org.buildsmali.viewer.utils.Utils
import org.buildsmali.viewer.utils.Utils.getProviderApkPath

class ComposeViewerActivity : ComponentActivity() {
    private val model: MyViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            BuildSmaliTestTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    HomeScreen(
                        modifier = Modifier.padding(innerPadding)
                    )
                }
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
        val providerPkg = "org.buildsmali.provider"
        val apkPath = getProviderApkPath(this, providerPkg)
        if (apkPath.isEmpty()) {
//            findViewById<TextView>(R.id.text).text = "未找到待解析的apk: $providerPkg"
            model.infoText.value = "未找到待解析的apk: $providerPkg"
            return
        }

        model.infoText.value = "find the apk $providerPkg"

        //更新了smaliData后 会自动出发重组，重组时读取新的smaliData
        val filledSmaliData = SmaliPackageData.newRoot()
        Utils.fillSmaliDataFromDex(apkPath, filledSmaliData)
        model.smaliData.value = filledSmaliData
    }
}



