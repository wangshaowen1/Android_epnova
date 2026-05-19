package com.kuaigeglobal.epnova.feature.creator.set

import com.kuaigeglobal.epnova.R
import com.kuaigeglobal.epnova.base.RecyclerViewVM
import com.kuaigeglobal.epnova.base.viewmodel.AppRepositoryViewModel
import com.kuaigeglobal.epnova.common.Constants
import com.kuaigeglobal.epnova.core.AppRepository
import com.kuaigeglobal.epnova.core.network.apis.entity.CreationModelInfo
import com.nhsd.basic.ext.getParcelableArrayListValue

class ModelChooseViewModel : AppRepositoryViewModel<AppRepository>() {

    val list by lazy { mBundle.getParcelableArrayListValue(Constants.Extra.DATA, CreationModelInfo::class.java) }
    val selectedCode by lazy { mBundle.getString(Constants.Extra.ID) }

    var checkedModel: ItemModelVm? = null
    val modelVms = arrayListOf<ItemModelVm>()

    var onModelSelected: ((CreationModelInfo?) -> Unit)? = null

    val mModelAdapterVm = RecyclerViewVM().Builder()
        .setQuickAdapter(R.layout.item_model_choose, modelVms)
        .build()

    override fun onCreate() {
        super.onCreate()
        list?.forEach { info ->
            modelVms.add(ItemModelVm(info).apply {
                if (selectedCode != null && info.configCode == selectedCode) {
                    checked.set(true)
                    checkedModel = this
                } else if (selectedCode == null && checkedModel == null && info.isValid()) {
                    checked.set(true)
                    checkedModel = this
                }
                onItemClick.value = {
                    if (checkedModel != this) {
                        checkedModel?.checked?.set(false)
                        this.checked.set(true)
                        checkedModel = this
                    }
                    onModelSelected?.invoke(data.get())
                }
            })
        }
        mModelAdapterVm.notifyDataSetChanged()
    }
}
