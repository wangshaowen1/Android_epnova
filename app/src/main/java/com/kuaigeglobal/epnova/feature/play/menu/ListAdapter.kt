package com.kuaigeglobal.epnova.feature.play.menu

import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.recyclerview.widget.RecyclerView
import com.kuaigeglobal.epnova.R
import com.nhsd.basic.ext.getResColor
import com.nhsd.basic.utils.Logger

/**
 * *****************************************
 * 项目名称: KidsTales
 * @Author: SZYMN
 * @Introduce:
 * @Data: 2025/4/23  13:56
 * @Version: 1.0
 * *****************************************
 */
class ListAdapter<T : SelectableItem>(
    private var dataList: List<T>,
    private var selectedItemId: String?,
    private var normalBackground: Drawable?,
    private var selectedBackground: Drawable?,
    private var normalBackgroundColor: Int? = R.color.white.getResColor(),
    private var selectedBackgroundColor: Int? = R.color.color_20_ff9932.getResColor(),
    private var normalColor: Int?,
    private var selectedColor: Int?,
    private val isSelectBold: Boolean = true
) : RecyclerView.Adapter<ListAdapter.ViewHolder>() {

    private var itemClickListener: ((T, Int) -> Unit)? = null

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        // 这里假设列表项布局中有一个TextView用于显示文本
        // 实际使用时可以根据需要修改
        val textView: AppCompatTextView = itemView.findViewById(R.id.text_view)
        val imageView: ImageView = itemView.findViewById(R.id.image_view)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.list_item_layout, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = dataList[position]
        holder.textView.text = item.getDisplayText()
        item.getImageResId()?.apply {
            holder.imageView.setImageResource(this)
        }

        // 设置背景
        if (item.getItemId() == selectedItemId) {
            selectedBackgroundColor?.let { holder.itemView.setBackgroundColor(it) }
            selectedBackground?.let { holder.itemView.background = it }
            selectedColor?.let { holder.textView.setTextColor(it) }
            if (isSelectBold) {
                holder.textView.setTypeface(null, Typeface.BOLD)
            }
        } else {
            normalBackgroundColor?.let { holder.itemView.setBackgroundColor(it) }
            normalBackground?.let { holder.itemView.background = it }
            normalColor?.let { holder.textView.setTextColor(it) }
            if (isSelectBold) {
                holder.textView.setTypeface(null, Typeface.NORMAL)
            }
        }

        // 设置点击事件
        holder.itemView.setOnClickListener {
            selectedItemId = item.getItemId()
            // 刷新列表以更新选中状态
            notifyDataSetChanged()
            itemClickListener?.invoke(item, position)
        }
    }

    override fun getItemCount(): Int = dataList.size

    fun setOnItemClickListener(listener: (T, Int) -> Unit) {
        this.itemClickListener = listener
    }

    fun setBackgrounds(normalBg: Drawable?, selectedBg: Drawable?) {
        this.normalBackground = normalBg
        this.selectedBackground = selectedBg
        notifyDataSetChanged()
    }

    fun setItemBackgroundsColor(normalBgColor: Int?, selectedBgColor: Int?) {
        this.normalBackgroundColor = normalBgColor
        this.selectedBackgroundColor = selectedBgColor
        notifyDataSetChanged()
    }

    fun updateData(newData: List<T>, newSelectedItemId: String? = null) {
        this.dataList = newData
        this.selectedItemId = newSelectedItemId
        notifyDataSetChanged()
    }
}