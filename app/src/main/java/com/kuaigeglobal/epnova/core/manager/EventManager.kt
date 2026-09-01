package com.kuaigeglobal.epnova.core.manager

import com.google.firebase.messaging.FirebaseMessaging
import com.kuaigeglobal.epnova.App
import com.kuaigeglobal.epnova.common.ext.ioScope
import com.kuaigeglobal.epnova.core.google.GoogleServices
import com.kuaigeglobal.epnova.core.google.event.AnalyticsHelper
import com.kuaigeglobal.epnova.core.User
import com.kuaigeglobal.epnova.core.network.apis.ApiFactory
import com.kuaigeglobal.epnova.core.network.apis.entity.CreatorPlayableEpisode
import com.kuaigeglobal.epnova.core.network.apis.entity.EpisodeInfo
import com.kuaigeglobal.epnova.core.network.apis.request.FcmTokenBody
import com.kuaigeglobal.epnova.core.network.apis.request.PlayEventBody
import com.kuaigeglobal.epnova.core.network.apis.request.ReportCreationUsageBody
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

    const val REPORT_CREATION_TYPE_INSPIRATION = 1
    const val REPORT_CREATION_TYPE_HOT_CASE = 2

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
        if (!GoogleServices.isPlayServicesAvailable(App.instance)) {
            Logger.w("EventManager", "Skip FCM token fetch: Google Play services unavailable")
            return
        }

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
     * 用户从 App 外点击推送通知时上报 GA4。
     *
     * 仅接受埋点表中约定的业务类型；旧版 pushMsgType=1/2 属于通知展示类型，不参与该埋点。
     */
    fun reportPushClick(pushType: String?) {
        pushType?.let {
            onFirebaseTrack(
                FirebaseEvent.Category.PUSH,
                FirebaseEvent.Action.PUSH_CLICK,
                pushType
            )
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

    fun reportCreationUsageCount(reportId: String?, reportType: Int?) {
        if (reportId.isNullOrBlank() || reportType == null) return
        ioScope.launchNoCrash {
            ApiFactory.apiService.reportCreationUsageCount(
                ReportCreationUsageBody(reportId, reportType)
            )
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
     * 按 Event-Action-Label 结构上报 GA4 自定义事件。
     *
     * Event 使用 [FirebaseEvent.Category]，Action 使用 [FirebaseEvent.Action]；
     * Label 按表格顺序传入，空值不会写入事件参数。
     */
    @JvmStatic
    fun onFirebaseTrack(
        event: String,
        action: String,
        label1: Any? = null,
        label2: Any? = null,
        label3: Any? = null,
        label4: Any? = null,
    ) {
        val params = buildMap<String, Any?> {
            put(FirebaseEvent.Label.ACTION, action)
            label1?.let { put(FirebaseEvent.Label.LABEL_1, it) }
            label2?.let { put(FirebaseEvent.Label.LABEL_2, it) }
            label3?.let { put(FirebaseEvent.Label.LABEL_3, it) }
            label4?.let { put(FirebaseEvent.Label.LABEL_4, it) }
        }
        Logger.d("[onFirebaseTrack] GA4 Event: $event, Action: $action")
        AnalyticsHelper.logEvent(event, params)
    }

}

/**
 * GA4 埋点定义，统一使用 Event-Action-Label 结构。
 *
 * 数据来源：EpNova GA埋点.xlsx（1.3.2 A1:M70；1.3.6 A1:M14；1.3.8 A1:M9，按业务批次接入）。
 */
object FirebaseEvent {

    /** Event：用于区分业务模块。 */
    object Category {
        /** 首次打开 APP。 */
        const val FIRST_OPEN = "app_first_open"
        /** 登录。 */
        const val LOGIN = "login"
        /** 首页。 */
        const val HOME = "home"
        /** 创作流程。 */
        const val CREATE = "create"
        /** 发布。 */
        const val PUBLISH = "publish"
        /** 精选。 */
        const val FEATURE = "feature"
        /** 商店。 */
        const val STORE = "store"
        /** Credits 首充礼包。 */
        const val FIRST_PURCHASE_CREDITS = "first_purchase_credits"
        /** Coins 首充礼包；值按表格保留为单数 coin。 */
        const val FIRST_PURCHASE_COIN = "first_purchase_coin"
        /** 我的列表。 */
        const val MY_LIST = "my_list"
        /** 福利中心。 */
        const val REWARDS = "rewards"
        /** 播放器。 */
        const val PLAYER = "player"
        /** 推送。 */
        const val PUSH = "push"
        /** 创作同款。 */
        const val HIT_SCRIPTS = "hit_scripts"
        /** 会员商店。 */
        const val MEMBERSHIP = "membership"
        /** 会员升级。 */
        const val MEMBERSHIP_UPGRADE = "membership_upgrade"
        /** 付费剧集解锁。 */
        const val PAID_EPISODE = "paid_episode"
        /** 好评弹窗。 */
        const val RATING = "rating"
        /** 合成播放页。 */
        const val WORK_PLAY = "work_play"
    }

    /** GA4 事件参数 key，顺序与表格的 Action、Label 1～4 一致。 */
    object Label {
        /** 数据行为。 */
        const val ACTION = "Action"
        /** 第一业务维度。 */
        const val LABEL_1 = "Label1"
        /** 第二业务维度。 */
        const val LABEL_2 = "Label2"
        /** 第三业务维度。 */
        const val LABEL_3 = "Label3"
        /** 第四业务维度。 */
        const val LABEL_4 = "Label4"
    }

    /** Action：同名 Action 可在不同 Event 下复用，具体 Label 规则见各常量注释。 */
    object Action {
        /** [first_open] 新用户首次打开 App。 */
        const val OPEN = "open"
        /** [login] 欢迎弹窗展示；值按表格保留为 welcom_popup_show。 */
        const val WELCOME_POPUP_SHOW = "welcom_popup_show"
        /** [login] 欢迎弹窗确认按钮点击。 */
        const val WELCOME_CONFIRM_CLICK = "welcome_confirm_click"
        /** [login] 欢迎弹窗关闭按钮点击。 */
        const val WELCOME_CLOSE_CLICK = "welcome_close_click"
        /** [login] 登录页面展示。 */
        const val LOGIN_SHOW = "login_show"
        /** [login] 登录点击；Label1=登录方式（google/apple）。 */
        const val LOGIN_CLICK = "login_click"
        /** [login] 登录结果；Label1=登录方式，Label2=结果（success/fail）。 */
        const val LOGIN_RESULT = "login_result"
        /** [home] 首页曝光。 */
        const val HOME_SHOW = "home_show"
        /** [home]/[feature] 剧封面点击；Label1=剧 ID。 */
        const val DRAMA_CLICK = "drama_click"
        /** [home] Banner 点击；Label1=Banner ID。 */
        const val BANNER_CLICK = "banner_click"
        /** [create] 故事灵感页面展示。 */
        const val INSPIRATION_SHOW = "inspiration_show"
        /** [create] 故事灵感 Tab 点击。 */
        const val INSPIRATION_TAB_CLICK = "inspiration_tab_click"
        /** [create] 故事灵感页面下一步点击。 */
        const val INSPIRATION_NEXT_CLICK = "inspiration_next_click"
        /** [create] 小说改编 Tab 点击。 */
        const val NOVEL_TAB_CLICK = "novel_tab_click"
        /** [create] 小说改编页面下一步点击。 */
        const val NOVEL_NEXT_CLICK = "novel_next_click"
        /** [create] Create Work 弹窗展示。 */
        const val CREATE_WORK_SHOW = "create_work_show"
        /** [create] 开始创作；Label1=来源，Label2=集数，Label3=视频比例，Label4=风格英文名。 */
        const val START_CREATING_CLICK = "start_creating_click"
        /** [create] 故事梗概环节展示。 */
        const val SYNOPSIS_SHOW = "synopsis_show"
        /** [create] 故事梗概下一步点击。 */
        const val SYNOPSIS_NEXT_CLICK = "synopsis_next_click"
        /** [create] 剧本页面展示。 */
        const val SCRIPT_SHOW = "script_show"
        /** [create] 剧本环节下一步点击。 */
        const val SCRIPT_NEXT_CLICK = "script_next_click"
        /** [create] 资产创建页面展示。 */
        const val ASSET_SHOW = "asset_show"
        /** [create] 资产一键生成点击。 */
        const val ASSET_GENERATE_CLICK = "asset_generate_click"
        /** [create] 资产管理下一步点击。 */
        const val ASSET_NEXT_CLICK = "asset_next_click"
        /** [create] 分镜设计页面展示。 */
        const val STORYBOARD_SHOW = "storyboard_show"
        /** [create] 生成分镜点击；Label1=模型（dynamic_comic/Narration_comic）。 */
        const val STORYBOARD_GENERATE_CLICK = "storyboard_generate_click"
        /** [create] 分镜视频生成页面展示。 */
        const val PREVIEW_VIDEO_SHOW = "preview_video_show"
        /** [create] 分镜预览下载视频点击。 */
        const val PREVIEW_VIDEO_DOWNLOAD_CLICK = "preview_video_download_click"
        /** [create] 分镜预览重绘分镜点击。 */
        const val STORYBOARD_REDRAW_CLICK = "storyboard_redraw_click"
        /** [create] 单个分镜立即重绘点击。 */
        const val REDRAW_NOW_CLICK = "redraw_now_click"
        /** [create] 合成视频点击。 */
        const val COMBINE_VIDEO_CLICK = "combine_video_click"
        /** [create] 作品播放页重绘点击。 */
        const val WORK_REDRAW_CLICK = "work_redraw_click"
        /** [create] 重绘二次确认弹窗确定点击。 */
        const val REDRAW_CONFIRM_CLICK = "redraw_confirm_click"
        /** [publish] 作品详情页展示。 */
        const val WORK_DETAIL_SHOW = "work_detail_show"
        /** [publish] 下一集 Pending Creation 点击。 */
        const val PENDING_CREATION_CLICK = "pending_creation_click"
        /** [publish] 作品详情页发布按钮点击。 */
        const val WORK_DETAIL_PUBLISH_CLICK = "work_detail_publish_click"
        /** [publish] 作品发布 H5 页面展示。 */
        const val PUBLISH_H5_SHOW = "publish_h5_show"
        /** [publish] 身份验证填写弹窗展示。 */
        const val IDENTITY_VERIFICATION_SHOW = "identity_verification_show"
        /** [publish] 身份验证提交点击。 */
        const val IDENTITY_VERIFICATION_SUBMIT_CLICK = "identity_verification_submit_click"
        /** [feature] 精选页面展示。 */
        const val FEATURED_SHOW = "featured_show"
        /** [feature] Creating Short Dramas 按钮点击。 */
        const val CREATE_BTN_CLICK = "create_btn_click"
        /** [store] 商店页面展示。 */
        const val STORE_SHOW = "store_show"
        /** [store] 交易历史记录明细页展示。 */
        const val TRANSACTION_HISTORY_SHOW = "transaction_history_show"
        /** [membership] 会员页面展示；Label1=触发场景。 */
        const val MEMBERSHIP_SHOW = "membership_show"
        /** [membership_upgrade] 会员升级页面展示。 */
        const val UPGRADE_SHOW = "upgrade_show"
        /** [store]/[membership]/[membership_upgrade] 商品卡片点击；Label 规则由 Event 决定。 */
        const val PRODUCT_CLICK = "product_click"
        /** [store]/[membership] 支付结果；Label 规则由 Event 决定。 */
        const val PURCHASE_RESULT = "purchase_result"
        /** [membership_upgrade] 升级结果；Label1=升级结果。 */
        const val UPGRADE_RESULT = "upgrade_result"
        /** [paid_episode] 付费剧集曝光；Label1=剧 ID，Label2=集 ID。 */
        const val PAID_EPISODE_SHOW = "paid_episode_show"
        /** [paid_episode] 金币购买弹窗曝光。 */
        const val BUY_COIN_POPUP_SHOW = "buy_coin_popup_show"
        /** [paid_episode] 点击购买金币；Label1=货币数量。 */
        const val BUY_COIN_CLICK = "buy_coin_click"
        /** [paid_episode] 金币购买结果；Label1=支付状态，Label2=货币数量。 */
        const val BUY_COIN_RESULT = "buy_coin_result"
        /** [paid_episode] 点击广告解锁按钮。 */
        const val AD_UNLOCK_CLICK = "ad_unlock_click"
        /** [paid_episode] 广告播放结果；Label1=播放状态。 */
        const val AD_PLAY_RESULT = "ad_play_result"
        /** [paid_episode] 剧集解锁结果；Label1=解锁状态，Label2=解锁来源。 */
        const val AD_UNLOCK_RESULT = "ad_unlock_result"
        /** [first_purchase_credits]/[first_purchase_coin]/[rating] 弹窗展示；首充事件 Label1=触发场景。 */
        const val POPUP_SHOW = "popup_show"
        /** [first_purchase_credits]/[first_purchase_coin] 首充购买按钮点击。 */
        const val PAY_NOW_CLICK = "pay_now_click"
        /** [rating] 评分弹窗按钮点击；Label1=评分分数，Label2=按钮名称。 */
        const val BUTTON_CLICK = "button_click"
        /** [rating] 评分弹窗关闭按钮点击；Label1=评分状态，Label2=评分分数。 */
        const val CLOSE_CLICK = "close_click"
        /** [work_play] 合成播放页展示。 */
        const val WORK_PLAY_SHOW = "work_play_show"
        /** [work_play] Keep Creating 按钮点击。 */
        const val CREATING_CLICK = "creating_click"
        /** [work_play] Download 按钮点击。 */
        const val DOWNLOAD_CLICK = "download_click"
        /** [my_list] My List 页面展示。 */
        const val MY_LIST_SHOW = "my_list_show"
        /** [my_list] 切换至 History Tab。 */
        const val HISTORY_CLICK = "history_click"
        /** [my_list] 切换至 Collection Tab。 */
        const val COLLECTION_CLICK = "collection_click"
        /** [my_list] 进入作品剧集列表；Label1=入口来源。 */
        const val GOTO_EPISODE_LIST = "goto_episode_list"
        /** [my_list] Continue Creating 按钮点击。 */
        const val CONTINUE_CREATING_CLICK = "continue_creating_click"
        /** [rewards] 福利中心页面展示。 */
        const val REWARDS_SHOW = "rewards_show"
        /** [rewards] 首页福利中心入口点击。 */
        const val HOME_REWARDS_BTN_CLICK = "home_rewards_btn_click"
        /** [rewards] 个人中心福利入口点击。 */
        const val PROFILE_REWARDS_BTN_CLICK = "profile_rewards_btn_click"
        /** [rewards] 金币奖励发放；Label1=发放来源，Label2=发放数量。 */
        const val COINS_GRANTED = "coins_granted"
        /** [player] 播放器展示；Label1=剧 ID，Label2=来源。 */
        const val PLAYER_SHOW = "player_show"
        /** [player] 有效播放；Label1=剧 ID，Label2=集 ID。 */
        const val PLAYER_VALID_PLAY = "player_valid_play"
        /** [player] 播放进度；Label1=剧 ID，Label2=集 ID，Label3=进度（25%/50%/75%）。 */
        const val PLAYER_PROCESS = "player_process"
        /** [player] 完播；Label1=剧 ID，Label2=集 ID。 */
        const val PLAYER_COMPLETE = "player_complete"
        /** [push] 服务端发送推送；Label1=推送类型，由服务端上报。 */
        const val PUSH_SEND = "push_send"
        /** [push] 用户站外点击推送；Label1=推送类型。 */
        const val PUSH_CLICK = "push_click"
        /** [hit_scripts] 爆款案例 Remake 点击；Label1=爆款案例名字。 */
        const val REMAKE_BTN_CLICK = "remake_btn_click"
        /** [hit_scripts] 创作同款页面展示。 */
        const val CREATE_SIMILAR_PAGE_SHOW = "create_similar_page_show"
        /** [hit_scripts] 开始创作同款；Label1=案例名，Label2=集数，Label3=比例，Label4=风格英文名。 */
        const val CREATE_SIMILAR_BTN_CLICK = "create_similar_btn_click"
    }

    /** 表格中已枚举的固定 Label 值；ID、数量、自定义集数和风格名等动态值由调用处传入。 */
    object Value {
        /** 登录方式：Google。 */
        const val LOGIN_GOOGLE = "google"
        /** 登录方式：Apple。 */
        const val LOGIN_APPLE = "apple"
        /** 结果：成功。 */
        const val RESULT_SUCCESS = "success"
        /** 结果：失败。 */
        const val RESULT_FAIL = "fail"
        /** 评分状态：未评分。 */
        const val RATING_STATUS_UNSCORED = "unscored"
        /** 评分状态：已评分。 */
        const val RATING_STATUS_SCORED = "scored"
        /** 评分按钮：低分完成。 */
        const val RATING_BUTTON_DONE = "done"
        /** 评分按钮：高分前往商店评分。 */
        const val RATING_BUTTON_RATE_NOW = "rate now"
        /** My List 剧集列表入口：作品卡片。 */
        const val MY_LIST_ENTRY_CARD = "card"
        /** My List 剧集列表入口：All List 按钮。 */
        const val MY_LIST_ENTRY_ALL_LIST_BTN = "all_list_btn"
        /** 创作来源：故事灵感。 */
        const val CREATION_INSPIRATION = "inspiration"
        /** 创作来源：小说改编。 */
        const val CREATION_NOVEL = "novel"
        /** 固定集数：30。 */
        const val EPISODES_30 = "30"
        /** 固定集数：45。 */
        const val EPISODES_45 = "45"
        /** 固定集数：60。 */
        const val EPISODES_60 = "60"
        /** 视频比例：16:9。 */
        const val VIDEO_RATIO_16_9 = "16:9"
        /** 视频比例：9:16。 */
        const val VIDEO_RATIO_9_16 = "9:16"
        /** 分镜模型：Dynamic Comic。 */
        const val MODEL_DYNAMIC_COMIC = "dynamic_comic"
        /** 分镜模型；大小写按表格原值保留。 */
        const val MODEL_NARRATION_COMIC = "Narration_comic"
        /** 货币类型：Coins。 */
        const val CURRENCY_COINS = "coins"
        /** 货币类型：Credits。 */
        const val CURRENCY_CREDITS = "credits"
        /** 商品货币数量：500。 */
        const val CURRENCY_AMOUNT_500 = "500"
        /** 商品货币数量：1000。 */
        const val CURRENCY_AMOUNT_1000 = "1000"
        /** 商品货币数量：1500。 */
        const val CURRENCY_AMOUNT_1500 = "1500"
        /** 商品货币数量：2000。 */
        const val CURRENCY_AMOUNT_2000 = "2000"
        /** 商品货币数量：3000。 */
        const val CURRENCY_AMOUNT_3000 = "3000"
        /** 商品货币数量：4000。 */
        const val CURRENCY_AMOUNT_4000 = "4000"
        /** 商品货币数量：5000。 */
        const val CURRENCY_AMOUNT_5000 = "5000"
        /** 商品货币数量：8000。 */
        const val CURRENCY_AMOUNT_8000 = "8000"
        /** 商品货币数量：10000。 */
        const val CURRENCY_AMOUNT_10000 = "10000"
        /** 页面/弹窗触发场景：点击创作按钮。 */
        const val TRIGGER_CREATE_BTN = "create_btn"
        /** 页面/弹窗触发场景：余额不足。 */
        const val TRIGGER_INSUFFICIENT_BALANCE = "insufficient_balance"
        /** 会员页触发场景：会员入口。 */
        const val TRIGGER_MEMBERSHIP_ENTRY = "membership_entry"
        /** 会员周期：周。 */
        const val MEMBERSHIP_PERIOD_WEEKLY = "Weekly"
        /** 会员周期：月。 */
        const val MEMBERSHIP_PERIOD_MONTHLY = "Monthly"
        /** 剧集解锁来源：金币。 */
        const val UNLOCK_SOURCE_COIN = "coin"
        /** 剧集解锁来源：广告。 */
        const val UNLOCK_SOURCE_AD = "ad"
        /** 金币发放来源：签到。 */
        const val GRANTED_SOURCE_CHECK_IN = "check_in"
        /** 金币发放来源：签到后激励广告。 */
        const val GRANTED_SOURCE_CHECK_IN_AD = "check_in_ad"
        /** 金币发放来源：福利中心广告。 */
        const val GRANTED_SOURCE_AD = "ad"
        /** 金币发放来源：看广告解锁剧集。 */
        const val GRANTED_SOURCE_UNLOCK_AD = "unlock_ad"
        /** 金币发放来源：登录。 */
        const val GRANTED_SOURCE_LOGIN = "login"
        /** 金币发放来源：打开通知权限。 */
        const val GRANTED_SOURCE_NOTICE = "notice"
        /** 播放来源：首页。 */
        const val SOURCE_HOME = "home"
        /** 播放来源：Banner。 */
        const val SOURCE_BANNER = "banner"
        /** 播放来源：搜索页。 */
        const val SOURCE_SEARCH_PAGE = "search_page"
        /** 播放来源：历史记录。 */
        const val SOURCE_HISTORY = "history"
        /** 播放来源：收藏。 */
        const val SOURCE_COLLECTION = "collection"
        /** 播放来源：Top Earners。 */
        const val SOURCE_TOP_EARNERS = "top_earners"
        /** 播放进度：25%。 */
        const val PROCESS_25 = "25%"
        /** 播放进度：50%。 */
        const val PROCESS_50 = "50%"
        /** 播放进度：75%。 */
        const val PROCESS_75 = "75%"
        /** 推送类型：签到。 */
        const val PUSH_TYPE_CHECK_IN = "check_in"
        /** 推送类型：金币礼包。 */
        const val PUSH_TYPE_COINS_GIFT = "coins_gift"
        /** 推送类型：积分礼包。 */
        const val PUSH_TYPE_CREDITS_GIFT = "credits_gift"
        /** 推送类型：创作流程。 */
        const val PUSH_TYPE_CREATION_PROCESS = "creation_process"
        /** 推送类型：发布成功。 */
        const val PUSH_TYPE_PUBLISH_SUCCESS = "publish_success"
    }
}
