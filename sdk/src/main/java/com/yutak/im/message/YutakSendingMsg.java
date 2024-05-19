package com.yutak.im.message;

import com.yutak.im.kit.DateKit;
import com.yutak.im.protocol.YutakSendMsg;

public class YutakSendingMsg {
    public int sendCount;
    // 发送时间
    public long sendTime;
    // 是否可重发本条消息
    public boolean isCanResend;

    public YutakSendMsg yutakSendMsg;

    public YutakSendingMsg(int sendCount, YutakSendMsg yutakSendMsg, boolean isCanResend) {
        this.sendCount = sendCount;
        this.yutakSendMsg = yutakSendMsg;
        this.isCanResend = isCanResend;
        this.sendTime = DateKit.get().nowSeconds();
    }
}
