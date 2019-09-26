package com.googamaphone.typeandspeak.utils

import java.lang.ref.WeakReference

import android.os.Handler
import android.os.Message

abstract class ReferencedHandler<T>(parent: T) : Handler() {
    private val mParentRef: WeakReference<T> = WeakReference(parent)

    override fun handleMessage(msg: Message) {
        val parent = mParentRef.get()

        if (parent != null) {
            handleMessage(msg, parent)
        }
    }

    protected abstract fun handleMessage(msg: Message, parent: T)
}