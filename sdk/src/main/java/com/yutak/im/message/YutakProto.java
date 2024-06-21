package com.yutak.im.message;


import android.text.TextUtils;

import com.yutak.im.YutakApplication;
import com.yutak.im.YutakIM;
import com.yutak.im.cs.DB;
import com.yutak.im.cs.Yutak;
import com.yutak.im.domain.YutakMsg;
import com.yutak.im.kit.CryptoKit;
import com.yutak.im.kit.LogKit;
import com.yutak.im.kit.TypeKit;
import com.yutak.im.msgmodel.YutakMediaMessageContent;
import com.yutak.im.msgmodel.YutakMessageContent;
import com.yutak.im.msgmodel.YutakMsgEntity;
import com.yutak.im.protocol.YutakBaseMsg;
import com.yutak.im.protocol.YutakConnectAckMsg;
import com.yutak.im.protocol.YutakConnectMsg;
import com.yutak.im.protocol.YutakDisconnectMsg;
import com.yutak.im.protocol.YutakPingMsg;
import com.yutak.im.protocol.YutakPongMsg;
import com.yutak.im.protocol.YutakReceivedAckMsg;
import com.yutak.im.protocol.YutakReceivedMsg;
import com.yutak.im.protocol.YutakSendAckMsg;
import com.yutak.im.protocol.YutakSendMsg;


import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;

/**
 * 5/21/21 11:28 AM
 * 收发消息转换
 */
class YutakProto {

    private YutakProto() {
    }

    private static class MessageConvertHandlerBinder {
        static final YutakProto msgConvert = new YutakProto();
    }

    public static YutakProto getInstance() {
        return MessageConvertHandlerBinder.msgConvert;
    }

    byte[] encodeMsg(YutakBaseMsg msg) {
        byte[] bytes = null;
        if (msg.packetType == Yutak.MsgType.CONNECT) {
            // 连接
            bytes = YutakProto.getInstance().enConnectMsg((YutakConnectMsg) msg);
        } else if (msg.packetType == Yutak.MsgType.REVACK) {
            // 收到消息回执
            bytes = YutakProto.getInstance().enReceivedAckMsg((YutakReceivedAckMsg) msg);
        } else if (msg.packetType == Yutak.MsgType.SEND) {
            // 发送聊天消息
            bytes = YutakProto.getInstance().enSendMsg((YutakSendMsg) msg);
        } else if (msg.packetType == Yutak.MsgType.PING) {
            // 发送心跳
            bytes = YutakProto.getInstance().enPingMsg((YutakPingMsg) msg);
            LogKit.get().e("ping...");
        }
        return bytes;
    }

    byte[] enConnectMsg(YutakConnectMsg connectMsg) {
        YutakApplication.get().protocolVersion = YutakApplication.get().defaultProtocolVersion;
        byte[] remainingBytes = TypeKit.getInstance().getRemainingLengthByte(connectMsg.getRemainingLength());
        int totalLen = connectMsg.getTotalLen();
        YutakWrite YutakWrite = new YutakWrite(totalLen);
        try {
            YutakWrite.writeByte(TypeKit.getInstance().getHeader(connectMsg.packetType, connectMsg.flag, 0, 0));
            YutakWrite.writeBytes(remainingBytes);
            YutakWrite.writeByte(YutakApplication.get().protocolVersion);
            YutakWrite.writeByte(connectMsg.deviceFlag);
            YutakWrite.writeString(connectMsg.deviceID);
            YutakWrite.writeString(YutakApplication.get().getUid());
            YutakWrite.writeString(YutakApplication.get().getToken());
            YutakWrite.writeLong(connectMsg.clientTimestamp);
            YutakWrite.writeString(CryptoKit.getInstance().getPublicKey());

        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return YutakWrite.getWriteBytes();
    }

    synchronized byte[] enReceivedAckMsg(YutakReceivedAckMsg receivedAckMsg) {
        byte[] remainingBytes = TypeKit.getInstance().getRemainingLengthByte(8 + 4);

        int totalLen = 1 + remainingBytes.length + 8 + 4;
        YutakWrite YutakWrite = new YutakWrite(totalLen);
        YutakWrite.writeByte(TypeKit.getInstance().getHeader(receivedAckMsg.packetType, receivedAckMsg.no_persist ? 1 : 0, receivedAckMsg.red_dot ? 1 : 0, receivedAckMsg.sync_once ? 1 : 0));
        YutakWrite.writeBytes(remainingBytes);
        BigInteger bigInteger = new BigInteger(receivedAckMsg.messageID);
        YutakWrite.writeLong(bigInteger.longValue());
        YutakWrite.writeInt(receivedAckMsg.messageSeq);
        return YutakWrite.getWriteBytes();
    }

    byte[] enPingMsg(YutakPingMsg pingMsg) {
        YutakWrite YutakWrite = new YutakWrite(1);
        YutakWrite.writeByte(TypeKit.getInstance().getHeader(pingMsg.packetType, pingMsg.flag, 0, 0));
        return YutakWrite.getWriteBytes();
    }

    byte[] enSendMsg(YutakSendMsg sendMsg) {
        // 先加密内容
        String sendContent = sendMsg.getSendContent();
        String msgKeyContent = sendMsg.getMsgKey();
        byte[] remainingBytes = TypeKit.getInstance().getRemainingLengthByte(sendMsg.getRemainingLength());
        int totalLen = sendMsg.getTotalLength();
        YutakWrite YutakWrite = new YutakWrite(totalLen);
        try {
            YutakWrite.writeByte(TypeKit.getInstance().getHeader(sendMsg.packetType, sendMsg.no_persist ? 1 : 0, sendMsg.red_dot ? 1 : 0, sendMsg.sync_once ? 1 : 0));
            YutakWrite.writeBytes(remainingBytes);
            YutakWrite.writeByte(TypeKit.getInstance().getMsgSetting(sendMsg.setting));
            YutakWrite.writeInt(sendMsg.clientSeq);
            YutakWrite.writeString(sendMsg.clientMsgNo);
            YutakWrite.writeString(sendMsg.channelId);
            YutakWrite.writeByte(sendMsg.channelType);
            if (YutakApplication.get().protocolVersion >= 3) {
                YutakWrite.writeInt(sendMsg.expire);
            }
            YutakWrite.writeString(msgKeyContent);
            if (sendMsg.setting.topic == 1) {
                YutakWrite.writeString(sendMsg.topicID);
            }
            YutakWrite.writePayload(sendContent);

        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return YutakWrite.getWriteBytes();
    }

    private YutakConnectAckMsg deConnectAckMsg(YutakRead YutakRead, int hasServerVersion) {
        YutakConnectAckMsg connectAckMsg = new YutakConnectAckMsg();
        try {
            if (hasServerVersion == 1) {
                byte serverVersion = YutakRead.readByte();
                if (serverVersion != 0) {
                    YutakApplication.get().protocolVersion = (byte) Math.min(serverVersion, YutakApplication.get().protocolVersion);
                }
            }
            long time = YutakRead.readLong();
            short reasonCode = YutakRead.readByte();
            String serverKey = YutakRead.readString();
            String salt = YutakRead.readString();
            connectAckMsg.serverKey = serverKey;
            connectAckMsg.salt = salt;
            //保存公钥和安全码
            CryptoKit.getInstance().setServerKeyAndSalt(connectAckMsg.serverKey, connectAckMsg.salt);
            connectAckMsg.timeDiff = time;
            connectAckMsg.reasonCode = reasonCode;
        } catch (IOException e) {
            LogKit.get().e("解码连接ack错误");
        }

        return connectAckMsg;
    }

    private YutakSendAckMsg deSendAckMsg(YutakRead YutakRead) {
        YutakSendAckMsg sendAckMsg = new YutakSendAckMsg();
        try {
            sendAckMsg.messageID = YutakRead.readMsgID();
            sendAckMsg.clientSeq = YutakRead.readInt();
            sendAckMsg.messageSeq = YutakRead.readInt();
            sendAckMsg.reasonCode = YutakRead.readByte();
        } catch (IOException e) {
            LogKit.get().e("解码发送消息ack错误");
        }
        return sendAckMsg;
    }

    private YutakDisconnectMsg deDisconnectMsg(YutakRead YutakRead) {
        YutakDisconnectMsg disconnectMsg = new YutakDisconnectMsg();
        try {
            disconnectMsg.reasonCode = YutakRead.readByte();
            disconnectMsg.reason = YutakRead.readString();
            LogKit.get().e("sdk收到被踢的消息code:" + disconnectMsg.reasonCode + ",reason:" + disconnectMsg.reason);
            return disconnectMsg;
        } catch (IOException e) {
            LogKit.get().e("解码断开连接错误");
        }
        return disconnectMsg;
    }

    private YutakReceivedMsg deReceivedMsg(YutakRead YutakRead) {
        YutakReceivedMsg receivedMsg = new YutakReceivedMsg();
        try {
            byte settingByte = YutakRead.readByte();
            receivedMsg.setting = TypeKit.getInstance().getMsgSetting(settingByte);
            receivedMsg.msgKey = YutakRead.readString();
            receivedMsg.fromUID = YutakRead.readString();
            receivedMsg.channelID = YutakRead.readString();
            receivedMsg.channelType = YutakRead.readByte();
            if (YutakApplication.get().protocolVersion >= 3) {
                receivedMsg.expire = YutakRead.readInt();
            }
            receivedMsg.clientMsgNo = YutakRead.readString();
            if (receivedMsg.setting.stream == 1) {
                receivedMsg.streamNO = YutakRead.readString();
                receivedMsg.streamSeq = YutakRead.readInt();
                receivedMsg.streamFlag = YutakRead.readByte();
            }
            receivedMsg.messageID = YutakRead.readMsgID();
            receivedMsg.messageSeq = YutakRead.readInt();
            receivedMsg.messageTimestamp = YutakRead.readInt();
            if (receivedMsg.setting.topic == 1) {
                receivedMsg.topicID = YutakRead.readString();
            }
            String content = YutakRead.readPayload();
            receivedMsg.payload = CryptoKit.getInstance().aesDecrypt(CryptoKit.getInstance().base64Decode(content));
            String msgKey = receivedMsg.messageID
                    + receivedMsg.messageSeq
                    + receivedMsg.clientMsgNo
                    + receivedMsg.messageTimestamp
                    + receivedMsg.fromUID
                    + receivedMsg.channelID
                    + receivedMsg.channelType
                    + content;
            byte[] result = CryptoKit.getInstance().aesEncrypt(msgKey);
            String base64Result = CryptoKit.getInstance().base64Encode(result);
            String localMsgKey = CryptoKit.getInstance().digestMD5(base64Result);
            if (!localMsgKey.equals(receivedMsg.msgKey)) {
                return null;
            }
            LogKit.get().e("接受到消息:" + receivedMsg.payload);
        } catch (IOException e) {
            LogKit.get().e("解码收到消息错误");
        }
        return receivedMsg;
    }

    YutakBaseMsg decodeMessage(byte[] bytes) {
        try {
            YutakRead YutakRead = new YutakRead(bytes);
            int packetType = YutakRead.readPacketType();
            LogKit.get().e("解码出包类型" + packetType);
            YutakRead.readRemainingLength();
            if (packetType == Yutak.MsgType.CONNACK) {
                int hasServerVersion = TypeKit.getInstance().getBit(bytes[0], 0);
                return deConnectAckMsg(YutakRead, hasServerVersion);
            } else if (packetType == Yutak.MsgType.SENDACK) {
                return deSendAckMsg(YutakRead);
            } else if (packetType == Yutak.MsgType.DISCONNECT) {
                return deDisconnectMsg(YutakRead);
            } else if (packetType == Yutak.MsgType.RECVEIVED) {
                return deReceivedMsg(YutakRead);
            } else if (packetType == Yutak.MsgType.PONG) {
                return new YutakPongMsg();
            } else {
                LogKit.get().e("解析协议类型失败--->：" + packetType);
                return null;
            }
        } catch (IOException e) {
            e.printStackTrace();
            LogKit.get().e("解析数据异常------>：" + e.getMessage());
            return null;
        }
    }

    JSONObject getSendPayload(YutakMsg msg) {
        JSONObject jsonObject = null;
        if (msg.baseContentMsgModel != null) {
            jsonObject = msg.baseContentMsgModel.encodeMsg();
        } else {
            msg.baseContentMsgModel = new YutakMessageContent();
        }
        try {
            if (jsonObject == null) jsonObject = new JSONObject();
            jsonObject.put(DB.MessageColumns.type, msg.type);
            //判断@情况
            if (msg.baseContentMsgModel.mentionInfo != null
                    && msg.baseContentMsgModel.mentionInfo.uids != null
                    && msg.baseContentMsgModel.mentionInfo.uids.size() > 0) {
                JSONArray jsonArray = new JSONArray();
                for (int i = 0, size = msg.baseContentMsgModel.mentionInfo.uids.size(); i < size; i++) {
                    jsonArray.put(msg.baseContentMsgModel.mentionInfo.uids.get(i));
                }
                if (!jsonObject.has("mention")) {
                    JSONObject mentionJson = new JSONObject();
                    mentionJson.put("all", msg.baseContentMsgModel.mentionAll);
                    mentionJson.put("uids", jsonArray);
                    jsonObject.put("mention", mentionJson);
                }

            } else {
                if (msg.baseContentMsgModel.mentionAll == 1) {
                    JSONObject mentionJson = new JSONObject();
                    mentionJson.put("all", msg.baseContentMsgModel.mentionAll);
                    jsonObject.put("mention", mentionJson);
                }
            }
            // 被回复消息
            if (msg.baseContentMsgModel.reply != null) {
                jsonObject.put("reply", msg.baseContentMsgModel.reply.encodeMsg());
            }
            // 机器人ID
            if (!TextUtils.isEmpty(msg.baseContentMsgModel.robotID)) {
                jsonObject.put("robot_id", msg.baseContentMsgModel.robotID);
            }
            if (msg.baseContentMsgModel.entities != null && msg.baseContentMsgModel.entities.size() > 0) {
                JSONArray jsonArray = new JSONArray();
                for (YutakMsgEntity entity : msg.baseContentMsgModel.entities) {
                    JSONObject jo = new JSONObject();
                    jo.put("offset", entity.offset);
                    jo.put("length", entity.length);
                    jo.put("type", entity.type);
                    jo.put("value", entity.value);
                    jsonArray.put(jo);
                }
                jsonObject.put("entities", jsonArray);
            }
            if (msg.flame != 0) {
                jsonObject.put("flame_second", msg.flameSecond);
                jsonObject.put("flame", msg.flame);
            }
            if (msg.baseContentMsgModel.flame != 0) {
                jsonObject.put("flame_second", msg.baseContentMsgModel.flameSecond);
                jsonObject.put("flame", msg.baseContentMsgModel.flame);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return jsonObject;
    }

    /**
     * 获取发送的消息
     *
     * @param msg 本地消息
     * @return 网络消息
     */
    YutakSendMsg getSendBaseMsg(YutakMsg msg) {
        //发送消息
        JSONObject jsonObject = getSendPayload(msg);
        YutakSendMsg sendMsg = new YutakSendMsg();
        // 默认先设置clientSeq，因为有可能本条消息并不需要入库，UI上自己设置了clientSeq
        sendMsg.clientSeq = (int) msg.clientSeq;
        sendMsg.sync_once = msg.header.syncOnce;
        sendMsg.no_persist = msg.header.noPersist;
        sendMsg.red_dot = msg.header.redDot;
        sendMsg.clientMsgNo = msg.clientMsgNO;
        sendMsg.channelId = msg.channelID;
        sendMsg.channelType = msg.channelType;
        sendMsg.topicID = msg.topicID;
        sendMsg.setting = msg.setting;
        sendMsg.expire = msg.expireTime;
        if (YutakMediaMessageContent.class.isAssignableFrom(msg.baseContentMsgModel.getClass())) {
            //多媒体数据
            if (jsonObject.has("localPath")) {
                jsonObject.remove("localPath");
            }
            //视频地址
            if (jsonObject.has("videoLocalPath")) {
                jsonObject.remove("videoLocalPath");
            }
        }
        sendMsg.payload = jsonObject.toString();
        LogKit.get().e(jsonObject.toString());
        return sendMsg;
    }

    YutakMsg baseMsg2YutakMsg(YutakBaseMsg baseMsg) {
        YutakReceivedMsg receivedMsg = (YutakReceivedMsg) baseMsg;
        YutakMsg msg = new YutakMsg();
        msg.channelType = receivedMsg.channelType;
        msg.channelID = receivedMsg.channelID;
        msg.content = receivedMsg.payload;
        msg.messageID = receivedMsg.messageID;
        msg.messageSeq = receivedMsg.messageSeq;
        msg.timestamp = receivedMsg.messageTimestamp;
        msg.fromUID = receivedMsg.fromUID;
        msg.setting = receivedMsg.setting;
        msg.clientMsgNO = receivedMsg.clientMsgNo;
        msg.status = Yutak.SendMsgResult.send_success;
        msg.topicID = receivedMsg.topicID;
        msg.expireTime = receivedMsg.expire;
        if (msg.expireTime > 0) {
            msg.expireTimestamp = msg.expireTime + msg.timestamp;
        }
        msg.orderSeq = YutakIM.get().getMsgManager().getMessageOrderSeq(msg.messageSeq, msg.channelID, msg.channelType);
        msg.isDeleted = isDelete(msg.content);
        return msg;
    }

    private int isDelete(String contentJson) {
        int isDelete = 0;
        if (!TextUtils.isEmpty(contentJson)) {
            try {
                JSONObject jsonObject = new JSONObject(contentJson);
                isDelete = YutakIM.get().getMsgManager().isDeletedMsg(jsonObject);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return isDelete;
    }
}
