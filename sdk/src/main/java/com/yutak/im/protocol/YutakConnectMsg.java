package com.yutak.im.protocol;



import com.yutak.im.YutakApplication;
import com.yutak.im.cs.Yutak;
import com.yutak.im.kit.CryptoKit;
import com.yutak.im.kit.DateKit;
import com.yutak.im.kit.TypeKit;

/**
 * 2019-11-11 10:22
 * 连接talk service消息
 */
public class YutakConnectMsg extends YutakBaseMsg {
    //设备标示(同标示同账号互踢)
    public byte deviceFlag;
    //设备唯一ID
    public String deviceID;
    //客户端当前时间戳(13位时间戳,到毫秒)
    public long clientTimestamp;
    //用户的token
    public String token;

    //协议版本号长度
    public char protocolVersionLength = 1;
    //设备标示长度
    public char deviceFlagLength = 1;
    //设备id长度
    public char deviceIDLength = 2;
    //token长度所占字节长度
    public char tokenLength = 2;
    //uid长度所占字节长度
    public char uidLength = 2;
    //ClientKey长度所占字节长度
    public char clientKeyLength = 2;
    //时间戳长度
    public char clientTimeStampLength = 8;

    public YutakConnectMsg() {
        token = YutakApplication.get().getToken();
        clientTimestamp = DateKit.get().now();
        packetType = Yutak.MsgType.CONNECT;
        deviceFlag = 0;
        deviceID = YutakApplication.get().getDeviceId();
        //todo take care for this remain length 
        remainingLength = 1 + 1 + 8;//(协议版本号+设备标示(同标示同账号互踢)+客户端当前时间戳(13位时间戳,到毫秒))
    }

    public int getRemainingLength() {
        remainingLength = getFixedHeaderLength()
                + deviceIDLength
                + deviceID.length()
                + uidLength
                + YutakApplication.get().getUid().length()
                + tokenLength
                + YutakApplication.get().getToken().length()
                + clientTimeStampLength
                + clientKeyLength
                + CryptoKit.getInstance().getPublicKey().length();
        return remainingLength;
    }

    public int getTotalLen() {
        byte[] remainingBytes = TypeKit.getInstance().getRemainingLengthByte(getRemainingLength());
        return 1 + remainingBytes.length
                + protocolVersionLength
                + deviceFlagLength
                + deviceIDLength
                + deviceID.length()
                + uidLength
                + YutakApplication.get().getUid().length()
                + tokenLength
                + YutakApplication.get().getToken().length()
                + clientTimeStampLength
                + clientKeyLength
                + CryptoKit.getInstance().getPublicKey().length();
    }
}
