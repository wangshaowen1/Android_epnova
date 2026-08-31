package com.kuaigeglobal.epnova.core.network.apis

import com.kuaigeglobal.epnova.core.model.H5PayInfo
import com.kuaigeglobal.epnova.core.network.SSEvent
import com.kuaigeglobal.epnova.core.network.apis.entity.AIPriceInfo
import com.kuaigeglobal.epnova.core.network.apis.entity.AssetGenerateInfo
import com.kuaigeglobal.epnova.core.network.apis.entity.AssetInfo
import com.kuaigeglobal.epnova.core.network.apis.entity.BannerInfo
import com.kuaigeglobal.epnova.core.network.apis.entity.CharacterInfo
import com.kuaigeglobal.epnova.core.network.apis.entity.CoinsAndWalletInfo
import com.kuaigeglobal.epnova.core.network.apis.entity.ContentInfo
import com.kuaigeglobal.epnova.core.network.apis.entity.CreationStyleInfo
import com.kuaigeglobal.epnova.core.network.apis.entity.CreatorEpisodeInfo
import com.kuaigeglobal.epnova.core.network.apis.entity.CreatorWorkInfo
import com.kuaigeglobal.epnova.core.network.apis.entity.DubbingGenStatusInfo
import com.kuaigeglobal.epnova.core.network.apis.entity.EpisodeInfo
import com.kuaigeglobal.epnova.core.network.apis.entity.ExampleWorkInfo
import com.kuaigeglobal.epnova.core.network.apis.entity.IncomeDetailInfo
import com.kuaigeglobal.epnova.core.network.apis.entity.IncomeSummaryInfo
import com.kuaigeglobal.epnova.core.network.apis.entity.InspirationInfo
import com.kuaigeglobal.epnova.core.network.apis.entity.SameStyleHotCaseInfo
import com.kuaigeglobal.epnova.core.network.apis.entity.SameStylePrefillInfo
import com.kuaigeglobal.epnova.core.network.apis.entity.LogOffStatusInfo
import com.kuaigeglobal.epnova.core.network.apis.entity.MyTvSeriesListInfo
import com.kuaigeglobal.epnova.core.network.apis.entity.OnceTaskListData
import com.kuaigeglobal.epnova.core.network.apis.entity.OfficialConsultationInfo
import com.kuaigeglobal.epnova.core.network.apis.entity.OrderStatusInfo
import com.kuaigeglobal.epnova.core.network.apis.entity.OverseasDeviceFingerprint
import com.kuaigeglobal.epnova.core.network.apis.entity.PaymentData
import com.kuaigeglobal.epnova.core.network.apis.entity.PaymentInfo
import com.kuaigeglobal.epnova.core.network.apis.entity.ReferenceImageInfo
import com.kuaigeglobal.epnova.core.network.apis.entity.RewardAdCompleteData
import com.kuaigeglobal.epnova.core.network.apis.entity.RewardAdPrepareData
import com.kuaigeglobal.epnova.core.network.apis.entity.RewardAdTaskListData
import com.kuaigeglobal.epnova.core.network.apis.entity.RewardCheckInListData
import com.kuaigeglobal.epnova.core.network.apis.entity.RewardCheckInReportData
import com.kuaigeglobal.epnova.core.network.apis.entity.RewardUserUncheckData
import com.kuaigeglobal.epnova.core.network.apis.entity.CreationModelInfo
import com.kuaigeglobal.epnova.core.network.apis.entity.RoleImageInfo
import com.kuaigeglobal.epnova.core.network.apis.entity.StoryUnlockInfoData
import com.kuaigeglobal.epnova.core.network.apis.entity.StoryUnlockPreloadData
import com.kuaigeglobal.epnova.core.network.apis.entity.StoryUnlockResultData
import com.kuaigeglobal.epnova.core.network.apis.entity.RoleTagInfo
import com.kuaigeglobal.epnova.core.network.apis.entity.SaveSequelNovelStoryInfo
import com.kuaigeglobal.epnova.core.network.apis.entity.SegmentImageHistoryInfo
import com.kuaigeglobal.epnova.core.network.apis.entity.SensitiveWordCheckData
import com.kuaigeglobal.epnova.core.network.apis.entity.SeriesPriceConfigInfo
import com.kuaigeglobal.epnova.core.network.apis.entity.ShortDramaInfo
import com.kuaigeglobal.epnova.core.network.apis.entity.SimpleStoryboardInfo
import com.kuaigeglobal.epnova.core.network.apis.entity.StoryStreamSessionInfo
import com.kuaigeglobal.epnova.core.network.apis.entity.StoryUnlockStatusData
import com.kuaigeglobal.epnova.core.network.apis.entity.SubtitleTaskStatusInfo
import com.kuaigeglobal.epnova.core.network.apis.entity.TaskStatusInfo
import com.kuaigeglobal.epnova.core.network.apis.entity.UserShareUrlInfo
import com.kuaigeglobal.epnova.core.network.apis.entity.VideoConfigData
import com.kuaigeglobal.epnova.core.network.apis.entity.VipConfigInfo
import com.kuaigeglobal.epnova.core.network.apis.entity.WorkInfo
import com.kuaigeglobal.epnova.core.network.apis.entity.WorkVOInfo
import com.kuaigeglobal.epnova.core.network.apis.request.AIExpandBody
import com.kuaigeglobal.epnova.core.network.apis.request.AISynopsisBody
import com.kuaigeglobal.epnova.core.network.apis.request.AppearanceIdBody
import com.kuaigeglobal.epnova.core.network.apis.request.AssetGenerateBody
import com.kuaigeglobal.epnova.core.network.apis.request.AssetSaveBody
import com.kuaigeglobal.epnova.core.network.apis.request.AssetStoryImageBody
import com.kuaigeglobal.epnova.core.network.apis.request.AssetUnlinkBody
import com.kuaigeglobal.epnova.core.network.apis.request.BannerBody
import com.kuaigeglobal.epnova.core.network.apis.request.BindPhoneBody
import com.kuaigeglobal.epnova.core.network.apis.request.BookShelfIdPagedBody
import com.kuaigeglobal.epnova.core.network.apis.request.CompleteUserDataInfoBody
import com.kuaigeglobal.epnova.core.network.apis.request.ContentBody
import com.kuaigeglobal.epnova.core.network.apis.request.ContinueTaskBody
import com.kuaigeglobal.epnova.core.network.apis.request.CreateOrderBody
import com.kuaigeglobal.epnova.core.network.apis.request.CreatePointOrderBody
import com.kuaigeglobal.epnova.core.network.apis.request.CreateStoryWorkBody
import com.kuaigeglobal.epnova.core.network.apis.request.CreateTaskBody
import com.kuaigeglobal.epnova.core.network.apis.request.CreateWorkBody
import com.kuaigeglobal.epnova.core.network.apis.request.CreationGeneratePreBody
import com.kuaigeglobal.epnova.core.network.apis.request.CurrentWorkIdBody
import com.kuaigeglobal.epnova.core.network.apis.request.DeleteWorkBody
import com.kuaigeglobal.epnova.core.network.apis.request.DetailTypeBody
import com.kuaigeglobal.epnova.core.network.apis.request.DramaContentBody
import com.kuaigeglobal.epnova.core.network.apis.request.DramaGenerateBody
import com.kuaigeglobal.epnova.core.network.apis.request.DramaGenerateSourceReportBody
import com.kuaigeglobal.epnova.core.network.apis.request.DramasIdBody
import com.kuaigeglobal.epnova.core.network.apis.request.DramasSeriesId
import com.kuaigeglobal.epnova.core.network.apis.request.DramasTimbreBody
import com.kuaigeglobal.epnova.core.network.apis.request.FeedbackBody
import com.kuaigeglobal.epnova.core.network.apis.request.FcmTokenBody
import com.kuaigeglobal.epnova.core.network.apis.request.FirstRechargeCreditsBody
import com.kuaigeglobal.epnova.core.network.apis.request.GenerateStoryboardBody
import com.kuaigeglobal.epnova.core.network.apis.request.GoogleMemberSubscriptionBody
import com.kuaigeglobal.epnova.core.network.apis.request.HotCaseIdBody
import com.kuaigeglobal.epnova.core.network.apis.request.GenerateSubtitleBody
import com.kuaigeglobal.epnova.core.network.apis.request.IdsBody
import com.kuaigeglobal.epnova.core.network.apis.request.ImgUrlBody
import com.kuaigeglobal.epnova.core.network.apis.request.IncomeDetailPageBody
import com.kuaigeglobal.epnova.core.network.apis.request.InsufficientGuideBody
import com.kuaigeglobal.epnova.core.network.apis.request.KeyworkBody
import com.kuaigeglobal.epnova.core.network.apis.request.LatestEpisodeBody
import com.kuaigeglobal.epnova.core.network.apis.request.LoginByFlashMobileBody
import com.kuaigeglobal.epnova.core.network.apis.request.LoginByPhoneBody
import com.kuaigeglobal.epnova.core.network.apis.request.LoginByThirdBody
import com.kuaigeglobal.epnova.core.network.apis.request.MemberSubscriptionPrepareBody
import com.kuaigeglobal.epnova.core.network.apis.request.MemberSubscriptionPrepareReleaseBody
import com.kuaigeglobal.epnova.core.network.apis.request.MusicGenerateSourceBody
import com.kuaigeglobal.epnova.core.network.apis.request.MyWorkBody
import com.kuaigeglobal.epnova.core.network.apis.request.OptimizeContentData
import com.kuaigeglobal.epnova.core.network.apis.request.OptimizeRoleImageDescBody
import com.kuaigeglobal.epnova.core.network.apis.request.OrderStatusBody
import com.kuaigeglobal.epnova.core.network.apis.request.PagedBody
import com.kuaigeglobal.epnova.core.network.apis.request.PlayEventBody
import com.kuaigeglobal.epnova.core.network.apis.request.PollScriptBody
import com.kuaigeglobal.epnova.core.network.apis.request.PollScriptData
import com.kuaigeglobal.epnova.core.network.apis.request.PollSummaryData
import com.kuaigeglobal.epnova.core.network.apis.request.PopularRecommendDramasIdBody
import com.kuaigeglobal.epnova.core.network.apis.request.PowerCalculateBody
import com.kuaigeglobal.epnova.core.network.apis.request.PowerCalculateData
import com.kuaigeglobal.epnova.core.network.apis.request.RatingPopupDisplayReportBody
import com.kuaigeglobal.epnova.core.network.apis.request.RatingScoreReportBody
import com.kuaigeglobal.epnova.core.network.apis.request.ReferenceImageBody
import com.kuaigeglobal.epnova.core.network.apis.request.ReportSubmissionBody
import com.kuaigeglobal.epnova.core.network.apis.request.ReportCreationUsageBody
import com.kuaigeglobal.epnova.core.network.apis.request.RoleIdBody
import com.kuaigeglobal.epnova.core.network.apis.request.RoleImageRegenerateBody
import com.kuaigeglobal.epnova.core.network.apis.request.RoleSwitchBody
import com.kuaigeglobal.epnova.core.network.apis.request.SSEContentBody
import com.kuaigeglobal.epnova.core.network.apis.request.SaveEpisodeScriptBody
import com.kuaigeglobal.epnova.core.network.apis.request.SaveScriptBody
import com.kuaigeglobal.epnova.core.network.apis.request.SaveSequelNovelStoryBody
import com.kuaigeglobal.epnova.core.network.apis.request.SaveStoryboardBody
import com.kuaigeglobal.epnova.core.network.apis.request.SceneContentEditBody
import com.kuaigeglobal.epnova.core.network.apis.request.SegmentImageDescOptimizeBody
import com.kuaigeglobal.epnova.core.network.apis.request.SegmentImageRegenerateBody
import com.kuaigeglobal.epnova.core.network.apis.request.SegmentImageReplaceBody
import com.kuaigeglobal.epnova.core.network.apis.request.SegmentImageRetryBody
import com.kuaigeglobal.epnova.core.network.apis.request.SendSmsValidateCodeBody
import com.kuaigeglobal.epnova.core.network.apis.request.SeriesFollowBody
import com.kuaigeglobal.epnova.core.network.apis.request.SeriesModifyInfoBody
import com.kuaigeglobal.epnova.core.network.apis.request.SeriesSetPriceBody
import com.kuaigeglobal.epnova.core.network.apis.request.ShareReportBody
import com.kuaigeglobal.epnova.core.network.apis.request.ShortLikeBody
import com.kuaigeglobal.epnova.core.network.apis.request.ShotSegmentIdBody
import com.kuaigeglobal.epnova.core.network.apis.request.RewardAdCompleteBody
import com.kuaigeglobal.epnova.core.network.apis.request.RewardAdTaskPrepareBody
import com.kuaigeglobal.epnova.core.network.apis.request.RoleIdPagedBody
import com.kuaigeglobal.epnova.core.network.apis.request.RollNovelSummaryResult
import com.kuaigeglobal.epnova.core.network.apis.request.SaveStorySummaryBody
import com.kuaigeglobal.epnova.core.network.apis.request.StoryStreamSessionBody
import com.kuaigeglobal.epnova.core.network.apis.request.StoryUnlockAdCompleteBody
import com.kuaigeglobal.epnova.core.network.apis.request.StoryUnlockCoinsBody
import com.kuaigeglobal.epnova.core.network.apis.request.StoryboardIdBody
import com.kuaigeglobal.epnova.core.network.apis.request.StrategyTypeBody
import com.kuaigeglobal.epnova.core.network.apis.request.SwitchModelBody
import com.kuaigeglobal.epnova.core.network.apis.request.SynthesisUploadBody
import com.kuaigeglobal.epnova.core.network.apis.request.TaskIdBody
import com.kuaigeglobal.epnova.core.network.apis.request.TipOffBody
import com.kuaigeglobal.epnova.core.network.apis.request.UploadStoryBody
import com.kuaigeglobal.epnova.core.network.apis.request.UserPlayBody
import com.kuaigeglobal.epnova.core.network.apis.request.UserShareUrlBody
import com.kuaigeglobal.epnova.core.network.apis.request.UserTokenBody
import com.kuaigeglobal.epnova.core.network.apis.request.VerifyPurchasedBody
import com.kuaigeglobal.epnova.core.network.apis.request.VideoSynthesisBody
import com.kuaigeglobal.epnova.core.network.apis.request.WorkBody
import com.kuaigeglobal.epnova.core.network.apis.request.WorkIdBody
import com.kuaigeglobal.epnova.core.network.apis.request.WorkIdPagedBody
import com.kuaigeglobal.epnova.core.network.apis.request.WorkInfoUpdateBody
import com.kuaigeglobal.epnova.core.network.apis.request.WorkPublishStatusBody
import com.kuaigeglobal.epnova.core.network.apis.request.WorkPublishSubmitBody
import com.kuaigeglobal.epnova.core.network.apis.request.WorkTaskData
import com.kuaigeglobal.epnova.core.network.apis.response.ActivityConfigData
import com.kuaigeglobal.epnova.core.network.apis.response.LoginTaskData
import com.kuaigeglobal.epnova.core.network.apis.response.AppConfigData
import com.kuaigeglobal.epnova.core.network.apis.response.AssetBatchGenerateResult
import com.kuaigeglobal.epnova.core.network.apis.response.AssetStoryListInfo
import com.kuaigeglobal.epnova.core.network.apis.response.BalanceData
import com.kuaigeglobal.epnova.core.network.apis.response.CharacterListData
import com.kuaigeglobal.epnova.core.network.apis.response.CollectData
import com.kuaigeglobal.epnova.core.network.apis.response.ComputingPowerDetailsInfo
import com.kuaigeglobal.epnova.core.network.apis.response.DramasIdData
import com.kuaigeglobal.epnova.core.network.apis.response.DramasSeriesIdData
import com.kuaigeglobal.epnova.core.network.apis.response.EpisodeDirectoryData
import com.kuaigeglobal.epnova.core.network.apis.response.EpisodePlayData
import com.kuaigeglobal.epnova.core.network.apis.response.FirstRechargeCreditsDialogData
import com.kuaigeglobal.epnova.core.network.apis.response.FirstRechargeDialogData
import com.kuaigeglobal.epnova.core.network.apis.response.FollowingData
import com.kuaigeglobal.epnova.core.network.apis.response.HomeBookShelfData
import com.kuaigeglobal.epnova.core.network.apis.response.LikeData
import com.kuaigeglobal.epnova.core.network.apis.response.MemberCardData
import com.kuaigeglobal.epnova.core.network.apis.response.MemberPlanListData
import com.kuaigeglobal.epnova.core.network.apis.response.MemberSubscriptionPrepareData
import com.kuaigeglobal.epnova.core.network.apis.response.MemberSubscriptionPrepareReleaseData
import com.kuaigeglobal.epnova.core.network.apis.response.OrderNoData
import com.kuaigeglobal.epnova.core.network.apis.response.PictureBook
import com.kuaigeglobal.epnova.core.network.apis.response.PointAmountData
import com.kuaigeglobal.epnova.core.network.apis.response.CoinConfigData
import com.kuaigeglobal.epnova.core.network.apis.response.PointInsufficientGuideData
import com.kuaigeglobal.epnova.core.network.apis.response.PointConfigData
import com.kuaigeglobal.epnova.core.network.apis.response.ShelfStatusData
import com.kuaigeglobal.epnova.core.network.apis.response.ShortLikeData
import com.kuaigeglobal.epnova.core.network.apis.response.StoryboardListData
import com.kuaigeglobal.epnova.core.network.apis.response.UpgradeData
import com.kuaigeglobal.epnova.core.network.apis.response.UnusedPackageData
import com.kuaigeglobal.epnova.core.network.apis.response.UserHomeData
import com.kuaigeglobal.epnova.core.network.apis.response.UserPlayData
import com.kuaigeglobal.epnova.core.network.apis.response.WorkIdData
import com.kuaigeglobal.epnova.core.network.apis.response.WorkPublishStatusData
import kotlinx.coroutines.flow.Flow
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST

interface ApiService {

    /**
     * 获取APP配置接口
     */
    @POST(ApiConstants.APP_CONFIG)
    suspend fun appConfig(@Body body: UserTokenBody): ApiResponse<AppConfigData>

    /**
     * 好评弹窗真实展示后上报。
     * 服务端按用户时区每天最多记录一次，已评分用户不再记录展示。
     */
    @POST(ApiConstants.RATING_POPUP_DISPLAY_REPORT)
    suspend fun reportRatingPopupDisplay(@Body body: RatingPopupDisplayReportBody): ApiResponse<Any?>

    /**
     * 好评评分上报。
     * 服务端保留首次评分及入口，成功后该用户永久不再展示好评弹窗。
     */
    @POST(ApiConstants.RATING_SCORE_REPORT)
    suspend fun reportRatingScore(@Body body: RatingScoreReportBody): ApiResponse<Any?>

    /**
     * 举报
     */
    @POST(ApiConstants.TIP_OFF)
    suspend fun tipOff(@Body body: TipOffBody): ApiResponse<Any>

    /**
     * 查询指定版本上架状态
     */
    @POST(ApiConstants.GET_APP_SHELF_STATUS)
    suspend fun getAppShelfStatus(): ApiResponse<ShelfStatusData?>


    /**
     * 游客登录
     */
    @POST(ApiConstants.LOGIN_BY_GUEST)
    suspend fun loginBygGuest(@Body body: OverseasDeviceFingerprint): ApiResponse<UserHomeData?>

    /**
     * 第三方登录
     */
    @POST(ApiConstants.LOGIN_BY_THIRD)
    suspend fun loginByThird(@Body body: LoginByThirdBody): ApiResponse<UserHomeData?>

    /**
     * 退出登录
     */
    @POST(ApiConstants.LOGOUT_APP)
    suspend fun logoutApp(@Body body: UserTokenBody): ApiResponse<Any>

    /**
     * 获取用户主页信息接口
     */
    @POST(ApiConstants.GET_USER_HOME_INFO)
    suspend fun getUserHomeInfo(@Body body: UserTokenBody): ApiResponse<UserHomeData?>

    /**
     * 完善用户资料接口
     */
    @POST(ApiConstants.COMPLETE_USER_DATA_INFO)
    suspend fun modifyUserInfo(@Body body: CompleteUserDataInfoBody): ApiResponse<Any?>

    /**
     * 设置App语言
     */
    @POST(ApiConstants.SET_APP_LANGUAGE)
    suspend fun setAppLanguage(@Body body: UserTokenBody): ApiResponse<Any?>

    /**
     * 注册 FCM 推送 Token
     */
    @POST(ApiConstants.REGISTER_FCM_TOKEN)
    suspend fun registerFcmToken(@Body body: FcmTokenBody): ApiResponse<Any?>

    /**
     * 获取APP升级信息
     */
    @POST(ApiConstants.GET_APP_UPGRADE_INFO)
    suspend fun upgrade(@Body userTokenBody: UserTokenBody): ApiResponse<UpgradeData>

    /**
     * 注销账号
     */
    @POST(ApiConstants.LOG_OFF)
    suspend fun logOff(@Body body: UserTokenBody): ApiResponse<Any?>

    /**
     * 获取用户分享链接
     */
    @POST(ApiConstants.GET_USER_SHARE_URL)
    suspend fun getUserShareUrl(@Body body: UserShareUrlBody): ApiResponse<UserShareUrlInfo?>

    /**
     * 用户分享操作上报
     */
    @POST(ApiConstants.SHARE_REPORT)
    suspend fun shareReport(@Body body: ShareReportBody): ApiResponse<Any?>

    /**
     * 提交用户的反馈数据
     */
    @POST(ApiConstants.FEED_BACK)
    suspend fun feedback(@Body body: FeedbackBody): ApiResponse<Any?>

    /**
     * 会员配置
     */
    @POST(ApiConstants.VIP_CONFIG)
    suspend fun vipConfig(@Body body: UserTokenBody): ApiResponse<VipConfigInfo?>

    /**
     * 查询会员中心卡片
     */
    @POST(ApiConstants.MEMBER_CARD_CURRENT)
    suspend fun getCurrentMemberCard(): ApiResponse<MemberCardData?>

    /**
     * 查询会员套餐
     */
    @POST(ApiConstants.MEMBER_PLAN_LIST)
    suspend fun getMemberPlanList(): ApiResponse<MemberPlanListData?>

    /**
     * 准备会员购买
     */
    @POST(ApiConstants.MEMBER_SUBSCRIPTION_PURCHASE_PREPARE)
    suspend fun prepareMemberSubscriptionPurchase(
        @Body body: MemberSubscriptionPrepareBody,
    ): ApiResponse<MemberSubscriptionPrepareData?>

    /**
     * 释放会员购买准备
     */
    @POST(ApiConstants.MEMBER_SUBSCRIPTION_PURCHASE_PREPARE_RELEASE)
    suspend fun releaseMemberSubscriptionPurchasePrepare(
        @Body body: MemberSubscriptionPrepareReleaseBody,
    ): ApiResponse<MemberSubscriptionPrepareReleaseData?>

    /**
     * 确认 Google 订阅购买
     */
    @POST(ApiConstants.MEMBER_SUBSCRIPTION_GOOGLE_PURCHASE_CONFIRM)
    suspend fun confirmGoogleMemberSubscriptionPurchase(
        @Body body: GoogleMemberSubscriptionBody,
    ): ApiResponse<Any?>

    /**
     * 自动同步 Google 订阅
     */
    @POST(ApiConstants.MEMBER_SUBSCRIPTION_GOOGLE_SYNC)
    suspend fun syncGoogleMemberSubscription(
        @Body body: GoogleMemberSubscriptionBody,
    ): ApiResponse<Any?>

    /**
     * 用户日活记录
     */
    @POST(ApiConstants.DAILY)
    suspend fun daily(@Body body: UserTokenBody): ApiResponse<Any>

    /**
     * 歌曲生成来源上报
     */
    @POST(ApiConstants.MUSIC_GENERATE_SOURCE_REPORT)
    suspend fun musicGenerateSourceReport(@Body body: MusicGenerateSourceBody): ApiResponse<Any?>

    /**
     * 发送验证码
     */
    @POST(ApiConstants.SEND_SMS_VALIDATE_CODE)
    suspend fun sendSmsValidateCode(@Body body: SendSmsValidateCodeBody): ApiResponse<Any?>


    /**
     * 绑定手机号
     */
    @POST(ApiConstants.BIND_PHONE)
    suspend fun bindPhone(@Body body: BindPhoneBody): ApiResponse<Any?>

    /**
     * 闪验登录
     */
    @POST(ApiConstants.LOGIN_BY_FLASH_MOBILE)
    suspend fun loginByFlashMobile(@Body body: LoginByFlashMobileBody): ApiResponse<UserHomeData?>

    /**
     * 手机登录接口
     */
    @POST(ApiConstants.LOGIN_BY_PHONE)
    suspend fun loginByPhone(@Body body: LoginByPhoneBody): ApiResponse<UserHomeData?>

    /**
     * 取消注销
     */
    @POST(ApiConstants.CANCEL_LOG_OFF)
    suspend fun cancelLogOff(@Body body: UserTokenBody): ApiResponse<Any?>

    /**
     * 用户注销状态
     */
    @POST(ApiConstants.LOG_OFF_STATUS)
    suspend fun logOffStatus(@Body body: UserTokenBody): ApiResponse<LogOffStatusInfo?>

    /**
     * 预生成短剧剧本大纲
     */
    @POST(ApiConstants.CREATION_GENERATE_PRE)
    suspend fun creationGeneratePre(@Body body: CreationGeneratePreBody): ApiResponse<DramasIdData?>

    /**
     * SSE流式输出：业务类型枚举10=随机灵感 11=AI优化灵感 20=剧集梗概 30=梗概剧集剧本 31=小说剧集剧本 32=扩写剧集剧本 33=优化作品简介
     */
    @Headers(
        "Accept: text/event-stream",  // 声明 SSE 媒体类型
        "Cache-Control: no-cache",     // 禁用缓存
        "Connection: keep-alive"       // 保持长连接
    )
    @POST(ApiConstants.SSE_CONTENT_OPTIMIZE)
    fun sseContentOptimize(@Body body: SSEContentBody): Flow<SSEvent>

    /**
     * 随机灵感
     */
    @Headers(
        "Accept: text/event-stream",  // 声明 SSE 媒体类型
        "Cache-Control: no-cache",     // 禁用缓存
        "Connection: keep-alive"       // 保持长连接
    )
    @POST(ApiConstants.SSE_CONTENT_RANDOM)
    fun sseContentRandom(@Body body: UserTokenBody): Flow<SSEvent>

    /**
     * 作品简介优化
     */
    @Headers(
        "Accept: text/event-stream",  // 声明 SSE 媒体类型
        "Cache-Control: no-cache",     // 禁用缓存
        "Connection: keep-alive"       // 保持长连接
    )
    @POST(ApiConstants.SSE_WORK_DESC_OPTIMIZE)
    fun sseWorkDescOptimize(@Body body: SSEContentBody): Flow<SSEvent>

    /**
     *灵感故事剧本（基于灵感写剧本/灵感续写/小说续写）
     */
    @Headers(
        "Accept: text/event-stream",  // 声明 SSE 媒体类型
        "Cache-Control: no-cache",     // 禁用缓存
        "Connection: keep-alive"       // 保持长连接
    )
    @POST(ApiConstants.SSE_SCRIPT_FROM_IDEA)
    fun sseScriptFromIdea(@Body body: AIExpandBody): Flow<SSEvent>

    /**
     *小说直接生成剧集剧本
     */
    @Headers(
        "Accept: text/event-stream",  // 声明 SSE 媒体类型
        "Cache-Control: no-cache",     // 禁用缓存
        "Connection: keep-alive"       // 保持长连接
    )
    @POST(ApiConstants.SSE_SCRIPT_FROM_STORY)
    fun sseScriptFromStory(@Body body: AISynopsisBody): Flow<SSEvent>

    /**
     *扩写剧集剧本（小说）
     */
    @Headers(
        "Accept: text/event-stream",  // 声明 SSE 媒体类型
        "Cache-Control: no-cache",     // 禁用缓存
        "Connection: keep-alive"       // 保持长连接
    )
    @POST(ApiConstants.SSE_CONTENT_AI_EXPAND)
    fun sseContentAIExpand(@Body body: AIExpandBody): Flow<SSEvent>

    /**
     *故事梗概生成（灵感）
     */
    @Headers(
        "Accept: text/event-stream",  // 声明 SSE 媒体类型
        "Cache-Control: no-cache",     // 禁用缓存
        "Connection: keep-alive"       // 保持长连接
    )
    @POST(ApiConstants.SSE_CONTENT_SYNOPSIS)
    fun sseContentSynopsis(@Body body: AISynopsisBody): Flow<SSEvent>

    /**
     * 故事梗概流式生成会话。
     *
     * 返回 result 时类型就是 RollNovelSummaryResult；ViewModel 再包装成旧 UI 使用的 PollSummaryData。
     * outline 阶段不要传 storyId，续传只依赖 workId + lastEventId。
     */
    @Headers("Accept: application/json")
    @POST(ApiConstants.STORY_STREAM_OUTLINE_SESSION)
    suspend fun storyStreamOutlineSession(
        @Body body: StoryStreamSessionBody
    ): ApiResponse<StoryStreamSessionInfo<RollNovelSummaryResult>?>

    /**
     * 故事剧本流式生成会话。
     *
     * 有当前剧集 ID 时 body.storyId 建议传入；session 可能直接返回 PollScriptData.result，
     * 也可能只告诉前端 subscribe 已就绪。
     */
    @Headers("Accept: application/json")
    @POST(ApiConstants.STORY_STREAM_SCRIPT_SESSION)
    suspend fun storyStreamScriptSession(
        @Body body: StoryStreamSessionBody
    ): ApiResponse<StoryStreamSessionInfo<PollScriptData>?>

    /**
     * 故事梗概流式输出订阅
     */
    @Headers(
        "Accept: text/event-stream",
        "Cache-Control: no-cache",
        "Connection: keep-alive"
    )
    @POST(ApiConstants.STORY_STREAM_OUTLINE_SUBSCRIBE)
    fun storyStreamOutlineSubscribe(
        @Body body: StoryStreamSessionBody
    ): Flow<SSEvent>

    /**
     * 故事剧本流式输出订阅
     */
    @Headers(
        "Accept: text/event-stream",
        "Cache-Control: no-cache",
        "Connection: keep-alive"
    )
    @POST(ApiConstants.STORY_STREAM_SCRIPT_SUBSCRIBE)
    fun storyStreamScriptSubscribe(
        @Body body: StoryStreamSessionBody
    ): Flow<SSEvent>

    /**
     * 灵感保存故事梗概（下一步创建剧本）
     */
    @POST(ApiConstants.SAVE_WORK_SUMMARY)
    suspend fun saveWorkSummary(@Body body: CreateWorkBody): ApiResponse<WorkIdData?>

    /**
     * 小说保存故事剧集剧本（首次创建）
     */
    @POST(ApiConstants.SAVE_STORY_EPISODE_SCRIPT)
    suspend fun saveStoryEpisodeScript(@Body body: CreateStoryWorkBody): ApiResponse<WorkIdData?>

    /**
     * 单集作品故事剧本保存（首次创建）
     */
    @POST(ApiConstants.SAVE_SINGLE_EPISODE_SCRIPT)
    suspend fun saveSingleEpisodeScript(@Body body: CreateStoryWorkBody): ApiResponse<WorkIdData?>

    /**
     *保存故事剧集剧本（灵感首创/灵感续集/小说续集）
     */
    @POST(ApiConstants.SAVE_EPISODE_SCRIPT)
    suspend fun saveEpisodeScript(@Body body: SaveEpisodeScriptBody): ApiResponse<WorkIdData?>

    /**
     * 轮询剧本分镜大纲
     */
    @POST(ApiConstants.GET_STORY_BOARD_LIST)
    suspend fun getStoryBoardList(@Body body: DramasIdBody): ApiResponse<StoryboardListData?>

    /**
     * 预生成剧本分镜大纲（重新生成分镜大纲）
     */
    @POST(ApiConstants.GET_STORY_BOARD_LIST_RETRY)
    suspend fun getStoryBoardListRetry(@Body body: DramasIdBody): ApiResponse<DramasIdData?>

    /**
     * 编辑故事剧本内容
     */
    @POST(ApiConstants.DRAMA_CONTENT_EDIT)
    suspend fun dramaContentEdit(@Body body: DramaContentBody): ApiResponse<Any>

    /**
     * 编辑剧本分镜内容(画面、旁白、动作)
     */
    @POST(ApiConstants.SCENE_CONTENT_EDIT)
    suspend fun sceneContentEdit(@Body body: SceneContentEditBody): ApiResponse<Any>

    /**
     * 计算生成剧本价格（积分）
     */
    @POST(ApiConstants.SHORT_DRAMAS_CALCULATE_PRICE)
    suspend fun shotDramasCalculatePrice(@Body body: DramasIdBody): ApiResponse<PointAmountData?>


    /**
     * 正式生成剧本(扣积分)
     */
    @POST(ApiConstants.SHOT_DRAMA_GENERATE)
    suspend fun shotDramaGenerate(@Body body: DramasIdBody): ApiResponse<DramasIdData?>

    /**
     * 短剧风格列表
     */
    @POST(ApiConstants.CREATION_STYLE_LIST)
    suspend fun creationStyleList(@Body body: PagedBody): ApiResponse<PagedList<CreationStyleInfo>?>

    /**
     * 优化生成剧本的角色的画面描述
     */
    @POST(ApiConstants.OPTIMIZE_ROLE_IMAGE_DESC)
    suspend fun optimizeRoleImageDesc(@Body body: OptimizeRoleImageDescBody): ApiResponse<OptimizeContentData?>

    /**
     * 重新生成剧本的角色
     */
    @POST(ApiConstants.ROLE_IMAGE_REGENERATE)
    suspend fun roleImageRegenerate(@Body body: RoleImageRegenerateBody): ApiResponse<Any>

    /**
     * 查询剧本的角色标签列表
     */
    @POST(ApiConstants.ROLE_TAG_LIST)
    suspend fun roleTagList(@Body body: UserTokenBody): ApiResponse<ListData<RoleTagInfo>?>

    /**
     * 生成中的绘本列表
     */
    @POST(ApiConstants.GENERATING_BOOK_LIST)
    suspend fun generatingBookList(@Body body: UserTokenBody): ApiResponse<PagedList<PictureBook>?>

    /**
     * 合成短剧
     */
    @POST(ApiConstants.SHORT_DRAMAS_SYNTHESIS)
    suspend fun shortDramasSynthesis(@Body body: DramasIdBody): ApiResponse<Any?>

    /**
     * 合成短剧(上传合成的视频)
     */
    @POST(ApiConstants.SHORT_DRAMAS_SYNTHESIS_UPLOAD)
    suspend fun shortDramasSynthesisUpload(@Body body: SynthesisUploadBody): ApiResponse<Any?>

    /**
     * 查询剧本信息（作品详情也用这个）
     */
    @POST(ApiConstants.SHORT_DRAMAS_INFO)
    suspend fun shortDramasInfo(@Body body: DramasIdBody): ApiResponse<ShortDramaInfo?>

    /**
     * 查询剧本信息（作品详情也用这个）
     */
    @POST(ApiConstants.SHORT_DRAMAS_RELEASED_INFO)
    suspend fun releasedShortDramasInfo(@Body body: DramasIdBody): ApiResponse<ShortDramaInfo?>

    /**
     * 提交字幕配音
     */
    @POST(ApiConstants.SHORT_DRAMAS_DUBBING)
    suspend fun shortDramasDubbing(@Body body: DramasTimbreBody): ApiResponse<Any?>

    /**
     * 配音失败-重新配音
     */
    @POST(ApiConstants.SHORT_DRAMAS_DUBBING_RETRY)
    suspend fun shortDramasDubbingRetry(@Body body: DramasTimbreBody): ApiResponse<Any?>

    /**
     * 解锁首次生成的短剧(首次生成的短剧付费)
     */
    @POST(ApiConstants.UNLOCK_FIRST_GENERATED_SHORT_DRAMA)
    suspend fun unlockFirstGeneratedShortDrama(@Body body: DramasIdBody): ApiResponse<Any>

    /**
     * 使用配音
     */
    @POST(ApiConstants.SHORT_DRAMAS_DUBBING_USE)
    suspend fun shortDramasDubbingUse(@Body body: DramasTimbreBody): ApiResponse<Any?>

    /**
     * 字幕配音生成状态
     */
    @POST(ApiConstants.SHORT_DRAMAS_DUBBING_STATUS)
    suspend fun shortDramasDubbingStatus(@Body body: DramasTimbreBody): ApiResponse<DubbingGenStatusInfo?>

    /**
     * 使用历史分镜（分镜替换）
     */
    @POST(ApiConstants.SHORT_DRAMAS_SEGMENT_IMAGE_REPLACE)
    suspend fun shortDramasSegmentImageReplace(@Body body: SegmentImageReplaceBody): ApiResponse<Any?>

    /**
     * 查询历史分镜
     */
    @POST(ApiConstants.SHORT_DRAMAS_SEGMENT_IMAGE_HISTORY)
    suspend fun shortDramasSegmentImageHistory(@Body body: ShotSegmentIdBody): ApiResponse<ListData<SegmentImageHistoryInfo>?>

    /**
     * 重绘分镜（重新生成分镜的图片）
     */
    @POST(ApiConstants.SHORT_DRAMAS_SEGMENT_IMAGE_REGENERATE)
    suspend fun shortDramasSegmentImageRegenerate(@Body body: SegmentImageRegenerateBody): ApiResponse<Any?>

    /**
     * 优化重绘分镜的画面描述
     */
    @POST(ApiConstants.SHORT_DRAMAS_SEGMENT_IMAGE_DESC_OPTIMIZE)
    suspend fun shortDramasSegmentImageDescOptimize(@Body body: SegmentImageDescOptimizeBody): ApiResponse<ContentInfo?>

    /**
     * 生成剧本图片(失败重试)
     */
    @POST(ApiConstants.SHORT_DRAMAS_SEGMENT_IMAGE_GENERATE_RETRY)
    suspend fun shortDramasSegmentImageGenerateRetry(@Body body: SegmentImageRetryBody): ApiResponse<Any?>

    /**
     * 删除剧本
     */
    @POST(ApiConstants.DELETE_DRAMA_INFO)
    suspend fun deleteDramaInfo(@Body body: DramasIdBody): ApiResponse<Any?>

    /**
     * 获取用户可续写的剧集列表
     */
    @POST(ApiConstants.USER_CONTINUABLE_DRAMAS)
    suspend fun userContinuableSeries(@Body body: PagedBody): ApiResponse<PagedList<MyTvSeriesListInfo>?>

    /**
     * 获取剧集最新一集剧本详情
     */
    @POST(ApiConstants.LATEST_DRAMAS_WORK_DETAIL)
    suspend fun latestEpisode(@Body body: LatestEpisodeBody): ApiResponse<EpisodeInfo?>

    /**
     * 查询我的作品详情
     */
    @POST(ApiConstants.WORK_INFO)
    suspend fun getWorkInfo(@Body body: WorkIdBody): ApiResponse<CreatorWorkInfo?>

    /**
     * 追剧或取消追剧
     */
    @POST(ApiConstants.SERIES_FOLLOW)
    suspend fun seriesFollow(@Body body: SeriesFollowBody): ApiResponse<FollowingData>

    /**
     * 点赞短剧
     */
    @POST(ApiConstants.LIKE_SHORT_DRAMA)
    suspend fun likeShortDrama(@Body body: ShortLikeBody): ApiResponse<ShortLikeData>

    /**
     * 修改剧集价格
     */
    @POST(ApiConstants.SERIES_SET_PRICE)
    suspend fun seriesSetPrice(@Body body: SeriesSetPriceBody): ApiResponse<Any>

    /**
     * 获取剧集价格配置
     */
    @POST(ApiConstants.SERIES_PRICE_CONFIG)
    suspend fun seriesPriceConfig(@Body body: WorkIdBody): ApiResponse<SeriesPriceConfigInfo?>

    /**
     * 修改剧集信息
     */
    @POST(ApiConstants.SERIES_MODIFY_INFO)
    suspend fun seriesModifyInfo(@Body body: SeriesModifyInfoBody): ApiResponse<Any>

    /**
     * 上架剧集
     */
    @POST(ApiConstants.SERIES_PUBLISH)
    suspend fun seriesPublish(@Body body: SeriesSetPriceBody): ApiResponse<Any>

    /**
     *分页查看作品剧集列表
     */
    @POST(ApiConstants.EPISODE_LIST)
    suspend fun episodeList(@Body body: WorkIdPagedBody): ApiResponse<PagedList<CreatorEpisodeInfo>?>

    /**
     * 预生成短剧剧本(流式输出完之后调)
     */
    @POST(ApiConstants.DRAMA_GENERATE)
    suspend fun dramaGenerate(@Body body: DramaGenerateBody): ApiResponse<DramasIdData?>

    /**
     * 生成短剧来源上报(调完生成短剧剧本接口后调用)
     */
    @POST(ApiConstants.DRAMA_GENERATE_SOURCE_REPORT)
    suspend fun dramaGenerateSourceReport(@Body body: DramaGenerateSourceReportBody): ApiResponse<Any>

    /**
     * 查询剧本的角色列表(轮询使用)
     */
    @POST(ApiConstants.DRAMA_ROLES_POLLING)
    suspend fun dramaRolesPolling(@Body body: DramasIdBody): ApiResponse<CharacterListData?>

    /**
     * 解锁剧集
     */
    @POST(ApiConstants.UNLOCK_SHORT_DRAMA)
    suspend fun unlockShortDrama(@Body body: DramasSeriesId): ApiResponse<BalanceData>

    /**
     * 自动解锁剧集开关上报
     */
    @POST(ApiConstants.AUTO_UNLOCK_SWITCH)
    suspend fun autoUnlockSwitch(@Body body: DramasSeriesId): ApiResponse<Any>

    /**
     * 发布剧本
     */
    @POST(ApiConstants.PUBLISH_SHORT_DRAMA)
    suspend fun publishShortDrama(@Body body: DramasIdBody): ApiResponse<DramasSeriesIdData?>

    /**
     * 查询作品发行状态
     */
    @POST(ApiConstants.WORK_PUBLISH_STATUS)
    suspend fun workPublishStatus(@Body body: WorkPublishStatusBody): ApiResponse<WorkPublishStatusData?>

    /**
     * 提交作品剧集集中审核
     */
    @POST(ApiConstants.WORK_PUBLISH_SUBMIT)
    suspend fun workPublishSubmit(@Body body: WorkPublishSubmitBody): ApiResponse<Any?>

    /**
     * 获取订单状态
     */
    @POST(ApiConstants.GET_ORDER_STATUS)
    suspend fun getOrderStatus(@Body body: OrderStatusBody): ApiResponse<OrderStatusInfo>

    @POST(ApiConstants.RECOMMEND_CLICK_REPORT)
    suspend fun recommendClickReport(@Body body: PopularRecommendDramasIdBody): ApiResponse<Any>

    /**
     * 新增剧本的角色
     */
    @POST(ApiConstants.DRAMAS_ADD_ROLE)
    suspend fun dramasAddRole(@Body body: RoleImageRegenerateBody): ApiResponse<Any>

    /**
     * 删除剧本的角色
     */
    @POST(ApiConstants.DRAMAS_DEL_ROLE)
    suspend fun dramasDelRole(@Body body: RoleIdBody): ApiResponse<Any>

    /**
     * 获取banner信息
     */
    @POST(ApiConstants.APP_BANNERS)
    suspend fun getAppBanner(@Body body: BannerBody): ApiResponse<List<BannerInfo>?>

    /**
     * 获取快捷灵感(2条)
     */
    @POST(ApiConstants.INSPIRATION_QUICK_LIST)
    suspend fun getInspirationQuickList(@Body body: UserTokenBody): ApiResponse<List<InspirationInfo>?>

    /** 创作同款热门案例分页列表。 */
    @POST(ApiConstants.SAME_STYLE_HOT_CASE_PAGE)
    suspend fun sameStyleHotCasePage(@Body body: PagedBody): ApiResponse<PagedList<SameStyleHotCaseInfo>?>

    /** 根据热门案例获取同款创作预填数据。 */
    @POST(ApiConstants.SAME_STYLE_PREFILL)
    suspend fun sameStylePrefill(@Body body: HotCaseIdBody): ApiResponse<SameStylePrefillInfo?>

    /**
     * 查询历史角色
     */
    @POST(ApiConstants.GET_HISTORY_ROLES)
    suspend fun getHistoryRoles(@Body body: RoleIdBody): ApiResponse<ListData<CharacterInfo>?>

    /**
     * 历史角色使用
     */
    @POST(ApiConstants.HISTORY_ROLE_SWITCH)
    suspend fun historyRoleSwitch(@Body body: RoleSwitchBody): ApiResponse<Any>

    /**
     * Top Earners-分页查询作品列表
     */
    @POST(ApiConstants.RECOMMEND_WORK_LIST)
    suspend fun recommendWorkList(@Body body: PagedBody): ApiResponse<PagedList<CreatorWorkInfo>?>

    /**
     * Resource Center 资讯列表
     */
    @POST(ApiConstants.OFFICIAL_CONSULTATION_LIST)
    suspend fun officialConsultationList(@Body body: PagedBody): ApiResponse<PagedList<OfficialConsultationInfo>?>

    /**
     * 获取集数/视频比例/视频风格配置
     */
    @POST(ApiConstants.VIDEO_SET_CONFIG)
    suspend fun videoSetConfig(@Body body: UserTokenBody): ApiResponse<VideoConfigData?>

    /**
     * 查询资产参考图列表
     */
    @POST(ApiConstants.WORK_ASSET_REFERENCE_IMAGE_LIST)
    suspend fun workAssetReferenceImageList(@Body body: ReferenceImageBody): ApiResponse<List<ReferenceImageInfo>?>

    /**
     * 历史形象图列表
     */
    @POST(ApiConstants.WORK_ASSET_HISTORY_IMAGE_LIST)
    suspend fun workAssetHistoryImageList(@Body body: AssetStoryImageBody): ApiResponse<List<RoleImageInfo>?>

    /**
     * 编辑我的作品信息
     */
    @POST(ApiConstants.UPDATE_WORK_INFO)
    suspend fun updateWorkInfo(@Body body: WorkInfoUpdateBody): ApiResponse<Any>

    /**
     * 作品播放量计数
     */
    @POST(ApiConstants.PLAY_EVENT_REPORT)
    suspend fun playEventReport(@Body body: PlayEventBody): ApiResponse<Any>

    /**
     * 生成所有对应资产描述
     */
    @POST(ApiConstants.GENERATE_ALL_ASSET_DESCRIPTION)
    suspend fun generateAllAssetDescription(@Body body: WorkBody): ApiResponse<TaskStatusInfo?>

    /**
     * 轮询查询全部资产描述
     */
    @POST(ApiConstants.WORK_ASSET_ALL_STATUS)
    suspend fun workAssetAllStatus(@Body body: TaskIdBody): ApiResponse<TaskStatusInfo?>

    /**
     * 轮询查询单个资产
     */
    @POST(ApiConstants.WORK_ASSET_SINGLE_STATUS)
    suspend fun workAssetSingleStatus(@Body body: TaskIdBody): ApiResponse<TaskStatusInfo?>

    /**
     * 当前集资产列表
     */
    @POST(ApiConstants.WORK_ASSET_STORY_LIST)
    suspend fun workAssetStoryList(@Body body: WorkBody): ApiResponse<AssetStoryListInfo?>

    /**
     * 保存资产
     */
    @POST(ApiConstants.WORK_ASSET_SAVE)
    suspend fun workAssetSave(@Body body: AssetSaveBody): ApiResponse<AssetInfo?>

    /**
     * 场景单个删除
     */
    @POST(ApiConstants.WORK_ASSET_SCENE_DELETE)
    suspend fun workAssetSceneDelete(@Body body: AssetUnlinkBody): ApiResponse<Any>

    /**
     * 角色单个删除
     */
    @POST(ApiConstants.WORK_ASSET_ROLE_DELETE)
    suspend fun workAssetRoleDelete(@Body body: AssetUnlinkBody): ApiResponse<Any>

    /**
     * 道具单个删除
     */
    @POST(ApiConstants.WORK_ASSET_PROP_DELETE)
    suspend fun workAssetPropDelete(@Body body: AssetUnlinkBody): ApiResponse<Any>

    /**
     * 生成单个资产图片（重新生成/生成）
     */
    @POST(ApiConstants.WORK_ASSET_SINGLE_GENERATE)
    suspend fun workAssetSingleGenerate(@Body body: AssetGenerateBody): ApiResponse<AssetGenerateInfo?>

    /**
     * 一键生成所有资产图片
     */
    @POST(ApiConstants.WORK_ASSET_ONE_KEY_GENERATE)
    suspend fun workAssetOneKeyGenerate(@Body body: WorkBody): ApiResponse<AssetBatchGenerateResult?>

    /**
     * 生成全部分镜描述
     */
    @POST(ApiConstants.GENERATE_ALL_STORYBOARD_DESCRIPTION)
    suspend fun generateAllStoryboardDescription(@Body body: WorkBody): ApiResponse<TaskStatusInfo?>

    /**
     * 轮询查询全部分镜描述
     */
    @POST(ApiConstants.GENERATE_ALL_STORYBOARD_DESCRIPTION_STATUS)
    suspend fun generateAllStoryboardDescriptionStatus(@Body body: TaskIdBody): ApiResponse<TaskStatusInfo?>

    /**
     * 轮询查询单个分镜图片
     */
    @POST(ApiConstants.WORK_STORYBOARD_SINGLE_STATUS)
    suspend fun workStoryboardSingleStatus(@Body body: TaskIdBody): ApiResponse<TaskStatusInfo?>

    /**
     * 分镜设计列表
     */
    @POST(ApiConstants.WORK_STORYBOARD_LIST)
    suspend fun workStoryboardList(@Body body: WorkBody): ApiResponse<List<SimpleStoryboardInfo>?>

    /**
     * 分镜保存
     */
    @POST(ApiConstants.WORK_STORYBOARD_SAVE)
    suspend fun workStoryboardSave(@Body body: SaveStoryboardBody): ApiResponse<SimpleStoryboardInfo?>

    /**
     * 分镜单个删除
     */
    @POST(ApiConstants.WORK_STORYBOARD_DELETE)
    suspend fun workStoryboardDelete(@Body body: IdsBody): ApiResponse<Any?>

    /**
     * 生成单个分镜图片（重新生成/点击生成）
     */
    @POST(ApiConstants.WORK_STORYBOARD_SINGLE_GENERATE)
    suspend fun workStoryboardSingleGenerate(@Body body: GenerateStoryboardBody): ApiResponse<TaskStatusInfo?>

    /**
     * 生成全部分镜视频
     */
    @POST(ApiConstants.WORK_STORYBOARD_VIDEO_GENERATE)
    suspend fun workStoryboardVideoGenerate(@Body body: WorkBody): ApiResponse<TaskStatusInfo?>

    /**
     * 生成单个分镜视频
     */
    @POST(ApiConstants.WORK_STORYBOARD_VIDEO_SINGLE_GENERATE)
    suspend fun workStoryboardVideoSingleGenerate(@Body body: GenerateStoryboardBody): ApiResponse<TaskStatusInfo?>

    /**
     * 重绘分镜完成后不合成还原
     */
    @POST(ApiConstants.WORK_STORYBOARD_RECREATION)
    suspend fun dropRepaint(@Body body: WorkBody): ApiResponse<Any?>

    /**
     * 分镜视频合成
     */
    @POST(ApiConstants.WORK_STORYBOARD_VIDEO_SYNTHESIS)
    suspend fun workStoryboardVideoSynthesis(@Body body: VideoSynthesisBody): ApiResponse<Any?>

    /**
     * 举报提交
     */
    @POST(ApiConstants.REPORT_SUBMISSION)
    suspend fun reportSubmission(@Body body: ReportSubmissionBody): ApiResponse<Any>


    /**
     * 分页查询我的作品列表
     */
    @POST(ApiConstants.MY_WORK)
    suspend fun getMyWork(@Body body: MyWorkBody): ApiResponse<PagedList<CreatorWorkInfo>>


    /**
     * 批量删除我的作品
     */
    @POST(ApiConstants.BATCH_DELETE_WORK)
    suspend fun batchDeleteWork(@Body body: DeleteWorkBody): ApiResponse<Any>


    /**
     * 批量删除我的作品剧集
     */
    @POST(ApiConstants.BATCH_DELETE_WORK_EPISODE)
    suspend fun batchDeleteWorkEpisode(@Body body: DeleteWorkBody): ApiResponse<Any>


    /**
     * 轮询查询我的作品状态列表
     */
    @POST(ApiConstants.GET_WORK_STATUS_LIST)
    suspend fun getWorkStatusList(@Body body: UserTokenBody): ApiResponse<List<CreatorWorkInfo>>

    /**
     * 查看作品单个剧集详情
     */
    @POST(ApiConstants.SINGLE_EPISODE_DETAILS)
    suspend fun singleEpisodeDetails(@Body body: WorkBody): ApiResponse<EpisodeInfo?>

    /**
     * 查看单集作品的剧集详情
     */
    @POST(ApiConstants.SINGLE_EPISODE_SERIES_DETAILS)
    suspend fun singleEpisodeSeriesDetails(@Body body: WorkBody): ApiResponse<EpisodeInfo?>

    /**
     * 小说-保存续创剧集（只有小说有）
     */
    @POST(ApiConstants.SAVE_SEQUEL_NOVEL_STORY)
    suspend fun saveSequelNovelStory(@Body body: SaveSequelNovelStoryBody): ApiResponse<SaveSequelNovelStoryInfo>

    /**
     * 查询用户金币和算力聚合钱包
     */
    @POST(ApiConstants.QUERY_COINS_AND_POINTS_WALLET_INFO)
    suspend fun queryCoinsAndPointsWalletInfo(@Body body: UserTokenBody): ApiResponse<CoinsAndWalletInfo?>

    /**
     * 用户收益汇总
     */
    @POST(ApiConstants.USER_INCOME_SUMMARY)
    suspend fun userIncomeSummary(@Body body: UserTokenBody): ApiResponse<IncomeSummaryInfo?>

    /**
     * 用户收益明细分页
     */
    @POST(ApiConstants.USER_INCOME_DETAIL_PAGE)
    suspend fun userIncomeDetailPage(@Body body: IncomeDetailPageBody): ApiResponse<PagedList<IncomeDetailInfo>?>

    /**
     * 查询算力明细列表（分页）
     */
    @POST(ApiConstants.QUERY_POINT_DETAIL_LIST)
    suspend fun queryPointDetailList(@Body body: DetailTypeBody): ApiResponse<PagedList<ComputingPowerDetailsInfo>?>

    /**
     * 查询金币明细列表（分页）
     */
    @POST(ApiConstants.QUERY_COINS_DETAIL_LIST)
    suspend fun queryCoinsDetailList(@Body body: DetailTypeBody): ApiResponse<PagedList<ComputingPowerDetailsInfo>?>

    /**
     * 算力信息配置列表
     */
    @POST(ApiConstants.COINS_CONFIG_LIST)
    suspend fun coinsConfigList(@Body body: UserTokenBody): ApiResponse<CoinConfigData?>

    /**
     * 算力信息配置列表
     */
    @POST(ApiConstants.POINT_CONFIG_LIST)
    suspend fun pointConfigList(@Body body: UserTokenBody): ApiResponse<PointConfigData?>

    /**
     * 查询算力不足付费引导
     */
    @POST(ApiConstants.POINT_INSUFFICIENT_GUIDE)
    suspend fun pointInsufficientGuide(@Body body: InsufficientGuideBody): ApiResponse<PointInsufficientGuideData?>

    /**
     * 交易支付方式配置列表
     */
    @POST(ApiConstants.PAYMENT_CONFIG_LIST)
    suspend fun paymentConfigList(@Body body: UserTokenBody): ApiResponse<ListData<PaymentInfo>?>

    /**
     * 创建app支付宝支付订单
     */
    @POST(ApiConstants.CREATE_ORDER_ALIPAY)
    suspend fun createOrderAlipay(@Body body: CreatePointOrderBody): ApiResponse<PaymentData?>

    /**
     * 创建app微信支付订单
     */
    @POST(ApiConstants.CREATE_ORDER_WECHAT)
    suspend fun createOrderWechat(@Body body: CreatePointOrderBody): ApiResponse<PaymentData?>

    /**
     * 获取扣费策略规则信息
     */
    @POST(ApiConstants.QUERY_AI_PRICE)
    suspend fun queryAiPrice(@Body body: StrategyTypeBody): ApiResponse<AIPriceInfo?>

    /**
     * App转发H5支付-微信下单
     */
    @POST(ApiConstants.H5_PAY_WX)
    suspend fun h5PayWx(@Body body: H5PayInfo): ApiResponse<PaymentData?>

    /**
     * 购买会员-支付宝方式
     */
    @POST(ApiConstants.H5_PAY_ALIPAY)
    suspend fun h5PayAlipay(@Body body: H5PayInfo): ApiResponse<PaymentData?>

    /**
     * 获取弹窗和浮窗配置信息
     */
    @POST(ApiConstants.ACTIVITY_CONFIG)
    suspend fun activityConfig(): ApiResponse<ActivityConfigData?>

    /**
     * 查询一次性任务弹窗信息-首页登录
     */
    @POST(ApiConstants.LOGIN_TASK_POPUP)
    suspend fun loginTaskPopup(): ApiResponse<LoginTaskData?>

    /**
     * 查询一次性奖励任务列表
     */
    @POST(ApiConstants.ONCE_TASK_LIST)
    suspend fun onceTaskList(): ApiResponse<OnceTaskListData?>

    /**
     * 保存一次性任务记录-保存登录奖励任务完成
     */
    @POST(ApiConstants.REPORT_LOGIN_TASK)
    suspend fun reportLoginTask(): ApiResponse<LoginTaskData?>

    /**
     * 保存一次性任务记录-打开消息通知奖励
     */
    @POST(ApiConstants.REPORT_PUSH_NOTIFICATION_TASK)
    suspend fun reportPushNotificationTask(): ApiResponse<LoginTaskData?>

    /**
     * 敏感词校验
     */
    @POST(ApiConstants.SENSITIVE_WORD_CHECK)
    suspend fun sensitiveWordCheck(@Body body: ContentBody): ApiResponse<SensitiveWordCheckData?>

    /**
     * 内容安全-图片校验
     */
    @POST(ApiConstants.SENSITIVE_IMAGE_CHECK)
    suspend fun sensitiveImageCheck(@Body body: ImgUrlBody): ApiResponse<SensitiveWordCheckData?>

    /**
     * 小说上传-创建任务
     */
    @POST(ApiConstants.NOVEL_UPLOAD_SUMMARY)
    suspend fun novelUploadSummary(@Body body: UploadStoryBody): ApiResponse<WorkTaskData?>

    /**
     * 轮询查询小说上传生成作品剧集梗概结果
     */
    @POST(ApiConstants.ROLL_NOVEL_SUMMARY)
    suspend fun pollNovelDocsSummary(@Body body: WorkIdBody): ApiResponse<PollSummaryData?>

    /**
     * 查询任务概要结果-通用
     */
    @POST(ApiConstants.POLL_SUMMARY_RESULT)
    suspend fun pollSummaryResult(@Body body: WorkIdBody): ApiResponse<PollSummaryData?>

    /**
     * 查询任务概要结果-通用
     */
    @POST(ApiConstants.SAVE_SCRIPT)
    suspend fun saveScript(@Body body: SaveScriptBody): ApiResponse<WorkTaskData?>

    /**
     * 小说上传-剧集剧本-查询单集
     */
    @POST(ApiConstants.POLL_NOVEL_SCRIPT)
    suspend fun pollNovelScript(@Body body: PollScriptBody): ApiResponse<PollScriptData?>

    /**
     * 查询任务剧本结果- 通用
     */
    @POST(ApiConstants.POLL_SCRIPT_RESULT)
    suspend fun pollScriptResult(@Body body: PollScriptBody): ApiResponse<PollScriptData?>

    /**
     *小说上传-删除作品草稿
     */
    @POST(ApiConstants.NOVEL_UPLOAD_DELETE)
    suspend fun novelUploadDelete(@Body body: WorkIdBody): ApiResponse<Any>

    /**
     *单集作品-故事剧本生成
     */
    @Headers(
        "Accept: text/event-stream",  // 声明 SSE 媒体类型
        "Cache-Control: no-cache",     // 禁用缓存
        "Connection: keep-alive"       // 保持长连接
    )
    @POST(ApiConstants.SINGLE_WORK_GENERATE)
    fun singleWorkGenerate(@Body body: ContentBody): Flow<SSEvent>

    /**
     *算力预估
     */
    @POST(ApiConstants.POWER_CALCULATE)
    suspend fun powerCalculate(@Body body: PowerCalculateBody): ApiResponse<PowerCalculateData>

    /**
     *获取新手创作流程作品列表
     */
    @POST(ApiConstants.GET_NEWER_EXAMPLE_WORK_LIST)
    suspend fun getNewerExampleWorkList(): ApiResponse<List<ExampleWorkInfo>>

    /**
     * 创建小说-生成剧集剧本（首创/续集）任务
     */
    @POST(ApiConstants.CREATE_TASK_FROM_NOVEL)
    suspend fun createTaskFromNovel(@Body body: CreateTaskBody): ApiResponse<WorkTaskData?>

    /**
     * 创建小说-生成剧集剧本-续写任务
     */
    @POST(ApiConstants.CONTINUE_TASK_FROM_NOVEL)
    suspend fun continueTaskFromNovel(@Body body: ContinueTaskBody): ApiResponse<WorkTaskData?>

    /**
     * 创建灵感故事剧本（基于灵感写剧本/灵感续写）任务
     */
    @POST(ApiConstants.CREATE_TASK_FROM_INSPIRATION)
    suspend fun createTaskFromInspiration(@Body body: ContinueTaskBody): ApiResponse<WorkTaskData?>

    /**
     * 创建灵感故事梗概生成任务
     */
    @POST(ApiConstants.CREATE_SUMMARY_TASK_FROM_INSPIRATION)
    suspend fun createSummaryTaskFromInspiration(@Body body: CreateTaskBody): ApiResponse<WorkTaskData?>

    /**
     * 重试灵感故事梗概生成任务
     */
    @POST(ApiConstants.RETRY_SUMMARY_TASK_FROM_INSPIRATION)
    suspend fun retrySummaryTaskFromInspiration(@Body body: ContinueTaskBody): ApiResponse<WorkTaskData?>

    /**
     * 创建单集生成任务
     */
    @POST(ApiConstants.CREATE_TASK_FROM_SINGLE_EPISODE)
    suspend fun createTaskFromSingleEpisode(@Body body: CreateTaskBody): ApiResponse<WorkTaskData?>

    /** 查询已购买且尚未使用的短剧创作套餐。 */
    @POST(ApiConstants.UNUSED_PACKAGE_LIST)
    suspend fun unusedPackageList(@Body body: UserTokenBody): ApiResponse<UnusedPackageData?>

    /**
     *  单集生成任务-重试任务
     */
    @POST(ApiConstants.RETRY_TASK_FROM_SINGLE_EPISODE)
    suspend fun retryTaskFromSingleEpisode(@Body body: ContinueTaskBody): ApiResponse<WorkTaskData?>

    /**
     *  小说上传-剧集剧本-创建任务
     */
    @POST(ApiConstants.CONTINUE_TASK_FROM_NOVEL_DOC)
    suspend fun continueTaskFromNovelDoc(@Body body: ContinueTaskBody): ApiResponse<WorkTaskData?>

    /**
     * 字幕-触发分镜字幕生成
     */
    @POST(ApiConstants.GENERATE_SUBTITLE)
    suspend fun generateSubtitle(@Body body: GenerateSubtitleBody): ApiResponse<Any?>

    /**
     * 字幕-查询分镜字幕状态及内容
     */
    @POST(ApiConstants.SUBTITLE_TASK_STATUS)
    suspend fun subtitleTaskStatus(@Body body: StoryboardIdBody): ApiResponse<SubtitleTaskStatusInfo?>

    /**
     * 获取新人专享礼
     */
    @POST(ApiConstants.GET_FIRST_RECHARGE_DIALOG_INFO)
    suspend fun getFirstRechargeDialogInfo(): ApiResponse<FirstRechargeDialogData?>

    /**
     * 获取算力新人专享礼
     */
    @POST(ApiConstants.GET_FIRST_RECHARGE_CREDITS_DIALOG_INFO)
    suspend fun getFirstRechargeCreditsDialogInfo(@Body body: FirstRechargeCreditsBody): ApiResponse<FirstRechargeCreditsDialogData?>

    /**
     * 播放-分页查询播放历史列表
     */
    @POST(ApiConstants.USER_PLAY_HISTORY_PAGE)
    suspend fun userPlayHistoryPage(@Body body: PagedBody): ApiResponse<PagedList<WorkVOInfo>?>

    /**
     * 播放-分页查询播放历史列表
     */
    @POST(ApiConstants.USER_COLLECTION_PAGE)
    suspend fun userCollectionPage(@Body body: PagedBody): ApiResponse<PagedList<WorkVOInfo>?>

    /**
     * 首页-书架首页聚合（横滑全部作品，和瀑布流id）
     */
    @POST(ApiConstants.BOOKSHELF_HOME)
    suspend fun bookShelfHome(): ApiResponse<HomeBookShelfData?>

    /**
     * 首页-分页查询书架瀑布流
     */
    @POST(ApiConstants.HOME_WATERFALL_PAGE)
    suspend fun homeWaterfallPage(@Body body: BookShelfIdPagedBody): ApiResponse<PagedList<WorkVOInfo>?>

    /**
     * 播放器-作品剧集目录分集分页
     */
    @POST(ApiConstants.EPISODE_DIRECTORY_PAGE)
    suspend fun episodeDirectoryPage(@Body body: WorkIdPagedBody): ApiResponse<PagedList<EpisodeInfo>?>

    /**
     * 播放器-作品剧集目录头信息
     */
    @POST(ApiConstants.EPISODE_DIRECTORY_INFO)
    suspend fun episodeDirectoryInfo(@Body body: WorkIdBody): ApiResponse<EpisodeDirectoryData?>

    /**
     * 播放器-单集播放信息
     */
    @POST(ApiConstants.EPISODE_PLAY_INFO)
    suspend fun episodePlayInfo(@Body body: WorkBody): ApiResponse<EpisodePlayData?>

    /**
     * 播放器-书架作品详情
     */
    @POST(ApiConstants.SIMPLE_WORK_INFO)
    suspend fun simpleWorkInfo(@Body body: WorkIdBody): ApiResponse<WorkVOInfo?>

    /**
     * 播放器-播放量累加
     */
    @POST(ApiConstants.PLAY_COUNT_INC)
    suspend fun playCountInc(@Body body: WorkBody): ApiResponse<Any?>

    /**
     * 点赞-切换点赞/取消
     */
    @POST(ApiConstants.LIKE_TOGGLE)
    suspend fun likeToggle(@Body body: WorkBody): ApiResponse<LikeData?>

    /**
     * 收藏-切换收藏/取消收藏作品
     */
    @POST(ApiConstants.COLLECT_TOGGLE)
    suspend fun collectToggle(@Body body: WorkIdBody): ApiResponse<CollectData?>

    /**
     * 播放器-下一部作品推荐
     */
    @POST(ApiConstants.NEXT_WORK_RECOMMEND)
    suspend fun nextWorkRecommend(@Body body: CurrentWorkIdBody): ApiResponse<WorkVOInfo?>

    /**
     * 播放器-用户态查询
     */
    @POST(ApiConstants.USER_PLAYER_STATE)
    suspend fun userPlayerState(@Body body: WorkBody): ApiResponse<UserPlayData?>

    /**
     *播放-播放行为上报
     */
    @POST(ApiConstants.USER_PLAY_REPORT)
    suspend fun userPlayReport(@Body body: UserPlayBody): ApiResponse<Any?>

    /**
     * google预下单
     */
    @POST(ApiConstants.CREATE_ORDER)
    suspend fun createOrder(@Body body: CreateOrderBody): ApiResponse<OrderNoData>

    /**
     * googlePay创建预内部单
     */
    @POST(ApiConstants.CREATE_GOOGLE_PRE_ORDER)
    suspend fun createGooglePreOrder(@Body body: CreateOrderBody): ApiResponse<OrderNoData>

    /**
     * 验证票据
     */
    @POST(ApiConstants.VERIFY_PURCHASED)
    suspend fun verifyPurchased(@Body body: VerifyPurchasedBody): ApiResponse<Any?>

    /**
     * 首页-搜索作品
     */
    @POST(ApiConstants.HOME_SEARCH)
    suspend fun homeSearch(@Body body: KeyworkBody): ApiResponse<List<WorkVOInfo>?>

    /**
     * 签到任务列表
     */
    @POST(ApiConstants.REWARD_CHECKIN_LIST)
    suspend fun rewardCheckInList(@Body body: UserTokenBody): ApiResponse<RewardCheckInListData>

    /**
     * 签到上报
     */
    @POST(ApiConstants.REWARD_CHECKIN_REPORT)
    suspend fun rewardCheckInReport(@Body body: UserTokenBody): ApiResponse<RewardCheckInReportData>

    /**
     * 观看广告任务列表
     */
    @POST(ApiConstants.REWARD_AD_TASK_LIST)
    suspend fun rewardAdTaskList(@Body body: UserTokenBody): ApiResponse<RewardAdTaskListData>

    /**
     * 获取用户福利未领取奖励总额
     */
    @POST(ApiConstants.REWARD_USER_UNCHECK)
    suspend fun rewardUserUncheck(@Body body: UserTokenBody): ApiResponse<RewardUserUncheckData?>

    /**
     * 播放器-剧集自动解锁（每集预加载时调用，余额充足则静默扣费解锁）
     */
    @POST(ApiConstants.STORY_UNLOCK_PRELOAD)
    suspend fun storyUnlockPreload(@Body body: WorkBody): ApiResponse<StoryUnlockPreloadData?>

    /**
     * 解锁弹窗-章节解锁信息
     */
    @POST(ApiConstants.STORY_UNLOCK_INFO)
    suspend fun storyUnlockInfo(@Body body: WorkBody): ApiResponse<StoryUnlockInfoData?>

    /**
     * 解锁弹窗-金币解锁
     */
    @POST(ApiConstants.STORY_UNLOCK_COINS)
    suspend fun storyUnlockCoins(@Body body: StoryUnlockCoinsBody): ApiResponse<StoryUnlockResultData?>

    /**
     * 剧集解锁-激励视频广告前置准备
     */
    @POST(ApiConstants.AD_REWARD_STORY_UNLOCK_PREPARE)
    suspend fun storyUnlockAdPrepare(@Body body: WorkBody): ApiResponse<RewardAdPrepareData?>

    /**
     * 剧集解锁-激励视频广告结果查询
     */
    @POST(ApiConstants.AD_REWARD_STORY_UNLOCK_COMPLETE)
    suspend fun storyUnlockAdComplete(@Body body: StoryUnlockAdCompleteBody): ApiResponse<RewardAdCompleteData?>

    /**
     * 签到任务-激励视频广告前置准备
     */
    @POST(ApiConstants.AD_REWARD_CHECKIN_PREPARE)
    suspend fun rewardAdCheckInPrepare(@Body body: UserTokenBody): ApiResponse<RewardAdPrepareData?>

    /**
     * 签到任务-激励视频广告结果查询
     */
    @POST(ApiConstants.AD_REWARD_CHECKIN_COMPLETE)
    suspend fun rewardAdCheckInComplete(@Body body: RewardAdCompleteBody): ApiResponse<RewardAdCompleteData?>

    /**
     * 广告任务-激励视频广告前置准备
     */
    @POST(ApiConstants.AD_REWARD_TASK_PREPARE)
    suspend fun rewardAdTaskPrepare(@Body body: RewardAdTaskPrepareBody): ApiResponse<RewardAdPrepareData?>

    /**
     * 广告任务-激励视频广告结果查询
     */
    @POST(ApiConstants.AD_REWARD_TASK_COMPLETE)
    suspend fun rewardAdTaskComplete(@Body body: RewardAdCompleteBody): ApiResponse<RewardAdCompleteData?>

    /**
     * 查询剧集锁定状态
     * 纯读接口，无副作用。返回 lockStatus：0=免费集 / 1=已解锁 / 2=未解锁付费集
     */
    @POST(ApiConstants.EPISODE_UNLOCK_LOCK_STATUS)
    suspend fun episodeUnlockLockStatus(@Body body: WorkBody): ApiResponse<StoryUnlockStatusData?>

    /**
     * 剧集-修改剧集梗概
     */
    @POST(ApiConstants.UPDATE_STORY_SUMMARY)
    suspend fun updateStorySummary(@Body body: SaveStorySummaryBody): ApiResponse<Any?>

    /**
     * 获取作品剧集分镜配置
     */
    @POST(ApiConstants.GET_STORYBOARD_MODEL_CONFIG)
    suspend fun getStoryboardModelConfig(@Body body: WorkBody): ApiResponse<List<CreationModelInfo>?>

    /**
     * 切换分镜模型
     */
    @POST(ApiConstants.SWITCH_MODEL)
    suspend fun switchModel(@Body body: SwitchModelBody): ApiResponse<Any>

    /**
     *取消场景形象选用
     */
    @POST(ApiConstants.CANCEL_SCENE_IMAGE)
    suspend fun cancelSceneImage(@Body body: AppearanceIdBody): ApiResponse<Any?>

    /**
     *取消角色形象选用
     */
    @POST(ApiConstants.CANCEL_ROLE_IMAGE)
    suspend fun cancelRoleImage(@Body body: AppearanceIdBody): ApiResponse<Any?>

    /**
     *取消道具形象选用
     */
    @POST(ApiConstants.CANCEL_PROP_IMAGE)
    suspend fun cancelPropImage(@Body body: AppearanceIdBody): ApiResponse<Any?>

    /**
     * 移除当前集资产图片
     */
    @POST(ApiConstants.WORK_ASSET_STORY_REMOVE_IMAGE)
    suspend fun workAssetStoryRemoveImage(@Body body: AssetStoryImageBody): ApiResponse<Any?>

    /**
     * 选择当前集资产图片
     */
    @POST(ApiConstants.WORK_ASSET_STORY_SELECT_IMAGE)
    suspend fun workAssetStorySelectImage(@Body body: AssetStoryImageBody): ApiResponse<Any?>

    /**
     * 作品角色形象列表
     */
    @POST(ApiConstants.WORK_ASSET_ROLE_IMAGE_PAGE)
    suspend fun workAssetRoleImagePage(@Body body: RoleIdPagedBody): ApiResponse<PagedList<RoleImageInfo>?>

    /** 灵感案例、热门案例使用次数上报。 */
    @POST(ApiConstants.REPORT_CREATION_USAGE_COUNT)
    suspend fun reportCreationUsageCount(@Body body: ReportCreationUsageBody): ApiResponse<Any?>

}
