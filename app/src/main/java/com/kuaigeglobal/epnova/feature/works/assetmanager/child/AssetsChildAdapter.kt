package com.kuaigeglobal.epnova.feature.works.assetmanager.child

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding
import com.kuaigeglobal.epnova.common.ext.getVm
import com.kuaigeglobal.epnova.core.network.apis.entity.AssetInfo
import com.kuaigeglobal.epnova.databinding.ItemAssetPropBinding
import com.kuaigeglobal.epnova.databinding.ItemAssetRoleBinding
import com.kuaigeglobal.epnova.databinding.ItemAssetSceneBinding
import com.nhsd.basic.item.BaseItemEventsViewModel
import com.nhsd.basic.item.BindingViewHolder

class AssetsChildAdapter(
    val model: Boolean = false,
    val itemClickVm: BaseItemEventsViewModel<AssetsEvent, AssetInfo>,
    val selectedImageId: String? = null
) :
    PagingDataAdapter<AssetInfo, RecyclerView.ViewHolder>(ITEM_DIFF_CALLBACK) {

    companion object {
        // 定义三种布局类型常量
        private const val TYPE_ROLE = 1
        private const val TYPE_SCENE = 2
        private const val TYPE_PROP = 3

        private val ITEM_DIFF_CALLBACK = object : DiffUtil.ItemCallback<AssetInfo>() {
            override fun areItemsTheSame(oldItem: AssetInfo, newItem: AssetInfo): Boolean {
                return oldItem.assetImageId == newItem.assetImageId
            }

            override fun areContentsTheSame(oldItem: AssetInfo, newItem: AssetInfo): Boolean {
                return oldItem.assetId == newItem.assetId
                        && oldItem.assetImageId == newItem.assetImageId
                        && oldItem.assetName == newItem.assetName
                        && oldItem.assetImage == newItem.assetImage
                        && oldItem.assetDesc == newItem.assetDesc
                        && oldItem.status == newItem.status
                        && oldItem.showReference == newItem.showReference
                        && oldItem.assetTaskId == newItem.assetTaskId
            }
        }
    }

    // 1. 根据数据类型返回不同的 ViewType
    override fun getItemViewType(position: Int): Int {
        return when (getItem(position)?.assetType) {
            AssetInfo.TYPE_ROLE -> TYPE_ROLE
            AssetInfo.TYPE_SCENE -> TYPE_SCENE
            else -> TYPE_PROP
        }
    }

    // 2. 根据 ViewType 加载对应的 Binding
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            TYPE_ROLE -> RoleViewHolder(ItemAssetRoleBinding.inflate(inflater, parent, false))
            TYPE_SCENE -> SceneViewHolder(ItemAssetSceneBinding.inflate(inflater, parent, false))
            else -> PropViewHolder(ItemAssetPropBinding.inflate(inflater, parent, false))
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        // 统一调用自定义的 bind
        if (holder is BaseAssetViewHolder<*>) {
            holder.bind(position)
        }
    }

    abstract class BaseAssetViewHolder<VB : ViewBinding>(binding: VB) :
        BindingViewHolder<VB>(binding)

    inner class RoleViewHolder(binding: ItemAssetRoleBinding) : BaseAssetViewHolder<ItemAssetRoleBinding>(binding) {
        override fun bind(position: Int) {
            getItem(position)?.let { item ->
                // 使用 getVm 维护状态
                val vm = binding.getVm<ItemAssetsChildVm, AssetInfo>(key = item.assetImageId, item, {
                    ItemAssetsChildVm(item, model, itemClickVm).apply {
                        if (model && selectedImageId == item.assetImageId) {
                            selectModel.set(true)
                            itemClickVm.postEvent(AssetsEvent.Select(this))
                        }
                    }
                }, {})
                vm.updateData(item)
                binding.item = vm
                binding.executePendingBindings()
            }
        }
    }

    inner class SceneViewHolder(binding: ItemAssetSceneBinding) : BaseAssetViewHolder<ItemAssetSceneBinding>(binding) {
        override fun bind(position: Int) {
            getItem(position)?.let { item ->
                val vm = binding.getVm<ItemAssetsChildVm, AssetInfo>(key = item.assetImageId, item, viewModelFactory = {
                    ItemAssetsChildVm(item, model, itemClickVm).apply {
                        if (model && selectedImageId == item.assetImageId) {
                            selectModel.set(true)
                            itemClickVm.postEvent(AssetsEvent.Select(this))
                        }
                    }
                }, {})
                vm.updateData(item)
                binding.item = vm
                binding.executePendingBindings()
            }
        }
    }

    inner class PropViewHolder(binding: ItemAssetPropBinding) : BaseAssetViewHolder<ItemAssetPropBinding>(binding) {
        override fun bind(position: Int) {
            getItem(position)?.let { item ->
                val vm = binding.getVm<ItemAssetsChildVm, AssetInfo>(key = item.assetImageId, item, viewModelFactory = {
                    ItemAssetsChildVm(item, model, itemClickVm).apply {
                        if (model && selectedImageId == item.assetImageId) {
                            selectModel.set(true)
                            itemClickVm.postEvent(AssetsEvent.Select(this))
                        }
                    }
                }, {})
                vm.updateData(item)
                binding.item = vm
                binding.executePendingBindings()
            }
        }
    }
}
