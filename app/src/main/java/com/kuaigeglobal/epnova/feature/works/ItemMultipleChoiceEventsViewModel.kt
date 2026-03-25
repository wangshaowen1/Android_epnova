package com.kuaigeglobal.epnova.feature.works

import com.nhsd.basic.item.BaseItemEventsViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * *****************************************
 * 项目名称: FastFilm
 * @Author: SZYMN
 * @Introduce:
 * @Data: 2026/2/3/周二  10:13
 * @Version: 1.0
 * *****************************************
 */
class ItemMultipleChoiceEventsViewModel<T, S>() : BaseItemEventsViewModel<T, String>() {

    // 切换编辑(false)查看(true)状态
    private val _switchStatus = MutableStateFlow(true)
    val switchStatus: StateFlow<Boolean> = _switchStatus.asStateFlow()

    // 选中状态（多选）
    private val _selectedItemIds = MutableStateFlow<Set<S>>(emptySet())
    val selectedItemIds: StateFlow<Set<S>> = _selectedItemIds.asStateFlow()

    /** 获取选中的 */
    fun getSelectedItems(): Set<S> {
        return _selectedItemIds.value
    }

    /** 设置选中（覆盖） */
    fun setSelectedItems(ids: Set<S>) {
        _selectedItemIds.value = ids
    }

    /** 增加选中 */
    fun addSelectedItem(id: S) {
        _selectedItemIds.value += id
    }

    /** 移除选中 */
    fun removeSelectedItem(id: S) {
        _selectedItemIds.value -= id
    }

    /** 是否选中 */
    fun isSelected(id: S): Boolean {
        return _selectedItemIds.value.contains(id)
    }

    /** 清空 */
    fun clearAllSelection() {
        _selectedItemIds.value = emptySet()
    }

    /** 切换状态 */
    fun switchStatus(status: Boolean) {
        _switchStatus.value = status
    }
}