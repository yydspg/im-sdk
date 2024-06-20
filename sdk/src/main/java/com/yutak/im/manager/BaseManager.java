package com.yutak.im.manager;

import android.os.Handler;
import android.os.Looper;

// make sure it run on base main thread
public class BaseManager {

    private Handler mainHandler;

    private boolean isMainThread() {
        // check
        return Looper.getMainLooper().getThread() == Thread.currentThread();
    }
    // sync
    synchronized void runOnMainThread(ICheckThreadBack iCheckThreadBack) {
        if(iCheckThreadBack == null) return;

        if(!isMainThread()) {
            if(mainHandler == null) mainHandler = new Handler(Looper.getMainLooper());
            mainHandler.post(iCheckThreadBack::onMainThread);
        }else iCheckThreadBack.onMainThread();
    }
    protected interface ICheckThreadBack {
        void onMainThread();
    }

}
