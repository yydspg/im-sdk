package com.yutak.im.message;


import android.text.TextUtils;

import com.yutak.im.YutakApplication;
import com.yutak.im.YutakIM;
import com.yutak.im.cs.DB;
import com.yutak.im.cs.Yutak;
import com.yutak.im.db.ConversationDBManager;
import com.yutak.im.db.MsgDBManager;
import com.yutak.im.domain.YutakChannelType;
import com.yutak.im.domain.YutakMsg;
import com.yutak.im.domain.YutakSyncMsg;
import com.yutak.im.domain.YutakUIConversationMsg;
import com.yutak.im.interfaces.IReceivedMsgListener;
import com.yutak.im.kit.LogKit;
import com.yutak.im.kit.TypeKit;
import com.yutak.im.manager.CMDManager;
import com.yutak.im.manager.ConversationManager;
import com.yutak.im.protocol.YutakBaseMsg;
import com.yutak.im.protocol.YutakConnectAckMsg;
import com.yutak.im.protocol.YutakDisconnectMsg;
import com.yutak.im.protocol.YutakPongMsg;
import com.yutak.im.protocol.YutakReceivedAckMsg;
import com.yutak.im.protocol.YutakSendAckMsg;

import org.json.JSONException;
import org.json.JSONObject;
import org.xsocket.connection.INonBlockingConnection;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.nio.BufferOverflowException;
import java.nio.channels.ClosedChannelException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

/**
 * 5/21/21 11:25 AM
 * msg handler
 */
public class MessageHandler {
    private MessageHandler() {
    }

    private static class MessageHandlerBinder {
        static final MessageHandler handler = new MessageHandler();
    }

    public static MessageHandler getInstance() {
        return MessageHandlerBinder.handler;
    }

    int sendMessage(INonBlockingConnection connection, YutakBaseMsg msg) {
        if (msg == null) {
            return 1;
        }
        byte[] bytes = YutakProto.getInstance().encodeMsg(msg);
        if (bytes == null || bytes.length == 0) {
            LogKit.get().e("发送未知消息包:" + msg.packetType);
            return 1;
        }

        if (connection != null && connection.isOpen()) {
            try {
                connection.write(bytes, 0, bytes.length);
                connection.flush();
                return 1;
            } catch (BufferOverflowException e) {
                e.printStackTrace();
                LogKit.get().e("sendMessages Exception BufferOverflowException"
                        + e.getMessage());
                return 0;
            } catch (ClosedChannelException e) {
                e.printStackTrace();
                LogKit.get().e("sendMessages Exception ClosedChannelException"
                        + e.getMessage());
                return 0;
            } catch (SocketTimeoutException e) {
                e.printStackTrace();
                LogKit.get().e("sendMessages Exception SocketTimeoutException"
                        + e.getMessage());
                return 0;
            } catch (IOException e) {
                e.printStackTrace();
                LogKit.get().e("sendMessages Exception IOException" + e.getMessage());
                return 0;
            }
        } else {
            LogKit.get().e("sendMessages Exception sendMessage conn null:"
                    + connection);
            return 0;
        }
    }


    private List<YutakSyncMsg> receivedMsgList;
    private byte[] cacheData = null;

    synchronized void cutBytes(byte[] available_bytes,
                               IReceivedMsgListener mIReceivedMsgListener) {

        if (cacheData == null || cacheData.length == 0) cacheData = available_bytes;
        else {
            //如果上次还存在未解析完的消息将新数据追加到缓存数据中
            byte[] temp = new byte[available_bytes.length + cacheData.length];
            try {
                System.arraycopy(cacheData, 0, temp, 0, cacheData.length);
                System.arraycopy(available_bytes, 0, temp, cacheData.length, available_bytes.length);
                cacheData = temp;
            } catch (Exception e) {
                LogKit.get().e("多条消息合并错误" + e.getMessage());
            }

        }
        byte[] lastMsgBytes = cacheData;
        int readLength = 0;

        while (lastMsgBytes.length > 0 && readLength != lastMsgBytes.length) {

            readLength = lastMsgBytes.length;
            int packetType = TypeKit.getInstance().getHeight4(lastMsgBytes[0]);
            // 是否不持久化：0。 是否显示红点：1。是否只同步一次：0
            //是否持久化[是否保存在数据库]
            int no_persist = TypeKit.getInstance().getBit(lastMsgBytes[0], 0);
            //是否显示红点
            int red_dot = TypeKit.getInstance().getBit(lastMsgBytes[0], 1);
            //是否只同步一次
            int sync_once = TypeKit.getInstance().getBit(lastMsgBytes[0], 2);
            LogKit.get().e("是否不存储：" + no_persist + "是否显示红点：" + red_dot + "是否只同步一次：" + sync_once);
            LogKit.get().e("消息包类型" + packetType);
            if (packetType == Yutak.MsgType.PONG) {
                //心跳ack
                mIReceivedMsgListener.pongMsg(new YutakPongMsg());
                LogKit.get().e("pong...");
                byte[] bytes = Arrays.copyOfRange(lastMsgBytes, 1, lastMsgBytes.length);
                cacheData = lastMsgBytes = bytes;
            } else {
                if (packetType < 10) {
                    // 2019-12-21 计算剩余长度
                    if (lastMsgBytes.length < 5) {
                        cacheData = lastMsgBytes;
                        break;
                    }
                    //其他消息类型
                    int remainingLength = TypeKit.getInstance().getRemainingLength(Arrays.copyOfRange(lastMsgBytes, 1, lastMsgBytes.length));
                    if (remainingLength == -1) {
                        //剩余长度被分包
                        cacheData = lastMsgBytes;
                        break;
                    }
                    if (remainingLength > 1 << 21) {
                        cacheData = null;
                        break;
                    }
                    byte[] bytes = TypeKit.getInstance().getRemainingLengthByte(remainingLength);
                    if (remainingLength + 1 + bytes.length > lastMsgBytes.length) {
                        //半包情况
                        cacheData = lastMsgBytes;
                    } else {
                        byte[] msg = Arrays.copyOfRange(lastMsgBytes, 0, remainingLength + 1 + bytes.length);
                        acceptMsg(msg, no_persist, sync_once, red_dot, mIReceivedMsgListener);
                        byte[] temps = Arrays.copyOfRange(lastMsgBytes, msg.length, lastMsgBytes.length);
                        cacheData = lastMsgBytes = temps;
                    }

                } else {
                    cacheData = null;
                    mIReceivedMsgListener.reconnect();
                    break;
                }
            }
        }
        saveReceiveMsg();
    }

    private void acceptMsg(byte[] bytes, int no_persist, int sync_once, int red_dot,
                           IReceivedMsgListener mIReceivedMsgListener) {

        if (bytes != null && bytes.length > 0) {
            YutakBaseMsg g_msg;
            g_msg = YutakProto.getInstance().decodeMessage(bytes);
            if (g_msg != null) {
                //连接ack
                if (g_msg.packetType == Yutak.MsgType.CONNACK) {
                    YutakConnectAckMsg loginStatusMsg = (YutakConnectAckMsg) g_msg;
                    mIReceivedMsgListener.loginStatusMsg(loginStatusMsg.reasonCode);
                    LogKit.get().e("头信息-->" + no_persist);
                } else if (g_msg.packetType == Yutak.MsgType.SENDACK) {
                    //发送ack
                    YutakSendAckMsg sendAckMsg = (YutakSendAckMsg) g_msg;
                    YutakMsg yutakMsg = null;
                    if (no_persist == 0) {
                        yutakMsg = MsgDBManager.getInstance().updateMsgSendStatus(sendAckMsg.clientSeq, sendAckMsg.messageSeq, sendAckMsg.messageID, sendAckMsg.reasonCode);
                    }
                    if (yutakMsg == null) {
                        yutakMsg = new YutakMsg();
                        yutakMsg.clientSeq = sendAckMsg.clientSeq;
                        yutakMsg.messageID = sendAckMsg.messageID;
                        yutakMsg.status = sendAckMsg.reasonCode;
                        yutakMsg.messageSeq = (int) sendAckMsg.messageSeq;
                    }
                    YutakIM.get().getMsgManager().setSendMsgAck(yutakMsg);

                    mIReceivedMsgListener
                            .sendAckMsg(sendAckMsg);
                } else if (g_msg.packetType == Yutak.MsgType.RECVEIVED) {
                    //收到消息
                    YutakMsg message = YutakProto.getInstance().baseMsg2YutakMsg(g_msg);
                    message.header.noPersist = no_persist == 1;
                    message.header.redDot = red_dot == 1;
                    message.header.syncOnce = sync_once == 1;
                    handleReceiveMsg(message);
                    // mIReceivedMsgListener.receiveMsg(message);
                } else if (g_msg.packetType == Yutak.MsgType.DISCONNECT) {
                    //被踢消息
                    YutakDisconnectMsg disconnectMsg = (YutakDisconnectMsg) g_msg;
                    mIReceivedMsgListener.kickMsg(disconnectMsg);
                } else if (g_msg.packetType == Yutak.MsgType.PONG) {
                    mIReceivedMsgListener.pongMsg((YutakPongMsg) g_msg);
                }
            }
        }
    }

    private void handleReceiveMsg(YutakMsg message) {
        message = parsingMsg(message);
        addReceivedMsg(message);
    }

    private synchronized void addReceivedMsg(YutakMsg msg) {

        if (receivedMsgList == null) receivedMsgList = new ArrayList<>();
        YutakSyncMsg syncMsg = new YutakSyncMsg();
        syncMsg.no_persist = msg.header.noPersist ? 1 : 0;
        syncMsg.sync_once = msg.header.syncOnce ? 1 : 0;
        syncMsg.red_dot = msg.header.redDot ? 1 : 0;
        syncMsg.yutakMsg = msg;
        receivedMsgList.add(syncMsg);
    }

    public synchronized void saveReceiveMsg() {

        if (receivedMsgList != null && receivedMsgList.size() > 0) {
            saveSyncMsg(receivedMsgList);

            List<YutakReceivedAckMsg> list = new ArrayList<>();
            for (int i = 0, size = receivedMsgList.size(); i < size; i++) {
                YutakReceivedAckMsg receivedAckMsg = new YutakReceivedAckMsg();
                receivedAckMsg.messageID = receivedMsgList.get(i).yutakMsg.messageID;
                receivedAckMsg.messageSeq = receivedMsgList.get(i).yutakMsg.messageSeq;
                receivedAckMsg.no_persist = receivedMsgList.get(i).no_persist == 1;
                receivedAckMsg.red_dot = receivedMsgList.get(i).red_dot == 1;
                receivedAckMsg.sync_once = receivedMsgList.get(i).sync_once == 1;
                list.add(receivedAckMsg);
            }
            sendAck(list);
            receivedMsgList.clear();
        }
    }

    //回复消息ack
    private void sendAck(List<YutakReceivedAckMsg> list) {
        if (list.size() == 1) {
            YutakConnection.getInstance().sendMessage(list.get(0));
            return;
        }
        final Timer sendAckTimer = new Timer();
        sendAckTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                if (list.size() > 0) {
                    YutakConnection.getInstance().sendMessage(list.get(0));
                    list.remove(0);
                } else {
                    sendAckTimer.cancel();
                }
            }
        }, 0, 100);
    }


    /**
     * 保存同步消息
     *
     * @param list 同步消息对象
     */
    public synchronized void saveSyncMsg(List<YutakSyncMsg> list) {
        List<YutakMsg> saveMsgList = new ArrayList<>();
        List<YutakMsg> allList = new ArrayList<>();
        for (YutakSyncMsg mMsg : list) {
            if (mMsg.no_persist == 0 && mMsg.sync_once == 0) {
                saveMsgList.add(mMsg.yutakMsg);
            }
            allList.add(mMsg.yutakMsg);
        }
        MsgDBManager.getInstance().insertMsgs(saveMsgList);
        //将消息push给UI
        YutakIM.get().getMsgManager().pushNewMsg(allList);
        groupMsg(list);
    }

    private void groupMsg(List<YutakSyncMsg> list) {
        LinkedHashMap<String, SavedMsg> savedList = new LinkedHashMap<>();
        //再将消息分组
        for (int i = 0, size = list.size(); i < size; i++) {
            YutakMsg lastMsg = null;
            int count;

            if (list.get(i).yutakMsg.channelType == YutakChannelType.PERSONAL) {
                //如果是单聊先将channelId改成发送者ID
                if (!TextUtils.isEmpty(list.get(i).yutakMsg.channelID) && !TextUtils.isEmpty(list.get(i).yutakMsg.fromUID) && list.get(i).yutakMsg.channelID.equals(YutakApplication.get().getUid())) {
                    list.get(i).yutakMsg.channelID = list.get(i).yutakMsg.fromUID;
                }
            }

            //将要存库的最后一条消息更新到会话记录表
            if (list.get(i).no_persist == 0
                    && list.get(i).yutakMsg.type != Yutak.MsgContentType.INSIDE_MSG
                    && list.get(i).yutakMsg.isDeleted == 0) {
                lastMsg = list.get(i).yutakMsg;
            }
            count = list.get(i).red_dot;
            if (lastMsg == null) {
//                Log.e("消息不存在", "---->");
                continue;
            }
            JSONObject jsonObject = null;
            if (!TextUtils.isEmpty(list.get(i).yutakMsg.content)) {
                try {
                    jsonObject = new JSONObject(list.get(i).yutakMsg.content);
                } catch (JSONException e) {
                    e.printStackTrace();
                    jsonObject = new JSONObject();
                }
            }
            if (lastMsg.baseContentMsgModel == null) {
                lastMsg.baseContentMsgModel = YutakIM.get().getMsgManager().getMsgContentModel(lastMsg.type, jsonObject);
                if (lastMsg.baseContentMsgModel != null) {
                    lastMsg.flame = lastMsg.baseContentMsgModel.flame;
                    lastMsg.flameSecond = lastMsg.baseContentMsgModel.flameSecond;
                }
            }
            boolean isSave = false;
            if (lastMsg.baseContentMsgModel != null && lastMsg.baseContentMsgModel.mentionAll == 1 && list.get(i).red_dot == 1) {
                isSave = true;
            } else {
                if (lastMsg.baseContentMsgModel != null && lastMsg.baseContentMsgModel.mentionInfo != null && lastMsg.baseContentMsgModel.mentionInfo.uids.size() > 0 && count == 1) {
                    for (int j = 0, len = lastMsg.baseContentMsgModel.mentionInfo.uids.size(); j < len; j++) {
                        if (!TextUtils.isEmpty(lastMsg.baseContentMsgModel.mentionInfo.uids.get(j)) && !TextUtils.isEmpty(YutakApplication.get().getUid()) && lastMsg.baseContentMsgModel.mentionInfo.uids.get(j).equalsIgnoreCase(YutakApplication.get().getUid())) {
                            isSave = true;
                        }
                    }
                }
            }
            if (isSave) {
                //如果存在艾特情况直接将消息存储
                YutakUIConversationMsg conversationMsg = ConversationDBManager.getInstance().insertOrUpdateWithMsg(lastMsg, 1);
                YutakIM.get().getConversationManager().setOnRefreshMsg(conversationMsg, true, "cutData");
                continue;
            }

            SavedMsg savedMsg = null;
            if (savedList.containsKey(lastMsg.channelID + "_" + lastMsg.channelType)) {
                savedMsg = savedList.get(lastMsg.channelID + "_" + lastMsg.channelType);
            }
            if (savedMsg == null) {
                savedMsg = new SavedMsg(lastMsg, count);
            } else {
                savedMsg.yutakMsg = lastMsg;
                savedMsg.redDot = savedMsg.redDot + count;
            }
            savedList.put(lastMsg.channelID + "_" + lastMsg.channelType, savedMsg);
        }

        List<YutakUIConversationMsg> refreshList = new ArrayList<>();
        // TODO: 4/27/21 这里未开事物是因为消息太多太快。事物来不及关闭
        for (Map.Entry<String, SavedMsg> entry : savedList.entrySet()) {
            YutakUIConversationMsg conversationMsg = ConversationDBManager.getInstance().insertOrUpdateWithMsg(entry.getValue().yutakMsg, entry.getValue().redDot);
            if (conversationMsg != null) {
                refreshList.add(conversationMsg);
            }
        }
        for (int i = 0, size = refreshList.size(); i < size; i++) {
            ConversationManager.getInstance().setOnRefreshMsg(refreshList.get(i), i == refreshList.size() - 1, "groupMsg");
        }
    }

    public YutakMsg parsingMsg(YutakMsg message) {
        JSONObject json = null;
        if (message.type != Yutak.MsgContentType.SIGNAL_DECRYPT_ERROR) {
            try {
                if (TextUtils.isEmpty(message.content)) return message;
                json = new JSONObject(message.content);
                if (json.has(DB.MessageColumns.type)) {
                    message.content = json.toString();
                    message.type = json.optInt(DB.MessageColumns.type);
                }
                if (TextUtils.isEmpty(message.fromUID)) {
                    if (json.has(DB.MessageColumns.from_uid)) {
                        message.fromUID = json.optString(DB.MessageColumns.from_uid);
                    } else {
                        message.fromUID = message.channelID;
                    }
                }

            } catch (JSONException e) {
                e.printStackTrace();
                message.type = Yutak.MsgContentType.CONTENT_FORMAT_ERROR;
                LogKit.get().e("解析消息错误，消息非json结构");
            }
        }


        if (json == null) {
            if (message.type != Yutak.MsgContentType.SIGNAL_DECRYPT_ERROR)
                message.type = Yutak.MsgContentType.CONTENT_FORMAT_ERROR;
        }

        if (message.type == Yutak.MsgContentType.INSIDE_MSG) {
            CMDManager.getInstance().handleCMD(json, message.channelID, message.channelType);
        }
        if (message.type != Yutak.MsgContentType.SIGNAL_DECRYPT_ERROR && message.type != Yutak.MsgContentType.CONTENT_FORMAT_ERROR) {
            message.baseContentMsgModel = YutakIM.get().getMsgManager().getMsgContentModel(message.type, json);
            if (message.baseContentMsgModel != null) {
                message.flame = message.baseContentMsgModel.flame;
                message.flameSecond = message.baseContentMsgModel.flameSecond;
            }
        }

        //如果是单聊先将channelId改成发送者ID
        if (!TextUtils.isEmpty(message.channelID)
                && !TextUtils.isEmpty(message.fromUID)
                && message.channelType == YutakChannelType.PERSONAL
                && message.channelID.equals(YutakApplication.get().getUid())) {
            message.channelID = message.fromUID;
        }
        return message;
    }

    public void updateLastSendingMsgFail() {
        MsgDBManager.getInstance().updateAllMsgSendFail();
    }
}
