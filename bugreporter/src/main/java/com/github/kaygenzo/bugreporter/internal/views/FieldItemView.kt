package com.github.kaygenzo.bugreporter.internal.views

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.constraintlayout.widget.ConstraintLayout
import com.github.kaygenzo.bugreporter.R
import com.github.kaygenzo.bugreporter.databinding.ViewItemFieldBinding

internal class FieldItemView : ConstraintLayout {

    private var label = ""
    val binding: ViewItemFieldBinding =
        ViewItemFieldBinding.inflate(LayoutInflater.from(context), this, true)

    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        initView(context, attrs)
    }

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    ) {
        initView(context, attrs, defStyleAttr)
    }

    private fun initView(context: Context, attrs: AttributeSet?, defStyleAttr: Int = 0) {
        attrs?.let {
            val typedArray =
                context.theme.obtainStyledAttributes(it, R.styleable.FieldItemView, defStyleAttr, 0)
            try {
                label = typedArray.getString(R.styleable.FieldItemView_label) ?: ""
            } finally {
                typedArray.recycle()
            }
        }
        setLabel(label)
    }

    init {
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
    }

    override fun isEnabled(): Boolean {
        return binding.fieldSwitch.isChecked
    }

    fun setText(text: String) {
        binding.fieldText.text = text
    }

    fun getLabel(): String {
        return binding.fieldLabel.text.toString()
    }

    fun setLabel(label: String) {
        binding.fieldLabel.text = label
    }
}