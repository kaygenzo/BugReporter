package com.github.kaygenzo.bugreporter.internal.screens

import android.graphics.Color
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.github.kaygenzo.bugreporter.api.FieldType
import com.github.kaygenzo.bugreporter.internal.views.FieldItemView

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
        with(holder.fieldView.binding) {
            fieldText.text = item.text
            fieldLabel.text = item.label
            fieldSwitch.apply {
                setOnCheckedChangeListener(null)
                isChecked = item.enabled
                setOnCheckedChangeListener { _, isChecked ->
                    item.enabled = isChecked
                }
            }
            if (item.visible) {
                fieldSwitch.visibility = View.VISIBLE
                fieldText.setTextColor(Color.BLACK)
            } else {
                fieldSwitch.visibility = View.GONE
                fieldText.setTextColor(Color.RED)
            }
        }
    }

    override fun getItemCount(): Int {
        return items.count()
    }
}