package com.yutak.im.interfaces;


import com.yutak.im.domain.YutakMsg;

import java.util.List;

/**
 * 2019-11-18 11:44
 * 新消息监听
 */
public interface INewMsgListener {
    void newMsg(List<YutakMsg> msgs);
}
