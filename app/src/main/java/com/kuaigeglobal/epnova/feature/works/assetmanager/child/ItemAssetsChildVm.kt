package com.kuaigeglobal.epnova.feature.works.assetmanager.child

import androidx.databinding.ObservableBoolean
import androidx.lifecycle.viewModelScope
import com.kuaigeglobal.epnova.base.item.AppItemViewModel
import com.kuaigeglobal.epnova.core.network.apis.entity.AssetInfo
import com.nhsd.basic.item.BaseItemEventsViewModel
import kotlinx.coroutines.launch

class ItemAssetsChildVm(
    data: AssetInfo?,
    val model: Boolean = false,
    val itemClickVm: BaseItemEventsViewModel<AssetsEvent, AssetInfo>
) : AppItemViewModel<AssetInfo>(data) {
    val selectModel = ObservableBoolean(false)
    val checked = ObservableBoolean(false)

    init {
        selectModel.set(model)
        if (model) {
            viewModelScope.launch {
                itemClickVm.events.collect {
                    when (it) {
                        is AssetsEvent.Click -> {
                        }

                        is AssetsEvent.Select -> {
                            checked.set(it.item?.data?.get()?.assetImageId == data?.assetImageId)
                        }
                    }
                }
            }
        }
    }

    fun onItemClick() {
        if (model) {
            itemClickVm.postEvent(AssetsEvent.Select(this))
        } else {
            itemClickVm.postEvent(AssetsEvent.Click(this))
        }
    }
}
