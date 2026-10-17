package com.huanchengfly.tieba.post.update

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/**
 * 更新模块 ViewModel：两个入口（启动检查弹窗 + 关于页手动检查）共用，
 * 状态与逻辑都在单例 [UpdateManager] 中，这里只做转发。
 */
@HiltViewModel
class UpdateViewModel @Inject constructor(
    private val manager: UpdateManager
) : ViewModel() {

    val state = manager.state

    /** 当前更新检查平台来源（关于页选择，DataStore 持久化）。 */
    val origin = manager.origin

    /**
     * 启动自动检查。走 GitHub（ZaiLinn/TiebaLite）检查；
     * debug 构建传 [enableMock]=true 时走本地 mock 源（[mockRoot] 可覆盖
     * gradle 注入的 MOCK_UPDATE_API_ROOT）。
     */
    fun startupCheck(mockRoot: String? = null, enableMock: Boolean = false) =
        manager.check(UpdateSource.STARTUP, mockRoot, enableMock)

    /**
     * 关于页手动检查（不节流、不受"本次忽略"限制、失败可见）。
     */
    fun manualCheck() = manager.check(UpdateSource.MANUAL)

    /**
     * 切换更新检查平台来源（当前只有 GitHub）。
     */
    fun setOrigin(origin: UpdateOrigin) = manager.setOrigin(origin)

    /**
     * "本次忽略"当前版本（强更版本不可忽略）。
     */
    fun ignoreCurrentVersion() {
        val available = manager.state.value as? UpdateUiState.Available ?: return
        manager.ignore(available.release.tagName, available.force)
    }

    fun download() = manager.download()

    fun install() = manager.install()
}
