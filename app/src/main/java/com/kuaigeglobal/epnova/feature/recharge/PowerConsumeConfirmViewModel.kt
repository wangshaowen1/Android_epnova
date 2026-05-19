package com.kuaigeglobal.epnova.feature.recharge

import androidx.databinding.ObservableBoolean
import com.kuaigeglobal.epnova.R
import com.kuaigeglobal.epnova.base.viewmodel.AppRepositoryViewModel
import com.kuaigeglobal.epnova.common.Constants
import com.kuaigeglobal.epnova.core.AppRepository
import com.kuaigeglobal.epnova.core.Config
import com.kuaigeglobal.epnova.router.AppRouter
import com.nhsd.basic.ext.getResString


class PowerConsumeConfirmViewModel : AppRepositoryViewModel<AppRepository>() {
    val scene by lazy { mBundle.getInt(Constants.Extra.TYPE) }
    val cost by lazy { mBundle.getInt(Constants.Extra.COST) }
    val pointPowerDto by lazy { Config.get().appConfig.workPointPowerPopUpDto }
    val title by lazy {
        when (scene) {
            PowerConsumeConfirmDialog.SCENE_GENERATE_ASSETS_IMAGE -> R.string.generate_asset_image.getResString()
            PowerConsumeConfirmDialog.SCENE_GENERATE_STORYBOARD_VIDEO -> R.string.generate_storyboard_video.getResString()
            PowerConsumeConfirmDialog.SCENE_REGENERATE_STORYBOARD_VIDEO -> R.string.redraw_storyboard_video.getResString()
            else -> ""
        }
    }

    /**
     * 协议是否勾选
     */
    val checkedPolicy by lazy { ObservableBoolean(true) }

    fun checkPolicy() {
        checkedPolicy.set(checkedPolicy.get().not())
    }

    fun powerService() {
        AppRouter.jump(pointPowerDto?.jumpUri)
    }
}