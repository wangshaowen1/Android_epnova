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
     * 广告位 ID 可由业务接口下发，未下发时由各平台实现类使用本地默认值。
     *
     * @param context    上下文
     * @param adUnitId   后台下发的广告单元 ID，空时使用本地默认广告位
     * @param customData 写入广告 SDK SSV 的自定义数据，当前 AdMob 使用 prepare 返回的 nonce
     * @param onLoaded   加载成功回调（主线程）
     * @param onFailed   加载失败回调（主线程）
     */
    fun load(
        context: Context,
        adUnitId: String?,
        customData: String?,
        onLoaded: () -> Unit,
        onFailed: () -> Unit
    )

    /**
     * 展示已加载好的广告，需在 [onLoaded] 回调后调用
     *
     * @param activity    宿主 Activity
     * @param onRewarded  用户完整观看广告并关闭广告页后回调
     * @param onDismissed 广告正常关闭时回调，参数表示用户是否已获得奖励
     * @param onFailed    展示失败时回调
     */
    fun show(
        activity: Activity,
        onRewarded: () -> Unit,
        onDismissed: (rewardEarned: Boolean) -> Unit,
        onFailed: () -> Unit
    )

    /**
     * 释放广告资源，在 ViewModel.onDestroy() 中调用
     */
    fun release()
}
