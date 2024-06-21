package com.yutak.im.manager;

import android.text.TextUtils;

import com.yutak.im.YutakApplication;
import com.yutak.im.YutakIM;
import com.yutak.im.interfaces.IConnectionStatus;
import com.yutak.im.interfaces.IGetIpAndPort;
import com.yutak.im.kit.LogKit;
import com.yutak.im.message.MessageHandler;
import com.yutak.im.message.YutakConnection;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

// connect manager
public class ConnectionManager extends BaseManager{
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
    public interface IRequestIP {
        void onResult(String requestId, String ip, int port);
    }

    public void getIpAndPort(String requestId, IRequestIP iRequestIP) {
        if (iGetIpAndPort != null) {
            LogKit.get().e("获取IP中...");
            runOnMainThread(() -> iGetIpAndPort.getIP((ip, port) -> iRequestIP.onResult(requestId, ip, port)));
        } else {
            LogKit.get().e("未注册获取IP事件");
        }
    }

    // 监听获取IP和port
    public void addOnGetIpAndPortListener(IGetIpAndPort iGetIpAndPort) {
        this.iGetIpAndPort = iGetIpAndPort;
    }

    public void setConnectionStatus(int status, String reason) {
        if (connectionListenerMap != null && connectionListenerMap.size() > 0) {
            runOnMainThread(() -> {
                for (Map.Entry<String, IConnectionStatus> entry : connectionListenerMap.entrySet()) {
                    entry.getValue().onStatus(status, reason);
                }
            });
        }
    }

    /**
     * 断开连接
     */
    private void stopConnect() {
        YutakApplication.get().isCanConnect = false;
        YutakConnection.getInstance().stopAll();
    }

    /**
     * 退出登录
     */
    private void logoutChat() {
        LogKit.get().e("退出登录设置不能连接");
        YutakApplication.get().isCanConnect = false;
        MessageHandler.getInstance().saveReceiveMsg();

        YutakApplication.get().setToken("");
        MessageHandler.getInstance().updateLastSendingMsgFail();
        YutakConnection.getInstance().stopAll();
        YutakIM.get().getChannelManager().clearARMCache();
        YutakApplication.get().closeDbHelper();
    }
    // 监听连接状态
    public void addOnConnectionStatusListener(String key, IConnectionStatus iConnectionStatus) {
        if (iConnectionStatus == null || TextUtils.isEmpty(key)) return;
        if (connectionListenerMap == null) connectionListenerMap = new ConcurrentHashMap<>();
        connectionListenerMap.put(key, iConnectionStatus);
    }

    // 移除监听
    public void removeOnConnectionStatusListener(String key) {
        if (!TextUtils.isEmpty(key) && connectionListenerMap != null) {
            connectionListenerMap.remove(key);
        }
    }
}
