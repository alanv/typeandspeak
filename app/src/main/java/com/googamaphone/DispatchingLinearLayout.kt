package com.googamaphone

import android.content.Context
import android.util.AttributeSet
import android.view.KeyEvent
import android.widget.LinearLayout

class DispatchingLinearLayout : LinearLayout {

    private var onKeyListener: OnKeyListener? = null

    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)

    override fun dispatchKeyEventPreIme(event: KeyEvent): Boolean {
        return if (onKeyListener != null && onKeyListener!!.onKey(this, event.keyCode, event)) {
            true
        } else super.dispatchKeyEvent(event)

    }

    override fun setOnKeyListener(listener: OnKeyListener) {
        onKeyListener = listener
    }
}