package com.huanchengfly.tieba.post.ui.page.settings.about

import com.huanchengfly.tieba.post.ui.page.Routes
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Card
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedButton
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.OpenInNew
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.google.accompanist.drawablepainter.rememberDrawablePainter
import com.huanchengfly.tieba.post.BuildConfig
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.arch.hiltViewModel
import com.huanchengfly.tieba.post.toastShort
import com.huanchengfly.tieba.post.ui.common.theme.compose.ExtendedTheme
import com.huanchengfly.tieba.post.ui.widgets.compose.BackNavigationIcon
import com.huanchengfly.tieba.post.ui.widgets.compose.MyScaffold
import com.huanchengfly.tieba.post.ui.widgets.compose.TitleCentredToolbar
import com.huanchengfly.tieba.post.update.UpdateDownloadProgress
import com.huanchengfly.tieba.post.update.UpdateOrigin
import com.huanchengfly.tieba.post.update.UpdateSource
import com.huanchengfly.tieba.post.update.UpdateUiState
import com.huanchengfly.tieba.post.update.UpdateViewModel
import com.huanchengfly.tieba.post.utils.appPreferences
import androidx.compose.runtime.collectAsState

@Composable
fun AboutPage(
    navigator: NavHostController,
) {
    var lastClickTime by remember { mutableLongStateOf(0L) }
    var clickCount by remember { mutableIntStateOf(0) }
    // 与启动路径共用同一更新模块（同一 ViewModel / 比对 / 元数据解析）
    val updateViewModel: UpdateViewModel = hiltViewModel()
    val updateState by updateViewModel.state.collectAsState()
    val updateOrigin by updateViewModel.origin.collectAsState()
    val context = LocalContext.current
    val currentVersion = BuildConfig.VERSION_NAME

    MyScaffold(
        backgroundColor = Color.Transparent,
        topBar = {
            TitleCentredToolbar(
                title = {
                    Text(
                        text = stringResource(id = R.string.title_about),
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.h6
                    )
                },
                navigationIcon = {
                    BackNavigationIcon(onBackPressed = { navigator.popBackStack() })
                }
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(modifier = Modifier.height(32.dp))

            // App Icon
            Image(
                painter = rememberDrawablePainter(
                    drawable = context.getDrawable(R.mipmap.ic_launcher_new)
                ),
                contentDescription = null,
                modifier = Modifier.size(88.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = stringResource(id = R.string.app_name),
                style = MaterialTheme.typography.h6,
                fontWeight = FontWeight.Bold,
                color = ExtendedTheme.colors.text
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = currentVersion,
                style = MaterialTheme.typography.body2,
                color = ExtendedTheme.colors.textSecondary,
                modifier = Modifier.clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {
                        val currentTime = System.currentTimeMillis()
                        if (currentTime - lastClickTime < 500) clickCount++
                        else clickCount = 1
                        lastClickTime = currentTime
                        if (clickCount >= 7) {
                            clickCount = 0
                            context.appPreferences.showExperimentalFeatures =
                                !context.appPreferences.showExperimentalFeatures
                            val msg = if (context.appPreferences.showExperimentalFeatures)
                                R.string.toast_experimental_features_enabled
                            else R.string.toast_experimental_features_disabled
                            context.toastShort(msg)
                        }
                    }
                )
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Update Origin Selector（更新渠道：Codeberg 主 / GitHub 镜像）
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                shape = RoundedCornerShape(16.dp),
                backgroundColor = ExtendedTheme.colors.text.copy(alpha = 0.06f),
                elevation = 0.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        stringResource(id = R.string.text_update_origin_title),
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = ExtendedTheme.colors.text
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        stringResource(id = R.string.text_update_origin_desc),
                        color = ExtendedTheme.colors.textSecondary,
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        UpdateOriginOption(
                            label = stringResource(id = R.string.text_update_origin_codeberg),
                            selected = updateOrigin == UpdateOrigin.CODEBERG,
                            onClick = { updateViewModel.setOrigin(UpdateOrigin.CODEBERG) },
                            modifier = Modifier.weight(1f)
                        )
                        UpdateOriginOption(
                            label = stringResource(id = R.string.text_update_origin_github),
                            selected = updateOrigin == UpdateOrigin.GITHUB,
                            onClick = { updateViewModel.setOrigin(UpdateOrigin.GITHUB) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Update Check Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                shape = RoundedCornerShape(16.dp),
                backgroundColor = ExtendedTheme.colors.text.copy(alpha = 0.06f),
                elevation = 0.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    when (val state = updateState) {
                        UpdateUiState.Idle -> {
                            Text(
                                stringResource(id = R.string.text_update_check_idle),
                                color = ExtendedTheme.colors.textSecondary,
                                fontSize = 14.sp
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = { updateViewModel.manualCheck() },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    backgroundColor = ExtendedTheme.colors.accent,
                                    contentColor = Color.White
                                )
                            ) {
                                Icon(
                                    Icons.Rounded.SystemUpdate,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(stringResource(id = R.string.text_update_check_now), fontWeight = FontWeight.Medium)
                            }
                        }
                        is UpdateUiState.Checking -> {
                            CircularProgressIndicator(
                                modifier = Modifier.size(32.dp),
                                strokeWidth = 3.dp,
                                color = ExtendedTheme.colors.accent
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                stringResource(id = R.string.text_update_checking),
                                color = ExtendedTheme.colors.textSecondary,
                                fontSize = 14.sp
                            )
                        }
                        is UpdateUiState.Available -> {
                            Icon(
                                Icons.Rounded.SystemUpdate,
                                contentDescription = null,
                                tint = ExtendedTheme.colors.accent,
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                stringResource(id = R.string.title_update_available),
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = ExtendedTheme.colors.accent
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                state.release.name.ifEmpty { state.release.tagName },
                                color = ExtendedTheme.colors.textSecondary,
                                fontSize = 14.sp
                            )
                            if (state.force) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    stringResource(id = R.string.text_update_force),
                                    color = ExtendedTheme.colors.accent,
                                    fontSize = 12.sp
                                )
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Button(
                                    onClick = { updateViewModel.download() },
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        backgroundColor = ExtendedTheme.colors.accent,
                                        contentColor = Color.White
                                    )
                                ) {
                                    Text(stringResource(id = R.string.button_update_now), fontWeight = FontWeight.Medium)
                                }
                                OutlinedButton(
                                    onClick = {
                                        try {
                                            val url = state.release.htmlUrl.ifEmpty {
                                                "https://codeberg.org/min09577/TiebaLite/releases"
                                            }
                                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                            context.startActivity(intent)
                                        } catch (e: Exception) {
                                            context.toastShort("无法打开链接")
                                        }
                                    },
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(
                                        Icons.Rounded.OpenInNew,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("GitHub", fontWeight = FontWeight.Medium)
                                }
                            }
                        }
                        is UpdateUiState.Downloading -> {
                            Spacer(modifier = Modifier.height(4.dp))
                            UpdateDownloadProgress(
                                downloaded = state.downloaded,
                                total = state.total
                            )
                        }
                        is UpdateUiState.ReadyToInstall -> {
                            Icon(
                                Icons.Rounded.CheckCircle,
                                contentDescription = null,
                                tint = Color(0xFF4CAF50),
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                stringResource(
                                    id = if (state.verified) R.string.text_update_ready
                                    else R.string.text_update_ready_unverified
                                ),
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = ExtendedTheme.colors.text
                            )
                            state.installError?.let { error ->
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    error,
                                    color = ExtendedTheme.colors.accent,
                                    fontSize = 12.sp,
                                    textAlign = TextAlign.Center
                                )
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = { updateViewModel.install() },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    backgroundColor = ExtendedTheme.colors.accent,
                                    contentColor = Color.White
                                )
                            ) {
                                Text(stringResource(id = R.string.button_update_install), fontWeight = FontWeight.Medium)
                            }
                        }
                        is UpdateUiState.DownloadFailed -> {
                            Text(
                                stringResource(id = R.string.text_update_download_failed) + "：" + state.message,
                                color = MaterialTheme.colors.error,
                                fontSize = 13.sp,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            TextButton(onClick = { updateViewModel.download() }) {
                                Text(stringResource(id = R.string.button_update_retry))
                            }
                        }
                        is UpdateUiState.UpToDate -> {
                            Icon(
                                Icons.Rounded.CheckCircle,
                                contentDescription = null,
                                tint = Color(0xFF4CAF50),
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                stringResource(id = R.string.text_update_up_to_date),
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = ExtendedTheme.colors.text
                            )
                        }
                        is UpdateUiState.Failed -> {
                            Text(
                                state.message,
                                color = MaterialTheme.colors.error,
                                fontSize = 13.sp,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            TextButton(onClick = { updateViewModel.manualCheck() }) {
                                Text(stringResource(id = R.string.button_update_retry))
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // GitHub Repo Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                shape = RoundedCornerShape(16.dp),
                backgroundColor = ExtendedTheme.colors.text.copy(alpha = 0.06f),
                elevation = 0.dp
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "项目地址",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                        color = ExtendedTheme.colors.textSecondary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    TextButton(
                        onClick = {
                            try {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/qaqmin/TiebaLite"))
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                context.toastShort("无法打开链接")
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            "github.com/qaqmin/TiebaLite",
                            color = ExtendedTheme.colors.accent,
                            fontSize = 13.sp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            Icons.Rounded.OpenInNew,
                            contentDescription = null,
                            tint = ExtendedTheme.colors.accent,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }


            TextButton(
                onClick = { navigator.navigate(Routes.LOG) },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                shape = RoundedCornerShape(12.dp),
            ) {
                Text("查看日志", color = ExtendedTheme.colors.textSecondary, fontSize = 13.sp)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = stringResource(id = R.string.tip_about, BuildConfig.VERSION_NAME),
                style = MaterialTheme.typography.caption,
                color = ExtendedTheme.colors.textSecondary
            )

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

/**
 * 更新渠道选项：选中态用主题色实心按钮，未选中用描边按钮（与页面其他按钮同风格）。
 */
@Composable
private fun UpdateOriginOption(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (selected) {
        Button(
            onClick = onClick,
            modifier = modifier,
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                backgroundColor = ExtendedTheme.colors.accent,
                contentColor = Color.White
            )
        ) {
            Text(label, fontWeight = FontWeight.Medium, fontSize = 13.sp)
        }
    } else {
        OutlinedButton(
            onClick = onClick,
            modifier = modifier,
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(label, fontWeight = FontWeight.Medium, fontSize = 13.sp)
        }
    }
}
