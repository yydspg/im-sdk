package com.yutak.im.message;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;

import com.yutak.im.YutakApplication;
import com.yutak.im.YutakIM;
import com.yutak.im.cs.Yutak;
import com.yutak.im.db.MsgDBManager;
import com.yutak.im.domain.YutakChannel;
import com.yutak.im.domain.YutakChannelType;
import com.yutak.im.domain.YutakConversationMsgExtra;
import com.yutak.im.domain.YutakMsg;
import com.yutak.im.domain.YutakMsgSetting;
import com.yutak.im.domain.YutakSyncMsgMode;
import com.yutak.im.domain.YutakUIConversationMsg;
import com.yutak.im.interfaces.IReceivedMsgListener;
import com.yutak.im.kit.DateKit;
import com.yutak.im.kit.FileKit;
import com.yutak.im.kit.LogKit;
import com.yutak.im.manager.ConnectionManager;
import com.yutak.im.msgmodel.YutakImageContent;
import com.yutak.im.msgmodel.YutakMediaMessageContent;
import com.yutak.im.msgmodel.YutakMessageContent;
import com.yutak.im.msgmodel.YutakVideoContent;
import com.yutak.im.protocol.YutakBaseMsg;
import com.yutak.im.protocol.YutakConnectMsg;
import com.yutak.im.protocol.YutakDisconnectMsg;
import com.yutak.im.protocol.YutakPingMsg;
import com.yutak.im.protocol.YutakPongMsg;
import com.yutak.im.protocol.YutakSendAckMsg;
import com.yutak.im.protocol.YutakSendMsg;


import org.json.JSONObject;
import org.xsocket.connection.IConnection;
import org.xsocket.connection.INonBlockingConnection;
import org.xsocket.connection.NonBlockingConnection;
import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 5/21/21 10:51 AM
 * IM connect
 */
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

    // 正在发送的消息
    private final ConcurrentHashMap<Integer, YutakSendingMsg> sendingMsgHashMap = new ConcurrentHashMap<>();
    // 正在重连中
    private boolean isReConnecting = false;
    // 连接状态
    private int connectStatus;
    private long lastMsgTime = 0;
    private String ip;
    private int port;
    volatile INonBlockingConnection connection;
    volatile ConnectionClient connectionClient;
    private long requestIPTime;
    private final long requestIPTimeoutTime = 6;
    public String socketSingleID;
    private String lastRequestId;
    private final long reconnectDelay = 1500;
    private int unReceivePongCount = 0;
    public volatile Handler reconnectionHandler = new Handler(Objects.requireNonNull(Looper.myLooper()));

    Runnable reconnectionRunnable = this::reconnection;

    public synchronized void forcedReconnection() {
        isReConnecting = false;
        requestIPTime = 0;
        reconnection();
    }

    public synchronized void reconnection() {
        ip = "";
        port = 0;
        if (isReConnecting) {
            long nowTime = DateKit.get().nowSeconds();
            if (nowTime - requestIPTime > requestIPTimeoutTime) {
                isReConnecting = false;
            }
            return;
        }
        connectStatus = Yutak.ConnectStatus.fail;
        reconnectionHandler.removeCallbacks(reconnectionRunnable);
        boolean isHaveNetwork = YutakApplication.get().isNetworkConnected();
        if (isHaveNetwork) {
            closeConnect();
            isReConnecting = true;
            requestIPTime = DateKit.get().nowSeconds();
            getIPAndPort();
        } else {
            if (!YutakTimers.getInstance().checkNetWorkTimerIsRunning) {
                YutakIM.get().getConnectionManager().setConnectionStatus(Yutak.ConnectStatus.noNetwork, Yutak.ConnectReason.NoNetwork);
                isReConnecting = false;
                reconnectionHandler.postDelayed(reconnectionRunnable, reconnectDelay);
            }
        }
    }

    private synchronized void getIPAndPort() {
        if (!YutakApplication.get().isNetworkConnected()) {
            isReConnecting = false;
            reconnectionHandler.postDelayed(reconnectionRunnable, reconnectDelay);
            return;
        }
        if (!YutakApplication.get().isCanConnect) {
            LogKit.get().e("sdk判断不能重连-->");
            stopAll();
            return;
        }
        YutakIM.get().getConnectionManager().setConnectionStatus(Yutak.ConnectStatus.connecting, Yutak.ConnectReason.Connecting);
        // 计算获取IP时长 todo
        startRequestIPTimer();
        lastRequestId = UUID.randomUUID().toString().replace("-", "");
        ConnectionManager.getInstance().getIpAndPort(lastRequestId, (requestId, ip, port) -> {
            if (TextUtils.isEmpty(ip) || port == 0) {
                LogKit.get().e("返回连接IP或port错误，" + String.format("ip:%s & port:%s", ip, port));
                isReConnecting = false;
                reconnectionHandler.postDelayed(reconnectionRunnable, reconnectDelay);
            } else {
                if (lastRequestId.equals(requestId)) {
                    YutakConnection.this.ip = ip;
                    YutakConnection.this.port = port;
                    LogKit.get().e("连接地址" + ip + ":" + port);
                    if (connectionIsNull()) {
                        executors.execute(YutakConnection.this::connSocket);
                        //  new Thread(YutakConnection.this::connSocket).start();
                    }
                } else {
                    if (connectionIsNull()) {
                        LogKit.get().e("请求IP的编号不一致，重连中");
                        reconnectionHandler.postDelayed(reconnectionRunnable, reconnectDelay);
                    }
                }
            }
        });
    }

    private void connSocket() {
        closeConnect();
        try {
            socketSingleID = UUID.randomUUID().toString().replace("-", "");
            connectionClient = new ConnectionClient();
//            InetAddress inetAddress = InetAddress.getByName(ip);
            connection = new NonBlockingConnection(ip, port, connectionClient);
            connection.setAttachment(socketSingleID);
            connection.setIdleTimeoutMillis(1000 * 3);
            connection.setConnectionTimeoutMillis(1000 * 3);
            connection.setFlushmode(IConnection.FlushMode.ASYNC);
            isReConnecting = false;
            if (connection != null)
                connection.setAutoflush(true);
        } catch (Exception e) {
            isReConnecting = false;
            LogKit.get().e("连接异常:" + e.getMessage());
            reconnection();
            e.printStackTrace();
        }
    }

    //发送连接消息
    void sendConnectMsg() {
        sendMessage(new YutakConnectMsg());
    }

    void receivedData(byte[] data) {
        MessageHandler.getInstance().cutBytes(data,
                new IReceivedMsgListener() {

                    public void sendAckMsg(
                            YutakSendAckMsg talkSendStatus) {
                        // 删除队列中正在发送的消息对象
                        YutakSendingMsg object = sendingMsgHashMap.get(talkSendStatus.clientSeq);
                        if (object != null) {
                            object.isCanResend = false;
                            sendingMsgHashMap.put(talkSendStatus.clientSeq, object);
                        }
                    }


                    @Override
                    public void reconnect() {
                        YutakApplication.get().isCanConnect = true;
                        reconnection();
                    }

                    @Override
                    public void loginStatusMsg(short status_code) {
                        handleLoginStatus(status_code);
                    }

                    @Override
                    public void pongMsg(YutakPongMsg msgHeartbeat) {
                        // 心跳消息
                        lastMsgTime = DateKit.get().nowSeconds();
                        unReceivePongCount = 0;
                    }

                    @Override
                    public void kickMsg(YutakDisconnectMsg disconnectMsg) {
                        YutakIM.get().getConnectionManager().disconnect(true);
                        YutakIM.get().getConnectionManager().setConnectionStatus(Yutak.ConnectStatus.kicked, Yutak.ConnectReason.ReasonConnectKick);
                    }

                });
    }


    //重发未发送成功的消息
    public void resendMsg() {
        removeSendingMsg();
        new Thread(() -> {
            for (Map.Entry<Integer, YutakSendingMsg> entry : sendingMsgHashMap.entrySet()) {
                if (entry.getValue().isCanResend) {
                    sendMessage(Objects.requireNonNull(sendingMsgHashMap.get(entry.getKey())).yutakSendMsg);
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }).start();

    }

    //将要发送的消息添加到队列
    private synchronized void addSendingMsg(YutakSendMsg sendingMsg) {
        removeSendingMsg();
        sendingMsgHashMap.put(sendingMsg.clientSeq, new YutakSendingMsg(1, sendingMsg, true));
    }

    //处理登录消息状态
    private void handleLoginStatus(short status) {
        LogKit.get().e("IM连接返回状态:" + status);
        String reason = Yutak.ConnectReason.ConnectSuccess;
        if (status == Yutak.ConnectStatus.kicked) {
            reason = Yutak.ConnectReason.ReasonAuthFail;
        }
        YutakIM.get().getConnectionManager().setConnectionStatus(status, reason);
        if (status == Yutak.ConnectStatus.success) {
            //等待中
            connectStatus = Yutak.ConnectStatus.success;
            YutakTimers.getInstance().startAll();
            resendMsg();
            YutakIM.get().getConnectionManager().setConnectionStatus(Yutak.ConnectStatus.syncMsg, Yutak.ConnectReason.SyncMsg);
            // 判断同步模式
            if (YutakApplication.get().getSyncMsgMode() == YutakSyncMsgMode.WRITE) {
                YutakIM.get().getMsgManager().setSyncOfflineMsg((isEnd, list) -> {
                    if (isEnd) {
                        MessageHandler.getInstance().saveReceiveMsg();
                        YutakApplication.get().isCanConnect = true;
                        YutakIM.get().getConnectionManager().setConnectionStatus(Yutak.ConnectStatus.success, Yutak.ConnectReason.ConnectSuccess);
                    }
                });
            } else {
                LogKit.get().e("通知UI同步会话-->");
                YutakIM.get().getConversationManager().setSyncConversationListener(syncChat -> {
                    YutakApplication.get().isCanConnect = true;
                    LogKit.get().e("同步会话完成-->");
                    YutakIM.get().getConnectionManager().setConnectionStatus(Yutak.ConnectStatus.success, Yutak.ConnectReason.ConnectSuccess);
                });
            }
        } else if (status == Yutak.ConnectStatus.kicked) {
            LogKit.get().e("解析登录返回被踢设置不能连接");
            MessageHandler.getInstance().updateLastSendingMsgFail();
            YutakApplication.get().isCanConnect = false;
            stopAll();
        } else {
            LogKit.get().e("sdk解析登录返回错误类型:" + status);
            stopAll();
            reconnection();
        }
    }

    void sendMessage(YutakBaseMsg mBaseMsg) {
        if (mBaseMsg == null) return;
        if (mBaseMsg.packetType != Yutak.MsgType.CONNECT) {
            if (connectStatus != Yutak.ConnectStatus.success) {
                return;
            }
        }
        if (mBaseMsg.packetType == Yutak.MsgType.PING) {
            unReceivePongCount++;
        }
        if (connection == null || !connection.isOpen()) {
            reconnection();
            return;
        }
        int status = MessageHandler.getInstance().sendMessage(connection, mBaseMsg);
        if (status == 0) {
            LogKit.get().e("发送消息失败");
            reconnection();
        }
    }

    // 查看心跳是否超时
    void checkHeartIsTimeOut() {
        if (unReceivePongCount >= 5) {
            reconnectionHandler.postDelayed(reconnectionRunnable, reconnectDelay);
            return;
        }
        long nowTime = DateKit.get().nowSeconds();
        if (nowTime - lastMsgTime >= 60) {
            sendMessage(new YutakPingMsg());
        }
    }

    private void removeSendingMsg() {
        if (sendingMsgHashMap.size() > 0) {
            Iterator<Map.Entry<Integer, YutakSendingMsg>> it = sendingMsgHashMap.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<Integer, YutakSendingMsg> entry = it.next();
                if (!entry.getValue().isCanResend) {
                    it.remove();
                }
            }
        }
    }

    //检测正在发送的消息
    synchronized void checkSendingMsg() {
        removeSendingMsg();
        if (sendingMsgHashMap.size() > 0) {
            Iterator<Map.Entry<Integer, YutakSendingMsg>> it = sendingMsgHashMap.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<Integer, YutakSendingMsg> item = it.next();
                YutakSendingMsg YutakSendingMsg = sendingMsgHashMap.get(item.getKey());
                if (YutakSendingMsg != null) {
                    if (YutakSendingMsg.sendCount == 5 && YutakSendingMsg.isCanResend) {
                        //标示消息发送失败
                        MsgDBManager.getInstance().updateMsgStatus(item.getKey(), Yutak.SendMsgResult.send_fail);
                        it.remove();
                        YutakSendingMsg.isCanResend = false;
                        LogKit.get().e("消息发送失败...");
                    } else {
                        long nowTime = DateKit.get().nowSeconds();
                        if (nowTime - YutakSendingMsg.sendTime > 10) {
                            YutakSendingMsg.sendTime = DateKit.get().nowSeconds();
                            sendingMsgHashMap.put(item.getKey(), YutakSendingMsg);
                            YutakSendingMsg.sendCount++;
                            sendMessage(Objects.requireNonNull(sendingMsgHashMap.get(item.getKey())).yutakSendMsg);
                            LogKit.get().e("消息发送失败...");
                        }
                    }
                }
            }
        }
    }

    public void sendMessage(YutakMessageContent baseContentModel, YutakMsgSetting YutakMsgSetting, String channelID, byte channelType) {
        final YutakMsg YutakMsg = new YutakMsg();
        YutakMsg.type = baseContentModel.type;
        YutakMsg.setting = YutakMsgSetting;
        //设置会话信息
        YutakMsg.channelID = channelID;
        YutakMsg.channelType = channelType;
        //检查频道信息
        YutakMsg.baseContentMsgModel = baseContentModel;
        YutakMsg.baseContentMsgModel.fromUID = YutakMsg.fromUID;
        YutakMsg.flame = baseContentModel.flame;
        YutakMsg.flameSecond = baseContentModel.flameSecond;
        YutakMsg.topicID = baseContentModel.topicID;
        sendMessage(YutakMsg);
    }

    /**
     * 发送消息
     *
     * @param baseContentModel 消息model
     * @param channelID        频道ID
     * @param channelType      频道类型
     */
    public void sendMessage(YutakMessageContent baseContentModel, String channelID, byte channelType) {
        YutakMsgSetting setting = new YutakMsgSetting();
        sendMessage(baseContentModel, setting, channelID, channelType);
    }

    public void sendMessage(YutakMsg msg) {
        if (TextUtils.isEmpty(msg.fromUID)) {
            msg.fromUID = YutakApplication.get().getUid();
        }
        if (msg.expireTime > 0) {
            msg.expireTimestamp = DateKit.get().nowSeconds() + msg.expireTime;
        }
        boolean hasAttached = false;
        //如果是图片消息
        if (msg.baseContentMsgModel instanceof YutakImageContent) {
            YutakImageContent imageContent = (YutakImageContent) msg.baseContentMsgModel;
            if (!TextUtils.isEmpty(imageContent.localPath)) {
                try {
                    File file = new File(imageContent.localPath);
                    if (file.exists() && file.length() > 0) {
                        hasAttached = true;
                        Bitmap bitmap = BitmapFactory.decodeFile(imageContent.localPath);
                        if (bitmap != null) {
                            imageContent.width = bitmap.getWidth();
                            imageContent.height = bitmap.getHeight();
                            msg.baseContentMsgModel = imageContent;
                        }
                    }
                } catch (Exception ignored) {
                }
            }
        }
        //视频消息
        if (msg.baseContentMsgModel instanceof YutakVideoContent) {
            YutakVideoContent videoContent = (YutakVideoContent) msg.baseContentMsgModel;
            if (!TextUtils.isEmpty(videoContent.localPath)) {
                try {
                    File file = new File(videoContent.coverLocalPath);
                    if (file.exists() && file.length() > 0) {
                        hasAttached = true;
                        Bitmap bitmap = BitmapFactory.decodeFile(videoContent.coverLocalPath);
                        if (bitmap != null) {
                            videoContent.width = bitmap.getWidth();
                            videoContent.height = bitmap.getHeight();
                            msg.baseContentMsgModel = videoContent;
                        }
                    }
                } catch (Exception ignored) {

                }
            }

        }
        saveSendMsg(msg);
        YutakSendMsg sendMsg = YutakProto.getInstance().getSendBaseMsg(msg);
//        if (base != null && msg.clientSeq == 0) {
//            msg.clientSeq = base.clientSeq;
//        }

        if (YutakMediaMessageContent.class.isAssignableFrom(msg.baseContentMsgModel.getClass())) {
            //如果是多媒体消息类型说明存在附件
            String url = ((YutakMediaMessageContent) msg.baseContentMsgModel).url;
            if (TextUtils.isEmpty(url)) {
                String localPath = ((YutakMediaMessageContent) msg.baseContentMsgModel).localPath;
                if (!TextUtils.isEmpty(localPath)) {
                    hasAttached = true;
                    ((YutakMediaMessageContent) msg.baseContentMsgModel).localPath = FileKit.getFileKit().save(localPath, msg.channelID, msg.channelType, msg.clientSeq + "");
                }
            }
            if (msg.baseContentMsgModel instanceof YutakVideoContent) {
                String coverLocalPath = ((YutakVideoContent) msg.baseContentMsgModel).coverLocalPath;
                if (!TextUtils.isEmpty(coverLocalPath)) {
                    ((YutakVideoContent) msg.baseContentMsgModel).coverLocalPath = FileKit.getFileKit().save(coverLocalPath, msg.channelID, msg.channelType, msg.clientSeq + "_1");
                    hasAttached = true;
                }
            }
            if (hasAttached) {
                msg.content = msg.baseContentMsgModel.encodeMsg().toString();
                YutakIM.get().getMsgManager().updateContentAndRefresh(msg.clientMsgNO, msg.baseContentMsgModel, false);
            }
        }
        //获取发送者信息
        YutakChannel from = YutakIM.get().getChannelManager().getChannel(YutakApplication.get().getUid(), YutakChannelType.PERSONAL);
        if (from == null) {
            YutakIM.get().getChannelManager().getChannel(YutakApplication.get().getUid(), YutakChannelType.PERSONAL, channel -> YutakIM.get().getChannelManager().saveOrUpdateChannel(channel));
        } else {
            msg.setFrom(from);
        }
        //将消息push回UI层
        YutakIM.get().getMsgManager().setSendMsgCallback(msg);
        if (hasAttached) {
            //存在附件处理
            YutakIM.get().getMsgManager().setUploadAttachment(msg, (isSuccess, messageContent) -> {
                if (isSuccess) {
                    msg.baseContentMsgModel = messageContent;
                    YutakIM.get().getMsgManager().updateContentAndRefresh(msg.clientMsgNO, msg.baseContentMsgModel, false);
                    if (!sendingMsgHashMap.containsKey((int) msg.clientSeq)) {
                        YutakSendMsg base1 = YutakProto.getInstance().getSendBaseMsg(msg);
                        addSendingMsg(base1);
                        sendMessage(base1);
                    }
                } else {
                    MsgDBManager.getInstance().updateMsgStatus(msg.clientSeq, Yutak.SendMsgResult.send_fail);
                }
            });
        } else {
            if (sendMsg != null) {
                if (msg.header != null && !msg.header.noPersist) {
                    addSendingMsg(sendMsg);
                }
                sendMessage(sendMsg);
            }
        }
    }

    public boolean connectionIsNull() {
        return connection == null || !connection.isOpen();
    }

    public void stopAll() {
        connectionClient = null;
        YutakTimers.getInstance().stopAll();
        closeConnect();
        connectStatus = Yutak.ConnectStatus.fail;
        isReConnecting = false;
        System.gc();
    }

    private void closeConnect() {
        if (connection != null && connection.isOpen()) {
            try {
                LogKit.get().e("stop connection:" + connection.getId());
//                connection.flush();
                connection.setAttachment("close" + connection.getId());
                connection.close();
            } catch (IOException e) {
                e.printStackTrace();
                LogKit.get().e("stop connection IOException" + e.getMessage());
            }
        }
        connection = null;
    }

    private Timer checkNetWorkTimer;

    private synchronized void startRequestIPTimer() {
        if (checkNetWorkTimer != null) {
            checkNetWorkTimer.cancel();
            checkNetWorkTimer = null;
        }
        checkNetWorkTimer = new Timer();
        LogKit.get().e("开始计算IP请求时间");
        checkNetWorkTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                long nowTime = DateKit.get().nowSeconds();
                if (nowTime - requestIPTime >= requestIPTimeoutTime) {
                    checkNetWorkTimer.cancel();
                    checkNetWorkTimer.purge();
                    checkNetWorkTimer = null;
                    if (TextUtils.isEmpty(ip) || port == 0) {
                        LogKit.get().e("请求IP已超时，开始重连--->");
                        isReConnecting = false;
                        reconnection();
                    }
                } else {
                    if (!TextUtils.isEmpty(ip) && port != 0) {
                        checkNetWorkTimer.cancel();
                        checkNetWorkTimer.purge();
                        checkNetWorkTimer = null;
                        LogKit.get().e("请求IP倒计时已销毁--->");
                    } else {
                        LogKit.get().e("请求IP倒计时中--->" + (nowTime - requestIPTime));
                    }
                }
            }
        }, 500, 1000L);
    }

    private YutakMsg saveSendMsg(YutakMsg msg) {
        if (msg.setting == null) msg.setting = new YutakMsgSetting();
        JSONObject jsonObject = YutakProto.getInstance().getSendPayload(msg);
        msg.content = jsonObject.toString();
        long tempOrderSeq = MsgDBManager.getInstance().queryMaxOrderSeqWithChannel(msg.channelID, msg.channelType);
        msg.orderSeq = tempOrderSeq + 1;
        // 需要存储的消息入库后更改消息的clientSeq
        if (!msg.header.noPersist) {
            msg.clientSeq = (int) MsgDBManager.getInstance().insert(msg);
            if (msg.clientSeq > 0) {
                YutakUIConversationMsg uiMsg = YutakIM.get().getConversationManager().updateWithYutakMsg(msg);
                if (uiMsg != null) {
                    long browseTo = YutakIM.get().getMsgManager().getMaxMessageSeqWithChannel(uiMsg.channelID, uiMsg.channelType);
                    if (uiMsg.getRemoteMsgExtra() == null) {
                        uiMsg.setRemoteMsgExtra(new YutakConversationMsgExtra());
                    }
                    uiMsg.getRemoteMsgExtra().browseTo = browseTo;
                    YutakIM.get().getConversationManager().setOnRefreshMsg(uiMsg, true, "getSendBaseMsg");
                }
            }
        }
        return msg;
    }
}
