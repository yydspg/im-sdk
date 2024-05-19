package com.yutak.im.interfaces;


import com.yutak.im.domain.YutakCMD;

/**
 * 2/3/21 2:23 PM
 * cmd监听
 */
public interface ICMDListener {
    void onMsg(YutakCMD Yutakcmd);
}
