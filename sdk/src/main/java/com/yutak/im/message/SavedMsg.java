package com.yutak.im.message;

import com.yutak.im.domain.YutakMsg;

public class SavedMsg {
    public YutakMsg yutakMsg;
    public int redDot;

    public SavedMsg(YutakMsg msg, int redDot) {
        this.redDot = redDot;
        this.yutakMsg = msg;
    }
}
