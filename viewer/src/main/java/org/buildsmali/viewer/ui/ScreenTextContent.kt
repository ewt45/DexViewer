package org.buildsmali.viewer.ui

import android.annotation.SuppressLint
import android.content.res.Configuration
import android.util.Log
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationEndReason
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.calculateTargetValue
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.rememberSplineBasedDecay
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.splineBasedDecay
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import jadx.api.JavaClass
import kotlinx.coroutines.launch
import org.buildsmali.viewer.MyViewModel
import org.buildsmali.viewer.R
import org.buildsmali.viewer.utils.CodeType
import org.buildsmali.viewer.utils.Utils
import org.buildsmali.viewer.utils.inOpenRange
import org.json.JSONObject
import kotlin.math.min

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
        }

        if (result == 1 && viewModel.javaClasses.isNotEmpty()) {
            val displayCls = remember { mutableStateOf(viewModel.javaClasses[0]) }
            Utils.fixCodeCacheIsNull(displayCls.value)

            Toolbar(
                viewModel.javaClasses,
                displayCls,
                codeType,
                onCodeTypeSwitch = { codeType = it }
            )

            CodeTextAndroidView(
                codeType,
                when (codeType) {
                    CodeType.Java -> displayCls.value.code
                    CodeType.Smali -> displayCls.value.smali
                }
            )
//            Card(
//                colors = CardDefaults.cardColors(
//                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
//                ),
//                modifier = Modifier
//            ) {
//
//            }
        }
    }
}

/**
 * 顶部工具栏。包含当前选择的类（可切换）以及当前显示的代码类型（java/smali)
 */
@Composable
fun Toolbar(
    classes: List<JavaClass>, mutableDisplayCls: MutableState<JavaClass>,
    currCodeType: CodeType,
    onCodeTypeSwitch: (CodeType) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        JavaClassSelect(
            modifier = Modifier.weight(1f).padding(8.dp),
            classes,
            mutableDisplayCls
        )
        JavaSmaliSwitch(
            modifier = Modifier.padding(8.dp),
            currCodeType = currCodeType,
            options = listOf(
                CodeType.Java to { onCodeTypeSwitch(CodeType.Java) },
                CodeType.Smali to { onCodeTypeSwitch(CodeType.Smali) },
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

/**
 * 切换当前显示此类的Java代码还是smali代码
 * @param options 一个列表，每一项代表一种代码语言类型。每一项的first表示代码类型，second表示切换到该类型时应该执行的操作
 */
@Composable
fun JavaSmaliSwitch(
    modifier: Modifier = Modifier,
    currCodeType: CodeType,
    options: List<Pair<CodeType, () -> Unit>>
) {

    val currOption = options.filter { it.first == currCodeType }[0]
//    var selectIdx by remember { mutableIntStateOf(0) }
    OutlinedButton(
        modifier = modifier,
        onClick = {
            val nextIdx = options.run { (indexOf(currOption) + 1) % size }
            options[nextIdx].second()
        },
        shape = RoundedCornerShape(8.dp),
        contentPadding = PaddingValues(8.dp)
    ) {
        AnimatedContent(
            targetState = currOption.first.value,
            transitionSpec = {
                slideInVertically { fullHeight -> fullHeight } + fadeIn() togetherWith
                        slideOutVertically() + fadeOut() using
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

/**
 * 顶部工具栏中，左侧显示当前可显示的类名。点击可切换
 * @param classes 全部可选的类
 * @param mutableDisplayCls 当前选中的类
 */
@Composable
fun JavaClassSelect(
    modifier: Modifier = Modifier,
    classes: List<JavaClass>,
    mutableDisplayCls: MutableState<JavaClass>
) {
    var expanded by remember { mutableStateOf(false) }
    var displayCls by mutableDisplayCls
    val onClick = { expanded = !expanded }
    Row(
        modifier = modifier.clip(RoundedCornerShape(8.dp)).clickable { onClick() },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TextButton(
            onClick = onClick,
            shape = RoundedCornerShape(8.dp),
            contentPadding = PaddingValues(8.dp, 8.dp, 8.dp, 8.dp)
        ) {
            JavaClassNameText(displayCls.name, displayCls.`package`)
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

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun CodeTextAndroidView(lang: CodeType, code: String) {
    val isDarkTheme = isSystemInDarkTheme()
    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { ctx ->
            WebView(ctx).apply {
                settings.javaScriptEnabled = true // 启用 JavaScript
                setBackgroundColor(android.graphics.Color.TRANSPARENT) //防止多余背景显示白色

                //loadUrl异步加载，没法直接执行js函数。得等他加载完了再执行
                webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView?, url: String?) {
                        updateCodeHighlight(lang, code, isDarkTheme)
                    }
                }
                loadUrl("file:///android_asset/html_code_highlight/index.html")

            }
        },
        update = { view ->
            Log.d("TAG", "CodeTextComposableView: AndroidView的update被调用一次")
            view.updateCodeHighlight(lang, code, isDarkTheme)
        }
    )
}

/**
 * 传入要显示的代码，高亮显示
 * @param lang 语言 java或smali
 */
fun WebView.updateCodeHighlight(lang: CodeType, code: String, isDark: Boolean) {
    val json = JSONObject().apply {
        put("code", code)
        put("lang", lang.value.lowercase())
        put("isDark", isDark)
    }
    this.evaluateJavascript("doHighlight($json)", null)
    //updateTheme(${ if (isDark) "true" else "false"})
}


@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun prev() {
    CodeTextAndroidView(
        CodeType.Java, """
        package test;

        import java.io.FileInputStream;
        import java.io.IOException;
        import java.io.PrintStream;

        /* loaded from: /home/cloud/codes/jadx反编译/exagear windows 3.0.1/反编译错误/5 try catch/try resource处理 7.dex */
        public class Test5 {
            boolean flag;

            public void test1_普通trycatch() {
                try {
                    System.out.println("try");
                } catch (RuntimeException e) {
                    System.out.println("catch");
                }
            }

            public void test2_加finally() {
                try {
                    try {
                        System.out.println("try");
                    } catch (RuntimeException e) {
                        System.out.println("catch");
                    }
                } finally {
                    System.out.println("finally");
                }
            }

            public void test2${'$'}1_加外部finally() {
                try {
                    try {
                        System.out.println("try");
                    } catch (RuntimeException e) {
                        System.out.println("catch");
                    }
                } finally {
                    System.out.println("finally");
                }
            }

            public void test2${'$'}2_加finally嵌套() {
                PrintStream printStream;
                try {
                    try {
                        System.out.println("outer try");
                    } finally {
                        System.out.println("outer finally");
                    }
                } catch (RuntimeException e) {
                    System.out.println("outer catch");
                }
                try {
                    try {
                        System.out.println("inner try");
                        printStream = System.out;
                    } catch (RuntimeException e2) {
                        System.out.println("inner catch");
                        printStream = System.out;
                    }
                    printStream.println("inner finally");
                } finally {
                }
            }

            public void test2${'$'}3_加finally_多catch() {
                try {
                    try {
                        try {
                            new FileInputStream("file");
                            System.out.println("try");
                        } catch (RuntimeException e) {
                            System.out.println("catch");
                        }
                    } catch (IOException e2) {
                        System.out.println("catch_io");
                    }
                } finally {
                    System.out.println("finally");
                }
            }

            public void test3_try多一行() {
                try {
                    try {
                        System.out.println("try");
                        this.flag = true;
                    } catch (RuntimeException e) {
                        System.out.println("catch");
                    }
                } finally {
                    System.out.println("finally");
                }
            }

            public void test3${'$'}1_try多一行是throw() {
                try {
                    try {
                        System.out.println("try");
                        throw new RuntimeException("throw");
                    } catch (RuntimeException e) {
                        System.out.println("catch");
                        System.out.println("finally");
                    }
                } catch (Throwable th) {
                    System.out.println("finally");
                    throw th;
                }
            }

            public void test3${'$'}2_try多一行是throw放中间() {
                try {
                    try {
                        System.out.println("try");
                    } catch (RuntimeException e) {
                        System.out.println("catch");
                    }
                    if (!this.flag) {
                        throw new RuntimeException("throw");
                    }
                    this.flag = true;
                } finally {
                    System.out.println("finally");
                }
            }

            public void test4_try外有一行() {
                try {
                    try {
                        System.out.println("try");
                    } catch (RuntimeException e) {
                        System.out.println("catch");
                    }
                    System.out.println("Code after try-catch-finally");
                } finally {
                    System.out.println("finally");
                }
            }

            public void test5_try多一行_try外有一行() {
                try {
                    try {
                        System.out.println("try");
                        this.flag = true;
                    } catch (RuntimeException e) {
                        System.out.println("catch");
                    }
                    System.out.println("Code after try-catch-finally");
                } finally {
                    System.out.println("finally");
                }
            }

            public void test6_自己捕捉throwable() {
                try {
                    System.out.println("try");
                } catch (Throwable th) {
                    System.out.println("catch");
                }
            }

            public void test7_自己捕捉throwable和finally同时存在() {
                try {
                    System.out.println("try");
                } finally {
                    try {
                    } finally {
                    }
                }
            }
        }
    """.trimIndent()
    )
}

//手动绘制文本和自由滚动
@Composable
fun ScrollableCodeText() {
    var text = ""
    text += text
    text += text
    for (i in 0..10)
        text += "$i oi3rewfjvi8huegirweroqiwd6^*BVTgh9fjiewmrk4triegd0efeafsde\n"


    //文本四周内边距，文本偏移范围应算上这个
    val textPaddingPx = with(LocalDensity.current) { 16.dp.toPx() }

    //计算文本大小
    val textMeasurer = rememberTextMeasurer()
    val measuredText =
        textMeasurer.measure(
            AnnotatedString(text),
//                    constraints = Constraints.fixedWidth((size.width * 2f / 3f).toInt()),
            overflow = TextOverflow.Visible, //默认是Clip, 移动之后原本在屏幕外的部分仍然不会显示。改成Visible就可以显示出界了
            softWrap = false,
            style = TextStyle(fontSize = 18.sp, fontFamily = FontFamily.Monospace)
        )

    //计算文本偏移范围
    var textParentSize by remember { mutableStateOf(IntSize.Zero) }
    val maxOffsetX = textPaddingPx
    val maxOffsetY = textPaddingPx
    //最小值应该小于等于最大值，而不一定是小于等于0
    val minOffsetX =
        min(maxOffsetX, textParentSize.width - (measuredText.size.width + textPaddingPx))
    val minOffsetY =
        min(maxOffsetX, textParentSize.height - (measuredText.size.height + textPaddingPx))

    LaunchedEffect(minOffsetX) {
        Log.e("aaa", "prev: 到底会不会自动变化$minOffsetX")
    }

    // 使用动画来平滑衰减移动
    val animScope = rememberCoroutineScope()
    val animDecay = rememberSplineBasedDecay<Float>()

    val animOffX = remember { Animatable(textPaddingPx) }
    val animOffY = remember { Animatable(textPaddingPx) }
    animOffX.updateBounds(minOffsetX, maxOffsetX)
    animOffY.updateBounds(minOffsetY, maxOffsetY)


    //计算手指离开时的文字移动速度，之后从这个速度衰减到0
    val velocityTracker = remember { VelocityTracker() }
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
        shape = RectangleShape,
        modifier = Modifier
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = {
                        velocityTracker.resetTracking()
                    },
                    onDragEnd = {
                        val vel = velocityTracker.calculateVelocity(Velocity(7000f, 7000f))
                        Log.d("aaa", "prev: 计算速度 ${vel.x}, ${vel.y}")

                        animScope.launch {
                            animOffX.animateTo(
                                animDecay.calculateTargetValue(animOffX.value, vel.x),
                                tween(800, easing = LinearOutSlowInEasing)
                            )
                        }
                        animScope.launch {
                            animOffY.animateTo(
                                animDecay.calculateTargetValue(animOffY.value, vel.y),
                                tween(800, easing = LinearOutSlowInEasing)
                            )
                        }
                    },
                    //change.position是手指与视图（锚点？）的距离
                    // change.positionChange()和dragAmount是未消耗（一般就是上次事件到这次事件之间移动）的距离。不是从按下手指开始的总距离
                    onDrag = { change, dragAmount ->
                        change.consume()
                        animScope.launch {
                            animOffX.snapTo(animOffX.targetValue + dragAmount.x)
                            animOffY.snapTo(animOffY.targetValue + dragAmount.y)
                        }
                        //速度追踪器应该接收当前的位置
                        velocityTracker.addPosition(change.uptimeMillis, change.position)
//                        Log.d("aaa", "prev: position到底是距离父视图还是手指总距离 ${change.position}")
                    }
                )
            }
            .onGloballyPositioned { textParentSize = it.size }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            translate(left = animOffX.value, top = animOffY.value) { drawText(measuredText) }
        }
    }
}


@Composable
fun test1() {
    var text = ""
    text += text
    text += text
    for (i in 0..10)
        text += "$i oi3rewfjvi8huegirweroqiwd\n"


    //TODO 手动绘制文字 https://developer.android.com/develop/ui/compose/graphics/draw/overview

    //文本四周内边距，文本偏移范围应算上这个
    val textPaddingPx = with(LocalDensity.current) { 16.dp.toPx() }

    //文本显示的偏移量（随手指移动）
    var offsetX by remember { mutableFloatStateOf(textPaddingPx) }
    var offsetY by remember { mutableFloatStateOf(textPaddingPx) }

    //计算文本大小
    val textMeasurer = rememberTextMeasurer()
    val measuredText =
        textMeasurer.measure(
            AnnotatedString(text),
//                    constraints = Constraints.fixedWidth((size.width * 2f / 3f).toInt()),
            overflow = TextOverflow.Visible, //默认是Clip, 移动之后原本在屏幕外的部分仍然不会显示。改成Visible就可以显示出界了
            softWrap = false,
            style = TextStyle(fontSize = 18.sp)
        )

    //计算文本偏移范围
    var textParentSize by remember { mutableStateOf(IntSize.Zero) }
    val maxOffsetX = textPaddingPx
    val maxOffsetY = textPaddingPx
    val minOffsetX = min(0f, textParentSize.width - (measuredText.size.width + textPaddingPx))
    val minOffsetY = min(0f, textParentSize.height - (measuredText.size.height + textPaddingPx))



    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
        shape = RectangleShape,
        modifier = Modifier
            .pointerInput(Unit) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    offsetX = (offsetX + dragAmount.x).coerceIn(minOffsetX, maxOffsetX)
                    offsetY = (offsetY + dragAmount.y).coerceIn(minOffsetY, maxOffsetY)
                }
            }
            .onGloballyPositioned { textParentSize = it.size }
    ) {


        Canvas(
            modifier = Modifier
                .fillMaxSize()
        ) {
//            drawText(textMeasurer, text)
            translate(left = offsetX, top = offsetY) {
                drawText(measuredText)
            }

        }
    }
}

@Composable
fun test2() {
    var text = ""
    text += text
    text += text
    for (i in 0..20)
        text += "$i oi3rewfjvi8huegirweroqiwd6^*BVTgh9fjiewmrk4triegd0sde\n"


    //TODO 手动绘制文字 https://developer.android.com/develop/ui/compose/graphics/draw/overview
    // 基于数值的动画 https://developer.android.com/develop/ui/compose/animation/value-based
    // 惯性滚动（滚动速度平滑衰减）https://jetpackcompose.cn/docs/design/gesture/gesture_with_anim/
    // 同上 https://juejin.cn/post/7171420234811703333

    //1. 文字跟随手指移动：
    //  - 使用modifier.pointerInput(Unit) { detectDragGestures() } 来获取手指移动距离，存入一个Offset
    //  - 有关detectDragGestures的onDrag
    //      - change.position是手指与视图（锚点？）的距离
    //      - change.positionChange()和dragAmount是未消耗（一般就是上次事件到这次事件之间移动）的距离。不是从按下手指开始的总距离
    //  - Canvas(){ translate(left, top){ drawText(measuredText) } }手动绘制文本并根据手指位置移动
    //  - textMeasurer.measure() 生成用于绘制的文字，
    //      - 设定overflow=TextOverflow.Visible 保证移动之后，原本在屏幕之外的位置的文字也会绘制出来。默认是Clip不会绘制。
    //      - 设定softWrap为false取消自动换行
    //2. 限制文字移动距离
    //  - 从measuredText获取这些文字总宽高，父布局的modifier.onGloballyPositioned获取父布局宽高，计算可移动范围
    //  - 注意文本四周的内边距也要考虑
    //3. 松手后速度平滑衰减（惯性滚动）
    //  - 将记录的Offset改为Animatable(Offset.Zero, Offset.VectorConverter)
    //      - updateBounds设置移动范围
    //      - onDrag时调用.snapTo直接变化数值,
    //      - onDragEnd时调用.animateDecay平滑衰减。
    //  - 使用VelocityTracker计算松手时的速度。
    //      - onDragStart时 重置记录
    //      - onDrag时 .addPosition(change.uptimeMillis, change.position) 添加记录。注意添加的数据应该有一个稳定的参考系
    //      - onDragEnd时，计算出速度vel, 然后调用Animatable.animateDecay(Offset(vel.x, vel.y), decay)实现平滑衰减。decay是splineBasedDecay<Offset>(this)
    //4. 这样虽然可以惯性滚动了，但是在x和y有任意一个方向达到可移动范围边界后就会立刻停止两个方向的动画。
    // 最好改一下，让它只停止一个，另一个方向继续动画。

    //文本四周内边距，文本偏移范围应算上这个
    val textPaddingPx = with(LocalDensity.current) { 16.dp.toPx() }

    //计算文本大小
    val textMeasurer = rememberTextMeasurer()
    val measuredText =
        textMeasurer.measure(
            AnnotatedString(text),
//                    constraints = Constraints.fixedWidth((size.width * 2f / 3f).toInt()),
            overflow = TextOverflow.Visible, //默认是Clip, 移动之后原本在屏幕外的部分仍然不会显示。改成Visible就可以显示出界了
            softWrap = false,
            style = TextStyle(fontSize = 18.sp)
        )

    //计算文本偏移范围
    var textParentSize by remember { mutableStateOf(IntSize.Zero) }
    val maxOffsetX = textPaddingPx
    val maxOffsetY = textPaddingPx
    //最小值应该小于等于最大值，而不一定是小于等于0
    val minOffsetX =
        min(maxOffsetX, textParentSize.width - (measuredText.size.width + textPaddingPx))
    val minOffsetY =
        min(maxOffsetX, textParentSize.height - (measuredText.size.height + textPaddingPx))

    // 使用动画来平滑衰减移动
    val animatedOffset =
        remember { Animatable(Offset(textPaddingPx, textPaddingPx), Offset.VectorConverter) }
    animatedOffset.updateBounds(Offset(minOffsetX, minOffsetY), Offset(maxOffsetX, maxOffsetY))
    val animScope = rememberCoroutineScope()


    //计算手指离开时的文字移动速度，之后从这个速度衰减到0
    val velocityTracker = remember { VelocityTracker() }
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
        shape = RectangleShape,
        modifier = Modifier
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = {
                        velocityTracker.resetTracking()
                    },
                    onDragEnd = {
                        val vel = velocityTracker.calculateVelocity()
//                        Log.d("aaa", "prev: 计算速度 ${vel.x}, ${vel.y}")
                        val decay = splineBasedDecay<Offset>(this)
                        animScope.launch {
                            animatedOffset.animateDecay(Offset(vel.x, vel.y), decay)
                                .run {
                                    //endReason是BoundReached. 此时可能只有一个方向到头，另一个方向还没到
                                    //此时虽然有一方到头但速度仍不是0, 应该用value来判断
                                    val secVel = endState.velocity
                                    val secOffset = endState.value
                                    if (endReason == AnimationEndReason.BoundReached) {
                                        // x仍应继续滚动
                                        if (secOffset.x > minOffsetX && secOffset.x < maxOffsetX) {
                                            Log.d("aaa", "prev: x仍应继续滚动")
                                            animatedOffset.animateDecay(Offset(0f, secVel.y), decay)
                                        }
                                        //y仍应继续滚动
                                        if (secOffset.y > minOffsetY && secOffset.y < maxOffsetY) {
                                            Log.d("aaa", "prev: y仍应继续滚动")
                                            animatedOffset.animateDecay(Offset(secVel.x, 0f), decay)
                                        }
                                    }
                                    Log.d(
                                        "aaa",
                                        "prev: 结束时理由=$endReason, 速度是否都为0 ${secVel.x}, ${secVel.y}, " +
                                                "是否没到达边界 ${
                                                    secOffset.x.inOpenRange(
                                                        minOffsetX,
                                                        maxOffsetX
                                                    )
                                                }， ${
                                                    secOffset.y.inOpenRange(
                                                        minOffsetY,
                                                        maxOffsetY
                                                    )
                                                }"
                                    )
                                }


//                            animatedOffset.animateTo(
//                                decay.calculateTargetValue(Offset.VectorConverter, animatedOffset.value, Offset(vel.x, vel.y)),
//                                tween(1000, easing = LinearOutSlowInEasing)
//                            )
                        }
                    },
                    //change.position是手指与视图（锚点？）的距离
                    // change.positionChange()和dragAmount是未消耗（一般就是上次事件到这次事件之间移动）的距离。不是从按下手指开始的总距离
                    onDrag = { change, dragAmount ->
                        change.consume()
                        animScope.launch { animatedOffset.snapTo(animatedOffset.targetValue + dragAmount) }
                        //速度追踪器应该接收当前的位置
                        velocityTracker.addPosition(change.uptimeMillis, change.position)
//                        Log.d("aaa", "prev: position到底是距离父视图还是手指总距离 ${change.position}")
                    }
                )
            }
            .onGloballyPositioned { textParentSize = it.size }
    ) {


        Canvas(
            modifier = Modifier
                .fillMaxSize()
        ) {
//            drawText(textMeasurer, text)
            translate(
                left = animatedOffset.value.x,
                top = animatedOffset.value.y
            ) {
                drawText(measuredText)
            }

        }
    }

}