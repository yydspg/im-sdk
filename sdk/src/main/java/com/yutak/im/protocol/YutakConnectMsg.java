package com.yutak.im.protocol;


import com.xinbida.wukongim.YutakIMApplication;
import com.yutak.im.cs.Yutak;
import com.xinbida.wukongim.utils.CryptoUtils;
import com.xinbida.wukongim.utils.YutakTypeUtils;

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
        token = YutakIMApplication.getInstance().getToken();
        clientTimestamp = DateUtils.getInstance().getCurrentMills();
        packetType = Yutak.MsgType.CONNECT;
        deviceFlag = 0;
        deviceID = YutakIMApplication.getInstance().getDeviceId();
        remainingLength = 1 + 1 + 8;//(协议版本号+设备标示(同标示同账号互踢)+客户端当前时间戳(13位时间戳,到毫秒))
    }

    public int getRemainingLength() {
        remainingLength = getFixedHeaderLength()
                + deviceIDLength
                + deviceID.length()
                + uidLength
                + YutakIMApplication.getInstance().getUid().length()
                + tokenLength
                + YutakIMApplication.getInstance().getToken().length()
                + clientTimeStampLength
                + clientKeyLength
                + CryptoUtils.getInstance().getPublicKey().length();
        return remainingLength;
    }

    public int getTotalLen() {
        byte[] remainingBytes = YutakTypeUtils.getInstance().getRemainingLengthByte(getRemainingLength());
        return 1 + remainingBytes.length
                + protocolVersionLength
                + deviceFlagLength
                + deviceIDLength
                + deviceID.length()
                + uidLength
                + YutakIMApplication.getInstance().getUid().length()
                + tokenLength
                + YutakIMApplication.getInstance().getToken().length()
                + clientTimeStampLength
                + clientKeyLength
                + CryptoUtils.getInstance().getPublicKey().length();
    }
}
