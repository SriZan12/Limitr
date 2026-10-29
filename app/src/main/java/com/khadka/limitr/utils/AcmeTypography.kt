package com.khadka.limitr.utils

import android.content.Context
import android.graphics.Typeface
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.TextView
import androidx.annotation.Keep
import androidx.appcompat.app.AppCompatViewInflater
import androidx.appcompat.widget.AppCompatEditText
import androidx.core.content.res.ResourcesCompat
import com.khadka.limitr.R

/** Bundled typography, including internal dialog/menu/picker text on API 23. */
object AcmeTypography {
    fun <T : TextView> apply(view: T): T = view.apply {
        setTypeface(ResourcesCompat.getFont(context, R.font.acme), typeface?.style ?: Typeface.NORMAL)
    }

    fun applyTree(view: View) {
        if (view is TextView) apply(view)
        if (view is ViewGroup) for (index in 0 until view.childCount) applyTree(view.getChildAt(index))
    }
}

// Instantiated by AppCompat from the theme's viewInflaterClass, including in R8 builds.
@Keep
class AcmeViewInflater : AppCompatViewInflater() {
    override fun createView(context: Context, name: String, attrs: AttributeSet): View? =
        if (name == "android.widget.NumberPicker\$CustomEditText") AcmeNumberInput(context, attrs)
        else super.createView(context, name, attrs)

    override fun createTextView(c: Context, a: AttributeSet) = AcmeTypography.apply(super.createTextView(c, a))
    override fun createButton(c: Context, a: AttributeSet) = AcmeTypography.apply(super.createButton(c, a))
    override fun createEditText(c: Context, a: AttributeSet) = AcmeTypography.apply(super.createEditText(c, a))
    override fun createCheckedTextView(c: Context, a: AttributeSet) = AcmeTypography.apply(super.createCheckedTextView(c, a))
    override fun createCheckBox(c: Context, a: AttributeSet) = AcmeTypography.apply(super.createCheckBox(c, a))
    override fun createRadioButton(c: Context, a: AttributeSet) = AcmeTypography.apply(super.createRadioButton(c, a))
    override fun createToggleButton(c: Context, a: AttributeSet) = AcmeTypography.apply(super.createToggleButton(c, a))
    override fun createAutoCompleteTextView(c: Context, a: AttributeSet) = AcmeTypography.apply(super.createAutoCompleteTextView(c, a))
    override fun createMultiAutoCompleteTextView(c: Context, a: AttributeSet) = AcmeTypography.apply(super.createMultiAutoCompleteTextView(c, a))
}

/** NumberPicker copies this input's font into its wheel paint during construction. */
private class AcmeNumberInput(context: Context, attrs: AttributeSet) : AppCompatEditText(context, attrs) {
    init { AcmeTypography.apply(this) }
    override fun onEditorAction(actionCode: Int) {
        super.onEditorAction(actionCode)
        // Preserve the platform NumberPicker input's commit-on-Done behavior.
        if (actionCode == EditorInfo.IME_ACTION_DONE) clearFocus()
    }
}
