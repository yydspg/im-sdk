package com.yutak.im.protocol;


import android.text.TextUtils;


import com.yutak.im.YutakApplication;
import com.yutak.im.cs.Yutak;
import com.yutak.im.domain.YutakMsgSetting;
import com.yutak.im.kit.CryptoKit;
import com.yutak.im.kit.TypeKit;

/**
 * 2019-11-11 10:30
 * send 包
 */
public class YutakSendMsg extends YutakBaseMsg {
    //客户端消息序列号(由客户端生成，每个客户端唯一)
    public int clientSeq;
    //频道ID（如果是个人频道ChannelId为个人的UID）
    public String channelId;
    //频道类型（1.个人 2.群组）
    public byte channelType;
    //消息内容
    public String payload;
    //客户端唯一ID
    public String clientMsgNo;
    //客户端唯一ID所占长度
    public int clientMsgNoLength = 2;
    //客户端消息序列号长度
    public int clientSeqLength = 4;
    //频道所占长度
    public short channelIdLength = 2;
    //渠道类型长度
    public char channelTypeLength = 1;
    //消息Key用于验证此消息是否合法
    public short msgKeyLength = 2;
    // 话题ID
    public String topicID;
    public short topicIDLength = 2;
    public YutakMsgSetting setting;
    //消息是否回执长度
    public short settingLength = 1;
    private String cryptoPayload;
    private String msgKey;
    public int expire;
    public int expireLength = 4;

    public YutakSendMsg() {
        packetType = Yutak.MsgType.SEND;
        remainingLength = 8 + 1;
        cryptoPayload = "";
        msgKey = "";
        expire = 0;
    }

    public String getSendContent() {
        if (TextUtils.isEmpty(cryptoPayload)) {
            cryptoPayload = CryptoKit.getInstance().base64Encode(CryptoKit.getInstance().aesEncrypt(payload));
        }
        return cryptoPayload;
    }

    public String getMsgKey() {
        if (TextUtils.isEmpty(msgKey)) {
            String sendContent = getSendContent();
            String key = clientSeq + clientMsgNo + channelId + channelType + sendContent;
            byte[] msgKeyByte = CryptoKit.getInstance().aesEncrypt(key);
            msgKey = CryptoKit.getInstance().digestMD5(CryptoKit.getInstance().base64Encode(msgKeyByte));
        }
        return msgKey;
    }

    private int getTopicLength() {
        int topicLen = 0;
        if (setting.topic == 1) {
            topicLen = topicID.length();
            topicLen += topicIDLength;
        }
        return topicLen;
    }

    private int getExpireLength() {
        // todo 这里已经变更协议了
        if (YutakApplication.get().protocolVersion >= 3) {
            return expireLength;
        }
        return 0;
    }

    public int getTotalLength() {
        int topicLen = getTopicLength();
        int expireLen = getExpireLength();
        String msgKeyContent = getMsgKey();
        String sendContent = getSendContent();
        byte[] remainingBytes = TypeKit.getInstance().getRemainingLengthByte(getRemainingLength());
        return 1 + remainingBytes.length
                + settingLength
                + clientSeqLength
                + clientMsgNoLength
                + clientMsgNo.length()
                + channelIdLength
                + channelId.length()
                + channelTypeLength
                + expireLen
                + msgKeyLength
                + msgKeyContent.length()
                + topicLen
                + sendContent.getBytes().length;
    }


    public int getRemainingLength() {
        String sendContent = getSendContent();
        String msgKeyContent = getMsgKey();
        int topicLen = getTopicLength();
        int expireLen = getExpireLength();
        remainingLength = settingLength
                + clientSeqLength
                + clientMsgNoLength + clientMsgNo.length()
                + channelIdLength + channelId.length()
                + channelTypeLength
                + expireLen
                + msgKeyLength + msgKeyContent.length()
                + topicLen
                + sendContent.getBytes().length;
        return remainingLength;
    }
}
