package com.kuaigeglobal.epnova.feature.home.search

import com.kuaigeglobal.epnova.base.item.AppItemViewModel
import com.kuaigeglobal.epnova.core.network.apis.entity.WorkVOInfo

class ItemSearchResultVm(data: WorkVOInfo?) : AppItemViewModel<WorkVOInfo>(data) {
    var keyWord: String? = null
}
