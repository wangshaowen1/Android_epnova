package com.kuaigeglobal.epnova.feature.member.vm

import android.graphics.Color
import com.kuaigeglobal.epnova.base.viewmodel.AppRepositoryViewModel
import com.kuaigeglobal.epnova.base.viewmodel.ToolbarViewModel
import com.kuaigeglobal.epnova.core.AppRepository

/** 
 * @createtime 2026年08月17日 09:51
 * @author TooWhiteT
 */

class MemberIndexViewModel : AppRepositoryViewModel<AppRepository>() {
    val mToolbarVM = ToolbarViewModel(
        navigationAction = { finish() },
        title = "",
        background = Color.TRANSPARENT,
    )


}