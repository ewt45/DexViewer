package org.buildsmali.viewer.ui

import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TriStateCheckbox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import org.buildsmali.viewer.MyViewModel
import org.buildsmali.viewer.R
import org.buildsmali.viewer.dex.SmaliPackageData

@Composable
fun HomeScreen(viewModel: MyViewModel = viewModel(), modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
    ) {
        Greeting(viewModel.infoText.value)

        PackageContent(viewModel.smaliData.value)
    }
}

@Composable
fun Greeting(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        modifier = modifier
    )
}

@Composable
fun PackageContent(pkg: SmaliPackageData) {
    val isRoot = pkg.fullPkgName == "L"
    val scrollState = if (isRoot) rememberScrollState() else null
    val viewModel: MyViewModel = viewModel()
    Column(
        modifier = Modifier
            .padding(start = if (isRoot) 8.dp else 16.dp)
            //最外层列允许滚动
            .then(if (isRoot) Modifier.verticalScroll(scrollState!!) else Modifier)
    ) {
        //子包
        pkg.subPackages.values.forEach { subPkg ->
            var expanded by remember { mutableStateOf(subPkg.fullPkgName == "Ltest/") }
            //包和类的勾选状态，依据map中对应类的boolean 来决定是否勾选。用户操作时，也通过修改map的value来触发重组
            val checked by derivedStateOf {
                when {
                    subPkg.allSubClasses.all { def ->
                        viewModel.checkedClassesMap.getOrDefault(def, false)
                    } -> ToggleableState.On

                    subPkg.allSubClasses.none { def ->
                        viewModel.checkedClassesMap.getOrDefault(def, false)
                    } -> ToggleableState.Off

                    else -> ToggleableState.Indeterminate
                }
            }

            ItemInfo(checked, R.drawable.ic_folder, subPkg.name,
                onCheckBoxClick = {
                    viewModel.checkedClassesMap.putAll(
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
                PackageContent(subPkg)
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
                if (viewModel.checkedClassesMap.getOrDefault(def, false))
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
                                viewModel.checkedClassesMap[def1] = nextState
                            }
                        }
                    },
                ),
                onItemClick = {
                    viewModel.checkedClassesMap[def] = checked != ToggleableState.On
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