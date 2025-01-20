package org.buildsmali.viewer.ui

import android.util.Log
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.draggable2D
import androidx.compose.foundation.gestures.rememberDraggable2DState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import jadx.api.JavaClass
import org.buildsmali.viewer.MyViewModel
import org.buildsmali.viewer.R
import org.buildsmali.viewer.utils.Consts
import org.buildsmali.viewer.utils.Utils
import kotlin.math.min
import kotlin.math.roundToInt

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TextContentScreen(
    viewModel: MyViewModel = viewModel(),
    modifier: Modifier = Modifier.fillMaxSize(),
    onNavigationToDexViewer: () -> Unit
) {
    Column(
        modifier = modifier
    ) {
        val result by viewModel.extractedResult
        var codeType by viewModel.displayCodeType
        when (result) {
            0 -> Text("正在提取中，请稍候")
            2 -> Text("提取失败！")
//            1 -> Text("提取成功！")
        }

        if (result == 1 && viewModel.javaClasses.isNotEmpty()) {
            val displayCls = remember { mutableStateOf(viewModel.javaClasses[0]) }
            Utils.fixCodeCacheIsNull(displayCls.value)

            Toolbar(
                viewModel.javaClasses,
                displayCls,
                onCodeTypeSwitch = { codeType = it }
            )

            val textVScrollState = rememberScrollState()
            val textHScrollState = rememberScrollState()


            //FIXME 这样不行，textSize是屏幕大小不是内容文本大小, minOffset是错的
            var textSize by remember { mutableStateOf(IntSize.Zero) }
            var textParentSize by remember { mutableStateOf(IntSize.Zero) }
            val textPadding = 16.dp
            var offsetX by remember { mutableFloatStateOf(0f) }
            var offsetY by remember { mutableFloatStateOf(0f) }

            val minOffsetX = min(0, textParentSize.width - textSize.width)
            val minOffsetY = min(0, textParentSize.height - textSize.height)
            LaunchedEffect(key1 = offsetX, key2 = offsetY) {
//                textHScrollState.scrollTo(-offsetX.toInt())
//                textVScrollState.scrollTo(-offsetY.toInt())
            }

            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                ),
                modifier = Modifier
                    .pointerInput(Unit) {
                        detectDragGestures { change, dragAmount ->
                            change.consume()
//                            Log.d("aaa", "TextContentScreen: 为啥不动了？ " +
//                                    "offsetX=${(offsetX + dragAmount.x)}, offsetY=${(offsetY + dragAmount.y)}, " +
//                                    "coerceIn ${minOffsetX.toFloat()}, ${minOffsetY.toFloat()}")
                            offsetX = (offsetX + dragAmount.x)
                            offsetY = (offsetY + dragAmount.y)
                        }
                    }
                    .onGloballyPositioned { textParentSize = it.size }
            ) {
                Text(
                    text = when (codeType) {
                        Consts.strJava -> displayCls.value.code
                        Consts.strSmali -> displayCls.value.smali
                        else -> displayCls.value.code
                    },
                    fontFamily = FontFamily.Monospace,
                    softWrap = false,
                    modifier = Modifier
                        .padding(textPadding)
                        .offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) }
                        .wrapContentSize(Alignment.TopStart, true) //允许显示出界
                        .onGloballyPositioned { textSize = it.size }
//                        .background(Color.Blue)
//                    .verticalScroll(textVScrollState, false)
//                    .horizontalScroll(textHScrollState, false)
//                        .pointerInput(Unit) {
//                            detectDragGestures { change, dragAmount ->
//                                change.consume()
//                                offsetX += dragAmount.x
//                                offsetY += dragAmount.y
//                            }
//                        }
//                        .draggable2D(
//                            state = rememberDraggable2DState { delta ->
//                                val newValueX = offsetX + delta. x
//                                val newValueY = offsetY + delta. y
//                                offsetX = newValueX//. coerceIn(minPx, maxPx)
//                                offsetY = newValueY//. coerceIn(minPx, maxPx)
//                            }
//                        )
//                    .draggable( // 使用 draggable 修饰符
//                        orientation = Orientation.Vertical, // 限制为水平拖拽
//                        state = rememberDraggableState { delta ->
//                            offsetX += delta
//                        }
//                    )
//                    .draggable( // 使用 draggable 修饰符
//                        orientation = Orientation.Vertical, // 限制为垂直拖拽
//                        state = rememberDraggableState { delta ->
//                            offsetY += delta
//                        }
//                    )
                )
            }
        }
    }
}


@Composable
fun Toolbar(
    classes: List<JavaClass>, mutableDisplayCls: MutableState<JavaClass>,
    onCodeTypeSwitch: (String) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        JavaClassSelect(modifier = Modifier.padding(8.dp), classes, mutableDisplayCls)
        JavaSmaliSwitch(
            modifier = Modifier.padding(8.dp),
            options = listOf(
                Consts.strJava to { onCodeTypeSwitch(Consts.strJava) },
                Consts.strSmali to { onCodeTypeSwitch(Consts.strSmali) },
            ),
        )
    }

}

@Composable
fun JavaClassNameText(name: String, pkg: String) {
//    LocalContentColor.current
//    MaterialTheme.colorScheme.onSurface
    Column {
        Spacer(modifier = Modifier.height(4.dp))
        Text(name, maxLines = 1, style = TextStyle(fontSize = 20.sp))
        Text(
            pkg, maxLines = 1, style = TextStyle(fontSize = 14.sp),
            color = LocalContentColor.current.copy(alpha = 0.53f)
        )
    }
}

@Composable
fun JavaSmaliSwitch(modifier: Modifier = Modifier, options: List<Pair<String, () -> Unit>>) {
    var selectIdx by remember { mutableIntStateOf(0) }
    OutlinedButton(
        modifier = modifier,
        onClick = {
            selectIdx = (selectIdx + 1) % options.size
            options[selectIdx].second()
        },
        shape = RoundedCornerShape(8.dp),
        contentPadding = PaddingValues(8.dp, 8.dp, 8.dp, 8.dp)
    ) {
        AnimatedContent(
            targetState = options[selectIdx].first,
            transitionSpec = {
                slideInVertically { fullHeight -> fullHeight } + fadeIn() togetherWith
                        slideOutVertically () + fadeOut() using
                        SizeTransform(clip = false)
            }
        ) { targetText ->
            Text(text = targetText)
        }
        Spacer(Modifier.width(8.dp))
        Icon(
            imageVector = ImageVector.vectorResource(R.drawable.swap_vert),
            contentDescription = "切换",
            Modifier.size(20.dp)
        )
    }
}

@Composable
fun JavaClassSelect(
    modifier: Modifier = Modifier,
    classes: List<JavaClass>,
    mutableDisplayCls: MutableState<JavaClass>
) {
    var expanded by remember { mutableStateOf(false) }
    var displayCls by mutableDisplayCls
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TextButton(
            onClick = { expanded = !expanded },
//            shape = RoundedCornerShape(8.dp),
            contentPadding = PaddingValues(8.dp, 8.dp, 8.dp, 8.dp)
        ) {
            JavaClassNameText("testCls", "test")
            Spacer(Modifier.width(8.dp))
            Icon(
                imageVector = Icons.Filled.ArrowDropDown,
                contentDescription = "下拉"
            )
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            classes.forEach { cls ->
                DropdownMenuItem(
                    text = { JavaClassNameText(cls.name, cls.`package`) },
                    onClick = {
                        displayCls = cls
                        expanded = false
                    }
                )
            }
        }
    }
}

@Preview
@Composable
fun prev() {
    var text = "oi3rewfjvi8huegirweroqiwdjp9as-ucs0srjiwon4thd98fweron3"
    text += text
    text +=text
    text += '\n' + text

    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
        modifier = Modifier.pointerInput(Unit) {
            detectDragGestures { change, dragAmount ->
                change.consume()
                offsetX += dragAmount.x
                offsetY += dragAmount.y
            }
        }
    ) {

        Text(text,
            softWrap = false,
            modifier = Modifier
                .padding(16.dp)
                .offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) }
                .wrapContentSize(Alignment.TopStart, true) //允许显示出界
//                    .verticalScroll(vScrollState)
//                    .horizontalScroll(textHScrollState)
        )
    }

}
