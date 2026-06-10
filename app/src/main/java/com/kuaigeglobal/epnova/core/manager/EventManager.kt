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
     * Firebase 埋点入口。
     *
     * 新版埋点文档里的事件统一走这里，后续按页面逐点接入。
     */
    @JvmStatic
    fun onFirebaseTrack(eventName: String, paramsMap: Map<String, Any?>? = null) {
        Logger.d("[onFirebaseTrack] Firebase Event -> Code: $eventName, Params: $paramsMap")
        AnalyticsHelper.logEvent(eventName, paramsMap)
    }

    /**
     * Firebase 埋点入口，便于调用处用 "key" to value 的形式传参。
     */
    @JvmStatic
    fun onFirebaseTrack(eventName: String, vararg params: Pair<String, Any?>) {
        onFirebaseTrack(eventName, if (params.isEmpty()) null else params.toMap())
    }

}

/**
 * Firebase 埋点事件。
 *
 * 来源：EpNova埋点文档_updated.xlsx / 新版。
 */
object FirebaseEvent {
    /**
     * 模块：创作入口
     * 事件类型：曝光
     * 事件名称：创作入口页面曝光
     * 说明：创作入口页面曝光：用户点击底部导航栏 Create tab，创作入口页面成功渲染展示时触发
     */
    const val CREATE_HOME_VIEW = "create_home_view"

    /**
     * 模块：创作入口
     * 事件类型：曝光
     * 事件名称：创作入口顶部banner曝光
     * 说明：创作入口顶部banner曝光：创作入口页面顶部banner图片成功渲染展示时触发
     */
    const val CREATE_BANNER_VIEW = "create_banner_view"

    /**
     * 模块：创作入口
     * 事件类型：点击
     * 事件名称：创作入口顶部banner点击
     * 说明：创作入口顶部banner点击：用户点击顶部banner图片时触发
     */
    const val CREATE_BANNER_CLICK = "create_banner_click"

    /**
     * 模块：创作入口
     * 事件类型：点击
     * 事件名称：点击Creating Short Dramas按钮
     * 说明：点击Creating Short Dramas按钮：用户点击创作入口页面中部"Creating Short Dramas"按钮时触发
     */
    const val CREATE_BTN_CLICK = "create_btn_click"

    /**
     * 模块：创作入口
     * 事件类型：点击
     * 事件名称：点击Resource Center tab
     * 说明：点击Resource Center tab：用户在创作入口页面主动切换至Resource Center tab时触发，默认选中Top Earners不触发
     */
    const val CREATE_RESOURCE_CENTER_CLICK = "create_resource_center_click"

    /**
     * 模块：创作入口
     * 事件类型：点击
     * 事件名称：点击top earners展示的作品封面
     * 说明：点击top earners展示的作品封面：用户点击Top Earners列表中任意作品卡片时触发
     * 参数：需同时上报点击的对应的作品的id（drama_id)
     */
    const val CREATE_TOP_EARNERS_DRAMA_CLICK = "create_top_earners_drama_click"

    /**
     * 模块：商店
     * 事件类型：曝光
     * 事件名称：商店曝光
     * 说明：商店曝光：用户进入商店页面时触发
     */
    const val STORE_VIEW = "store_view"

    /**
     * 模块：商店
     * 事件类型：曝光
     * 事件名称：交易历史记录明细页曝光
     * 说明：交易历史记录明细页曝光：用户进入交易历史记录明细页面时触发
     */
    const val TRANSACTION_HISTORY_VIEW = "transaction_history_view"

    /**
     * 模块：商店
     * 事件类型：点击
     * 事件名称：点击切换至credits购买页
     * 说明：点击切换至credits购买页：用户在商店页面主动切换至Credits tab时触发，默认选中Coins不触发
     */
    const val STORE_CREDITS_CLICK = "store_credits_click"

    /**
     * 模块：商店
     * 事件类型：点击
     * 事件名称：商店内点击套餐卡片
     * 说明：商店内点击套餐卡片：用户在商店页面点击任意充值套餐卡片时触发，同时上报套餐id和价格
     * 参数：需同时上报:
     * 1. 商品id（product_id)
     * 2. 货币类型（currency_type: coins / credits)
     * 3. 当时的商品价格（price）
     */
    const val STORE_PRODUCT_CLICK = "store_product_click"

    /**
     * 模块：商店
     * 事件类型：行为
     * 事件名称：商店内点击套餐卡片并支付成功
     * 说明：商店内支付成功：用户在商店页面发起支付后，支付成功时触发
     * 参数：需同时上报:
     * 1. 商品id（product_id)
     * 2. 货币类型（currency_type: coins / credits)
     * 3. 当时的商品价格（price）
     */
    const val STORE_PRODUCT_SUCCESS = "store_product_success"

    /**
     * 模块：Credits首充礼包
     * 事件类型：曝光
     * 事件名称：Credits的首充礼包曝光
     * 说明：Credits的首充礼包曝光：Credits首充礼包弹窗成功展示，或商城/充值弹窗里的算力首充礼包卡片展示时触发
     * 参数：因有两个触发场景，需同时上报：
     * 1. 触发场景（trigger：create_btn / insufficient_balance)
     * 2. 礼包id（product_id）
     * 3. 当时的礼包价格（price）
     * 参数说明：注：create_btn为点击开始创作后弹出
     * 的场景，insufficient_balance为创作
     * 流程后余额不足的触发场景
     */
    const val CREDITS_FIRST_PURCHASE_GIFT_POPUP_VIEW = "credits_first_purchase_gift_popup_view"

    /**
     * 模块：Credits首充礼包
     * 事件类型：点击
     * 事件名称：Credits的首充礼包点击
     * 说明：Credits的首充礼包点击：用户点击Credits首充礼包套餐卡片或弹窗购买按钮时触发
     */
    const val CREDITS_FIRST_PURCHASE_GIFT_POPUP_CLICK = "credits_first_purchase_gift_popup_click"

    /**
     * 模块：Credits首充礼包
     * 事件类型：行为
     * 事件名称：Credits的首充礼包支付成功
     * 说明：Credits的首充礼包支付成功：用户在弹窗、商城或充值弹窗完成Credits首充礼包支付时触发
     */
    const val CREDITS_FIRST_PURCHASE_GIFT_POPUP_SUCCESS = "credits_first_purchase_gift_popup_success"

    /**
     * 模块：Coins 首充礼包
     * 事件类型：曝光
     * 事件名称：Coins的首充礼包曝光
     * 说明：Coins的首充礼包曝光：Coins首充礼包弹窗成功展示，或商城/充值弹窗里的首充礼包卡片展示时触发
     */
    const val COINS_FIRST_PURCHASE_GIFT_POPUP_VIEW = "coins_first_purchase_gift_popup_view"

    /**
     * 模块：Coins 首充礼包
     * 事件类型：点击
     * 事件名称：Coins的首充礼包点击
     * 说明：Coins的首充礼包点击：用户点击Coins首充礼包套餐卡片或弹窗购买按钮时触发
     */
    const val COINS_FIRST_PURCHASE_GIFT_POPUP_CLICK = "coins_first_purchase_gift_popup_click"

    /**
     * 模块：Coins 首充礼包
     * 事件类型：行为
     * 事件名称：Coins的首充礼包支付成功
     * 说明：Coins的首充礼包支付成功：用户在弹窗、商城或充值弹窗完成Coins首充礼包支付时触发
     */
    const val COINS_FIRST_PURCHASE_GIFT_POPUP_SUCCESS = "coins_first_purchase_gift_popup_success"

    /**
     * 模块：提现
     * 事件类型：曝光
     * 事件名称：提现页面曝光
     * 说明：提现页面曝光：用户进入收益提现页面时触发
     */
    const val INCOME_PAGE_VIEW = "Income_page_view"

    /**
     * 模块：提现
     * 事件类型：点击
     * 事件名称：提现按钮点击
     * 说明：提现按钮点击：用户点击提现页面Withdraw按钮时触发，同时上报当前可提现余额
     * 参数：需同时上报：
     * 用户可提现金额（available_balance)
     */
    const val INCOME_WITHDRAW_CLICK = "Income_withdraw_click"

    /**
     * 模块：作品发布
     * 事件类型：曝光
     * 事件名称：ugc剧集详情页曝光
     * 说明：剧集详情页曝光：用户进入ugc剧集详情页时触发
     * 参数：需同时上报：
     * 剧id（drama_id)
     */
    const val UGC_DRAMA_DETAIL_VIEW = "ugc_drama_detail_view"

    /**
     * 模块：作品发布
     * 事件类型：点击
     * 事件名称：ugc剧集详情页点击发布按钮
     * 说明：剧集详情页点击发布按钮：用户点击剧集详情页Publish按钮时触发，包含余额不足等被拦截情况
     * 参数：需同时上报：
     * 1. 剧id（drama_id)
     */
    const val UGC_DRAMA_PUBLISH_CLICK = "ugc_drama_publish_click"

    /**
     * 模块：作品发布
     * 事件类型：曝光
     * 事件名称：作品发布h5页面曝光
     * 说明：作品发布h5页面曝光：用户首次发布时进入H5发布页面时触发
     */
    const val PUBLISH_H5_PAGE_VIEW = "publish_h5_page_view"

    /**
     * 模块：my list
     * 事件类型：曝光
     * 事件名称：my_list页面曝光
     * 说明：my_list页面曝光：用户进入My List页面时触发
     */
    const val MY_LIST_VIEW = "my_list_view"

    /**
     * 模块：my list
     * 事件类型：点击
     * 事件名称：my_list页面内切换至History tab
     * 说明：my_list页面内切换至History tab：用户在My List页面主动切换至History tab时触发
     */
    const val MY_LIST_HISTORY_CLICK = "my_list_history_click"

    /**
     * 模块：my list
     * 事件类型：点击
     * 事件名称：my_list页面内切换至Collection tab
     * 说明：my_list页面内切换至Collection tab：用户在My List页面主动切换至Collection tab时触发
     */
    const val MY_LIST_COLLECTION_CLICK = "my_list_collection_click"

    /**
     * 模块：push
     * 事件类型：行为
     * 事件名称：用户站外点击推送
     * 说明：用户站外点击推送：用户在App外点击推送通知打开App时触发
     * 实现边界：点击来源用客户端写入的 from_push 标识判断；不从 FCM payload 读取非固定字段。
     */
    const val PUSH_CLICK = "push_click"

    /**
     * 模块：福利中心
     * 事件类型：曝光
     * 事件名称：福利中心页面曝光
     * 说明：福利中心页面曝光：用户进入福利中心页面时触发
     */
    const val REWARDS_CENTER_PAGE_VIEW = "rewards_center_page_view"

    /**
     * 模块：福利中心
     * 事件类型：点击
     * 事件名称：福利中心首页入口点击
     * 说明：福利中心首页入口点击：用户点击首页福利中心入口时触发
     */
    const val HOME_REWARDS_CENTER_BTN_CLICK = "home_rewards_center_btn_click"

    /**
     * 模块：福利中心
     * 事件类型：点击
     * 事件名称：福利中心个人中心入口点击
     * 说明：福利中心个人中心入口点击：用户点击个人中心页面福利中心入口时触发
     */
    const val PROFILE_REWARDS_CENTER_BTN_CLICK = "profile_rewards_center_btn_click"

    /**
     * 模块：福利中心
     * 事件类型：行为
     * 事件名称：金币奖励发放
     * 说明：金币奖励发放：用户通过签到、看广告、完成任务等方式获得虚拟货币奖励时触发，同时上报奖励类型、数量及触发来源
     * 参数：需同时上报：
     * 1. 发放数量（amount）
     * 2. 奖励来源（granted_source: check_in / check_in_ad / task_done / login_popup )
     * 3. 任务id（task_id)     // 仅granted_source = task_done时上报
     * 参数说明：奖励来源（granted_source: check_in（签到）/
     * check_in_ad （签到后的一次激励广告机会）/ task_done（完成任务，
     * 这里包含登录奖励、开启消息通知权限任务、广告任务等) / login_popup（登录弹窗）
     */
    const val REWARD_COINS_GRANTED = "reward_coins_granted"

    /**
     * 模块：首页
     * 事件类型：曝光
     * 事件名称：首页曝光
     * 说明：首页曝光：用户进入首页时触发
     */
    const val HOME_PAGE_VIEW = "home_page_view"

    /**
     * 模块：首页
     * 事件类型：曝光
     * 事件名称：首页banner曝光
     * 说明：首页banner曝光：首页顶部banner图片成功渲染展示时触发
     * 参数：需同时上报：
     * banner_id
     */
    const val HOME_BANNER_VIEW = "home_banner_view"

    /**
     * 模块：首页
     * 事件类型：点击
     * 事件名称：首页banner点击
     * 说明：首页banner点击：用户点击首页banner时触发
     * 参数：需同时上报：
     * banner_id
     */
    const val HOME_BANNER_CLICK = "home_banner_click"

    /**
     * 模块：首页
     * 事件类型：曝光
     * 事件名称：首页剧封面曝光
     * 说明：首页剧封面曝光：首页剧集卡片出现在用户可视区域内时触发，需客户端做可见性检测
     * 参数：需同时上报：
     * 剧id（drama_id)
     */
    const val HOME_DRAMA_VIEW = "home_drama_view"

    /**
     * 模块：首页
     * 事件类型：点击
     * 事件名称：首页剧封面点击
     * 说明：首页剧封面点击：用户点击首页任意剧集封面卡片时触发
     * 参数：需同时上报：
     * 剧id（drama_id）
     */
    const val HOME_DRAMA_CLICK = "home_drama_click"

    /**
     * 模块：播放器
     * 事件类型：行为
     * 事件名称：进入播放器
     * 说明：进入播放器：用户进入视频播放器页面，视频开始自动播放时触发，同时上报剧id、集id及进入来源
     * 参数：需同时上报：
     * 1. 剧id（drama_id）
     * 2. 集id（episode_id)
     * 3. 从哪里进入播放器（source: home / banner / search_page / history / collection / top_earners)
     * 4. banner_id（仅source = banner时传）
     * 参数说明：从哪里进入播放器页面说明
     * source: home（主页） / banner（任意banner） / search_page（搜索页，指的是输入文本点击搜索后出现的作品封面） / history（历史记录） / collection（收藏） / top_earners（创作入口页的top_earners处）)
     */
    const val PLAYER_VIEW = "player_view"

    /**
     * 模块：播放器
     * 事件类型：行为
     * 事件名称：有效播放
     * 说明：有效播放：视频播放进度达到10%时触发，标记为一次有效播放
     * 参数：触发时机：当播放进度>10%的时候
     * 需同时上报：
     * 1. 剧id（drama_id)
     * 2. 集id（episode_id)
     * 3.（文档未补充字段名）
     */
    const val PLAYER_VALID_PLAY = "player_valid_play"

    /**
     * 模块：播放器
     * 事件类型：行为
     * 事件名称：播放进度
     * 说明：播放进度：视频播放进度分别达到25%、50%、75%时各触发一次，用于分析用户在各阶段的流失情况
     * 参数：触发时机：当播放进度分别等于25%, 50%和75%的时候
     * 需同时上报：
     * 1. 剧id（drama_id)
     * 2. 集id（episode_id)
     * 3. 播放进度（process: 25/ 50/ 75)
     */
    const val PLAYER_PROCESS = "player_process"

    /**
     * 模块：播放器
     * 事件类型：行为
     * 事件名称：完播
     * 说明：完播：视频播放进度达到90%及以上时触发，标记为一次完播
     * 参数：触发时机：当播放进度>10%的时候
     * 需同时上报：
     * 1. 剧id（drama_id)
     * 2. 集id（episode_id)
     */
    const val PLAYER_COMPLETE = "player_complete"

    /**
     * 模块：创作流程
     * 事件类型：曝光
     * 事件名称：用户进入故事灵感页面
     * 说明：用户进入故事灵感页面，显示的时候上报
     */
    const val STORY_INSPIRATION_VIEW = "story_inspiration_view"

    /**
     * 模块：创作流程
     * 事件类型：点击
     * 事件名称：故事灵感tab
     * 说明：用户点击故事灵感tab
     */
    const val STORY_INSPIRATION_TAB_CLICK = "story_inspiration_tab_click"

    /**
     * 模块：创作流程
     * 事件类型：点击
     * 事件名称：故事灵感页面的下一步按钮
     * 说明：用户在故事灵感tab页 输入文字后点击下一步按钮
     */
    const val STORY_INSPIRATION_NEXT_CLICK = "story_inspiration_next_click"

    /**
     * 模块：创作流程
     * 事件类型：点击
     * 事件名称：小说改编tab页面
     * 说明：用户点击小说改编tab
     */
    const val NOVEL_ADAPTATION_TAB_CLICK = "novel_adaptation_tab_click"

    /**
     * 模块：创作流程
     * 事件类型：点击
     * 事件名称：小说改编页面的下一步按钮
     * 说明：用户在小说改编tab页 输入文字后点击下一步按钮
     */
    const val NOVEL_ADAPTATION_NEXT_CLICK = "novel_adaptation_next_click"

    /**
     * 模块：创作流程
     * 事件类型：曝光
     * 事件名称：故事梗概环节
     * 说明：用户进入创作流程中的故事梗概环节页面
     * 参数：需同时上报：
     * 1. 创作来源类型：creation_source_type: novel / inspiration
     * 参数说明：创作来源类型：小说 / 灵感
     */
    const val STORY_OUTLINE_VIEW = "story_outline_view"

    /**
     * 模块：创作流程
     * 事件类型：点击
     * 事件名称：故事梗概环节的下一步按钮
     * 说明：用户在故事梗概环节想要进入下一步，点击下一步按钮时上报
     */
    const val STORY_OUTLINE_NEXT_CLICK = "story_outline_next_click"

    /**
     * 模块：创作流程
     * 事件类型：点击
     * 事件名称：创建作品页选择“单集短片”
     * 说明：用户点击单集短片按钮时上报
     */
    const val SINGLE_EPISODE_CLICK = "single_episode_click"

    /**
     * 模块：创作流程
     * 事件类型：点击
     * 事件名称：创建作品页 输入“自定义”集数
     * 说明：用户点击自定义输入集数时上报
     */
    const val CUSTOM_EPISODE_INPUT = "custom_episode_input"

    /**
     * 模块：创作流程
     * 事件类型：点击
     * 事件名称：创建作品页“开始创作”按钮
     * 说明：用户填完作品信息，点击开始创作按钮
     */
    const val START_CREATION_CLICK = "start_creation_click"

    /**
     * 模块：创作流程
     * 事件类型：曝光
     * 事件名称：故事剧本环节
     * 说明：用户进入创作流程中的故事剧本环节页面
     */
    const val SCRIPT_VIEW = "script_view"

    /**
     * 模块：创作流程
     * 事件类型：点击
     * 事件名称：故事剧本环节的下一步按钮
     * 说明：用户在故事剧本环节想要进入下一步，点击下一步按钮时上报
     */
    const val SCRIPT_NEXT_CLICK = "script_next_click"

    /**
     * 模块：创作流程
     * 事件类型：曝光
     * 事件名称：资产管理环节
     * 说明：用户进入资产管理页面（角色/场景/道具管理）
     */
    const val ASSET_MANAGEMENT_VIEW = "asset_management_view"

    /**
     * 模块：创作流程
     * 事件类型：点击
     * 事件名称：资产管理环节的下一步按钮
     * 说明：用户在资产管理环节想要进入下一步，点击下一步按钮时上报
     */
    const val ASSET_MANAGEMENT_NEXT_CLICK = "asset_management_next_click"

    /**
     * 模块：创作流程
     * 事件类型：点击
     * 事件名称：资产管理页的一键生成按钮
     * 说明：用户在资产管理页面点击“一键生成”按钮进行资产生成
     */
    const val ASSET_GENERATE_CLICK = "asset_generate_click"

    /**
     * 模块：创作流程
     * 事件类型：曝光
     * 事件名称：分镜设计环节
     * 说明：用户进入分镜设计环节页面
     */
    const val STORYBOARD_VIEW = "storyboard_view"

    /**
     * 模块：创作流程
     * 事件类型：点击
     * 事件名称：分镜设计环节“生成分镜”按钮
     * 说明：用户在分镜设计环节想要进入下一步，点击 生成分镜 按钮时上报
     */
    const val STORYBOARD_GENERATE_CLICK = "storyboard_generate_click"

    /**
     * 模块：创作流程
     * 事件类型：曝光
     * 事件名称：分镜视频生成环节
     * 说明：用户进入创作流程中的分镜视频生成环节页面
     */
    const val VIDEO_GENERATION_VIEW = "video_generation_view"

    /**
     * 模块：创作流程
     * 事件类型：点击
     * 事件名称：分镜预览页面的“下载视频”按钮
     * 说明：在分镜预览页面，用户点击“下载视频”按钮
     */
    const val VIDEO_DOWNLOAD_CLICK = "video_download_click"

    /**
     * 模块：创作流程
     * 事件类型：点击
     * 事件名称：分镜预览页面的重绘分镜按钮
     * 说明：在重绘已合成视频页面 或新集创作的分镜视频合成页面，用户点击左下角“重绘分镜”按钮
     */
    const val STORYBOARD_REDRAW_CLICK = "storyboard_redraw_click"

    /**
     * 模块：创作流程
     * 事件类型：点击
     * 事件名称：重绘分镜页面的“立即重绘”按钮
     * 说明：在单个分镜视频设计页面，用户点击“立即重绘”按钮
     */
    const val REDRAW_NOW_CLICK = "redraw_now_click"

    /**
     * 模块：创作流程
     * 事件类型：点击
     * 事件名称：重绘分镜页面的“重新生成”按钮
     * 说明：在单个分镜视频设计页面，用户点击“重新生成”按钮
     */
    const val REGENERATE_CLICK = "regenerate_click"

    /**
     * 模块：创作流程
     * 事件类型：点击
     * 事件名称：合成按钮
     * 说明：在分镜视频生成页面 和重绘视频页面的 右上角的用户点击“合成”按钮开始视频生成
     */
    const val COMBINE_VIDEO_CLICK = "combine_video_click"

    /**
     * 模块：创作流程
     * 事件类型：点击
     * 事件名称：创建续集按钮
     * 说明：在作品续集页，用户点击创建续集按钮
     */
    const val CREATE_SEQUEL_CLICK = "create_sequel_click"

    /**
     * 模块：创作流程
     * 事件类型：点击
     * 事件名称：用户作品播放页重绘视频按钮
     * 说明：在用户作品播放页中，用户点击“重绘视频”按钮进行视频重绘
     */
    const val WORK_REPLAY_REDRAW_CLICK = "work_replay_redraw_click"

    /**
     * 模块：通用
     * 事件类型：行为
     * 事件名称：新用户首次打开app
     * 说明：新用户首次打开app
     * 参数：设备首次安装打开app时触发
     */
    const val APP_FIRST_OPEN = "app_first_open"

    /**
     * 模块：登录弹窗
     * 事件类型：曝光
     * 事件名称：登录弹窗曝光
     * 说明：登录弹窗曝光
     */
    const val LOGIN_POPUP_VIEW = "login_popup_view"

    /**
     * 模块：登录弹窗
     * 事件类型：点击
     * 事件名称：登录弹窗点击Claim按钮
     * 说明：登录弹窗
     */
    const val LOGIN_POPUP_CLAIM_CLICK = "login_popup_claim_click"

    /**
     * 模块：登录弹窗
     * 事件类型：点击
     * 事件名称：登录弹窗点击关闭
     * 说明：登录弹窗点击关闭
     */
    const val LOGIN_POPUP_CLOSE_CLICK = "login_popup_close_click"

    /**
     * 模块：登录弹窗
     * 事件类型：行为
     * 事件名称：登录成功
     * 说明：登录成功
     * 参数：需要同时上报：
     * 登录方式（login_way: facebook / apple / google）
     */
    const val LOGIN_SUCCESS = "login_success"
}

/**
 * Firebase 埋点参数 key。
 *
 * 来源：EpNova埋点文档_updated.xlsx / 新版。
 */
object FirebaseEventParam {
    /** 剧 id */
    const val DRAMA_ID = "drama_id"

    /** 集 id */
    const val EPISODE_ID = "episode_id"

    /** banner id */
    const val BANNER_ID = "banner_id"

    /** 商品或礼包 id */
    const val PRODUCT_ID = "product_id"

    /** 货币类型：coins / credits */
    const val CURRENCY_TYPE = "currency_type"

    /** 商品或礼包价格 */
    const val PRICE = "price"

    /** 触发场景：create_btn / insufficient_balance */
    const val TRIGGER = "trigger"

    /** 用户可提现金额 */
    const val AVAILABLE_BALANCE = "available_balance"

    /** 奖励发放数量 */
    const val AMOUNT = "amount"

    /** 奖励来源：check_in / check_in_ad / task_done / login_popup */
    const val GRANTED_SOURCE = "granted_source"

    /** 任务 id，仅 granted_source = task_done 时上报 */
    const val TASK_ID = "task_id"

    /** 播放器进入来源：home / banner / search_page / history / collection / top_earners */
    const val SOURCE = "source"

    /** 播放进度：25 / 50 / 75 */
    const val PROCESS = "process"

    /** 创作来源类型：novel / inspiration */
    const val CREATION_SOURCE_TYPE = "creation_source_type"

    /** 登录方式：facebook / apple / google */
    const val LOGIN_WAY = "login_way"
}

/**
 * Firebase 埋点参数值。
 *
 * 来源：EpNova埋点文档_updated.xlsx / 新版。
 */
object FirebaseEventValue {
    /** currency_type: coins */
    const val CURRENCY_COINS = "coins"

    /** currency_type: credits */
    const val CURRENCY_CREDITS = "credits"

    /** trigger: create_btn */
    const val TRIGGER_CREATE_BTN = "create_btn"

    /** trigger: insufficient_balance */
    const val TRIGGER_INSUFFICIENT_BALANCE = "insufficient_balance"

    /** granted_source: check_in */
    const val GRANTED_SOURCE_CHECK_IN = "check_in"

    /** granted_source: check_in_ad */
    const val GRANTED_SOURCE_CHECK_IN_AD = "check_in_ad"

    /** granted_source: task_done */
    const val GRANTED_SOURCE_TASK_DONE = "task_done"

    /** granted_source: login_popup */
    const val GRANTED_SOURCE_LOGIN_POPUP = "login_popup"

    /** source: home */
    const val SOURCE_HOME = "home"

    /** source: banner */
    const val SOURCE_BANNER = "banner"

    /** source: search_page */
    const val SOURCE_SEARCH_PAGE = "search_page"

    /** source: history */
    const val SOURCE_HISTORY = "history"

    /** source: collection */
    const val SOURCE_COLLECTION = "collection"

    /** source: top_earners */
    const val SOURCE_TOP_EARNERS = "top_earners"

    /** creation_source_type: novel */
    const val CREATION_SOURCE_NOVEL = "novel"

    /** creation_source_type: inspiration */
    const val CREATION_SOURCE_INSPIRATION = "inspiration"

    /** login_way: facebook */
    const val LOGIN_WAY_FACEBOOK = "facebook"

    /** login_way: google */
    const val LOGIN_WAY_GOOGLE = "google"

    /** login_way: apple */
    const val LOGIN_WAY_APPLE = "apple"
}
