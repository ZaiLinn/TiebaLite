package com.huanchengfly.tieba.post.ui.page

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.huanchengfly.tieba.post.ui.common.theme.compose.TonalPalette
import com.huanchengfly.tieba.post.ui.common.theme.compose.dynamicTonalPalette
import kotlinx.collections.immutable.toImmutableList
@Composable
fun MonetTestPage() {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
        Text(text = "Only available on Android 12+")
        return
    }
    val context = LocalContext.current
    val tonalPalette = remember { dynamicTonalPalette(context) }
    // 手动列出所有颜色属性，免去为一个调试页引入 kotlin-reflect（会让 APK 多 1MB 以上）
    val properties = remember {
        listOf(
            TonalPalette::neutral100,
            TonalPalette::neutral99,
            TonalPalette::neutral95,
            TonalPalette::neutral90,
            TonalPalette::neutral80,
            TonalPalette::neutral70,
            TonalPalette::neutral60,
            TonalPalette::neutral50,
            TonalPalette::neutral40,
            TonalPalette::neutral30,
            TonalPalette::neutral20,
            TonalPalette::neutral10,
            TonalPalette::neutral0,
            TonalPalette::neutralVariant100,
            TonalPalette::neutralVariant99,
            TonalPalette::neutralVariant95,
            TonalPalette::neutralVariant90,
            TonalPalette::neutralVariant80,
            TonalPalette::neutralVariant70,
            TonalPalette::neutralVariant60,
            TonalPalette::neutralVariant50,
            TonalPalette::neutralVariant40,
            TonalPalette::neutralVariant30,
            TonalPalette::neutralVariant20,
            TonalPalette::neutralVariant10,
            TonalPalette::neutralVariant0,
            TonalPalette::primary100,
            TonalPalette::primary99,
            TonalPalette::primary95,
            TonalPalette::primary90,
            TonalPalette::primary80,
            TonalPalette::primary70,
            TonalPalette::primary60,
            TonalPalette::primary50,
            TonalPalette::primary40,
            TonalPalette::primary30,
            TonalPalette::primary20,
            TonalPalette::primary10,
            TonalPalette::primary0,
            TonalPalette::secondary100,
            TonalPalette::secondary99,
            TonalPalette::secondary95,
            TonalPalette::secondary90,
            TonalPalette::secondary80,
            TonalPalette::secondary70,
            TonalPalette::secondary60,
            TonalPalette::secondary50,
            TonalPalette::secondary40,
            TonalPalette::secondary30,
            TonalPalette::secondary20,
            TonalPalette::secondary10,
            TonalPalette::secondary0,
            TonalPalette::tertiary100,
            TonalPalette::tertiary99,
            TonalPalette::tertiary95,
            TonalPalette::tertiary90,
            TonalPalette::tertiary80,
            TonalPalette::tertiary70,
            TonalPalette::tertiary60,
            TonalPalette::tertiary50,
            TonalPalette::tertiary40,
            TonalPalette::tertiary30,
            TonalPalette::tertiary20,
            TonalPalette::tertiary10,
            TonalPalette::tertiary0,
        ).sortedBy { it.name }.toImmutableList()
    }
    LazyColumn {
        items(properties) {
            Row {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(color = it.get(tonalPalette))
                )
                Text(text = it.name)
            }
        }
    }
}
