package com.kuaigeglobal.epnova.common.rv.recyclerholder

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView

/**
 * Created by lu on 2017/8/7.
 */
interface ContentViewProvider<DATA, VH : RecyclerView.ViewHolder> {
    fun onCreateView(inflater: LayoutInflater, parent: ViewGroup): View

    fun bind(holder: VH, item: DATA)
}
