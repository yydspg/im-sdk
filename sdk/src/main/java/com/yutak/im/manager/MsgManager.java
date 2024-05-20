package com.yutak.im.manager;

public class MsgManager {
    private MsgManager() {
    }

    private static class MsgManagerBinder {
        static final MsgManager msgManager = new MsgManager();
    }

    public static MsgManager getInstance() {
        return MsgManagerBinder.msgManager;
    }
}
