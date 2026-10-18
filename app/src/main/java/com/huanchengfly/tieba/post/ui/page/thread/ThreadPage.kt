package com.huanchengfly.tieba.post.ui.page.thread

import android.net.Uri
import android.util.Log
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.ModalBottomSheetLayout
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material.ProvideTextStyle
import androidx.compose.material.SnackbarDuration
import androidx.compose.material.SnackbarResult
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AlignVerticalTop
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material.rememberModalBottomSheetState
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.rememberLottieComposition
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.api.booleanToString
import com.huanchengfly.tieba.post.api.models.protos.Post
import com.huanchengfly.tieba.post.api.models.protos.ThreadInfo
import com.huanchengfly.tieba.post.api.models.protos.User
import com.huanchengfly.tieba.post.arch.AppScope
import com.huanchengfly.tieba.post.arch.GlobalEvent
import com.huanchengfly.tieba.post.arch.ImmutableHolder
import com.huanchengfly.tieba.post.arch.collectPartialAsState
import com.huanchengfly.tieba.post.arch.onEvent
import com.huanchengfly.tieba.post.arch.onGlobalEvent
import com.huanchengfly.tieba.post.arch.pageViewModel
import com.huanchengfly.tieba.post.arch.wrapImmutable
import com.huanchengfly.tieba.post.models.ThreadHistoryInfoBean
import com.huanchengfly.tieba.post.models.database.History
import com.huanchengfly.tieba.post.toJson
import com.huanchengfly.tieba.post.ui.common.PbContentRender
import com.huanchengfly.tieba.post.ui.common.PicContentRender
import com.huanchengfly.tieba.post.ui.common.theme.compose.ExtendedTheme
import com.huanchengfly.tieba.post.ui.common.theme.compose.loadMoreIndicator
import com.huanchengfly.tieba.post.ui.common.theme.compose.pullRefreshIndicator
import com.huanchengfly.tieba.post.ui.page.ProvideNavigator
import com.huanchengfly.tieba.post.ui.widgets.compose.Button
import com.huanchengfly.tieba.post.ui.widgets.compose.Container
import com.huanchengfly.tieba.post.ui.widgets.compose.ErrorScreen
import com.huanchengfly.tieba.post.ui.widgets.compose.HorizontalDivider
import com.huanchengfly.tieba.post.ui.widgets.compose.LazyLoad
import com.huanchengfly.tieba.post.ui.widgets.compose.LoadMoreLayout
import com.huanchengfly.tieba.post.ui.widgets.compose.MyBackHandler
import com.huanchengfly.tieba.post.ui.widgets.compose.MyLazyColumn
import com.huanchengfly.tieba.post.ui.widgets.compose.MyScaffold
import com.huanchengfly.tieba.post.ui.widgets.compose.OriginThreadCard
import com.huanchengfly.tieba.post.ui.widgets.compose.TipScreen
import com.huanchengfly.tieba.post.ui.widgets.compose.VerticalDivider
import com.huanchengfly.tieba.post.ui.widgets.compose.rememberBlockRulesChangeCount
import com.huanchengfly.tieba.post.ui.widgets.compose.rememberBlocked
import com.huanchengfly.tieba.post.ui.widgets.compose.rememberDialogState
import com.huanchengfly.tieba.post.ui.widgets.compose.states.StateScreen
import com.huanchengfly.tieba.post.utils.BlockManager.shouldBlock
import com.huanchengfly.tieba.post.utils.HistoryUtil
import com.huanchengfly.tieba.post.utils.StringUtil
import com.huanchengfly.tieba.post.utils.TiebaUtil
import com.huanchengfly.tieba.post.utils.appPreferences
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlin.math.max

object ThreadPageFrom {
    const val FROM_FORUM = "forum"

    // 收藏
    const val FROM_STORE = "store_thread"
    const val FROM_PERSONALIZED = "personalized"
    const val FROM_HISTORY = "history"
}

@Serializable
sealed interface ThreadPageExtra

@Serializable
data object ThreadPageNoExtra : ThreadPageExtra

@Serializable
data class ThreadPageFromStoreExtra(
    val maxPid: Long,
    val maxFloor: Int,
) : ThreadPageExtra

@Composable
private fun ThreadLoadMoreIndicator(
    isLoading: Boolean,
    loadMoreEnd: Boolean,
    willLoad: Boolean,
    hasMore: Boolean,
) {
    Surface(
        elevation = 8.dp,
        shape = RoundedCornerShape(100),
        color = ExtendedTheme.colors.loadMoreIndicator,
        contentColor = ExtendedTheme.colors.text
    ) {
        Row(
            modifier = Modifier
                .height(IntrinsicSize.Min)
                .padding(10.dp)
                .animateContentSize(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ProvideTextStyle(value = MaterialTheme.typography.body2.copy(fontSize = 13.sp)) {
                when {
                    isLoading -> {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 3.dp,
                            color = ExtendedTheme.colors.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(id = R.string.text_loading),
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )
                    }

                    loadMoreEnd -> {
                        Text(
                            text = stringResource(id = R.string.no_more),
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )
                    }

                    hasMore -> {
                        Text(
                            text = if (willLoad) stringResource(id = R.string.release_to_load) else stringResource(
                                id = R.string.pull_to_load
                            ),
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )
                    }

                    else -> {
                        Text(
                            text = if (willLoad) stringResource(id = R.string.release_to_load_latest_posts) else stringResource(
                                id = R.string.pull_to_load_latest_posts
                            ),
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )
                    }
                }
            }
        }
    }
}

private const val HISTORY_SAVE_DEBOUNCE_MS = 800L

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterialApi::class, FlowPreview::class)
@Composable
fun ThreadPage(
    threadId: Long,
    navigator: NavHostController,
    forumId: Long? = null,
    postId: Long = 0,
    seeLz: Boolean = false,
    sortType: Int = 0,
    from: String = "",
    extra: ThreadPageExtra? = null,
    threadInfo: ThreadInfo? = null,
    scrollToReply: Boolean = false,
    viewModel: ThreadViewModel = pageViewModel(),
) {
    val appPreferences = LocalContext.current.appPreferences
    val effectiveSeeLz =
        if (from == ThreadPageFrom.FROM_STORE) appPreferences.collectThreadSeeLz else seeLz
    val effectiveSortType =
        if (from == ThreadPageFrom.FROM_STORE) {
            if (appPreferences.collectThreadDescSort) {
                ThreadSortType.SORT_TYPE_DESC
            } else {
                ThreadSortType.SORT_TYPE_DEFAULT
            }
        } else sortType
    LazyLoad(loaded = viewModel.initialized) {
        viewModel.send(
            ThreadUiIntent.Init(
                threadId,
                forumId,
                postId,
                threadInfo,
                effectiveSeeLz,
                effectiveSortType
            )
        )
        viewModel.send(
            ThreadUiIntent.Load(
                threadId,
                page = 0,
                postId = postId,
                forumId = forumId,
                seeLz = effectiveSeeLz,
                sortType = effectiveSortType,
                from = from
            )
        )
        viewModel.initialized = true
    }
    val scaffoldState = rememberScaffoldState()
    val data by viewModel.uiState.collectPartialAsState(
        prop1 = ThreadUiState::data,
        initial = persistentListOf()
    )
    val author by viewModel.uiState.collectPartialAsState(
        prop1 = ThreadUiState::author,
        initial = null
    )
    val thread by viewModel.uiState.collectPartialAsState(
        prop1 = ThreadUiState::threadInfo,
        initial = null
    )
    val firstPost by viewModel.uiState.collectPartialAsState(
        prop1 = ThreadUiState::firstPost,
        initial = null
    )
    val forum by viewModel.uiState.collectPartialAsState(
        prop1 = ThreadUiState::forum,
        initial = null
    )
    val user by viewModel.uiState.collectPartialAsState(
        prop1 = ThreadUiState::user,
        initial = wrapImmutable(User())
    )
    val blockRulesChangeCount = rememberBlockRulesChangeCount()
    val anti by viewModel.uiState.collectPartialAsState(
        prop1 = ThreadUiState::anti,
        initial = null
    )
    val firstPostContentRenders by viewModel.uiState.collectPartialAsState(
        prop1 = ThreadUiState::firstPostContentRenders,
        initial = persistentListOf()
    )
    val isRefreshing by viewModel.uiState.collectPartialAsState(
        prop1 = ThreadUiState::isRefreshing,
        initial = false
    )
    val isLoadingMore by viewModel.uiState.collectPartialAsState(
        prop1 = ThreadUiState::isLoadingMore,
        initial = false
    )
    val isError by viewModel.uiState.collectPartialAsState(
        prop1 = ThreadUiState::isError,
        initial = false
    )
    val error by viewModel.uiState.collectPartialAsState(
        prop1 = ThreadUiState::error,
        initial = null
    )
    val hasMore by viewModel.uiState.collectPartialAsState(
        prop1 = ThreadUiState::hasMore,
        initial = true
    )
    val nextPagePostId by viewModel.uiState.collectPartialAsState(
        prop1 = ThreadUiState::nextPagePostId,
        initial = 0L
    )
    val hasPrevious by viewModel.uiState.collectPartialAsState(
        prop1 = ThreadUiState::hasPrevious,
        initial = true
    )
    val currentPageMax by viewModel.uiState.collectPartialAsState(
        prop1 = ThreadUiState::currentPageMax,
        initial = 0
    )
    val totalPage by viewModel.uiState.collectPartialAsState(
        prop1 = ThreadUiState::totalPage,
        initial = 0
    )
    val isSeeLz by viewModel.uiState.collectPartialAsState(
        prop1 = ThreadUiState::seeLz,
        initial = effectiveSeeLz
    )
    val curSortType by viewModel.uiState.collectPartialAsState(
        prop1 = ThreadUiState::sortType,
        initial = effectiveSortType
    )
    val isImmersiveMode by viewModel.uiState.collectPartialAsState(
        prop1 = ThreadUiState::isImmersiveMode,
        initial = false
    )
    val latestPosts by viewModel.uiState.collectPartialAsState(
        prop1 = ThreadUiState::latestPosts,
        initial = persistentListOf()
    )

    val isEmpty by remember {
        derivedStateOf { data.isEmpty() && firstPost == null }
    }
    val enablePullRefresh by remember {
        derivedStateOf {
            hasPrevious || curSortType == ThreadSortType.SORT_TYPE_DESC
        }
    }
    val loadMoreEnd by remember {
        derivedStateOf {
            !hasMore && curSortType == ThreadSortType.SORT_TYPE_DESC
        }
    }
    val loadMorePreloadCount by remember {
        derivedStateOf {
            // 还剩 5 层楼时就开始加载下一页；原来是 1，要等最后一层露出来才请求
            if (hasMore) 5 else 0
        }
    }
    val isCollected = remember(thread) {
        thread?.get { collectStatus != 0 } == true
    }
    val hasThreadAgreed = remember(thread) {
        thread?.get { agree?.hasAgree == 1 } == true
    }
    val threadAgreeNum = remember(thread) {
        thread?.get { agree?.diffAgreeNum } ?: 0L
    }
    val threadTitle = remember(thread) {
        thread?.get { title } ?: ""
    }
    val curForumId = remember(forumId, forum) {
        forumId ?: forum?.get { id }
    }
    val curForumName = remember(forum) { forum?.get { name } }
    val curTbs = remember(anti) { anti?.get { tbs } }
    var waitLoadSuccessAndScrollToFirstReply by remember { mutableStateOf(scrollToReply) }

    val lazyListState = rememberSaveable(saver = LazyListState.Saver) { LazyListState() }
    val bottomSheetState = rememberModalBottomSheetState(
        initialValue = ModalBottomSheetValue.Hidden,
        skipHalfExpanded = true
    )
    // 滑动时每帧都会重算：先从列表项 key 解析出楼层 id 再按 id 查找，
    // 不要对每层楼都 toString 再比较字符串
    val lastVisibilityPost by remember {
        derivedStateOf {
            val lastPostId = lazyListState.layoutInfo.visibleItemsInfo
                .lastOrNull { info -> (info.key as? String)?.startsWith("Post_") == true }
                ?.let { (it.key as String).removePrefix("Post_").toLongOrNull() }
            lastPostId?.let { id -> data.firstOrNull { (post) -> post.get { this.id } == id }?.post } ?: firstPost
        }
    }
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    val openBottomSheet = {
        coroutineScope.launch {
            bottomSheetState.show()
        }
    }
    val closeBottomSheet = {
        coroutineScope.launch {
            bottomSheetState.hide()
        }
    }

    MyBackHandler(
        enabled = bottomSheetState.isVisible,
        currentScreenRoute = "thread/{threadId}"
    ) {
        closeBottomSheet()
    }

    viewModel.onEvent<ThreadUiEvent.ScrollToFirstReply> {
        lazyListState.animateScrollToItem(1)
    }
    viewModel.onEvent<ThreadUiEvent.ScrollToLatestReply> {
        if (curSortType != ThreadSortType.SORT_TYPE_DESC) {
            lazyListState.animateScrollToItem(2 + data.size)
        } else {
            lazyListState.animateScrollToItem(1)
        }
    }
    viewModel.onEvent<ThreadUiEvent.LoadSuccess> {
        if (it.page > 1 || waitLoadSuccessAndScrollToFirstReply) {
            waitLoadSuccessAndScrollToFirstReply = false
            lazyListState.animateScrollToItem(1)
        }
    }
    viewModel.onEvent<ThreadUiEvent.AddFavoriteSuccess> {
        scaffoldState.snackbarHostState.showSnackbar(
            context.getString(R.string.message_add_favorite_success, it.floor)
        )
    }
    viewModel.onEvent<ThreadUiEvent.RemoveFavoriteSuccess> {
        scaffoldState.snackbarHostState.showSnackbar(
            context.getString(R.string.message_remove_favorite_success)
        )
    }

    onGlobalEvent<GlobalEvent.ReplySuccess>(
        filter = { it.threadId == threadId }
    ) { event ->
        viewModel.send(
            ThreadUiIntent.LoadMyLatestReply(
                threadId = threadId,
                postId = event.newPostId,
                forumId = curForumId,
                isDesc = curSortType == ThreadSortType.SORT_TYPE_DESC,
                curLatestPostFloor = if (curSortType == ThreadSortType.SORT_TYPE_DESC) {
                    data.firstOrNull()?.post?.get { floor } ?: 1
                } else {
                    data.lastOrNull()?.post?.get { floor } ?: 1
                },
                curPostIds = data.map { it.post.get { id } },
            )
        )
    }

    val updateCollectMarkDialogState = rememberDialogState()
    var readFloorBeforeBack by remember {
        mutableIntStateOf(1)
    }
    UpdateCollectMarkDialog(
        dialogState = updateCollectMarkDialogState,
        readFloor = readFloorBeforeBack,
        threadId = threadId,
        markPostId = { lastVisibilityPost?.get { id } ?: 0L },
        onDismiss = { navigator.popBackStack() }
    )
    MyBackHandler(
        enabled = isCollected && !bottomSheetState.isVisible,
        currentScreenRoute = "thread/{threadId}"
    ) {
        readFloorBeforeBack = lastVisibilityPost?.get { floor } ?: 0
        if (readFloorBeforeBack != 0) {
            updateCollectMarkDialogState.show()
        } else {
            navigator.popBackStack()
        }
    }

    val confirmDeleteDialogState = rememberDialogState()
    var deletePost by remember { mutableStateOf<ImmutableHolder<Post>?>(null) }
    DeleteConfirmDialog(
        dialogState = confirmDeleteDialogState,
        deletePost = deletePost,
        onConfirm = {
            curForumId ?: return@DeleteConfirmDialog
            viewModel.send(
                buildDeleteIntent(
                    deletePost = deletePost?.get(),
                    forumId = curForumId,
                    forumName = curForumName.orEmpty(),
                    threadId = threadId,
                    threadAuthorId = author?.get { id },
                    userId = user.get { id },
                    tbs = curTbs
                )
            )
        }
    )

    val jumpToPageDialogState = rememberDialogState()
    JumpToPageDialog(
        dialogState = jumpToPageDialogState,
        currentPageMax = currentPageMax,
        totalPage = totalPage,
        onJump = { page ->
            viewModel.send(
                ThreadUiIntent.Load(
                    threadId = threadId,
                    forumId = forum?.get { id } ?: forumId,
                    page = page,
                    seeLz = isSeeLz,
                    sortType = curSortType,
                    from = from
                )
            )
        }
    )

    LaunchedEffect(Unit) {
        if (from == ThreadPageFrom.FROM_STORE && extra is ThreadPageFromStoreExtra && extra.maxPid != postId) {
            val result = scaffoldState.snackbarHostState.showSnackbar(
                context.getString(R.string.message_store_thread_update, extra.maxFloor),
                context.getString(R.string.button_load_new),
                SnackbarDuration.Long
            )
            if (result == SnackbarResult.ActionPerformed) {
                viewModel.send(
                    ThreadUiIntent.Load(
                        threadId,
                        page = 0,
                        postId = extra.maxPid,
                        forumId = curForumId,
                        seeLz = isSeeLz,
                        sortType = curSortType,
                        from = from
                    )
                )
            }
        }
    }

    // 浏览历史：最后可见楼层变化后防抖再保存。原来每出现一层新楼就新开线程写一次数据库，
    // 而且在组合阶段读取最后可见楼层，滑动时整页跟着重组
    val latestThreadTitle by rememberUpdatedState(threadTitle)
    LaunchedEffect(threadId) {
        snapshotFlow {
            val post = lastVisibilityPost
            Triple(latestThreadTitle, post?.get { id } ?: 0L, post?.get { floor })
        }
            .filter { (title) -> title.isNotBlank() }
            .distinctUntilChanged()
            .debounce(HISTORY_SAVE_DEBOUNCE_MS)
            .collect { (title, postId, floor) ->
                val history = History(
                    title = title,
                    data = threadId.toString(),
                    type = HistoryUtil.TYPE_THREAD,
                    extras = ThreadHistoryInfoBean(
                        isSeeLz = isSeeLz,
                        pid = postId.toString(),
                        forumName = forum?.get { name },
                        floor = floor?.toString()
                    ).toJson(),
                    avatar = StringUtil.getAvatarUrl(author?.get { portrait }),
                    username = author?.get { nameShow }
                )
                AppScope.launch(Dispatchers.IO) {
                    runCatching { HistoryUtil.saveHistory(history, async = true) }
                    Log.i("ThreadPage", "saveHistory $postId")
                }
            }
    }

    val pullRefreshState = rememberPullRefreshState(
        refreshing = isRefreshing,
        onRefresh = {
            viewModel.send(
                ThreadUiIntent.LoadFirstPage(
                    threadId,
                    curForumId,
                    isSeeLz,
                    curSortType
                )
            )
        }
    )

    @Composable
    fun PostCard(
        item: ImmutableHolder<Post>,
        contentRenders: ImmutableList<PbContentRender>,
        subPosts: ImmutableList<SubPostItemData>,
        blocked: Boolean,
    ) {
        PostCard(
            postHolder = item,
            contentRenders = contentRenders,
            subPosts = subPosts,
            threadAuthorId = author?.get { id } ?: 0L,
            blocked = rememberBlocked(item, blocked, blockRulesChangeCount) { item.get { shouldBlock() } },
            currentUserId = user.get { id },
            blockRulesChangeCount = blockRulesChangeCount,
            canDelete = { it.author_id == user.get { id } },
            immersiveMode = isImmersiveMode,
            isCollected = { it.id == thread?.get { collectMarkPid.toLongOrNull() } },
            onUserClick = {
                navigator.navigate("user/${it.id}")
            },
            onAgree = {
                val postHasAgreed =
                    item.get { agree?.hasAgree == 1 }
                viewModel.send(
                    ThreadUiIntent.AgreePost(
                        threadId = threadId,
                        postId = item.get { id },
                        agree = !postHasAgreed
                    )
                )
            },
            onReplyClick = { post ->
                navigator.navigate("reply/$threadId?forumId=${curForumId ?: 0}&forumName=${Uri.encode(forum?.get { name }.orEmpty())}&postId=${post.id}")
            },
            onSubPostReplyClick = { post, subPost ->
                navigator.navigate("reply/$threadId?postId=${post.id}&subPostId=${subPost.id}")
            },
            onOpenSubPosts = {
                if (curForumId != null) {
                    navigator.navigate("subposts/$threadId?forumId=$curForumId&postId=${item.get { id }}&subPostId=$it")
                }
            },
            onMenuCopyClick = {
                navigator.navigate("copy_dialog/${Uri.encode(it)}")
            },
            onMenuFavoriteClick = {
                val isPostCollected =
                    it.id == thread?.get { collectMarkPid.toLongOrNull() }
                val fid = forum?.get { id } ?: forumId
                val tbs = anti?.get { tbs }
                if (fid != null) {
                    if (isPostCollected) {
                        viewModel.send(
                            ThreadUiIntent.RemoveFavorite(
                                threadId = threadId,
                                forumId = fid,
                                tbs = tbs
                            )
                        )
                    } else {
                        viewModel.send(
                            ThreadUiIntent.AddFavorite(
                                threadId = threadId,
                                postId = it.id,
                                floor = it.floor
                            )
                        )
                    }
                }
            },
            onMenuDeleteClick = {
                deletePost = it.wrapImmutable()
                confirmDeleteDialogState.show()
            },
        )
    }

    fun LazyListScope.latestPosts(desc: Boolean) {
        if (latestPosts.isNotEmpty()) {
            if (!desc) {
                item("LatestPostsTip") {
                    Container {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            VerticalDivider(modifier = Modifier.weight(1f))
                            Text(
                                text = stringResource(id = R.string.below_is_latest_post),
                                color = ExtendedTheme.colors.textSecondary,
                                style = MaterialTheme.typography.caption,
                            )
                            VerticalDivider(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
            items(
                items = latestPosts,
                key = { (item) -> "LatestPost_${item.get { id }}" },
                // 带图和纯文字楼层结构差别大，分开复用
                contentType = { (_, _, renders) -> if (renders.any { it is PicContentRender }) "PostWithPic" else "Post" },
            ) { (item, blocked, renders, subPosts) ->
                Container {
                    PostCard(
                        item,
                        renders,
                        subPosts,
                        blocked
                    )
                }
            }
            if (desc) {
                item("LatestPostsTip") {
                    Container {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            VerticalDivider(modifier = Modifier.weight(1f))
                            Text(
                                text = stringResource(id = R.string.above_is_latest_post),
                                color = ExtendedTheme.colors.textSecondary,
                                style = MaterialTheme.typography.caption,
                            )
                            VerticalDivider(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }

    ProvideNavigator(navigator = navigator) {
        StateScreen(
            modifier = Modifier.fillMaxSize(),
            isEmpty = isEmpty,
            isError = isError,
            isLoading = isRefreshing,
            errorScreen = {
                error?.let {
                    val (e) = it
                    ErrorScreen(error = e)
                }
            },
            onReload = {
                viewModel.send(
                    ThreadUiIntent.Load(
                        threadId,
                        page = 0,
                        postId = postId,
                        forumId = curForumId,
                        seeLz = isSeeLz,
                        sortType = curSortType,
                        from = from
                    )
                )
            }
        ) {
            MyScaffold(
                scaffoldState = scaffoldState,
                topBar = {
                    TopBar(
                        forum = forum,
                        onBack = { navigator.popBackStack() },
                        onForumClick = {
                            val forumName = forum?.get { name }
                            if (forumName != null) navigator.navigate("forum/$forumName")
                        }
                    )
                },
                bottomBar = {
                    BottomBar(
                        user = user,
                        onClickReply = {
                            navigator.navigate("reply/$threadId?forumId=${curForumId ?: 0}&forumName=${Uri.encode(forum?.get { name }.orEmpty())}")
                        },
                        onAgree = {
                            val firstPostId =
                                thread?.get { firstPostId }.takeIf { it != 0L }
                                    ?: firstPost?.get { id }
                                    ?: 0L
                            if (firstPostId != 0L) viewModel.send(
                                ThreadUiIntent.AgreeThread(
                                    threadId,
                                    firstPostId,
                                    !hasThreadAgreed
                                )
                            )
                        },
                        onClickMore = {
                            if (bottomSheetState.isVisible) {
                                closeBottomSheet()
                            } else {
                                openBottomSheet()
                            }
                        },
                        hasAgreed = hasThreadAgreed,
                        agreeNum = threadAgreeNum,
                        modifier = Modifier
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = {}
                            )
                    )
                },
            ) { paddingValues ->
                ModalBottomSheetLayout(
                    sheetState = bottomSheetState,
                    sheetShape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp),
                    sheetBackgroundColor = ExtendedTheme.colors.windowBackground,
                    sheetContent = {
                        ThreadMenu(
                            isSeeLz = isSeeLz,
                            isCollected = isCollected,
                            isImmersiveMode = isImmersiveMode,
                            isDesc = curSortType == ThreadSortType.SORT_TYPE_DESC,
                            canDelete = { author?.get { id } == user.get { id } },
                            onSeeLzClick = {
                                viewModel.send(
                                    ThreadUiIntent.LoadFirstPage(
                                        threadId,
                                        curForumId,
                                        !isSeeLz,
                                        curSortType
                                    )
                                )
                                closeBottomSheet()
                            },
                            onCollectClick = {
                                if (isCollected) {
                                    val fid = forum?.get { id } ?: forumId
                                    val tbs = anti?.get { tbs }
                                    if (fid != null) {
                                        viewModel.send(
                                            ThreadUiIntent.RemoveFavorite(
                                                threadId,
                                                fid,
                                                tbs
                                            )
                                        )
                                    }
                                } else {
                                    val readItem = lastVisibilityPost
                                    if (readItem != null) {
                                        viewModel.send(
                                            ThreadUiIntent.AddFavorite(
                                                threadId,
                                                readItem.get { id },
                                                readItem.get { floor }
                                            )
                                        )
                                    }
                                }
                                closeBottomSheet()
                            },
                            onImmersiveModeClick = {
                                if (!isImmersiveMode && !isSeeLz) {
                                    viewModel.send(
                                        ThreadUiIntent.LoadFirstPage(
                                            threadId,
                                            curForumId,
                                            true,
                                            curSortType
                                        )
                                    )
                                }
                                viewModel.send(ThreadUiIntent.ToggleImmersiveMode(!isImmersiveMode))
                                closeBottomSheet()
                            },
                            onDescClick = {
                                viewModel.send(
                                    ThreadUiIntent.LoadFirstPage(
                                        threadId,
                                        curForumId,
                                        isSeeLz,
                                        if (curSortType != ThreadSortType.SORT_TYPE_DESC) ThreadSortType.SORT_TYPE_DESC else ThreadSortType.SORT_TYPE_DEFAULT
                                    )
                                )
                                closeBottomSheet()
                            },
                            onJumpPageClick = {
                                closeBottomSheet()
                                jumpToPageDialogState.show()
                            },
                            onShareClick = {
                                TiebaUtil.shareText(
                                    context,
                                    "https://tieba.baidu.com/p/$threadId",
                                    threadTitle
                                )
                            },
                            onCopyLinkClick = {
                                TiebaUtil.copyText(
                                    context,
                                    "https://tieba.baidu.com/p/$threadId?see_lz=${isSeeLz.booleanToString()}"
                                )
                            },
                            onReportClick = {
                                val firstPostId =
                                    thread?.get { firstPostId }.takeIf { it != 0L }
                                        ?: firstPost?.get { id }
                                        ?: 0L
                                coroutineScope.launch {
                                    TiebaUtil.reportPost(
                                        context,
                                        navigator,
                                        firstPostId.toString()
                                    )
                                }
                            },
                            onDeleteClick = {
                                deletePost = null
                                confirmDeleteDialogState.show()
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 16.dp)
                                .defaultMinSize(minHeight = 1.dp)
                        )
                    },
                    scrimColor = Color.Transparent,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                ) {
                    Box(
                        modifier = Modifier
                            .pullRefresh(state = pullRefreshState, enabled = enablePullRefresh)
                    ) {
                        LoadMoreLayout(
                            isLoading = isLoadingMore,
                            onLoadMore = {
                                if (hasMore) {
                                    viewModel.send(
                                        ThreadUiIntent.LoadMore(
                                            threadId = threadId,
                                            page = if (curSortType == ThreadSortType.SORT_TYPE_DESC) totalPage - currentPageMax
                                            else currentPageMax + 1,
                                            forumId = curForumId,
                                            postId = nextPagePostId,
                                            seeLz = isSeeLz,
                                            sortType = curSortType,
                                            postIds = data.map { it.post.get { id } }
                                        )
                                    )
                                } else if (data.isNotEmpty() && curSortType != ThreadSortType.SORT_TYPE_DESC) {
                                    viewModel.send(
                                        ThreadUiIntent.LoadLatestPosts(
                                            threadId = threadId,
                                            curLatestPostId = data.last().post.get { id },
                                            forumId = curForumId,
                                            seeLz = isSeeLz,
                                            sortType = curSortType
                                        )
                                    )
                                }
                            },
                            loadEnd = loadMoreEnd,
                            indicator = { isLoading, loadMoreEnd, willLoad ->
                                ThreadLoadMoreIndicator(
                                    isLoading,
                                    loadMoreEnd,
                                    willLoad,
                                    hasMore
                                )
                            },
                            lazyListState = lazyListState,
                            isEmpty = data.isEmpty(),
                            preloadCount = loadMorePreloadCount,
                        ) {
                            MyLazyColumn(
                                state = lazyListState,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                item(key = "FirstPost") {
                                    if (firstPost != null) {
                                        Container {
                                            Column {
                                                PostCard(
                                                    postHolder = firstPost!!,
                                                    contentRenders = firstPostContentRenders,
                                                    canDelete = { it.author_id == user.get { id } },
                                                    immersiveMode = isImmersiveMode,
                                                    isCollected = {
                                                        it.id == thread?.get { collectMarkPid }
                                                            ?.toLongOrNull()
                                                    },
                                                    showSubPosts = false,
                                                    onUserClick = {
                                                        navigator.navigate("user/${it.id}")
                                                    },
                                                    onReplyClick = {
                                                        navigator.navigate("reply/$threadId?forumId=${curForumId ?: 0}&forumName=${Uri.encode(forum?.get { name }.orEmpty())}")
                                                    },
                                                    onMenuCopyClick = {
                                                        navigator.navigate("copy_dialog/${Uri.encode(it)}")
                                                    },
                                                    onMenuFavoriteClick = {
                                                        viewModel.send(
                                                            ThreadUiIntent.AddFavorite(
                                                                threadId,
                                                                it.id,
                                                                it.floor
                                                            )
                                                        )
                                                    },
                                                ) {
                                                    deletePost = null
                                                    confirmDeleteDialogState.show()
                                                }

                                                thread?.getNullableImmutable { origin_thread_info }
                                                    .takeIf { thread?.get { is_share_thread } == 1 }
                                                    ?.let {
                                                        OriginThreadCard(
                                                            originThreadInfo = it,
                                                            modifier = Modifier
                                                                .padding(horizontal = 16.dp)
                                                                .padding(bottom = 16.dp)
                                                                .clip(RoundedCornerShape(6.dp))
                                                                .background(ExtendedTheme.colors.floorCard)
                                                                .clickable {
                                                                    navigator.navigate("thread/${it.get { tid.toLong() }}")
                                                                }
                                                                .padding(16.dp)
                                                        )
                                                    }

                                                VerticalDivider(
                                                    modifier = Modifier
                                                        .padding(horizontal = 16.dp)
                                                        .padding(bottom = 8.dp),
                                                    thickness = 2.dp
                                                )
                                            }
                                        }
                                    }
                                }
                                stickyHeader(key = "ThreadHeader") {
                                    Container {
                                        Row(
                                            modifier = Modifier
                                                .background(MaterialTheme.colors.background)
                                                .padding(8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = stringResource(
                                                    R.string.title_thread_header,
                                                    "${thread?.get { replyNum - 1 } ?: 0}"),
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = ExtendedTheme.colors.text,
                                                modifier = Modifier.padding(horizontal = 8.dp),
                                            )
                                            Spacer(modifier = Modifier.weight(1f))
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.height(IntrinsicSize.Min)
                                            ) {
                                                Text(
                                                    text = stringResource(R.string.text_all),
                                                    modifier = Modifier
                                                        .padding(horizontal = 8.dp)
                                                        .clickable(
                                                            interactionSource = remember { MutableInteractionSource() },
                                                            indication = null,
                                                            enabled = isSeeLz
                                                        ) {
                                                            if (isSeeLz) {
                                                                viewModel.send(
                                                                    ThreadUiIntent.LoadFirstPage(
                                                                        threadId = threadId,
                                                                        forumId = curForumId,
                                                                        seeLz = false,
                                                                        sortType = curSortType
                                                                    )
                                                                )
                                                            }
                                                        },
                                                    fontSize = 13.sp,
                                                    fontWeight = if (!isSeeLz) FontWeight.SemiBold else FontWeight.Normal,
                                                    color = if (!isSeeLz) ExtendedTheme.colors.text else ExtendedTheme.colors.textSecondary,
                                                )
                                                HorizontalDivider()
                                                Text(
                                                    text = stringResource(R.string.title_see_lz),
                                                    modifier = Modifier
                                                        .padding(horizontal = 8.dp)
                                                        .clickable(
                                                            interactionSource = remember { MutableInteractionSource() },
                                                            indication = null,
                                                            enabled = !isSeeLz
                                                        ) {
                                                            if (!isSeeLz) {
                                                                viewModel.send(
                                                                    ThreadUiIntent.LoadFirstPage(
                                                                        threadId = threadId,
                                                                        forumId = curForumId,
                                                                        seeLz = true,
                                                                        sortType = curSortType
                                                                    )
                                                                )
                                                            }
                                                        },
                                                    fontSize = 13.sp,
                                                    fontWeight = if (isSeeLz) FontWeight.SemiBold else FontWeight.Normal,
                                                    color = if (isSeeLz) ExtendedTheme.colors.text else ExtendedTheme.colors.textSecondary,
                                                )
                                            }
                                        }
                                    }
                                }
                                if (curSortType == ThreadSortType.SORT_TYPE_DESC) {
                                    latestPosts(true)
                                }
                                item(key = "LoadPreviousBtn") {
                                    if (hasPrevious) {
                                        Container {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clickable {
                                                        viewModel.send(
                                                            ThreadUiIntent.LoadPrevious(
                                                                threadId,
                                                                max(currentPageMax - 1, 1),
                                                                curForumId,
                                                                postId = data
                                                                    .first()
                                                                    .post
                                                                    .get { id },
                                                                seeLz = isSeeLz,
                                                                sortType = curSortType,
                                                                postIds = data.map { it.post.get { id } }
                                                            )
                                                        )
                                                    }
                                                    .padding(8.dp),
                                                horizontalArrangement = Arrangement.Center,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Rounded.AlignVerticalTop,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Spacer(modifier = Modifier.width(16.dp))
                                                Text(
                                                    text = stringResource(id = R.string.btn_load_previous),
                                                    color = ExtendedTheme.colors.text,
                                                    fontSize = 14.sp
                                                )
                                            }
                                        }
                                    }
                                }
                                if (!isRefreshing && data.isEmpty()) {
                                    item(key = "EmptyTip") {
                                        Container {
                                            TipScreen(
                                                title = { Text(text = stringResource(id = R.string.title_empty)) },
                                                image = {
                                                    val composition by rememberLottieComposition(
                                                        LottieCompositionSpec.RawRes(R.raw.lottie_empty_box)
                                                    )
                                                    LottieAnimation(
                                                        composition = composition,
                                                        iterations = LottieConstants.IterateForever,
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .aspectRatio(2f)
                                                    )
                                                },
                                                actions = {
                                                    if (canReload) {
                                                        Button(onClick = { reload() }) {
                                                            Text(text = stringResource(id = R.string.btn_refresh))
                                                        }
                                                    }
                                                },
                                                modifier = Modifier.fillMaxSize(),
                                                scrollable = false
                                            )
                                        }
                                    }
                                } else {
                                    items(
                                        items = data,
                                        key = { (item) -> "Post_${item.get { id }}" },
                                        // 带图和纯文字楼层结构差别大，分开复用
                                        contentType = { (_, _, renders) -> if (renders.any { it is PicContentRender }) "PostWithPic" else "Post" },
                                    ) { (item, blocked, renders, subPosts) ->
                                        Container {
                                            PostCard(
                                                item,
                                                renders,
                                                subPosts,
                                                blocked
                                            )
                                        }
                                    }
                                }
                                if (curSortType != ThreadSortType.SORT_TYPE_DESC) {
                                    latestPosts(false)
                                }
                            }
                        }

                        PullRefreshIndicator(
                            refreshing = isRefreshing,
                            state = pullRefreshState,
                            modifier = Modifier.align(Alignment.TopCenter),
                            backgroundColor = ExtendedTheme.colors.pullRefreshIndicator,
                            contentColor = ExtendedTheme.colors.primary,
                        )
                    }
                }
            }
        }
    }
}
