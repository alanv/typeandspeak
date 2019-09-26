package com.googamaphone

import com.googamaphone.typeandspeak.R

import android.content.Context
import android.graphics.PixelFormat
import android.graphics.Rect
import android.view.Gravity
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.View.OnKeyListener
import android.view.View.OnTouchListener
import android.view.ViewGroup
import android.view.ViewTreeObserver.OnGlobalLayoutListener
import android.view.WindowManager
import android.view.WindowManager.LayoutParams
import android.widget.FrameLayout
import android.widget.ImageView

/**
 * Creates a new simple overlay.
 *
 * @param context The parent context.
 */
class PinnedDialog(private val context: Context) {
    private val anchorRect = Rect()
    private val boundsRect = Rect()
    private val screenRect = Rect()
    private val windowManager: WindowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val windowView: ViewGroup
    private val contentView: ViewGroup
    private val tickAbove: ImageView
    private val tickBelow: ImageView
    private val tickAbovePadding: View
    private val tickBelowPadding: View
    private val layoutParams: LayoutParams
    private val onGlobalLayoutListener: OnGlobalLayoutListener

    var pinnedView: View? = null
    var visible: Boolean = false

    var params: LayoutParams
        get() {
            val copy = LayoutParams()
            copy.copyFrom(layoutParams)
            return copy
        }
        set(params) {
            layoutParams.copyFrom(params)

            if (!isVisible) {
                return
            }

            windowManager.updateViewLayout(windowView, layoutParams)
        }

    /**
     * @return `true` if this overlay is visible.
     */
    private val isVisible: Boolean
        get() = visible && windowView.isShown


    private val onKeyListener = OnKeyListener { _, keyCode, event ->
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (event.action == KeyEvent.ACTION_UP) {
                cancel()
            }
            return@OnKeyListener true
        }

        false
    }

    private val onTouchListener = OnTouchListener { v, event ->
        if (event.action == MotionEvent.ACTION_UP) {
            v.getHitRect(anchorRect)

            if (!anchorRect.contains(event.x.toInt(), event.y.toInt())) {
                cancel()
            }
        }

        false
    }

    init {
        windowView = PinnedLayout(context)
        windowView.setOnTouchListener(onTouchListener)
        windowView.setOnKeyListener(onKeyListener)

        onGlobalLayoutListener = OnGlobalLayoutListener {
            if (!isVisible) {
                // This dialog is not showing.
                return@OnGlobalLayoutListener
            }

            if (windowView.windowToken == null) {
                // This dialog was removed by the system.
                visible = false
                return@OnGlobalLayoutListener
            }

            updatePinningOffset()
        }

        LayoutInflater.from(context).inflate(R.layout.pinned_dialog, windowView)

        contentView = windowView.findViewById(R.id.content)
        tickBelow = windowView.findViewById(R.id.tick_below)
        tickAbove = windowView.findViewById(R.id.tick_above)
        tickAbovePadding = windowView.findViewById(R.id.tick_above_padding)
        tickBelowPadding = windowView.findViewById(R.id.tick_below_padding)

        layoutParams = LayoutParams()
        layoutParams.type = LayoutParams.TYPE_APPLICATION_PANEL
        layoutParams.format = PixelFormat.TRANSLUCENT
        layoutParams.flags = layoutParams.flags or LayoutParams.FLAG_ALT_FOCUSABLE_IM
        layoutParams.width = LayoutParams.WRAP_CONTENT
        layoutParams.height = LayoutParams.WRAP_CONTENT
        layoutParams.windowAnimations = R.style.fade_dialog

        visible = false
    }

    private fun cancel() {
        dismiss()
    }

    /**
     * Shows the overlay.
     */
    fun show(pinnedView: View) {
        if (isVisible) {
            return
        }

        this.pinnedView = pinnedView

        windowManager.addView(windowView, layoutParams)
        visible = true

        val observer = pinnedView.viewTreeObserver
        observer.addOnGlobalLayoutListener(onGlobalLayoutListener)
    }

    /**
     * Hides the overlay.
     */
    fun dismiss() {
        if (!isVisible) {
            return
        }

        windowManager.removeView(windowView)
        visible = false

        val observer = pinnedView!!.viewTreeObserver
        observer.removeGlobalOnLayoutListener(onGlobalLayoutListener)
    }

    /**
     * Finds and returns the view within the overlay content.
     *
     * @param id The ID of the view to return.
     * @return The view with the specified ID, or `null` if not found.
     */
    fun findViewById(id: Int): View {
        return windowView.findViewById(id)
    }

    /**
     * Inflates the specified resource ID and sets it as the content view.
     *
     * @param layoutResId The layout ID of the view to set as the content view.
     */
    fun setContentView(layoutResId: Int): PinnedDialog {
        val inflater = LayoutInflater.from(context)
        inflater.inflate(layoutResId, contentView)
        return this
    }

    private fun updatePinningOffset() {
        val width = windowView.width
        val height = windowView.height
        val rootView = pinnedView!!.rootView
        val parentContent = rootView.findViewById<View>(android.R.id.content)

        rootView.getGlobalVisibleRect(screenRect)
        parentContent.getGlobalVisibleRect(boundsRect)
        pinnedView!!.getGlobalVisibleRect(anchorRect)

        when {
            anchorRect.bottom + height <= boundsRect.bottom -> {
                // Place below.
                params.y = anchorRect.bottom
                tickBelow.visibility = View.VISIBLE
                tickAbove.visibility = View.GONE
            }
            anchorRect.top - height >= screenRect.top -> {
                // Place above.
                params.y = anchorRect.top - height
                tickBelow.visibility = View.GONE
                tickAbove.visibility = View.VISIBLE
            }
            else -> {
                // Center on screen.
                params.y = screenRect.centerY() - height / 2
                tickBelow.visibility = View.GONE
                tickAbove.visibility = View.GONE
            }
        }

        // First, attempt to center on the pinned view.
        params.x = anchorRect.centerX() - width / 2

        if (params.x < boundsRect.left) {
            // Align to left of parent.
            params.x = boundsRect.left
        } else if (params.x + width > boundsRect.right) {
            // Align to right of parent.
            params.x = boundsRect.right - width
        }

        val tickLeft = anchorRect.centerX() - params.x - tickAbove.width / 2
        tickAbovePadding.layoutParams.width = tickLeft
        tickBelowPadding.layoutParams.width = tickLeft

        params.gravity = Gravity.LEFT or Gravity.TOP
    }

    private inner class PinnedLayout(context: Context) : FrameLayout(context) {

        override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
            super.onLayout(changed, left, top, right, bottom)

            updatePinningOffset()
        }
    }
}
