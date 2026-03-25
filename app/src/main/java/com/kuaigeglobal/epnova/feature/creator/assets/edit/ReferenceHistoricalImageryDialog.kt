package com.kuaigeglobal.epnova.feature.creator.assets.edit

import android.os.Bundle
import com.kuaigeglobal.epnova.core.network.apis.entity.ReferenceImageInfo
import com.kuaigeglobal.epnova.core.network.apis.request.ReferenceImageBody
import com.kuaigeglobal.epnova.databinding.DialogReferenceHistoricalImageryBinding
import com.nhsd.basic.BaseBottomPushVMDialogFragment


class ReferenceHistoricalImageryDialog :
    BaseBottomPushVMDialogFragment<DialogReferenceHistoricalImageryBinding, ReferenceHistoricalImageryViewModel>() {

    companion object {
        //资产id
        const val ASSET_ID = "assetId"

        //剧集id
        const val DRAMA_ID = "dramaID"

        //作品id
        const val WORK_ID = "workId"

        //资产类型
        const val BIZ_TYPE = "bizType"

        //角色图片
        const val BIZ_TYPE_ROLE_IMAGE = ReferenceImageBody.BIZ_TYPE_ROLE_IMAGE

        //道具图片
        const val BIZ_TYPE_PROP_IMAGE = ReferenceImageBody.BIZ_TYPE_PROP_IMAGE

        //场景图片
        const val BIZ_TYPE_SCENE_IMAGE = ReferenceImageBody.BIZ_TYPE_SCENE_IMAGE


        /**
         * 是否参考历史形象生图
         * @param assetId 资产id
         * @param dramaID 剧集id
         * @param workId 作品id
         * @param bizType 资产类型
         */
        fun newInstance(
            assetId: String? = null,
            dramaID: String? = null,
            workId: String? = null,
            bizType: Int? = BIZ_TYPE_ROLE_IMAGE
        ): ReferenceHistoricalImageryDialog {
            val fragment = ReferenceHistoricalImageryDialog()
            val builder = Bundle()
            builder.putString(ASSET_ID, assetId)
            builder.putString(DRAMA_ID, dramaID)
            builder.putString(WORK_ID, workId)
            builder.putInt(BIZ_TYPE, bizType ?: 0)
            fragment.arguments = builder
            return fragment
        }
    }

    var onUserReference: (ReferenceImageInfo?) -> Unit = {}

    override fun onPrepareView() {
        super.onPrepareView()
        binding.close.setOnClickListener { dismissAllowingStateLoss() }
        binding.btnCancel.setOnClickListener {
            onUserReference.invoke(null)
            dismissAllowingStateLoss()
        }
        binding.btnConfirm.setOnClickListener {
            onUserReference.invoke(mRealVM.checkedVm?.data?.get())
            dismissAllowingStateLoss()
        }
    }
}