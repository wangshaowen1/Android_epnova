package com.kuaigeglobal.epnova.base.item

import com.nhsd.basic.item.BaseUpdatableItemViewModel
import com.kuaigeglobal.epnova.BR


 open class AppItemViewModel<T>(data: T?) : BaseUpdatableItemViewModel<T>(data) {
    override fun id(): Int = BR.item

}