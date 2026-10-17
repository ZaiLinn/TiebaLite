package com.huanchengfly.tieba.post.utils

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.ext.SdkExtensions.getExtensionVersion
import android.provider.MediaStore
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContract
import androidx.appcompat.app.AppCompatActivity
import com.huanchengfly.tieba.post.App
import com.huanchengfly.tieba.post.R
import com.zhihu.matisse.Matisse
import com.zhihu.matisse.MimeType
import com.zhihu.matisse.engine.impl.GlideEngine
import com.zhihu.matisse.ui.MatisseActivity

fun AppCompatActivity.registerPickMediasLauncher(callback: (PickMediasResult) -> Unit): ActivityResultLauncher<PickMediasRequest> {
    return registerForActivityResult(
        PickMediasContract
    ) {
        callback(it)
    }
}

fun isPhotoPickerAvailable(): Boolean {
    val versionOk = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        true
    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        getExtensionVersion(Build.VERSION_CODES.R) >= 2
    } else {
        false
    }
    if (!versionOk) return false
    // 模块在 ≠ 选图器真能用：部分 ROM（MuMu 模拟器，Android 12）装着 media module、
    // 扩展版本也够，但 picker activity 实际无法 resolve（直接 am start 报
    // unable to resolve Intent）。不做这道检查时 launch 抛 ActivityNotFoundException
    // 被协程吞掉，用户侧症状是点「添加图片」毫无反应（2026-10-16 模拟器实测复现）。
    // 加一道解析检查，不可用回退 Matisse 内置选图器。QUERY_ALL_PACKAGES 已在
    // manifest 声明，Android 11+ 的包可见性不受影响。
    val probe = Intent(MediaStore.ACTION_PICK_IMAGES).apply { type = "image/*" }
    return probe.resolveActivity(App.INSTANCE.packageManager) != null
}

fun shouldUsePhotoPicker(): Boolean {
    return !App.INSTANCE.appPreferences.doNotUsePhotoPicker && isPhotoPickerAvailable()
}

fun Intent.getClipDataUris(): List<Uri> {
    // Use a LinkedHashSet to maintain any ordering that may be
    // present in the ClipData
    val resultSet = LinkedHashSet<Uri>()
    data?.let { data ->
        resultSet.add(data)
    }
    val clipData = clipData
    if (clipData == null && resultSet.isEmpty()) {
        return emptyList()
    } else if (clipData != null) {
        for (i in 0 until clipData.itemCount) {
            val uri = clipData.getItemAt(i).uri
            if (uri != null) {
                resultSet.add(uri)
            }
        }
    }
    return resultSet.toList()
}

@SuppressLint("NewApi")
private fun getMaxItems() = if (shouldUsePhotoPicker()) {
    MediaStore.getPickImagesMaxLimit()
} else {
    Integer.MAX_VALUE
}

data class PickMediasRequest(
    val id: String = "",
    val maxItems: Int = 1,
    val mediaType: MediaType = ImageAndVideo
) {
    sealed interface MediaType

    data object ImageOnly : MediaType

    data object VideoOnly : MediaType

    data object ImageAndVideo : MediaType

    data class SingleMimeType(val mimeType: String) : MediaType

    companion object {
        internal fun getMimeType(input: MediaType): String? {
            return when (input) {
                is ImageOnly -> "image/*"
                is VideoOnly -> "video/*"
                is SingleMimeType -> input.mimeType
                is ImageAndVideo -> null
            }
        }
    }
}

data class PickMediasResult(
    val id: String,
    val uris: List<Uri>
)

object PickMediasContract : ActivityResultContract<PickMediasRequest, PickMediasResult>() {
    private var curRequestId: String? = null

    val hasCurrentRequest: Boolean
        get() = curRequestId != null

    override fun createIntent(context: Context, input: PickMediasRequest): Intent {
        curRequestId = input.id
        if (shouldUsePhotoPicker()) {
            return Intent(MediaStore.ACTION_PICK_IMAGES).apply {
                type = PickMediasRequest.getMimeType(input.mediaType)
                if (input.maxItems > 1) {
                    putExtra(MediaStore.EXTRA_PICK_IMAGES_MAX, input.maxItems)
                }
            }
        }
        val mimeType: Set<MimeType> = when (val mediaType = input.mediaType) {
            is PickMediasRequest.ImageOnly -> MimeType.ofImage()
            is PickMediasRequest.VideoOnly -> MimeType.ofVideo()
            is PickMediasRequest.SingleMimeType -> setOf(MimeType.valueOf(mediaType.mimeType))
            is PickMediasRequest.ImageAndVideo -> MimeType.ofAll()
        }
        Matisse.from(context as Activity)
            .choose(mimeType)
            .theme(if (ThemeUtil.isNightMode()) R.style.Matisse_Dracula else R.style.Matisse_Zhihu)
            .countable(input.maxItems > 1)
            .maxSelectable(input.maxItems)
            .imageEngine(GlideEngine())
        return Intent(context, MatisseActivity::class.java)
    }

    override fun parseResult(resultCode: Int, intent: Intent?): PickMediasResult {
        val id = curRequestId + ""
        curRequestId = null
        if (resultCode != Activity.RESULT_OK || intent == null) {
            return PickMediasResult(id, emptyList())
        }
        if (shouldUsePhotoPicker()) {
            return PickMediasResult(id, intent.getClipDataUris())
        }
        return PickMediasResult(id, Matisse.obtainResult(intent))
    }
}