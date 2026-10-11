package com.kuaigeglobal.epnova.feature.creator.set

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import com.kuaigeglobal.epnova.R
import com.kuaigeglobal.epnova.common.Constants
import com.kuaigeglobal.epnova.common.SimpleTextWatcher
import com.kuaigeglobal.epnova.common.ext.applySmartFocusManager
import com.kuaigeglobal.epnova.core.Config
import com.kuaigeglobal.epnova.core.Events
import com.kuaigeglobal.epnova.core.User
import com.kuaigeglobal.epnova.core.ext.sensitiveWordCheck
import com.kuaigeglobal.epnova.core.manager.EventManager
import com.kuaigeglobal.epnova.core.manager.FirebaseEvent
import com.kuaigeglobal.epnova.core.network.apis.entity.UnusedPackageInfo
import com.kuaigeglobal.epnova.core.network.apis.entity.VideoConfigData
import com.kuaigeglobal.epnova.core.network.apis.entity.VideoConfigInfo
import com.kuaigeglobal.epnova.core.network.apis.entity.WorkInfo
import com.kuaigeglobal.epnova.core.network.apis.request.CreateTaskBody
import com.kuaigeglobal.epnova.databinding.ActivityVideoSetBinding
import com.kuaigeglobal.epnova.feature.creator.data.DramaGenerateConstants
import com.kuaigeglobal.epnova.feature.creator.generate.StoryEditorActivity
import com.kuaigeglobal.epnova.feature.creator.set.allstyle.CreationStyleChooseDialog
import com.kuaigeglobal.epnova.feature.editor.info.InputDialog
import com.kuaigeglobal.epnova.feature.splash.newer.FirstRechargeDialog
import com.kuaigeglobal.epnova.router.AppNavigator
import com.kuaigeglobal.epnova.widget.GeneratingDialog
import com.nhsd.basic.BaseViewModelActivity
import com.nhsd.basic.BasicEdgeToEdgeConfig
import com.nhsd.basic.bus.AppEvent
import com.nhsd.basic.ext.dp
import com.nhsd.basic.ext.getResString
import com.nhsd.basic.ext.getThemeColor
import com.nhsd.basic.ext.setOnSingleClickListener
import com.nhsd.basic.ext.setupTranslucentStatus
import com.nhsd.basic.ext.showToast
import kotlinx.coroutines.launch
import kotlin.collections.set

class VideoSetActivity : BaseViewModelActivity<ActivityVideoSetBinding, VideoSetViewModel>() {

    companion object {
        private const val SAME_STORY_MAX_LENGTH = 20000
        private val SAME_STORY_MIN_HEIGHT = 136.dp

        @JvmStatic
        fun start(
            context: Context?,
            workTitle: String?,
            sameStory: String?,
            list: VideoConfigData?,
            charCount: Int? = null,
            hotCaseId: String? = null,
            hotCaseTitle: String? = null
//            creationPackage: UnusedPackageInfo? = null
        ) {
            val current = context ?: return
            val starter = Intent(current, VideoSetActivity::class.java)
            starter.putExtra(Constants.Extra.TITLE, workTitle)
            starter.putExtra(Constants.Extra.CONTENT, sameStory)
            starter.putExtra(Constants.Extra.DATA, list)
            charCount?.let { starter.putExtra(Constants.Extra.COUNT, it) }
            hotCaseId?.let { starter.putExtra(Constants.Extra.ID, it) }
            hotCaseTitle?.let {
                starter.putExtra(Constants.Extra.TITLE, it)
                starter.putExtra(Constants.Extra.NAME, it)
            }
//            creationPackage?.let { starter.putExtra(VideoSetViewModel.ARG_CREATION_PACKAGE, it) }
            current.startActivity(starter)
        }
    }

    override fun setTranslucentStatus() {
        // 导航栏图标浅色、背景透明
        setupTranslucentStatus(false, getThemeColor(R.attr.bgColor1))
    }

    private var sameStoryContent = ""
    private var workCreatingDialog: GeneratingDialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        EventManager.onFirebaseTrack(
            FirebaseEvent.Category.CREATE,
            FirebaseEvent.Action.CREATE_WORK_SHOW
        )
        if (!mRealVM.hotCaseId.isNullOrBlank()) {
            // GA4：创作同款页面展示。
            EventManager.onFirebaseTrack(
                FirebaseEvent.Category.HIT_SCRIPTS,
                FirebaseEvent.Action.CREATE_SIMILAR_PAGE_SHOW
            )
        }
        binding.contentLayout.applySmartFocusManager()
        initView()
        initListener()
        observeData()
    }

    private fun initView() {
        binding.nameInputer.setText(mRealVM.workTitle)
        updateSameStory(mRealVM.sameStory)
        updateConfirmButtonAlpha()
        binding.scrollView.addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
            updateSameStoryContainerHeight()
        }
        binding.contentLayout.addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
            updateSameStoryContainerHeight()
        }
    }

    private fun initListener() {
        val textWatcher = object : SimpleTextWatcher() {
            override fun afterTextChanged(s: Editable?) {
                updateConfirmButtonAlpha()
            }
        }
        binding.nameInputer.addTextChangedListener(textWatcher)

        binding.btnConfirm.setOnSingleClickListener {
            submit()
        }

        binding.sameStoryContainer.setOnSingleClickListener {
            showSameStoryInputDialog()
        }
        binding.sameStoryContent.setOnSingleClickListener {
            showSameStoryInputDialog()
        }
        binding.sameStoryContentScroll.setOnSingleClickListener {
            showSameStoryInputDialog()
        }

        binding.btnAllStyle.setOnSingleClickListener {
            val dialog = CreationStyleChooseDialog.newInstance(
                mRealVM.checkedStyle?.data?.get()?.styleCode,
                mRealVM.configsData?.styleItemVOList
            )
            dialog.onSuccess = { info ->
                info?.let {
                    mRealVM.styleVms.find { vm -> vm.data.get()?.styleCode == it.styleCode }?.let { vm ->
                        mRealVM.checkedStyle?.checked?.set(false)
                        vm.checked.set(true)
                        mRealVM.checkedStyle = vm
                        binding.recyclerView.scrollToPosition(mRealVM.styleVms.indexOf(vm))
                    }
                }
            }
            dialog.show(supportFragmentManager)
        }
    }

    private fun observeData() {
        mRealVM.workCreating.observe(this) { show ->
            if (show) {
                workCreatingDialog?.dismiss()
                workCreatingDialog = GeneratingDialog(this, getString(R.string.work_creating))
                workCreatingDialog?.show()
            } else {
                workCreatingDialog?.dismiss()
                workCreatingDialog = null
            }
        }
        mRealVM.firstRechargeCache.observe(this) { data ->
            data?.let {
//                EventManager.onAppTrack(AppEvent.SF_VIEW_HOME_NEWCOMER_GIFTPACK_PAGE)
                FirstRechargeDialog.show(FirstRechargeDialog.SCENE_HOME, it)
                Config.get().markFirstRechargeShown(FirstRechargeDialog.SCENE_HOME)
            }
        }
    }

    private fun updateConfirmButtonAlpha() {
        binding.btnConfirm.alpha = if (
            binding.nameInputer.text.isNullOrBlank() ||
            sameStoryContent.isBlank()
        ) {
            0.6f
        } else {
            1f
        }
    }

    private fun updateSameStory(content: CharSequence?) {
        val text = content?.toString().orEmpty()
        sameStoryContent = text
        if (text.isBlank()) {
            binding.sameStoryContent.text = getString(R.string.create_same_story_synopsis_hint)
            binding.sameStoryContent.setTextColor(getThemeColor(R.attr.textColor4))
        } else {
            binding.sameStoryContent.text = content
            binding.sameStoryContent.setTextColor(getThemeColor(R.attr.textColor2))
        }
        binding.sameStoryLength.text = "${text.length}/$SAME_STORY_MAX_LENGTH"
        binding.sameStoryContentScroll.post {
            binding.sameStoryContentScroll.scrollTo(0, 0)
        }
    }

    private fun updateSameStoryContainerHeight() {
        binding.scrollView.post {
            val scrollHeight = binding.scrollView.height
            if (scrollHeight <= 0) return@post
            val usedHeight = binding.contentLayout.paddingTop + binding.contentLayout.paddingBottom +
                (0 until binding.contentLayout.childCount).sumOf { index ->
                    val child = binding.contentLayout.getChildAt(index)
                    if (child == binding.sameStoryContainer || child.visibility == View.GONE) {
                        0
                    } else {
                        child.measuredHeight + child.verticalMargins()
                    }
                }
            val targetHeight = (scrollHeight - usedHeight - binding.sameStoryContainer.verticalMargins())
                .coerceAtLeast(SAME_STORY_MIN_HEIGHT)
            if (binding.sameStoryContainer.layoutParams.height == targetHeight) return@post
            binding.sameStoryContainer.layoutParams = binding.sameStoryContainer.layoutParams.apply {
                height = targetHeight
            }
        }
    }

    private fun View.verticalMargins(): Int {
        val params = layoutParams as? ViewGroup.MarginLayoutParams ?: return 0
        return params.topMargin + params.bottomMargin
    }

    private fun showSameStoryInputDialog(highlightedContent: CharSequence? = null) {
        InputDialog.show(
            type = InputDialog.TYPE_SAME_STORY,
            content = sameStoryContent,
            highlightedContent = highlightedContent
        ) {
            updateSameStory(it)
            updateConfirmButtonAlpha()
        }
    }

    private fun submit() {
        if (AppNavigator.checkLoginIntercepted(true)) {
            return
        }
        val workTitle = binding.nameInputer.text.trim()
        val sameStory = sameStoryContent.trim()
        if (workTitle.isEmpty()) {
            R.string.please_enter_work_name.getResString().showToast()
            return
        }
        if (sameStory.isEmpty()) {
            getString(R.string.create_same_story_synopsis_hint).showToast()
            return
        }
        if (mRealVM.checkedEpisodeSet == null) {
            R.string.please_set_total_episodes.getResString().showToast()
            return
        }
        if (mRealVM.checkedEpisodeSet?.data?.get()?.isCustomInput() == true &&
            mRealVM.checkedEpisodeSet?.input?.get().isNullOrEmpty()
        ) {
            R.string.please_enter_custom_episodes.getResString().showToast()
            return
        }
        if (mRealVM.checkedRatio == null) {
            R.string.please_select_video_aspect_ratio.getResString().showToast()
            return
        }
        if (mRealVM.checkedStyle == null) {
            R.string.please_select_video_style.getResString().showToast()
            return
        }

        lifecycleScope.launch {
            val titleCheck = sensitiveWordCheck(workTitle)
            if (!titleCheck.first) {
                titleCheck.second?.let { binding.nameInputer.setText(it) }
                return@launch
            }
            val storyCheck = sensitiveWordCheck(sameStory)
            if (!storyCheck.first) {
                showSameStoryInputDialog(storyCheck.second ?: sameStory)
                return@launch
            }
//            val isPackageWork = mRealVM.creationPackage != null
//            EventManager.onAppTrack(AppEvent.SF_CLICK_MATERIAL_SAME_PAGE_BUTTON, GrowingIO.sfWorkType(isPackageWork))
            mRealVM.hotCaseId?.let {
                EventManager.reportCreationUsageCount(it, EventManager.REPORT_CREATION_TYPE_HOT_CASE)
            }
            val episodeConfig = mRealVM.checkedEpisodeSet?.data?.get()
            EventManager.onFirebaseTrack(
                FirebaseEvent.Category.CREATE,
                FirebaseEvent.Action.START_CREATING_CLICK,
                FirebaseEvent.Value.CREATION_INSPIRATION,
                if (episodeConfig?.isCustomInput() == true) {
                    mRealVM.checkedEpisodeSet?.input?.get()
                } else {
                    episodeConfig?.configValue
                },
                mRealVM.checkedRatio?.data?.get()?.configValue,
                mRealVM.checkedStyle?.data?.get()?.styleCode
            )
            if (!mRealVM.hotCaseId.isNullOrBlank()) {
                // GA4：创作同款页点击 Start Creating，Label1～4 按表格顺序上报。
                EventManager.onFirebaseTrack(
                    FirebaseEvent.Category.HIT_SCRIPTS,
                    FirebaseEvent.Action.CREATE_SIMILAR_BTN_CLICK,
                    mRealVM.hotCaseTitle,
                    if (episodeConfig?.isCustomInput() == true) {
                        mRealVM.checkedEpisodeSet?.input?.get()
                    } else {
                        episodeConfig?.configValue
                    },
                    mRealVM.checkedRatio?.data?.get()?.configValue,
                    selectedStyleAnalyticsName()
                )
            }

            createSameTask(workTitle, sameStory)
        }
    }

    /** 将风格英文名中的单词分隔符统一为下划线，符合 GA4 Label4 约定。 */
    private fun selectedStyleAnalyticsName(): String? {
        val style = mRealVM.checkedStyle?.data?.get()
        // styleCode 是服务端稳定英文标识且已使用下划线；仅在缺失时用展示名兜底。
        val sourceName = style?.styleCode?.takeIf { it.isNotBlank() } ?: style?.styleName
        return sourceName
            ?.trim()
            ?.replace(Regex("[\\s-]+"), "_")
    }

    private fun createSameTask(workTitle: String, sameStory: String) {
//        val packageEpisodeSelection = if (mRealVM.creationPackage != null) {
//            mRealVM.getPackageEpisodeSelection()
//        } else {
//            null
//        }
//        val range = packageEpisodeSelection?.first ?: mRealVM.checkedEpisodeSet?.data?.get()?.configCode
        val range = mRealVM.checkedEpisodeSet?.data?.get()?.configCode
//        val custom = packageEpisodeSelection?.second ?: mRealVM.checkedEpisodeSet?.input?.get()
        val custom = mRealVM.checkedEpisodeSet?.input?.get()
//        val singleModel = mRealVM.creationPackage == null &&
//            (range == VideoConfigInfo.MODE_SINGLE || (range == VideoConfigInfo.MODE_CUSTOM && custom?.toIntOrNull() == 1))
        val singleModel = (range == VideoConfigInfo.MODE_SINGLE || (range == VideoConfigInfo.MODE_CUSTOM && custom?.toIntOrNull() == 1))
        val modelType = if (singleModel) {
            DramaGenerateConstants.MODEL_STORY_SINGLE
        } else {
            DramaGenerateConstants.MODEL_INSPIRATION
        }
        val reportId = mRealVM.hotCaseId?.takeIf { it.isNotBlank() }
        val body = CreateTaskBody(
            workInspiration = sameStory,
            content = sameStory,
            workTitle = workTitle,
            totalEpisodeCntCode = range,
            totalEpisodeCnt = custom,
            workRatio = mRealVM.checkedRatio?.data?.get()?.configValue,
            workStyleCode = mRealVM.checkedStyle?.data?.get()?.styleCode,
            reportType = if (reportId != null) {
                EventManager.REPORT_CREATION_TYPE_HOT_CASE
            } else {
                null
            },
            reportId = reportId,
            workType = WorkInfo.INSPIRATION_CREATION
//            packageInstanceId = mRealVM.creationPackage?.packageInstanceId
        )
        val progressLogic = {
            mRealVM.createSameTask(modelType, body) { data ->
                StoryEditorActivity.start(
                    context = this,
                    workId = data?.workId,
                    name = workTitle,
                    workType = WorkInfo.INSPIRATION_CREATION,
                    model = modelType
                )
//                FlowEventBus.postSync(Events.MainTabEvent(Events.MainTabEvent.TAB_MY_WORKS))
                finish()
            }
        }
        val canShowFirstRecharge = !(User.get().firstRechargeDialog ?: false) &&
            !Config.get().isFirstRechargeShown(FirstRechargeDialog.SCENE_HOME)
        if (canShowFirstRecharge) {
            mRealVM.getFirstRechargeInfo { progressLogic() }
        } else {
            progressLogic()
        }
    }
}
