package com.github.kaygenzo.bugreporter

import android.graphics.Color
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.github.kaygenzo.bugreporter.views.FieldItemView
import kotlinx.android.synthetic.main.view_item_field.view.*

enum class FieldType {
    DATE_TIME,
    MANUFACTURER,
    BRAND,
    MODEL,
    APP_VERSION,
    ANDROID_VERSION,
    LOCALE,
    SCREEN_DENSITY,
    SCREEN_RESOLUTION,
    ORIENTATION,
    BATTERY_STATUS,
    BT_STATUS,
    WIFI_STATUS,
    NETWORK_STATUS
}

internal data class FieldItem(val type: FieldType, val label: String, val text: String, var enabled: Boolean = true, val visible: Boolean = true)

internal class FieldAdapter(private val items: List<FieldItem>): RecyclerView.Adapter<FieldAdapter.FieldViewHolder>() {

    inner class FieldViewHolder(val fieldView: FieldItemView): RecyclerView.ViewHolder(fieldView)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FieldViewHolder {
        return FieldViewHolder(FieldItemView(parent.context))
    }

    override fun onBindViewHolder(holder: FieldViewHolder, position: Int) {
        val item = items[position]
        holder.fieldView.fieldText.text = item.text
        holder.fieldView.fieldLabel.text = item.label
        holder.fieldView.fieldSwitch.isChecked = item.enabled
        if(item.visible) {
            holder.fieldView.fieldSwitch.visibility =  View.VISIBLE
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