package com.huanchengfly.tieba.post.ui.page.thread.post

import android.view.View
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AlternateEmail
import androidx.compose.material.icons.outlined.EmojiEmotions
import androidx.compose.material.icons.outlined.InsertPhoto
import androidx.compose.material.icons.rounded.Send
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.widget.addTextChangedListener
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.toastShort
import com.huanchengfly.tieba.post.ui.common.theme.compose.ExtendedTheme
import com.huanchengfly.tieba.post.ui.page.reply.AtUserPickerDialog
import com.huanchengfly.tieba.post.ui.page.reply.EmoticonPanel
import com.huanchengfly.tieba.post.ui.page.reply.ImagePanel
import com.huanchengfly.tieba.post.ui.page.reply.VcodeDialog
import com.huanchengfly.tieba.post.ui.widgets.compose.BackNavigationIcon
import com.huanchengfly.tieba.post.ui.widgets.compose.MyScaffold
import com.huanchengfly.tieba.post.ui.widgets.compose.TitleCentredToolbar
import com.huanchengfly.tieba.post.ui.widgets.edittext.widget.UndoableEditText
import com.huanchengfly.tieba.post.utils.StringUtil
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList

private enum class PostPanel { NONE, EMOJI, IMAGE }

/** 发新帖：标题（可空）、正文、表情、图片、@ 好友，需要验证码时弹出验证 */
@Composable
fun PostThreadPage(
    forumId: Long,
    forumName: String,
    navigator: NavHostController,
    viewModel: PostThreadViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val sending by viewModel.sending.collectAsState()
    var title by rememberSaveable { mutableStateOf("") }
    var content by remember { mutableStateOf("") }
    var editText by remember { mutableStateOf<UndoableEditText?>(null) }
    var panel by remember { mutableStateOf(PostPanel.NONE) }
    var images by remember { mutableStateOf(persistentListOf<String>().toImmutableList()) }
    var isOriginImage by remember { mutableStateOf(false) }
    var showAtPicker by remember { mutableStateOf(false) }
    var pendingVcode by remember { mutableStateOf<PostThreadViewModel.Event.NeedVcode?>(null) }

    fun insert(text: String) {
        editText?.apply {
            val start = selectionStart
            this.text?.insert(start, text)
            setText(StringUtil.getEmoticonContent(this, this.text))
            setSelection(start + text.length)
        }
    }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is PostThreadViewModel.Event.Success -> {
                    context.toastShort(event.message.ifEmpty { context.getString(R.string.toast_post_thread_success) })
                    navigator.popBackStack()
                    navigator.navigate("thread/${event.threadId}")
                }

                is PostThreadViewModel.Event.NeedVcode -> pendingVcode = event
                is PostThreadViewModel.Event.Failure ->
                    context.toastShort(context.getString(R.string.toast_post_thread_failed, event.message))
            }
        }
    }

    pendingVcode?.let { request ->
        VcodeDialog(
            challenge = request.challenge,
            message = request.message,
            onResult = { vcode ->
                pendingVcode = null
                viewModel.resend(
                    request.retry.copy(vcode = vcode, vcodeMd5 = request.challenge.md5, vcodeType = request.challenge.type)
                )
            },
            onDismiss = { pendingVcode = null },
        )
    }
    if (showAtPicker) {
        AtUserPickerDialog(
            onPick = { user ->
                showAtPicker = false
                insert("@${user.displayName} ")
            },
            onDismiss = { showAtPicker = false },
        )
    }

    MyScaffold(
        backgroundColor = Color.Transparent,
        topBar = {
            TitleCentredToolbar(
                title = stringResource(id = R.string.title_post_thread, forumName),
                navigationIcon = { BackNavigationIcon { navigator.popBackStack() } },
                actions = {
                    if (sending) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp).padding(end = 4.dp), strokeWidth = 2.dp)
                    } else {
                        IconButton(
                            enabled = content.isNotBlank() || images.isNotEmpty(),
                            onClick = {
                                viewModel.send(title, content, images, isOriginImage, forumId, forumName)
                            },
                        ) {
                            Icon(imageVector = Icons.Rounded.Send, contentDescription = stringResource(id = R.string.button_send))
                        }
                    }
                },
            )
        },
        modifier = Modifier.fillMaxSize(),
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
                .navigationBarsPadding()
                .imePadding(),
        ) {
            OutlinedTextField(
                value = title,
                onValueChange = { title = it.take(TITLE_MAX_LENGTH) },
                singleLine = true,
                placeholder = { Text(text = stringResource(id = R.string.hint_post_thread_title)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp),
            ) {
                AndroidView(
                    factory = { ctx ->
                        (View.inflate(ctx, R.layout.layout_reply_edit_text, null) as UndoableEditText).apply {
                            editText = this
                            hint = ctx.getString(R.string.hint_post_thread_content)
                            setOnFocusChangeListener { _, hasFocus -> if (hasFocus) panel = PostPanel.NONE }
                            addTextChangedListener(afterTextChanged = { content = it?.toString().orEmpty() })
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = { showAtPicker = true }) {
                    Icon(imageVector = Icons.Outlined.AlternateEmail, contentDescription = stringResource(id = R.string.title_at_user))
                }
                IconButton(onClick = { panel = if (panel == PostPanel.EMOJI) PostPanel.NONE else PostPanel.EMOJI }) {
                    Icon(imageVector = Icons.Outlined.EmojiEmotions, contentDescription = stringResource(id = R.string.insert_emotions))
                }
                IconButton(onClick = { panel = if (panel == PostPanel.IMAGE) PostPanel.NONE else PostPanel.IMAGE }) {
                    Icon(imageVector = Icons.Outlined.InsertPhoto, contentDescription = stringResource(id = R.string.insert_photo))
                }
                Spacer(modifier = Modifier.weight(1f))
                if (images.isNotEmpty()) {
                    Text(
                        text = stringResource(id = R.string.tip_post_thread_images, images.size),
                        color = ExtendedTheme.colors.textSecondary,
                    )
                }
            }
            when (panel) {
                PostPanel.EMOJI -> EmoticonPanel(
                    modifier = Modifier.fillMaxWidth().height(PANEL_HEIGHT),
                    onEmoticonClick = { insert("#(${it.name})") },
                )

                PostPanel.IMAGE -> ImagePanel(
                    selectedImages = images,
                    onNewImageSelected = { uris ->
                        images = (images + uris.map { it.toString() }).take(MAX_IMAGES).toImmutableList()
                    },
                    onRemoveImage = { index ->
                        images = images.filterIndexed { i, _ -> i != index }.toImmutableList()
                    },
                    isOriginImage = isOriginImage,
                    onIsOriginImageChange = { isOriginImage = it },
                    modifier = Modifier.fillMaxWidth().height(PANEL_HEIGHT),
                )

                PostPanel.NONE -> Unit
            }
        }
    }
}

private const val TITLE_MAX_LENGTH = 31
private const val MAX_IMAGES = 9
private val PANEL_HEIGHT = 240.dp
