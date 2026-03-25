package com.kuaigeglobal.epnova.feature.aicompose.generating

import androidx.databinding.ObservableBoolean
import androidx.databinding.ObservableField
import androidx.databinding.ObservableInt
import com.kuaigeglobal.epnova.BR
import com.kuaigeglobal.epnova.R
import com.kuaigeglobal.epnova.core.ext.launch
import com.kuaigeglobal.epnova.core.network.apis.response.PictureBook
import com.kuaigeglobal.epnova.feature.aicompose.generating.GeneratingViewModel.generateTime
import com.nhsd.basic.ext.getResString
import com.nhsd.basic.item.QuickItemViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlin.math.min

/**
 * 生成中
 */
class ItemGeneratingVM(info: PictureBook?) : QuickItemViewModel() {

    val playable = ObservableBoolean()
    val progress by lazy { ObservableInt() }
    val data = ObservableField<PictureBook?>()
    private var updateProgressJob: Job? = null

    init {
        update(info)
    }

    private fun updateProgress() {
        progress.set(0)
        updateProgressJob?.cancel()
        updateProgressJob = launch {
            while (progress.get() < 99) {
                val pastSeconds = (System.currentTimeMillis() - (data.get()?.pictureBookId?.generateTime ?: 0L)) / 1000f
                progress.set(min((pastSeconds * 2).toInt(), 95))
                delay(500)
            }
        }
    }

    fun title(book: PictureBook?, progress: Int): String? {
        //2）标题：绘本生成中...1%(百分比动态展示）每5秒加载10%，直到加载到95%，等待真实生成完成后，展示100%
        //[枚举]绘本生成(合成)状态 [0=待生成 1=生成中 2=生成成功 3=生成失败]
        return when (book?.pictureBookGenStatus) {
            2 -> book.pictureBookName
            else -> "绘本生成中...${progress}%"
        }
    }

    fun status(progress: Int): String? {
        //[枚举]绘本生成(合成)状态 [0=待生成 1=生成中 2=生成成功 3=生成失败]
//        return when {
            //3）小标题五种状态对应百分比
            //0%-20%正在构思您的专属故事
            //21%-40%正在制作分镜草图
            //41%-60%正在合成绘本配音
            //61%-80%画师AI正在描绘场景
            //81%-99%您的绘本即将完成
            //100%您的绘本已完成
//            progress <= 20 -> R.string.progress_story_creating_simply.getResString()
//            progress <= 40 -> R.string.progress_storyboarding_simply.getResString()
//            progress <= 60 -> R.string.progress_voice_arranging_simply.getResString()
//            progress <= 80 -> R.string.progress_illustration_drawing_simply.getResString()
//            progress <= 99 -> R.string.progress_preview_ready_simply.getResString()
//            else -> R.string.progress_complete_simply.getResString()
//        }
        return ""
    }

    fun update(book: PictureBook?) {
        val bookChanged = data.get()?.pictureBookId != book?.pictureBookId
        data.set(book)
        playable.set(book?.pictureBookGenStatus != 0)
        if (bookChanged) updateProgress()
    }

    fun play() {
        when (data.get()?.pictureBookGenStatus) {
//            1 -> BookPreviewActivity.start(data.get()?.pictureBookId)
//            2 -> PictureBookPlayerActivity.start(data.get()?.pictureBookId)
        }
    }

    fun release() {
        updateProgressJob?.cancel()
    }

    override fun onCleared() {
        updateProgressJob?.cancel()
        super.onCleared()
    }

    override fun id(): Int = BR.item
}

