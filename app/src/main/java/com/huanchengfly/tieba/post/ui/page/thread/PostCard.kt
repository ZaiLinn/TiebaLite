package com.huanchengfly.tieba.post.ui.page.thread

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.ProvideTextStyle
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Star
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.fastForEach
import com.huanchengfly.tieba.post.App
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.api.models.protos.Post
import com.huanchengfly.tieba.post.api.models.protos.SubPostList
import com.huanchengfly.tieba.post.api.models.protos.User
import com.huanchengfly.tieba.post.api.models.protos.bawuType
import com.huanchengfly.tieba.post.api.models.protos.plainText
import com.huanchengfly.tieba.post.arch.ImmutableHolder
import com.huanchengfly.tieba.post.ui.common.PbContentRender
import com.huanchengfly.tieba.post.ui.common.PbContentText
import com.huanchengfly.tieba.post.ui.common.theme.compose.ExtendedTheme
import com.huanchengfly.tieba.post.ui.page.LocalNavigator
import com.huanchengfly.tieba.post.ui.widgets.compose.Avatar
import com.huanchengfly.tieba.post.ui.widgets.compose.BlockTip
import com.huanchengfly.tieba.post.ui.widgets.compose.BlockableContent
import com.huanchengfly.tieba.post.ui.widgets.compose.Button
import com.huanchengfly.tieba.post.ui.widgets.compose.Card
import com.huanchengfly.tieba.post.ui.widgets.compose.Chip
import com.huanchengfly.tieba.post.ui.widgets.compose.LongClickMenu
import com.huanchengfly.tieba.post.ui.widgets.compose.Sizes
import com.huanchengfly.tieba.post.ui.widgets.compose.UserHeader
import com.huanchengfly.tieba.post.ui.widgets.compose.buildChipInlineContent
import com.huanchengfly.tieba.post.ui.widgets.compose.rememberMenuState
import com.huanchengfly.tieba.post.utils.DateTimeUtils.getRelativeTimeString
import com.huanchengfly.tieba.post.utils.StringUtil
import com.huanchengfly.tieba.post.utils.StringUtil.getShortNumString
import com.huanchengfly.tieba.post.utils.TiebaUtil
import com.huanchengfly.tieba.post.utils.Util.getIconColorByLevel
import com.huanchengfly.tieba.post.utils.appPreferences
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.launch

private fun getDescText(
    time: Long?,
    floor: Int,
    ipAddress: String?
): String {
    val texts = listOfNotNull(
        time?.let { getRelativeTimeString(App.INSTANCE, it) },
        if (floor > 1) App.INSTANCE.getString(R.string.tip_post_floor, floor) else null,
        if (ipAddress.isNullOrEmpty()) null else App.INSTANCE.getString(
            R.string.text_ip_location,
            ipAddress
        )
    )
    if (texts.isEmpty()) {
        return ""
    }
    return texts.joinToString(" · ")
}

@Composable
fun PostAgreeBtn(
    hasAgreed: Boolean,
    agreeNum: Long,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val animatedColor by animateColorAsState(
        targetValue = if (hasAgreed) ExtendedTheme.colors.accent else ExtendedTheme.colors.textSecondary,
        label = "postAgreeBtnColor"
    )
    Button(
        onClick = onClick,
        shape = RoundedCornerShape(4.dp),
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
        colors = ButtonDefaults.buttonColors(
            backgroundColor = ExtendedTheme.colors.background,
            contentColor = animatedColor
        ),
        modifier = modifier
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = if (hasAgreed) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                contentDescription = stringResource(id = R.string.title_agree),
                tint = animatedColor,
                modifier = Modifier.size(16.dp)
            )
            if (agreeNum > 0) {
                Text(
                    text = agreeNum.getShortNumString(),
                    color = animatedColor,
                    style = MaterialTheme.typography.caption,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
fun PostCard(
    postHolder: ImmutableHolder<Post>,
    contentRenders: ImmutableList<PbContentRender>,
    subPosts: ImmutableList<SubPostItemData> = persistentListOf(),
    threadAuthorId: Long = 0L,
    blocked: Boolean = false,
    canDelete: (Post) -> Boolean = { false },
    immersiveMode: Boolean = false,
    isCollected: (Post) -> Boolean = { false },
    showSubPosts: Boolean = true,
    onUserClick: (User) -> Unit = {},
    onAgree: () -> Unit = {},
    onReplyClick: (Post) -> Unit = {},
    onSubPostReplyClick: ((Post, SubPostList) -> Unit)? = null,
    onOpenSubPosts: (subPostId: Long) -> Unit = {},
    onMenuCopyClick: ((String) -> Unit)? = null,
    onMenuFavoriteClick: ((Post) -> Unit)? = null,
    onMenuDeleteClick: ((Post) -> Unit)? = null,
) {
    val context = LocalContext.current
    val navigator = LocalNavigator.current
    val coroutineScope = rememberCoroutineScope()
    val post = remember(postHolder) { postHolder.get() }
    val hasPadding = remember(key1 = postHolder, key2 = immersiveMode) {
        postHolder.get { floor > 1 } && !immersiveMode
    }
    val paddingModifier = Modifier.padding(start = if (hasPadding) Sizes.Small + 8.dp else 0.dp)
    val author = postHolder.get { author!! }
    val showTitle = remember(postHolder) {
        post.title.isNotBlank() && post.floor <= 1 && post.is_ntitle != 1
    }
    val hasAgreed = remember(postHolder) {
        post.agree?.hasAgree == 1
    }
    val agreeNum = remember(postHolder) {
        post.agree?.diffAgreeNum ?: 0L
    }
    val menuState = rememberMenuState()
    BlockableContent(
        blocked = blocked,
        blockedTip = {
            BlockTip {
                Text(
                    text = stringResource(id = R.string.tip_blocked_post, postHolder.get { floor }),
                )
            }
        },
        hideBlockedContent = context.appPreferences.hideBlockedContent || immersiveMode,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp, horizontal = 16.dp)
    ) {
        LongClickMenu(
            menuState = menuState,
            indication = null,
            onClick = {
                onReplyClick(post)
            }.takeIf { !context.appPreferences.hideReply },
            menuContent = {
                if (!context.appPreferences.hideReply) {
                    DropdownMenuItem(
                        onClick = {
                            onReplyClick(post)
                            menuState.expanded = false
                        }
                    ) {
                        Text(text = stringResource(id = R.string.btn_reply))
                    }
                }
                if (onMenuCopyClick != null) {
                    DropdownMenuItem(
                        onClick = {
                            onMenuCopyClick(post.content.plainText)
                            menuState.expanded = false
                        }
                    ) {
                        Text(text = stringResource(id = R.string.menu_copy))
                    }
                }
                DropdownMenuItem(
                    onClick = {
                        coroutineScope.launch {
                            TiebaUtil.reportPost(context, navigator, post.id.toString())
                        }
                        menuState.expanded = false
                    }
                ) {
                    Text(text = stringResource(id = R.string.title_report))
                }
                if (onMenuFavoriteClick != null) {
                    DropdownMenuItem(
                        onClick = {
                            onMenuFavoriteClick(post)
                            menuState.expanded = false
                        }
                    ) {
                        if (isCollected(post)) {
                            Text(text = stringResource(id = R.string.title_collect_on))
                        } else {
                            Text(text = stringResource(id = R.string.title_collect_floor))
                        }
                    }
                }
                if (canDelete(post) && onMenuDeleteClick != null) {
                    DropdownMenuItem(
                        onClick = {
                            onMenuDeleteClick(post)
                            menuState.expanded = false
                        }
                    ) {
                        Text(text = stringResource(id = R.string.title_delete))
                    }
                }
            }
        ) {
            Card(
                header = {
                    if (!immersiveMode) {
                        UserHeader(
                            avatar = {
                                Avatar(
                                    data = StringUtil.getAvatarUrl(author.portrait),
                                    size = Sizes.Small,
                                    contentDescription = null
                                )
                            },
                            name = {
                                UserNameText(
                                    userName = StringUtil.getUsernameAnnotatedString(
                                        LocalContext.current,
                                        author.name,
                                        author.nameShow
                                    ),
                                    userLevel = author.level_id,
                                    isLz = author.id == threadAuthorId,
                                    bawuType = author.bawuType,
                                )
                            },
                            desc = {
                                Text(
                                    text = getDescText(
                                        post.time.toLong(),
                                        post.floor,
                                        author.ip_address
                                    )
                                )
                            },
                            onClick = {
                                onUserClick(author)
                            }
                        ) {
                            if (post.floor > 1) {
                                PostAgreeBtn(
                                    hasAgreed = hasAgreed,
                                    agreeNum = agreeNum,
                                    onClick = onAgree
                                )
                            }
                        }
                    }
                },
                content = {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = paddingModifier
                            .fillMaxWidth()
                    ) {
                        if (showTitle) {
                            Text(
                                text = post.title,
                                style = MaterialTheme.typography.subtitle1,
                                fontSize = 15.sp
                            )
                        }

                        if (isCollected(post)) {
                            Chip(
                                text = stringResource(id = R.string.title_collected_floor),
                                invertColor = true,
                                prefixIcon = {
                                    Icon(
                                        imageVector = Icons.Rounded.Star,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            )
                        }

                        contentRenders.fastForEach { it.Render() }
                    }

                    if (showSubPosts && post.sub_post_number > 0 && subPosts.isNotEmpty() && !immersiveMode) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .then(paddingModifier)
                                .clip(RoundedCornerShape(6.dp))
                                .background(ExtendedTheme.colors.floorCard)
                                .padding(vertical = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            subPosts.fastForEach { item ->
                                BlockableContent(
                                    blocked = item.blocked,
                                    blockedTip = {
                                        Text(
                                            text = stringResource(id = R.string.tip_blocked_sub_post),
                                            style = MaterialTheme.typography.body2.copy(
                                                color = ExtendedTheme.colors.textDisabled,
                                                fontSize = 13.sp
                                            ),
                                            modifier = Modifier.padding(horizontal = 12.dp)
                                        )
                                    },
                                ) {
                                    SubPostItem(
                                        subPostList = item.subPost,
                                        subPostContent = item.subPostContent,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 12.dp),
                                        onReplyClick = {
                                            onSubPostReplyClick?.invoke(post, it)
                                        },
                                        onOpenSubPosts = onOpenSubPosts,
                                        onMenuCopyClick = {
                                            onMenuCopyClick?.invoke(it.content.plainText)
                                        }
                                    )
                                }
                            }

                            if (post.sub_post_number > subPosts.size) {
                                Text(
                                    text = stringResource(
                                        id = R.string.open_all_sub_posts,
                                        post.sub_post_number
                                    ),
                                    style = MaterialTheme.typography.caption,
                                    fontSize = 13.sp,
                                    color = ExtendedTheme.colors.accent,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 2.dp)
                                        .clickable {
                                            onOpenSubPosts(0)
                                        }
                                        .padding(horizontal = 12.dp)
                                )
                            }
                        }
                    }
                }
            )
        }
    }
}

@Composable
private fun SubPostItem(
    subPostList: ImmutableHolder<SubPostList>,
    subPostContent: AnnotatedString,
    modifier: Modifier = Modifier,
    onReplyClick: ((SubPostList) -> Unit)?,
    onOpenSubPosts: (Long) -> Unit,
    onMenuCopyClick: ((SubPostList) -> Unit)?,
) {
    val context = LocalContext.current
    val navigator = LocalNavigator.current
    val coroutineScope = rememberCoroutineScope()
    val menuState = rememberMenuState()
    LongClickMenu(
        menuState = menuState,
        menuContent = {
            if (!context.appPreferences.hideReply) {
                DropdownMenuItem(
                    onClick = {
                        onReplyClick?.invoke(subPostList.get())
                        menuState.expanded = false
                    }
                ) {
                    Text(text = stringResource(id = R.string.title_reply))
                }
            }
            if (onMenuCopyClick != null) {
                DropdownMenuItem(
                    onClick = {
                        onMenuCopyClick(subPostList.get())
                        menuState.expanded = false
                    }
                ) {
                    Text(text = stringResource(id = R.string.menu_copy))
                }
            }
            DropdownMenuItem(
                onClick = {
                    coroutineScope.launch {
                        TiebaUtil.reportPost(context, navigator, subPostList.get { id }.toString())
                    }
                    menuState.expanded = false
                }
            ) {
                Text(text = stringResource(id = R.string.title_report))
            }
        },
        shape = RoundedCornerShape(0),
        onClick = {
            onOpenSubPosts(subPostList.get { id })
        }
    ) {
        ProvideTextStyle(value = MaterialTheme.typography.body2.copy(color = ExtendedTheme.colors.text)) {
            PbContentText(
                text = subPostContent,
                modifier = modifier,
                fontSize = 13.sp,
                emoticonSize = 0.9f,
                overflow = TextOverflow.Ellipsis,
                maxLines = 4,
                lineSpacing = 0.4.sp,
                inlineContent = mapOf(
                    "Lz" to buildChipInlineContent(
                        stringResource(id = R.string.tip_lz),
                        backgroundColor = ExtendedTheme.colors.textSecondary.copy(alpha = 0.1f),
                        color = ExtendedTheme.colors.textSecondary
                    ),
                )
            )
        }
    }
}

@Composable
fun UserNameText(
    userName: AnnotatedString,
    userLevel: Int,
    modifier: Modifier = Modifier,
    isLz: Boolean = false,
    bawuType: String? = null,
) {
    val text = buildAnnotatedString {
        append(userName)
        append(" ")
        if (userLevel > 0) appendInlineContent("Level", alternateText = "$userLevel")
        if (!bawuType.isNullOrBlank()) {
            append(" ")
            appendInlineContent("Bawu", alternateText = bawuType)
        }
        if (isLz) {
            append(" ")
            appendInlineContent("Lz")
        }
    }
    Text(
        text = text,
        inlineContent = mapOf(
            "Level" to buildChipInlineContent(
                "18",
                color = Color(getIconColorByLevel("$userLevel")),
                backgroundColor = Color(getIconColorByLevel("$userLevel")).copy(alpha = 0.25f)
            ),
            "Bawu" to buildChipInlineContent(
                bawuType ?: "",
                color = ExtendedTheme.colors.primary,
                backgroundColor = ExtendedTheme.colors.primary.copy(alpha = 0.1f)
            ),
            "Lz" to buildChipInlineContent(stringResource(id = R.string.tip_lz)),
        ),
        modifier = modifier
    )
}
