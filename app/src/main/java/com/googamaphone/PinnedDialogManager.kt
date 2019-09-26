package com.googamaphone

import android.os.Bundle
import android.util.SparseArray
import android.view.View

open class PinnedDialogManager {
    private val mPinnedDialogs = SparseArray<PinnedDialog>()

    protected open fun onCreatePinnedDialog(id: Int): PinnedDialog? {
        return null
    }

    protected open fun onPreparePinnedDialog(id: Int, dialog: PinnedDialog?, arguments: Bundle?) {
        // Do nothing.
    }

    @JvmOverloads
    fun showPinnedDialog(id: Int, pinnedView: View, arguments: Bundle? = null) {
        var dialog: PinnedDialog? = mPinnedDialogs.get(id)

        if (dialog == null) {
            dialog = onCreatePinnedDialog(id)
            mPinnedDialogs.put(id, dialog)
        }

        onPreparePinnedDialog(id, dialog, arguments)

        dialog!!.show(pinnedView)
    }

    fun dismissPinnedDialog(id: Int) {
        val dialog = mPinnedDialogs.get(id) ?: return

        dialog.dismiss()
    }

    fun getPinnedView(id: Int): View? {
        val dialog = mPinnedDialogs.get(id) ?: return null

        return dialog.pinnedView
    }
}
