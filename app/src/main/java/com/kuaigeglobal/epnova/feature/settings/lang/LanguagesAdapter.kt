package com.kuaigeglobal.epnova.feature.settings.lang

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.kuaigeglobal.epnova.R

class LanguagesAdapter(
    private val items: List<LanguageItem>,
    private var selectedCode: String,
    private val onSelect: (LanguageItem) -> Unit
) : RecyclerView.Adapter<LanguagesAdapter.ViewHolder>() {

    fun updateSelected(code: String) {
        selectedCode = code
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_language, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.tvName.text = item.name
        holder.ivCheck.visibility = if (item.code == selectedCode) View.VISIBLE else View.GONE
        holder.divider.visibility = if (position == items.size - 1) View.GONE else View.VISIBLE
        holder.itemView.setOnClickListener { onSelect(item) }
    }

    override fun getItemCount() = items.size

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvName: TextView = view.findViewById(R.id.tvName)
        val ivCheck: ImageView = view.findViewById(R.id.ivCheck)
        val divider: View = view.findViewById(R.id.divider)
    }
}
