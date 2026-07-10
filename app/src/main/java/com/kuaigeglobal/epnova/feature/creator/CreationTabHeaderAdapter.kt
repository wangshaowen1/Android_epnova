package com.kuaigeglobal.epnova.feature.creator

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.RecyclerView
import com.kuaigeglobal.epnova.databinding.LayoutCreationTabHeaderBinding

class CreationTabHeaderAdapter(
    private val viewModel: CreationViewModel,
    private val lifecycleOwner: LifecycleOwner,
    private val onBindingCreated: (LayoutCreationTabHeaderBinding) -> Unit
) : RecyclerView.Adapter<CreationTabHeaderAdapter.HeaderVH>() {

    var binding: LayoutCreationTabHeaderBinding? = null
        private set

    private var minHeight = 0

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HeaderVH {
        val itemBinding = LayoutCreationTabHeaderBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        ).apply {
            lifecycleOwner = this@CreationTabHeaderAdapter.lifecycleOwner
            layout = viewModel
            root.minimumHeight = minHeight
            executePendingBindings()
        }
        binding = itemBinding
        onBindingCreated(itemBinding)
        return HeaderVH(itemBinding)
    }

    override fun onBindViewHolder(holder: HeaderVH, position: Int) {
        binding = holder.binding
        holder.bind(viewModel, minHeight)
    }

    override fun getItemCount(): Int = 1

    override fun onViewRecycled(holder: HeaderVH) {
        if (binding === holder.binding) {
            binding = null
        }
        super.onViewRecycled(holder)
    }

    fun updateMinHeight(height: Int) {
        if (height <= 0 || minHeight == height) {
            return
        }
        minHeight = height
        binding?.root?.minimumHeight = height
    }

    fun clearBinding() {
        binding = null
    }

    class HeaderVH(
        val binding: LayoutCreationTabHeaderBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(viewModel: CreationViewModel, minHeight: Int) {
            binding.layout = viewModel
            binding.root.minimumHeight = minHeight
            binding.executePendingBindings()
        }
    }
}
