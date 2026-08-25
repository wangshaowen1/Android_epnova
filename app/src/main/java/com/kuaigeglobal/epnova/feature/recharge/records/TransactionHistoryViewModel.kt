package com.kuaigeglobal.epnova.feature.recharge.records

import android.graphics.Color
import com.kuaigeglobal.epnova.R
import com.kuaigeglobal.epnova.base.viewmodel.AppRepositoryViewModel
import com.kuaigeglobal.epnova.base.viewmodel.ToolbarViewModel
import com.kuaigeglobal.epnova.core.AppRepository
import com.nhsd.basic.ext.getResString

/** 
 * @createtime 2026年08月19日 10:14
 * @author TooWhiteT
 */

class TransactionHistoryViewModel : AppRepositoryViewModel<AppRepository>() {
    var mToolbarVM = ToolbarViewModel(
        navigationAction = { back() },
        title = R.string.transaction_history_title.getResString(),
        background = Color.TRANSPARENT
    )
}