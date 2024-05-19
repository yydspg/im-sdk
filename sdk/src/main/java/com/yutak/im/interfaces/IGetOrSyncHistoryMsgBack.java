package com.yutak.im.interfaces;


import com.yutak.im.domain.YutakMsg;

import java.util.List;

/**
 * 2020-10-10 13:40
 * 获取或同步消息返回
 */
public interface IGetOrSyncHistoryMsgBack {
    void onSyncing();
    void onResult(List<YutakMsg> msgs);
}
