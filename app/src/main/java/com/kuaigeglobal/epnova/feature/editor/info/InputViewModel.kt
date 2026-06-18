package com.kuaigeglobal.epnova.feature.editor.info

import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import androidx.core.graphics.toColorInt
import androidx.databinding.ObservableField
import androidx.databinding.ObservableInt
import androidx.lifecycle.MutableLiveData
import com.blankj.utilcode.util.StringUtils.getString
import com.kuaigeglobal.epnova.R
import com.kuaigeglobal.epnova.base.viewmodel.AppRepositoryViewModel
import com.kuaigeglobal.epnova.common.Constants
import com.kuaigeglobal.epnova.core.AppRepository
import com.kuaigeglobal.epnova.core.ext.requestWithLoading
import com.kuaigeglobal.epnova.core.network.apis.request.SaveStorySummaryBody
import com.kuaigeglobal.epnova.core.network.apis.request.WorkInfoUpdateBody
import java.util.regex.Pattern

class InputViewModel : AppRepositoryViewModel<AppRepository>() {

    val extra by lazy { mBundle.getString(Constants.Extra.EXTRA) }
    val type by lazy { mBundle.getInt(Constants.Extra.TYPE) }
    val workId by lazy { mBundle.getString(Constants.Extra.WORK_ID) }
    val content by lazy { mBundle.getString(Constants.Extra.CONTENT) }
    val storyId by lazy { mBundle.getString(Constants.Extra.STORY_ID) }
    val input = MutableLiveData<String>()
    val title = ObservableField<String>()
    val maxLength = ObservableInt()
    val subTitle = ObservableField<CharSequence>()
    var hasChange = false

    override fun onCreate() {
        super.onCreate()
        input.value = content
        extra?.let {
            subTitle.set(formatEpisodeTitle(it, "#80ffffff".toColorInt()))
        }
        setTitle()
        // 海外版
        maxLength.set(
            (when (type) {
                InputDialog.TYPE_WORK_DESC -> 500
                InputDialog.TYPE_EPISODE_SUMMARY -> 300
                InputDialog.TYPE_EPISODE_CONTENT -> 5000
                InputDialog.TYPE_ASSET_DESC -> 200
                InputDialog.TYPE_STORYBOARD_DESC -> 1000
                else -> 500
            } * 5)
        )
    }

    fun setTitle() {
        when (type) {
            InputDialog.TYPE_WORK_DESC -> {
                title.set(getString(R.string.edit_story_synopsis))
            }

            InputDialog.TYPE_EPISODE_SUMMARY -> {
                title.set(getString(R.string.edit_story_synopsis))
            }

            InputDialog.TYPE_EPISODE_CONTENT -> {
                title.set(getString(R.string.edit_episode_content))
            }

            InputDialog.TYPE_ASSET_DESC -> {
                title.set(getString(R.string.edit_asset_desc))
            }

            InputDialog.TYPE_STORYBOARD_DESC -> {
                title.set(getString(R.string.edit_storyboard_desc))
            }
        }
    }

    fun save(onSuccess: (() -> Unit)?) {
        when (type) {
            InputDialog.TYPE_WORK_DESC -> {
                saveDesc { onSuccess?.invoke() }
            }

            InputDialog.TYPE_EPISODE_SUMMARY -> {
                saveStorySummary { onSuccess?.invoke() }
            }

            else -> {
                onSuccess?.invoke()
            }
        }
    }

    fun saveDesc(onSuccess: (() -> Unit)? = null) {
        requestWithLoading({
            mRepo.api.updateWorkInfo(
                WorkInfoUpdateBody(
                    workId,
                    workDesc = input.value
                )
            )
        }) {
            onSuccess?.invoke()
        }
    }

    fun saveStorySummary(onSuccess: (() -> Unit)? = null) {
        requestWithLoading({
            mRepo.api.updateStorySummary(
                SaveStorySummaryBody(
                    input.value,
                    storyId,
                    workId
                )
            )
        }) {
            onSuccess?.invoke()
        }
    }

    /**
     * 格式化剧集标题颜色
     * @param fullText 完整文本，例如 "第1集 代码觉醒"
     * @param color 想要修改的目标颜色值，例如 Color.RED
     * @return 处理后的 SpannableString
     */
    fun formatEpisodeTitle(fullText: String, color: Int): SpannableString {
        val spannable = SpannableString(fullText)

        // 正则表达式解析：
        // ^ 表示从字符串开头匹配
        // 第 表示匹配字符“第”
        // \d+ 表示匹配一个或多个数字
        // 集 表示匹配字符“集”
        val pattern = Pattern.compile("^第\\d+集")
        val matcher = pattern.matcher(fullText)

        if (matcher.find()) {
            // matcher.start() 通常为 0，matcher.end() 是匹配到的结尾索引
            spannable.setSpan(
                ForegroundColorSpan(color),
                matcher.start(),
                matcher.end(),
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }

        return spannable
    }
}