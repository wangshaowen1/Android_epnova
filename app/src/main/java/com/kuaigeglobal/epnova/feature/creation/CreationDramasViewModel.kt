package com.kuaigeglobal.epnova.feature.creation

import androidx.databinding.ObservableFloat
import androidx.lifecycle.MutableLiveData
import com.kuaigeglobal.epnova.base.viewmodel.AppRepositoryViewModel
import com.kuaigeglobal.epnova.core.AppRepository

/** 
 * @createtime 2026年07月10日 14:36
 * @author TooWhiteT
 */

class CreationDramasViewModel : AppRepositoryViewModel<AppRepository>() {

    val tabIndicatorPosition by lazy { ObservableFloat(0f) }
    val selectedTab by lazy { MutableLiveData<Int>() }

}