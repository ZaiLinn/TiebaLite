package com.huanchengfly.tieba.post.ui.page.im

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.LinearProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.im.ImMessage
import com.huanchengfly.tieba.post.im.ImPeer
import com.huanchengfly.tieba.post.im.PrivateMessageRepository
import com.huanchengfly.tieba.post.im.PrivateMessages
import com.huanchengfly.tieba.post.toastShort
import com.huanchengfly.tieba.post.ui.common.theme.compose.ExtendedTheme
import com.huanchengfly.tieba.post.ui.widgets.compose.Avatar
import com.huanchengfly.tieba.post.ui.widgets.compose.BackNavigationIcon
import com.huanchengfly.tieba.post.ui.widgets.compose.MyLazyColumn
import com.huanchengfly.tieba.post.ui.widgets.compose.MyScaffold
import com.huanchengfly.tieba.post.ui.widgets.compose.TitleCentredToolbar
import com.huanchengfly.tieba.post.utils.DateTimeUtils
import com.huanchengfly.tieba.post.utils.StringUtil
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** 私信页面打开期间每隔多久拉一次新消息 */
private const val POLL_INTERVAL_MS = 5_000L

fun privateChatRoute(peer: ImPeer): String =
    "im/chat/${peer.uid}?name=${Uri.encode(peer.name)}&portrait=${Uri.encode(peer.portrait)}"

/** 页面可见期间持有长连接并定时拉取 */
@Composable
private fun KeepPrivateMessagesFresh() {
    DisposableEffect(Unit) {
        PrivateMessageRepository.acquire()
        onDispose { PrivateMessageRepository.release() }
    }
    LaunchedEffect(Unit) {
        while (true) {
            PrivateMessageRepository.refresh()
            delay(POLL_INTERVAL_MS)
        }
    }
}

@Composable
fun PrivateMessageListPage(navigator: NavHostController) {
    KeepPrivateMessagesFresh()
    val state by PrivateMessageRepository.state.collectAsState()
    val conversations = remember(state.messages) { PrivateMessages.conversations(state.messages) }
    val context = LocalContext.current
    MyScaffold(
        backgroundColor = Color.Transparent,
        topBar = {
            TitleCentredToolbar(
                title = stringResource(id = R.string.title_private_messages),
                navigationIcon = { BackNavigationIcon { navigator.popBackStack() } },
            ) {
                if (state.loading && conversations.isEmpty()) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }
            }
        },
        modifier = Modifier.fillMaxSize(),
    ) { paddingValues ->
        Column(modifier = Modifier.padding(paddingValues).fillMaxSize()) {
            state.error?.let { ErrorBar(it) }
            if (conversations.isEmpty()) {
                if (!state.loading) CenterTip(stringResource(id = R.string.tip_private_message_empty))
            } else {
                MyLazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(conversations, key = { it.peer.uid }) { conversation ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { navigator.navigate(privateChatRoute(conversation.peer)) }
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Avatar(
                                data = StringUtil.getAvatarUrl(conversation.peer.portrait),
                                size = 48.dp,
                                contentDescription = null,
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = conversation.peer.name.ifEmpty { conversation.peer.uid.toString() },
                                        style = MaterialTheme.typography.subtitle1,
                                        color = ExtendedTheme.colors.text,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f),
                                    )
                                    Text(
                                        text = DateTimeUtils.getRelativeTimeString(
                                            context,
                                            conversation.lastMessage.createTime
                                        ),
                                        style = MaterialTheme.typography.caption,
                                        color = ExtendedTheme.colors.textSecondary,
                                    )
                                }
                                Text(
                                    text = conversation.lastMessage.displayText(),
                                    style = MaterialTheme.typography.body2,
                                    color = ExtendedTheme.colors.textSecondary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PrivateChatPage(peer: ImPeer, navigator: NavHostController) {
    KeepPrivateMessagesFresh()
    val state by PrivateMessageRepository.state.collectAsState()
    val messages = remember(state.messages, peer.uid) { state.messages.filter { it.peer.uid == peer.uid } }
    val title = messages.lastOrNull { it.peer.name.isNotEmpty() }?.peer?.name
        ?: peer.name.ifEmpty { peer.uid.toString() }
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    var input by rememberSaveable { mutableStateOf("") }
    var sending by remember { mutableStateOf(false) }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) listState.scrollToItem(messages.lastIndex)
    }

    MyScaffold(
        backgroundColor = Color.Transparent,
        topBar = {
            TitleCentredToolbar(
                title = title,
                navigationIcon = { BackNavigationIcon { navigator.popBackStack() } },
            )
        },
        bottomBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(ExtendedTheme.colors.bottomBar)
                    .navigationBarsPadding()
                    .imePadding()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it },
                    placeholder = { Text(text = stringResource(id = R.string.hint_private_message_input)) },
                    maxLines = 4,
                    modifier = Modifier.weight(1f),
                )
                if (sending) {
                    CircularProgressIndicator(modifier = Modifier.padding(horizontal = 12.dp))
                } else {
                    TextButton(
                        enabled = input.isNotBlank(),
                        onClick = {
                            val text = input.trim()
                            sending = true
                            coroutineScope.launch {
                                PrivateMessageRepository.send(peer, text)
                                    .onSuccess { input = "" }
                                    .onFailure {
                                        context.toastShort(
                                            R.string.toast_private_message_send_failed,
                                            it.message.orEmpty()
                                        )
                                    }
                                sending = false
                            }
                        },
                    ) {
                        Text(text = stringResource(id = R.string.button_send))
                    }
                }
            }
        },
        modifier = Modifier.fillMaxSize(),
    ) { paddingValues ->
        Column(modifier = Modifier.padding(paddingValues).fillMaxSize()) {
            state.error?.let { ErrorBar(it) }
            if (messages.isEmpty()) {
                if (!state.loading) CenterTip(stringResource(id = R.string.tip_private_message_chat_empty))
            } else {
                MyLazyColumn(
                    state = listState,
                    contentPadding = PaddingValues(vertical = 8.dp),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    items(messages, key = { it.msgId }) { message -> MessageBubble(message) }
                }
            }
        }
    }
}

@Composable
private fun MessageBubble(message: ImMessage) {
    val context = LocalContext.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp),
        horizontalAlignment = if (message.fromSelf) Alignment.End else Alignment.Start,
    ) {
        Text(
            text = message.displayText(),
            color = if (message.fromSelf) Color.White else ExtendedTheme.colors.text,
            style = MaterialTheme.typography.body1,
            modifier = Modifier
                .widthIn(max = 280.dp)
                .background(
                    color = if (message.fromSelf) ExtendedTheme.colors.primary else ExtendedTheme.colors.card,
                    shape = RoundedCornerShape(12.dp),
                )
                .padding(horizontal = 12.dp, vertical = 8.dp),
        )
        Text(
            text = DateTimeUtils.getRelativeTimeString(context, message.createTime),
            style = MaterialTheme.typography.caption,
            color = ExtendedTheme.colors.textSecondary,
            modifier = Modifier.padding(top = 2.dp, start = 4.dp, end = 4.dp),
        )
    }
}

@Composable
private fun ImMessage.displayText(): String =
    if (supported) text else stringResource(id = R.string.tip_private_message_unsupported)

@Composable
private fun ErrorBar(error: String) {
    Text(
        text = stringResource(id = R.string.tip_private_message_connect_failed, error),
        color = MaterialTheme.colors.error,
        style = MaterialTheme.typography.body2,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
    )
}

@Composable
private fun CenterTip(text: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(text = text, color = ExtendedTheme.colors.textSecondary)
    }
}
