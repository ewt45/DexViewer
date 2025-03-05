package org.buildsmali.viewer.ui

import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TriStateCheckbox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import org.buildsmali.viewer.MyViewModel
import org.buildsmali.viewer.R
import org.buildsmali.viewer.dex.SmaliPackageData
import org.buildsmali.viewer.utils.findActivity
import org.jf.dexlib2.dexbacked.DexBackedClassDef

/**
 * @param onNavigationToTextViewer 提取dex, jadx反编译，然后跳转到代码显示界面
 */
@Composable
fun HomeScreen(
    viewModel: MyViewModel = viewModel(),
    modifier: Modifier = Modifier,
    onNavigationToTextViewer: () -> Unit
) {
    val context = LocalContext.current.findActivity()
    val vScrollState = rememberScrollState() //滚动
    val hScrollState = rememberScrollState()

    ExportResultDialog(viewModel.exportResult, onNavigationToTextViewer)
    Column(modifier = modifier) {
        //infoText, 刷新按钮
        //TODO 下拉即可刷新 https://developer.android.google.cn/develop/ui/compose/components/pull-to-refresh?hl=zh-cn
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                viewModel.infoText.value, Modifier
                    .padding(8.dp)
                    .weight(1f)
            )

            IconButton(onClick = context::readDexFromApk) {
                Icon(Icons.Filled.Refresh, "重新加载")
            }
            Spacer(Modifier.width(8.dp))
        }

        //标题， 导出dex，反编译按钮
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            //标题
            Text(
                text = "选择并提取",
                modifier = Modifier
                    .padding(8.dp)
                    .weight(1f),
                fontSize = 24.sp
            )

            //FIXME 长按按钮 应该显示文字啊，没这功能？
            //导出选中文件为dex
            IconButton(
                onClick = { context.exportDex() }
            ) { Icon(ImageVector.vectorResource(R.drawable.ic_save), "导出") }

            //查看反编译的smali和java
            IconButton(
                onClick = onNavigationToTextViewer,
            ) { Icon(ImageVector.vectorResource(R.drawable.ic_preview), "查看") }

            Spacer(Modifier.width(8.dp))
        }

        //smali类
        Box(
            Modifier
                .verticalScroll(vScrollState)
//                .horizontalScroll(hScrollState) //为什么加上横向滚动之后，内部Text就没法占满屏幕宽度了呢
                .weight(1f, true)
                .fillMaxWidth()
        ) {
            PackageContent(
                Modifier.padding(8.dp),
                viewModel.checkedClassesMap,
                viewModel.smaliData.value
            )
        }
    }
}

@Composable
fun PackageContent(
    modifier: Modifier = Modifier,
    checkedMap: SnapshotStateMap<DexBackedClassDef, Boolean>,
    pkg: SmaliPackageData
) {
    Column(
        modifier = modifier.fillMaxWidth(),
    ) {
        //子包
        pkg.subPackages.values.forEach { subPkg ->
            var expanded by remember { mutableStateOf(subPkg.fullPkgName == "Ltest/") }
            //包和类的勾选状态，依据map中对应类的boolean 来决定是否勾选。用户操作时，也通过修改map的value来触发重组
            val checked by derivedStateOf {
                when {
                    subPkg.allSubClasses.size == 0 -> ToggleableState.Off

                    subPkg.allSubClasses.all { def ->
                        checkedMap.getOrDefault(def, false)
                    } -> ToggleableState.On

                    subPkg.allSubClasses.none { def ->
                        checkedMap.getOrDefault(def, false)
                    } -> ToggleableState.Off

                    else -> ToggleableState.Indeterminate
                }
            }

            ItemInfo(checked, R.drawable.ic_folder, subPkg.name,
                onCheckBoxClick = {
                    checkedMap.putAll(
                        subPkg.allSubClasses.associateWith { checked != ToggleableState.On })
                },
                onItemClick = {
                    expanded = !expanded
                })

            //显隐时动画效果
            AnimatedVisibility(
                visible = expanded,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                PackageContent(Modifier.padding(start = 16.dp), checkedMap, subPkg)
            }
        }
//        val classesChecked = remember {
//            mutableStateMapOf<String, Boolean>().apply {
//                putAll(pkg.classes.keys.associateWith { false })
//            }
//        }

        //子类
        pkg.classes.forEach { (name, def) ->
            val checked by derivedStateOf {
                if (checkedMap.getOrDefault(def, false))
                    ToggleableState.On
                else
                    ToggleableState.Off
            }

            ItemInfo(checked, R.drawable.ic_class, name,
                onLongClick = mapOf(
                    "勾选此类及其(所属)内部类" to {
                        val nextState = checked != ToggleableState.On
                        //找到外层类名
                        val prefix = name.split("\\$".toRegex())[0]
                        pkg.classes.forEach { (name1, def1) ->
                            // 如果当前选择的是内部类，注意外部类也要被勾选。
                            if (name1 == prefix || name1.startsWith("$prefix\$")) {
                                checkedMap[def1] = nextState
                            }
                        }
                    },
                ),
                onItemClick = {
                    checkedMap[def] = checked != ToggleableState.On
                })
        }
    }
}

/**
 * 显示一条 包或类的名称，图标，及复选框。
 * @param onItemClick 点击这一行中任意元素时的操作，被设置到最外层Row上。复选框操作可能被onCheckBoxClick覆盖。
 * @param onLongClick 长按时的操作，被设置到最外层Row上。map的key是菜单项文字，value是要执行的操作
 * @param onCheckBoxClick 点击复选框时的操作，被设置到Checkbox上。默认为null,即回退到onItemClick
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ItemInfo(
    checkState: ToggleableState,
    @DrawableRes iconId: Int,
    name: String,
    onCheckBoxClick: (() -> Unit)? = null,
    onLongClick: (Map<String, () -> Unit>)? = null,
    onItemClick: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier
            .height(32.dp)
            .fillMaxWidth() // 设置 Row 的高度和宽度
            .combinedClickable(
                onLongClick = {
                    menuExpanded = !onLongClick.isNullOrEmpty() && true
                },
                onClick = onItemClick,
            ),
        verticalAlignment = Alignment.CenterVertically // 设置垂直居中对齐
    ) {
        TriStateCheckbox(
            state = checkState,
            modifier = Modifier.size(32.dp),
            onClick = onCheckBoxClick, //如果不为null,会覆盖掉Row的点击事件。
        )
        Icon(
            painter = painterResource(id = iconId),
            contentDescription = null
        )
        Spacer(Modifier.width(4.dp))
        Text(
            text = name,
            modifier = Modifier
        )

        DropdownMenu(
            expanded = menuExpanded,
            onDismissRequest = { menuExpanded = false },
        ) {
            onLongClick?.forEach { (name, func) ->
                DropdownMenuItem(
                    text = { Text(name) },
                    onClick = {
                        menuExpanded = false
                        func()
                    }
                )
            }

        }
    }
}

@Composable
fun ExportResultDialog(textState: MutableState<String>, onNavigationToTextViewer: () -> Unit) {
    var text by textState
    if (text.isNotEmpty()) {
        AlertDialog(
            onDismissRequest = { text = "" },
            confirmButton = {
                TextButton(onClick = { text = "" }) { Text("确定") }
            },
            dismissButton = {
                TextButton(onClick = {
                    onNavigationToTextViewer()
                    text = ""
                }) { Text("查看反编译代码") }
            },
            text = { Text(text) },
        )
    }
}