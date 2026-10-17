package com.huanchengfly.tieba.post.ui.utils

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp

/**
 * 取底部安全区高度：仅 navigationBars 底部值。
 *
 * 此前取 navigationBars ∪ safeGestures 并在两者均为 0 时兜底 28dp，
 * 手势导航下 safeGestures 普遍高于导航栏，底栏下方因此多出大块空白，已回退。
 */
@Composable
fun bottomBarInsetsHeight(): Dp {
    val density = LocalDensity.current
    val bottomPx = WindowInsets.navigationBars.getBottom(density)
    return with(density) { bottomPx.toDp() }
}

/**
 * 底栏手势热区避让：高度取 [bottomBarInsetsHeight]，
 * 并在该区域内消费点击，避免触摸穿透到手势热区之下的内容。
 */
fun Modifier.bottomBarInsetsPadding(): Modifier = composed {
    val height = bottomBarInsetsHeight()
    this
        .height(height)
        .clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null,
            onClick = {}
        )
}

/**
 * 底栏手势热区占位 Spacer：高度逻辑同 [bottomBarInsetsPadding]，
 * 并消费点击防止触摸穿透。用于替换裸的 windowInsetsBottomHeight Spacer。
 */
@Composable
fun BottomInsetsSpacer(modifier: Modifier = Modifier) {
    Spacer(modifier = modifier.bottomBarInsetsPadding())
}
