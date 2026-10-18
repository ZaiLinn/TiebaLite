package com.huanchengfly.tieba.post.utils

import com.huanchengfly.tieba.post.api.models.MessageListBean
import com.huanchengfly.tieba.post.api.models.protos.Post
import com.huanchengfly.tieba.post.api.models.protos.SubPostList
import com.huanchengfly.tieba.post.api.models.protos.ThreadInfo
import com.huanchengfly.tieba.post.api.models.protos.abstractText
import com.huanchengfly.tieba.post.api.models.protos.plainText
import com.huanchengfly.tieba.post.models.database.Block
import com.huanchengfly.tieba.post.models.database.Block.Companion.getKeywords
import com.huanchengfly.tieba.post.toJson
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import org.litepal.LitePal
import org.litepal.extension.delete
import org.litepal.extension.findAllAsync

object BlockManager {
    private val blockList: MutableList<Block> = mutableListOf()

    /**
     * 规则的不可变快照，关键词预先解析。
     *
     * shouldBlock 在加载每页数据时对每个楼层、楼中楼都会调用（在 IO 线程），
     * 原来每次都重新过滤列表、用 Gson 解析关键词 JSON，而且与主线程的增删并发访问同一个可变列表。
     */
    private class Rules(blocks: List<Block>) {
        val blackList = blocks.filter { it.category == Block.CATEGORY_BLACK_LIST }
        val whiteList = blocks.filter { it.category == Block.CATEGORY_WHITE_LIST }
        val blackKeywords = blackList.filter { it.type == Block.TYPE_KEYWORD }.map { it.getKeywords() }
        val whiteKeywords = whiteList.filter { it.type == Block.TYPE_KEYWORD }.map { it.getKeywords() }
        val blackExactContents = blackList.filter { it.type == Block.TYPE_EXACT_CONTENT }
            .flatMap { it.getKeywords() }
            .mapTo(HashSet()) { it.trim() }
        val blackUsers = blackList.filter { it.type == Block.TYPE_USER }
        val whiteUsers = whiteList.filter { it.type == Block.TYPE_USER }
    }

    @Volatile
    private var rules = Rules(emptyList())

    private val _version = MutableStateFlow(0)

    /** 规则每变化一次加一；界面据此重新判断已加载的内容是否被屏蔽 */
    val version: StateFlow<Int> = _version.asStateFlow()

    private fun updateRules(change: MutableList<Block>.() -> Unit) {
        synchronized(blockList) {
            blockList.change()
            rules = Rules(blockList.toList())
        }
        _version.update { it + 1 }
    }

    val blackList: List<Block>
        get() = rules.blackList

    val whiteList: List<Block>
        get() = rules.whiteList

    fun addBlock(block: Block) {
        block.save()
        updateRules { add(block) }
    }

    fun addBlockAsync(
        block: Block,
        callback: ((Boolean) -> Unit)? = null,
    ) {
        block.saveAsync()
            .listen {
                callback?.invoke(it)
                updateRules { add(block) }
            }
    }

    /** 屏蔽与 [content] 完全相同的内容 */
    fun blockExactContent(content: String, callback: ((Boolean) -> Unit)? = null) {
        addBlockAsync(
            Block(
                category = Block.CATEGORY_BLACK_LIST,
                type = Block.TYPE_EXACT_CONTENT,
                keywords = listOf(content.trim()).toJson()
            ),
            callback
        )
    }

    fun blockUser(uid: Long, username: String?, callback: ((Boolean) -> Unit)? = null) {
        addBlockAsync(
            Block(
                category = Block.CATEGORY_BLACK_LIST,
                type = Block.TYPE_USER,
                username = username,
                uid = uid.toString()
            ),
            callback
        )
    }

    fun removeBlock(id: Long) {
        LitePal.delete<Block>(id)
        updateRules { removeAll { it.id == id } }
    }

    fun init() {
        LitePal.findAllAsync<Block>().listen { blocks ->
            updateRules { addAll(blocks) }
        }
    }

    fun shouldBlock(content: String): Boolean {
        val rules = rules
        val blackMatched = rules.blackKeywords.any { keywords -> keywords.all { content.contains(it) } } ||
            (rules.blackExactContents.isNotEmpty() && content.trim() in rules.blackExactContents)
        return blackMatched &&
            rules.whiteKeywords.none { keywords -> keywords.all { content.contains(it) } }
    }

    fun shouldBlock(userId: Long = 0L, userName: String? = null): Boolean {
        val rules = rules
        if (rules.blackUsers.isEmpty()) return false
        val uid = userId.toString()
        return rules.blackUsers.any { it.uid == uid || it.username == userName } &&
            rules.whiteUsers.none { it.uid == uid || it.username == userName }
    }

    fun ThreadInfo.shouldBlock(): Boolean =
        shouldBlock(title) || shouldBlock(abstractText) || shouldBlock(authorId, author?.name)

    fun Post.shouldBlock(): Boolean =
        shouldBlock(content.plainText) || shouldBlock(author_id, author?.name)

    fun SubPostList.shouldBlock(): Boolean =
        shouldBlock(content.plainText) || shouldBlock(author_id, author?.name)

    fun MessageListBean.MessageInfoBean.shouldBlock(): Boolean =
        shouldBlock(content.orEmpty()) || shouldBlock(
            this.replyer?.id?.toLongOrNull() ?: -1,
            this.replyer?.name.orEmpty()
        )

    fun MessageListBean.AgreeBean.shouldBlock(): Boolean =
        shouldBlock(threadInfo?.title.orEmpty()) || shouldBlock(
            agreeer?.id?.toLongOrNull() ?: -1,
            agreeer?.name.orEmpty()
        )
}