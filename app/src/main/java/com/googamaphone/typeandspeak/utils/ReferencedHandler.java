package com.googamaphone.typeandspeak.utils;

import java.lang.ref.WeakReference;

import android.os.Handler;
import android.os.Message;
import androidx.annotation.NonNull;

public abstract class ReferencedHandler<T> extends Handler {
    private final WeakReference<T> mParentRef;
    
    public ReferencedHandler(T parent) {
        mParentRef = new WeakReference<>(parent);
    }
    
    @Override
    public final void handleMessage(@NonNull Message msg) {
        final T parent = mParentRef.get();
        
        if (parent != null) {
            handleMessage(msg, parent);
        }
    }
    
    protected abstract void handleMessage(Message msg, T parent);
}