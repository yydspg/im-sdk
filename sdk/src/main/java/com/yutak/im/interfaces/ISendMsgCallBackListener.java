package com.yutak.im.interfaces;


import com.yutak.im.domain.YutakMsg;

/**
 * 2020-08-02 00:21
 * 发送消息监听
 */
public interface ISendMsgCallBackListener {
    void onInsertMsg(YutakMsg msg);
}
