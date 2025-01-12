package org.buildsmali.viewer.ui

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import org.buildsmali.viewer.MyViewModel
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun TextContentScreen(viewModel: MyViewModel = viewModel(),
                      modifier: Modifier = Modifier,
                      onNavigationToDexViewer: () -> Unit) {
    val result by viewModel.extractedResult
    when (result) {
        0 -> Text("正在提取中，请稍候")
        2 -> Text("提取失败！")
        1 -> Text("提取成功！")
    }

}