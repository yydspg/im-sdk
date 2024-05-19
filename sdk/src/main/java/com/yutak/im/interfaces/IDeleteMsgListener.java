package com.yutak.im.interfaces;


import com.yutak.im.domain.YutakMsg;

/**
 * 2020-08-19 21:42
 * 删除消息监听
 */
public interface IDeleteMsgListener {
    void onDeleteMsg(YutakMsg msg);
}
