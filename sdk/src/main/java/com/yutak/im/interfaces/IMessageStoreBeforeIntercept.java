package com.yutak.im.interfaces;


import com.yutak.im.domain.YutakMsg;

/**
 * 2020-12-04 17:33
 * 存库之前拦截器
 */
public interface IMessageStoreBeforeIntercept {
    boolean isSaveMsg(YutakMsg msg);
}
