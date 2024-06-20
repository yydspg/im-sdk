package com.yutak.im.manager;

import android.text.TextUtils;

import com.yutak.im.YutakApplication;
import com.yutak.im.interfaces.IConnectionStatus;
import com.yutak.im.interfaces.IGetIpAndPort;
import com.yutak.im.kit.LogKit;
import com.yutak.im.message.YutakConnection;

import java.util.concurrent.ConcurrentHashMap;

public class ConnectionManager {
    private ConnectionManager() {

    }

    private static class ConnectionManagerBinder {
        static final ConnectionManager connectManager = new ConnectionManager();
    }

    public static ConnectionManager getInstance() {
        return ConnectionManagerBinder.connectManager;
    }
    private IGetIpAndPort iGetIpAndPort;
    // connect status
    private ConcurrentHashMap<String, IConnectionStatus> connectionListenerMap;
    public void connection() {
        if (TextUtils.isEmpty(YutakApplication.get().getToken()) || TextUtils.isEmpty(YutakApplication.get().getUid())) {
            LogKit.get().e("未初始化uid和token");
            return;
        }
        YutakApplication.get().isCanConnect = true;
        if (YutakConnection.getInstance().connectionIsNull()) {
            YutakConnection.getInstance().reconnection();
        }
    }


    public void disconnect(boolean isLogout) {
        if (TextUtils.isEmpty(YutakApplication.get().getToken())) return;
        LogKit.get().e("断开连接，是否退出IM:" + isLogout);
        if (isLogout) {
            logoutChat();
        } else {
            stopConnect();
        }
    }
}
