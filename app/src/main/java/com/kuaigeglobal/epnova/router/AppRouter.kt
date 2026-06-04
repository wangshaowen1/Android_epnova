package com.kuaigeglobal.epnova.router

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.text.TextUtils
import androidx.core.net.toUri
import androidx.fragment.app.FragmentActivity
import com.blankj.utilcode.util.ActivityUtils
import com.kuaigeglobal.epnova.R
import com.kuaigeglobal.epnova.common.Constants
import com.kuaigeglobal.epnova.common.ext.ioScope
import com.kuaigeglobal.epnova.common.ext.topActivity
import com.kuaigeglobal.epnova.common.permissions.requestRead2WritePermissions
import com.kuaigeglobal.epnova.common.utils.IntentUtils
import com.kuaigeglobal.epnova.common.utils.Utils
import com.kuaigeglobal.epnova.core.Events
import com.kuaigeglobal.epnova.core.manager.IRouter
import com.kuaigeglobal.epnova.core.network.apis.ApiFactory
import com.kuaigeglobal.epnova.core.network.apis.entity.CreatorWorkInfo
import com.kuaigeglobal.epnova.core.network.apis.entity.EpisodeInfo
import com.kuaigeglobal.epnova.core.network.apis.entity.WorkInfo
import com.kuaigeglobal.epnova.core.network.apis.request.WorkBody
import com.kuaigeglobal.epnova.feature.creator.data.DramaGenerateConstants
import com.kuaigeglobal.epnova.feature.creator.generate.StoryEditorActivity
import com.kuaigeglobal.epnova.feature.editor.EditStoryboardVideoActivity
import com.kuaigeglobal.epnova.feature.main.MainActivity
import com.kuaigeglobal.epnova.feature.play.DramasPlayPagerActivity
import com.kuaigeglobal.epnova.feature.recharge.MyBalanceActivity
import com.kuaigeglobal.epnova.feature.recharge.MyBalanceViewModel
import com.kuaigeglobal.epnova.feature.rewards.RewardsActivity
import com.kuaigeglobal.epnova.feature.series.SeriesActivity
import com.kuaigeglobal.epnova.feature.web.SimpleBrowserActivity.Companion.launch
import com.kuaigeglobal.epnova.widget.LoadingDialog2
import com.nhsd.basic.bus.FlowEventBus
import com.nhsd.basic.ext.launchNoCrash
import com.nhsd.basic.ext.showToast
import com.nhsd.basic.utils.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.withContext

object AppRouter : IRouter {

    override fun openFromPush(context: Context, uri: String?) {
        uri?.let {
            AppLaunchJump.launchJump(uri.toUri())
        }
    }

    override fun navigate(context: Context, path: String?) {
        path?.let { jump(it) }
    }

    @JvmStatic
    fun jump(context: Context, schemeUri: String, openBrowser: Boolean) {
        if (openBrowser && schemeUri.startsWith("http")) {
            openBrowser(context, schemeUri)
        } else {
            jump(schemeUri)
        }
    }

    @JvmStatic
    fun jump(schemeUri: String?) {
        if (TextUtils.isEmpty(schemeUri)) {
            return
        }
        val uri = Uri.parse(schemeUri)
        if (uri != null) {
            var context: Context? = ActivityUtils.getTopActivity()
            if (context == null) {
                context = ActivityUtils.getTopActivity()
            }
            if (Constants.APP_SCHEME == uri.scheme) {
                jump(uri)
            } else if (schemeUri!!.endsWith(".apk")) {
                if (context is Activity) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU || Utils.hasPermission(
                            Manifest.permission.WRITE_EXTERNAL_STORAGE
                        )
                    ) {
                        IntentUtils.startDownload(context, schemeUri)
                    } else {
                        if (!Utils.isActivityFinishing(context)) {
                            val finalContext: Context? = context
                            (context as FragmentActivity).requestRead2WritePermissions(
                                context.getString(
                                    R.string.sdcard_permission_description
                                )
                            ) { allGranted: Boolean ->
                                if (allGranted && !Utils.isActivityFinishing(finalContext)) {
                                    IntentUtils.startDownload(finalContext, schemeUri)
                                }
                                null
                            }
                        }
                    }
                }
            } else if (schemeUri.startsWith("http")) {
                launch(context, schemeUri)
            }
        }
    }


    @JvmStatic
    fun jump(uri: Uri) {
        val host = uri.host
        Logger.i("host " + uri.host + ", uri=" + uri.toString())
        if (TextUtils.isEmpty(host)) {
            return
        }

        when (host) {
            "restoreCreator" -> {//epnovascheme://toHome
                val workId = uri.getQueryParameter("workId")
                val storyId = uri.getQueryParameter("storyId")
                val workTitle = uri.getQueryParameter("workTitle")
                val workType = uri.getQueryParameter("workType")
                val workRatio = uri.getQueryParameter("workRatio")
                val totalEpisodeCnt = uri.getQueryParameter("totalEpisodeCnt")
                Logger.i("retoreCreator workId=$workId, storyId=$storyId, workTitle=$workTitle, workType=$workType, workRatio=$workRatio")
                startStory(workId, storyId, workTitle, workType?.toIntOrNull(), totalEpisodeCnt?.toIntOrNull(), workRatio)
            }

            "toHome" -> {//epnovascheme://toHome
                FlowEventBus.postSync(Events.MainTabEvent(Events.MainTabEvent.TAB_HOME))
                ActivityUtils.finishToActivity(MainActivity::class.java, false)
            }

            "toRewards" -> {//epnovascheme://toRewards
                if (AppNavigator.checkLoginIntercepted(showToast = false)) {
                    return
                }
                val context = topActivity ?: return
                RewardsActivity.start(context)
            }

            "toStoreCoins" -> {//epnovascheme://toStoreCoins
                if (AppNavigator.checkLoginIntercepted(showToast = false)) {
                    return
                }
                MyBalanceActivity.start(MyBalanceViewModel.TAB_COINS)
            }

            "toStoreCredits" -> {//epnovascheme://toStoreCredits
                if (AppNavigator.checkLoginIntercepted(showToast = false)) {
                    return
                }
                MyBalanceActivity.start(MyBalanceViewModel.TAB_CREDITS)
            }

            "toSeriesDetail" -> {//epnovascheme://toSeriesDetail?workId=%s
                val workId = uri.getQueryParameter("workId")
                if (workId.isNullOrEmpty()) {
                    return
                }
                SeriesActivity.start(workId)
            }

            "toTargetEpisode" -> {//epnovascheme://toTargetEpisode?workId=%s&storyId=%s
                val workId = uri.getQueryParameter("workId")
                val storyId = uri.getQueryParameter("storyId")
                ioScope.launchNoCrash {
                    val unlockInfoData = ApiFactory.apiService.episodeUnlockLockStatus(WorkBody(workId, storyId))
                    withContext(Dispatchers.Main){
                        DramasPlayPagerActivity.start(workId, if (unlockInfoData.data?.lockStatus == 2) 0 else unlockInfoData.data?.episodeNo, false)
                    }
                }
            }

            else -> {}
        }
    }

    fun openBrowser(context: Context, url: String?) {
        val intent = Intent()
        intent.action = Intent.ACTION_VIEW
        intent.data = url?.toUri()
        // 注意此处的判断intent.resolveActivity()可以返回显示该Intent的Activity对应的组件名
        // 官方解释 : Name of the component implementing an activity that can display the intent
        if (intent.resolveActivity(context.packageManager) != null) {
            val componentName = intent.resolveActivity(context.packageManager)
            context.startActivity(Intent.createChooser(intent, context.getString(R.string.select_browser)))
        } else {
            // GlobalMethod.showToast(context, "链接错误或无浏览器");
        }
    }

    @JvmStatic
    fun startStory(work: WorkInfo?, storyId: String?, from: String? = null) {
        work?.apply {
            startStory(workId, storyId, workTitle, workType, totalEpisodeCnt, workRatio, from)
        }
    }

    @JvmStatic
    fun startStory(
        workId: String?,
        storyId: String?,
        workTitle: String?,
        workType: Int?,
        totalEpisodeCnt: Int?,
        workRatio: String?,
        from: String? = null,
    ) {
        if (workId.isNullOrEmpty() || storyId.isNullOrEmpty()) {
            return
        }
        val loading = (topActivity as? FragmentActivity)?.supportFragmentManager?.let {
            val dialog = LoadingDialog2()
            dialog.show(it)
            dialog
        }
        MainScope().launchNoCrash(
            block = {
                val res = withContext(Dispatchers.IO) {
                    ApiFactory.apiService.singleEpisodeDetails(
                        WorkBody(
                            workId,
                            storyId
                        )
                    )
                }
                if (!res.isSuccess) {
                    res.msg?.showToast()
                    loading?.dismissAllowingStateLoss()
                    return@launchNoCrash
                }
                jumpToTarget(
                    workId = workId,
                    workTitle = workTitle,
                    workType = workType,
                    totalEpisodeCnt = totalEpisodeCnt,
                    singleEpisodeDetailsInfo = res.data,
                    from = from
                )
                loading?.dismissAllowingStateLoss()
            },
            onError = {
                loading?.dismissAllowingStateLoss()
            }
        )
    }

    /**
     *查看单集作品的剧集详情
     */
    @JvmStatic
    fun singleEpisodeSeriesDetails(
        work: WorkInfo?,
    ) {
        singleEpisodeSeriesDetails(
            workId = work?.workId,
            workTitle = work?.workTitle,
            workType = work?.workType
        )
    }

    /**
     * 查看单集 Creator 作品的剧集详情。
     */
    @JvmStatic
    fun singleEpisodeSeriesDetails(
        work: CreatorWorkInfo?,
    ) {
        singleEpisodeSeriesDetails(
            workId = work?.workId,
            workTitle = work?.workTitle,
            workType = work?.workType
        )
    }

    private fun singleEpisodeSeriesDetails(
        workId: String?,
        workTitle: String?,
        workType: Int?
    ) {
        if (workId.isNullOrEmpty()) return
        val loading = (topActivity as? FragmentActivity)?.supportFragmentManager?.let {
            val dialog = LoadingDialog2()
            dialog.show(it)
            dialog
        }
        MainScope().launchNoCrash(
            block = {
                val res = withContext(Dispatchers.IO) {
                    ApiFactory.apiService.singleEpisodeSeriesDetails(
                        WorkBody(
                            workId, null
                        )
                    )
                }
                if (!res.isSuccess) {
                    res.msg?.showToast()
                    loading?.dismissAllowingStateLoss()
                    return@launchNoCrash
                }
                jumpToTarget(
                    workId = workId,
                    workTitle = workTitle,
                    workType = workType,
                    1,
                    singleEpisodeDetailsInfo = res.data
                )
                loading?.dismissAllowingStateLoss()
            },
            onError = {
                loading?.dismissAllowingStateLoss()
            }
        )
    }


    /**
     * 通过条件判断跳转到指定页面
     * @param workId 作品id
     * @param workTitle 作品标题
     * @param workType 作品类型
     * @param singleEpisodeDetailsInfo 剧集详情
     */
    private fun jumpToTarget(
        workId: String?,
        workTitle: String?,
        workType: Int?,
        totalEpisodeCnt: Int?,
        singleEpisodeDetailsInfo: EpisodeInfo?,
        from: String? = null
    ) {
        val info = singleEpisodeDetailsInfo ?: return

        // 获取安全的集数索引，后续拼接和传参直接使用该非空值
        val sortNo = info.sortNo ?: 1
        Logger.d("打印参数：jumpToTarget singleEpisodeDetailsInfo?.stepProgress = ${info.stepProgress}")

        when (info.stepProgress) {
            // 10=剧本输入, 15=剧本生成中, 20=资产生成中, 30=分镜描述中
            EpisodeInfo.PROGRESS_SUMMARY_GENERATING,
            EpisodeInfo.PROGRESS_STORY_GENERATING,
            EpisodeInfo.PROGRESS_ASSET_GENERATING,
            EpisodeInfo.PROGRESS_STORYBOARD_GENERATING_ASSET -> {

                // 提取出唯一不同的 step 参数
                val targetStep = when (info.stepProgress) {
                    EpisodeInfo.PROGRESS_SUMMARY_GENERATING -> 1
                    EpisodeInfo.PROGRESS_STORY_GENERATING -> 2
                    EpisodeInfo.PROGRESS_ASSET_GENERATING -> 3
                    else -> 4 // EpisodeInfo.PROGRESS_STORYBOARD_GENERATING_ASSET
                }

                StoryEditorActivity.restore(
                    context = topActivity,
                    step = targetStep,
                    workId = workId,
                    storyId = info.storyId,
                    workTitle = workTitle,
                    taskId = info.taskGenResultStoryVO?.taskId,
                    chapterIndex = sortNo,
                    workType = workType,
                    model = getModel(totalEpisodeCnt, workType, sortNo, info.isFirstCreated()),
                    videoRatio = info.workRatio,
                    from = from
                )
            }

            // 32=分镜视频生成中 35=待合成 45=分镜重绘中 50=重绘已完成
            EpisodeInfo.PROGRESS_STORYBOARD_GENERATING,
            EpisodeInfo.PROGRESS_WAITING_COMPOSING,
            EpisodeInfo.EPISODE_REPAINT_STATE,
            EpisodeInfo.EPISODE_REPAINT_COMPLETED -> {
                EditStoryboardVideoActivity.start(
                    // 优化：原代码使用了可能为空的 info.sortNo，这里直接使用兜底过的非空 sortNo
                    storyTitle = workTitle?.let { "$it：第${sortNo}集" },
                    workId = workId,
                    storyId = info.storyId,
                    workRatio = info.workRatio,
                    story = info,
                    from = from,
                    model = getModel(totalEpisodeCnt, workType, sortNo, info.isFirstCreated())
                )
            }
        }
    }

    fun getModel(totalEpisodeCnt: Int?, workType: Int?, sortNo: Int = 0, firstCreated: Boolean = true): Int {
        if (totalEpisodeCnt == 1) return DramaGenerateConstants.MODEL_STORY_SINGLE
        return getModel(workType = workType, sortNo = sortNo, firstCreated = firstCreated)
    }

    fun getModel(workType: Int?, sortNo: Int = 0, firstCreated: Boolean = true): Int {
        Logger.d("打印参数：getModel workType = $workType ,sortNo = $sortNo ")
        when (workType) {
            WorkInfo.INSPIRATION_CREATION -> {
                //作品类型：1-灵感创作
                if (firstCreated && sortNo <= 1) {
                    //第一集创作(灵感)
                    return DramaGenerateConstants.MODEL_INSPIRATION
                }
                //继续创作(灵感)
                return DramaGenerateConstants.MODEL_CONTINUE_INSPIRATION
            }

            WorkInfo.NOVEL_CREATION -> {
                //作品类型：2-小说创作
                if (firstCreated && sortNo <= 1) {
                    //第一集创作(小说)
                    return DramaGenerateConstants.MODEL_STORY
                }
                //继续创作(小说)
                return DramaGenerateConstants.MODEL_CONTINUE_STORY
            }

            WorkInfo.NOVEL_UPLOAD -> {
                //作品类型：3-上传小说
                if (firstCreated && sortNo <= 1) {
                    //第一集创作(小说文档模式)
                    return DramaGenerateConstants.MODEL_STORY_DOCUMENTS
                }
                //继续创作(小说文档模式)
                return DramaGenerateConstants.MODE_CONTINUE_WRITING_NOVEL_DOCUMENTS
            }

            else -> {
                if (firstCreated && sortNo <= 1) {
                    //第一集创作(小说)
                    return DramaGenerateConstants.MODEL_STORY
                }
                //继续创作(小说)
                return DramaGenerateConstants.MODEL_CONTINUE_STORY
            }
        }
    }
}
