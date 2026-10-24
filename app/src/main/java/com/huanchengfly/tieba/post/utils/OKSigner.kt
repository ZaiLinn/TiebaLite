package com.huanchengfly.tieba.post.utils

import android.content.Context
import android.util.Log
import com.huanchengfly.tieba.post.api.TiebaApi
import com.huanchengfly.tieba.post.api.models.CommonResponse
import com.huanchengfly.tieba.post.api.models.GetForumListBean
import com.huanchengfly.tieba.post.api.models.SignResultBean
import com.huanchengfly.tieba.post.api.retrofit.exception.TiebaApiException
import com.huanchengfly.tieba.post.api.retrofit.exception.getErrorCode
import com.huanchengfly.tieba.post.api.retrofit.exception.getErrorMessage
import com.huanchengfly.tieba.post.models.SignDataBean
import com.huanchengfly.tieba.post.models.database.Account
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.lang.ref.WeakReference
import java.util.concurrent.ThreadLocalRandom

abstract class IOKSigner(
    context: Context
) {
    private val contextWeakReference: WeakReference<Context> = WeakReference(context)

    val context: Context
        get() = contextWeakReference.get()!!

    abstract suspend fun start(): Boolean

    fun signFlow(signDataBean: SignDataBean): Flow<SignResultBean> {
        return TiebaApi.getInstance()
            .signFlow(signDataBean.forumId, signDataBean.forumName, signDataBean.tbs)
    }

    fun getSignDelay(): Long {
        return if (context.appPreferences.oksignSlowMode) {
            ThreadLocalRandom.current().nextInt(3500, 8000).toLong()
        } else {
            2000
        }
    }
}

/*
class MultiAccountSigner(
        context: Context
) : IOKSigner(context) {
    private val accounts: MutableList<Account> = mutableListOf()

    override suspend fun start() {
        accounts.clear()
        accounts.addAll(AccountUtil.allAccounts)
    }

    interface ProgressListener {
        fun onStart(
                total: Int
        )

        fun onProgressStart(
                signDataBean: SignDataBean,
                current: Int,
                total: Int
        )

        fun onProgressFinish(
                signResultBean: SignResultBean,
                current: Int,
                total: Int
        )

        fun onFinish(
                success: Boolean,
                signedCount: Int,
                total: Int
        )

        fun onFailure(
                current: Int,
                total: Int,
                errorCode: Int,
                errorMsg: String
        )
    }
}
*/

class SingleAccountSigner(
    context: Context,
    private val account: Account,
    /** 不为 null 时只重签这些吧（上次签到失败的），不再拉取关注列表 */
    private val retryForums: List<OKSignRecord.Failure>? = null,
) : IOKSigner(context) {
    companion object {
        const val TAG = "SingleAccountSigner"

        /** 「亲，你之前已经签过了」 */
        private const val ERROR_ALREADY_SIGNED = 160002
    }

    private val signData: MutableList<SignDataBean> = mutableListOf()
    private val failures: MutableList<OKSignRecord.Failure> = mutableListOf()
    private var successCount = 0
    private var totalCount = 0

    var lastFailure: Throwable? = null

    private var mProgressListener: ProgressListener? = null

    fun setProgressListener(listener: ProgressListener?): SingleAccountSigner {
        mProgressListener = listener
        return this
    }

    override suspend fun start(): Boolean {
        signData.clear()
        failures.clear()
        successCount = 0
        // 先用本地保存的账号信息兜底：刷新账号（登录、zid、用户信息）任一步失败，
        // 之前会直接抛出让签到进程崩掉，表现为点了一键签到毫无反应
        var userName = account.name
        var tbs = account.tbs
        Log.i(TAG, "start, retry = ${retryForums != null}")

        try {
            AccountUtil.fetchAccountFlow(account).collect { acc ->
                userName = acc.name
                tbs = acc.tbs
            }
        } catch (e: Exception) {
            Log.w(TAG, "refresh account failed, use saved account info", e)
        }

        val forums: List<Pair<String, String>>
        if (retryForums != null) {
            forums = retryForums.map { it.forumId to it.forumName }
        } else {
            val (allForums, fetchFailure) = fetchAllForums()
            if (allForums.isEmpty()) {
                // 拉取关注列表失败时之前会显示「没有可签到的吧」，让人以为签到正常结束了
                withContext(Dispatchers.Main) {
                    if (fetchFailure != null) {
                        mProgressListener?.onFailure(
                            0, 0, fetchFailure.getErrorCode(), fetchFailure.getErrorMessage()
                        )
                    } else {
                        mProgressListener?.onFinish(false, 0, 0)
                    }
                }
                return false
            }
            forums = allForums
        }

        forums.forEach {
            signData.add(SignDataBean(it.second, it.first, userName, tbs, false))
        }
        totalCount = signData.size
        withContext(Dispatchers.Main) {
            mProgressListener?.onStart(totalCount)
        }

        // 官方一键签到一次能签一批吧，先用它签掉能签的，剩下的再逐个签。重签时逐个签，好拿到具体失败原因
        val signedIds = if (retryForums == null && context.appPreferences.oksignUseOfficialOksign) {
            withContext(Dispatchers.Main) {
                mProgressListener?.onOfficialSignStart()
            }
            officialSign(tbs)
        } else {
            emptySet()
        }
        val remaining = signData.filter { it.forumId !in signedIds }
        successCount = totalCount - remaining.size
        Log.i(TAG, "official sign done: $successCount, remaining: ${remaining.size}")

        remaining.forEachIndexed { index, data ->
            val current = successCount + failures.size
            withContext(Dispatchers.Main) {
                mProgressListener?.onProgressStart(data, current, totalCount)
            }
            try {
                val result = signFlow(data).first()
                successCount += 1
                withContext(Dispatchers.Main) {
                    mProgressListener?.onProgressFinish(data, result, current, totalCount)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                if (e.getErrorCode() == ERROR_ALREADY_SIGNED) {
                    successCount += 1
                } else {
                    lastFailure = e
                    failures.add(
                        OKSignRecord.Failure(
                            data.forumId, data.forumName, e.getErrorCode(), e.getErrorMessage()
                        )
                    )
                    withContext(Dispatchers.Main) {
                        mProgressListener?.onFailure(
                            current, totalCount, e.getErrorCode(), e.getErrorMessage()
                        )
                    }
                }
            }
            if (index < remaining.lastIndex) delay(getSignDelay())
        }

        saveRecord(userName)

        withContext(Dispatchers.Main) {
            mProgressListener?.onFinish(
                successCount == totalCount,
                successCount,
                totalCount
            )
        }

        return successCount > 0
    }

    /** 分页拉取全部关注的吧，返回 (吧 id, 吧名) 列表和拉取失败的原因 */
    private fun fetchAllForums(): Pair<List<Pair<String, String>>, Throwable?> {
        val myUid = AccountUtil.getUid() ?: ""
        val allForums = mutableListOf<Pair<String, String>>()
        val seenIds = mutableSetOf<String>()
        var page = 1
        var hasMore = true
        val pageSize = 200
        var fetchFailure: Throwable? = null

        while (hasMore) {
            try {
                val response = TiebaApi.getInstance()
                    .userLikeForum(uid = myUid, page = page).execute()
                val body = response.body()
                if (body != null) {
                    body.forumList?.forumList?.forEach { forum ->
                        if (seenIds.add(forum.id ?: "")) {
                            allForums.add((forum.id ?: "") to (forum.name ?: ""))
                        }
                    }
                    body.commonForumList?.forumList?.forEach { forum ->
                        if (seenIds.add(forum.id ?: "")) {
                            allForums.add((forum.id ?: "") to (forum.name ?: ""))
                        }
                    }
                    hasMore = body.hasMore == "1"
                    if (allForums.isEmpty() && body.errorCode != "0") {
                        fetchFailure = TiebaApiException(
                            CommonResponse(body.errorCode.toIntOrNull() ?: -1, body.errorMsg)
                        )
                    }
                } else {
                    fetchFailure = Exception("HTTP ${response.code()}")
                    hasMore = false
                }
            } catch (e: Exception) {
                Log.w(TAG, "fetch forum list failed", e)
                fetchFailure = e
                hasMore = false
            }
            page++
            if (allForums.size >= pageSize * 15) hasMore = false // safety: max 3000
        }
        Log.i(TAG, "Collected ${allForums.size} forums for signing")
        return allForums to fetchFailure
    }

    /**
     * 官方一键签到：getForumList 给出今天的签到状态和可用一键签到的最低等级，
     * 再按 msign_step_num 分批调用 msign。返回已签上的吧 id（含今天之前就签过的）。
     * 任何一步出错都只是少签几个，剩下的交给逐个签到。
     */
    private suspend fun officialSign(tbs: String): Set<String> {
        val signedIds = mutableSetOf<String>()
        try {
            val plan = planOfficialSign(TiebaApi.getInstance().getForumListFlow().first())
            signedIds.addAll(plan.alreadySigned)
            plan.batches.forEach { chunk ->
                try {
                    val result = TiebaApi.getInstance().mSign(chunk.joinToString(","), tbs).first()
                    @Suppress("USELESS_ELVIS")
                    (result.info ?: emptyList())
                        .filter { it.signed == "1" }
                        .mapTo(signedIds) { it.forumId }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Log.w(TAG, "msign failed", e)
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "official sign failed", e)
        }
        return signedIds
    }

    private fun saveRecord(userName: String) {
        val previous = if (retryForums != null) OKSignRecord.load(context) else null
        val record = if (previous != null) {
            // 重签只覆盖上次失败的那部分
            previous.copy(
                time = System.currentTimeMillis(),
                successCount = previous.successCount + successCount,
                failures = failures.toList(),
            )
        } else {
            OKSignRecord(
                time = System.currentTimeMillis(),
                userName = userName,
                total = totalCount,
                successCount = successCount,
                failures = failures.toList(),
            )
        }
        OKSignRecord.save(context, record)
    }
}

internal data class OfficialSignPlan(
    /** 今天已经签过的吧 */
    val alreadySigned: Set<String>,
    /** 每批一次 msign 请求的吧 id */
    val batches: List<List<String>>,
)

private const val DEFAULT_MSIGN_STEP = 50

/**
 * 根据 getForumList 的结果决定官方一键签到签哪些吧：等级不低于 level 且今天没签的，
 * 按 msign_step_num 分批。can_use 不为 1 时不能用官方一键签到。
 * 字段是 Gson 填的，接口缺字段时可能为 null，所以都按可空处理。
 */
@Suppress("USELESS_ELVIS", "UNNECESSARY_SAFE_CALL", "SENSELESS_COMPARISON")
internal fun planOfficialSign(forumList: GetForumListBean): OfficialSignPlan {
    val forumInfo = forumList.forumInfo ?: emptyList()
    val alreadySigned = forumInfo.filter { it.isSignIn == "1" && it.forumId != null }
        .map { it.forumId }.toSet()
    val minLevel = forumList.level?.toIntOrNull()
    if (forumList.canUse != "1" || minLevel == null) return OfficialSignPlan(alreadySigned, emptyList())
    val step = forumList.msignStepNum?.toIntOrNull()?.takeIf { it > 0 } ?: DEFAULT_MSIGN_STEP
    val ids = forumInfo
        .filter { it.isSignIn != "1" && it.forumId != null && (it.userLevel?.toIntOrNull() ?: 0) >= minLevel }
        .map { it.forumId }
    return OfficialSignPlan(alreadySigned, ids.chunked(step))
}

interface ProgressListener {
    fun onStart(
        total: Int
    )

    fun onOfficialSignStart() {}

    fun onProgressStart(
        signDataBean: SignDataBean,
        current: Int,
        total: Int
    )

    fun onProgressFinish(
        signDataBean: SignDataBean,
        signResultBean: SignResultBean,
        current: Int,
        total: Int
    )

    fun onFinish(
        success: Boolean,
        signedCount: Int,
        total: Int
    )

    fun onFailure(
        current: Int,
        total: Int,
        errorCode: Int,
        errorMsg: String
    )
}
