package com.kuaigeglobal.epnova.feature.web

import android.graphics.Color
import androidx.lifecycle.MutableLiveData
import com.kuaigeglobal.epnova.R
import com.kuaigeglobal.epnova.base.viewmodel.AppRepositoryViewModel
import com.kuaigeglobal.epnova.base.viewmodel.ToolbarViewModel
import com.kuaigeglobal.epnova.common.Constants
import com.kuaigeglobal.epnova.core.model.H5PayInfo
import com.kuaigeglobal.epnova.core.model.H5PayStatusInfo
import com.kuaigeglobal.epnova.feature.web.data.BrowserRepository
import com.nhsd.basic.ext.getDrawable
import com.nhsd.basic.ext.getResColor

class BrowserViewModel : AppRepositoryViewModel<BrowserRepository>() {

    val title by lazy { mBundle.getString(Constants.Extra.TITLE) }
    val url by lazy { mBundle.getString(Constants.Extra.URL) }
    val interceptBack by lazy { mBundle.getBoolean(Constants.Extra.INTERCEPT_BACK) }
    val darkStyle by lazy { mBundle.getBoolean(Constants.Extra.DARK_STYLE) }
    val isLoading = MutableLiveData(false)
    val isLoadError = MutableLiveData(false)

    var orderNo: String? = null

    val mToolbarVM by lazy {
        ToolbarViewModel(
            navigationAction = { back() },
            navigationDrawable = if (darkStyle) R.drawable.ic_nav_back.getDrawable() else R.drawable.ic_nav_back_black.getDrawable(),
            title = title,
            titleColor = if (darkStyle) R.color.text_color_1_dark.getResColor() else R.color.color_121212.getResColor(),
            background = if (darkStyle) Color.TRANSPARENT else Color.WHITE
        )
    }
    val openWebUrl = MutableLiveData<String?>(null)
    val payStatusCallback = MutableLiveData<H5PayStatusInfo?>(null)
    fun startPay(info: H5PayInfo) {

    }
}