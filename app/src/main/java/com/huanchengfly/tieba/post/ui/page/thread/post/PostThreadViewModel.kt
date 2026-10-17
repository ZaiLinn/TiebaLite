package com.huanchengfly.tieba.post.ui.page.thread.post

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.huanchengfly.tieba.post.App
import com.huanchengfly.tieba.post.api.retrofit.exception.getErrorMessage
import com.huanchengfly.tieba.post.components.ImageUploader
import com.huanchengfly.tieba.post.repository.AddThreadRepository
import com.huanchengfly.tieba.post.repository.NeedVcodeException
import com.huanchengfly.tieba.post.repository.VcodeChallenge
import com.huanchengfly.tieba.post.utils.FileUtil
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PostThreadViewModel @Inject constructor() : ViewModel() {
    /** 一次发帖请求；[content] 已拼好图片，验证码重发时直接复用，不再重新上传图片 */
    data class Request(
        val title: String,
        val content: String,
        val forumId: Long,
        val forumName: String,
        val vcode: String? = null,
        val vcodeMd5: String? = null,
        val vcodeType: String? = null,
    )

    sealed interface Event {
        data class Success(val threadId: Long, val message: String) : Event
        data class NeedVcode(val challenge: VcodeChallenge, val message: String, val retry: Request) : Event
        data class Failure(val message: String) : Event
    }

    private val _sending = MutableStateFlow(false)
    val sending: StateFlow<Boolean> = _sending.asStateFlow()

    private val _events = MutableSharedFlow<Event>(extraBufferCapacity = 4)
    val events: SharedFlow<Event> = _events.asSharedFlow()

    fun send(
        title: String,
        content: String,
        imageUris: List<String>,
        isOriginImage: Boolean,
        forumId: Long,
        forumName: String,
    ) {
        if (_sending.value) return
        viewModelScope.launch {
            _sending.value = true
            try {
                val imageContent = if (imageUris.isEmpty()) "" else ImageUploader(forumName)
                    .uploadImages(imageUris.map { FileUtil.getRealPathFromUri(App.INSTANCE, Uri.parse(it)) }, isOriginImage)
                    .first()
                    .joinToString("\n") { "#(pic,${it.picId},${it.picInfo.originPic.width},${it.picInfo.originPic.height})" }
                val fullContent = listOf(content, imageContent).filter { it.isNotEmpty() }.joinToString("\n")
                post(Request(title.trim(), fullContent, forumId, forumName))
            } catch (e: Exception) {
                _events.emit(Event.Failure(e.getErrorMessage()))
            } finally {
                _sending.value = false
            }
        }
    }

    /** 完成验证码后重发 */
    fun resend(request: Request) {
        if (_sending.value) return
        viewModelScope.launch {
            _sending.value = true
            try {
                post(request)
            } finally {
                _sending.value = false
            }
        }
    }

    private suspend fun post(request: Request) {
        try {
            val result = AddThreadRepository.addThread(
                title = request.title,
                content = request.content,
                forumId = request.forumId,
                forumName = request.forumName,
                vcode = request.vcode,
                vcodeMd5 = request.vcodeMd5,
                vcodeType = request.vcodeType,
            ).first()
            _events.emit(Event.Success(result.threadId, result.message))
        } catch (e: NeedVcodeException) {
            _events.emit(Event.NeedVcode(e.challenge, e.message.orEmpty(), request))
        } catch (e: Exception) {
            _events.emit(Event.Failure(e.getErrorMessage()))
        }
    }
}
