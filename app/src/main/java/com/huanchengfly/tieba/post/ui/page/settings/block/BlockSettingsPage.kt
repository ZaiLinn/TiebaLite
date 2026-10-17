package com.huanchengfly.tieba.post.ui.page.settings.block

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.FilterAlt
import androidx.compose.material.icons.outlined.HideSource
import androidx.compose.material.icons.outlined.HowToVote
import androidx.compose.material.icons.outlined.Redeem
import androidx.compose.material.icons.outlined.ShoppingBag
import androidx.compose.material.icons.outlined.SportsEsports
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material.icons.outlined.VideocamOff
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.dataStore
import com.huanchengfly.tieba.post.ui.common.prefs.PrefsScreen
import com.huanchengfly.tieba.post.ui.common.prefs.widgets.SliderPref
import com.huanchengfly.tieba.post.ui.common.prefs.widgets.SwitchPref
import com.huanchengfly.tieba.post.ui.common.prefs.widgets.TextPref
import com.huanchengfly.tieba.post.ui.page.settings.LeadingIcon
import com.huanchengfly.tieba.post.ui.widgets.compose.AvatarIcon
import com.huanchengfly.tieba.post.ui.widgets.compose.BackNavigationIcon
import com.huanchengfly.tieba.post.ui.widgets.compose.MyScaffold
import com.huanchengfly.tieba.post.ui.widgets.compose.Sizes
import com.huanchengfly.tieba.post.ui.widgets.compose.TitleCentredToolbar
import com.huanchengfly.tieba.post.utils.CommentLevelFilter
import com.huanchengfly.tieba.post.utils.appPreferences

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun BlockSettingsPage(
    navigator: NavHostController
) {
    val context = LocalContext.current
    // 等级过滤关闭时，阈值和附加选项置灰
    var levelFilterEnabled by remember { mutableStateOf(context.appPreferences.commentLevelFilter) }
    MyScaffold(
        backgroundColor = Color.Transparent,
        topBar = {
            TitleCentredToolbar(
                title = {
                    Text(
                        text = stringResource(id = R.string.title_block_settings),
                        fontWeight = FontWeight.Bold, style = MaterialTheme.typography.h6
                    )
                },
                navigationIcon = {
                    BackNavigationIcon(onBackPressed = { navigator.popBackStack() })
                }
            )
        },
    ) { paddingValues ->
        PrefsScreen(
            dataStore = context.dataStore,
            dividerThickness = 0.dp,
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize(),
        ) {
            prefsItem {
                TextPref(
                    title = stringResource(id = R.string.title_block_list),
                    leadingIcon = {
                        LeadingIcon {
                            AvatarIcon(
                                icon = Icons.Outlined.Block,
                                size = Sizes.Small,
                                contentDescription = null,
                            )
                        }
                    },
                    onClick = { navigator.navigate("settings/block/list") }
                )
            }
            prefsItem {
                SwitchPref(
                    key = "hideBlockedContent",
                    title = stringResource(id = R.string.settings_hide_blocked_content),
                    defaultChecked = false
                ) {
                    LeadingIcon {
                        AvatarIcon(
                            icon = Icons.Outlined.HideSource,
                            size = Sizes.Small,
                            contentDescription = null,
                        )
                    }
                }
            }
            prefsItem {
                SwitchPref(
                    key = "blockVideo",
                    title = stringResource(id = R.string.settings_block_video),
                    summary = stringResource(id = R.string.settings_block_video_summary),
                    defaultChecked = false,
                    leadingIcon = {
                        LeadingIcon {
                            AvatarIcon(
                                icon = Icons.Outlined.VideocamOff,
                                size = Sizes.Small,
                                contentDescription = null,
                            )
                        }
                    }
                )
            }
            prefsItem {
                SwitchPref(
                    key = "blockVoteThread",
                    title = stringResource(id = R.string.settings_block_vote_thread),
                    summary = stringResource(id = R.string.settings_block_vote_thread_summary),
                    defaultChecked = false,
                    leadingIcon = {
                        LeadingIcon {
                            AvatarIcon(
                                icon = Icons.Outlined.HowToVote,
                                size = Sizes.Small,
                                contentDescription = null,
                            )
                        }
                    }
                )
            }
            prefsItem {
                SwitchPref(
                    key = "blockGoodsThread",
                    title = stringResource(id = R.string.settings_block_goods_thread),
                    summary = stringResource(id = R.string.settings_block_goods_thread_summary),
                    defaultChecked = false,
                    leadingIcon = {
                        LeadingIcon {
                            AvatarIcon(
                                icon = Icons.Outlined.ShoppingBag,
                                size = Sizes.Small,
                                contentDescription = null,
                            )
                        }
                    }
                )
            }
            prefsItem {
                SwitchPref(
                    key = "blockAppPromotionThread",
                    title = stringResource(id = R.string.settings_block_app_promotion_thread),
                    summary = stringResource(id = R.string.settings_block_app_promotion_thread_summary),
                    defaultChecked = false,
                    leadingIcon = {
                        LeadingIcon {
                            AvatarIcon(
                                icon = Icons.Outlined.SportsEsports,
                                size = Sizes.Small,
                                contentDescription = null,
                            )
                        }
                    }
                )
            }
            prefsItem {
                SwitchPref(
                    key = "blockLotteryThread",
                    title = stringResource(id = R.string.settings_block_lottery_thread),
                    summary = stringResource(id = R.string.settings_block_lottery_thread_summary),
                    defaultChecked = false,
                    leadingIcon = {
                        LeadingIcon {
                            AvatarIcon(
                                icon = Icons.Outlined.Redeem,
                                size = Sizes.Small,
                                contentDescription = null,
                            )
                        }
                    }
                )
            }
            prefsItem {
                SwitchPref(
                    key = "blockScoreThread",
                    title = stringResource(id = R.string.settings_block_score_thread),
                    summary = stringResource(id = R.string.settings_block_score_thread_summary),
                    defaultChecked = false,
                    leadingIcon = {
                        LeadingIcon {
                            AvatarIcon(
                                icon = Icons.Outlined.StarOutline,
                                size = Sizes.Small,
                                contentDescription = null,
                            )
                        }
            prefsItem {
                SwitchPref(
                    key = "commentLevelFilter",
                    title = stringResource(id = R.string.settings_comment_level_filter),
                    summary = stringResource(id = R.string.settings_comment_level_filter_summary),
                    defaultChecked = false,
                    onCheckedChange = { levelFilterEnabled = it },
                    leadingIcon = {
                        LeadingIcon {
                            AvatarIcon(
                                icon = Icons.Outlined.FilterAlt,
                                size = Sizes.Small,
                                contentDescription = null,
                            )
                        }
                    }
                )
            }
            prefsItem {
                SliderPref(
                    key = "commentFilterMinimumLevel",
                    title = stringResource(id = R.string.settings_comment_filter_minimum_level),
                    defaultValue = CommentLevelFilter.DEFAULT_MINIMUM_LEVEL.toFloat(),
                    valueRange = CommentLevelFilter.MIN_LEVEL.toFloat()..CommentLevelFilter.MAX_LEVEL.toFloat(),
                    steps = CommentLevelFilter.MAX_LEVEL - CommentLevelFilter.MIN_LEVEL - 1,
                    showValue = true,
                    enabled = levelFilterEnabled,
                )
            }
            prefsItem {
                SwitchPref(
                    key = "commentFilterKeepWithReplies",
                    title = stringResource(id = R.string.settings_comment_filter_keep_with_replies),
                    summary = stringResource(id = R.string.settings_comment_filter_keep_with_replies_summary),
                    defaultChecked = false,
                    enabled = levelFilterEnabled,
                )
            }
            prefsItem {
                SwitchPref(
                    key = "commentFilterSkipNested",
                    title = stringResource(id = R.string.settings_comment_filter_skip_nested),
                    summary = stringResource(id = R.string.settings_comment_filter_skip_nested_summary),
                    defaultChecked = false,
                    enabled = levelFilterEnabled,
                )
            }
                    }
                )
            }
        }
    }
}