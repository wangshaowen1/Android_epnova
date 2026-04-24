package com.kuaigeglobal.epnova.core.network.apis

import com.kuaigeglobal.epnova.BuildConfig

object ApiConstants {

    //改地址
    //"http://hk308942lu51.vicp.fun"
    val BASE_URL: String = BuildConfig.SERVER_URL

    /**
     * 获取软件配置信息接口
     */
    const val APP_CONFIG = "app/getAppConfigInfo"

    //用户举报
    const val TIP_OFF = "user/tipOff"

    //查询指定版本上架状态
    const val GET_APP_SHELF_STATUS = "app/getVersionShelfStatus"

    /**
     * 获取用户主页信息接口
     */
    const val GET_USER_HOME_INFO = "auth/user/info-detail"

    /**
     * 修改用户信息
     */
    const val COMPLETE_USER_DATA_INFO = "user/modifyUserInfo"

    /**
     * 获取APP升级信息
     */
    const val GET_APP_UPGRADE_INFO = "app/getAppUpgradeInfo"

    /**
     *游客登陆
     */
    const val LOGIN_BY_GUEST = "auth/guest/login"

    /**
     * 第三方登录
     */
    const val LOGIN_BY_THIRD = "auth/loginByThirdParty"

    /**
     * 退出登录
     */
    const val LOGOUT_APP = "auth/sign-out"

    /**
     * 注销账号
     */
    const val LOG_OFF = "auth/logout"

    /**
     * 获取用户分享链接
     */
    const val GET_USER_SHARE_URL = "user/getUserShareUrl"


    /**
     * 用户分享操作上报
     */
    const val SHARE_REPORT = "user/action/share/report"

    /**
     * 提交用户的反馈数据
     */
    const val FEED_BACK = "app/feedback/save"

    /**
     * 分页查询生成中状态的歌曲列表-轮询使用
     */
    const val GENERATING_BOOK_LIST = "userPictureBook/current/list"

    /**
     * 会员配置
     */
    const val VIP_CONFIG = "vip/getVipConfig"


    /**
     * 用户日活记录
     */
    const val DAILY = "user/login/daily"

    /**
     * 歌曲生成来源上报
     */
    const val MUSIC_GENERATE_SOURCE_REPORT = "music/generate/source/report"


    /**
     * 发送短信验证码
     */
    const val SEND_SMS_VALIDATE_CODE = "auth/sendSmsValidateCode"

    /**
     * 绑定手机号
     */
    const val BIND_PHONE = "user/phone/action/bind"


    /**
     * 手机登录接口--闪验方式
     */
    const val LOGIN_BY_FLASH_MOBILE = "auth/loginByFlashMobile"

    /**
     * 手机登录接口--验证码方式
     */
    const val LOGIN_BY_PHONE = "auth/register"

    /**
     * 取消注销
     */
    const val CANCEL_LOG_OFF = "user/remove/cancel"

    /**
     * 用户注销状态
     */
    const val LOG_OFF_STATUS = "user/remove/state"

    /**
     * 预生成短剧剧本大纲
     */
    const val CREATION_GENERATE_PRE = "shortDramas/structure/script/generate/pre"

    /**
     * 灵感内容优化（AI优化灵感）
     */
    const val SSE_CONTENT_OPTIMIZE = "ai/stream/inspiration/refine"

    /**
     * 随机灵感（无入参）
     */
    const val SSE_CONTENT_RANDOM = "ai/stream/inspiration/random"

    /**
     * 作品简介优化
     */
    const val SSE_WORK_DESC_OPTIMIZE = "ai/stream/intro/refine"

    /**
     * 灵感故事剧本（基于灵感写剧本/灵感续写/小说续写）
     */
    const val SSE_SCRIPT_FROM_IDEA = "ai/stream/script/from-idea"

    /**
     * 小说直接生成剧集剧本
     */
    const val SSE_SCRIPT_FROM_STORY = "ai/stream/script/from-novel"

    /**
     * 扩写剧集剧本（小说）
     */
    const val SSE_CONTENT_AI_EXPAND = "ai/stream/script/expand"

    /**
     * 故事梗概生成（灵感）
     */
    const val SSE_CONTENT_SYNOPSIS = "ai/stream/script/synopsis"

    /**
     *灵感保存故事梗概（下一步创建剧本）
     */
    const val SAVE_WORK_SUMMARY = "work/production/new/saveStorySummary"

    /**
     *小说保存故事剧集剧本（首次创建）
     */
    const val SAVE_STORY_EPISODE_SCRIPT = "work/production/new/novel/saveStoryContent"

    /**
     *单集作品故事剧本保存（首次创建）
     */
    const val SAVE_SINGLE_EPISODE_SCRIPT = "work/production/new/singleEpisode/saveStoryContent"

    /**
     *保存故事剧集剧本（灵感首创/灵感续集/小说续集）
     */
    const val SAVE_EPISODE_SCRIPT = "work/production/new/saveStoryContent"

    /**
     *轮询剧本分镜大纲
     */
    const val GET_STORY_BOARD_LIST = "shortDramas/structure/shot/info"

    /**
     *预生成剧本分镜大纲（重新生成分镜大纲）
     */
    const val GET_STORY_BOARD_LIST_RETRY = "shortDramas/structure/shot/generate/pre"

    /**
     * 编辑故事剧本内容
     */
    const val DRAMA_CONTENT_EDIT = "shortDramas/scriptContent/edit"

    /**
     * 编辑剧本分镜内容(画面)
     */
    const val SCENE_CONTENT_EDIT = "shortDramas/structure/shot/content/edit"

    /**
     * 计算生成剧本价格（积分）
     */
    const val SHORT_DRAMAS_CALCULATE_PRICE = "shortDramas/script/generate/price/calculate"

    /**
     * 正式生成剧本(扣积分)
     */
    const val SHOT_DRAMA_GENERATE = "shortDramas/script/generate"

    /**
     *短剧风格列表
     */
    const val CREATION_STYLE_LIST = "shortDramas/style/list"

    /**
     * 优化生成剧本的角色的画面描述
     */
    const val OPTIMIZE_ROLE_IMAGE_DESC = "shortDramas/script/role/imageDesc/optimize"

    /**
     * 重新生成剧本的角色
     */
    const val ROLE_IMAGE_REGENERATE = "shortDramas/script/role/generate"

    /**
     * 查询剧本的角色标签列表
     */
    const val ROLE_TAG_LIST = "shortDramas/script/role/tag/list"

    /**
     * 合成短剧
     */
    const val SHORT_DRAMAS_SYNTHESIS = "shortDramas/shot/synthesis"

    /**
     * 合成短剧(上传合成的视频)
     */
    const val SHORT_DRAMAS_SYNTHESIS_UPLOAD = "shortDramas/shot/synthesis/upload"

    /**
     * 查询剧本信息（作品草稿详情也用这个、未上架使用）
     */
    const val SHORT_DRAMAS_INFO = "shortDramas/script/info/unlaunch"

    /**
     * 查询剧本信息（已经上架使用，看他人的剧本）
     */
    const val SHORT_DRAMAS_RELEASED_INFO = "shortDramas/script/info/launched/other"

    /**
     * 提交字幕配音
     */
    const val SHORT_DRAMAS_DUBBING = "shortDramas/confirm/subtitle/voice"

    /**
     * 配音失败-重新配音
     */
    const val SHORT_DRAMAS_DUBBING_RETRY = "shortDramas/new/dubbing"

    /**
     * 使用配音
     */
    const val SHORT_DRAMAS_DUBBING_USE = "shortDramas/use/subtitle/voice"

    /**
     * 字幕配音生成状态
     */
    const val SHORT_DRAMAS_DUBBING_STATUS = "shortDramas/subtitle/voice/status"

    /**
     * 使用历史分镜（分镜替换）
     */
    const val SHORT_DRAMAS_SEGMENT_IMAGE_REPLACE = "shortDramas/script/shot/image/history/use"

    /**
     * 查询历史分镜
     */
    const val SHORT_DRAMAS_SEGMENT_IMAGE_HISTORY = "shortDramas/script/shot/image/history"

    /**
     * 重绘分镜（重新生成分镜的图片）
     */
    const val SHORT_DRAMAS_SEGMENT_IMAGE_REGENERATE = "shortDramas/script/shot/image/regenerate"

    /**
     * 优化重绘分镜的画面描述
     */
    const val SHORT_DRAMAS_SEGMENT_IMAGE_DESC_OPTIMIZE = "shortDramas/script/shot/imageDesc/optimize"

    /**
     * 生成剧本图片(失败重试)
     */
    const val SHORT_DRAMAS_SEGMENT_IMAGE_GENERATE_RETRY = "shortDramas/script/image/generate/retry"

    /**
     * 删除剧本
     */
    const val DELETE_DRAMA_INFO = "shortDramas/work/remove"

    /**
     * 用户收益信息
     */
    const val USER_REVENUE_INFORMATION = "user/profit/info"

    /**
     * 解锁首次生成的短剧(首次生成的短剧付费)
     */
    const val UNLOCK_FIRST_GENERATED_SHORT_DRAMA = "shortDramas/script/generate/first/payStatus/unlock"

    /**
     * 追剧或取消追剧
     */
    const val SERIES_FOLLOW = "shortDramasSeries/follow"

    /**
     * 点赞剧本
     */
    const val LIKE_SHORT_DRAMA = "shortDramas/script/like"

    /**
     * 修改剧集价格
     */
    const val SERIES_SET_PRICE = "shortDramasSeries/modifyPrice"

    /**
     * 获取用户可续写的剧集列表
     */
    const val USER_CONTINUABLE_DRAMAS = "shortDramasSeries/userCanRewriteDramasList"

    /**
     * 续创时查询上一集内容详情
     */
    const val LATEST_DRAMAS_WORK_DETAIL = "work/production/sequel/sequelDetail"

    /**
     * 查询我的作品详情
     */
    const val WORK_INFO = "work/detail"

    /**
     * 获取剧集价格配置
     */
    const val SERIES_PRICE_CONFIG = "shortDramasSeries/priceConfig"

    /**
     * 修改剧集信息
     */
    const val SERIES_MODIFY_INFO = "shortDramasSeries/modify"

    /**
     * 上架剧集
     */
    const val SERIES_PUBLISH = "shortDramasSeries/shelf"

    /**
     * 分页查看作品剧集列表
     */
    const val EPISODE_LIST = "work/storyPage"

    /**
     * 预生成短剧剧本(流式输出完之后调)
     */
    const val DRAMA_GENERATE = "shortDramas/script/generate/pre"

    /**
     * 生成短剧来源上报(调完生成短剧剧本接口后调用)
     */
    const val DRAMA_GENERATE_SOURCE_REPORT = "shortDramas/script/generate/source/report"

    /**
     * 查询剧本的角色列表和标题(轮询使用)
     */
    const val DRAMA_ROLES_POLLING = "shortDramas/script/roleListAndName/polling"

    /**
     * 解锁剧集
     */
    const val UNLOCK_SHORT_DRAMA = "shortDramasSeries/unlockDramas"

    /**
     * 自动解锁剧集开关上报
     */
    const val AUTO_UNLOCK_SWITCH = "shortDramasSeries/autoUnlockSwitch"

    /**
     * 发布剧本
     */
    const val PUBLISH_SHORT_DRAMA = "shortDramas/work/publish"

    /**
     * 获取订单状态
     */
    const val GET_ORDER_STATUS = "order/getOrderStatus"

    /**
     * 爆款推荐点击上报
     */
    const val RECOMMEND_CLICK_REPORT = "popular/recommend/dramas/click"

    /**
     * 新增剧本的角色
     */
    const val DRAMAS_ADD_ROLE = "shortDramas/script/role/create"

    /**
     * 删除剧本的角色
     */
    const val DRAMAS_DEL_ROLE = "shortDramas/script/role/remove"

    const val APP_BANNERS = "app/home/banner/list"

    /**
     * 获取随机灵感示例
     */
    const val INSPIRATION_QUICK_LIST = "work/config/getInspirationExamples"

    /**
     * 使用历史角色
     */
    const val HISTORY_ROLE_SWITCH = "shortDramas/script/role/history/use"

    /**
     * 查询历史角色
     */
    const val GET_HISTORY_ROLES = "shortDramas/script/role/history"

    /**
     * 首页作品列表
     */
    const val RECOMMEND_WORK_LIST = "app/home/work/page"

    /**
     * 获取集数/视频比例/视频风格配置
     */
    const val VIDEO_SET_CONFIG = "work/config/getWorkSetUpConfig"

    /**
     * 查询资产参考图列表
     */
    const val GET_HISTORY_REFERENCE_IMAGE = "work/asset/getReferenceImage"

    /**
     * 编辑我的作品信息
     */
    const val UPDATE_WORK_INFO = "work/update"

    /**
     * 作品播放量计数
     */
    const val PLAY_EVENT_REPORT = "app/home/work/playCount"

    /**
     * 生成所有对应资产描述
     */
    const val GENERATE_ALL_ASSET_DESCRIPTION = "ai/asset/generation/description"

    /**
     * 轮询查询全部资产描述
     */
    const val WORK_ASSET_ALL_STATUS = "ai/task/query/asset/all"

    /**
     * 轮询查询单个资产
     */
    const val WORK_ASSET_SINGLE_STATUS = "ai/task/query/asset/single"

    /**
     * 场景列表
     */
    const val WORK_ASSET_SCENE_LIST = "work/asset/scene/list"

    /**
     * 场景保存
     */
    const val WORK_ASSET_SCENE_SAVE = "work/asset/scene/save"

    /**
     * 场景单个删除
     */
    const val WORK_ASSET_SCENE_DELETE = "work/asset/scene/delete"

    /**
     * 角色列表
     */
    const val WORK_ASSET_ROLE_LIST = "work/asset/role/list"

    /**
     * 角色保存
     */
    const val WORK_ASSET_ROLE_SAVE = "work/asset/role/save"

    /**
     * 角色单个删除
     */
    const val WORK_ASSET_ROLE_DELETE = "work/asset/role/delete"

    /**
     * 道具列表
     */
    const val WORK_ASSET_PROP_LIST = "work/asset/prop/list"

    /**
     * 道具保存
     */
    const val WORK_ASSET_PROP_SAVE = "work/asset/prop/save"

    /**
     * 道具单个删除
     */
    const val WORK_ASSET_PROP_DELETE = "work/asset/prop/delete"

    /**
     * 生成单个资产图片（重新生成/生成）
     */
    const val WORK_ASSET_SINGLE_GENERATE = "ai/asset/generation/single/image"

    /**
     * 一键生成所有资产图片
     */
    const val WORK_ASSET_ONE_KEY_GENERATE = "ai/asset/oneClickGeneration"

    /**
     * 生成全部分镜描述
     */
    const val GENERATE_ALL_STORYBOARD_DESCRIPTION = "ai/storyboard/generation/description"

    /**
     * 轮询查询全部分镜描述
     */
    const val GENERATE_ALL_STORYBOARD_DESCRIPTION_STATUS = "ai/task/query/storyboard/all"

    /**
     * 轮询查询单个分镜图片
     */
    const val WORK_STORYBOARD_SINGLE_STATUS = "ai/task/query/storyboard/single"

    /**
     * 分镜设计列表
     */
    const val WORK_STORYBOARD_LIST = "work/storyboard/designList"

    /**
     * 分镜保存
     */
    const val WORK_STORYBOARD_SAVE = "work/storyboard/save"

    /**
     * 分镜单个删除
     */
    const val WORK_STORYBOARD_DELETE = "work/storyboard/delete"

    /**
     * 生成单个分镜图片（重新生成/点击生成）
     */
    const val WORK_STORYBOARD_SINGLE_GENERATE = "ai/storyboard/generation/image"

    /**
     * 生成全部分镜视频
     */
    const val WORK_STORYBOARD_VIDEO_GENERATE = "ai/storyboard/generation/video"

    /**
     * 生成单个分镜视频
     */
    const val WORK_STORYBOARD_VIDEO_SINGLE_GENERATE = "ai/storyboard/generation/single/video"

    /**
     * 重绘分镜完成后不合成还原
     */
    const val WORK_STORYBOARD_RECREATION = "work/storyboard/storyboardRecreation"

    /**
     * 分镜视频合成
     */
    const val WORK_STORYBOARD_VIDEO_SYNTHESIS = "work/storyboard/synthesizeVideo"

    /**
     * 举报提交
     */
    const val REPORT_SUBMISSION = "app/report/save"

    /**
     * 分页查询我的作品列表
     */
    const val MY_WORK = "work/page"


    /**
     * 批量删除我的作品
     */
    const val BATCH_DELETE_WORK = "work/batchDeleteIds"

    /**
     * 批量删除我的作品剧集
     */
    const val BATCH_DELETE_WORK_EPISODE = "work/batchDeleteStoryByIds"


    /**
     * 轮询查询我的作品状态列表
     */
    const val GET_WORK_STATUS_LIST = "work/getWorkStatusList"

    /**
     * 查看作品单个剧集详情
     */
    const val SINGLE_EPISODE_DETAILS = "work/storyDetail"

    /**
     * 查看单集作品的剧集详情
     */
    const val SINGLE_EPISODE_SERIES_DETAILS = "work/singleEpisode/storyDetail"


    /**
     * 小说-保存续创剧集（只有小说有）
     */
    const val SAVE_SEQUEL_NOVEL_STORY = "work/production/sequel/saveSequelNovelStory"

    /**
     * 查询用户钱包信息
     */
    const val QUERY_WALLET_INFO = "trade/user/wallet/current"

    /**
     * 查询算力明细列表（分页）
     */
    const val QUERY_POINT_DETAIL_LIST = "trade/point/detail/queryPointDetailList"

    /**
     * 算力信息配置列表
     */
    const val POINT_CONFIG_LIST = "trade/point/config/list"

    /**
     * 交易支付方式配置列表
     */
    const val PAYMENT_CONFIG_LIST = "trade/pay/way/config/list"

    /**
     * 创建app支付宝支付订单
     */
    const val CREATE_ORDER_ALIPAY = "trade/pay/order/createAppAlipay"

    /**
     * 创建app微信支付订单
     */
    const val CREATE_ORDER_WECHAT = "trade/pay/order/createAppWechat"

    /**
     * 获取扣费策略规则信息
     */
    const val QUERY_AI_PRICE = "trade/deduct/strategy/rule/info"

    /**
     * App转发H5支付-微信下单
     */
    const val H5_PAY_WX = "app/forward/h5/createAppWechat"

    /**
     * App转发H5支付-支付宝下单
     */
    const val H5_PAY_ALIPAY = "app/forward/h5/createAppAliPay"

    /**
     * 获取弹窗和浮窗配置信息
     */
    const val ACTIVITY_CONFIG = "app/window/config"

    /**
     * 敏感词校验
     */
    const val SENSITIVE_WORD_CHECK = "ai/content-security/text/check"

    /**
     * 内容安全-图片校验
     */
    const val SENSITIVE_IMAGE_CHECK = "ai/content-security/img/check"

    /**
     * 小说上传-创建任务
     */
    const val NOVEL_UPLOAD_SUMMARY = "ai/novel/upload/task/create"

    /**
     * 轮询查询小说上传生成作品剧集梗概结果
     */
    const val ROLL_NOVEL_SUMMARY = "ai/novel/upload/task/query"

    /**
     * 小说上传-剧集剧本-查询单集
     */
    const val POLL_NOVEL_SCRIPT = "ai/novel/upload/story/content/query"

    /**
     * 小说上传-删除作品草稿
     */
    const val NOVEL_UPLOAD_DELETE = "ai/novel/upload/draft/delete"

    /**
     * 单集作品-生成标题/剧本/资产
     */
    const val SINGLE_WORK_GENERATE = "ai/stream/script/single/work"

    /**
     * 算力预估
     */
    const val POWER_CALCULATE = "trade/point/estimate/calculate"

    /**
     * 获取新手创作流程作品列表
     */
    const val GET_NEWER_EXAMPLE_WORK_LIST = "app/creative/work/list"

    /**
     * 创建小说-生成剧集剧本（首创/续集）任务
     */
    const val CREATE_TASK_FROM_NOVEL = "ai/story/script/createFromNovelFirstTask"

    /**
     * 创建小说-生成剧集剧本-续写任务
     */
    const val CONTINUE_TASK_FROM_NOVEL = "ai/story/script/createFromNovelContinueTask"

    /**
     * 创建灵感故事剧本（基于灵感写剧本/灵感续写）任务
     */
    const val CREATE_TASK_FROM_INSPIRATION = "ai/story/script/createFromIdeaFirstTask"

    /**
     * 创建灵感故事梗概生成任务
     */
    const val CREATE_SUMMARY_TASK_FROM_INSPIRATION = "ai/story/script/createSynopsisTask"

    /**
     * 重试灵感故事梗概生成任务
     */
    const val RETRY_SUMMARY_TASK_FROM_INSPIRATION = "ai/story/script/retrySynopsisTask"

    /**
     * 创建单集生成任务
     */
    const val CREATE_TASK_FROM_SINGLE_EPISODE = "ai/story/script/single/createWorkTask"

    /**
     * 单集生成任务-重试任务
     */
    const val RETRY_TASK_FROM_SINGLE_EPISODE = "ai/story/script/single/retry/createWorkTask"

    /**
     * 小说上传-剧集剧本-创建任务
     */
    const val CONTINUE_TASK_FROM_NOVEL_DOC = "ai/novel/upload/story/content/create"

    /**
     * 查询任务剧本结果
     */
    const val POLL_SCRIPT_RESULT = "ai/story/script/script/result"

    /**
     * 查询任务概要结果-通用
     */
    const val POLL_SUMMARY_RESULT = "ai/story/script/outline/result"

    /**
     * 保存剧本内容
     */
    const val SAVE_SCRIPT = "ai/story/script/saveContent"

    /**
     * 字幕-触发分镜字幕生成
     */
    const val GENERATE_SUBTITLE = "work/subtitle/generate"

    /**
     * 字幕-查询分镜字幕状态及内容
     */
    const val SUBTITLE_TASK_STATUS = "work/subtitle/query"

    /**
     * 获取新人专享礼
     */
    const val GET_FIRST_RECHARGE_DIALOG_INFO = "gift/newUser/popup/info"

    /**
     * 播放-分页查询播放历史列表
     */
    const val USER_PLAY_HISTORY_PAGE = "user/work/history/page"
    /**
     * 收藏-分页查询收藏列表
     */
    const val USER_COLLECTION_PAGE = "user/work/collection/page"

    /**
     * 首页-分页查询书架瀑布流
     */
    const val HOME_WATERFALL_PAGE = "app/bookshelf/waterfall/page"

    /**
     * 首页-书架首页聚合（横滑全部作品，和瀑布流id）
     */
    const val BOOKSHELF_HOME = "app/bookshelf/home"

    /**
     * 播放器-作品剧集目录分集分页
     */
    const val EPISODE_DIRECTORY_PAGE = "app/bookshelf/story/catalog/item/page"

    /**
     * 播放器-作品剧集目录头信息
     */
    const val EPISODE_DIRECTORY_INFO = "app/bookshelf/story/catalog/info"

    /**
     * 播放器-单集播放信息
     */
    const val EPISODE_PLAY_INFO = "app/bookshelf/story/play/detail"

    /**
     * 播放器-作品简要详情
     */
    const val SIMPLE_WORK_INFO = "app/bookshelf/work/detail"

    /**
     * 播放器-播放量累加
     */
    const val PLAY_COUNT_INC = "app/bookshelf/playCount/inc"

    /**
     * 点赞-切换点赞/取消
     */
    const val LIKE_TOGGLE = "user/work/story/like/toggle"

    /**
     * 收藏-切换收藏/取消收藏作品
     */
    const val COLLECT_TOGGLE = "user/work/collection/toggle"

    /**
     * 播放器-下一部作品推荐
     */
    const val NEXT_WORK_RECOMMEND = "user/work/next/recommend"

    /**
     * 播放器-用户状态查询
     */
    const val USER_PLAYER_STATE = "user/work/player/state"

    /**
     * 播放-播放行为上报
     */
    const val USER_PLAY_REPORT = "user/work/play/report"

    /**
     * google预下单
     */
    const val CREATE_ORDER = "trade/pay/google/createPreOrder"

    /**
     * 验证票据
     */
    const val VERIFY_PURCHASED = "trade/pay/google/payCallBack"

    /**
     * 首页-搜索作品
     */
    const val HOME_SEARCH = "app/bookshelf/work/search"

    // ─── 解锁 ────────────────────────────────────────────────────────────────

    /**
     * 播放器-剧集自动解锁（预加载时调用，余额充足则静默扣费）
     */
    const val STORY_UNLOCK_PRELOAD = "app/story/unlock/preload"

    /**
     * 解锁弹窗-章节解锁信息
     */
    const val STORY_UNLOCK_INFO = "app/story/unlock/info"

    /**
     * 解锁弹窗-金币解锁
     */
    const val STORY_UNLOCK_COINS = "app/story/unlock/coins"

    /**
     * 解锁弹窗-广告前置准备（返回 customData / nonce）
     */
    const val STORY_UNLOCK_AD_PREPARE = "app/story/unlock/ad/prepare"

    /**
     * 解锁弹窗-广告解锁结果轮询
     */
    const val STORY_UNLOCK_AD_RESULT = "app/story/unlock/ad/result"

    /**
     * 签到任务列表
     */
    const val REWARD_CHECKIN_LIST = "reward/task/checkin/list"

    /**
     * 签到上报
     */
    const val REWARD_CHECKIN_REPORT = "reward/task/checkin/report"

    /**
     * 观看广告任务列表
     */
    const val REWARD_AD_TASK_LIST = "reward/task/adTask/list"

    /**
     * 广告观看前置上报
     */
    const val REWARD_AD_PRE_REPORT = "reward/task/ad/preReport"
}