package com.kuaigeglobal.epnova.feature.creator.assets.edit

import com.kuaigeglobal.epnova.R
import com.kuaigeglobal.epnova.base.RecyclerViewVM
import com.kuaigeglobal.epnova.base.viewmodel.AppRepositoryViewModel
import com.kuaigeglobal.epnova.core.AppRepository
import com.kuaigeglobal.epnova.core.ext.request
import com.kuaigeglobal.epnova.core.network.apis.entity.ReferenceImageInfo
import com.kuaigeglobal.epnova.core.network.apis.request.ReferenceImageBody

/**
 * *****************************************
 * @Introduce:创作
 * *****************************************
 */
class ReferenceHistoricalImageryViewModel : AppRepositoryViewModel<AppRepository>() {
    var checkedVm: ItemReferenceImageVm? = null

    val imagesVms = arrayListOf<ItemReferenceImageVm>()

    //资产id
    private val assetId by lazy { mBundle.getString(ReferenceHistoricalImageryDialog.ASSET_ID) }

    //剧集id
    private val dramaID by lazy { mBundle.getString(ReferenceHistoricalImageryDialog.DRAMA_ID) }

    //作品id
    private val workId by lazy { mBundle.getString(ReferenceHistoricalImageryDialog.WORK_ID) }

    val mAdapterVm =
        RecyclerViewVM().Builder()
            .setQuickAdapter(R.layout.item_reference_historical_imagery, imagesVms).build()

    fun initVm(info: ReferenceImageInfo): ItemReferenceImageVm {
        return ItemReferenceImageVm(info).apply {
            if (checkedVm == null) {
                checked.set(true)
                checkedVm = this
            }
            onItemClick.value = {
                if (checkedVm != this) {
                    checkedVm?.checked?.set(false)
                    this.checked.set(true)
                    checkedVm = this
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        getReferenceHistoricalImagery()
    }


    fun getReferenceHistoricalImagery() {
        request({
            mRepo.api.workAssetReferenceImageList(
                ReferenceImageBody(
                    workId = workId,
                    storyId = dramaID,
                    assetId = assetId
                )
            )
        }) {
            data?.forEach {
                imagesVms.add(initVm(it))
            }
            mAdapterVm.notifyDataSetChanged()
        }
    }


}
