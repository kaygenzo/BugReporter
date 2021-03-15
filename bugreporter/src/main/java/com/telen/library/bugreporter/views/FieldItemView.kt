package com.telen.library.bugreporter.views

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.constraintlayout.widget.ConstraintLayout
import com.telen.library.bugreporter.R
import kotlinx.android.synthetic.main.view_item_field.view.*

class FieldItemView: ConstraintLayout {

    private var label = ""

    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) { initView(context, attrs) }
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr) { initView(context, attrs, defStyleAttr) }

    private fun initView(context: Context, attrs: AttributeSet?, defStyleAttr: Int = 0) {
        attrs?.let {
            val typedArray = context.theme.obtainStyledAttributes(it, R.styleable.FieldItemView, defStyleAttr, 0)
            try {
                label = typedArray.getString(R.styleable.FieldItemView_label) ?: ""
            } finally {
                typedArray.recycle()
            }
        }
        setLabel(label)
    }

    init {
        LayoutInflater.from(context).inflate(R.layout.view_item_field, this)
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
    }

    override fun isEnabled(): Boolean {
        return fieldSwitch.isChecked
    }

    fun setText(text: String) {
        fieldText.text = text
    }

    fun getLabel(): String {
        return fieldLabel.text.toString()
    }

    fun setLabel(label: String) {
        fieldLabel.text = label
    }
}