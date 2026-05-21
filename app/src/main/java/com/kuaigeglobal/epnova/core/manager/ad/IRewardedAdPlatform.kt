package com.kuaigeglobal.epnova.core.manager.ad

import android.app.Activity
import android.content.Context

/**
 * @Introduce: 激励广告平台抽象接口
 *
 * 业务代码只依赖此接口，不直接引用任何广告 SDK。
 * 切换广告平台时，只需新增对应实现类并修改 [RewardedAdManager] 的默认参数。
 */
interface IRewardedAdPlatform {

    /**
     * 加载广告
     *
     * 广告位 ID 由各平台实现类自行持有，Manager 无需关心具体格式。
     *
     * @param context    上下文
     * @param customData SSV 自定义校验数据，null 表示不需要服务端验证
     * @param onLoaded   加载成功回调（主线程）
     * @param onFailed   加载失败回调（主线程）
     */
    fun load(
        context: Context,
        customData: String?,
        adMarkup: String? = null,
        objectId: String? = null,
        onLoaded: () -> Unit,
        onFailed: () -> Unit
    )

    /**
     * 展示已加载好的广告，需在 [onLoaded] 回调后调用
     *
     * @param activity    宿主 Activity
     * @param onRewarded  用户完整观看广告、获得奖励时回调
     * @param onDismissed 广告正常关闭时回调（含看完关闭）
     * @param onFailed    展示失败时回调
     */
    fun show(
        activity: Activity,
        onRewarded: () -> Unit,
        onDismissed: () -> Unit,
        onFailed: () -> Unit
    )

    /**
     * 释放广告资源，在 ViewModel.onCleared() 中调用
     */
    fun release()
}
