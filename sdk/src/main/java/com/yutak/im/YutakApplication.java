package com.yutak.im;

import android.content.Context;

import com.yutak.im.domain.YutakChannel;

import java.lang.ref.WeakReference;

public class YutakApplication {
    //协议版本
    public final byte defaultProtocolVersion = 4;
    public byte protocolVersion = 4;
    private WeakReference<Context> mContext;
    private static final YutakApplication yutakApplication = new YutakApplication();

    //    private String tempUid;
    private String tempRSAPublicKey;
    private YutakDBHelper mDbHelper;
    public static YutakApplication get() {
        return yutakApplication;
    }

    public Context getContext() {
        if (mContext == null) {
            return null;
        }
        return mContext.get();
    }
    void initContext(Context context) {
        this.mContext = new WeakReference<>(context);
    }

}
