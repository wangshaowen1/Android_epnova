package com.kuaigeglobal.epnova.core.manager

import com.google.firebase.messaging.FirebaseMessaging
import com.kuaigeglobal.epnova.common.ext.ioScope
import com.kuaigeglobal.epnova.core.User
import com.kuaigeglobal.epnova.core.network.apis.ApiFactory
import com.kuaigeglobal.epnova.core.network.apis.entity.CreatorPlayableEpisode
import com.kuaigeglobal.epnova.core.network.apis.entity.EpisodeInfo
import com.kuaigeglobal.epnova.core.network.apis.request.FcmTokenBody
import com.kuaigeglobal.epnova.core.network.apis.request.PlayEventBody
import com.kuaigeglobal.epnova.core.network.apis.request.UserPlayBody
import com.kuaigeglobal.epnova.core.network.apis.request.WorkBody
import com.nhsd.basic.ext.launchNoCrash
import com.nhsd.basic.utils.Logger
import kotlinx.coroutines.delay


/**
 * 自定义事件管理器（事件埋点）
 * @author xiaoowuu
 */
object EventManager {

    private var fetchingFcmToken = false

    /**
     * 上报 FCM registration token。
     *
     * 这里同时负责本地缓存和网络上报，Firebase Service 只负责把系统回调转交进来，
     * 避免后续再出现多个类各自保存、各自直调推送 token 接口的分散入口。
     */
    fun reportFcmToken(token: String?) {
        val safeToken = token?.trim()?.takeIf { it.isNotEmpty() } ?: return

        // FCM token 是设备级 token，先落本地，便于后续需要补报时复用；但接口是用户维度绑定，
        // 当前没有有效 userToken 时不发请求，避免服务端收到无法归属用户的推送 token。
        User.get().storeFcmToken(safeToken)
        if (User.get().isVisitor) return

        ioScope.launchNoCrash {
            val maxRetries = 3
            var retryCount = 0
            var delayMillis = 1000L

            while (retryCount < maxRetries) {
                try {
                    val response = ApiFactory.apiService.registerFcmToken(FcmTokenBody(safeToken))
                    if (response.isSuccess) {
                        Logger.d("EventManager", "FCM token report succeeded")
                        return@launchNoCrash
                    }
                    Logger.w("EventManager", "FCM token report failed: ${response.errorMsg ?: response.msg}")
                } catch (e: Exception) {
                    Logger.w("EventManager", "FCM token report error: ${e.message}")
                }

                retryCount++
                if (retryCount < maxRetries) {
                    delay(delayMillis)
                    delayMillis *= 2
                }
            }

            Logger.e("EventManager", "FCM token report failed after $maxRetries attempts")
        }
    }

    /**
     * FCM token 补报入口。
     *
     * onNewToken 不是每次启动或登录都会触发：如果设备已经有有效 token，Firebase 不会再回调。
     * 因此启动后和登录成功后都走这里兜底，优先复用本地已缓存的 token；只有本地为空时才主动向 Firebase 获取，
     * 避免多个业务入口重复取 token、重复散落调用注册接口。
     */
    fun reportStoredOrCurrentFcmToken() {
        val cachedToken = User.get().fcmToken?.trim()?.takeIf { it.isNotEmpty() }
        if (cachedToken != null) {
            reportFcmToken(cachedToken)
            return
        }
        if (fetchingFcmToken) return

        fetchingFcmToken = true
        FirebaseMessaging.getInstance().token
            .addOnSuccessListener { token ->
                fetchingFcmToken = false
                reportFcmToken(token)
            }
            .addOnFailureListener { error ->
                fetchingFcmToken = false
                Logger.w("EventManager", "FCM token fetch failed: ${error.message}")
            }
    }

    /**
     * 作品播放量计数
     */
    fun onPlayEvent(workId: String?, episodeId: String?) {
        ioScope.launchNoCrash {
            ApiFactory.apiService.playCountInc(WorkBody(workId, episodeId))
        }
    }

    fun onPlayEvent(item: CreatorPlayableEpisode?) {
        item ?: return
        val workId = item.workContext.workId ?: item.episode.workId ?: return
        ioScope.launchNoCrash {
            ApiFactory.apiService.playEventReport(
                PlayEventBody(
                    workId = workId,
                    userId = item.workContext.userId,
                    storyId = item.episode.storyId
                )
            )
        }
    }

    fun onPlayEvent(workId: String?, episode: EpisodeInfo?, progress: Long) {
        reportPlaybackProgress(workId, episode, progress, (episode?.duration ?: 0L) * 1000L)
    }

    fun reportPlaybackProgress(
        workId: String?,
        episode: EpisodeInfo?,
        progressMs: Long,
        durationMs: Long,
        completed: Boolean = false,
        progressPercent: Int? = null,
    ) {
        val targetEpisode = episode ?: return
        val storyId = targetEpisode.storyId ?: return
        val safeDurationMs = durationMs.takeIf { it > 0 } ?: ((targetEpisode.duration ?: 0L) * 1000L)

        // 非完播上报必须带有效播放位置，避免 ViewHolder 复用、首帧未回调或退出时把服务端进度覆盖成 0。
        // 完播上报允许没有最新 current 回调，下面会用总时长作为 playSecond。
        if (!completed && (progressMs <= 0L || safeDurationMs <= 0L)) return
        val durationSeconds = (safeDurationMs / 1000L).toInt().takeIf { it > 0 }

        // completed=true 时代表“已到片尾”，服务端下次会从 00:00 起播；
        // 普通节点/暂停/切集上报只记录当前位置，不改变片尾语义。
        val playSecond = if (completed && durationSeconds != null) {
            durationSeconds
        } else {
            (progressMs.coerceAtLeast(0L) / 1000L).toInt()
        }
        val safeProgress = progressPercent ?: if (safeDurationMs > 0) {
            ((progressMs.coerceAtLeast(0L) * 100L) / safeDurationMs).toInt().coerceIn(0, 100)
        } else {
            null
        }

        ioScope.launchNoCrash {
            ApiFactory.apiService.userPlayReport(
                UserPlayBody(
                    playProgress = if (completed) 100 else safeProgress,
                    sortNo = targetEpisode.sortNo,
                    storyId = storyId,
                    workId = workId,
                    playSecond = playSecond,
                    durationSeconds = durationSeconds,
                    completed = completed,
                )
            )
        }
    }

    /**
     * @param code GrowingIO上配置的Code
     * @param paramsMap code附带的子属性 传参：mapOf("属性key" to "属性值")
     */
    @JvmStatic
    fun onAppTrack(code: String, paramsMap: Map<String, String?>? = null) {
        Logger.d("[onAppTrack] Tracking Event -> Code: $code, Params: $paramsMap")
//        GrowingAutotracker.get().trackCustomEvent(code, paramsMap)
    }

    /**
     * @param code GrowingIO上配置的Code
     * @param params code附带的子属性 传参："属性key" to "属性值"
     */
    @JvmStatic
    fun onAppTrack(code: String, vararg params: Pair<String, String?>) {
        if (params.isEmpty()) {
            onAppTrack(code, null)
        } else {
            onAppTrack(code, params.toMap())
        }
    }

}

/**
 * App功能使用数据
 */
object AppEvent {
    /**
     * 访问首页｜用户进入App访问首页页面
     */
    const val SF_VIEW_HOME_PAGE = "sf_view_home_page"

    /**
     * 浏览首页推荐视频点击观看｜用户在首页浏览推荐视频内容并进行观看
     */
    const val SF_CLICK_HOME_RECOMMEND_VIDEO = "sf_click_home_recommend_video"

    /**
     * 访问创作tab页｜用户点击底部导航栏创作Tab，进入创作页
     */
    const val SF_VIEW_CREATE_TAB_PAGE = "sf_view_create_tab_page"

    /**
     * 访问作品tab页｜用户点击底部导航栏作品Tab，进入个人作品页
     */
    const val SF_VIEW_WORKS_TAB_PAGE = "sf_view_works_tab_page"

    /**
     * 访问我的tab页｜用户点击底部导航栏我的Tab，进入个人页面
     */
    const val SF_VIEW_MINE_TAB_PAGE = "sf_view_mine_tab_page"

    /**
     * 算力充值页面｜用户进入算力充值页面
     */
    const val SF_VIEW_RECHARGE_PAGE = "sf_view_recharge_page"

    /**
     * 用户进入故事灵感页面｜用户进入故事灵感页面
     */
    const val SF_VIEW_STORY_INSPIRATION_PAGE = "sf_view_story_inspiration_page"

    /**
     * 故事灵感tab｜用户进入故事灵感页面
     */
    const val SF_VIEW_STORY_INSPIRATION_TAB = "sf_view_story_inspiration_tab"

    /**
     * 故事灵感页面的下一步按钮｜用户在故事灵感tab页 输入文字后点击下一步按钮
     */
    const val SF_CLICK_STORY_INSPIRATION_NEXT_BUTTON = "sf_click_story_inspiration_next_button"

    /**
     * 小说改编tab页面｜用户进入小说改编
     */
    const val SF_VIEW_NOVEL_ADAPTATION_TAB = "sf_view_novel_adaptation_tab"

    /**
     * 小说改编页面的下一步按钮｜用户在小说改编tab页 输入文字后点击下一步按钮
     */
    const val SF_CLICK_NOVEL_ADAPTATION_NEXT_BUTTON = "sf_click_novel_adaptation_next_button"

    /**
     * 故事梗概环节｜用户进入创作流程中的故事梗概环节页面
     */
    const val SF_VIEW_STORY_OUTLINE_PAGE = "sf_view_story_outline_page"

    /**
     * 故事剧本环节｜用户进入创作流程中的故事剧本环节页面
     */
    const val SF_VIEW_SCRIPT_PAGE = "sf_view_script_page"

    /**
     * 资产管理环节｜用户进入资产管理页面（角色/场景/道具管理）
     */
    const val SF_VIEW_ASSET_MANAGEMENT_PAGE = "sf_view_asset_management_page"

    /**
     * 资产管理页的一键生成｜用户在资产管理页面点击“一键生成”按钮进行资产生成
     */
    const val SF_CLICK_ASSET_GENERATE_BUTTON = "sf_click_asset_generate_button"

    /**
     * 分镜设计环节｜用户进入创作流程中的分镜设计环节页面
     */
    const val SF_VIEW_STORYBOARD_DESIGN_PAGE = "sf_view_storyboard_design_page"

    /**
     * 分镜视频生成环节｜用户进入创作流程中的分镜视频生成环节页面
     */
    const val SF_VIEW_VIDEO_GENERATION_PAGE = "sf_view_video_generation_page"

    /**
     * 重绘分镜按钮｜在重绘已合成视频页面 或新集创作的分镜视频合成页面，用户点击左下角“重绘分镜”按钮
     */
    const val SF_CLICK_REDRAW_STORYBOARD_BUTTON = "sf_click_redraw_storyboard_button"

    /**
     * 合成按钮｜在分镜视频生成页面 和重绘视频页面的 右上角的用户点击“合成”按钮开始视频生成
     */
    const val SF_CLICK_COMBINE_VIDEO_BUTTON = "sf_click_combine_video_button"

    /**
     * 用户作品播放页重绘视频按钮｜在用户作品播放页中，用户点击“重绘视频”按钮进行视频重绘
     */
    const val SF_CLICK_WORK_REPLAY_REDRAW_BUTTON = "sf_click_work_replay_redraw_button"

    /**
     * 算力充值支付弹窗｜在创作各流程（如生成资产、分镜或合成视频）算力不足或需付费时，系统弹出支付弹窗
     */
    const val SF_SHOW_COMPUTE_RECHARGE_PAYMENT_POPUP = "sf_show_compute_recharge_payment_popup"

    /**
     * 算力充值支付弹窗购买按钮｜用户在购买弹窗点击了购买按钮
     */
    const val SF_CLICK_COMPUTE_RECHARGE_PAYMENT_BUTTON = "sf_click_compute_recharge_payment_button"

    /**
     * 首页课程报名弹窗｜首页的课程弹窗
     */
    const val SF_SHOW_COURSE_ENROLL_POPUP = "sf_show_course_enroll_popup"

    /**
     * 悬浮窗课程报名｜用户点击页面悬浮窗中的“课程报名”按钮
     */
    const val SF_CLICK_FLOATING_COURSE_ENROLL = "sf_click_floating_course_enroll"

    /**
     * H5的课程页面｜用户通过课程入口进入访问H5课程页面
     */
    const val SF_VIEW_H5_COURSE_PAGE = "sf_view_h5_course_page"

    /**
     * 课程报名支付弹窗｜用户在课程报名流程中触发并看到课程支付弹窗
     */
    const val SF_SHOW_COURSE_PAYMENT_POPUP = "sf_show_course_payment_popup"

    /**
     * 课程报名支付弹窗购买按钮｜用户点击课程购买按钮
     */
    const val SF_CLICK_COURSE_PAYMENT_BUTTON = "sf_click_course_payment_button"

    /**
     * 去发行按钮｜在作品完成页或签约入口页，用户点击“去发行”按钮
     */
    const val SF_CLICK_GO_PUBLISH_BUTTON = "sf_click_go_publish_button"

    /**
     * 提交申请按钮｜在签约/发行申请填写完成后，用户点击“提交申请”按钮
     */
    const val SF_CLICK_SUBMIT_APPLY_BUTTON = "sf_click_submit_apply_button"

    /**
     * 申请签约按钮｜在签约/发行页面中，用户点击“申请签约”按钮
     */
    const val SF_CLICK_SIGN_CONTRACT_BUTTON = "sf_click_sign_contract_button"

    /**
     * 签约/发行主页面｜用户进入签约/发行主页面
     */
    const val SF_VIEW_SIGN_PUBLISH_MAIN_PAGE = "sf_view_sign_publish_main_page"

    /**
     *快出片｜课程页面支付成功
     */
    const val SF_CLICK_COURSE_PAYMENT_BUTTON_PAYSUCCESS = "sf_click_course_payment_button_paysuccess"

    /**
     *快出片｜算力商城页面支付成功
     */
    const val SF_CLICK_RECHARGE_PAGE_PAYSUCCESS = "sf_click_recharge_page_paysuccess"

    /**
     *快出片｜算力充值支付弹窗支付成功
     */
    const val SF_CLICK_COMPUTE_RECHARGE_PAYMENT_POPUP_PAYSUCCESS = "sf_click_compute_recharge_payment_popup_paysuccess"

    /**
     *
     * 快出片｜分镜视频字幕生成失败点击“重试按钮”
     */
    const val SF_CLICK_SUBTITLE_FAIL_PROMT_BUTTON = "sf_click_subtitle_fail_promt_button"

    /**
     *快出片｜分镜视频字幕生成失败
     */
    const val SF_VIEW_SUBTITLE_FAIL_PROMT = "sf_view_subtitle_fail_promt"

    /**
     *快出片｜创作流程新人礼包支付成功
     */
    const val SF_CLICK_CREATE_NEWCOMER_GIFTPACK_PAYSUCCESS = "sf_click_create_newcomer_giftpack_paysuccess"

    /**
     *
     * 快出片｜创作流程新人礼包点击“立即支付”按钮
     */
    const val SF_CLICK_CREATE_NEWCOMER_GIFTPACK_PAGE = "sf_click_create_newcomer_giftpack_page"

    /**
     *快出片｜创作流程新人礼包曝光
     */
    const val SF_VIEW_CREATE_NEWCOMER_GIFTPACK_PAGE = "sf_view_create_newcomer_giftpack_page"

    /**
     *快出片｜首页新人礼包支付成功
     */
    const val SF_CLICK_HOME_NEWCOMER_GIFTPACK_PAYSUCCESS = "sf_click_home_newcomer_giftpack_paysuccess"

    /**
     *快出片｜首页新人礼包点击 "立即支付"
     */
    const val SF_CLICK_HOME_NEWCOMER_GIFTPACK_PAGE = "sf_click_home_newcomer_giftpack_page"

    /**
     *
     * 快出片｜新手创作营引导首页"立即体验"按钮
     */
    const val SF_CLICK_BEGINNER_CREATION_GUIDE_PAGE_BUTTON = "sf_click_beginner_creation_guide_page_button"

    /**
     *快出片｜新手创作营故事选择页曝光
     */
    const val SF_VIEW_BEGINNER_CREATION_STORY_PAGE = "sf_view_beginner_creation_story_page"

    /**
     *
     * 快出片｜新手创作营故事选择页点击 “跳过”按钮
     */
    const val SF_CLICK_BEGINNER_CREATION_STORY_PAGE_SKIP = "sf_click_beginner_creation_story_page_skip"

    /**
     *快出片｜新手创作营故事选择页“立即生成”按钮
     */
    const val SF_CLICK_BEGINNER_CREATION_STORY_PAGE_BUTTON = "sf_click_beginner_creation_story_page_button"

    /**
     *
     * 快出片｜新手创作营故事生成页曝光
     */
    const val SF_VIEW_BEGINNER_CREATION_GENERATE_PAGE = "sf_view_beginner_creation_generate_page"

    /**
     *快出片｜新手创作营成片结果页曝光
     */
    const val SF_VIEW_BEGINNER_CREATION_RESULT_PAGE = "sf_view_beginner_creation_result_page"

    /**
     *快出片 | 新手创作营成片结果页点击“开启我的创作变现”按钮
     */
    const val SF_CLICK_BEGINNER_CREATION_RESULT_PAGE_BUTTON = "sf_click_beginner_creation_result_page_button"

    /**
     *快出片 | 首页新人礼包曝光
     */
    const val SF_VIEW_HOME_NEWCOMER_GIFTPACK_PAGE = "sf_view_home_newcomer_giftpack_page"

    /**
     *
     * 快出片 | 新手创作营首页曝光
     */
    const val SF_VIEW_BEGINNER_CREATION_GUIDE_PAGE = "sf_view_beginner_creation_guide_page"
}


object GrowingIO {

    /**
     * 通用｜是否为新用户
     * 用户登录时候是否为首次注册账号，如果是="yes"，如果是老用户登录非首次注册="no"
     */
    const val KEY_ALL_IS_NEW_USER = "all_is_new_user"


    /**
     * 通用｜用户登录类型
     * 用户登录类型： unknown = 其他或未知 phone = 手机号 wechat = 微信 AppleID = AppleID
     */
    const val KEY_ALL_LOGIN_TYPE = "all_login_type"
}
