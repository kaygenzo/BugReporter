package com.github.kaygenzo.bugreporter.internal.screens

import android.graphics.Color
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.github.kaygenzo.bugreporter.api.FieldType
import com.github.kaygenzo.bugreporter.internal.views.FieldItemView
import kotlinx.android.synthetic.main.view_item_field.view.*

internal data class FieldItem(
    val type: FieldType,
    val label: String,
    val text: String,
    var enabled: Boolean = true,
    val visible: Boolean = true
)

internal class FieldAdapter(private val items: List<FieldItem>) :
    RecyclerView.Adapter<FieldAdapter.FieldViewHolder>() {

    inner class FieldViewHolder(val fieldView: FieldItemView) : RecyclerView.ViewHolder(fieldView)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FieldViewHolder {
        return FieldViewHolder(FieldItemView(parent.context))
    }

    override fun onBindViewHolder(holder: FieldViewHolder, position: Int) {
        val item = items[position]
        holder.fieldView.fieldText.text = item.text
        holder.fieldView.fieldLabel.text = item.label
        holder.fieldView.fieldSwitch.apply {
            setOnCheckedChangeListener(null)
            isChecked = item.enabled
            setOnCheckedChangeListener { _, isChecked ->
                item.enabled = isChecked
            }
        }
        if (item.visible) {
            holder.fieldView.fieldSwitch.visibility = View.VISIBLE
            holder.fieldView.fieldText.setTextColor(Color.BLACK)
        } else {
            holder.fieldView.fieldSwitch.visibility = View.GONE
            holder.fieldView.fieldText.setTextColor(Color.RED)
        }
    }

    override fun getItemCount(): Int {
        return items.count()
    }
}