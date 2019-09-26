package com.googamaphone

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.Checkable
import android.widget.LinearLayout

class CheckableLinearLayout : LinearLayout, Checkable {

    private var checked: Boolean = false

    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)

    override fun isChecked(): Boolean {
        return checked
    }

    override fun setChecked(checked: Boolean) {
        this.checked = checked
        refreshDrawableState()
    }

    override fun toggle() {
        isChecked = !checked
    }

    override fun onCreateDrawableState(extraSpace: Int): IntArray {
        val drawableState = super.onCreateDrawableState(extraSpace + 1)
        if (isChecked) {
            View.mergeDrawableStates(drawableState, CHECKED_STATE_SET)
        }
        return drawableState
    }

    override fun drawableStateChanged() {
        super.drawableStateChanged()

        if (background != null) {
            val myDrawableState = drawableState

            // Set the state of the Drawable
            background.state = myDrawableState

            invalidate()
        }
    }

    companion object {
        private val CHECKED_STATE_SET = intArrayOf(android.R.attr.state_checked)
    }
}
