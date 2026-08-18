package com.kuaigeglobal.epnova.core.network.apis.response

import com.google.gson.annotations.SerializedName
import com.kuaigeglobal.epnova.core.network.apis.entity.KeyValueInfo


/**
 * 软件配置信息
 */
class AppConfigData {
    /**
     * 用户协议
     */
    var userProtocolUrl: String? = null

    /**
     * 隐私政策
     */
    var privacyProtocolUrl: String? = null

    /**
     * 备案号
     */
    var beianNo: String? = null

    /**
     * 备案号跳转链接
     */
    var beianNoJumpUrl: String? = null

    /**
     * 算法公式
     */
    var algorithmUrl: String? = null

    /**
     * 合法域名集合
     */
    var clientLegalH5Domain: List<String>? = null

    /**
     * 用户端-APP存储TOS域名集合
     */
    val tosDomainList: List<String>? = null

    /**
     * 用户端-APP存储OSS域名集合
     */
    val ossDomainList: List<String>? = null

    /**
     * 用户端-支付协议
     */
    val clientPaymentProtocolUrl: String? = null

    /**
     * 用户端-服务购买须知
     */
    val clientTermsOfServiceUrl: String? = null

    /**
     * 个人信息收集清单
     */
    val personalInfoCollectionListUrl: String? = null

    /**
     * 第三方信息共享清单
     */
    val thirdPartyInfoShareListUrl: String? = null

    /**
     * 用户反馈类型下拉选项
     */
    val userFeedBackTypeList: List<KeyValueInfo>? = null

    //TODO下面这些配置信息是老的，可能实际不存在，使用前请注意

    /**
     * 支付排序和状态
     */
    val paymentStatus: PaymentStatus? = null

    /**
     * 联系客服
     */
    val customerServiceUrl: String? = null

    val cloneTimbreOriginalVocalsDurationLimit: String = "30-60"


    /**
     * 付费会员协议
     */
    val userVipProtocolUrl: String? = null

    //1v1客服信息
    val customerServiceInfo: CustomerServiceInfo? = null

    //字数输入限制相关
    val shortDramas: TextLimitNum? = null

    @SerializedName("eraV2Configs")
    val ageRangeList: List<AgeRange>? = null


    /**
     * 用户举报类型下拉选项
     */
    val userTipoffTypeList: List<KeyValueInfo>? = null

    /**
     * 打开app加载tab 0=首页 1=爆款 2=消息 3=我的
     */
    val openAppLoadTab: Int? = null

    /**
     * 付费须知文案
     */
    val paymentNotice: String? = null

    /**
     * 积分兑换收益协议
     */
    val shortDramasIntegralExchangeProfitProtocol: String? = null

    /**
     * 小程序类型： 0 = 正式版 1 = 测试版 2 = 体验版
     */
    var clientWxMiniShareMode: Int = 0

    /**
     * 作品-小说字数限制数字（500000）
     */
    val workNovelWordLimit: Int? = 600000

    /**
     * 课程-课程banner
     */
    val workCourseBanner: SimpleBannerInfo? = null
    /**
     * 帮助中心
     */
    val appHelpCenterUrl: String? = null

    /**
     * APP-添加客户引导弹窗图-算力购买后 20260323新增
     */
    val appPopupPointGuideImageUrl: String? = null

    /**
     * APP-添加客户引导弹窗图-课程购买后 20260323新增
     */
    val appPopupCourseGuideImageUrl: String? = null

    /**
     * APP-跳转小程序地址-客服 20260323新增
     */
    val appJumpMiniProgramCustomerServiceUrl: String? = null

    /**
     * APP-跳转小程序地址-导师 20260323新增
     */
    val appJumpMiniProgramAdvisorUrl: String? = null

    /**
     * 20260512新增作品-算力/权益类弹窗配置（先根据enableSwitch判断开关再跳转url）
     */
    val workPointPowerPopUpDto: WorkPointPowerPopUpDto? = null

    /**
     * 20260817新增APP 默认进入页面
     * 0 - Home
     * 1 - Featured
     * 2 - Create
     * 3 - My list
     * 4 - Profile
     */
    val defaultLandingTab: Int? = null
}

data class SimpleBannerInfo(
    val bannerUrl: String? = null,
    val jumpUri: String? = null,
)

data class PaymentStatus(
    val wxpay: PaymentRanking? = null,
    val alipay: PaymentRanking? = null
)

data class PaymentRanking(
    //0:关闭 1:开启
    val status: Int? = null,
    //排序，数字大的在前面
    val short: Int? = null
)


data class WorkPointPowerPopUpDto(
    val enableSwitch: Int? = null,//是否开启（0-关，1-开）
    val jumpUri: String? = null,
    val titleText: String? = null,
)

/**
 * 年龄范围
 */
data class AgeRange(
    val id: String? = null,
    val eraDesc: String? = null,
    val eraMinAge: Int? = null,
    val eraMaxAge: Int? = null,
)

data class CustomerServiceInfo(
    //客服链接
    val customerServiceJmpUrl: String? = null,
    //企业id
    val enterpriseId: String? = null
)

/**
 * {
 *             "shortDramasWriteStoryLimitNum": "自己写故事字数上限-普通用户",
 *             "shortDramasWriteStoryLimitNumForVip": "自己写故事字数上限-会员用户",
 *             "shortDramasEditStoryLimitNum": "编辑故事剧本字数上限",
 *             "shortDramasSceneDescriptionLimitNum": "分镜画面描述字数上限",
 *             "shortDramasSceneActionLimitNum": "分镜动作描述字数上限",
 *             "shortDramasSceneSideWhiteLimitNum": "分镜旁白字数上限"
 *         }
 */
data class TextLimitNum(
    val shortDramasEditStoryLimitNum: Int? = 2000,
    val shortDramasGenerateRoleImageDescriptionLimitNum: Int? = 500,//生成角色形象描述文字字数上限
    val shortDramasSceneActionLimitNum: Int? = 500,
    val shortDramasSceneDescriptionLimitNum: Int? = 500,
    val shortDramasSceneSideWhiteLimitNum: Int? = 80,
    val shortDramasWriteStoryLimitNum: Int? = 1000,
    val shortDramasWriteStoryLimitNumForVip: Int? = 5000,
    val shortDramasInspirationWriteStoryLimitNum: Int? = 1000,//灵感写故事字数上限
    val shortDramasInspirationStoryRemindText: String? = null,//灵感故事提醒文案
    val shortDramasWriteStoryRemindText: String? = null//自己写故事提醒文案
)
