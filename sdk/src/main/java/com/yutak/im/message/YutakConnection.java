package com.yutak.im.message;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class YutakConnection {
    private YutakConnection() {
    }

    private static class ConnectHandleBinder {
        private static final YutakConnection CONNECT = new YutakConnection();
    }

    public static YutakConnection getInstance() {
        return ConnectHandleBinder.CONNECT;
    }

    final ExecutorService executors = new ThreadPoolExecutor(1, 4, 1, TimeUnit.SECONDS,
            new LinkedBlockingQueue(100),
            new ThreadPoolExecutor.DiscardOldestPolicy());
    // 正在重连中
    private boolean isReConnecting = false;
    // 连接状态
    private int connectStatus;
    private long lastMsgTime = 0;
    private String ip;
    private int port;
//    volatile  connection;
    volatile ConnectionClient connectionClient;
    private long requestIPTime;
    private final long requestIPTimeoutTime = 6;
    public String socketSingleID;
    private String lastRequestId;
    private final long reconnectDelay = 1500;
    private int unReceivePongCount = 0;
}
