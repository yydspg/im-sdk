package com.yutak.im.manager;

import android.os.Handler;
import android.os.Looper;

public class BaseManager {

    private Handler mainHandler;

    private boolean isMainThread() {
        return Looper.getMainLooper().getThread() == Thread.currentThread();
    }
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
