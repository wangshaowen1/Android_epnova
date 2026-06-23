package com.kuaigeglobal.epnova.feature.play.menu

import android.content.Context
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupWindow
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.kuaigeglobal.epnova.R
import com.nhsd.basic.ext.dp

/**
 * *****************************************
 * 项目名称: KidsTales
 * @Author: SZYMN
 * @Introduce:
 * @Data: 2025/4/23  13:56
 * @Version: 1.0
 * *****************************************
 */
class ListPopupWindow<T : SelectableItem>(
    context: Context,
    private var dataList: List<T>,
    private var selectedItemId: String? = null,
) : PopupWindow() {

    private var normalBackground: Drawable? = null
    private var selectedBackground: Drawable? = null
    private var normalBackgroundColor: Int? = null
    private var selectedBackgroundColor: Int? = null
    private var normalColor: Int? = null
    private var selectedColor: Int? = null
    private var isSelectBold: Boolean = true
    private var itemSelectedListener: OnItemSelectedListener<T>? = null
    private var adapter: ListAdapter<T>? = null

    private var recyclerView: RecyclerView

    init {
        // 设置PopupWindow基本属性
        width = 120.dp
        height = ViewGroup.LayoutParams.WRAP_CONTENT
        isFocusable = true
        isOutsideTouchable = true
        // 添加这行来设置带阴影的背景
//        setBackgroundDrawable(ContextCompat.getDrawable(context, R.drawable.popup_background_with_shadow))
        setBackgroundDrawable(ColorDrawable())

        // 创建内容视图
        val contentView = LayoutInflater.from(context).inflate(R.layout.popup_list_layout, null,false)
        contentView.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED)
        this.contentView = contentView

        // 初始化RecyclerView
        recyclerView = contentView.findViewById<RecyclerView>(R.id.recycler_view)
        recyclerView.layoutManager = LinearLayoutManager(context)
    }

    fun setAdapter() {
        adapter = ListAdapter(
            dataList = dataList,
            selectedItemId = selectedItemId,
            normalBackground = normalBackground,
            selectedBackground = selectedBackground,
            normalBackgroundColor = normalBackgroundColor,
            selectedBackgroundColor = selectedBackgroundColor,
            normalColor = normalColor,
            selectedColor = selectedColor,
            isSelectBold = isSelectBold
        )
        adapter?.setOnItemClickListener { item, position ->
            itemSelectedListener?.onItemSelected(item, position)
            dismiss()
        }
        recyclerView.adapter = adapter
    }


    fun setStatus(
        normalBackground: Drawable? = null,
        selectedBackground: Drawable? = null,
        normalBackgroundColor: Int? = null,
        selectedBackgroundColor: Int? = null,
        normalColor: Int? = null,
        selectedColor: Int? = null,
        isSelectBold: Boolean = true
    ) {
        this.normalBackground = normalBackground
        this.selectedBackground = selectedBackground
        this.normalBackgroundColor = normalBackgroundColor
        this.selectedBackgroundColor = selectedBackgroundColor
        this.normalColor = normalColor
        this.selectedColor = selectedColor
        this.isSelectBold = isSelectBold
    }


    // 设置背景样式
    fun setItemBackgrounds(normalBg: Drawable?, selectedBg: Drawable?) {
        this.normalBackground = normalBg
        this.selectedBackground = selectedBg
        adapter?.setBackgrounds(normalBg, selectedBg)
    }

    // 设置背景样式
    fun setItemBackgrounds(normalBgColor: Int?, selectedBgColor: Int?) {
        this.normalBackgroundColor = normalBgColor
        this.selectedBackgroundColor = selectedBgColor
        adapter?.setItemBackgroundsColor(normalBgColor, selectedBgColor)
    }

    // 设置选中项监听器
    fun setOnItemSelectedListener(listener: OnItemSelectedListener<T>) {
        this.itemSelectedListener = listener
    }

    // 更新数据
    fun updateData(newData: List<T>, newSelectedItemId: String? = null) {
        this.dataList = newData
        this.selectedItemId = newSelectedItemId
        adapter?.updateData(newData, newSelectedItemId)
    }
}