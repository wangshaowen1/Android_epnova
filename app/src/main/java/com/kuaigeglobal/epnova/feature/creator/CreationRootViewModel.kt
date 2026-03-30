package com.kuaigeglobal.epnova.feature.creator

import android.graphics.Color
import androidx.databinding.ObservableFloat
import androidx.lifecycle.MutableLiveData
import com.kuaigeglobal.epnova.R
import com.kuaigeglobal.epnova.base.viewmodel.AppRepositoryViewModel
import com.kuaigeglobal.epnova.base.viewmodel.ToolbarViewModel
import com.kuaigeglobal.epnova.feature.creator.data.CreationRepository
import com.nhsd.basic.ext.getResColor
import com.nhsd.basic.ext.getResDrawable
import com.nhsd.basic.ext.getResString

class CreationRootViewModel : AppRepositoryViewModel<CreationRepository>() {

    val mToolbarVM = ToolbarViewModel(
        navigationAction = { back() },
        navigationDrawable = R.drawable.icon_close.getResDrawable(),
        title = R.string.story_inspiration.getResString(),
        background = Color.TRANSPARENT,
    )

    val tabIndicatorPosition by lazy { ObservableFloat(0f) }
    val selectedTab by lazy { MutableLiveData<Int>() }



}