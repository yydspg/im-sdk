package com.yutak.im.message;


import com.yutak.im.YutakApplication;
import com.yutak.im.YutakIM;
import com.yutak.im.cs.Yutak;
import com.yutak.im.kit.LogKit;
import com.yutak.im.protocol.YutakPingMsg;

import java.util.Timer;
import java.util.TimerTask;

/**
 * 5/21/21 11:19 AM
 */
class YutakTimers {
    private YutakTimers() {
    }

    private static class ConnectionTimerHandlerBinder {
        static final YutakTimers timeHandle = new YutakTimers();
    }

    public static YutakTimers getInstance() {
        return ConnectionTimerHandlerBinder.timeHandle;
    }


    // 发送心跳定时器
    private Timer heartBeatTimer;
    // 检查心跳定时器
    private Timer checkHeartTimer;
    // 检查网络状态定时器
    private Timer checkNetWorkTimer;
    boolean checkNetWorkTimerIsRunning = false;

    //关闭所有定时器
    void stopAll() {
        stopHeartBeatTimer();
        stopCheckHeartTimer();
        stopCheckNetWorkTimer();
    }

    //开启所有定时器
    void startAll() {
        startHeartBeatTimer();
        startCheckHeartTimer();
        startCheckNetWorkTimer();
    }

    //检测网络
    private void stopCheckNetWorkTimer() {
        if (checkNetWorkTimer != null) {
            checkNetWorkTimer.cancel();
            checkNetWorkTimer.purge();
            checkNetWorkTimer = null;
            checkNetWorkTimerIsRunning = false;
        }
    }

    //检测心跳
    private void stopCheckHeartTimer() {
        if (checkHeartTimer != null) {
            checkHeartTimer.cancel();
            checkHeartTimer.purge();
            checkHeartTimer = null;
        }
    }

    //停止心跳Timer
    private void stopHeartBeatTimer() {
        if (heartBeatTimer != null) {
            heartBeatTimer.cancel();
            heartBeatTimer.purge();
            heartBeatTimer = null;
        }
    }

    //开始心跳
    private void startHeartBeatTimer() {
        stopHeartBeatTimer();
        heartBeatTimer = new Timer();
        // 心跳时间
        int heart_time = 60 * 2;
        heartBeatTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                //发送心跳
                YutakConnection.getInstance().sendMessage(new YutakPingMsg());
            }
        }, 0, heart_time * 1000);
    }

    //开始检查心跳Timer
    private void startCheckHeartTimer() {
        stopCheckHeartTimer();
        checkHeartTimer = new Timer();
        checkHeartTimer.schedule(new TimerTask() {

            @Override
            public void run() {
                if (YutakConnection.getInstance().connection == null || heartBeatTimer == null) {
                    YutakConnection.getInstance().reconnection();
                }
                YutakConnection.getInstance().checkHeartIsTimeOut();
            }
        }, 1000 * 7, 1000 * 7);
    }


    //开启检测网络定时器
    void startCheckNetWorkTimer() {
        stopCheckNetWorkTimer();
        checkNetWorkTimer = new Timer();
        checkNetWorkTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                boolean is_have_network = YutakApplication.get().isNetworkConnected();
                if (!is_have_network) {
                    YutakIM.get().getConnectionManager().setConnectionStatus(Yutak.ConnectStatus.noNetwork, Yutak.ConnectReason.NoNetwork);
                    LogKit.get().e("无网络连接...");
                    YutakConnection.getInstance().checkSendingMsg();
                } else {
                    //有网络
                    if (YutakConnection.getInstance().connectionIsNull())
                        YutakConnection.getInstance().reconnection();
                }
                if (YutakConnection.getInstance().connection == null || !YutakConnection.getInstance().connection.isOpen()) {
                    YutakConnection.getInstance().reconnection();
                }
                checkNetWorkTimerIsRunning = true;
            }
        }, 0, 1000);
    }
}
