package com.kuaigeglobal.epnova.feature.play.menu

/**
 * *****************************************
 * 项目名称: KidsTales
 * @Author: SZYMN
 * @Introduce:
 * @Data: 2025/4/23  13:56
 * @Version: 1.0
 * *****************************************
 */
interface OnItemSelectedListener<T : SelectableItem> {
    fun onItemSelected(selectedItem: T, position: Int)
}